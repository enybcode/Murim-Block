package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.combat.CombatService;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.qi.QiService;
import com.murimblock.world.raven.RavenHalls;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.entity.projectile.Arrow;
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
import yesman.epicfight.gameasset.Animations;

/**
 * Development-only real-client film of the Raven Messenger Art's techniques (gradlew -PgameTests runRavenSkillsTour): at
 * dusk on a flat world, the iron fan in hand, each technique cast on sturdy zombies and filmed from the side: Raven Gate
 * (the pit, then the fall from the sky), Halls Step, Exchange, Swallowing Gate (arrows swallowed and sent back), Closed
 * Room (the duel's room), The Halls Unfold. Frames in build/raven-skills-tour-run/screenshots/&lt;move&gt;/.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class RavenSkillsTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static Marker camera;
    private static Vec3 start;
    private static final List<Zombie> zombies = new ArrayList<>();
    private static java.util.UUID firstId;
    private static final List<String> report = new ArrayList<>();
    private static final MartialArts.Technique[] BAR = {MartialArts.RAVEN_GATE, MartialArts.HALLS_STEP, MartialArts.EXCHANGE,
            MartialArts.SWALLOWING_GATE, MartialArts.CLOSED_ROOM, MartialArts.HALLS_UNFOLD};

    private RavenSkillsTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.ravenSkillsTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-raven-skills", GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-raven-skills", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Raven skills tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (camera != null && mc.getCameraEntity() == camera && mc.getConnection() != null) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
        }
        switch (ticks) {
            case 20 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(12600);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer self = self(server);
                CultivationService.setCultivation(self, MartialArts.RAVEN_REALM, CultivationStage.EARLY);
                MartialData data = MartialService.data(self).learn(MartialArts.art(MartialArts.RAVEN_ART));
                for (int i = 0; i < BAR.length; i++) data = data.assign(i, BAR[i].id());
                MartialService.setForTests(self, data);
                MartialService.setMainArt(self, MartialArts.RAVEN_ART);
                self.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.IRON_FAN.get()));
                CombatService.setCombatMode(self, true);
                self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 0);
                start = self.position();
            });
            case 36 -> {
                mc.options.hideGui = true;
                yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
                yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
                server.execute(() -> reset(server, 0, 4));
                side(mc, 5, 2.2, 2);
            }
            // Raven Gate: the pit, the fall into it, then (4 s later) the fall out of the sky.
            // The gate's zombie keeps its AI (to fall back out of the sky), held in place.
            case 40 -> server.execute(() -> {
                Zombie z = zombies.getFirst();
                z.setNoAi(false);
                z.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 400, 10, false, false));
            });
            case 44 -> cast(server, 0);
            case 120 -> side(mc, 9, 5, 3);
            case 157 -> server.execute(() -> {
                var back = server.overworld().getEntity(firstId);
                check(back instanceof Zombie z && z.getHealth() < 200, "the Raven Gate's target fell back out of the sky, hurt");
            });
            // Halls Step.
            case 160 -> server.execute(() -> { reset(server, 0, 0); self(server).setXRot(14); });
            case 164 -> side(mc, 6, 2.4, 4);
            case 166 -> cast(server, 1);
            // Exchange.
            case 196 -> server.execute(() -> reset(server, 0, 5));
            case 200 -> side(mc, 6, 2.4, 2.5);
            case 202 -> cast(server, 2);
            case 222 -> server.execute(() -> check(self(server).getZ() > start.z + 3, "Exchange put the player where the target was"));
            // Swallowing Gate: three arrows from a zombie archer's place, swallowed, then sent back.
            case 230 -> server.execute(() -> reset(server, 0, 9));
            case 234 -> side(mc, 6, 2.4, 3);
            case 236 -> cast(server, 3);
            case 246, 254, 262 -> server.execute(() -> arrow(server));
            // Closed Room: the player and the zombie in a room of the halls; filmed from the player's eyes.
            case 330 -> server.execute(() -> reset(server, 0, 3));
            case 334 -> side(mc, 5, 2.2, 1.5);
            case 336 -> cast(server, 4);
            case 346 -> {
                mc.setCameraEntity(mc.player);
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            }
            case 360 -> server.execute(() -> check(RavenHalls.isHalls(self(server).level()) && RavenHalls.dueling(self(server)), "the Closed Room took both into a room"));
            case 365 -> {
                int seen = 0;
                for (var e : mc.level.entitiesForRendering()) if (e instanceof Zombie) seen++;
                int n = seen;
                server.execute(() -> {
                    var foe = RavenHalls.duelFoe(self(server));
                    check(n > 0, "the duel's foe is seen in the room (client sees " + n + ", server foe " + foe + ")");
                });
            }
            case 376 -> server.execute(() -> {
                var foe = RavenHalls.duelFoe(self(server));
                if (foe != null) foe.kill();
            });
            // The Halls Unfold: five zombies round, all taken.
            case 400 -> {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                server.execute(() -> {
                    reset(server, 0, 4);
                    for (double[] p : new double[][]{{-4, 2}, {4, 2}, {-3, -4}, {3, -4}}) zombie(server, p[0], p[1]);
                });
            }
            case 408 -> side(mc, 11, 6, 0);
            case 412 -> cast(server, 5);
            case 470 -> server.execute(() -> check(zombies.stream().allMatch(z -> server.overworld().getEntity(z.getUUID()) == null),
                    "The Halls Unfold took every zombie"));
            case 480 -> finish(mc);
            default -> { }
        }
        record(mc, "1-raven-gate", 44, 76, 2);
        record(mc, "1b-raven-gate-fall", 126, 156, 2);
        record(mc, "2-halls-step", 166, 190, 2);
        record(mc, "3-exchange", 202, 226, 2);
        record(mc, "4-swallowing-gate", 236, 320, 3);
        record(mc, "5-closed-room", 336, 372, 2);
        record(mc, "6-halls-unfold", 412, 466, 2);
    }

    private static void cast(MinecraftServer server, int slot) {
        server.execute(() -> {
            MartialService.CastResult r = MartialService.castSlot(self(server), slot);
            check(r == MartialService.CastResult.CAST, "slot " + slot + " cast (" + r + ")");
        });
    }

    /** A sturdy zombie without AI, `dx` to the side and `dz` in front of the start. */
    private static void zombie(MinecraftServer server, double dx, double dz) {
        Zombie z = EntityType.ZOMBIE.create(server.overworld());
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        z.setHealth(200);
        z.moveTo(start.x + dx, start.y, start.z + dz, dz >= 0 ? 180 : 0, 0);
        z.setNoAi(true);
        z.setPersistenceRequired();
        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        server.overworld().addFreshEntity(z);
        zombies.add(z);
        if (zombies.size() == 1) firstId = z.getUUID();
    }

    /** The player back at the start, facing south, idle, techniques ready, full Qi; one fresh zombie `dz` ahead (none if 0). */
    private static void reset(MinecraftServer server, double dx, double dz) {
        ServerPlayer self = self(server);
        zombies.forEach(z -> { var e = server.overworld().getEntity(z.getUUID()); if (e != null) e.discard(); });
        zombies.clear();
        // Nothing else about: the night's own monsters taken away.
        server.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, self.getBoundingBox().inflate(96)).forEach(net.minecraft.world.entity.Entity::discard);
        if (self.level() != server.overworld()) self.teleportTo(server.overworld(), start.x, start.y, start.z, 0, 0);
        else self.teleportTo(start.x, start.y, start.z);
        self.setYRot(0);
        self.setXRot(0);
        self.setDeltaMovement(Vec3.ZERO);
        var patch = EpicFightBridge.patch(self);
        if (patch != null) patch.playAnimationSynchronized(Animations.BIPED_IDLE, 0.0F);
        MartialService.clearCooldownsForTests(self);
        QiService.setQiMax(self, 2000);
        QiService.setQi(self, 2000);
        if (dz != 0) zombie(server, dx, dz);
    }

    /** An arrow flying at the player from far ahead (as from an archer). */
    private static void arrow(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Zombie archer = zombies.isEmpty() ? null : zombies.getFirst();
        Arrow a = new Arrow(level, archer != null ? archer : self(server), new ItemStack(Items.ARROW), null);
        a.setPos(start.x, start.y + 1.6, start.z + 8);
        a.shoot(0, 0.02, -1, 1.2F, 0);
        level.addFreshEntity(a);
    }

    /** The side camera: `side` blocks to the side, `up` above, looking at a point `ahead` blocks in front of the start. */
    private static void side(Minecraft mc, double side, double up, double ahead) {
        Vec3 target = start.add(0, 1.0, ahead);
        Vec3 eye = start.add(side, up, ahead - 1.5);
        Vec3 d = target.subtract(eye);
        camera = new Marker(EntityType.MARKER, mc.level);
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

    @SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        if (Boolean.getBoolean("murimblock.ravenSkillsTour") && camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
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
        LOGGER.info("Raven skills tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("raven-skills-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Raven skills tour report", e);
        }
        LOGGER.info("Raven skills tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
