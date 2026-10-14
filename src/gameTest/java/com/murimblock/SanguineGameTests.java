package com.murimblock;

import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.*;
import com.murimblock.qi.QiService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import static com.murimblock.testing.GameTestPlayers.player;
import static com.murimblock.testing.GameTestPlayers.cleanup;

/** Actual server animations, collision, damage and combat-state transitions. */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SanguineGameTests {
    private static GameTestHelper arenaOwner;
    private static int participants;
    private static final java.util.Map<java.util.UUID,java.util.List<net.minecraft.world.entity.Entity>> OWNED=new java.util.HashMap<>();
    private static ServerPlayer adept(GameTestHelper h) {
        var p=player(h);EpicFightCombatDefaults.enforce(p);
        new ItemStack(MurimItems.SANGUINE_ART_SCROLL.get()).use(p.level(),p,InteractionHand.MAIN_HAND);
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        // Keep contacts in the already simulated spawn chunks; new distant chunks load their entity sections asynchronously.
        if(arenaOwner!=h){arenaOwner=h;participants=0;}
        double x=6.5+6*participants++;int chunkX=(int)x>>4;
        var owned=new java.util.ArrayList<net.minecraft.world.entity.Entity>();owned.add(p);
        var floor=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int dx=-2;dx<=2;dx++)for(int dz=0;dz<=10;dz++) {
            var pos=BlockPos.containing(x+dx,119,8.5+dz);floor.put(pos,h.getLevel().getBlockState(pos));
            h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        }
        var forced=new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int cx=chunkX-1;cx<=chunkX+1;cx++)for(int cz=-1;cz<=1;cz++) {
            if(!h.getLevel().getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(cx,cz))) {
                forced.add(new net.minecraft.world.level.ChunkPos(cx,cz));h.getLevel().setChunkForced(cx,cz,true);
            }
            h.getLevel().getChunk(cx,cz);
        }
        h.testInfo.addListener(new net.minecraft.gametest.framework.GameTestListener() {
            private void clear() {
                for(var entity:owned) {
                    if(entity instanceof ServerPlayer player) {
                        MartialCombat.forget(player);
                        if(player.server.getPlayerList().getPlayer(player.getUUID())!=null)cleanup(player);
                    } else entity.discard();
                }
                floor.forEach((pos,state)->h.getLevel().setBlockAndUpdate(pos,state));
                for(var chunk:forced)h.getLevel().setChunkForced(chunk.x,chunk.z,false);
                OWNED.remove(p.getUUID());
            }
            public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo test) { }
            public void testPassed(net.minecraft.gametest.framework.GameTestInfo test,net.minecraft.gametest.framework.GameTestRunner runner) {clear();}
            public void testFailed(net.minecraft.gametest.framework.GameTestInfo test,net.minecraft.gametest.framework.GameTestRunner runner) {clear();}
            public void testAddedForRerun(net.minecraft.gametest.framework.GameTestInfo old,net.minecraft.gametest.framework.GameTestInfo next,net.minecraft.gametest.framework.GameTestRunner runner) { }
        });
        OWNED.put(p.getUUID(),owned);p.moveTo(x,120,8.5,0,0);
        face(p,0);p.setNoGravity(false);p.setOnGround(true);p.setHealth(10);p.getFoodData().setFoodLevel(6);
        // Settle the held-item capabilities before starting a charge or a passive M1.
        p.doTick();p.doTick();
        MartialService.clearCooldownsForTests(p);QiService.setQi(p,100);return p;
    }
    private static void face(ServerPlayer p,float yaw) {
        p.setYRot(yaw);p.setYHeadRot(yaw);p.yHeadRotO=yaw;p.setYBodyRot(yaw);p.yBodyRotO=yaw;p.yRotO=yaw;
        var patch=EpicFightBridge.patch(p);patch.setYRot(yaw);patch.setYRotO(yaw);patch.setModelYRot(yaw,false);
    }
    private static Zombie target(GameTestHelper h,ServerPlayer p,double dx,double dz) {
        var z=EntityType.ZOMBIE.create(h.getLevel());z.setNoAi(true);z.setNoGravity(true);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);z.setHealth(500);
        z.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));
        z.moveTo(p.getX()+dx,p.getY(),p.getZ()+dz,180,0);h.getLevel().addFreshEntity(z);OWNED.get(p.getUUID()).add(z);return z;
    }
    private static void ticks(GameTestHelper h,ServerPlayer p,int count) {
        for(int t=1;t<=count;t++)h.runAfterDelay(t,p::doTick);
    }
    private static void cast(GameTestHelper h,ServerPlayer p,int slot) {
        var result=MartialService.castSlot(p,slot);h.assertTrue(result==MartialService.CastResult.CAST,"Cast refused: "+result);
    }
    private static void end(GameTestHelper h,ServerPlayer p,Zombie... targets) {
        for(var z:targets)z.discard();MartialCombat.forget(p);cleanup(p);h.succeed();
    }
    private static void form(GameTestHelper h,ServerPlayer p) {
        MartialService.unlock(p,MartialArts.BLOOD_CRYSTAL_FORM);MartialService.assign(p,2,MartialArts.BLOOD_CRYSTAL_FORM.id());
        p.setData(BloodCrystalAttachments.STATE,new BloodCrystalState(10,false,0));cast(h,p,2);
    }
    private static void strike(ServerPlayer p) {face(p,0);EpicFightBridge.patch(p).playAnimationSynchronized(MurimAnimations.SANGUINE_STRAIGHT,0);}
    private static void idle(ServerPlayer p) {EpicFightBridge.patch(p).playAnimationSynchronized(Animations.BIPED_IDLE,0);}
    @GameTest(template="foundation_arena",batch="crystal_rules")
    public static void scrollGuardM1AndBothEmptyHands(GameTestHelper h) {
        var p=adept(h);var patch=EpicFightBridge.patch(p);
        var chain=patch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getAutoAttackMotion(patch);
        h.assertTrue(chain.getFirst()==MurimAnimations.SANGUINE_STRAIGHT&&chain.get(3)==MurimAnimations.SANGUINE_RISE,"Wrong M1 chain");
        h.assertTrue(MurimSkills.MURIM_GUARD.get().canExecute(patch.getSkill(SkillSlots.GUARD)),"No bare hand guard");
        for(var hand:InteractionHand.values()) {
            p.setItemInHand(hand,new ItemStack(Items.STICK));
            h.assertTrue(MartialService.castSlot(p,0)==MartialService.CastResult.NO_WEAPON,"Occupied hand accepted");
            h.assertTrue(QiService.getQi(p)==100,"Refused cast spent Qi");p.setItemInHand(hand,ItemStack.EMPTY);
        }
        end(h,p);
    }
    @GameTest(template="foundation_arena",batch="crystal_charge",timeoutTicks=85)
    public static void chargeGrowsThenARealProjectileHitsWithoutHealing(GameTestHelper h) {
        var p=adept(h);var z=target(h,p,0,7);p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(12);
        h.assertTrue(MartialService.aim(p,0),"Charge refused");ticks(h,p,70);
        h.runAfterDelay(10,()->{
            var crystals=h.getLevel().getEntitiesOfClass(BloodCrystalEntity.class,p.getBoundingBox().inflate(3));
            h.assertTrue(crystals.size()==1&&crystals.getFirst().crystalScale()>.35,"Hand crystal did not grow: "+crystals.stream().map(e->e.state()+":"+e.crystalScale()).toList()+", has="+SanguineArt.hasCharge(p)+", held="+SanguineArt.charging(p)+", pos="+p.position());
            h.assertTrue(z.getHealth()==500&&QiService.getQi(p)==100,"Holding dealt damage or spent Qi");cast(h,p,0);
        });
        h.runAfterDelay(65,()->{
            h.assertTrue(z.getHealth()<500,"Thrown crystal missed: "+z.getHealth());
            h.assertTrue(p.getHealth()==10,"Crystal healed its caster");h.assertTrue(QiService.getQi(p)>=82&&QiService.getQi(p)<83,"Wrong Qi cost: "+QiService.getQi(p));end(h,p,z);
        });
    }
    private static BloodCrystalEntity held(GameTestHelper h,ServerPlayer p) {
        return h.getLevel().getEntitiesOfClass(BloodCrystalEntity.class,p.getBoundingBox().inflate(5)).stream()
                .filter(c->c.state()==BloodCrystalEntity.CHARGING).findFirst().orElse(null);
    }
    @GameTest(template="foundation_arena",batch="crystal_draw",timeoutTicks=100)
    public static void holdingDrawsTheGaugeIntoTheCrystal(GameTestHelper h) {
        var p=adept(h);var z=target(h,p,0,7);var side=target(h,p,2.2,7);
        p.setData(BloodCrystalAttachments.STATE,new BloodCrystalState(6,false,0));
        h.assertTrue(MartialService.aim(p,0),"Charge refused");ticks(h,p,90);
        h.runAfterDelay(45,()->{
            var c=held(h,p);
            h.assertTrue(SanguineArt.state(p).gauge()==0,"The charge did not draw the gauge: "+SanguineArt.state(p));
            h.assertTrue(c!=null&&c.points()==6&&!c.great()&&c.crystalScale()>2.5F&&c.crystalScale()<2.8F,
                    "The crystal does not hold six points: "+(c==null?"none":c.points()+" points, scale "+c.crystalScale()));
            cast(h,p,0);
        });
        h.runAfterDelay(85,()->{
            h.assertTrue(z.getHealth()<500,"The charged crystal missed: "+z.getHealth());
            h.assertTrue(side.getHealth()==500,"A crystal of six points exploded: "+side.getHealth());end(h,p,z,side);
        });
    }
    @GameTest(template="foundation_arena",batch="crystal_great",timeoutTicks=125)
    public static void aFullGaugeMakesAGreatCrystalThatExplodesWithoutBreakingBlocks(GameTestHelper h) {
        var p=adept(h);var z=target(h,p,0,8);var near=target(h,p,1.8,9.2);
        p.setData(BloodCrystalAttachments.STATE,new BloodCrystalState(10,false,0));
        h.assertTrue(MartialService.aim(p,0),"Charge refused");ticks(h,p,115);
        h.runAfterDelay(56,()->{
            var c=held(h,p);
            h.assertTrue(c!=null&&c.great()&&Math.abs(c.crystalScale()-4F)<.01F&&SanguineArt.state(p).gauge()==0,
                    "No great crystal of ten points: "+(c==null?"none":c.points()+" points, scale "+c.crystalScale())+", "+SanguineArt.state(p));
            h.assertTrue(Math.abs(c.getBbWidth()-3)<.01,"The great crystal is not 3 blocks across: "+c.getBbWidth());
        });
        h.runAfterDelay(110,()->{
            h.assertTrue(z.getHealth()<500,"The great crystal missed: "+z.getHealth());
            h.assertTrue(near.getHealth()<500,"The explosion spared the body beside it: "+near.getHealth());
            h.assertTrue(z.getHealth()<near.getHealth(),"The body struck took less than the one beside: "+z.getHealth()+" / "+near.getHealth());
            for(int dx=-2;dx<=2;dx++)for(int dz=0;dz<=10;dz++)
                h.assertTrue(h.getLevel().getBlockState(BlockPos.containing(p.getX()+dx,119,8.5+dz)).is(Blocks.STONE),"The explosion broke a block");
            h.assertTrue(QiService.getQi(p)>=82&&QiService.getQi(p)<83,"Wrong Qi cost: "+QiService.getQi(p));end(h,p,z,near);
        });
    }
    @GameTest(template="foundation_arena",batch="scarlet_sword",timeoutTicks=110)
    public static void scarletTearSwingsItsBloodSwordThenThrowsItToExplode(GameTestHelper h) {
        var p=adept(h);var near=target(h,p,0,1.6);var far=target(h,p,0,7);far.setNoGravity(true);
        MartialService.unlock(p,MartialArts.SCARLET_TEAR);MartialService.assign(p,1,MartialArts.SCARLET_TEAR.id());
        h.runAfterDelay(2,()->cast(h,p,1));ticks(h,p,100);
        h.runAfterDelay(10,()->h.assertTrue(SanguineArt.holdsSword(p),"No blood sword forming in the hand"));
        // The arc lands 0.75 to 0.9 s in, on the body in reach only.
        h.runAfterDelay(24,()->{
            h.assertTrue(near.getHealth()<500,"The arc missed the body in reach: "+near.getHealth());
            h.assertTrue(far.getHealth()==500,"The arc reached 7 blocks: "+far.getHealth());
            h.assertTrue(SanguineArt.state(p).gauge()==3,"The arc did not build three gauge points: "+SanguineArt.state(p));
            near.discard();
        });
        // Thrown at 1.35 s, the sword explodes on the body it is aimed at.
        h.runAfterDelay(95,()->{
            h.assertTrue(!SanguineArt.holdsSword(p),"The sword was never thrown");
            h.assertTrue(far.getHealth()<500,"The thrown sword missed: "+far.getHealth());end(h,p,far);
        });
    }
    @GameTest(template="foundation_arena",batch="crystal_wall",timeoutTicks=90)
    public static void projectileStopsAtTheWall(GameTestHelper h) {
        var p=adept(h);var z=target(h,p,0,7);var wall=BlockPos.containing(p.getX(),120,p.getZ()+3);
        for(int x=-1;x<=1;x++)for(int y=0;y<3;y++)h.getLevel().setBlockAndUpdate(wall.offset(x,y,0),Blocks.STONE.defaultBlockState());
        cast(h,p,0);ticks(h,p,70);
        h.runAfterDelay(65,()->{h.assertTrue(z.getHealth()==500,"Crystal passed through stone");
            for(int x=-1;x<=1;x++)for(int y=0;y<3;y++)h.getLevel().setBlockAndUpdate(wall.offset(x,y,0),Blocks.AIR.defaultBlockState());end(h,p,z);});
    }
    @GameTest(template="foundation_arena",batch="crystal_interrupt",timeoutTicks=65)
    public static void anInterruptedChargeCannotThrowLater(GameTestHelper h) {
        var p=adept(h);var z=target(h,p,0,5);cast(h,p,0);ticks(h,p,45);
        h.runAfterDelay(5,()->idle(p));h.runAfterDelay(40,()->{
            h.assertTrue(z.getHealth()==500&&!SanguineArt.hasCharge(p),"Interrupted charge still threw");end(h,p,z);});
    }
    @GameTest(template="foundation_arena",batch="crystal_cancel",timeoutTicks=95)
    public static void aFailedAutomaticReleaseUnlocksTheHeldPose(GameTestHelper h) {
        var p=adept(h);h.assertTrue(MartialService.aim(p,0),"Charge refused");ticks(h,p,80);
        h.runAfterDelay(8,()->QiService.setQi(p,0));
        // The crystal leaves on its own after 3 s; without Qi that release fails.
        h.runAfterDelay(75,()->{
            h.assertTrue(!SanguineArt.hasCharge(p)&&!SanguineArt.charging(p),"Failed release left the player in the charge animation");
            h.assertTrue(!EpicFightBridge.patch(p).getEntityState().inaction(),"Failed release kept actions locked");end(h,p);
        });
    }
    @GameTest(template="foundation_arena",batch="crystal_gauge",timeoutTicks=90)
    public static void oneM1AndOneScarletBuildOneAndThreePointsAcrossSeveralTargets(GameTestHelper h) {
        var p=adept(h);var z=target(h,p,0,1.3);var second=target(h,p,.25,1.3);
        MartialService.unlock(p,MartialArts.SCARLET_TEAR);MartialService.assign(p,1,MartialArts.SCARLET_TEAR.id());
        h.runAfterDelay(5,()->strike(p));ticks(h,p,75);
        h.runAfterDelay(22,()->{h.assertTrue(SanguineArt.state(p).gauge()==1,"Multi-target M1 multiplied gauge: "+SanguineArt.state(p)+", targetHealth="+z.getHealth()+", pos="+p.position()+", target="+z.position()+", second="+second.position());idle(p);cast(h,p,1);});
        h.runAfterDelay(70,()->{h.assertTrue(SanguineArt.state(p).gauge()==4,"Scarlet did not add exactly three: "+SanguineArt.state(p));end(h,p,z,second);});
    }
    @GameTest(template="foundation_arena",batch="crystal_manual",timeoutTicks=680)
    public static void fullGaugeNeedsTheKeyAndFormLastsBeyondThirtySeconds(GameTestHelper h) {
        var p=adept(h);MartialService.unlock(p,MartialArts.BLOOD_CRYSTAL_FORM);MartialService.assign(p,2,MartialArts.BLOOD_CRYSTAL_FORM.id());
        p.setData(BloodCrystalAttachments.STATE,new BloodCrystalState(9,false,0));
        h.assertTrue(MartialService.castSlot(p,2)==MartialService.CastResult.FAILED&&QiService.getQi(p)==100,"Partial gauge transformed");
        p.setData(BloodCrystalAttachments.STATE,new BloodCrystalState(10,false,0));
        h.assertTrue(!SanguineArt.state(p).active(),"Gauge transformed automatically");cast(h,p,2);ticks(h,p,650);
        h.runAfterDelay(640,()->{h.assertTrue(SanguineArt.state(p).active()&&SanguineArt.state(p).crystals()==0,"Form expired on a timer");end(h,p);});
    }
    @GameTest(template="foundation_arena",batch="crystal_stacks",timeoutTicks=555)
    public static void landedM1EarnTenCrystalsAndBarrageConsumesThemButKeepsForm(GameTestHelper h) {
        var p=adept(h);var z=target(h,p,0,1.3);form(h,p);ticks(h,p,525);
        MartialService.unlock(p,MartialArts.CRYSTAL_BARRAGE);MartialService.assign(p,3,MartialArts.CRYSTAL_BARRAGE.id());
        for(int i=0;i<20;i++)h.runAfterDelay(35+i*22,()->{z.invulnerableTime=0;strike(p);});
        h.runAfterDelay(475,()->{
            h.assertTrue(SanguineArt.state(p).crystals()==10,"M1 stack cap failed: "+SanguineArt.state(p));idle(p);
            QiService.setQi(p,100);cast(h,p,3);
            h.assertTrue(SanguineArt.state(p).active()&&SanguineArt.state(p).crystals()==0,"Salvo ended form or did not consume crystals");
        });
        h.runAfterDelay(520,()->{h.assertTrue(SanguineArt.state(p).active(),"Form ended after salvo");end(h,p,z);});
    }
    @GameTest(template="foundation_arena",batch="crystal_damage",timeoutTicks=365)
    public static void fiveRapidCrystalsDealMoreDamageThanOne(GameTestHelper h) {
        var one=adept(h);var five=adept(h);var a=target(h,one,0,1.3);var b=target(h,five,0,1.3);
        one.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(12);five.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(12);
        form(h,one);form(h,five);ticks(h,one,340);ticks(h,five,340);
        for(var p:new ServerPlayer[]{one,five}){MartialService.unlock(p,MartialArts.CRYSTAL_BARRAGE);MartialService.assign(p,3,MartialArts.CRYSTAL_BARRAGE.id());}
        for(int i=0;i<10;i++)h.runAfterDelay(35+i*22,()->{
            if(SanguineArt.state(one).crystals()<1)strike(one);
            if(SanguineArt.state(five).crystals()<5)strike(five);
        });
        h.runAfterDelay(270,()->{
            h.assertTrue(SanguineArt.state(one).crystals()==1&&SanguineArt.state(five).crystals()==5,"Wrong fixture stack counts");
            a.moveTo(one.getX(),one.getY(),one.getZ()+7);b.moveTo(five.getX(),five.getY(),five.getZ()+7);
            a.setHealth(500);b.setHealth(500);a.invulnerableTime=b.invulnerableTime=0;idle(one);idle(five);
            cast(h,one,3);cast(h,five,3);
        });
        h.runAfterDelay(335,()->{
            float lossA=500-a.getHealth(),lossB=500-b.getHealth();h.assertTrue(lossA>0&&lossB>lossA*4.5,"Rapid hits lost to immunity: one="+lossA+", five="+lossB);
            MartialCombat.forget(five);cleanup(five);b.discard();end(h,one,a);
        });
    }
    @GameTest(template="foundation_arena",batch="crystal_defense",timeoutTicks=90)
    public static void defenseIsExactlyFifteenPercentAtZeroAndTenCrystals(GameTestHelper h) {
        var p=adept(h);form(h,p);ticks(h,p,65);
        h.runAfterDelay(35,()->{
            p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),8);
            h.assertTrue(Math.abs(p.getHealth()-13.2)<.001,"Zero crystal defense differs from 15%: "+p.getHealth());
            p.setData(BloodCrystalAttachments.STATE,new BloodCrystalState(0,true,10));p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),8);
            h.assertTrue(Math.abs(p.getHealth()-13.2)<.001,"Defense stacked per crystal: "+p.getHealth());
            CombatService.setCombatMode(p,false);p.setHealth(20);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),8);
            h.assertTrue(p.getHealth()==12,"Defense survived combat exit");end(h,p);
        });
    }
    @GameTest(template="foundation_arena",batch="crystal_exit",timeoutTicks=90)
    public static void exitingCombatClearsFormGaugeAndAttachedCrystals(GameTestHelper h) {
        var p=adept(h);var z=target(h,p,0,1.3);form(h,p);ticks(h,p,65);
        h.runAfterDelay(35,()->strike(p));h.runAfterDelay(52,()->{
            h.assertTrue(SanguineArt.state(p).crystals()==1,"No dorsal crystal to clean up");CombatService.setCombatMode(p,false);
            h.assertTrue(SanguineArt.state(p).equals(BloodCrystalState.EMPTY),"Combat exit left state active");
            h.assertTrue(h.getLevel().getEntitiesOfClass(BloodCrystalEntity.class,p.getBoundingBox().inflate(4)).isEmpty(),"Combat exit leaked crystals");end(h,p,z);
        });
    }
    @GameTest(template="foundation_arena",batch="crystal_parry",timeoutTicks=85)
    public static void aPerfectParryStopsTheThrownCrystal(GameTestHelper h) {
        var attacker=adept(h);var defender=adept(h);h.getLevel().getServer().setPvpAllowed(true);
        defender.moveTo(attacker.getX(),attacker.getY(),attacker.getZ()+5,180,0);face(defender,180);defender.setHealth(20);
        var patch=EpicFightBridge.patch(defender);h.assertTrue(patch.startSkillHolding(MurimSkills.MURIM_GUARD.get()),"Could not guard");
        cast(h,attacker,0);ticks(h,attacker,65);
        for(int t=1;t<=65;t++)h.runAfterDelay(t,()->{defender.doTick();patch.getSkill(SkillSlots.GUARD).getDataManager().setData(EpicFightSkillDataKeys.LAST_ACTIVE,defender.tickCount-1);});
        h.runAfterDelay(60,()->{h.assertTrue(defender.getHealth()==20,"Perfect parry did not stop projectile: "+defender.getHealth());
            patch.resetHolding();MartialCombat.forget(defender);cleanup(defender);h.getLevel().getServer().setPvpAllowed(false);end(h,attacker);});
    }
}
