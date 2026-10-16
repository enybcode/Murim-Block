package com.murimblock;

import com.murimblock.combat.CombatService;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.integration.epicfight.EpicFightContentPolicy;
import com.murimblock.mob.MurimEntities;
import com.murimblock.qi.QiRewardManager;
import com.murimblock.qi.QiService;
import com.murimblock.cultivation.MeditationService;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightItems;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.ZombiePatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

import static com.murimblock.testing.GameTestPlayers.player;
import static com.murimblock.testing.GameTestPlayers.cleanup;

/** Runs against a disposable server world and is excluded from the shipped mod. */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class FoundationGameTests {
    @SubscribeEvent
    public static void template(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getServer() instanceof GameTestServer)) return;
        CompoundTag nbt = new CompoundTag();
        ListTag size = new ListTag();
        for (int i = 0; i < 3; i++) size.add(IntTag.valueOf(5));
        nbt.put("size", size);
        ListTag palette = new ListTag();
        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        palette.add(air);
        nbt.put("palette", palette);
        nbt.put("blocks", new ListTag());
        nbt.put("entities", new ListTag());
        level.getStructureManager().getOrCreate(ResourceLocation.parse("murimblock:foundation_arena"))
                .load(level.registryAccess().lookupOrThrow(Registries.BLOCK), nbt);
        // The Blood Iceberg's arena is 45 blocks across: its own empty template, so no other test stands next to it.
        CompoundTag wide = nbt.copy();
        ListTag wideSize = new ListTag();
        for (int v : new int[]{48, 32, 48}) wideSize.add(IntTag.valueOf(v));
        wide.put("size", wideSize);
        level.getStructureManager().getOrCreate(ResourceLocation.parse("murimblock:iceberg_arena"))
                .load(level.registryAccess().lookupOrThrow(Registries.BLOCK), wide);
        // The spear's targets stand up to 4 blocks from the player: 9 across, so they stay inside the test. A GameTest
        // force-loads only its template's chunks, and a mob moved past it, into a chunk not loaded, is out of reach.
        CompoundTag spear = nbt.copy();
        ListTag spearSize = new ListTag();
        for (int v : new int[]{9, 5, 9}) spearSize.add(IntTag.valueOf(v));
        spear.put("size", spearSize);
        level.getStructureManager().getOrCreate(ResourceLocation.parse("murimblock:spear_arena"))
                .load(level.registryAccess().lookupOrThrow(Registries.BLOCK), spear);
    }

    @GameTest(template = "foundation_arena", batch = "foundation", timeoutTicks = 40)
    public static void swordAttackAppliesOneImmediateHitAndNoDelayedHit(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        AtomicInteger contacts = new AtomicInteger();
        Consumer<LivingDamageEvent.Post> observer = event -> {
            if (event.getEntity() == target && event.getSource().getEntity() == player) contacts.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(observer);
        try {
            float before = target.getHealth();
            player.attack(target);
            float after = target.getHealth();
            helper.assertTrue(after < before, "The default sword hit was deferred or canceled");
            helper.assertTrue(contacts.get() == 1, "One attack generated more than one damage contact");
            helper.runAfterDelay(5, () -> {
                try {
                    helper.assertTrue(target.getHealth() == after && contacts.get() == 1,
                            "Delayed damage: health=" + target.getHealth() + ", expected=" + after + ", contacts=" + contacts.get());
                    helper.assertTrue(QiService.getQi(player) == 100, "Sword attacks spent Qi");
                    helper.succeed();
                } finally {
                    NeoForge.EVENT_BUS.unregister(observer);
                    cleanup(player);
                }
            });
        } catch (RuntimeException exception) {
            NeoForge.EVENT_BUS.unregister(observer);
            cleanup(player);
            throw exception;
        }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void incomingMeleeDamageDoesNotSpendQiOrGrantSwordProtection(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
            attacker.setNoAi(true);
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(attacker), 6);
            helper.assertTrue(player.getHealth() == health - 6, "The mode flag intercepted default damage");
            helper.assertTrue(QiService.getQi(player) == 100, "Incoming damage spent Qi");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void itemUseKeepsDefaultSwordAndOffhandShieldBehavior(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            var result = player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
            helper.assertTrue(result == InteractionResult.PASS && !player.isUsingItem(), "Sword use was intercepted");
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
            player.gameMode.useItem(player, helper.getLevel(), player.getOffhandItem(), InteractionHand.OFF_HAND);
            helper.assertTrue(player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND,
                    "The offhand shield could not be used");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void modeToggleDoesNotChangeMovementQiOrCultivation(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            var cultivation = CultivationService.getCultivation(player);
            CombatService.setCombatMode(player, false);
            CombatService.setCombatMode(player, true);
            helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed, "Mode toggle slowed the player");
            helper.assertTrue(QiService.getQi(player) == 100 && QiService.getQiMax(player) == 100, "Mode toggle changed Qi");
            helper.assertTrue(CultivationService.getCultivation(player).equals(cultivation), "Mode toggle changed cultivation");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void meditationLocksAndRestoresMovement(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double jump = player.getAttributeValue(Attributes.JUMP_STRENGTH);
            player.setOnGround(true);
            helper.assertTrue(MeditationService.start(player), "Meditation could not start with a sword equipped");
            helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0
                    && player.getAttributeValue(Attributes.JUMP_STRENGTH) == 0, "Meditation did not lock movement");
            MeditationService.stop(player, false);
            helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed
                    && player.getAttributeValue(Attributes.JUMP_STRENGTH) == jump, "Meditation left movement locked");
            helper.assertTrue(QiService.getQi(player) == 100, "Sitting down and getting up spent Qi");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void chargeAndHudMirrorResetWithoutOverwritingEpicFightMode(GameTestHelper helper) {
        ServerPlayer original = player(helper);
        ServerPlayer clone = player(helper);
        try {
            clone.setOnGround(true);
            MeditationService.start(clone);
            NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(clone, original, true));
            helper.assertFalse(CombatService.getData(clone).combatMode(), "Respawn inherited a stale HUD mirror");
            CombatService.refreshMode(clone);
            helper.assertTrue(CombatService.getData(clone).combatMode() == EpicFightBridge.isCombatMode(clone), "HUD mirror disagreed with Epic Fight");
            helper.assertFalse(MeditationService.isMeditating(clone), "Respawn inherited the meditation");
            original.setOnGround(true);
            MeditationService.start(original);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(original));
            helper.assertFalse(CombatService.getData(original).combatMode(), "Logout kept the HUD mirror");
            helper.assertFalse(MeditationService.isMeditating(original), "Logout kept the meditation");
            helper.succeed();
        } finally {
            cleanup(original);
            cleanup(clone);
        }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void playerDataRoundTripsQiCultivationAndEpicFightMode(GameTestHelper helper) {
        ServerPlayer original = player(helper);
        ServerPlayer restored = player(helper);
        try {
            QiService.setQiMax(original, 150);
            QiService.setQi(original, 73);
            CultivationService.setCultivation(original, CultivationRealm.QI_CONDENSATION, CultivationStage.MIDDLE);
            var cultivation = CultivationService.getCultivation(original);
            CompoundTag saved = new CompoundTag();
            original.saveWithoutId(saved);
            helper.assertFalse(saved.toString().contains("murimblock:player_combat"), "The temporary mode was persisted");
            CombatService.setCombatMode(restored, false);
            // Keep the connected mock player's identity separate from the saved player.
            saved.remove("UUID");
            restored.load(saved);
            helper.assertTrue(QiService.getQi(restored) == 73 && QiService.getQiMax(restored) == 150,
                    "Qi values did not survive save/load");
            helper.assertTrue(CultivationService.getCultivation(restored).equals(cultivation), "Cultivation did not survive save/load");
            helper.assertTrue(CombatService.isInCombatMode(restored) == CombatService.isInCombatMode(original),
                    "Epic Fight mode did not survive its own save/load");
            helper.succeed();
        } finally {
            cleanup(original);
            cleanup(restored);
        }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void basicsNeedNoBooksAndNativeProgressionCannotCast(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            var patch = (ServerPlayerPatch) EpicFightBridge.patch(player);
            var passive = patch.getSkill(SkillSlots.PASSIVE1);
            passive.setSkill(EpicFightSkills.FORBIDDEN_STRENGTH.get());
            helper.assertFalse(EpicFightCombatDefaults.allowed(passive.getSkill()), "Native passive allowed");
            var innate = patch.getSkill(SkillSlots.WEAPON_INNATE);
            innate.setSkill(EpicFightSkills.SWEEPING_EDGE.get());
            player.setOnGround(true);
            helper.assertFalse(innate.requestCasting(patch, new CompoundTag()), "Native special attack accepted");
            EpicFightCombatDefaults.enforce(player);
            EpicFightCombatDefaults.enforce(player);
            helper.assertTrue(passive.isEmpty() && innate.isEmpty(), "Native progression was not cleared");
            helper.assertTrue(patch.getSkill(SkillSlots.GUARD).getSkill() == MurimSkills.MURIM_GUARD.get(), "Default (Murim) guard missing");
            helper.assertTrue(patch.getSkill(SkillSlots.DODGE).getSkill() == EpicFightSkills.ROLL.get(), "Default dodge missing");
            helper.assertTrue(QiService.getQi(player) == 100, "Default actions changed Qi");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void legacyItemsAreRemovedWithoutChangingVanillaInventory(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            var chest = player.getEnderChestInventory();
            chest.setItem(0, new ItemStack(EpicFightItems.SKILLBOOK.get(), 2));
            chest.setItem(1, new ItemStack(Items.DIAMOND, 7));
            player.getInventory().setItem(5, new ItemStack(EpicFightItems.SKILLBOOK.get(), 3));
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
            helper.assertTrue(chest.getItem(0).isEmpty() && player.getInventory().getItem(5).isEmpty(), "Legacy books remained");
            helper.assertTrue(chest.getItem(1).is(Items.DIAMOND) && chest.getItem(1).getCount() == 7, "Vanilla contents changed");
            helper.assertTrue(player.getMainHandItem().is(Items.IRON_SWORD) && QiService.getQi(player) == 100, "Cleanup changed unrelated state");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void dawnDayContentIsRetiredButLoaded(GameTestHelper helper) {
        // Dawn Day is loaded for its animations; its weapons are kept (the Weapons tab), every other item and potion retired.
        helper.assertTrue(net.neoforged.fml.ModList.get().isLoaded(EpicFightContentPolicy.DAWN_DAY), "Dawn Day not loaded");
        int items = 0, weapons = 0, potions = 0;
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(EpicFightContentPolicy.DAWN_DAY)) continue;
            items++;
            boolean weapon = com.murimblock.item.WeaponsTab.isWeapon(item);
            if (weapon) weapons++;
            helper.assertTrue(EpicFightContentPolicy.blocked(new ItemStack(item)) != weapon, (weapon ? "Dawn Day weapon retired: " : "Dawn Day item kept: ") + item);
        }
        helper.assertTrue(weapons > 0 && weapons < items, "Dawn Day weapons " + weapons + " of " + items);
        for (var entry : net.minecraft.core.registries.BuiltInRegistries.POTION.holders().toList()) {
            if (!entry.key().location().getNamespace().equals(EpicFightContentPolicy.DAWN_DAY)) continue;
            potions++;
            helper.assertTrue(EpicFightContentPolicy.blocked(net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, entry)),
                    "Dawn Day potion kept: " + entry.key().location());
        }
        helper.assertTrue(items > 0, "Dawn Day registered no item");
        helper.assertFalse(EpicFightContentPolicy.blocked(net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,
                net.minecraft.world.item.alchemy.Potions.HEALING)), "A vanilla potion was retired");
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void nativeRecipesAreFilteredAndVanillaRecipesRemain(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (var recipe : manager.getRecipes()) {
            // Only the recipes of their weapons remain.
            ItemStack made = recipe.value().getResultItem(helper.getLevel().registryAccess());
            helper.assertFalse(EpicFightContentPolicy.isNative(recipe.id()) && !com.murimblock.item.WeaponsTab.isWeapon(made.getItem()), "Native recipe remained: " + recipe.id());
            helper.assertFalse(EpicFightContentPolicy.blocked(made), "Native recipe output remained");
        }
        helper.assertTrue(manager.byKey(ResourceLocation.parse("minecraft:iron_sword")).isPresent(), "Vanilla sword recipe missing");
        helper.succeed();
    }

    /** The Weapons tab: Murimblock's weapons first, then Epic Fight's, Dawn Day's and Minecraft's; no skill book, gem or pickaxe. */
    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void theWeaponsTabHoldsEveryWeapon(GameTestHelper helper) {
        var tab = com.murimblock.item.MurimItems.WEAPONS_TAB.get();
        tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess()));
        var shown = tab.getDisplayItems().stream().map(s -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString()).toList();
        for (String id : new String[] { "murimblock:alliance_spear", "murimblock:snow_plum_jian", "murimblock:iron_fan", "murimblock:rattan_shield", "murimblock:bandit_dao",
                "epicfight:iron_spear", "epicfight:iron_greatsword", "epicfight:uchigatana", "epicfight_dd:iron_halberd", "minecraft:diamond_sword", "minecraft:bow", "minecraft:trident", "minecraft:shield" })
            helper.assertTrue(shown.contains(id), "Not in the Weapons tab: " + id);
        for (String id : new String[] { "epicfight:skillbook", "epicfight_dd:amber", "minecraft:iron_pickaxe", "murimblock:silver_tael" })
            helper.assertFalse(shown.contains(id), "In the Weapons tab: " + id);
        helper.assertTrue(shown.get(0).startsWith("murimblock:") && shown.indexOf("epicfight:iron_spear") < shown.indexOf("minecraft:diamond_sword"), "The tab's order is off: " + shown.subList(0, 5));
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void lootModifierRemovesOnlyNativeItems(GameTestHelper helper) {
        var loot = new ObjectArrayList<ItemStack>();
        loot.add(new ItemStack(EpicFightItems.SKILLBOOK.get(), 4));
        loot.add(new ItemStack(Items.DIAMOND, 3));
        var params = new LootParams.Builder(helper.getLevel())
                .create(LootContextParamSets.EMPTY);
        var context = new LootContext.Builder(params).create(Optional.empty());
        loot = CommonHooks.modifyLoot(ResourceLocation.parse("murimblock:test_loot"), loot, context);
        helper.assertTrue(loot.size() == 1 && loot.getFirst().is(Items.DIAMOND) && loot.getFirst().getCount() == 3, "Loot filter changed vanilla drops");
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void openedContainersAreCleanedAndBookUseIsRejected(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            var chest = new SimpleContainer(27);
            chest.setItem(0, new ItemStack(EpicFightItems.SKILLBOOK.get()));
            chest.setItem(1, new ItemStack(Items.GOLD_INGOT, 6));
            var menu = ChestMenu.threeRows(0, player.getInventory(), chest);
            menu.setCarried(new ItemStack(EpicFightItems.SKILLBOOK.get()));
            NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, menu));
            helper.assertTrue(chest.getItem(0).isEmpty() && menu.getCarried().isEmpty(), "Opened legacy contents remained");
            helper.assertTrue(chest.getItem(1).getCount() == 6 && chest.getItem(1).is(Items.GOLD_INGOT), "Vanilla chest content changed");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EpicFightItems.SKILLBOOK.get()));
            var use = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
            NeoForge.EVENT_BUS.post(use);
            helper.assertTrue(use.isCanceled() && use.getCancellationResult() == InteractionResult.FAIL, "Native book use not rejected");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void documentedBehaviorFragmentUsesPinnedEngineSchema(GameTestHelper helper) throws Exception {
        String json = Files.readString(Path.of("../../docs/examples/mobs/humanoid_behavior.fragment.json"));
        var tag = TagParser.parseTag(json);
        var behaviors = MobPatchReloadListener
                .deserializeHumanoidCombatBehaviors(tag.getList("combat_behavior", 10));
        helper.assertTrue(!behaviors.isEmpty(), "Example behavior was not deserialized by the real engine");
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void nativeDropsAreRejectedAndStrayKeepsVanillaEquipment(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var dropped = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                new ItemStack(EpicFightItems.SKILLBOOK.get()));
        helper.assertFalse(level.addFreshEntity(dropped), "Native item entity was added");
        var stray = EntityType.STRAY.create(level);
        stray.moveTo(pos.getX(), pos.getY(), pos.getZ());
        stray.setNoAi(true);
        stray.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        helper.assertTrue(level.addFreshEntity(stray), "Stray spawn failed");
        helper.assertTrue(stray.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "Stray combat patch overwrote vanilla helmet");
        for (var slot : EquipmentSlot.values()) helper.assertFalse(EpicFightContentPolicy.blocked(stray.getItemBySlot(slot)), "Native mob equipment remained");
        helper.assertTrue(EpicFightCapabilities.getEntityPatch(stray,
                LivingEntityPatch.class) != null, "Stray engine patch missing");
        stray.discard();
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "foundation", timeoutTicks = 60)
    public static void epicFightSwordAnimationDealsOneHitWithoutSpendingQi(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        // Vanilla GameTest scatters arenas millions of blocks away; float-based engine colliders need a local fixture.
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        player.moveTo(0.5, 70, 0.5, 0, 0);
        target.moveTo(0.5, 70, 1.7, 180, 0);
        EpicFightBridge.patch(player).setModelYRot(0, false);
        player.connection.tick();
        AtomicInteger contacts = new AtomicInteger();
        Consumer<LivingDamageEvent.Post> observer = event -> {
            if (event.getEntity() == target && event.getSource().getEntity() == player) contacts.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(observer);
        float before = target.getHealth();
        try {
            EpicFightBridge.patch(player).playAnimationSynchronized(Animations.SWORD_AUTO1, 0);
            // The embedded connection is not polled by the network acceptor; the world drives Epic Fight's clock.
            for (int tick = 1; tick <= 35; tick++) helper.runAfterDelay(tick, player.connection::tick);
            helper.runAfterDelay(35, () -> {
                try {
                    helper.assertTrue(target.getHealth() < before, "Animated blade did not hit the target");
                    helper.assertTrue(contacts.get() == 1, "Animated attack damage contacts=" + contacts.get() + ", health=" + target.getHealth());
                    helper.assertTrue(QiService.getQi(player) == 100, "Animated attack spent Qi");
                    helper.succeed();
                } finally { NeoForge.EVENT_BUS.unregister(observer); target.discard(); cleanup(player); }
            });
        } catch (RuntimeException exception) {
            NeoForge.EVENT_BUS.unregister(observer);
            cleanup(player);
            throw exception;
        }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void everyVanillaSwordResolvesToAnEpicFightSword(GameTestHelper helper) {
        for (var item : List.of(Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD,
                Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD)) {
            var capability = EpicFightCapabilities.getItemStackCapability(new ItemStack(item));
            helper.assertTrue(capability.getWeaponCategory()
                    == CapabilityItem.WeaponCategories.SWORD,
                    "Invalid sword capability: " + item);
        }
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void staminaCostsCannotExhaustGuardOrDodgeAndKeepOtherResources(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            var patch = (ServerPlayerPatch) EpicFightBridge.patch(player);
            var guard = EpicFightSkills.GUARD.get();
            var roll = EpicFightSkills.ROLL.get();
            patch.getSkill(SkillSlots.GUARD).setSkill(guard);
            patch.getSkill(SkillSlots.DODGE).setSkill(roll);
            patch.getSkill(SkillSlots.PASSIVE1).setSkill(EpicFightSkills.FORBIDDEN_STRENGTH.get());
            patch.setStamina(0);
            float health = player.getHealth();
            for (int i = 0; i < 40; i++) {
                float cost = patch.getMaxStamina() + 100;
                helper.assertTrue(patch.consumeForSkill(guard, Skill.Resource.STAMINA, cost), "Guard still limited by stamina");
                helper.assertTrue(patch.consumeForSkill(roll, Skill.Resource.STAMINA, cost), "Dodge still limited by stamina");
            }
            helper.assertTrue(patch.getStamina() == patch.getMaxStamina(), "Stamina was depleted");
            helper.assertTrue(player.getHealth() == health, "Stamina costs fell back to health");
            helper.assertTrue(QiService.getQi(player) == 100, "Removing stamina spent Qi");
            helper.assertFalse(patch.consumeForSkill(guard, Skill.Resource.WEAPON_CHARGE, 1000), "Weapon charge requirement removed");
            helper.assertFalse(patch.consumeForSkill(guard, Skill.Resource.COOLDOWN, 1000), "Cooldown requirement removed");
            helper.assertTrue(patch.consumeForSkill(guard, Skill.Resource.HEALTH, 1), "Unrelated health resource changed");
            helper.assertTrue(player.getHealth() == health - 1, "Unrelated health cost was removed");
            helper.assertFalse(patch.hasStamina(Float.NaN) || patch.hasStamina(Float.POSITIVE_INFINITY), "Invalid cost accepted");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void dodgeCastsWithoutStaminaButStillRequiresGround(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            var patch = (ServerPlayerPatch) EpicFightBridge.patch(player);
            var dodge = patch.getSkill(SkillSlots.DODGE);
            dodge.setSkill(EpicFightSkills.ROLL.get());
            patch.setStamina(0);
            CompoundTag args = new CompoundTag();
            args.putInt("direction", 0);
            args.putFloat("yRot", 0);
            player.setOnGround(false);
            helper.assertFalse(dodge.requestCasting(patch, args), "Stamina removal permitted an airborne dodge");
            player.setOnGround(true);
            helper.assertTrue(dodge.requestCasting(patch, args), "Grounded dodge was rejected");
            helper.assertTrue(patch.getStamina() == patch.getMaxStamina() && QiService.getQi(player) == 100,
                    "Dodge consumed stamina or Qi");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void freeGuardStillBlocksOnlyFromTheFront(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            var patch = (ServerPlayerPatch) EpicFightBridge.patch(player);
            var guard = patch.getSkill(SkillSlots.GUARD);
            guard.setSkill(EpicFightSkills.GUARD.get());
            player.connection.tick();
            patch.setStamina(0);
            player.setYRot(0);
            helper.assertTrue(guard.requestHold(patch, new CompoundTag()), "Guard could not start");
            var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
            attacker.setNoAi(true);
            attacker.setNoGravity(true);
            attacker.moveTo(player.getX(), player.getY(), player.getZ() + 1);
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(attacker), 6);
            helper.assertTrue(player.getHealth() == health, "Frontal guard failed");
            player.invulnerableTime = 0;
            attacker.moveTo(player.getX(), player.getY(), player.getZ() - 1);
            player.hurt(player.damageSources().mobAttack(attacker), 6);
            helper.assertTrue(player.getHealth() < health, "Guard incorrectly blocked a rear hit");
            helper.assertTrue(patch.getStamina() == patch.getMaxStamina() && QiService.getQi(player) == 100,
                    "Guard consumed stamina or Qi");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "mob_integration")
    public static void trainingOpponentReceivesEngineAttributesAndOnlyAnimatedMeleeGoals(GameTestHelper helper) {
        var mob = helper.spawn(MurimEntities.TRAINING_OPPONENT.get(), new BlockPos(2, 1, 2));
        try {
            var patch = EpicFightCapabilities.getEntityPatch(mob,
                    ZombiePatch.class);
            helper.assertTrue(patch != null && patch.getArmature() != null, "Custom mob patch or armature missing");
            for (var attribute : List.of(EpicFightAttributes.WEIGHT,
                    EpicFightAttributes.IMPACT,
                    EpicFightAttributes.MAX_STRIKES,
                    EpicFightAttributes.OFFHAND_IMPACT)) {
                helper.assertTrue(mob.getAttribute(attribute) != null, "Custom mob engine attribute missing: " + attribute);
            }
            long animated = mob.goalSelector.getAvailableGoals().stream().filter(goal -> goal.getGoal()
                    instanceof AnimatedAttackGoal).count();
            helper.assertTrue(animated == 1, "Expected exactly one animated melee goal, got " + animated);
            // Epic Fight's TargetChasingGoal extends MeleeAttackGoal but overrides its damage callback with a no-op.
            helper.assertFalse(mob.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal()
                    instanceof MeleeAttackGoal
                    && !(goal.getGoal() instanceof TargetChasingGoal)),
                    "Vanilla melee goal would double attacks");
            helper.assertTrue(mob.getMainHandItem().is(Items.IRON_SWORD), "Fixture sword missing");
            mob.setBaby(true);
            helper.assertFalse(mob.isBaby(), "Fixture became a baby with an incompatible rig");
            CompoundTag saved = new CompoundTag();
            mob.saveWithoutId(saved);
            saved.remove("UUID");
            var restored = MurimEntities.TRAINING_OPPONENT.get().create(helper.getLevel());
            restored.load(saved);
            helper.assertTrue(restored.getMainHandItem().is(Items.IRON_SWORD) && !restored.isBaby(), "Mob save/load changed equipment or rig");
            helper.succeed();
        } finally { mob.discard(); }
    }

    @GameTest(template = "foundation_arena", batch = "mob_integration")
    public static void trainingOpponentCannotAwardQiLootOrExperience(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var mob = helper.spawn(MurimEntities.TRAINING_OPPONENT.get(), new BlockPos(1, 1, 1));
        mob.setNoAi(true);
        mob.setNoGravity(true);
        try {
            helper.assertTrue(QiRewardManager.rewardTarget(mob.getType()).baseReward() == 0,
                    "Prototype type received the fallback Qi reward");
            QiService.setQi(player, 50);
            helper.assertTrue(QiRewardManager.awardKillReward(player, mob).isEmpty()
                    && QiService.getQi(player) == 50, "Fixture awarded progression Qi");
            helper.assertTrue(mob.getExperienceReward(helper.getLevel(), player) == 0, "Fixture awarded XP");
            // Observe the actual death pipeline, including equipment (not just the empty JSON loot table).
            AtomicInteger deathEvents = new AtomicInteger();
            Consumer<LivingDropsEvent> observer = event -> {
                if (event.getEntity() != mob) return;
                helper.assertTrue(event.getDrops().isEmpty(), "Fixture death generated equipment or loot");
                deathEvents.incrementAndGet();
            };
            NeoForge.EVENT_BUS.addListener(observer);
            try {
                mob.hurt(player.damageSources().playerAttack(player), 1000);
                helper.assertTrue(!mob.isAlive() && deathEvents.get() == 1, "Fixture death pipeline did not run");
            } finally { NeoForge.EVENT_BUS.unregister(observer); }
            helper.succeed();
        } finally { mob.discard(); cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "mob_integration", timeoutTicks = 60)
    public static void customMobAnimatedSwordDealsOneContactWithoutSpendingPlayerQi(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var mob = helper.spawn(MurimEntities.TRAINING_OPPONENT.get(), new BlockPos(2, 1, 2));
        mob.setNoAi(true);
        mob.setNoGravity(true);
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        mob.moveTo(0.5, 70, 0.5, 0, 0);
        player.moveTo(0.5, 70, 1.7, 180, 0);
        mob.setTarget(player);
        player.connection.tick();
        var patch = EpicFightCapabilities.getEntityPatch(mob,
                ZombiePatch.class);
        mob.yBodyRot = 0;
        mob.yHeadRot = 0;
        AtomicInteger contacts = new AtomicInteger();
        Consumer<LivingDamageEvent.Post> observer = event -> {
            if (event.getEntity() == player && event.getSource().getEntity() == mob) contacts.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(observer);
        float health = player.getHealth();
        try {
            patch.playAnimationSynchronized(Animations.BIPED_MOB_ONEHAND1, 0);
            helper.runAfterDelay(8, () -> helper.assertTrue(contacts.get() == 0 && player.getHealth() == health,
                    "Animated mob dealt damage during windup, before the contact window"));
            for (int tick = 1; tick <= 35; tick++) helper.runAfterDelay(tick, player.connection::tick);
            helper.runAfterDelay(35, () -> {
                try {
                    helper.assertTrue(player.getHealth() < health && contacts.get() == 1,
                            "Custom animated mob contacts=" + contacts.get() + ", health=" + player.getHealth());
                    helper.assertTrue(QiService.getQi(player) == 100, "Mob attack changed player Qi");
                    helper.succeed();
                } finally { NeoForge.EVENT_BUS.unregister(observer); mob.discard(); cleanup(player); }
            });
        } catch (RuntimeException exception) {
            NeoForge.EVENT_BUS.unregister(observer);
            mob.discard();
            cleanup(player);
            throw exception;
        }
    }

    @GameTest(template = "foundation_arena", batch = "mob_interrupt", timeoutTicks = 60)
    public static void removingTrainingOpponentDuringWindupCannotLeaveADelayedHit(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var mob = helper.spawn(MurimEntities.TRAINING_OPPONENT.get(), new BlockPos(2, 1, 2));
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.moveTo(0.5, 70, 0.5, 0, 0);
        mob.yBodyRot = 0;
        player.moveTo(0.5, 70, 1.7, 180, 0);
        player.connection.tick();
        var patch = EpicFightCapabilities.getEntityPatch(mob,
                ZombiePatch.class);
        float health = player.getHealth();
        patch.playAnimationSynchronized(Animations.BIPED_MOB_ONEHAND1, 0);
        helper.runAfterDelay(4, mob::discard);
        for (int tick = 1; tick <= 35; tick++) helper.runAfterDelay(tick, player.connection::tick);
        helper.runAfterDelay(35, () -> {
            try {
                helper.assertTrue(player.getHealth() == health, "Removed attacker left a delayed animated hit");
                helper.succeed();
            } finally { mob.discard(); cleanup(player); }
        });
    }

    @GameTest(template = "foundation_arena", batch = "mob_ai", timeoutTicks = 100)
    public static void trainingOpponentAiSelectsAnAnimatedAttack(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var mob = helper.spawn(MurimEntities.TRAINING_OPPONENT.get(), new BlockPos(2, 1, 2));
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        mob.setNoGravity(true);
        mob.moveTo(0.5, 70, 0.5, 0, 0);
        player.moveTo(0.5, 70, 1.7, 180, 0);
        mob.setTarget(player);
        float health = player.getHealth();
        for (int tick = 1; tick <= 80; tick++) helper.runAfterDelay(tick, player.connection::tick);
        helper.runAfterDelay(80, () -> {
            try {
                helper.assertTrue(player.getHealth() < health, "Autonomous combat AI never landed an attack");
                helper.assertTrue(QiService.getQi(player) == 100, "Autonomous attacks spent player Qi");
                helper.succeed();
            } finally { mob.discard(); cleanup(player); }
        });
    }

}
