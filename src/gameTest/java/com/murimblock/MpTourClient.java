package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.client.CloudStepEffects;
import com.murimblock.client.CombatBarClient;
import com.murimblock.client.DrizzleSlashEffects;
import com.murimblock.integration.epicfight.MurimAnimations;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The two clients of the multiplayer check (MpTourServer, -Dmurimblock.mp=A or B). Each follows the phases the server
 * announces in its chat and presses its real keys: A attacks (keys 1 to 3 in combat mode, the attack key), B guards
 * (Epic Fight's guard key). Each checks what it sees of the other: B sees A's draw stance, dash and afterimages, the
 * delayed cuts on itself, itself carried up and brought down, its death by the slam; A sees its own dash, the delayed
 * cuts on B, its bar lit while the slam is ready, the slam. Report in build/mp-run/a or b/mp-tour-client.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MpTourClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static String phase = "";
    private static int at;
    private static boolean finished, failed;
    private static final List<String> report = new ArrayList<>();
    // What was seen during a phase.
    private static boolean sawAim, sawDash, sawAfterimages, sawDeath, sawSlam;
    private static double startY, peakY, startZ;
    private static int cutsBefore;
    private static int guardFrom = -1, guardTo = -1;
    private static int disconnected;

    private MpTourClient() { }

    private static String role() {
        return System.getProperty("murimblock.mp", "");
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent event) {
        String text = event.getMessage().getString();
        if (!text.startsWith("[mp] ") || role().isEmpty() || role().equals("server")) return;
        Minecraft mc = Minecraft.getInstance();
        phase = text.substring(5).trim();
        at = 0;
        sawAim = sawDash = sawAfterimages = sawDeath = sawSlam = false;
        guardFrom = guardTo = -1;
        if (mc.player != null) {
            startY = peakY = mc.player.getY();
            startZ = mc.player.getZ();
            Player other = other(mc);
            cutsBefore = other == null ? 0 : DrizzleSlashEffects.delayedCutsSeenOn(role().equals("A") ? other.getId() : mc.player.getId());
        }
        LOGGER.info("MP tour ({}): phase {}", role(), phase);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        String role = role();
        if (!role.equals("A") && !role.equals("B")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        // Disconnected (kicked, the server gone) after the tour began: the report, and quit, not stuck on that screen.
        if (!finished && !phase.isEmpty() && mc.player == null && ++disconnected > 200) {
            check(false, role + ": disconnected during phase " + phase);
            finish(mc);
        }
        if (finished || mc.player == null || phase.isEmpty()) return;
        at++;
        try {
            if (role.equals("A")) attacker(mc);
            else target(mc);
        } catch (Exception e) {
            check(false, "client tour crashed: " + e);
            LOGGER.error("MP tour crashed", e);
        }
        if (phase.equals("end") && at == 20) finish(mc);
    }

    // ---------------------------------------------------------------- A, who attacks

    private static void attacker(Minecraft mc) {
        KeyMapping k1 = mc.options.keyHotbarSlots[0], k2 = mc.options.keyHotbarSlots[1], k3 = mc.options.keyHotbarSlots[2];
        // Against a raised guard, the attack comes well after it went up (a normal parry: more than 0.5 s).
        int go = phase.startsWith("parry_") ? 40 : 10;
        // For the perfect parry, the draw stance is held only a moment (the guard goes up on seeing it).
        int hold = phase.equals("perfect_dash") ? 6 : 12;
        switch (phase) {
            case "dash", "parry_dash", "perfect_dash" -> {
                // Key 1 held (the draw stance) a moment, then let go: the dash.
                if (at == go) KeyMapping.click(k1.getKey());
                if (at >= go && at < go + hold) k1.setDown(true);
                if (at == 16 && phase.equals("dash")) check(playing(mc.player, MurimAnimations.CLOUD_STEP_AIM), "A: holding key 1 takes the draw stance");
                if (at == go + hold) k1.setDown(false);
                if (at == 60 && phase.equals("dash")) check(mc.player.getZ() - startZ > 3.5, String.format("A: the dash carries A about 5 blocks (%.2f)", mc.player.getZ() - startZ));
            }
            case "m1" -> {
                if (at == 10 || at == 24 || at == 38 || at == 52) KeyMapping.click(mc.options.keyAttack.getKey());
            }
            case "flurry", "parry_flurry" -> {
                if (at == go) KeyMapping.click(k2.getKey());
                if (at > go && at < go + 30 && playing(mc.player, MurimAnimations.THOUSAND_DRIZZLE)) sawSlam = true;
                if (at == go + 30) check(sawSlam, "A: key 2 plays the flurry (" + phase + ")");
                if (at == 90 && phase.equals("flurry")) {
                    Player b = other(mc);
                    check(b != null && DrizzleSlashEffects.delayedCutsSeenOn(b.getId()) > cutsBefore, "A: sees the delayed cuts open on B");
                }
            }
            case "wave" -> {
                if (at == 10) KeyMapping.click(k2.getKey());
            }
            case "crescent", "execute", "parry_crescent" -> {
                if (at == go) KeyMapping.click(k3.getKey());
                if (phase.equals("parry_crescent")) return;
                if (at == 38) check(CombatBarClient.slamReady(), "A: hanging at the top, the crescent's slot glows (slam ready)");
                if (at == 40 && phase.equals("crescent")) KeyMapping.click(mc.options.keyAttack.getKey());
                if (at == 50) KeyMapping.click(k3.getKey());
                if (at > 50 && at < 60 && playing(mc.player, MurimAnimations.CRESCENT_SLAM)) sawSlam = true;
                if (at == 60) check(sawSlam, "A: key 3 again plays the slam");
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- B, who takes it

    private static void target(Minecraft mc) {
        Player a = other(mc);
        peakY = Math.max(peakY, mc.player.getY());
        if (a != null) {
            if (playing(a, MurimAnimations.CLOUD_STEP_AIM)) sawAim = true;
            if (playing(a, MurimAnimations.CLOUD_STEP_SLASH)) sawDash = true;
            if (CloudStepEffects.hasAfterimages(a.getUUID())) sawAfterimages = true;
        }
        if (mc.screen instanceof DeathScreen) sawDeath = true;
        switch (phase) {
            case "dash" -> {
                if (at == 60) {
                    check(sawAim, "B: sees A take the draw stance");
                    check(sawDash, "B: sees A dash");
                    check(sawAfterimages, "B: sees A's afterimages");
                }
            }
            case "flurry" -> {
                if (at == 90) check(DrizzleSlashEffects.delayedCutsSeenOn(mc.player.getId()) > cutsBefore, "B: sees the delayed cuts open on itself");
            }
            case "crescent" -> {
                if (at == 100) {
                    check(peakY - startY > 5.5, String.format("B: sees itself carried up (%.2f blocks)", peakY - startY));
                    check(mc.player.getY() - startY < 0.6, String.format("B: sees itself thrown back down (%.2f blocks up)", mc.player.getY() - startY));
                }
            }
            case "execute" -> {
                if (at == 100) check(sawDeath, "B: dies from the slam's landing (the death screen, no knock-down)");
                // Back for the guard phases.
                if (at == 115 && mc.player.isDeadOrDying()) mc.player.respawn();
            }
            case "parry_dash", "parry_crescent", "parry_flurry" -> {
                // The guard held well before and through the attack: normal parries.
                guard(mc, at >= 5 && at < 95);
                if (phase.equals("parry_flurry") && at == 90)
                    check(DrizzleSlashEffects.delayedCutsSeenOn(mc.player.getId()) == cutsBefore, "B: no delayed cuts after a guarded flurry");
            }
            case "perfect_dash" -> {
                // The guard raised on seeing A's draw stance: up just before the dash, a perfect parry.
                if (guardFrom < 0 && a != null && playing(a, MurimAnimations.CLOUD_STEP_AIM)) {
                    guardFrom = at;
                    guardTo = at + 30;
                }
                guard(mc, guardFrom >= 0 && at >= guardFrom && at < guardTo);
            }
            default -> guard(mc, false);
        }
    }

    private static boolean guarding;

    private static void guard(Minecraft mc, boolean up) {
        KeyMapping key = EpicFightKeyMappings.GUARD;
        if (up && !guarding) KeyMapping.click(key.getKey());
        key.setDown(up);
        guarding = up;
    }

    // ---------------------------------------------------------------- shared

    private static Player other(Minecraft mc) {
        String name = role().equals("A") ? MpTourServer.TARGET : MpTourServer.ATTACKER;
        for (Player p : mc.level.players()) if (p.getGameProfile().getName().equals(name)) return p;
        return null;
    }

    private static boolean playing(Player player, AnimationAccessor<?> animation) {
        var patch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
        if (patch == null) return false;
        var main = patch.getAnimator().getPlayerFor(null);
        return main != null && main.getRealAnimation() != null && main.getRealAnimation().get() == animation.get();
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("MP tour ({}): {} {}", role(), ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        finished = true;
        report.add(failed ? "FAILED" : "PASSED");
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("mp-tour-client.txt"), String.join(System.lineSeparator(), report) + System.lineSeparator());
        } catch (Exception e) {
            LOGGER.error("Could not write the MP tour report", e);
        }
        LOGGER.info("MP tour ({}) finished: {}", role(), failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
