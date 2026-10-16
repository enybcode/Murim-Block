package com.murimblock;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.CrabKingAnimations;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
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
 * The Crab King too (a mob, the camera farther back): standing, his heavy walk, his three attacks, his grab (a husk
 * before him to catch), his roar and his death.
 * MURIM_TOUR_ONLY=bandit, =king, =axe or =armour films one of them. The camera follows the actor (the cuts carry it forward), and
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
    private enum Actor { BANDIT, AXE, ARMOUR, SPEAR, JIAN, JIAN_AZURE, SHIELD, STAFF, GALLERY, KING }

    /** The actors that are mobs (filmed as they are, not a player holding a weapon): the bandit, the Crab King. */
    private static boolean isMob(Actor actor) {
        return actor == Actor.BANDIT || actor == Actor.KING;
    }

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
    private static volatile net.minecraft.world.entity.Mob bandit;
    /** How much farther and higher the camera stands for this actor (the Crab King is twice a man). */
    private static double zoom = 1;
    /** Who the Crab King's grab catches in its takes: a husk before his great pincer. */
    private static volatile net.minecraft.world.entity.monster.Husk victim;
    private static volatile ServerPlayer wielder;
    /** What the server last asked the actor to play (the same accessor on both sides of an integrated server). */
    private static volatile AnimationAccessor<? extends StaticAnimation> asked;
    private static double baseX, baseY, baseZ;
    private static int take = -1, takeStart, settle;

    private static void king(String name, Side side, AnimationAccessor<? extends StaticAnimation> played, LivingMotion motion, int frames, int every) {
        TAKES.add(new Take("king-" + name, Actor.KING, side, played, -1, motion, frames, every));
    }

    private static void bandit(String name, Side side, AnimationAccessor<? extends StaticAnimation> played, LivingMotion motion, int frames, int every) {
        TAKES.add(new Take("bandit-" + name, Actor.BANDIT, side, played, -1, motion, frames, every));
    }

    private static void axe(String name, Side side, int combo, LivingMotion motion, int frames, int every) {
        TAKES.add(new Take("axe-" + name, Actor.AXE, side, null, combo, motion, frames, every));
    }

    private static void armour(String name, Side side, int combo, LivingMotion motion, int frames, int every) {
        TAKES.add(new Take("armour-" + name, Actor.ARMOUR, side, null, combo, motion, frames, every));
    }

    private static void spear(String name, Side side, int combo, LivingMotion motion, int frames, int every) {
        TAKES.add(new Take("spear-" + name, Actor.SPEAR, side, null, combo, motion, frames, every));
    }

    private static void jian(Actor actor, String name, Side side, int combo, LivingMotion motion, int frames, int every) {
        TAKES.add(new Take((actor == Actor.JIAN ? "jian-" : "jian-azure-") + name, actor, side, null, combo, motion, frames, every));
    }

    /**
     * An addon's staff, to see its moves (never shipped with Murimblock): its registry name from MURIM_TOUR_STAFF, by
     * default Dawn Day's iron battlestaff; air when that addon is not installed (the tour's build/weapon-tour-run/mods).
     * Skipped while Murimblock retires it (docs/DAWN_DAY.md).
     */
    private static Item addonStaff() {
        String id = System.getenv().getOrDefault("MURIM_TOUR_STAFF", "epicfight_dd:iron_battlestaff");
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id));
    }

    private static void staff(String name, Side side, int combo, LivingMotion motion, int frames, int every) {
        TAKES.add(new Take("staff-" + name, Actor.STAFF, side, null, combo, motion, frames, every));
    }

    /**
     * A datapack for the tour only (MURIM_TOUR_PACK, a folder with its pack.mcmeta), copied into the tour's world and
     * enabled: for instance one giving vanilla swords the Epic Fight weapon types whose own items Murimblock keeps out of
     * players' hands, to film those types' moves.
     */
    private static void loadPack(MinecraftServer server) throws java.io.IOException {
        String dir = System.getenv("MURIM_TOUR_PACK");
        if (dir == null || dir.isBlank()) return;
        Path from = Path.of(dir), to = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR).resolve("murim-tour");
        try (var files = Files.walk(from)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                Path target = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(target);
                else Files.copy(p, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        server.execute(() -> {
            var packs = server.getPackRepository();
            packs.reload();
            var selected = new ArrayList<>(packs.getSelectedIds());
            selected.add("file/murim-tour");
            server.reloadResources(selected).thenRun(() -> report.add("info   tour datapack enabled: " + dir));
        });
    }

    /** The gallery's items, by take; the one the current take holds. */
    private static final java.util.Map<Take, Item> GALLERY_ITEMS = new java.util.HashMap<>();
    private static volatile Item galleryItem = net.minecraft.world.item.Items.AIR;

    /**
     * A gallery of weapons by registry name (MURIM_TOUR_ITEMS, comma-separated): each one's stance, then every M1 of its
     * chain, from the front right. Names that are not registered (an addon not installed) are left out.
     */
    private static void listGallery() {
        String ids = System.getenv("MURIM_TOUR_ITEMS");
        if (ids == null || ids.isBlank()) return;
        String[] chain = {"m1-1", "m1-2", "m1-3", "m1-4", "m1-5", "m1-6", "m1-7"};
        for (String id : ids.split(",")) {
            var key = net.minecraft.resources.ResourceLocation.tryParse(id.trim());
            Item item = key == null ? net.minecraft.world.item.Items.AIR : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key);
            if (item == net.minecraft.world.item.Items.AIR && !id.trim().equals("minecraft:air")) {
                report.add("info   gallery: no item " + id.trim());
                continue;
            }
            String prefix = "gallery-" + key.getNamespace() + "-" + key.getPath() + "-";
            Take idle = new Take(prefix + "idle", Actor.GALLERY, Side.FRONT_RIGHT, null, -1, LivingMotions.IDLE, 1, 1);
            TAKES.add(idle);
            GALLERY_ITEMS.put(idle, item);
            for (int i = 0; i < chain.length; i++) {
                Take m1 = new Take(prefix + chain[i], Actor.GALLERY, Side.FRONT_RIGHT, null, i, null, 24, 1);
                TAKES.add(m1);
                GALLERY_ITEMS.put(m1, item);
            }
        }
    }

    /** A take's combo for raising the guard instead of an M1. */
    private static final int GUARD = 99;

    private static void shield(String name, Side side, int combo, LivingMotion motion, int frames, int every) {
        TAKES.add(new Take("shield-" + name, Actor.SHIELD, side, null, combo, motion, frames, every));
    }

    /** The weapon each of the player's takes holds. */
    private static Item weaponOf(Actor actor) {
        return switch (actor) {
            case BANDIT -> MurimItems.BANDIT_DAO.get();
            case KING -> net.minecraft.world.item.Items.AIR;
            case SPEAR -> MurimItems.ALLIANCE_SPEAR.get();
            case JIAN, JIAN_AZURE, SHIELD -> MurimItems.SNOW_PLUM_JIAN.get();
            case STAFF -> addonStaff();
            case GALLERY -> galleryItem;
            default -> MurimItems.CHIEF_GREAT_AXE.get();
        };
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
        // The Crab King: standing from each side, his loop, his walk, his attacks and roar, his death.
        for (Side side : List.of(Side.FRONT, Side.FRONT_RIGHT, Side.RIGHT, Side.BACK, Side.LEFT, Side.TOP)) king("idle", side, null, LivingMotions.IDLE, 1, 1);
        king("idle-loop", Side.FRONT_RIGHT, null, LivingMotions.IDLE, 25, 4);
        for (Side side : List.of(Side.FRONT_RIGHT, Side.RIGHT)) king("walk", side, null, LivingMotions.WALK, 16, 2);
        for (Side side : List.of(Side.FRONT, Side.FRONT_RIGHT, Side.RIGHT)) {
            king("sweep", side, CrabKingAnimations.SWEEP, null, 22, 1);
            king("snap", side, CrabKingAnimations.SNAP, null, 26, 1);
            king("slam", side, CrabKingAnimations.SLAM, null, 30, 1);
            king("roar", side, CrabKingAnimations.ROAR, null, 20, 2);
        }
        for (Side side : List.of(Side.FRONT_RIGHT, Side.RIGHT, Side.LEFT)) king("grab", side, CrabKingAnimations.GRAB, null, 46, 2);
        // The grab lived from inside: the camera's own player caught (client/GrabCinematic films it).
        king("grab-pov", Side.FRONT, CrabKingAnimations.GRAB, null, 56, 2);
        king("death", Side.FRONT_RIGHT, Animations.BIPED_DEATH, null, 14, 2);
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
        // The same player with the Alliance's Azure Peak Spear: its stance, walk and run, then each M1 of its chain.
        for (Side side : List.of(Side.FRONT, Side.FRONT_RIGHT, Side.RIGHT, Side.BACK, Side.LEFT)) spear("idle", side, -1, LivingMotions.IDLE, 1, 1);
        for (Side side : List.of(Side.FRONT_RIGHT, Side.RIGHT, Side.FRONT)) spear("walk", side, -1, LivingMotions.WALK, 16, 2);
        for (Side side : List.of(Side.FRONT_RIGHT, Side.RIGHT, Side.FRONT)) spear("run", side, -1, LivingMotions.RUN, 12, 2);
        for (Side side : List.of(Side.FRONT_RIGHT, Side.RIGHT, Side.FRONT, Side.TOP)) {
            for (int i = 0; i < chain.length; i++) spear(chain[i], side, i, null, 24, 1);
        }
        // The same player with the Alliance's Snow Plum Jian: its stance, walk and run, Epic Fight's sword M1s; then, as a
        // student of the Azure Cloud Sword Art, Murim's own M1s (Flowing Cloud Stance). From above, where the edge shows.
        for (Side side : List.of(Side.FRONT, Side.FRONT_RIGHT, Side.RIGHT, Side.BACK, Side.LEFT)) jian(Actor.JIAN, "idle", side, -1, LivingMotions.IDLE, 1, 1);
        for (Side side : List.of(Side.FRONT_RIGHT, Side.RIGHT)) jian(Actor.JIAN, "walk", side, -1, LivingMotions.WALK, 16, 2);
        jian(Actor.JIAN, "run", Side.RIGHT, -1, LivingMotions.RUN, 12, 2);
        String[] swordChain = {"m1-1", "m1-2", "m1-3", "dash", "air"};
        for (Side side : List.of(Side.FRONT_RIGHT, Side.RIGHT, Side.TOP)) {
            for (int i = 0; i < swordChain.length; i++) jian(Actor.JIAN, swordChain[i], side, i, null, 20, 1);
        }
        for (Side side : List.of(Side.FRONT_RIGHT, Side.TOP)) {
            for (int i = 0; i < 4; i++) jian(Actor.JIAN_AZURE, "m1-" + (i + 1), side, i, null, 20, 1);
        }
        // The jian with the Alliance's Rattan Shield on the other arm: standing, walking, two M1s, and the guard raised.
        for (Side side : List.of(Side.FRONT, Side.FRONT_RIGHT, Side.LEFT, Side.BACK, Side.TOP)) shield("idle", side, -1, LivingMotions.IDLE, 1, 1);
        for (Side side : List.of(Side.FRONT, Side.LEFT)) shield("walk", side, -1, LivingMotions.WALK, 16, 2);
        for (Side side : List.of(Side.FRONT_RIGHT, Side.LEFT)) {
            shield("m1-1", side, 0, null, 18, 1);
            shield("m1-2", side, 1, null, 18, 1);
        }
        for (Side side : List.of(Side.FRONT, Side.FRONT_RIGHT, Side.LEFT, Side.TOP)) shield("guard", side, GUARD, null, 12, 2);
        // An addon's staff (only with the addon installed): its stance, walk, run, its whole M1 chain.
        // Not when Murimblock retires that item (Dawn Day's, since it became a dependency for its animations only): it
        // would be taken from the player's hand. MURIM_TOUR_STAFF can still name another addon's staff.
        if (addonStaff() != net.minecraft.world.item.Items.AIR
                && !com.murimblock.integration.epicfight.EpicFightContentPolicy.blocked(new net.minecraft.world.item.ItemStack(addonStaff()))) {
            for (Side side : List.of(Side.FRONT, Side.FRONT_RIGHT, Side.RIGHT, Side.BACK)) staff("idle", side, -1, LivingMotions.IDLE, 1, 1);
            for (Side side : List.of(Side.FRONT_RIGHT, Side.RIGHT)) staff("walk", side, -1, LivingMotions.WALK, 16, 2);
            staff("run", Side.FRONT_RIGHT, -1, LivingMotions.RUN, 12, 2);
            String[] staffChain = {"m1-1", "m1-2", "m1-3", "m1-4", "m1-5", "dash", "air"};
            for (Side side : List.of(Side.FRONT_RIGHT, Side.RIGHT, Side.TOP)) {
                for (int i = 0; i < staffChain.length; i++) staff(staffChain[i], side, i, null, 26, 1);
            }
        }
        listGallery();
        String only = System.getenv("MURIM_TOUR_ONLY");
        if (only != null && !only.isBlank()) TAKES.removeIf(t -> !t.actor().name().toLowerCase().startsWith(only.trim().toLowerCase()));
        check(TAKES.stream().filter(t -> isMob(t.actor())).allMatch(t -> t.motion() != null || t.played() != null), "every bandit take has its animation");
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
        if (ticks == 8) loadPack(server);
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
                    if (victim != null) victim.discard();
                    if (wielder != null) server.getPlayerList().remove(wielder);
                });
                finish(mc);
                return;
            }
            Take t = TAKES.get(take);
            if (t.actor() == Actor.GALLERY) galleryItem = GALLERY_ITEMS.get(t);
            Take before = take == 0 ? null : TAKES.get(take - 1);
            // A new actor when it changes and after each death; otherwise the same one, back on its spot.
            boolean fresh = before == null || before.actor() != t.actor() || before.name().endsWith("-death");
            settle = fresh ? FRESH : SETTLE;
            server.execute(() -> begin(server, t, fresh));
            return;
        }
        Take t = TAKES.get(take);
        int at = ticks - takeStart;
        LivingEntity seen = seen(mc, t.actor());
        // The camera follows the actor: the cuts carry it forward, the player walks.
        // In the grab lived from inside, the camera is the caught player's own (the server moves it).
        if (seen != null && at > 2 && !t.name().equals(POV)) look(mc, seen.getX(), seen.getY(), seen.getZ(), t.side());
        boolean moving = t.motion() == LivingMotions.WALK || t.motion() == LivingMotions.CHASE || t.motion() == LivingMotions.RUN;
        if (moving && isMob(t.actor()) && seen != null) {
            // On the spot: the client is told the bandit walks (Epic Fight's client animator picks the walk or the run from it).
            seen.walkAnimation.setSpeed(t.motion() == LivingMotions.CHASE ? 0.95F : 0.55F);
        }
        if (!isMob(t.actor()) && seen != null) {
            // Epic Fight walks a player from the movement keys its own client sends; this one has none: forward, held
            // while it walks or runs, released otherwise.
            PlayerPatch<?> remote = EpicFightCapabilities.getEntityPatch(seen, PlayerPatch.class);
            if (remote != null) {
                remote.dx = 0;
                remote.dz = moving && at > 2 ? 1 : 0;
            }
        }
        if (moving && !isMob(t.actor()) && at > 2) {
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

    /** The take in which the camera's own player is caught by the Crab King. */
    private static final String POV = "king-grab-pov";

    /** The camera on the take's side of its actor (a new one, facing +Z, when asked). */
    private static void begin(MinecraftServer server, Take t, boolean fresh) {
        ServerLevel level = server.overworld();
        // Back to watching after the grab lived from inside.
        ServerPlayer me = self(server);
        if (me.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            me.setGameMode(GameType.SPECTATOR);
            me.removeAllEffects();
        }
        zoom = t.actor() == Actor.KING ? 2.4 : 1;
        // A stone wall across his throw for the grab's takes (he knocks a crater into it), gone for the others.
        boolean wall = t.name().startsWith("king-grab");
        for (int x = -5; x <= 5; x++) for (int y = 0; y < 6; y++) {
            net.minecraft.core.BlockPos p = net.minecraft.core.BlockPos.containing(baseX + x, baseY + y, baseZ + 7);
            level.setBlockAndUpdate(p, wall ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        if (victim != null) {
            victim.discard();
            victim = null;
        }
        if (isMob(t.actor())) {
            if (fresh) {
                if (bandit != null) bandit.discard();
                if (t.actor() == Actor.KING) bandit = MurimEntities.CRAB_KING.get().create(level);
                else {
                    MountainBanditEntity b = MurimEntities.MOUNTAIN_BANDIT.get().create(level);
                    b.setVariant(1);
                    b.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.BANDIT_DAO.get()));
                    bandit = b;
                }
                bandit.moveTo(baseX, baseY, baseZ, 0, 0);
                bandit.setYBodyRot(0);
                bandit.setYHeadRot(0);
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
            if (t.name().equals("king-grab")) {
                // Something for the pincer to catch, before him and a little to his right (-x as he faces +z): a husk, which does
                // not burn in the sun.
                victim = net.minecraft.world.entity.EntityType.HUSK.create(level);
                victim.moveTo(baseX - 0.75, baseY, baseZ + 2.6, 180, 0);
                victim.setBaby(false);
                victim.setPersistenceRequired();
                level.addFreshEntity(victim);
                // With its AI: a mob without it does not move, so it would not fly when thrown.
            }
        } else {
            if (fresh) {
                if (bandit != null) {
                    bandit.discard();
                    bandit = null;
                }
                if (wielder == null) wielder = spawnWielder(server, level);
            }
            // The shield on the off hand for its takes only, the guard lowered between takes.
            ItemStack offhand = t.actor() == Actor.SHIELD ? new ItemStack(MurimItems.RATTAN_SHIELD.get()) : ItemStack.EMPTY;
            if (!ItemStack.isSameItem(wielder.getOffhandItem(), offhand)) {
                wielder.setItemInHand(InteractionHand.OFF_HAND, offhand);
                EpicFightCombatDefaults.enforce(wielder);
            }
            EpicFightBridge.patch(wielder).resetHolding();
            wielder.stopUsingItem();
            if (!wielder.getMainHandItem().is(weaponOf(t.actor()))) {
                wielder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(weaponOf(t.actor())));
                EpicFightCombatDefaults.enforce(wielder);
            }
            if (fresh && t.actor() == Actor.JIAN_AZURE) {
                // A student of the Azure Cloud Sword Art, with it as main style: its passive replaces a sword's M1s.
                MartialService.learn(wielder, MartialArts.art(MartialArts.AZURE_CLOUD_SWORD));
                MartialService.setMainArt(wielder, MartialArts.AZURE_CLOUD_SWORD);
            }
            if (fresh || t.actor() == Actor.GALLERY && t.name().endsWith("-idle")) report.add("info   " + weaponOf(t.actor()) + " in a player's hand: " + describe(EpicFightBridge.patch(wielder)));
            // The air attack from a little above the ground (a player on a mock connection stays where it is put).
            double lift = t.name().endsWith("-air") && !isMob(t.actor()) ? 1.4 : 0;
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
        double y = t.name().endsWith("-air") && !isMob(t.actor()) ? baseY + 1.4 : baseY;
        if (s == Side.TOP) self(server).teleportTo(level, baseX, y + 4.6 * zoom - 1.62, baseZ + 0.01, s.yaw, 90);
        else self(server).teleportTo(level, baseX + s.dx * DISTANCE * zoom, y + EYE * zoom - 1.62, baseZ + s.dz * DISTANCE * zoom, s.yaw, 6);
        if (t.name().equals(POV)) {
            // The camera's player in survival before the pincer (CrabKingGrip.CATCH: his right and ahead), unhurt.
            me.setGameMode(GameType.SURVIVAL);
            me.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 400, 4, false, false));
            me.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 400, 4, false, false));
            me.teleportTo(level, baseX - 0.85, baseY, baseZ + 2.4, 180, -10);
        }
    }

    /** On the server: the take's animation, for the axe the M1 of its chain the take names. */
    private static void play(Take t) {
        if (isMob(t.actor())) {
            asked = t.played();
            EpicFightCapabilities.getEntityPatch(bandit, LivingEntityPatch.class).playAnimationSynchronized(t.played(), 0);
        } else {
            PlayerPatch<?> patch = EpicFightBridge.patch(wielder);
            if (t.combo() == GUARD) {
                // The shield raised as a player raises it (using it, the right mouse button): Epic Fight's shield block.
                wielder.startUsingItem(InteractionHand.OFF_HAND);
                asked = null;
                report.add("info   " + t.name() + " from " + t.side().name().toLowerCase() + ": shield raised (using " + wielder.isUsingItem() + "), off hand " + wielder.getOffhandItem()
                        + ", style " + patch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getStyle(patch));
                return;
            }
            var chain = patch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getAutoAttackMotion(patch);
            // A chain shorter than the takes asked for: nothing to film for the missing M1.
            if (t.combo() >= chain.size()) {
                asked = null;
                report.add("info   " + t.name() + ": the chain has only " + chain.size() + " M1s");
                return;
            }
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
        Item weapon = weaponOf(t.actor());
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
            if (!isMob(t.actor()) && t.motion() != LivingMotions.IDLE) {
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
        if (animation == null && !isMob(t.actor())) return;
        boolean playing = patch != null && animation != null && patch.getAnimator().isPlaying(animation);
        if (!second) playingOnFirstFrame = playing;
        else check(playingOnFirstFrame || playing, t.name() + " from " + t.side().name().toLowerCase() + ": the client plays "
                + (animation == null ? "nothing" : animation.registryName()));
    }

    // ---------------------------------------------------------------- helpers

    /** The client's copy of the actor. */
    private static LivingEntity seen(Minecraft mc, Actor actor) {
        LivingEntity server = isMob(actor) ? bandit : wielder;
        return server == null || mc.level == null ? null : mc.level.getEntity(server.getId()) instanceof LivingEntity e ? e : null;
    }

    /** The local camera on the take's side of the actor standing at (x, y, z). */
    private static void look(Minecraft mc, double x, double y, double z, Side s) {
        if (s == Side.TOP) mc.player.moveTo(x, y + 4.6 * zoom - 1.62, z + 0.01, s.yaw, 90);
        else mc.player.moveTo(x + s.dx * DISTANCE * zoom, y + EYE * zoom - 1.62, z + s.dz * DISTANCE * zoom, s.yaw, 6);
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
