import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/**
 * Assembles PNG frames into a looping animated GIF, for checking or sharing an in-game animation.
 * Usage: java scripts/animation/FramesToGif.java <frames dir> <out.gif> [delay ms, default 50] [width, default 640]
 *        [crop x,y,w,h in frame pixels, optional]
 * Or a contact sheet: java scripts/animation/FramesToGif.java --sheet <frames dir> <out.png> [columns] [width per frame]
 */
public class FramesToGif {
    public static void main(String[] args) throws Exception {
        if (args[0].equals("--sheet")) { sheet(args); return; }
        File[] frames = frames(args[0]);
        int delay = args.length > 2 ? Integer.parseInt(args[2]) : 50;
        int width = args.length > 3 ? Integer.parseInt(args[3]) : 640;
        int[] crop = args.length > 4 ? Arrays.stream(args[4].split(",")).mapToInt(Integer::parseInt).toArray() : null;
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(new File(args[1]))) {
            writer.setOutput(out);
            writer.prepareWriteSequence(null);
            for (File f : frames) {
                BufferedImage img = scale(cropped(ImageIO.read(f), crop), width);
                IIOMetadata meta = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(img), null);
                String format = meta.getNativeMetadataFormatName();
                IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);
                IIOMetadataNode gce = node(root, "GraphicControlExtension");
                gce.setAttribute("disposalMethod", "none");
                gce.setAttribute("userInputFlag", "FALSE");
                gce.setAttribute("transparentColorFlag", "FALSE");
                gce.setAttribute("delayTime", Integer.toString(delay / 10));
                gce.setAttribute("transparentColorIndex", "0");
                IIOMetadataNode apps = node(root, "ApplicationExtensions");
                IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
                app.setAttribute("applicationID", "NETSCAPE");
                app.setAttribute("authenticationCode", "2.0");
                app.setUserObject(new byte[]{1, 0, 0}); // loop forever
                apps.appendChild(app);
                meta.setFromTree(format, root);
                writer.writeToSequence(new IIOImage(img, null, meta), null);
            }
            writer.endWriteSequence();
        }
        System.out.println(frames.length + " frames -> " + args[1]);
    }

    private static void sheet(String[] args) throws Exception {
        File[] frames = frames(args[1]);
        int columns = args.length > 3 ? Integer.parseInt(args[3]) : 8;
        int width = args.length > 4 ? Integer.parseInt(args[4]) : 240;
        BufferedImage first = scale(ImageIO.read(frames[0]), width);
        int rows = (frames.length + columns - 1) / columns;
        BufferedImage out = new BufferedImage(columns * width, rows * first.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        for (int i = 0; i < frames.length; i++) {
            g.drawImage(scale(ImageIO.read(frames[i]), width), (i % columns) * width, (i / columns) * first.getHeight(), null);
            g.drawString(Integer.toString(i + 1), (i % columns) * width + 4, (i / columns) * first.getHeight() + 14);
        }
        g.dispose();
        ImageIO.write(out, "png", new File(args[2]));
    }

    private static File[] frames(String dir) {
        File[] frames = new File(dir).listFiles((d, n) -> n.endsWith(".png"));
        Arrays.sort(frames);
        return frames;
    }

    private static BufferedImage cropped(BufferedImage img, int[] crop) {
        return crop == null ? img : img.getSubimage(crop[0], crop[1], crop[2], crop[3]);
    }

    private static BufferedImage scale(BufferedImage src, int width) {
        int height = src.getHeight() * width / src.getWidth();
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    private static IIOMetadataNode node(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) if (root.item(i).getNodeName().equalsIgnoreCase(name)) return (IIOMetadataNode) root.item(i);
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }
}
