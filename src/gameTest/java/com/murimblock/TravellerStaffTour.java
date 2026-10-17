package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.client.gui.MurimProfileScreen;
import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimAnimations.Motions;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.TravellerStaff;
import com.murimblock.martial.TravellerStaffItems;
import com.murimblock.client.StaffClient;
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
 * Development-only real-client check of the Traveller's Staff Art (gradlew -PgameTests runTravellerStaffTour): the scroll
 * and the staff in the hotbar, reading the scroll, the Techniques tab (library, passive, each technique with its icon), the
 * combat bar with the three icons, then filmed from the side: the Vertical Strike and the Flurry of Points on keys 1 and 2,
 * the Spinning Rampart held on key 3 (a blow from the front stopped, one from behind not), its cooldown from its end.
 * Report in traveller-staff-tour-report.txt, pictures in build/traveller-staff-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class TravellerStaffTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static Marker camera;
    private static Vec3 start;
    private static final List<Zombie> zombies = new ArrayList<>();
    private static final List<String> report = new ArrayList<>();
    private static volatile float health;

    private TravellerStaffTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.travellerStaffTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-traveller-staff", GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-traveller-staff", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Traveller's Staff tour crashed", e);
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
        if (camera != null && ticks >= 74 && ticks < 214 && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
        // The rampart's key held (read again every tick: Minecraft reads the real keyboard back otherwise).
        if (ticks >= 150 && ticks < 196) mc.options.keyHotbarSlots[2].setDown(true);
        switch (ticks) {
            case 20 -> {
                mc.options.hideGui = false;
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    ServerPlayer self = self(server);
                    self.getInventory().selected = 0;
                    self.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
                    self.getInventory().setItem(0, new ItemStack(TravellerStaffItems.SCROLL.get()));
                    self.getInventory().setItem(1, new ItemStack(MurimItems.TRAVELLER_STAFF.get()));
                    self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 0);
                });
            }
            // The scroll and the staff in the hotbar, the scroll in hand.
            case 28 -> capture(mc, "00-scroll-in-hand.png");
            case 30 -> server.execute(() -> {
                ServerPlayer self = self(server);
                ItemStack scroll = self.getMainHandItem();
                scroll.use(server.overworld(), self, InteractionHand.MAIN_HAND);
                var data = MartialService.data(self);
                check(data.hasTechnique(MartialArts.VERTICAL_STRIKE.id()) && data.hasTechnique(MartialArts.POINT_FLURRY.id())
                        && data.hasTechnique(MartialArts.SPINNING_RAMPART.id()), "reading the scroll teaches the three techniques");
                QiService.setQiMax(self, 100);
                QiService.setQi(self, 100);
            });
            case 34 -> mc.setScreen(MurimProfileScreen.techniques());
            case 38 -> capture(mc, "01-library.png");
            case 39 -> ((MurimProfileScreen) mc.screen).selectForTests(MartialArts.THIRD_RESONANCE.id());
            case 44 -> capture(mc, "02-third-resonance.png");
            case 45 -> ((MurimProfileScreen) mc.screen).selectForTests(MartialArts.VERTICAL_STRIKE.id());
            case 50 -> capture(mc, "03-vertical-strike.png");
            case 51 -> ((MurimProfileScreen) mc.screen).selectForTests(MartialArts.POINT_FLURRY.id());
            case 56 -> capture(mc, "04-point-flurry.png");
            case 57 -> ((MurimProfileScreen) mc.screen).selectForTests(MartialArts.SPINNING_RAMPART.id());
            case 62 -> capture(mc, "05-spinning-rampart.png");
            case 64 -> {
                mc.setScreen(null);
                server.execute(() -> {
                    ServerPlayer self = self(server);
                    self.getInventory().selected = 1;
                    self.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(1));
                    CombatService.setCombatMode(self, true);
                });
            }
            // In combat mode: the combat bar in the hotbar's place, the three icons on keys 1 to 3.
            case 72 -> {
                check(TravellerStaff.holdsStaff(mc.player), "the staff is in hand");
                capture(mc, "06-combat-bar.png");
            }
            case 74 -> {
                yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
                yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
                start = mc.player.position();
                server.execute(() -> reset(server, 0, 2.0));
                placeSideCamera(mc);
            }
            // Key 1: the Vertical Strike, a zombie 2 blocks ahead.
            case 80 -> press(mc, 0);
            case 82 -> mc.options.keyHotbarSlots[0].setDown(false);
            case 85 -> check(clientPlaying(mc, MurimAnimations.STAFF_VERTICAL_STRIKE), "key 1 plays the Vertical Strike (playing: " + clientAnimation(mc) + ")");
            case 104 -> server.execute(() -> check(zombies.getFirst().getHealth() < health, "the strike lands"));
            // Key 2: the Flurry of Points.
            case 110 -> server.execute(() -> reset(server, 0, 2.4));
            case 114 -> press(mc, 1);
            case 116 -> mc.options.keyHotbarSlots[1].setDown(false);
            case 119 -> check(clientPlaying(mc, MurimAnimations.STAFF_POINT_FLURRY), "key 2 plays the Flurry of Points (playing: " + clientAnimation(mc) + ")");
            case 140 -> server.execute(() -> check(health - zombies.getFirst().getHealth() > 1.0, "the thrusts strike again and again (" + (health - zombies.getFirst().getHealth()) + ")"));
            // Key 3 held: the Spinning Rampart, a zombie in front and one behind.
            case 144 -> server.execute(() -> {
                reset(server, 0, 1.6);
                zombie(server, 0, -1.6);
            });
            case 150 -> KeyMapping.click(mc.options.keyHotbarSlots[2].getKey());
            case 164 -> {
                PlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(mc.player, PlayerPatch.class);
                check(StaffClient.SPINNING.contains(mc.player.getId()) && patch != null && patch.currentLivingMotion == Motions.STAFF_RAMPART,
                        "holding key 3 spins the staff (motion " + (patch == null ? null : patch.currentLivingMotion) + ")");
                server.execute(() -> {
                    ServerPlayer self = self(server);
                    float before = self.getHealth();
                    self.hurt(self.damageSources().mobAttack(zombies.get(0)), 4);
                    check(self.getHealth() == before, "a blow from the front is stopped");
                });
            }
            case 172 -> server.execute(() -> {
                ServerPlayer self = self(server);
                float before = self.getHealth();
                self.hurt(self.damageSources().mobAttack(zombies.get(1)), 4);
                check(self.getHealth() < before, "a blow from behind goes through");
                self.setHealth(self.getMaxHealth());
            });
            case 196 -> mc.options.keyHotbarSlots[2].setDown(false);
            case 204 -> server.execute(() -> {
                ServerPlayer self = self(server);
                check(!TravellerStaff.spinning(self), "letting go stops the rampart");
                check(MartialService.cooldownLeft(self, MartialArts.SPINNING_RAMPART.id()) > 200, "its cooldown runs from its end");
            });
            case 214 -> {
                mc.setCameraEntity(mc.player);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                capture(mc, "10-cooldowns.png");
            }
            case 218 -> {
                server.execute(() -> zombies.forEach(Zombie::discard));
                finish(mc);
            }
            default -> { }
        }
        record(mc, "07-vertical-strike", 78, 104, 1);
        record(mc, "08-point-flurry", 112, 140, 1);
        record(mc, "09-spinning-rampart", 148, 210, 1);
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
        if (Boolean.getBoolean("murimblock.travellerStaffTour") && camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
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
        LOGGER.info("Traveller's Staff tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("traveller-staff-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Traveller's Staff tour report", e);
        }
        LOGGER.info("Traveller's Staff tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
