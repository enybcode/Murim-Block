package com.murimblock.client.gui.data;

import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.client.innerworld.InnerWorldClient;
import com.murimblock.innerworld.InnerWorld;
import com.murimblock.innerworld.MortalTrial;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.CultivationPayloads;
import com.murimblock.cultivation.CultivationRealms;
import com.murimblock.cultivation.CultivationRules;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Realms and meridians. Each Murim rank holds a nine-stage cultivation realm; the twelve meridians open one after
 * another in the order Qi flows through them, then the Small Heavenly Circuit joins the Ren and Du vessels.
 * Progress comes from the server ({@link ClientCultivation}).
 */
public final class CultivationData {
    public record Realm(String rank, String realm) {
        public Component rankName() {
            return Component.translatable("murimblock.realm." + rank);
        }

        public Component realmName() {
            return Component.translatable("murimblock.cultivation_realm." + realm);
        }
    }

    /** An acupoint, placed on the front of a Minecraft player body 160 x 320 units (10 units per skin pixel). */
    public record Acupoint(String code, int x, int y) {
        public Component name() {
            return Component.translatable("murimblock.acupoint." + code.toLowerCase(Locale.ROOT));
        }
    }

    public record Meridian(String id, String code, String region, List<Acupoint> points) {
        public Component name() {
            return Component.translatable("murimblock.meridian." + id);
        }

        public Component effect() {
            return Component.translatable("murimblock.meridian." + id + ".effect");
        }

        public Component regionName() {
            return Component.translatable("gui.murimblock.cultivation.region." + region);
        }
    }

    public static final List<Realm> REALMS = CultivationRealms.LADDER.stream()
            .map(r -> new Realm(r.rank(), r.realm())).toList();

    public static final int STAGES = CultivationRules.STAGES;

    public static final List<Meridian> MERIDIANS = List.of(
            meridian("lung", "LU", "left_arm", p("LU1", 46, 92), p("LU5", 8, 140), p("LU7", 6, 176), p("LU9", 9, 186), p("LU11", 4, 196)),
            meridian("large_intestine", "LI", "left_arm", p("LI1", 14, 197), p("LI4", 17, 188), p("LI11", 4, 134), p("LI15", 18, 86), p("LI20", 68, 52)),
            meridian("stomach", "ST", "left_leg", p("ST1", 58, 50), p("ST25", 64, 160), p("ST36", 66, 262), p("ST41", 62, 304), p("ST45", 58, 316)),
            meridian("spleen", "SP", "left_leg", p("SP1", 75, 316), p("SP6", 75, 290), p("SP9", 75, 256), p("SP10", 73, 236), p("SP21", 44, 128)),
            meridian("heart", "HT", "left_arm", p("HT1", 36, 90), p("HT3", 34, 138), p("HT7", 34, 184), p("HT9", 35, 197)),
            meridian("small_intestine", "SI", "right_arm", p("SI1", 124, 197), p("SI3", 126, 188), p("SI8", 125, 136), p("SI10", 140, 86), p("SI19", 116, 44)),
            meridian("bladder", "BL", "left_leg", p("BL1", 72, 40), p("BL40", 52, 250), p("BL60", 44, 302), p("BL67", 44, 316)),
            meridian("kidney", "KI", "right_leg", p("KI1", 92, 316), p("KI3", 85, 300), p("KI10", 85, 252), p("KI27", 92, 86)),
            meridian("pericardium", "PC", "right_arm", p("PC1", 110, 104), p("PC3", 140, 140), p("PC6", 140, 170), p("PC8", 140, 187), p("PC9", 140, 197)),
            meridian("triple_burner", "TE", "right_arm", p("TE1", 151, 197), p("TE5", 152, 172), p("TE10", 155, 132), p("TE14", 150, 86), p("TE23", 110, 30)),
            meridian("gallbladder", "GB", "right_leg", p("GB1", 112, 42), p("GB20", 114, 74), p("GB30", 116, 206), p("GB34", 116, 262), p("GB44", 114, 316)),
            meridian("liver", "LR", "right_leg", p("LR1", 86, 316), p("LR3", 90, 306), p("LR8", 86, 250), p("LR13", 116, 150), p("LR14", 104, 118)),
            meridian("small_heavenly_circuit", "CV GV", "ren_du", p("GV20", 80, 2), p("CV24", 80, 68), p("CV17", 80, 110), p("CV12", 80, 140), p("CV6", 80, 160), p("CV1", 80, 198)));

    /** The dantian, below the navel on the body. */
    public static final int DANTIAN_X = 80;
    public static final int DANTIAN_Y = 172;

    private CultivationData() {
    }

    private static Acupoint p(String code, int x, int y) {
        return new Acupoint(code, x, y);
    }

    private static Meridian meridian(String id, String code, String region, Acupoint... points) {
        return new Meridian(id, code, region, List.of(points));
    }

    private static Cultivation.Snapshot now() {
        return ClientCultivation.get();
    }

    public static int currentRealm() {
        return now().realm();
    }

    /** Realms above the next one stay hidden as ???. */
    public static boolean revealed(int realmIndex) {
        return realmIndex <= currentRealm() + 1 && !REALMS.get(realmIndex).rank().equals("unknown");
    }

    public static int stage() {
        return now().stage();
    }

    /** Max Qi, growing toward the stage's peak. */
    public static int qi() {
        return now().maxQi();
    }

    /** How far the stage is from its floor to its peak, 0 to 1. */
    public static float stageShare() {
        return (float) (now().stageQi() / CultivationRules.gauge(now().realm(), now().stage()));
    }

    public static int qiToFill() {
        return CultivationRules.peak(now().realm(), now().stage());
    }

    public static boolean canAdvance() {
        return now().meditating() && CultivationRules.canAdvance(now().realm(), now().stage(), now().stageQi());
    }

    /** Last stage with a full gauge: only the trial leads further. */
    public static boolean trialReady() {
        return now().stage() >= STAGES && CultivationRules.gaugeFull(now().realm(), now().stage(), now().stageQi());
    }

    public static int meridiansForTrial() {
        return CultivationRules.meridiansForTrial(now().realm());
    }

    public static int openMeridians() {
        return now().meridians();
    }

    public static boolean isOpen(int meridian) {
        return meridian < openMeridians();
    }

    /** Meridians open in order: only the first sealed one can be opened. */
    public static boolean isNext(int meridian) {
        return meridian == openMeridians();
    }

    /** Whether the current realm lets this meridian open. */
    public static boolean realmAllows(int meridian) {
        return meridian < CultivationRules.meridianCap(now().realm());
    }

    public static boolean meditating() {
        return now().meditating();
    }

    public static boolean innerWorldUnlocked() {
        return InnerWorld.unlocked(now().realm());
    }

    public static boolean inInnerWorld() {
        return InnerWorldClient.inside();
    }

    /** Enter from meditation once unlocked; inside, always free to return. */
    public static boolean canTravel() {
        return inInnerWorld() || innerWorldUnlocked() && meditating();
    }

    public static void travel() {
        InnerWorldClient.travel();
    }

    /** The mortal trial: ready, meditating, and not already away from the body. */
    public static boolean canBeginTrial() {
        return now().meditating() && !inInnerWorld() && MortalTrial.ready(now().realm(), now().stage(), now().stageQi(), now().meridians());
    }

    public static void beginTrial() {
        PacketDistributor.sendToServer(new MortalTrial.Begin());
    }

    public static void toggleMeditation() {
        PacketDistributor.sendToServer(new CultivationPayloads.ToggleMeditation());
    }

    public static void advance() {
        PacketDistributor.sendToServer(new CultivationPayloads.Advance());
    }
}
