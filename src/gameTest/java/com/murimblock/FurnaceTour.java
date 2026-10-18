package com.murimblock;

import com.murimblock.alchemy.AlchemyItems;
import com.murimblock.alchemy.AlchemyKnowledge;
import com.murimblock.alchemy.AlchemyRecipes;
import com.murimblock.alchemy.PillFurnaceBlockEntity;
import com.murimblock.qi.QiService;
import com.murimblock.world.plant.HerbItem;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

/**
 * Opt-in graphical check of the pill furnace (docs/cultivation/alchemy.md) in a real client: cold, then lit and
 * refining, its fire screen while extracting, its finish (the lid flying, the pills out), at dusk. Never in the player JAR.
 * Run: gradlew -PgameTests runFurnaceTour; pictures in build/furnace-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class FurnaceTour {
    private static boolean creating, finished;
    private static int ticks;
    private static Marker camera;
    private static final BlockPos AT = new BlockPos(0, -60, 4);

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.furnaceTour") || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("Furnace", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("furnace-" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            MinecraftServer server = mc.getSingleplayerServer();
            if (ticks == 15) server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(12600);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                level.setBlock(AT, AlchemyItems.FURNACE.get().defaultBlockState(), 3);
                ServerPlayer p = player(server);
                p.teleportTo(level, 0.5, -60, 1.0, 0, 20);
                AlchemyKnowledge.learn(p, AlchemyRecipes.byId("qi_gathering_pill"));
                for (AlchemyRecipes.Recipe r : AlchemyRecipes.ALL) AlchemyKnowledge.learn(p, r);
                int slot = 18;
                for (String e : com.murimblock.alchemy.AlchemyItems.ELIXIRS.keySet()) if (slot < 36) p.getInventory().setItem(slot++, new ItemStack(com.murimblock.alchemy.AlchemyItems.ELIXIRS.get(e).get()));
                p.getInventory().setItem(9, HerbItem.of(AlchemyRecipes.item("ginseng_root").get(), 2, "mature", 70));
                p.getInventory().setItem(10, HerbItem.of(AlchemyRecipes.item("chrysanthemum_flower").get(), 7, "mature", 70));
                p.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                p.getInventory().setItem(1, new ItemStack(Items.BREAD, 12));
                p.getInventory().setItem(2, HerbItem.of(AlchemyRecipes.item("goji_berries").get(), 5, "mature", 70));
                p.getInventory().setItem(3, new ItemStack(Items.CHARCOAL, 23));
                QiService.setQiMax(p, 300);
                QiService.setQi(p, 300);
            });
            // The screen of the cold, empty furnace: its recipe book, what goes where.
            if (ticks == 20) server.execute(() -> player(server).openMenu(furnace(server), AT));
            if (ticks == 32) capture(mc, "00-screen-empty.png");
            if (ticks == 34) mc.setScreen(null);
            if (ticks == 40) {
                camera = new Marker(EntityType.MARKER, mc.level);
                place(-2.6, -58.2, 1.2, -30, 22);
                mc.options.hideGui = true;
            }
            if (camera != null && mc.screen == null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            if (ticks == 55) capture(mc, "01-cold.png");
            // Water, herbs, charcoal: it lights and starts refining.
            if (ticks == 60) server.execute(() -> {
                PillFurnaceBlockEntity f = furnace(server);
                ServerPlayer p = player(server);
                f.pourWater(p);
                for (String h : new String[]{"lotus_seed_pod", "sea_buckthorn_berries", "chrysanthemum_flower"}) f.addHerb(p, HerbItem.of(AlchemyRecipes.item(h).get(), 1, "mature", 85));
                f.addFuel(new ItemStack(Items.CHARCOAL));
                f.addFuel(new ItemStack(Items.CHARCOAL));
                f.setBellows(true);
            });
            if (ticks == 120) capture(mc, "02-refining.png");
            if (ticks == 126) capture(mc, "02-refining-b.png");
            // The fire screen, mid-extraction.
            if (ticks == 130) {
                mc.setCameraEntity(mc.player);
                mc.options.hideGui = false;
                server.execute(() -> player(server).openMenu(furnace(server), AT));
            }
            if (ticks == 150) capture(mc, "03-screen.png");
            if (ticks == 152) {
                mc.setScreen(null);
                mc.options.hideGui = true;
                server.execute(() -> {
                    PillFurnaceBlockEntity f = furnace(server);
                    f.setBellows(false);
                    f.setInfusing(player(server), true);
                });
            }
            // Through the steps quickly: the heat held where each step wants it.
            if (ticks > 152 && ticks < 400) server.execute(() -> {
                PillFurnaceBlockEntity f = furnace(server);
                var r = f.recipe();
                if (r != null && f.phase() >= PillFurnaceBlockEntity.EXTRACT && f.phase() <= PillFurnaceBlockEntity.REFINE) {
                    var b = r.band(f.phase());
                    for (int i = 0; i < 3; i++) { f.setHeatForTests((b.low() + b.high()) / 2.0); f.tickForTests(server.overworld()); }
                }
            });
            if (ticks > 152 && ticks < 400 && ticks % 3 == 0 && camera != null) capture(mc, String.format("04-finish-%03d.png", ticks));
            if (ticks == 400) {
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Furnace tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static PillFurnaceBlockEntity furnace(MinecraftServer server) {
        return (PillFurnaceBlockEntity) server.overworld().getBlockEntity(AT);
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void place(double x, double y, double z, float yaw, float pitch) {
        camera.setPos(x, y, z);
        camera.xo = camera.xOld = x;
        camera.yo = camera.yOld = y;
        camera.zo = camera.zOld = z;
        camera.setYRot(yaw);
        camera.yRotO = yaw;
        camera.setXRot(pitch);
        camera.xRotO = pitch;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        var path = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(path.getParent());
        var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        net.minecraft.Util.ioPool().execute(() -> { try (image) { image.writeToFile(path); } catch (Exception e) { throw new RuntimeException(e); } });
    }
}
