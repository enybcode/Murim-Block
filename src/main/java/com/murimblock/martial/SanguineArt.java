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
    /**
     * Scarlet Tear's blood sword, out in the hand until it is thrown; throwAt is the tick it leaves the hand once the
     * javelin throw has begun (-1 before).
     */
    private static final class Sword {
        final BloodCrystalEntity entity; long throwAt=-1;
        Sword(BloodCrystalEntity entity){this.entity=entity;}
    }
    private static final Map<UUID,Sword> SWORDS=new HashMap<>();
    /** The javelin throw lets the sword go 0.35 s in. */
    public static final int SWORD_RELEASE_TICKS=7;
    /** The blood sword in hand strikes like a sword: attack added while it is out. */
    public static final double SWORD_DAMAGE=5;
    private static final ResourceLocation SWORD_MODIFIER=ResourceLocation.fromNamespaceAndPath("murimblock","blood_sword");
    /**
     * The M1 chain while the blood sword is out: Epic Fight's base sword combo (three cuts, the dash and the air slash),
     * as for any sword (Enzo, 15/10/2026). Their hit sparks are silenced for the blood sword ({@link #silencesHit}).
     */
    private static List<AnimationAccessor<? extends AttackAnimation>> swordChain() {
        return List.of(yesman.epicfight.gameasset.Animations.SWORD_AUTO1,yesman.epicfight.gameasset.Animations.SWORD_AUTO2,
                yesman.epicfight.gameasset.Animations.SWORD_AUTO3,yesman.epicfight.gameasset.Animations.SWORD_DASH,
                yesman.epicfight.gameasset.Animations.SWORD_AIR_SLASH);
    }
    static boolean isSwordStrike(AnimationAccessor<? extends StaticAnimation> a) { return a!=null&&swordChain().contains(a); }
    /** The Sanguine Art shows no particles: a blood sword's cut spawns no Epic Fight hit sparks. */
    public static boolean silencesHit(net.minecraft.world.entity.Entity attacker,AnimationAccessor<? extends StaticAnimation> a) {
        return attacker instanceof Player p&&isSwordStrike(a)&&holdsSword(p);
    }
    /**
     * Whether the player's blood sword is out (not yet thrown). The server keeps it; a client sees it as the sword
     * entity in the player's hand.
     */
    public static boolean holdsSword(Player p) {
        if(!p.level().isClientSide)return SWORDS.containsKey(p.getUUID());
        return !p.level().getEntitiesOfClass(BloodCrystalEntity.class,p.getBoundingBox().inflate(4),
                e->e.sword()&&e.state()==BloodCrystalEntity.CHARGING&&e.visualOwner()==p).isEmpty();
    }
    /** Scarlet Tear's second press: the javelin throw. False if no sword is out or it is already on its way. */
    public static boolean throwSword(ServerPlayer p) {
        Sword sword=SWORDS.get(p.getUUID());
        if(sword==null||sword.throwAt>=0||!eligible(p))return false;
        var patch=EpicFightBridge.patch(p);if(patch==null)return false;
        sword.throwAt=p.level().getGameTime()+SWORD_RELEASE_TICKS;
        patch.playAnimationSynchronized(MurimAnimations.SANGUINE_SWORD_THROW,0);
        return true;
    }
    /** A thrown sword landed on someone: Scarlet Tear's 3 points of gauge (outside the form), once a throw. */
    static void swordLanded(ServerPlayer p) {
        var before=state(p);if(!before.active())set(p,before.gain(3));
    }
    private static void arm(ServerPlayer p,boolean on) {
        var attack=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);if(attack==null)return;
        attack.removeModifier(SWORD_MODIFIER);
        if(on)attack.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SWORD_MODIFIER,SWORD_DAMAGE,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
    }
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
        // The blood sword out: it cuts as a sword.
        if(holdsSword(patch.getOriginal()))return swordChain();
        return List.of(MurimAnimations.SANGUINE_STRAIGHT,MurimAnimations.SANGUINE_CROSS,MurimAnimations.SANGUINE_STRAIGHT,
                MurimAnimations.SANGUINE_RISE,MurimAnimations.SANGUINE_STRAIGHT,MurimAnimations.SANGUINE_RISE);
    }
    static boolean isStrike(AnimationAccessor<? extends StaticAnimation> a) {
        return a!=null&&(a==MurimAnimations.SANGUINE_STRAIGHT||a==MurimAnimations.SANGUINE_CROSS||a==MurimAnimations.SANGUINE_RISE);
    }
    @Nullable static ResourceLocation creditOf(ServerPlayer p,AnimationAccessor<? extends StaticAnimation> a) {
        return (isStrike(a)||holdsSword(p)&&isSwordStrike(a))&&guardsBareHanded(p)?MartialArts.CRIMSON_STANCE.id():null;
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
        if(t==MartialArts.SCARLET_TEAR)a=MurimAnimations.SANGUINE_SWORD_SUMMON;
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
        MartialCombat.expect(p,a,t.id(),false);patch.playAnimationSynchronized(a,0);
        // Scarlet Tear: the blood sword forms in the hand and stays out, a sword to fight with, until thrown.
        if(t==MartialArts.SCARLET_TEAR){USERS.put(p.getUUID(),p);cancelSword(p.getUUID());SWORDS.put(p.getUUID(),new Sword(BloodCrystalEntity.sword(p)));arm(p,true);}
        return true;
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
        boolean strike=isStrike(a)||holdsSword(p)&&isSwordStrike(a);
        if(!strike||!CREDITED.add(p.getUUID()))return;
        var before=state(p);
        {
            set(p,before.gain(1));
            if(before.active()) {
                if(before.crystals()<BloodCrystalState.MAX_CRYSTALS)CRYSTALS.computeIfAbsent(p.getUUID(),id->new ArrayList<>())
                        .add(BloodCrystalEntity.attached(p,BloodCrystalEntity.ORBIT,before.crystals()));
                MartialProgress.count(p,MartialArts.CRYSTAL_MASTERY_HITS,1);
            }
        }
    }
    static void projectileHit(ServerPlayer p,LivingEntity target,MartialArts.Technique technique) {
        if(target==p||target.isAlliedTo(p)||!(target instanceof Enemy||target instanceof Player))return;
        MartialProgress.addMastery(p,MartialArts.art(MartialArts.SANGUINE_ART),technique.id(),1);
        if(technique==MartialArts.BLOOD_CRYSTAL)MartialProgress.count(p,MartialArts.CRIMSON_CONTACT_HITS,1);
        if(technique==MartialArts.SCARLET_TEAR)MartialProgress.count(p,MartialArts.SCARLET_RELEASE_HITS,1);
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
        var sword=SWORDS.get(id);
        if(sword!=null) {
            if(sword.entity.isRemoved()){SWORDS.remove(id);arm(p,false);}
            else if(sword.throwAt>=0&&now>=sword.throwAt) {
                SWORDS.remove(id);arm(p,false);
                sword.entity.launch(Math.max(.5F,TechniquePower.damage(p,MartialArts.SANGUINE_ART,1)),false);
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
    private static void cancelSword(UUID id) {
        var s=SWORDS.remove(id);if(s==null)return;
        if(s.entity.state()==BloodCrystalEntity.CHARGING)s.entity.discard();
        var p=USERS.get(id);if(p!=null)arm(p,false);
    }
    private static void cancelSalvo(UUID id){var cs=SALVOS.remove(id);if(cs!=null)cs.forEach(e->{if(e.state()!=BloodCrystalEntity.FLYING)e.discard();});}
    static void forget(@Nullable UUID id) {
        if(id==null){for(UUID user:List.copyOf(USERS.keySet()))forget(user);CREDITED.clear();return;}
        cancelSword(id);
        var p=USERS.remove(id);if(p!=null)p.setData(BloodCrystalAttachments.STATE,BloodCrystalState.EMPTY);
        cancelCharge(id);cancelSalvo(id);CREDITED.remove(id);
        var orbit=CRYSTALS.remove(id);if(orbit!=null)orbit.forEach(BloodCrystalEntity::discard);
    }
}
