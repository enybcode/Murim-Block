package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.client.WarlordCinematic;
import com.murimblock.integration.epicfight.BanditWarlordAnimations;
import com.murimblock.integration.epicfight.BanditWarlordPatch;
import com.murimblock.mob.BanditWarlordEntity;
import com.murimblock.world.structure.site.BanditCampCount;
import com.murimblock.world.structure.site.SitePiece;
import com.murimblock.world.structure.site.SitePlan;
import com.murimblock.world.structure.site.WarlordCrater;
import com.murimblock.world.structure.site.WarlordEntrance;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * Development-only film of the Bandit Warlord in a real client (gradlew -PgameTests runWarlordTour): in a fresh flat
 * world, a bandit camp is built by hand around the player, its garrison taken away, and the warlord's entrance begun as
 * when its last man dies: the film as the player sees it (the drum, the meteor, the impact, his rise and name), the
 * crater seen from above, then each of his moves. A picture every other tick, to build/warlord-tour-run/screenshots/
 * &lt;part&gt;/frame-NNN.png (GIFs with scripts/animation/FramesToGif.java). With murimblock.transformTour (gradlew -PgameTests
 * runTransformTour), after the crater a killing blow instead of his moves: his transformation into the Son of the Demon
 * as the player sees it, the arena in the sky built over the camp and the player carried onto it (transform/). With
 * murimblock.videoTour too (gradlew -PgameTests runTransformVideo), the film is recorded for a video: the server slowed to
 * a quarter of its speed so that no frame is missed, a picture every 1/30 s of the film's own time (video/frames), and
 * every sound it plays logged with its time, file, volume, pitch and distance (video/sounds.tsv), for
 * scripts/animation/make-video.cjs to build the MP4 with its soundtrack.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class WarlordTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** When the camp is built, the entrance begun, and the moves filmed (ticks of the tour). */
    private static final int BUILD = 20, ENTRANCE = 80, CINEMA = 260, CRATER = 60, MOVES = ENTRANCE + CINEMA + CRATER;
    private static boolean creating, finished, failed, sawFilm;
    private static int ticks;
    private static volatile BanditWarlordEntity warlord;
    private static int cx, cz, g;
    private static final List<String> report = new ArrayList<>();

    private record Part(String name, int length, Runnable start) { }

    private static Runnable play(Supplier<AnimationAccessor<? extends StaticAnimation>> animation) {
        return () -> {
            place();
            EpicFightCapabilities.getEntityPatch(warlord, BanditWarlordPatch.class).playAnimationSynchronized(animation.get(), 0);
        };
    }

    private static final List<Part> PARTS = List.of(
            new Part("idle", 50, WarlordTour::place),
            new Part("walk", 40, () -> { place(); warlord.setNoAi(false); warlord.getNavigation().moveTo(warlord.getX() - 8, warlord.getY(), warlord.getZ(), 1.0); }),
            new Part("crusher", 30, play(() -> BanditWarlordAnimations.CRUSHER)),
            new Part("earth_breaker", 44, () -> { play(() -> BanditWarlordAnimations.EARTH_BREAKER).run(); warlord.setNoAi(false); }),
            new Part("bull_charge", 40, () -> { play(() -> BanditWarlordAnimations.BULL_CHARGE).run(); warlord.setNoAi(false); }),
            new Part("sky_breaker", 30, play(() -> BanditWarlordAnimations.SKY_BREAKER)),
            new Part("roar", 56, play(() -> BanditWarlordAnimations.ROAR)),
            new Part("collapse", 50, () -> { play(() -> BanditWarlordAnimations.COLLAPSE).run(); warlord.setNoAi(false); }),
            new Part("leap", 70, () -> { play(() -> BanditWarlordAnimations.LEAP).run(); warlord.setNoAi(false); }));

    private WarlordTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.warlordTour")) return;
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
                    LevelSettings settings = new LevelSettings("murim-warlord", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-warlord", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Warlord tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (ticks == BUILD) {
            mc.options.hideGui = true;
            server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer self = self(server);
                cx = (int) Math.floor(self.getX());
                cz = (int) Math.floor(self.getZ()) - 20;
                // A plateau sixteen blocks thick: the flat world is too shallow for the crater.
                g = (int) Math.floor(self.getY()) + 15;
                net.minecraft.core.BlockPos.MutableBlockPos p = new net.minecraft.core.BlockPos.MutableBlockPos();
                for (int x = cx - 60; x <= cx + 60; x++) for (int z = cz - 60; z <= cz + 60; z++) for (int y = g - 16; y <= g; y++)
                    level.setBlock(p.set(x, y, z), (y == g ? net.minecraft.world.level.block.Blocks.GRASS_BLOCK : y > g - 3 ? net.minecraft.world.level.block.Blocks.DIRT : net.minecraft.world.level.block.Blocks.STONE).defaultBlockState(), 2 | 16);
                self.teleportTo(level, self.getX(), g + 1, self.getZ(), 0, 0);
                self.getAbilities().flying = true;
                self.getAbilities().invulnerable = true;
                self.onUpdateAbilities();
                SitePiece.Built built = SitePiece.buildWhole(level, SitePlan.BANDIT_CAMP, cx, cz, Rotation.NONE, 7L, new int[] {g});
                built.spawned().forEach(Mob::discard);
                check(built.spawned().size() == 35, "the camp was built with its garrison of 35 (" + built.spawned().size() + "), then emptied");
            });
            return;
        }
        if (ticks == ENTRANCE - 10) {
            server.execute(() -> look(self(server), cx + 6.5, g + 3.5, cz + 24.5, cx + 0.5, g + 2, cz + 0.5));
            return;
        }
        if (ticks < ENTRANCE) return;
        if (ticks == ENTRANCE) {
            mc.options.hideGui = false;
            server.execute(() -> {
                ServerLevel level = server.overworld();
                long camp = BanditCampCount.camp(cx, cz);
                // The drum: before the longhall, the camp's local (8, -21) (rotation none).
                WarlordEntrance.beginAt(level, camp, new BlockPos(cx, g + 1, cz), new BlockPos(cx + 8, g + 2, cz - 21));
            });
            return;
        }
        if (ticks > ENTRANCE && ticks <= ENTRANCE + CINEMA) {
            int t = ticks - ENTRANCE;
            sawFilm |= WarlordCinematic.active();
            if (t % 2 == 0) shot(mc, "entrance/frame-" + String.format("%03d", t / 2));
            if (warlord == null) server.execute(() -> {
                var found = server.overworld().getEntitiesOfClass(BanditWarlordEntity.class, new net.minecraft.world.phys.AABB(cx - 30, g - 20, cz - 30, cx + 30, g + 120, cz + 30));
                if (!found.isEmpty()) warlord = found.get(0);
            });
            if (t == CINEMA) {
                check(sawFilm, "the entrance played as a film");
                check(warlord != null, "the warlord fell into the camp");
                check(warlord != null && !warlord.isMeteor() && warlord.getY() < g - 4 && warlord.getY() > WarlordCrater.floor(g, 0) - 1, "he landed at the bottom of the crater (y " + (warlord == null ? "?" : warlord.getY()) + ")");
                check(!WarlordCrater.carving(), "the crater was carved");
            }
            return;
        }
        if (warlord == null) { finish(mc); return; }
        if (Boolean.getBoolean("murimblock.transformTour") && ticks > ENTRANCE + CINEMA + CRATER) { transform(mc, server, ticks - MOVES - 1); return; }
        if (ticks <= ENTRANCE + CINEMA + CRATER) {
            int t = ticks - ENTRANCE - CINEMA;
            mc.options.hideGui = true;
            server.execute(() -> look(self(server), cx + 34.5 - t * 0.2, g + 34, cz + 46.5, cx + 0.5, g - 4, cz + 0.5));
            if (t == CRATER / 2 || t == CRATER - 1) shot(mc, "crater/frame-" + String.format("%03d", t));
            return;
        }
        int at = ticks - MOVES, n = 0;
        while (n < PARTS.size() && at >= PARTS.get(n).length()) { at -= PARTS.get(n).length(); n++; }
        if (n >= PARTS.size()) { finish(mc); return; }
        Part part = PARTS.get(n);
        final int t = at;
        server.execute(() -> {
            if (t == 0) part.start().run();
            double tx = warlord.getX(), ty = warlord.getY(), tz = warlord.getZ();
            look(self(server), tx - 8.5, ty + 5.5, tz - 9.0, tx, ty + 2.0, tz + 0.5);
        });
        if (t % 2 == 1) shot(mc, part.name() + "/frame-" + String.format("%03d", t / 2));
        if (t == part.length() - 1) check(true, part.name() + " filmed");
    }

    private static boolean sawTransformation;

    // ---------------------------------------------------------------- the video
    private static final boolean VIDEO = Boolean.getBoolean("murimblock.videoTour");
    /** The server's speed while recording (ticks a second): a quarter of the game's, so every frame is drawn in time. */
    private static final float SLOW = 5;
    private static final double FPS = 30;
    private static long filmStart = -1;
    private static int frame;
    private static final StringBuilder sounds = new StringBuilder();

    private static double filmTime(Minecraft mc, float partial) {
        return filmStart < 0 || mc.level == null ? -1 : mc.level.getGameTime() - filmStart + partial;
    }

    /** A picture every 1/30 s of the film's time (a frame repeated when the game drew none in that time). */
    @SubscribeEvent
    public static void onVideoFrame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
        if (!VIDEO || filmStart < 0 || finished) return;
        Minecraft mc = Minecraft.getInstance();
        double ft = filmTime(mc, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (ft < 0) return;
        try {
            Path file = null;
            while (frame * 20.0 / FPS <= ft) {
                Path next = mc.gameDirectory.toPath().resolve("video/frames").resolve(String.format("f-%05d.png", frame));
                if (file == null) {
                    try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                        image.writeToFile(next);
                    }
                    file = next;
                } else {
                    Files.copy(file, next, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                frame++;
            }
        } catch (Exception e) {
            LOGGER.error("Video frame failed", e);
        }
    }

    /** Every sound the film plays: its film time (ticks), file, volume, pitch, whether heard everywhere, its distance. */
    @SubscribeEvent
    public static void onVideoSound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
        if (!VIDEO || filmStart < 0 || finished) return;
        Minecraft mc = Minecraft.getInstance();
        var s = event.getSound();
        if (s == null || mc.level == null) return;
        if (s.getSound() == null) s.resolve(mc.getSoundManager());
        if (s.getSound() == null || s.getSound() == net.minecraft.client.sounds.SoundManager.EMPTY_SOUND) return;
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        boolean everywhere = s.isRelative() || s.getAttenuation() == net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE;
        double d = everywhere ? 0 : Math.sqrt(cam.distanceToSqr(s.getX(), s.getY(), s.getZ()));
        float volume = s.getVolume() * s.getSound().getVolume().sample(mc.level.random);
        sounds.append(String.format(java.util.Locale.ROOT, "%.3f	%s	%.3f	%.3f	%s	%.2f	%s%n", filmTime(mc, 0), s.getSound().getPath(),
                volume, s.getPitch(), everywhere, d, s.getLocation()));
    }

    /** The killing blow, then the transformation filmed as the player sees it, to its end and a little after. */
    private static void transform(Minecraft mc, MinecraftServer server, int t) throws Exception {
        if (t == 0) {
            mc.options.hideGui = false;
            if (VIDEO) {
                filmStart = mc.level.getGameTime();
                Files.createDirectories(mc.gameDirectory.toPath().resolve("video/frames"));
                sounds.setLength(0);
                server.execute(() -> server.tickRateManager().setTickRate(SLOW));
            }
            server.execute(() -> {
                // One warlord only (the emptied camp may have called a second): the others and their bars go.
                for (BanditWarlordEntity other : server.overworld().getEntitiesOfClass(BanditWarlordEntity.class, new net.minecraft.world.phys.AABB(cx - 60, g - 40, cz - 60, cx + 60, g + 200, cz + 60)))
                    if (other != warlord) other.discard();
                place();
                warlord.setNoAi(false);
                ServerPlayer self = self(server);
                self.getAbilities().flying = false;
                self.onUpdateAbilities();
                look(self, warlord.getX() + 3, warlord.getY() + 1.62, warlord.getZ() - 5, warlord.getX(), warlord.getY() + 2, warlord.getZ());
                warlord.hurt(server.overworld().damageSources().playerAttack(self), 10_000);
                check(warlord.isAlive() && warlord.transforming(), "the killing blow began his transformation");
            });
            return;
        }
        sawTransformation |= com.murimblock.client.TransformationCinematic.active();
        if (!VIDEO && t % 2 == 0) shot(mc, "transform/frame-" + String.format("%03d", t / 2));
        if (VIDEO && filmTime(mc, 0) > com.murimblock.world.structure.site.WarlordTransformation.END + 40) {
            Files.writeString(mc.gameDirectory.toPath().resolve("video/sounds.tsv"), sounds.toString());
            server.execute(() -> server.tickRateManager().setTickRate(20));
            check(frame > 900, "the film was recorded: " + frame + " frames");
            finish(mc);
            return;
        }
        if (VIDEO) return;
        if (t == com.murimblock.world.structure.site.WarlordTransformation.END + 30) {
            check(sawTransformation, "the transformation played as a film");
            server.execute(() -> {
                ServerLevel level = server.overworld();
                var sons = level.getEntitiesOfClass(com.murimblock.mob.SonOfTheDemonEntity.class, new net.minecraft.world.phys.AABB(cx - 40, g - 40, cz - 40, cx + 40, 330, cz + 40));
                check(sons.size() == 1, "one Son of the Demon (" + sons.size() + ")");
                check(warlord.isRemoved(), "the warlord gave his place");
                BlockPos floor = com.murimblock.world.structure.site.SkyArena.floorMiddle(level, cx, cz);
                check(!level.getBlockState(floor.below()).isAir(), "the arena's floor stands in the sky at y " + floor.below().getY());
                check(!com.murimblock.world.structure.site.SkyArena.building(), "the arena was finished");
                ServerPlayer self = self(server);
                check(Math.abs(self.getY() - floor.getY()) < 3, "the player was carried onto the arena (y " + self.getY() + ")");
                check(!sons.isEmpty() && Math.abs(sons.get(0).getY() - floor.getY()) < 3 && !sons.get(0).awakening(), "the Son of the Demon awaits on it, awake");
            });
            return;
        }
        if (t > com.murimblock.world.structure.site.WarlordTransformation.END + 40) finish(mc);
    }

    /** Back to the crater's floor, frozen, facing north (towards the camera's side). */
    private static void place() {
        warlord.setNoAi(true);
        warlord.setDeltaMovement(0, 0, 0);
        warlord.moveTo(cx + 0.5, WarlordCrater.floor(g, 0) + 1, cz + 0.5, 180, 0);
        warlord.setYBodyRot(180);
        warlord.setYHeadRot(180);
        warlord.yRotO = 180;
        warlord.yHeadRotO = 180;
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
        LOGGER.info("Warlord tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("warlord-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the warlord tour report", e);
        }
        LOGGER.info("Warlord tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
