package com.murimblock.client;

import com.murimblock.martial.MartialArts;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The icons of the techniques and passives (textures/gui/technique/&lt;id&gt;.png, 32x32, drawn by
 * scripts/worldgen/TechniqueIcons.java), at any size; one without its icon yet shows its initials on the art's jade.
 */
public final class TechniqueIcons {
    private static final Map<ResourceLocation, Boolean> PRESENT = new HashMap<>();

    private TechniqueIcons() {
    }

    public static ResourceLocation texture(MartialArts.Technique technique) {
        return texture(technique.id());
    }

    public static ResourceLocation texture(ResourceLocation id) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/gui/technique/" + id.getPath() + ".png");
    }

    static boolean has(ResourceLocation id) {
        return PRESENT.computeIfAbsent(id, i -> Minecraft.getInstance().getResourceManager().getResource(texture(i)).isPresent());
    }

    /** Draws the technique's icon in a size x size square at (x, y), in screen coordinates. */
    public static void draw(GuiGraphics g, MartialArts.Technique technique, int x, int y, int size) {
        draw(g, technique.id(), Component.translatable(technique.translationKey()), x, y, size, 0xFF2E8A72);
    }

    /** Draws the icon of a technique or a passive by its id; without one, its initials on `color`. */
    public static void draw(GuiGraphics g, ResourceLocation id, Component name, int x, int y, int size, int color) {
        if (has(id)) {
            g.blit(texture(id), x, y, size, size, 0, 0, 32, 32, 32, 32);
            return;
        }
        drawLabel(g, name, x, y, size, color);
    }

    /** A plain square of `color` with the initials of `name`. */
    public static void drawLabel(GuiGraphics g, Component name, int x, int y, int size, int color) {
        g.fill(x, y, x + size, y + size, color);
        Minecraft mc = Minecraft.getInstance();
        g.drawCenteredString(mc.font, initials(name.getString()), x + size / 2, y + (size - 8) / 2, 0xFFF3EBD4);
    }

    static String initials(String name) {
        StringBuilder out = new StringBuilder();
        for (String word : name.split("[ -]")) if (!word.isEmpty() && out.length() < 2) out.append(Character.toUpperCase(word.charAt(0)));
        return out.toString();
    }
}
