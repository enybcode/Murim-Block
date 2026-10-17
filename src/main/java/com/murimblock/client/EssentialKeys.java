package com.murimblock.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.murimblock.Murimblock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Essential (essential.gg) beside Murimblock: its keys that fall on ours (zoom on C where we meditate, the emote wheel on
 * B where we execute, anything on the combat bar's 1 to 0, on V, K or G) are unbound, so one press never does two
 * things. Done once, the first time both are there (a mark in the config folder), so a player who binds them again
 * afterwards keeps their choice.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class EssentialKeys {
    static final String MARK = "murimblock-essential-keys.done";
    private static boolean checked;

    private EssentialKeys() {
    }

    /** Whether a key mapping is Essential's: by its name or its category. */
    static boolean essentials(String name, String category) {
        return name.toLowerCase(Locale.ROOT).contains("essential") || category.toLowerCase(Locale.ROOT).contains("essential");
    }

    /** Once the options are loaded and Essential's keys registered: the first client tick on the title screen or later. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (checked) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null || mc.screen == null && mc.level == null) return;
        checked = true;
        Path mark = FMLPaths.CONFIGDIR.get().resolve(MARK);
        if (Files.exists(mark)) return;
        Set<InputConstants.Key> ours = new HashSet<>();
        for (KeyMapping k : mc.options.keyMappings) if (k.getCategory().equals(MurimblockKeyMappings.CATEGORY)) ours.add(k.getKey());
        for (KeyMapping k : mc.options.keyHotbarSlots) ours.add(k.getKey());
        // Essential may come through its own loader: it is known by its keys, not by a mod id.
        boolean essential = false;
        int unbound = 0;
        for (KeyMapping k : mc.options.keyMappings) {
            if (!essentials(k.getName(), k.getCategory())) continue;
            essential = true;
            if (k.isUnbound() || !ours.contains(k.getKey())) continue;
            LogUtils.getLogger().info("Murimblock: Essential's key '{}' was on {}, which Murimblock uses: unbound", k.getName(), k.getKey().getName());
            k.setKey(InputConstants.UNKNOWN);
            unbound++;
        }
        if (!essential) return;
        if (unbound > 0) {
            KeyMapping.resetMapping();
            mc.options.save();
        }
        try {
            Files.writeString(mark, "Essential's keys on Murimblock's were unbound once (" + unbound + "). Delete this file to do it again.\n");
        } catch (Exception e) {
            LogUtils.getLogger().warn("Murimblock: could not write {}", mark, e);
        }
    }
}
