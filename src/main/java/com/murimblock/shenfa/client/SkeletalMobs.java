package com.murimblock.shenfa.client;

import com.murimblock.Murimblock;
import com.murimblock.client.MurimClientConfig;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Armature;
import com.murimblock.shenfa.SkinnedModel;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.BipedPoses;
import com.murimblock.shenfa.JointTransform;
import com.murimblock.shenfa.LivingMotion;
import com.murimblock.shenfa.MotionSet;
import com.murimblock.shenfa.Pose;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Quaternionf;

/**
 * Vanilla mobs drawn with a look of ours on the biped's skeleton ({@code docs/mobs/}): the same mob (id, spawns,
 * drops, behaviour), a new model, texture and motion. The zombie is the Disciple déchu, the skeleton the Archer
 * impérial (aiming its bow like vanilla's until it has clips of its own).
 * <p>
 * A look with clips of its own ({@code skeletal/animations/<set>.gltf}, motions in {@code skeletal/motions/<set>.json})
 * moves with them: its living motions, its combat stance while it is aggressive, the moves the server picks
 * ({@code DiscipleFighting}), a flinch when it is hurt, its death. A look without clips moves with the biped's motions and
 * reaches and swings like vanilla's zombie. {@code shenfaMobs = false} in the client config brings vanilla's models back.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SkeletalMobs {
    /** A mob's look: its model (skeletal/), texture, glowing parts (or null) and clip set (or null). */
    public record Look(ResourceLocation model, ResourceLocation texture, ResourceLocation glow, String clips) {
        ResourceLocation clip(String name) {
            return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, clips + "/" + name);
        }

        ResourceLocation motions() {
            return clips == null ? PlayerAnimators.MOTIONS : ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, clips);
        }
    }

    public static final Look DISCIPLE = new Look(id("mobs/disciple"), id("textures/entity/disciple.png"),
            id("textures/entity/disciple_glow.png"), "disciple");
    public static final Look ARCHER = new Look(id("mobs/archer"), id("textures/entity/archer.png"), null, "archer");
    /** The stray as the Esprit de l'Archer: the archer's clips on a body of mist, drawn see-through. */
    public static final Look ESPRIT = new Look(id("mobs/esprit"), id("textures/entity/esprit.png"),
            id("textures/entity/esprit_glow.png"), "archer");
    /** The zombie family: the disciple's clips on looks of their own ({@code docs/mobs/zombie-family.png}). */
    public static final Look SABLES = disciple("sables"), NOYE = disciple("noye"), PAYSAN = disciple("paysan"),
            SANGLIER = disciple("sanglier");
    /** The spider, on a skeleton of its own. */
    public static final Look FILEUSE = new Look(id("mobs/spider"), id("textures/entity/spider.png"),
            id("textures/entity/spider_glow.png"), "spider");
    /** The creeper, on a skeleton of its own. */
    public static final Look FOURNEAU = new Look(id("mobs/furnace"), id("textures/entity/furnace.png"),
            id("textures/entity/furnace_glow.png"), "furnace");
    /** The Enderman as the Moine du Vide: the biped's joints stretched to its height, a joint per rosary bead. */
    public static final Look MOINE = new Look(id("mobs/moine"), id("textures/entity/moine.png"),
            id("textures/entity/moine_glow.png"), "moine");
    /** The Iron Golem as the Gardien-talisman: the biped's joints set to its build, a joint for its talisman. */
    public static final Look GOLEM = new Look(id("mobs/golem"), id("textures/entity/golem.png"),
            id("textures/entity/golem_glow.png"), "golem");
    private static final Map<EntityType<?>, Look> LOOKS = Map.ofEntries(Map.entry(EntityType.ZOMBIE, DISCIPLE),
            Map.entry(EntityType.SKELETON, ARCHER), Map.entry(EntityType.HUSK, SABLES), Map.entry(EntityType.DROWNED, NOYE),
            Map.entry(EntityType.ZOMBIE_VILLAGER, PAYSAN), Map.entry(EntityType.ZOMBIFIED_PIGLIN, SANGLIER),
            Map.entry(EntityType.SPIDER, FILEUSE), Map.entry(EntityType.CREEPER, FOURNEAU), Map.entry(EntityType.ENDERMAN, MOINE),
            Map.entry(EntityType.IRON_GOLEM, GOLEM), Map.entry(EntityType.STRAY, ESPRIT));
    private static final float MOVING = 0.6f, RUNNING = 4.5f;
    /** A baby's head, relative to its half-size body. */
    private static final float BABY_HEAD = 1.5f;
    private static final Map<Mob, State> STATES = new WeakHashMap<>();
    private static Boolean enabled;
    /** Dev: told of each move a mob starts (strike, double, flinch, death). */
    public static java.util.function.BiConsumer<Mob, String> moveProbe;

    private SkeletalMobs() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    private static Look disciple(String name) {
        return new Look(id("mobs/" + name), id("textures/entity/" + name + ".png"),
                id("textures/entity/" + name + "_glow.png"), "disciple");
    }

    private static final class State {
        // The look's own skeleton, or the biped's (whose helpers settle after each pose).
        final Armature armature;
        final boolean biped;
        final Animator animator;

        State(Armature armature) {
            this.armature = armature;
            biped = armature == Biped.ARMATURE;
            animator = new Animator(armature, biped ? Biped::settleHelpers : null);
        }

        LivingMotion motion = LivingMotion.IDLE;
        float speed;
        int airTicks;
        /** How far the arms are raised toward the target (0 to 1), now and a tick ago (looks without clips). */
        float raise, previousRaise;
        boolean dying;
        int hurtTime;
        /** A creeper's fuse: 1 while it swells, -1 while it cools (or idles). */
        int swellDir = -1;
    }

    /**
     * The skeleton {@code look} moves: its model's when it brings its own (no chest, or the Moine's stretched biped),
     * else the biped's; the biped's too until the model is loaded.
     */
    static Armature armature(Look look) {
        SkinnedModel model = look == null ? null : SkeletalModels.get(look.model());
        return model == null || model.armature().find(Biped.CHEST) != null && look != MOINE && look != GOLEM
                ? Biped.ARMATURE
                : model.armature();
    }

    /** Whether a mob's glowing parts are lit: the Gardien's eyes and runes only once it is woken. */
    static boolean glows(Mob mob, Look look) {
        return look != GOLEM || mob.isAggressive() || mob.getTarget() != null;
    }

    private static State state(Mob mob) {
        Armature armature = armature(LOOKS.get(mob.getType()));
        State state = STATES.get(mob);
        if (state == null || state.armature != armature) {
            state = new State(armature);
            STATES.put(mob, state);
        }
        return state;
    }

    /** Whether mobs are drawn with our looks (the client config, unless a dev command overrides it). */
    public static boolean enabled() {
        return enabled != null ? enabled : MurimClientConfig.shenfaMobs();
    }

    public static void enable(Boolean on) {
        enabled = on;
    }

    /** Whether {@code look} is drawn see-through (its texture's alpha blended, not cut out). */
    public static boolean translucent(Look look) {
        return look == ESPRIT;
    }

    /** The look {@code entity} wears, or null to draw it as vanilla does. */
    public static Look look(Entity entity) {
        return enabled() ? LOOKS.get(entity.getType()) : null;
    }

    /** For checks: the clip {@code mob} plays at {@code priority}, or null. */
    public static Animation playing(Mob mob, Animator.Priority priority) {
        State state = STATES.get(mob);
        return state == null ? null : state.animator.playing(priority);
    }

    /** For checks: the clip of {@code mob}'s living motion, or null. */
    public static Animation baseAnimation(Mob mob) {
        State state = STATES.get(mob);
        return state == null ? null : state.animator.baseAnimation();
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.ZOMBIE, context -> new SkeletalMobRenderer<>(context,
                new net.minecraft.client.renderer.entity.ZombieRenderer(context)));
        event.registerEntityRenderer(EntityType.SKELETON, context -> new SkeletalMobRenderer<>(context,
                new net.minecraft.client.renderer.entity.SkeletonRenderer(context)));
        event.registerEntityRenderer(EntityType.STRAY, context -> new SkeletalMobRenderer<>(context,
                new net.minecraft.client.renderer.entity.StrayRenderer(context)));
        event.registerEntityRenderer(EntityType.HUSK, context -> new SkeletalMobRenderer<>(context,
                new net.minecraft.client.renderer.entity.HuskRenderer(context)));
        event.registerEntityRenderer(EntityType.DROWNED, context -> new SkeletalMobRenderer<>(context,
                new net.minecraft.client.renderer.entity.DrownedRenderer(context)));
        event.registerEntityRenderer(EntityType.ZOMBIE_VILLAGER, context -> new SkeletalMobRenderer<>(context,
                new net.minecraft.client.renderer.entity.ZombieVillagerRenderer(context)));
        event.registerEntityRenderer(EntityType.CREEPER, context -> new SkeletalMobRenderer<>(context,
                new net.minecraft.client.renderer.entity.CreeperRenderer(context)));
        event.registerEntityRenderer(EntityType.SPIDER, context -> new SkeletalMobRenderer<>(context,
                new net.minecraft.client.renderer.entity.SpiderRenderer<>(context)));
        event.registerEntityRenderer(EntityType.IRON_GOLEM, context -> new SkeletalMobRenderer<>(context,
                new net.minecraft.client.renderer.entity.IronGolemRenderer(context)));
        event.registerEntityRenderer(EntityType.ENDERMAN, context -> new SkeletalMobRenderer<>(context,
                new net.minecraft.client.renderer.entity.EndermanRenderer(context)));
        event.registerEntityRenderer(EntityType.ZOMBIFIED_PIGLIN, context -> new SkeletalMobRenderer<>(context,
                new net.minecraft.client.renderer.entity.PiglinRenderer(context,
                        net.minecraft.client.model.geom.ModelLayers.ZOMBIFIED_PIGLIN,
                        net.minecraft.client.model.geom.ModelLayers.ZOMBIFIED_PIGLIN_INNER_ARMOR,
                        net.minecraft.client.model.geom.ModelLayers.ZOMBIFIED_PIGLIN_OUTER_ARMOR, true)));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || !enabled()) {
            return;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Mob mob && LOOKS.containsKey(mob.getType())) {
                Look look = LOOKS.get(mob.getType());
                State state = state(mob);
                update(mob, look, state);
                if (look.clips() != null) {
                    moves(mob, look, state);
                }
                state.animator.tick(1);
                ClipEvents.run(mob, state.animator.events());
                state.previousRaise = state.raise;
                state.raise += ((mob.isAggressive() ? 1 : 0) - state.raise) * 0.25f;
            }
        }
    }

    /** Flinches and the death, as the mob is hurt and dies; its blows come from the server ({@link #play}). */
    private static void moves(Mob mob, Look look, State state) {
        if (mob.deathTime > 0) {
            if (!state.dying) {
                state.dying = true;
                state.animator.stop(Animator.Priority.MIDDLE);
                state.animator.stop(Animator.Priority.LOW);
                play(mob, state, look, Animator.Priority.HIGHEST, "death");
            }
            return;
        }
        if (mob.hurtTime > state.hurtTime) {
            play(mob, state, look, Animator.Priority.LOW, "flinch");
        }
        state.hurtTime = mob.hurtTime;
        if (mob instanceof Creeper creeper) {
            fuse(creeper, look, state);
        }
        if (look == ESPRIT) {
            trail(mob);
        }
    }

    /**
     * The Esprit de l'Archer's mist: flakes shed from the end of its tail, more as it glides, and now and then a
     * frosted breath from the hood.
     */
    private static void trail(Mob mob) {
        var random = mob.getRandom();
        double moved = mob.getDeltaMovement().horizontalDistance();
        if (random.nextFloat() < 0.12f + moved * 3) {
            mob.level().addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                    mob.getX() + (random.nextFloat() - 0.5f) * 0.3, mob.getY() + 0.25 + random.nextFloat() * 0.3,
                    mob.getZ() + (random.nextFloat() - 0.5f) * 0.3, 0, -0.01, 0);
        }
        if (random.nextInt(60) == 0) {
            float yaw = mob.getYHeadRot() * Mth.DEG_TO_RAD;
            mob.level().addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                    mob.getX() - Mth.sin(yaw) * 0.35, mob.getEyeY() - 0.15, mob.getZ() + Mth.cos(yaw) * 0.35,
                    -Mth.sin(yaw) * 0.04, 0.01, Mth.cos(yaw) * 0.04);
        }
    }

    /**
     * The Fourneau's fuse, like a kettle's: its clip as it starts to swell (the heat lifts the lid, then it bursts),
     * a long cooling one if it calms down first; steam escapes under the lid, more as it swells.
     */
    private static void fuse(Creeper creeper, Look look, State state) {
        int dir = creeper.getSwellDir() > 0 ? 1 : -1;
        float swell = creeper.getSwelling(1);
        if (dir != state.swellDir) {
            if (dir > 0) {
                play(creeper, state, look, Animator.Priority.MIDDLE, "fuse");
            } else if (swell > 0) {
                play(creeper, state, look, Animator.Priority.MIDDLE, "calm");
            }
            state.swellDir = dir;
        }
        if (swell > 0.15f && creeper.getRandom().nextFloat() < swell) {
            float yaw = creeper.yBodyRot * Mth.DEG_TO_RAD, side = (creeper.getRandom().nextFloat() - 0.5f) * 0.7f;
            double x = creeper.getX() - Mth.sin(yaw) * 0.1 + Mth.cos(yaw) * side;
            double z = creeper.getZ() + Mth.cos(yaw) * 0.1 + Mth.sin(yaw) * side;
            creeper.level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, x,
                    creeper.getY() + creeper.getBbHeight() * 0.95, z, Mth.cos(yaw) * side * 0.15, 0.06 + swell * 0.08,
                    Mth.sin(yaw) * side * 0.15);
        }
    }

    private static void play(Mob mob, State state, Look look, Animator.Priority priority, String clip) {
        if (moveProbe != null) {
            moveProbe.accept(mob, clip);
        }
        Animation animation = SkeletalClips.get(look.clip(clip));
        if (animation != null) {
            state.animator.play(priority, animation, 1, false);
        }
    }

    /** A clip the server tells {@code mob} to play on {@code layer} (null stops the layer): its fighting moves. */
    public static void play(Mob mob, Animator.Priority layer, ResourceLocation id) {
        State state = state(mob);
        if (id == null) {
            state.animator.stop(layer);
            return;
        }
        if (moveProbe != null) {
            moveProbe.accept(mob, id.getPath().substring(id.getPath().indexOf('/') + 1));
        }
        Animation animation = SkeletalClips.get(id);
        if (animation != null && !state.dying) {
            state.animator.play(layer, animation, 1, false);
        }
    }

    /**
     * Whether {@code mob}'s right hand shows nothing: the Disciple noyé's fork is gone from its release (tick 27 of
     * noye_throw) until it draws another from its back (tick 40).
     */
    static boolean handEmpty(Mob mob) {
        State state = STATES.get(mob);
        if (state == null || look(mob) != NOYE) {
            return false;
        }
        Animation clip = state.animator.playing(Animator.Priority.MIDDLE);
        float ticks = state.animator.time(Animator.Priority.MIDDLE) * 20;
        return clip != null && clip.id().endsWith("noye_throw") && ticks >= 27 && ticks < 40;
    }

    /** Whether {@code mob}'s look falls by a clip of its own when it dies (vanilla's fall to the side is left out). */
    static boolean diesByClip(Mob mob) {
        Look look = look(mob);
        return look != null && look.clips() != null && SkeletalClips.get(look.clip("death")) != null;
    }

    /** {@code mob}'s pose this frame: its motion and moves, its head's look. */
    public static Pose pose(Mob mob, float partialTick, float netHeadYaw, float headPitch) {
        Look look = LOOKS.get(mob.getType());
        State state = state(mob);
        if (state.animator.baseAnimation() == null && look != null) {
            update(mob, look, state);
        }
        Pose pose = state.animator.pose(partialTick);
        float raise = Mth.lerp(partialTick, state.previousRaise, state.raise);
        if (state.biped && look != null && look.clips() == null && raise > 0.01f && mob.getMainHandItem().getItem()
                instanceof net.minecraft.world.item.BowItem) {
            // Vanilla's bow aim (HumanoidModel.ArmPose.BOW_AND_ARROW): both arms toward the target, following the head.
            float yaw = Mth.DEG_TO_RAD * netHeadYaw, pitch = Mth.DEG_TO_RAD * headPitch;
            arm(pose, "Arm_R", "Shoulder_R", -Mth.HALF_PI + pitch, -0.1f + yaw, raise);
            arm(pose, "Arm_L", "Shoulder_L", -Mth.HALF_PI + pitch, 0.5f + yaw, raise);
        } else if (state.biped && look != null && look.clips() == null && raise > 0.01f) {
            // Vanilla's zombie arms (AnimationUtils.animateZombieArms): reaching forward, a swing down and in.
            float attack = mob.getAttackAnim(partialTick);
            float swing = Mth.sin(attack * Mth.PI);
            float lift = Mth.sin((1 - (1 - attack) * (1 - attack)) * Mth.PI);
            // Straight toward the target (vanilla lifts them higher, -120°), then the swing.
            float reach = -Mth.HALF_PI + swing * 1.2f - lift * 0.4f;
            float in = 0.1f - swing * 0.6f;
            arm(pose, "Arm_R", "Shoulder_R", reach, -in, raise);
            arm(pose, "Arm_L", "Shoulder_L", reach, in, raise);
        }
        if (state.biped && !state.dying) {
            BipedPoses.look(pose, netHeadYaw, headPitch);
        }
        if (look == MOINE) {
            MoineRosary.apply(mob, state.animator, state.armature, pose, partialTick);
        }
        if (look == GOLEM) {
            GolemTalisman.apply(mob, state.armature, pose, partialTick);
        }
        if (state.biped && mob.isBaby()) {
            // A child's big head, as vanilla's baby models: the head at three quarters of the adult's on a half-size body.
            int head = Biped.ARMATURE.get(Biped.HEAD).index();
            JointTransform current = pose.get(head);
            pose.set(head, JointTransform.of(current.translation(), current.rotation(),
                    new org.joml.Vector3f(current.scale()).mul(BABY_HEAD)));
        }
        return pose;
    }

    /**
     * Turns an arm toward vanilla's turn ({@code x}, {@code y}, radians, relative to the body) by {@code weight}: the
     * turn is the arm's in model space, so it is taken back into the shoulder's space first.
     */
    private static void arm(Pose pose, String joint, String shoulder, float x, float y, float weight) {
        int index = Biped.ARMATURE.get(joint).index();
        Quaternionf parent = pose.toModelMatrices(Biped.ARMATURE)[Biped.ARMATURE.get(shoulder).index()]
                .getUnnormalizedRotation(new Quaternionf()).normalize();
        Quaternionf target = parent.conjugate().mul(BipedPoses.fromVanilla(x, y, 0));
        JointTransform current = pose.get(index);
        pose.set(index, JointTransform.of(current.translation(),
                new Quaternionf(current.rotation()).slerp(target, weight), current.scale()));
    }

    private static void update(Mob mob, Look look, State state) {
        double dx = mob.getX() - mob.xo, dz = mob.getZ() - mob.zo, dy = mob.getY() - mob.yo;
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz) * 20;
        // A client does not always hear that a mob stands on the ground: it is in the air while it moves up or down.
        boolean grounded = mob.onGround() || mob.isInWater() || Math.abs(dy) < 1e-3;
        state.airTicks = grounded ? 0 : state.airTicks + 1;
        state.motion = state.airTicks > 2 ? (dy > 0 ? LivingMotion.JUMP : LivingMotion.FALL)
                : horizontal < MOVING ? LivingMotion.IDLE : horizontal > RUNNING ? LivingMotion.RUN : LivingMotion.WALK;
        if (mob instanceof net.minecraft.world.entity.monster.Spider spider && spider.isClimbing()) {
            // A spider against a wall climbs it, at the speed it goes up or down.
            state.motion = LivingMotion.CLIMB;
            horizontal = Math.abs((float) dy) * 20;
        }
        state.speed += (horizontal - state.speed) * 0.5f;
        MotionSet set = SkeletalClips.motions(look.motions());
        boolean fighting = look.clips() != null && mob.isAggressive();
        String id = set == null ? null : set.clip(state.motion, fighting);
        if (look == NOYE && mob.isInWater()) {
            // The Disciple noyé swims like an eel in the water, whatever it does (docs/mobs/boards/noye.png).
            state.motion = LivingMotion.SWIM;
            id = look.clip("noye_swim").toString();
            fighting = false;
        }
        if (fighting && "disciple".equals(look.clips()) && state.motion == LivingMotion.IDLE) {
            // The disciple's guard holds what it holds (DiscipleFighting.Weapon: fists, sword, axe or tool).
            id = look.clip(com.murimblock.combat.DiscipleFighting.Weapon.of(mob.getMainHandItem()).stance()).toString();
        }
        Animation animation = id == null ? null : SkeletalClips.get(ResourceLocation.parse(id));
        if (animation == null) {
            return;
        }
        // A baby's steps are half as long: its legs turn twice as fast for the same speed.
        float speed = animation.moveSpeed() > 0
                ? Mth.clamp(state.speed / (animation.moveSpeed() * mob.getAgeScale()), 0.4f, 2.5f) : 1;
        state.animator.motion(state.motion.key());
        state.animator.playBase(animation, speed);
        if (look.clips() != null) {
            for (String additive : set.additive()) {
                Animation clip = SkeletalClips.get(ResourceLocation.parse(additive));
                if (clip != null) {
                    state.animator.playAdditive(clip);
                }
            }
        }
    }
}
