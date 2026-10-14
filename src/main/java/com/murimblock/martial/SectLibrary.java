package com.murimblock.martial;

import com.murimblock.item.MurimItems;
import com.murimblock.world.structure.SanctuaryPlacement;
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
 * The Azure Cloud Sword Manual's only source is Sensei Yun, in the Misty Bamboo Temple (decision of 14/10/2026): spoken
 * to, he hands every player their copy once; defeated, he may drop it. The Cloud Cliff Sanctuary's lectern gives nothing
 * any more. Its bell still counts for "Above the Sea of Clouds".
 */
public final class SectLibrary {
    private SectLibrary() {
    }

    /** How far from the sanctuary's centre its bell tower stands, at most (blocks, horizontally). */
    static final double BELL_REACH = 48;

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().dimension() != Level.OVERWORLD) return;
        // Ringing the sanctuary's bell (it rings as usual): the first step of "Above the Sea of Clouds".
        if (player.level().getBlockState(event.getPos()).is(Blocks.BELL) && nearSanctuary(player.serverLevel(), event.getPos())) {
            MartialProgress.count(player, MartialArts.SEA_OF_CLOUDS_BELL, 1);
        }
    }

    /** Whether a block belongs to this world's sanctuary (its bell tower). */
    static boolean nearSanctuary(ServerLevel overworld, BlockPos pos) {
        BlockPos centre = SanctuaryPlacement.get(overworld).centre();
        if (centre == null) return false;
        double dx = pos.getX() - centre.getX(), dz = pos.getZ() - centre.getZ();
        return dx * dx + dz * dz <= BELL_REACH * BELL_REACH;
    }

    /** Gives the player their copy of the manual, if they never received one, in the giver's own words (translation keys). */
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
