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
                    if (color == 0xF0E7CE) paper++;
                    if (color == 0x202522) ink++;
                    if (color == 0xC8A64A) gold++;
                }
            }
            int pixels = image.getWidth() * image.getHeight();
            require(paper > pixels / 5 && ink > pixels / 25 && gold > 100,
                    "Clear Manuscript palette missing from real capture: " + name);
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
