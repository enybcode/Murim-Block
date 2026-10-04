package com.murimblock.combat.preview;

import com.murimblock.Murimblock;
import com.murimblock.combat.attack.AttackTimeline;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

/** Original test motion, not an attack definition and never a damage source. */
public final class PreviewDefinition {
    public static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "sword_preview");
    public static final AttackTimeline TIMELINE = new AttackTimeline(8, 4, 12);
    public static final int DURATION_TICKS = 24;

    private PreviewDefinition() { }

    public static String weaponSocket(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? "right_item" : "left_item";
    }
}
