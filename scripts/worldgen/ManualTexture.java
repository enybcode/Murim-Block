import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the 16x16 texture of the Azure Cloud Sword Manual: a jade-green bound book with a gold frame, a dark spine and
 * a pale cloud swirl on the cover.
 * Usage: java scripts/worldgen/ManualTexture.java src/main/resources/assets/murimblock/textures/item
 */
public class ManualTexture {
    static final int CLEAR = 0x00000000;
    static final int SPINE = 0xFF0E3B33, SPINE_LIGHT = 0xFF1B5A4C;
    static final int COVER = 0xFF2E8A72, COVER_DARK = 0xFF236B59, COVER_LIGHT = 0xFF49A98D;
    static final int GOLD = 0xFFE2B746, GOLD_DARK = 0xFFA7792A;
    static final int PAGES = 0xFFEDE3C6, PAGES_SHADE = 0xFFC9BC97;
    static final int CLOUD = 0xFFDDF1EA, CLOUD_SHADE = 0xFFA9D6C6;

    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) img.setRGB(x, y, CLEAR);
        // Book body: x 2..13, y 1..14; spine on the left (x 2..3), pages showing on the right edge and bottom.
        for (int y = 1; y <= 14; y++) for (int x = 2; x <= 13; x++) {
            int c;
            if (x <= 3) c = (x == 3 ? SPINE_LIGHT : SPINE);
            else if (x == 13 || y == 14) c = (y == 14 && x >= 5) || (x == 13 && y >= 2) ? ((x + y) % 2 == 0 ? PAGES : PAGES_SHADE) : COVER_DARK;
            else c = (x + y * 3) % 7 == 0 ? COVER_DARK : COVER;
            img.setRGB(x, y, c);
        }
        // Gold frame on the cover (x 5..11, y 2..12) and corners.
        for (int x = 5; x <= 11; x++) { img.setRGB(x, 2, GOLD); img.setRGB(x, 12, GOLD_DARK); }
        for (int y = 2; y <= 12; y++) { img.setRGB(5, y, GOLD); img.setRGB(11, y, GOLD_DARK); }
        img.setRGB(4, 1, GOLD); img.setRGB(12, 1, GOLD); img.setRGB(4, 13, GOLD_DARK); img.setRGB(12, 13, GOLD_DARK);
        // Spine bands.
        for (int y : new int[]{3, 7, 11}) { img.setRGB(2, y, GOLD_DARK); img.setRGB(3, y, GOLD); }
        // Cloud swirl in the frame.
        int[][] cloud = {{7, 5}, {8, 5}, {6, 6}, {9, 6}, {6, 7}, {8, 7}, {9, 7}, {7, 8}, {10, 8}, {7, 9}, {8, 9}, {9, 9}};
        for (int[] p : cloud) img.setRGB(p[0], p[1], CLOUD);
        for (int[] p : new int[][]{{7, 6}, {8, 8}, {9, 8}, {6, 8}}) img.setRGB(p[0], p[1], CLOUD_SHADE);
        img.setRGB(8, 4, COVER_LIGHT); img.setRGB(9, 10, COVER_LIGHT);
        File out = new File(args[0], "azure_cloud_sword_manual.png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println("wrote " + out);
    }
}
