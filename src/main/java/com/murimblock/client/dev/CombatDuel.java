package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.MurimKeys;
import com.murimblock.client.combat.ClientCombat;
import com.murimblock.combat.CombatMoves;
import com.murimblock.combat.CombatMovesets;
import com.murimblock.combat.FighterState;
import com.murimblock.dev.CombatDuelServer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev tool, off unless {@code murimblock.combatDuel} is {@code attacker} or {@code defender} (run
 * {@code gradlew runCombatDuelServer}, {@code runCombatDuelAttacker} and {@code runCombatDuelDefender} together): two
 * real clients on a dedicated server. The attacker turns combat mode on and strikes; each side checks what crossed
 * the network (moves sent, the hit dealt, the strike and the blow seen by the defender), logs PASS/FAIL and quits.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CombatDuel {
    private static final String ROLE = System.getProperty("murimblock.combatDuel", "");
    private static final boolean ATTACKER = ROLE.equals("attacker");
    private static final boolean ENABLED = ATTACKER || ROLE.equals("defender");
    private static final String ADDRESS = "localhost:25565";
    /** Ticks after both stand face to face. */
    private static final int STRIKE = 60;
    private static final int CHECK = STRIKE + 15;
    /** The defender waits for the blow at most this long. */
    private static final int GIVE_UP = 600;
    private static int felt = -1;

    private static int retry;
    private static int together = -1;
    private static float otherHealth;
    private static float ownHealth;
    private static final List<String> FAILED = new ArrayList<>();

    private CombatDuel() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // Two windows share the screen: the one in the back must not pause (closing the pause menu would
        // release every key, clicks included).
        mc.options.pauseOnLostFocus = false;
        if (mc.options.onboardAccessibility) {
            mc.options.onboardAccessibility = false;
            mc.options.save();
            mc.setScreen(new TitleScreen());
            return;
        }
        if (mc.player == null) {
            // The server may still be starting: try again every five seconds.
            if (mc.screen instanceof DisconnectedScreen) {
                mc.setScreen(new TitleScreen());
            }
            if (mc.screen instanceof TitleScreen && retry-- <= 0) {
                retry = 100;
                ConnectScreen.startConnecting(mc.screen, mc, ServerAddress.parseString(ADDRESS),
                        new ServerData("Combat duel", ADDRESS, ServerData.Type.OTHER), false, null);
            }
            return;
        }
        if (mc.screen instanceof PauseScreen) {
            mc.setScreen(null);
        }
        Player other = other(mc);
        if (together < 0) {
            if (other != null && other.distanceTo(mc.player) < 3 && mc.screen == null) {
                together = 0;
            }
            return;
        }
        together++;
        if (ATTACKER) {
            attacker(mc, other);
        } else {
            defender(mc);
        }
    }

    private static void finish(Minecraft mc) {
        FAILED.forEach(name -> Murimblock.LOGGER.error("Combat duel: FAIL {}", name));
        Murimblock.LOGGER.info("Combat duel ({}): {}", ROLE, FAILED.isEmpty() ? "PASS" : FAILED.size() + " FAILED");
        mc.stop();
    }

    private static void attacker(Minecraft mc, Player other) {
        switch (together) {
            case 20 -> {
                check("moves and styles arrive over the network",
                        !CombatMoves.all().isEmpty() && !CombatMovesets.all().isEmpty());
                KeyMapping.click(MurimKeys.COMBAT_MODE.getKey());
            }
            case 40 -> check("the server turns combat mode on", ClientCombat.inCombatMode());
            case STRIKE -> {
                otherHealth = other == null ? -1 : other.getHealth();
                KeyMapping.click(mc.options.keyAttack.getKey());
            }
            case STRIKE + 1 -> check("the strike starts at once (screen " + mc.screen + ", style "
                    + CombatMovesets.forHeld(mc.player.getMainHandItem()) + ", hand " + mc.player.getMainHandItem() + ")",
                    ClientCombat.ownState() == FighterState.ATTACK);
            case CHECK -> {
                float lost = other == null ? -1 : otherHealth - other.getHealth();
                check("the defender loses one strike of health (lost " + lost + ")", lost > 2.9f && lost < 3.1f);
            }
            // Stay a little so the defender sees everything before the attacker leaves.
            case CHECK + 40 -> finish(mc);
            default -> {
            }
        }
    }

    /** The defender does nothing but wait for the blow, then checks what reached it. */
    private static void defender(Minecraft mc) {
        if (together == 1) {
            ownHealth = mc.player.getHealth();
        }
        if (together == 10) {
            // In combat mode a blow taken flinches: the defender checks it reaches it over the network.
            KeyMapping.click(MurimKeys.COMBAT_MODE.getKey());
        }
        if (felt < 0 && ClientCombat.blowsFelt() >= 1) {
            felt = together;
        }
        if (felt >= 0 && together == felt + 30) {
            float lost = ownHealth - mc.player.getHealth();
            check("the defender sees the attacker's strike", ClientCombat.strikesSeen() >= 1);
            check("the defender feels the blow (shake)", ClientCombat.blowsFelt() >= 1);
            check("the defender flinches", ClientCombat.flinches() >= 1);
            check("the defender loses one strike of health (lost " + lost + ")", lost > 2.9f && lost < 3.1f);
            finish(mc);
        } else if (felt < 0 && together == GIVE_UP) {
            check("the defender feels the blow within " + GIVE_UP / 20 + " s", false);
            finish(mc);
        }
    }

    private static Player other(Minecraft mc) {
        String name = ATTACKER ? CombatDuelServer.DEFENDER : CombatDuelServer.ATTACKER;
        for (Player player : mc.level.players()) {
            if (player.getGameProfile().getName().equals(name)) {
                return player;
            }
        }
        return null;
    }

    private static void check(String name, boolean ok) {
        Murimblock.LOGGER.info("Combat duel ({}): {} {}", ROLE, ok ? "ok  " : "FAIL", name);
        if (!ok) {
            FAILED.add(name);
        }
    }
}
