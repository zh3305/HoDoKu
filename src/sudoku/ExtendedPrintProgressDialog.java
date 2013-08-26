/*
 * Copyright (C) 2008-12  Bernhard Hobiger
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

import generator.BackgroundGeneratorThread;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import solver.SudokuSolver;
import solver.SudokuSolverFactory;

/**
 * This class handles extended printing (printing with different layouts). It is constructed
 * and called from {@link ExtendedPrintDialog}, where all options have been collected.<br><br>
 * 
 * Five different layouts are available:
 * <ul>
 *   <li>0: One puzzle per page PORTRAIT</li>
 *   <li>1: Two puzzles per page PORTRAIT</li>
 *   <li>2: Four puzzles per page PORTRAIT</li>
 *   <li>3: Two puzzles per page LANDSCAPE</li>
 *   <li>4: Four puzzles per page LANDSCAPE</li>
 * </ul>
 * For layouts 3 and 4 booklet printing is available: The puzzles are printed in a way,
 * that the pages can be folded in the middle and form a booklet.<br><br>
 * 
 * The number and format of the sudokus is passed in in five sections, stored in the
 * synchronized array {@link #numberTextFields}, {@link #levelComboBoxes}, {@link #modeComboBoxes}
 * and {@link #candCheckBoxes}. To make printing easier, the sudokus are created first
 * and stored in the array {@link #sudokus}. The appropriate option from {@link #candCheckBoxes}
 * is stored in {@link #candidates}.
 * 
 * @author hobiwan
 */
public class ExtendedPrintProgressDialog extends javax.swing.JDialog implements Runnable, Printable {
    /** Standard factor for calculating gaps: half a cell (two sudokus means 18 cells plus the gap) */
    private static final double GAP_FACTOR = 1.0 / 37.0;
    /** Number of puzzles per page for every layout */
    private static final double[] PPP = new double[] {
       1.0, 2.0, 4.0, 2.0, 4.0 
    };
    private static final long serialVersionUID = 1L;
    /** The background thread, that is used to do the necessary calculations. */
    private Thread thread;
    /** The textfields with the number of puzzles for all sections */
    private JTextField[] numberTextFields;
    /** The levels of the puzzles for all sections */
    private JComboBox[] levelComboBoxes;
    /** The modes of the puzzles for all sections */
    private JComboBox[] modeComboBoxes;
    /** The candidate checkboxes for all sections */
    private JCheckBox[] candCheckBoxes;
    /** The layout: index into {@link ExtendedPrintDialog#layoutList}. */
    private int layout;
    /** Defines, whether the rating should be printed with the puzzles or not. */
    private boolean printRating;
    /** If set, all colors are replaced with black */
    private boolean allBlack;
    /** Print a booklet: extend the number of pages to a multiple of 2. */
    private boolean printBooklet;
    /** Print all the fronts of booklet first, then pause and print the other half. */
    private boolean manualDuplex;
    /** The current print job */
    private PrinterJob job = null;
    /** An array with the puzzles that have to be printed. Synchronized with {@link #candidates} */
    private Sudoku2[] sudokus;
    /** An array with "print candidates" options for every puzzle. Synchronized with {@link #sudokus}. */
    private boolean[] candidates;
    /** The percentage to be shown in the progress bar.Access has to be synchronized! */
    private int percentage;
    /** The number of pages for this print job. */
    private volatile int numberOfPages;
    /** The font for the rating */
    private Font smallFont;
    /** A {@link SudokuPanel} to do the actual rendering */
    private SudokuPanel panel;
    /** The size of one puzzle in pixel. */
    private int imagePrintSize;
    /** The gap between two puzzles in x */
    private int horizontalGap;
    /** The gap between two puzzles in y */
    private int verticalGap;
    /** the total width of the printable area (without insets) */
    private int printWidth;
    /** the total height of the printable area (without insets) */
    private int printHeight;
    /** The width of the area that contains one sudoku and optionally its rating. */
    private int borderWidth;
    /** The height of the area that contains one sudoku and optionally its rating. */
    private int borderHeight;
    /** The height of the footer that contains a rating */
    private int footerHeight;
    /** A flag to indicate whether all properties for the current job have been initalized. */
    private boolean initialized;
    /** First or second half of a booklet print job */
    private boolean firstHalf = true;
    /** Job aborted in a booklet print */
    private boolean jobAborted = false;

    /** Creates new form ExtendedPrintProgressDialog
     * @param parent
     * @param modal
     * @param numberTextFields
     * @param levelComboBoxes
     * @param modeComboBoxes
     * @param candCheckBoxes
     * @param layout
     * @param printRating
     * @param allBlack
     * @param printBooklet
     * @param manualDuplex  
     */
    public ExtendedPrintProgressDialog(java.awt.Frame parent, boolean modal,
            JTextField[] numberTextFields, JComboBox[] levelComboBoxes,
            JComboBox[] modeComboBoxes, JCheckBox[] candCheckBoxes,
            int layout, boolean printRating, boolean allBlack,
            boolean printBooklet, boolean manualDuplex) {
        super(parent, modal);
        initComponents();
        getRootPane().setDefaultButton(cancelButton);
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible( false );
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
        
        this.numberTextFields = numberTextFields;
        this.levelComboBoxes = levelComboBoxes;
        this.modeComboBoxes = modeComboBoxes;
        this.candCheckBoxes = candCheckBoxes;
        this.layout = layout;
        this.printRating = printRating;
        this.allBlack = allBlack;
        this.printBooklet = printBooklet;
        this.manualDuplex = manualDuplex;
        
        thread = new Thread(this);
        // start the thread only when window is shown (formWindowOpened)
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        printProgressBar = new javax.swing.JProgressBar();
        jPanel1 = new javax.swing.JPanel();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("intl/ExtendedPrintProgressDialog"); // NOI18N
        setTitle(bundle.getString("ExtendedPrintProgressDialog.title")); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        printProgressBar.setStringPainted(true);

        cancelButton.setMnemonic(java.util.ResourceBundle.getBundle("intl/ExtendedPrintProgressDialog").getString("ExtendedPrintProgressDialog.cancelButton.mnemonic").charAt(0));
        cancelButton.setText(bundle.getString("ExtendedPrintProgressDialog.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        jPanel1.add(cancelButton);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(printProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(printProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        // the printer thread only runs a loop when creating the necessary
        // puzzles. To interrupt at this stage, thread.interrupt() must
        // be used. When the actual printing has started, the print job
        // itself must be cancelled by calling job.cancel()
        thread.interrupt();
        job.cancel();
        try {
            thread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Interrupted while waiting for generation thread", ex);
        }
        setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        cancelButtonActionPerformed(null);
    }//GEN-LAST:event_formWindowClosing

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        // window is open -> start the Thread (important, because
        // some JRE implementations are so slow that the creation is complete before the
        // window has been drawn; the setVisible(false) will then be useless
        // and the window will not be closed
        thread.start();
    }//GEN-LAST:event_formWindowOpened

    /**
     * The background thread: creates the puzzles and prints them.
     * Can be interrupted at any time.
     */
    @Override
    public void run() {
        try {
            initialized = false;
            if (job != null) {
                // first create the necessary puzzles
                // method handles cancel requests
                if (createSudokus()) {
                    // all puzzles created, job not cancelled -> print it!

                    // determine the number of pages
                    if (layout < 3) {
                        // 1, 2 or 4 puzzles per page
                        numberOfPages = (int) Math.ceil(sudokus.length / PPP[layout]);
                    } else {
                        int adjusted = sudokus.length;
                        double factor = PPP[layout];
                        if (printBooklet) {
                            // number of puzzles must be adjusted to a multiple
                            // of 4 (layout 3) or 8 (layout 4)
                            adjusted = (int) (Math.ceil(sudokus.length / (factor * 2)) * factor * 2);
                        }
                        // 2 or 4 puzzles per page
                        numberOfPages = (int) Math.ceil(adjusted / factor);
                    }
//                    System.out.println("numberOfPages = " + numberOfPages);
                    // start the print job
                    try {
                        //job.setPrintable(this, pageFormat);
//                        System.out.println("2 job: " + job + "/ service: " + job.getPrintService());
                        firstHalf = true;
                        job.print();
                        if (! job.isCancelled() && printBooklet) {
                            // show the MessageDialog
                            setJobAborted(false);
                            if (manualDuplex) {
                                EventQueue.invokeLater(new Runnable() {

                                    @Override
                                    public void run() {
                                        showMessageDialog();
                                    }
                                });
                                synchronized (this) {
                                    wait();
                                }
                                if (!isJobAborted()) {
                                    firstHalf = false;
                                    job.print();
                                }
                            }
                        }
                    } catch (PrinterException ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error while printing", ex);
                    }
                }
            } else {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error: no PrinterJob set");
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error while printing", ex);
        }
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                setVisible(false);
            }
        });
    }
    
    /**
     * Creates all the necessary sudokus. Returns <code>false</code>
     * if interrupted.
     * 
     * @return 
     */
    private boolean createSudokus() {
        int anzPuzzles = 0;
        for (int i = 0; i < numberTextFields.length; i++) {
            anzPuzzles += getNumberOfPuzzles(i);
        }
        if (anzPuzzles == 0) {
            // nothing to do
            return false;
        }
        sudokus = new Sudoku2[anzPuzzles];
        candidates = new boolean[anzPuzzles];
        int index = 0;
        for (int i = 0; i < numberTextFields.length; i++) {
            DifficultyLevel actDiffLevel = Options.getInstance().getDifficultyLevel(levelComboBoxes[i].getSelectedIndex() + 1);
            GameMode actGameMode = null;
            boolean withCandidates = candCheckBoxes[i].isSelected();
            switch (modeComboBoxes[i].getSelectedIndex()) {
                case 0: actGameMode = GameMode.PLAYING; break;
                case 1: actGameMode = GameMode.LEARNING; break;
                case 2: actGameMode = GameMode.PRACTISING; break;
            }
            for (int j = 0; j < getNumberOfPuzzles(i); j++) {
                sudokus[index] = getSudoku(actDiffLevel, actGameMode);
                if (sudokus[index] == null || thread.isInterrupted()) {
                    return false;
                }
                candidates[index] = withCandidates;
                // put puzzle in history
                Options.getInstance().addSudokuToHistory(sudokus[index]);
                index++;
                // update progress bar
                setPercentage((int)Math.round(index * 100.0 / anzPuzzles));
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setProgress();
                    }
                });
            }
        }
        
        return true;
    }
    
    /**
     * Creates a new Sudoku with a given{@link DifficultyLevel} and a 
     * given {@link GameMode}. The puzzles are taken from the cache if
     * possible.<br>
     * If no puzzle could be generated (or the generation was aborted),
     * <code>null</code> is returned.
     * 
     * @param level
     * @param mode
     * @return 
     */
    private Sudoku2 getSudoku(DifficultyLevel level, GameMode mode) {
        if (mode == GameMode.LEARNING) {
            // in LEARNING ANY puzzle is accepted, that has at least one Training Step in it
            level = Options.getInstance().getDifficultyLevel(DifficultyType.EXTREME.ordinal());
        }
        String preGenSudoku = BackgroundGeneratorThread.getInstance().getSudoku(level, mode);
        Sudoku2 tmpSudoku = null;
        SudokuSolver solver = SudokuSolverFactory.getDefaultSolverInstance();
        if (preGenSudoku == null) {
            // no pregenerated puzzle available -> do it in GUI
            GenerateSudokuProgressDialog dlg = new GenerateSudokuProgressDialog(null, true, level, mode);
            dlg.setVisible(true);
            tmpSudoku = dlg.getSudoku();
        } else {
            tmpSudoku = new Sudoku2();
            tmpSudoku.setSudoku(preGenSudoku, true);
            Sudoku2 solvedSudoku = tmpSudoku.clone();
            solver.solve(level, solvedSudoku, true, null, false, 
                    Options.getInstance().solverSteps, Options.getInstance().getGameMode());
            tmpSudoku.setLevel(solvedSudoku.getLevel());
            tmpSudoku.setScore(solvedSudoku.getScore());
        }
        if (tmpSudoku == null) {
            // couldnt create anything or was aborted
            return null;
        }
        if (mode == GameMode.LEARNING) {
            // solve the sudoku up until the first trainingStep
            List<SolutionStep> steps = solver.getSteps();
            for (SolutionStep step : steps) {
                if (step.getType().getStepConfig().isEnabledTraining()) {
                    break;
                } else {
                    //System.out.println("doStep(): " + step.getType().getStepName());
                    solver.doStep(tmpSudoku, step);
                }
            }
        }
        return tmpSudoku;
    }

    /**
     * The actual printing is handled here. Note, that the method can be
     * called more than once for every page.
     * 
     * @param graphics
     * @param pageFormat
     * @param pageIndex
     * @return
     * @throws PrinterException 
     */
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex >= numberOfPages) {
            return Printable.NO_SUCH_PAGE;
        }
        if (printBooklet && manualDuplex && pageIndex >= (numberOfPages / 2)) {
            return Printable.NO_SUCH_PAGE;
        }
        
        setPercentage(-(pageIndex + 1));
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                setProgress();
            }
        });
        
        // CAUTION: The Graphics2D object is created with the native printer
        // resolution, but scaled down to 72dpi using an AffineTransform.
        // To print in high resolution this downscaling has to be reverted.
        Graphics2D printG2 = (Graphics2D) graphics;
        double scale = SudokuUtil.adjustGraphicsForPrinting(printG2);
        printG2.translate((int)(pageFormat.getImageableX() * scale), (int)(pageFormat.getImageableY() * scale));
        printWidth = (int) (pageFormat.getImageableWidth() * scale);
        printHeight = (int) (pageFormat.getImageableHeight() * scale);
//        printG2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        printG2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (! initialized) {
            // create a few global attributes for that job
            // scale fonts up too fit the printer resolution
            Font tmpFont = Options.getInstance().getSmallFont();
            smallFont = new Font(tmpFont.getName(), tmpFont.getStyle(), (int)(tmpFont.getSize() * scale));
            printG2.setFont(smallFont);
            // gaps between puzzles, depend on layout
            footerHeight = 0;
            if (printRating) {
                FontMetrics metrics = printG2.getFontMetrics();
                footerHeight = metrics.getHeight() * 2;
            }
            horizontalGap = 0;
            verticalGap = 0;
            borderWidth = printWidth;
            borderHeight = printHeight;
            if (!printBooklet) {
                // for layouts 1, 2 and 4 (no booklet) a vertical gap is needed
                if (layout == 1 || layout == 2 || layout == 4) {
                    // 2 puzzles vertically
                    verticalGap = (int) (borderHeight * GAP_FACTOR);
                    borderHeight = (borderHeight - verticalGap) / 2;
                }
                // for layouts 2, 3 and 4 (no booklet) a horizontal gap is needed
                if (layout == 2 || layout == 3 || layout == 4) {
                    // 2 puzzles horizontally
                    horizontalGap = (int) (borderWidth * GAP_FACTOR);
                    borderWidth = (borderWidth - horizontalGap) / 2;
                }
            } else {
                // if printing a booklet, layouts 3 and 4 need a horizontal gap, 
                // 4 needs a vertical gap
                // in a booklet the gap in the middle is the sum of
                // insets (left inset - puzzle - right inset - left inset - puzzle - right inset)
                if (layout == 3 || layout == 4) {
                    // should always be true...
                    horizontalGap = (int) ((pageFormat.getWidth() - pageFormat.getImageableWidth()) * scale);
                    borderWidth = (borderWidth - horizontalGap) / 2;
                }
                if (layout == 4) {
                    verticalGap = (int) ((pageFormat.getHeight() - pageFormat.getImageableHeight()) * scale);
                    // less vertical gap
                    verticalGap /= 2;
                    borderHeight = (borderHeight - verticalGap) / 2;
                }
            }
            // ok: we know the space available for our sudoku; now determine the
            // actual imageSize
            imagePrintSize = (borderWidth < (borderHeight - footerHeight)) ? borderWidth : (borderHeight - footerHeight);
            
            // Construct Sudoku2
            panel = new SudokuPanel(null);
            
            initialized = true;
        }

        // ok - draw the puzzles
        // first determine, which puzzles must be printed on the current page
        int leftStartIndex = 0;
        int rightStartIndex = 0;
        if (! printBooklet) {
            //int dummy = layout > 2 ? layout - 2 : layout;
            leftStartIndex = (int)Math.round(pageIndex * PPP[layout]);
            rightStartIndex = leftStartIndex + 1;
            if (PPP[layout] == 4) {
                rightStartIndex = leftStartIndex + 2;
            }
        } else {
            // number of puzzles per half page
            int dummy = layout - 2;
            // booklet contains numberOfPages * dummy * 2 puzzles;
            // the first puzzle on the right is in the middle
            int firstRightIndex = numberOfPages * dummy;
            int actPage = pageIndex;
            if (printBooklet && manualDuplex) {
                if (firstHalf) {
                    actPage = (numberOfPages - pageIndex * 2 - 1);
                } else {
                    actPage = (numberOfPages - (pageIndex + 1) * 2);
                }
            }
            leftStartIndex = firstRightIndex - (actPage + 1) * dummy;
            rightStartIndex = firstRightIndex + actPage * dummy;
        }
        // now print them
        // lets do every case on its own; bloats the code, but makes things easier
        switch (layout) {
            case 0:
                printSudoku(printG2, leftStartIndex, 0, scale);
                break;
            case 1:
                printSudoku(printG2, leftStartIndex, 0, scale);
                printSudoku(printG2, leftStartIndex + 1, 2, scale);
                break;
            case 2:
                printSudoku(printG2, leftStartIndex, 0, scale);
                printSudoku(printG2, leftStartIndex + 1, 1, scale);
                printSudoku(printG2, leftStartIndex + 2, 2, scale);
                printSudoku(printG2, leftStartIndex + 3, 3, scale);
                break;
            case 3:
                if (!printBooklet) {
                    printSudoku(printG2, leftStartIndex, 0, scale);
                    printSudoku(printG2, rightStartIndex, 1, scale);
                } else if ((manualDuplex && ! firstHalf) || (! manualDuplex && (pageIndex % 2) == 1)) {
                    printSudoku(printG2, leftStartIndex, 1, scale);
                    printSudoku(printG2, rightStartIndex, 0, scale);
                } else {
                    printSudoku(printG2, leftStartIndex, 0, scale);
                    printSudoku(printG2, rightStartIndex, 1, scale);
                }
                break;
            case 4:
                if (! printBooklet) {
                    printSudoku(printG2, leftStartIndex, 0, scale);
                    printSudoku(printG2, leftStartIndex + 1, 2, scale);
                    printSudoku(printG2, rightStartIndex, 1, scale);
                    printSudoku(printG2, rightStartIndex + 1, 3, scale);
                } else if ((manualDuplex && ! firstHalf) || (! manualDuplex && (pageIndex % 2) == 1)) {
                    printSudoku(printG2, leftStartIndex, 1, scale);
                    printSudoku(printG2, leftStartIndex + 1, 3, scale);
                    printSudoku(printG2, rightStartIndex, 0, scale);
                    printSudoku(printG2, rightStartIndex + 1, 2, scale);
                } else {
                    printSudoku(printG2, leftStartIndex, 0, scale);
                    printSudoku(printG2, leftStartIndex + 1, 2, scale);
                    printSudoku(printG2, rightStartIndex, 1, scale);
                    printSudoku(printG2, rightStartIndex + 1, 3, scale);
                }
                break;
        }
        return Printable.PAGE_EXISTS;
    }
    
    /**
     * Prints <code>sudoku</code> on the graphics context <code>g2</code>.
     * <code>actTransform</code> describes the printable area, <code>position</code>
     * determines the placement of the sudoku (starting with 0): top left -
     * top right - bottom left - bottom right.<br><br>
     * 
     * Additional info is taken from {@link #printWidth}, {@link #printHeight},
     * {@link #borderWidth}, {@link #borderHeight}, {@link #footerHeight},
     * {@link #imagePrintSize}, {@link #horizontalGap} and {@link #verticalGap}.
     * 
     * @param g2
     * @param index
     * @param position
     * @param scale
     */
    private void printSudoku(Graphics2D g2, int index, int position, double scale) {
        if (index >= sudokus.length || index < 0) {
            // nothing to do
            return;
        }
        // calculate the position of the puzzle
        int startX = 0;
        int startY = 0;
        switch (position) {
            case 0:
                // top left
                startX = (borderWidth - imagePrintSize) / 2;
                startY = (borderHeight - imagePrintSize - footerHeight) / 2;
                break;
            case 1:
                // top right
                startX = borderWidth + horizontalGap + (borderWidth - imagePrintSize) / 2;
                startY = (borderHeight - imagePrintSize - footerHeight) / 2;
                break;
            case 2:
                // bottom left
                startX = (borderWidth - imagePrintSize) / 2;
                startY = borderHeight + verticalGap + (borderHeight - imagePrintSize - footerHeight) / 2;
                break;
            case 3:
                // bottom right
                startX = borderWidth + horizontalGap + (borderWidth - imagePrintSize) / 2;
                startY = borderHeight + verticalGap + (borderHeight - imagePrintSize - footerHeight) / 2;
                break;
        }
        Sudoku2 sudoku = sudokus[index];
        panel.setSudoku(sudoku.getSudoku(ClipboardMode.LIBRARY), true);
        panel.setShowCandidates(candidates[index]);
        panel.printSudoku(g2, startX, startY, imagePrintSize, allBlack, scale);
        if (printRating && sudoku != null && sudoku.getLevel() != null) {
            String title = sudoku.getLevel().getName() + " (" + sudoku.getScore() + ")";
            g2.setFont(smallFont);
            FontMetrics metrics = g2.getFontMetrics();
            int textWidth = metrics.stringWidth(title);
            int textHeight = metrics.getHeight();
            g2.setColor(Color.BLACK);
            g2.drawString(title, startX + imagePrintSize / 2 - textWidth / 2, (int)(startY + imagePrintSize + textHeight * 1.5));
        }
    }
    
    /**
     * Utility method: reads the contents of the textfield
     * {@link #numberTextFields}[<code>index</code>] and returns
     * it as integer. If the field is empty or an error occurs,
     * <code>0</code> is returned.
     * #
     * @param index
     * @return 
     */
    private int getNumberOfPuzzles(int index) {
        int ret = 0;
        try {
            ret = Integer.parseInt(numberTextFields[index].getText());
        } catch (NumberFormatException ex) {
            // simply assume 0 has been entered into the textfield
            ret = 0;
        }
        return ret;
    }

    /**
     * Updates the progress bar. {@link #percentage}
     * MUST be accessed via the getter to provide
     * synchronization. {@link #numberOfPages} is only read
     * and never updated during the process,so synchronization is not 
     * necessary.
     */
    private void setProgress() {
        int value = getPercentage();
        if (value >= 0) {
            setTitle(java.util.ResourceBundle.getBundle("intl/ExtendedPrintProgressDialog").getString("ExtendedPrintProgressDialog.title"));
            printProgressBar.setValue(value);
            printProgressBar.setString(value + " %");
        } else {
            value = -value;
            setTitle(java.util.ResourceBundle.getBundle("intl/ExtendedPrintProgressDialog").getString("ExtendedPrintProgressDialog.title2"));
            printProgressBar.setValue((int)(value * 100.0 / numberOfPages));
            printProgressBar.setString(value + " / " + numberOfPages);
        }
    }
    
    /**
     * Shows a message dialog to ask the user to turn the paper. Is used, when
     * in a booklet print {@link #manualDuplex} is selected.<br><br>
     * 
     * The second half of the print job can be aborted by the user.
     * 
     */
    private void showMessageDialog() {
        int ret = JOptionPane.showConfirmDialog(null, 
                java.util.ResourceBundle.getBundle("intl/ExtendedPrintProgressDialog").getString("ExtendedPrintProgressDialog.flipPaper"),
                java.util.ResourceBundle.getBundle("intl/ExtendedPrintProgressDialog").getString("ExtendedPrintProgressDialog.flipPaperTitle"),
                JOptionPane.OK_CANCEL_OPTION);
        if (ret == JOptionPane.OK_OPTION) {
            setJobAborted(false);
        } else {
            setJobAborted(true);
        }
        synchronized (this) {
            notify();
        }
    }
    
    /**
     * Set {@link #percentage}.
     * @param p 
     */
    private synchronized void setPercentage(int p) {
        percentage = p;
    }
    
    /**
     * Retrieve {@link #percentage}.
     * @return 
     */
    private synchronized int getPercentage() {
        return percentage;
    }
    
    /**
     * @param job the job to set
     */
    public void setJob(PrinterJob job) {
        this.job = job;
    }

    /**
     * Set {@link #jobAborted}.
     * @param p 
     */
    private synchronized void setJobAborted(boolean ja) {
        jobAborted = ja;
    }
    
    /**
     * Retrieve {@link #jobAborted}.
     * @return 
     */
    private synchronized boolean isJobAborted() {
        return jobAborted;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ExtendedPrintProgressDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ExtendedPrintProgressDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ExtendedPrintProgressDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ExtendedPrintProgressDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                ExtendedPrintProgressDialog dialog = new ExtendedPrintProgressDialog(new javax.swing.JFrame(), true,
                        null, null, null, null, 1, false, false, false, false);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JProgressBar printProgressBar;
    // End of variables declaration//GEN-END:variables
}
