package com.murimblock.combat.preview;

import com.murimblock.combat.CombatAttachments;
import com.murimblock.combat.CombatProfiles;
import com.murimblock.combat.MeleeCombatService;
import com.murimblock.qi.charge.QiChargeService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Server-only command preview. Deliberately contains no damage, movement or resource calls. */
public final class CombatPreviewService {
    private static final Map<UUID, WeaponBinding> BINDINGS = new HashMap<>();
    private static final int SNAPSHOT_INTERVAL = 4;

    private CombatPreviewService() { }

    public static PreviewState getData(Player player) {
        return player.getData(CombatAttachments.PLAYER_PREVIEW);
    }

    public static boolean canPreview(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isUsingItem()
                && !player.swinging
                && !player.isPassenger() && !player.isFallFlying() && !player.isSwimming() && !player.isSleeping()
                && player.getOffhandItem().isEmpty() && CombatProfiles.basicFor(player.getMainHandItem()).isPresent()
                && !MeleeCombatService.getData(player).isActive(player.level().getGameTime())
                && !QiChargeService.isCharging(player);
    }

    public static boolean start(ServerPlayer player) {
        if (!canPreview(player) || getData(player).isActive(player.level().getGameTime())) return false;
        BINDINGS.put(player.getUUID(), new WeaponBinding(player));
        player.setData(CombatAttachments.PLAYER_PREVIEW, getData(player).begin(player.level().getGameTime()));
        return true;
    }

    public static boolean cancel(ServerPlayer player) {
        BINDINGS.remove(player.getUUID());
        if (!player.hasData(CombatAttachments.PLAYER_PREVIEW)) return false;
        PreviewState state = getData(player);
        if (!state.playing()) return false;
        player.setData(CombatAttachments.PLAYER_PREVIEW, state.stop(player.level().getGameTime()));
        return true;
    }

    public static void tick(ServerPlayer player) {
        if (!player.hasData(CombatAttachments.PLAYER_PREVIEW)) return;
        PreviewState state = getData(player);
        if (!state.playing()) return;
        long now = player.level().getGameTime();
        WeaponBinding binding = BINDINGS.get(player.getUUID());
        if (!state.isActive(now) || !canPreview(player) || binding == null || !binding.matches(player)) {
            cancel(player);
        } else if (now - state.sampledAt() >= SNAPSHOT_INTERVAL) {
            player.setData(CombatAttachments.PLAYER_PREVIEW, state.sample(now));
        }
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player);
    }

    public static void refreshTracking(ServerPlayer player) {
        tick(player);
        PreviewState state = getData(player);
        if (state.playing()) player.setData(CombatAttachments.PLAYER_PREVIEW, state.sample(player.level().getGameTime()));
    }

    public static void clearAll() {
        BINDINGS.clear();
    }

    private record WeaponBinding(int slot, ItemStack weapon, ResourceKey<Level> dimension, HumanoidArm arm) {
        WeaponBinding(ServerPlayer player) {
            this(player.getInventory().selected, player.getMainHandItem().copy(), player.level().dimension(), player.getMainArm());
        }

        boolean matches(ServerPlayer player) {
            return slot == player.getInventory().selected && ItemStack.matches(weapon, player.getMainHandItem())
                    && dimension.equals(player.level().dimension()) && arm == player.getMainArm();
        }
    }
}
