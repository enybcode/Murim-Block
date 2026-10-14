package com.murimblock.martial;

import com.murimblock.client.BloodCrystalEffects;
import com.murimblock.combat.CombatService;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * One blood crystal of the Sanguine Art, drawn by GeckoLib. Its life:
 * <ul>
 *   <li>CHARGING in the thrower's hand: it forms, its fragments circling it, and grows with every point of the blood
 *   gauge it draws (BloodCrystalCharge); ten points make the great crystal, 3 blocks across;</li>
 *   <li>ORBIT / PENDING on the back: a crystal earned in Blood Crystal Form, waiting for Crystal Barrage;</li>
 *   <li>FLYING: thrown like a stone (drag and gravity, aimed so it lands where the thrower looks), spinning; a
 *   barrage crystal flies straight;</li>
 *   <li>then it shatters, as the crab man's water ball bursts: on the ground, on a wall or on a body, each with its
 *   own animation; the great crystal explodes instead, hurting everything around it (no block is broken).</li>
 * </ul>
 * The entity's position is the bottom of its box; the model is drawn round the box's middle.
 */
public final class BloodCrystalEntity extends Projectile implements GeoEntity {
    public static final byte CHARGING=0, ORBIT=1, PENDING=2, FLYING=3, SHATTER_GROUND=4, SHATTER_WALL=5, SHATTER_BODY=6, EXPLODE=7;
    /** Barrage crystals: straight and fast. */
    public static final double SPEED=.9;
    /** A crystal still flying after this distance breaks in the air. */
    public static final double RANGE=48;
    /** Breaks last 0.75 s, the explosion 0.9 s (their animations). */
    static final int SHATTER_TICKS=15, EXPLODE_TICKS=18;
    public static final ResourceKey<DamageType> DAMAGE_TYPE=ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("murimblock","blood_crystal"));
    private static final EntityDataAccessor<Byte> STATE=SynchedEntityData.defineId(BloodCrystalEntity.class,EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> OWNER_ID=SynchedEntityData.defineId(BloodCrystalEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLOT=SynchedEntityData.defineId(BloodCrystalEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE=SynchedEntityData.defineId(BloodCrystalEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> POINTS=SynchedEntityData.defineId(BloodCrystalEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BARRAGE=SynchedEntityData.defineId(BloodCrystalEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> BREAK_YAW=SynchedEntityData.defineId(BloodCrystalEntity.class,EntityDataSerializers.FLOAT);
    // Exact kit pivots (pixels). X is mirrored by GeckoLib; Z is behind the player's body.
    private static final double[][] SOCKETS={{12.839948427736791,20.6269184364184,13},{13.186896557987843,24.375247516916236,14},{9.192388155425117,26.353910524340094,15},{6.719059396145292,29.549517246390277,13},{2.0336480455230013,29.271958742189433,14},{-2.315230082595417,30.69422995264643,15},{-5.901876496614108,28.266467851559028,13},{-10.465180361560902,27.372144289248723,14},{-11.583084814448782,23.72150119729129,15},{-14.617787440808039,20.852184066076333,13}};
    private static final RawAnimation HOVER=RawAnimation.begin().thenLoop("animation.blood_crystal_floating.hover");
    private static final RawAnimation FORM=RawAnimation.begin().thenPlay("animation.blood_crystal_shot.form").thenLoop("animation.blood_crystal_shot.charge");
    private static final RawAnimation FLY=RawAnimation.begin().thenLoop("animation.blood_crystal_shot.fly");
    private static final RawAnimation[] BREAKS={
            RawAnimation.begin().thenPlayAndHold("animation.blood_crystal_shot.shatter_ground"),
            RawAnimation.begin().thenPlayAndHold("animation.blood_crystal_shot.shatter_wall"),
            RawAnimation.begin().thenPlayAndHold("animation.blood_crystal_shot.shatter_body"),
            RawAnimation.begin().thenPlayAndHold("animation.blood_crystal_shot.explode")};
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int pendingTicks, flightTicks, stateTicks;
    private double travelled;
    /** The thrower's attack for a share of 1 (TechniquePower), fixed at the launch; damage is a share of it. */
    private float unit;
    @Nullable private Entity followed;
    private Vec3 followOffset=Vec3.ZERO;

    public BloodCrystalEntity(EntityType<? extends BloodCrystalEntity> type,Level level) { super(type,level); noPhysics=true; }
    public static BloodCrystalEntity attached(ServerPlayer owner,byte state,int slot) {
        var crystal=new BloodCrystalEntity(MurimEntities.BLOOD_CRYSTAL.get(),owner.level());
        crystal.setOwner(owner); crystal.entityData.set(OWNER_ID,owner.getId());
        crystal.entityData.set(STATE,state); crystal.entityData.set(SLOT,Math.clamp(slot,0,9));
        crystal.follow(owner); owner.serverLevel().addFreshEntity(crystal);
        return crystal;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE,CHARGING); builder.define(OWNER_ID,-1); builder.define(SLOT,0); builder.define(SCALE,.2F);
        builder.define(POINTS,0); builder.define(BARRAGE,false); builder.define(BREAK_YAW,0F);
    }
    public byte state() { return entityData.get(STATE); }
    public int slot() { return entityData.get(SLOT); }
    public float crystalScale() { return entityData.get(SCALE); }
    public int points() { return entityData.get(POINTS); }
    public boolean great() { return BloodCrystalCharge.great(points()); }
    public boolean barrage() { return entityData.get(BARRAGE); }
    public float breakYaw() { return entityData.get(BREAK_YAW); }
    public boolean dorsal() { return state()==ORBIT||state()==PENDING; }
    public boolean broken() { return state()>=SHATTER_GROUND; }
    public Entity visualOwner() { Entity owner=getOwner(); return owner==null?level().getEntity(entityData.get(OWNER_ID)):owner; }
    public static double[] socket(int index) { return SOCKETS[Math.clamp(index,0,9)].clone(); }

    /** While charging: the crystal's size and the points of the gauge it holds. */
    public void charge(float scale,int points) { entityData.set(SCALE,scale); entityData.set(POINTS,Math.clamp(points,0,BloodCrystalCharge.MAX_POINTS)); }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) refreshDimensions();
        if (STATE.equals(key)) {
            stateTicks=0;
            // Light and, for the great crystal, its shards (Photon, client only).
            if (level().isClientSide && broken()) BloodCrystalEffects.broke(this);
        }
    }
    @Override public EntityDimensions getDimensions(Pose pose) { return super.getDimensions(pose).scale(dorsal()?1:crystalScale()); }
    public Vec3 centre() { return position().add(0,getBbHeight()/2,0); }
    private void placeCentre(Vec3 centre) { setPos(centre.x,centre.y-getBbHeight()/2,centre.z); }

    public void queue(int delay,float damage) {
        unit=Math.max(.5F,damage); pendingTicks=Math.max(0,delay);
        entityData.set(BARRAGE,true); entityData.set(STATE,PENDING); entityData.set(SCALE,.5F);
    }

    /**
     * Throws the crystal where its thrower looks: at the first body or block on their line of sight within 24 blocks,
     * the flight bent upward to make up for its fall. `unit` is the thrower's attack for a share of 1.
     */
    public void launch(float unit,boolean barrage) {
        if (state()==FLYING||broken()||isRemoved()) return;
        this.unit=Math.max(.5F,unit); entityData.set(BARRAGE,barrage);
        Entity owner=getOwner(); if (!(owner instanceof ServerPlayer player)) { discard();return; }
        if (!barrage) follow(player);
        Vec3 eye=player.getEyePosition(), end=eye.add(player.getViewVector(1).scale(24));
        HitResult wall=level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if (wall.getType()==HitResult.Type.BLOCK) end=wall.getLocation();
        EntityHitResult target=ProjectileUtil.getEntityHitResult(player,eye,end,player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
                e -> e instanceof LivingEntity && e!=player && e.isAlive() && e.isPickable() && !e.isSpectator() && !e.isAlliedTo(player),eye.distanceToSqr(end));
        if (target!=null) end=target.getEntity().position().add(0,target.getEntity().getBbHeight()*.6,0);
        Vec3 from=centre();
        Vec3 velocity=barrage?direction(from,end,player).scale(SPEED)
                :aim(from,end,BloodCrystalCharge.speed(points()),BloodCrystalCharge.gravity(points()),player);
        if (barrage) entityData.set(SCALE,.5F);
        entityData.set(STATE,FLYING);
        setDeltaMovement(velocity); updateRotation(); flightTicks=0;travelled=0;
        playSound(SoundEvents.AMETHYST_BLOCK_RESONATE,great()?1.4F:.7F,great()?.6F:1.4F);
    }
    private static Vec3 direction(Vec3 from,Vec3 to,Player player) {
        Vec3 d=to.subtract(from);
        return d.lengthSqr()<.001?player.getViewVector(1):d.normalize();
    }
    /**
     * Velocity that reaches `to` at `speed` blocks/tick horizontally under drag 0.99 and `gravity`: ticks to cover
     * the distance, then the vertical speed found by bisection (as the crab man aims his water ball).
     */
    public static Vec3 aim(Vec3 from,Vec3 to,double speed,double gravity,Player player) {
        double dx=to.x-from.x, dz=to.z-from.z, dy=to.y-from.y, d=Math.sqrt(dx*dx+dz*dz);
        if (d<.5) return direction(from,to,player).scale(speed);
        double k=1-.01*d/speed, ticks=k>.05?Math.log(k)/Math.log(.99):60;
        double low=-3, high=3;
        for (int i=0;i<40;i++) { double mid=(low+high)/2; if (heightAfter(mid,ticks,gravity)<dy) low=mid; else high=mid; }
        return new Vec3(dx/d*speed,(low+high)/2,dz/d*speed);
    }
    /** Height gained after a fractional number of ticks: move, then drag, then gravity (as the crystal's tick does). */
    static double heightAfter(double vy,double ticks,double gravity) {
        double y=0; int whole=(int)ticks;
        for (int i=0;i<whole;i++) { y+=vy; vy=vy*.99-gravity; }
        return y+vy*(ticks-whole);
    }

    private void follow(Entity owner) {
        if (dorsal()) {
            double[] pivot=SOCKETS[slot()];
            float yaw=owner instanceof LivingEntity living?living.yBodyRot:owner.getYRot();
            Vec3 forward=Vec3.directionFromRotation(0,yaw),right=new Vec3(-forward.z,0,forward.x);
            setPos(owner.position().add(right.scale(-pivot[0]/16)).add(forward.scale(-pivot[2]/16)).add(0,pivot[1]/16,0));
            setYRot(yaw);
        } else {
            // In the hand, the crystal's middle sits ahead of the fist by half its size, so a great one is held out.
            Vec3 look=Vec3.directionFromRotation(0,owner.getYRot());
            Vec3 hand=owner instanceof Player player ? SanguineArt.handPosition(player,1)
                    : owner.position().add(new Vec3(-look.z,0,look.x).scale(.35)).add(look.scale(.65)).add(0,1.22,0);
            double half=.375*crystalScale();
            placeCentre(hand.add(look.scale(Math.max(0,half-.15))).add(0,Math.max(0,half-.3)*.6,0));
            setYRot(owner.getYRot());
        }
        setDeltaMovement(Vec3.ZERO);
    }

    @Override public void tick() {
        super.tick(); stateTicks++; Entity owner=visualOwner();
        if (broken()) {
            if (followed!=null&&followed.isAlive()) setPos(followed.position().add(followOffset));
            if (!level().isClientSide && stateTicks>=(state()==EXPLODE?EXPLODE_TICKS:SHATTER_TICKS)) discard();
            return;
        }
        if (state()!=FLYING) {
            if (owner!=null) follow(owner);
            if (level().isClientSide) return;
            if (!(owner instanceof ServerPlayer p) || !p.isAlive() || p.level()!=level() || KnockdownService.isDowned(p)
                    || !CombatService.isInCombatMode(p) || !SanguineArt.bareHanded(p)) { discard();return; }
            if (state()==ORBIT && (!SanguineArt.state(p).active() || slot()>=SanguineArt.state(p).crystals())) { discard();return; }
            if (state()==CHARGING && !SanguineArt.hasCharge(p)) { discard();return; }
            if (state()==PENDING) {
                if (!SanguineArt.state(p).active()) {discard();return;}
                if (pendingTicks--<=0) launch(unit,true);
            }
            return;
        }
        if (!level().isClientSide) {
            HitResult hit=nextHit();
            if (hit.getType()!=HitResult.Type.MISS && !EventHooks.onProjectileImpact(this,hit)) hitTargetOrDeflectSelf(hit);
        }
        if (isRemoved()||state()!=FLYING) return;
        Vec3 velocity=getDeltaMovement();setPos(position().add(velocity));travelled+=velocity.length();
        // A thrown crystal falls like a stone; a barrage crystal flies straight.
        if (!barrage()) setDeltaMovement(velocity.scale(.99).add(0,-BloodCrystalCharge.gravity(points()),0));
        updateRotation();
        if (!level().isClientSide && (++flightTicks>100 || travelled>RANGE)) breakAt(SHATTER_BODY,yawOf(velocity),null);
    }

    /**
     * What the crystal meets this tick, traced from its middle (its box's bottom would meet the ground at once for a
     * great one held at the hand): the first body within its own half-width of the path, else the first block.
     */
    private HitResult nextHit() {
        Vec3 from=centre(), to=from.add(getDeltaMovement());
        HitResult block=level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        if (block.getType()!=HitResult.Type.MISS) to=block.getLocation();
        EntityHitResult body=ProjectileUtil.getEntityHitResult(level(),this,from,to,
                getBoundingBox().expandTowards(getDeltaMovement()).inflate(1),this::canHitEntity,getBbWidth()/2);
        return body!=null?body:block;
    }
    @Override protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target instanceof LivingEntity && target!=getOwner()
                && target.isAlive() && !target.isSpectator() && !(getOwner()!=null&&target.isAlliedTo(getOwner()));
    }
    @Override protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide||state()!=FLYING) return;
        Entity hit=result.getEntity();
        Vec3 dir=horizontal();
        if (hit instanceof LivingEntity target && getOwner() instanceof ServerPlayer player) {
            float before=target.getHealth();
            boolean hurt=target.hurt(source(player),unit*(float)(barrage()?1:BloodCrystalCharge.share(points())));
            if (hurt && target.getHealth()<before && !MurimGuardSkill.parriedThisTick(target) && !MurimGuardSkill.guardedThisTick(target))
                SanguineArt.projectileHit(player,target,barrage());
        }
        // The crystal breaks against the front of the body, at chest height, and its pieces follow it as they fall.
        double s=.375*crystalScale();
        Vec3 centre=new Vec3(hit.getX(),hit.getY()+Math.min(1.25,hit.getBbHeight()*.7),hit.getZ()).subtract(dir.scale(hit.getBbWidth()/2+s*.5));
        if (great()) { explode(centre,hit); return; }
        breakAt(SHATTER_BODY,yawOf(dir),centre);
        followed=hit; followOffset=position().subtract(hit.position());
    }
    @Override protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide||state()!=FLYING) return;
        Direction face=result.getDirection();
        double r=.375*crystalScale();
        Vec3 normal=Vec3.atLowerCornerOf(face.getNormal());
        Vec3 centre=result.getLocation().add(normal.scale(face==Direction.UP?r*.6:r));
        if (great()) { explode(centre,null); return; }
        if (face==Direction.UP) breakAt(SHATTER_GROUND,yawOf(horizontal()),centre);
        // Walls and ceilings: the model's -Z points into the surface, its pieces burst back off it.
        else breakAt(SHATTER_WALL,face.getAxis().isHorizontal()?yawOf(normal.scale(-1)):yawOf(horizontal()),centre);
    }
    private DamageSource source(ServerPlayer player) {
        return new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE),this,player);
    }
    private void breakAt(byte kind,float yaw,@Nullable Vec3 centre) {
        entityData.set(BREAK_YAW,yaw); setDeltaMovement(Vec3.ZERO);
        if (centre!=null) placeCentre(centre);
        entityData.set(STATE,kind); stateTicks=0;
        level().playSound(null,getX(),getY(),getZ(),SoundEvents.AMETHYST_CLUSTER_BREAK,SoundSource.PLAYERS,
                .5F+.25F*crystalScale(),barrage()?1.5F:1.25F-.1F*crystalScale());
    }
    /**
     * The great crystal explodes: everything round it within 3.5 blocks (but the thrower, their allies and the body it
     * struck, already hurt by the crystal) takes a share of 2 at the centre down to 1 at the edge and is thrown back.
     * No block is broken.
     */
    private void explode(Vec3 centre,@Nullable Entity struck) {
        breakAt(EXPLODE,yawOf(horizontal()),centre);
        if (!(getOwner() instanceof ServerPlayer player)) return;
        double radius=BloodCrystalCharge.EXPLOSION_RADIUS;
        level().playSound(null,centre.x,centre.y,centre.z,SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,1.6F,.85F);
        for (LivingEntity target:level().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(centre,centre).inflate(radius))) {
            if (target==player||target==struck||target.isAlliedTo(player)||!target.isAlive()||target.isSpectator()) continue;
            if (!(target instanceof Enemy||target instanceof Player||target instanceof net.minecraft.world.entity.Mob)) continue;
            double distance=target.position().add(0,target.getBbHeight()/2,0).distanceTo(centre);
            double share=BloodCrystalCharge.explosionShare(distance);
            if (share<=0) continue;
            float before=target.getHealth();
            if (target.hurt(source(player),unit*(float)share) && target.getHealth()<before) SanguineArt.projectileHit(player,target,false);
            Vec3 away=target.position().subtract(centre).multiply(1,0,1);
            if (away.lengthSqr()<1.0E-4) away=horizontal();
            double push=.9*(1-distance/radius)*Math.max(0,1-target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (push>0) { away=away.normalize(); target.push(away.x*push,.35*push,away.z*push); target.hurtMarked=true; }
        }
    }
    private Vec3 horizontal() {
        Vec3 v=getDeltaMovement().multiply(1,0,1);
        if (v.lengthSqr()<1.0E-6) v=Vec3.directionFromRotation(0,getYRot()).multiply(1,0,1);
        return v.normalize();
    }
    /** Render yaw (degrees, around +Y) that turns the model's -Z toward `dir`. */
    static float yawOf(Vec3 dir) { return (float)Math.toDegrees(Mth.atan2(-dir.x,-dir.z)); }

    @Override public boolean shouldBeSaved() {return false;}
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"crystal",0,event -> {
            var crystal=event.getAnimatable();
            if (crystal.dorsal()) return event.setAndContinue(HOVER);
            if (crystal.broken()) return event.setAndContinue(BREAKS[crystal.state()-SHATTER_GROUND]);
            return event.setAndContinue(crystal.state()==CHARGING?FORM:FLY);
        }));
    }
}
