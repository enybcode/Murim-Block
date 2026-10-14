package com.murimblock.martial;

import com.murimblock.combat.CombatService;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/** Server combat state; the synchronized attachment drives the supplied body overlay. */
public final class SanguineArt {
    private static final class Charge {
        final ServerPlayer owner;
        final BloodCrystalEntity crystal;
        final long started;
        final int slot;
        /** Points of the blood gauge drawn into the crystal so far. */
        int points;
        long throwAt=-1, launchAt=-1;
        Charge(ServerPlayer owner,int slot) {
            this.owner=owner;this.slot=slot;started=owner.level().getGameTime();
            crystal=BloodCrystalEntity.attached(owner,BloodCrystalEntity.CHARGING,0);
        }
    }
    private static final Map<UUID,ServerPlayer> USERS=new HashMap<>();
    private static final Map<UUID,Charge> CHARGES=new HashMap<>();
    private static final Map<UUID,List<BloodCrystalEntity>> CRYSTALS=new HashMap<>(), SALVOS=new HashMap<>();
    private static final Set<UUID> CREDITED=new HashSet<>();
    private SanguineArt() { }

    public static BloodCrystalState state(Player p) {return p.getData(BloodCrystalAttachments.STATE);}
    public static boolean bareHanded(Player p) {return p.getMainHandItem().isEmpty()&&p.getOffhandItem().isEmpty();}
    public static boolean stanceActive(Player p) {
        return (p.level().isClientSide?MeditationService.aura(p).mainArt():MartialService.data(p).mainArt())
                .filter(MartialArts.SANGUINE_ART::equals).isPresent();
    }
    public static boolean guardsBareHanded(Player p) {return bareHanded(p)&&stanceActive(p);}
    private static boolean eligible(ServerPlayer p) {
        return p.isAlive()&&!p.isSpectator()&&guardsBareHanded(p)&&CombatService.isInCombatMode(p)
                &&!KnockdownService.isHindered(p)&&!KnockdownService.isCarried(p)&&!MeditationService.isMeditating(p);
    }
    private static void set(ServerPlayer p,BloodCrystalState state) {
        USERS.put(p.getUUID(),p);
        if(!state(p).equals(state))p.setData(BloodCrystalAttachments.STATE,state);
    }
    @Nullable public static List<AnimationAccessor<? extends AttackAnimation>> autoAttackMotions(PlayerPatch<?> patch,CapabilityItem item) {
        if(patch==null||!guardsBareHanded(patch.getOriginal())||item.getWeaponCategory()!=CapabilityItem.WeaponCategories.FIST
                ||MurimAnimations.SANGUINE_STRAIGHT==null)return null;
        return List.of(MurimAnimations.SANGUINE_STRAIGHT,MurimAnimations.SANGUINE_CROSS,MurimAnimations.SANGUINE_STRAIGHT,
                MurimAnimations.SANGUINE_RISE,MurimAnimations.SANGUINE_STRAIGHT,MurimAnimations.SANGUINE_RISE);
    }
    static boolean isStrike(AnimationAccessor<? extends StaticAnimation> a) {
        return a!=null&&(a==MurimAnimations.SANGUINE_STRAIGHT||a==MurimAnimations.SANGUINE_CROSS||a==MurimAnimations.SANGUINE_RISE);
    }
    @Nullable static ResourceLocation creditOf(ServerPlayer p,AnimationAccessor<? extends StaticAnimation> a) {
        return isStrike(a)&&guardsBareHanded(p)?MartialArts.CRIMSON_STANCE.id():null;
    }
    public static boolean hasCharge(Player p) { return CHARGES.containsKey(p.getUUID()); }
    public static boolean charging(Player p) {
        var patch=EpicFightBridge.patch(p);
        var clock=patch==null?null:patch.getAnimator().getPlayerFor(MurimAnimations.SANGUINE_CRYSTAL_CHARGE);
        return clock!=null&&!clock.isEmpty()&&clock.getRealAnimation()==MurimAnimations.SANGUINE_CRYSTAL_CHARGE;
    }
    public static boolean aim(ServerPlayer p,int slot) {
        if(!eligible(p)||CHARGES.containsKey(p.getUUID()))return false;
        var patch=EpicFightBridge.patch(p);if(patch==null)return false;
        USERS.put(p.getUUID(),p);CHARGES.put(p.getUUID(),new Charge(p,slot));
        patch.playAnimationSynchronized(MurimAnimations.SANGUINE_CRYSTAL_CHARGE,0);return true;
    }
    static boolean perform(ServerPlayer p,PlayerPatch<?> patch,MartialArts.Technique t) {
        if(!eligible(p))return false;
        if(t==MartialArts.BLOOD_CRYSTAL) {
            if(!CHARGES.containsKey(p.getUUID())&&!aim(p,-1))return false;
            Charge c=CHARGES.get(p.getUUID());if(c.throwAt>=0||c.launchAt>=0)return false;
            c.throwAt=Math.max(p.level().getGameTime(),c.started+MartialArts.BLOOD_CRYSTAL_MIN_CHARGE);return true;
        }
        AnimationAccessor<? extends StaticAnimation> a;
        if(t==MartialArts.SCARLET_TEAR)a=MurimAnimations.SCARLET_TEAR;
        else if(t==MartialArts.BLOOD_CRYSTAL_FORM) {
            var before=state(p);var after=before.activate();if(before.equals(after))return false;
            set(p,after);a=MurimAnimations.SANGUINE_CRYSTAL_FORM;
        } else if(t==MartialArts.CRYSTAL_BARRAGE) {
            var before=state(p);if(!before.active()||before.crystals()==0)return false;
            var crystals=CRYSTALS.get(p.getUUID());
            if(crystals==null||crystals.stream().filter(e->!e.isRemoved()).count()!=before.crystals())return false;
            CRYSTALS.remove(p.getUUID());set(p,before.consume());a=MurimAnimations.SANGUINE_CRYSTAL_BARRAGE;
            float damage=Math.max(.5F,TechniquePower.damage(p,MartialArts.SANGUINE_ART,.45));
            for(int i=0;i<crystals.size();i++)crystals.get(i).queue(6+i*2,damage);
            SALVOS.put(p.getUUID(),crystals);
        } else return false;
        MartialCombat.expect(p,a,t.id(),false);patch.playAnimationSynchronized(a,0);return true;
    }
    static void animationStarted(ServerPlayer p,AnimationAccessor<? extends StaticAnimation> a) {
        CREDITED.remove(p.getUUID());
        if(a!=MurimAnimations.SANGUINE_CRYSTAL_CHARGE&&a!=MurimAnimations.SANGUINE_CRYSTAL_THROW)cancelCharge(p.getUUID(),false);
        if(a!=MurimAnimations.SANGUINE_CRYSTAL_BARRAGE)cancelSalvo(p.getUUID());
    }
    static void onHit(ServerPlayer p,LivingEntity target,AnimationAccessor<? extends StaticAnimation> a,boolean ignored) {
        if(!eligible(p)||target==p||target.isAlliedTo(p)||!(target instanceof Enemy||target instanceof Player)
                ||MurimGuardSkill.guardedThisTick(target)||MurimGuardSkill.parriedThisTick(target)
                ||target instanceof Player other&&KnockdownService.isDowned(other))return;
        if(a==MurimAnimations.SANGUINE_RISE)MartialCombat.lift(target,.35);
        if((!isStrike(a)&&a!=MurimAnimations.SCARLET_TEAR)||!CREDITED.add(p.getUUID()))return;
        var before=state(p);
        if(isStrike(a)) {
            set(p,before.gain(1));
            if(before.active()) {
                if(before.crystals()<BloodCrystalState.MAX_CRYSTALS)CRYSTALS.computeIfAbsent(p.getUUID(),id->new ArrayList<>())
                        .add(BloodCrystalEntity.attached(p,BloodCrystalEntity.ORBIT,before.crystals()));
                MartialProgress.count(p,MartialArts.CRYSTAL_MASTERY_HITS,1);
            }
        } else {
            if(!before.active())set(p,before.gain(3));
            MartialProgress.count(p,MartialArts.SCARLET_RELEASE_HITS,1);
        }
    }
    static void projectileHit(ServerPlayer p,LivingEntity target,boolean barrage) {
        if(target==p||target.isAlliedTo(p)||!(target instanceof Enemy||target instanceof Player))return;
        MartialProgress.addMastery(p,MartialArts.art(MartialArts.SANGUINE_ART),(barrage?MartialArts.CRYSTAL_BARRAGE:MartialArts.BLOOD_CRYSTAL).id(),1);
        if(!barrage)MartialProgress.count(p,MartialArts.CRIMSON_CONTACT_HITS,1);
    }
    public static void defend(LivingDamageEvent.Pre event) {
        if(event.getEntity() instanceof ServerPlayer p&&eligible(p)&&state(p).active()
                &&!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            event.setNewDamage(event.getNewDamage()*state(p).damageFactor());
    }
    /** Epic Fight's Tool_R trail transform, with world coordinates kept in doubles. */
    public static Vec3 handPosition(Player p,float partialTick) {
        var patch=EpicFightBridge.patch(p);if(patch==null)return p.getEyePosition().add(p.getLookAngle().scale(.5));
        var armature=patch.getArmature();var joint=armature.searchJointByName("Tool_R");
        var at=OpenMatrix4f.transform(armature.getBoundTransformFor(patch.getAnimator().getPose(partialTick),joint),new Vec3(0,.08,-.08));
        at=OpenMatrix4f.transform(patch.getModelMatrix(partialTick),at);
        return p.getPosition(partialTick).add(-at.x,at.y,-at.z);
    }
    static void tick(ServerPlayer p) {
        UUID id=p.getUUID();ServerPlayer tracked=USERS.get(id);
        if(!eligible(p)||tracked!=null&&tracked!=p){forget(id);return;}
        Charge c=CHARGES.get(id);long now=p.level().getGameTime();
        if(c!=null) {
            if(c.owner.level()!=c.crystal.level()||c.crystal.isRemoved()){cancelCharge(id);return;}
            long held=now-c.started;
            // Still held: past the minimum charge, the crystal draws the blood gauge into itself, a point at a time.
            if(c.throwAt<0&&BloodCrystalCharge.drawsPoint(held,c.points)) {
                var gauge=state(p);
                if(!gauge.active()&&gauge.gauge()>0){set(p,gauge.spend(1));c.points++;}
            }
            c.crystal.charge(BloodCrystalCharge.scale(held,c.points),c.points);
            if(c.launchAt>=0) {
                if(now>=c.launchAt){c.crystal.launch(Math.max(.5F,TechniquePower.damage(p,MartialArts.SANGUINE_ART,1)),false);CHARGES.remove(id);}
            } else if(c.throwAt>=0&&now>=c.throwAt) {
                c.launchAt=now+6;EpicFightBridge.patch(p).playAnimationSynchronized(MurimAnimations.SANGUINE_CRYSTAL_THROW,0);
            } else if(c.throwAt<0&&now-c.started>=MartialArts.BLOOD_CRYSTAL_MAX_CHARGE&&c.slot>=0) {
                if (!MartialService.data(p).slot(c.slot).filter(MartialArts.BLOOD_CRYSTAL.id()::equals).isPresent()
                        || MartialService.castSlot(p,c.slot)!=MartialService.CastResult.CAST) cancelCharge(id);
                else if (p.connection != null) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,
                        new com.murimblock.network.TechniquePayloads.Cooldown(MartialArts.BLOOD_CRYSTAL.id(),MartialService.cooldownLeft(p,MartialArts.BLOOD_CRYSTAL.id())));
            }
        }
        var salvo=SALVOS.get(id);
        if(salvo!=null){salvo.removeIf(e->e.isRemoved()||e.state()==BloodCrystalEntity.FLYING);if(salvo.isEmpty())SALVOS.remove(id);}
        var orbit=CRYSTALS.get(id);
        if(orbit!=null&&orbit.stream().anyMatch(e->e.isRemoved()||e.level()!=p.level()))forget(id);
    }
    private static void cancelCharge(UUID id){cancelCharge(id,true);}
    private static void cancelCharge(UUID id,boolean releasePose) {
        var c=CHARGES.remove(id);if(c==null)return;c.crystal.discard();
        if(releasePose&&c.owner.isAlive()&&!KnockdownService.isHindered(c.owner)&&charging(c.owner))
            EpicFightBridge.patch(c.owner).playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_IDLE,0);
    }
    private static void cancelSalvo(UUID id){var cs=SALVOS.remove(id);if(cs!=null)cs.forEach(e->{if(e.state()!=BloodCrystalEntity.FLYING)e.discard();});}
    static void forget(@Nullable UUID id) {
        if(id==null){for(UUID user:List.copyOf(USERS.keySet()))forget(user);CREDITED.clear();return;}
        var p=USERS.remove(id);if(p!=null)p.setData(BloodCrystalAttachments.STATE,BloodCrystalState.EMPTY);
        cancelCharge(id);cancelSalvo(id);CREDITED.remove(id);
        var orbit=CRYSTALS.remove(id);if(orbit!=null)orbit.forEach(BloodCrystalEntity::discard);
    }
}
