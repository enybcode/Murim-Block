package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.MurimKeys;
import com.murimblock.client.combat.ClientCombat;
import com.murimblock.combat.CombatMoveset;
import com.murimblock.combat.CombatMovesets;
import com.murimblock.combat.CombatServer;
import com.murimblock.combat.FighterState;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private static final ResourceLocation TEST_SWORD = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "test_sword");

    private static boolean started;
    private static int ticks = -1;
    private static UUID zombieId;
    private static BlockPos center;
    private static float health;
    private static Vec3 before;
    private static final List<String> FAILED = new ArrayList<>();

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
            case 60 -> {
                placeZombie(server);
                health = health(server);
                before = mc.player.position();
                click(mc);
            }
            case 61 -> check("strike starts at once (predicted)", ClientCombat.ownState() == FighterState.ATTACK);
            case 62 -> click(mc);
            case 70 -> check("steps forward", mc.player.position().distanceTo(before) > 0.1);
            case 80 -> {
                float lost = health - health(server);
                check("one strike deals its damage once, even clicked twice (lost " + lost + ")", lost > 2.5f && lost < 3.1f);
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
            case 195 -> {
                FAILED.forEach(name -> Murimblock.LOGGER.error("Combat test: FAIL {}", name));
                Murimblock.LOGGER.info("Combat test: {}", FAILED.isEmpty() ? "PASS" : FAILED.size() + " FAILED");
                mc.stop();
            }
            default -> {
            }
        }
        // Side view: the body turned a quarter from the camera, the strike thrown into the air.
        if (ticks >= 146) {
            mc.player.setYBodyRot(mc.player.getYRot() - 90);
            mc.player.yBodyRotO = mc.player.yBodyRot;
            mc.player.setYHeadRot(mc.player.getYRot() - 90);
            mc.player.yHeadRotO = mc.player.yHeadRot;
        }
        film(mc, "fp", ticks - 61);
        film(mc, "side", ticks - 156);
    }

    private static boolean serverMode(MinecraftServer server) {
        return server.submit(() -> CombatServer.inCombatMode(player(server))).join();
    }

    private static void film(Minecraft mc, String name, int frame) {
        if (frame >= 0 && frame < FRAMES) {
            Screenshot.grab(mc.gameDirectory, String.format("combat/%s_%02d.png", name, frame), mc.getMainRenderTarget(),
                    message -> { });
        }
    }

    private static void click(Minecraft mc) {
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
