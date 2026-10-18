package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.martial.KaguneState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Development-only real-client look at the Devouring Demon Art's kagune on a player (KaguneState, client/KaguneOverlay):
 * in a new flat world, the camera's own player in front view, then from behind, with the four tentacles, through the
 * transformation, then with the two centipedes and the mask. Run with the weapon tour's client:
 * MURIM_KAGUNE=1 gradlew -PgameTests runWeaponTour. Shots go to build/weapon-tour-run/screenshots/kagune.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class KaguneTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static final List<String> report = new ArrayList<>();

    private KaguneTour() { }

    public static boolean wanted() {
        return "1".equals(System.getenv("MURIM_KAGUNE"));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.weaponTour") || !wanted()) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    String name = "murim-kagune-" + System.currentTimeMillis();
                    LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(7L, false, false),
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
            LOGGER.error("Kagune tour crashed", e);
            finish(mc);
        }
    }

    private static void set(MinecraftServer server, KaguneState.Form form) {
        server.execute(() -> {
            ServerPlayer self = server.getPlayerList().getPlayers().get(0);
            KaguneState.set(self, form);
        });
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        // MURIM_KAGUNE_FILM=1: a film instead, a frame every other tick, from the front: the tentacles, the
        // transformation at 100, then the centipedes.
        // MURIM_KAGUNE_FILM=beast: the demon form on all fours, from the front then from behind: still, walking, running.
        // MURIM_KAGUNE_FILM=art: the art in play, the HUD shown: the Fury ring at 0, 25, 50 and 75, Lash on three zombies,
        // the Fury full and the transformation, then Centipede Bite.
        if ("art".equals(System.getenv("MURIM_KAGUNE_FILM"))) {
            art(mc, server);
            return;
        }
        if ("beast".equals(System.getenv("MURIM_KAGUNE_FILM")) && ticks > 20) {
            var up = mc.options.keyUp.getKey();
            var sprint = mc.options.keySprint.getKey();
            if (ticks == 40) { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); set(server, KaguneState.Form.DEMON); }
            if (ticks == 100) net.minecraft.client.KeyMapping.set(up, true);
            if (ticks == 160) net.minecraft.client.KeyMapping.set(sprint, true);
            if (ticks == 200) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            if (ticks == 240) { net.minecraft.client.KeyMapping.set(sprint, false); net.minecraft.client.KeyMapping.set(up, false); }
            // Then it turns on the spot, left then right: the kagune swings like a tail.
            if (ticks > 244 && ticks <= 300) { mc.player.setYRot(mc.player.getYRot() + (ticks <= 272 ? 9 : -9)); mc.player.setYBodyRot(mc.player.getYRot()); }
            if (ticks >= 60 && ticks <= 320 && ticks % 2 == 0) capture(mc, String.format("beast/%03d.png", (ticks - 60) / 2));
            if (ticks == 150) check(com.murimblock.client.DemonBeastClient.motion(mc.player) == com.murimblock.integration.epicfight.MurimAnimations.Motions.DEMON_BEAST_WALK,
                    "walking in the demon form is the beast's walk: " + com.murimblock.client.DemonBeastClient.motion(mc.player));
            if (ticks == 90) {
                check(Math.abs(mc.player.getBbHeight() - KaguneState.BEAST_SIZE.height()) < 0.01F, "on all fours the hitbox is the beast's: " + mc.player.getBbHeight() + " high");
                var speed = server.getPlayerList().getPlayers().get(0).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                check(speed != null && speed.getValue() > 0.1 * 1.3, "on all fours he is faster than standing: speed " + (speed == null ? "?" : speed.getValue()));
            }
            if (ticks == 322) finish(mc);
            return;
        }
        if ("1".equals(System.getenv("MURIM_KAGUNE_FILM")) && ticks > 20) {
            if (ticks == 40) { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); set(server, KaguneState.Form.HUNGER); }
            if (ticks == 108) set(server, KaguneState.Form.TRANSFORM);
            if (ticks >= 60 && ticks <= 260 && ticks % 2 == 0) capture(mc, String.format("film/%03d.png", (ticks - 60) / 2));
            if (ticks == 262) { check(true, "filmed"); finish(mc); }
            return;
        }
        switch (ticks) {
            case 20 -> {
                mc.options.hideGui = true;
                server.execute(() -> {
                    var level = server.overworld();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    ServerPlayer self = server.getPlayerList().getPlayers().get(0);
                    self.getAbilities().flying = false;
                    self.onUpdateAbilities();
                    self.teleportTo(level, 0.5, -60, 0.5, 0, 0);
                });
            }
            case 40 -> { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); set(server, KaguneState.Form.HUNGER); }
            case 70 -> capture(mc, "01-hunger-front-a.png");
            case 80 -> capture(mc, "02-hunger-front-b.png");
            case 82 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 100 -> capture(mc, "03-hunger-back.png");
            case 102 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 110 -> set(server, KaguneState.Form.TRANSFORM);
            case 116, 122, 128, 134, 140, 146, 152, 158, 164 -> capture(mc, String.format("04-transform-%03d.png", ticks - 110));
            case 190 -> capture(mc, "05-demon-front.png");
            case 192 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 210 -> capture(mc, "06-demon-back.png");
            case 212 -> {
                check(KaguneState.of(mc.player).form() == KaguneState.Form.TRANSFORM, "the client sees the kagune's form: " + KaguneState.of(mc.player).form());
                finish(mc);
            }
            default -> { }
        }
    }

    private static net.minecraft.world.entity.Marker camera;
    private static final List<java.util.UUID> zombies = new ArrayList<>();
    private static final List<String> casts = new ArrayList<>();

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void art(Minecraft mc, MinecraftServer server) throws Exception {
        if (ticks == 20) server.execute(() -> {
            var level = server.overworld();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            // Not peaceful: the zombies would vanish.
            server.setDifficulty(Difficulty.NORMAL, true);
            ServerPlayer player = self(server);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.setGameMode(GameType.SURVIVAL);
            com.murimblock.martial.MartialService.learn(player, com.murimblock.martial.MartialArts.art(com.murimblock.martial.MartialArts.DEVOURING_DEMON));
            com.murimblock.martial.MartialService.unlock(player, com.murimblock.martial.MartialArts.LASH);
            com.murimblock.martial.MartialService.assign(player, 0, com.murimblock.martial.MartialArts.LASH.id());
            com.murimblock.martial.MartialService.unlock(player, com.murimblock.martial.MartialArts.IMPALING_THORNS);
            com.murimblock.martial.MartialService.assign(player, 1, com.murimblock.martial.MartialArts.IMPALING_THORNS.id());
            // The art's own realm, its Qi Max halfway to the next stage: no breakthrough (and its ring) while filming.
            com.murimblock.cultivation.CultivationService.setCultivation(player, com.murimblock.cultivation.CultivationRealm.FOUNDATION_ESTABLISHMENT,
                    com.murimblock.cultivation.CultivationStage.MIDDLE);
            var cultivation = com.murimblock.cultivation.CultivationService.getCultivation(player);
            double floor = com.murimblock.cultivation.CultivationRules.floorOf(cultivation);
            double next = com.murimblock.cultivation.CultivationProgression.getRequiredQiMaxForNext(cultivation).orElse(floor * 2);
            com.murimblock.qi.QiService.setQiMax(player, (floor + next) / 2);
            com.murimblock.qi.QiService.setQi(player, (floor + next) / 2);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            com.murimblock.combat.CombatService.setCombatMode(player, true);
            player.teleportTo(level, 0.5, -60, 0.5, 0, 0);
            for (double[] at : new double[][]{{0.5, 3.6}, {-2.4, 2.4}, {3.4, 2.4}}) {
                var zombie = EntityType.ZOMBIE.create(level);
                zombie.moveTo(at[0], -60, at[1], 180, 0);
                zombie.setYHeadRot(180);
                zombie.yBodyRot = 180;
                zombie.setNoAi(true);
                zombie.setPersistenceRequired();
                zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
                zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
                zombie.setHealth(400);
                level.addFreshEntity(zombie);
                zombies.add(zombie.getUUID());
            }
        });
        if (ticks == 40) {
            camera = new net.minecraft.world.entity.Marker(EntityType.MARKER, mc.level);
            // From his right side: the zombies to the south on the right of the picture.
            // MURIM_KAGUNE_CAM=top: from above, to see that nothing goes through him.
            if ("top".equals(System.getenv("MURIM_KAGUNE_CAM"))) place(0.5, -53.5, -1.5, 0, 62);
            else place(-6.0, -58.0, 1.6, -90, 12);
            mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
            mc.gui.getChat().clearMessages(false);
            mc.options.fov().set(60);
        }
        if (camera != null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
        // He faces the zombies (south): the client turns its own player, not the server's teleport.
        if (ticks > 30) { mc.player.setYRot(0); mc.player.yRotO = 0; mc.player.setYHeadRot(0); mc.player.yHeadRotO = 0; mc.player.setYBodyRot(0); mc.player.yBodyRotO = 0; mc.player.setXRot(0); }
        // The Fury ring at a quarter, half and three quarters.
        int[][] steps = {{52, 25}, {62, 50}, {72, 75}, {82, 10}};
        for (int[] s : steps) if (ticks == s[0]) server.execute(() -> com.murimblock.martial.DevouringDemon.setFury(self(server), s[1]));
        if (ticks == 50) capture(mc, "art-01-fury-0.png");
        if (ticks == 60) capture(mc, "art-02-fury-25.png");
        if (ticks == 70) capture(mc, "art-03-fury-50.png");
        if (ticks == 80) capture(mc, "art-04-fury-75.png");
        // Slowed to 5 ticks a second while filming: every tick gets its own frame. Lash (90), Impaling Thorns (118), the
        // Fury full (150) and the transformation, Centipede Bite (220), Burrow (250).
        if (ticks == 84) server.execute(() -> server.tickRateManager().setTickRate(5));
        if (ticks == 280) server.execute(() -> server.tickRateManager().setTickRate(20));
        if (ticks == 90) cast(server, 0);
        if (ticks == 117 || ticks == 249) server.execute(() -> healthBefore = health(server));
        if (ticks == 118) cast(server, 1);
        if (ticks == 150) server.execute(() -> com.murimblock.martial.DevouringDemon.setFury(self(server), KaguneState.MAX_FURY));
        if (ticks == 215) capture(mc, "art-05-demon.png");
        if (ticks == 220) cast(server, 0);
        if (ticks == 250) cast(server, 1);
        if (ticks >= 86 && ticks <= 280) capture(mc, String.format("art/%03d.png", ticks - 86));
        if (ticks == 100) server.execute(() -> {
            float total = 0;
            for (float h : health(server)) total += 400 - h;
            float hurt = total;
            mc.execute(() -> check(hurt > 0, "Lash struck the zombies ahead: " + hurt + " in all"));
        });
        if (ticks == 118 + com.murimblock.martial.DevouringDemon.THORNS_HIT_TICK + 3) server.execute(() -> {
            float[] now = health(server);
            int struck = 0;
            for (int i = 0; i < now.length; i++) if (now[i] < healthBefore[i]) struck++;
            int n = struck;
            mc.execute(() -> check(n == 1, "Impaling Thorns impaled the zombie he looked at, alone: " + n));
        });
        if (ticks == 235) server.execute(() -> {
            boolean ahead = server.overworld().getEntity(zombies.get(0)) instanceof net.minecraft.world.entity.LivingEntity z && z.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            mc.execute(() -> check(ahead, "Centipede Bite bit the zombie ahead"));
        });
        if (ticks == 250 + com.murimblock.martial.DevouringDemon.BURROW_HIT_TICK + 3) server.execute(() -> {
            float[] now = health(server);
            int struck = 0;
            for (int i = 0; i < now.length; i++) if (now[i] < healthBefore[i]) struck++;
            int n = struck;
            mc.execute(() -> check(n == 2, "Burrow came up under the two nearest zombies: " + n));
        });
        if (ticks == 284) {
            check(casts.equals(List.of("CAST", "CAST", "CAST", "CAST")), "the four techniques were cast: " + casts);
            check(com.murimblock.client.KaguneStrikes.jointsSeenInBody() == 0, "the tentacles never went through him: " + com.murimblock.client.KaguneStrikes.jointsSeenInBody() + " joints at worst");
            check(com.murimblock.client.KaguneStrikes.biteJointsSeenInBody() == 0, "the centipedes never went through him: " + com.murimblock.client.KaguneStrikes.biteJointsSeenInBody() + " joints at worst");
            finish(mc);
        }
    }

    private static float[] healthBefore = new float[0];

    /** The zombies' health now, in order (400 for one gone). */
    private static float[] health(MinecraftServer server) {
        float[] out = new float[zombies.size()];
        for (int i = 0; i < out.length; i++)
            out[i] = server.overworld().getEntity(zombies.get(i)) instanceof net.minecraft.world.entity.LivingEntity z ? z.getHealth() : 400;
        return out;
    }

    private static void cast(MinecraftServer server, int slot) {
        server.execute(() -> {
            ServerPlayer player = self(server);
            com.murimblock.martial.MartialService.resetCooldowns(player);
            casts.add(String.valueOf(com.murimblock.martial.MartialService.castSlot(player, slot)));
        });
    }

    private static void place(double x, double y, double z, float yaw, float pitch) {
        camera.setPos(x, y, z);
        camera.xo = camera.xOld = x;
        camera.yo = camera.yOld = y;
        camera.zo = camera.zOld = z;
        camera.setYRot(yaw);
        camera.yRotO = yaw;
        camera.setXRot(pitch);
        camera.xRotO = pitch;
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        if (!ok) failed = true;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve("kagune").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("weapon-tour-report.txt"), String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Kagune tour: could not write its report", e);
        }
        mc.stop();
    }
}
