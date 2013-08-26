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

import generator.SudokuGenerator;
import generator.SudokuGeneratorFactory;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import org.w3c.dom.Node;
import solver.SudokuSolver;
import solver.SudokuSolverFactory;
import solver.SudokuStepFinder;

/**
 * A specialized JPanel for displaying and manipulating Sudokus.<br>
 *
 * Mouse click detection:<br><br>
 *
 * AWT seems to have problems with mouse click detection: if the mouse moves a
 * tiny little bit between PRESSED and RELEASED, no CLICKED event is produced.
 * This means, that when playing HoDoKu with the mouse fast, the program often
 * seems to ignore the mouse.<br><br>
 *
 * The solution is simple: Catch the PRESSED and RELEASED events and decide for
 * yourself, if a CLICKED has happened. For HoDoKu a CLICKED event is generated,
 * if PRESSED and RELEASED occured on the same candidate.
 *
 * @author hobiwan
 */
public class SudokuPanel extends javax.swing.JPanel implements Printable {

    private static final long serialVersionUID = 1L;
    // Konstante
    /**
     * Translation of KeyChars in KeyCodes for laptops that use special key
     * combinations for number
     */
    private static final int[] KEY_CODES = new int[]{
        KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
        KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9
    };
    private static final int DELTA = 5; // Abstand zwischen den Quadraten in Pixel
    private static final int DELTA_RAND = 5; // Abstand zu den Rändern
    private static BufferedImage[] colorKuImagesSmall = new BufferedImage[Sudoku2.UNITS + 2];
    private static BufferedImage[] colorKuImagesLarge = new BufferedImage[Sudoku2.UNITS];
    // Konfigurationseigenschaften
    private boolean showCandidates = Options.getInstance().isShowCandidates(); // Alle möglichen Kandidaten anzeigen
    private boolean showWrongValues = Options.getInstance().isShowWrongValues();    // falsche Werte mit anderer Farbe
    private boolean showDeviations = Options.getInstance().isShowDeviations();  // Werte und Kandidaten, die von der Lösung abweichen
    private boolean invalidCells = Options.getInstance().isInvalidCells(); // true: ungültige Zellen, false: mögliche Zellen
    private boolean showInvalidOrPossibleCells = false;  // Ungültige/Mögliche Zellen für showHintCellValue mit anderem Hintergrund
    /**
     * An array for every candidate for which a filter is set; index 10 stands
     * for "filter bivalue cells"
     */
    private boolean[] showHintCellValues = new boolean[11];
    //private int showHintCellValue = 0;
    private boolean showAllCandidatesAkt = false; // bei alle Kandidaten anzeigen (nur aktive Zelle)
    private boolean showAllCandidates = false; // bei alle Kandidaten anzeigen (alle Zellen)
    private int delta = DELTA; // Zwischenraum zwischen Blöcken
    private int deltaRand = DELTA_RAND; // Zwischenraum zu den Rändern
    private Font valueFont;    // Font für die Zellenwerte
    private Font candidateFont; // Font für die Kandidaten
    /**
     * Height of a digit in the current {@link AboutDialog#candidateFont}
     */
    private int candidateHeight;
    /**
     * Font used for printing: Since it has to be scaled according to the
     * printer resolution, {@link Options#bigFont} cannot be used directly.
     */
    private Font bigFont;
    /**
     * Font used for printing: Since it has to be scaled according to the
     * printer resolution, {@link Options#smallFont} cannot be used directly.
     */
    private Font smallFont;
    // interne Variable
    private Sudoku2 sudoku; // Daten für das Sudoku
    private SudokuSolver solver; // Lösung für das Sudoku
    private SudokuGenerator generator; // Lösung mit BruteForce (Dancing Links)
    private MainFrame mainFrame; // für Oberfläche
    private CellZoomPanel cellZoomPanel; // active cell display and color chooser
    private SolutionStep step;   // für Anzeige der Hinweise
    private int chainIndex = -1; // if != -1, only the chain with the right index is shown
    private List<Integer> alsToShow = new ArrayList<Integer>(); // if chainIndex is != -1, alsToShow contains the indices of the ALS, that are part of the chain
    private int oldWidth; // Breite des Panels, als das letzte Mal Fonts erzeugt wurden
    private int width;    // Breite des Panels, auf Quadrat normiert
    private int height;   // Höhe des Panels, auf Quadrat normiert
    private int cellSize; // Kantenlänge einer Zelle
    private int startSX;  // x-Koordinate des linken oberen Punktes des Sudoku
    private int startSY;  // y-Koordinate des linken oberen Punktes des Sudoku
    private Graphics2D g2; // zum Zeichnen, spart eine Menge Parameter
    private CubicCurve2D.Double cubicCurve = new CubicCurve2D.Double(); // für Chain-Pfeile
    private Polygon arrow = new Polygon(); // Pfeilspitze
    private Stroke arrowStroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); // Pfeilspitzen abrunden
    private Stroke strongLinkStroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); // strong links durchziehen
    private Stroke weakLinkStroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            10.0f, new float[]{5.0f}, 0.0f); // weak links punkten
    private List<Point2D.Double> points = new ArrayList<Point2D.Double>(200);
    private double arrowLengthFactor = 1.0 / 6.0;
    private double arrowHeightFactor = 1.0 / 3.0;
    private int aktLine = 4; // aktuell markiertes Feld (Zeile)
    private int aktCol = 4;  // aktuell markiertes Feld (Spalte)
    private int shiftLine = -1; // second cell for creating regions with the keyboard (shift pressed)
    private int shiftCol = -1; // second cell for creating regions with the keyboard (shift pressed)
    // Undo/Redo
    private Stack<Sudoku2> undoStack = new Stack<Sudoku2>();
    private Stack<Sudoku2> redoStack = new Stack<Sudoku2>();
    // coloring: contains cell index + index in coloringColors[]
    private SortedMap<Integer, Integer> coloringMap = new TreeMap<Integer, Integer>();
    // coloring canddiates: contains cell index * 10 + candidate + index in coloringColors[]
    private SortedMap<Integer, Integer> coloringCandidateMap = new TreeMap<Integer, Integer>();
    // indicates wether coloring is active (-1 means "not active"
    private int aktColorIndex = -1;
    // coloring is meant for cells or candidates
    private boolean colorCells = Options.getInstance().isColorCells();
    // Cursor for coloring: shows the strong color
    private Cursor colorCursor = null;
    // Cursor for coloring: shows the weak color
    private Cursor colorCursorShift = null;
    // old cursor for reset
    private Cursor oldCursor = null;
    // if more than one cell is selected, the indices of all selected cells are stored here
    private SortedSet<Integer> selectedCells = new TreeSet<Integer>();
    // Array containing all "Make x" menu items from the popup menu
    private JMenuItem[] makeItems = null;
    // Array containing all "Exclude x" menu items from the popup menu
    private JMenuItem[] excludeItems = null;
    // Array containing all "Toggle color x" menu items from the popup menu
    private JMenuItem[] toggleColorItems = null;
    /**
     * for background progress checks
     */
    private ProgressChecker progressChecker = null;
    /**
     * A timer for deleting the cursor marker after one second
     */
    private Timer deleteCursorTimer = new Timer(Options.getInstance().getDeleteCursorDisplayLength(), null);
    /**
     * The time of the last cursor movement
     */
    private long lastCursorChanged = -1;
    // mouse handling
    /**
     * The line in which the last mouse PRESSED occured
     */
    private int lastPressedLine = -1;
    /**
     * The column in which the last mouse PRESSED occured
     */
    private int lastPressedCol = -1;
    /**
     * The candidate for which the last mouse PRESSED occured
     */
    private int lastPressedCand = -1;
    /**
     * The line in which the last mouse CLICKED occured
     */
    private int lastClickedLine = -1;
    /**
     * The column in which the last mouse CLICKED occured
     */
    private int lastClickedCol = -1;
    /**
     * The candidate for which the last mouse CLICKED occured
     */
    private int lastClickedCand = -1;
    /**
     * The time in TICKS of the last CLICKED event
     */
    private long lastClickedTime = 0;
    /**
     * The AWT double click speed (depends on system settings)
     */
    private long doubleClickSpeed = -1;
    /**
     * <
     * code>true</code> for every candidate that can still be filtered
     */
    private boolean[] remainingCandidates = new boolean[Sudoku2.UNITS];

    /**
     * Creates new form SudokuPanel
     *
     * @param mf
     */
    public SudokuPanel(MainFrame mf) {
        mainFrame = mf;
        sudoku = new Sudoku2();
        sudoku.clearSudoku();
        setShowCandidates(Options.getInstance().isShowCandidates());
        generator = SudokuGeneratorFactory.getDefaultGeneratorInstance();
        solver = SudokuSolverFactory.getDefaultSolverInstance();
        solver.setSudoku(sudoku.clone());
        solver.solve();
        progressChecker = new ProgressChecker(mainFrame);

        initComponents();

        makeItems = new JMenuItem[]{
            make1MenuItem, make2MenuItem, make3MenuItem, make4MenuItem, make5MenuItem,
            make6MenuItem, make7MenuItem, make8MenuItem, make9MenuItem
        };
        excludeItems = new JMenuItem[]{
            exclude1MenuItem, exclude2MenuItem, exclude3MenuItem,
            exclude4MenuItem, exclude5MenuItem, exclude6MenuItem,
            exclude7MenuItem, exclude8MenuItem, exclude9MenuItem
        };
        toggleColorItems = new JMenuItem[]{
            color1aMenuItem, color1bMenuItem, color2aMenuItem, color2bMenuItem,
            color3aMenuItem, color3bMenuItem, color4aMenuItem, color4bMenuItem,
            color5aMenuItem, color5bMenuItem
        };
        setColorIconsInPopupMenu();
        updateCellZoomPanel();

        deleteCursorTimer.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                deleteCursorTimer.stop();
                // make sure the cursor is disabled when this event fires
                lastCursorChanged = System.currentTimeMillis() - Options.getInstance().getDeleteCursorDisplayLength() - 100;
//                System.out.println("Timer stopped");
                repaint();
            }
        });

        Object cs = Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
        if (cs instanceof Integer) {
            doubleClickSpeed = ((Integer) cs).intValue();
        }
        if (doubleClickSpeed == -1) {
            doubleClickSpeed = 500;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cellPopupMenu = new javax.swing.JPopupMenu();
        make1MenuItem = new javax.swing.JMenuItem();
        make2MenuItem = new javax.swing.JMenuItem();
        make3MenuItem = new javax.swing.JMenuItem();
        make4MenuItem = new javax.swing.JMenuItem();
        make5MenuItem = new javax.swing.JMenuItem();
        make6MenuItem = new javax.swing.JMenuItem();
        make7MenuItem = new javax.swing.JMenuItem();
        make8MenuItem = new javax.swing.JMenuItem();
        make9MenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        exclude1MenuItem = new javax.swing.JMenuItem();
        exclude2MenuItem = new javax.swing.JMenuItem();
        exclude3MenuItem = new javax.swing.JMenuItem();
        exclude4MenuItem = new javax.swing.JMenuItem();
        exclude5MenuItem = new javax.swing.JMenuItem();
        exclude6MenuItem = new javax.swing.JMenuItem();
        exclude7MenuItem = new javax.swing.JMenuItem();
        exclude8MenuItem = new javax.swing.JMenuItem();
        exclude9MenuItem = new javax.swing.JMenuItem();
        excludeSeveralMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        color1aMenuItem = new javax.swing.JMenuItem();
        color1bMenuItem = new javax.swing.JMenuItem();
        color2aMenuItem = new javax.swing.JMenuItem();
        color2bMenuItem = new javax.swing.JMenuItem();
        color3aMenuItem = new javax.swing.JMenuItem();
        color3bMenuItem = new javax.swing.JMenuItem();
        color4aMenuItem = new javax.swing.JMenuItem();
        color4bMenuItem = new javax.swing.JMenuItem();
        color5aMenuItem = new javax.swing.JMenuItem();
        color5bMenuItem = new javax.swing.JMenuItem();
        deleteValuePopupMenu = new javax.swing.JPopupMenu();
        deleteValueMenuItem = new javax.swing.JMenuItem();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("intl/SudokuPanel"); // NOI18N
        make1MenuItem.setText(bundle.getString("SudokuPanel.popup.make1")); // NOI18N
        make1MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                make1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(make1MenuItem);

        make2MenuItem.setText(bundle.getString("SudokuPanel.popup.make2")); // NOI18N
        make2MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                make1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(make2MenuItem);

        make3MenuItem.setText(bundle.getString("SudokuPanel.popup.make3")); // NOI18N
        make3MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                make1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(make3MenuItem);

        make4MenuItem.setText(bundle.getString("SudokuPanel.popup.make4")); // NOI18N
        make4MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                make1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(make4MenuItem);

        make5MenuItem.setText(bundle.getString("SudokuPanel.popup.make5")); // NOI18N
        make5MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                make1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(make5MenuItem);

        make6MenuItem.setText(bundle.getString("SudokuPanel.popup.make6")); // NOI18N
        make6MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                make1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(make6MenuItem);

        make7MenuItem.setText(bundle.getString("SudokuPanel.popup.make7")); // NOI18N
        make7MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                make1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(make7MenuItem);

        make8MenuItem.setText(bundle.getString("SudokuPanel.popup.make8")); // NOI18N
        make8MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                make1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(make8MenuItem);

        make9MenuItem.setText(bundle.getString("SudokuPanel.popup.make9")); // NOI18N
        make9MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                make1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(make9MenuItem);
        cellPopupMenu.add(jSeparator1);

        exclude1MenuItem.setText(bundle.getString("SudokuPanel.popup.exclude1")); // NOI18N
        exclude1MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exclude1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(exclude1MenuItem);

        exclude2MenuItem.setText(bundle.getString("SudokuPanel.popup.exclude2")); // NOI18N
        exclude2MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exclude1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(exclude2MenuItem);

        exclude3MenuItem.setText(bundle.getString("SudokuPanel.popup.exclude3")); // NOI18N
        exclude3MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exclude1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(exclude3MenuItem);

        exclude4MenuItem.setText(bundle.getString("SudokuPanel.popup.exclude4")); // NOI18N
        exclude4MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exclude1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(exclude4MenuItem);

        exclude5MenuItem.setText(bundle.getString("SudokuPanel.popup.exclude5")); // NOI18N
        exclude5MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exclude1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(exclude5MenuItem);

        exclude6MenuItem.setText(bundle.getString("SudokuPanel.popup.exclude6")); // NOI18N
        exclude6MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exclude1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(exclude6MenuItem);

        exclude7MenuItem.setText(bundle.getString("SudokuPanel.popup.exclude7")); // NOI18N
        exclude7MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exclude1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(exclude7MenuItem);

        exclude8MenuItem.setText(bundle.getString("SudokuPanel.popup.exclude8")); // NOI18N
        exclude8MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exclude1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(exclude8MenuItem);

        exclude9MenuItem.setText(bundle.getString("SudokuPanel.popup.exclude9")); // NOI18N
        exclude9MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exclude1MenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(exclude9MenuItem);

        excludeSeveralMenuItem.setText(bundle.getString("SudokuPanel.popup.several")); // NOI18N
        excludeSeveralMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                excludeSeveralMenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(excludeSeveralMenuItem);
        cellPopupMenu.add(jSeparator2);

        color1aMenuItem.setText(bundle.getString("SudokuPanel.popup.color1a")); // NOI18N
        color1aMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                color1aMenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(color1aMenuItem);

        color1bMenuItem.setText(bundle.getString("SudokuPanel.popup.color1b")); // NOI18N
        color1bMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                color1aMenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(color1bMenuItem);

        color2aMenuItem.setText(bundle.getString("SudokuPanel.popup.color2a")); // NOI18N
        color2aMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                color1aMenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(color2aMenuItem);

        color2bMenuItem.setText(bundle.getString("SudokuPanel.popup.color2b")); // NOI18N
        color2bMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                color1aMenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(color2bMenuItem);

        color3aMenuItem.setText(bundle.getString("SudokuPanel.popup.color3a")); // NOI18N
        color3aMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                color1aMenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(color3aMenuItem);

        color3bMenuItem.setText(bundle.getString("SudokuPanel.popup.color3b")); // NOI18N
        color3bMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                color1aMenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(color3bMenuItem);

        color4aMenuItem.setText(bundle.getString("SudokuPanel.popup.color4a")); // NOI18N
        color4aMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                color1aMenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(color4aMenuItem);

        color4bMenuItem.setText(bundle.getString("SudokuPanel.popup.color4b")); // NOI18N
        color4bMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                color1aMenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(color4bMenuItem);

        color5aMenuItem.setText(bundle.getString("SudokuPanel.popup.color5a")); // NOI18N
        color5aMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                color1aMenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(color5aMenuItem);

        color5bMenuItem.setText(bundle.getString("SudokuPanel.popup.color5b")); // NOI18N
        color5bMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                color1aMenuItemActionPerformed(evt);
            }
        });
        cellPopupMenu.add(color5bMenuItem);

        deleteValueMenuItem.setText(bundle.getString("SudokuPanel.deleteValueItem.text")); // NOI18N
        deleteValueMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteValueMenuItemActionPerformed(evt);
            }
        });
        deleteValuePopupMenu.add(deleteValueMenuItem);

        setBackground(new java.awt.Color(255, 255, 255));
        setMinimumSize(new java.awt.Dimension(300, 300));
        setPreferredSize(new java.awt.Dimension(600, 600));
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                formMouseReleased(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                formMousePressed(evt);
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                formKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 600, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 600, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyReleased
        handleKeysReleased(evt);
        updateCellZoomPanel();
        mainFrame.fixFocus();
    }//GEN-LAST:event_formKeyReleased

    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
        int keyCode = evt.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_ESCAPE:
                mainFrame.coloringPanelClicked(-1);
                clearRegion();
                if (step != null) {
                    mainFrame.abortStep();
                }
                break;
            default:
                handleKeys(evt);
        }
        updateCellZoomPanel();
        mainFrame.fixFocus();
    }//GEN-LAST:event_formKeyPressed

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
//        handleMouseClicked(evt, evt.getClickCount() == 2);
    }//GEN-LAST:event_formMouseClicked

    private void make1MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_make1MenuItemActionPerformed
        popupSetCell((JMenuItem) evt.getSource());
    }//GEN-LAST:event_make1MenuItemActionPerformed

    private void exclude1MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exclude1MenuItemActionPerformed
        popupExcludeCandidate((JMenuItem) evt.getSource());
    }//GEN-LAST:event_exclude1MenuItemActionPerformed

    private void excludeSeveralMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_excludeSeveralMenuItemActionPerformed
        String input = JOptionPane.showInputDialog(this, ResourceBundle.getBundle("intl/SudokuPanel").getString("SudokuPanel.cmessage"),
                ResourceBundle.getBundle("intl/SudokuPanel").getString("SudokuPanel.ctitle"), JOptionPane.QUESTION_MESSAGE);
        if (input != null) {
            undoStack.push(sudoku.clone());
            boolean changed = false;
            for (int i = 0; i < input.length(); i++) {
                char digit = input.charAt(i);
                if (Character.isDigit(digit)) {
                    if (removeCandidateFromActiveCells(Character.getNumericValue(digit))) {
                        changed = true;
                    }
                }
            }
            if (changed) {
                redoStack.clear();
                checkProgress();
            } else {
                undoStack.pop();
            }
            updateCellZoomPanel();
            mainFrame.check();
            repaint();
        }
    }//GEN-LAST:event_excludeSeveralMenuItemActionPerformed

    private void color1aMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_color1aMenuItemActionPerformed
        popupToggleColor((JMenuItem) evt.getSource());
    }//GEN-LAST:event_color1aMenuItemActionPerformed

    private void formMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMousePressed
        lastPressedLine = getLine(evt.getPoint());
        lastPressedCol = getCol(evt.getPoint());
        lastPressedCand = getCandidate(evt.getPoint(), lastPressedLine, lastPressedCol);
    }//GEN-LAST:event_formMousePressed

    private void formMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseReleased
        int line = getLine(evt.getPoint());
        int col = getCol(evt.getPoint());
        int cand = getCandidate(evt.getPoint(), line, col);
        if (line == lastPressedLine && col == lastPressedCol && cand == lastPressedCand) {
            // RELEASED on same candidate as PRESSED -> this is a click
            // is it a double click?
            long ticks = System.currentTimeMillis();
            if (lastClickedTime != -1 && (ticks - lastClickedTime) <= doubleClickSpeed && line == lastClickedLine
                    && col == lastClickedCol && cand == lastClickedCand) {
                // this is a double click
                handleMouseClicked(evt, true);
                lastClickedTime = -1;
            } else {
                // normal click
                handleMouseClicked(evt, false);
                lastClickedTime = ticks;
            }
            lastClickedLine = line;
            lastClickedCol = col;
            lastClickedCand = cand;
        }
        lastPressedLine = -1;
        lastPressedCol = -1;
        lastPressedCand = -1;
    }//GEN-LAST:event_formMouseReleased

    private void deleteValueMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteValueMenuItemActionPerformed
        popupDeleteValueFromCell();
    }//GEN-LAST:event_deleteValueMenuItemActionPerformed

    /**
     * New mouse control for version 2.0: <ul> <li>clicking a cell sets the
     * cursor to the cell (not in coloring mode)</li> <li>holding shift or ctrl
     * down while clicking selects a region of cells</li> <li>double clicking a
     * cell with only one candidate left sets that candidate in the cell</li>
     * <li>double clicking a cell containing a Hidden Single sets that cell if
     * filters are applied for the candidate</li> <li>double clicking a
     * candidate with ctrl pressed toggles the candidate</li> <li>right click on
     * a cell activates the context menu</li> </ul> If {@link #aktColorIndex} is
     * set (ne -1), coloring mode is in effect and the mouse behaviour changes
     * completely (whether a cell or a candidate should be colored is decided by {@link #colorCells}):
     * <ul> <li>left click on a cell/candidate toggles the color on the
     * cell/candidate</li> <li>left click on a cell/candidate with shift pressed
     * toggles the alternate color on the cell/candidate</li> </ul> Context
     * menu:<br> The context menu for a single cell shows entries to set the
     * cell to all remaining candidates, entries to remove all remaining
     * candidates (including one entry to remove multiple candidates in one
     * move) and entries for coloring.<br> <b>Alternative Mouse Mode:</b><br>
     * Since v2.1a new alternative mouse mode is available: Left click on a
     * candidate sets the candidate in the cell(s), right click toggles the
     * candidate in the cell(s). Selection works as before.
     *
     * @param evt
     */
    private void handleMouseClicked(MouseEvent evt, boolean doubleClick) {
        // undo/Redo siehe handleKeys()
        undoStack.push(sudoku.clone());
        boolean changed = false;
        //undoStack.push(sudoku.clone()); - nach unten geschoben

        int line = getLine(evt.getPoint());
        int col = getCol(evt.getPoint());
        int cand = getCandidate(evt.getPoint(), line, col);
//        System.out.println("Mouse pressed: " + evt.getButton() + "/" + evt.getModifiersEx() + "/" + evt.getClickCount());
//        System.out.println("    line/col/cand " + line + "/" + col + "/" + cand);
        boolean ctrlPressed = (evt.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0;
        boolean shiftPressed = (evt.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
        if (line >= 0 && line <= 8 && col >= 0 && col <= 8) {
            //System.out.println((evt.getButton() == MouseEvent.BUTTON1) + "/" + (evt.getButton() == MouseEvent.BUTTON2) + "/" + (evt.getButton() == MouseEvent.BUTTON3));
            if (evt.getButton() == MouseEvent.BUTTON3) {
                if (Options.getInstance().isAlternativeMouseMode()) {
                    // toggle candidate in cell(s) (three state mode)
                    if (selectedCells.contains(Sudoku2.getIndex(line, col))) {
                        // a region select exists and the cells lies within: toggle candidate
                        if (cand != -1) {
                            changed = toggleCandidateInAktCells(cand);
                        }
                    } else {
                        // no region or cell outside region -> change focus and toggle candidate
                        setAktRowCol(line, col);
                        clearRegion();
                        if (sudoku.getValue(line, col) != 0 && !sudoku.isFixed(line, col)) {
                            deleteValuePopupMenu.show(this, getX(line, col) + cellSize, getY(line, col));
                        } else {
                            int showHintCellValue = getShowHintCellValue();
                            if ((cand == -1 || !sudoku.isCandidate(line, col, cand, !showCandidates))
                                    && showHintCellValue != 0) {
                                // if the candidate is not present, but part of the solution and
                                // show deviations is set, it is displayed, although technically
                                // not present: it should be toggled, even if it is not the
                                // hint value
                                if (showDeviations && sudoku.isSolutionSet()
                                        && cand == sudoku.getSolution(aktLine, aktCol)) {
                                    toggleCandidateInCell(aktLine, aktCol, cand);
                                } else {
                                    toggleCandidateInCell(aktLine, aktCol, showHintCellValue);
                                }
                            } else if (cand != -1) {
                                toggleCandidateInCell(aktLine, aktCol, cand);
                            }
                            changed = true;
                        }
                    }
                } else {
                    // bring up popup menu
                    showPopupMenu(line, col);
                }
            } else {
                if (aktColorIndex != -1) {
                    // coloring is active
                    int colorNumber = aktColorIndex;
                    if (shiftPressed || evt.getButton() == MouseEvent.BUTTON2) {
                        if (colorNumber % 2 == 0) {
                            colorNumber++;
                        } else {
                            colorNumber--;
                        }
                    }
                    //System.out.println(line + "/" + col + "/" + cand + "/" + colorNumber + "/" + colorCells);
                    if (colorCells) {
                        // coloring for cells
                        handleColoring(line, col, -1, colorNumber);
                    } else {
                        // coloring for candidates
                        if (cand != -1) {
                            handleColoring(line, col, cand, colorNumber);
                        }
                    }
                    // we do adjust the selected cell (ranges are not allowed in coloring)
                    setAktRowCol(line, col);
//                    aktLine = line;
//                    aktCol = col;
                } else if (evt.getButton() == MouseEvent.BUTTON1) {
                    // in normal mode we only react to the left mouse button
                    //System.out.println("BUTTON1/" + evt.getClickCount() + "/" + ctrlPressed + "/" + cand);
                    int index = Sudoku2.getIndex(line, col);
//                    if (evt.getClickCount() == 2) {
                    if (doubleClick) {
                        if (ctrlPressed) {
                            if (cand != -1) {
                                // toggle candidate
                                if (sudoku.isCandidate(index, cand, !showCandidates)) {
                                    sudoku.setCandidate(index, cand, false, !showCandidates);
                                } else {
                                    sudoku.setCandidate(index, cand, true, !showCandidates);
                                }
                                clearRegion();
                                changed = true;
                            }
                        } else {
                            if (sudoku.getValue(index) == 0) {
                                int showHintCellValue = getShowHintCellValue();
                                if (sudoku.getAnzCandidates(index, !showCandidates) == 1) {
                                    // Naked single -> set it!
                                    int actCand = sudoku.getAllCandidates(index, !showCandidates)[0];
                                    setCell(line, col, actCand);
                                    changed = true;
                                } else if (showHintCellValue != 0 && isHiddenSingle(showHintCellValue, line, col)) {
                                    // Hidden Single -> it
                                    setCell(line, col, showHintCellValue);
                                    changed = true;
                                } else if (cand != -1) {
                                    // candidate double clicked -> set it
                                    // (only if that candidate is still set in the cell)
                                    if (sudoku.isCandidate(index, cand, !showCandidates)) {
                                        setCell(line, col, cand);
                                    }
                                    changed = true;
                                }
                            }
                        }
//                    } else if (evt.getClickCount() == 1) {
                    } else if (!doubleClick) {
                        if (ctrlPressed) {
                            // select additional cell
                            if (selectedCells.size() == 0) {
                                // the last selected cell is not yet in the set
                                selectedCells.add(Sudoku2.getIndex(aktLine, aktCol));
                                selectedCells.add(Sudoku2.getIndex(line, col));
                                setAktRowCol(line, col);
//                                aktLine = line;
//                                aktCol = col;
                            } else {
                                int index2 = Sudoku2.getIndex(line, col);
                                if (selectedCells.contains(index2)) {
                                    selectedCells.remove(index2);
                                } else {
                                    selectedCells.add(Sudoku2.getIndex(line, col));
                                }
                                setAktRowCol(line, col);
//                                aktLine = line;
//                                aktCol = col;
                            }
                        } else if (shiftPressed) {
                            if (Options.getInstance().isUseShiftForRegionSelect()) {
                                // select range of cells
                                selectRegion(line, col);
                            } else {
                                if (cand != -1) {
                                    // toggle candidate
                                    if (sudoku.isCandidate(index, cand, !showCandidates)) {
                                        sudoku.setCandidate(index, cand, false, !showCandidates);
                                    } else {
                                        sudoku.setCandidate(index, cand, true, !showCandidates);
                                    }
                                    clearRegion();
                                    changed = true;
                                }
                            }
                        } else {
                            // select single cell, delete old markings if available
                            // in the alternative mouse mode a single cell is only
                            // selected, if the cell is outside a selected region
                            if (Options.getInstance().isAlternativeMouseMode() == false
                                    || (Options.getInstance().isAlternativeMouseMode() == true
                                    && selectedCells.contains(Sudoku2.getIndex(line, col)) == false)) {
                                setAktRowCol(line, col);
//                                aktLine = line;
//                                aktCol = col;
                                clearRegion();
                            }
                            if (Options.getInstance().isAlternativeMouseMode()) {
//                                System.out.println(index + "/" + cand);
                                // the selected cell(s) must be set to cand
                                if (sudoku.getValue(index) == 0) {
                                    if (selectedCells.isEmpty()) {
                                        int showHintCellValue = getShowHintCellValue();
                                        if (sudoku.getAnzCandidates(index, !showCandidates) == 1) {
                                            // Naked single -> set it!
                                            int actCand = sudoku.getAllCandidates(index, !showCandidates)[0];
                                            setCell(line, col, actCand);
                                            changed = true;
                                        } else if (showHintCellValue != 0 && isHiddenSingle(showHintCellValue, line, col)) {
                                            // Hidden Single -> it
                                            setCell(line, col, showHintCellValue);
                                            changed = true;
                                        } else if (cand != -1) {
                                            // set candidate
                                            // (only if that candidate is still set in the cell)
                                            if (sudoku.isCandidate(index, cand, !showCandidates)) {
                                                setCell(line, col, cand);
                                            }
                                            changed = true;
                                        }
                                    } else {
                                        if (cand == -1 || !sudoku.isCandidate(index, cand, !showCandidates)) {
                                            // an empty space was clicked in the cell -> clear region
                                            setAktRowCol(line, col);
                                            clearRegion();
                                        } else if (cand != -1) {
                                            // an actual candiate was clicked -> set value in all cells where it is
                                            // still possible (collect cells first to avoid side effects!)
                                            List<Integer> cells = new ArrayList<Integer>();
                                            for (int selIndex : selectedCells) {
                                                if (sudoku.getValue(selIndex) == 0
                                                        && sudoku.isCandidate(selIndex, cand, !showCandidates)) {
                                                    cells.add(selIndex);
                                                }
                                            }
                                            for (int selIndex : cells) {
                                                setCell(Sudoku2.getLine(selIndex), Sudoku2.getCol(selIndex), cand);
                                            }
                                        }
                                    }
                                } else {
                                    // clear selection
                                    setAktRowCol(line, col);
//                                    aktLine = line;
//                                    aktCol = col;
                                    clearRegion();
                                }
                                changed = true;
                            }
                        }
                    }
                }
            }
            if (changed) {
                redoStack.clear();
                checkProgress();
            } else {
                undoStack.pop();
            }
            updateCellZoomPanel();
            mainFrame.check();
            repaint();
        }
    }

    /**
     * Moves the cursor. If the cell actually changed, a timer is started, that
     * triggers a repaint after {@link Options#getDeleteCursorDisplayLength() }
     * ms.
     *
     * @param row
     * @param col
     */
    private void setAktRowCol(int row, int col) {
        if (aktLine != row) {
            aktLine = row;
        }
        if (aktCol != col) {
            aktCol = col;
        }
        if (Options.getInstance().isDeleteCursorDisplay()) {
            deleteCursorTimer.stop();
            lastCursorChanged = System.currentTimeMillis();
            deleteCursorTimer.setDelay(Options.getInstance().getDeleteCursorDisplayLength());
            deleteCursorTimer.setInitialDelay(Options.getInstance().getDeleteCursorDisplayLength());
            deleteCursorTimer.start();
//            System.out.println("Timer startet (" + lastCursorChanged + "/" + deleteCursorTimer.getDelay() + ")");
        }
    }

    /**
     * Loads all relevant objects into
     * <code>state</code>. If
     * <code>copy</code> is true, all objects are copied.<br> Some objects have
     * to be copied regardless of parameter
     * <code>copy</code>.
     *
     * @param state
     * @param copy
     */
    @SuppressWarnings("unchecked")
    public void getState(GuiState state, boolean copy) {
        // items that dont have to be copied
        state.setChainIndex(chainIndex);
        // items that must be copied anyway
        state.setUndoStack((Stack<Sudoku2>) undoStack.clone());
        state.setRedoStack((Stack<Sudoku2>) redoStack.clone());
        state.setColoringMap((SortedMap<Integer, Integer>) ((TreeMap) coloringMap).clone());
        state.setColoringCandidateMap((SortedMap<Integer, Integer>) ((TreeMap) coloringCandidateMap).clone());
        // items that might be null (and therefore wont be copied)
        state.setSudoku(sudoku);
        state.setStep(step);
        if (copy) {
            state.setSudoku(sudoku.clone());
            if (step != null) {
                state.setStep((SolutionStep) step.clone());
            }
        }
    }

    /**
     * Loads back a saved state. Whether the objects had been copied before is
     * irrelevant here.<br> The optional objects {@link GuiState#undoStack} and {@link GuiState#redoStack}
     * can be null. If this is the case they are cleared.
     *
     * @param state
     */
    public void setState(GuiState state) {
        chainIndex = state.getChainIndex();
        if (state.getUndoStack() != null) {
            undoStack = state.getUndoStack();
        } else {
            undoStack.clear();
        }
        if (state.getRedoStack() != null) {
            redoStack = state.getRedoStack();
        } else {
            redoStack.clear();
        }
        if (state.getColoringMap() != null) {
            coloringMap = state.getColoringMap();
        } else {
            coloringMap.clear();
        }
        if (state.getColoringCandidateMap() != null) {
            coloringCandidateMap = state.getColoringCandidateMap();
        } else {
            coloringCandidateMap.clear();
        }
        sudoku = state.getSudoku();
        sudoku.checkSudoku();
        step = state.getStep();
        updateCellZoomPanel();
        checkProgress();
        mainFrame.check();
        repaint();
    }

//    public void loadFromFile(Sudoku sudoku, Sudoku solvedSudoku) {
//        this.sudoku = sudoku;
//        this.solvedSudoku = solvedSudoku;
//        redoStack.clear();
//        undoStack.clear();
//        coloringMap.clear();
//        coloringCandidateMap.clear();
//        step = null;
//        setChainInStep(-1);
//        updateCellZoomPanel();
//        mainFrame.check();
//        repaint();
//    }
    private void checkShowAllCandidates(int modifiers, int keyCode) {
        // wenn <Shift> und <Ctrl> gedrückt sind, soll showAllCandidatesAkt true sein, sonst false
        boolean oldShowAllCandidatesAkt = showAllCandidatesAkt;
        showAllCandidatesAkt = false;
        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0 && (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
            showAllCandidatesAkt = true;
        }
        // wenn <Shift> und <Alt> gedrückt sind, soll showAllCandidates true sein, sonst false
        boolean oldShowAllCandidates = showAllCandidates;
        showAllCandidates = false;
        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0 && (modifiers & KeyEvent.ALT_DOWN_MASK) != 0) {
            showAllCandidates = true;
        }
        if (oldShowAllCandidatesAkt != showAllCandidatesAkt || oldShowAllCandidates != showAllCandidates) {
            repaint();
        }
    }

    public void handleKeysReleased(KeyEvent evt) {
        // wenn <Left-Shift> und <Left-Ctrl> gedrückt sind, soll showAllCandidatesAkt true sein, sonst false
        int modifiers = evt.getModifiersEx();
        int keyCode = 0; // getKeyCode() liefert immer noch die zuletzt gedrückte Taste

        checkShowAllCandidates(modifiers, keyCode);

        if (aktColorIndex >= 0) {
            if (getCursor() == colorCursorShift) {
                setCursor(colorCursor);
                //System.out.println("normal cursor set");
            }
        }
    }

    public void handleKeys(KeyEvent evt) {
        // Undo/Redo: alten Zustand speichern, wenn nichts geändert wurde, wieder entfernen
        boolean changed = false;
        undoStack.push(sudoku.clone());

        int keyCode = evt.getKeyCode();
        int modifiers = evt.getModifiersEx();

        // wenn <Shift> und <Ctrl> gedrückt sind, soll showAllCandidatesAkt true sein, sonst false
        // wenn <Shift> und <Alt> gedrückt sind, soll showAllCandidates true sein, sonst false
        checkShowAllCandidates(modifiers, keyCode);

        // if only <shift> is pressed and coloring is active, the cursor should change to complementary color
        if (aktColorIndex >= 0) {
            if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                if (getCursor() == colorCursor) {
                    setCursor(colorCursorShift);
                    //System.out.println("cursor shift set");
                }
            }
        }

        // "normale" Tastaturbehandlung
        // bei keyPressed funktioniert getKeyChar() nicht zuverlässig, daher die Zahl selbst ermitteln
        // 20120111: makes problems on certain laptops where key combinations are
        // used to produce numbers. New try: If getKeyChar() gives a number, the corresponding key code is set
        char keyChar = evt.getKeyChar();
        if (Character.isDigit(keyChar)) {
            keyCode = KEY_CODES[keyChar - '0'];
        }
        int number = 0;
        boolean clearSelectedRegion = true;
        switch (keyCode) {
            case KeyEvent.VK_DOWN:
                if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0
                        && (modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0
                        && getShowHintCellValue() != 0) {
                    // go to next filtered candidate
                    int index = findNextHintCandidate(aktLine, aktCol, keyCode);
                    setAktRowCol(Sudoku2.getLine(index), Sudoku2.getCol(index));
//                    aktLine = Sudoku2.getLine(index);
//                    aktCol = Sudoku2.getCol(index);
                } else if (aktLine < 8) {
                    // go to the next line
                    setAktRowCol(aktLine + 1, aktCol);
//                    aktLine++;
                    if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
                        // go to the next unset cell
                        while (aktLine < 8 && sudoku.getValue(aktLine, aktCol) != 0) {
                            setAktRowCol(aktLine + 1, aktCol);
//                            aktLine++;
                        }
                    } else if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                        // expand the selected region
                        setAktRowCol(aktLine - 1, aktCol);
//                        aktLine--;
                        setShift();
                        if (shiftLine < 8) {
                            shiftLine++;
                        }
                        selectRegion(shiftLine, shiftCol);
                        clearSelectedRegion = false;
                    }
                }
                if (clearSelectedRegion) {
                    clearRegion();
                }
                break;
            case KeyEvent.VK_UP:
                if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0
                        && (modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0
                        && getShowHintCellValue() != 0) {
                    // go to next filtered candidate
                    int index = findNextHintCandidate(aktLine, aktCol, keyCode);
                    setAktRowCol(Sudoku2.getLine(index), Sudoku2.getCol(index));
//                    aktLine = Sudoku2.getLine(index);
//                    aktCol = Sudoku2.getCol(index);
                } else if (aktLine > 0) {
                    // go to the next line
                    setAktRowCol(aktLine - 1, aktCol);
//                    aktLine--;
                    if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
                        // go to the next unset cell
                        while (aktLine > 0 && sudoku.getValue(aktLine, aktCol) != 0) {
                            setAktRowCol(aktLine - 1, aktCol);
//                            aktLine--;
                        }
                    } else if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                        // expand the selected region
                        setAktRowCol(aktLine + 1, aktCol);
//                        aktLine++;
                        setShift();
                        if (shiftLine > 0) {
                            shiftLine--;
                        }
                        selectRegion(shiftLine, shiftCol);
                        clearSelectedRegion = false;
                    }
                }
                if (clearSelectedRegion) {
                    clearRegion();
                }
                break;
            case KeyEvent.VK_RIGHT:
                if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0
                        && (modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0
                        && getShowHintCellValue() != 0) {
                    // go to next filtered candidate
                    int index = findNextHintCandidate(aktLine, aktCol, keyCode);
                    setAktRowCol(Sudoku2.getLine(index), Sudoku2.getCol(index));
//                    aktLine = Sudoku2.getLine(index);
//                    aktCol = Sudoku2.getCol(index);
                } else if (aktCol < 8) {
                    // go to the next line
                    setAktRowCol(aktLine, aktCol + 1);
//                    aktCol++;
                    if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
                        // go to the next unset cell
                        while (aktCol < 8 && sudoku.getValue(aktLine, aktCol) != 0) {
                            setAktRowCol(aktLine, aktCol + 1);
//                            aktCol++;
                        }
                    } else if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                        // expand the selected region
                        setAktRowCol(aktLine, aktCol - 1);
//                        aktCol--;
                        setShift();
                        if (shiftCol < 8) {
                            shiftCol++;
                        }
                        selectRegion(shiftLine, shiftCol);
                        clearSelectedRegion = false;
                    }
                }
                if (clearSelectedRegion) {
                    clearRegion();
                }
                break;
            case KeyEvent.VK_LEFT:
                if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0
                        && (modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0
                        && getShowHintCellValue() != 0) {
                    // go to next filtered candidate
                    int index = findNextHintCandidate(aktLine, aktCol, keyCode);
                    setAktRowCol(Sudoku2.getLine(index), Sudoku2.getCol(index));
//                    aktLine = Sudoku2.getLine(index);
//                    aktCol = Sudoku2.getCol(index);
                } else if (aktCol > 0) {
                    // go to the next col
                    setAktRowCol(aktLine, aktCol - 1);
//                    aktCol--;
                    if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
                        // go to the next unset cell
                        while (aktCol > 0 && sudoku.getValue(aktLine, aktCol) != 0) {
                            setAktRowCol(aktLine, aktCol - 1);
//                            aktCol--;
                        }
                    } else if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                        // expand the selected region
                        setAktRowCol(aktLine, aktCol + 1);
//                        aktCol++;
                        setShift();
                        if (shiftCol > 0) {
                            shiftCol--;
                        }
                        selectRegion(shiftLine, shiftCol);
                        clearSelectedRegion = false;
                    }
                }
                if (clearSelectedRegion) {
                    clearRegion();
                }
                break;
            case KeyEvent.VK_HOME:
                if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                    setShift();
                    if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
                        shiftLine = 0;
                    } else {
                        shiftCol = 0;
                    }
                    selectRegion(shiftLine, shiftCol);
                    clearSelectedRegion = false;
                } else {
                    if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
                        setAktRowCol(0, aktCol);
//                        aktLine = 0;
                    } else {
                        setAktRowCol(aktLine, 0);
//                        aktCol = 0;
                    }
                }
                if (clearSelectedRegion) {
                    clearRegion();
                }
                break;
            case KeyEvent.VK_END:
                if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                    setShift();
                    if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
                        shiftLine = 8;
                    } else {
                        shiftCol = 8;
                    }
                    selectRegion(shiftLine, shiftCol);
                    clearSelectedRegion = false;
                } else {
                    if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
                        setAktRowCol(8, aktCol);
//                        aktLine = 8;
                    } else {
                        setAktRowCol(aktLine, 8);
//                        aktCol = 8;
                    }
                }
                if (clearSelectedRegion) {
                    clearRegion();
                }
                break;
            case KeyEvent.VK_ENTER: {
                int index = Sudoku2.getIndex(aktLine, aktCol);
                if (sudoku.getValue(index) == 0) {
                    int showHintCellValue = getShowHintCellValue();
                    if (sudoku.getAnzCandidates(index, !showCandidates) == 1) {
                        // Naked single -> set it!
                        int actCand = sudoku.getAllCandidates(index, !showCandidates)[0];
                        setCell(aktLine, aktCol, actCand);
                        changed = true;
                    } else if (showHintCellValue != 0 && isHiddenSingle(showHintCellValue,
                            aktLine, aktCol)) {
                        // Hidden Single -> it
                        setCell(aktLine, aktCol, showHintCellValue);
                        changed = true;
                    }
                }
            }
            break;
            case KeyEvent.VK_9:
            case KeyEvent.VK_NUMPAD9:
                number++;
            case KeyEvent.VK_8:
            case KeyEvent.VK_NUMPAD8:
                number++;
            case KeyEvent.VK_7:
            case KeyEvent.VK_NUMPAD7:
                number++;
            case KeyEvent.VK_6:
            case KeyEvent.VK_NUMPAD6:
                number++;
            case KeyEvent.VK_5:
            case KeyEvent.VK_NUMPAD5:
                number++;
            case KeyEvent.VK_4:
            case KeyEvent.VK_NUMPAD4:
                number++;
            case KeyEvent.VK_3:
            case KeyEvent.VK_NUMPAD3:
                number++;
            case KeyEvent.VK_2:
            case KeyEvent.VK_NUMPAD2:
                number++;
            case KeyEvent.VK_1:
            case KeyEvent.VK_NUMPAD1:
                number++;
                //int number = Character.digit(evt.getKeyChar(), 10);
                if ((modifiers & KeyEvent.CTRL_DOWN_MASK) == 0) {
                    // Zelle setzen
                    if (selectedCells.isEmpty()) {
                        setCell(aktLine, aktCol, number);
                        if (mainFrame.isEingabeModus() && Options.getInstance().isEditModeAutoAdvance()) {
                            // automatically advance to the next cell
                            if (aktCol < 8) {
                                setAktRowCol(aktLine, aktCol + 1);
                            } else if (aktLine < 8) {
                                setAktRowCol(aktLine + 1, 0);
                            }
                        }
                    } else {
                        // set value only in cells where the candidate is still present
                        // problem: setting the first removes all other candidates in the
                        // corresponding blocks so we have to collect the applicable cells first
                        List<Integer> cells = new ArrayList<Integer>();
                        for (int index : selectedCells) {
                            if (sudoku.getValue(index) == 0 && sudoku.isCandidate(index, number, !showCandidates)) {
                                cells.add(index);
                            }
                        }
                        for (int index : cells) {
                            setCell(Sudoku2.getLine(index), Sudoku2.getCol(index), number);
                        }
                    }
                    changed = true;
//                } else if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) == 0) {
                } else {
                    // only when shift is NOT pressed (if pressed its a menu accelerator)
                    // 20120115: the accelerators have been removed!
                    if (selectedCells.isEmpty()) {
                        toggleCandidateInCell(aktLine, aktCol, number);
                        changed = true;
                    } else {
                        changed = toggleCandidateInAktCells(number);
                    }
                }
                break;
            case KeyEvent.VK_BACK_SPACE:
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_0:
            case KeyEvent.VK_NUMPAD0:
                if ((modifiers & KeyEvent.CTRL_DOWN_MASK) == 0) {
                    // Zelle löschen
                    if (sudoku.getValue(aktLine, aktCol) != 0 && !sudoku.isFixed(aktLine, aktCol)) {
                        sudoku.setCell(aktLine, aktCol, 0);
                        changed = true;
                    }
                    if (mainFrame.isEingabeModus() && Options.getInstance().isEditModeAutoAdvance()) {
                        // automatically advance to the next cell
                        if (aktCol < 8) {
                            setAktRowCol(aktLine, aktCol + 1);
                        } else if (aktLine < 8) {
                            setAktRowCol(aktLine + 1, 0);
                        }
                    }
                }
                break;
            case KeyEvent.VK_F9:
                number++;
            case KeyEvent.VK_F8:
                number++;
            case KeyEvent.VK_F7:
                number++;
            case KeyEvent.VK_F6:
                number++;
            case KeyEvent.VK_F5:
                number++;
            case KeyEvent.VK_F4:
                number++;
            case KeyEvent.VK_F3:
                number++;
            case KeyEvent.VK_F2:
                number++;
            case KeyEvent.VK_F1:
                number++;
                if ((modifiers & KeyEvent.ALT_DOWN_MASK) == 0) {
                    // pressing <Alt><F4> changes the selection ... not good
                    // <fn> toggles the corresponding filter
                    // <ctrl><fn> selects an additional candidate for filtering
                    // <shift><fn> additionally toggles the filter mode
                    if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
                        // just toggle the candidate
                        showHintCellValues[number] = !showHintCellValues[number];
                        // no bivalue cells!
                        showHintCellValues[10] = false;
                    } else {
                        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                            invalidCells = !invalidCells;
                        }
                        setShowHintCellValue(number);
                    }
                    checkIsShowInvalidOrPossibleCells();
                }
                break;
            case KeyEvent.VK_SPACE:
                int candidate = getShowHintCellValue();
                if (isShowInvalidOrPossibleCells() && candidate != 0) {
                    changed = toggleCandidateInAktCells(candidate);
                }
                break;
            case KeyEvent.VK_E:
                number++;
            case KeyEvent.VK_D:
                number++;
            case KeyEvent.VK_C:
                number++;
            case KeyEvent.VK_B:
                number++;
            case KeyEvent.VK_A:
                // if ctrl or alt or meta is pressed, it's a shortcut
                if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0
                        || (modifiers & KeyEvent.ALT_GRAPH_DOWN_MASK) != 0
                        || (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0
                        || (modifiers & KeyEvent.META_DOWN_MASK) != 0) {
                    // do nothing!
                    break;
                }
                // calculate index in coloringColors[]
                number *= 2;
                if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                    number++;
                }
                handleColoring(-1, number);
//                int index = Sudoku.getIndex(aktLine, aktCol);
//                if (coloringMap.containsKey(index) && coloringMap.get(index) == number) {
//                    // pressing the same key on the same cell twice removes the coloring
//                    coloringMap.remove(index);
//                } else {
//                    // either newly colored cell or change of cell color
//                    coloringMap.put(index, number);
//                }
                break;
            case KeyEvent.VK_R:
                clearColoring();
                break;
            case KeyEvent.VK_GREATER:
            case KeyEvent.VK_COMMA:
            case KeyEvent.VK_LESS:
            case KeyEvent.VK_PERIOD:
            default:
                // doesnt work on all keyboards :-(
                // more precisely: doesnt work, if the keyboard layout in the OS
                // doesnt match the physical layout of the keyboard
                short rem = sudoku.getRemainingCandidates();
                char ch = evt.getKeyChar();
                if (ch == '<' || ch == '>' || ch == ',' || ch == '.') {
                    boolean isUp = ch == '>' || ch == '.';
                    if (isShowInvalidOrPossibleCells()) {
                        int cand = 0;
                        for (int i = 1; i < showHintCellValues.length - 1; i++) {
                            if (showHintCellValues[i]) {
                                cand = i;
                                if (!isUp) {
                                    // get the first candidate
                                    break;
                                }
                            }
                        }
                        int count = 0;
                        do {
                            if (isUp) {
                                cand++;
                                if (cand > 9) {
                                    cand = 1;
                                }
                            } else {
                                cand--;
                                if (cand < 1) {
                                    cand = 9;
                                }
                            }
                            count++;
                        } while (count < 8 && (Sudoku2.MASKS[cand] & rem) == 0);
                        if (count < 8) {
                            // if only one candidate is left for filtering,
                            // it would be toggled without this check
                            setShowHintCellValue(cand);
                            checkIsShowInvalidOrPossibleCells();
                        }
                    }
                }
                break;
        }
        if (changed) {
            // Undo wurde schon behandelt, Redo ist nicht mehr möglich
            redoStack.clear();
            checkProgress();
        } else {
            // kein Undo nötig -> wieder entfernen
            undoStack.pop();
        }
        updateCellZoomPanel();
        mainFrame.check();
        repaint();
    }

    /**
     * Clears a selected region of cells
     */
    private void clearRegion() {
        selectedCells.clear();
        shiftLine = -1;
        shiftCol = -1;
    }

    /**
     * Initializes {@link #shiftLine}/{@link #shiftCol} for selecting regions of
     * cells using the keyboard
     */
    private void setShift() {
        if (shiftLine == -1) {
            shiftLine = aktLine;
            shiftCol = aktCol;
        }
    }

    /**
     * Select all cells in the rectangle defined by
     * {@link #aktLine}/{@link #aktCol} and line/col
     *
     * @param line
     * @param col
     */
    private void selectRegion(int line, int col) {
        selectedCells.clear();
        if (line == aktLine && col == aktCol) {
            // same cell clicked twice -> no region selected -> do nothing
        } else {
            // every cell in the region gets selected, aktLine and aktCol are not changed
            int cStart = col < aktCol ? col : aktCol;
            int lStart = line < aktLine ? line : aktLine;
            for (int i = cStart; i <= cStart + Math.abs(col - aktCol); i++) {
                for (int j = lStart; j <= lStart + Math.abs(line - aktLine); j++) {
                    selectedCells.add(Sudoku2.getIndex(j, i));
                }
            }
        }
    }

    /**
     * Finds the next colored cell, if filters are applied. mode gives the
     * direction in which to search (as KeyEvent). The search wraps at sudoku
     * boundaries.
     *
     * @param line
     * @param col
     * @param mode
     * @return
     */
    private int findNextHintCandidate(int line, int col, int mode) {
        int index = Sudoku2.getIndex(line, col);
        int showHintCellValue = getShowHintCellValue();
        if (showHintCellValue == 0) {
            return index;
        }
        switch (mode) {
            case KeyEvent.VK_DOWN:
                // let's start with the next line
                line++;
                if (line == 9) {
                    line = 0;
                    col++;
                    if (col == 9) {
                        return index;
                    }
                }
                for (int i = col; i < 9; i++) {
                    int j = i == col ? line : 0;
                    for (; j < 9; j++) {
                        if (sudoku.getValue(j, i) == 0
                                && sudoku.isCandidate(j, i, showHintCellValue, !showCandidates)) {
                            return Sudoku2.getIndex(j, i);
                        }
                    }
                }
                break;
            case KeyEvent.VK_UP:
                // let's start with the previous line
                line--;
                if (line < 0) {
                    line = 8;
                    col--;
                    if (col < 0) {
                        return index;
                    }
                }
                for (int i = col; i >= 0; i--) {
                    int j = i == col ? line : 8;
                    for (; j >= 0; j--) {
                        if (sudoku.getValue(j, i) == 0
                                && sudoku.isCandidate(j, i, showHintCellValue, !showCandidates)) {
                            return Sudoku2.getIndex(j, i);
                        }
                    }
                }
                break;
            case KeyEvent.VK_LEFT:
                // lets start left
                index--;
                if (index < 0) {
                    return index + 1;
                }
                while (index >= 0) {
                    if (sudoku.getValue(index) == 0
                            && sudoku.isCandidate(index, showHintCellValue, !showCandidates)) {
                        return index;
                    }
                    index--;
                }
                if (index < 0) {
                    index = Sudoku2.getIndex(line, col);
                }
                break;
            case KeyEvent.VK_RIGHT:
                // lets start right
                index++;
                if (index >= sudoku.getCells().length) {
                    return index - 1;
                }
                while (index < sudoku.getCells().length) {
                    if (sudoku.getValue(index) == 0
                            && sudoku.isCandidate(index, showHintCellValue, !showCandidates)) {
                        return index;
                    }
                    index++;
                }
                if (index >= sudoku.getCells().length) {
                    index = Sudoku2.getIndex(line, col);
                }
                break;
        }
        return index;
    }

    /**
     * Removes all coloring info
     */
    public void clearColoring() {
        coloringMap.clear();
        coloringCandidateMap.clear();
        setActiveColor(-1);
        updateCellZoomPanel();
        mainFrame.check();
    }

    /**
     * Handles coloring for all selected cells, delegates to {@link #handleColoring(int, int, int, int)}
     * (see description there).
     *
     * @param candidate
     * @param colorNumber
     */
    private void handleColoring(int candidate, int colorNumber) {
        if (selectedCells.isEmpty()) {
            handleColoring(aktLine, aktCol, candidate, colorNumber);
        } else {
            for (int index : selectedCells) {
                handleColoring(Sudoku2.getLine(index), Sudoku2.getCol(index), candidate, colorNumber);
            }
        }
    }

    /**
     * Toggles Color for candidate in active cell; only called from
     * {@link CellZoomPanel}.
     *
     * @param candidate
     */
    public void handleColoring(int candidate) {
        handleColoring(aktLine, aktCol, candidate, aktColorIndex);
        repaint();
        updateCellZoomPanel();
        mainFrame.fixFocus();
    }

    /**
     * Handles the coloring of a cell or a candidate. If candidate equals -1, a
     * cell is to be coloured, else a candidate. If the target is already
     * colored and the new color matches the old one, coloring is removed, else
     * it is set to the new color.<br>
     * {@link Options#colorValues} decides, whether cells, that have already
     * been set, may be colored.
     *
     * @param line
     * @param col
     * @param candidate
     * @param colorNumber
     */
    private void handleColoring(int line, int col, int candidate, int colorNumber) {
        if (!Options.getInstance().isColorValues() && sudoku.getValue(line, col) != 0) {
            // do nothing
            return;
        }
        SortedMap<Integer, Integer> map = coloringMap;
        int key = Sudoku2.getIndex(line, col);
        if (candidate != -1) {
            key = key * 10 + candidate;
            map = coloringCandidateMap;
        }
        if (map.containsKey(key) && map.get(key) == colorNumber) {
            // pressing the same key on the same cell twice removes the coloring
            map.remove(key);
        } else {
            // either newly colored cell or change of cell color
            map.put(key, colorNumber);
        }
        updateCellZoomPanel();
    }

    /**
     * Handles "set value" done in {@link CellZoomPanel}. Should not be used
     * otherwise.
     *
     * @param number
     */
    public void setCellFromCellZoomPanel(int number) {
        undoStack.push(sudoku.clone());
        if (selectedCells.isEmpty()) {
            setCell(aktLine, aktCol, number);
        } else {
            for (int index : selectedCells) {
                setCell(Sudoku2.getLine(index), Sudoku2.getCol(index), number);
            }
        }
        updateCellZoomPanel();
        mainFrame.check();
        repaint();
    }

    private void setCell(int line, int col, int number) {
        //SudokuCell cell = sudoku.getCell(line, col);
        int index = Sudoku2.getIndex(line, col);
        if (!sudoku.isFixed(index) && sudoku.getValue(index) != number) {
            // Setzen ist möglich, auf löschen prüfen
            if (sudoku.getValue(index) != 0) {
                sudoku.setCell(line, col, 0);
            }
            sudoku.setCell(line, col, number);
            repaint();
            if (sudoku.isSolved() && Options.getInstance().isShowSudokuSolved()) {
                JOptionPane.showMessageDialog(this, java.util.ResourceBundle.getBundle("intl/MainFrame").getString("MainFrame.sudoku_solved"),
                        java.util.ResourceBundle.getBundle("intl/MainFrame").getString("MainFrame.congratulations"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Toggles candidate in all active cells (all cells in {@link #selectedCells}
     * or cell denoted by {@link #aktLine}/{@link #aktCol} if {@link #selectedCells}
     * is empty).<br>
     *
     * @param candidate
     * @return
     * <code>true</code>, if at least one cell was changed
     */
    private boolean toggleCandidateInAktCells(int candidate) {
        boolean changed = false;
        if (selectedCells.isEmpty()) {
            toggleCandidateInCell(aktLine, aktCol, candidate);
            return true;
        } else {
            boolean candPresent = false;
            for (int index : selectedCells) {
                if (sudoku.getValue(index) == 0 && sudoku.isCandidate(index, candidate, !showCandidates)) {
                    candPresent = true;
                    break;
                }
            }
            for (int index : selectedCells) {
                if (candPresent) {
                    if (sudoku.getValue(index) == 0 && sudoku.isCandidate(index, candidate, !showCandidates)) {
                        sudoku.setCandidate(index, candidate, false, !showCandidates);
                        changed = true;
                    }
                } else {
                    if (sudoku.getValue(index) == 0 && !sudoku.isCandidate(index, candidate, !showCandidates)) {
                        sudoku.setCandidate(index, candidate, true, !showCandidates);
                        changed = true;
                    }
                }
            }
        }
        updateCellZoomPanel();
        return changed;
    }

    /**
     * Toggles candidate in the cell denoted by line/col. Uses {@link #candidateMode}.
     *
     * @param line
     * @param col
     * @param candidate
     */
    private void toggleCandidateInCell(int line, int col, int candidate) {
        int index = Sudoku2.getIndex(line, col);
        if (sudoku.getValue(index) == 0) {
            if (sudoku.isCandidate(index, candidate, !showCandidates)) {
                sudoku.setCandidate(index, candidate, false, !showCandidates);
            } else {
                sudoku.setCandidate(index, candidate, true, !showCandidates);
            }
        }
        updateCellZoomPanel();
    }

    /**
     * Creates an image of the current sudoku in the given size.
     *
     * @param size
     * @return
     */
    public BufferedImage getSudokuImage(int size) {
        return getSudokuImage(size, false);
    }

    /**
     * Creates an image of the current sudoku in the given size.
     *
     * @param size
     * @param allBlack
     * @return
     */
    public BufferedImage getSudokuImage(int size, boolean allBlack) {
        BufferedImage fileImage = new BufferedImage(size, size, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = fileImage.createGraphics();
        this.g2 = g;
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, size, size);
        drawPage(size, size, true, false, allBlack, 1.0);
        return fileImage;
    }

    /**
     * Prints the current sudoku into the graphics context
     * <code>g</code> at the position
     * <code>x</code>/
     * <code>y</code> with size
     * <code>size</code>.
     *
     * @param g
     * @param x
     * @param y
     * @param size
     * @param allBlack
     * @param scale
     */
    public void printSudoku(Graphics2D g, int x, int y, int size, boolean allBlack, double scale) {
        Graphics2D oldG2 = this.g2;
        this.g2 = g;
        AffineTransform trans = g.getTransform();
        g.translate(x, y);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, size, size);
        drawPage(size, size, true, true, allBlack, scale);
        g.setTransform(trans);
        this.g2 = oldG2;
    }

    /**
     * Writes an image of the current sudoku as png into a file. The image is
     * <code>size</code> pixels wide and high, the resolution in the png file is
     * set to
     * <code>dpi</code>.
     *
     * @param file
     * @param size
     * @param dpi
     */
    public void saveSudokuAsPNG(File file, int size, int dpi) {
        BufferedImage fileImage = getSudokuImage(size);
        writePNG(fileImage, dpi, file);
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return Printable.NO_SUCH_PAGE;
        }

        // Graphics2D-Objekt herrichten
        // CAUTION: The Graphics2D object is created with the native printer
        // resolution, but scaled down to 72dpi using an AffineTransform.
        // To print in high resolution this downscaling has to be reverted.
        Graphics2D printG2 = (Graphics2D) graphics;
        double scale = SudokuUtil.adjustGraphicsForPrinting(printG2);
//        AffineTransform at = printG2.getTransform();
//        double scale = at.getScaleX();
//        int resolution = (int)(72.0 * scale);
//        AffineTransform newAt = new AffineTransform();
//        newAt.translate(at.getTranslateX(), at.getTranslateY());
//        newAt.shear(at.getShearX(), at.getShearY());
//        printG2.setTransform(newAt);
        printG2.translate((int) (pageFormat.getImageableX() * scale), (int) (pageFormat.getImageableY() * scale));
        int printWidth = (int) (pageFormat.getImageableWidth() * scale);
        int printHeight = (int) (pageFormat.getImageableHeight() * scale);
//        printG2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        printG2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Überschrift
        // scale fonts up too fit the printer resolution
        Font tmpFont = Options.getInstance().getBigFont();
        bigFont = new Font(tmpFont.getName(), tmpFont.getStyle(), (int) (tmpFont.getSize() * scale));
        tmpFont = Options.getInstance().getSmallFont();
        smallFont = new Font(tmpFont.getName(), tmpFont.getStyle(), (int) (tmpFont.getSize() * scale));
        printG2.setFont(bigFont);
        String title = MainFrame.VERSION;
        FontMetrics metrics = printG2.getFontMetrics();
        int textWidth = metrics.stringWidth(title);
        int textHeight = metrics.getHeight();
        int y = 2 * textHeight;
        printG2.drawString(title, (printWidth - textWidth) / 2, textHeight);

        // Level
        printG2.setFont(smallFont);
        if (sudoku != null && sudoku.getLevel() != null) {
            title = sudoku.getLevel().getName() + " (" + sudoku.getScore() + ")";
            metrics = printG2.getFontMetrics();
            textWidth = metrics.stringWidth(title);
            textHeight = metrics.getHeight();
            printG2.drawString(title, (printWidth - textWidth) / 2, y);
            y += textHeight;
        }

        printG2.translate(0, y);
        this.g2 = printG2;
        drawPage(printWidth, printHeight, true, true, false, scale);
        return Printable.PAGE_EXISTS;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g2 = (Graphics2D) g;
        drawPage(getBounds().width, getBounds().height, false, true, false, 1.0);
    }

//    private void drawPage(int totalWidth, int totalHeight) {
//        drawPage(totalWidth, totalHeight, false, true, false, 1.0);
//    }
//
//    private void drawPage(int totalWidth, int totalHeight, boolean isPrint) {
//        drawPage(totalWidth, totalHeight, isPrint, true, false, 1.0);
//    }
    /**
     * Draws the sudoku in its current state on the graphics context denoted by
     * {@link #g2} (code>g2</code> has to be set before calling this method).
     * The graphics context can belong to a
     * <code>Component</code> (basic redraw), a
     * <code>BufferedImage</code> (save Sudoku as image) or to a print
     * canvas.<br><br>
     *
     * Sudokus are always drawn as quads, even if
     * <code>totalWidth</code> and
     * <code>totalHeight</code> are not the same. The quadrat is then center
     * within the available space.
     *
     * @param totalWidth The width of the sudoku in pixel
     * @param totalHeight The height of the sudoku in pixel
     * @param isPrint The sudoku is drawn on a print canvas: always draw at the
     * uper left corner and dont draw a cursor
     * @param withBorder A white border of at least {@link #DELTA_RAND} pixels
     * is drawn around the sudoku.
     * @param allBlack Replace all colors with black. Should only be used, if
     * filters, steps or coloring are not used.
     * @param scale Necessary for high resolution printing
     */
    private void drawPage(int totalWidth, int totalHeight, boolean isPrint, boolean withBorder, boolean allBlack, double scale) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (lastCursorChanged == -1) {
            lastCursorChanged = System.currentTimeMillis();
        }

        // determine the actual size of the quad
        width = totalWidth;
        height = totalHeight;
        width = (height < width) ? height : width;
        height = (width < height) ? width : height;

        // make the size of the lines larger, especially for high res printing
        float strokeWidth = 2.0f / 1000.0f * width;
        if (width > 1000) {
            strokeWidth *= 1.5f;
        }
        float boxStrokeWidth = (float) (strokeWidth * Options.getInstance().getBoxLineFactor());
        int strokeWidthInt = Math.round(boxStrokeWidth / 2);
//        delta = (int)(DELTA * scale);
//        deltaRand = (int)(DELTA_RAND * scale);
        delta = totalWidth / 100;
        deltaRand = totalWidth / 100;
        if (deltaRand < strokeWidthInt) {
            deltaRand = strokeWidthInt;
        }

        if (Options.getInstance().getDrawMode() == 1) {
            delta = 0;
        }

        // calculate the size of the cells and adjust for rounding errors
        if (withBorder) {
            cellSize = (width - 4 * delta - 2 * deltaRand) / 9;
        } else {
            cellSize = (width - 4 * delta) / 9;
        }
        width = height = cellSize * 9 + 4 * delta;
        startSX = (totalWidth - width) / 2;
        if (isPrint && withBorder) {
            startSY = 0;
        } else {
            startSY = (totalHeight - height) / 2;
        }
        int colorKuCellSize = (int) (cellSize * 0.9);

        // get the fonts every time the size of the grid changes or
        // the user selects a different font in the preferences dialog
        Font tmpFont = Options.getInstance().getDefaultValueFont();
        if (valueFont != null) {
            if (!valueFont.getName().equals(tmpFont.getName())
                    || valueFont.getStyle() != tmpFont.getStyle()
                    || valueFont.getSize() != ((int) (cellSize * Options.getInstance().getValueFontFactor()))) {
                valueFont = new Font(tmpFont.getName(), tmpFont.getStyle(),
                        (int) (cellSize * Options.getInstance().getValueFontFactor()));
            }
        }
        tmpFont = Options.getInstance().getDefaultCandidateFont();
        if (candidateFont != null) {
            if (!candidateFont.getName().equals(tmpFont.getName())
                    || candidateFont.getStyle() != tmpFont.getStyle()
                    || candidateFont.getSize() != ((int) (cellSize * Options.getInstance().getCandidateFontFactor()))) {
                int oldCandidateHeight = candidateHeight;
                candidateFont = new Font(tmpFont.getName(), tmpFont.getStyle(),
                        (int) (cellSize * Options.getInstance().getCandidateFontFactor()));
                FontMetrics cm = getFontMetrics(candidateFont);
                candidateHeight = (int) ((cm.getAscent() - cm.getDescent()) * 1.3);
                if (candidateHeight != oldCandidateHeight) {
                    resetColorKuImages();
                }
            }
        }
        if (oldWidth != width) {
            int oldCandidateHeight = candidateHeight;
            oldWidth = width;
            valueFont = new Font(Options.getInstance().getDefaultValueFont().getName(),
                    Options.getInstance().getDefaultValueFont().getStyle(),
                    (int) (cellSize * Options.getInstance().getValueFontFactor()));
            candidateFont = new Font(Options.getInstance().getDefaultCandidateFont().getName(),
                    Options.getInstance().getDefaultCandidateFont().getStyle(),
                    (int) (cellSize * Options.getInstance().getCandidateFontFactor()));
            FontMetrics cm = getFontMetrics(candidateFont);
            candidateHeight = (int) ((cm.getAscent() - cm.getDescent()) * 1.3);
            if (candidateHeight != oldCandidateHeight) {
                resetColorKuImages();
            }
        }

        // draw the cells
        // dx, dy: Offset in a cell for drawing values
        // dcx, dcy: Offset in one nineth of a cell for drawing candidates
        // ddx, ddy: Height and width of the background circle for a candidate
        //      more specifically: ddy is the diameter of the background circle
        double dx = 0, dy = 0, dcx = 0, dcy = 0, ddy = 0;
        for (int line = 0; line < 9; line++) {
            for (int col = 0; col < 9; col++) {

                // background first (ignore allBlack here!)
                g2.setColor(Options.getInstance().getDefaultCellColor());
                if (Sudoku2.getBlock(Sudoku2.getIndex(line, col)) % 2 != 0) {
                    // every other block may have a different background color
                    g2.setColor(Options.getInstance().getAlternateCellColor());
                }

                int cellIndex = Sudoku2.getIndex(line, col);
                boolean isSelected = (selectedCells.isEmpty() && line == aktLine && col == aktCol) || selectedCells.contains(cellIndex);
                // the cell doesnt count as selected, if the last change of the cursor has been a while
                if (isSelected && selectedCells.isEmpty() && Options.getInstance().isDeleteCursorDisplay()) {
//                    System.out.println("--- " + (System.currentTimeMillis() - lastCursorChanged));
                    if ((System.currentTimeMillis() - lastCursorChanged) > Options.getInstance().getDeleteCursorDisplayLength()) {
                        isSelected = false;
                    }
                }
                // dont paint the whole cell yellow, just a small frame, if onlySmallCursors is set
                if (isSelected && !isPrint && !Options.getInstance().isOnlySmallCursors()) {
                    setColor(g2, allBlack, Options.getInstance().getAktCellColor());
                }
                // check if the candidate denoted by showHintCellValue is a valid candidate; if showCandidates == true,
                // this can be done by SudokuCell.isCandidateValid(); if it is false, candidates entered by the user
                // are highlighted, regardless of validity
                // CHANGE: no filters if showCandiates == false
                boolean candidateValid = false;
                if (showInvalidOrPossibleCells) {
                    if (showCandidates) {
                        candidateValid = sudoku.areCandidatesValid(cellIndex, showHintCellValues, false);
//                    } else {
//                        candidateValid = sudoku.isCandidateValid(cellIndex, showHintCellValue, ! showCandidates);
                    }
                }
                if (isShowInvalidOrPossibleCells() && isInvalidCells()
                        && (sudoku.getValue(cellIndex) != 0 || (showInvalidOrPossibleCells && !candidateValid))) {
//                        (cell.getValue() != 0 || (getShowHintCellValue() != 0 && !cell.isCandidateValid(SudokuCell.PLAY, getShowHintCellValue())))) {
                    setColor(g2, allBlack, Options.getInstance().getInvalidCellColor());
                }
                if (isShowInvalidOrPossibleCells() && !isInvalidCells() && sudoku.getValue(cellIndex) == 0
                        && candidateValid && !Options.getInstance().isOnlySmallFilters()) {
//                        getShowHintCellValue() != 0 && cell.isCandidateValid(SudokuCell.PLAY, getShowHintCellValue())) {
                    setColor(g2, allBlack, Options.getInstance().getPossibleCellColor());
                }
                //if (cell.getValue() == 0 && coloringMap.containsKey(cellIndex)) {
                if (coloringMap.containsKey(cellIndex) && (sudoku.getValue(cellIndex) == 0 || Options.getInstance().isColorValues())) {
                    // coloring
                    setColor(g2, allBlack, Options.getInstance().getColoringColors()[coloringMap.get(cellIndex)]);
                }
                g2.fillRect(getX(line, col), getY(line, col), cellSize, cellSize);
                if (isSelected && !isPrint && g2.getColor() != Options.getInstance().getAktCellColor()) {
                    setColor(g2, allBlack, Options.getInstance().getAktCellColor());
                    int frameSize = (int) (cellSize * Options.getInstance().getCursorFrameSize());
//                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
//                    g2.fillRect(getX(line, col), getY(line, col), cellSize, cellSize);
//                    g2.setPaintMode();
                    int cx = getX(line, col);
                    int cy = getY(line, col);
                    g2.fillRect(cx, cy, cellSize, frameSize);
                    g2.fillRect(cx, cy, frameSize, cellSize);
                    g2.fillRect(cx + cellSize - frameSize, cy, frameSize, cellSize);
                    g2.fillRect(cx, cy + cellSize - frameSize, cellSize, frameSize);
                }


                // background is done, draw the value
                int startX = getX(line, col);
                int startY = getY(line, col);
                Color offColor = null;
                int offCand = 0;
                if (sudoku.getValue(cellIndex) != 0) {
                    // value set in cell: draw it
                    setColor(g2, allBlack, Options.getInstance().getCellValueColor());
                    if (sudoku.isFixed(cellIndex)) {
                        setColor(g2, allBlack, Options.getInstance().getCellFixedValueColor());
                    } else if (isShowWrongValues() == true && !sudoku.isValidValue(line, col, sudoku.getValue(cellIndex))) {
                        offColor = Options.getInstance().getColorKuColor(10);
                        offCand = 10;
                        setColor(g2, allBlack, Options.getInstance().getWrongValueColor());
                    } else if (isShowDeviations() && sudoku.isSolutionSet() && sudoku.getValue(cellIndex) != sudoku.getSolution(cellIndex)) {
                        offColor = Options.getInstance().getColorKuColor(11);
                        offCand = 11;
                        setColor(g2, allBlack, Options.getInstance().getDeviationColor());
                    }
                    g2.setFont(valueFont);
                    dx = (cellSize - g2.getFontMetrics().stringWidth("8")) / 2.0;
                    dy = (cellSize + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2.0;
                    int value = sudoku.getValue(cellIndex);
                    if (Options.getInstance().isShowColorKuAct()) {
                        // draw the corresponding icon
                        drawColorBox(value, g2, getX(line, col) + (cellSize - colorKuCellSize) / 2,
                                getY(line, col) + (cellSize - colorKuCellSize) / 2, colorKuCellSize, true);
//                        drawColorBox(value, g2, startX + 3, startY + 2, cellSize - 4);
                        if (offColor != null) {
                            // invalid values or deviations are shown with an "X" in different colors
                            setColor(g2, allBlack, offColor);
                            g2.drawString("X", (int) (startX + dx), (int) (startY + dy));
                        }
                    } else {
                        // draw the value
                        g2.drawString(Integer.toString(value), (int) (startX + dx), (int) (startY + dy));
                    }
                } else {
                    // draw the candidates equally distributed within the cell
                    // if showCandidates is false, the candidates are drawn anyway, if
                    // the user presses <shift><ctrl> (current cell - showAllCandidatesAkt) 
                    // or <shift><alt> (all cells - showAllCandidates)
                    g2.setFont(candidateFont);
                    boolean userCandidates = !showCandidates;
                    if (showAllCandidates || showAllCandidatesAkt && line == aktLine && col == aktCol) {
                        userCandidates = false;
                    }
                    // calculate the width of the space for one candidate
                    double third = cellSize / 3.0;
                    dcx = (third - g2.getFontMetrics().stringWidth("8")) / 2.0;
                    dcy = (third + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2.0;
//                    ddx = g2.getFontMetrics().stringWidth("8") * Options.getInstance().getHintBackFactor();
                    ddy = (g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) * Options.getInstance().getHintBackFactor();
                    for (int i = 1; i <= 9; i++) {
                        offColor = null;
                        // one candidate at a time
                        if (sudoku.isCandidate(cellIndex, i, userCandidates)
                                || (showCandidates && showDeviations && sudoku.isSolutionSet() && i == sudoku.getSolution(cellIndex))) {
                            Color hintColor = null;
                            Color candColor = null;
//                            setColor(g2, allBlack, Options.getInstance().getCandidateColor());
                            candColor = Options.getInstance().getCandidateColor();
                            double shiftX = ((i - 1) % 3) * third;
                            double shiftY = ((i - 1) / 3) * third;
                            if (Options.getInstance().isShowColorKuAct()) {
                                // Colorku has to be drawm here, or filters, coloring, hints wont be visible
//                                int ccx = (int) Math.round(startX + shiftX + third / 2.0 - ddy / 2.0);
//                                int ccy = (int) Math.round(startY + shiftY + third / 2.0 - ddy / 2.0);
//                                int ccs = (int) Math.round(ddy);
//                                drawColorBox(i, g2, ccx, ccy, ccs, false);
                                int ccx = (int) Math.round(startX + shiftX + third / 2.0 - candidateHeight / 2.0);
                                int ccy = (int) Math.round(startY + shiftY + third / 2.0 - candidateHeight / 2.0);
                                drawColorBox(i, g2, ccx, ccy, candidateHeight, false);
//                                drawColorBox(i, g2, (int) (startX + shiftX + 1), (int) (startY + shiftY + 1), (int) ddy - 1);
                            }
                            if (step != null) {
                                int index = Sudoku2.getIndex(line, col);
                                if (step.getIndices().indexOf(index) >= 0 && step.getValues().indexOf(i) >= 0) {
                                    hintColor = Options.getInstance().getHintCandidateBackColor();
                                    candColor = Options.getInstance().getHintCandidateColor();
                                }
                                int alsIndex = step.getAlsIndex(index, chainIndex);
                                if (alsIndex != -1 && ((chainIndex == -1 && !step.getType().isKrakenFish()) || alsToShow.contains(alsIndex))) {
                                    hintColor = Options.getInstance().getHintCandidateAlsBackColors()[alsIndex % Options.getInstance().getHintCandidateAlsBackColors().length];
                                    candColor = Options.getInstance().getHintCandidateAlsColors()[alsIndex % Options.getInstance().getHintCandidateAlsColors().length];
                                }
                                for (int k = 0; k < step.getChains().size(); k++) {
                                    if (step.getType().isKrakenFish() && chainIndex == -1) {
                                        // Index 0 means show no chain at all
                                        continue;
                                    }
                                    if (chainIndex != -1 && k != chainIndex) {
                                        // show only one chain in Forcing Chains/Nets
                                        continue;
                                    }
                                    Chain chain = step.getChains().get(k);
                                    for (int j = chain.getStart(); j <= chain.getEnd(); j++) {
                                        if (chain.getChain()[j] == Integer.MIN_VALUE) {
                                            // Trennmarker für mins -> ignorieren
                                            continue;
                                        }
                                        int chainEntry = Math.abs(chain.getChain()[j]);
                                        int index1 = -1, index2 = -1, index3 = -1;
                                        if (Chain.getSNodeType(chainEntry) == Chain.NORMAL_NODE) {
                                            index1 = Chain.getSCellIndex(chainEntry);
                                        }
                                        if (Chain.getSNodeType(chainEntry) == Chain.GROUP_NODE) {
                                            index1 = Chain.getSCellIndex(chainEntry);
                                            index2 = Chain.getSCellIndex2(chainEntry);
                                            index3 = Chain.getSCellIndex3(chainEntry);
                                        }
                                        if ((index == index1 || index == index2 || index == index3) && Chain.getSCandidate(chainEntry) == i) {
                                            if (Chain.isSStrong(chainEntry)) {
                                                // strong link
                                                hintColor = Options.getInstance().getHintCandidateBackColor();
                                                candColor = Options.getInstance().getHintCandidateColor();
                                            } else {
                                                hintColor = Options.getInstance().getHintCandidateFinBackColor();
                                                candColor = Options.getInstance().getHintCandidateFinColor();
                                            }
                                        }
                                    }
                                }
                                for (Candidate cand : step.getFins()) {
                                    if (cand.getIndex() == index && cand.getValue() == i) {
                                        hintColor = Options.getInstance().getHintCandidateFinBackColor();
                                        candColor = Options.getInstance().getHintCandidateFinColor();
                                    }
                                }
                                for (Candidate cand : step.getEndoFins()) {
                                    if (cand.getIndex() == index && cand.getValue() == i) {
                                        hintColor = Options.getInstance().getHintCandidateEndoFinBackColor();
                                        candColor = Options.getInstance().getHintCandidateEndoFinColor();
                                    }
                                }
                                if (step.getValues().contains(i) && step.getColorCandidates().containsKey(index)) {
                                    hintColor = Options.getInstance().getColoringColors()[step.getColorCandidates().get(index)];
                                    candColor = Options.getInstance().getCandidateColor();
                                }
                                for (Candidate cand : step.getCandidatesToDelete()) {
                                    if (cand.getIndex() == index && cand.getValue() == i) {
                                        hintColor = Options.getInstance().getHintCandidateDeleteBackColor();
                                        candColor = Options.getInstance().getHintCandidateDeleteColor();
                                    }
                                }
                                for (Candidate cand : step.getCannibalistic()) {
                                    if (cand.getIndex() == index && cand.getValue() == i) {
                                        hintColor = Options.getInstance().getHintCandidateCannibalisticBackColor();
                                        candColor = Options.getInstance().getHintCandidateCannibalisticColor();
                                    }
                                }
                            }
                            if (isShowWrongValues() == true && !sudoku.isCandidateValid(cellIndex, i, userCandidates)) {
                                offColor = Options.getInstance().getColorKuColor(10);
                                offCand = 10;
//                                setColor(g2, allBlack, Options.getInstance().getWrongValueColor());
                                candColor = Options.getInstance().getWrongValueColor();
                            }
                            if (!sudoku.isCandidate(cellIndex, i, userCandidates) && isShowDeviations() && sudoku.isSolutionSet()
                                    && i == sudoku.getSolution(cellIndex)) {
                                offColor = Options.getInstance().getColorKuColor(11);
                                offCand = 11;
//                                setColor(g2, allBlack, Options.getInstance().getDeviationColor());
                                candColor = Options.getInstance().getDeviationColor();
                            }

                            // filters on candidates instead of cells
                            if (isShowInvalidOrPossibleCells() && !isInvalidCells()
                                    && showHintCellValues[i] && Options.getInstance().isOnlySmallFilters()) {
                                setColor(g2, allBlack, Options.getInstance().getPossibleCellColor());
                                g2.fillRect((int) Math.round(startX + shiftX + third / 2.0 - ddy / 2.0),
                                        (int) Math.round(startY + shiftY + third / 2.0 - ddy / 2.0),
                                        (int) Math.round(ddy), (int) Math.round(ddy));
                            }

                            // Coloring
                            Color coloringColor = null;
                            if (coloringCandidateMap.containsKey(cellIndex * 10 + i)) {
                                //if (coloringMap.containsKey(cellIndex)) {
                                // coloring
                                coloringColor = Options.getInstance().getColoringColors()[coloringCandidateMap.get(cellIndex * 10 + i)];
                                //System.out.println("coloringColor for " + cellIndex + "/" + i + " is " + coloringColor.toString());
                            }
//                            Color oldColor = g2.getColor();

                            if (coloringColor != null) {
                                setColor(g2, allBlack, coloringColor);
                                //g2.fillRect(startX + shiftX + dcx - 2 * (ddy - ddx) / 3, startY + shiftY + dcy - 4 * ddy / 5 - 1, ddy, ddy);
                                g2.fillRect((int) Math.round(startX + shiftX + third / 2.0 - ddy / 2.0),
                                        (int) Math.round(startY + shiftY + third / 2.0 - ddy / 2.0),
                                        (int) Math.round(ddy), (int) Math.round(ddy));
                            }
                            if (hintColor != null) {
                                setColor(g2, allBlack, hintColor);
                                //g2.fillOval(startX + shiftX + dcx - 2 * (ddy - ddx) / 3, startY + shiftY + dcy - 4 * ddy / 5 - 1, ddy, ddy);
                                g2.fillOval((int) Math.round(startX + shiftX + third / 2.0 - ddy / 2.0),
                                        (int) Math.round(startY + shiftY + third / 2.0 - ddy / 2.0),
                                        (int) Math.round(ddy), (int) Math.round(ddy));
                            }
                            setColor(g2, allBlack, candColor);
//                            setColor(g2, allBlack, oldColor);
                            if (!Options.getInstance().isShowColorKuAct()) {
                                g2.drawString(Integer.toString(i), (int) Math.round(startX + dcx + shiftX), (int) Math.round(startY + dcy + shiftY));
//                            } else {
////                                int ccx = (int) Math.round(startX + shiftX + third / 2.0 - ddy / 2.0);
////                                int ccy = (int) Math.round(startY + shiftY + third / 2.0 - ddy / 2.0);
////                                int ccs = (int) Math.round(ddy);
////                                drawColorBox(i, g2, ccx, ccy, ccs, false);
//                                int ccx = (int) Math.round(startX + shiftX + third / 2.0 - candidateHeight / 2.0);
//                                int ccy = (int) Math.round(startY + shiftY + third / 2.0 - candidateHeight / 2.0);
//                                drawColorBox(i, g2, ccx, ccy, candidateHeight, false);
////                                drawColorBox(i, g2, (int) (startX + shiftX + 1), (int) (startY + shiftY + 1), (int) ddy - 1);
//                                if (offColor != null) {
//                                    setColor(g2, allBlack, offColor);
//                                    g2.drawString("X", (int) Math.round(startX + dcx + shiftX), (int) Math.round(startY + dcy + shiftY));
//                                }
                            } else {
                                if (offColor != null) {
//                                    setColor(g2, allBlack, offColor);
//                                    g2.drawString("X", (int) Math.round(startX + dcx + shiftX), (int) Math.round(startY + dcy + shiftY));
                                    int ccx = (int) Math.round(startX + shiftX + third / 2.0 - candidateHeight / 2.0);
                                    int ccy = (int) Math.round(startY + shiftY + third / 2.0 - candidateHeight / 2.0);
                                    drawColorBox(offCand, g2, ccx, ccy, candidateHeight, false);
                                }
                            }

                        }
                    }
                }
            }
        }

        // Rahmen zeichnen: muss am Schluss sein, wegen der Hintergründe
        switch (Options.getInstance().getDrawMode()) {
            case 0:
//                g2.setStroke(new BasicStroke((float) (2 * scale)));
//                setColor(g2, allBlack, Options.getInstance().getGridColor());
//                g2.drawRect(startSX, startSY, width, height);
                if (allBlack) {
                    g2.setStroke(new BasicStroke(strokeWidth / 2));
                } else {
                    g2.setStroke(new BasicStroke(strokeWidth));
                }
                setColor(g2, allBlack, Options.getInstance().getInnerGridColor());
                drawBlockLine(delta + startSX, 1 * delta + startSY, true);
                drawBlockLine(delta + startSX, 2 * delta + startSY + 3 * cellSize, true);
                drawBlockLine(delta + startSX, 3 * delta + startSY + 6 * cellSize, true);
                setColor(g2, allBlack, Options.getInstance().getGridColor());
                g2.setStroke(new BasicStroke(boxStrokeWidth));
                g2.drawRect(startSX, startSY, width, height);
                for (int i = 0; i < 3; i++) {
                    g2.drawRect((i + 1) * delta + startSX + i * 3 * cellSize, 1 * delta + startSY, 3 * cellSize, 3 * cellSize);
                    g2.drawRect((i + 1) * delta + startSX + i * 3 * cellSize, 2 * delta + startSY + 3 * cellSize, 3 * cellSize, 3 * cellSize);
                    g2.drawRect((i + 1) * delta + startSX + i * 3 * cellSize, 3 * delta + startSY + 6 * cellSize, 3 * cellSize, 3 * cellSize);
//                    g2.drawLine(startSX, startSY + i * 3 * cellSize, startSX + 9 * cellSize, startSY + i * 3 * cellSize);
//                    g2.drawLine(startSX + i * 3 * cellSize, startSY, startSX + i * 3 * cellSize, startSY + 9 * cellSize);
                }
                break;
            case 1:
                if (allBlack) {
                    g2.setStroke(new BasicStroke(strokeWidth / 2));
                } else {
                    g2.setStroke(new BasicStroke(strokeWidth));
                }
                setColor(g2, allBlack, Options.getInstance().getInnerGridColor());
                drawBlockLine(delta + startSX, 1 * delta + startSY, false);
                drawBlockLine(delta + startSX, 2 * delta + startSY + 3 * cellSize, false);
                drawBlockLine(delta + startSX, 3 * delta + startSY + 6 * cellSize, false);
                setColor(g2, allBlack, Options.getInstance().getGridColor());
                g2.setStroke(new BasicStroke(boxStrokeWidth));
                g2.drawRect(startSX, startSY, width, height);
                for (int i = 0; i < 3; i++) {
                    g2.drawLine(startSX, startSY + i * 3 * cellSize, startSX + 9 * cellSize, startSY + i * 3 * cellSize);
                    g2.drawLine(startSX + i * 3 * cellSize, startSY, startSX + i * 3 * cellSize, startSY + 9 * cellSize);
                }
                break;
        }

        // Chains zeichnen, wenn vorhanden
        if (step != null && !step.getChains().isEmpty()) {
            // es gibt mindestens eine Chain
            // zuerst alle Punkte sammeln (auch zu löschende Kandidaten und ALS)
            points.clear();
            //for (Chain chain : step.getChains()) {
            for (int ci = 0; ci < step.getChainAnz(); ci++) {
                if (step.getType().isKrakenFish() && chainIndex == -1) {
                    continue;
                }
                if (chainIndex != -1 && chainIndex != ci) {
                    continue;
                }
                Chain chain = step.getChains().get(ci);
                for (int i = chain.getStart(); i <= chain.getEnd(); i++) {
                    int che = Math.abs(chain.getChain()[i]);
                    points.add(getCandKoord(Chain.getSCellIndex(che), Chain.getSCandidate(che), cellSize));
                    if (Chain.getSNodeType(che) == Chain.GROUP_NODE) {
                        int indexC = Chain.getSCellIndex2(che);
                        if (indexC != -1) {
                            points.add(getCandKoord(indexC, Chain.getSCandidate(che), cellSize));
                        }
                        indexC = Chain.getSCellIndex3(che);
                        if (indexC != -1) {
                            points.add(getCandKoord(indexC, Chain.getSCandidate(che), cellSize));
                        }
                    }
                }
            }
            for (Candidate cand : step.getCandidatesToDelete()) {
                points.add(getCandKoord(cand.getIndex(), cand.getValue(), cellSize));
            }
            //for (AlsInSolutionStep als : step.getAlses()) {
            for (int ai = 0; ai < step.getAlses().size(); ai++) {
                if (step.getType().isKrakenFish() && chainIndex == -1) {
                    continue;
                }
                if (chainIndex != -1 && !alsToShow.contains(ai)) {
                    continue;
                }
                AlsInSolutionStep als = step.getAlses().get(ai);
                for (int i = 0; i < als.getIndices().size(); i++) {
                    int index = als.getIndices().get(i);
                    int[] cands = sudoku.getAllCandidates(index);
                    for (int j = 0; j < cands.length; j++) {
                        points.add(getCandKoord(index, cands[j], cellSize));
                    }
                }
            }
            // dann zeichnen
            //for (Chain chain : step.getChains()) {
            for (int ci = 0; ci < step.getChainAnz(); ci++) {
                if (step.getType().isKrakenFish() && chainIndex == -1) {
                    continue;
                }
                if (chainIndex != -1 && ci != chainIndex) {
                    continue;
                }
                Chain chain = step.getChains().get(ci);
                drawChain(g2, chain, cellSize, ddy, allBlack);
            }
        }
    }

    /**
     * Convenience method to make printing in all black easier.
     *
     * @param g2
     * @param color
     * @param allBlack
     */
    private void setColor(Graphics2D g2, boolean allBlack, Color color) {
        if (allBlack) {
            g2.setColor(Color.BLACK);
        } else {
            g2.setColor(color);
        }
    }

    /**
     * Draws a chain. <ul> <li>Calculate the end points of each link</li>
     * <li>Check, if another node is on the direct line between the end
     * points</li> <li>If so, draw a Bezier curve instead of a line (tangents
     * are 45 degrees of the direct line)</li> <li>If the length is very small,
     * the link is ommitted</li> </ul>
     *
     * @param g2
     * @param chain
     * @param cellSize
     * @param ddy
     * @param allBlack
     */
    private void drawChain(Graphics2D g2, Chain chain, int cellSize, double ddy, boolean allBlack) {
        // Calculate the coordinates of the startpoint for every link
        //System.out.println("Chain: " + chain.start + "/" + chain.end + "/" + chain.chain);
        int[] ch = chain.getChain();
        //List<Point> points1 = new ArrayList<Point>(chain.end - chain.start + 1);
        List<Point2D.Double> points1 = new ArrayList<Point2D.Double>(chain.getEnd() + 1);
        //for (int i = chain.start; i <= chain.end; i++) {
        for (int i = 0; i <= chain.getEnd(); i++) {
            if (i < chain.getStart()) {
                // belongs to some other chain-> ignore!
                points1.add(null);
                continue;
            }
            int che = Math.abs(ch[i]);
            points1.add(getCandKoord(Chain.getSCellIndex(che), Chain.getSCandidate(che), cellSize));
        }
        Stroke oldStroke = g2.getStroke();
        int oldChe = 0;
        int oldIndex = 0;
        int index = 0;
        for (int i = chain.getStart(); i < chain.getEnd(); i++) {
            // link is only drawn between different cells
            if (ch[i + 1] == Integer.MIN_VALUE) {
                // end point of a net branch -> ignore
                continue;
            }
            index = i;
            int che = Math.abs(ch[i]);
            int che1 = Math.abs(ch[i + 1]);
            if (ch[i] > 0 && ch[i + 1] < 0) {
                oldChe = che;
                oldIndex = i;
            }
            if (ch[i] == Integer.MIN_VALUE && ch[i + 1] < 0) {
                che = oldChe;
                index = oldIndex;
            }
            if (ch[i] < 0 && ch[i + 1] > 0) {
                che = oldChe;
                index = oldIndex;
            }
            if (Chain.getSCellIndex(che) == Chain.getSCellIndex(che1)) {
                // same cell -> ignore
                continue;
            }
            setColor(g2, allBlack, Options.getInstance().getArrowColor());
            if (Chain.isSStrong(che1)) {
                g2.setStroke(strongLinkStroke);
            } else {
                g2.setStroke(weakLinkStroke);
            }
            drawArrow(g2, index, i + 1, cellSize, ddy, points1);
        }
        g2.setStroke(oldStroke);

    }

    private void drawArrow(Graphics2D g2, int index1, int index2, int cellSize,
            double ddy, List<Point2D.Double> points1) {
        // calculate the start and end points for the arrow
        Point2D.Double p1 = (Point2D.Double) (points1.get(index1).clone());
        Point2D.Double p2 = (Point2D.Double) (points1.get(index2).clone());
        double length = p1.distance(p2);
        double deltaX = p2.x - p1.x;
        double deltaY = p2.y - p1.y;
        double alpha = Math.atan2(deltaY, deltaX);
        adjustEndPoints(p1, p2, alpha, ddy);

        // check, if another candidate lies on the direct line
        double epsilon = 0.1;
        double dx1 = deltaX;
        double dy1 = deltaY;
        boolean doesIntersect = false;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).equals(points1.get(index1)) || points.get(i).equals(points1.get(index2))) {
                continue;
            }
            Point2D.Double point = points.get(i);
            double dx2 = point.x - p1.x;
            double dy2 = point.y - p1.y;
            // Kontrolle mit Ähnlichkeitssatz
            if (Math.signum(dx1) == Math.signum(dx2) && Math.signum(dy1) == Math.signum(dy2)
                    && Math.abs(dx2) <= Math.abs(dx1) && Math.abs(dy2) <= Math.abs(dy1)) {
                // Punkt könnte auf der Geraden liegen
                if (dx1 == 0.0 || dy1 == 0.0 || Math.abs(dx1 / dy1 - dx2 / dy2) < epsilon) {
                    // Punkt liegt auf der Geraden
                    doesIntersect = true;
                    break;
                }
            }
        }
        if (length < 2.0 * ddy) {
            // line is very short, would not be seen if drawn directly
            doesIntersect = true;
        }

        // values for arrow head
        double aAlpha = alpha;

        // draw the line of the arrow
        if (doesIntersect) {
            double bezierLength = 20.0;
            // adjust for very short lines
            if (length < 2.0 * ddy) {
                bezierLength = length / 4.0;
            }
            // the end points are rotated 45 degrees (counter clockwise for the
            // start point, clockwise for the end point)
            rotatePoint(points1.get(index1), p1, -Math.PI / 4.0);
            rotatePoint(points1.get(index2), p2, Math.PI / 4.0);

            aAlpha = alpha - Math.PI / 4.0;
            double bX1 = p1.x + bezierLength * Math.cos(aAlpha);
            double bY1 = p1.y + bezierLength * Math.sin(aAlpha);
            aAlpha = alpha + Math.PI / 4.0;
            double bX2 = p2.x - bezierLength * Math.cos(aAlpha);
            double bY2 = p2.y - bezierLength * Math.sin(aAlpha);
            cubicCurve.setCurve(p1.x, p1.y, bX1, bY1, bX2, bY2, p2.x, p2.y);
            g2.draw(cubicCurve);

//            g2.drawLine(p1.x, p1.y, bX1, bY1);
//            g2.drawLine(p2.x, p2.y, bX2, bY2);

        } else {
            g2.drawLine((int) Math.round(p1.x), (int) Math.round(p1.y),
                    (int) Math.round(p2.x), (int) Math.round(p2.y));
        }

        // Pfeilspitzen zeichnen
        g2.setStroke(arrowStroke);
        double arrowLength = cellSize * arrowLengthFactor;
        double arrowHeight = arrowLength * arrowHeightFactor;
        if (length > (arrowLength * 2 + ddy)) {
            // calculate values for arrow head
            double sin = Math.sin(aAlpha);
            double cos = Math.cos(aAlpha);
            double aX = p2.x - cos * arrowLength;
            double aY = p2.y - sin * arrowLength;
            if (doesIntersect) {
                // try to calculate the real intersection point of
                // the bezier curve with the arrows middle line
                // the distance between p2 and aX/aY must be arrowLength
                // aX/aY should lie on the cubic curve
                double aXTemp = 0;
                double aYTemp = 0;
                double eps = Double.MAX_VALUE;
                double[] tmpPoints = new double[6];
                PathIterator pIt = cubicCurve.getPathIterator(null, 0.01);
                while (!pIt.isDone()) {
                    int type = pIt.currentSegment(tmpPoints);
                    double dist = p2.distance(tmpPoints[0], tmpPoints[1]);
                    if (Math.abs(dist - arrowLength) < eps) {
                        eps = Math.abs(dist - arrowLength);
                        aXTemp = tmpPoints[0];
                        aYTemp = tmpPoints[1];
                    }
                    pIt.next();
                }
                //ok, closest point is now in aXTemp/aYTemp
                aX = aXTemp;
                aY = aYTemp;
                aAlpha = Math.atan2(p2.y - aY, p2.x - aX);
                sin = Math.sin(aAlpha);
                cos = Math.cos(aAlpha);
            }
            double daX = sin * arrowHeight;
            double daY = cos * arrowHeight;
            arrow.reset();
            arrow.addPoint((int) Math.round(aX - daX), (int) Math.round(aY + daY));
            arrow.addPoint((int) Math.round(p2.x), (int) Math.round(p2.y));
            arrow.addPoint((int) Math.round(aX + daX), (int) Math.round(aY - daY));
            g2.fill(arrow);
            g2.draw(arrow);
        }
    }

    /**
     * Rotate
     * <code>p2</code>
     * <code>angle</code> degrees counterclockwise around
     * <code>p1</code>.
     *
     * @param p1
     * @param p2
     * @param angle
     */
    private void rotatePoint(Point2D.Double p1, Point2D.Double p2, double angle) {
        // translate p2 to 0/0
        p2.x -= p1.x;
        p2.y -= p1.y;

        // rotate angle degrees
        double sinAngle = Math.sin(angle);
        double cosAngle = Math.cos(angle);
        double xact = p2.x;
        double yact = p2.y;
        p2.x = xact * cosAngle - yact * sinAngle;
        p2.y = xact * sinAngle + yact * cosAngle;

        // und zurückschieben
        p2.x += p1.x;
        p2.y += p1.y;
    }

    /**
     * Adjust the end points of an arrow: the arrow should start and end outside
     * the circular background of the candidate.
     *
     * @param p1
     * @param p2
     * @param alpha
     * @param ddy
     */
    private void adjustEndPoints(Point2D.Double p1, Point2D.Double p2, double alpha, double ddy) {
        double tmpDelta = ddy / 2.0 + 4.0;
        int pX = (int) (tmpDelta * Math.cos(alpha));
        int pY = (int) (tmpDelta * Math.sin(alpha));
        p1.x += pX;
        p1.y += pY;
        p2.x -= pX;
        p2.y -= pY;
    }

    /**
     * Returns the center of the position of a candidate in the grid.
     *
     * @param index
     * @param cand
     * @param cellSize
     * @return
     */
    private Point2D.Double getCandKoord(int index, int cand, int cellSize) {
        double third = cellSize / 3;
        double startX = getX(Sudoku2.getLine(index), Sudoku2.getCol(index));
        double startY = getY(Sudoku2.getLine(index), Sudoku2.getCol(index));
        double shiftX = ((cand - 1) % 3) * third;
        double shiftY = ((cand - 1) / 3) * third;
        double x = startX + shiftX + third / 2.0;
        double y = startY + shiftY + third / 2.0;
        return new Point2D.Double(x, y);
    }

    private int getX(int line, int col) {
        int x = col * cellSize + delta + startSX;
        if (col > 2) {
            x += delta;
        }
        if (col > 5) {
            x += delta;
        }
        return x;
    }

    private int getY(int line, int col) {
        int y = line * cellSize + delta + startSY;
        if (line > 2) {
            y += delta;
        }
        if (line > 5) {
            y += delta;
        }
        return y;
    }

    private int getLine(Point p) {
        double tmp = p.y - startSY - delta;
        if ((tmp >= 3 * cellSize && tmp <= 3 * cellSize + delta)
                || (tmp >= 6 * cellSize + delta && tmp <= 6 * cellSize + 2 * delta)) {
            return -1;
        }
        if (tmp > 3 * cellSize) {
            tmp -= delta;
        }
        if (tmp > 6 * cellSize) {
            tmp -= delta;
        }
        return (int) Math.ceil((tmp / cellSize) - 1);
    }

    private int getCol(Point p) {
        double tmp = p.x - startSX - delta;
        if ((tmp >= 3 * cellSize && tmp <= 3 * cellSize + delta)
                || (tmp >= 6 * cellSize + delta && tmp <= 6 * cellSize + 2 * delta)) {
            return -1;
        }
        if (tmp > 3 * cellSize) {
            tmp -= delta;
        }
        if (tmp > 6 * cellSize) {
            tmp -= delta;
        }
        return (int) Math.ceil((tmp / cellSize) - 1);
    }

    /**
     * Checks whether a candidate has been clicked. The correct values for font
     * metrics and candidate factors are ignored: the valid candidate region is
     * simple the corresponding ninth of the cell.<br><br>
     *
     * @param p The point of a mouse click
     * @param line The line, in which p lies (may be -1 for "invalid")
     * @param col The column, in which p lies (may be -1 for "invalid")
     * @return The number of a candidate, if a click could be confirmed, or else
     * -1
     */
    private int getCandidate(Point p, int line, int col) {
        // check if a cell was clicked
        if (line < 0 || col < 0) {
            // clicked between cells -> cant mean a candidate
            return -1;
        }
        // calculate the coordinates of the left upper corner of the cell
        //System.out.println("startSX = " + startSX + ", startSY = " + startSY + ", cellSize = " + cellSize + ", delta = " + delta);
        double startX = startSX + col * cellSize;
        if (col > 2) {
            startX += delta;
        }
        if (col > 5) {
            startX += delta;
        }
        double startY = startSY + line * cellSize;
        if (line > 2) {
            startY += delta;
        }
        if (line > 5) {
            startY += delta;
        }
        // now check if a candidate was clicked
        int candidate = -1;
        double cs3 = cellSize / 3.0;
        // dont restrict the area
//        double dx = cs3 * 2.0 / 3.0;
//        double leftDx = cs3 / 6.0;
        double dx = cs3;
        double leftDx = 0;
        //System.out.println("p = " + p + ", startX = " + startX + ", startY = " + startY + ", dx = " + dx + ", leftDX = " + leftDx);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double sx = startX + i * cs3 + leftDx;
                double sy = startY + j * cs3 + leftDx;
                //System.out.println("cand = " + (j * 3 + i + 1) + ", sx = " + sx + ", sy = " + sy);
                if (p.x >= sx && p.x <= sx + dx
                        && p.y >= sy && p.y <= sy + dx) {
                    // canddiate was clicked
                    candidate = j * 3 + i + 1;
                    //System.out.println("Candidate clicked: " + candidate);
                    return candidate;
                }
            }
        }
        return -1;
    }

    public void setActiveColor(int colorNumber) {
        aktColorIndex = colorNumber;
        if (aktColorIndex < 0) {
            // reset everything to normal
            if (oldCursor != null) {
                setCursor(oldCursor);
                colorCursor = null;
                colorCursorShift = null;
            }
        } else {
            // create new Cursors and set them
            if (oldCursor == null) {
                oldCursor = getCursor();
            }
            createColorCursors();
            setCursor(colorCursor);
        }
        // no region selectes are allowed in coloring
        clearRegion();
        updateCellZoomPanel();
    }

    public int getActiveColor() {
        return aktColorIndex;
    }

    public void resetActiveColor() {
        int temp = aktColorIndex;
        setActiveColor(-1);
        setActiveColor(temp);
    }

    private void drawBlockLine(int x, int y, boolean withRect) {
        drawBlock(x, y, withRect);
        drawBlock(x + 3 * cellSize + delta, y, withRect);
        drawBlock(x + 6 * cellSize + 2 * delta, y, withRect);
    }

    private void drawBlock(int x, int y, boolean withRect) {
        if (withRect) {
            g2.drawRect(x, y, 3 * cellSize, 3 * cellSize);
        }
        g2.drawLine(x, y + 1 * cellSize, x + 3 * cellSize, y + 1 * cellSize);
        g2.drawLine(x, y + 2 * cellSize, x + 3 * cellSize, y + 2 * cellSize);
        g2.drawLine(x + 1 * cellSize, y, x + 1 * cellSize, y + 3 * cellSize);
        g2.drawLine(x + 2 * cellSize, y, x + 2 * cellSize, y + 3 * cellSize);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPopupMenu cellPopupMenu;
    private javax.swing.JMenuItem color1aMenuItem;
    private javax.swing.JMenuItem color1bMenuItem;
    private javax.swing.JMenuItem color2aMenuItem;
    private javax.swing.JMenuItem color2bMenuItem;
    private javax.swing.JMenuItem color3aMenuItem;
    private javax.swing.JMenuItem color3bMenuItem;
    private javax.swing.JMenuItem color4aMenuItem;
    private javax.swing.JMenuItem color4bMenuItem;
    private javax.swing.JMenuItem color5aMenuItem;
    private javax.swing.JMenuItem color5bMenuItem;
    private javax.swing.JMenuItem deleteValueMenuItem;
    private javax.swing.JPopupMenu deleteValuePopupMenu;
    private javax.swing.JMenuItem exclude1MenuItem;
    private javax.swing.JMenuItem exclude2MenuItem;
    private javax.swing.JMenuItem exclude3MenuItem;
    private javax.swing.JMenuItem exclude4MenuItem;
    private javax.swing.JMenuItem exclude5MenuItem;
    private javax.swing.JMenuItem exclude6MenuItem;
    private javax.swing.JMenuItem exclude7MenuItem;
    private javax.swing.JMenuItem exclude8MenuItem;
    private javax.swing.JMenuItem exclude9MenuItem;
    private javax.swing.JMenuItem excludeSeveralMenuItem;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JMenuItem make1MenuItem;
    private javax.swing.JMenuItem make2MenuItem;
    private javax.swing.JMenuItem make3MenuItem;
    private javax.swing.JMenuItem make4MenuItem;
    private javax.swing.JMenuItem make5MenuItem;
    private javax.swing.JMenuItem make6MenuItem;
    private javax.swing.JMenuItem make7MenuItem;
    private javax.swing.JMenuItem make8MenuItem;
    private javax.swing.JMenuItem make9MenuItem;
    // End of variables declaration//GEN-END:variables

    public Sudoku2 getSudoku() {
        return sudoku;
    }

    public boolean isShowCandidates() {
        return showCandidates;
    }

    public final void setShowCandidates(boolean showCandidates) {
        this.showCandidates = showCandidates;
        repaint();
    }

    public boolean isShowWrongValues() {
        return showWrongValues;
    }

    public void setShowWrongValues(boolean showWrongValues) {
        this.showWrongValues = showWrongValues;
        repaint();
    }

    public boolean undoPossible() {
        //System.out.println("undoStack: " + undoStack + "/" + undoStack.size());
        return undoStack.size() > 0;
    }

    public boolean redoPossible() {
        //System.out.println("redoStack: " + redoStack + "/" + redoStack.size());
        return redoStack.size() > 0;
    }

    public void undo() {
        if (undoPossible()) {
            redoStack.push(sudoku);
            sudoku = undoStack.pop();
            updateCellZoomPanel();
            checkProgress();
            mainFrame.setCurrentLevel(sudoku.getLevel());
            mainFrame.setCurrentScore(sudoku.getScore());
            mainFrame.check();
            repaint();
        }
    }

    public void redo() {
        if (redoPossible()) {
            undoStack.push(sudoku);
            sudoku = redoStack.pop();
            updateCellZoomPanel();
            checkProgress();
            mainFrame.setCurrentLevel(sudoku.getLevel());
            mainFrame.setCurrentScore(sudoku.getScore());
            mainFrame.check();
            repaint();
        }
    }

    /**
     * Clears undo/redo. Is called from {@link SolutionPanel} when a step is
     * double clicked.
     */
    public void clearUndoRedo() {
        undoStack.clear();
        redoStack.clear();
    }

    public void setSudoku(Sudoku2 newSudoku) {
        setSudoku(newSudoku.getSudoku(ClipboardMode.PM_GRID, null), false);
    }

    public void setSudoku(Sudoku2 newSudoku, boolean alreadySolved) {
        setSudoku(newSudoku.getSudoku(ClipboardMode.PM_GRID, null), alreadySolved);
    }

    public void setSudoku(String init) {
        setSudoku(init, false);
    }

    public void setSudoku(String init, boolean alreadySolved) {
        step = null;
        setChainInStep(-1);
        undoStack.clear();
        redoStack.clear();
        coloringMap.clear();
        resetShowHintCellValues();
        if (init == null || init.length() == 0) {
            sudoku = new Sudoku2();
        } else {
            sudoku.setSudoku(init);
            // the sudoku must be set in the solver to reset the step list
            // (otherwise the result panels are not updated correctly)
            sudoku.setLevel(Options.getInstance().getDifficultyLevels()[DifficultyType.EASY.ordinal()]);
            sudoku.setScore(0);
            Sudoku2 tmpSudoku = sudoku.clone();
            if (!alreadySolved) {
                getSolver().setSudoku(tmpSudoku);
            }
            //boolean unique = generator.validSolution(sudoku);
            int anzSolutions = generator.getNumberOfSolutions(sudoku);
            if (anzSolutions == 0) {
                JOptionPane.showMessageDialog(this,
                        java.util.ResourceBundle.getBundle("intl/SudokuPanel").getString("SudokuPanel.no_solution"),
                        java.util.ResourceBundle.getBundle("intl/SudokuPanel").getString("SudokuPanel.invalid_puzzle"),
                        JOptionPane.ERROR_MESSAGE);
                sudoku.setStatus(SudokuStatus.INVALID);
            } else if (anzSolutions > 1) {
                JOptionPane.showMessageDialog(this,
                        java.util.ResourceBundle.getBundle("intl/SudokuPanel").getString("SudokuPanel.multiple_solutions"),
                        java.util.ResourceBundle.getBundle("intl/SudokuPanel").getString("SudokuPanel.invalid_puzzle"),
                        JOptionPane.ERROR_MESSAGE);
                sudoku.setStatus(SudokuStatus.MULTIPLE_SOLUTIONS);
            } else {
                if (!sudoku.checkSudoku()) {
                    JOptionPane.showMessageDialog(this,
                            java.util.ResourceBundle.getBundle("intl/SudokuPanel").getString("SudokuPanel.wrong_values"),
                            java.util.ResourceBundle.getBundle("intl/SudokuPanel").getString("SudokuPanel.invalid_puzzle"),
                            JOptionPane.ERROR_MESSAGE);
                    sudoku.setStatus(SudokuStatus.INVALID);
                } else {
                    sudoku.setStatus(SudokuStatus.VALID);
                    if (sudoku.getFixedCellsAnz() > 17) {
                        Sudoku2 fixedOnly = new Sudoku2();
                        fixedOnly.setSudoku(sudoku.getSudoku(ClipboardMode.CLUES_ONLY));
                        int anzFixedSol = generator.getNumberOfSolutions(fixedOnly);
                        sudoku.setStatusGivens(anzFixedSol);
                    }

                    if (!alreadySolved) {
                        //Sudoku tmpSudoku = sudoku.clone();
                        //getSolver().setSudoku(tmpSudoku);
                        tmpSudoku.setStatus(SudokuStatus.VALID);
                        tmpSudoku.setStatusGivens(sudoku.getStatusGivens());
                        tmpSudoku.setSolution(sudoku.getSolution());
                        getSolver().solve(true);
                    }
//                sudoku.setLevel(tmpSudoku.getLevel());
//                sudoku.setScore(tmpSudoku.getScore());
                    sudoku.setLevel(getSolver().getSudoku().getLevel());
                    sudoku.setScore(getSolver().getSudoku().getScore());
                }
            }
        }
        updateCellZoomPanel();
        if (mainFrame != null) {
            mainFrame.setCurrentLevel(sudoku.getLevel());
            mainFrame.setCurrentScore(sudoku.getScore());
            mainFrame.check();
        }
        repaint();
    }

    public String getSudokuString(ClipboardMode mode) {
        return sudoku.getSudoku(mode, step);
    }

    public SudokuSolver getSolver() {
        return solver;
    }

    /**
     * Solves the sudoku to a certain point: if game mode is playing, the sudoku
     * is solved until the first non progress step is reached; in all other
     * modes the solving stops, when the first training step has been reached.
     */
    public void solveUpTo() {
        SolutionStep actStep = null;
        boolean changed = false;
        undoStack.push(sudoku.clone());
        GameMode gm = Options.getInstance().getGameMode();
        while ((actStep = solver.getHint(sudoku, false)) != null) {
            if (gm == GameMode.PLAYING) {
                if (!actStep.getType().getStepConfig().isEnabledProgress()) {
                    // solving stops
                    break;
                }
            } else {
                if (actStep.getType().getStepConfig().isEnabledTraining()) {
                    // solving stops
                    break;
                }
            }
            // still here? do the step
            getSolver().doStep(sudoku, actStep);
            changed = true;
        }
        if (changed) {
            redoStack.clear();
        } else {
            undoStack.pop();
        }
        step = null;
        setChainInStep(-1);
        updateCellZoomPanel();
        checkProgress();
        mainFrame.check();
        repaint();
    }

    /**
     * When solving manually, the sudoku can be in an invalid state. This should
     * be handled before calling this method.
     *
     * @param singlesOnly
     * @return
     */
    public SolutionStep getNextStep(boolean singlesOnly) {
        step = solver.getHint(sudoku, singlesOnly);
        setChainInStep(-1);
        repaint();
        return step;
    }

    public void setStep(SolutionStep step) {
        this.step = step;
        setChainInStep(-1);
        repaint();
    }

    public SolutionStep getStep() {
        return step;
    }

    public void setChainInStep(int chainIndex) {
        if (step == null) {
            chainIndex = -1;
        } else if (step.getType().isKrakenFish() && chainIndex > -1) {
            chainIndex--;
        }
        if (chainIndex >= 0 && chainIndex > step.getChainAnz() - 1) {
            chainIndex = -1;
        }
        //System.out.println("chainIndex = " + chainIndex);
        this.chainIndex = chainIndex;
        alsToShow.clear();
        if (chainIndex != -1) {
            Chain chain = step.getChains().get(chainIndex);
            for (int i = chain.getStart(); i <= chain.getEnd(); i++) {
                if (chain.getNodeType(i) == Chain.ALS_NODE) {
                    alsToShow.add(Chain.getSAlsIndex(chain.getChain()[i]));
                }
            }
        }
//        StringBuffer tmp = new StringBuffer();
//        tmp.append("setChainInStep(" + chainIndex + "): ");
//        for (int i = 0; i < alsToShow.size(); i++) {
//            tmp.append(alsToShow.get(i) + " ");
//        }
//        System.out.println(tmp);
        repaint();
    }

    public void doStep() {
        if (step != null) {
            undoStack.push(sudoku.clone());
            redoStack.clear();
            getSolver().doStep(sudoku, step);
            step = null;
            setChainInStep(-1);
            updateCellZoomPanel();
            checkProgress();
            mainFrame.check();
            repaint();
            if (sudoku.isSolved() && Options.getInstance().isShowSudokuSolved()) {
                JOptionPane.showMessageDialog(this, java.util.ResourceBundle.getBundle("intl/MainFrame").getString("MainFrame.sudoku_solved"),
                        java.util.ResourceBundle.getBundle("intl/MainFrame").getString("MainFrame.congratulations"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void abortStep() {
        step = null;
        setChainInStep(-1);
        repaint();
    }

    public int getSolvedCellsAnz() {
        return sudoku.getSolvedCellsAnz();
    }

    public void setNoClues() {
        sudoku.setNoClues();
        repaint();
    }

    public boolean isInvalidCells() {
        return invalidCells;
    }

    public void setInvalidCells(boolean invalidCells) {
        this.invalidCells = invalidCells;
    }

    public boolean isShowInvalidOrPossibleCells() {
        return showInvalidOrPossibleCells;
    }

    public void setShowInvalidOrPossibleCells(boolean showInvalidOrPossibleCells) {
        this.showInvalidOrPossibleCells = showInvalidOrPossibleCells;
    }

    public boolean[] getShowHintCellValues() {
        return showHintCellValues;
    }

    public void setShowHintCellValues(boolean[] showHintCellValues) {
        this.showHintCellValues = showHintCellValues;
    }

    public void setShowHintCellValue(int candidate) {
        if (candidate == 10) {
            // filter bivalue cells
            for (int i = 0; i < showHintCellValues.length - 1; i++) {
                showHintCellValues[i] = false;
            }
            showHintCellValues[10] = !showHintCellValues[10];
        } else {
            showHintCellValues[10] = false;
            for (int i = 0; i < showHintCellValues.length - 1; i++) {
                if (i == candidate) {
                    showHintCellValues[i] = !showHintCellValues[i];
                } else {
                    showHintCellValues[i] = false;
                }
            }
        }
    }

    public void resetShowHintCellValues() {
        for (int i = 1; i < showHintCellValues.length; i++) {
            showHintCellValues[i] = false;
        }
        showInvalidOrPossibleCells = false;
    }

    public boolean isShowDeviations() {
        return showDeviations;
    }

    public void setShowDeviations(boolean showDeviations) {
        this.showDeviations = showDeviations;
        mainFrame.check();
        repaint();
    }

    /**
     * Schreibt ein BufferedImage in eine PNG-Datei. Dabei wird die Auflösung
     * in die Metadaten der Datei geschrieben, was alles etwas kompliziert
     * macht.
     *
     * @param bi Zu zeichnendes Bild
     * @param dpi Auflösung in dots per inch
     * @param fileName Pfad und Name der neuen Bilddatei
     */
    private void writePNG(BufferedImage bi, int dpi, File file) {
        Iterator<ImageWriter> i = ImageIO.getImageWritersByFormatName("png");
        //are there any jpeg encoders available?

        if (i.hasNext()) //there's at least one ImageWriter, just use the first one
        {
            ImageWriter imageWriter = i.next();
            //get the param
            ImageWriteParam param = imageWriter.getDefaultWriteParam();
            ImageTypeSpecifier its = new ImageTypeSpecifier(bi.getColorModel(), bi.getSampleModel());

            //get metadata
            IIOMetadata iomd = imageWriter.getDefaultImageMetadata(its, param);

            String formatName = "javax_imageio_png_1.0";//this is the DOCTYPE of the metadata we need

            Node node = iomd.getAsTree(formatName);

            // standardmäßig ist nur IHDR gesetzt, pHYs dazufügen
            int dpiRes = (int) (dpi / 2.54 * 100);
            IIOMetadataNode res = new IIOMetadataNode("pHYs");
            res.setAttribute("pixelsPerUnitXAxis", String.valueOf(dpiRes));
            res.setAttribute("pixelsPerUnitYAxis", String.valueOf(dpiRes));
            res.setAttribute("unitSpecifier", "meter");
            node.appendChild(res);

            try {
                iomd.setFromTree(formatName, node);
            } catch (IIOInvalidTreeException e) {
                JOptionPane.showMessageDialog(this, e.getLocalizedMessage(),
                        java.util.ResourceBundle.getBundle("intl/SudokuPanel").getString("SudokuPanel.error"),
                        JOptionPane.ERROR_MESSAGE);
            }
            //attach the metadata to an image
            IIOImage iioimage = new IIOImage(bi, null, iomd);
            try {
                FileImageOutputStream out = new FileImageOutputStream(file);
                imageWriter.setOutput(out);
                imageWriter.write(iioimage);
                out.close();

                String companionFileName = file.getPath();
                if (companionFileName.toLowerCase().endsWith(".png")) {
                    companionFileName = companionFileName.substring(0, companionFileName.length() - 4);
                }
                companionFileName += ".txt";
                PrintWriter cOut = new PrintWriter(new BufferedWriter(new FileWriter(companionFileName)));
                cOut.println(getSudokuString(ClipboardMode.CLUES_ONLY));
                cOut.println(getSudokuString(ClipboardMode.LIBRARY));
                cOut.println(getSudokuString(ClipboardMode.PM_GRID));
                if (step != null) {
                    cOut.println(getSudokuString(ClipboardMode.PM_GRID_WITH_STEP));
                }
                cOut.close();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, e.getLocalizedMessage(),
                        java.util.ResourceBundle.getBundle("intl/SudokuPanel").getString("SudokuPanel.error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * @return the colorCells
     */
    public boolean isColorCells() {
        return colorCells;
    }

    /**
     * @param colorCells the colorCells to set
     */
    public void setColorCells(boolean colorCells) {
        this.colorCells = colorCells;
        Options.getInstance().setColorCells(colorCells);
        updateCellZoomPanel();
    }

    /**
     * Creates cursors for coloring: The color is specified by {@link #aktColorIndex},
     * cursors for both colors of the pair are created and stored in {@link #colorCursor}
     * and {@link #colorCursorShift}.
     */
    private void createColorCursors() {
        try {
            Point cursorHotSpot = new Point(2, 4);
            BufferedImage img1 = ImageIO.read(getClass().getResource("/img/c_color.png"));
            Graphics2D gImg1 = (Graphics2D) img1.getGraphics();
            gImg1.setColor(Options.getInstance().getColoringColors()[aktColorIndex]);
            gImg1.fillRect(19, 18, 12, 12);
            //System.out.println(aktColorIndex + "/" + Options.getInstance().coloringColors[aktColorIndex]);
            colorCursor = Toolkit.getDefaultToolkit().createCustomCursor(img1, cursorHotSpot, "c_strong");

            BufferedImage img2 = ImageIO.read(getClass().getResource("/img/c_color.png"));
            Graphics2D gImg2 = (Graphics2D) img2.getGraphics();
            if (aktColorIndex % 2 == 0) {
                gImg2.setColor(Options.getInstance().getColoringColors()[aktColorIndex + 1]);
            } else {
                gImg2.setColor(Options.getInstance().getColoringColors()[aktColorIndex - 1]);
            }
            //System.out.println(aktColorIndex + "/" + Options.getInstance().coloringColors[aktColorIndex + 1]);
            gImg2.fillRect(19, 18, 12, 12);
            colorCursorShift = Toolkit.getDefaultToolkit().createCustomCursor(img2, cursorHotSpot, "c_weak");
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error creating color cursors", ex);
        }
    }

    /**
     * Checks whether the candidate in the given cell is a Hidden Single.
     *
     * @param candidate
     * @param line
     * @param col
     * @return
     */
    private boolean isHiddenSingle(int candidate, int line, int col) {
        // sometimes the internal singles queues get corrupted
        sudoku.rebuildInternalData();
        SudokuStepFinder finder = SudokuSolverFactory.getDefaultSolverInstance().getStepFinder();
        List<SolutionStep> steps = finder.findAllHiddenSingles(sudoku);
        for (SolutionStep act : steps) {
            if (act.getType() == SolutionType.HIDDEN_SINGLE && act.getValues().get(0) == candidate
                    && act.getIndices().get(0) == Sudoku2.getIndex(line, col)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets all the color icons in the popup menu.
     */
    public final void setColorIconsInPopupMenu() {
        setColorIconInPopupMenu(color1aMenuItem, Options.getInstance().getColoringColors()[0], false);
        setColorIconInPopupMenu(color1bMenuItem, Options.getInstance().getColoringColors()[1], false);
        setColorIconInPopupMenu(color2aMenuItem, Options.getInstance().getColoringColors()[2], false);
        setColorIconInPopupMenu(color2bMenuItem, Options.getInstance().getColoringColors()[3], false);
        setColorIconInPopupMenu(color3aMenuItem, Options.getInstance().getColoringColors()[4], false);
        setColorIconInPopupMenu(color3bMenuItem, Options.getInstance().getColoringColors()[5], false);
        setColorIconInPopupMenu(color4aMenuItem, Options.getInstance().getColoringColors()[6], false);
        setColorIconInPopupMenu(color4bMenuItem, Options.getInstance().getColoringColors()[7], false);
        setColorIconInPopupMenu(color5aMenuItem, Options.getInstance().getColoringColors()[8], false);
        setColorIconInPopupMenu(color5bMenuItem, Options.getInstance().getColoringColors()[9], false);
    }

    public void setColorkuInPopupMenu(boolean on) {
        if (on) {
            setColorIconInPopupMenu(make1MenuItem, Options.getInstance().getColorKuColor(1), true);
            setColorIconInPopupMenu(make2MenuItem, Options.getInstance().getColorKuColor(2), true);
            setColorIconInPopupMenu(make3MenuItem, Options.getInstance().getColorKuColor(3), true);
            setColorIconInPopupMenu(make4MenuItem, Options.getInstance().getColorKuColor(4), true);
            setColorIconInPopupMenu(make5MenuItem, Options.getInstance().getColorKuColor(5), true);
            setColorIconInPopupMenu(make6MenuItem, Options.getInstance().getColorKuColor(6), true);
            setColorIconInPopupMenu(make7MenuItem, Options.getInstance().getColorKuColor(7), true);
            setColorIconInPopupMenu(make8MenuItem, Options.getInstance().getColorKuColor(8), true);
            setColorIconInPopupMenu(make9MenuItem, Options.getInstance().getColorKuColor(9), true);

            setColorIconInPopupMenu(exclude1MenuItem, Options.getInstance().getColorKuColor(1), true);
            setColorIconInPopupMenu(exclude2MenuItem, Options.getInstance().getColorKuColor(2), true);
            setColorIconInPopupMenu(exclude3MenuItem, Options.getInstance().getColorKuColor(3), true);
            setColorIconInPopupMenu(exclude4MenuItem, Options.getInstance().getColorKuColor(4), true);
            setColorIconInPopupMenu(exclude5MenuItem, Options.getInstance().getColorKuColor(5), true);
            setColorIconInPopupMenu(exclude6MenuItem, Options.getInstance().getColorKuColor(6), true);
            setColorIconInPopupMenu(exclude7MenuItem, Options.getInstance().getColorKuColor(7), true);
            setColorIconInPopupMenu(exclude8MenuItem, Options.getInstance().getColorKuColor(8), true);
            setColorIconInPopupMenu(exclude9MenuItem, Options.getInstance().getColorKuColor(9), true);

            excludeSeveralMenuItem.setEnabled(false);
        } else {
            setColorIconInPopupMenu(make1MenuItem, null, false);
            setColorIconInPopupMenu(make2MenuItem, null, false);
            setColorIconInPopupMenu(make3MenuItem, null, false);
            setColorIconInPopupMenu(make4MenuItem, null, false);
            setColorIconInPopupMenu(make5MenuItem, null, false);
            setColorIconInPopupMenu(make6MenuItem, null, false);
            setColorIconInPopupMenu(make7MenuItem, null, false);
            setColorIconInPopupMenu(make8MenuItem, null, false);
            setColorIconInPopupMenu(make9MenuItem, null, false);

            setColorIconInPopupMenu(exclude1MenuItem, null, false);
            setColorIconInPopupMenu(exclude2MenuItem, null, false);
            setColorIconInPopupMenu(exclude3MenuItem, null, false);
            setColorIconInPopupMenu(exclude4MenuItem, null, false);
            setColorIconInPopupMenu(exclude5MenuItem, null, false);
            setColorIconInPopupMenu(exclude6MenuItem, null, false);
            setColorIconInPopupMenu(exclude7MenuItem, null, false);
            setColorIconInPopupMenu(exclude8MenuItem, null, false);
            setColorIconInPopupMenu(exclude9MenuItem, null, false);

            excludeSeveralMenuItem.setEnabled(true);
        }
    }

    /**
     * Creates an icon (rectangle showing color) and sets it on the MenuItem.
     *
     * @param item
     * @param color
     */
    private void setColorIconInPopupMenu(JMenuItem item, Color color, boolean colorKu) {
        if (color == null) {
            // delete the icon
            item.setIcon(null);
            return;
        }
        try {
            BufferedImage img = null;
            if (colorKu) {
                img = new ColorKuImage(12, color);
            } else {
                img = ImageIO.read(getClass().getResource("/img/c_icon.png"));
                Graphics2D gImg = (Graphics2D) img.getGraphics();
                gImg.setColor(color);
                gImg.fillRect(1, 1, 12, 12);
            }
            item.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error setting color icons in popup menu", ex);
        }
    }

    /**
     * Collects the intersection or union of all valid candidates in all
     * selected cells. Used to adjust the popup menu.
     *
     * @param intersection
     * @return
     */
    private SudokuSet collectCandidates(boolean intersection) {
        SudokuSet resultSet = new SudokuSet();
        SudokuSet tmpSet = new SudokuSet();
        if (intersection) {
            resultSet.setAll();
        }
        if (selectedCells.isEmpty()) {
            if (sudoku.getValue(aktLine, aktCol) == 0) {
                // get candidates only when cell is not set!
                sudoku.getCandidateSet(aktLine, aktCol, tmpSet);
                if (intersection) {
                    resultSet.and(tmpSet);
                } else {
                    resultSet.or(tmpSet);
                }
            }
        } else {
            // BUG: if all cells in the selection are set,
            // all candidates becomevalid for intersection == true
            boolean emptyCellsOnly = true;
            for (int index : selectedCells) {
                if (sudoku.getValue(index) == 0) {
                    emptyCellsOnly = false;
                    // get candidates only when cell is not set!
                    sudoku.getCandidateSet(index, tmpSet);
                    if (intersection) {
                        resultSet.and(tmpSet);
                    } else {
                        resultSet.or(tmpSet);
                    }
                }
            }
            if (intersection && emptyCellsOnly) {
                resultSet.clear();
            }
        }
        return resultSet;
    }

    /**
     * Brings up the popup menu for the cell at line/col. If the cell is already
     * set, a different menu is displayed, that allowsto delete the value from
     * the cell. For every other cell the contents of the menu is restricted to
     * sensible actions.<br> If a region of cells is selected, "Make x" is
     * restricted to candidates, that appear in all cells, "Exclude x" is
     * restricted to the combined set of candidates in all cells.
     *
     * @param line
     * @param col
     */
    private void showPopupMenu(int line, int col) {
        jSeparator2.setVisible(true);
        if (sudoku.getValue(line, col) != 0 && selectedCells.isEmpty()) {
            // cell is already set -> delete value popup (not for givens!)
            if (!sudoku.isFixed(aktLine, aktCol)) {
                setAktRowCol(line, col);
                deleteValuePopupMenu.show(this, getX(line, col) + cellSize, getY(line, col));
            }
            return;
        }
        if (selectedCells.isEmpty()) {
            setAktRowCol(line, col);
        }
        excludeSeveralMenuItem.setVisible(false);
        for (int i = 1; i <= 9; i++) {
            makeItems[i - 1].setVisible(false);
            excludeItems[i - 1].setVisible(false);
        }
        SudokuSet candSet = collectCandidates(true);
        for (int i = 0; i < candSet.size(); i++) {
            makeItems[candSet.get(i) - 1].setVisible(true);
        }
        candSet = collectCandidates(false);
        if (candSet.size() > 1) {
            if (candSet.size() > 2) {
                excludeSeveralMenuItem.setVisible(true);
            }
            for (int i = 0; i < candSet.size(); i++) {
                excludeItems[candSet.get(i) - 1].setVisible(true);
            }
        } else {
            jSeparator2.setVisible(false);
        }
        cellPopupMenu.show(this, getX(line, col) + cellSize, getY(line, col));
    }

    /**
     * Handles activation of a "Make x" menu item. The selected number is set in
     * all selected cells (if they are not already set).
     *
     * @param menuItem
     */
    private void popupSetCell(JMenuItem menuItem) {
        int candidate = -1;
        for (int i = 0; i < makeItems.length; i++) {
            if (makeItems[i] == menuItem) {
                candidate = i + 1;
                break;
            }
        }
        if (candidate != -1) {
            undoStack.push(sudoku.clone());
            boolean changed = false;
            if (selectedCells.isEmpty()) {
                if (sudoku.getValue(aktLine, aktCol) == 0) {
                    setCell(aktLine, aktCol, candidate);
                    changed = true;
                }
            } else {
                for (int index : selectedCells) {
                    if (sudoku.getValue(index) == 0) {
                        sudoku.setCell(index, candidate);
                        changed = true;
                    }
                }
            }
            if (changed) {
                redoStack.clear();
                checkProgress();
            } else {
                undoStack.pop();
            }
            updateCellZoomPanel();
            mainFrame.check();
            mainFrame.fixFocus();
            repaint();
        }
    }

    /**
     * Deletes the value from the active cell.
     */
    private void popupDeleteValueFromCell() {
//        System.out.println("delete valuefrom " + aktLine+ "/"+aktCol);
        undoStack.push(sudoku.clone());
        boolean changed = false;
        if (sudoku.getValue(aktLine, aktCol) != 0 && !sudoku.isFixed(aktLine, aktCol)) {
//            System.out.println("clear cell: ");
            sudoku.setCell(aktLine, aktCol, 0);
            changed = true;
        }
        if (changed) {
            // Undo wurde schon behandelt, Redo ist nicht mehr möglich
            redoStack.clear();
            checkProgress();
        } else {
            // kein Undo nötig -> wieder entfernen
            undoStack.pop();
        }
        updateCellZoomPanel();
        mainFrame.fixFocus();
        mainFrame.check();
        repaint();
    }

    /**
     * Removes the candidate from all selected cells.
     *
     * @param candidate
     * @return true if sudoku is changed, false otherwise
     */
    private boolean removeCandidateFromActiveCells(int candidate) {
        boolean changed = false;
        if (selectedCells.isEmpty()) {
            int index = Sudoku2.getIndex(aktLine, aktCol);
            if (sudoku.getValue(index) == 0 && sudoku.isCandidate(index, candidate, !showCandidates)) {
                sudoku.setCandidate(index, candidate, false, !showCandidates);
                changed = true;
            }
        } else {
            for (int index : selectedCells) {
                if (sudoku.getValue(index) == 0 && sudoku.isCandidate(index, candidate, !showCandidates)) {
                    sudoku.setCandidate(index, candidate, false, !showCandidates);
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Handles candidate changed done in {@link CellZoomPanel}. Should not be
     * used otherwise.
     *
     * @param candidate
     */
    public void toggleOrRemoveCandidateFromCellZoomPanel(int candidate) {
        if (candidate != -1) {
            undoStack.push(sudoku.clone());
            boolean changed = false;
            if (selectedCells.isEmpty()) {
                int index = Sudoku2.getIndex(aktLine, aktCol);
                if (sudoku.isCandidate(index, candidate, !showCandidates)) {
                    sudoku.setCandidate(index, candidate, false, !showCandidates);
                } else {
                    sudoku.setCandidate(index, candidate, true, !showCandidates);
                }
                changed = true;
            } else {
                changed = removeCandidateFromActiveCells(candidate);
            }
            if (changed) {
                redoStack.clear();
                checkProgress();
            } else {
                undoStack.pop();
            }
            updateCellZoomPanel();
            mainFrame.check();
            repaint();
        }
    }

    /**
     * Handles activation of an "Exclude x" menu item. The selected number is
     * deleted in all selected cells (if they present).
     *
     * @param menuItem
     */
    private void popupExcludeCandidate(JMenuItem menuItem) {
        int candidate = -1;
        for (int i = 0; i < excludeItems.length; i++) {
            if (excludeItems[i] == menuItem) {
                candidate = i + 1;
                break;
            }
        }
        if (candidate != -1) {
            undoStack.push(sudoku.clone());
            boolean changed = removeCandidateFromActiveCells(candidate);
            if (changed) {
                redoStack.clear();
                checkProgress();
            } else {
                undoStack.pop();
            }
            updateCellZoomPanel();
            mainFrame.check();
            mainFrame.fixFocus();
            repaint();
        }
    }

    /**
     * Handles activation of an "Toggle color x" menu item. Th color is set in
     * the cell if not present or deleted if already present.
     *
     * @param menuItem
     */
    private void popupToggleColor(JMenuItem menuItem) {
        int color = -1;
        for (int i = 0; i < toggleColorItems.length; i++) {
            if (toggleColorItems[i] == menuItem) {
                color = i;
                break;
            }
        }
        if (color != -1) {
            //removeCandidateFromActiveCells(color);
            // coloring is active
            handleColoring(aktLine, aktCol, -1, color);
            updateCellZoomPanel();
            mainFrame.check();
            repaint();
        }
    }

    /**
     * @return the cellZoomPanel
     */
    public CellZoomPanel getCellZoomPanel() {
        return cellZoomPanel;
    }

    /**
     * @param cellZoomPanel the cellZoomPanel to set
     */
    public void setCellZoomPanel(CellZoomPanel cellZoomPanel) {
        this.cellZoomPanel = cellZoomPanel;
    }

    /**
     * Update the {@link CellZoomPanel}. For more information see
     * {@link CellZoomPanel#update(sudoku.SudokuSet, sudoku.SudokuSet, int, boolean, java.util.SortedMap, java.util.SortedMap) CellZoomPanel.update()}.
     */
    private void updateCellZoomPanel() {
        if (cellZoomPanel != null) {
            int index = Sudoku2.getIndex(aktLine, aktCol);
            boolean singleCell = selectedCells.isEmpty() && sudoku.getValue(index) == 0;
            if (aktColorIndex == -1) {
                // normal operation -> collect candidates for selected cell(s)
                if (sudoku.getValue(index) != 0 && selectedCells.isEmpty()) {
                    // cell is already set -> nothing can be selected
                    cellZoomPanel.update(SudokuSetBase.EMPTY_SET, SudokuSetBase.EMPTY_SET, -1, index, false, singleCell, null, null);
                } else {
                    SudokuSet valueSet = collectCandidates(true);
                    SudokuSet candSet = collectCandidates(false);
                    cellZoomPanel.update(valueSet, candSet, -1, index, false, singleCell, null, null);
                }
            } else {
                if (!selectedCells.isEmpty() || (selectedCells.isEmpty() && sudoku.getValue(index) != 0)) {
                    // no coloring, when set of cells is selected
                    cellZoomPanel.update(SudokuSetBase.EMPTY_SET, SudokuSetBase.EMPTY_SET, aktColorIndex, index, colorCells, singleCell, null, null);
                } else {
                    SudokuSet valueSet = collectCandidates(true);
                    SudokuSet candSet = collectCandidates(false);
                    cellZoomPanel.update(valueSet, candSet, aktColorIndex, index, colorCells, singleCell, coloringMap, coloringCandidateMap);
                }
            }
        }
    }

    /**
     * Gets a 81 character string. For every digit in that string, the
     * corresponding cell is set as a given.
     *
     * @param givens
     */
    public void setGivens(String givens) {
        undoStack.push(sudoku.clone());
        sudoku.setGivens(givens);
        updateCellZoomPanel();
        repaint();
        mainFrame.check();
    }

    /**
     * Checks the progress of the current {@link #sudoku}. If the Sudoku is not
     * yet valid, only the status of the sudoku is updated. If it is valid, a
     * background progress check is scheduled.<br>
     */
    public void checkProgress() {
        int anz = sudoku.getSolvedCellsAnz();
//        System.out.println("  checkProgress() - anz: " + anz);
        if (anz == 0) {
            sudoku.setStatus(SudokuStatus.EMPTY);
            sudoku.setStatusGivens(SudokuStatus.EMPTY);
        } else if (anz <= 17) {
            sudoku.setStatus(SudokuStatus.INVALID);
            sudoku.setStatusGivens(SudokuStatus.INVALID);
        } else {
            // we have to check!
            int anzSol = generator.getNumberOfSolutions(sudoku);
//            System.out.println("  checkProgress() - anzSol: " + anzSol);
            sudoku.setStatus(anzSol);
            // the status of the givens is not changed here; it only changes
            // when the givens themselved are changed
            //sudoku.setStatusGivens(anzSol);
            if (anzSol == 1) {
                // the sudoku is valid -> check the progress
                progressChecker.startCheck(sudoku);
            }
        }
    }

    /**
     * Checks the array {@link #showHintCellValues}. If only one value is
     * selected, that value is returned. If no or more than one values are
     * selected, 0 is returned.
     *
     * @return
     */
    private int getShowHintCellValue() {
        int value = 0;
        for (int i = 1; i < showHintCellValues.length - 1; i++) {
            if (showHintCellValues[i]) {
                if (value == 0) {
                    value = i;
                } else {
                    // more than one value
                    return 0;
                }
            }
        }
        return value;
    }

    public void checkIsShowInvalidOrPossibleCells() {
        showInvalidOrPossibleCells = false;
        for (int i = 1; i < showHintCellValues.length; i++) {
            if (showHintCellValues[i]) {
                showInvalidOrPossibleCells = true;
            }
        }
    }

    public void setShowColorKu() {
        setColorkuInPopupMenu(Options.getInstance().isShowColorKuAct());
        cellZoomPanel.calculateLayout();
        updateCellZoomPanel();
        repaint();
    }

    public void resetColorKuImages() {
        for (int i = 0; i < colorKuImagesLarge.length; i++) {
            colorKuImagesLarge[i] = null;
            colorKuImagesSmall[i] = null;
        }
    }

    private void drawColorBox(int n, Graphics gc, int cx, int cy, int boxSize, boolean large) {
        BufferedImage[] images = null;
        if (large) {
            images = colorKuImagesLarge;
        } else {
            images = colorKuImagesSmall;
        }
        if (images[0] == null || images[0].getWidth() != boxSize) {
            for (int i = 0; i < images.length; i++) {
                images[i] = new ColorKuImage(boxSize, Options.getInstance().getColorKuColor(i + 1));
            }
        }
//        double drawFactor = 0.9;
//        int rand = (int)(boxSize * (1.0 - drawFactor));
//        Color cc = Options.getInstance().getColorKuColor(n);
//        Color old = gc.getColor();
//        gc.setColor(cc);
//        gc.fillOval(cx + rand / 2, cy + rand / 2, boxSize - rand, boxSize - rand);
//        gc.fillRect(cx + delta / 2, cy + delta / 2, boxSize - delta, boxSize - delta);
        gc.drawImage(images[n - 1], cx, cy, null);
//        gc.setColor(old);
    }

    /**
     * Returns an array, that controls the display of the filter buttons in the
     * toolbar. For every candidate, that is still present as candidate and thus
     * can be filtered, the appropriate array element is
     * <code>true</code>.<br><br>
     *
     * Care has to be taken with prerequisites: <ul> <li>If "Show all
     * candidates" is disabled, filtering is not possible</li> <li>...</li>
     * </ul>
     *
     * @return
     */
    public boolean[] getRemainingCandidates() {
        for (int i = 0; i < remainingCandidates.length; i++) {
            remainingCandidates[i] = false;
        }
        if (isShowCandidates()) {
            final int[] cands = Sudoku2.POSSIBLE_VALUES[sudoku.getRemainingCandidates()];
            for (int i = 0; i < cands.length; i++) {
                remainingCandidates[cands[i] - 1] = true;
            }
        }
        return remainingCandidates;
    }
}
