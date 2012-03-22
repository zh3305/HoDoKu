/*
 * Copyright (C) 2008-11  Bernhard Hobiger
 *
 * This file is part of HoDoKu.
 *
 * HoDoKu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoDoKu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
 */

package sudoku;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.math.BigInteger;
import java.util.List;

/**
 *
 * @author hobiwan
 */
public class SudokuUtil {
    /** A global PrinterJob; is here and not in {@link Options} because it needs a getter but should not be written to a configuration file. */
    private static PrinterJob printerJob;
    /** A global PageFormat; is here and not in {@link Options} because it needs a getter but should not be written to a configuration file. */
    private static PageFormat pageFormat;

    /**
     * Clears the list. To avoid memory leaks all steps in the list
     * are explicitly nullified.
     * @param steps
     */
    public static void clearStepListWithNullify(List<SolutionStep> steps) {
        if (steps != null) {
            for (int i = 0; i < steps.size(); i++) {
                steps.get(i).reset();
                steps.set(i, null);
            }
            steps.clear();
        }
    }

    /**
     * Clears the list. The steps are not nullfied, but the list items are.
     * @param steps
     */
    public static void clearStepList(List<SolutionStep> steps) {
        if (steps != null) {
            for (int i = 0; i < steps.size(); i++) {
                steps.set(i, null);
            }
            steps.clear();
        }
    }
    
    /**
     * Calculates n over k
     *
     * @param n
     * @param k
     * @return
     */
    public static int combinations(int n, int k) {
        if (n <= 167) {
            double fakN = 1;
            for (int i = 2; i <= n; i++) {
                fakN *= i;
            }
            double fakNMinusK = 1;
            for (int i = 2; i <= n - k; i++) {
                fakNMinusK *= i;
            }
            double fakK = 1;
            for (int i = 2; i <= k; i++) {
                fakK *= i;
            }
            return (int) (fakN / (fakNMinusK * fakK));
        } else {
            BigInteger fakN = BigInteger.ONE;
            for (int i = 2; i <= n; i++) {
                fakN = fakN.multiply(new BigInteger(i + ""));
            }
            BigInteger fakNMinusK = BigInteger.ONE;
            for (int i = 2; i <= n - k; i++) {
                fakNMinusK = fakNMinusK.multiply(new BigInteger(i + ""));
            }
            BigInteger fakK = BigInteger.ONE;
            for (int i = 2; i <= k; i++) {
                fakK = fakK.multiply(new BigInteger(i + ""));
            }
            fakNMinusK = fakNMinusK.multiply(fakK);
            fakN = fakN.divide(fakNMinusK);
            return fakN.intValue();
        }
    }

    /**
     * @return the printerJob
     */
    public static PrinterJob getPrinterJob() {
        if (printerJob == null) {
            printerJob = PrinterJob.getPrinterJob();
        }
        return printerJob;
    }

    /**
     * @return the pageFormat
     */
    public static PageFormat getPageFormat() {
        if (pageFormat == null) {
            pageFormat = getPrinterJob().defaultPage();
        }
        return pageFormat;
    }
    
    /**
     * HiRes printing in Java is difficult: The whole printing engine
     * (except apparently text printing) is scaled down to 72dpi. This is
     * done by applying an AffineTransform object with a scale to the Graphics2D
     * object of the printer. To make things more complicated just reversing 
     * the scale is not enough: for Landscape printing a rotation is applied
     * after the scale.<br><br>
     * 
     * The easiest way to really achieve hires printing is to directly manipulate
     * the transformation matrix. The default matrix looks like this:<br>
     * <pre>
     *      Portrait     Landscape
     *    [ d  0  x ]   [  0  d  x ]
     *    [ 0  d  y ]   [ -d  0  y ]
     *    [ 0  0  1 ]   [  0  0  1 ]
     * 
     *    d = printerResolution / 72.0
     * </pre>
     * x and y are set by the printer engine and should not be changed.<br><br>
     * 
     * The values from the {@link PageFormat} object are scaled down to 72dpi
     * as well and have to be multiplied with d to get the correct hires values.
     * 
     * @param g2
     * @return The scale factor
     */
    public static double adjustGraphicsForPrinting(Graphics2D g2) {
        AffineTransform at = g2.getTransform();
        double[] matrix = new double[6];
        at.getMatrix(matrix);
        //System.out.println("matrix: " + Arrays.toString(matrix));
        double scale = matrix[0];
        if (scale != 0) {
            // Portrait
            matrix[0] = 1;
            matrix[3] = 1;
        } else {
            // Landscape
            scale = matrix[2];
            matrix[1] = -1;
            matrix[2] = 1;
        }
//        int resolution = (int)(72.0 * scale);
        AffineTransform newAt = new AffineTransform(matrix);
        g2.setTransform(newAt);
        return scale;
    }
}
