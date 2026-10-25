package com.murimblock.innerworld;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Per player, saved: whether their spirit is in the inner world, where the body sits and which entity it is.
 * Saved because a player can log out inside. Field names in {@link #CODEC} are saved IDs: keep them.
 */
public final class InnerWorldState {
    public static final Codec<InnerWorldState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("inside").forGetter(s -> s.inside),
            ResourceLocation.CODEC.fieldOf("return_dim").forGetter(s -> s.returnDim.location()),
            Codec.DOUBLE.fieldOf("x").forGetter(s -> s.x),
            Codec.DOUBLE.fieldOf("y").forGetter(s -> s.y),
            Codec.DOUBLE.fieldOf("z").forGetter(s -> s.z),
            Codec.FLOAT.fieldOf("y_rot").forGetter(s -> s.yRot),
            Codec.FLOAT.fieldOf("x_rot").forGetter(s -> s.xRot),
            UUIDUtil.CODEC.optionalFieldOf("body").forGetter(s -> Optional.ofNullable(s.body)))
            .apply(i, InnerWorldState::new));

    private boolean inside;
    private ResourceKey<Level> returnDim = Level.OVERWORLD;
    private double x;
    private double y;
    private double z;
    private float yRot;
    private float xRot;
    private UUID body;

    public InnerWorldState() {
    }

    private InnerWorldState(boolean inside, ResourceLocation dim, double x, double y, double z, float yRot, float xRot,
                            Optional<UUID> body) {
        this.inside = inside;
        this.returnDim = ResourceKey.create(Registries.DIMENSION, dim);
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yRot;
        this.xRot = xRot;
        this.body = body.orElse(null);
    }

    public void enter(ResourceKey<Level> dim, double x, double y, double z, float yRot, float xRot, UUID body) {
        this.inside = true;
        this.returnDim = dim;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yRot;
        this.xRot = xRot;
        this.body = body;
    }

    public void clear() {
        inside = false;
        body = null;
    }

    public boolean inside() {
        return inside;
    }

    public ResourceKey<Level> returnDim() {
        return returnDim;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yRot() {
        return yRot;
    }

    public float xRot() {
        return xRot;
    }

    public UUID body() {
        return body;
    }
}
