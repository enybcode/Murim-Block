package com.murimblock.shenfa;

import com.murimblock.Murimblock;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * The clips as the server knows them, read from the mod's own files (a dedicated server has no resource packs): the
 * biped's clip sets ({@code skeletal/animations/*.gltf} and sidecars). The server times moves with them and places
 * the colliders of the biped's strikes; what clients draw comes from their resource packs.
 */
public final class ShenfaLibrary {
    private static final String ANIMATIONS = "skeletal/animations";
    private static Map<ResourceLocation, Animation> clips;

    private ShenfaLibrary() {
    }

    /** The clip {@code id}, or null. */
    public static Animation get(ResourceLocation id) {
        return clips().get(id);
    }

    public static synchronized Map<ResourceLocation, Animation> clips() {
        if (clips == null) {
            clips = load();
        }
        return clips;
    }

    /** Reads the clips again (a dev reload). */
    public static synchronized void reload() {
        clips = null;
    }

    private static Map<ResourceLocation, Animation> load() {
        Map<ResourceLocation, Animation> loaded = new HashMap<>();
        Path assets = ModList.get().getModFileById(Murimblock.MOD_ID).getFile().findResource("assets",
                Murimblock.MOD_ID);
        Path animations = assets.resolve(ANIMATIONS);
        for (Path gltf : list(animations, ".gltf")) {
            String set = name(gltf, ".gltf");
            try {
                Path sidecar = animations.resolve(set + ".json");
                String settings = Files.exists(sidecar) ? Files.readString(sidecar, StandardCharsets.UTF_8) : null;
                ClipLoader.load(set, Files.readString(gltf, StandardCharsets.UTF_8), uri -> read(animations.resolve(uri)),
                        Biped.ARMATURE, settings).values().forEach(animation -> loaded.put(id(animation), animation));
            } catch (IOException | RuntimeException e) {
                Murimblock.LOGGER.error("Shenfa clips {} could not be loaded: {}", set, e.getMessage());
            }
        }
        Murimblock.LOGGER.info("Shenfa library: {} clips", loaded.size());
        return Map.copyOf(loaded);
    }

    private static ResourceLocation id(Animation animation) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, animation.id());
    }

    private static Iterable<Path> list(Path folder, String suffix) {
        if (!Files.isDirectory(folder)) {
            return java.util.List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> file.getFileName().toString().endsWith(suffix)).sorted().toList();
        } catch (IOException e) {
            Murimblock.LOGGER.error("Shenfa library: cannot list {}: {}", folder, e.getMessage());
            return java.util.List.of();
        }
    }

    private static String name(Path file, String suffix) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - suffix.length());
    }

    private static byte[] read(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
