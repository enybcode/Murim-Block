package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.integration.epicfight.ColossusAnimations;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.SonOfTheDemonEntity;
import com.murimblock.mob.majin.MajinFist;
import com.murimblock.world.structure.site.SkyArena;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Development-only film of the Majin Fist in a real client (gradlew -PgameTests runMajinTour): in a new flat world the
 * real sky arena is built, the Son of the Demon stands in its middle facing a husk, and each of his skills is played in
 * turn, seen from the side: the Sunfire Fist, the Pride Flare, the Cruel Sun, the Noon Judgment, the Midday Meteor; then
 * his life falls to a third and The One comes, whole. A picture every other tick (every fourth in The One) to
 * build/majin-tour-run/screenshots/&lt;skill&gt;/frame-NNN.png.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MajinTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks, built = -1;
    private static volatile SonOfTheDemonEntity him;
    private static volatile Husk foe;
    private static BlockPos middle = BlockPos.ZERO;
    private static final List<String> report = new ArrayList<>();

    private record Part(String name, int length, int every, Supplier<AnimationAccessor<? extends StaticAnimation>> move) { }

    private static final List<Part> PARTS = List.of(
            new Part("sunfire_fist", 34, 2, () -> ColossusAnimations.PUNCH),
            new Part("pride_flare", 50, 2, () -> ColossusAnimations.FLARE),
            new Part("cruel_sun", 90, 2, () -> ColossusAnimations.CONJURE),
            new Part("noon_judgment", 60, 2, () -> ColossusAnimations.JUDGMENT),
            new Part("midday_meteor", 80, 2, () -> ColossusAnimations.LEAP),
            new Part("the_one", MajinFist.ONE_LENGTH + MajinFist.ONE_FALL + 40, 4, null));

    private MajinTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.majinTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-majin", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-majin", settings, new WorldOptions(1L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            mc.getToasts().clear();
            step(mc, mc.getSingleplayerServer());
        } catch (Exception e) {
            report.add("FAILED tour crashed: " + e);
            failed = true;
            LOGGER.error("Majin tour crashed", e);
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
                self.getAbilities().flying = true;
                self.getAbilities().invulnerable = true;
                self.onUpdateAbilities();
                middle = SkyArena.floorMiddle(level, (int) self.getX(), (int) self.getZ());
                check(SkyArena.place(level, middle.getX(), middle.getZ()), "the sky arena is being built");
                self.teleportTo(level, middle.getX() + 30.5, middle.getY() + 10, middle.getZ() + 0.5, 90, 20);
            });
            return;
        }
        if (built < 0) {
            if (ticks > 30 && !SkyArena.building()) {
                built = ticks;
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    him = MurimEntities.SON_OF_THE_DEMON.get().spawn(level, middle, MobSpawnType.COMMAND);
                    him.setNoAi(true);
                    him.setArena(middle);
                    him.setYRot(90);
                    him.setYBodyRot(90);
                    him.setYHeadRot(90);
                    foe = EntityType.HUSK.spawn(level, middle.offset(-14, 0, 0), MobSpawnType.COMMAND);
                    foe.setNoAi(true);
                    foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
                    foe.setHealth(1024);
                    foe.setInvulnerable(false);
                    him.setTarget(foe);
                    check(him != null && foe != null, "the Son of the Demon faces his foe in the arena");
                });
            }
            return;
        }
        int at = ticks - built - 40, n = 0;
        if (at < 0) return;
        while (n < PARTS.size() && at >= PARTS.get(n).length()) { at -= PARTS.get(n).length(); n++; }
        if (n >= PARTS.size()) { finish(mc); return; }
        Part part = PARTS.get(n);
        final int t = at;
        server.execute(() -> {
            ServerLevel level = server.overworld();
            if (him == null || !him.isAlive()) return;
            if (t == 0) {
                // Back in the middle, facing the husk, its health whole.
                him.art().land();
                him.setNoGravity(false);
                him.teleportTo(middle.getX() + 0.5, middle.getY(), middle.getZ() + 0.5);
                him.setYRot(90);
                him.setYBodyRot(90);
                him.setYHeadRot(90);
                foe.teleportTo(middle.getX() - 13.5, middle.getY(), middle.getZ() + 0.5);
                foe.setHealth(1024);
                him.setTarget(foe);
                if (part.move() != null) EpicFightCapabilities.getEntityPatch(him, LivingEntityPatch.class).playAnimationSynchronized(part.move().get(), 0);
                else him.setHealth(him.getMaxHealth() * 0.3F);
            }
            // The camera: beside them, a little above, him and his foe both in view (wider for The One).
            Vec3 c = new Vec3(middle.getX() - 6.5, middle.getY(), middle.getZ() + 0.5);
            boolean one = part.move() == null;
            look(self(server), c.x, c.y + (one ? 16 : 7), c.z - (one ? 40 : 17), c.x, c.y + (one ? 9 : 4), c.z);
        });
        if (t % part.every() == 0) shot(mc, part.name() + "/frame-" + String.format("%03d", t / part.every()));
        if (t == part.length() - 1) check(true, part.name() + " filmed");
        if (part.move() == null && t == MajinFist.ONE_RISE + 10) check(him != null && him.theOneTicks() > 0, "The One came at a third of his life");
    }

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void look(ServerPlayer self, double ex, double ey, double ez, double tx, double ty, double tz) {
        double dx = tx - ex, dy = ty - ey, dz = tz - ez;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        self.teleportTo(self.serverLevel(), ex, ey - self.getEyeHeight(), ez, yaw, pitch);
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
        LOGGER.info("Majin tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("majin-tour-report.txt"), String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the majin tour report", e);
        }
        mc.stop();
    }
}
