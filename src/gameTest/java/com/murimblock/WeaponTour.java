package com.murimblock;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MountainBanditPatch;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.MountainBanditEntity;
import com.murimblock.mob.MurimEntities;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * Development-only real-client check of the weapons in hand (gradlew -PgameTests runWeaponTour), in a fresh flat world
 * filmed from several sides by the local spectator:
 * <ul>
 *   <li>a Mountain Bandit with the Bandit Dao: its idle, walk and run (on the spot: the client is told it walks, so Epic
 *   Fight picks the walk), its three cuts and the engine's hit, knock-down, guard-break and death reactions. One bandit
 *   for all of it, a new one after each death;</li>
 *   <li>a player (a server player on a mock connection) with the Bandit Chief's great axe: standing, walking, running
 *   (it really moves; Epic Fight walks a player that moves) and its M1 chain, read from the axe itself: the combo,
 *   the dash attack and the air attack.</li>
 *   <li>the same player in the Bandit Chief's armour (its four pieces, Epic Fight's armour meshes): standing, walking,
 *   running and the axe's first two M1s.</li>
 * </ul>
 * MURIM_TOUR_ONLY=bandit, =axe or =armour films one of them. The camera follows the actor (the cuts carry it forward), and
 * Epic Fight's red outline of the aimed-at entity is turned off. Frames go to
 * build/weapon-tour-run/screenshots/&lt;take&gt;/&lt;side&gt;/, the report to weapon-tour-report.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class WeaponTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** How far the camera stands from the actor, and the height it looks from. */
    private static final double DISTANCE = 3.2, EYE = 1.15;
    /** Ticks between moving the camera and a take's first frame. */
    private static final int SETTLE = 10;
    /** Ticks a new actor stands before its first take (tracking, its patch, its stance under way). */
    private static final int FRESH = 30;
    /** A player's walking and sprinting speeds, blocks a tick. */
    private static final double WALK_SPEED = 0.216, SPRINT_SPEED = 0.28;

    /** Who is filmed. */
    /** Who is filmed: the bandit, the player with the great axe, the same player in the Bandit Chief's armour too. */
    private enum Actor { BANDIT, AXE, ARMOUR }

    /** Where the camera stands, the actor facing +Z (its right hand toward -X). */
    private enum Side {
        FRONT(0, 1, 180), FRONT_RIGHT(-Math.sqrt(0.5), Math.sqrt(0.5), -135), RIGHT(-1, 0, -90), BACK(0, -1, 0), LEFT(1, 0, 90), TOP(0, 0, 180);

        final double dx, dz;
        final float yaw;

        Side(double dx, double dz, float yaw) {
            this.dx = dx;
            this.dz = dz;
            this.yaw = yaw;
        }
    }

    /**
     * One take: the actor from one side, {@code frames} captures {@code every} ticks apart, playing {@code played} (or the
     * axe's M1 number {@code combo}), or in the living motion {@code motion}.
     */
    private record Take(String name, Actor actor, Side side, AnimationAccessor<? extends StaticAnimation> played, int combo,
                        LivingMotion motion, int frames, int every) {
        int length() {
            return settle + frames * every + 2;
        }

        boolean plays() {
            return played != null || combo >= 0;
        }
    }

    private static final List<Take> TAKES = new ArrayList<>();
    private static final List<String> report = new ArrayList<>();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static volatile MountainBanditEntity bandit;
    private static volatile ServerPlayer wielder;
    /** What the server last asked the actor to play (the same accessor on both sides of an integrated server). */
    private static volatile AnimationAccessor<? extends StaticAnimation> asked;
    private static double baseX, baseY, baseZ;
    private static int take = -1, takeStart, settle;

    private static void bandit(String name, Side side, AnimationAccessor<? extends StaticAnimation> played, LivingMotion motion, int frames, int every) {
        TAKES.add(new Take("bandit-" + name, Actor.BANDIT, side, played, -1, motion, frames, every));
    }

    private static void axe(String name, Side side, int combo, LivingMotion motion, int frames, int every) {
        TAKES.add(new Take("axe-" + name, Actor.AXE, side, null, combo, motion, frames, every));
    }

    private static void armour(String name, Side side, int combo, LivingMotion motion, int frames, int every) {
        TAKES.add(new Take("armour-" + name, Actor.ARMOUR, side, null, combo, motion, frames, every));
    }

    /** The takes, listed once in game: the animations' accessors are only filled in once Epic Fight has built them. */
    private static void listTakes() {
        // The bandit standing: one picture from each side, then its 5 s loop.
        for (Side side : Side.values()) bandit("idle", side, null, LivingMotions.IDLE, 1, 1);
        bandit("idle-loop", Side.FRONT_RIGHT, null, LivingMotions.IDLE, 10, 10);
        // Walking and running (a picture every two ticks over a whole stride).
        for (Side side : List.of(Side.FRONT, Side.RIGHT, Side.LEFT, Side.BACK)) bandit("walk", side, null, LivingMotions.WALK, 16, 2);
        for (Side side : List.of(Side.FRONT, Side.RIGHT, Side.LEFT, Side.BACK)) bandit("run", side, null, LivingMotions.CHASE, 12, 2);
        // Its cuts, every tick.
        for (Side side : List.of(Side.FRONT, Side.FRONT_RIGHT, Side.RIGHT, Side.LEFT, Side.BACK, Side.TOP)) {
            bandit("drifting-cut", side, MurimAnimations.DRIFTING_CUT, null, 18, 1);
            bandit("returning-cut", side, MurimAnimations.RETURNING_CUT, null, 18, 1);
            bandit("falling-rain", side, MurimAnimations.FALLING_RAIN, null, 22, 1);
        }
        // What the engine plays when it is hit, knocked down, guard-broken (a perfect parry) and killed.
        for (Side side : List.of(Side.FRONT, Side.RIGHT)) {
            bandit("hit-short", side, Animations.BIPED_HIT_SHORT, null, 8, 1);
            bandit("hit-long", side, Animations.BIPED_HIT_LONG, null, 10, 2);
            bandit("neutralized", side, Animations.BIPED_COMMON_NEUTRALIZED, null, 14, 3);
            bandit("knockdown", side, Animations.BIPED_KNOCKDOWN, null, 16, 3);
            bandit("death", side, Animations.BIPED_DEATH, null, 14, 2);
        }
        // A player with the great axe: standing, walking, running, then each M1 of its chain.
        for (Side side : List.of(Side.FRONT, Side.FRONT_RIGHT, Side.RIGHT, Side.BACK, Side.LEFT)) axe("idle", side, -1, LivingMotions.IDLE, 1, 1);
        for (Side side : List.of(Side.FRONT_RIGHT, Side.RIGHT, Side.FRONT)) axe("walk", side, -1, LivingMotions.WALK, 16, 2);
        for (Side side : List.of(Side.FRONT_RIGHT, Side.RIGHT, Side.FRONT)) axe("run", side, -1, LivingMotions.RUN, 12, 2);
        String[] chain = {"m1-1", "m1-2", "dash", "air"};
        for (Side side : List.of(Side.FRONT_RIGHT, Side.RIGHT, Side.FRONT, Side.TOP)) {
            for (int i = 0; i < chain.length; i++) axe(chain[i], side, i, null, 24, 1);
        }
        // The same player in the chief's armour: from every side standing, walking and running, two M1s.
        for (Side side : List.of(Side.FRONT, Side.FRONT_RIGHT, Side.RIGHT, Side.BACK, Side.LEFT, Side.TOP)) armour("idle", side, -1, LivingMotions.IDLE, 1, 1);
        for (Side side : List.of(Side.FRONT_RIGHT, Side.BACK)) armour("walk", side, -1, LivingMotions.WALK, 16, 2);
        armour("run", Side.RIGHT, -1, LivingMotions.RUN, 12, 2);
        for (Side side : List.of(Side.FRONT_RIGHT, Side.BACK)) {
            armour("m1-1", side, 0, null, 24, 1);
            armour("m1-2", side, 1, null, 24, 1);
        }
        String only = System.getenv("MURIM_TOUR_ONLY");
        if (only != null && !only.isBlank()) TAKES.removeIf(t -> !t.actor().name().equalsIgnoreCase(only.trim()));
        check(TAKES.stream().filter(t -> t.actor() == Actor.BANDIT).allMatch(t -> t.motion() != null || t.played() != null), "every bandit take has its animation");
    }

    private WeaponTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.weaponTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-weapon-tour", GameType.SPECTATOR, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-weapon-tour", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            MinecraftServer server = mc.getSingleplayerServer();
            // A player on a mock connection is not ticked by its connection.
            server.execute(() -> { if (wielder != null && !wielder.isRemoved()) wielder.doTick(); });
            step(mc, server);
        } catch (Exception e) {
            report.add("FAILED tour crashed: " + e);
            failed = true;
            LOGGER.error("Weapon tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (ticks == 20) {
            listTakes();
            mc.options.hideGui = true;
            // Epic Fight outlines the entity the camera aims at in red: it would hide the blade's edge.
            ClientConfig.enableTargetEntityGuide = false;
            ClientConfig.showTargetIndicator = false;
            server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer self = self(server);
                baseX = Math.floor(self.getX()) + 0.5;
                baseY = self.getY();
                baseZ = Math.floor(self.getZ()) + 0.5;
            });
            return;
        }
        if (ticks < 30) return;
        if (take < 0 || ticks - takeStart >= TAKES.get(take).length()) {
            take++;
            takeStart = ticks;
            if (take == TAKES.size()) {
                server.execute(() -> {
                    if (bandit != null) bandit.discard();
                    if (wielder != null) server.getPlayerList().remove(wielder);
                });
                finish(mc);
                return;
            }
            Take t = TAKES.get(take);
            Take before = take == 0 ? null : TAKES.get(take - 1);
            // A new actor when it changes and after each death; otherwise the same one, back on its spot.
            boolean fresh = before == null || before.actor() != t.actor() || before.name().equals("bandit-death");
            settle = fresh ? FRESH : SETTLE;
            server.execute(() -> begin(server, t, fresh));
            return;
        }
        Take t = TAKES.get(take);
        int at = ticks - takeStart;
        LivingEntity seen = seen(mc, t.actor());
        // The camera follows the actor: the cuts carry it forward, the player walks.
        if (seen != null && at > 2) look(mc, seen.getX(), seen.getY(), seen.getZ(), t.side());
        boolean moving = t.motion() == LivingMotions.WALK || t.motion() == LivingMotions.CHASE || t.motion() == LivingMotions.RUN;
        if (moving && t.actor() == Actor.BANDIT && seen != null) {
            // On the spot: the client is told the bandit walks (Epic Fight's client animator picks the walk or the run from it).
            seen.walkAnimation.setSpeed(t.motion() == LivingMotions.CHASE ? 0.95F : 0.55F);
        }
        if (t.actor() != Actor.BANDIT && seen != null) {
            // Epic Fight walks a player from the movement keys its own client sends; this one has none: forward, held
            // while it walks or runs, released otherwise.
            PlayerPatch<?> remote = EpicFightCapabilities.getEntityPatch(seen, PlayerPatch.class);
            if (remote != null) {
                remote.dx = 0;
                remote.dz = moving && at > 2 ? 1 : 0;
            }
        }
        if (moving && t.actor() != Actor.BANDIT && at > 2) {
            double step = t.motion() == LivingMotions.RUN ? SPRINT_SPEED : WALK_SPEED;
            server.execute(() -> {
                wielder.moveTo(wielder.getX(), baseY, wielder.getZ() + step, 0, 0);
                wielder.setYHeadRot(0);
                wielder.setYBodyRot(0);
                wielder.setOnGround(true);
            });
        }
        if (at == settle - 1 && t.plays()) server.execute(() -> play(t));
        if (at >= settle && (at - settle) % t.every() == 0 && (at - settle) / t.every() < t.frames()) {
            int frame = (at - settle) / t.every();
            if (frame == 0) checkTake(mc, t);
            if (t.plays() && frame <= 1) checkPlaying(mc, t, frame == 1);
            capture(mc, String.format("%s/%s/%02d.png", t.name(), t.side().name().toLowerCase(), frame));
        }
    }

    /** The camera on the take's side of its actor (a new one, facing +Z, when asked). */
    private static void begin(MinecraftServer server, Take t, boolean fresh) {
        ServerLevel level = server.overworld();
        if (t.actor() == Actor.BANDIT) {
            if (fresh) {
                if (bandit != null) bandit.discard();
                bandit = MurimEntities.MOUNTAIN_BANDIT.get().create(level);
                bandit.moveTo(baseX, baseY, baseZ, 0, 0);
                bandit.setYBodyRot(0);
                bandit.setYHeadRot(0);
                bandit.setVariant(1);
                bandit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.BANDIT_DAO.get()));
                bandit.setPersistenceRequired();
                level.addFreshEntity(bandit);
                bandit.setNoAi(true);
            } else {
                // Back to its spot, facing +Z: the last take's cut carried it forward.
                bandit.teleportTo(baseX, baseY, baseZ);
                bandit.setYRot(0);
                bandit.setYBodyRot(0);
                bandit.setYHeadRot(0);
            }
            // Running is chasing: Epic Fight runs an aggressive mob that moves.
            bandit.setAggressive(t.motion() == LivingMotions.CHASE);
        } else {
            if (fresh) {
                if (bandit != null) {
                    bandit.discard();
                    bandit = null;
                }
                if (wielder == null) wielder = spawnWielder(server, level);
                report.add("info   great axe in a player's hand: " + describe(EpicFightBridge.patch(wielder)));
            }
            // The air attack from a little above the ground (a player on a mock connection stays where it is put).
            double lift = t.name().equals("axe-air") ? 1.4 : 0;
            wielder.moveTo(baseX, baseY + lift, baseZ, 0, 0);
            wielder.setYHeadRot(0);
            wielder.setYBodyRot(0);
            wielder.setOnGround(lift == 0);
            wielder.setSprinting(t.motion() == LivingMotions.RUN);
            // The chief's armour on for its takes, off for the axe's.
            boolean armoured = t.actor() == Actor.ARMOUR;
            wielder.setItemSlot(EquipmentSlot.HEAD, armoured ? new ItemStack(MurimItems.CHIEF_BROW_BAND.get()) : ItemStack.EMPTY);
            wielder.setItemSlot(EquipmentSlot.CHEST, armoured ? new ItemStack(MurimItems.CHIEF_LAMELLAR_VEST.get()) : ItemStack.EMPTY);
            wielder.setItemSlot(EquipmentSlot.LEGS, armoured ? new ItemStack(MurimItems.CHIEF_TASSETS.get()) : ItemStack.EMPTY);
            wielder.setItemSlot(EquipmentSlot.FEET, armoured ? new ItemStack(MurimItems.CHIEF_FUR_BOOTS.get()) : ItemStack.EMPTY);
        }
        Side s = t.side();
        double y = t.name().equals("axe-air") ? baseY + 1.4 : baseY;
        if (s == Side.TOP) self(server).teleportTo(level, baseX, y + 4.6 - 1.62, baseZ + 0.01, s.yaw, 90);
        else self(server).teleportTo(level, baseX + s.dx * DISTANCE, y + EYE - 1.62, baseZ + s.dz * DISTANCE, s.yaw, 6);
    }

    /** On the server: the take's animation, for the axe the M1 of its chain the take names. */
    private static void play(Take t) {
        if (t.actor() == Actor.BANDIT) {
            asked = t.played();
            EpicFightCapabilities.getEntityPatch(bandit, MountainBanditPatch.class).playAnimationSynchronized(t.played(), 0);
        } else {
            PlayerPatch<?> patch = EpicFightBridge.patch(wielder);
            var chain = patch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getAutoAttackMotion(patch);
            asked = chain.get(t.combo());
            patch.playAnimationSynchronized(asked, 0);
        }
    }

    /** The axe as Epic Fight sees it in a player's hand: its category and style, its M1 chain and its stance. */
    private static String describe(PlayerPatch<?> patch) {
        CapabilityItem item = patch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        String chain = item.getAutoAttackMotion(patch).stream().map(a -> a.registryName().toString()).collect(Collectors.joining(", "));
        var stance = item.getLivingMotionModifier(patch, InteractionHand.MAIN_HAND);
        String motions = stance == null ? "none" : stance.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().registryName()).sorted().collect(Collectors.joining(", "));
        return item.getWeaponCategory() + " " + item.getStyle(patch) + "; M1 chain (combo..., dash, air): " + chain + "; stance: " + motions;
    }

    private static ServerPlayer spawnWielder(MinecraftServer server, ServerLevel level) {
        var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), "Wielder"), 0,
                ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        for (int i = 0; i <= 60; i++) player.tick();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.CHIEF_GREAT_AXE.get()));
        CombatService.setCombatMode(player, true);
        EpicFightCombatDefaults.enforce(player);
        return player;
    }

    /** The take's first frame: the actor is in the client's world with its weapon in hand, in the motion asked. */
    private static void checkTake(Minecraft mc, Take t) {
        LivingEntity seen = seen(mc, t.actor());
        String what = t.name() + " from " + t.side().name().toLowerCase();
        Item weapon = t.actor() == Actor.BANDIT ? MurimItems.BANDIT_DAO.get() : MurimItems.CHIEF_GREAT_AXE.get();
        check(seen != null && seen.getMainHandItem().is(weapon), what + ": holds " + weapon);
        if (t.actor() == Actor.ARMOUR) {
            check(seen != null && seen.getItemBySlot(EquipmentSlot.HEAD).is(MurimItems.CHIEF_BROW_BAND.get())
                    && seen.getItemBySlot(EquipmentSlot.CHEST).is(MurimItems.CHIEF_LAMELLAR_VEST.get())
                    && seen.getItemBySlot(EquipmentSlot.LEGS).is(MurimItems.CHIEF_TASSETS.get())
                    && seen.getItemBySlot(EquipmentSlot.FEET).is(MurimItems.CHIEF_FUR_BOOTS.get()), what + ": the client sees the chief's four pieces worn");
            if (t.name().equals("armour-idle") && t.side() == Side.FRONT) mc.getSingleplayerServer().execute(WeaponTour::checkArmour);
        }
        if (seen != null && t.motion() != null) {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(seen, LivingEntityPatch.class);
            LivingMotion motion = patch == null ? null : patch.getCurrentLivingMotion();
            check(motion == t.motion(), what + ": living motion " + motion);
            if (t.actor() != Actor.BANDIT && t.motion() != LivingMotions.IDLE) {
                report.add(String.format("info   %s: z from %.2f, server %.2f, client %.2f", what, baseZ, wielder.getZ(), seen.getZ()));
            }
        }
    }

/**
     * On the server, the wielder in the chief's armour (a real player, ticked: its worn equipment counts): 17 armour and
     * 4 toughness, and a zombie's blow of 10 takes what armour leaves of it (about 4.5), not 10 (generic damage would
     * go through any armour).
     */
    private static void checkArmour() {
        double armour = wielder.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        double toughness = wielder.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        check(Math.abs(armour - 17) < 0.01 && Math.abs(toughness - 4) < 0.01, String.format("the wielder in the chief's armour has armour %.1f, toughness %.1f", armour, toughness));
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(wielder.serverLevel());
        var blow = wielder.damageSources().mobAttack(zombie);
        float expected = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(wielder, 10, blow, (float) armour, (float) toughness);
        wielder.setHealth(wielder.getMaxHealth());
        wielder.invulnerableTime = 0;
        float before = wielder.getHealth();
        wielder.hurt(blow, 10);
        float taken = before - wielder.getHealth();
        check(taken < 6 && Math.abs(taken - expected) < 0.6, String.format("a zombie's blow of 10 on the chief's armour takes %.2f (armour leaves %.2f)", taken, expected));
        wielder.setHealth(wielder.getMaxHealth());
        wielder.invulnerableTime = 0;
    }

    private static boolean playingOnFirstFrame;

    /** On the take's first or second frame, the client plays its animation (the short hit is over within two ticks). */
    private static void checkPlaying(Minecraft mc, Take t, boolean second) {
        LivingEntity seen = seen(mc, t.actor());
        LivingEntityPatch<?> patch = seen == null ? null : EpicFightCapabilities.getEntityPatch(seen, LivingEntityPatch.class);
        var animation = asked;
        boolean playing = patch != null && animation != null && patch.getAnimator().isPlaying(animation);
        if (!second) playingOnFirstFrame = playing;
        else check(playingOnFirstFrame || playing, t.name() + " from " + t.side().name().toLowerCase() + ": the client plays "
                + (animation == null ? "nothing" : animation.registryName()));
    }

    // ---------------------------------------------------------------- helpers

    /** The client's copy of the actor. */
    private static LivingEntity seen(Minecraft mc, Actor actor) {
        LivingEntity server = actor == Actor.BANDIT ? bandit : wielder;
        return server == null || mc.level == null ? null : mc.level.getEntity(server.getId()) instanceof LivingEntity e ? e : null;
    }

    /** The local camera on the take's side of the actor standing at (x, y, z). */
    private static void look(Minecraft mc, double x, double y, double z, Side s) {
        if (s == Side.TOP) mc.player.moveTo(x, y + 4.6 - 1.62, z + 0.01, s.yaw, 90);
        else mc.player.moveTo(x + s.dx * DISTANCE, y + EYE - 1.62, z + s.dz * DISTANCE, s.yaw, 6);
    }

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Weapon tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("weapon-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the weapon tour report", e);
        }
        LOGGER.info("Weapon tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
