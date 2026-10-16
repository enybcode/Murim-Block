package com.colleuhu95.bloodsect.ninja;

import com.colleuhu95.bloodsect.BloodSectDecor;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative, grounded draw-cut. Each dash owns its hit and healing budgets. */
final class BloodStepAttack {
    static final int WINDUP=DaoAttackMotion.WINDUP, DASH_TICKS=4, COOLDOWN=110, MAX_TARGETS=4;
    static final double DISTANCE=5;
    private static final ResourceKey<DamageType> DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(BloodSectDecor.ID,"dao_cut"));
    private final BloodNinja ninja;
    private final Set<UUID> attempted=new HashSet<>();
    private LivingEntity target;
    private Vec3 direction=Vec3.ZERO;
    private long readyAt;
    private int movementTicks, hits;
    private boolean blocked, drained;

    BloodStepAttack(BloodNinja ninja) { this.ninja=ninja; }

    boolean start(LivingEntity target) {
        var world=ninja.level();
        double distance=ninja.distanceToSqr(target);
        if(ninja.isDaoAttacking() || world.getGameTime()<readyAt || !ninja.onGround() || ninja.isInWaterOrBubble()
            || !ninja.getMainHandItem().is(BloodSectDecor.BLOOD_DAO.get()) || !ninja.canAttack(target)
            || !target.isAlive() || distance>36 || Math.abs(target.getY()-ninja.getY())>1.25 || !ninja.hasLineOfSight(target)) return false;
        var delta=target.position().subtract(ninja.position()).multiply(1,0,1);
        if(delta.lengthSqr()<.01) return false;
        this.direction=delta.normalize();
        this.target=target;
        attempted.clear();movementTicks=0;hits=0;drained=false;blocked=false;
        readyAt=world.getGameTime()+COOLDOWN;
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        ninja.beginDaoAnimation(yaw);
        ninja.getNavigation().stop();
        ninja.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(),.65F,.65F);
        return true;
    }

    void tick() {
        if(target==null) return;
        // Another animation may start on the exact tick this one finishes.
        if(ninja.getDaoAttackVariant()!=DaoAttackMotion.CLOUD_STEP) { target=null;return; }
        float age=ninja.getDaoAttackAge(0);
        if(!ninja.isAlive() || ninja.isRemoved() || !ninja.getMainHandItem().is(BloodSectDecor.BLOOD_DAO.get())
            || (age<WINDUP && (ninja.getTarget()!=target || !target.isAlive() || !ninja.canAttack(target)))) {
            resetAnimation();return;
        }
        // Once released, finish the committed line even if its first victim dies.
        if(age>=DaoAttackMotion.DURATION) {target=null;return;}
        ninja.getNavigation().stop();
        if(age<WINDUP || age>=WINDUP+DASH_TICKS || blocked) return;
        if(movementTicks==0) ninja.playSound(SoundEvents.PLAYER_ATTACK_SWEEP,.95F,.62F);
        if(movementTicks++>=DASH_TICKS) return;
        for(int step=0;step<5;step++) {
            Vec3 before=ninja.position();
            Vec3 stride=direction.scale(DISTANCE/DASH_TICKS/5);
            AABB next=ninja.getBoundingBox().move(stride);
            Vec3 destination=before.add(stride);
            // Quarter-block collision checks prevent tunnelling, stepping through walls, or crossing a ledge.
            AABB feet=new AABB(next.minX+.04,next.minY-.22,next.minZ+.04,next.maxX-.04,next.minY+.01,next.maxZ-.04);
            if(!ninja.level().hasChunkAt(BlockPos.containing(destination))
                || !ninja.level().getWorldBorder().isWithinBounds(next)
                || destination.distanceToSqr(ninja.getGuardPost().getBottomCenter())>28*28
                || !ninja.level().noCollision(ninja,next)
                || !ninja.level().getBlockCollisions(ninja,feet).iterator().hasNext()
                || ninja.level().containsAnyLiquid(next)) {blocked=true;break;}
            ninja.move(MoverType.SELF,stride);
            Vec3 after=ninja.position();
            if(after.distanceToSqr(before)<1e-8) {blocked=true;break;}
            cutAlong(before,after);
            ninja.hasImpulse=true;
            if(after.subtract(before).distanceToSqr(stride)>1e-5) {blocked=true;break;}
        }
    }

    private void cutAlong(Vec3 from,Vec3 to) {
        if(hits>=MAX_TARGETS) return;
        var sweep=new AABB(from.x,from.y+.15,from.z,to.x,to.y+1.75,to.z).inflate(.65,0,.65);
        var victims=ninja.level().getEntitiesOfClass(LivingEntity.class,sweep,
            e->e!=ninja && !(e instanceof BloodNinja) && e.isAlive() && !ninja.isAlliedTo(e)
                && ninja.canAttack(e) && !attempted.contains(e.getUUID()));
        victims.sort(Comparator.comparingDouble(e->e.position().distanceToSqr(from)));
        var damageType=ninja.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE);
        for(var victim:victims) {
            if(hits>=MAX_TARGETS) break;
            // The broad phase is a box, but the narrow phase follows the swept blade corridor.
            Vec3 low=from.add(0,.9,0), high=to.add(0,.9,0);
            var targetBox=victim.getBoundingBox().inflate(.55,.4,.55);
            if(!(targetBox.contains(low) || targetBox.clip(low,high).isPresent()) || !ninja.hasLineOfSight(victim)) continue;
            attempted.add(victim.getUUID());
            float before=victim.getHealth();
            float damage=(float)ninja.getAttributeValue(Attributes.ATTACK_DAMAGE)*1.5F;
            boolean hurt=victim.hurt(new DamageSource(damageType,ninja),damage);
            if(!hurt || victim.getHealth()>=before) continue;
            hits++;
            ninja.setLastHurtMob(victim);
            if(!drained) {
                // One successful cut restores 15% of maximum health. Further targets
                // in the same dash cannot multiply the heal, even if the first heal was clamped.
                drained=true;
                float oldHealth=ninja.getHealth();
                ninja.heal(ninja.getMaxHealth()*.15F);
                if(ninja.getHealth()>oldHealth) ninja.level().broadcastEntityEvent(ninja,(byte)78);
            }
        }
    }
    private void resetAnimation() { target=null; ninja.endDaoAnimation(); }
    void reset() { target=null;readyAt=0;attempted.clear();ninja.endDaoAnimation(); }
}
