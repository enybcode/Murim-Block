package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.combat.CombatService;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialService;
import com.murimblock.qi.QiService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Development-only two-client check of the Azure Cloud Sword Art on a dedicated server (scripts/mp/run-mp-tour.sh,
 * -Dmurimblock.mp=server): two real clients, MurimA (who attacks) and MurimB (who takes it, and guards), join a flat
 * world in offline mode. The server places them for each phase, announces it in their chat ("[mp] name"; each client
 * then presses its real keys, MpTourClient), and checks what happened on the server: the dash, the M1 chain, the flurry,
 * the wave, the crescent with its hang, its M1 in the air and its slam (blast, the attacker unhurt), the slam's
 * execution of a player, then the guard against the dash (normal and perfect), the crescent and the flurry. Report in
 * build/mp-run/server/mp-tour-server.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class MpTourServer {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final String ATTACKER = "MurimA", TARGET = "MurimB";
    /** Each phase's start (server ticks after both players are in) and name, as the clients know them. */
    static final int DASH = 140, M1 = 230, FLURRY = 320, WAVE = 430, CRESCENT = 520, EXECUTE = 650, PARRY_DASH = 850,
            PERFECT_DASH = 950, PARRY_CRESCENT = 1050, PARRY_FLURRY = 1150, END = 1270;
    private static final float TARGET_HEALTH = 100.0F;

    private static int ticks = -1;
    private static int afterDone;
    private static boolean done, written, failed;
    private static final List<String> report = new ArrayList<>();
    private static int groundY;
    private static float healthAtStart, dashDamage, flurryDamage;
    private static double targetPeakY;

    private MpTourServer() { }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!"server".equals(System.getProperty("murimblock.mp"))) return;
        MinecraftServer server = event.getServer();
        ServerPlayer a = server.getPlayerList().getPlayerByName(ATTACKER), b = server.getPlayerList().getPlayerByName(TARGET);
        if (done) {
            // Both gone (or a client stuck for half a minute): the report, and the server stops.
            if (!written && ((a == null && b == null) || ++afterDone > 600)) {
                written = true;
                write(server);
                server.halt(false);
            }
            return;
        }
        if (a == null || b == null) {
            if (ticks > 0) {
                check(false, "both players stayed online through the tour (" + (a == null ? ATTACKER : TARGET) + " left at tick " + ticks + ")");
                done = true;
            }
            return;
        }
        ticks++;
        countParries(b);
        try {
            step(server, server.overworld(), a, b);
        } catch (Exception e) {
            check(false, "server tour crashed: " + e);
            LOGGER.error("MP tour crashed", e);
            done = true;
        }
    }

    private static void step(MinecraftServer server, ServerLevel level, ServerPlayer a, ServerPlayer b) {
        if (ticks == 20) {
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            level.setDayTime(6000);
            groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
            for (ServerPlayer p : List.of(a, b)) prepare(p);
        }
        // Each phase: places the two, announces it, then checks.
        if (ticks == DASH) start(server, a, b, "dash", 3.5);
        if (ticks == DASH + 70) {
            dashDamage = taken(b);
            check(dashDamage > 0, "the dash cuts the other player (" + dashDamage + ")");
            check(a.getZ() - 0.5 > 3.5 && a.getZ() - 0.5 < 6.5, String.format("the dash carries its player about 5 blocks, as the server sees it (%.2f)", a.getZ() - 0.5));
        }
        if (ticks == M1) start(server, a, b, "m1", 2.3);
        if (ticks == M1 + 80) check(taken(b) > 6, "the four M1 land on the other player (" + taken(b) + ")");
        if (ticks == FLURRY) start(server, a, b, "flurry", 1.8);
        if (ticks == FLURRY + 100) {
            flurryDamage = taken(b);
            check(flurryDamage > 8, "the flurry and its delayed cuts land on the other player (" + flurryDamage + ")");
        }
        if (ticks == WAVE) start(server, a, b, "wave", 7.5);
        if (ticks == WAVE + 70) check(taken(b) > 0, "the wave hits the other player 7 blocks away (" + taken(b) + ")");
        if (ticks == CRESCENT) start(server, a, b, "crescent", 1.5);
        if (ticks > CRESCENT && ticks < CRESCENT + 100) targetPeakY = Math.max(targetPeakY, b.getY());
        if (ticks == CRESCENT + 110) {
            check(targetPeakY - groundY > 5.5, String.format("the crescent carries the other player up (%.2f blocks)", targetPeakY - groundY));
            check(b.getY() - groundY < 0.6, String.format("the slam brings the other player back down (%.2f blocks up)", b.getY() - groundY));
            check(taken(b) > 10, "the crescent, the M1 in the air, the slam and its landing hurt the other player (" + taken(b) + ")");
            check(a.getHealth() >= a.getMaxHealth() - 0.01F, "no fall or blast damage for the one who slams (" + a.getHealth() + ")");
            check(level.getBlockState(BlockPos.containing(b.getX(), groundY - 1, b.getZ())).isAir()
                    || level.getBlockState(BlockPos.containing(b.getX(), groundY - 2, b.getZ())).isAir(), "the slam's blast breaks the ground");
            check(!a.isNoGravity() && !MartialCombat.hovering(a), "the hang is over");
        }
        if (ticks == EXECUTE) {
            refill(level);
            start(server, a, b, "execute", 1.5);
            b.setHealth(2.0F);
        }
        if (ticks == EXECUTE + 110) check(!b.isAlive() || b.isDeadOrDying(), "the slam's landing executes a player it would knock down (alive " + b.isAlive()
                + ", down " + KnockdownService.isDowned(b) + ")");
        if (ticks == PARRY_DASH) {
            refill(level);
            prepare(b);
            start(server, a, b, "parry_dash", 3.5);
        }
        if (ticks == PARRY_DASH + 80) check(taken(b) > 0 && taken(b) <= dashDamage * 0.6F, "a normal parry takes half the dash (" + taken(b) + " of " + dashDamage + "; " + parries() + ")");
        if (ticks == PERFECT_DASH) start(server, a, b, "perfect_dash", 3.5);
        if (ticks == PERFECT_DASH + 80) check(taken(b) < 0.01F, "a perfect parry, raised on seeing the draw stance, stops the dash (" + taken(b) + "; " + parries() + ")");
        if (ticks == PARRY_CRESCENT) {
            targetPeakY = groundY;
            start(server, a, b, "parry_crescent", 1.5);
        }
        if (ticks > PARRY_CRESCENT && ticks < PARRY_CRESCENT + 85) targetPeakY = Math.max(targetPeakY, b.getY());
        if (ticks == PARRY_CRESCENT + 85) {
            check(taken(b) > 0 && taken(b) <= 7.2F * 0.6F, "a normal parry takes half the crescent's cut (" + taken(b) + "; " + parries() + ")");
            check(targetPeakY - groundY < 1.5, String.format("a guard is not carried up by the crescent (%.2f blocks)", targetPeakY - groundY));
            check(!MartialCombat.hovering(a), "no hang after a guarded crescent");
        }
        if (ticks == PARRY_FLURRY) start(server, a, b, "parry_flurry", 1.8);
        if (ticks == PARRY_FLURRY + 100) check(taken(b) > 0 && taken(b) <= flurryDamage * 0.6F,
                "a normal parry takes half the flurry, with no delayed cuts (" + taken(b) + " of " + flurryDamage + "; " + parries() + ")");
        if (ticks == END) {
            announce(server, "end");
            done = true;
        }
    }

    /** Every blow the target takes, as it lands: how much, and whether its guard took it (normal parry). */
    @SubscribeEvent
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (!"server".equals(System.getProperty("murimblock.mp")) || !(event.getEntity() instanceof ServerPlayer p) || !p.getGameProfile().getName().equals(TARGET)) return;
        boolean guarded = com.murimblock.integration.epicfight.MurimGuardSkill.guardedThisTick(p);
        if (guarded) normals++;
        LOGGER.info("MP tour: {} takes {} from {} at tick {}{}", TARGET, event.getNewDamage(), event.getSource().getMsgId(), ticks, guarded ? " (normal parry)" : "");
    }

    /** Perfect and normal parries by the target in the current phase, and how often it raised its guard. */
    private static int normals, raises, lastRaised = -1;

    private static void countParries(ServerPlayer b) {
        var patch = com.murimblock.integration.epicfight.EpicFightBridge.patch(b);
        var guard = patch == null ? null : patch.getSkill(yesman.epicfight.skill.SkillSlots.GUARD);
        if (guard == null) return;
        int raised = guard.getDataManager().getDataValue(yesman.epicfight.registry.entries.EpicFightSkillDataKeys.LAST_ACTIVE);
        if (raised != 0 && raised != lastRaised) raises++;
        lastRaised = raised;
    }

    private static String parries() {
        return normals + " blows on a normal parry, guard raised " + raises + " times";
    }

    /** A student of the art: manual read, sword in hand, combat mode, the three techniques on keys 1 to 3. */
    private static void prepare(ServerPlayer p) {
        MartialService.learn(p, MartialArts.art(MartialArts.AZURE_CLOUD_SWORD));
        for (MartialArts.Technique t : List.of(MartialArts.CLOUD_STEP_SLASH, MartialArts.THOUSAND_DRIZZLE, MartialArts.HEAVEN_SPLITTING_CRESCENT)) MartialService.unlock(p, t);
        MartialService.assign(p, 0, MartialArts.CLOUD_STEP_SLASH.id());
        MartialService.assign(p, 1, MartialArts.THOUSAND_DRIZZLE.id());
        MartialService.assign(p, 2, MartialArts.HEAVEN_SPLITTING_CRESCENT.id());
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        CombatService.setCombatMode(p, true);
    }

    /** Places the two for a phase (the target `ahead` blocks south of the attacker, facing each other) and announces it. */
    private static void start(MinecraftServer server, ServerPlayer a, ServerPlayer b, String phase, double ahead) {
        ServerLevel level = server.overworld();
        a.teleportTo(level, 0.5, groundY, 0.5, 0, 0);
        b.teleportTo(level, 0.5, groundY, 0.5 + ahead, 180, 0);
        for (ServerPlayer p : List.of(a, b)) {
            MartialService.resetCooldowns(p);
            QiService.setQi(p, QiService.getQiMax(p));
            p.removeAllEffects();
            p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        b.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TARGET_HEALTH);
        b.setHealth(TARGET_HEALTH);
        a.setHealth(a.getMaxHealth());
        healthAtStart = b.getHealth();
        targetPeakY = groundY;
        normals = raises = 0;
        announce(server, phase);
    }

    private static float taken(ServerPlayer b) {
        return healthAtStart - b.getHealth();
    }

    /** The slam's crater filled back in, the ground clear above. */
    private static void refill(ServerLevel level) {
        BlockPos base = new BlockPos(0, groundY, 0);
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-6, -6, -6), base.offset(6, -1, 12)))
            level.setBlockAndUpdate(p, (p.getY() == groundY - 1 ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState());
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-6, 0, -6), base.offset(6, 4, 12))) level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
    }

    private static void announce(MinecraftServer server, String phase) {
        LOGGER.info("MP tour: phase {} at tick {}", phase, ticks);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) p.sendSystemMessage(Component.literal("[mp] " + phase));
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("MP tour (server): {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void write(MinecraftServer server) {
        report.add(failed ? "FAILED" : "PASSED");
        try {
            Files.writeString(Path.of("mp-tour-server.txt"), String.join(System.lineSeparator(), report) + System.lineSeparator());
        } catch (Exception e) {
            LOGGER.error("Could not write the MP tour report", e);
        }
        LOGGER.info("MP tour finished: {}", failed ? "FAILED" : "PASSED");
    }
}
