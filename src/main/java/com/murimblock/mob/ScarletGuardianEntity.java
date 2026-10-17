package com.murimblock.mob;

import com.murimblock.bloodsect.ninja.BloodNinja;
import com.murimblock.integration.epicfight.*;
import com.murimblock.martial.BloodCrystalEntity;
import com.murimblock.martial.BloodIceberg;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/** The existing guardian model made playable, wielding the existing Sanguine Art. All melee hits are Epic Fight hits. */
public final class ScarletGuardianEntity extends Monster {
    public static final float SCALE=2.2F;
    public enum Art { NONE, CRYSTAL, SWORD, FORM, BARRAGE, ICEBERG }
    private static final EntityDataAccessor<Boolean> FORM=SynchedEntityData.defineId(ScarletGuardianEntity.class,EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bar=new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.NOTCHED_10);
    private final List<BloodCrystalEntity> dorsal=new ArrayList<>();
    private BloodCrystalEntity held;
    private Art art=Art.NONE;
    private int age, nextAction, nextCrystal, nextSword, nextBarrage, nextTempleCheck, formUntil, quietTicks;
    private boolean formUsed, icebergUsed;

    public ScarletGuardianEntity(EntityType<? extends ScarletGuardianEntity> type,Level level) {
        super(type,level);xpReward=80;setPersistenceRequired();setCanPickUpLoot(false);
    }
    public static AttributeSupplier.Builder attributes() {
        return EpicFightMobSupport.humanoidAttributes(Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH,1000).add(Attributes.ARMOR,16).add(Attributes.ARMOR_TOUGHNESS,6).add(Attributes.ATTACK_DAMAGE,6)
            .add(Attributes.MOVEMENT_SPEED,.24).add(Attributes.FOLLOW_RANGE,32).add(Attributes.STEP_HEIGHT,1)
            .add(Attributes.KNOCKBACK_RESISTANCE,.95)).add(EpicFightAttributes.STUN_ARMOR,20).add(EpicFightAttributes.IMPACT,2);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {super.defineSynchedData(b);b.define(FORM,false);}
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,16));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public boolean isAlliedTo(Entity other) {return other instanceof ScarletGuardianEntity || other instanceof BloodNinja || super.isAlliedTo(other);}
    @Override public boolean canAttack(LivingEntity other) {return !isAlliedTo(other)&&super.canAttack(other);}
    public ScarletGuardianPatch patch() {return EpicFightCapabilities.getEntityPatch(this,ScarletGuardianPatch.class);}
    public Art currentArt() {return art;}
    public boolean crystalForm() {return entityData.get(FORM);}
    public int crystals() {return (int)dorsal.stream().filter(c -> c.dorsal()&&!c.isRemoved()).count();}
    public boolean canMelee() {
        return art==Art.NONE&&tickCount>=nextAction&&validTarget()
            && !(tickCount>=nextSword&&distanceToSqr(getTarget())<=100&&hasLineOfSight(getTarget()))
            && !(!formUsed&&getHealth()<=getMaxHealth()*.6F);
    }
    private boolean validTarget() {
        LivingEntity t=getTarget();return t!=null&&t.isAlive()&&!t.isSpectator()&&!isAlliedTo(t)&&distanceToSqr(t)<40*40
            &&!(t instanceof Player p&&(p.isCreative()||p.isSpectator()));
    }
    public boolean ownsCrystal(BloodCrystalEntity c) {return c==held || crystalForm()&&dorsal.contains(c);}
    private void play(AnimationAccessor<? extends StaticAnimation> animation) {
        var p=patch();if(p!=null)p.playAnimationSynchronized(animation,0);
    }
    private void faceTarget() {
        var target=getTarget();if(target==null)return;
        getLookControl().setLookAt(target,30,30);
        double dx=target.getX()-getX(),dz=target.getZ()-getZ();float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));
        setYRot(yaw);yBodyRot=yaw;setYHeadRot(yaw);
        var p=patch();if(p!=null){p.setYRot(yaw);p.setYRotO(yaw);}
    }
    /** The actual animated hand; the renderer uses the same scale about the feet. */
    public Vec3 handPosition(float partial) {
        var p=patch();if(p==null)return getEyePosition();
        var a=p.getArmature();var j=a.searchJointByName("Tool_R");
        var v=OpenMatrix4f.transform(a.getBoundTransformFor(p.getAnimator().getPose(partial),j),new Vec3(0,.08,-.08)).scale(SCALE);
        v=OpenMatrix4f.transform(p.getModelMatrix(partial),v);
        return getPosition(partial).add(-v.x,v.y,-v.z);
    }
    /** Normal combat entry point, also used by the opt-in review tour; enforces cooldowns and target validity. */
    public boolean tryArt(Art choice) {
        if(level().isClientSide||!isAlive()||!validTarget()||art!=Art.NONE||tickCount<nextAction||patch()==null||!hasLineOfSight(getTarget()))return false;
        if(choice==Art.NONE)return false;
        if(choice==Art.CRYSTAL&&tickCount<nextCrystal || choice==Art.SWORD&&tickCount<nextSword)return false;
        if(choice==Art.FORM&&(formUsed||getHealth()>getMaxHealth()*.6F))return false;
        if(choice==Art.BARRAGE&&(!crystalForm()||crystals()==0||tickCount<nextBarrage))return false;
        if(choice==Art.ICEBERG) {
            if(icebergUsed||getHealth()>getMaxHealth()*.35F||tickCount<nextTempleCheck)return false;
            if(!templeRoom()){nextTempleCheck=tickCount+20;return false;}
        }
        art=choice;age=0;getNavigation().stop();faceTarget();
        switch(choice) {
            case CRYSTAL -> {nextCrystal=tickCount+160;held=BloodCrystalEntity.attached(this,BloodCrystalEntity.CHARGING,0);play(MurimAnimations.SANGUINE_CRYSTAL_CHARGE);}
            case SWORD -> {nextSword=tickCount+240;held=BloodCrystalEntity.sword(this);play(MurimAnimations.SANGUINE_SWORD_SUMMON);}
            case FORM -> {formUsed=true;formUntil=tickCount+600;entityData.set(FORM,true);play(MurimAnimations.SANGUINE_CRYSTAL_FORM);}
            case BARRAGE -> {nextBarrage=tickCount+160;play(MurimAnimations.SANGUINE_CRYSTAL_BARRAGE);}
            case ICEBERG -> {icebergUsed=true;play(MurimAnimations.SANGUINE_ICEBERG);}
            default -> { }
        }
        playSound(SoundEvents.AMETHYST_BLOCK_RESONATE,1F,.6F);return true;
    }
    // Do not raise the temple under an occupied ceiling: the old base must remain visitable.
    private boolean templeRoom() {
        // Check every column, including the outer spires, with standing room above their tops.
        // Conservatively use the bounding square: the seeded temple outline varies between casts.
        var centre=blockPosition();var pos=new net.minecraft.core.BlockPos.MutableBlockPos();
        int top=BloodIceberg.TOP+Mth.ceil(getBbHeight())+1;
        if(centre.getY()+top>=level().getMaxBuildHeight())return false;
        for(int x=-BloodIceberg.RADIUS;x<=BloodIceberg.RADIUS;x++)for(int z=-BloodIceberg.RADIUS;z<=BloodIceberg.RADIUS;z++) {
            pos.set(centre.getX()+x,centre.getY(),centre.getZ()+z);
            if(!level().hasChunkAt(pos))return false;
            for(int y=1;y<=top;y++) {
                pos.setY(centre.getY()+y);
                if(!level().getBlockState(pos).isAir())return false;
            }
        }
        return true;
    }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide)updateBossBar();
    }
    private void updateBossBar() {
        // ServerBossEvent broadcasts only changed values to all players tracking this entity.
        // Keep this outside the AI step so an idle/NoAI guardian still reflects damage and healing.
        bar.setName(getDisplayName());
        bar.setProgress(Mth.clamp(getHealth()/getMaxHealth(),0F,1F));
        bar.setVisible(isAlive());
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        BloodIceberg.tick(this);
        dorsal.removeIf(c -> c.isRemoved()||!c.dorsal());
        if(crystalForm()&&tickCount>=formUntil)endForm();
        if(!validTarget()) {
            setTarget(null);setAggressive(false);cancelCast();
            if(++quietTicks==200){endForm();formUsed=false;icebergUsed=false;BloodIceberg.collapse(this);}return;
        }
        quietTicks=0;setAggressive(true);
        if(art!=Art.NONE){tickArt();return;}
        var p=patch();if(p==null||p.getEntityState().inaction()||tickCount<nextAction)return;
        if(!formUsed&&getHealth()<=getMaxHealth()*.6F&&tryArt(Art.FORM))return;
        if(!icebergUsed&&getHealth()<=getMaxHealth()*.35F&&tryArt(Art.ICEBERG))return;
        if(crystals()>0&&tryArt(Art.BARRAGE))return;
        double distance=distanceTo(getTarget());
        if(distance>=5&&tryArt(Art.CRYSTAL))return;
        if(distance<=10&&tryArt(Art.SWORD))return;
        if(distance>=3.5)tryArt(Art.CRYSTAL);
    }
    private void tickArt() {
        age++;getNavigation().stop();
        // A stun replacing the skill cancels its pending projectile, rather than firing invisibly afterwards.
        var p=patch();
        // The engine uses EMPTY between completed stages; that is not an interruption.
        if(p!=null&&p.getEntityState().hurt()){cancelCast();return;}
        if(age<12 || art==Art.CRYSTAL&&age<28)faceTarget();
        switch(art) {
            case CRYSTAL -> {
                if(held!=null)held.charge(.6F+Math.min(1,age/32F)*2.8F,20);
                if(age==32)play(MurimAnimations.SANGUINE_CRYSTAL_THROW);
                if(age==38&&held!=null){held.launch(4,false);held=null;}
                if(age>=55)finishCast();
            }
            case SWORD -> {
                if(age==26){faceTarget();play(Animations.SWORD_AUTO1);}
                if(age==46){faceTarget();play(Animations.SWORD_AUTO2);}
                if(age==72){faceTarget();play(MurimAnimations.SANGUINE_SWORD_THROW);}
                if(age==89&&held!=null){held.launch(5,false);held=null;}
                if(age>=104)finishCast();
            }
            case FORM -> {
                if(age>=22&&age<=42&&age%4==2)replenishCrystal();
                if(age>=44)finishCast();
            }
            case BARRAGE -> {
                if(age==14){int i=0;for(var c:List.copyOf(dorsal))c.queue(2*i++,4);}
                if(age>=40)finishCast();
            }
            case ICEBERG -> {
                // A roof can be placed during the wind-up; recheck before raising any blocks.
                if(age==8) {
                    if(!templeRoom()){icebergUsed=false;nextTempleCheck=tickCount+20;finishCast();return;}
                    BloodIceberg.raise(this);
                }
                if(age>=45)finishCast();
            }
            default -> { }
        }
    }
    public void replenishCrystal() {
        if(level().isClientSide||!crystalForm()||crystals()>=10)return;
        int slot=0;while(slot<10){final int s=slot;if(dorsal.stream().noneMatch(c -> c.dorsal()&&c.slot()==s))break;slot++;}
        dorsal.add(BloodCrystalEntity.attached(this,BloodCrystalEntity.ORBIT,slot));
    }
    private void finishCast(){art=Art.NONE;nextAction=tickCount+24;play(ScarletGuardianAnimations.IDLE);}
    private void cancelCast(){if(held!=null){held.discard();held=null;}if(art!=Art.NONE)finishCast();}
    private void endForm(){entityData.set(FORM,false);for(var c:dorsal)c.discard();dorsal.clear();}
    @Override public boolean hurt(DamageSource source,float damage) {
        return super.hurt(source,crystalForm()&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)?damage*.7F:damage);
    }
    @Override public void die(DamageSource source){cancelCast();endForm();BloodIceberg.collapse(this);super.die(source);bar.removeAllPlayers();}
    @Override public void remove(RemovalReason reason){if(!level().isClientSide){cancelCast();endForm();BloodIceberg.collapse(this);bar.removeAllPlayers();}super.remove(reason);}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);updateBossBar();bar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("GuardianFormUsed",formUsed);tag.putBoolean("GuardianTempleUsed",icebergUsed);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);formUsed=tag.getBoolean("GuardianFormUsed");icebergUsed=tag.getBoolean("GuardianTempleUsed");}
}
