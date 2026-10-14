package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.client.gui.MurimProfileScreen;
import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.MurimAnimations;
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
import net.minecraft.world.entity.Marker;
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

/**
 * Development-only real-client check of the Azure Cloud Sword Art (gradlew -PgameTests runMartialTour): reading the
 * manual, the Techniques tab, then Cloud Step Slash cast with the real key 1 in combat mode, dashing through three
 * zombies; then a real M1 (the stance's Drifting Cut), Thousand Drizzle close and at mid-distance (key 2), and the
 * crescent (key 3) taking the player up; filmed from the side; last the passive's and Thousand Drizzle's pages of the
 * Techniques tab. Report in martial-tour-report.txt, pictures in build/martial-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MartialTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static Marker camera;
    private static Vec3 start;
    private static final List<Zombie> zombies = new ArrayList<>();
    private static final List<String> report = new ArrayList<>();

    private MartialTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.martialTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-martial", GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-martial", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Martial tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        // Minecraft only sends the player's position to the server while the player is the camera: with the filming
        // camera, the tour sends it itself, or the server would keep the player where the filming began.
        if (camera != null && mc.getCameraEntity() == camera && mc.getConnection() != null) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
        }
        record(mc, "02-cloud-step-slash", 86, 126, 1);
        // The side camera holds through the attacks (the game puts the view back on the player otherwise).
        if (camera != null && (ticks < 278 || (ticks >= 316 && ticks < 380)) && !(ticks >= 128 && ticks < 136) && mc.getCameraEntity() != camera) {
            if (ticks > 61) LOGGER.info("Martial tour: camera taken back at tick {}", ticks);
            mc.setCameraEntity(camera);
        }
        switch (ticks) {
            case 20 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer self = self(server);
                // Reading the manual.
                ItemStack manual = new ItemStack(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get());
                self.setItemInHand(InteractionHand.MAIN_HAND, manual);
                manual.use(level, self, InteractionHand.MAIN_HAND);
                check(MartialService.data(self).hasTechnique(MartialArts.CLOUD_STEP_SLASH.id()), "reading the manual teaches Cloud Step Slash");
            });
            // The Techniques tab, Cloud Step Slash selected.
            case 30 -> mc.setScreen(MurimProfileScreen.techniques());
            case 34 -> {
                var screen = (MurimProfileScreen) mc.screen;
                screen.selectForTests(MartialArts.CLOUD_STEP_SLASH.id());
            }
            case 40 -> capture(mc, "01-techniques-tab.png");
            case 44 -> mc.setScreen(null);
            // Combat mode, a sword, zombies on the path; a side camera.
            case 50 -> server.execute(() -> {
                ServerPlayer self = self(server);
                ServerLevel level = server.overworld();
                self.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                CombatService.setCombatMode(self, true);
                QiService.setQiMax(self, 100);
                QiService.setQi(self, 100);
                self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 0);
                for (int i = 0; i < 3; i++) {
                    Zombie z = EntityType.ZOMBIE.create(level);
                    z.moveTo(self.getX() + (i - 1) * 0.6, self.getY(), self.getZ() + 1.6 + i * 1.3, 180, 0);
                    z.setNoAi(true);
                    z.setPersistenceRequired();
                    z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                    level.addFreshEntity(z);
                    zombies.add(z);
                }
            });
            case 60 -> {
                mc.options.hideGui = false;
                // No Epic Fight target outline on the zombies in the pictures.
                yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
                yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
                start = mc.player.position();
                camera = new Marker(EntityType.MARKER, mc.level);
                // Third person, so Epic Fight draws the whole body (not first-person arms): the view then backs off 4 blocks
                // from the marker, which therefore stands 4 blocks nearer than the wanted eye.
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                Vec3 wanted = start.add(6.5, 1.8, 2.5);
                Vec3 d = start.add(0, 0.9, 2.5).subtract(wanted);
                Vec3 eye = wanted.add(d.normalize().scale(4));
                camera.setPos(eye);
                // Old position too: the camera interpolates between the two.
                camera.xo = camera.xOld = eye.x;
                camera.yo = camera.yOld = eye.y;
                camera.zo = camera.zOld = eye.z;
                camera.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
                camera.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
                camera.yRotO = camera.getYRot();
                camera.xRotO = camera.getXRot();
                mc.setCameraEntity(camera);
            }
            case 84 -> capture(mc, "02-before.png");
            // The real key 1 (the first hotbar key), in combat mode, held: Cloud Step Slash's draw stance, to aim.
            // (Held again every tick: Minecraft reads the real keyboard back when its window is not in front.)
            case 88 -> {
                mc.options.keyHotbarSlots[0].setDown(true);
                KeyMapping.click(mc.options.keyHotbarSlots[0].getKey());
            }
            case 89, 90, 91, 92, 93, 94, 95, 97, 98, 99 -> mc.options.keyHotbarSlots[0].setDown(true);
            case 96 -> {
                mc.options.keyHotbarSlots[0].setDown(true);
                check(clientPlaying(mc, MurimAnimations.CLOUD_STEP_AIM), "holding key 1 takes Cloud Step Slash's draw stance");
                capture(mc, "02b-aim.png");
                server.execute(() -> check(Math.abs(QiService.getQi(self(server)) - 100) < 0.2, "the stance alone costs no Qi (" + QiService.getQi(self(server)) + ")"));
            }
            // Let go: the dash (filmed into 02-cloud-step-slash, with its afterimages and wind).
            case 100 -> mc.options.keyHotbarSlots[0].setDown(false);
            case 120 -> {
                capture(mc, "03-after.png");
                double moved = mc.player.position().subtract(start).horizontalDistance();
                check(moved > 3.5 && moved < 6.5, String.format("Cloud Step Slash carries the player about 5 blocks (moved %.2f)", moved));
                check(mc.player.getInventory().selected == 0, "key 1 did not change the held item in combat mode");
                server.execute(() -> {
                    ServerPlayer self = self(server);
                    long hit = zombies.stream().filter(z -> z.getHealth() < z.getMaxHealth()).count();
                    check(hit >= 2, "Cloud Step Slash cuts the zombies on its path (" + hit + " of 3)");
                    // The passive regeneration adds a little over the second since the cast.
                    check(Math.abs(QiService.getQi(self) - (100 - MartialArts.CLOUD_STEP_SLASH.qiCost())) < 0.2, "it costs its Qi (" + QiService.getQi(self) + ")");
                    check(MartialService.cooldownLeft(self, MartialArts.CLOUD_STEP_SLASH.id()) > 0, "it is on cooldown");
                });
                check(com.murimblock.client.CombatBarClient.cooldownLeft(MartialArts.CLOUD_STEP_SLASH.id()) > 0, "the client knows the cooldown (for the bar)");
                server.execute(() -> {
                });
            }
            case 124 -> capture(mc, "04-cooldown-bar.png");
            // The rest of the art: a sturdy zombie in front, the three techniques on keys 1 to 3, half the Qi.
            case 130 -> server.execute(() -> {
                ServerPlayer self = self(server);
                ServerLevel level = server.overworld();
                zombies.forEach(Zombie::discard);
                zombies.clear();
                MartialService.unlock(self, MartialArts.THOUSAND_DRIZZLE);
                MartialService.unlock(self, MartialArts.HEAVEN_SPLITTING_CRESCENT);
                MartialService.assign(self, 1, MartialArts.THOUSAND_DRIZZLE.id());
                MartialService.assign(self, 2, MartialArts.HEAVEN_SPLITTING_CRESCENT.id());
                MartialService.resetCooldowns(self);
                QiService.setQi(self, 50);
                Zombie z = EntityType.ZOMBIE.create(level);
                z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
                z.setHealth(200);
                z.moveTo(self.getX(), self.getY(), self.getZ() + 1.8, 180, 0);
                z.setNoAi(true);
                z.setPersistenceRequired();
                // A helmet: no burning in the sun (the flames would hide the cuts in the pictures).
                z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                level.addFreshEntity(z);
                zombies.add(z);
            });
            case 136 -> placeSideCamera(mc);
            // A real M1 (the attack key): Flowing Cloud Stance's first strike, Drifting Cut.
            case 140 -> KeyMapping.click(mc.options.keyAttack.getKey());
            case 146 -> check(clientPlaying(mc, MurimAnimations.DRIFTING_CUT), "the attack key plays the stance's first strike, Drifting Cut");
            case 158 -> server.execute(() -> {
                check(zombies.getFirst().getHealth() < 200, "Drifting Cut lands");
                check(QiService.getQi(self(server)) >= 50.9, "a strike that lands gives back Qi (" + QiService.getQi(self(server)) + ")");
                zombies.getFirst().setHealth(200);
            });
            // Key 2: Thousand Drizzle, close: the flurry.
            case 164 -> KeyMapping.click(mc.options.keyHotbarSlots[1].getKey());
            case 168 -> check(clientPlaying(mc, MurimAnimations.THOUSAND_DRIZZLE), "key 2 plays Thousand Drizzle's flurry");
            case 200 -> server.execute(() -> {
                check(200 - zombies.getFirst().getHealth() > 6, "the flurry strikes again and again (" + (200 - zombies.getFirst().getHealth()) + " damage)");
                zombies.getFirst().setHealth(200);
                zombies.getFirst().moveTo(self(server).getX(), self(server).getY(), self(server).getZ() + 7.0, 180, 0);
                MartialService.resetCooldowns(self(server));
            });
            // Key 2 again, the zombie 5 blocks away: the wave.
            case 206 -> KeyMapping.click(mc.options.keyHotbarSlots[1].getKey());
            case 210 -> check(clientPlaying(mc, MurimAnimations.DRIZZLE_WAVE), "at mid-distance, Thousand Drizzle sends its wave");
            case 228 -> server.execute(() -> {
                check(zombies.getFirst().getHealth() < 200, "the wave reaches the zombie 7 blocks away");
                zombies.getFirst().setHealth(200);
                zombies.getFirst().moveTo(self(server).getX(), self(server).getY(), self(server).getZ() + 1.8, 180, 0);
                QiService.setQi(self(server), 50);
            });
            // Key 3: Heaven-Splitting Crescent, both in the air.
            case 234 -> {
                startY = mc.player.getY();
                KeyMapping.click(mc.options.keyHotbarSlots[2].getKey());
            }
            case 262 -> {
                check(peakY - startY > 2.6 && peakY - startY < 3.4, String.format("the crescent takes the player 3 blocks up (%.2f blocks)", peakY - startY));
                check(zombiePeakY - startY > 2.3, String.format("the crescent carries the zombie up with the player (%.2f blocks)", zombiePeakY - startY));
                server.execute(() -> check(zombies.getFirst().getHealth() < 200, "the crescent cuts the zombie"));
            }
            // Hanging at the top: an M1 in the air, then key 3 again, the slam that throws the zombie back down.
            case 254 -> KeyMapping.click(mc.options.keyAttack.getKey());
            case 260 -> {
                server.execute(() -> slamHealth = zombies.getFirst().getHealth());
                KeyMapping.click(mc.options.keyHotbarSlots[2].getKey());
            }
            case 264 -> check(clientPlaying(mc, MurimAnimations.CRESCENT_SLAM), "key 3 again at the top plays the slam (playing: " + clientAnimation(mc) + ")");
            case 276 -> server.execute(() -> {
                Zombie z = zombies.getFirst();
                check(z.getY() - startY < 0.4, String.format("the slam brings the zombie back to the ground (%.2f blocks up)", z.getY() - startY));
                check(z.getHealth() < slamHealth - 1, "the slam and its landing hurt the zombie (" + slamHealth + " then " + z.getHealth() + ")");
            });
            // The Techniques tab: the passive and its M1 slots, then Thousand Drizzle.
            case 280 -> {
                mc.setCameraEntity(mc.player);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                mc.setScreen(MurimProfileScreen.techniques());
            }
            case 284 -> ((MurimProfileScreen) mc.screen).selectForTests(MartialArts.FLOWING_CLOUD_STANCE.id());
            case 290 -> capture(mc, "09-passive-tab.png");
            case 292 -> ((MurimProfileScreen) mc.screen).selectForTests(MartialArts.THOUSAND_DRIZZLE.id());
            case 298 -> capture(mc, "10-thousand-drizzle-tab.png");
            // The Profile page: the passive in use.
            case 300 -> mc.setScreen(new MurimProfileScreen());
            case 306 -> capture(mc, "11-profile-passive.png");
            // The whole M1 chain with four real clicks: the four default strikes in order, each landing.
            case 310 -> {
                mc.setScreen(null);
                server.execute(() -> {
                    ServerPlayer self = self(server);
                    Zombie z = zombies.getFirst();
                    z.moveTo(self.getX(), self.getY(), self.getZ() + 1.8, 180, 0);
                    z.setHealth(200);
                    z.setDeltaMovement(Vec3.ZERO);
                });
            }
            case 316 -> placeTopCamera(mc);
            case 320, 334, 348, 362 -> {
                server.execute(() -> chainHealth = zombies.getFirst().getHealth());
                KeyMapping.click(mc.options.keyAttack.getKey());
            }
            case 325, 339, 353, 367 -> {
                int strike = (ticks - 325) / 14;
                var expected = List.of(MurimAnimations.DRIFTING_CUT, MurimAnimations.RETURNING_CUT, MurimAnimations.PIERCING_MIST, MurimAnimations.FALLING_RAIN).get(strike);
                check(clientPlaying(mc, expected), "M1 " + (strike + 1) + " plays " + List.of("Drifting Cut", "Returning Cut", "Piercing Mist", "Falling Rain").get(strike)
                        + " (playing: " + clientAnimation(mc) + ")");
            }
            case 332, 346, 360, 374 -> {
                int strike = (ticks - 332) / 14 + 1;
                server.execute(() -> check(zombies.getFirst().getHealth() < chainHealth, "M1 " + strike + " lands on the zombie in front"));
            }
            // Last, Cloud Step Slash as its player sees it: first person, two zombies on the way.
            case 380 -> {
                mc.setCameraEntity(mc.player);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                mc.setScreen(null);
                mc.player.setXRot(8);
                server.execute(() -> {
                    ServerPlayer self = self(server);
                    zombies.forEach(Zombie::discard);
                    zombies.clear();
                    MartialService.resetCooldowns(self);
                    QiService.setQi(self, 100);
                    Vec3 ahead = Vec3.directionFromRotation(0, self.getYRot());
                    for (double d : new double[]{2.2, 4.0}) {
                        Zombie z = EntityType.ZOMBIE.create(server.overworld());
                        z.moveTo(self.getX() + ahead.x * d, self.getY(), self.getZ() + ahead.z * d, self.getYRot() + 180, 0);
                        z.setNoAi(true);
                        z.setPersistenceRequired();
                        z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                        server.overworld().addFreshEntity(z);
                        zombies.add(z);
                    }
                });
            }
            case 388 -> {
                mc.options.keyHotbarSlots[0].setDown(true);
                KeyMapping.click(mc.options.keyHotbarSlots[0].getKey());
            }
            case 389, 390, 391, 392, 393, 394, 395, 396, 397 -> mc.options.keyHotbarSlots[0].setDown(true);
            case 398 -> mc.options.keyHotbarSlots[0].setDown(false);
            case 430 -> {
                server.execute(() -> zombies.forEach(Zombie::discard));
                finish(mc);
            }
            default -> { }
        }
        record(mc, "05-drifting-cut", 140, 156, 1);
        record(mc, "06-thousand-drizzle", 164, 204, 1);
        record(mc, "07-drizzle-wave", 206, 226, 1);
        record(mc, "08-crescent", 234, 278, 1);
        record(mc, "12-m1-chain", 318, 378, 1);
        record(mc, "13-first-person", 384, 426, 1);
        if (ticks >= 234 && ticks <= 262) {
            peakY = Math.max(peakY, mc.player.getY());
            // The zombie as this client sees it.
            var seen = zombies.isEmpty() ? null : mc.level.getEntity(zombies.getFirst().getId());
            if (seen != null) zombiePeakY = Math.max(zombiePeakY, seen.getY());
        }
    }

    private static double startY, peakY = Double.NEGATIVE_INFINITY, zombiePeakY = Double.NEGATIVE_INFINITY;
    private static volatile float chainHealth;
    private static volatile float slamHealth;

    /** A camera right above the player, looking down at the ground in front of them: a blade lying level shows wide. */
    private static void placeTopCamera(Minecraft mc) {
        Vec3 at = mc.player.position();
        Vec3 wanted = at.add(0.01, 4.2, 0.9);
        Vec3 d = at.add(0, 1.0, 1.0).subtract(wanted);
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

    /** The side camera again, beside where the player stands now. */
    private static void placeSideCamera(Minecraft mc) {
        Vec3 at = mc.player.position();
        Vec3 wanted = at.add(6.5, 1.8, 1.5);
        Vec3 d = at.add(0, 0.9, 1.5).subtract(wanted);
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

    /** What this client's own player is playing, by its registry name (for a report). */
    private static String clientAnimation(Minecraft mc) {
        var patch = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mc.player, yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch.class);
        var main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        return main == null || main.getRealAnimation() == null ? "nothing" : String.valueOf(main.getRealAnimation().get());
    }

    /** Whether this client's own player is playing an animation (Epic Fight's client animator). */
    private static boolean clientPlaying(Minecraft mc, yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<?> animation) {
        var patch = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mc.player, yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch.class);
        if (patch == null) return false;
        var main = patch.getAnimator().getPlayerFor(null);
        return main != null && main.getRealAnimation() != null && main.getRealAnimation().get() == animation.get();
    }

    /** No first-person hand while the side camera films. */
    @SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        if (Boolean.getBoolean("murimblock.martialTour") && camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
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
        // Written off the render thread: a slow write would hold the game back and the frames would bunch up.
        var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        net.minecraft.Util.ioPool().execute(() -> {
            try (image) { image.writeToFile(file); } catch (Exception e) { LOGGER.error("Frame write failed", e); }
        });
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Martial tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("martial-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the martial tour report", e);
        }
        LOGGER.info("Martial tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
