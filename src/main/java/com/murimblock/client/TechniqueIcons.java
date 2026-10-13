package com.murimblock.client;

import com.murimblock.martial.MartialArts;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The icons of the techniques (textures/gui/technique/&lt;id&gt;.png, 32x32, drawn by scripts/worldgen/TechniqueIcons.java),
 * at any size; a technique without its icon yet shows its initials on the art's jade.
 */
public final class TechniqueIcons {
    private static final Map<ResourceLocation, Boolean> PRESENT = new HashMap<>();

    private TechniqueIcons() {
    }

    public static ResourceLocation texture(MartialArts.Technique technique) {
        return ResourceLocation.fromNamespaceAndPath(technique.id().getNamespace(), "textures/gui/technique/" + technique.id().getPath() + ".png");
    }

    static boolean has(MartialArts.Technique technique) {
        return PRESENT.computeIfAbsent(technique.id(), id -> Minecraft.getInstance().getResourceManager().getResource(texture(technique)).isPresent());
    }

    /** Draws the technique's icon in a size x size square at (x, y), in screen coordinates. */
    public static void draw(GuiGraphics g, MartialArts.Technique technique, int x, int y, int size) {
        if (has(technique)) {
            g.blit(texture(technique), x, y, size, size, 0, 0, 32, 32, 32, 32);
            return;
        }
        g.fill(x, y, x + size, y + size, 0xFF2E8A72);
        Minecraft mc = Minecraft.getInstance();
        g.drawCenteredString(mc.font, initials(Component.translatable(technique.translationKey()).getString()), x + size / 2, y + (size - 8) / 2, 0xFFF3EBD4);
    }

    static String initials(String name) {
        StringBuilder out = new StringBuilder();
        for (String word : name.split("[ -]")) if (!word.isEmpty() && out.length() < 2) out.append(Character.toUpperCase(word.charAt(0)));
        return out.toString();
    }
}
