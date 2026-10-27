package com.murimblock.shenfa.client.mob;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Armature;
import com.murimblock.shenfa.ClipLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * Clips keyed on a mob's model parts ({@link ClipLoader#loadKeyframes}): {@code assets/<namespace>/skeletal/
 * mob_animations/<set>.json}, ids {@code <namespace>:<set>/<clip>}. A set is read with the resource packs and bound
 * to each rig that plays it (part names are looked up in that rig's skeleton); a clip naming a part the rig lacks is
 * logged once and not played.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MobClips implements ResourceManagerReloadListener {
    static final String FOLDER = "skeletal/mob_animations";
    private static Map<ResourceLocation, String> sets = Map.of();
    /** Each set bound to each skeleton that asked for it. */
    private static final Map<Armature, Map<ResourceLocation, Map<String, Animation>>> BOUND = new WeakHashMap<>();

    private MobClips() {
    }

    /** The clip {@code id} bound to {@code armature}, or null. */
    public static synchronized Animation get(ResourceLocation id, Armature armature) {
        int slash = id.getPath().lastIndexOf('/');
        if (slash < 0) {
            return null;
        }
        ResourceLocation set = id.withPath(id.getPath().substring(0, slash));
        String json = sets.get(set);
        if (json == null) {
            return null;
        }
        Map<String, Animation> clips = BOUND.computeIfAbsent(armature, key -> new HashMap<>()).computeIfAbsent(set,
                key -> {
                    try {
                        return ClipLoader.loadKeyframes(set.getPath(), json, armature);
                    } catch (RuntimeException e) {
                        Murimblock.LOGGER.error("Mob clips {} do not fit this mob: {}", set, e.getMessage());
                        return Map.of();
                    }
                });
        return clips.get(id.getPath().substring(slash + 1));
    }

    @SubscribeEvent
    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new MobClips());
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<ResourceLocation, String> loaded = new HashMap<>();
        manager.listResources(FOLDER, path -> path.getPath().endsWith(".json")).forEach((file, resource) -> {
            String path = file.getPath();
            ResourceLocation id = file.withPath(path.substring(FOLDER.length() + 1, path.length() - ".json".length()));
            try (InputStream in = resource.open()) {
                loaded.put(id, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                Murimblock.LOGGER.error("Mob clips {} could not be read: {}", file, e.getMessage());
            }
        });
        synchronized (MobClips.class) {
            sets = Map.copyOf(loaded);
            BOUND.clear();
        }
        MobShenfa.clear();
        Murimblock.LOGGER.info("Loaded {} mob clip sets", sets.size());
    }
}
