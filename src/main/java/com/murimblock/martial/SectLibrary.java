package com.murimblock.martial;

import com.murimblock.item.MurimItems;
import com.murimblock.world.structure.SanctuaryBuilder;
import com.murimblock.world.structure.SanctuaryPlacement;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The library of the Cloud Cliff Sanctuary: its lectern hands every player their own copy of the Azure Cloud Sword
 * Manual, once (docs/combat/styles/azure-cloud-sword.md). Sensei Yun, in the Misty Bamboo Temple, hands out the same
 * copy: a player receives one, from either.
 */
public final class SectLibrary {
    private SectLibrary() {
    }

    /** The library's lectern in this world, or null without a sanctuary. */
    @Nullable public static BlockPos lectern(ServerLevel overworld) {
        SanctuaryPlacement data = SanctuaryPlacement.get(overworld);
        if (data.centre() == null) return null;
        return SanctuaryBuilder.libraryLectern(data.centre(), data.top());
    }

    /** How far from the sanctuary's centre its bell tower stands, at most (blocks, horizontally). */
    static final double BELL_REACH = 48;

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().dimension() != Level.OVERWORLD) return;
        // Ringing the sanctuary's bell (it rings as usual): the first step of "Above the Sea of Clouds".
        if (player.level().getBlockState(event.getPos()).is(Blocks.BELL) && nearSanctuary(player.serverLevel(), event.getPos())) {
            MartialProgress.count(player, MartialArts.SEA_OF_CLOUDS_BELL, 1);
            return;
        }
        if (!player.level().getBlockState(event.getPos()).is(Blocks.LECTERN)) return;
        BlockPos lectern = lectern(player.serverLevel());
        if (lectern == null || !lectern.equals(event.getPos())) return;
        event.setCanceled(true);
        give(player);
    }

    /** Whether a block belongs to this world's sanctuary (its bell tower). */
    static boolean nearSanctuary(ServerLevel overworld, BlockPos pos) {
        BlockPos centre = SanctuaryPlacement.get(overworld).centre();
        if (centre == null) return false;
        double dx = pos.getX() - centre.getX(), dz = pos.getZ() - centre.getZ();
        return dx * dx + dz * dz <= BELL_REACH * BELL_REACH;
    }

    /** Gives the player their copy of the manual, if they never received one. */
    public static boolean give(ServerPlayer player) {
        return give(player, "art.murimblock.library.given", "art.murimblock.library.already");
    }

    /** The same, in the giver's own words (translation keys): when it is given, and when it was already. */
    public static boolean give(ServerPlayer player, String given, String already) {
        MartialData data = MartialService.data(player);
        if (data.manualsReceived().contains(MartialArts.AZURE_CLOUD_SWORD)) {
            player.displayClientMessage(Component.translatable(already), true);
            return false;
        }
        MartialService.set(player, data.receivedManual(MartialArts.AZURE_CLOUD_SWORD));
        ItemStack manual = new ItemStack(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get());
        if (!player.getInventory().add(manual)) player.drop(manual, false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.translatable(given));
        return true;
    }
}
