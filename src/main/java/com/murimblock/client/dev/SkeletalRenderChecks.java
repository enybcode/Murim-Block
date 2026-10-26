package com.murimblock.client.dev;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.murimblock.Murimblock;
import com.murimblock.anim.client.SkeletalPlayerRenderer;
import com.murimblock.anim.client.SkeletalPlayers;
import com.murimblock.client.dev.EngineTest.Step;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Step 3 of the engine roadmap in a real client: a lineup of players with default skins (wide and slim arms, outer
 * layers, items in hand) shot from five sides drawn by vanilla, then by our skinned body, compared pixel by pixel;
 * then the test poses shot for a GIF, and the time our renderer takes per player.
 */
final class SkeletalRenderChecks {
    /** Feet of the lineup's middle, high in the sky so nothing else is in the shots. */
    private static final double X = 0.5, Y = 200, Z = 0.5;
    private static final float SPACING = 1.1f;
    /**
     * Default skins picked by UUID (index = hash % 18; slims first): slim Alex, wide Steve, slim Kai, wide Kai, wide
     * Zuri, slim Sunny.
     */
    private static final int[] SKINS = {0, 15, 3, 12, 17, 7};
    /** At most this share of the picture may differ between vanilla and us (vanilla's idle arm sway moves a few). */
    private static final double MAX_DIFFERENT = 0.004;

    /**
     * A camera place (feet, relative to the lineup), its look, which way the lineup faces, and whether a white floor
     * hides the far terrain (which loads while the test runs) behind the lineup.
     */
    private record View(String name, double dy, double dz, float pitch, float faces, boolean floor) {
    }

    private static final List<View> VIEWS = List.of(
            new View("front", -0.6, -4.5, 0, 180, false),
            new View("back", -0.6, -4.5, 0, 0, false),
            new View("left_side", -0.6, -4.5, 0, 90, false),
            new View("right_side", -0.6, -4.5, 0, -90, false),
            new View("below", -3.5, -1.8, -55, 180, false),
            new View("above", 4, -2.6, 55, 180, true));

    private static final List<RemotePlayer> LINEUP = new ArrayList<>();
    private static NativeImage vanilla;

    private SkeletalRenderChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("lineup", SkeletalRenderChecks::stage));
        for (View view : VIEWS) {
            steps.add(new Step(view.name() + " (place)", mc -> {
                place(mc, view);
                SkeletalPlayers.enable(false);
            }));
            steps.add(new Step(view.name() + " (vanilla)", mc -> {
                vanilla = Screenshot.takeScreenshot(mc.getMainRenderTarget());
                SkeletalPlayers.enable(true);
            }));
            steps.add(new Step(view.name() + " (skeletal)", mc -> compare(mc, view)));
        }
        for (String pose : List.of("bend", "guard", "joints")) {
            for (View view : List.of(VIEWS.get(0), VIEWS.get(2))) {
                steps.add(new Step(pose + " " + view.name() + " (place)", mc -> {
                    place(mc, view);
                    SkeletalPlayers.pose(pose);
                }));
                steps.add(new Step(pose + " " + view.name() + " (shot)", mc ->
                        save(mc, Screenshot.takeScreenshot(mc.getMainRenderTarget()), pose + "_" + view.name())));
            }
        }
        steps.add(new Step("timing (start)", mc -> {
            SkeletalPlayers.pose("bend");
            SkeletalPlayerRenderer.resetTiming();
        }));
        steps.add(new Step("timing", mc -> {
            double micros = SkeletalPlayerRenderer.averageMicros();
            Murimblock.LOGGER.info("Engine test: skeletal player drawn in {} µs on average", String.format("%.1f", micros));
            EngineTest.check("a skeletal player takes under 200 µs to draw (" + String.format("%.1f", micros) + ")",
                    micros > 0 && micros < 200);
            SkeletalPlayers.enable(false);
            SkeletalPlayers.pose("stand");
        }));
        return steps;
    }

    private static void stage(Minecraft mc) {
        mc.options.hideGui = true;
        mc.options.cloudStatus().set(CloudStatus.OFF);
        EngineTest.server(mc, player -> {
            player.serverLevel().setDayTime(6000);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        for (int i = 0; i < SKINS.length; i++) {
            // Frozen (the level counts ticks before calling tick), so the arm sway is the same in both shots.
            RemotePlayer fake = new RemotePlayer(mc.level, new GameProfile(new UUID(0, SKINS[i]), "Test" + i)) {
                @Override
                public void tick() {
                    tickCount = 0;
                }
            };
            fake.setId(-1000 - i);
            if (i == 1) {
                fake.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                fake.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            } else if (i == 4) {
                fake.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                fake.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
            }
            mc.level.addEntity(fake);
            LINEUP.add(fake);
        }
        EngineTest.check("the lineup is in the world", LINEUP.stream().allMatch(fake -> mc.level.getEntity(fake.getId()) == fake));
    }

    private static void place(Minecraft mc, View view) {
        for (int i = 0; i < LINEUP.size(); i++) {
            RemotePlayer fake = LINEUP.get(i);
            fake.moveTo(X + (i - (LINEUP.size() - 1) / 2.0) * SPACING, Y, Z, view.faces(), 0);
            fake.setOldPosAndRot();
            fake.yBodyRot = fake.yBodyRotO = fake.yHeadRot = fake.yHeadRotO = view.faces();
        }
        EngineTest.server(mc, player -> {
            BlockState floor = view.floor() ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState();
            for (BlockPos pos : BlockPos.betweenClosed((int) X - 12, (int) Y - 1, (int) Z - 4, (int) X + 12, (int) Y - 1, (int) Z + 12)) {
                player.serverLevel().setBlockAndUpdate(pos, floor);
            }
            player.connection.teleport(X, Y + view.dy(), Z + view.dz(), 0, view.pitch());
        });
    }

    private static void compare(Minecraft mc, View view) {
        NativeImage ours = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        int different = 0;
        for (int y = 0; y < ours.getHeight(); y++) {
            for (int x = 0; x < ours.getWidth(); x++) {
                int a = vanilla.getPixelRGBA(x, y), b = ours.getPixelRGBA(x, y);
                int most = 0;
                for (int shift = 0; shift < 24; shift += 8) {
                    most = Math.max(most, Math.abs((a >> shift & 0xFF) - (b >> shift & 0xFF)));
                }
                if (most > 32) {
                    different++;
                }
            }
        }
        double share = (double) different / (ours.getWidth() * ours.getHeight());
        EngineTest.check(view.name() + ": skeletal looks like vanilla (" + String.format("%.3f%%", share * 100)
                + " of pixels differ)", share < MAX_DIFFERENT);
        save(mc, vanilla, view.name() + "_1_vanilla");
        save(mc, ours, view.name() + "_2_skeletal");
        vanilla = null;
    }

    private static void save(Minecraft mc, NativeImage image, String name) {
        File file = new File(mc.gameDirectory, "screenshots/step3/" + name + ".png");
        file.getParentFile().mkdirs();
        try (image) {
            image.writeToFile(file);
        } catch (IOException e) {
            Murimblock.LOGGER.error("Engine test: could not save {}", file, e);
        }
    }
}
