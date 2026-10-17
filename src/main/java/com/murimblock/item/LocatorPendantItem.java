package com.murimblock.item;

import com.murimblock.Murimblock;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * A locator pendant, sold at a high price by the Alliance Merchant: like the sect token, a needle in a jade disc, here
 * pointing to the nearest structure of one kind (docs/structures/README.md). It looks again whenever its holder has
 * travelled far from where it last looked, and on every right-click, which also tells the direction and distance.
 */
public class LocatorPendantItem extends Item {
    /** How far it looks (chunks; /locate looks 100). */
    public static final int SEARCH_CHUNKS = 160;
    /**
     * It looks again by itself only once its holder is this far from where it last looked (blocks), and no more often
     * than every RESEARCH_TICKS: a search for a far structure takes the server one to two seconds (site tour, 15/10).
     * A right-click always looks again.
     */
    static final int RESEARCH_DISTANCE = 2048;
    static final int RESEARCH_TICKS = 1200;
    private static final String DATA = "murimblock_pendant";

    private final ResourceKey<Structure> structure;

    public LocatorPendantItem(Properties properties, String structure) {
        super(properties);
        this.structure = ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, structure));
    }

    public ResourceKey<Structure> structure() { return structure; }

    /** The structure's name, for its messages. */
    public Component structureName() {
        return Component.translatable("structure.murimblock." + structure.location().getPath());
    }

    /** The nearest structure of its kind, or null when none stands within reach (or outside the Overworld). */
    @Nullable public static BlockPos locate(ServerLevel level, ResourceKey<Structure> structure, BlockPos from) {
        if (level.dimension() != Level.OVERWORLD) return null;
        var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(structure).orElse(null);
        if (holder == null) return null;
        var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), from, SEARCH_CHUNKS, false);
        return found == null ? null : found.getFirst();
    }

    /** Looks for its structure from where its holder stands, keeps the answer on the pendant, and returns it. */
    @Nullable BlockPos search(ItemStack stack, ServerLevel level, Entity holder) {
        BlockPos target = locate(level, structure, holder.blockPosition());
        CompoundTag data = new CompoundTag();
        data.putInt("x", holder.getBlockX());
        data.putInt("z", holder.getBlockZ());
        data.putLong("time", level.getGameTime());
        data.putBoolean("found", target != null);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(DATA, data));
        LodestoneTracker wanted = new LodestoneTracker(target == null ? Optional.empty() : Optional.of(GlobalPos.of(Level.OVERWORLD, target)), false);
        if (!wanted.equals(stack.get(DataComponents.LODESTONE_TRACKER))) stack.set(DataComponents.LODESTONE_TRACKER, wanted);
        return target;
    }

    /** Whether it should look again: never looked, or its holder went far, and not too soon. */
    boolean stale(ItemStack stack, ServerLevel level, Entity holder) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(DATA);
        if (data.isEmpty()) return true;
        if (level.getGameTime() - data.getLong("time") < RESEARCH_TICKS) return false;
        double dx = holder.getX() - data.getInt("x"), dz = holder.getZ() - data.getInt("z");
        return dx * dx + dz * dz > (double) RESEARCH_DISTANCE * RESEARCH_DISTANCE;
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 20 != 0 || level.dimension() != Level.OVERWORLD) return;
        if (stale(stack, (ServerLevel) level, entity)) search(stack, (ServerLevel) level, entity);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (level.dimension() != Level.OVERWORLD) {
            player.displayClientMessage(Component.translatable("item.murimblock.locator_pendant.other_world"), true);
        } else {
            BlockPos target = search(stack, (ServerLevel) level, player);
            if (target == null) {
                player.displayClientMessage(Component.translatable("item.murimblock.locator_pendant.none", structureName()), true);
            } else {
                double dx = target.getX() + 0.5 - player.getX(), dz = target.getZ() + 0.5 - player.getZ();
                int distance = (int) Math.round(Math.hypot(dx, dz));
                player.displayClientMessage(distance < 32
                        ? Component.translatable("item.murimblock.locator_pendant.here", structureName())
                        : Component.translatable("item.murimblock.locator_pendant.direction", structureName(), distance,
                                Component.translatable("direction.murimblock." + SectTokenItem.compass(dx, dz))), true);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.0F);
            }
        }
        // A search walks the world's structure grid: not on every click.
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
