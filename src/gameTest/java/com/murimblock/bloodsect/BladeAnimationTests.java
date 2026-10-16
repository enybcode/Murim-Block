package com.murimblock.bloodsect;

import com.murimblock.bloodsect.ninja.BladeAnimation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BloodSect.ID)
@PrefixGameTestTemplate(false)
public final class BladeAnimationTests {
    @GameTest(template = "blood_sect_empty", timeoutTicks = 20)
    public static void bladeFormsOnceAndRestartsWhenReequipped(GameTestHelper test) {
        var blade = new BladeAnimation();
        var item = new Object();
        blade.update(item, 100);
        test.assertTrue(blade.frame(100, 0, true) == 0, "Newly equipped blade did not start forming");
        test.assertTrue(blade.frame(106, 0, true) == 12, "Formation did not advance over time");
        blade.update(item, 107);
        test.assertTrue(blade.frame(112, 0, true) == 24, "Tracking the same item restarted formation");
        blade.update(null, 120);
        blade.update(item, 130);
        test.assertTrue(blade.frame(130, 0, true) == 0, "Reequipping did not restart formation");
        test.succeed();
    }

    @GameTest(template = "blood_sect_empty", timeoutTicks = 20)
    public static void bladeStrikeOverridesFormationOnlyForSwingingHand(GameTestHelper test) {
        var blade = new BladeAnimation();
        blade.update(new Object(), 100);
        test.assertTrue(blade.frame(106, .5F, true) == 52, "Strike did not take precedence over forming");
        test.assertTrue(blade.frame(106, .5F, false) == 12, "Other hand's swing stretched this blade");
        test.assertTrue(blade.frame(112, 0, true) == 24, "Blade did not settle after the strike");
        test.assertTrue(blade.frame(116, 0, true) != blade.frame(112, 0, true), "Resting blade stopped pulsing");
        test.assertTrue(blade.frame(144, 0, true) == 24, "Resting pulse is not periodic");
        test.succeed();
    }

    @GameTest(template = "blood_sect_empty", timeoutTicks = 20)
    public static void bladeEquipStateIsIndependentBetweenHolders(GameTestHelper test) {
        var first = new BladeAnimation();
        var second = new BladeAnimation();
        first.update(new Object(), 100);
        second.update(new Object(), 112);
        test.assertTrue(first.frame(112, 0, true) == 24 && second.frame(112, 0, true) == 0,
            "One holder restarted the other holder's animation");
        first.update(null, 115);
        test.assertTrue(first.frame(115, .5F, true) == 24, "Unequipped blade retained the attack state");
        test.assertTrue(second.frame(118, 0, true) == 12, "Clearing another holder interrupted formation");
        test.succeed();
    }
}
