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
 * Turns the PNG frames of an in-game film (a dev tour's screenshots) into a looping GIF to show Enzo. Keep GIFs out of
 * the repository. Usage: java scripts/frames-to-gif.java <frames dir> <out.gif> [delay ms, 50] [width, 640]
 */
public class FramesToGif {
    public static void main(String[] args) throws Exception {
        File[] frames = new File(args[0]).listFiles((dir, name) -> name.endsWith(".png"));
        if (frames == null || frames.length == 0) {
            throw new IllegalArgumentException("no PNG frames in " + args[0]);
        }
        Arrays.sort(frames);
        int delay = args.length > 2 ? Integer.parseInt(args[2]) : 50;
        int width = args.length > 3 ? Integer.parseInt(args[3]) : 640;
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(new File(args[1]))) {
            writer.setOutput(out);
            writer.prepareWriteSequence(null);
            for (File frame : frames) {
                BufferedImage image = scale(ImageIO.read(frame), width);
                IIOMetadata meta = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), null);
                timing(meta, delay);
                writer.writeToSequence(new IIOImage(image, null, meta), null);
            }
            writer.endWriteSequence();
        }
        System.out.println("wrote " + args[1] + " (" + frames.length + " frames)");
    }

    private static BufferedImage scale(BufferedImage source, int width) {
        int height = Math.round(source.getHeight() * (float) width / source.getWidth());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return image;
    }

    /** Sets the frame delay and makes the GIF loop forever. */
    private static void timing(IIOMetadata meta, int delayMs) throws Exception {
        String format = meta.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);
        IIOMetadataNode control = child(root, "GraphicControlExtension");
        control.setAttribute("disposalMethod", "none");
        control.setAttribute("userInputFlag", "FALSE");
        control.setAttribute("transparentColorFlag", "FALSE");
        control.setAttribute("delayTime", String.valueOf(delayMs / 10));
        control.setAttribute("transparentColorIndex", "0");
        IIOMetadataNode loop = new IIOMetadataNode("ApplicationExtension");
        loop.setAttribute("applicationID", "NETSCAPE");
        loop.setAttribute("authenticationCode", "2.0");
        loop.setUserObject(new byte[] {1, 0, 0});
        child(root, "ApplicationExtensions").appendChild(loop);
        meta.setFromTree(format, root);
    }

    /** The node's child of this name, added if it has none. */
    private static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equals(name)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }
}
