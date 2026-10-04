package com.murimblock.client;

import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.combat.MeleeCombatService;
import com.murimblock.combat.MeleeData;
import com.murimblock.combat.CombatProfiles;
import com.murimblock.network.GuardStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SwordCombatClientHandler {
    private static boolean sentGuard;
    private static Player lastPlayer;

    private SwordCombatClientHandler() { }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != lastPlayer) {
            sentGuard = false;
            lastPlayer = minecraft.player;
        }
        boolean guard = minecraft.player != null && minecraft.screen == null
                && minecraft.isWindowActive()
                && MeleeCombatService.canUseSwordCombat(minecraft.player) && minecraft.options.keyUse.isDown();
        if ((guard != sentGuard || (guard && minecraft.player.tickCount % MeleeCombatService.GUARD_REFRESH_TICKS == 0)) && minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new GuardStatePayload(guard));
            sentGuard = guard;
        }
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !MeleeCombatService.canUseSwordCombat(minecraft.player)) {
            return;
        }
        if (event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        } else if (event.isAttack() && MeleeCombatService.getData(minecraft.player).locksAttack(minecraft.level.getGameTime())) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onMovement(MovementInputUpdateEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player && MeleeCombatService.canUseSwordCombat(event.getEntity())
                && MeleeCombatService.getData(event.getEntity()).isGuarding()) {
            event.getEntity().setSprinting(false);
        }
    }

    @SubscribeEvent
    public static void onPlayerRender(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        MeleeData data = MeleeCombatService.getData(player);
        // Combat Mode is owner-only; observers use the server-synchronized action instead.
        if (!player.isAlive() || player.isSpectator() || player.isUsingItem() || !player.getOffhandItem().isEmpty()
                || CombatProfiles.basicFor(player.getMainHandItem()).isEmpty()
                || !data.isActive(player.level().getGameTime()) || !data.isGuarding()) {
            return;
        }
        var model = event.getRenderer().getModel();
        if (player.getMainArm() == HumanoidArm.RIGHT) {
            model.rightArmPose = HumanoidModel.ArmPose.BLOCK;
        } else {
            model.leftArmPose = HumanoidModel.ArmPose.BLOCK;
        }
    }

    @SubscribeEvent
    public static void onHandRender(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || event.getHand() != InteractionHand.MAIN_HAND || !MeleeCombatService.canUseSwordCombat(player)
                || !ItemStack.isSameItem(player.getMainHandItem(), event.getItemStack())) {
            return;
        }
        MeleeData data = MeleeCombatService.getData(player);
        if (!data.isActive(player.level().getGameTime())) {
            return;
        }
        float age = player.level().getGameTime() - data.startedAt() + event.getPartialTick();
        float side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (!data.isGuarding() && data.action() != MeleeData.Action.GUARD_BREAK) {
            return;
        }
        event.setCanceled(true);
        event.getPoseStack().pushPose();
        try {
            if (data.isGuarding()) {
                float blend = data.action() == MeleeData.Action.GUARD_IMPACT ? 1 : Math.min(1, Math.max(0, age / 3));
                float recoil = data.action() == MeleeData.Action.GUARD_IMPACT
                        ? (float) Math.sin(Math.min(1, age / Math.max(1, data.endsAt() - data.startedAt())) * Math.PI) : 0;
                event.getPoseStack().translate(-0.35F * side * blend, 0.16F * blend, 0.10F * blend + 0.10F * recoil);
                event.getPoseStack().mulPose(Axis.YP.rotationDegrees(35 * side * blend));
                event.getPoseStack().mulPose(Axis.ZP.rotationDegrees((55 * blend + 12 * recoil) * side));
            } else if (data.action() == MeleeData.Action.GUARD_BREAK) {
                float recoil = (float) Math.sin(Math.min(1, age / (data.endsAt() - data.startedAt())) * Math.PI);
                event.getPoseStack().translate(0, -0.2F * recoil, 0.15F * recoil);
            }
            Minecraft.getInstance().gameRenderer.itemInHandRenderer.renderArmWithItem(
                    Minecraft.getInstance().player, event.getPartialTick(), event.getInterpolatedPitch(), event.getHand(),
                    0, event.getItemStack(), event.getEquipProgress(), event.getPoseStack(),
                    event.getMultiBufferSource(), event.getPackedLight());
        } finally {
            event.getPoseStack().popPose();
        }
    }
}
