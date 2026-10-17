package com.murimblock.item;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.phys.Vec3;

/**
 * A crossing talisman, made by the Golden Dragon in human form for 32 gold yuanbao (GoldenDragonSageDialogue): a
 * one-time way to another world and back (Enzo, 17/10/2026). Held for three seconds, it carries its bearer to its world
 * (the Nether, the End, or the Murim, the overworld) and remembers where they stood; held again, it brings them back
 * to that very spot and crumbles. It does nothing in its own world when it has not carried anyone yet.
 */
public class CrossingTalismanItem extends Item {
    /** How long it is held before it carries (ticks). */
    public static final int CHANT_TICKS = 60;
    private static final String ORIGIN = "murimblock_origin";

    private final ResourceKey<Level> world;

    public CrossingTalismanItem(Properties properties, ResourceKey<Level> world) {
        super(properties);
        this.world = world;
    }

    public ResourceKey<Level> world() {
        return world;
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CHANT_TICKS;
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override public boolean isFoil(ItemStack stack) {
        return origin(stack).isPresent();
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (origin(stack).isEmpty() && level.dimension() == world) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.murimblock.crossing_talisman.here"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide && remaining % 2 == 0)
            for (int k = 0; k < 3; k++) level.addParticle(ParticleTypes.REVERSE_PORTAL, entity.getRandomX(0.8), entity.getRandomY(), entity.getRandomZ(0.8), 0, 0.05, 0);
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel here)) return stack;
        Optional<Origin> back = origin(stack);
        if (back.isPresent()) {
            // The way back: to the spot it remembers, then it crumbles.
            Origin o = back.get();
            ServerLevel there = player.server.getLevel(o.dimension);
            if (there == null) there = player.server.overworld();
            carry(player, here, there, new Vec3(o.x, o.y, o.z), o.yaw, o.pitch);
            there.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 0.7F);
            player.displayClientMessage(Component.translatable("message.murimblock.crossing_talisman.crumbles"), true);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return stack;
        }
        if (here.dimension() == world) return stack;
        ServerLevel there = player.server.getLevel(world);
        if (there == null) return stack;
        // The way out: one talisman of the stack takes the memory of where its bearer stood, and goes with them.
        ItemStack bound = stack.getCount() > 1 ? stack.split(1) : stack;
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", here.dimension().location().toString());
        tag.putDouble("x", player.getX());
        tag.putDouble("y", player.getY());
        tag.putDouble("z", player.getZ());
        tag.putFloat("yaw", player.getYRot());
        tag.putFloat("pitch", player.getXRot());
        CompoundTag data = bound.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.put(ORIGIN, tag);
        bound.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        Vec3 arrival = arrival(player, here, there);
        carry(player, here, there, arrival, player.getYRot(), 0);
        player.displayClientMessage(Component.translatable("message.murimblock.crossing_talisman.bound"), true);
        if (bound != stack && !player.getInventory().add(bound)) player.drop(bound, false);
        return stack;
    }

    private static void carry(ServerPlayer player, ServerLevel from, ServerLevel to, Vec3 at, float yaw, float pitch) {
        from.playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.35F, 1.4F);
        player.stopUsingItem();
        player.teleportTo(to, at.x, at.y, at.z, yaw, pitch);
        player.resetFallDistance();
        to.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1, at.z, 60, 0.4, 0.8, 0.4, 0.1);
        to.playSound(null, BlockPos.containing(at), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.6F);
    }

    /** Where the talisman sets its bearer down in its world: safe ground, never in lava or a wall. */
    static Vec3 arrival(ServerPlayer player, ServerLevel from, ServerLevel to) {
        if (to.dimension() == Level.END) {
            // The obsidian landing of the End, as an End portal makes it.
            BlockPos p = ServerLevel.END_SPAWN_POINT;
            EndPlatformFeature.createEndPlatform(to, p.below(), true);
            return Vec3.atBottomCenterOf(p);
        }
        if (to.dimension() == Level.NETHER) {
            double scale = from.dimensionType().coordinateScale() / to.dimensionType().coordinateScale();
            BlockPos centre = BlockPos.containing(player.getX() * scale, 64, player.getZ() * scale);
            BlockPos found = netherGround(to, centre);
            if (found != null) return Vec3.atBottomCenterOf(found);
            // No ground near: a small obsidian landing, the space above it cleared.
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                to.setBlockAndUpdate(centre.offset(dx, -1, dz), Blocks.OBSIDIAN.defaultBlockState());
                for (int dy = 0; dy < 3; dy++) to.setBlockAndUpdate(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
            return Vec3.atBottomCenterOf(centre);
        }
        // The Murim (the overworld): their own bed or anchor there if they have one, else the world's spawn.
        BlockPos home = player.getRespawnDimension() == to.dimension() && player.getRespawnPosition() != null ? player.getRespawnPosition() : to.getSharedSpawnPos();
        to.getChunk(home);
        BlockPos top = to.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, home);
        return Vec3.atBottomCenterOf(top);
    }

    /** Solid ground with two blocks of air above, not lava, the nearest to `centre` within 24 blocks, between y 32 and 110. */
    private static BlockPos netherGround(ServerLevel level, BlockPos centre) {
        for (int r = 0; r <= 24; r += 2)
            for (int dx = -r; dx <= r; dx += 2) for (int dz = -r; dz <= r; dz += 2) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                int x = centre.getX() + dx, z = centre.getZ() + dz;
                level.getChunk(x >> 4, z >> 4);
                for (int y = 100; y >= 32; y--) {
                    BlockPos feet = new BlockPos(x, y, z);
                    BlockState below = level.getBlockState(feet.below());
                    if (below.isSolidRender(level, feet.below()) && !below.is(Blocks.MAGMA_BLOCK) && level.getBlockState(feet).isAir()
                            && level.getBlockState(feet.above()).isAir() && level.getFluidState(feet).isEmpty()) return feet;
                }
            }
        return null;
    }

    private record Origin(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) { }

    private static Optional<Origin> origin(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return Optional.empty();
        CompoundTag tag = data.copyTag();
        if (!tag.contains(ORIGIN)) return Optional.empty();
        CompoundTag o = tag.getCompound(ORIGIN);
        ResourceLocation id = ResourceLocation.tryParse(o.getString("dimension"));
        if (id == null) return Optional.empty();
        return Optional.of(new Origin(ResourceKey.create(Registries.DIMENSION, id), o.getDouble("x"), o.getDouble("y"), o.getDouble("z"),
                o.getFloat("yaw"), o.getFloat("pitch")));
    }

    /** Whether this talisman remembers where its bearer came from (it has carried them out). */
    public static boolean bound(ItemStack stack) {
        return origin(stack).isPresent();
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        if (bound(stack)) lines.add(Component.translatable("item.murimblock.crossing_talisman.bound").withStyle(ChatFormatting.GOLD));
        else lines.add(Component.translatable("item.murimblock.crossing_talisman.unbound").withStyle(ChatFormatting.GRAY));
    }
}
