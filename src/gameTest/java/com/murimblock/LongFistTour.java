package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.client.gui.MurimProfileScreen;
import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimAnimations.Motions;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import com.murimblock.qi.QiService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Development-only real-client check of the Long Fist (gradlew -PgameTests runLongFistTour): reading the scroll, the
 * Techniques tab (library, passive, Tiger Step Punch), then bare-handed in combat mode, filmed from the side: a real M1
 * (Epic Fight's punch), the bare-hand guard held on its real key, Tiger Step Punch, Rapid Fists and Whirlwind Kick on
 * keys 1 to 3, Inch Force, the dive from the air and the finish's motion; last the Profile page. Report in
 * long-fist-tour-report.txt, pictures in build/long-fist-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class LongFistTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static Marker camera;
    private static Vec3 start;
    private static final List<Zombie> zombies = new ArrayList<>();
    private static final List<String> report = new ArrayList<>();
    private static volatile float health;
    private static volatile double zombieZ;

    private LongFistTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.longFistTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-long-fist", GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-long-fist", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            step(mc, mc.getSingleplayerServer());
        } catch (Exception e) {
            check(false, "tour crashed: " + e);
            LOGGER.error("Long Fist tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        // Minecraft only sends the player's position to the server while the player is the camera: with the filming
        // camera, the tour sends it itself.
        if (camera != null && mc.getCameraEntity() == camera && mc.getConnection() != null) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
        }
        if (camera != null && ticks >= 56 && ticks < 362 && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
        // The guard's real key held (read again every tick: Minecraft reads the real keyboard back otherwise).
        if (ticks >= 84 && ticks < 112) EpicFightKeyMappings.GUARD.setDown(true);
        switch (ticks) {
            case 20 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer self = self(server);
                ItemStack scroll = new ItemStack(MurimItems.LONG_FIST_SCROLL.get());
                self.setItemInHand(InteractionHand.MAIN_HAND, scroll);
                scroll.use(level, self, InteractionHand.MAIN_HAND);
                check(MartialService.data(self).hasTechnique(MartialArts.TIGER_STEP_PUNCH.id()), "reading the scroll teaches Tiger Step Punch");
                self.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                MartialService.unlock(self, MartialArts.RAPID_FISTS);
                MartialService.unlock(self, MartialArts.WHIRLWIND_KICK);
                CombatService.setCombatMode(self, true);
                QiService.setQiMax(self, 100);
                QiService.setQi(self, 50);
                self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 0);
            });
            case 30 -> mc.setScreen(MurimProfileScreen.techniques());
            case 33 -> capture(mc, "00-library.png");
            case 34 -> ((MurimProfileScreen) mc.screen).selectForTests(MartialArts.STEADY_BREATH.id());
            case 40 -> capture(mc, "01-passive.png");
            case 41 -> ((MurimProfileScreen) mc.screen).selectForTests(MartialArts.TIGER_STEP_PUNCH.id());
            case 46 -> capture(mc, "02-tiger-step-page.png");
            case 48 -> {
                mc.setScreen(null);
                server.execute(() -> zombie(server, 0, 1.4));
            }
            case 56 -> {
                mc.options.hideGui = false;
                yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
                yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
                start = mc.player.position();
                placeSideCamera(mc);
            }
            // A real M1: Epic Fight's own first punch.
            case 60 -> KeyMapping.click(mc.options.keyAttack.getKey());
            case 63 -> check(clientPlaying(mc, Animations.FIST_AUTO1), "the attack key bare-handed plays Epic Fight's punch (playing: " + clientAnimation(mc) + ")");
            case 78 -> server.execute(() -> {
                check(zombies.getFirst().getHealth() < zombies.getFirst().getMaxHealth(), "the punch lands");
                check(QiService.getQi(self(server)) >= 50.9, "a punch that lands gives back Qi (" + QiService.getQi(self(server)) + ")");
            });
            // The guard, held from tick 84 on its real key: the art's forearms up.
            case 98 -> {
                PlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(mc.player, PlayerPatch.class);
                check(patch != null && patch.currentCompositeMotion == Motions.FIST_GUARD,
                        "holding the guard bare-handed shows the Long Fist's guard (composite " + (patch == null ? null : patch.currentCompositeMotion) + ")");
                capture(mc, "04-guard.png");
            }
            case 112 -> EpicFightKeyMappings.GUARD.setDown(false);
            // Key 1: Tiger Step Punch, the zombie 2.8 blocks ahead.
            case 116 -> server.execute(() -> reset(server, 0, 2.8));
            case 120 -> press(mc, 0);
            case 122 -> mc.options.keyHotbarSlots[0].setDown(false);
            case 125 -> check(clientPlaying(mc, MurimAnimations.TIGER_STEP_PUNCH), "key 1 plays Tiger Step Punch (playing: " + clientAnimation(mc) + ")");
            case 146 -> {
                double moved = mc.player.position().subtract(start).horizontalDistance();
                check(moved > 1.5 && moved < 4.0, String.format("the bound carries the player about 3 blocks (moved %.2f)", moved));
                server.execute(() -> check(zombies.getFirst().getHealth() < health, "the bound's punch lands"));
            }
            // Key 2: Rapid Fists.
            case 156 -> server.execute(() -> reset(server, 0, 1.5));
            case 160 -> press(mc, 1);
            case 162 -> mc.options.keyHotbarSlots[1].setDown(false);
            case 165 -> check(clientPlaying(mc, MurimAnimations.RAPID_FISTS), "key 2 plays Rapid Fists (playing: " + clientAnimation(mc) + ")");
            case 190 -> server.execute(() -> check(health - zombies.getFirst().getHealth() > 1.0, "the flurry strikes again and again (" + (health - zombies.getFirst().getHealth()) + ")"));
            // Key 3: Whirlwind Kick, a zombie in front and one behind.
            case 196 -> server.execute(() -> {
                reset(server, 0, 1.6);
                zombie(server, 0, -1.6);
            });
            case 200 -> press(mc, 2);
            case 202 -> mc.options.keyHotbarSlots[2].setDown(false);
            case 205 -> check(clientPlaying(mc, MurimAnimations.WHIRLWIND_KICK), "key 3 plays Whirlwind Kick (playing: " + clientAnimation(mc) + ")");
            case 228 -> server.execute(() -> check(zombies.stream().allMatch(z -> z.getHealth() < z.getMaxHealth()), "the whirlwind strikes in front and behind"));
            // Inch Force, played as the server would after a parry: the fist at the chest, the body sent flying.
            case 236 -> server.execute(() -> {
                reset(server, 0, 1.0);
                zombies.getFirst().setNoAi(false);
                zombies.getFirst().addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 400, 9));
                zombieZ = zombies.getFirst().getZ();
            });
            case 240 -> server.execute(() -> EpicFightCapabilities.getEntityPatch(self(server), PlayerPatch.class).playAnimationSynchronized(MurimAnimations.INCH_FORCE, 0.0F));
            case 262 -> server.execute(() -> check(zombies.getFirst().getZ() - zombieZ > 2.5, String.format("Inch Force sends the body flying (%.2f blocks)", zombies.getFirst().getZ() - zombieZ)));
            // From the air: the dive.
            case 270 -> server.execute(() -> {
                reset(server, 0, 3.0);
                ServerPlayer self = self(server);
                self.teleportTo(server.overworld(), self.getX(), self.getY() + 2.5, self.getZ(), 0, 0);
            });
            case 274 -> press(mc, 0);
            case 276 -> mc.options.keyHotbarSlots[0].setDown(false);
            case 279 -> check(clientPlaying(mc, MurimAnimations.TIGER_STEP_DIVE), "key 1 in the air plays the dive (playing: " + clientAnimation(mc) + ")");
            // The finish's motion, alone (no one downed here): Final Palm.
            case 306 -> server.execute(() -> {
                reset(server, 0, 1.5);
                EpicFightCapabilities.getEntityPatch(self(server), PlayerPatch.class).playAnimationSynchronized(MurimAnimations.FINISH_LONG_FIST, 0.0F);
            });
            case 312 -> check(clientPlaying(mc, MurimAnimations.FINISH_LONG_FIST), "Final Palm plays (playing: " + clientAnimation(mc) + ")");
            case 362 -> {
                mc.setCameraEntity(mc.player);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                mc.setScreen(new MurimProfileScreen());
            }
            case 368 -> capture(mc, "11-profile.png");
            case 372 -> {
                server.execute(() -> zombies.forEach(Zombie::discard));
                finish(mc);
            }
            default -> { }
        }
        record(mc, "03-m1", 58, 76, 1);
        record(mc, "05-tiger-step-punch", 118, 146, 1);
        record(mc, "06-rapid-fists", 158, 190, 1);
        record(mc, "07-whirlwind-kick", 198, 228, 1);
        record(mc, "08-inch-force", 238, 262, 1);
        record(mc, "09-dive", 272, 300, 1);
        record(mc, "10-final-palm", 304, 356, 2);
    }

    /** A sturdy zombie without AI, `dx` to the side and `dz` in front of the player. */
    private static void zombie(MinecraftServer server, double dx, double dz) {
        ServerPlayer self = self(server);
        Zombie z = EntityType.ZOMBIE.create(server.overworld());
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        z.setHealth(200);
        z.moveTo(self.getX() + dx, self.getY(), self.getZ() + dz, dz >= 0 ? 180 : 0, 0);
        z.setNoAi(true);
        z.setPersistenceRequired();
        // A helmet: no burning in the sun.
        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        server.overworld().addFreshEntity(z);
        zombies.add(z);
    }

    /** The player back at the start, facing south, techniques ready; one fresh zombie `dz` ahead. */
    private static void reset(MinecraftServer server, double dx, double dz) {
        ServerPlayer self = self(server);
        zombies.forEach(Zombie::discard);
        zombies.clear();
        self.teleportTo(server.overworld(), start.x, start.y, start.z, 0, 0);
        self.setDeltaMovement(Vec3.ZERO);
        MartialService.resetCooldowns(self);
        QiService.setQi(self, 100);
        zombie(server, dx, dz);
        health = 200;
    }

    /** Presses a combat bar key (its release, two ticks later, casts). */
    private static void press(Minecraft mc, int slot) {
        mc.options.keyHotbarSlots[slot].setDown(true);
        KeyMapping.click(mc.options.keyHotbarSlots[slot].getKey());
    }

    /** The side camera, beside where the player stands. */
    private static void placeSideCamera(Minecraft mc) {
        Vec3 at = mc.player.position();
        Vec3 wanted = at.add(6.5, 1.8, 1.0);
        Vec3 d = at.add(0, 0.9, 1.0).subtract(wanted);
        Vec3 eye = wanted.add(d.normalize().scale(4));
        if (camera == null) camera = new Marker(EntityType.MARKER, mc.level);
        camera.setPos(eye);
        camera.xo = camera.xOld = eye.x;
        camera.yo = camera.yOld = eye.y;
        camera.zo = camera.zOld = eye.z;
        camera.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        camera.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        camera.yRotO = camera.getYRot();
        camera.xRotO = camera.getXRot();
        mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        mc.setCameraEntity(camera);
    }

    private static String clientAnimation(Minecraft mc) {
        var patch = EpicFightCapabilities.getEntityPatch(mc.player, PlayerPatch.class);
        var main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        return main == null || main.getRealAnimation() == null ? "nothing" : String.valueOf(main.getRealAnimation().get());
    }

    private static boolean clientPlaying(Minecraft mc, AnimationAccessor<?> animation) {
        var patch = EpicFightCapabilities.getEntityPatch(mc.player, PlayerPatch.class);
        if (patch == null) return false;
        var main = patch.getAnimator().getPlayerFor(null);
        return main != null && main.getRealAnimation() != null && main.getRealAnimation().get() == animation.get();
    }

    /** No first-person hand while the side camera films. */
    @SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        if (Boolean.getBoolean("murimblock.longFistTour") && camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
    }

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void record(Minecraft mc, String name, int from, int to, int every) throws Exception {
        if (ticks >= from && ticks <= to && (ticks - from) % every == 0) capture(mc, String.format("%s/frame-%03d.png", name, (ticks - from) / every));
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        net.minecraft.Util.ioPool().execute(() -> {
            try (image) { image.writeToFile(file); } catch (Exception e) { LOGGER.error("Frame write failed", e); }
        });
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Long Fist tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("long-fist-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Long Fist tour report", e);
        }
        LOGGER.info("Long Fist tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
