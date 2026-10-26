package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.MurimKeys;
import com.murimblock.client.combat.ClientCombat;
import com.murimblock.client.combat.HitboxDebugView;
import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.client.innerworld.TrialEntry;
import com.murimblock.combat.CombatMove;
import com.murimblock.combat.CombatMoves;
import com.murimblock.combat.CombatMoveset;
import com.murimblock.combat.CombatMovesets;
import com.murimblock.combat.CombatServer;
import com.murimblock.combat.FighterState;
import com.murimblock.combat.HitShape;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev tool, off unless {@code murimblock.combatTest} is set (run {@code gradlew runCombatTest}): presses B and clicks like a player
 * at a still husk (a zombie that does not burn) and checks the bare-handed strike (PASS/FAIL in the log), films it in first person and from the
 * side into {@code screenshots/combat/}, then quits.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CombatTest {
    private static final boolean ENABLED = Boolean.getBoolean("murimblock.combatTest");
    private static final String WORLD = "murim-combat-test";
    private static final int FRAMES = 16;
    private static final double ZOMBIE_DISTANCE = 2.2;
    private static final ResourceLocation JAB = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "jab");
    private static final ResourceLocation TEST_SWORD = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "test_sword");

    private static boolean started;
    private static int ticks = -1;
    private static UUID zombieId;
    private static BlockPos center;
    private static float health;
    private static Vec3 before;
    private static final List<String> FAILED = new ArrayList<>();
    /** During the chain checks, clicks go on with the chain instead of starting it over. */
    private static boolean chaining;

    private CombatTest() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (mc.options.onboardAccessibility) {
                mc.options.onboardAccessibility = false;
                mc.options.save();
                mc.setScreen(new TitleScreen());
            } else if (mc.screen instanceof TitleScreen) {
                started = true;
                openWorld(mc);
            }
            return;
        }
        if (mc.player == null || mc.screen != null) {
            return;
        }
        ticks++;
        MinecraftServer server = mc.getSingleplayerServer();
        switch (ticks) {
            case 1 -> {
                setUp(mc, server);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                // The GUI stays for the first person shots: hiding it would hide the arms too.
                new File(mc.gameDirectory, "screenshots/combat").mkdirs();
            }
            case 30 -> {
                check("aims at the zombie", mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY);
                health = health(server);
                click(mc);
            }
            case 31 -> check("out of combat mode, no strike", ClientCombat.ownState() == FighterState.IDLE);
            case 38 -> {
                float lost = health - health(server);
                check("out of combat mode, the vanilla punch stays (lost " + lost + ")", lost > 0.1f && lost < 1.5f);
                KeyMapping.click(MurimKeys.COMBAT_MODE.getKey());
            }
            case 42 -> check("B turns combat mode on", ClientCombat.inCombatMode() && serverMode(server));
            case 46 -> {
                // The getting up of the trial plays on the cultivation layer: combat must not cover it.
                if (PlayerAnimationAccess.getPlayerAnimationLayer(mc.player, ClientCultivation.LAYER)
                        instanceof PlayerAnimationController controller) {
                    controller.triggerAnimation(TrialEntry.RISE);
                }
                click(mc);
            }
            case 47 -> {
                check("no strike while the getting up plays", ClientCombat.ownState() == FighterState.IDLE);
                if (PlayerAnimationAccess.getPlayerAnimationLayer(mc.player, ClientCultivation.LAYER)
                        instanceof PlayerAnimationController controller) {
                    controller.stopTriggeredAnimation();
                }
            }
            case 60 -> {
                placeZombie(server);
                health = health(server);
                before = mc.player.position();
                click(mc);
            }
            case 61 -> check("strike starts at once (predicted)", ClientCombat.ownState() == FighterState.ATTACK);
            case 70 -> check("steps forward", mc.player.position().distanceTo(before) > 0.1);
            case 80 -> {
                float lost = health - health(server);
                check("one strike deals its damage once (lost " + lost + ")", lost > 2.5f && lost < 3.1f);
                placeZombie(server);
                health = health(server);
                click(mc);
            }
            case 100 -> {
                float lost = health - health(server);
                check("next strike lands at once, no vanilla cooldown (lost " + lost + ")", lost > 2.5f && lost < 3.1f);
                server.execute(() -> player(server).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK)));
            }
            case 105 -> {
                check("an item without a style ends combat mode", !ClientCombat.inCombatMode() && !serverMode(server));
                // A sword style made for the test only, matched by item tag: the engine is not tied to bare hands.
                Map<ResourceLocation, CombatMoveset> styles = new HashMap<>(CombatMovesets.all());
                styles.put(TEST_SWORD, new CombatMoveset(List.of("#minecraft:swords"),
                        List.of(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "jab")), 10, 0));
                CombatMovesets.replace(styles);
                server.execute(() -> player(server).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD)));
            }
            case 108 -> KeyMapping.click(MurimKeys.COMBAT_MODE.getKey());
            case 112 -> {
                check("a sword with a style allows combat mode", ClientCombat.inCombatMode() && serverMode(server));
                placeZombie(server);
                health = health(server);
                click(mc);
            }
            case 113 -> check("the sword style strikes", ClientCombat.ownState() == FighterState.ATTACK);
            case 130 -> {
                float lost = health - health(server);
                check("the sword strike lands once, no vanilla sword hit (lost " + lost + ")", lost > 2.5f && lost < 3.1f);
                Map<ResourceLocation, CombatMoveset> styles = new HashMap<>(CombatMovesets.all());
                styles.remove(TEST_SWORD);
                CombatMovesets.replace(styles);
                server.execute(() -> player(server).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
            }
            case 133 -> {
                check("back to bare hands, combat mode stays", ClientCombat.inCombatMode() && serverMode(server));
                mc.player.setXRot(80);
            }
            case 136 -> {
                check("aims at the floor", mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK);
                click(mc);
                mc.options.keyAttack.setDown(true);
            }
            case 137 -> check("in combat mode a block aimed at is struck, not mined",
                    ClientCombat.ownState() == FighterState.ATTACK && !mc.gameMode.isDestroying());
            case 146 -> {
                check("the button held does not mine either", !mc.gameMode.isDestroying());
                mc.options.keyAttack.setDown(false);
                mc.player.setXRot(0);
                mc.player.setYRot(90);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.options.hideGui = true;
            }
            case 155 -> click(mc);
            case 180 -> {
                // A world data pack makes the jab hit for 5, then /reload: the move changes without a restart.
                writeTestPack(server, true);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
            }
            case 230 -> {
                CombatMove jab = CombatMoves.get(JAB);
                check("/reload reads the jab from a world data pack", jab != null && jab.damage() == 5f);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.hideGui = false;
                mc.player.setYRot(0);
                mc.player.setXRot(0);
                server.submit(() -> player(server).connection.teleport(center.getX() + 0.5, center.getY(),
                        center.getZ() + 0.5, 0, 0)).join();
                placeZombie(server);
            }
            case 236 -> {
                health = health(server);
                click(mc);
            }
            case 256 -> {
                float lost = health - health(server);
                check("the reloaded jab hits for 5 (lost " + lost + ")", lost > 4.5f && lost < 5.1f);
                writeTestPack(server, false);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
            }
            case 300 -> {
                CombatMove jab = CombatMoves.get(JAB);
                check("removing the pack and /reload bring the jab back to 3", jab != null && jab.damage() == 3f);
            }
            case 305 -> {
                placeZombie(server);
                health = health(server);
                click(mc);
            }
            // The husk hits back during the windup: the strike stops before it lands.
            case 307 -> blow(server, false);
            case 309 -> check("a blow taken flinches the fighter", ClientCombat.ownState() == FighterState.STUNNED
                    && ClientCombat.flinches() >= 1);
            case 325 -> {
                float lost = health - health(server);
                check("a blow taken in the windup cancels the strike (lost " + lost + ")", lost == 0);
                click(mc);
            }
            case 326 -> check("after the flinch the next click strikes", ClientCombat.ownState() == FighterState.ATTACK);
            case 345 -> {
                placeZombie(server);
                health = health(server);
                click(mc);
            }
            // Fire is no blow: the strike goes on.
            case 347 -> blow(server, true);
            case 365 -> {
                float lost = health - health(server);
                check("burning does not cancel the strike (lost " + lost + ")", lost > 2.5f && lost < 3.1f);
            }
            case 370 -> {
                server.getCommands().performPrefixedCommand(player(server).createCommandSourceStack(),
                        "murim debug hitboxes on");
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.options.hideGui = true;
            }
            case 374 -> check("/murim debug hitboxes on shows the hitboxes", HitboxDebugView.shown());
            case CHAIN + 330 -> {
                FAILED.forEach(name -> Murimblock.LOGGER.error("Combat test: FAIL {}", name));
                Murimblock.LOGGER.info("Combat test: {}", FAILED.isEmpty() ? "PASS" : FAILED.size() + " FAILED");
                mc.stop();
            }
            default -> {
            }
        }
        // Side view: the body turned a quarter from the camera, the strike thrown into the air.
        if (ticks >= 146 && ticks < 230) {
            mc.player.setYBodyRot(mc.player.getYRot() - 90);
            mc.player.yBodyRotO = mc.player.yBodyRot;
            mc.player.setYHeadRot(mc.player.getYRot() - 90);
            mc.player.yHeadRotO = mc.player.yHeadRot;
        }
        film(mc, "fp", ticks - 61);
        film(mc, "side", ticks - 156);
        edges(mc, server);
        chain(mc, server);
    }

    private static boolean serverMode(MinecraftServer server) {
        return server.submit(() -> CombatServer.inCombatMode(player(server))).join();
    }


    /** Writes (or removes) a world data pack whose jab hits for 5 instead of 3. */
    private static void writeTestPack(MinecraftServer server, boolean on) {
        Path pack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("murim_combat_test");
        try {
            if (!on) {
                if (Files.exists(pack)) {
                    try (Stream<Path> files = Files.walk(pack)) {
                        files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                    }
                }
                return;
            }
            Path move = pack.resolve("data/murimblock/combat_moves/jab.json");
            Files.createDirectories(move.getParent());
            Files.writeString(pack.resolve("pack.mcmeta"),
                    "{\"pack\": {\"pack_format\": 48, \"description\": \"Combat test\"}}");
            String jab;
            try (InputStream in = CombatTest.class.getResourceAsStream("/data/murimblock/combat_moves/jab.json")) {
                jab = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            Files.writeString(move, jab.replace("\"damage\": 3.0", "\"damage\": 5.0"));
        } catch (IOException e) {
            check("test data pack written: " + e, false);
        }
    }

    /** One damage on the player: from the husk's fist, or from fire. */
    private static void blow(MinecraftServer server, boolean fire) {
        server.submit(() -> {
            ServerPlayer player = player(server);
            player.invulnerableTime = 0;
            if (fire) {
                player.hurt(player.damageSources().onFire(), 1);
            } else if (server.overworld().getEntity(zombieId) instanceof Zombie zombie) {
                player.hurt(player.damageSources().mobAttack(zombie), 1);
            }
        }).join();
    }

    /** A target placed around the player, and whether the strike should reach it. */
    private record Edge(String name, double dx, double dz, boolean hit, boolean wall, boolean hook) {
    }

    private static final int EDGES_START = 375;
    // Facing +z, the left is +x. The jab's box reaches 3 blocks from the eyes, 1.2 wide; its step adds about 0.3.
    private static final Edge[] EDGES = {
            new Edge("the jab reaches a target 2.6 ahead", 0, 2.6, true, false, false),
            new Edge("the jab misses a target 3.9 ahead, step included", 0, 3.9, false, false, false),
            new Edge("the jab misses a target to the side", 1.6, 1.4, false, false, false),
            new Edge("the jab misses a target behind", 0, -1.5, false, false, false),
            new Edge("the jab does not go through a wall", 0, 2.2, false, true, false),
            new Edge("a hook (arc) reaches a target 45 degrees to the left", 1.3, 1.3, true, false, true),
    };
    private static Map<ResourceLocation, CombatMove> savedMoves;
    private static Map<ResourceLocation, CombatMoveset> savedStyles;

    /** Each edge case: the player back at the center facing +z, the husk placed, one click, the health checked. */
    private static void edges(Minecraft mc, MinecraftServer server) {
        int index = (ticks - EDGES_START) / 25, at = (ticks - EDGES_START) % 25;
        if (ticks < EDGES_START || index >= EDGES.length) {
            return;
        }
        Edge edge = EDGES[index];
        if (at == 0) {
            if (edge.hook()) {
                useTestHook(true);
            }
            BlockPos wall = center.offset(0, 0, 1);
            server.submit(() -> {
                ServerPlayer player = player(server);
                player.connection.teleport(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0, 0);
                for (int y = 0; y < 3; y++) {
                    player.serverLevel().setBlockAndUpdate(wall.above(y),
                            (edge.wall() ? Blocks.STONE : Blocks.AIR).defaultBlockState());
                }
                if (player.serverLevel().getEntity(zombieId) instanceof Zombie zombie) {
                    zombie.moveTo(center.getX() + 0.5 + edge.dx(), center.getY(), center.getZ() + 0.5 + edge.dz(), 180, 0);
                    zombie.setDeltaMovement(Vec3.ZERO);
                    zombie.setHealth(zombie.getMaxHealth());
                }
            }).join();
            mc.player.setYRot(0);
            mc.player.setXRot(0);
        }
        if (at == 2) {
            health = health(server);
            click(mc);
        }
        if (index == 0 || edge.hook()) {
            film(mc, edge.hook() ? "hitbox_hook" : "hitbox_jab", at - 2);
        }
        if (at == 22) {
            float lost = health - health(server);
            check(edge.name() + " (lost " + lost + ")", edge.hit() == lost > 0);
            if (edge.wall()) {
                server.submit(() -> {
                    for (int y = 0; y < 3; y++) {
                        player(server).serverLevel().setBlockAndUpdate(center.offset(0, y, 1), Blocks.AIR.defaultBlockState());
                    }
                }).join();
            }
            if (edge.hook()) {
                useTestHook(false);
            }
        }
    }

    /** Swaps the bare hands' chain for a test hook with an arc hitbox, or puts the real data back. */
    private static void useTestHook(boolean on) {
        if (!on) {
            CombatMoves.replace(savedMoves);
            CombatMovesets.replace(savedStyles);
            return;
        }
        savedMoves = CombatMoves.all();
        savedStyles = CombatMovesets.all();
        CombatMove jab = CombatMoves.get(JAB);
        ResourceLocation hookId = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "test_hook");
        CombatMove hook = new CombatMove(jab.animation(), jab.phases(), HitShape.arc(2.6, 1.4, -70, 20), jab.damage(),
                jab.knockback(), 0, 0, jab.moveSpeed(), jab.hitStop(), 0, CombatMove.FirstPerson.NONE);
        Map<ResourceLocation, CombatMove> moves = new HashMap<>(savedMoves);
        moves.put(hookId, hook);
        CombatMoves.replace(moves);
        Map<ResourceLocation, CombatMoveset> styles = new HashMap<>(savedStyles);
        CombatMoveset bare = styles.get(CombatMovesets.BARE_HANDS);
        styles.put(CombatMovesets.BARE_HANDS, new CombatMoveset(bare.held(), List.of(hookId), bare.chainWindow(),
                bare.priority()));
        CombatMovesets.replace(styles);
    }

    private static final int CHAIN = EDGES_START + 25 * 6 + 15;
    private static final List<String> CHAIN_ORDER = List.of("jab", "cross", "hook", "uppercut");
    private static final List<String> thrown = new ArrayList<>();
    private static double huskStartY;
    private static double huskTopY;

    /**
     * The M1 chain, then the same chain filmed from the side and from the front. Clicks during a move are kept and
     * thrown when it allows, so four clicks in a row give the four moves.
     */
    private static void chain(Minecraft mc, MinecraftServer server) {
        int t = ticks - CHAIN;
        if (t < 0) {
            return;
        }
        ResourceLocation now = ClientCombat.ownMove();
        if (now != null && (thrown.isEmpty() || !thrown.getLast().equals(now.getPath()))) {
            thrown.add(now.getPath());
        }
        switch (t) {
            case 0 -> {
                chaining = true;
                server.getCommands().performPrefixedCommand(player(server).createCommandSourceStack(),
                        "murim debug hitboxes off");
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.hideGui = false;
                standStill(mc, server, 0);
                placeZombie(server);
                server.submit(() -> {
                    if (server.overworld().getEntity(zombieId) instanceof Zombie zombie) {
                        // No knockback, so all four reach it; the uppercut's lift is what moves it.
                        zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
                        huskStartY = zombie.getY();
                        huskTopY = huskStartY;
                    }
                }).join();
            }
            case 2 -> {
                thrown.clear();
                health = health(server);
                click(mc);
            }
            case 4, 17, 32 -> click(mc);
            case 70 -> {
                float lost = health - health(server);
                check("four clicks throw jab, cross, hook, uppercut (" + thrown + ")", thrown.equals(CHAIN_ORDER));
                // 3 + 3.9 + 4.5 + 6 = 17.4 before the husk's armor.
                check("the chain deals its four blows (lost " + lost + ")", lost > 16.5f && lost < 17.5f);
                check("the uppercut pushes the target up (" + (huskTopY - huskStartY) + ")", huskTopY - huskStartY > 0.3);
                placeZombie(server);
            }
            case 90 -> {
                thrown.clear();
                click(mc);
            }
            case 91 -> check("after a long pause the chain starts over at the jab", "jab".equals(path(ClientCombat.ownMove())));
            case 106 -> click(mc);
            case 107 -> check("a click within the window goes on with the cross", "cross".equals(path(ClientCombat.ownMove())));
            case 140 -> click(mc);
            case 142 -> blow(server, false);
            // Without the reset this click, 10 ticks after the jab was cut, would throw the cross.
            case 153 -> click(mc);
            case 154 -> check("a blow taken starts the chain over", "jab".equals(path(ClientCombat.ownMove())));
            case 190 -> {
                // From the side: the body turned a quarter from the camera, the strikes thrown into the air.
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.options.hideGui = true;
                standStill(mc, server, 90);
                server.submit(() -> {
                    if (server.overworld().getEntity(zombieId) instanceof Zombie zombie) {
                        zombie.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 6.5, 180, 0);
                    }
                }).join();
            }
            case 192, 194, 206, 221 -> click(mc);
            case 260 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                standStill(mc, server, 0);
            }
            case 262, 264, 276, 291 -> click(mc);
            default -> {
            }
        }
        if (t >= 46 && t <= 70) {
            server.submit(() -> {
                if (server.overworld().getEntity(zombieId) instanceof Zombie zombie) {
                    // A husk without AI never moves: its upward push is what the uppercut gives.
                    huskTopY = Math.max(huskTopY, huskStartY + zombie.getDeltaMovement().y);
                }
            }).join();
        }
        if (t >= 190 && t < 260) {
            mc.player.setYBodyRot(mc.player.getYRot() - 90);
            mc.player.yBodyRotO = mc.player.yBodyRot;
            mc.player.setYHeadRot(mc.player.getYRot() - 90);
            mc.player.yHeadRotO = mc.player.yHeadRot;
        }
        filmEvery(mc, "chain_fp", t - 3, 64);
        filmEvery(mc, "chain_side", t - 193, 64);
        filmEvery(mc, "chain_front", t - 263, 64);
    }

    private static String path(ResourceLocation id) {
        return id == null ? null : id.getPath();
    }

    /** The player back at the center, looking level along this yaw. */
    private static void standStill(Minecraft mc, MinecraftServer server, float yaw) {
        server.submit(() -> player(server).connection.teleport(center.getX() + 0.5, center.getY(), center.getZ() + 0.5,
                yaw, 0)).join();
        mc.player.setYRot(yaw);
        mc.player.setXRot(0);
    }

    /** One shot every other tick, up to {@code frames} shots. */
    private static void filmEvery(Minecraft mc, String name, int tick, int frames) {
        if (tick >= 0 && tick % 2 == 0 && tick / 2 < frames / 2) {
            Screenshot.grab(mc.gameDirectory, String.format("combat/%s_%02d.png", name, tick / 2), mc.getMainRenderTarget(),
                    message -> { });
        }
    }
    private static void film(Minecraft mc, String name, int frame) {
        if (frame >= 0 && frame < FRAMES) {
            Screenshot.grab(mc.gameDirectory, String.format("combat/%s_%02d.png", name, frame), mc.getMainRenderTarget(),
                    message -> { });
        }
    }

    /** A click; outside the chain checks it starts the chain over, so the strike is always the jab. */
    private static void click(Minecraft mc) {
        if (!chaining) {
            ClientCombat.restartChain();
            MinecraftServer server = mc.getSingleplayerServer();
            server.submit(() -> CombatServer.fighter(player(server)).chain().reset()).join();
        }
        KeyMapping.click(mc.options.keyAttack.getKey());
    }

    private static void check(String name, boolean ok) {
        Murimblock.LOGGER.info("Combat test: {} {}", ok ? "ok  " : "FAIL", name);
        if (!ok) {
            FAILED.add(name);
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static float health(MinecraftServer server) {
        return server.submit(() -> server.overworld().getEntity(zombieId) instanceof Zombie zombie
                ? zombie.getHealth() : -1f).join();
    }

    private static void setUp(Minecraft mc, MinecraftServer server) {
        server.submit(() -> {
            ServerPlayer player = player(server);
            server.setDifficulty(Difficulty.NORMAL, true);
            server.overworld().setDayTime(1000);
            server.overworld().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            player.setGameMode(GameType.SURVIVAL);
            center = BlockPos.containing(player.getX(), 200, player.getZ());
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, -1, -6), center.offset(6, -1, 6))) {
                player.serverLevel().setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
            }
            // The world is kept between runs: clear the mobs of earlier runs.
            player.serverLevel().getEntitiesOfClass(Mob.class, new AABB(center).inflate(16)).forEach(Mob::discard);
            player.connection.teleport(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0, 10);
            Zombie zombie = EntityType.HUSK.create(player.serverLevel());
            zombie.setNoAi(true);
            zombie.setPersistenceRequired();
            zombieId = zombie.getUUID();
            player.serverLevel().addFreshEntity(zombie);
            place(zombie);
        }).join();
    }

    /** Back in front of the player, facing them, healed: knockback moved it. */
    private static void placeZombie(MinecraftServer server) {
        server.submit(() -> {
            if (server.overworld().getEntity(zombieId) instanceof Zombie zombie) {
                place(zombie);
            }
        }).join();
    }

    private static void place(Zombie zombie) {
        zombie.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5 + ZOMBIE_DISTANCE, 180, 0);
        zombie.setDeltaMovement(Vec3.ZERO);
        zombie.setYHeadRot(180);
        zombie.setYBodyRot(180);
        zombie.setHealth(zombie.getMaxHealth());
    }

    private static void openWorld(Minecraft mc) {
        if (mc.getLevelSource().levelExists(WORLD)) {
            mc.createWorldOpenFlows().openWorld(WORLD, () -> Murimblock.LOGGER.error("Combat test: could not open {}", WORLD));
            return;
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(23102026L, false, false),
                WorldPresets::createNormalWorldDimensions, mc.screen);
    }
}
