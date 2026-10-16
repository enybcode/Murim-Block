package com.colleuhu95.bloodsect;

import com.colleuhu95.bloodsect.ninja.BloodNinja;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BloodSectDecor.ID)
@PrefixGameTestTemplate(false)
public final class BloodDaoTests {
    private record Fixture(BloodNinja ninja, Cow victim, MeleeAttackGoal goal, Vec3 start) {
        void begin(GameTestHelper test) {
            // NoAI fixtures skip travel(), so initialise contact with the template's solid floor.
            ninja.setOnGround(true);
            ninja.setTarget(victim);
            goal.start(); goal.tick();
            test.assertTrue(ninja.isDaoAttacking(), "Cloud Step did not start: ground="+ninja.onGround()+", sight="+ninja.hasLineOfSight(victim)+", distance="+ninja.distanceTo(victim)+", position="+ninja.position()+", blockBelow="+ninja.level().getBlockState(ninja.blockPosition().below()));
        }
    }
    private static Cow victim(GameTestHelper test, int x, int z) {
        var cow=test.spawnWithNoFreeWill(EntityType.COW,x,2,z);
        cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        return cow;
    }
    private static Fixture fixture(GameTestHelper test) {
        var ninja=test.spawn(BloodSectDecor.BLOOD_NINJA.get(),2,2,4);
        ninja.setNoAi(true); ninja.setHealth(10);
        ninja.setGuardPost(ninja.blockPosition());
        var cow=victim(test,6,4);
        var goal=(MeleeAttackGoal)ninja.goalSelector.getAvailableGoals().stream().map(w->w.getGoal())
            .filter(g->g instanceof MeleeAttackGoal).findFirst().orElseThrow();
        return new Fixture(ninja,cow,goal,ninja.position());
    }
    @GameTest(template="patrol",timeoutTicks=65)
    public static void cloudStepTelegraphsThenDashesFiveBlocksAndDrainsOnce(GameTestHelper test) {
        var f=fixture(test);
        test.runAfterDelay(4,()->{
            float health=f.victim.getHealth(); f.begin(test);
            test.runAfterDelay(9,()->{
                test.assertTrue(f.ninja.position().distanceTo(f.start)<.1,"Ninja moved before the 0.5-second telegraph");
                test.assertTrue(f.victim.getHealth()==health && f.ninja.getHealth()==10,"Damage or healing during the telegraph");
            });
            test.runAfterDelay(14,()->{
                test.assertTrue(Math.abs(f.ninja.getX()-f.start.x-5)<.08,"Dash did not travel five blocks: "+f.ninja.position().subtract(f.start));
                test.assertTrue(health-f.victim.getHealth()==6,"Cloud Step must deal exactly six damage, once");
                test.assertTrue(Math.abs(f.ninja.getHealth()-13.6F)<.001F,"One successful dash must restore 15 percent of max health (3.6 HP)");
            });
            test.runAfterDelay(42,()->{
                test.assertTrue(health-f.victim.getHealth()==6 && Math.abs(f.ninja.getHealth()-13.6F)<.001F,"A finished dash repeated damage or healing");
                test.succeed();
            });
        });
    }
    @GameTest(template="patrol",timeoutTicks=55)
    public static void cloudStepHitsAtMostFourTargetsAndHealsOnlyFifteenPercent(GameTestHelper test) {
        var f=fixture(test);
        f.victim.moveTo(f.victim.getX()+1,f.victim.getY(),f.victim.getZ());
        var cows=new ArrayList<Cow>();
        for(int x=3;x<=6;x++) cows.add(victim(test,x,4));
        cows.add(f.victim);
        test.runAfterDelay(4,()->{
            f.begin(test);
            test.runAfterDelay(30,()->{
                long hit=cows.stream().filter(c->c.getHealth()==4).count();
                long unhurt=cows.stream().filter(c->c.getHealth()==10).count();
                test.assertTrue(hit==4 && unhurt==1,"Expected four distinct cuts and one untouched target; hit="+hit+", unhurt="+unhurt);
                test.assertTrue(Math.abs(f.ninja.getHealth()-13.6F)<.001F,"Multiple targets must not multiply the 15 percent drain");
                test.succeed();
            });
        });
    }
    @GameTest(template="patrol",timeoutTicks=55)
    public static void cloudStepCanBeDodgedAndDoesNotHealOnMiss(GameTestHelper test) {
        var f=fixture(test);
        test.runAfterDelay(4,()->{
            float hp=f.victim.getHealth();f.begin(test);
            test.runAfterDelay(5,()->f.victim.moveTo(f.victim.getX(),f.victim.getY(),f.victim.getZ()+3));
            test.runAfterDelay(35,()->{
                test.assertTrue(f.victim.getHealth()==hp,"Dash steered onto a target that dodged its telegraphed line");
                test.assertTrue(f.ninja.getHealth()==10,"Missed dash healed its caster"); test.succeed();
            });
        });
    }
    @GameTest(template="patrol",timeoutTicks=55)
    public static void cloudStepStopsAtWallsAndCannotDrainThroughThem(GameTestHelper test) {
        var f=fixture(test);
        test.runAfterDelay(4,()->{
            float hp=f.victim.getHealth();f.begin(test);
            test.runAfterDelay(5,()->{
                for(int z=3;z<=5;z++) for(int y=2;y<=4;y++) test.setBlock(new BlockPos(4,y,z),Blocks.STONE);
            });
            test.runAfterDelay(35,()->{
                test.assertTrue(f.ninja.getBoundingBox().maxX<=test.absolutePos(new BlockPos(4,2,4)).getX()+.01,"Dash passed through a wall");
                test.assertTrue(f.victim.getHealth()==hp && f.ninja.getHealth()==10,"Hit or drain through a wall");test.succeed();
            });
        });
    }
    @GameTest(template="patrol",timeoutTicks=55)
    public static void cloudStepLosingWeaponCancelsDashAndHealing(GameTestHelper test) {
        var f=fixture(test);
        test.runAfterDelay(4,()->{
            float hp=f.victim.getHealth();f.begin(test);
            test.runAfterDelay(5,()->f.ninja.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY));
            test.runAfterDelay(35,()->{
                test.assertTrue(f.ninja.position().distanceTo(f.start)<.1,"Disarmed ninja still dashed");
                test.assertTrue(f.victim.getHealth()==hp && f.ninja.getHealth()==10,"Disarmed ninja damaged or healed");test.succeed();
            });
        });
    }
    @GameTest(template="patrol",timeoutTicks=55)
    public static void cloudStepDoesNotHealFromAnInvulnerableTarget(GameTestHelper test) {
        var f=fixture(test);
        test.runAfterDelay(4,()->{
            f.begin(test);
            test.runAfterDelay(5,()->f.victim.setInvulnerable(true));
            test.runAfterDelay(35,()->{
                test.assertTrue(f.victim.getHealth()==10 && f.ninja.getHealth()==10,"Invulnerable target granted life steal");test.succeed();
            });
        });
    }
    @GameTest(template="patrol",timeoutTicks=55)
    public static void cloudStepCannotOverhealOrHurtAnotherBloodNinja(GameTestHelper test) {
        var f=fixture(test); f.ninja.setHealth(23);
        var ally=test.spawn(BloodSectDecor.BLOOD_NINJA.get(),4,2,4);ally.setNoAi(true);
        test.runAfterDelay(4,()->{
            f.begin(test);
            test.runAfterDelay(35,()->{
                test.assertTrue(f.ninja.getHealth()==24,"Life steal exceeded max health or missed its target");
                test.assertTrue(ally.getHealth()==24,"Cloud Step damaged an allied Blood Ninja");test.succeed();
            });
        });
    }
    @GameTest(template="patrol",timeoutTicks=155)
    public static void cloudStepCooldownLastsFiveAndAHalfSeconds(GameTestHelper test) {
        var f=fixture(test);
        test.runAfterDelay(4,()->f.begin(test));
        test.runAfterDelay(50,()->{
            f.ninja.moveTo(f.start);f.victim.setHealth(10);f.ninja.setTarget(f.victim);
            f.goal.start();f.goal.tick();
            test.assertTrue(!f.ninja.isDaoAttacking(),"Cloud Step restarted before its 5.5-second cooldown");
        });
        test.runAfterDelay(115,()->{ f.begin(test);test.succeed(); });
    }
    @GameTest(template="patrol",timeoutTicks=55)
    public static void cloudStepHealingScalesWithMaximumHealth(GameTestHelper test) {
        var f=fixture(test);
        f.ninja.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
        test.runAfterDelay(4,()->{
            f.begin(test);
            test.runAfterDelay(30,()->{
                test.assertTrue(Math.abs(f.ninja.getHealth()-16)<.001F,"Drain must restore 15 percent of max health, not a fixed amount: "+f.ninja.getHealth());
                test.assertTrue(!f.ninja.isDaoAttacking(),"Cloud Step recovery should end after 1.4 seconds");
                test.succeed();
            });
        });
    }
    @GameTest(template="patrol",timeoutTicks=50)
    public static void ninjaDaoIsNotDropped(GameTestHelper test) {
        var ninja=test.spawn(BloodSectDecor.BLOOD_NINJA.get(),4,2,4);ninja.setNoAi(true);
        var area=ninja.getBoundingBox().inflate(5);
        ninja.hurt(test.getLevel().damageSources().genericKill(),1000);
        test.runAfterDelay(25,()->{
            test.assertTrue(test.getLevel().getEntitiesOfClass(ItemEntity.class,area).stream().noneMatch(e->
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString().equals("bloodsect:blood_dao")),"Dao was dropped");
            test.succeed();
        });
    }
}
