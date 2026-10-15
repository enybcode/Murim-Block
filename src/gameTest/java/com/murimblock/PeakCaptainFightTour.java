package com.murimblock;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.mob.AllianceSoldierEntity;
import com.murimblock.mob.PeakCaptainEntity;
import com.murimblock.world.structure.site.SitePiece;
import com.murimblock.world.structure.site.SitePlan;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Rotation;
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
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Development-only real-client check of the whole fight against the Captain of the Peak Guard, on his own leitai
 * (gradlew -PgameTests runPeakCaptainFightTour): the Alliance arena is built in a flat world, a second player (on a
 * mock connection) fights the captain who stands there, with a sword, through his three phases. Checked every tick:
 * he strikes and his blows land, the fighter's blows hurt him (but for the retreat), neither he nor his men ever end in
 * a block or below the arena, his men land unhurt, and in the last phase he uses the Celestial Peak Spear. Frames go to
 * build/peak-captain-fight-tour-run/screenshots/fight, the report to peak-captain-fight-tour-report.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class PeakCaptainFightTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static volatile ServerPlayer fighter;
    private static volatile PeakCaptainEntity captain;
    private static double groundY;
    private static final List<String> report = new ArrayList<>();
    /** What the fight showed, counted every tick. */
    private static int hitsOnFighter, hitsOnFighterInArt, blowsLanded, blowsLandedInArt, blowsInRetreat, blowsHurtInRetreat;
    private static double lowest = Double.MAX_VALUE;
    private static final Set<String> captainMoves = new HashSet<>(), problems = new HashSet<>();
    private static final Set<UUID> men = new HashSet<>();
    /** The tour's timeline (ticks). */
    private static final int BUILD = 20, FIGHT = 60, HALF = FIGHT + 300, KILL_FIRST = HALF + 90, KILL_SECOND = KILL_FIRST + 110,
            FAR = KILL_SECOND + 40, END = FAR + 500;

    private PeakCaptainFightTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.peakCaptainFightTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-peak-captain-fight", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-peak-captain-fight", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            MinecraftServer server = mc.getSingleplayerServer();
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            server.execute(() -> {
                if (fighter != null && !fighter.isRemoved()) fighter.doTick();
                if (captain != null && ticks > FIGHT) watch(server);
            });
            if (ticks >= FIGHT && ticks <= END && (ticks - FIGHT) % 4 == 0) capture(mc, String.format("fight/frame-%03d.png", (ticks - FIGHT) / 4));
            step(mc, server);
        } catch (Exception e) {
            check(false, "tour crashed: " + e);
            LOGGER.error("Peak Captain fight tour crashed", e);
            finish(mc, mc.getSingleplayerServer());
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) {
        switch (ticks) {
            case BUILD -> {
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    ServerPlayer self = self(server);
                    groundY = Math.floor(self.getY());
                    int ox = (int) Math.floor(self.getX()) + 80, oz = (int) Math.floor(self.getZ());
                    var built = SitePiece.buildWhole(level, SitePlan.ALLIANCE_ARENA, ox, oz, Rotation.NONE, 0x5EEDL, new int[] {(int) groundY});
                    captain = built.spawned().stream().filter(m -> m instanceof PeakCaptainEntity).map(m -> (PeakCaptainEntity) m).findFirst().orElse(null);
                    check(captain != null, "the arena is built with its captain on the leitai");
                    // The other soldiers of the arena stay out of it: this is the captain's fight.
                    built.spawned().stream().filter(m -> m instanceof AllianceSoldierEntity).forEach(m -> m.discard());
                });
            }
            case BUILD + 10 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                fighter = dummy(server, level, "Challenger");
                fighter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
                // Facing him, three blocks off.
                fighter.teleportTo(level, captain.getX(), captain.getY(), captain.getZ() + 3, 180, 0);
                self(server).getAbilities().flying = true;
                self(server).onUpdateAbilities();
                view(server);
            });
            // 1. The spear: the fighter strikes first, then both trade blows.
            case FIGHT -> server.execute(() -> strike(server));
            case HALF -> server.execute(() -> {
                // Brought to half his health by one more blow: the retreat and the waves.
                captain.setHealth(captain.getMaxHealth() * PeakCaptainEntity.RETREAT_AT + 0.5F);
                strike(server);
            });
            // The next blows that land take him under half (a blow may meet his armour or his stance).
            case HALF + 30 -> server.execute(() -> check(captain.phase() == PeakCaptainEntity.Phase.RETREAT, "at half his health he retreats (" + captain.phase() + ", " + captain.getHealth() + ")"));
            case KILL_FIRST, KILL_SECOND -> server.execute(() -> {
                BlockPos post = captain.post();
                check(post != null && captain.distanceToSqr(Vec3.atBottomCenterOf(post)) < 3 * 3 && captain.getY() > post.getY() - 0.5,
                        "in his retreat he stands on his leitai, at his post (" + captain.position() + " for " + post + ")");
                var wave = captain.waveMen();
                check(wave.stream().allMatch(m -> !m.skyFalling() && m.getHealth() > 0), "wave " + (ticks == KILL_FIRST ? 1 : 2) + " has landed (" + wave.size() + " men)");
                wave.forEach(m -> m.kill());
            });
            case FAR -> server.execute(() -> {
                check(captain.phase() == PeakCaptainEntity.Phase.ART, "both waves down, he takes up the art (" + captain.phase() + ")");
                // Six blocks off: the Piercing Dragon's range.
                fighter.teleportTo(server.overworld(), captain.getX(), captain.getY(), captain.getZ() + 6, 180, 0);
            });
            // The fight over (the fighter gone far off and forgotten), he goes back up onto his leitai.
            case END - 200 -> server.execute(() -> {
                fighter.teleportTo(server.overworld(), captain.getX() + 60, captain.getY() + 20, captain.getZ(), 0, 0);
                fighter.setGameMode(GameType.SPECTATOR);
                captain.setTarget(null);
                captain.stopBeingAngry();
            });
            case END - 10 -> server.execute(() -> {
                BlockPos post = captain.post();
                check(captain.distanceToSqr(Vec3.atBottomCenterOf(post)) < 3 * 3 && captain.getY() > post.getY() - 0.5,
                        "after the fight he is back on his leitai (" + captain.position() + " for " + post + ", target " + captain.getTarget() + ", angry " + captain.getRemainingPersistentAngerTime() + ", goals " + captain.goalSelector.getAvailableGoals().stream().map(g -> g.getGoal().getClass().getSimpleName() + (g.isRunning() ? "*" : "") + (g.getGoal().canUse() ? "+" : "-")).toList() + ", noAi " + captain.isNoAi() + ", disabled " + disabled(captain) + ", nav done " + captain.getNavigation().isDone() + ")");
            });
            case END -> server.execute(() -> {
                check(hitsOnFighter > 0, "his blows land on the fighter (" + hitsOnFighter + ", " + hitsOnFighterInArt + " with the art)");
                check(hitsOnFighterInArt > 0, "his blows land in the last phase too");
                check(blowsLanded > 0 && blowsLandedInArt > 0, "the fighter's blows hurt him (" + blowsLanded + ", " + blowsLandedInArt + " in the last phase)");
                check(blowsInRetreat > 0 && blowsHurtInRetreat == 0, "in the retreat, blows glance off him (" + blowsInRetreat + " tried, " + blowsHurtInRetreat + " hurt)");
                check(captainMoves.stream().anyMatch(m -> m.contains("celestial")), "he uses the Celestial Peak Spear: " + captainMoves);
                check(men.size() == 7, "seven men dropped in all (" + men.size() + ")");
                check(problems.isEmpty(), "no one ends in a block or under the arena" + (problems.isEmpty() ? "" : ": " + problems));
                check(lowest >= groundY - 0.01, "the lowest anyone went: " + (lowest - groundY) + " from the court's ground");
            });
            case END + 6 -> finish(mc, server);
            default -> { }
        }
    }

    /** Every tick of the fight. */
    private static void watch(MinecraftServer server) {
        if (!captain.isAlive()) return;
        ServerLevel level = server.overworld();
        // The fighter: healed after every blow he takes (counted), kept close to the captain unless he is meant far.
        if (fighter.getHealth() < fighter.getMaxHealth()) {
            hitsOnFighter++;
            if (captain.phase() == PeakCaptainEntity.Phase.ART && fighter.getLastHurtByMob() == captain) hitsOnFighterInArt++;
            fighter.setHealth(fighter.getMaxHealth());
        }
        if (fighter.distanceTo(captain) > 9 && (ticks < FAR || ticks > FAR + 60) && ticks < END - 200)
            fighter.teleportTo(level, captain.getX(), captain.getY(), captain.getZ() + 2.5, 180, 0);
        if (ticks % 14 == 0 && ticks < END - 200) strike(server);
        // What he plays.
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(captain, LivingEntityPatch.class);
        var player = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        // (Epic Fight's empty animation has no name.)
        if (player != null && player.getRealAnimation() != null && player.getRealAnimation().registryName() != null)
            captainMoves.add(player.getRealAnimation().registryName().getPath());
        // Never in a block, never under the court.
        List<LivingEntity> all = new ArrayList<>(captain.waveMen());
        all.add(captain);
        for (LivingEntity e : all) {
            if (e instanceof AllianceSoldierEntity man) men.add(man.getUUID());
            if (e.isInWall()) problems.add(e.getName().getString() + " in a block at tick " + ticks);
            if (e.onGround()) lowest = Math.min(lowest, e.getY());
            if (e.getY() < groundY - 1) problems.add(e.getName().getString() + " under the arena at tick " + ticks + " (" + e.getY() + ")");
            if (e instanceof AllianceSoldierEntity man && !man.skyFalling() && man.getHealth() < man.getMaxHealth() && man.getLastHurtByMob() == null)
                problems.add("a man hurt by his fall");
        }
        view(server);
    }

    private static String disabled(PeakCaptainEntity c) {
        try {
            for (var f : net.minecraft.world.entity.ai.goal.GoalSelector.class.getDeclaredFields())
                if (java.util.Map.class.isAssignableFrom(f.getType())) { f.setAccessible(true); var m = (java.util.Map<?, ?>) f.get(c.goalSelector); StringBuilder b = new StringBuilder("locked "); m.forEach((k, v) -> b.append(k).append("=").append(((net.minecraft.world.entity.ai.goal.WrappedGoal) v).getGoal().getClass().getSimpleName()).append(((net.minecraft.world.entity.ai.goal.WrappedGoal) v).isRunning() ? "(running)" : "(stopped)").append(" ")); return b.toString(); }
        } catch (Exception e) { return e.toString(); }
        return "?";
    }

    /** The fighter swings at the captain (full strength). */
    private static void strike(MinecraftServer server) {
        float before = captain.getHealth();
        boolean retreat = captain.phase() == PeakCaptainEntity.Phase.RETREAT;
        fighter.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, captain.getEyePosition());
        fighter.resetAttackStrengthTicker();
        for (int i = 0; i < 40; i++) fighter.tick();
        captain.invulnerableTime = 0;
        fighter.attack(captain);
        boolean hurt = captain.getHealth() < before;
        if (retreat) {
            blowsInRetreat++;
            if (hurt) blowsHurtInRetreat++;
        } else if (hurt) {
            blowsLanded++;
            if (captain.phase() == PeakCaptainEntity.Phase.ART) blowsLandedInArt++;
        }
    }

    /** The camera: above and beside the leitai, on the captain. */
    private static void view(MinecraftServer server) {
        ServerPlayer self = self(server);
        double x = captain.getX() - 8, y = captain.getY() + 6, z = captain.getZ() - 8;
        double dx = captain.getX() - x, dy = captain.getY() + 1 - (y + self.getEyeHeight()), dz = captain.getZ() - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        self.teleportTo(server.overworld(), x, y, z, yaw, pitch);
    }

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    /** A second player, on a mock connection: the server tracks it and sends it to the client like any other. */
    private static ServerPlayer dummy(MinecraftServer server, ServerLevel level, String name) {
        var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), name), 0, ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        for (int i = 0; i <= 60; i++) player.tick();
        return player;
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Peak Captain fight tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }

    private static void finish(Minecraft mc, MinecraftServer server) {
        if (finished) return;
        finished = true;
        // The fighter leaves first, or the shutdown waits for him forever.
        if (server != null) server.execute(() -> {
            if (fighter != null) server.getPlayerList().remove(fighter);
        });
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("peak-captain-fight-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Peak Captain fight tour report", e);
        }
        LOGGER.info("Peak Captain fight tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.execute(mc::stop);
    }
}
