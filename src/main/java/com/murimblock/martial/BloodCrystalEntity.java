package com.murimblock.martial;

import com.murimblock.combat.CombatService;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** One earned crystal: charges at a hand, hovers at its supplied dorsal socket, or flies as a parryable projectile. */
public final class BloodCrystalEntity extends Projectile implements GeoEntity {
    public static final byte CHARGING=0, ORBIT=1, PENDING=2, FLYING=3;
    public static final double SPEED=.9, RANGE=24;
    public static final ResourceKey<DamageType> DAMAGE_TYPE=ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("murimblock","blood_crystal"));
    private static final EntityDataAccessor<Byte> STATE=SynchedEntityData.defineId(BloodCrystalEntity.class,EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> OWNER_ID=SynchedEntityData.defineId(BloodCrystalEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLOT=SynchedEntityData.defineId(BloodCrystalEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE=SynchedEntityData.defineId(BloodCrystalEntity.class,EntityDataSerializers.FLOAT);
    // Exact kit pivots (pixels). X is mirrored by GeckoLib; Z is behind the player's body.
    private static final double[][] SOCKETS={{12.839948427736791,20.6269184364184,13},{13.186896557987843,24.375247516916236,14},{9.192388155425117,26.353910524340094,15},{6.719059396145292,29.549517246390277,13},{2.0336480455230013,29.271958742189433,14},{-2.315230082595417,30.69422995264643,15},{-5.901876496614108,28.266467851559028,13},{-10.465180361560902,27.372144289248723,14},{-11.583084814448782,23.72150119729129,15},{-14.617787440808039,20.852184066076333,13}};
    private static final RawAnimation HOVER=RawAnimation.begin().thenLoop("animation.blood_crystal_floating.hover");
    private static final RawAnimation SPIN=RawAnimation.begin().thenLoop("animation.blood_crystal_projectile.spin");
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int pendingTicks, flightTicks;
    private double travelled;
    private float damage;
    private boolean barrage;

    public BloodCrystalEntity(EntityType<? extends BloodCrystalEntity> type,Level level) { super(type,level); noPhysics=true; }
    public static BloodCrystalEntity attached(ServerPlayer owner,byte state,int slot) {
        var crystal=new BloodCrystalEntity(MurimEntities.BLOOD_CRYSTAL.get(),owner.level());
        crystal.setOwner(owner); crystal.entityData.set(OWNER_ID,owner.getId());
        crystal.entityData.set(STATE,state); crystal.entityData.set(SLOT,Math.clamp(slot,0,9));
        crystal.follow(owner); owner.serverLevel().addFreshEntity(crystal);
        return crystal;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE,CHARGING); builder.define(OWNER_ID,-1); builder.define(SLOT,0); builder.define(SCALE,.1F);
    }
    public byte state() { return entityData.get(STATE); }
    public int slot() { return entityData.get(SLOT); }
    public float crystalScale() { return entityData.get(SCALE); }
    public boolean dorsal() { return state()==ORBIT||state()==PENDING; }
    public Entity visualOwner() { Entity owner=getOwner(); return owner==null?level().getEntity(entityData.get(OWNER_ID)):owner; }
    public static double[] socket(int index) { return SOCKETS[Math.clamp(index,0,9)].clone(); }
    public void chargeScale(float fraction) { entityData.set(SCALE,.2F+.8F*Math.clamp(fraction,0,1)); }
    public void queue(int delay,float damage) {
        this.damage=Math.max(.5F,damage); barrage=true; pendingTicks=Math.max(0,delay);
        entityData.set(STATE,PENDING); entityData.set(SCALE,1.0F);
    }
    public void launch(float damage,boolean barrage) {
        if (state()==FLYING||isRemoved()) return;
        this.damage=Math.max(.5F,damage); this.barrage=barrage;
        Entity owner=getOwner(); if (!(owner instanceof ServerPlayer player)) { discard();return; }
        follow(player);
        Vec3 eye=player.getEyePosition(), end=eye.add(player.getViewVector(1).scale(RANGE));
        HitResult wall=level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if (wall.getType()==HitResult.Type.BLOCK) end=wall.getLocation();
        EntityHitResult target=ProjectileUtil.getEntityHitResult(player,eye,end,player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
                e -> e instanceof LivingEntity && e!=player && e.isAlive() && e.isPickable() && !e.isSpectator() && !e.isAlliedTo(player),eye.distanceToSqr(end));
        if (target!=null) end=target.getLocation();
        Vec3 direction=end.subtract(position()).normalize();
        if (direction.lengthSqr()<.001) direction=player.getViewVector(1);
        entityData.set(STATE,FLYING); entityData.set(SCALE,1.0F);
        setDeltaMovement(direction.scale(SPEED)); updateRotation(); flightTicks=0;travelled=0;
    }
    private void follow(Entity owner) {
        Vec3 at;
        if (dorsal()) {
            double[] pivot=SOCKETS[slot()];
            float yaw=owner instanceof LivingEntity living?living.yBodyRot:owner.getYRot();
            Vec3 forward=Vec3.directionFromRotation(0,yaw),right=new Vec3(-forward.z,0,forward.x);
            at=owner.position().add(right.scale(-pivot[0]/16)).add(forward.scale(-pivot[2]/16)).add(0,pivot[1]/16,0);
            setYRot(yaw);
        } else {
            Vec3 forward=Vec3.directionFromRotation(0,owner.getYRot()),right=new Vec3(-forward.z,0,forward.x);
            at=owner instanceof net.minecraft.world.entity.player.Player player ? SanguineArt.handPosition(player,1)
                    : owner.position().add(right.scale(.35)).add(forward.scale(.65)).add(0,1.22,0);
            setYRot(owner.getYRot());
        }
        setPos(at);setDeltaMovement(Vec3.ZERO);
    }
    @Override public void tick() {
        super.tick(); Entity owner=visualOwner();
        if (state()!=FLYING) {
            if (owner!=null) follow(owner);
            if (level().isClientSide) return;
            if (!(owner instanceof ServerPlayer p) || !p.isAlive() || p.level()!=level() || KnockdownService.isDowned(p)
                    || !CombatService.isInCombatMode(p) || !SanguineArt.bareHanded(p)) { discard();return; }
            if (state()==ORBIT && (!SanguineArt.state(p).active() || slot()>=SanguineArt.state(p).crystals())) { discard();return; }
            if (state()==CHARGING && !SanguineArt.hasCharge(p)) { discard();return; }
            if (state()==PENDING) {
                if (!SanguineArt.state(p).active()) {discard();return;}
                if (pendingTicks--<=0) launch(damage,true);
            }
            return;
        }
        if (!level().isClientSide) {
            HitResult hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
            if (hit.getType()!=HitResult.Type.MISS && !EventHooks.onProjectileImpact(this,hit)) hitTargetOrDeflectSelf(hit);
        }
        if (!isRemoved()) { Vec3 velocity=getDeltaMovement();setPos(position().add(velocity));updateRotation();travelled+=velocity.length(); }
        if (!level().isClientSide && (++flightTicks>40 || travelled>RANGE)) discard();
    }
    @Override protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target instanceof LivingEntity && target!=getOwner()
                && target.isAlive() && !target.isSpectator() && !(getOwner()!=null&&target.isAlliedTo(getOwner()));
    }
    @Override protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;
        if (result.getEntity() instanceof LivingEntity target && getOwner() instanceof ServerPlayer player) {
            float before=target.getHealth();
            DamageSource source=new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE),this,player);
            boolean hurt=target.hurt(source,damage);
            if (hurt && target.getHealth()<before && !MurimGuardSkill.parriedThisTick(target) && !MurimGuardSkill.guardedThisTick(target))
                SanguineArt.projectileHit(player,target,barrage);
        }
        discard();
    }
    @Override protected void onHitBlock(BlockHitResult result) {super.onHitBlock(result);if(!level().isClientSide)discard();}
    @Override public boolean shouldBeSaved() {return false;}
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"crystal",0,event -> event.setAndContinue(dorsal()?HOVER:SPIN)));
    }
}
