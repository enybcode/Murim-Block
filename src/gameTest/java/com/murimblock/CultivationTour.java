package com.murimblock;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.murimblock.client.MurimblockKeyMappings;
import com.murimblock.client.cultivation.QiSenseClient;
import com.murimblock.client.gui.MurimProfileScreen;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationRegistries;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.cultivation.inner.HeartDemonEntity;
import com.murimblock.cultivation.inner.InnerMissions;
import com.murimblock.cultivation.inner.InnerWorldService;
import com.murimblock.cultivation.inner.MeditatingBodyEntity;
import com.murimblock.cultivation.inner.QiMoteEntity;
import com.murimblock.cultivation.inner.RemnantSoulEntity;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.MurimEntities;
import com.murimblock.qi.QiService;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;

/**
 * Development-only real-client tour of cultivation (gradlew -PgameTests runCultivationTour), filmed for its sheet: the
 * real meditation key (sitting in the lotus, the aura filling, the flames), a small breakthrough, the aura's colours
 * (three meditating players: no art, the Azure Cloud Sword, the Long Fist), the Cultivation tab, the inner world opened at
 * the gate, the Heart Demon, its defeat and the great breakthrough, a deviation, a body left seated, Qi Sense on the real
 * key (five auras), the remnant soul and the Qi storm. Report in cultivation-tour-report.txt, pictures in
 * build/cultivation-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CultivationTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static Marker camera;
    private static Vec3 start;
    private static final List<ServerPlayer> dummies = new ArrayList<>();
    private static final List<String> report = new ArrayList<>();
    private static ServerPlayer shen, lan, hu;

    private CultivationTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.cultivationTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-cultivation", GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-cultivation", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            MinecraftServer server = mc.getSingleplayerServer();
            server.execute(() -> dummies.forEach(d -> { if (!d.isRemoved()) d.doTick(); }));
            step(mc, server);
        } catch (Exception e) {
            check(false, "tour crashed: " + e);
            LOGGER.error("Cultivation tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (camera != null && mc.getCameraEntity() == camera && mc.getConnection() != null) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
        }
        if (camera != null && mc.getCameraEntity() != camera && camera.level() == mc.level) mc.setCameraEntity(camera);
        // The meditation key: a tap (down two ticks, then up).
        if (ticks >= 30 && ticks <= 32) MurimblockKeyMappings.MEDITATE.setDown(ticks < 32);
        // Qi Sense on its real key, held.
        if (ticks >= 580 && ticks < 650) MurimblockKeyMappings.QI_SENSE.setDown(true);
        if (ticks == 650) MurimblockKeyMappings.QI_SENSE.setDown(false);
        // In the Heart Demon's fight the tour's player only stands: kept alive.
        if (ticks >= 262 && ticks < 342) server.execute(() -> self(server).setHealth(self(server).getMaxHealth()));
        switch (ticks) {
            case 20 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(13000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer self = self(server);
                self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 0);
                new ItemStack(MurimItems.LONG_FIST_SCROLL.get()).use(level, self, InteractionHand.MAIN_HAND);
                self.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                QiService.setQi(self, 40);
                // Three cultivators meditating side by side: no art, the Azure Cloud Sword, the Long Fist.
                shen = dummy(server, level, "Shen", -3.5, 6);
                lan = dummy(server, level, "Lan", 0, 6);
                hu = dummy(server, level, "Hu", 3.5, 6);
                new ItemStack(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get()).use(level, lan, InteractionHand.MAIN_HAND);
                new ItemStack(MurimItems.LONG_FIST_SCROLL.get()).use(level, hu, InteractionHand.MAIN_HAND);
                for (ServerPlayer d : List.of(shen, lan, hu)) {
                    d.setOnGround(true);
                    MeditationService.start(d);
                    QiService.refillQi(d);
                }
            });
            case 26 -> {
                start = mc.player.position();
                yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
                yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
                film(mc, start.add(2.6, 1.3, 2.4), start.add(0, 0.6, 0));
            }
            case 40 -> check(MeditationService.isMeditating(mc.player), "the meditation key sits the player down (the client sees it)");
            case 60 -> check(MeditationService.aura(mc.player).fill() < 0.6F, "the aura starts small, the reserve low (" + MeditationService.aura(mc.player).fill() + ")");
            case 90 -> server.execute(() -> QiService.refillQi(self(server)));
            case 128 -> check(MeditationService.aura(mc.player).fill() >= 0.98F, "full, the aura burns");
            case 135 -> server.execute(() -> CultivationService.gainQiMax(self(server), 1, CultivationService.Source.COMMAND));
            case 140 -> server.execute(() -> check(CultivationService.getStage(self(server)) == CultivationStage.MIDDLE, "a small breakthrough to Middle Qi Sensing"));
            case 175 -> film(mc, start.add(0, 2.4, 1.6), start.add(0, 0.6, 6));
            case 200 -> check(MeditationService.aura(mc.level.getPlayerByUUID(lan.getUUID())).mainArt().isPresent(), "the client knows the others' styles (their aura's colour)");
            case 220 -> mc.setScreen(MurimProfileScreen.cultivation());
            case 226 -> capture(mc, "05-cultivation-tab.png");
            case 228 -> mc.setScreen(null);
            case 230 -> server.execute(() -> {
                ServerPlayer self = self(server);
                CultivationService.gainQiMax(self, 500, CultivationService.Source.COMMAND);
                QiService.refillQi(self);
            });
            case 234 -> mc.setScreen(MurimProfileScreen.cultivation());
            case 240 -> capture(mc, "06-cultivation-gate.png");
            case 242 -> {
                mc.setScreen(null);
                release(mc);
            }
            case 246 -> server.execute(() -> check(InnerWorldService.enter(self(server)), "at the gate, the inner world opens (the Heart Demon waits)"));
            case 264 -> {
                check(InnerWorldService.isInside(mc.player), "the client is in the inner world");
                Vec3 p = mc.player.position();
                film(mc, p.add(5, 2.6, 2.5), p.add(0, 1.0, -2.5));
            }
            case 300 -> server.execute(() -> check(!demons(server).isEmpty(), "the Heart Demon stands on the island"));
            case 290, 304, 318 -> KeyMapping.click(mc.options.keyAttack.getKey());
            case 342 -> server.execute(() -> demons(server).forEach(d -> d.hurt(self(server).damageSources().playerAttack(self(server)), 1000)));
            case 378 -> release(mc);
            case 392 -> {
                check(!InnerWorldService.isInside(mc.player), "back in the body after the victory");
                server.execute(() -> check(CultivationService.getRealm(self(server)) == CultivationRealm.QI_GUIDING, "the great breakthrough: Qi Guiding"));
            }
            case 470 -> server.execute(() -> check(self(server).getMaxHealth() == 24, "Qi Guiding gives two hearts"));
            case 473 -> film(mc, start.add(2.6, 1.3, 2.4), start.add(0, 0.6, 0));
            case 476 -> server.execute(() -> {
                ServerPlayer self = self(server);
                QiService.refillQi(self);
                CultivationService.deviation(self);
            });
            case 520 -> server.execute(() -> {
                check(self(server).hasEffect(CultivationRegistries.QI_DEVIATION), "a deviation of the Qi");
                ServerPlayer s = shen;
                MeditationService.start(s);
                QiService.refillQi(s);
                check(InnerWorldService.enter(s), "Shen opens their inner world: their body stays seated");
            });
            case 524 -> film(mc, shenSeat().add(2.0, 1.3, 2.0), shenSeat().add(0, 0.5, 0));
            case 540 -> check(!mc.level.getEntitiesOfClass(MeditatingBodyEntity.class, mc.player.getBoundingBox().inflate(20)).isEmpty(), "the client sees the body left seated");
            case 570 -> {
                release(mc);
                server.execute(() -> {
                    ServerPlayer self = self(server);
                    self.removeEffect(CultivationRegistries.QI_DEVIATION);
                    MeditationService.stop(self, false);
                    QiService.refillQi(self);
                    CultivationService.setCultivation(self, CultivationRealm.QI_CONDENSATION, CultivationStage.MIDDLE);
                    CultivationService.setCultivation(lan, CultivationRealm.QI_SENSING, CultivationStage.LATE);
                    CultivationService.setCultivation(hu, CultivationRealm.QI_CONDENSATION, CultivationStage.PEAK);
                    ServerLevel level = server.overworld();
                    ServerPlayer mo = dummy(server, level, "Mo", -2, 4);
                    CultivationService.setCultivation(mo, CultivationRealm.QI_LIQUIDATION, CultivationStage.EARLY);
                    ServerPlayer xue = dummy(server, level, "Xue", 2, 4);
                    CultivationService.setCultivation(xue, CultivationRealm.CORE_FORMATION, CultivationStage.EARLY);
                    var bandit = MurimEntities.MOUNTAIN_BANDIT.get().create(level);
                    bandit.moveTo(start.x + 5.5, start.y, start.z + 5, 180, 0);
                    bandit.setNoAi(true);
                    bandit.setPersistenceRequired();
                    level.addFreshEntity(bandit);
                    self.teleportTo(level, start.x, start.y, start.z, 0, 12);
                });
            }
            case 600 -> check(QiSenseClient.open(), "Qi Sense opens on its key at Qi Condensation");
            case 664 -> server.execute(() -> {
                ServerPlayer self = self(server);
                self.setOnGround(true);
                if (!MeditationService.isMeditating(self) && !MeditationService.start(self)) MeditationService.resume(self);
                QiService.refillQi(self);
                check(InnerWorldService.enter(self), "a visit to the inner world (no gate)");
            });
            case 680 -> {
                Vec3 p = mc.player.position();
                var soul = mc.level.getEntitiesOfClass(RemnantSoulEntity.class, mc.player.getBoundingBox().inflate(40));
                check(!soul.isEmpty(), "the remnant soul is on the island");
                Vec3 at = soul.isEmpty() ? p.add(-3, 0, 2) : soul.get(0).position();
                // From the platform's side, where he looks (at the player), a little higher than his face.
                Vec3 toward = p.subtract(at).multiply(1, 0, 1).normalize();
                film(mc, at.add(toward.scale(2.6)).add(0, 1.9, 0), at.add(0, 1.5, 0));
                capture(mc, "16-remnant-soul.png");
            }
            case 715 -> server.execute(() -> {
                ServerPlayer self = self(server);
                var souls = self.serverLevel().getEntitiesOfClass(RemnantSoulEntity.class, self.getBoundingBox().inflate(40));
                if (!souls.isEmpty()) InnerMissions.talk(self, souls.get(0));
            });
            case 718 -> {
                Vec3 p = mc.player.position();
                film(mc, p.add(0, 10, 10), p.add(0, 0, -1));
            }
            case 730 -> check(mc.level.getEntitiesOfClass(QiMoteEntity.class, mc.player.getBoundingBox().inflate(40)).size() >= 8, "the motes of the Qi storm drift over the island");
            case 760 -> release(mc);
            case 763 -> server.execute(() -> InnerWorldService.leave(self(server), InnerWorldService.Exit.WOKE));
            case 775 -> {
                check(!InnerWorldService.isInside(mc.player), "back from the visit");
                // The extra players leave (Shen first goes back to their body), as real ones would.
                server.execute(() -> {
                    if (InnerWorldService.isInside(shen)) InnerWorldService.leave(shen, InnerWorldService.Exit.WOKE);
                    dummies.forEach(d -> server.getPlayerList().remove(d));
                    dummies.clear();
                });
            }
            case 785 -> finish(mc);
            default -> { }
        }
        record(mc, "01-meditation", 28, 88, 1);
        record(mc, "02-aura-full", 92, 132, 1);
        record(mc, "03-minor-breakthrough", 133, 173, 1);
        record(mc, "04-aura-colours", 177, 217, 1);
        record(mc, "07-enter-inner-world", 244, 262, 1);
        record(mc, "08-heart-demon", 266, 340, 1);
        record(mc, "09-demon-slain", 340, 376, 1);
        record(mc, "10-great-breakthrough", 384, 470, 1);
        record(mc, "11-deviation", 475, 518, 1);
        record(mc, "12-body-left", 526, 566, 1);
        record(mc, "13-qi-sense", 585, 645, 1);
        record(mc, "14-island-soul", 682, 712, 1);
        record(mc, "15-qi-storm", 720, 758, 1);
    }

    /** No first-person hand while the tour's camera films. */
    @SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        if (Boolean.getBoolean("murimblock.cultivationTour") && camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
    }

    private static Vec3 shenSeat() {
        return start.add(-3.5, 0, 6);
    }

    private static List<HeartDemonEntity> demons(MinecraftServer server) {
        ServerPlayer self = self(server);
        return self.serverLevel().getEntitiesOfClass(HeartDemonEntity.class, self.getBoundingBox().inflate(40), d -> self.getUUID().equals(d.owner()));
    }

    /** The view from `eye` looking at `look`, from a marker in the client's current level. */
    private static void film(Minecraft mc, Vec3 eye, Vec3 look) {
        camera = new Marker(EntityType.MARKER, mc.level);
        Vec3 d = look.subtract(eye);
        camera.setPos(eye);
        camera.xo = camera.xOld = eye.x;
        camera.yo = camera.yOld = eye.y;
        camera.zo = camera.zOld = eye.z;
        camera.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        camera.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        camera.yRotO = camera.getYRot();
        camera.xRotO = camera.getXRot();
        mc.setCameraEntity(camera);
    }

    private static void release(Minecraft mc) {
        camera = null;
        if (mc.player != null) mc.setCameraEntity(mc.player);
    }

    private static ServerPlayer dummy(MinecraftServer server, ServerLevel level, String name, double dx, double dz) {
        var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), name), 0,
                ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        for (int i = 0; i <= 60; i++) player.tick();
        ServerPlayer self = self(server);
        player.teleportTo(level, self.getX() + dx, self.getY(), self.getZ() + dz, 180, 0);
        player.setOnGround(true);
        dummies.add(player);
        return player;
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
        LOGGER.info("Cultivation tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("cultivation-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the cultivation tour report", e);
        }
        LOGGER.info("Cultivation tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
