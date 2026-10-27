package com.murimblock.anim.client;

import com.murimblock.Murimblock;
import com.murimblock.anim.Animation;
import com.murimblock.anim.Biped;
import com.murimblock.anim.ClipLoader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * The biped's animations: each {@code assets/<namespace>/skeletal/animations/<set>.gltf} with its sidecar
 * {@code <set>.json}, loaded with the resource packs (F3+T reloads them). Ids are {@code <namespace>:<set>/<clip>}.
 * A broken set is logged and left out.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SkeletalClips implements ResourceManagerReloadListener {
    static final String FOLDER = "skeletal/animations";
    private static Map<ResourceLocation, Animation> animations = Map.of();

    private SkeletalClips() {
    }

    /** The animation with this id, or null. */
    public static Animation get(ResourceLocation id) {
        return animations.get(id);
    }

    public static Map<ResourceLocation, Animation> all() {
        return animations;
    }

    @SubscribeEvent
    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SkeletalClips());
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<ResourceLocation, Animation> loaded = new HashMap<>();
        manager.listResources(FOLDER, path -> path.getPath().endsWith(".gltf")).forEach((file, resource) -> {
            String path = file.getPath();
            String set = path.substring(FOLDER.length() + 1, path.length() - ".gltf".length());
            String folder = path.substring(0, path.lastIndexOf('/') + 1);
            try (InputStream in = resource.open()) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                ResourceLocation sidecarFile = file.withPath(path.substring(0, path.length() - ".gltf".length()) + ".json");
                Optional<Resource> sidecar = manager.getResource(sidecarFile);
                String settings = sidecar.isPresent() ? new String(read(manager, sidecarFile), StandardCharsets.UTF_8) : null;
                ClipLoader.load(set, json, uri -> read(manager, file.withPath(folder + uri)), Biped.ARMATURE, settings)
                        .forEach((name, animation) -> loaded.put(
                                ResourceLocation.fromNamespaceAndPath(file.getNamespace(), animation.id()), animation));
            } catch (IOException | RuntimeException e) {
                Murimblock.LOGGER.error("Skeletal clips {} could not be loaded: {}", file, e.getMessage());
            }
        });
        animations = Map.copyOf(loaded);
        Murimblock.LOGGER.info("Loaded {} skeletal clips", animations.size());
    }

    private static byte[] read(ResourceManager manager, ResourceLocation file) {
        try (InputStream in = manager.open(file)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("missing " + file, e);
        }
    }
}
