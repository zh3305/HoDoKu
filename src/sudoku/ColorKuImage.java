/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sudoku;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
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
        if (lastOverlay == null || (lastOverlay != null && lastOverlay.getWidth() != sizeR)) {
            // not loaded -> do it
            String res = String.format("%03d", sizeR);
            try {
                lastOverlay = ImageIO.read(getClass().getResource("/overlay/ov" + res + ".png"));
            } catch (IOException ex) {
                Logger.getLogger(ColorKuImage.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }

        // create overlay image in the right size
        WritableRaster src = lastOverlay.getRaster();
        int[] srcPixel = new int[4];

        // get the color components
        int[] rgb = new int[3];
        rgb[0] = color.getRed();
        rgb[1] = color.getGreen();
        rgb[2] = color.getBlue();
        
        // where do we start with the new image?
        int delta = (getWidth() - sizeR) / 2;

        // now manipulate image
        WritableRaster dest = getRaster();
        int[] destPixel = new int[4];

        // draw the image and apply pattern
        for (int x = 0; x < lastOverlay.getWidth(); x++) {
            for (int y = 0; y < lastOverlay.getHeight(); y++) {
                if ((x + delta) >= dest.getWidth() || (y + delta) >= dest.getHeight() ||
                        (x + delta) < 0 || (y + delta) < 0) {
                    continue;
                }
                src.getPixel(x, y, srcPixel);
                dest.getPixel(x + delta, y + delta, destPixel);
                destPixel[0] = (int)(rgb[0] * (srcPixel[0] / 256.0));
                destPixel[1] = (int)(rgb[1] * (srcPixel[1] / 256.0));
                destPixel[2] = (int)(rgb[2] * (srcPixel[2] / 256.0));
                destPixel[3] = srcPixel[3];
                dest.setPixel(x + delta, y + delta, destPixel);
            }
        }
        
        // paint a frame
//        Graphics2D g2 = createGraphics();
//        g2.setColor(Color.WHITE);
//        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        
        ticks = System.nanoTime() - ticks;
//        System.out.println("createImage(): " + (ticks / 1000000) + "ms");
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
