package com.murimblock;

import com.murimblock.client.gui.MurimProfileScreen;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
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
 * Opt-in graphical check of the HUD and the Techniques page at full screen (1920 x 1080, the GUI scale left on auto), as
 * a player sees them: in survival, out of combat then in combat, the combat bar full (the Azure Cloud Sword, a sword in
 * hand), Qi spent, a technique cooling down; then the Techniques page, a technique's page, the passive's, the Profile.
 * Never included in the player JAR. Run: gradlew -PgameTests runHudTour; pictures in build/hud-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class HudTour {
    private static boolean creating, finished;
    private static int ticks;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.hudTour") || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("HUD", GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("hud-" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.options.hideGui = false;
            mc.getToasts().clear();
            var server = mc.getSingleplayerServer();
            switch (ticks) {
                case 15 -> server.execute(() -> {
                    var level = server.overworld();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                    ServerPlayer player = player(server);
                    MartialService.learn(player, MartialArts.art(MartialArts.GOLDEN_BELL));
                    MartialService.learn(player, MartialArts.art(MartialArts.AZURE_CLOUD_SWORD));
                    MartialService.setMainArt(player, MartialArts.AZURE_CLOUD_SWORD);
                    int slot = 0;
                    for (MartialArts.Technique t : MartialArts.art(MartialArts.AZURE_CLOUD_SWORD).techniques()) {
                        MartialService.unlock(player, t);
                        if (t.castable() && slot < 9) MartialService.assign(player, slot++, t.id());
                    }
                    MartialArts.Technique[] bell = {MartialArts.GOLDEN_BELL_FORM, MartialArts.IRON_REBOUND, MartialArts.ROOTED_MOUNTAIN};
                    for (int i = 0; i < bell.length; i++) { MartialService.unlock(player, bell[i]); MartialService.assign(player, 5 + i, bell[i].id()); }
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                    player.getInventory().setItem(1, new ItemStack(Items.BREAD, 12));
                    player.getInventory().setItem(2, new ItemStack(Items.TORCH, 30));
                    com.murimblock.qi.QiService.setQiMax(player, 200);
                    com.murimblock.qi.QiService.setQi(player, 130);
                    player.setHealth(15);
                    player.teleportTo(level, player.getX(), player.getY(), player.getZ(), 30, 8);
                });
                case 40 -> capture(mc, "01-hud-out-of-combat.png");
                case 42 -> server.execute(() -> com.murimblock.combat.CombatService.setCombatMode(player(server), true));
                case 60 -> capture(mc, "02-hud-combat.png");
                case 62 -> server.execute(() -> {
                    ServerPlayer player = player(server);
                    com.mojang.logging.LogUtils.getLogger().info("HUD tour: slot 1 -> {}", MartialService.castSlot(player, 1));
                });
                case 64 -> {
                    // The cooldown as the client hears it from a real key (the tour cast it on the server).
                    com.murimblock.client.CombatBarClient.cooldown(MartialArts.THOUSAND_DRIZZLE.id(), 160);
                    com.murimblock.client.CombatBarClient.cooldown(MartialArts.IRON_REBOUND.id(), 60);
                }
                case 90 -> capture(mc, "03-hud-cooldown.png");
                case 100 -> mc.setScreen(MurimProfileScreen.techniques());
                case 106 -> capture(mc, "04-techniques.png");
                case 108 -> ((MurimProfileScreen) mc.screen).selectForTests(MartialArts.CLOUD_STEP_SLASH.id());
                case 114 -> capture(mc, "05-technique-page.png");
                case 116 -> ((MurimProfileScreen) mc.screen).selectForTests(MartialArts.art(MartialArts.AZURE_CLOUD_SWORD).passive().id());
                case 122 -> capture(mc, "06-passive-page.png");
                case 124 -> mc.setScreen(new MurimProfileScreen());
                case 130 -> capture(mc, "07-profile.png");
                case 140 -> {
                    finished = true;
                    mc.stop();
                }
                default -> { }
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("HUD tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        var path = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(path.getParent());
        var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        net.minecraft.Util.ioPool().execute(() -> { try (image) { image.writeToFile(path); } catch (Exception e) { throw new RuntimeException(e); } });
    }
}
