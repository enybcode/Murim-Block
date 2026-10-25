package com.murimblock.innerworld;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.Meditation;
import com.murimblock.registry.MurimAttachments;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The inner world: each player's own island in the dimension {@code murimblock:inner_world}, opened once they leave the
 * mortal realm. Entered from meditation; the body stays seated where the player sat. Only this code sends a player
 * there, always to their own island.
 */
public final class InnerWorld {
    public static final ResourceKey<Level> LEVEL =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "inner_world"));
    /** First realm past Mortal (Body Tempering): Third-rate Warrior, Qi Condensation. */
    public static final int UNLOCK_REALM = 1;
    /** Falling this far below the surface sends the player back to the body. */
    private static final int FALL_DEPTH = 40;
    /** Farther than this from their own island center, a player is not where they belong. */
    private static final int OWN_ISLAND_RANGE = InnerIslands.SPACING / 4;

    private InnerWorld() {
    }

    public static InnerWorldState state(ServerPlayer player) {
        return player.getData(MurimAttachments.INNER_WORLD);
    }

    public static boolean isInnerWorld(Level level) {
        return level.dimension() == LEVEL;
    }

    public static boolean unlocked(int realm) {
        return realm >= UNLOCK_REALM;
    }

    /** From the System button: go in, or come back if already inside. */
    public static void travel(ServerPlayer player) {
        if (isInnerWorld(player.level())) {
            leave(player, "message.murimblock.inner_world.leave");
        } else {
            enter(player);
        }
    }

    public static void enter(ServerPlayer player) {
        Cultivation c = Meditation.of(player);
        InnerWorldState state = state(player);
        ServerLevel inner = player.server.getLevel(LEVEL);
        if (state.inside() || isInnerWorld(player.level())) {
            return;
        }
        if (!unlocked(c.realm())) {
            system(player, "message.murimblock.inner_world.locked", ChatFormatting.GOLD);
            return;
        }
        if (!c.meditating()) {
            system(player, "message.murimblock.inner_world.meditate", ChatFormatting.GOLD);
            return;
        }
        if (inner == null) {
            system(player, "message.murimblock.inner_world.unavailable", ChatFormatting.RED);
            return;
        }
        BlockPos center = InnerIslands.get(player.server).prepare(player.getUUID(), inner);
        MeditatingBody body = MeditatingBody.seat(player);
        state.enter(player.level().dimension(), player.getX(), player.getY(), player.getZ(), player.getYRot(),
                player.getXRot(), body.getUUID());
        Meditation.stop(player, null);
        // Facing north, toward the arena.
        player.teleportTo(inner, center.getX() + 0.5, InnerIsland.SURFACE_Y + 1, center.getZ() + InnerIsland.ARRIVAL_Z + 0.5,
                180, 0);
        player.fallDistance = 0;
        OldMaster.summon(inner, center, player);
        system(player, "message.murimblock.inner_world.enter", ChatFormatting.AQUA);
    }

    /** Back to the body: the spirit returns where it sat and the body vanishes. */
    public static void leave(ServerPlayer player, String reasonKey) {
        InnerWorldState state = state(player);
        if (!state.inside()) {
            if (isInnerWorld(player.level())) {
                evict(player);
            }
            return;
        }
        ServerLevel back = player.server.getLevel(state.returnDim());
        if (back == null) {
            back = player.server.overworld();
        }
        UUID body = state.body();
        state.clear();
        player.teleportTo(back, state.x(), state.y(), state.z(), state.yRot(), state.xRot());
        player.fallDistance = 0;
        Entity seated = body == null ? null : back.getEntity(body);
        if (seated != null) {
            seated.discard();
        }
        if (reasonKey != null) {
            system(player, reasonKey, ChatFormatting.AQUA);
        }
    }

    /** A blow on the seated body tears its owner back, if this is still their body. */
    static void bodyStruck(ServerPlayer owner, MeditatingBody body) {
        InnerWorldState state = state(owner);
        if (state.inside() && body.getUUID().equals(state.body())) {
            leave(owner, "message.murimblock.inner_world.struck");
        }
    }

    /** Checked every tick for players in the inner world. */
    static void tick(ServerPlayer player) {
        InnerWorldState state = state(player);
        if (!state.inside()) {
            evict(player);
            return;
        }
        if (player.getY() < InnerIsland.SURFACE_Y - FALL_DEPTH) {
            leave(player, "message.murimblock.inner_world.fell");
            return;
        }
        BlockPos own = InnerIslands.get(player.server).existingCenter(player.getUUID());
        if (own == null || Math.abs(player.getX() - own.getX()) > OWN_ISLAND_RANGE
                || Math.abs(player.getZ() - own.getZ()) > OWN_ISLAND_RANGE) {
            leave(player, "message.murimblock.inner_world.leave");
        }
    }

    /** Someone in the inner world with no body to return to (teleported in by hand): sent to the world spawn. */
    private static void evict(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        player.teleportTo(overworld, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, player.getYRot(), 0);
        player.fallDistance = 0;
    }

    static void system(ServerPlayer player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }
}
