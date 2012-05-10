/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sudoku;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * A <code>ColorKuImage</code> is a <code>BufferedImage</code> that
 * has a specific color and has an overlay applied to it.<br><br>
 * 
 * Overlays are in the package <code>overlay</code> and named
 * <code>ovnnn.png</code>, where "nnn" stands for a number between
 * {@link #IMG_MIN} and {@link #IMG_MAX} (both inclusive) with an increment
 * of {@link #IMG_FACTOR}.<br><br>
 * 
 * When creating a ColoKuImage, the best matched overlay is taken and
 * centered within the image.
 * 
 * @author hobiwan
 */
public class ColorKuImage extends BufferedImage {

    /** Size of smallest overlay in pixel */
    private static final int IMG_MIN = 10;
    /** Size of largest overlay in pixel */
    private static final int IMG_MAX = 98;
    /** Increment for overlay sizes in pixel */
    private static final int IMG_FACTOR = 4;
    /** The latest overlay loaded, for caching */
    private static BufferedImage lastOverlay = null;
    /** The color of the image */
    private Color color = null;

    public ColorKuImage(int size, Color color) {
        super(size, size, BufferedImage.TYPE_4BYTE_ABGR);
        this.color = color;
        createImage();
    }

    /**
     * Get the overlay that fits best and load it into
     * {@link #lastOverlay} if necessary. Then paint a
     * filled circle using <code>color</code> centered
     * on the image and apply the pattern from <code>lastOverlay</code>.<br><br>
     * 
     * Pixels, that are white in <code>lastOverlay</code>, become
     * completely transparent in <code>this</code>.
     */
    private void createImage() {
        // get the overlay that fits best
        long ticks = System.nanoTime();
        int sizeR = ((getWidth() - IMG_MIN) / IMG_FACTOR) * IMG_FACTOR + IMG_MIN;
        if (sizeR < IMG_MIN) {
            sizeR = IMG_MIN;
        }
        if (sizeR > IMG_MAX) {
            sizeR = IMG_MAX;
        }
        // pattern already loaded?
        if (lastOverlay == null) {
            // not loaded -> do it
            try {
                lastOverlay = ImageIO.read(getClass().getResource("/overlay/ov078.png"));
            } catch (IOException ex) {
                Logger.getLogger(ColorKuImage.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }

        Graphics2D g2 = createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(0, 0, sizeR, sizeR);
        double scale = sizeR / 78.0;
        g2.scale(scale, scale);
        g2.drawImage(lastOverlay, 0, 0, null);

        ticks = System.nanoTime() - ticks;
//        System.out.println("createImage(): " + (ticks / 1000000) + "ms");
    }

//    /**
//     * Get the overlay that fits best and load it into
//     * {@link #lastOverlay} if necessary. Then paint a
//     * filled circle using <code>color</code> centered
//     * on the image and apply the pattern from <code>lastOverlay</code>.<br><br>
//     * 
//     * Pixels, that are white in <code>lastOverlay</code>, become
//     * completely transparent in <code>this</code>.
//     */
//    private void createImage() {
//        // get the overlay that fits best
//        long ticks = System.nanoTime();
//        int sizeR = ((getWidth() - IMG_MIN) / IMG_FACTOR) * IMG_FACTOR + IMG_MIN;
//        if (sizeR < IMG_MIN) {
//            sizeR = IMG_MIN;
//        }
//        if (sizeR > IMG_MAX) {
//            sizeR = IMG_MAX;
//        }
//        // TODO
//        System.out.println("create colorku icon " + sizeR);
//        // pattern already loaded?
//        if (lastOverlay == null || (lastOverlay != null && lastOverlay.getWidth() != sizeR)) {
//            // not loaded -> do it
//            String res = String.format("%03d", sizeR);
//            try {
//                lastOverlay = ImageIO.read(getClass().getResource("/overlay/ov" + res + ".png"));
//            } catch (IOException ex) {
//                Logger.getLogger(ColorKuImage.class.getName()).log(Level.SEVERE, null, ex);
//                return;
//            }
//        }
//
//        //TODO
//        if (sizeR == 54 || sizeR == 78) {
//            Graphics2D g2 = createGraphics();
//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//            g2.setColor(color);
//            g2.fillOval(0, 0, sizeR, sizeR);
//            g2.drawImage(lastOverlay, 0, 0, null);
//            return;
//        }
//        // create overlay image in the right size
//        WritableRaster src = lastOverlay.getRaster();
//        int[] srcPixel = new int[4];
//
//        // get the color components
//        int[] rgb = new int[3];
//        rgb[0] = color.getRed();
//        rgb[1] = color.getGreen();
//        rgb[2] = color.getBlue();
//        
//        // where do we start with the new image?
//        int delta = (getWidth() - sizeR) / 2;
//
//        // now manipulate image
//        WritableRaster dest = getRaster();
//        int[] destPixel = new int[4];
//
//        // draw the image and apply pattern
//        for (int x = 0; x < lastOverlay.getWidth(); x++) {
//            for (int y = 0; y < lastOverlay.getHeight(); y++) {
//                if ((x + delta) >= dest.getWidth() || (y + delta) >= dest.getHeight() ||
//                        (x + delta) < 0 || (y + delta) < 0) {
//                    continue;
//                }
//                src.getPixel(x, y, srcPixel);
//                dest.getPixel(x + delta, y + delta, destPixel);
//                destPixel[0] = (int)(rgb[0] * (srcPixel[0] / 256.0));
//                destPixel[1] = (int)(rgb[1] * (srcPixel[1] / 256.0));
//                destPixel[2] = (int)(rgb[2] * (srcPixel[2] / 256.0));
//                destPixel[3] = srcPixel[3];
//                dest.setPixel(x + delta, y + delta, destPixel);
//            }
//        }
//        
//        // paint a frame
////        Graphics2D g2 = createGraphics();
////        g2.setColor(Color.WHITE);
////        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
//        
//        ticks = System.nanoTime() - ticks;
////        System.out.println("createImage(): " + (ticks / 1000000) + "ms");
//    }
    /**
     * Convenience method that returns a scaled instance of the
     * provided {@code BufferedImage}.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance,
     *    in pixels
     * @param targetHeight the desired height of the scaled instance,
     *    in pixels
     * @param hint one of the rendering hints that corresponds to
     *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step
     *    scaling technique that provides higher quality than the usual
     *    one-step technique (only useful in downscaling cases, where
     *    {@code targetWidth} or {@code targetHeight} is
     *    smaller than the original dimensions, and generally only when
     *    the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public BufferedImage getScaledInstance(BufferedImage img,
            int targetWidth,
            int targetHeight,
            Object hint,
            boolean higherQuality) {
        int type = (img.getTransparency() == Transparency.OPAQUE)
                ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color = color;
    }
}
