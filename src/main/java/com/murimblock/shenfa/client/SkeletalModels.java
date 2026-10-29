package com.murimblock.shenfa.client;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.GltfLoader;
import com.murimblock.shenfa.SkinnedModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * The skinned models of {@code assets/<namespace>/skeletal/*.gltf}, loaded with the resource packs (F3+T reloads
 * them). The biped files are checked against {@link Biped}; a broken file is logged and left out.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SkeletalModels implements ResourceManagerReloadListener {
    /** Mobs on the biped's joints stretched to another size, plus joints of their own: their skeleton is their model's. */
    static final java.util.Set<String> STRETCHED = java.util.Set.of("moine");
    public static final ResourceLocation BIPED = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "biped");
    public static final ResourceLocation BIPED_SLIM = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "biped_slim");

    static final String FOLDER = "skeletal";
    private static Map<ResourceLocation, SkinnedModel> models = Map.of();

    private SkeletalModels() {
    }

    /** The model with this id (namespace and file name without the extension), or null. */
    public static SkinnedModel get(ResourceLocation id) {
        return models.get(id);
    }

    @SubscribeEvent
    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SkeletalModels());
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<ResourceLocation, SkinnedModel> loaded = new HashMap<>();
        manager.listResources(FOLDER, path -> path.getPath().endsWith(".gltf")
                && !path.getPath().startsWith(SkeletalClips.FOLDER + "/")).forEach((file, resource) -> {
            String name = file.getPath().substring(FOLDER.length() + 1, file.getPath().length() - ".gltf".length());
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), name);
            try (InputStream in = resource.open()) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                String folder = file.getPath().substring(0, file.getPath().lastIndexOf('/') + 1);
                // Mob models (skeletal/mobs/) are on the biped's skeleton too, unless they bring their own (no chest, or
                // the biped's joints stretched: STRETCHED).
                boolean biped = name.startsWith("biped") || name.startsWith("mobs/") && json.contains("\"Chest\"")
                        && !STRETCHED.contains(name.substring("mobs/".length()));
                loaded.put(id, GltfLoader.load(json, uri -> read(manager,
                        ResourceLocation.fromNamespaceAndPath(file.getNamespace(), folder + uri)), biped ? Biped.ARMATURE : null));
            } catch (IOException | RuntimeException e) {
                Murimblock.LOGGER.error("Skeletal model {} could not be loaded: {}", file, e.getMessage());
            }
        });
        models = Map.copyOf(loaded);
        MobCloth.clear();
        Murimblock.LOGGER.info("Loaded {} skeletal models", models.size());
    }

    private static byte[] read(ResourceManager manager, ResourceLocation file) {
        try (InputStream in = manager.open(file)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("missing " + file, e);
        }
    }
}
