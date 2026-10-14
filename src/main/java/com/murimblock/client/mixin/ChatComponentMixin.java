package com.murimblock.client.mixin;

import com.murimblock.client.hud.HudScale;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The chat sits 40 pixels above the bottom of the screen, just over the hotbar. With a larger or smaller HUD size it
 * moves with the bottom HUD, so it never covers the combat bar or the Qi bar; its clicks follow it.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @ModifyConstant(method = "render", constant = @Constant(intValue = 40))
    private int murimblock$drawAboveHud(int margin) {
        return HudScale.chatMargin(margin);
    }

    @ModifyConstant(method = {"screenToChatY", "handleChatQueueClicked"}, constant = @Constant(doubleValue = 40.0))
    private double murimblock$clickAboveHud(double margin) {
        return HudScale.chatMargin((int) margin);
    }
}
