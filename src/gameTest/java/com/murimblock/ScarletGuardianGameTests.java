package com.murimblock;

import com.murimblock.martial.BloodCrystalEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import com.murimblock.mob.ScarletGuardianEntity;
import com.murimblock.mob.ScarletGuardianEntity.Art;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ScarletGuardianGameTests {
    /** Observe the real vanilla packets delivered to two independent player connections. */
    private static final class BarView implements net.minecraft.network.protocol.game.ClientboundBossEventPacket.Handler {
        final net.minecraft.server.level.ServerPlayer player;
        net.minecraft.network.chat.Component name;
        float progress;
        boolean visible;
        BarView(net.minecraft.server.level.ServerPlayer player) { this.player=player; }
        void receive() {
            var channel=(io.netty.channel.embedded.EmbeddedChannel)player.connection.getConnection().channel();
            channel.runPendingTasks();
            Object message;
            while((message=channel.readOutbound())!=null) {
                if(message instanceof net.minecraft.network.protocol.game.ClientboundBossEventPacket packet)packet.dispatch(this);
                io.netty.util.ReferenceCountUtil.release(message);
            }
        }
        public void add(java.util.UUID id,net.minecraft.network.chat.Component name,float progress,net.minecraft.world.BossEvent.BossBarColor color,net.minecraft.world.BossEvent.BossBarOverlay overlay,boolean dark,boolean music,boolean fog) {
            this.name=name;this.progress=progress;visible=true;
        }
        public void updateName(java.util.UUID id,net.minecraft.network.chat.Component name) { this.name=name; }
        public void updateProgress(java.util.UUID id,float progress) { this.progress=progress; }
        public void remove(java.util.UUID id) { visible=false; }
        void check(GameTestHelper h,ScarletGuardianEntity boss) {
            receive();h.assertTrue(visible,"Nearby player did not receive a visible boss bar");
            var expected=boss.getDisplayName();
            h.assertTrue(expected.equals(name),"Boss bar must show only the boss name, without HP: "+name);
            h.assertTrue(Math.abs(progress-boss.getHealth()/boss.getMaxHealth())<.001F,"Boss fill does not match actual health");
        }
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_health_bar",timeoutTicks=45)
    public static void healthBarUpdatesBothPlayersAndClearsAfterLeavingOrDeath(GameTestHelper h) {
        var p=pair(h,9);p.boss.setNoAi(true);
        var a=com.murimblock.testing.GameTestPlayers.player(h);
        var b=com.murimblock.testing.GameTestPlayers.player(h);
        h.testInfo.addListener(new net.minecraft.gametest.framework.GameTestListener(){
            private void clear(){com.murimblock.testing.GameTestPlayers.cleanup(a);com.murimblock.testing.GameTestPlayers.cleanup(b);}
            public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo t){}
            public void testPassed(net.minecraft.gametest.framework.GameTestInfo t,net.minecraft.gametest.framework.GameTestRunner r){clear();}
            public void testFailed(net.minecraft.gametest.framework.GameTestInfo t,net.minecraft.gametest.framework.GameTestRunner r){clear();}
            public void testAddedForRerun(net.minecraft.gametest.framework.GameTestInfo a,net.minecraft.gametest.framework.GameTestInfo b,net.minecraft.gametest.framework.GameTestRunner r){}
        });
        var first=new BarView(a);var second=new BarView(b);
        first.receive();second.receive();p.boss.startSeenByPlayer(a);p.boss.startSeenByPlayer(b);
        h.runAfterDelay(2,()->{first.check(h,p.boss);second.check(h,p.boss);p.boss.setHealth(500.25F);});
        h.runAfterDelay(4,()->{first.check(h,p.boss);second.check(h,p.boss);p.boss.heal(10);});
        h.runAfterDelay(6,()->{
            first.check(h,p.boss);second.check(h,p.boss);
            p.boss.stopSeenByPlayer(a);p.boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300);p.boss.setHealth(200);
        });
        h.runAfterDelay(8,()->{
            first.receive();h.assertTrue(!first.visible,"Departed player kept a boss bar");second.check(h,p.boss);
            p.boss.startSeenByPlayer(a);p.boss.setCustomName(net.minecraft.network.chat.Component.literal("Arena Guardian"));
        });
        h.runAfterDelay(10,()->{first.check(h,p.boss);second.check(h,p.boss);p.boss.kill();});
        h.runAfterDelay(12,()->{
            first.receive();second.receive();h.assertTrue(!first.visible&&!second.visible,"Dead boss left a visible bar");h.succeed();
        });
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_endurance",timeoutTicks=30)
    public static void guardianEnduresRepeatedHitsWithoutBecomingInvulnerable(GameTestHelper h) {
        var p=pair(h,9);p.boss.setNoAi(true);
        h.runAfterDelay(5,()->{
            h.assertTrue(p.boss.getMaxHealth()==1000&&p.boss.getHealth()==1000,"New guardian must start with 1000 actual HP, got "+p.boss.getHealth()+"/"+p.boss.getMaxHealth());
            float before=p.boss.getHealth();
            // GENERIC is tagged bypasses_armor; use a real melee source to exercise the armour.
            var strike=p.boss.damageSources().mobAttack(p.target);
            h.assertTrue(!strike.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR),"Endurance fixture bypasses armour");
            for(int i=0;i<10;i++){
                // Independent hits: remove only the fixture's vanilla damage cooldown.
                p.boss.invulnerableTime=0;
                float previous=p.boss.getHealth();
                h.assertTrue(p.boss.hurt(strike,18),"A test strike was refused");
                h.assertTrue(p.boss.getHealth()<previous,"Armour made the guardian immune to damage");
            }
            float lost=before-p.boss.getHealth();
            h.assertTrue(lost>=90&&lost<=120,"Ten 18-damage hits should cost 90-120 HP with reinforced armour; got "+lost);
            h.assertTrue(p.boss.isAlive()&&p.boss.getHealth()>=880,"Guardian cannot endure sustained pressure: "+p.boss.getHealth());
            h.succeed();
        });
    }
    private record Pair(ScarletGuardianEntity boss,Zombie target,java.util.Map<BlockPos,net.minecraft.world.level.block.state.BlockState> changed) { }
    private static void block(GameTestHelper h,Pair p,BlockPos pos,net.minecraft.world.level.block.Block block) {
        p.changed.putIfAbsent(pos,h.getLevel().getBlockState(pos));h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
    }
    private static Pair pair(GameTestHelper h,double distance) {
        // These distinct batches run sequentially. Reuse the already simulated spawn area, as SanguineGameTests does:
        // forcing a newly generated distant chunk does not synchronously load its entity sections.
        var at=new BlockPos(8,120,8);
        var forced=new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int x=(at.getX()>>4)-1;x<=(at.getX()>>4)+1;x++)for(int z=(at.getZ()>>4)-1;z<=(at.getZ()>>4)+1;z++) {
            var chunk=new net.minecraft.world.level.ChunkPos(x,z);
            if(!h.getLevel().getForcedChunks().contains(chunk.toLong())){h.getLevel().setChunkForced(x,z,true);forced.add(chunk);}
            h.getLevel().getChunk(x,z);
        }
        var floor=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int x=-5;x<=5;x++)for(int z=-4;z<=13;z++)
            {var pos=at.offset(x,-1,z);floor.put(pos,h.getLevel().getBlockState(pos));h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());}
        var boss=MurimEntities.SCARLET_GUARDIAN.get().create(h.getLevel());boss.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        var target=EntityType.ZOMBIE.create(h.getLevel());target.moveTo(boss.getX(),boss.getY(),boss.getZ()+distance,180,0);
        target.setNoAi(true);target.setNoGravity(true);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
        h.getLevel().addFreshEntity(boss);h.getLevel().addFreshEntity(target);
        h.testInfo.addListener(new net.minecraft.gametest.framework.GameTestListener(){
            private void clear(){
                for(var c:h.getLevel().getEntitiesOfClass(BloodCrystalEntity.class,boss.getBoundingBox().inflate(64)))if(c.getOwner()==boss)c.discard();
                boss.discard();target.discard();floor.forEach((p,s)->h.getLevel().setBlockAndUpdate(p,s));
                for(var chunk:forced)h.getLevel().setChunkForced(chunk.x,chunk.z,false);
            }
            public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo t){}
            public void testPassed(net.minecraft.gametest.framework.GameTestInfo t,net.minecraft.gametest.framework.GameTestRunner r){clear();}
            public void testFailed(net.minecraft.gametest.framework.GameTestInfo t,net.minecraft.gametest.framework.GameTestRunner r){clear();}
            public void testAddedForRerun(net.minecraft.gametest.framework.GameTestInfo a,net.minecraft.gametest.framework.GameTestInfo b,net.minecraft.gametest.framework.GameTestRunner r){}
        });
        return new Pair(boss,target,floor);
    }
    @GameTest(template="foundation_arena", batch="scarlet_guardian")
    public static void mobOwnedSanguineProjectileCanLaunch(GameTestHelper h) {
        var owner = EntityType.ZOMBIE.create(h.getLevel());
        var target = EntityType.ZOMBIE.create(h.getLevel());
        owner.moveTo(0, 120, 0); target.moveTo(0, 120, 8);
        owner.setTarget(target);
        var crystal = new BloodCrystalEntity(MurimEntities.BLOOD_CRYSTAL.get(), h.getLevel());
        crystal.setOwner(owner); crystal.setPos(0, 122, 0); crystal.charge(1, 0);
        crystal.launch(4, true);
        h.assertTrue(!crystal.isRemoved() && crystal.state() == BloodCrystalEntity.FLYING,
                "The existing Sanguine projectile rejects a living mob caster");
        h.assertTrue(crystal.getDeltaMovement().lengthSqr() > .1, "Projectile never started flying");
        crystal.discard(); owner.discard(); target.discard(); h.succeed();
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_charge",timeoutTicks=110)
    public static void guardianAiActuallyChargesAndHurtsTarget(GameTestHelper h) {
        var p=pair(h,9);p.boss.setTarget(p.target);
        h.runAfterDelay(20,()->h.assertTrue(p.boss.currentArt()==Art.CRYSTAL,"AI did not choose a crystal at range"));
        h.runAfterDelay(95,()->{h.assertTrue(p.target.getHealth()<1000,"Real boss projectile never dealt damage");h.succeed();});
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_sword",timeoutTicks=150)
    public static void guardianSwordUsesEngineHitsAndThrows(GameTestHelper h) {
        var p=pair(h,3);p.boss.setTarget(p.target);
        h.runAfterDelay(12,()->h.assertTrue(p.boss.currentArt()==Art.SWORD,"AI did not summon Scarlet Tear"));
        h.runAfterDelay(68,()->h.assertTrue(p.target.getHealth()<1000,"Epic Fight sword cuts missed a nearby enemy"));
        h.runAfterDelay(98,()->{
            boolean thrown=h.getLevel().getEntitiesOfClass(BloodCrystalEntity.class,p.boss.getBoundingBox().inflate(30)).stream()
                .anyMatch(c -> c.sword()&&c.getOwner()==p.boss&&c.state()!=BloodCrystalEntity.CHARGING);
            h.assertTrue(thrown,"Scarlet Tear was not thrown after its cuts; art="+p.boss.currentArt()+", target="+p.target.getHealth());h.succeed();
        });
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_form",timeoutTicks=140)
    public static void guardianTransformsConsumesBarrageAndKeepsForm(GameTestHelper h) {
        var p=pair(h,9);p.boss.setHealth(p.boss.getMaxHealth()*.58F);p.boss.setTarget(p.target);
        h.runAfterDelay(46,()->{h.assertTrue(p.boss.crystalForm(),"Transformation absent: age="+p.boss.tickCount+", health="+p.boss.getHealth()+", art="+p.boss.currentArt()+", target="+p.boss.getTarget()+", removed="+p.boss.isRemoved());h.assertTrue(p.boss.crystals()==6,"Form did not grow six crystals: "+p.boss.crystals());});
        h.runAfterDelay(130,()->{h.assertTrue(p.boss.crystalForm(),"Salvo ended form");h.assertTrue(p.boss.crystals()==0,"Salvo did not consume crystals");h.assertTrue(p.target.getHealth()<1000,"Barrage never hit");h.succeed();});
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_cleanup",timeoutTicks=60)
    public static void targetLossCancelsChargeAndCooldownPreventsImmediateRepeat(GameTestHelper h) {
        var p=pair(h,9);p.boss.setTarget(p.target);
        h.runAfterDelay(15,()->{h.assertTrue(p.boss.currentArt()==Art.CRYSTAL,"No charge to cancel");p.target.discard();});
        h.runAfterDelay(22,()->{
            h.assertTrue(p.boss.currentArt()==Art.NONE,"Lost target did not cancel cast");
            h.assertTrue(h.getLevel().getEntitiesOfClass(BloodCrystalEntity.class,p.boss.getBoundingBox().inflate(6)).stream().noneMatch(c->c.getOwner()==p.boss&&!c.isRemoved()),"Orphan held crystal");
            h.assertTrue(!p.boss.tryArt(Art.CRYSTAL),"Cooldown/target requirement bypassed");h.succeed();
        });
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_allies")
    public static void guardianCannotSelectFellowCultists(GameTestHelper h) {
        var p=pair(h,4);var ally=MurimEntities.SCARLET_GUARDIAN.get().create(h.getLevel());
        h.assertTrue(p.boss.isAlliedTo(ally)&&!p.boss.canAttack(ally),"Guardian can attack own faction");
        p.boss.setTarget(ally);h.assertTrue(!p.boss.tryArt(Art.CRYSTAL),"Cast accepted an allied target");ally.discard();h.succeed();
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_removal",timeoutTicks=65)
    public static void removalClearsHeldAndDorsalEntities(GameTestHelper h) {
        var p=pair(h,9);p.boss.setHealth(p.boss.getMaxHealth()*.58F);p.boss.setTarget(p.target);
        h.runAfterDelay(45,()->{
            h.assertTrue(p.boss.crystals()>0,"No crystals were present before removal");p.boss.discard();
        });
        h.runAfterDelay(48,()->{
            h.assertTrue(h.getLevel().getEntitiesOfClass(BloodCrystalEntity.class,p.boss.getBoundingBox().inflate(12)).stream()
                .noneMatch(c -> c.getOwner()==p.boss&&!c.isRemoved()),"Removed guardian left attached crystals");h.succeed();
        });
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_collateral",timeoutTicks=100)
    public static void realSwordCutsSpareNearbyNinjasAndSoldiers(GameTestHelper h) {
        var p=pair(h,3);
        var ninja=com.murimblock.bloodsect.BloodSect.BLOOD_NINJA.get().create(h.getLevel());
        var soldier=com.murimblock.bloodsect.BloodSect.BLOOD_SOLDIER.get().create(h.getLevel());
        var allies=java.util.List.of(ninja,soldier);
        for(int i=0;i<allies.size();i++) {
            var ally=allies.get(i);ally.moveTo(p.target.getX()+(i==0?-.4:.4),p.target.getY(),p.target.getZ()-.2);
            ally.setNoAi(true);ally.setNoGravity(true);ally.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);ally.setHealth(1000);
            ally.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);h.getLevel().addFreshEntity(ally);
        }
        h.testInfo.addListener(new net.minecraft.gametest.framework.GameTestListener(){
            private void clear(){allies.forEach(net.minecraft.world.entity.Entity::discard);}
            public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo t){}
            public void testPassed(net.minecraft.gametest.framework.GameTestInfo t,net.minecraft.gametest.framework.GameTestRunner r){clear();}
            public void testFailed(net.minecraft.gametest.framework.GameTestInfo t,net.minecraft.gametest.framework.GameTestRunner r){clear();}
            public void testAddedForRerun(net.minecraft.gametest.framework.GameTestInfo a,net.minecraft.gametest.framework.GameTestInfo b,net.minecraft.gametest.framework.GameTestRunner r){}
        });
        p.boss.setTarget(p.target);
        h.runAfterDelay(68,()->{
            for(var ally:allies) {
                h.assertTrue(ally.getHealth()==1000,"A real sword cut hurt a fellow cultist: "+ally.getType()+" health="+ally.getHealth());
                h.assertTrue(p.boss.patch().isTargetInvulnerable(ally),"Epic Fight collider still accepts a cult ally");
            }
            h.assertTrue(p.target.getHealth()<1000,"Sword fixture never hit its hostile target");
            h.assertTrue(!p.boss.patch().isTargetInvulnerable(p.target),"Epic Fight collider rejects the hostile target");h.succeed();
        });
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_clearance_beam",timeoutTicks=25)
    public static void templeRefusesLowBeamBetweenOldSampleColumns(GameTestHelper h) {
        templeClearance(h,5);
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_clearance_edge",timeoutTicks=25)
    public static void templeRefusesRoofAtOuterFootprint(GameTestHelper h) {
        templeClearance(h,21);
    }
    private static void templeClearance(GameTestHelper h,int x) {
        var p=pair(h,9);p.boss.setNoAi(true);p.boss.setTarget(p.target);p.boss.setHealth(300);
        block(h,p,p.boss.blockPosition().offset(x,4,0),Blocks.OBSIDIAN);
        h.runAfterDelay(5,()->{
            h.assertTrue(!p.boss.tryArt(Art.ICEBERG),"Temple accepted a low solid roof at x="+x);h.succeed();
        });
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_clearance_open",timeoutTicks=25)
    public static void templeAcceptsOpenArena(GameTestHelper h) {
        var p=pair(h,9);p.boss.setNoAi(true);p.boss.setTarget(p.target);p.boss.setHealth(300);
        h.runAfterDelay(5,()->{h.assertTrue(p.boss.tryArt(Art.ICEBERG),"Open arena refused the temple");h.succeed();});
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_clearance_windup",timeoutTicks=35)
    public static void templeRechecksRoofAddedDuringWindup(GameTestHelper h) {
        var p=pair(h,9);p.boss.setNoAi(true);p.boss.setTarget(p.target);p.boss.setHealth(300);
        h.runAfterDelay(5,()->{
            h.assertTrue(p.boss.tryArt(Art.ICEBERG),"Open arena refused the temple");
            block(h,p,p.boss.blockPosition().offset(5,4,0),Blocks.OBSIDIAN);p.boss.setNoAi(false);
        });
        h.runAfterDelay(20,()->{
            h.assertTrue(!com.murimblock.martial.BloodIceberg.inArena(h.getLevel(),p.boss.blockPosition()),"Temple grew under a new roof during its wind-up");h.succeed();
        });
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_defence",timeoutTicks=30)
    public static void formReducesDamageByThirtyPercent(GameTestHelper h) {
        var p=pair(h,9);p.boss.getAttribute(Attributes.ARMOR).setBaseValue(0);p.boss.setHealth(p.boss.getMaxHealth()*.58F);p.boss.setTarget(p.target);
        h.runAfterDelay(8,()->{
            h.assertTrue(p.boss.crystalForm(),"Form not active");float before=p.boss.getHealth();
            p.boss.hurt(p.boss.damageSources().generic(),10);
            h.assertTrue(Math.abs(before-p.boss.getHealth()-7)<.01,"Wrong form mitigation: "+(before-p.boss.getHealth()));h.succeed();
        });
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_cover",timeoutTicks=100)
    public static void guardianCannotCastThroughSolidWall(GameTestHelper h) {
        var p=pair(h,9);
        for(int x=-4;x<=4;x++)for(int y=0;y<7;y++)block(h,p,p.boss.blockPosition().offset(x,y,4),Blocks.STONE);
        p.boss.setTarget(p.target);
        h.assertTrue(!p.boss.tryArt(Art.CRYSTAL),"Crystal cast accepted a target behind solid cover");
        p.boss.setNoAi(true);h.succeed();
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_temple",timeoutTicks=85)
    public static void guardianTemplePreservesStoneAndCollapsesWithCaster(GameTestHelper h) {
        var p=pair(h,9);p.boss.setNoAi(true);var stone=p.boss.blockPosition().offset(2,0,0);
        block(h,p,stone,Blocks.OBSIDIAN);
        h.assertTrue(com.murimblock.martial.BloodIceberg.raise(p.boss),"Guardian temple refused");
        for(int i=1;i<55;i++)h.runAfterDelay(i,()->com.murimblock.martial.BloodIceberg.tick(p.boss));
        h.runAfterDelay(55,()->{
            h.assertTrue(h.getLevel().getBlockState(stone).is(Blocks.OBSIDIAN),"Temple replaced existing construction");
            h.assertTrue(com.murimblock.martial.BloodIceberg.inArena(h.getLevel(),p.boss.blockPosition()),"Temple did not rise");
            p.boss.discard();h.assertTrue(!com.murimblock.martial.BloodIceberg.inArena(h.getLevel(),p.boss.blockPosition()),"Arena stayed active after owner removal");
        });
        h.runAfterDelay(72,()->{
            h.assertTrue(!(h.getLevel().getBlockState(stone.offset(1,0,0)).getBlock() instanceof com.murimblock.martial.BloodIcebergBlock),"Temple did not collapse");h.succeed();
        });
    }
    @GameTest(template="foundation_arena",batch="scarlet_guardian_projectile_wall",timeoutTicks=50)
    public static void launchedBarrageBreaksAgainstWall(GameTestHelper h) {
        var p=pair(h,9);p.boss.setNoAi(true);p.boss.setTarget(p.target);
        for(int x=-4;x<=4;x++)for(int y=0;y<7;y++)block(h,p,p.boss.blockPosition().offset(x,y,4),Blocks.STONE);
        h.runAfterDelay(12,()->{
            var c=BloodCrystalEntity.attached(p.boss,BloodCrystalEntity.CHARGING,0);c.charge(1,0);
            c.setPos(p.boss.position().add(0,2,0));c.launch(5,true);
            h.runAfterDelay(9,()->{h.assertTrue(c.broken(),"Projectile did not shatter on solid wall");h.assertTrue(p.target.getHealth()==1000,"Projectile damaged target through wall");c.discard();h.succeed();});
        });
    }
}
