package com.murimblock.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side options, saved in {@code config/murimblock-client.toml}. */
public final class MurimClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_GUIDE = BUILDER
            .comment("Show the Guide sub-tab (controls and topics) on the Info page of the System menu.")
            .define("showGuide", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MurimClientConfig() {
    }

    public static boolean showGuide() {
        return SHOW_GUIDE.get();
    }

    public static void setShowGuide(boolean show) {
        SHOW_GUIDE.set(show);
        SHOW_GUIDE.save();
    }
}
