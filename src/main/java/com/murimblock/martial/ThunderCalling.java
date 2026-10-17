package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.network.ThunderSaberPayloads;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

/**
 * How the Purple Heaven Thunder Saber is found (Enzo, 17/10/2026: "frappé par la foudre, au Qi Crystallization, avec un
 * pôle de cuivre et une grosse arme en main"): in a real thunderstorm, a player at Qi Crystallization or beyond stands by
 * a copper lightning rod open to the sky, a greatsword in hand. The storm turns to them; after {@link #CALL_TICKS} of
 * standing there, a bolt strikes the rod and runs through them, harmless, and the sky gives them the art's manual, once.
 * Below the realm, the storm passes them by and they are told so. The Purple Heaven Zhanmadao itself will be crafted
 * (its recipe is still Enzo's to choose).
 */
public final class ThunderCalling {
    private static final Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    /** The realm the storm answers from. */
    public static final CultivationRealm REALM = CultivationRealm.QI_CRYSTALLIZATION;
    /** How long the player must stand there, the storm gathering, before the bolt (6 s). */
    public static final int CALL_TICKS = 120;
    /** How near the rod (blocks, horizontally; a little below or above the feet). */
    public static final int ROD_REACH = 2;
    /** The player's saved mark: given the art by the storm. */
    static final String GIFT = Murimblock.MOD_ID + ":thunder_saber_gift";

    private ThunderCalling() {
    }

    /** Since when each player has been calling the storm (game time). */
    private static final Map<UUID, Long> CALLING = new HashMap<>();
    /** When each player was last told the storm passes them by (not every second). */
    private static final Map<UUID, Long> TOLD = new HashMap<>();

    /** A copper lightning rod near the player, open to the sky, or null. */
    @Nullable static BlockPos rodNear(ServerPlayer player) {
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(feet.offset(-ROD_REACH, -1, -ROD_REACH), feet.offset(ROD_REACH, 2, ROD_REACH)))
            if (level.getBlockState(p).is(Blocks.LIGHTNING_ROD) && level.canSeeSky(p.above())) return p.immutable();
        return null;
    }

    /** Whether the storm could answer this player now, the realm aside: thunder, outdoors, a greatsword, a rod. */
    static boolean calling(ServerPlayer player) {
        return player.level().dimension() == Level.OVERWORLD && (player.level().isThundering() || stormForTests) && player.isAlive()
                && player.level().canSeeSky(player.blockPosition().above()) && ThunderSaber.holdsSaber(player) && rodNear(player) != null;
    }

    public static boolean given(ServerPlayer player) {
        return player.getPersistentData().getBoolean(GIFT);
    }

    /** Every tick of a player: the storm gathering over them, then the bolt and the gift. */
    static void tick(ServerPlayer player) {
        if (player.tickCount % 5 != 0) return;
        UUID id = player.getUUID();
        if (given(player) || !calling(player)) { CALLING.remove(id); return; }
        long now = player.level().getGameTime();
        if (CultivationService.getRealm(player).ordinal() < REALM.ordinal()) {
            CALLING.remove(id);
            Long told = TOLD.get(id);
            if (told == null || now - told > 200) {
                TOLD.put(id, now);
                player.displayClientMessage(Component.translatable("message.murimblock.thunder_calling.silent", REALM.displayName()), true);
            }
            return;
        }
        Long since = CALLING.get(id);
        if (since == null) {
            CALLING.put(id, now);
            player.displayClientMessage(Component.translatable("message.murimblock.thunder_calling.gathers"), true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.6F, 0.6F);
            return;
        }
        if (now - since >= CALL_TICKS) {
            CALLING.remove(id);
            strike(player);
        }
    }

    /** The bolt strikes the rod and runs through the player, harmless; the manual is given, and the mark. */
    static void strike(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos rod = rodNear(player);
        Vec3 at = rod != null ? Vec3.atBottomCenterOf(rod.above()) : player.position();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at);
            // Only its light and its thunder: the storm chose them, it does not burn them.
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        ThunderSaberMoves.fx(player, ThunderSaberPayloads.Fx.BOLT, player.getId(), List.of(at.add(0, 26, 0), player.position()), List.of(), 22);
        ThunderSaberMoves.fx(player, ThunderSaberPayloads.Fx.SHOCKWAVE, -1, List.of(player.position()), List.of(1), 35);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ThunderSaberPayloads.Electrified(player.getId(), 100));
        give(player);
    }

    static void give(ServerPlayer player) {
        player.getPersistentData().putBoolean(GIFT, true);
        ItemStack manual = new ItemStack(ThunderSaberItems.MANUAL.get());
        if (!player.getInventory().add(manual)) player.drop(manual, false);
        player.displayClientMessage(Component.translatable("message.murimblock.thunder_calling.gift"), false);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.8F);
        LOGGER.info("{} was given the Purple Heaven Thunder Saber by the storm", player.getGameProfile().getName());
    }

    static void forget(@Nullable UUID player) {
        if (player == null) { CALLING.clear(); TOLD.clear(); return; }
        CALLING.remove(player);
        TOLD.remove(player);
    }

    /** For tests: a storm for the calling alone (the real weather would double every test's beats). */
    public static boolean stormForTests;

    /** For tests: forget that a player was given it. */
    public static void resetForTests(ServerPlayer player) {
        player.getPersistentData().remove(GIFT);
        forget(player.getUUID());
    }
}
