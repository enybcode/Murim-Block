package com.murimblock.cultivation.inner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

/**
 * A journey into the inner world, saved with the player (so a cultivator who logs out inside comes back to their body):
 * where the body sits (dimension, position, facing), which body entity it is, why they went (a visit, or the great
 * breakthrough) and how strong their Heart Demon is.
 */
public record InnerSession(boolean active, ResourceLocation dimension, double x, double y, double z, float yaw, float pitch,
                           Optional<UUID> body, int purpose, double strength) {
    public static final int VISIT = 0, BREAKTHROUGH = 1;
    public static final InnerSession NONE = new InnerSession(false, ResourceLocation.withDefaultNamespace("overworld"), 0, 0, 0, 0, 0,
            Optional.empty(), VISIT, 1);

    public static final Codec<InnerSession> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("active").forGetter(InnerSession::active),
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(InnerSession::dimension),
            Codec.DOUBLE.fieldOf("x").forGetter(InnerSession::x),
            Codec.DOUBLE.fieldOf("y").forGetter(InnerSession::y),
            Codec.DOUBLE.fieldOf("z").forGetter(InnerSession::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(InnerSession::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(InnerSession::pitch),
            UUIDUtil.CODEC.optionalFieldOf("body").forGetter(InnerSession::body),
            Codec.INT.optionalFieldOf("purpose", VISIT).forGetter(InnerSession::purpose),
            Codec.DOUBLE.optionalFieldOf("strength", 1.0).forGetter(InnerSession::strength)
    ).apply(i, InnerSession::new));

    public boolean breakthrough() {
        return purpose == BREAKTHROUGH;
    }
}
