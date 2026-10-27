package com.murimblock.shenfa.client.mob;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.Pose;
import com.murimblock.shenfa.client.ClipEvents;
import com.murimblock.shenfa.client.SkeletalClips;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Mobs on Shenfa. Each mob that plays a clip gets an {@link Animator} on its rig ({@link HumanoidRig} for humanoid
 * models, which play the player's clips; {@link PartRig} for every other model, which plays clips keyed on its parts,
 * {@link MobClips}). Vanilla still poses the model each frame; the layers are laid on that pose and written back
 * into the model before it is drawn. A mob that plays nothing costs nothing: vanilla draws it untouched.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MobShenfa {
    /** A mob whose layers have all stopped this many ticks is let go. */
    private static final int IDLE_TICKS = 40;
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();
    private static final Map<EntityModel<?>, MobRig> RIGS = new WeakHashMap<>();
    /** Dev: when false, mobs are drawn as vanilla draws them even while they play clips. */
    private static boolean enabled = true;

    private MobShenfa() {
    }

    private static final class State {
        final MobRig rig;
        final Animator animator;
        int idle;
        /** Ticks the layers stay frozen (a hit stop). */
        int frozen;

        State(MobRig rig) {
            this.rig = rig;
            this.animator = new Animator(rig.armature(), rig.biped() ? Biped::settleHelpers : null);
        }
    }

    public static void enable(boolean on) {
        enabled = on;
    }

    /** Forgets every mob's animator: after a reload their clips are stale. */
    public static void clear() {
        STATES.clear();
    }

    /** The rig of {@code entity}'s model, or null when it has no model Shenfa can drive. */
    public static MobRig rig(LivingEntity entity) {
        EntityModel<?> model = model(entity);
        return model == null ? null : RIGS.computeIfAbsent(model, MobShenfa::rigOf);
    }

    private static MobRig rigOf(EntityModel<?> model) {
        return model instanceof HumanoidModel<?> ? HumanoidRig.HUMANOID
                : model instanceof IllagerModel<?> ? HumanoidRig.ILLAGER : PartRig.of(model);
    }

    private static EntityModel<?> model(LivingEntity entity) {
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        return renderer instanceof LivingEntityRenderer<?, ?> living ? living.getModel() : null;
    }

    /** The clip {@code id} as {@code entity}'s rig plays it, or null. */
    public static Animation clip(LivingEntity entity, ResourceLocation id) {
        MobRig rig = rig(entity);
        if (rig == null) {
            return null;
        }
        return rig.biped() ? SkeletalClips.get(id) : MobClips.get(id, rig.armature());
    }

    /** Plays {@code id} on a layer of {@code entity}; false when the mob or the clip cannot. */
    public static boolean play(LivingEntity entity, Animator.Priority priority, ResourceLocation id, float speed,
                               boolean mirrored) {
        if (entity instanceof Player) {
            return false;
        }
        Animation animation = clip(entity, id);
        if (animation == null) {
            return false;
        }
        MobRig rig = rig(entity);
        State state = STATES.get(entity);
        if (state == null || state.rig != rig) {
            state = new State(rig);
            STATES.put(entity, state);
        }
        state.animator.play(priority, animation, speed, mirrored);
        state.idle = 0;
        return true;
    }

    public static void stop(LivingEntity entity, Animator.Priority priority) {
        State state = STATES.get(entity);
        if (state != null) {
            state.animator.stop(priority);
        }
    }

    /** Seconds into the clip on a layer of {@code entity}, or -1. */
    public static float time(LivingEntity entity, Animator.Priority priority) {
        State state = STATES.get(entity);
        return state == null ? -1 : state.animator.time(priority);
    }

    /** Freezes {@code entity}'s layers for {@code ticks} (a hit stop). */
    public static void freeze(LivingEntity entity, int ticks) {
        State state = STATES.get(entity);
        if (state != null) {
            state.frozen = Math.max(state.frozen, ticks);
        }
    }

    /** The clip on a layer of {@code entity}, or null. */
    public static Animation playing(LivingEntity entity, Animator.Priority priority) {
        State state = STATES.get(entity);
        return state == null ? null : state.animator.playing(priority);
    }

    /** Called by the renderer right after vanilla posed the model: lays the mob's layers on that pose. */
    public static void afterSetupAnim(LivingEntity entity, EntityModel<?> model, float partialTick) {
        if (!enabled) {
            return;
        }
        State state = STATES.get(entity);
        if (state == null || RIGS.get(model) != state.rig) {
            return;
        }
        Pose captured = state.rig.capture(model);
        Pose pose = state.animator.pose(partialTick, captured);
        state.rig.apply(model, pose, captured);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        for (Iterator<Map.Entry<LivingEntity, State>> it = STATES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<LivingEntity, State> entry = it.next();
            LivingEntity entity = entry.getKey();
            State state = entry.getValue();
            if (entity.isRemoved() || entity.level() != mc.level) {
                it.remove();
                continue;
            }
            state.animator.tick(state.frozen > 0 ? 0 : 1);
            if (state.frozen > 0) {
                state.frozen--;
            }
            ClipEvents.run(entity, state.animator.events());
            state.idle = state.animator.anyLayer() ? 0 : state.idle + 1;
            if (state.idle > IDLE_TICKS) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("shenfamob")
                .then(Commands.literal("parts").executes(MobShenfa::parts))
                .then(Commands.literal("on").executes(context -> switchTo(context, true)))
                .then(Commands.literal("off").executes(context -> switchTo(context, false)))
                .then(Commands.literal("stop").executes(context -> {
                    LivingEntity target = target();
                    if (target != null) {
                        for (Animator.Priority priority : Animator.Priority.values()) {
                            stop(target, priority);
                        }
                    }
                    return 1;
                }))
                .then(Commands.literal("play").then(Commands.argument("clip", StringArgumentType.greedyString())
                        .executes(context -> play(context, StringArgumentType.getString(context, "clip"))))));
    }

    private static int switchTo(CommandContext<CommandSourceStack> context, boolean on) {
        enable(on);
        context.getSource().sendSystemMessage(Component.literal("Mobs on Shenfa: " + (on ? "on" : "off")));
        return 1;
    }

    /** The living entity the player looks at, or the nearest within 8 blocks. */
    public static LivingEntity target() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.crosshairPickEntity instanceof LivingEntity living && !(living instanceof Player)) {
            return living;
        }
        if (mc.player == null) {
            return null;
        }
        AABB around = mc.player.getBoundingBox().inflate(8);
        LivingEntity nearest = null;
        for (Entity entity : mc.level.getEntities(mc.player, around, entity -> entity instanceof LivingEntity
                && !(entity instanceof Player))) {
            if (nearest == null || entity.distanceToSqr(mc.player) < nearest.distanceToSqr(mc.player)) {
                nearest = (LivingEntity) entity;
            }
        }
        return nearest;
    }

    private static int parts(CommandContext<CommandSourceStack> context) {
        LivingEntity target = target();
        MobRig rig = target == null ? null : rig(target);
        String text = rig == null ? "No mob in sight" : target.getType().toShortString() + ": "
                + (rig.biped() ? "biped (plays the player's clips)" : String.join(", ", ((PartRig) rig).names()));
        context.getSource().sendSystemMessage(Component.literal(text));
        Murimblock.LOGGER.info("Shenfa mob parts: {}", text);
        return 1;
    }

    /** {@code /shenfamob play <namespace:set/clip> [speed] [priority]}. */
    private static int play(CommandContext<CommandSourceStack> context, String arguments) {
        String[] words = arguments.trim().split(" +");
        ResourceLocation id = ResourceLocation.tryParse(words[0]);
        float speed = words.length > 1 ? parse(words[1]) : 1;
        Animator.Priority priority = words.length > 2 ? Animator.Priority.valueOf(words[2].toUpperCase(Locale.ROOT))
                : Animator.Priority.HIGH;
        LivingEntity target = target();
        boolean played = target != null && id != null && play(target, priority, id, speed, false);
        context.getSource().sendSystemMessage(Component.literal(played ? "Playing " + id + " on "
                + target.getType().toShortString() : "Cannot play " + words[0]));
        return played ? 1 : 0;
    }

    private static float parse(String word) {
        try {
            return Float.parseFloat(word);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
