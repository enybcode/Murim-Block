package com.murimblock.bloodsect;

import com.murimblock.bloodsect.ninja.BloodSoldier;
import com.murimblock.item.MurimItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Blood Soldier, the Blood Sect's middle rank: its look, its stats, the Blood Lacquer it wears and may drop. */
@GameTestHolder(BloodSect.ID)
@PrefixGameTestTemplate(false)
public final class BloodSoldierTests {
    private static BloodSoldier spawn(GameTestHelper test) {
        return BloodSect.BLOOD_SOLDIER.get().spawn(test.getLevel(), test.absolutePos(new BlockPos(4, 2, 4)), MobSpawnType.SPAWN_EGG);
    }

    /** It wears the whole set, which counts as armour; it is a grade above a cultist in life and attack. */
    @GameTest(template = "blood_sect_patrol", timeoutTicks = 20)
    public static void aSoldierWearsTheBloodLacquer(GameTestHelper test) {
        var s = spawn(test);
        test.assertTrue(s.getItemBySlot(EquipmentSlot.HEAD).is(MurimItems.BLOOD_LACQUER_MASK.get()), "No mask");
        test.assertTrue(s.getItemBySlot(EquipmentSlot.CHEST).is(MurimItems.BLOOD_LACQUER_CUIRASS.get()), "No cuirass");
        test.assertTrue(s.getItemBySlot(EquipmentSlot.LEGS).is(MurimItems.BLOOD_LACQUER_TASSETS.get()), "No tassets");
        test.assertTrue(s.getItemBySlot(EquipmentSlot.FEET).is(MurimItems.BLOOD_LACQUER_GREAVES.get()), "No greaves");
        test.assertTrue(s.getMaxHealth() == 40 && s.getAttributeValue(Attributes.ATTACK_DAMAGE) == 6, "Not a grade above a cultist");
        test.assertTrue(s.getArmour() == 0, "A soldier drew a cultist's light armour");
        // Its equipment's attributes apply on its next tick.
        test.runAfterDelay(2, () -> {
            test.assertTrue(s.getAttributeValue(Attributes.ARMOR) == 18, "Its armour counts " + s.getAttributeValue(Attributes.ARMOR) + ", not 18");
            test.assertTrue(s.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 4, "Its toughness is " + s.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            s.discard();
            test.succeed();
        });
    }

    /** Each piece may drop (its drop chance saved with it), the look is one of the three outfits and survives a save. */
    @GameTest(template = "blood_sect_patrol", timeoutTicks = 20)
    public static void aSoldiersLookAndArmourAreKept(GameTestHelper test) {
        var seen = new java.util.HashSet<Integer>();
        for (int i = 0; i < 30; i++) {
            var s = spawn(test);
            test.assertTrue(s.getOutfit() >= 0 && s.getOutfit() < BloodSoldier.SOLDIER_OUTFITS, "An outfit out of range: " + s.getOutfit());
            seen.add(s.getOutfit());
            var tag = new CompoundTag();
            s.saveWithoutId(tag);
            var chances = tag.getList("ArmorDropChances", Tag.TAG_FLOAT);
            for (int k = 0; k < 4; k++)
                test.assertTrue(Math.abs(chances.getFloat(k) - BloodSoldier.ARMOUR_DROP_CHANCE) < 1e-4, "Armour piece " + k + " drops at " + chances.getFloat(k));
            var copy = BloodSect.BLOOD_SOLDIER.get().create(test.getLevel());
            copy.load(tag);
            test.assertTrue(copy.getOutfit() == s.getOutfit(), "The outfit was not saved");
            test.assertTrue(copy.getItemBySlot(EquipmentSlot.CHEST).is(MurimItems.BLOOD_LACQUER_CUIRASS.get()), "The cuirass was not saved");
            s.discard();
            copy.discard();
        }
        test.assertTrue(seen.size() == BloodSoldier.SOLDIER_OUTFITS, "Thirty soldiers drew only " + seen);
        test.succeed();
    }
}
