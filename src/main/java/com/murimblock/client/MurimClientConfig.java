package com.murimblock.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side options, saved in {@code config/murimblock-client.toml}. */
public final class MurimClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_GUIDE = BUILDER
            .comment("Show the Guide sub-tab (controls and topics) on the Info page of the System menu.")
            .define("showGuide", true);

    public static final ModConfigSpec.BooleanValue SHENFA_PLAYERS = BUILDER
            .comment("Draw players with Shenfa, the mod's animation engine (needed to see combat moves).")
            .define("shenfaPlayers", true);

    public static final ModConfigSpec.BooleanValue SHENFA_MOBS = BUILDER
            .comment("Draw mobs with the mod's own looks (the zombie as the Disciple d00e9chu); false shows vanilla's models.")
            .define("shenfaMobs", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MurimClientConfig() {
    }

    public static boolean showGuide() {
        return SHOW_GUIDE.get();
    }

    /** Whether players are drawn by Shenfa; on before the config is read. */
    public static boolean shenfaPlayers() {
        try {
            return SHENFA_PLAYERS.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    /** Whether mobs wear the mod's looks; on before the config is read. */
    public static boolean shenfaMobs() {
        try {
            return SHENFA_MOBS.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    public static void setShowGuide(boolean show) {
        SHOW_GUIDE.set(show);
        SHOW_GUIDE.save();
    }
}
