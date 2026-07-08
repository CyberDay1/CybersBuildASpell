package buildaspell.events;

import buildaspell.BuildASpell;
import buildaspell.config.ModConfig;
import buildaspell.enchanting.EnchantmentCostManager;
import buildaspell.item.BlankRuneItem;
import buildaspell.mana.PlayerManaData;
import buildaspell.spell.DeliveryMethod;
import buildaspell.spell.SpellEffect;
import buildaspell.network.SyncComponentRegistryPacket;
import buildaspell.network.SyncPlayerManaPacket;
import buildaspell.network.SyncPlayerSpellDataPacket;
import buildaspell.network.SyncPlayerSpellSlotsPacket;
import buildaspell.portal.PortalManager;
import buildaspell.spell.MarkManager;
import buildaspell.spell.SpellLootingTracker;
import buildaspell.registry.ModAttachments;
import buildaspell.spell.PlayerSpellData;
import buildaspell.spell.PlayerSpellSlots;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BuildASpell.MOD_ID)
public class ServerEvents {

    // Registered manually on mod bus in BuildASpell constructor
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(
                Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, "enchantment_costs"),
                new EnchantmentCostManager()
        );
        event.addListener(
                Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, "spell_effects"),
                new buildaspell.spell.data.EffectRegistry()
        );
        event.addListener(
                Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, "spell_modifiers"),
                new buildaspell.spell.data.ModifierRegistry()
        );
        event.addListener(
                Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, "spell_deliveries"),
                new buildaspell.spell.data.DeliveryRegistry()
        );
        event.addListener(
                Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, "spell_combos"),
                new buildaspell.spell.data.ComboRegistry()
        );
    }

    /**
     * Pushes datapack-authored components to clients. Fires on join (single player) and on
     * {@code /reload} (every player), so the spell builder palette always reflects the loaded
     * datapacks without the client ever parsing datapack files itself.
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncComponentRegistryPacket packet = SyncComponentRegistryPacket.build();
        if (event.getPlayer() != null) {
            PacketDistributor.sendToPlayer(event.getPlayer(), packet);
        } else {
            PacketDistributor.sendToAllPlayers(packet);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SpellLootingTracker.tick();
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        PortalManager.setServer(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PortalManager.setServer(null);
        MarkManager.clearAll();
        SpellLootingTracker.clearAll();
        buildaspell.spell.ImbueManager.clearAll();
    }

    /**
     * TOUCH (Imbue) discharge on melee: if the attacker is holding a pending imbued spell, cast it at
     * the struck entity's position and consume the charge. The vanilla hit still lands normally.
     */
    @SubscribeEvent
    public static void onAttackEntity(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        dischargeImbue(player, event.getTarget().position());
    }

    /**
     * TOUCH (Imbue) discharge on interaction: right-clicking an entity or block while holding a
     * pending imbued spell casts it at the interaction point and consumes the charge.
     */
    @SubscribeEvent
    public static void onInteractEntity(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        dischargeImbue(player, event.getTarget().position());
    }

    @SubscribeEvent
    public static void onInteractBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        dischargeImbue(player, net.minecraft.world.phys.Vec3.atCenterOf(event.getPos()));
    }

    private static void dischargeImbue(Player player, net.minecraft.world.phys.Vec3 location) {
        if (!buildaspell.spell.ImbueManager.hasImbue(player.getUUID())) {
            return;
        }
        buildaspell.spell.Spell spell = buildaspell.spell.ImbueManager.consumeIfValid(
                player.getUUID(), player.level().getGameTime());
        if (spell != null) {
            buildaspell.spell.execution.SpellExecutor.executeSpellAtLocationWithDelay(player, spell, location);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerSpellData spellData = serverPlayer.getData(ModAttachments.PLAYER_SPELL_DATA.get());
            if (!spellData.isStarterKitGranted()) {
                grantStarterKit(spellData);
                spellData.markStarterKitGranted();
            }
            if (ModConfig.giveStarterGuidebook() && !spellData.isGuidebookGranted()) {
                if (grantGuidebook(serverPlayer)) {
                    spellData.markGuidebookGranted();
                }
            }
            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerSpellDataPacket.fromPlayerData(spellData));
            PlayerSpellSlots spellSlots = serverPlayer.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerSpellSlotsPacket.fromPlayerSlots(spellSlots));
            PlayerManaData manaData = serverPlayer.getData(ModAttachments.PLAYER_MANA.get());
            PacketDistributor.sendToPlayer(serverPlayer, new SyncPlayerManaPacket(manaData.getCurrentMana()));
        }
    }

    /**
     * Despawns expired spell summons (Skeletons/Vindicators; Vexes use vanilla limited life).
     * Runs on entity tick so an expiry survives chunk unloads and world restarts.
     */
    @SubscribeEvent
    public static void onEntityTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)) {
            return;
        }
        if (buildaspell.spell.MobSpellState.isExpiredSummon(mob)) {
            if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                        mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                        10, 0.3, 0.3, 0.3, 0.02);
            }
            mob.discard();
        }
    }

    /**
     * Target gating for spell-touched mobs: summons never turn on the player who summoned them, and
     * Charmed (pacified) mobs can't acquire any target while the pacify window lasts.
     */
    @SubscribeEvent
    public static void onLivingChangeTarget(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob mob) || mob.level().isClientSide()) {
            return;
        }
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget == null) {
            return;
        }
        if (buildaspell.spell.MobSpellState.isPacified(mob)
                || buildaspell.spell.MobSpellState.isSummonerOf(mob, newTarget)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getSource().getEntity() instanceof Player player) {
            LivingEntity killedEntity = event.getEntity();
            BlankRuneItem.depositEssence(player, BlankRuneItem.getEssenceValue(killedEntity));
        }
    }

    /**
     * Gives the player the Arcane Codex (Modonomicon book). Built by registry-id lookup so the code
     * carries no compile dependency on Modonomicon internals and degrades gracefully if Modonomicon is
     * absent. Returns true if the book was actually given (so the caller only flips the one-time flag
     * when the grant succeeded).
     */
    private static boolean grantGuidebook(ServerPlayer player) {
        var bookItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("modonomicon", "modonomicon"));
        if (bookItem == net.minecraft.world.item.Items.AIR) {
            BuildASpell.LOGGER.warn("[guidebook] modonomicon:modonomicon item not found - Modonomicon not installed?");
            return false; // Modonomicon not installed
        }
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(bookItem);
        var bookIdType = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("modonomicon", "book_id"));
        if (bookIdType != null) {
            @SuppressWarnings("unchecked")
            var typed = (net.minecraft.core.component.DataComponentType<net.minecraft.resources.Identifier>) bookIdType;
            // Book id is namespace "spell_guidebook", path = mod id (see SpellGuidebook datagen:
            // SingleBookSubProvider(bookId, namespace, ...) -> spell_guidebook:buildaspell).
            stack.set(typed, net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    "spell_guidebook", BuildASpell.MOD_ID));
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        return true;
    }

    private static void grantStarterKit(PlayerSpellData data) {
        for (String id : ModConfig.getStarterDeliveries()) {
            DeliveryMethod method = DeliveryMethod.fromId(id);
            if (method != null) {
                data.unlockDelivery(method);
            }
        }
        for (String id : ModConfig.getStarterEffects()) {
            SpellEffect effect = SpellEffect.fromId(id);
            if (effect != null) {
                data.unlockEffect(effect);
            }
        }
    }
}
