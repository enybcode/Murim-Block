package com.murimblock;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.murimblock.client.EpicFightControls;
import com.murimblock.client.MurimblockKeyMappings;
import com.murimblock.client.gui.MurimProfileScreen;
import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.ArmoredCrabPatch;
import com.murimblock.integration.epicfight.CrabManEpicFight;
import com.murimblock.integration.epicfight.CrabManPatch;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.EpicFightContentPolicy;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.integration.epicfight.BanditChiefPatch;
import com.murimblock.integration.epicfight.MountainBanditPatch;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.BanditChiefEntity;
import com.murimblock.mob.MountainBanditEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.WaterBallEntity;
import com.murimblock.qi.QiService;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.client.gui.screen.SkillEditScreen;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.ZombiePatch;

/** Opt-in real-client rendering checks. Never loads into a published jar or the normal IntelliJ run. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ClientVisualSmoke {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int ticks;
    private static int bootTicks;
    private static boolean initialized;
    private static boolean failed;
    private static final AtomicBoolean ready = new AtomicBoolean();
    private static volatile UUID opponentId;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.visualSmoke")) return;
        Minecraft mc = Minecraft.getInstance();
        if (failed) { mc.stop(); return; }
        try {
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
                if (++bootTicks == 40) {
                    LOGGER.info("Visual smoke bootstrap screen: {}", mc.screen == null ? "none" : mc.screen.getClass().getName());
                    capture("00-bootstrap.png");
                }
                if (mc.screen instanceof BackupConfirmScreen) {
                    // Only the disposable copied world is ever opened by this test run.
                    for (var child : mc.screen.children()) {
                        if (child instanceof Button button && button.getMessage().getString().equals("Proceed")) {
                            button.onPress();
                            break;
                        }
                    }
                }
                if (bootTicks > 1200) throw new IllegalStateException("Visual client did not enter the disposable world");
                return;
            }
            if (!initialized) {
                initialized = true;
                mc.options.guiScale().set(3);
                mc.options.renderDistance().set(4);
                mc.options.tutorialStep = TutorialSteps.NONE;
                // Every frame drawn, even behind other windows: with vsync on, Windows slows a client in the background
                // to a few frames a second and the night flash falls between them; a paused client stops the scene.
                mc.options.enableVsync().set(false);
                mc.getWindow().updateVsync(false);
                mc.options.pauseOnLostFocus = false;
                mc.resizeDisplay();
                mc.getSingleplayerServer().execute(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    player.setGameMode(GameType.CREATIVE);
                    player.setNoGravity(true);
                    player.teleportTo(0.5, 80, 0.5);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                    CombatService.setCombatMode(player, true);
                    QiService.setQiMax(player, 120);
                    QiService.setQi(player, 87);
                    EpicFightCombatDefaults.enforce(player);
                    EpicFightBridge.patch(player).setStamina(5);
                    ready.set(true);
                });
            }
            if (!ready.get()) return;
            mc.getToasts().clear();
            ticks++;
            nightFrames(mc);
            banditCutFrames();
            switch (ticks) {
                case 50 -> mc.setScreen(new MurimProfileScreen());
                case 70 -> capture("01-profile.png");
                case 80 -> mc.setScreen(new SkillEditScreen(mc.player, EpicFightBridge.patch(mc.player).getPlayerSkills()));
                case 82 -> require(mc.screen instanceof MurimProfileScreen, "Native skills GUI remained open");
                case 100 -> {
                    require(mc.screen.children().size() == 5, "Native technique library widgets remained");
                    var patch = EpicFightBridge.patch(mc.player);
                    require(patch.getSkill(SkillSlots.GUARD).getSkill() == MurimSkills.MURIM_GUARD.get(), "Default (Murim) guard not synchronized");
                    require(patch.getSkill(SkillSlots.DODGE).getSkill() == EpicFightSkills.ROLL.get(), "Default dodge not synchronized");
                }
                case 115 -> capture("02-techniques.png");
                case 125 -> mc.setScreen(new SkillBookScreen(mc.player, EpicFightSkills.GUARD.get(), null, null));
                case 127 -> require(mc.screen instanceof MurimProfileScreen, "Native skill book GUI remained open");
                case 140 -> { mc.setScreen(new MurimProfileScreen()); clickButton("Cultivation"); }
                case 155 -> capture("03-cultivation.png");
                case 165 -> { mc.setScreen(new MurimProfileScreen()); clickButton("Info"); }
                case 180 -> capture("04-info.png");
                case 190 -> clickButton("Combat Settings");
                case 192 -> {
                    checkToggle("Blood effects:", () -> ClientConfig.bloodEffects);
                    checkToggle("Camera motion:", () -> ClientConfig.enableFirstPersonCameraMove);
                    checkToggle("Auto perspective:", () -> ClientConfig.autoPerspectiveSwithing);
                    checkToggle("First-person body:", () -> ClientConfig.enableAnimatedFirstPersonModel);
                    checkToggle("Lock-on snapping:", () -> ClientConfig.lockOnSnapping);
                    var before = ClientConfig.tpsType;
                    clickButton("Camera:");
                    require(ClientConfig.tpsType != before, "Camera mode did not change");
                    for (int count = 0; ClientConfig.tpsType != before && count < 10; count++) clickButton("Camera:");
                    require(ClientConfig.tpsType == before, "Camera mode did not restore");
                    mc.screen.setFocused(null);
                }
                case 205 -> capture("05-settings.png");
                case 210 -> {
                    clickButton("Keybinds");
                    require(mc.screen instanceof ControlsScreen,
                            "The GUI Keybinds action did not open controls");
                }
                case 215 -> mc.setScreen(new KeyBindsScreen(mc.screen, mc.options));
                case 220 -> checkControls();
                case 235 -> capture("06-keybinds.png");
                case 245 -> {
                    mc.setScreen(null);
                    mc.getSingleplayerServer().execute(() -> {
                        var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                        player.setGameMode(GameType.SURVIVAL);
                        EpicFightBridge.patch(player).setStamina(5);
                    });
                }
                case 265 -> {
                    var patch = EpicFightBridge.patch(mc.player);
                    patch.setStamina(0);
                    require(patch.getStamina() == patch.getMaxStamina(), "Client still reads depleted stamina");
                    require(patch.hasStamina(patch.getMaxStamina() + 100), "Client still limits actions by stamina");
                    capture("07-hud.png");
                }
                case 280 -> {
                    mc.getWindow().setWindowed(960, 720);
                    mc.options.guiScale().set(3);
                    mc.resizeDisplay();
                    mc.setScreen(MurimProfileScreen.techniques());
                }
                case 295 -> capture("08-small-scale.png");
                case 300 -> {
                    mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getPlayerList()
                            .getPlayer(mc.player.getUUID()).setGameMode(GameType.CREATIVE));
                }
                case 305 -> {
                    require(mc.gameMode.hasInfiniteItems(), "Creative mode switch was not synchronized");
                    CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), true, mc.level.registryAccess());
                    for (var tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
                        require(tab.getDisplayItems().stream().noneMatch(EpicFightContentPolicy::blocked), "Native creative item remained");
                        require(tab.getSearchTabDisplayItems().stream().noneMatch(EpicFightContentPolicy::blocked), "Native creative search item remained");
                    }
                    mc.setScreen(new CreativeModeInventoryScreen(mc.player,
                            mc.level.enabledFeatures(), true));
                }
                case 315 -> {
                    require(mc.screen instanceof CreativeModeInventoryScreen,
                            "Creative inventory was replaced by survival inventory");
                    capture("09-creative.png");
                }
                case 320 -> {
                    mc.setScreen(new MurimProfileScreen());
                    clickButton("Close");
                    require(mc.screen == null, "Close button did not close the GUI");
                    mc.setScreen(new MurimProfileScreen());
                    mc.screen.keyPressed(GLFW.GLFW_KEY_K, 0, 0);
                    require(mc.screen == null, "Profile key did not close the GUI");
                    mc.setScreen(new MurimProfileScreen());
                    mc.screen.keyPressed(GLFW.GLFW_KEY_E, 0, 0);
                    require(mc.screen == null, "Inventory key did not close the GUI");
                    mc.setScreen(new MurimProfileScreen());
                    mc.screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
                    require(mc.screen == null, "Escape did not close the GUI");
                    mc.options.guiScale().set(2);
                    mc.getWindow().setWindowed(1280, 720);
                    mc.resizeDisplay();
                    mc.setScreen(new MurimProfileScreen());
                }
                case 335 -> capture("10-profile-large.png");
                case 340 -> clickButton("Cultivation");
                case 355 -> capture("11-cultivation-large.png");
                case 360 -> clickButton("Info");
                case 365 -> clickButton("Combat Settings");
                case 380 -> capture("12-settings-large.png");
                case 390 -> mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var mob = MurimEntities.TRAINING_OPPONENT.get().create(level);
                    mob.setNoGravity(true);
                    mob.moveTo(1.5, 80, 0.5, 0, 0);
                    require(level.addFreshEntity(mob), "Custom mob spawn failed");
                    mob.setNoAi(true);
                    opponentId = mob.getUUID();
                });
                case 410 -> {
                    var mob = mc.level.entitiesForRendering().iterator();
                    LivingEntity opponent = null;
                    while (mob.hasNext()) {
                        var candidate = mob.next();
                        if (candidate.getUUID().equals(opponentId)) opponent = (LivingEntity) candidate;
                    }
                    require(opponent != null, "Custom mob was not tracked on the real client");
                    var patch = EpicFightCapabilities.getEntityPatch(opponent,
                            ZombiePatch.class);
                    require(patch != null, "Custom client mob patch missing");
                    require(RenderEngine.getInstance().hasRendererFor(opponent),
                            "Custom mob animated renderer missing");
                    mc.setScreen(new MobPreviewScreen(opponent));
                }
                case 415 -> capture("13-mob-idle.png");
                case 420 -> mc.getSingleplayerServer().execute(() -> {
                    var mob = mc.getSingleplayerServer().overworld().getEntity(opponentId);
                    var patch = EpicFightCapabilities.getEntityPatch(mob,
                            ZombiePatch.class);
                    patch.playAnimationSynchronized(Animations.BIPED_MOB_ONEHAND1, 0);
                });
                case 430 -> capture("14-mob-windup.png");
                case 433 -> capture("15-mob-contact.png");
                // Murimblock creatures with their own Epic Fight armature: crab man (idle, jab) and armored crab.
                case 440 -> mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var old = level.getEntity(opponentId);
                    if (old != null) old.discard();
                    opponentId = spawnPreviewMob(level, MurimEntities.CRAB_MAN.get());
                });
                case 460 -> previewTracked(CrabManPatch.class);
                case 466 -> capture("16-crab-man-idle.png");
                case 470 -> mc.getSingleplayerServer().execute(() -> EpicFightCapabilities
                        .getEntityPatch(mc.getSingleplayerServer().overworld().getEntity(opponentId), CrabManPatch.class)
                        .playAnimationSynchronized(CrabManEpicFight.CLAW_SNAP, 0));
                case 487 -> capture("17-crab-man-jab.png");
                case 495 -> mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var old = level.getEntity(opponentId);
                    if (old != null) old.discard();
                    opponentId = spawnPreviewMob(level, MurimEntities.ARMORED_CRAB.get());
                });
                case 515 -> previewTracked(ArmoredCrabPatch.class);
                case 521 -> capture("18-armored-crab-idle.png");
                // Crab man water ball (GeckoLib model): forming in the claw, in flight, burst on a wall 6 blocks ahead.
                case 525 -> mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var old = level.getEntity(opponentId);
                    if (old != null) old.discard();
                    for (int x = -2; x <= 5; x++) for (int y = 77; y <= 84; y++)
                        level.setBlockAndUpdate(new BlockPos(x, y, 7), Blocks.STONE.defaultBlockState());
                    opponentId = spawnPreviewMob(level, MurimEntities.CRAB_MAN.get());
                });
                case 545 -> {
                    previewTracked(CrabManPatch.class);
                    mc.setScreen(new WaterBallPreviewScreen(opponentId));
                }
                case 548 -> mc.getSingleplayerServer().execute(() -> EpicFightCapabilities
                        .getEntityPatch(mc.getSingleplayerServer().overworld().getEntity(opponentId), CrabManPatch.class)
                        .playAnimationSynchronized(CrabManEpicFight.WATER_BALL, 0));
                case 562 -> capture("19-water-ball-charge.png");
                case 569 -> capture("20-water-ball-flight.png");
                case 580 -> capture("21-water-ball-splash.png");
                case 595 -> mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    for (int x = -2; x <= 5; x++) for (int y = 77; y <= 84; y++)
                        level.setBlockAndUpdate(new BlockPos(x, y, 7), Blocks.AIR.defaultBlockState());
                });
                // Water ball at night in a closed stone room, seen from the player's eyes: its reflections glow a
                // little (GeckoLib glow mask) and its burst flashes light on the room (Photon dynamic light).
                case 600 -> {
                    mc.setScreen(null);
                    mc.options.hideGui = true;
                    mc.getSingleplayerServer().execute(() -> {
                        var level = mc.getSingleplayerServer().overworld();
                        var old = level.getEntity(opponentId);
                        if (old != null) old.discard();
                        darkRoom(level, true);
                        level.setDayTime(18000);
                        opponentId = spawnPreviewMob(level, MurimEntities.CRAB_MAN.get());
                        var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                        // Side-on: the crab man on the right, the ball flying away to the left.
                        player.connection.teleport(8.0, 80, 3.0, 75, 10);
                    });
                }
                case 618 -> capture("22-night-before.png");
                case 620 -> mc.getSingleplayerServer().execute(() -> EpicFightCapabilities
                        .getEntityPatch(mc.getSingleplayerServer().overworld().getEntity(opponentId), CrabManPatch.class)
                        .playAnimationSynchronized(CrabManEpicFight.WATER_BALL, 0));
                case 640 -> capture("23-night-charge.png");
                case 690 -> {
                    mc.options.hideGui = false;
                    mc.getSingleplayerServer().execute(() -> {
                        var level = mc.getSingleplayerServer().overworld();
                        darkRoom(level, false);
                        level.setDayTime(6000);
                    });
                }
                // The Mountain Bandit: its five outfits holding the Bandit Dao, then one cut with it.
                case 705 -> mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var old = level.getEntity(opponentId);
                    if (old != null) old.discard();
                    opponentId = spawnPreviewMob(level, MurimEntities.MOUNTAIN_BANDIT.get());
                    var bandit = (MountainBanditEntity) level.getEntity(opponentId);
                    bandit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.BANDIT_DAO.get()));
                });
                case 725 -> {
                    // Three quarters, so the dao shows along its length (face on, it points at the camera).
                    previewTurn = -70;
                    previewTracked(MountainBanditPatch.class);
                }
                case 731, 741, 751, 761, 771 -> capture(String.format("%02d-bandit-%s.png", 26 + (ticks - 731) / 10, MountainBanditEntity.OUTFITS[(ticks - 731) / 10]));
                case 735, 745, 755, 765 -> {
                    int outfit = (ticks - 735) / 10 + 1;
                    mc.getSingleplayerServer().execute(() ->
                            ((MountainBanditEntity) mc.getSingleplayerServer().overworld().getEntity(opponentId)).setVariant(outfit));
                }
                case 775 -> mc.getSingleplayerServer().execute(() -> {
                    var bandit = (MountainBanditEntity) mc.getSingleplayerServer().overworld().getEntity(opponentId);
                    bandit.setVariant(1);
                    EpicFightCapabilities.getEntityPatch(bandit, MountainBanditPatch.class).playAnimationSynchronized(MurimAnimations.DRIFTING_CUT, 0);
                });
                // The cut every tick for a second and a half: the dao's tassel trails, swings back and settles.
                case 777, 778, 779, 780, 781, 782, 783, 784, 785, 786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796,
                     797, 798, 799, 800, 801, 802, 803, 804, 805, 806 -> capture(String.format("bandit-cut/%02d.png", ticks - 777));
                case 808 -> previewTurn = 0;
                // The same bandit in the world, seen from behind its right hand at the hand's height: the dao held edge
                // down, its tassel hanging under the pommel, then trailing behind a cut.
                case 810 -> {
                    mc.setScreen(null);
                    mc.options.hideGui = true;
                    mc.getSingleplayerServer().execute(() -> {
                        mc.getSingleplayerServer().overworld().setBlockAndUpdate(new BlockPos(-1, 78, -1), Blocks.STONE.defaultBlockState());
                        mc.getSingleplayerServer().getPlayerList().getPlayers().get(0).connection.teleport(-0.5, 79, -0.5, -54, 0);
                    });
                }
                case 820 -> capture("31-bandit-world.png");
                // Each of the bandit's cuts, every tick, from where its edge shows: the level cut and its backhand from
                // straight above (the blade flat, its edge on the side it sweeps toward), the falling cut from its right
                // side (the edge leading down). Barriers (invisible) hold the camera up.
                case 822 -> standAndLook(mc, new BlockPos(1, 82, 0), 1.5, 83, 0.5, 0, 90);
                case 828 -> playBanditCut(mc, MurimAnimations.DRIFTING_CUT);
                case 850 -> playBanditCut(mc, MurimAnimations.RETURNING_CUT);
                case 872 -> standAndLook(mc, new BlockPos(-3, 78, 0), -2.5, 79, 0.5, -90, -6);
                case 880 -> playBanditCut(mc, MurimAnimations.FALLING_RAIN);
                case 902 -> mc.options.hideGui = false;
                // The Bandit Chief: in the preview with his great axe, then each of his attacks in the world: the cleave and the
                // leaping slam from his right side, the sweep from straight above (where its edge shows), a pause before each.
                case 906 -> mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var old = level.getEntity(opponentId);
                    if (old != null) old.discard();
                    // Clear the bandit's camera barriers first: the chief stands 2.5 blocks tall.
                    level.setBlockAndUpdate(new BlockPos(1, 82, 0), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(-3, 78, 0), Blocks.AIR.defaultBlockState());
                    opponentId = spawnPreviewMob(level, MurimEntities.BANDIT_CHIEF.get());
                    ((BanditChiefEntity) level.getEntity(opponentId)).setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.CHIEF_GREAT_AXE.get()));
                });
                case 920 -> {
                    previewTurn = -70;
                    previewTracked(BanditChiefPatch.class);
                }
                case 926 -> capture("32-chief.png");
                case 928 -> previewTurn = 0;
                case 932 -> capture("33-chief-front.png");
                case 936 -> {
                    mc.setScreen(null);
                    mc.options.hideGui = true;
                    standAndLook(mc, new BlockPos(-4, 79, 0), -3.4, 80, 1.0, -90, 4);
                }
                // Standing with the axe on his shoulder (his idle), from his right, then from the front after the slam.
                case 944 -> capture("36-chief-idle-side.png");
                // He plays his attacks at about two thirds of their speed (the great axe is slow): each is captured whole, and
                // the camera moves a dozen ticks before (the client takes that long to show it from its new place).
                case 946 -> playChief(mc, MurimAnimations.CHIEF_CLEAVE);
                case 984 -> standAndLook(mc, new BlockPos(1, 85, 0), 1.5, 86, 0.5, 0, 90);
                case 996 -> playChief(mc, MurimAnimations.CHIEF_SWEEP);
                case 1032 -> standAndLook(mc, new BlockPos(-5, 79, 1), -4.6, 80, 1.8, -90, 4);
                case 1046 -> playChief(mc, MurimAnimations.CHIEF_SLAM);
                case 1096 -> standAndLook(mc, new BlockPos(3, 79, 5), 4.0, 80, 6.0, 146, 6);
                case 1110 -> capture("37-chief-idle-front.png");
                case 1111 -> {
                    mc.options.hideGui = false;
                    mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().overworld().setBlockAndUpdate(new BlockPos(1, 85, 0), Blocks.AIR.defaultBlockState()));
                }
                // The faces up close in the preview: a bandit, then the chief, each in its own idle (blinks, glances).
                case 1113 -> mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var old = level.getEntity(opponentId);
                    if (old != null) old.discard();
                    opponentId = spawnPreviewMob(level, MurimEntities.MOUNTAIN_BANDIT.get());
                    var bandit = (MountainBanditEntity) level.getEntity(opponentId);
                    bandit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.BANDIT_DAO.get()));
                    bandit.setVariant(1);
                });
                case 1125 -> {
                    previewTurn = -25;
                    previewScale = 300;
                    previewLift = 0.72F;
                    previewTracked(MountainBanditPatch.class);
                }
                case 1201 -> mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var old = level.getEntity(opponentId);
                    if (old != null) old.discard();
                    opponentId = spawnPreviewMob(level, MurimEntities.BANDIT_CHIEF.get());
                    ((BanditChiefEntity) level.getEntity(opponentId)).setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.CHIEF_GREAT_AXE.get()));
                });
                case 1213 -> {
                    previewLift = 0.95F;
                    previewTracked(BanditChiefPatch.class);
                }
                case 1291 -> {
                    previewTurn = 0;
                    previewScale = 80;
                    previewLift = 0.0625F;
                }
                // A band of three bandits standing in their idle, side by side in the world: each at its own point of
                // the loop (IdlePhase), not moving in unison.
                case 1295 -> {
                    mc.setScreen(null);
                    mc.options.hideGui = true;
                    mc.getSingleplayerServer().execute(() -> {
                        var level = mc.getSingleplayerServer().overworld();
                        var old = level.getEntity(opponentId);
                        if (old != null) old.discard();
                        for (int i = 0; i < 3; i++) {
                            var bandit = MurimEntities.MOUNTAIN_BANDIT.get().create(level);
                            bandit.setNoGravity(true);
                            bandit.moveTo(0.5 + i * 1.5, 80, 0.5, 0, 0);
                            bandit.setYHeadRot(0);
                            bandit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.BANDIT_DAO.get()));
                            bandit.setVariant(i);
                            level.addFreshEntity(bandit);
                            bandit.setNoAi(true);
                        }
                        mc.getSingleplayerServer().getPlayerList().getPlayers().get(0).connection.teleport(2.0, 79.6, 5.2, 180, 4);
                    });
                }
                case 1319 -> capture("34-bandits-out-of-step.png");
                case 1331 -> capture("35-bandits-out-of-step-later.png");
                case 1335 -> mc.options.hideGui = false;
                case 1339 -> {
                    ClientCaptureChecks.verify(mc.gameDirectory.toPath().resolve("screenshots"));
                    Files.writeString(mc.gameDirectory.toPath().resolve("visual-smoke-passed.txt"),
                            "Clear Manuscript: four pages and settings; button/K/E/Escape close; all six settings toggled and restored; keybind action; compact/large screenshots. Custom mob tracking, Epic Fight patch/renderer and synchronized attack captures; crab man water ball (GeckoLib) charge, flight and burst. Techniques empty; native screens replaced; guard/roll synchronized; one key category; native items absent; stamina disabled.\n");
                    mc.getSingleplayerServer().execute(() -> {
                        var mob = mc.getSingleplayerServer().overworld().getEntity(opponentId);
                        if (mob != null) mob.discard();
                    });
                    LOGGER.info("Murim client visual smoke checks passed");
                    mc.stop();
                }
                default -> { }
            }
        } catch (Exception exception) {
            failed = true;
            LOGGER.error("Murim client visual smoke failed", exception);
        }
    }

    /** The bandit's cuts and the chief's attacks, one capture a tick (see the cases 822 to 1110). */
    private static void banditCutFrames() throws Exception {
        if (ticks >= 947 && ticks <= 982) capture(String.format("chief-cleave/%02d.png", ticks - 947));
        if (ticks >= 997 && ticks <= 1030) capture(String.format("chief-sweep/%02d.png", ticks - 997));
        if (ticks >= 1047 && ticks <= 1094) capture(String.format("chief-slam/%02d.png", ticks - 1047));
        if (ticks >= 1129 && ticks < 1199) capture(String.format("bandit-face/%02d.png", ticks - 1129));
        if (ticks >= 1217 && ticks < 1287) capture(String.format("chief-face/%02d.png", ticks - 1217));
        if (ticks >= 829 && ticks <= 846) capture(String.format("bandit-cuts/drifting/%02d.png", ticks - 829));
        if (ticks >= 851 && ticks <= 868) capture(String.format("bandit-cuts/returning/%02d.png", ticks - 851));
        if (ticks >= 881 && ticks <= 900) capture(String.format("bandit-cuts/falling/%02d.png", ticks - 881));
    }

    /** The player stands on a barrier at (x, y, z) and looks (yaw, pitch); the bandit preview mob stays where it is. */
    private static void standAndLook(Minecraft mc, BlockPos floor, double x, double y, double z, float yaw, float pitch) {
        mc.getSingleplayerServer().execute(() -> {
            mc.getSingleplayerServer().overworld().setBlockAndUpdate(floor, Blocks.BARRIER.defaultBlockState());
            mc.getSingleplayerServer().getPlayerList().getPlayers().get(0).connection.teleport(x, y, z, yaw, pitch);
        });
    }

    private static void playChief(Minecraft mc, AnimationManager.AnimationAccessor<? extends StaticAnimation> attack) {
        mc.getSingleplayerServer().execute(() -> EpicFightCapabilities
                .getEntityPatch(mc.getSingleplayerServer().overworld().getEntity(opponentId), BanditChiefPatch.class)
                .playAnimationSynchronized(attack, 0));
    }

    private static void playBanditCut(Minecraft mc, AnimationManager.AnimationAccessor<? extends StaticAnimation> cut) {
        mc.getSingleplayerServer().execute(() -> EpicFightCapabilities
                .getEntityPatch(mc.getSingleplayerServer().overworld().getEntity(opponentId), MountainBanditPatch.class)
                .playAnimationSynchronized(cut, 0));
    }

    private static int flashTick = -1;

    /** Night sequence frames (for a preview GIF) and the first tick a ball bursts (the Photon flash). */
    private static void nightFrames(Minecraft mc) throws Exception {
        if (ticks < 618 || ticks > 685) return;
        capture("night/frame-" + String.format("%03d", ticks - 618) + ".png");
        if (flashTick < 0 && ticks > 620) {
            for (var e : mc.level.entitiesForRendering()) {
                if (e instanceof WaterBallEntity ball && ball.isBig() && ball.state() >= WaterBallEntity.SPLASH_GROUND) {
                    flashTick = ticks;
                    break;
                }
            }
        }
        // The screenshot shows the last rendered frame: the flash is on screen from the next ticks.
        if (flashTick > 0 && ticks == flashTick + 2) capture("24-night-flash.png");
        if (flashTick > 0 && ticks == flashTick + 25) capture("25-night-after.png");
    }

    /** Hollow stone box around the crab man's spot: floor y 79, roof y 86, x -5..9, z -4..15. */
    private static void darkRoom(ServerLevel level, boolean build) {
        var stone = Blocks.STONE.defaultBlockState();
        var air = Blocks.AIR.defaultBlockState();
        for (int x = -5; x <= 9; x++) for (int y = 79; y <= 86; y++) for (int z = -4; z <= 15; z++) {
            boolean shell = x == -5 || x == 9 || y == 79 || y == 86 || z == -4 || z == 15;
            if (shell) level.setBlockAndUpdate(new BlockPos(x, y, z), build ? stone : air);
        }
    }

    private static void clickButton(String message) {
        Screen screen = Minecraft.getInstance().screen;
        for (var child : screen.children()) {
            if (child instanceof Button button && button.getMessage().getString().startsWith(message)) {
                require(screen.mouseClicked(button.getX() + button.getWidth() / 2.0,
                        button.getY() + button.getHeight() / 2.0, 0), "Button did not handle click: " + message);
                return;
            }
        }
        throw new IllegalStateException("Missing button: " + message);
    }

    private static void checkToggle(String label, BooleanSupplier value) {
        boolean before = value.getAsBoolean();
        clickButton(label);
        require(value.getAsBoolean() != before, "Setting did not toggle: " + label);
        clickButton(label);
        require(value.getAsBoolean() == before, "Setting did not restore: " + label);
    }

    /** How far to the side the preview looks (pixels of a mouse offset): 0 faces the camera, more turns it to three quarters. */
    private static float previewTurn;
    /** The preview's size and the height it centres on (above the mob's middle, in blocks): raised for a face close up. */
    private static int previewScale = 80;
    private static float previewLift = 0.0625F;

    /** Frozen preview mob created directly (no finalizeSpawn, so the crab man comes without its escort). */
    private static UUID spawnPreviewMob(ServerLevel level, EntityType<? extends Mob> type) {
        var mob = type.create(level);
        mob.setNoGravity(true);
        mob.moveTo(1.5, 80, 0.5, 0, 0);
        require(level.addFreshEntity(mob), "Preview mob spawn failed: " + type);
        mob.setNoAi(true);
        return mob.getUUID();
    }

    private static void previewTracked(Class<? extends EntityPatch<?>> patchType) {
        var mc = Minecraft.getInstance();
        LivingEntity found = null;
        for (var candidate : mc.level.entitiesForRendering()) {
            if (candidate.getUUID().equals(opponentId)) found = (LivingEntity) candidate;
        }
        require(found != null, "Preview mob was not tracked on the real client");
        require(EpicFightCapabilities.getEntityPatch(found, patchType) != null, "Client patch missing: " + patchType.getSimpleName());
        require(RenderEngine.getInstance().hasRendererFor(found), "Animated renderer missing for " + found.getType());
        mc.setScreen(new MobPreviewScreen(found));
    }

    private static final class MobPreviewScreen extends Screen {
        private final LivingEntity mob;
        MobPreviewScreen(LivingEntity mob) { super(mob.getName()); this.mob = mob; }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float tick) {
            g.fill(0, 0, width, height, 0xFFF0E7CE);
            g.drawCenteredString(font, title, width / 2, 16, 0xFF202522);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g,
                    width / 2 - 90, 40, width / 2 + 90, height - 20, previewScale, previewLift, width / 2.0F + previewTurn, height / 2.0F, mob);
        }
    }

    /**
     * Side view of the crab man and every water ball near it, drawn by the real entity renderers (Epic Fight for the
     * crab man, GeckoLib for the balls). Blocks are not drawn: the wall the balls burst on is invisible here.
     */
    private static final class WaterBallPreviewScreen extends Screen {
        private final UUID manId;
        WaterBallPreviewScreen(UUID manId) { super(Component.literal("Crab Man - water ball")); this.manId = manId; }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float tick) {
            g.fill(0, 0, width, height, 0xFFF0E7CE);
            g.drawCenteredString(font, title, width / 2, 16, 0xFF202522);
            var mc = Minecraft.getInstance();
            Entity man = null;
            List<Entity> drawn = new ArrayList<>();
            for (var e : mc.level.entitiesForRendering()) {
                if (e.getUUID().equals(manId)) man = e;
                if (e instanceof WaterBallEntity) drawn.add(e);
            }
            if (man == null) return;
            drawn.add(0, man);
            var pose = g.pose();
            pose.pushPose();
            float scale = Math.min(width, height) / 7.0F;
            pose.translate(width * 0.3F, height * 0.82F, 100);
            pose.scale(scale, scale, -scale);
            pose.mulPose(Axis.ZP.rotationDegrees(180));
            pose.mulPose(Axis.XP.rotationDegrees(-12));
            pose.mulPose(Axis.YP.rotationDegrees(-90));
            Lighting.setupForEntityInInventory();
            var dispatcher = mc.getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            for (var e : drawn) {
                var off = e.getPosition(tick).subtract(man.getPosition(tick));
                dispatcher.render(e, off.x, off.y, off.z, 0, tick, pose, g.bufferSource(), 0xF000F0);
            }
            g.flush();
            dispatcher.setRenderShadow(true);
            pose.popPose();
            Lighting.setupFor3DItems();
        }
    }

    private static void checkControls() {
        Minecraft mc = Minecraft.getInstance();
        long headers = 0;
        int visible = 0;
        for (var listener : mc.screen.children()) {
            if (!(listener instanceof KeyBindsList list)) continue;
            for (var entry : list.children()) {
                if (!(entry instanceof KeyBindsList.KeyEntry key)) continue;
                require(!EpicFightControls.hidden(key.key), "Hidden native key row remained");
                if (key.key.getCategory().equals(MurimblockKeyMappings.CATEGORY)) visible++;
            }
            headers = list.children().stream().filter(entry -> entry instanceof KeyBindsList.CategoryEntry).count();
            list.setScrollAmount(Math.max(0, list.getMaxScroll() - 80));
        }
        // Five Murim controls (charge Qi, combat mode, profile, execute, combat bar slot 10), attack, guard, dodge and
        // four target-lock controls.
        require(visible == 12, "Expected 12 useful mappings in a single Murim category, got " + visible);
        long categories = Arrays.stream(mc.options.keyMappings).filter(mapping -> !EpicFightControls.hidden(mapping))
                .map(KeyMapping::getCategory).distinct().count();
        require(headers == categories, "Unexpected empty or duplicated keybind sections: " + headers);
        require(!EpicFightKeyMappings.ATTACK.isUnbound() && !EpicFightKeyMappings.GUARD.isUnbound(), "Combat inputs were disabled");
        require(EpicFightKeyMappings.SWITCH_MODE.isUnbound() && EpicFightKeyMappings.SKILL_EDIT.isUnbound(), "Duplicate shortcuts active");
    }

    private static void capture(String name) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        var folder = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(folder);
        Files.createDirectories(folder.resolve(name).getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(folder.resolve(name));
        }
        LOGGER.info("Visual capture: {}", name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
