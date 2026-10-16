package com.murimblock.bloodsect.ninja;

import com.murimblock.bloodsect.BloodSect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** A planted diagonal dao cut. Its committed arc and impact are authoritative on the server. */
final class BloodDaoM1Attack {
    private static final int RELEASE=5, IMPACT=8, COOLDOWN=23;
    private static final double REACH=2.5, ARC_COS=Math.cos(Math.toRadians(55));
    private final BloodNinja ninja;
    private LivingEntity target;
    private Vec3 direction=Vec3.ZERO;
    private long readyAt;
    private boolean released, attempted;

    BloodDaoM1Attack(BloodNinja ninja) { this.ninja=ninja; }

    boolean start(LivingEntity target) {
        if(ninja.isDaoAttacking() || ninja.level().getGameTime()<readyAt || !ninja.onGround()
            || ninja.isInWaterOrBubble() || !ninja.getMainHandItem().is(BloodSect.BLOOD_DAO.get())
            || !canHit(target) || !ninja.hasLineOfSight(target)) return false;
        Vec3 delta=target.position().subtract(ninja.position()).multiply(1,0,1);
        if(delta.lengthSqr()<.01) return false;
        direction=delta.normalize();this.target=target;released=false;attempted=false;
        readyAt=ninja.level().getGameTime()+COOLDOWN;
        ninja.beginDaoAnimation((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)),DaoAttackMotion.M1);
        ninja.getNavigation().stop();
        ninja.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(),.3F,.85F);
        return true;
    }

    void tick() {
        if(target==null) return;
        if(ninja.getDaoAttackVariant()!=DaoAttackMotion.M1) {target=null;return;}
        float age=ninja.getDaoAttackAge(0);
        if(!ninja.isAlive() || ninja.isRemoved() || !ninja.getMainHandItem().is(BloodSect.BLOOD_DAO.get())
            || (!released && (ninja.getTarget()!=target || !target.isAlive() || !ninja.canAttack(target)))) {
            target=null;ninja.endDaoAnimation();return;
        }
        if(age>=DaoAttackMotion.duration(DaoAttackMotion.M1)) {target=null;return;}
        ninja.getNavigation().stop();
        if(age>=RELEASE && !released) {
            released=true;ninja.playSound(SoundEvents.PLAYER_ATTACK_SWEEP,.8F,.75F);
        }
        if(age<IMPACT || attempted) return;
        attempted=true;
        Vec3 delta=target.position().subtract(ninja.position()).multiply(1,0,1);
        if(!canHit(target) || ninja.getTarget()!=target || !ninja.hasLineOfSight(target)
            || (delta.lengthSqr()>.01 && direction.dot(delta.normalize())<ARC_COS)) return;
        float before=target.getHealth();
        boolean hit=target.hurt(ninja.damageSources().mobAttack(ninja),(float)ninja.getAttributeValue(Attributes.ATTACK_DAMAGE));
        if(hit && target.getHealth()<before) {
            ninja.playSound(SoundEvents.PLAYER_ATTACK_STRONG,.7F,.72F);
            // A confirmed wound creates droplets. Misses, shields and immunity create none.
            ((ServerLevel)ninja.level()).sendParticles(BloodSect.BLOOD_DROPLET.get(),
                target.getX()-direction.x*.2,target.getY()+target.getBbHeight()*.6,target.getZ()-direction.z*.2,
                15,.14,.18,.14,.12);
        }
    }

    private boolean canHit(LivingEntity victim) {
        return victim.isAlive() && !(victim instanceof BloodNinja) && !ninja.isAlliedTo(victim) && ninja.canAttack(victim)
            && ninja.distanceToSqr(victim)<=REACH*REACH && Math.abs(victim.getY()-ninja.getY())<=1.25;
    }
    void reset() {target=null;readyAt=0;}
}
