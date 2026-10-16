package com.colleuhu95.bloodsect;

import com.colleuhu95.bloodsect.ninja.BloodNinja;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BloodSectDecor.ID)
@PrefixGameTestTemplate(false)
public final class BloodDaoM1Tests {
    private record Fixture(BloodNinja ninja, Cow cow, MeleeAttackGoal goal, Vec3 start) {}
    // First spend the special normally. M1 must be available during its cooldown.
    private static void withM1(GameTestHelper test, java.util.function.Consumer<Fixture> check) {
        var ninja=test.spawn(BloodSectDecor.BLOOD_NINJA.get(),2,2,4);
        ninja.setNoAi(true);ninja.setHealth(10);ninja.setGuardPost(ninja.blockPosition());
        var cow=test.spawnWithNoFreeWill(EntityType.COW,6,2,4);
        cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        var goal=(MeleeAttackGoal)ninja.goalSelector.getAvailableGoals().stream().map(w->w.getGoal())
            .filter(g->g instanceof MeleeAttackGoal).findFirst().orElseThrow();
        var start=ninja.position();
        test.runAfterDelay(4,()->{
            ninja.setOnGround(true);ninja.setTarget(cow);goal.start();goal.tick();
            test.assertTrue(ninja.isDaoAttacking() && ninja.getDaoAttackVariant()==0,"Special did not start");
        });
        test.runAfterDelay(50,()->{
            ninja.moveTo(start);ninja.setOnGround(true);ninja.setHealth(10);cow.setHealth(10);
            cow.moveTo(start.add(1.7,0,0));ninja.setTarget(cow);goal.start();goal.tick();
            test.assertTrue(ninja.isDaoAttacking() && ninja.getDaoAttackVariant()==1,"Close-range M1 unavailable during Cloud Step cooldown");
            check.accept(new Fixture(ninja,cow,goal,start));
        });
    }
    @GameTest(template="patrol",timeoutTicks=90)
    public static void m1TelegraphsCutsOnceAndDoesNotDrainOrDash(GameTestHelper test) {
        withM1(test,f->{
            test.runAfterDelay(7,()->test.assertTrue(f.cow.getHealth()==10,"M1 injured target before its visible cut"));
            test.runAfterDelay(9,()->{
                test.assertTrue(f.cow.getHealth()==6,"M1 must deal four damage at the cut");
                test.assertTrue(f.ninja.getHealth()==10,"M1 incorrectly inherited life steal");
                test.assertTrue(f.ninja.position().distanceTo(f.start)<.1,"M1 incorrectly inherited dash movement");
            });
            test.runAfterDelay(20,()->{
                test.assertTrue(f.cow.getHealth()==6 && f.ninja.getHealth()==10,"M1 repeated damage or healing");
                test.assertTrue(!f.ninja.isDaoAttacking(),"M1 did not return to rest");test.succeed();
            });
        });
    }
    @GameTest(template="patrol",timeoutTicks=90)
    public static void m1CanBeSidestepped(GameTestHelper test) {
        withM1(test,f->{
            test.runAfterDelay(3,()->f.cow.moveTo(f.start.add(0,0,2)));
            test.runAfterDelay(25,()->{test.assertTrue(f.cow.getHealth()==10,"M1 turned onto a target outside its committed arc");test.succeed();});
        });
    }
    @GameTest(template="patrol",timeoutTicks=90)
    public static void m1CannotCutThroughAWall(GameTestHelper test) {
        withM1(test,f->{
            test.runAfterDelay(3,()->{
                // Keep the cow clear of the new solid wall so suffocation cannot masquerade as a sword hit.
                f.cow.moveTo(f.start.add(2.4,0,0));
                for(int y=2;y<=4;y++)for(int z=3;z<=5;z++)test.setBlock(new BlockPos(3,y,z),Blocks.STONE);
                test.assertTrue(!test.getLevel().getBlockCollisions(f.cow,f.cow.getBoundingBox()).iterator().hasNext(),"Wall fixture intersects victim");
                test.assertTrue(!f.ninja.hasLineOfSight(f.cow),"Wall fixture does not block sight");
            });
            test.runAfterDelay(25,()->{test.assertTrue(f.cow.getHealth()==10,"M1 cut through a wall: victim health="+f.cow.getHealth());test.succeed();});
        });
    }
    @GameTest(template="patrol",timeoutTicks=90)
    public static void m1DisarmCancelsDamage(GameTestHelper test) {
        withM1(test,f->{
            test.runAfterDelay(3,()->f.ninja.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY));
            test.runAfterDelay(25,()->{test.assertTrue(f.cow.getHealth()==10,"Disarmed ninja still cut its target");test.succeed();});
        });
    }
    @GameTest(template="patrol",timeoutTicks=90)
    public static void m1RejectsAnInvulnerableVictim(GameTestHelper test) {
        withM1(test,f->{
            test.runAfterDelay(3,()->f.cow.setInvulnerable(true));
            test.runAfterDelay(25,()->{test.assertTrue(f.cow.getHealth()==10 && f.ninja.getHealth()==10,"Invulnerable victim was damaged or granted healing");test.succeed();});
        });
    }
    @GameTest(template="patrol",timeoutTicks=155)
    public static void m1CannotSpamOrResetCloudStepCooldown(GameTestHelper test) {
        withM1(test,f->{
            test.runAfterDelay(21,()->{
                f.cow.setHealth(10);f.goal.tick();
                test.assertTrue(!f.ninja.isDaoAttacking(),"M1 restarted before its recovery cooldown");
            });
            test.runAfterDelay(24,()->{
                f.goal.tick();test.assertTrue(f.ninja.isDaoAttacking() && f.ninja.getDaoAttackVariant()==1,"M1 failed to restart after recovery");
            });
            test.runAfterDelay(77,()->{
                f.cow.setHealth(10);f.cow.moveTo(f.start.add(4,0,0));f.goal.tick();
                test.assertTrue(f.ninja.isDaoAttacking() && f.ninja.getDaoAttackVariant()==0,"M1 reset or blocked the original special cooldown");test.succeed();
            });
        });
    }
}
