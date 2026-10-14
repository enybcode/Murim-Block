package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.AzureSenseiEntity;
import com.murimblock.mob.MurimEntities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Development-only real-client check of Sensei Yun (gradlew -PgameTests runSenseiTour): in a fresh flat world the sensei
 * stands in his idle (filmed from the front, from the side, and close on the Azure Cloud and its tassel), walks six
 * blocks (filmed from the side), then fights a training zombie with the Azure Cloud Sword Art, then in his second phase
 * (his moves are listed in the report); last, the HUD with his boss bar in its clouds. The client's own animator is
 * checked to play his idle and his walk. Frame sequences go to build/sensei-tour-run/screenshots, the report to
 * sensei-tour-report.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SenseiTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static volatile AzureSenseiEntity sensei;
    private static volatile Zombie zombie;
    private static double sx, sy, sz;
    private static boolean sawIdle, sawWalk;
    private static final java.util.Set<String> moves = java.util.concurrent.ConcurrentHashMap.newKeySet(), movesInEarnest = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final List<String> report = new ArrayList<>();

    private SenseiTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.senseiTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        // No Epic Fight target outline around him in the pictures.
        yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
        yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-sensei", GameType.SPECTATOR, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-sensei", settings, new WorldOptions(1L, false, false),
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
            report.add("FAILED tour crashed: " + e);
            failed = true;
            LOGGER.error("Sensei tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        record(mc, "01-idle-front", 40, 140, 2);
        record(mc, "02-idle-side", 150, 250, 2);
        record(mc, "03-sword-close", 254, 302, 2);
        record(mc, "04-walk", 312, 372, 1);
        record(mc, "05-fight", 392, 470, 1);
        record(mc, "06-second-phase", 482, 580, 1);
        record(mc, "07-boss-bar", 612, 612, 1);
        // Standing scenes: he stays where he is, facing north (his stroll and his looks about would turn him away).
        if (ticks > 20 && ticks < 300) server.execute(() -> face(180));
        if (ticks >= 60 && ticks <= 240) sawIdle |= clientPlays(mc, MurimAnimations.SENSEI_IDLE);
        if (ticks >= 320 && ticks <= 360) sawWalk |= clientPlays(mc, MurimAnimations.SENSEI_WALK);
        if (ticks > 380 && ticks <= 612) server.execute(() -> {
            sensei.setTarget(zombie);
            // Which of his moves he plays, and when.
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(sensei, LivingEntityPatch.class);
            AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
            if (main != null && main.getRealAnimation() != null) (sensei.secondPhase() ? movesInEarnest : moves).add(main.getRealAnimation().toString());
        });
        // Walking: on his way south, whatever his own stroll would choose.
        if (ticks > 304 && ticks < 372 && ticks % 10 == 0) server.execute(() -> sensei.getNavigation().moveTo(sx, sy, sz + 6, 0.6));
        switch (ticks) {
            case 20 -> {
                mc.options.hideGui = true;
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    ServerPlayer self = self(server);
                    sx = Math.floor(self.getX()) + 0.5;
                    sy = self.getY();
                    sz = Math.floor(self.getZ()) + 0.5;
                    sensei = MurimEntities.AZURE_SENSEI.get().spawn(level, BlockPos.containing(sx, sy, sz), MobSpawnType.COMMAND);
                    sensei.setNoAi(true);
                    face(180);    // north, -Z
                    // The camera: creative flight, not spectator (spectators see entities outlined).
                    self.setGameMode(GameType.CREATIVE);
                    self.getAbilities().flying = true;
                    self.getAbilities().invulnerable = true;
                    self.onUpdateAbilities();
                    check(sensei.getItemBySlot(EquipmentSlot.MAINHAND).is(MurimItems.AZURE_CLOUD_SWORD.get()), "the sensei holds the Azure Cloud");
                    // From the front three-quarter, on his right (east, as he faces north): the sword in his hand.
                    look(self, sx + 1.7, sy + 1.75, sz - 2.6, sx, sy + 1.05, sz);
                });
            }
            case 142 -> server.execute(() -> look(self(server), sx + 3.2, sy + 1.5, sz, sx, sy + 1.0, sz));
            default -> { }
        }
        // Close on the sword low in his right hand and its tassel, the camera going once round the hand.
        if (ticks >= 252 && ticks <= 302) {
            double turn = Math.toRadians((ticks - 252) * 7.2), hx = sx + 0.4, hy = sy + 0.7, hz = sz - 0.1;
            server.execute(() -> look(self(server), hx + 1.5 * Math.sin(turn), hy + 0.35, hz - 1.5 * Math.cos(turn), hx, hy, hz));
        }
        switch (ticks) {
            // He walks six blocks south, filmed from the side (his AI back a few ticks first: a mob without it is never
            // on the ground, and finds no path).
            case 300 -> server.execute(() -> sensei.setNoAi(false));
            case 304 -> server.execute(() -> {
                look(self(server), sx - 4.5, sy + 1.4, sz + 3.0, sx, sy + 1.0, sz + 3.0);
                sensei.setHome(BlockPos.containing(sx, sy, sz + 3));
                check(sensei.getNavigation().moveTo(sx, sy, sz + 6, 0.6), "the sensei finds his way to walk");
            });
            // A training zombie before him: he answers with the Azure Cloud's cuts.
            case 378 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                sensei.getNavigation().stop();
                double x = sensei.getX(), z = sensei.getZ();
                zombie = EntityType.ZOMBIE.create(level);
                zombie.moveTo(x, sy, z - 2.0, 0, 0);
                zombie.setNoAi(true);
                zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
                zombie.setHealth(1000);
                zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                // The same sword in a tight-sleeved hand: its grip, pommel and tassel in full view.
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.AZURE_CLOUD_SWORD.get()));
                zombie.setPersistenceRequired();
                level.addFreshEntity(zombie);
                sensei.setTarget(zombie);
                look(self(server), x + 4.5, sy + 1.7, z - 1.0, x, sy + 1.0, z - 1.0);
            });
            // Below half his health: his second phase, quicker, with the art's great techniques.
            case 476 -> server.execute(() -> {
                sensei.setHealth(sensei.getMaxHealth() * 0.45F);
                check(true, "his moves so far: " + moves);
            });
            case 482 -> server.execute(() -> check(sensei.secondPhase(), "below half his health, he fights in earnest"));
            // The HUD back for one picture: his azure boss bar framed in clouds at the top of the screen.
            case 591 -> mc.options.hideGui = false;
            case 612 -> server.execute(() -> check(sensei.showsBossBarTo(self(server)), "the camera player sees his boss bar"));
            case 620 -> {
                check(sawIdle, "the client plays his own idle");
                check(sawWalk, "the client plays his own walk");
                server.execute(() -> {
                    check(zombie.getHealth() < 1000, "the sensei's cuts land (zombie at " + zombie.getHealth() + ")");
                    check(true, "his moves in earnest: " + movesInEarnest);
                    zombie.discard();
                    sensei.discard();
                });
            }
            case 630 -> finish(mc);
            default -> { }
        }
    }

    /** Turns him (body and head) to this yaw. */
    private static void face(float yaw) {
        sensei.setYRot(yaw);
        sensei.yRotO = yaw;
        sensei.setYBodyRot(yaw);
        sensei.setYHeadRot(yaw);
        sensei.setXRot(0);
    }

    /** Whether the client's copy of the sensei now plays this animation. */
    private static boolean clientPlays(Minecraft mc, AssetAccessor<?> animation) {
        if (sensei == null || mc.level == null) return false;
        Entity entity = mc.level.getEntity(sensei.getId());
        LivingEntityPatch<?> patch = entity == null ? null : EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
        AnimationPlayer player = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        return player != null && (player.getAnimation() == animation || player.getRealAnimation() == animation);
    }

    // ---------------------------------------------------------------- helpers

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    /** Puts the camera's eye at (ex, ey, ez) looking at (tx, ty, tz). */
    private static void look(ServerPlayer self, double ex, double ey, double ez, double tx, double ty, double tz) {
        double dx = tx - ex, dy = ty - ey, dz = tz - ez;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        self.teleportTo(self.serverLevel(), ex, ey - self.getEyeHeight(), ez, yaw, pitch);
    }

    private static void record(Minecraft mc, String name, int from, int to, int every) throws Exception {
        if (ticks >= from && ticks <= to && (ticks - from) % every == 0) {
            Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(String.format("%s/frame-%03d.png", name, (ticks - from) / every));
            Files.createDirectories(file.getParent());
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(file);
            }
        }
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Sensei tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("sensei-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the sensei tour report", e);
        }
        LOGGER.info("Sensei tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
