import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the 32 frames of the sect token (a jade medallion with a gold rim and a needle), like the vanilla compass:
 * frame 16 points up (straight ahead), each frame turns the needle 11.25 degrees clockwise.
 *
 *   java scripts/worldgen/TokenTextures.java src/main/resources/assets/murimblock/textures/item
 */
public class TokenTextures {
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "src/main/resources/assets/murimblock/textures/item");
        out.mkdirs();
        for (int frame = 0; frame < 32; frame++) {
            BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                double dx = x - 7.5, dy = y - 7.5, r = Math.hypot(dx, dy);
                if (r > 7.4) continue;
                int c;
                if (r > 6.3) c = (dx + dy < 0) ? 0xF2D27A : (dx + dy > 4 ? 0x9C7420 : 0xD4A93A); // gold rim, lit top-left
                else {
                    // Jade face: deep green, paler veins, a soft highlight.
                    double vein = Math.sin(x * 1.3 + y * 0.7) + Math.cos(y * 1.1 - x * 0.4);
                    c = vein > 1.2 ? 0x8FD1A8 : vein > 0.4 ? 0x4FA07A : 0x2E7A5A;
                    if (dx < -2 && dy < -2 && r < 5) c = 0xA9E2C0;
                    if (Math.abs(r - 3.2) < 0.5) c = 0x24684C; // carved ring of the sect seal
                }
                im.setRGB(x, y, 0xFF000000 | c);
            }
            // Needle: red tip towards the sanctuary, ivory tail.
            double a = (frame - 16) / 32.0 * Math.PI * 2; // clockwise from up
            double ux = Math.sin(a), uy = -Math.cos(a);
            for (double s = -3.5; s <= 5.2; s += 0.25) {
                int x = (int) Math.round(7.5 + ux * s), y = (int) Math.round(7.5 + uy * s);
                if (x < 0 || y < 0 || x > 15 || y > 15) continue;
                im.setRGB(x, y, 0xFF000000 | (s > 2.6 ? 0xE0362A : s > 0 ? 0xB8322A : 0xF3EBD4));
            }
            im.setRGB(7, 7, 0xFF000000 | 0xC9A74B); im.setRGB(8, 8, 0xFF000000 | 0xC9A74B);
            ImageIO.write(im, "png", new File(out, String.format("sect_token_%02d.png", frame)));
        }
        System.out.println("32 sect token frames -> " + out);
    }
}
