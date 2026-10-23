package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.Meditation;
import com.murimblock.innerworld.InnerIsland;
import com.murimblock.innerworld.InnerIslands;
import com.murimblock.innerworld.InnerWorld;
import com.murimblock.innerworld.MeditatingBody;
import java.util.UUID;
import java.util.function.Consumer;
import com.murimblock.client.gui.SystemPage;
import com.murimblock.client.gui.SystemScreen;
import com.murimblock.client.gui.page.CultivationPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Dev tool, off unless {@code murimblock.innerWorldTour} is set (run {@code gradlew runInnerWorldTour}): checks the
 * inner world in a real client (locked as a mortal, entry, island, nothing breakable or placeable, falling, the seated
 * body and a blow on it), logs PASS or FAIL for each, saves screenshots to run/screenshots, then quits.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class InnerWorldTour {
    private static final boolean ENABLED = Boolean.getBoolean("murimblock.innerWorldTour");
    private static final String WORLD = "murim-inner-world-tour";
    private static final int WORLD_FRAMES = 120;
    private static final int STEP_FRAMES = 60;

    private static boolean started;
    private static int frames = -1;
    private static int step;
    private static int failures;
    private static BlockPos sat;

    private InnerWorldTour() {
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            // A fresh game folder opens on the accessibility screen first.
            if (mc.options.onboardAccessibility) {
                mc.options.onboardAccessibility = false;
                mc.options.save();
                mc.setScreen(new TitleScreen());
            } else if (mc.screen instanceof TitleScreen) {
                started = true;
                frames = -1;
                openWorld(mc);
            } else if (++frames % 300 == 0) {
                Murimblock.LOGGER.info("Inner world tour: waiting for the title screen, on {}", mc.screen);
            }
            return;
        }
        if (mc.player == null || mc.screen != null && !(mc.screen instanceof SystemScreen)) {
            return;
        }
        if (++frames < WORLD_FRAMES || (frames - WORLD_FRAMES) % STEP_FRAMES != 0) {
            return;
        }
        run(mc, step++);
    }

    private static void run(Minecraft mc, int index) {
        switch (index) {
            case 0 -> server(mc, player -> {
                // A mortal, meditating on a platform in the sky: the inner world stays shut.
                sat = BlockPos.containing(player.getX(), 200, player.getZ());
                for (BlockPos pos : BlockPos.betweenClosed(sat.offset(-3, -1, -3), sat.offset(3, -1, 3))) {
                    player.serverLevel().setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.teleportTo(sat.getX() + 0.5, sat.getY(), sat.getZ() + 0.5);
                Cultivation c = Meditation.of(player);
                c.set(0, 9, 0, c.maxQi(), c.maxQi(), 3);
            });
            case 1 -> server(mc, player -> {
                Meditation.start(player);
                InnerWorld.enter(player);
                check("mortal stays out", !InnerWorld.state(player).inside() && !InnerWorld.isInnerWorld(player.level()));
                Cultivation c = Meditation.of(player);
                c.set(1, 1, 0, c.maxQi(), c.maxQi(), 3);
                Meditation.sync(player);
                InnerWorld.enter(player);
                check("third-rate enters", InnerWorld.state(player).inside() && InnerWorld.isInnerWorld(player.level()));
                BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
                check("arrival on own island", center != null && Math.abs(player.getX() - center.getX() - 0.5) < 1
                        && Math.abs(player.getZ() - center.getZ() - InnerIsland.ARRIVAL_Z - 0.5) < 1);
                check("body left seated", player.server.overworld().getEntity(InnerWorld.state(player).body())
                        instanceof MeditatingBody);
            });
            case 2 -> {
                shoot(mc, "murim_inner_arrival.png");
                mc.setScreen(new SystemScreen(SystemPage.CULTIVATION, new CultivationPage(false)));
            }
            case 3 -> {
                // The Cultivation page inside: the button now reads RETURN TO BODY.
                shoot(mc, "murim_inner_page.png");
                mc.setScreen(null);
            }
            case 4 -> server(mc, player -> {
                BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
                BlockPos floor = center.offset(3, 0, 3);
                BlockState before = player.level().getBlockState(floor);
                player.gameMode.destroyBlock(floor);
                check("arena block cannot be broken", player.level().getBlockState(floor) == before && !before.isAir());
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
                player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
                check("no block placed", player.level().getBlockState(floor.above()).isAir());
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                // Hover above the south shore, looking down on the whole island.
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.connection.teleport(center.getX() + 0.5, InnerIsland.SURFACE_Y + 30, center.getZ() + 58.5, 180, 30);
            });
            case 5 -> shoot(mc, "murim_inner_aerial.png");
            case 6 -> server(mc, player -> {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
                player.teleportTo(center.getX() + 0.5, InnerIsland.SURFACE_Y - 50, center.getZ() + 0.5);
            });
            case 7 -> server(mc, player -> {
                check("fall returns to body", !InnerWorld.state(player).inside() && !InnerWorld.isInnerWorld(player.level())
                        && player.blockPosition().distManhattan(sat) <= 2);
                // The body seen from the front: seat one, then step back to look at it.
                Meditation.start(player);
                MeditatingBody body = MeditatingBody.seat(player);
                body.setYRot(180);
                body.setYHeadRot(180);
                body.setYBodyRot(180);
                InnerWorld.state(player).enter(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                        player.getYRot(), player.getXRot(), body.getUUID());
                Meditation.stop(player, null);
                player.connection.teleport(sat.getX() + 0.5, sat.getY(), sat.getZ() - 2.5, 0, 25);
            });
            case 8 -> {
                mc.options.hideGui = true;
                return;
            }
            case 9 -> {
                shoot(mc, "murim_inner_body.png");
                mc.options.hideGui = false;
            }
            case 10 -> server(mc, player -> {
                UUID bodyId = InnerWorld.state(player).body();
                if (player.serverLevel().getEntity(bodyId) instanceof MeditatingBody body) {
                    body.hurt(player.damageSources().playerAttack(player), 1);
                }
                check("a blow on the body ends the trip", !InnerWorld.state(player).inside()
                        && player.serverLevel().getEntity(bodyId) == null);
            });
            default -> {
                Murimblock.LOGGER.info("Inner world tour: done, {} failure(s)", failures);
                mc.stop();
            }
        }
    }

    private static void check(String what, boolean ok) {
        if (!ok) {
            failures++;
        }
        Murimblock.LOGGER.info("Inner world tour: {} {}", ok ? "PASS" : "FAIL", what);
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        UUID id = mc.player.getUUID();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(id)));
    }

    private static void shoot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                message -> Murimblock.LOGGER.info("Inner world tour shot: {}", message.getString()));
    }

    private static void openWorld(Minecraft mc) {
        if (mc.getLevelSource().levelExists(WORLD)) {
            mc.createWorldOpenFlows().openWorld(WORLD, () -> Murimblock.LOGGER.error("Inner world tour: could not open {}", WORLD));
            return;
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(23102026L, false, false),
                WorldPresets::createNormalWorldDimensions, mc.screen);
    }
}
