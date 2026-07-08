package buildaspell.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import buildaspell.network.OpenSpellBuilderScreenPacket;
import buildaspell.network.SyncPlayerSpellDataPacket;
import buildaspell.network.SyncPlayerSpellSlotsPacket;
import buildaspell.registry.ModAttachments;
import buildaspell.spell.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpellCommand {
    private static final SuggestionProvider<CommandSourceStack> DELIVERY_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(DeliveryMethod.values()).map(DeliveryMethod::getSerializedName),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> EFFECT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(SpellEffect.values()).map(SpellEffect::getSerializedName),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> MODIFIER_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(SpellModifier.values()).map(SpellModifier::getSerializedName),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> COMBO_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(SpellCombo.values()).map(SpellCombo::getId),
                    builder
            );

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("spell")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("gui")
                        .executes(context -> openGui(context, context.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("unlock")
                        .then(Commands.literal("all")
                                .executes(context -> unlockAll(context, context.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> unlockAll(context, EntityArgument.getPlayer(context, "target")))
                                )
                        )
                        .then(Commands.literal("delivery")
                                .then(Commands.argument("delivery_id", StringArgumentType.string())
                                        .suggests(DELIVERY_SUGGESTIONS)
                                        .executes(context -> unlockDelivery(context, context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "delivery_id")))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> unlockDelivery(context, EntityArgument.getPlayer(context, "target"), StringArgumentType.getString(context, "delivery_id")))
                                        )
                                )
                        )
                        .then(Commands.literal("effect")
                                .then(Commands.argument("effect_id", StringArgumentType.string())
                                        .suggests(EFFECT_SUGGESTIONS)
                                        .executes(context -> unlockEffect(context, context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "effect_id")))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> unlockEffect(context, EntityArgument.getPlayer(context, "target"), StringArgumentType.getString(context, "effect_id")))
                                        )
                                )
                        )
                        .then(Commands.literal("modifier")
                                .then(Commands.argument("modifier_id", StringArgumentType.string())
                                        .suggests(MODIFIER_SUGGESTIONS)
                                        .executes(context -> unlockModifier(context, context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "modifier_id")))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> unlockModifier(context, EntityArgument.getPlayer(context, "target"), StringArgumentType.getString(context, "modifier_id")))
                                        )
                                )
                        )
                )
                .then(Commands.literal("list")
                        .executes(context -> listUnlocked(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> listUnlocked(context, EntityArgument.getPlayer(context, "target")))
                        )
                )
                .then(Commands.literal("export")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 10))
                                .executes(context -> exportSpell(context, context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "slot") - 1))
                        )
                )
                .then(Commands.literal("import")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 10))
                                .then(Commands.argument("code", StringArgumentType.greedyString())
                                        .executes(context -> importSpell(context, context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "slot") - 1, StringArgumentType.getString(context, "code")))
                                )
                        )
                )
                .then(Commands.literal("copy")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 10))
                                .executes(context -> copySpell(context, context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "slot") - 1))
                        )
                )
                .then(Commands.literal("testkit")
                        .executes(context -> giveTestKit(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> giveTestKit(context, EntityArgument.getPlayer(context, "target")))
                        )
                )
                .then(Commands.literal("combo")
                        .then(Commands.argument("combo_id", StringArgumentType.string())
                                .suggests(COMBO_SUGGESTIONS)
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 10))
                                        .executes(context -> addComboSpell(
                                                context,
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "combo_id"),
                                                IntegerArgumentType.getInteger(context, "slot") - 1
                                        ))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> addComboSpell(
                                                        context,
                                                        EntityArgument.getPlayer(context, "target"),
                                                        StringArgumentType.getString(context, "combo_id"),
                                                        IntegerArgumentType.getInteger(context, "slot") - 1
                                                ))
                                        )
                                )
                        )
                )
                ;
    }

    private static int openGui(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenSpellBuilderScreenPacket());
        return 1;
    }

    private static int unlockAll(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        PlayerSpellData spellData = player.getData(ModAttachments.PLAYER_SPELL_DATA.get());
        spellData.unlockAll();

        PacketDistributor.sendToPlayer(player, SyncPlayerSpellDataPacket.fromPlayerData(spellData));

        context.getSource().sendSuccess(
                () -> Component.literal("Unlocked all spell components for " + player.getName().getString()),
                true
        );
        return 1;
    }

    private static int unlockDelivery(CommandContext<CommandSourceStack> context, ServerPlayer player, String deliveryId) {
        DeliveryMethod method = DeliveryMethod.fromId(deliveryId);
        if (method == null) {
            context.getSource().sendFailure(Component.literal("Unknown delivery method: " + deliveryId));
            return 0;
        }

        PlayerSpellData spellData = player.getData(ModAttachments.PLAYER_SPELL_DATA.get());
        spellData.unlockDelivery(method);

        PacketDistributor.sendToPlayer(player, SyncPlayerSpellDataPacket.fromPlayerData(spellData));

        context.getSource().sendSuccess(
                () -> Component.literal("Unlocked delivery method '" + deliveryId + "' for " + player.getName().getString()),
                true
        );
        return 1;
    }

    private static int unlockEffect(CommandContext<CommandSourceStack> context, ServerPlayer player, String effectId) {
        SpellEffect effect = SpellEffect.fromId(effectId);
        if (effect == null) {
            context.getSource().sendFailure(Component.literal("Unknown effect: " + effectId));
            return 0;
        }

        PlayerSpellData spellData = player.getData(ModAttachments.PLAYER_SPELL_DATA.get());
        spellData.unlockEffect(effect);

        PacketDistributor.sendToPlayer(player, SyncPlayerSpellDataPacket.fromPlayerData(spellData));

        context.getSource().sendSuccess(
                () -> Component.literal("Unlocked effect '" + effectId + "' for " + player.getName().getString()),
                true
        );
        return 1;
    }

    private static int unlockModifier(CommandContext<CommandSourceStack> context, ServerPlayer player, String modifierId) {
        SpellModifier modifier = SpellModifier.fromId(modifierId);
        if (modifier == null) {
            context.getSource().sendFailure(Component.literal("Unknown modifier: " + modifierId));
            return 0;
        }

        PlayerSpellData spellData = player.getData(ModAttachments.PLAYER_SPELL_DATA.get());
        spellData.unlockModifier(modifier);

        PacketDistributor.sendToPlayer(player, SyncPlayerSpellDataPacket.fromPlayerData(spellData));

        context.getSource().sendSuccess(
                () -> Component.literal("Unlocked modifier '" + modifierId + "' for " + player.getName().getString()),
                true
        );
        return 1;
    }

    private static int listUnlocked(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        PlayerSpellData spellData = player.getData(ModAttachments.PLAYER_SPELL_DATA.get());

        StringBuilder message = new StringBuilder(player.getName().getString() + "'s Unlocked Spell Components:\n");

        message.append("Delivery Methods: ");
        if (spellData.getUnlockedDeliveryMethods().isEmpty()) {
            message.append("None");
        } else {
            message.append(spellData.getUnlockedDeliveryMethods().stream()
                    .map(DeliveryMethod::getSerializedName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("None"));
        }
        message.append("\n");

        message.append("Effects: ");
        if (spellData.getUnlockedEffects().isEmpty()) {
            message.append("None");
        } else {
            message.append(spellData.getUnlockedEffects().stream()
                    .map(SpellEffect::getSerializedName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("None"));
        }
        message.append("\n");

        message.append("Modifiers: ");
        if (spellData.getUnlockedModifiers().isEmpty()) {
            message.append("None");
        } else {
            message.append(spellData.getUnlockedModifiers().stream()
                    .map(SpellModifier::getSerializedName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("None"));
        }

        String finalMessage = message.toString();
        context.getSource().sendSuccess(
                () -> Component.literal(finalMessage),
                false
        );
        return 1;
    }

    private static int exportSpell(CommandContext<CommandSourceStack> context, ServerPlayer player, int slotIndex) {
        PlayerSpellSlots spellSlots = player.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
        SpellSlot slot = spellSlots.getSlot(slotIndex);

        if (slot == null || slot.getSpell() == null || slot.getSpell().getDelivery() == null) {
            context.getSource().sendFailure(Component.literal("Slot " + (slotIndex + 1) + " is empty or invalid"));
            return 0;
        }

        String encoded = SpellExporter.encode(slot.getSpell());
        String plaintext = SpellExporter.getPlaintext(encoded);

        context.getSource().sendSuccess(
                () -> Component.literal("Spell in slot " + (slotIndex + 1) + ":\n")
                        .append(Component.literal("Code: " + encoded + "\n"))
                        .append(Component.literal("Format: " + plaintext)),
                false
        );
        return 1;
    }

    private static int importSpell(CommandContext<CommandSourceStack> context, ServerPlayer player, int slotIndex, String code) {
        Spell spell = SpellExporter.decode(code.trim());

        if (spell == null) {
            context.getSource().sendFailure(Component.literal("Invalid spell code"));
            return 0;
        }

        PlayerSpellSlots spellSlots = player.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
        SpellSlot slot = spellSlots.getSlot(slotIndex);

        if (slot == null) {
            context.getSource().sendFailure(Component.literal("Invalid slot index"));
            return 0;
        }

        slot.setSpell(spell);

        SyncPlayerSpellSlotsPacket syncPacket = SyncPlayerSpellSlotsPacket.fromPlayerSlots(spellSlots);
        PacketDistributor.sendToPlayer(player, syncPacket);

        context.getSource().sendSuccess(
                () -> Component.literal("Imported spell to slot " + (slotIndex + 1)),
                false
        );
        return 1;
    }

    private static int copySpell(CommandContext<CommandSourceStack> context, ServerPlayer player, int slotIndex) {
        PlayerSpellSlots spellSlots = player.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
        SpellSlot slot = spellSlots.getSlot(slotIndex);

        if (slot == null || slot.getSpell() == null || slot.getSpell().getDelivery() == null) {
            context.getSource().sendFailure(Component.literal("Slot " + (slotIndex + 1) + " is empty or invalid"));
            return 0;
        }

        String encoded = SpellExporter.encode(slot.getSpell());

        Component message = Component.literal("Spell code (click to copy): ")
                .append(Component.literal("[" + encoded + "]")
                        .setStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, encoded))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")))));

        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }

    private static int giveTestKit(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        var registryAccess = player.level().registryAccess();
        var enchantmentRegistry = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);

        ResourceLocation manaPoolId = ResourceLocation.fromNamespaceAndPath("buildaspell", "mana_pool");
        ResourceLocation manaRegenId = ResourceLocation.fromNamespaceAndPath("buildaspell", "mana_regeneration");
        ResourceLocation spellPowerId = ResourceLocation.fromNamespaceAndPath("buildaspell", "spell_power");

        Holder<Enchantment> manaPool = enchantmentRegistry.get(ResourceKey.create(Registries.ENCHANTMENT, manaPoolId)).orElse(null);
        Holder<Enchantment> manaRegen = enchantmentRegistry.get(ResourceKey.create(Registries.ENCHANTMENT, manaRegenId)).orElse(null);
        Holder<Enchantment> spellPower = enchantmentRegistry.get(ResourceKey.create(Registries.ENCHANTMENT, spellPowerId)).orElse(null);

        if (manaPool == null || manaRegen == null || spellPower == null) {
            context.getSource().sendFailure(Component.literal("Failed to load enchantments"));
            return 0;
        }

        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        ItemStack leggings = new ItemStack(Items.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        helmet.enchant(manaPool, 10);
        helmet.enchant(manaRegen, 10);
        chestplate.enchant(manaPool, 10);
        chestplate.enchant(manaRegen, 10);
        leggings.enchant(manaPool, 10);
        leggings.enchant(manaRegen, 10);
        boots.enchant(manaPool, 10);
        boots.enchant(manaRegen, 10);
        sword.enchant(spellPower, 10);

        player.getInventory().add(helmet);
        player.getInventory().add(chestplate);
        player.getInventory().add(leggings);
        player.getInventory().add(boots);
        player.getInventory().add(sword);

        context.getSource().sendSuccess(
                () -> Component.literal("Gave test kit to " + player.getName().getString() + " (40 Mana Pool, 40 Mana Regen, 10 Spell Power)"),
                true
        );
        return 1;
    }

    private static int addComboSpell(CommandContext<CommandSourceStack> context, ServerPlayer player, String comboId, int slotIndex) {
        SpellCombo combo = null;
        for (SpellCombo c : SpellCombo.values()) {
            if (c.getId().equals(comboId)) {
                combo = c;
                break;
            }
        }

        if (combo == null) {
            context.getSource().sendFailure(Component.literal("Unknown combo: " + comboId));
            return 0;
        }

        final SpellCombo finalCombo = combo;
        Spell spell = createSpellForCombo(finalCombo);

        PlayerSpellSlots spellSlots = player.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
        SpellSlot slot = spellSlots.getSlot(slotIndex);

        if (slot == null) {
            context.getSource().sendFailure(Component.literal("Invalid slot index"));
            return 0;
        }

        slot.setSpell(spell);

        SyncPlayerSpellSlotsPacket syncPacket = SyncPlayerSpellSlotsPacket.fromPlayerSlots(spellSlots);
        PacketDistributor.sendToPlayer(player, syncPacket);

        context.getSource().sendSuccess(
                () -> Component.literal("Added combo spell '" + finalCombo.getId() + "' to slot " + (slotIndex + 1) + " for " + player.getName().getString()),
                true
        );
        return 1;
    }

    private static Spell createSpellForCombo(SpellCombo combo) {
        Spell spell = new Spell();
        spell.setDelivery(DeliveryMethod.CAST);

        List<SpellComponent> components = new ArrayList<>();

        switch (combo) {
            case BLACK_HOLE -> {
                components.add(new SpellComponent.Effect(SpellEffect.PULL));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
                components.add(new SpellComponent.Effect(SpellEffect.TELEPORT));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
            }
            case TORNADO -> {
                components.add(new SpellComponent.Effect(SpellEffect.PULL));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
                components.add(new SpellComponent.Effect(SpellEffect.LAUNCH));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
            }
            case CREATIVE_FLIGHT -> {
                components.add(new SpellComponent.Effect(SpellEffect.LAUNCH));
                components.add(new SpellComponent.Effect(SpellEffect.LEVITATION));
                components.add(new SpellComponent.Effect(SpellEffect.SLOW_FALL));
            }
            case IRON_GOLEM -> {
                components.add(new SpellComponent.Effect(SpellEffect.SUMMON));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_POWER));
                components.add(new SpellComponent.Effect(SpellEffect.HEAL));
            }
            case VEXES -> {
                components.add(new SpellComponent.Effect(SpellEffect.SUMMON));
                components.add(new SpellComponent.Effect(SpellEffect.TELEPORT));
                components.add(new SpellComponent.Effect(SpellEffect.DAMAGE));
            }
            case SKELETONS -> {
                components.add(new SpellComponent.Effect(SpellEffect.SUMMON));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_POWER));
                components.add(new SpellComponent.Effect(SpellEffect.LIGHTNING));
            }
            case VINDICATORS -> {
                components.add(new SpellComponent.Effect(SpellEffect.SUMMON));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_POWER));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
                components.add(new SpellComponent.Effect(SpellEffect.DAMAGE));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_POWER));
            }
            case VOID_RIFT -> {
                components.add(new SpellComponent.Effect(SpellEffect.TELEPORT));
                components.add(new SpellComponent.Modifier(SpellModifier.DURATION));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
            }
            case FORTRESS -> {
                components.add(new SpellComponent.Effect(SpellEffect.CONJURE));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
                components.add(new SpellComponent.Modifier(SpellModifier.DURATION));
            }
            case FLOOD -> {
                components.add(new SpellComponent.Effect(SpellEffect.CREATE_WATER));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
                components.add(new SpellComponent.Modifier(SpellModifier.CHAIN));
            }
            case FLOOD_LAVA -> {
                components.add(new SpellComponent.Effect(SpellEffect.CREATE_WATER));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
                components.add(new SpellComponent.Modifier(SpellModifier.CHAIN));
                components.add(new SpellComponent.Modifier(SpellModifier.CHAIN));
                components.add(new SpellComponent.Effect(SpellEffect.IGNITE));
            }
            case EMERGENCY_ESCAPE -> {
                components.add(new SpellComponent.Effect(SpellEffect.BLINK));
                components.add(new SpellComponent.Effect(SpellEffect.RECALL));
                components.add(new SpellComponent.Effect(SpellEffect.TELEPORT));
            }
            case METEOR_STRIKE -> {
                components.add(new SpellComponent.Effect(SpellEffect.LAUNCH));
                components.add(new SpellComponent.Effect(SpellEffect.EXPLOSION));
                components.add(new SpellComponent.Effect(SpellEffect.IGNITE));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_POWER));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_POWER));
            }
            case BLIZZARD -> {
                components.add(new SpellComponent.Effect(SpellEffect.FREEZE));
                components.add(new SpellComponent.Effect(SpellEffect.PULL));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
                components.add(new SpellComponent.Modifier(SpellModifier.DURATION));
            }
            case LIGHTNING_STORM -> {
                components.add(new SpellComponent.Effect(SpellEffect.LIGHTNING));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
                components.add(new SpellComponent.Modifier(SpellModifier.DURATION));
            }
            case EARTHQUAKE -> {
                components.add(new SpellComponent.Effect(SpellEffect.SLAM));
                components.add(new SpellComponent.Effect(SpellEffect.EXPLOSION));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
            }
            case SANCTUARY -> {
                components.add(new SpellComponent.Effect(SpellEffect.HEAL));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
                components.add(new SpellComponent.Modifier(SpellModifier.DURATION));
            }
            case FIRESTORM -> {
                components.add(new SpellComponent.Effect(SpellEffect.IGNITE));
                components.add(new SpellComponent.Effect(SpellEffect.EXPLOSION));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
            }
            case GEYSER -> {
                components.add(new SpellComponent.Effect(SpellEffect.CREATE_WATER));
                components.add(new SpellComponent.Effect(SpellEffect.LAUNCH));
                components.add(new SpellComponent.Modifier(SpellModifier.INCREASED_AREA));
            }
        }

        spell.setComponents(components);
        return spell;
    }
}
