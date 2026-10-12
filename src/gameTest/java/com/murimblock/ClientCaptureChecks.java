package com.murimblock;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Pixel checks on real Minecraft captures, never shipped with the mod. */
final class ClientCaptureChecks {
    private ClientCaptureChecks() { }

    static void verify(Path directory) throws IOException {
        for (String name : new String[]{"01-profile.png", "02-techniques.png", "03-cultivation.png",
                "04-info.png", "05-settings.png", "08-small-scale.png", "10-profile-large.png",
                "11-cultivation-large.png", "12-settings-large.png"}) {
            BufferedImage image = read(directory.resolve(name));
            int paper = 0;
            int ink = 0;
            int gold = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int color = image.getRGB(x, y) & 0xFFFFFF;
                    if (color == 0xF3EBD4 || color == 0xEAE0C4) paper++;
                    if (color == 0x1F2420) ink++;
                    if (color == 0xC9A74B) gold++;
                }
            }
            int pixels = image.getWidth() * image.getHeight();
            // The compact menu (max 320 x 200 GUI units) covers less of large viewports than the old one.
            require(paper > pixels / 12 && ink > pixels / 60 && gold > 100,
                    "Clear Manuscript palette missing from real capture: " + name
                            + " (paper " + paper + ", ink " + ink + ", gold " + gold + " of " + pixels + ")");
        }
        BufferedImage idle = read(directory.resolve("13-mob-idle.png"));
        BufferedImage attack = read(directory.resolve("15-mob-contact.png"));
        require(idle.getWidth() == attack.getWidth() && idle.getHeight() == attack.getHeight(), "Mob capture sizes differ");
        int changed = 0;
        int model = 0;
        for (int y = idle.getHeight() / 6; y < idle.getHeight() * 5 / 6; y++) {
            for (int x = idle.getWidth() / 3; x < idle.getWidth() * 2 / 3; x++) {
                if ((idle.getRGB(x, y) & 0xFFFFFF) != 0xF0E7CE) model++;
                if (idle.getRGB(x, y) != attack.getRGB(x, y)) changed++;
            }
        }
        require(model > 5000 && changed > 3000, "Mob preview blank or animated pose did not change");
        // Murimblock creatures drawn by Epic Fight from their own armature and mesh.
        BufferedImage manIdle = read(directory.resolve("16-crab-man-idle.png"));
        BufferedImage manJab = read(directory.resolve("17-crab-man-jab.png"));
        BufferedImage crab = read(directory.resolve("18-armored-crab-idle.png"));
        int manPixels = 0, manChanged = 0, crabPixels = 0;
        for (int y = manIdle.getHeight() / 6; y < manIdle.getHeight() * 5 / 6; y++) {
            for (int x = manIdle.getWidth() / 4; x < manIdle.getWidth() * 3 / 4; x++) {
                if ((manIdle.getRGB(x, y) & 0xFFFFFF) != 0xF0E7CE) manPixels++;
                if (manIdle.getRGB(x, y) != manJab.getRGB(x, y)) manChanged++;
                if ((crab.getRGB(x, y) & 0xFFFFFF) != 0xF0E7CE) crabPixels++;
            }
        }
        require(manPixels > 5000 && manChanged > 3000, "Crab man preview blank or jab pose did not change (" + manPixels + ", " + manChanged + ")");
        require(crabPixels > 2000, "Armored crab preview blank (" + crabPixels + ")");
        // Water ball drawn by GeckoLib: translucent blue in the claw, then its burst further ahead.
        double[] charge = water(read(directory.resolve("19-water-ball-charge.png")));
        double[] burst = water(read(directory.resolve("21-water-ball-splash.png")));
        require(charge[0] > 1000, "Water ball missing from the crab man's claw (" + charge[0] + " water pixels)");
        require(burst[0] > 300 && burst[1] > charge[1] + 50, "Water ball burst missing or not ahead of the claw ("
                + burst[0] + " pixels, x " + burst[1] + " vs " + charge[1] + ")");
        // At night in a closed room: the reflections glow (GeckoLib glow mask) and the burst lights the room (Photon).
        // Checked on the per-tick frames: the client draws fewer frames than ticks, so fixed ticks can miss the flash.
        java.io.File[] frames = directory.resolve("night").toFile().listFiles((d, n) -> n.startsWith("frame-"));
        require(frames != null && frames.length > 40, "Night frames missing");
        java.util.Arrays.sort(frames);
        double[] dark = luminance(read(frames[0].toPath()));
        double brightest = 0, glowMax = 0;
        for (java.io.File frame : frames) {
            double[] l = luminance(read(frame.toPath()));
            brightest = Math.max(brightest, l[0]);
            glowMax = Math.max(glowMax, l[1]);
        }
        require(dark[1] < 60 && glowMax > 90, "Water ball reflections do not glow in the dark (max " + dark[1] + " -> " + glowMax + ")");
        // Frame-to-frame noise in the dark room is about 0.05; how many frames catch the flash depends on the client's frame rate.
        require(brightest > dark[0] + 0.4, "No Photon flash at the burst (mean " + dark[0] + " -> " + brightest + ")");
    }

    /** Mean and max luminance (0-255). */
    private static double[] luminance(BufferedImage image) {
        double sum = 0;
        int max = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int c = image.getRGB(x, y);
                int l = ((c >> 16 & 255) * 3 + (c >> 8 & 255) * 6 + (c & 255)) / 10;
                sum += l;
                max = Math.max(max, l);
            }
        }
        return new double[]{sum / (image.getWidth() * image.getHeight()), max};
    }

    /** Water-blue pixels (blue well above green and red, unlike the crab man's teal shell): count and mean x. */
    private static double[] water(BufferedImage image) {
        long count = 0, sumX = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int c = image.getRGB(x, y);
                int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
                if (b > g + 25 && b > r + 60) { count++; sumX += x; }
            }
        }
        return new double[]{count, count == 0 ? 0 : (double) sumX / count};
    }

    private static BufferedImage read(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) throw new IOException("Invalid capture: " + path);
        return image;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
