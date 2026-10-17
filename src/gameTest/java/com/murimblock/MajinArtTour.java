package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.combat.ShatteredGround;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import com.murimblock.qi.QiService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Development-only film of the Majin Fist in a player's hands (gradlew -PgameTests runMajinArtTour): in a fresh flat world,
 * at noon, a player who has read the scroll casts each technique at straw-men husks, seen from the side: Sunfire Fist,
 * Pride Flare, Cruel Sun, Noon Judgment, Midday Meteor, the secret Grand Noon, The One whole. A picture every other tick
 * (every fourth in The One) to build/majin-art-tour-run/screenshots/&lt;part&gt;/frame-NNN.png.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MajinArtTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int START = 80;
    private static boolean creating, finished, failed;
    private static int ticks;
    private static int cx, cz, g;
    private static Marker camera;
    private static final List<String> report = new ArrayList<>();
    private static final List<Zombie> dummies = new ArrayList<>();

    /** A part of the film: how long, what happens at its start (server thread), how far the camera stands back. */
    private record Part(String name, int length, java.util.function.Consumer<ServerPlayer> start, double back) { }

    private static final List<Part> PARTS = List.of(
            new Part("sunfire_fist", 34, p -> { place(p); dummy(p, 0, 2.6); dummy(p, 1.4, 3.2); cast(p, 0); }, 8),
            new Part("pride_flare", 50, p -> { place(p); dummy(p, 0, 5); dummy(p, 1, 9); dummy(p, -1, 13); cast(p, 1); }, 12),
            new Part("cruel_sun", 80, p -> { place(p); dummy(p, 0, 12); dummy(p, 2, 13); p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), 0, 14); cast(p, 2); }, 16),
            new Part("noon_judgment", 60, p -> { place(p); dummy(p, 3, 6); dummy(p, -3, 8); dummy(p, 0, 11); cast(p, 3); }, 16),
            new Part("midday_meteor", 60, p -> { place(p); dummy(p, 0, 10); dummy(p, 2, 11); p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), 0, 22); cast(p, 4); }, 16),
            new Part("grand_noon", 40, p -> { place(p); dummy(p, 0, 4); p.setShiftKeyDown(true); cast(p, 0); p.setShiftKeyDown(false); }, 12),
            new Part("the_one", 470, p -> { place(p); dummy(p, 0, 12); dummy(p, 2, 13); dummy(p, 2, 3); p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), 0, 18); cast(p, 5); }, 20));

    private MajinArtTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.majinArtTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
        yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-majin-art", GameType.SURVIVAL, false, Difficulty.EASY, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-majin-art", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            step(mc, mc.getSingleplayerServer());
        } catch (Exception e) {
            report.add("FAILED tour crashed: " + e);
            failed = true;
            LOGGER.error("Majin art tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (ticks == 20) {
            mc.options.hideGui = true;
            server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer self = self(server);
                cx = (int) Math.floor(self.getX());
                cz = (int) Math.floor(self.getZ());
                g = (int) Math.floor(self.getY()) + 9;
                BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
                for (int x = cx - 30; x <= cx + 30; x++) for (int z = cz - 30; z <= cz + 50; z++) for (int y = g - 10; y <= g; y++)
                    level.setBlock(q.set(x, y, z), (y == g ? Blocks.GRASS_BLOCK : y > g - 3 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), 2 | 16);
                self.getAbilities().invulnerable = true;
                self.onUpdateAbilities();
                EpicFightCombatDefaults.enforce(self);
                CultivationService.setCultivation(self, MartialArts.MAJIN_REALM, CultivationStage.EARLY);
                new ItemStack(MurimItems.MAJIN_FIST_SCROLL.get()).use(level, self, InteractionHand.MAIN_HAND);
                self.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                check(MartialService.data(self).knowsArt(MartialArts.MAJIN_FIST), "the scroll teaches the art at Qi Liquidation");
                var all = List.of(MartialArts.PRIDE_FLARE, MartialArts.CRUEL_SUN, MartialArts.NOON_JUDGMENT, MartialArts.MIDDAY_METEOR, MartialArts.THE_ONE);
                for (int i = 0; i < all.size(); i++) { MartialService.unlock(self, all.get(i)); MartialService.assign(self, i + 1, all.get(i).id()); }
                place(self);
            });
            return;
        }
        if (ticks < START) return;
        int at = ticks - START, n = 0;
        while (n < PARTS.size() && at >= PARTS.get(n).length()) { at -= PARTS.get(n).length(); n++; }
        if (n >= PARTS.size()) { finish(mc); return; }
        Part part = PARTS.get(n);
        final int t = at;
        if (t == 0) server.execute(() -> {
            ServerPlayer self = self(server);
            dummies.forEach(z -> z.discard());
            dummies.clear();
            MartialService.clearCooldownsForTests(self);
            QiService.setQiMax(self, 2000);
            QiService.setQi(self, 2000);
            // Each part on whole ground (but the last, which waits for the broken ground to come back by itself).
            {
                ShatteredGround.restoreAllForTests(self.serverLevel());
                BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
                for (int x = cx - 10; x <= cx + 10; x++) for (int z = cz - 8; z <= cz + 26; z++) for (int y = g + 1; y <= g + 5; y++)
                    self.serverLevel().setBlock(q.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
            part.start().accept(self);
        });
        // The camera beside the player, looking across at him (wider for the ultimate).
        if (camera == null) {
            camera = new Marker(EntityType.MARKER, mc.level);
            mc.setCameraEntity(camera);
        }
        Vec3 him = mc.player.position();
        boolean wide = part.back() >= 12;
        Vec3 eye = wide ? new Vec3(cx + 0.5 - part.back() * 0.6, g + 1 + part.back() * 0.22, cz + 2.5) : him.add(-part.back() * 0.7, 2.2, -1.0);
        Vec3 look = wide ? new Vec3(cx + 0.5, g + 2.5, cz + 5.5) : him.add(0, 1.2, 1.5);
        Vec3 d = look.subtract(eye);
        camera.moveTo(eye.x, eye.y, eye.z, (float) Math.toDegrees(Math.atan2(-d.x, d.z)), (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        camera.xo = camera.xOld = eye.x; camera.yo = camera.yOld = eye.y; camera.zo = camera.zOld = eye.z;
        camera.yRotO = camera.getYRot(); camera.xRotO = camera.getXRot();
        if (part.name().equals("the_one") ? t % 4 == 1 : t % 2 == 1) shot(mc, part.name() + "/frame-" + String.format("%03d", t / 2));
        if (t == part.length() - 1) check(true, part.name() + " filmed");
    }

    /** Back to the middle of the plateau, facing south (+z), on the ground. */
    private static void place(ServerPlayer p) {
        p.teleportTo(p.serverLevel(), cx + 0.5, g + 1, cz + 0.5, 0, 0);
        p.setDeltaMovement(Vec3.ZERO);
    }

    private static void cast(ServerPlayer p, int slot) {
        var result = MartialService.castSlot(p, slot);
        check(result == MartialService.CastResult.CAST, "slot " + (slot + 1) + " cast (" + result + ")");
    }

    /** A stone wall three wide and three high, `ahead` blocks before the player. */
    private static void wall(ServerPlayer p, int ahead) {
        for (int x = -1; x <= 1; x++) for (int y = 1; y <= 3; y++)
            p.serverLevel().setBlock(new BlockPos(cx + x, g + y, cz + ahead), Blocks.COBBLESTONE.defaultBlockState(), 3);
    }

    /** A straw-man: a zombie with no mind, much health. */
    private static void dummy(ServerPlayer p, double dx, double dz) {
        Zombie z = EntityType.HUSK.spawn(p.serverLevel(), BlockPos.containing(cx + dx, g + 1, cz + dz), MobSpawnType.COMMAND);
        if (z == null) return;
        z.setNoAi(true);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300);
        z.setHealth(300);
        z.moveTo(cx + 0.5 + dx, g + 1, cz + 0.5 + dz, 180, 0);
        z.setPersistenceRequired();
        dummies.add(z);
    }

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void shot(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name + ".png");
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Majin art tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("majin-art-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the majin art tour report", e);
        }
        LOGGER.info("Majin art tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
