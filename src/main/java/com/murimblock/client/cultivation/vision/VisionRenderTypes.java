package com.murimblock.client.cultivation.vision;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.RenderType;

/**
 * What the spiritual vision draws with: a dark veil over the world, then glowing lines and quads added on top. None
 * of them tests depth, since the vision sees through walls.
 */
final class VisionRenderTypes extends RenderType {
    static final RenderType DARK = create("murimblock_vision_dark", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    /** Stars, halos, afterimages and the faint faces: light added, scaled by the vertex alpha. */
    static final RenderType GLOW = create("murimblock_vision_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1 << 16, false, false, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    /** The block outlines: a wide soft pass under a thin bright one. */
    static final RenderType HALO_LINES = lines("murimblock_vision_halo_lines", 4);
    static final RenderType CORE_LINES = lines("murimblock_vision_core_lines", 1.5);

    private VisionRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                              boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }

    private static RenderType lines(String name, double width) {
        return create(name, DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1 << 16, false, false,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(width)))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .createCompositeState(false));
    }
}
