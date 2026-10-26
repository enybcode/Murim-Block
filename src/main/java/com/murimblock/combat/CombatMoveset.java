package com.murimblock.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/**
 * A fighting style, read from {@code data/<namespace>/combat_movesets/<name>.json}: what it is fought with, and its
 * moves. Bare hands are one style; swords and spears will be others, on the same engine.
 *
 * @param held      what the main hand holds: {@code "empty"}, an item id, or {@code "#"} and an item tag
 * @param chain     the left-click chain (M1), move ids in order
 * @param chainWindow ticks after a move ends in which the next click goes on with the chain instead of restarting it
 * @param priority  when several styles match the same item, the highest wins
 */
public record CombatMoveset(List<String> held, List<ResourceLocation> chain, int chainWindow, int priority) {
    public static final String EMPTY = "empty";
    public static final Codec<CombatMoveset> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().fieldOf("held").forGetter(CombatMoveset::held),
            ResourceLocation.CODEC.listOf().fieldOf("chain").forGetter(CombatMoveset::chain),
            Codec.intRange(0, 100).optionalFieldOf("chain_window", 10).forGetter(CombatMoveset::chainWindow),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(CombatMoveset::priority)
    ).apply(i, CombatMoveset::new));
    public static final StreamCodec<ByteBuf, CombatMoveset> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public boolean matches(ItemStack stack) {
        for (String entry : held) {
            if (entry.equals(EMPTY)) {
                if (stack.isEmpty()) {
                    return true;
                }
            } else if (!stack.isEmpty()) {
                if (entry.startsWith("#")) {
                    if (stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.substring(1))))) {
                        return true;
                    }
                } else if (BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ResourceLocation.parse(entry))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The move at this place in the chain, wrapping round. */
    public ResourceLocation chainMove(int index) {
        return chain.get(Math.floorMod(index, chain.size()));
    }
}
