package buildaspell.network;

import buildaspell.enchanting.EnchantmentCost;
import buildaspell.enchanting.EnchantmentCostManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.Optional;

public record ArcaneAltarEnchantPacket(String enchantmentId, int level) implements CustomPacketPayload {
    public static final Type<ArcaneAltarEnchantPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("buildaspell", "arcane_altar_enchant"));

    public static final StreamCodec<ByteBuf, ArcaneAltarEnchantPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ArcaneAltarEnchantPacket::enchantmentId,
            ByteBufCodecs.INT,
            ArcaneAltarEnchantPacket::level,
            ArcaneAltarEnchantPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ArcaneAltarEnchantPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;

            // Validate the player has the altar menu open
            if (!(serverPlayer.containerMenu instanceof buildaspell.menu.ArcaneAltarMenu altarMenu)) {
                return;
            }

            // Get the item in the altar slot (slot 0)
            ItemStack targetItem = altarMenu.getSlot(0).getItem();
            if (targetItem.isEmpty()) {
                serverPlayer.sendSystemMessage(Component.literal("Place an item in the altar first").withStyle(ChatFormatting.RED));
                return;
            }

            // Validate level range
            if (packet.level() < 1 || packet.level() > 20) return;

            // Look up enchantment from registry
            ResourceLocation enchantId;
            try {
                enchantId = ResourceLocation.parse(packet.enchantmentId());
            } catch (Exception e) {
                serverPlayer.sendSystemMessage(Component.literal("Invalid enchantment ID").withStyle(ChatFormatting.RED));
                return;
            }
            ResourceKey<Enchantment> enchantmentKey = ResourceKey.create(Registries.ENCHANTMENT, enchantId);
            Optional<Holder.Reference<Enchantment>> enchantmentHolder =
                    serverPlayer.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantmentKey);

            if (enchantmentHolder.isEmpty()) {
                serverPlayer.sendSystemMessage(Component.literal("Unknown enchantment").withStyle(ChatFormatting.RED));
                return;
            }

            // Authoritative pairing gate, ahead of every charge: ItemStack#enchant does not consult
            // supported_items, so without this the altar will happily sell Mana Pool on a wand or
            // Spell Power on a chestplate — glint, tooltip and all — for an enchantment nothing
            // ever reads back. The screen greys the same combinations out; this is what enforces it.
            if (!buildaspell.block.entity.ArcaneAltarBlockEntity.isValidEnchantTarget(enchantId, targetItem)) {
                serverPlayer.sendSystemMessage(
                        buildaspell.block.entity.ArcaneAltarBlockEntity.enchantTargetRequirement(enchantId)
                                .withStyle(ChatFormatting.RED));
                return;
            }

            // Cumulative cost: climbing from the item's current level to the target charges
            // every level along the way, so you can't cheaply skip straight to max.
            int currentLevel = net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getItemEnchantmentLevel(enchantmentHolder.get(), targetItem);
            if (packet.level() <= currentLevel) {
                serverPlayer.sendSystemMessage(Component.literal("Item is already at that level or higher")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            EnchantmentCostManager.CumulativeCost cost =
                    EnchantmentCostManager.getCumulativeCost(packet.enchantmentId(), currentLevel, packet.level());

            if (EnchantmentCostManager.totalXpPoints(serverPlayer) < cost.xpPoints()) {
                serverPlayer.sendSystemMessage(Component.literal("Insufficient experience")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            // Tally what the ingredient slots (container slots 1..4) hold, then check the bill.
            int start = buildaspell.block.entity.ArcaneAltarBlockEntity.INGREDIENT_START;
            int end = start + buildaspell.block.entity.ArcaneAltarBlockEntity.INGREDIENT_COUNT;
            for (Map.Entry<net.minecraft.world.item.Item, Integer> need : cost.items().entrySet()) {
                int have = 0;
                for (int i = start; i < end; i++) {
                    ItemStack stack = altarMenu.getSlot(i).getItem();
                    if (stack.getItem() == need.getKey()) have += stack.getCount();
                }
                if (have < need.getValue()) {
                    serverPlayer.sendSystemMessage(Component.literal("Missing materials — check the ingredient slots")
                            .withStyle(ChatFormatting.RED));
                    return;
                }
            }

            // Consume the materials from the ingredient slots and the XP points.
            for (Map.Entry<net.minecraft.world.item.Item, Integer> need : cost.items().entrySet()) {
                int remaining = need.getValue();
                for (int i = start; i < end && remaining > 0; i++) {
                    ItemStack stack = altarMenu.getSlot(i).getItem();
                    if (stack.getItem() == need.getKey()) {
                        int take = Math.min(remaining, stack.getCount());
                        stack.shrink(take);
                        remaining -= take;
                        altarMenu.getSlot(i).set(stack);
                    }
                }
            }
            serverPlayer.giveExperiencePoints(-cost.xpPoints());

            // Apply enchantment (set() re-writes the slot so the block entity broadcasts
            // the update and the floating item above the altar shows the new glint)
            targetItem.enchant(enchantmentHolder.get(), packet.level());
            altarMenu.getSlot(0).set(targetItem);

            serverPlayer.sendSystemMessage(Component.literal("Enchantment applied!").withStyle(ChatFormatting.GREEN));
        });
    }
}
