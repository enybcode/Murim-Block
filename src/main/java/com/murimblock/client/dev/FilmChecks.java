package com.murimblock.client.dev;

import com.mojang.blaze3d.platform.NativeImage;
import com.murimblock.Murimblock;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.AttackData;
import com.murimblock.shenfa.client.SkeletalClips;
import com.murimblock.shenfa.client.SkeletalPlayers;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Contact sheets of the fighting clips, to look at them: each clip of a set is shown on a still test player at a few
 * moments (the start, the windup, the middle of the hit window, the recovery, the end), seen from its left side, one
 * row per clip ({@code screenshots/film/<set>.png}).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class FilmChecks {
    private static final double X = 0.5, Y = 200, Z = 30.5;
    /** The part of each shot kept on the sheet (pixels of the window), and its place there. */
    private static final int CELL_W = 300, CELL_H = 400;
    private static final int COLUMNS = 5;

    private record Shot(int row, int column, float time) {
    }

    private static SkeletalRenderChecks.Dummy actor;
    private static final Queue<Shot> shots = new ArrayDeque<>();
    private static NativeImage sheet;
    private static String sheetName;
    private static Shot pending;
    private static int wait;

    private FilmChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("film setup", mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            SkeletalPlayers.enable(true);
            SkeletalRenderChecks.clearLineup(mc);
            actor = new SkeletalRenderChecks.Dummy(mc, 21, 51);
            mc.level.addEntity(actor);
            // Seen from its left side: it faces east, the camera looks north at it from the south.
            actor.moveTo(X, Y, Z, -90, 0);
            actor.setOldPosAndRot();
            actor.yBodyRot = actor.yBodyRotO = actor.yHeadRot = actor.yHeadRotO = -90;
            EngineTest.server(mc, player -> {
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.connection.teleport(X, Y - 0.75, Z + 4.2, 180, 0);
            });
            mc.player.getAbilities().flying = true;
        }));
        for (String set : List.of("fists", "combat", "blade", "humanoid")) {
            steps.add(new Step("film " + set, mc -> film(mc, set)));
            steps.add(new Step("film " + set + " saved", mc -> EngineTest.check("the " + set + " sheet is filmed",
                    shots.isEmpty() && sheet == null)));
        }
        steps.add(new Step("film done", mc -> {
            SkeletalPlayers.slowMotion(1);
            SkeletalPlayers.previewPlayer().stop();
            SkeletalPlayers.enable(false);
            mc.level.removeEntity(actor.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            EngineTest.busy = () -> false;
        }));
        return steps;
    }

    private static void film(Minecraft mc, String set) {
        List<ResourceLocation> clips = SkeletalClips.all().keySet().stream()
                .filter(id -> id.getNamespace().equals(Murimblock.MOD_ID) && id.getPath().startsWith(set + "/"))
                .sorted().toList();
        sheet = new NativeImage(CELL_W * COLUMNS, CELL_H * Math.max(1, clips.size()), true);
        sheet.fillRect(0, 0, sheet.getWidth(), sheet.getHeight(), 0xFF202020);
        sheetName = set;
        for (int row = 0; row < clips.size(); row++) {
            Animation clip = SkeletalClips.get(clips.get(row));
            float end = clip.clip().duration();
            float[] times = {0, end * 0.25f, end * 0.5f, end * 0.75f, end};
            AttackData attack = clip.attack();
            if (attack != null && !attack.phases().isEmpty()) {
                AttackData.Phase phase = attack.phases().get(0);
                times = new float[] {0, phase.windup(), (phase.activeFrom() + phase.activeTo()) / 2, phase.recovery(),
                        end};
            }
            for (int column = 0; column < COLUMNS; column++) {
                shots.add(new Shot(row, column, times[column]));
            }
            Murimblock.LOGGER.info("Engine test film {} row {}: {}", set, row, clips.get(row));
        }
        SkeletalPlayers.slowMotion(0);
        wait = 10;
        EngineTest.busy = () -> sheet != null;
    }

    /** One shot per frame: the frame drawn after a seek is cut into the sheet, then the next moment is set. */
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        if (sheet == null) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (pending != null) {
            try (NativeImage shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                int x = Math.max(0, shot.getWidth() / 2 - CELL_W / 2), y = Math.max(0, shot.getHeight() / 2 - CELL_H / 2);
                int w = Math.min(CELL_W, shot.getWidth()), h = Math.min(CELL_H, shot.getHeight());
                shot.copyRect(sheet, x, y, pending.column() * CELL_W, pending.row() * CELL_H, w, h, false, false);
            }
            pending = null;
        }
        Shot next = shots.poll();
        if (next == null) {
            File file = new File(mc.gameDirectory, "screenshots/film/" + sheetName + ".png");
            file.getParentFile().mkdirs();
            try {
                sheet.writeToFile(file);
            } catch (IOException e) {
                Murimblock.LOGGER.error("Engine test: could not save {}", file, e);
            }
            sheet.close();
            sheet = null;
            return;
        }
        if (next.column() == 0) {
            ResourceLocation id = SkeletalClips.all().keySet().stream()
                    .filter(clip -> clip.getPath().startsWith(sheetName + "/")).sorted().toList().get(next.row());
            SkeletalPlayers.preview(id);
        }
        SkeletalPlayers.previewPlayer().seek(next.time());
        pending = next;
        wait = 2;
    }
}
