package com.murimblock.client.hud;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The size of the HUD at the bottom of the screen (combat bar, Qi bar, hearts, hunger), apart from Minecraft's GUI
 * scale. Each size is a whole number of screen pixels per HUD pixel, one fewer or more than the GUI scale, so the
 * pixel art stays crisp. Saved in Murimblock's client config; kept free of client classes so the server can load it.
 */
public enum HudSize {
    SMALL(-1),
    NORMAL(0),
    LARGE(1),
    HUGE(2);

    /** The widest part of the bottom HUD, in HUD pixels: the combat bar, the weapon slot and its labels. */
    static final int HUD_WIDTH = 240;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<HudSize> VALUE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        VALUE = builder.comment("Size of the HUD at the bottom of the screen, apart from the GUI scale.")
                .defineEnum("hudSize", NORMAL);
        SPEC = builder.build();
    }

    private final int step;

    HudSize(int step) {
        this.step = step;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public HudSize next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** The size in use; Normal until the config is loaded. */
    public static HudSize current() {
        return SPEC.isLoaded() ? VALUE.get() : NORMAL;
    }

    public static void set(HudSize size) {
        VALUE.set(size);
        if (SPEC.isLoaded()) SPEC.save();
    }

    /**
     * Screen pixels per HUD pixel: the GUI scale plus this size's step, at least 1, and no larger than lets the HUD
     * fit across the window.
     */
    public int pixels(int guiScale, int windowWidth) {
        int fit = Math.max(1, windowWidth / HUD_WIDTH);
        return Math.clamp(guiScale + step, 1, Math.max(guiScale, fit));
    }
}
