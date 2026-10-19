package com.murimblock.world.raven;

import com.murimblock.Murimblock;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The Raven Halls (docs/combat/styles/raven-art.md), the Raven Art's inner world: a dimension of its own
 * (murimblock:raven_halls) where each student has a hall far from the others, their own world and base, built the first
 * time they come; and closed rooms, apart, where the swallowed are kept for a while. Everything the art moves from a
 * world to the other goes through here: players home and back (the crouching Closed Room), the doors of the hall to the
 * perches, the swallowed and their return, the duels of the Closed Room.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class RavenHalls {
    public static final ResourceKey<Level> HALLS = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "raven_halls"));
    /** The halls are this far apart (blocks); the rooms stand in a row far from all of them. */
    public static final int SPACING = 4096, ROOM_SPACING = 48, ROOMS = 256, ROOM_ROW_Z = 300_000, FLOOR = 64;
    /** Where a player stood before coming home (their saved data). */
    static final String RETURN = "murimblock:raven_return";
    /** A door is used at most once in this many ticks by the same player. */
    static final int DOOR_COOLDOWN = 40;

    private RavenHalls() {
    }

    // ---------------------------------------------------------------- the level

    /** The server of GameTests loads no datapack dimension: its tests use another level instead, as if it were the halls. */
    @Nullable private static ServerLevel standIn;
    private static int standInZ;

    public static void useStandInForTests(@Nullable ServerLevel level, int z) {
        standIn = level;
        standInZ = z;
    }

    @Nullable public static ServerLevel halls(MinecraftServer server) {
        ServerLevel level = server.getLevel(HALLS);
        return level != null ? level : standIn;
    }

    public static boolean isHalls(Level level) {
        return level.dimension() == HALLS || (standIn != null && level == standIn);
    }

    /** The middle of this player's hall floor (where one stands). */
    public static BlockPos hallOf(ServerPlayer player) {
        int slot = RavenHallsData.get(player.server.overworld()).slot(player.getUUID());
        return new BlockPos(slot * SPACING, FLOOR, player.server.getLevel(HALLS) == null ? standInZ : 0);
    }

    /** The middle of a closed room's floor. */
    public static BlockPos roomAt(MinecraftServer server, int index) {
        return new BlockPos(index * ROOM_SPACING, FLOOR, (server.getLevel(HALLS) == null ? standInZ : 0) + ROOM_ROW_Z);
    }

    // ---------------------------------------------------------------- home and back

    /** Takes the player home, to their hall (built the first time), remembering where they came in from. */
    public static boolean home(ServerPlayer player) {
        ServerLevel halls = halls(player.server);
        if (halls == null || isHalls(player.level())) return false;
        CompoundTag back = new CompoundTag();
        back.putString("Dim", player.level().dimension().location().toString());
        back.putDouble("X", player.getX());
        back.putDouble("Y", player.getY());
        back.putDouble("Z", player.getZ());
        back.putFloat("Yaw", player.getYRot());
        player.getPersistentData().put(RETURN, back);
        BlockPos hall = hallOf(player);
        RavenHallsData data = RavenHallsData.get(player.server.overworld());
        if (!data.built(player.getUUID())) {
            RavenHallBuilder.hall(halls, hall);
            data.markBuilt(player.getUUID());
        }
        gateFx(player.serverLevel(), player.position());
        player.teleportTo(halls, hall.getX() + 0.5, hall.getY(), hall.getZ() + 6.5, 180, 0);
        gateFx(halls, player.position());
        player.displayClientMessage(Component.translatable("message.murimblock.raven_halls.home"), true);
        return true;
    }

    /** Back from the halls to where the player came in from (or the world's spawn). */
    public static void leave(ServerPlayer player) {
        CompoundTag back = player.getPersistentData().getCompound(RETURN);
        ServerLevel to = back.isEmpty() ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(back.getString("Dim"))));
        if (to == null) to = player.server.overworld();
        Vec3 at = back.isEmpty() ? Vec3.atBottomCenterOf(to.getSharedSpawnPos()) : new Vec3(back.getDouble("X"), back.getDouble("Y"), back.getDouble("Z"));
        gateFx(player.serverLevel(), player.position());
        player.teleportTo(to, at.x, at.y, at.z, back.getFloat("Yaw"), 0);
        gateFx(to, at);
        player.getPersistentData().remove(RETURN);
    }

    private static final Map<UUID, Long> DOOR_USED = new HashMap<>();

    /** The player walked into a door of their hall: the way out (0), or to one of their perches (1 to 3). */
    static void throughDoor(ServerPlayer player, int index) {
        long now = player.server.overworld().getGameTime();
        Long last = DOOR_USED.get(player.getUUID());
        if (last != null && now - last < DOOR_COOLDOWN) return;
        DOOR_USED.put(player.getUUID(), now);
        if (index == 0) {
            leave(player);
            return;
        }
        List<GlobalPos> perches = RavenHallsData.get(player.server.overworld()).perches(player.getUUID());
        if (index > perches.size()) {
            player.displayClientMessage(Component.translatable("message.murimblock.raven_halls.no_perch", index), true);
            return;
        }
        GlobalPos perch = perches.get(index - 1);
        ServerLevel to = player.server.getLevel(perch.dimension());
        if (to == null) return;
        gateFx(player.serverLevel(), player.position());
        BlockPos p = perch.pos();
        player.teleportTo(to, p.getX() + 0.5, p.getY(), p.getZ() + 1.5, player.getYRot(), 0);
        gateFx(to, player.position());
        player.getPersistentData().remove(RETURN);
    }

    // ---------------------------------------------------------------- perches

    /** A perch set down: one of the player's if they study the art (the oldest beyond three taken down). */
    static void perchSet(ServerPlayer player, GlobalPos at) {
        if (!MartialService.data(player).knowsArt(MartialArts.RAVEN_ART)) {
            player.displayClientMessage(Component.translatable("message.murimblock.raven_perch.mute"), true);
            return;
        }
        RavenHallsData data = RavenHallsData.get(player.server.overworld());
        GlobalPos out = data.addPerch(player.getUUID(), at);
        if (out != null) {
            ServerLevel level = player.server.getLevel(out.dimension());
            if (level != null && level.isLoaded(out.pos()) && level.getBlockState(out.pos()).is(RavenBlocks.RAVEN_PERCH.get())) level.destroyBlock(out.pos(), true);
        }
        player.displayClientMessage(Component.translatable("message.murimblock.raven_perch.set", data.perches(player.getUUID()).size(), RavenHallsData.MAX_PERCHES), true);
    }

    static void perchGone(ServerLevel level, GlobalPos at) {
        RavenHallsData.get(level.getServer().overworld()).removePerch(at);
    }

    // ---------------------------------------------------------------- the swallowed and the duels

    private record Captive(UUID entity, Entity body, ResourceKey<Level> from, Vec3 back, float yaw, double drop, long until, int room) { }

    private record Duel(UUID player, UUID foe, Entity foeBody, ResourceKey<Level> from, Vec3 playerBack, Vec3 foeBack, long until, int room) { }

    /**
     * The body as it is now: a player by their id (a respawn makes a new one); anything else as it was put there, for a
     * room nobody stands in keeps its bodies out of the level's lookup (they do not tick, and are not found by id).
     */
    @Nullable private static Entity resolve(MinecraftServer server, ServerLevel halls, UUID id, Entity body) {
        if (body instanceof ServerPlayer) return server.getPlayerList().getPlayer(id);
        if (!body.isRemoved()) return body;
        return halls.getEntity(id);
    }

    private static final List<Captive> CAPTIVES = new ArrayList<>();
    private static final List<Duel> DUELS = new ArrayList<>();
    private static int nextRoom;

    /** A room with somebody in it stays loaded (nobody else is near to keep it): its bodies keep living, and come back. */
    static final TicketType<ChunkPos> ROOM_TICKET = TicketType.create(Murimblock.MOD_ID + ":raven_room", java.util.Comparator.comparingLong(ChunkPos::toLong));

    private static int takeRoom(MinecraftServer server) {
        int index = nextRoom++ % ROOMS;
        ServerLevel halls = halls(server);
        if (halls != null) {
            ChunkPos c = new ChunkPos(roomAt(server, index));
            halls.getChunkSource().addRegionTicket(ROOM_TICKET, c, 2, c);
            RavenHallBuilder.room(halls, roomAt(server, index));
        }
        return index;
    }

    private static void freeRoom(MinecraftServer server, int index) {
        ServerLevel halls = halls(server);
        ChunkPos c = new ChunkPos(roomAt(server, index));
        if (halls != null) halls.getChunkSource().removeRegionTicket(ROOM_TICKET, c, 2, c);
    }

    /** Nothing kept in the halls despawns there (no player is near enough to keep a mob). */
    @SubscribeEvent
    public static void onDespawn(MobDespawnEvent event) {
        if (event.getLevel() instanceof ServerLevel level && isHalls(level)) event.setResult(MobDespawnEvent.Result.DENY);
    }

    /**
     * Swallows a body into a closed room for `ticks` ticks; it comes back where it was, `drop` blocks higher (falling).
     * False if it cannot be (no halls).
     */
    public static boolean swallow(LivingEntity body, int ticks, double drop) {
        MinecraftServer server = body.getServer();
        ServerLevel halls = server == null ? null : halls(server);
        if (halls == null) return false;
        int room = takeRoom(server);
        Vec3 back = body.position();
        ResourceKey<Level> from = body.level().dimension();
        float yaw = body.getYRot();
        gateFx((ServerLevel) body.level(), back);
        Entity moved = move(body, halls, Vec3.atBottomCenterOf(roomAt(server, room)), yaw);
        if (moved == null) return false;
        CAPTIVES.add(new Captive(moved.getUUID(), moved, from, back, yaw, drop, server.overworld().getGameTime() + ticks, room));
        return true;
    }

    /** Takes the player and their foe into a closed room for `ticks` ticks, alone; both come back to their places after. */
    public static boolean duel(ServerPlayer player, LivingEntity foe, int ticks) {
        ServerLevel halls = halls(player.server);
        if (halls == null || isHalls(player.level())) return false;
        int room = takeRoom(player.server);
        BlockPos c = roomAt(player.server, room);
        ResourceKey<Level> from = player.level().dimension();
        Vec3 pb = player.position(), fb = foe.position();
        gateFx(player.serverLevel(), pb);
        gateFx(player.serverLevel(), fb);
        Entity moved = move(foe, halls, new Vec3(c.getX() + 0.5, c.getY(), c.getZ() - 3.5), 0);
        player.teleportTo(halls, c.getX() + 0.5, c.getY(), c.getZ() + 3.5, 180, 0);
        if (moved == null) return false;
        DUELS.add(new Duel(player.getUUID(), moved.getUUID(), moved, from, pb, fb, player.server.overworld().getGameTime() + ticks, room));
        return true;
    }

    /** Whether this player is in a Closed Room's duel now. */
    public static boolean dueling(ServerPlayer player) {
        for (Duel d : DUELS) if (d.player.equals(player.getUUID())) return true;
        return false;
    }

    /** This player's foe in their Closed Room's duel, or null. */
    @Nullable public static Entity duelFoe(ServerPlayer player) {
        ServerLevel halls = halls(player.server);
        for (Duel d : DUELS) if (d.player.equals(player.getUUID())) return halls == null ? null : resolve(player.server, halls, d.foe, d.foeBody);
        return null;
    }

    /** Ends this player's duel now (one of them fell). */
    public static void endDuel(ServerPlayer player) {
        for (Iterator<Duel> it = DUELS.iterator(); it.hasNext(); ) {
            Duel d = it.next();
            if (!d.player.equals(player.getUUID())) continue;
            it.remove();
            finish(player.server, d);
            return;
        }
    }

    private static void finish(MinecraftServer server, Duel d) {
        freeRoom(server, d.room);
        ServerLevel halls = halls(server), from = server.getLevel(d.from);
        if (halls == null || from == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(d.player);
        Entity foe = resolve(server, halls, d.foe, d.foeBody);
        if (player != null && isHalls(player.level())) player.teleportTo(from, d.playerBack.x, d.playerBack.y, d.playerBack.z, player.getYRot(), 0);
        if (foe != null && foe.isAlive()) move(foe, from, d.foeBack, foe.getYRot());
        gateFx(from, d.playerBack);
        gateFx(from, d.foeBack);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        ServerLevel halls = halls(server);
        if (halls == null) return;
        for (Iterator<Captive> it = CAPTIVES.iterator(); it.hasNext(); ) {
            Captive c = it.next();
            if (now < c.until) continue;
            it.remove();
            freeRoom(server, c.room);
            Entity e = resolve(server, halls, c.entity, c.body);
            ServerLevel from = server.getLevel(c.from);
            if (e == null || from == null || !e.isAlive()) continue;
            Vec3 at = c.back.add(0, c.drop, 0);
            move(e, from, at, c.yaw);
            gateFx(from, at);
        }
        for (Iterator<Duel> it = DUELS.iterator(); it.hasNext(); ) {
            Duel d = it.next();
            Entity foe = resolve(server, halls, d.foe, d.foeBody);
            ServerPlayer player = server.getPlayerList().getPlayer(d.player);
            boolean over = now >= d.until || player == null || !player.isAlive() || foe == null || !foe.isAlive()
                    || com.murimblock.combat.knockdown.KnockdownService.isDowned(player)
                    || foe instanceof ServerPlayer p && com.murimblock.combat.knockdown.KnockdownService.isDowned(p);
            if (!over) continue;
            it.remove();
            finish(server, d);
        }
    }

    public static void clearForTests() {
        CAPTIVES.clear();
        DUELS.clear();
        DOOR_USED.clear();
    }

    // ---------------------------------------------------------------- moving between the worlds

    /** Moves a body to this level and place: a player by a teleport, anything else through the dimension (a new entity). */
    @Nullable public static Entity move(Entity e, ServerLevel to, Vec3 at, float yaw) {
        if (e instanceof ServerPlayer p) {
            p.teleportTo(to, at.x, at.y, at.z, yaw, p.getXRot());
            return p;
        }
        if (e.level() == to) {
            e.teleportTo(at.x, at.y, at.z);
            e.setYRot(yaw);
            return e;
        }
        return e.changeDimension(new DimensionTransition(to, at, Vec3.ZERO, yaw, e.getXRot(), DimensionTransition.DO_NOTHING));
    }

    /** A door opening there: black feathers whirling, a deep sound. */
    public static void gateFx(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 1, at.z, 24, 0.4, 0.8, 0.4, 0.05);
        level.sendParticles(ParticleTypes.ASH, at.x, at.y + 1, at.z, 30, 0.6, 1.0, 0.6, 0.02);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.6F);
    }
}
