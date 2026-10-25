package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.combat.ClientCombat;
import com.murimblock.combat.FighterState;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
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
 * Dev tool, off unless {@code murimblock.combatTest} is set (run {@code gradlew runCombatTest}): clicks like a player
 * at a still husk (a zombie that does not burn) and checks the bare-handed strike (PASS/FAIL in the log), films it in first person and from the
 * side into {@code screenshots/combat/}, then quits.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CombatTest {
    private static final boolean ENABLED = Boolean.getBoolean("murimblock.combatTest");
    private static final String WORLD = "murim-combat-test";
    private static final int FRAMES = 16;
    private static final double ZOMBIE_DISTANCE = 2.2;

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
            case 40 -> {
                check("aims at the zombie", mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY);
                health = health(server);
                before = mc.player.position();
                click(mc);
            }
            case 41 -> check("strike starts at once (predicted)", ClientCombat.ownState() == FighterState.ATTACK);
            case 42 -> click(mc);
            case 50 -> check("steps forward", mc.player.position().distanceTo(before) > 0.1);
            case 60 -> {
                float lost = health - health(server);
                check("one strike deals its damage once, even clicked twice (lost " + lost + ")", lost > 2.5f && lost < 3.1f);
                placeZombie(server);
                health = health(server);
                click(mc);
            }
            case 80 -> {
                float lost = health - health(server);
                check("next strike lands at once, no vanilla cooldown (lost " + lost + ")", lost > 2.5f && lost < 3.1f);
                placeZombie(server);
                server.execute(() -> player(server).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK)));
            }
            case 84 -> {
                health = health(server);
                click(mc);
            }
            case 85 -> check("holding an item, no strike", ClientCombat.ownState() == FighterState.IDLE);
            case 95 -> {
                float lost = health - health(server);
                check("holding an item, the vanilla hit stays (lost " + lost + ")", lost > 0.5f && lost < 1.5f);
                server.execute(() -> player(server).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
                mc.player.setXRot(80);
            }
            case 100 -> {
                check("aims at the floor", mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK);
                click(mc);
            }
            case 101 -> check("a block aimed at is mined, not struck",
                    ClientCombat.ownState() == FighterState.IDLE && mc.player.swinging);
            case 110 -> {
                mc.gameMode.stopDestroyBlock();
                mc.player.setXRot(0);
                mc.player.setYRot(90);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.options.hideGui = true;
            }
            case 120 -> click(mc);
            case 160 -> {
                FAILED.forEach(name -> Murimblock.LOGGER.error("Combat test: FAIL {}", name));
                Murimblock.LOGGER.info("Combat test: {}", FAILED.isEmpty() ? "PASS" : FAILED.size() + " FAILED");
                mc.stop();
            }
            default -> {
            }
        }
        // Side view: the body turned a quarter from the camera, the strike thrown into the air.
        if (ticks >= 110) {
            mc.player.setYBodyRot(mc.player.getYRot() - 90);
            mc.player.yBodyRotO = mc.player.yBodyRot;
            mc.player.setYHeadRot(mc.player.getYRot() - 90);
            mc.player.yHeadRotO = mc.player.yHeadRot;
        }
        film(mc, "fp", ticks - 41);
        film(mc, "side", ticks - 121);
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
