package com.murimblock.item;

import com.murimblock.world.structure.SanctuaryPlacement;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

/**
 * Sect token: a jade medallion of the Cloud Cliff sect whose needle points to the sanctuary, anywhere in the overworld
 * (a lodestone target that needs no lodestone). Right-click tells the direction and distance. Every player receives
 * one on first joining; other ways to get one (a mob drop...) are still to be decided with Enzo.
 */
public class SectTokenItem extends Item {

    public SectTokenItem(Properties properties) {
        super(properties);
    }

    /** Keeps the needle on the sanctuary (server side; the component syncs to the client). */
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 20 != 0) return;
        BlockPos target = target(((ServerLevel) level).getServer().overworld());
        if (target == null) return;
        LodestoneTracker wanted = new LodestoneTracker(Optional.of(GlobalPos.of(Level.OVERWORLD, target)), false);
        if (!wanted.equals(stack.get(DataComponents.LODESTONE_TRACKER))) stack.set(DataComponents.LODESTONE_TRACKER, wanted);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        BlockPos target = target(((ServerLevel) level).getServer().overworld());
        if (target == null) {
            player.displayClientMessage(Component.translatable("item.murimblock.sect_token.lost"), true);
        } else if (level.dimension() != Level.OVERWORLD) {
            player.displayClientMessage(Component.translatable("item.murimblock.sect_token.other_world"), true);
        } else {
            double dx = target.getX() + 0.5 - player.getX(), dz = target.getZ() + 0.5 - player.getZ();
            int distance = (int) Math.round(Math.hypot(dx, dz));
            if (distance < 24) {
                player.displayClientMessage(Component.translatable("item.murimblock.sect_token.here"), true);
            } else {
                player.displayClientMessage(Component.translatable("item.murimblock.sect_token.direction",
                        distance, Component.translatable("direction.murimblock." + compass(dx, dz))), true);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.2F);
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** One of the eight winds for a horizontal offset (north is -Z). */
    public static String compass(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz)); // 0 = north, clockwise
        String[] winds = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};
        return winds[(int) Math.floorMod(Math.round(angle / 45.0), 8)];
    }

    /** The sanctuary's centre at its summit, or its foot if unknown. */
    static BlockPos target(ServerLevel overworld) {
        SanctuaryPlacement data = SanctuaryPlacement.get(overworld);
        return data.centre() == null ? null : data.centre().atY(data.top());
    }
}
