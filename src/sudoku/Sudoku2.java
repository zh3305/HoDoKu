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
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hobiwan
 */
@SuppressWarnings("empty-statement")
public class Sudoku2 implements Cloneable {

    /** conditional compilation */
    private static final boolean DEBUG = false;
    /** The number of cells in the sudoku */
    public static final int LENGTH = 81;
    /** Number of units in each constraint */
    public static final int UNITS = 9;
    /** The internal number for block units. */
    public static final int BLOCK = 0;
    /** The internal number for line units. */
    public static final int LINE = 1;
    /** The internal number for column units. */
    public static final int COL = 2;
    /** The internal number for cell units. */
    public static final int CELL = 3;
    /** All indices for every line */
    public static final int[][] LINES = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8},
        {9, 10, 11, 12, 13, 14, 15, 16, 17},
        {18, 19, 20, 21, 22, 23, 24, 25, 26},
        {27, 28, 29, 30, 31, 32, 33, 34, 35},
        {36, 37, 38, 39, 40, 41, 42, 43, 44},
        {45, 46, 47, 48, 49, 50, 51, 52, 53},
        {54, 55, 56, 57, 58, 59, 60, 61, 62},
        {63, 64, 65, 66, 67, 68, 69, 70, 71},
        {72, 73, 74, 75, 76, 77, 78, 79, 80}
    };
    /** All indices for every column */
    public static final int[][] COLS = {
        {0, 9, 18, 27, 36, 45, 54, 63, 72},
        {1, 10, 19, 28, 37, 46, 55, 64, 73},
        {2, 11, 20, 29, 38, 47, 56, 65, 74},
        {3, 12, 21, 30, 39, 48, 57, 66, 75},
        {4, 13, 22, 31, 40, 49, 58, 67, 76},
        {5, 14, 23, 32, 41, 50, 59, 68, 77},
        {6, 15, 24, 33, 42, 51, 60, 69, 78},
        {7, 16, 25, 34, 43, 52, 61, 70, 79},
        {8, 17, 26, 35, 44, 53, 62, 71, 80}
    };
    /** All indices for every block */
    public static final int[][] BLOCKS = {
        {0, 1, 2, 9, 10, 11, 18, 19, 20},
        {3, 4, 5, 12, 13, 14, 21, 22, 23},
        {6, 7, 8, 15, 16, 17, 24, 25, 26},
        {27, 28, 29, 36, 37, 38, 45, 46, 47},
        {30, 31, 32, 39, 40, 41, 48, 49, 50},
        {33, 34, 35, 42, 43, 44, 51, 52, 53},
        {54, 55, 56, 63, 64, 65, 72, 73, 74},
        {57, 58, 59, 66, 67, 68, 75, 76, 77},
        {60, 61, 62, 69, 70, 71, 78, 79, 80}
    };
    /** All indices for all constraints: first lines, then cols, then blocks */
    public static final int[][] ALL_UNITS = {
        LINES[0], LINES[1], LINES[2], LINES[3], LINES[4], LINES[5], LINES[6], LINES[7], LINES[8],
        COLS[0], COLS[1], COLS[2], COLS[3], COLS[4], COLS[5], COLS[6], COLS[7], COLS[8],
        BLOCKS[0], BLOCKS[1], BLOCKS[2], BLOCKS[3], BLOCKS[4], BLOCKS[5], BLOCKS[6], BLOCKS[7], BLOCKS[8]
    };
    /** All indices for lines and blocks (fish search) */
    public static final int[][] LINE_BLOCK_UNITS = {
        LINES[0], LINES[1], LINES[2], LINES[3], LINES[4], LINES[5], LINES[6], LINES[7], LINES[8],
        BLOCKS[0], BLOCKS[1], BLOCKS[2], BLOCKS[3], BLOCKS[4], BLOCKS[5], BLOCKS[6], BLOCKS[7], BLOCKS[8]
    };
    /** All indices for columns and blocks (fish search) */
    public static final int[][] COL_BLOCK_UNITS = {
        COLS[0], COLS[1], COLS[2], COLS[3], COLS[4], COLS[5], COLS[6], COLS[7], COLS[8],
        BLOCKS[0], BLOCKS[1], BLOCKS[2], BLOCKS[3], BLOCKS[4], BLOCKS[5], BLOCKS[6], BLOCKS[7], BLOCKS[8]
    };
    /** The index in {@link #BLOCKS} for every cell (speeds up lookup) */
    private static final int[] BLOCK_FROM_INDEX = {
        0, 0, 0, 1, 1, 1, 2, 2, 2,
        0, 0, 0, 1, 1, 1, 2, 2, 2,
        0, 0, 0, 1, 1, 1, 2, 2, 2,
        3, 3, 3, 4, 4, 4, 5, 5, 5,
        3, 3, 3, 4, 4, 4, 5, 5, 5,
        3, 3, 3, 4, 4, 4, 5, 5, 5,
        6, 6, 6, 7, 7, 7, 8, 8, 8,
        6, 6, 6, 7, 7, 7, 8, 8, 8,
        6, 6, 6, 7, 7, 7, 8, 8, 8
    };
    /**
     * Every block for every line
     */
    public static final int[][] BLOCKS_FROM_LINES = {
        {0, 1, 2},
        {0, 1, 2},
        {0, 1, 2},
        {3, 4, 5},
        {3, 4, 5},
        {3, 4, 5},
        {6, 7, 8},
        {6, 7, 8},
        {6, 7, 8}
    };
    /**
     * Every block for every col
     */
    public static final int[][] BLOCKS_FROM_COLS = {
        {0, 3, 6},
        {0, 3, 6},
        {0, 3, 6},
        {1, 4, 7},
        {1, 4, 7},
        {1, 4, 7},
        {2, 5, 8},
        {2, 5, 8},
        {2, 5, 8}
    };
    /** The internal Unit for every constraint */
    public static final int[] CONSTRAINT_TYPE_FROM_CONSTRAINT = {
        LINE, LINE, LINE, LINE, LINE, LINE, LINE, LINE, LINE,
        COL, COL, COL, COL, COL, COL, COL, COL, COL,
        BLOCK, BLOCK, BLOCK, BLOCK, BLOCK, BLOCK, BLOCK, BLOCK, BLOCK
    };
    /** The internal unit number for every constraint */
    public static final int[] CONSTRAINT_NUMBER_FROM_CONSTRAINT = {
        1, 2, 3, 4, 5, 6, 7, 8, 9,
        1, 2, 3, 4, 5, 6, 7, 8, 9,
        1, 2, 3, 4, 5, 6, 7, 8, 9
    };
    // Sets for units
    // Helper arrays for candidates
    /** All possible masks for the digits 1 to 9. */
    public static final short[] MASKS = {
        0x0000,
        0x0001, 0x0002, 0x0004, 0x0008,
        0x0010, 0x0020, 0x0040, 0x0080,
        0x0100
    };
    /** Mask for "all digits set" */
    public static final short MAX_MASK = 0x01ff;
    /** for each of the 256 possible bit combinations the matching array (for iteration) */
    public static final int[][] POSSIBLE_VALUES = new int[0x200][];
    /** The length of the array in {@link #POSSIBLE_VALUES} for each bit combination. */
    public static final int[] ANZ_VALUES = new int[0x200];
    /** The indices of the constraints for every cell (LINE, COL, BLOCK) */
    public static int[][] CONSTRAINTS = new int[LENGTH][3];
    /** The candidate represented by the least significant bit that is set in a candidate mask.
     *  If only one bit is set, the array contains the value of that bit (candidate). */
    public static final short[] CAND_FROM_MASK = new short[0x200];
    // Templates
    //
    /** One template for every possible combination of 9 equal digits in the grid. */
    public static SudokuSetBase[] templates = new SudokuSetBase[46656];
    /** One bitmap with all buddies of each cell */
    public static SudokuSet[] buddies = new SudokuSet[LENGTH];
    /** The low order long from {@link #buddies} */
    public static long[] buddiesM1 = new long[LENGTH];
    /** The high order long from {@link #buddies} */
    public static long[] buddiesM2 = new long[LENGTH];
    /** For every group of 8 cells (denoted by a byte in a SudokuSetBase) all possible buddies */
    public static SudokuSetBase[][] groupedBuddies = new SudokuSetBase[11][256];
    /** The low order long from {@link #groupedBuddies} */
    public static long[][] groupedBuddiesM1 = new long[11][256];
    /** The high order long from {@link #groupedBuddies} */
    public static long[][] groupedBuddiesM2 = new long[11][256];
    /** One bitmap with all cells of each line */
    public static SudokuSet[] LINE_TEMPLATES = new SudokuSet[LINES.length];
    /** One bitmap with all cells of each column */
    public static SudokuSet[] COL_TEMPLATES = new SudokuSet[COLS.length];
    /** One bitmap with all cells of each block */
    public static SudokuSet[] BLOCK_TEMPLATES = new SudokuSet[BLOCKS.length];
    /** One bitmap with all cells of each line and each block */
    public static SudokuSet[] LINE_BLOCK_TEMPLATES = new SudokuSet[LINE_BLOCK_UNITS.length];
    /** One bitmap with all cells of each column and each block */
    public static SudokuSet[] COL_BLOCK_TEMPLATES = new SudokuSet[COL_BLOCK_UNITS.length];
    /** One bitmap with all cells of each constraint */
    public static SudokuSet[] ALL_CONSTRAINTS_TEMPLATES = new SudokuSet[ALL_UNITS.length];
    /** The low order long from {@link #ALL_CONSTRAINTS_TEMPLATES} */
    public static long[] ALL_CONSTRAINTS_TEMPLATES_M1 = new long[ALL_UNITS.length];
    /** The high order long from {@link #ALL_CONSTRAINTS_TEMPLATES} */
    public static long[] ALL_CONSTRAINTS_TEMPLATES_M2 = new long[ALL_UNITS.length];
    // The data of a Sudoku
    /** Candidate bitmaps for all cells. 0 stands for "cell already set". */
    private short[] cells = new short[LENGTH];
    /** Bitmaps for candidates set by the user if "show all candidates" is not set. */
    private short[] userCells = new short[LENGTH];
    /** Number of free cells per constraint and per candidate (CAUTION: candidates
     *  go from 1 to 9). Used to detect Hidden Singles easily */
    private byte[][] free = new byte[ALL_UNITS.length][UNITS + 1];
    /** number of unfilled cells in the grid */
    private int unsolvedCellsAnz;
    /** The values of the cells (0 means cell not set); if a cell is set, the corresponding entry in {@link #cells} is deleted */
    private int[] values = new int[LENGTH];
    /** Determines if cell is a given */
    private boolean[] fixed = new boolean[LENGTH];
    /** The correct values of the solution */
    private int[] solution = new int[LENGTH];
    /** Indicates if solution has been set! */
    private boolean solutionSet = false;
    /** The difficulty level of this puzzle */
    private DifficultyLevel level = null;
    /** The difficulty score of this puzzle */
    private int score;
    /** the state in which this Sudoku was loaded (for "Reset Puzzle") */
    private String initialState = null;
    /** the state of the sudoku (for progress display) */
    private SudokuStatus status = SudokuStatus.EMPTY;
    /** the state of the sudoku if only the givens are taken into account */
    private SudokuStatus statusGivens = SudokuStatus.EMPTY;
    // Queues for detecting Singles: Naked Singles and Hidden Singles become obvious
    // while setting/deleting candidates; two synchronized arrays contain index/value pairs
    /** A queue for newly detected Naked Singles */
    private SudokuSinglesQueue nsQueue = new SudokuSinglesQueue();
    /** A queue for newly detected Hidden Singles */
    private SudokuSinglesQueue hsQueue = new SudokuSinglesQueue();

    static {
        // Buddies und Unit-Sets initialisieren
        long ticks = System.currentTimeMillis();
        //System.out.println("Init...");
        initBuddies();
        ticks = System.currentTimeMillis() - ticks;
        //System.out.println("Init buddies: " + ticks + "ms");

        // Templates initialisieren
        ticks = System.currentTimeMillis();
        initTemplates();
        ticks = System.currentTimeMillis() - ticks;
        //System.out.println("Init templates: " + ticks + "ms");

        // Grouped buddies
        ticks = System.currentTimeMillis();
        initGroupedBuddies();
        ticks = System.currentTimeMillis() - ticks;
        //System.out.println("Init grouped buddies: " + ticks + "ms");

        // initialize POSSIBLE_VALUES
        POSSIBLE_VALUES[0] = new int[0];
        ANZ_VALUES[0] = 0;
        int[] temp = new int[9];
        for (int i = 1; i <= 0x1ff; i++) {
            int index = 0;
            int mask = 1;
            for (int j = 1; j <= 0x1ff; j++) {
                if ((i & mask) != 0) {
                    temp[index++] = j;
                }
                mask <<= 1;
            }
            POSSIBLE_VALUES[i] = new int[index];
//            for (int k = 0; k < index; k++) {
//                POSSIBLE_VALUES[i][k] = temp[k];
//            }
            System.arraycopy(temp, 0, POSSIBLE_VALUES[i], 0, index);
            ANZ_VALUES[i] = index;
        }

        // initialize the constraints table
        // lookup tables for constraints: three constraints for every cell
        // lines go from 0 .. 8
        // cols go from 9 .. 17
        // boxes from 18 .. 26
        int index = 0;
        // one loop for every line
        for (int line = 0; line < 9; line++) {
            // base box index for line index
            int boxBase = 2 * 9 + ((line / 3) * 3);
            // one loop for every column in line index
            for (int col = 9; col < 2 * 9; col++) {
                CONSTRAINTS[index][0] = line;
                CONSTRAINTS[index][1] = col;
                CONSTRAINTS[index][2] = boxBase + ((col / 3) % 3);
                index++;
            }
        }

        // initialize CAND_FROM_MASK
        // CAND_FROM_MASK: The candidate represented by the least siginificant bit that is set in a candidate mask
        for (int i = 1; i < CAND_FROM_MASK.length; i++) {
            short j = -1;
            while ((i & MASKS[++j]) == 0);
            CAND_FROM_MASK[i] = j;
        }
    }

    /** Creates a new instance of Sudoku2.<br>
     *  All bitmaps are initialized with all bits set (in every cell every
     *  candidate is allowed)
     */
    public Sudoku2() {
        clearSudoku();
    }

    /**
     * Clones a Sudoku. Makes a valid deep copy.
     * 
     * @return
     */
    @Override
    public Sudoku2 clone() {
        Sudoku2 newSudoku = null;
        try {
            newSudoku = (Sudoku2) super.clone();
            newSudoku.cells = cells.clone();
            newSudoku.userCells = userCells.clone();
            newSudoku.values = values.clone();
            newSudoku.solution = solution.clone();
            newSudoku.fixed = fixed.clone();
            newSudoku.free = new byte[free.length][];
            for (int i = 0; i < free.length; i++) {
                newSudoku.free[i] = free[i].clone();
            }
            if (initialState != null) {
                // no copy needed, is immutable!
                newSudoku.initialState = initialState;
            }
            newSudoku.nsQueue = nsQueue.clone();
            newSudoku.hsQueue = hsQueue.clone();
            // no deep copy required for level, it is constant
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error while cloning", ex);
        }
        return newSudoku;
    }

    /**
     * Sets a Sudoku with the values from <code>src</code>.
     * @param src
     */
    public void set(Sudoku2 src) {
        System.arraycopy(src.cells, 0, cells, 0, LENGTH);
        System.arraycopy(src.userCells, 0, userCells, 0, LENGTH);
        System.arraycopy(src.values, 0, values, 0, LENGTH);
        System.arraycopy(src.solution, 0, solution, 0, LENGTH);
        System.arraycopy(src.fixed, 0, fixed, 0, LENGTH);
        for (int i = 0; i < free.length; i++) {
            System.arraycopy(src.free[i], 0, free[i], 0, UNITS + 1);
        }
        unsolvedCellsAnz = src.unsolvedCellsAnz;
        solutionSet = src.solutionSet;
        score = src.score;
        level = src.level; // no deep copy required, level is constant
        if (src.initialState != null) {
            // no copy needed, is immutable!
            initialState = src.initialState;
        }
        status = src.status;
        statusGivens = src.statusGivens;
        nsQueue.set(src.nsQueue);
        hsQueue.set(src.hsQueue);
    }

    /**
     * A simplified version of {@link #set(sudoku.Sudoku2)} that doesnt set all
     * fields. Must only be used by the BacktrackingSolver!
     * @param src
     */
    public void setBS(Sudoku2 src) {
        cells = Arrays.copyOf(src.cells, cells.length);
        values = Arrays.copyOf(src.values, values.length);
        for (int i = 0; i < free.length; i++) {
            free[i] = Arrays.copyOf(src.free[i], free[i].length);
        }
        unsolvedCellsAnz = src.unsolvedCellsAnz;
        nsQueue.clear();
        hsQueue.clear();
    }

    /**
     * Initialize the data structure to an empty grid (no values set,
     * in all cells all candidates are possible), the queues are deleted.
     */
    public final void clearSudoku() {
        for (int i = 0; i < cells.length; i++) {
            cells[i] = MAX_MASK;
            userCells[i] = 0;
        }
        for (int i = 0; i < free.length; i++) {
            for (int j = 1; j < free[i].length; j++) {
                free[i][j] = 9;
            }
        }
        for (int i = 0; i < values.length; i++) {
            values[i] = 0;
            solution[i] = 0;
            fixed[i] = false;
        }
        unsolvedCellsAnz = LENGTH;
        initialState = null;
        solutionSet = false;
        status = SudokuStatus.EMPTY;
        statusGivens = SudokuStatus.EMPTY;
        // dont change the score!
        //score = 0;
        // dont change the level!
        //level = Options.getInstance().getDifficultyLevel(DifficultyType.EASY.ordinal());
        //level = null;
        // delete queues
        nsQueue.clear();
        hsQueue.clear();
    }

    /**
     * Reset the Sudoku to its initial state as stored in {@link #initialState}.
     */
    public void resetSudoku() {
        if (initialState != null) {
            setSudoku(initialState, true);
        }
    }

    /**
     * Loads the Sudoku from a string. Delegates to {@link #setSudoku(java.lang.String, boolean) }.
     * @param init
     */
    public void setSudoku(String init) {
        setSudoku(init, true);
    }

    /**
     * Loads a Sudoku from a String. Possible formats:
     * <ul>
     *      <li>81 character string with givens (line may contain comments started with '#')</li>
     *      <li>One line from gsf's q1 taxonomy</li>
     *      <li>A PM grid (may contain markups)</li>
     *      <li>A combination of givens + PM grid (Simple Sudoku)</li>
     *      <li>The HoDoKu library format</li>
     * </ul>
     * @param init
     * @param saveInitialState
     */
    public void setSudoku(String init, boolean saveInitialState) {
        clearSudoku();
        if (init == null) {
            return;
        }

        //
        // Possible formats: ...1.32.4..1.
        //
        //                   0001032040010
        //
        //                   +-------+-------+-------+
        //                   | . . . | . . . | . . . | ('0' allowed)
        //                   | 2 4 . | 3 5 . | . . . | ('0' allowed)
        //                   | . . . | . . . | . . . | ('0' allowed)
        //                   +-------+-------+-------+
        //                   | . . . | . 7 . | 3 . 1 |
        //                   | . . . | . . . | . . . |
        //                   | . . . | . . . | . . . |
        //                   +-------+-------+-------+
        //                   | . . . | . . . | . . . |
        //                   | . . . | . . . | . . . |
        //                   | . . . | . . . | . . . |
        //                   +-------+-------+-------+
        //
        //                   *-----------------------------------------------------------*
        //                   | 6     19    2     | 3     19    5     | 4     8     7     |
        //                   | 578   19    4     | 28    67    1679  | 3     159   1259  |
        //                   | 578   3     57    | 28    4     179   | 19    6     1259  |
        //                   |-------------------+-------------------+-------------------|
        //                   | 3    *678   568   |*17    2     179   | 1679  159   4     |
        //                   | 57    4     1     | 6     379   8     | 2     3579  359   |
        //                   | 9     2     67    | 4     5     137   | 167   137   8     |
        //                   |-------------------+-------------------+-------------------|
        //                   | 2     5   #367    |*17    8     1367  | 179   4     139   |
        //                   | 4    *67   9      | 5     1367  2     | 8     137   13    |
        //                   | 1    *78   378    | 9     37    4     | 5     2     6     |
        //                   *-----------------------------------------------------------*
        //
        // CAUTION: When importing a PM grid, a single value in a cell could be a Naked Single.
        // Solution: If one of the houses of a single candidate contains that candidate in another
        //           cell, that candidate is treated as Naked Single; if not, the cell is set
        //
        // Special case SimpleSudoku: SS exports puzzles in two formats:
        //    - givens + pm
        //    - givens + set cells + pm
        //
        // example for type 2:
//             *-----------*
//             |8..|4..|...|
//             |.65|...|.42|
//             |9..|.8.|1.6|
//             |---+---+---|
//             |7..|.65|..8|
//             |...|...|...|
//             |2..|94.|..5|
//             |---+---+---|
//             |5.9|.2.|..7|
//             |47.|...|29.|
//             |...|..1|..4|
//             *-----------*
//
//
//             *-----------*
//             |8..|4..|...|
//             |.65|.7.|.42|
//             |9..|.8.|1.6|
//             |---+---+---|
//             |7..|.65|..8|
//             |...|...|...|
//             |2..|94.|..5|
//             |---+---+---|
//             |5.9|.24|..7|
//             |47.|...|29.|
//             |...|791|..4|
//             *-----------*
//
//
//             *-----------------------------------------------------------------------------*
//             | 8       123     1237    | 4       135     2369    | 3579    357     39      |
//             | 13      6       5       | 13      7       39      | 89      4       2       |
//             | 9       234     2347    | 235     8       23      | 1       357     6       |
//             |-------------------------+-------------------------+-------------------------|
//             | 7       1349    134     | 123     6       5       | 349     123     8       |
//             | 136     134589  13468   | 1238    13      2378    | 34679   12367   139     |
//             | 2       138     1368    | 9       4       378     | 367     1367    5       |
//             |-------------------------+-------------------------+-------------------------|
//             | 5       138     9       | 368     2       4       | 368     1368    7       |
//             | 4       7       1368    | 3568    35      368     | 2       9       13      |
//             | 36      238     2368    | 7       9       1       | 3568    3568    4       |
//             *-----------------------------------------------------------------------------*


        // Split input in lines, identify border lines (SudoCue uses '.' in borders,
        // gives an error) and erase markup characters
        // a line counts as border line, if it contains at least one occurence of "---"
        String lineEnd = null;
        int[][] cands = new int[9][9];
        if (init.contains("\r\n")) {
            lineEnd = "\r\n";
        } else if (init.contains("\r")) {
            lineEnd = "\r";
        } else if (init.contains("\n")) {
            lineEnd = "\n";
        }
        String[] lines = null;
        if (lineEnd != null) {
            lines = init.split(lineEnd);
//            StringBuilder tmpBuffer = new StringBuilder();
//            tmpBuffer.append("lines org\r\n");
//            for (int i = 0; i < lines.length; i++) {
//                tmpBuffer.append("lines[" + i + "]: " + lines[i] + "\r\n");
//            }
//            Logger.getLogger(getClass().getName()).log(Level.FINE, tmpBuffer.toString());
        } else {
            lines = new String[1];
            lines[0] = init;
//            Logger.getLogger(getClass().getName()).log(Level.FINE, "Einzeiler: <" + lines[0] + ">");
        }
        int anzLines = lines.length;

        // Check for library format: a one liner with 6 or 7 ":"
        boolean libraryFormat = false;
        String libraryCandStr = null;
        if (anzLines == 1) {
            int anzDoppelpunkt = getAnzPatternInString(init, ":");
            if (anzDoppelpunkt == 6 || anzDoppelpunkt == 7) {
                libraryFormat = true;
                String[] libLines = init.split(":");
                lines[0] = libLines[3];
                if (libLines.length >= 5) {
                    libraryCandStr = libLines[4];
                } else {
                    libraryCandStr = "";
                }
//                Logger.getLogger(getClass().getName()).log(Level.FINE, "LF - lines[0]: " + lines[0]);
//                Logger.getLogger(getClass().getName()).log(Level.FINE, "LF - libraryCandStr: " + libraryCandStr);
            }
        }

        // many formats append additional info after a '#', but are one liners -> check
        if (anzLines == 1) {
            if (lines[0].contains("#")) {
                String tmpStr = lines[0].substring(0, lines[0].indexOf("#")).trim();
                if (tmpStr.length() >= 81) {
                    // was comment at end of line
                    lines[0] = tmpStr;
                }
            }
        }

        // gsf's q2-taxonomy: more than 6 ',' in one line
        // 99480,99408,114,1895,100000002090400050006000700050903000000070000000850040700000600030009080002000001,Easter-Monster,20.00s,0,C21.m/F350642.604765/N984167.2160032/B207128.704971.243458.236078/P9.16.467645.2.20.1697341.25.5.2.445049253/M3.566.938,C21.m/F10.28/N8.25/B3.10.10/H2.4.2/W1.1.1/X1.8/Y1.13/K1.8.18.0.0.0.6.2/O3.3/G4.0.1/M1.4.29
        if (anzLines == 1) {
            if (getAnzPatternInString(init, ",") >= 6) {
                String[] gsfLines = init.split(",");
                lines[0] = gsfLines[4];
            }
        }

        // In the library format solved cells, that are not givens, can be marked with '+'
        // solvedButNotGivens is initialized with 'false'
        boolean[] solvedButNotGivens = new boolean[81];
        if (libraryFormat) {
            // in library format the sudoku itself is always in the form
            // "81.37.6..4..18....53....8.1.73...51..65...98..84...36..5.....29...561....48792156"
            StringBuilder tmp = new StringBuilder(lines[0]);
            for (int i = 0; i < tmp.length(); i++) {
                char ch = tmp.charAt(i);
                if (ch == '+') {
                    // cell is not a given!
                    solvedButNotGivens[i] = true;
                    tmp.deleteCharAt(i);
                    if (i >= 0) {
                        i--;
                    }
                }
            }
        }

        // delete markup characters, parse the candidates and set them
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null) {
                // all characters except digits, '.' and ' ' are deleted (consecutive blanks
                // are reduced to one blank only).
                // '|' is practically always used as block delimiter -> replaced by blank
                StringBuilder tmp = new StringBuilder(lines[i].trim());
                // border lines have to contain "---"
                int tmpIndex = -1;
                while ((tmpIndex = tmp.indexOf("---")) >= 0) {
                    if (tmpIndex > 0) {
                        char ch = tmp.charAt(tmpIndex - 1);
                        if (!Character.isDigit(ch) && ch != ' ' && ch != '|') {
                            tmpIndex--;
                        }
                    }
                    int endIndex = tmpIndex + 1;
                    while (endIndex < tmp.length() && tmp.charAt(endIndex) == '-') {
                        endIndex++;
                    }
                    if (endIndex < tmp.length() - 1) {
                        char ch = tmp.charAt(endIndex + 1);
                        if (!Character.isDigit(ch) && ch != ' ' && ch != '|') {
                            endIndex++;
                        }
                    }
                    //tmp.delete(0, tmp.length());
                    tmp.delete(tmpIndex, endIndex + 1);
                }
                // throw out garbage
                for (int j = 0; j < tmp.length(); j++) {
                    char ch = tmp.charAt(j);
                    if (ch == '|') {
                        tmp.setCharAt(j, ' ');
                    } else if (!Character.isDigit(ch) && ch != '.' && ch != ' ') {
                        tmp.deleteCharAt(j);
                        if (j >= 0) {
                            j--;
                        }
                    }
                }
                // remove consecutive blanks
                int index = 0;
                while ((index = tmp.indexOf("  ")) != -1) {
                    tmp.deleteCharAt(index);
                }
                lines[i] = tmp.toString().trim();
                // if lines[i] is now empty it is removed
                if (lines[i].length() == 0) {
                    for (int j = i; j < lines.length - 1; j++) {
                        lines[j] = lines[j + 1];
                    }
                    lines[lines.length - 1] = null;
                    anzLines--;
                    i--;
                }
            }
        }
        if (DEBUG) {
            System.out.println("lines trimmed:");
            for (int i = 0; i < lines.length; i++) {
                System.out.println("lines[" + i + "]: " + lines[i]);
            }
            System.out.println("anzLines: " + anzLines);
        }

        // special case SimpleSudoku: contains PM grid and one liner -> ignore one liner
        if (anzLines == 10) {
            anzLines--;
        }
        // SimpleSudoku can contain 3 grids: givens, solved cells and PM
        boolean logAgain = false;
        boolean ssGivensRead = false;
        String ssGivens = null;
        boolean ssCellsRead = false;
        String ssCells = null;
        while (anzLines > 9 && anzLines % 9 == 0) {
            if (!ssGivensRead) {
                ssGivens = getSSString(lines);
                ssGivensRead = true;
                ssCellsRead = true;
                ssCells = ssGivens;
                if (DEBUG) {
                    System.out.println("givens for SimpleSudoku: " + ssGivens);
                }
            } else {
                ssCells = getSSString(lines);
                ssCellsRead = true;
            }
            logAgain = true;
            for (int i = 9; i < anzLines; i++) {
                lines[i - 9] = lines[i];
                if (i >= anzLines - 9) {
                    lines[i] = null;
                }
            }
            anzLines -= 9;
        }
        if (logAgain) {
            if (DEBUG) {
                System.out.println("lines after SimpleSudoku:");
                for (int i = 0; i < lines.length; i++) {
                    System.out.println("lines[" + i + "]: " + lines[i]);
                }
                System.out.println("anzLines: " + anzLines);
            }
        }

        // if we have a PM grid, candidates are parsed to cands; for one liners the cells are set directly
        int sRow = 0;
        int sCol = 0;
        int sIndex = 0;
        boolean singleDigits = true;
        boolean isPmGrid = false;
        String sInit = lines[0];
        for (int i = 1; i < anzLines; i++) {
            sInit += " " + lines[i];
        }
        if (DEBUG) {
            System.out.println("sInit: " + sInit);
        }
        if (sInit.length() > 81) {
            singleDigits = false;
        }
        if (sInit.length() > 2 * 81) {
            isPmGrid = true;
        }
        if (DEBUG) {
            System.out.println(singleDigits + "/" + isPmGrid + "/" + sInit);
        }
        while (sIndex < sInit.length()) {
            // jump to next block of digits
            char ch = sInit.charAt(sIndex);
            while (sIndex < sInit.length() && !(Character.isDigit(ch) || ch == '.')) {
                sIndex++;
                ch = sInit.charAt(sIndex);
            }
            if (sIndex >= sInit.length()) {
                break;
            }
            if (isPmGrid) {
                if (ch == '.' || ch == '0') {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid character: {0}", ch);
                    cands[sRow][sCol] = 0;
                    sIndex++;
                } else {
                    if (singleDigits) {
                        cands[sRow][sCol] = Integer.parseInt(sInit.substring(sIndex, sIndex + 1));
                        sIndex++;
                    } else {
                        int endIndex = sInit.indexOf(" ", sIndex);
                        if (endIndex < 0) {
                            endIndex = sInit.length();
                        }
                        cands[sRow][sCol] = Integer.parseInt(sInit.substring(sIndex, endIndex));
                        if (DEBUG) {
                            System.out.println("candidates[" + sRow + "][" + sCol + "] = " + cands[sRow][sCol]);
                        }
                        sIndex = endIndex;
                    }
                }
            } else {
                if (Character.isDigit(ch) && Character.digit(ch, 10) > 0) {
                    boolean given = true;
                    if (libraryFormat) {
                        given = !solvedButNotGivens[sRow * 9 + sCol];
                    }
                    if (DEBUG) {
                        System.out.println("sRow=" + sRow + ", sCol=" + sCol + ", digit=" + Character.digit(ch, 10) + ", given=" + given);
                    }
                    setCell(sRow, sCol, Character.digit(ch, 10), given);
                }
                sIndex++;
            }
            sCol++;
            if (sCol == 9) {
                sCol = 0;
                sRow++;
            }
        }

        if (isPmGrid) {
            // set the sudoku: set all candidates in a first pass; then check for all cells,
            // that contain only one candidate, if that candidate is set in a buddy; if not,
            // the cell is set
            int[] cands1 = new int[10];
            for (int row = 0; row < cands.length; row++) {
                for (int col = 0; col < cands[row].length; col++) {
                    // Es dürfen nur die angegebenen Kandidaten gesetzt sein
                    Arrays.fill(cands1, 0);
                    int sum = cands[row][col];
                    while (sum > 0) {
                        cands1[sum % 10] = 1;
                        sum /= 10;
                    }
                    int cellIndex = getIndex(row, col);
                    for (int i = 1; i < cands1.length; i++) {
                        if (cands1[i] == 0 && isCandidate(cellIndex, i)) {
                            setCandidate(row, col, i, false);
                        } else if (cands1[i] == 1 && !isCandidate(cellIndex, i)) {
                            setCandidate(row, col, i, true);
                        }
                    }
                }
            }
            // Jetzt eventuelle Zellen setzen
            for (int i = 0; i < values.length; i++) {
                if (getAnzCandidates(i) == 1) {
                    if (ssCellsRead) {
                        // special case SimpleSudoku: a 81 character string with all
                        // cell values is available
                        char ch = ssCells.charAt(i);
                        if (ch != '0' && ch != '.') {
                            // cell must be set
                            setCell(i, Character.digit(ch, 10), true);
                        }
                    } else {
                        // check if the candidate is set in one of the buddies
                        for (int j = 1; j <= 9; j++) {
                            if (!isCandidate(i, j)) {
                                // not the correct candidate
                                continue;
                            }
                            int count = 0;
                            for (int k = 0; k < buddies[i].size(); k++) {
                                int buddyIndex = buddies[i].get(k);
                                if (values[buddyIndex] == 0 && isCandidate(buddyIndex, j)) {
                                    count++;
                                    break;
                                }
                            }
                            if (count == 0) {
                                // no buddies -> set the cell
                                setCell(i, j, true);
                            }
                        }
                    }
                }
            }
        }

        // delete candidates for library format
        if (libraryFormat && libraryCandStr.length() > 0) {
            String[] candArr = libraryCandStr.split(" ");
//            tmpBuffer = new StringBuilder();
//            tmpBuffer.append("libraryCandStr: <" + libraryCandStr + ">\r\n");
            for (int i = 0; i < candArr.length; i++) {
//                tmpBuffer.append("candArr[" + i + "]: <" + candArr[i] + ">\r\n");
                if (candArr[i].length() == 0) {
                    continue;
                }
                int candPos = Integer.parseInt(candArr[i]);
                int col = candPos % 10;
                candPos /= 10;
                int row = candPos % 10;
                candPos /= 10;
                setCandidate(row - 1, col - 1, candPos, false);
            }
//            Logger.getLogger(getClass().getName()).log(Level.FINE, tmpBuffer.toString());
        }

        // for SimpleSudoku the givens are set in an extra pass
        if (ssGivensRead) {
            setGivens(ssGivens);
        }

        if (saveInitialState) {
            //setInitialState(getSudoku(ClipboardMode.PM_GRID));
            setInitialState(getSudoku(ClipboardMode.LIBRARY));
        }

        // we simply assume the sudoku is valid; for batch use, status
        // is ignored and statusGivens has to be VALID (no checks ar made).
        // for the GUI,status and statusGivens are checked elsewhere
        status = SudokuStatus.VALID;
        statusGivens = SudokuStatus.VALID;
    }

    /**
     * Takes the first 9 Strings of <code>lines</code> and
     * condenses it into one 81 character string.
     * 
     * @param lines
     * @return 
     */
    private String getSSString(String[] lines) {
        StringBuilder ssTemp = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            ssTemp.append(lines[i]);
        }
        for (int i = 0; i < ssTemp.length(); i++) {
            char ch = ssTemp.charAt(i);
            if (!Character.isDigit(ch) && ch != '.') {
                ssTemp.deleteCharAt(i);
                i--;
            }
        }
        return ssTemp.toString();
    }

    /**
     * Checks, if a necessary candidate is missing from {@link #userCells}.
     * "Necessary" means, that a candidate is missing, that has to be set
     * in the cell. Is used to determine, whether a hint can be created
     * when not using "Show all candidates".
     * 
     * @return 
     */
    public boolean checkUserCands() {
        if (!solutionSet) {
            // we cant check without the solution!
            return false;
        }
        for (int i = 0; i < LENGTH; i++) {
            if (values[i] != 0) {
                // cell already set, ignore!
                continue;
            }
            if ((userCells[i] & MASKS[solution[i]]) == 0) {
                // candidate is missing!
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the number of candidates in a cell, that is not set.<br>
     * If the cell is already set, <code>cells[index]</code> will contain 0,
     * which gives a return code of 0 as well.
     * 
     * @param index
     * @return
     */
    public int getAnzCandidates(int index) {
        return ANZ_VALUES[cells[index]];
    }

    /**
     * Returns an array that holds all candidate values that
     * are still possible in cell <code>index</code>.
     * @param index
     * @return
     */
    public int[] getAllCandidates(int index) {
        return POSSIBLE_VALUES[cells[index]];
    }

    /**
     * Returns an array that holds all candidate values that
     * are still possible in cell <code>index</code>.
     *
     * @param index
     * @param user
     * @return
     */
    public int[] getAllCandidates(int index, boolean user) {
        if (user) {
            return POSSIBLE_VALUES[userCells[index]];
        } else {
            return getAllCandidates(index);
        }
    }

    /**
     * Calculates the number of candidates in a cell, that is not set.
     *
     * @param index
     * @param user
     * @return
     */
    public int getAnzCandidates(int index, boolean user) {
        if (user) {
            return ANZ_VALUES[userCells[index]];
        } else {
            return getAnzCandidates(index);
        }
    }

    /**
     * Search for <code>pattern</code> in string <code>str</code>.
     * Return the number of occurrencies.
     *
     * @param str
     * @param pattern
     * @return
     */
    private int getAnzPatternInString(String str, String pattern) {
        int anzPattern = 0;
        int index = -1;
        while ((index = str.indexOf(pattern, index + 1)) >= 0) {
            anzPattern++;
        }
        return anzPattern;
    }

    /**
     * {@link #unsolvedCellsAnz} is checked and {@link #free} and
     * the queues for Naked and Hidden Singles are rebuilt.
     *
     */
    public void rebuildInternalData() {
        // delete the queues
        nsQueue.clear();
        hsQueue.clear();
        // reset free
        for (int i = 0; i < free.length; i++) {
            for (int j = 0; j < free[i].length; j++) {
                free[i][j] = 0;
            }
        }
        // now check all cells
        int anz = 0;
        for (int index = 0; index < values.length; index++) {
            if (values[index] != 0) {
                // just to be sure
                cells[index] = 0;
            } else {
                // one more unsolved cell
                anz++;
                // check the candidates and rebuild the Naked Single queue
                int[] cands = POSSIBLE_VALUES[cells[index]];
                for (int i = 0; i < cands.length; i++) {
                    // add candidate to free
                    for (int j = 0; j < CONSTRAINTS[index].length; j++) {
                        free[CONSTRAINTS[index][j]][cands[i]]++;
                    }
                }
                // Naked Single?
                if (ANZ_VALUES[cells[index]] == 1) {
                    addNakedSingle(index, CAND_FROM_MASK[cells[index]]);
                }
            }
        }
        unsolvedCellsAnz = anz;
        // now rebuild the Hidden Single queue
        for (int i = 0; i < free.length; i++) {
            for (int j = 1; j <= 9; j++) {
//                System.out.println("free[" + i + "][" + j + "] = " + free[i][j]);
                if (free[i][j] == 1) {
                    while (addHiddenSingle(i, j) == false);
                }
            }
        }
    }

    /**
     * Check if the sudoku is valid. If {@link #solution} has already
     * been set, the sudoku is checked against the solution.<br>
     * For candidates every candidate set must be allowed (not invalidated by
     * an already set cell) and no candidate, that is part of the solution,
     * must be missing. {@link #unsolvedCellsAnz} is checked and {@link #free} and 
     * the queues for Naked and Hidden Singles are rebuilt.
     * 
     * @return
     */
    public boolean checkSudoku() {
        // rebuild the internal data
        rebuildInternalData();
        // now check all cells
        for (int index = 0; index < values.length; index++) {
            if (values[index] != 0) {
                // check values: must be valid and equal the solution
                if (!isValidValue(index, values[index])) {
                    // value is invalid
                    return false;
                }
                if (solutionSet && solution[index] != values[index]) {
                    // value is valid but deviates from the solution
                    return false;
                }
            } else {
                // check the candidates
                int[] cands = POSSIBLE_VALUES[cells[index]];
                for (int i = 0; i < cands.length; i++) {
                    // all candidates must be valid
                    if (!isValidValue(index, cands[i])) {
                        // candidate is invalid (value set in a buddy)
                        return false;
                    }
                }
                // the candidate for the solution must be still there
                if (solutionSet && !isCandidate(index, solution[index])) {
                    // sudoku cannot be solved
                    return false;
                }
            }
        }
        return true;
    }

//    /**
//     * Returns the current state of the sudoku as PM grid.
//     * @return
//     */
//    public String getSudoku() {
//        return getSudoku(ClipboardMode.PM_GRID, null);
//    }
    /**
     * Returns the current state of the sudoku as string. The
     * desired format is given by <code>mode</code>.
     * @param mode
     * @return
     */
    public String getSudoku(ClipboardMode mode) {
        return getSudoku(mode, null);
    }

    /**
     * Returns the current state of the sudoku as string. The
     * desired format is given by <code>mode</code>. If <code>step</code>
     * is not null, it is included in the output.
     *
     * @param mode
     * @param step
     * @return
     */
    public String getSudoku(ClipboardMode mode, SolutionStep step) {
        String dot = Options.getInstance().isUseZeroInsteadOfDot() ? "0" : ".";
        StringBuilder out = new StringBuilder();
        if (mode == ClipboardMode.LIBRARY) {
            if (step == null) {
                out.append(":0000:x:");
            } else {
                String type = step.getType().getLibraryType();
                if (step.getType().isFish() && step.isIsSiamese()) {
                    type += "1";
                }
                out.append(":").append(type).append(":");
//                for (int i = 0; i < step.getValues().size(); i++) {
//                    out.append(step.getValues().get(i));
//                }
                // append the candidates, that can be deleted
                SortedSet<Integer> candToDeleteSet = new TreeSet<Integer>();
                if (step.getType().useCandToDelInLibraryFormat()) {
                    for (Candidate cand : step.getCandidatesToDelete()) {
                        candToDeleteSet.add(cand.getValue());
                    }
                }
                // if nothing can be deleted, append the cells, that can be set
                if (candToDeleteSet.isEmpty()) {
                    for (int i = 0; i < step.getValues().size(); i++) {
                        candToDeleteSet.add(step.getValues().get(i));
                    }
                }
                for (int cand : candToDeleteSet) {
                    out.append(cand);
                }
                out.append(":");
            }
        }
        if (mode == ClipboardMode.CLUES_ONLY || mode == ClipboardMode.VALUES_ONLY
                || mode == ClipboardMode.LIBRARY) {
            for (int i = 0; i < LENGTH; i++) {
                if (getValue(i) == 0 || (mode == ClipboardMode.CLUES_ONLY && !isFixed(i))) {
                    //out.append(".");
                    out.append(dot);
                } else {
                    if (mode == ClipboardMode.LIBRARY && !isFixed(i)) {
                        out.append("+");
                    }
                    out.append(Integer.toString(getValue(i)));
                }
            }
        }
        if (mode == ClipboardMode.PM_GRID || mode == ClipboardMode.PM_GRID_WITH_STEP
                || mode == ClipboardMode.CLUES_ONLY_FORMATTED || mode == ClipboardMode.VALUES_ONLY_FORMATTED) {
            // new: create one StringBuilder per cell with all candidates/values; add
            // special characters for step if necessary; if a '*' is added to a cell,
            // insert a blank in all other cells of that col that don't have a '*';
            // calculate fieldLength an write it
            StringBuilder[] cellBuffers = new StringBuilder[cells.length];
            for (int i = 0; i < cells.length; i++) {
                cellBuffers[i] = new StringBuilder();
                int value = getValue(i);
                if (mode == ClipboardMode.CLUES_ONLY_FORMATTED && !isFixed(i)) {
                    // only clues!
                    value = 0;
                }
                if (value != 0) {
                    cellBuffers[i].append(String.valueOf(value));
                } else {
                    String candString = "";
                    if (mode != ClipboardMode.CLUES_ONLY_FORMATTED && mode != ClipboardMode.VALUES_ONLY_FORMATTED) {
                        candString = getCandidateString(i);
                    }
                    if (candString.isEmpty()) {
                        candString = dot;
                    }
                    cellBuffers[i].append(candString);
                }
            }

            // now add markings for step
            if (mode == ClipboardMode.PM_GRID_WITH_STEP && step != null) {
                boolean[] cellsWithExtraChar = new boolean[cells.length];
                // indices
                for (int index : step.getIndices()) {
                    insertOrReplaceChar(cellBuffers[index], '*');
                    cellsWithExtraChar[index] = true;
                }
                // fins and endo-fins
                if (SolutionType.isFish(step.getType())
                        || step.getType() == SolutionType.W_WING) {
                    for (Candidate cand : step.getFins()) {
                        int index = cand.getIndex();
                        insertOrReplaceChar(cellBuffers[index], '#');
                        cellsWithExtraChar[index] = true;
                    }
                }
                if (SolutionType.isFish(step.getType())) {
                    for (Candidate cand : step.getEndoFins()) {
                        int index = cand.getIndex();
                        insertOrReplaceChar(cellBuffers[index], '@');
                        cellsWithExtraChar[index] = true;
                    }
                }
                // chains
                for (Chain chain : step.getChains()) {
                    for (int i = chain.getStart(); i <= chain.getEnd(); i++) {
                        if (chain.getNodeType(i) == Chain.ALS_NODE) {
                            // ALS are handled separately
                            continue;
                        }
                        int index = chain.getCellIndex(i);
                        insertOrReplaceChar(cellBuffers[index], '*');
                        cellsWithExtraChar[index] = true;
                        if (chain.getNodeType(i) == Chain.GROUP_NODE) {
                            index = Chain.getSCellIndex2(chain.getChain()[i]);
                            if (index != -1) {
                                insertOrReplaceChar(cellBuffers[index], '*');
                                cellsWithExtraChar[index] = true;
                            }
                            index = Chain.getSCellIndex3(chain.getChain()[i]);
                            if (index != -1) {
                                insertOrReplaceChar(cellBuffers[index], '*');
                                cellsWithExtraChar[index] = true;
                            }
                        }
                    }
                }

                // ALS
                char alsChar = 'A';
                for (AlsInSolutionStep als : step.getAlses()) {
                    for (int index : als.getIndices()) {
                        insertOrReplaceChar(cellBuffers[index], alsChar);
                        cellsWithExtraChar[index] = true;
                    }
                    alsChar++;
                }

                // candidates to delete
                for (Candidate cand : step.getCandidatesToDelete()) {
                    int index = cand.getIndex();
                    char candidate = Character.forDigit(cand.getValue(), 10);
                    for (int i = 0; i < cellBuffers[index].length(); i++) {
                        if (cellBuffers[index].charAt(i) == candidate && (i == 0 || (i > 0 && cellBuffers[index].charAt(i - 1) != '-'))) {
                            cellBuffers[index].insert(i, '-');
                            if (i == 0) {
                                cellsWithExtraChar[index] = true;
                            }
                        }
                    }
                }

                // now adjust columns, where a character was added
                for (int i = 0; i < cellsWithExtraChar.length; i++) {
                    if (cellsWithExtraChar[i]) {
                        int[] indices = COLS[Sudoku2.getCol(i)];
                        for (int j = 0; j < indices.length; j++) {
                            if (Character.isDigit(cellBuffers[indices[j]].charAt(0))) {
                                cellBuffers[indices[j]].insert(0, ' ');
                            }
                        }
                    }
                }
            }

            int[] fieldLengths = new int[COLS.length];
            for (int i = 0; i < cellBuffers.length; i++) {
                int col = getCol(i);
                if (cellBuffers[i].length() > fieldLengths[col]) {
                    fieldLengths[col] = cellBuffers[i].length();
                }
            }
            for (int i = 0; i < fieldLengths.length; i++) {
                fieldLengths[i] += 2;
            }
            String separator = System.getProperty("line.separator");
            for (int i = 0; i < 9; i++) {
                if ((i % 3) == 0) {
                    writeLine(out, i, fieldLengths, null, true, separator);
                }
                writeLine(out, i, fieldLengths, cellBuffers, false, separator);
            }
            writeLine(out, 9, fieldLengths, null, true, separator);

            if (mode == ClipboardMode.PM_GRID_WITH_STEP && step != null) {
                //out.append("\r\n");
                out.append(step.toString(2));
            }
        }
        if (mode == ClipboardMode.LIBRARY) {
            // gelöschte Kandidaten anhängen
            boolean first = true;
            out.append(":");
            for (int i = 0; i < cells.length; i++) {
                if (getValue(i) == 0) {
                    for (int j = 1; j <= 9; j++) {
                        if (isValidValue(i, j) && !isCandidate(i, j)) {
                            if (first) {
                                first = false;
                            } else {
                                out.append(" ");
                            }
                            out.append(Integer.toString(j)).append(Integer.toString((i / 9) + 1)).append(Integer.toString((i % 9) + 1));
                        }
                    }
                }
            }
            if (step == null) {
                out.append("::");
            } else {
                String candString = step.getCandidateString(true);
                out.append(":").append(candString).append(":");
                if (candString.isEmpty()) {
                    out.append(step.getValueIndexString());
                }
                out.append(":");
                if (step.getType().isSimpleChainOrLoop()) {
                    out.append((step.getChainLength() - 1));
                }
            }
        }
        return out.toString();
    }

    /**
     * If the first character in <code>buffer</code> is a digit, <code>ch</code>
     * is inserted before it. If it is not a digit, it is replaced by <code>ch</code>.
     * @param buffer
     * @param ch
     */
    private void insertOrReplaceChar(StringBuilder buffer, char ch) {
        if (Character.isDigit(buffer.charAt(0))) {
            buffer.insert(0, ch);
        } else {
            buffer.replace(0, 1, Character.toString(ch));
        }
    }

    /**
     * Writes a line of a sudoku (cand be a border line). <code>fieldLengths</code>
     * holds the width of each column.
     * @param out
     * @param line
     * @param fieldLengths
     * @param cellBuffers
     * @param drawOutline
     */
    private void writeLine(StringBuilder out, int line, int[] fieldLengths,
            StringBuilder[] cellBuffers, boolean drawOutline, String separator) {
        if (drawOutline) {
            char leftRight = '.';
            char middle = '.';
            if (line == 3 || line == 6) {
                leftRight = ':';
                middle = '+';
            } else if (line == 9) {
                leftRight = '\'';
                middle = '\'';
            }
            out.append(leftRight);
            //for (int i = 0; i < 3 * fieldLength; i++) {
            for (int i = 0; i < fieldLengths[0] + fieldLengths[1] + fieldLengths[2]; i++) {
                out.append('-');
            }
            out.append(middle);
            for (int i = 0; i < fieldLengths[3] + fieldLengths[4] + fieldLengths[5]; i++) {
                out.append('-');
            }
            out.append(middle);
            for (int i = 0; i < fieldLengths[6] + fieldLengths[7] + fieldLengths[8]; i++) {
                out.append('-');
            }
            out.append(leftRight);
        } else {
            for (int i = line * 9; i < (line + 1) * 9; i++) {
                if ((i % 3) == 0) {
                    out.append("|");
                    if ((i % 9) != 8) {
                        out.append(' ');
                    }
                } else {
                    out.append(' ');
                }
                int tmp = fieldLengths[getCol(i)];
                out.append(cellBuffers[i]);
                tmp -= cellBuffers[i].length();
                for (int j = 0; j < tmp - 1; j++) {
                    out.append(' ');
                }
            }
            out.append('|');
        }
        out.append(separator);
    }

    /**
     * Returns the value set in <code>index</code> or 0, if
     * the cell is not set.
     * @param line 
     * @param col 
     * @return
     */
    public int getValue(int line, int col) {
        return getValue(getIndex(line, col));
    }

    /**
     * Returns the value set in <code>index</code> or 0, if
     * the cell is not set.
     * @param index
     * @return
     */
    public int getValue(int index) {
        return values[index];
    }

    /**
     * Returns the solution set in <code>index</code> or 0, if
     * the solution is unknown.
     * @param line 
     * @param col 
     * @return
     */
    public int getSolution(int line, int col) {
        return getSolution(getIndex(line, col));
    }

    /**
     * Returns the solution set in <code>index</code> or 0, if
     * the solution is unknown.
     * @param index
     * @return
     */
    public int getSolution(int index) {
        if (solutionSet == false) {
            return 0;
        }
        return solution[index];
    }

    /**
     * Returns if <code>index</code> is a given.
     *
     * @param line
     * @param col
     * @return
     */
    public boolean isFixed(int line, int col) {
        return isFixed(getIndex(line, col));
    }

    /**
     * Returns if <code>index</code> is a given.
     * @param index
     * @return
     */
    public boolean isFixed(int index) {
        return fixed[index];
    }

    /**
     * Set a cell as given.
     * @param index
     * @param isFixed
     */
    public void setIsFixed(int index, boolean isFixed) {
        fixed[index] = isFixed;
    }

    /**
     * Checks if <code>cand</code> is set as candidate in <code>line</code>/<code>col</code>.
     *
     * @param line
     * @param col
     * @param cand
     * @return
     */
    public boolean isCandidate(int line, int col, int cand) {
        return isCandidate(getIndex(line, col), cand);
    }

    /**
     * Checks if <code>cand</code> is set as candidate in <code>index</code>.
     *
     * @param index
     * @param cand
     * @return
     */
    public boolean isCandidate(int index, int cand) {
        return ((cells[index] & MASKS[cand]) != 0);
//        return candidates[cand].contains(index);
    }

    /**
     * Checks if <code>cand</code> is set as candidate in <code>line</code>/<code>col</code>.
     *
     * @param line
     * @param col
     * @param cand
     * @param user
     * @return
     */
    public boolean isCandidate(int line, int col, int cand, boolean user) {
        return isCandidate(getIndex(line, col), cand, user);
    }

    /**
     * Checks if <code>cand</code> is set as candidate in <code>index</code>.
     * If <code>user</code> is <code>true</code>, the test is made against
     * {@link #userCells}, if it is <code>false</code>, against
     * {@link #cells}.
     * @param index
     * @param cand
     * @param user
     * @return
     */
    public boolean isCandidate(int index, int cand, boolean user) {
        if (user) {
//            return userCandidates[cand].contains(index);
            return ((userCells[index] & MASKS[cand]) != 0);
        } else {
            return isCandidate(index, cand);
        }
    }

    /**
     * Checks, if a given candidate is set and valid; used for display.
     * @param index
     * @param value
     * @param user
     * @return
     */
    public boolean isCandidateValid(int index, int value, boolean user) {
        if (isCandidate(index, value, user) && isValidValue(index, value)) {
            return true;
        }
        return false;
    }

    /**
     * Checks, if a given set of candidates is present in the cell
     * @param index
     * @param candidates 
     * @param user
     * @return
     */
    public boolean areCandidatesValid(int index, boolean[] candidates, boolean user) {
        if (values[index] != 0) {
            return false;
        }
        if (candidates[candidates.length - 1] == true) {
            return getAnzCandidates(index) == 2;
        }
        if (!Options.getInstance().isUseOrInsteadOfAndForFilter()) {
            for (int i = 1; i < candidates.length - 1; i++) {
                if (candidates[i] && !isCandidate(index, i, user)) {
                    return false;
                }
            }
            return true;
        } else {
            for (int i = 1; i < candidates.length; i++) {
                if (candidates[i] && isCandidate(index, i, user)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Returns a string containing all candidates for the given cell.
     * @param index 
     * @return
     */
    public String getCandidateString(int index) {
        StringBuilder tmp = new StringBuilder();
        int[] cands = POSSIBLE_VALUES[cells[index]];
        for (int i = 0; i < cands.length; i++) {
            tmp.append(cands[i]);
        }
        return tmp.toString();
    }

    /**
     * Fill a {@link SudokuSet} with the candidates for a cell.
     * @param line
     * @param col
     * @param candSet
     */
    public void getCandidateSet(int line, int col, SudokuSet candSet) {
        getCandidateSet(getIndex(line, col), candSet);
    }

    /**
     * Fill a {@link SudokuSet} with the candidates for a cell.
     * @param index
     * @param candSet
     */
    public void getCandidateSet(int index, SudokuSet candSet) {
        candSet.set(cells[index] << 1);
    }

    /**
     * Deletes a candidate. Delegates to {@link #setCandidate(int, int, boolean)}.
     * @param index
     * @param value
     * @return <code>false</code>, if the puzzle becomes invalid by deleting a candidate
     */
    public boolean delCandidate(int index, int value) {
        return setCandidate(index, value, false);
    }

    /**
     * Deletes a candidate. Delegates to {@link #setCandidate(int, int, boolean, boolean)}.
     * @param index
     * @param value
     * @param user
     * @return <code>false</code>, if the puzzle becomes invalid by deleting a candidate
     */
    public boolean delCandidate(int index, int value, boolean user) {
        return setCandidate(index, value, false, user);
    }

    /**
     * Sets a candidate. Delegates to {@link #setCandidate(int, int, boolean)}.
     * @param index
     * @param value
     */
    public void setCandidate(int index, int value) {
        setCandidate(index, value, true);
    }

    /**
     * Sets or deletes a candidate. Delegates to {@link #setCandidate(int, int, boolean)}.
     * @param line
     * @param col
     * @param value
     * @param set
     * @return <code>false</code>, if the puzzle becomes invalid by deleting a candidate
     */
    public boolean setCandidate(int line, int col, int value, boolean set) {
        return setCandidate(getIndex(line, col), value, set);
    }

    /**
     * Sets or deletes a candidate. The candidate is added to or
     * removed from {@link #cells} and {@link #free} is updated accordingly.
     * If the update of {@link #free} indicates a new single, it is added
     * to the appropriate queue.<br>
     * To support the {@link BacktrackingSolver} checks are made if the solution
     * becomes invalid by the move.
     * @param index
     * @param value
     * @param set
     * @return <code>false</code>, if the puzzle becomes invalid by deleting a candidate
     */
    public boolean setCandidate(int index, int value, boolean set) {
        if (set) {
            if ((cells[index] & MASKS[value]) == 0) {
                cells[index] |= MASKS[value];
                int newAnz = ANZ_VALUES[cells[index]];
                if (newAnz == 1) {
                    addNakedSingle(index, value);
                } else if (newAnz == 2) {
                    nsQueue.deleteNakedSingle(index);
                }
                for (int i = 0; i < CONSTRAINTS[index].length; i++) {
                    int newFree = ++free[CONSTRAINTS[index][i]][value];
                    if (newFree == 1) {
                        addHiddenSingle(CONSTRAINTS[index][i], value);
                    } else if (newFree == 2) {
                        hsQueue.deleteHiddenSingle(CONSTRAINTS[index][i], value);
                    }
                }
            }
        } else {
            if ((cells[index] & MASKS[value]) != 0) {
                cells[index] &= ~MASKS[value];
                if (cells[index] == 0) {
                    // puzzle invalid
                    return false;
                }
                if (ANZ_VALUES[cells[index]] == 1) {
                    addNakedSingle(index, CAND_FROM_MASK[cells[index]]);
                }
                for (int i = 0; i < CONSTRAINTS[index].length; i++) {
                    int newFree = --free[CONSTRAINTS[index][i]][value];
                    if (newFree == 1) {
                        addHiddenSingle(CONSTRAINTS[index][i], value);
                    } else if (newFree == 0) {
                        // can happen, if the candidate was invalid;
                        // invalid candidates produce an entry in the HS queue
                        // that has to be deleted again (BUG 3515379)
                        hsQueue.deleteHiddenSingle(CONSTRAINTS[index][i], value);
                    }
                }
            }
        }
        return true;
    }

    /**
     * Sets or deletes a candidate. Most work is delegated to
     * {@link #setCandidate(int, int, boolean)}. If
     * <code>user</code> is set, the changes are made
     * also in {@link #userCells}.
     *
     * @param index
     * @param value
     * @param set
     * @param user
     * @return <code>false</code>, if the puzzle becomes invalid by deleting a candidate
     */
    public boolean setCandidate(int index, int value, boolean set, boolean user) {
        boolean ret = setCandidate(index, value, set);
        if (user) {
            if (set) {
                userCells[index] |= MASKS[value];
            } else {
                userCells[index] &= ~MASKS[value];
            }
        }
        return ret;
    }

    /**
     * Sets or a value in or deletes a value from a cell.
     * Delegates to {@link #setCell(int, int) }.
     * @param line
     * @param col
     * @param value
     * @return <code>false</code>, if the puzzle becomes invalid by setting a cell
     */
    public boolean setCell(int line, int col, int value) {
        return setCell(getIndex(line, col), value);
    }

    /**
     * Sets or a value in or deletes a value from a cell.
     * Delegates to {@link #setCell(int, int, boolean, boolean) }.
     * @param index
     * @param value
     * @return <code>false</code>, if the puzzle becomes invalid by setting a cell
     */
    public boolean setCell(int index, int value) {
        return setCell(index, value, false, true);
    }

    /**
     * Sets or a value in or deletes a value from a cell.
     * Delegates to {@link #setCell(int, int, boolean, boolean) }.
     * @param line
     * @param col
     * @param value
     * @param isFixed
     * @return <code>false</code>, if the puzzle becomes invalid by setting a cell
     */
    public boolean setCell(int line, int col, int value, boolean isFixed) {
        return setCell(getIndex(line, col), value, isFixed, true);
    }

    /**
     * Sets or a value in or deletes a value from a cell.
     * Delegates to {@link #setCell(int, int, boolean, boolean) }.
     * @param index
     * @param value
     * @param isFixed
     * @return
     */
    public boolean setCell(int index, int value, boolean isFixed) {
        return setCell(index, value, isFixed, true);
    }

    /**
     * Sets or a value in or deletes a value from a cell. The internal candidates
     * are automatically adapted:
     * <ul>
     *    <li>If a value is set, the candidate is removed from all buddies</li>
     *    <li>If a value is removed, the candidate is added to all unsolved cells in
     *        which it is not invalid.</li>
     * </ul>
     * Setting a value automatically affects {@link #userCells} as well, removing it
     * leaves the user candidates unchanged. {@link #unsolvedCellsAnz} is
     * changed accordingly.<br>
     * Eliminating candidates in the buddies automatically makes the correct
     * entries in the Hidden Singles queue. If a cell is set, a manual check
     * for Hidden Singles in the cell's constraints is done.
     * @param index
     * @param value
     * @param isFixed
     * @param user 
     * @return <code>false</code>, if the puzzle becomes invalid by setting a cell
     */
    public boolean setCell(int index, int value, boolean isFixed, boolean user) {
        if (value == 0) {
//            System.out.println("setCell(" + index + ", " + value + ", " + isFixed + ", " + user + ");");
        }
        if (values[index] == value) {
            // nothing to do
            return true;
        }
        boolean valid = true; // puzzle still valid after setting a cell?
        int oldValue = values[index]; // needed for delete
        values[index] = value;
        fixed[index] = isFixed;
        if (value != 0) {
//            System.out.println("   set " + index + "/" + value);
            // set a cell
            // adjust mask and check for Hidden Singles
            int[] cands = POSSIBLE_VALUES[cells[index]];
            cells[index] = 0;
            if (user) {
                userCells[index] = 0;
            }
            unsolvedCellsAnz--;
            // check the buddies
            for (int i = 0; i < buddies[index].size(); i++) {
                int buddyIndex = buddies[index].get(i);
                // candidates are deleted in userCells as well
                // delCandidate does the check for Naked or Hidden Single
//                if (! delCandidate(buddyIndex, value, true)) {
                if (!setCandidate(buddyIndex, value, false)) {
                    valid = false;
                }
                if (user) {
                    userCells[buddyIndex] &= ~MASKS[value];
                }
            }
            // now check all candidates from the cell itself
            for (int i = 0; i < cands.length; i++) {
                int cand = cands[i];
                for (int j = 0; j < CONSTRAINTS[index].length; j++) {
                    int constr = CONSTRAINTS[index][j];
                    int newFree = --free[constr][cand];
                    if (newFree == 1 && value != cand) {
                        addHiddenSingle(constr, cand);
                    } else if (newFree == 0 && cand != value) {
                        valid = false;
                    }
                }
            }
        } else {
            // in the cell itself all candidates that are possible are set
            // userCandidates is not changed
            for (int cand = 1; cand <= 9; cand++) {
                if (isValidValue(index, cand)) {
                    setCandidate(index, cand);
                }
            }
            // the deleted value is a candidate in all buddies that are not set and not invalid
            for (int i = 0; i < buddies[index].size(); i++) {
                int buddyIndex = buddies[index].get(i);
                if (getValue(buddyIndex) == 0 && isValidValue(buddyIndex, oldValue)) {
                    setCandidate(buddyIndex, oldValue);
                }
            }
            // the singles queues get invalid by deleting a value from a cell
            // -> rebuild everything from scratch!
            // adjusts unsolvedCellsAnz as well!
//            unsolvedCellsAnz++;
            rebuildInternalData();
        }
        return valid;
    }

    public void setCellBS(int index, int value) {
//        if (values[index] == value) {
//            // nothing to do
//            return true;
//        }
        values[index] = value;
//            System.out.println("   set " + index + "/" + value);
        // set a cell
        // adjust mask
        cells[index] = 0;
        // check the buddies
        for (int i = 0; i < buddies[index].size(); i++) {
            int buddyIndex = buddies[index].get(i);
            cells[buddyIndex] &= ~MASKS[value];
        }
    }

    /**
     * Checks if a certain value is valid for a certain index.
     * 
     * @param line
     * @param col
     * @param value
     * @return
     */
    public boolean isValidValue(int line, int col, int value) {
        return isValidValue(getIndex(line, col), value);
    }

    /**
     * Checks if a certain value is valid for a certain index. The value is invalid
     * if one of the buddies contains that value already.<br>
     * This method works for candidates too.
     * @param index 
     * @param value 
     * @return
     */
    public boolean isValidValue(int index, int value) {
        for (int i = 0; i < buddies[index].size(); i++) {
            if (values[buddies[index].get(i)] == value) {
                // value set in a buddy -> invalid
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the index into {@link #LINES} from the cell index.
     * @param index
     * @return
     */
    public static int getLine(int index) {
        return index / UNITS;
    }

    /**
     * Calculates the index into {@link #COLS} from the cell index.
     * @param index
     * @return
     */
    public static int getCol(int index) {
        return index % UNITS;
    }

    /**
     * Calculates the index into {@link #BLOCKS} from the cell index.
     * @param index
     * @return
     */
    public static int getBlock(int index) {
        return BLOCK_FROM_INDEX[index];
    }

    /**
     * Calculates the cell index. <code>line</code> and <code>col</code> are
     * zero based.
     * @param line
     * @param col
     * @return
     */
    public static int getIndex(int line, int col) {
        return line * 9 + col;
    }

    /**
     * Checks if the sudoku has been solved completely.
     * @return
     */
    public boolean isSolved() {
//        for (int i = 0; i < values.length; i++) {
//            if (values[i] == 0) {
//                return false;
//            }
//        }
//        return true;
        return unsolvedCellsAnz == 0;
    }

    /**
     * Determines the number of cells that have already been set.
     * @return
     */
    public int getSolvedCellsAnz() {
        return LENGTH - unsolvedCellsAnz;
    }

    /**
     * Determines the number of givens.
     * @return
     */
    public int getFixedCellsAnz() {
        int anz = 0;
        for (int i = 0; i < fixed.length; i++) {
            if (fixed[i]) {
                anz++;
            }
        }
        return anz;
    }

    /**
     * Returns the number of candidates in all unsolved cells. Needed
     * for progress bar when solving puzzles.
     * @return
     */
    public int getUnsolvedCandidatesAnz() {
        int anz = 0;
        for (int i = 0; i < cells.length; i++) {
            anz += ANZ_VALUES[cells[i]];
        }
        return anz;
    }

    /**
     * Calculates buddies. Needed for checks, if cells see each other ("are buddies").
     * Two cells <code>index1</code> and <code>index2</code> can see each other if
     * <code>buddies[index1].contains(index2)</codes> returns <code>true</code>.
     */
    private static void initBuddies() {
        if (buddies[0] != null) {
            // ist schon initialisiert
            return;
        }

        // ein Template mit allen Buddies pro Zelle
        for (int i = 0; i < 81; i++) {
            buddies[i] = new SudokuSet();
            for (int j = 0; j < 81; j++) {
                if (i != j && (Sudoku2.getLine(i) == Sudoku2.getLine(j)
                        || Sudoku2.getCol(i) == Sudoku2.getCol(j)
                        || Sudoku2.getBlock(i) == Sudoku2.getBlock(j))) {
                    // Zelle ist Buddy
                    buddies[i].add(j);
                }
            }
            buddiesM1[i] = buddies[i].getMask1();
            buddiesM2[i] = buddies[i].getMask2();
        }

        // Ein Set für jedes Haus mit allen Zellen des Hauses
        for (int i = 0; i < UNITS; i++) {
            LINE_TEMPLATES[i] = new SudokuSet();
            for (int j = 0; j < LINES[i].length; j++) {
                LINE_TEMPLATES[i].add(LINES[i][j]);
            }
            LINE_BLOCK_TEMPLATES[i] = LINE_TEMPLATES[i];
            ALL_CONSTRAINTS_TEMPLATES[i] = LINE_TEMPLATES[i];
            COL_TEMPLATES[i] = new SudokuSet();
            for (int j = 0; j < COLS[i].length; j++) {
                COL_TEMPLATES[i].add(COLS[i][j]);
            }
            COL_BLOCK_TEMPLATES[i] = COL_TEMPLATES[i];
            ALL_CONSTRAINTS_TEMPLATES[i + 9] = COL_TEMPLATES[i];
            BLOCK_TEMPLATES[i] = new SudokuSet();
            for (int j = 0; j < BLOCKS[i].length; j++) {
                BLOCK_TEMPLATES[i].add(BLOCKS[i][j]);
            }
            LINE_BLOCK_TEMPLATES[i + 9] = BLOCK_TEMPLATES[i];
            COL_BLOCK_TEMPLATES[i + 9] = BLOCK_TEMPLATES[i];
            ALL_CONSTRAINTS_TEMPLATES[i + 18] = BLOCK_TEMPLATES[i];
        }
        for (int i = 0; i < ALL_CONSTRAINTS_TEMPLATES.length; i++) {
            ALL_CONSTRAINTS_TEMPLATES_M1[i] = ALL_CONSTRAINTS_TEMPLATES[i].getMask1();
            ALL_CONSTRAINTS_TEMPLATES_M2[i] = ALL_CONSTRAINTS_TEMPLATES[i].getMask2();
        }
    }

    /**
     * Optimization: For every group of 8 cells all possible buddies -> 11 * 256 combinations.
     * These group buddies are used by SudokuSetBase: might speed up the search for
     * all possible buddies of multiple units (mainly in fish and ALS search)
     */
    private static void initGroupedBuddies() {
        for (int i = 0; i < 11; i++) {
            initGroupForGroupedBuddies(i * 8, groupedBuddies[i]);
        }
        for (int i = 0; i < groupedBuddies.length; i++) {
            for (int j = 0; j < groupedBuddies[i].length; j++) {
                groupedBuddiesM1[i][j] = groupedBuddies[i][j].getMask1();
                groupedBuddiesM2[i][j] = groupedBuddies[i][j].getMask2();
            }
        }
    }

    /**
     * First compute all possible combinations of 8 cells starting with
     * groupOffset, then get a set with all budies for every combination.
     * 
     * @param groupOffset The first index in the group of 8 cells
     * @param groupArray The array that stores all possible buddy sets
     */
    private static void initGroupForGroupedBuddies(int groupOffset, SudokuSetBase[] groupArray) {
        SudokuSet groupSet = new SudokuSet();
        for (int i = 0; i < 256; i++) {
            groupSet.clear();
            int mask = 0x01;
            for (int j = 0; j < 8; j++) {
                //System.out.print("i: " + i + ", i: " + i + " (mask: " + Integer.toHexString(mask) + ") ");
                if ((i & mask) != 0 && (groupOffset + j + 1) <= 81) {
                    groupSet.add(groupOffset + j);
                    //System.out.print("  ADD: " + (groupOffset + i + 1));
                }
                //System.out.println();
                mask <<= 1;
            }
            SudokuSetBase buddiesSet = new SudokuSetBase(true);
            for (int j = 0; j < groupSet.size(); j++) {
                buddiesSet.and(buddies[groupSet.get(j)]);
            }
            groupArray[i] = buddiesSet;
            //System.out.println("Grouped Buddies " + groupOffset + "/" + i + ": " + groupSet + ", " + buddiesSet);
        }
    }

    /**
     * Calculates all common buddies of all cells in <code>cells</code> and
     * returns them in <code>buddies</code>. groupedBuddies is used
     * for calculations.
     * 
     * @param cells The cells for which the buddies should be calculated
     * @param buddiesOut The resulting buddies
     */
    public static void getBuddies(SudokuSetBase cells, SudokuSetBase buddiesOut) {
        buddiesOut.setAll();
        if (cells.mask1 != 0) {
            for (int i = 0, j = 0; i < 8; i++, j += 8) {
                int mIndex = (int) ((cells.mask1 >> j) & 0xFF);
                buddiesOut.and(groupedBuddies[i][mIndex]);
            }
        }
        if (cells.mask2 != 0) {
            for (int i = 8, j = 0; i < 11; i++, j += 8) {
                int mIndex = (int) ((cells.mask2 >> j) & 0xFF);
                buddiesOut.and(groupedBuddies[i][mIndex]);
            }
        }
    }

    /**
     * Calculates all common buddies of all cells in <code>cells</code> and
     * returns them in <code>buddies</code>. groupedBuddies is used
     * for calculations.
     *
     * @param mask1 
     * @param mask2 
     * @param buddiesOut The resulting buddies
     */
    public static void getBuddies(long mask1, long mask2, SudokuSetBase buddiesOut) {
        long outM1 = SudokuSetBase.MAX_MASK1;
        long outM2 = SudokuSetBase.MAX_MASK2;
        if (mask1 != 0) {
            for (int i = 0, j = 0; i < 8; i++, j += 8) {
                int mIndex = (int) ((mask1 >> j) & 0xFF);
                outM1 &= groupedBuddiesM1[i][mIndex];
                outM2 &= groupedBuddiesM2[i][mIndex];
            }
        }
        if (mask2 != 0) {
            for (int i = 8, j = 0; i < 11; i++, j += 8) {
                int mIndex = (int) ((mask2 >> j) & 0xFF);
                outM1 &= groupedBuddiesM1[i][mIndex];
                outM2 &= groupedBuddiesM2[i][mIndex];
            }
        }
        buddiesOut.set(outM1, outM2);
    }

    /**
     * Calculates all common buddies of all cells in <code>cells</code> and
     * returns them in <code>buddies</code>. Calculations are done the
     * traditional way.
     * 
     * @param cells The cells for which the buddies should be calculated
     * @param buddiesOut The resulting buddies
     */
//    public static void getBuddiesWG(SudokuSet cells, SudokuSetBase buddiesOut) {
//        buddiesOut.setAll();
//        for (int i = 0; i < cells.size(); i++) {
//            buddiesOut.and(buddies[cells.get(i)]);
//        }
//    }
    /**
     * Create all 46656 possible templates. Since the calculation has become incredibly
     * slow on Windows 7 64bit, the templates are read from a file.
     */
    @SuppressWarnings("CallToThreadDumpStack")
    private static void initTemplates() {
        // alle 46656 möglichen Templates anlegen
        try {
            //System.out.println("Start Templates lesen...");
            long ticks = System.currentTimeMillis();
            ObjectInputStream in = new ObjectInputStream(Sudoku2.class.getResourceAsStream("/templates.dat"));
            templates = (SudokuSetBase[]) in.readObject();
            in.close();
            ticks = System.currentTimeMillis() - ticks;
            //System.out.println("Templates lesen: " + ticks + "ms");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
//        //Sudoku2 sudoku = new Sudoku2(false);
//        Sudoku2 sudoku = new Sudoku2();
//        SudokuSetBase set = new SudokuSetBase();
//        initTemplatesRecursive(sudoku, 0, 0, 1, set);
//        try {
////            PrintWriter out = new PrintWriter(new FileWriter("templates.txt"));
////            for (int i = 0; i < templates.length; i++) {
////                if ((i % 2) == 0) {
////                    out.print("        new SudokuSetBase(" + templates[i].mask1 + "L, " + templates[i].mask2 + "L),");
////                } else {
////                    out.println(" new SudokuSetBase(" + templates[i].mask1 + "L, " + templates[i].mask2 + "L),");
////                }
////            }
////            out.close();
//            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("templates.dat"));
//            out.writeObject(templates);
//            out.close();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }

        // jetzt noch die Templates für die Häuser
        for (int i = 0; i < LINES.length; i++) {
            for (int j = 0; j < LINES[i].length; j++) {
                LINE_TEMPLATES[i].add(LINES[i][j]);
                COL_TEMPLATES[i].add(COLS[i][j]);
                BLOCK_TEMPLATES[i].add(BLOCKS[i][j]);
            }
        }
    }

//    private static int initTemplatesRecursive(Sudoku2 sudoku, int line, int index, int cand, SudokuSetBase set) {
//        if (line >= Sudoku2.LINES.length) {
//            templates[index++] = set.clone();
//            return index;
//        }
//        for (int i = 0; i < Sudoku2.LINES[line].length; i++) {
//            int lIndex = Sudoku2.LINES[line][i];
//            SudokuCell cell = sudoku.getCell(lIndex);
//            if (cell.getValue() != 0 || !cell.isCandidate(SudokuCell.ALL, cand)) {
//                // Zelle ist schon belegt (darf eigentlich nicht sein) oder der Kandidat ist hier nicht mehr m�glich
//                continue;
//            }
//            // Kandidat in dieser Zeile setzen
//            sudoku.setCell(lIndex, cand);
//            set.add(lIndex);
//            // weiter in n�chste Zeile
//            index = initTemplatesRecursive(sudoku, line + 1, index, cand, set);
//            // und Zelle wieder l�schen
//            sudoku.setCell(lIndex, 0);
//            set.remove(lIndex);
//        }
//        return index;
//    }
    /**
     * Makes all cells editable; needed to edit a puzzle
     */
    public void setNoClues() {
        for (int i = 0; i < fixed.length; i++) {
            fixed[i] = false;
        }
        setStatusGivens(SudokuStatus.INVALID);
    }

    /**
     * Gets a 81 character string. For every digit in that string, the corresponding cell is set
     * as a given.<br>
     * 
     * Since the givens are changed, the {@link #statusGivens} has to be
     * rechecked.
     *
     * @param givens
     */
    public void setGivens(String givens) {
        for (int i = 0; i < givens.length(); i++) {
            char ch = givens.charAt(i);
            if (Character.isDigit(ch) && ch != '0') {
                fixed[i] = true;
            } else {
                fixed[i] = false;
            }
        }
        Sudoku2 act = new Sudoku2();
        act.set(this);
        SudokuGenerator generator = SudokuGeneratorFactory.getDefaultGeneratorInstance();
        int anzSol = generator.getNumberOfSolutions(act);
        setStatusGivens(anzSol);
    }

    /**
     * Adds a new Naked Single to the Naked Single queue formed by
     * the synchronized arrays {@link #nsIndices} and {@link #nsValues}.
     * @param index
     * @param value
     */
    private void addNakedSingle(int index, int value) {
        nsQueue.addSingle(index, value);
    }

    /**
     * Adds a new Hidden Single to the Hidden Single queue formed by
     * the synchronized arrays {@link #hsIndices} and {@link #hsValues}.
     * The cell containing the single has to be searched for in the
     * corresponding constraint.
     * @param index
     * @param value
     * @return Returns, if Hidden Single could be found
     */
    private boolean addHiddenSingle(int constraint, int value) {
        for (int i = 0; i < ALL_UNITS[constraint].length; i++) {
            // Hidden Single: The candidate in question is present in only one cell of
            // the constraint
            int hsIndex = ALL_UNITS[constraint][i];
            if (isCandidate(hsIndex, value)) {
                // Hidden Single found -> store it
                hsQueue.addSingle(hsIndex, value);
//                System.out.println("addHiddenSingle: " + hsIndex + "/" + value);
                return true;
            }
        }
        return false;
    }

    /**
     * @return the score
     */
    public int getScore() {
        return score;
    }

    /**
     * @param score the score to set
     */
    public void setScore(int score) {
        this.score = score;
    }

    /**
     * @return the level
     */
    public DifficultyLevel getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(DifficultyLevel level) {
        this.level = level;
    }

    /**
     * @return the initialState
     */
    public String getInitialState() {
        return initialState;
    }

    /**
     * @param initialState the initialState to set
     */
    public void setInitialState(String initialState) {
        this.initialState = initialState;
    }

    /**
     * @return the values
     */
    public int[] getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(int[] values) {
        this.values = values;
    }

    /**
     * @return the solution
     */
    public int[] getSolution() {
        return solution;
    }

    /**
     * @param solution the solution to set
     */
    public void setSolution(int[] solution) {
        this.solution = solution;
        solutionSet = true;
    }

    /**
     * @return the solutionSet
     */
    public boolean isSolutionSet() {
        return solutionSet;
    }

    /**
     * @param solutionSet the solutionSet to set
     */
    public void setSolutionSet(boolean solutionSet) {
        this.solutionSet = solutionSet;
    }

    /**
     * @return the fixed
     */
    public boolean[] getFixed() {
        return fixed;
    }

    /**
     * @param fixed the fixed to set
     */
    public void setFixed(boolean[] fixed) {
        this.fixed = fixed;
    }

    /**
     * Get the candidate mask of one single cell
     * @param index
     * @return
     */
    public short getCell(int index) {
        return cells[index];
    }

    /**
     * @return the cells
     */
    public short[] getCells() {
        return cells;
    }

    /**
     * @param cells the cells to set
     */
    public void setCells(short[] cells) {
        this.cells = cells;
    }

    /**
     * @return the userCells
     */
    public short[] getUserCells() {
        return userCells;
    }

    /**
     * @param userCells the userCells to set
     */
    public void setUserCells(short[] userCells) {
        this.userCells = userCells;
    }

    /**
     * @return the free
     */
    public byte[][] getFree() {
        return free;
    }

    /**
     * @param free the free to set
     */
    public void setFree(byte[][] free) {
        this.free = free;
    }

    /**
     * @return the number of unsolved cells
     */
    public int getUnsolvedCellsAnz() {
        return unsolvedCellsAnz;
    }

    /**
     * @param unsolvedCellsAnz the unsolvedCellsAnz to set
     */
    public void setUnsolvedCellsAnz(int unsolvedCellsAnz) {
        this.unsolvedCellsAnz = unsolvedCellsAnz;
    }

    /**
     * @return the nsQueue
     */
    public SudokuSinglesQueue getNsQueue() {
        return nsQueue;
    }

    /**
     * @param nsQueue the nsQueue to set
     */
    public void setNsQueue(SudokuSinglesQueue nsQueue) {
        this.nsQueue = nsQueue;
    }

    /**
     * @return the hsQueue
     */
    public SudokuSinglesQueue getHsQueue() {
        return hsQueue;
    }

    /**
     * @param hsQueue the hsQueue to set
     */
    public void setHsQueue(SudokuSinglesQueue hsQueue) {
        this.hsQueue = hsQueue;
    }

    /**
     * @return the status
     */
    public SudokuStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(SudokuStatus status) {
        this.status = status;
    }

    /**
     * @param numberOfSolutions the number of solutions for this sudoku
     */
    public void setStatus(int numberOfSolutions) {
        switch (numberOfSolutions) {
            case 0:
                status = SudokuStatus.INVALID;
                break;
            case 1:
                status = SudokuStatus.VALID;
                break;
            default:
                status = SudokuStatus.MULTIPLE_SOLUTIONS;
                break;
        }
    }

    /**
     * @return the statusGivens
     */
    public SudokuStatus getStatusGivens() {
        return statusGivens;
    }

    /**
     * @param statusGivens the statusGivens to set
     */
    public void setStatusGivens(SudokuStatus statusGivens) {
        this.statusGivens = statusGivens;
    }

    /**
     * @param numberOfSolutions the number of solutions for this sudoku
     */
    public void setStatusGivens(int numberOfSolutions) {
        switch (numberOfSolutions) {
            case 0:
                statusGivens = SudokuStatus.INVALID;
                break;
            case 1:
                statusGivens = SudokuStatus.VALID;
                break;
            default:
                statusGivens = SudokuStatus.MULTIPLE_SOLUTIONS;
                break;
        }
    }

    /**
     * Checks, if user candidate have been set in the Sudoku. Needed for
     * determining, how to switch between {@link #cells} and {@link #userCells}.
     * 
     * @return 
     */
    public boolean userCandidatesEmpty() {
        for (int i = 0; i < userCells.length; i++) {
            if (userCells[i] != 0) {
                //at least one candidate has already been set
                return false;
            }
        }
        return true;
    }

    /**
     * Switches from user candidates to all candidates: candidates already
     * eliminated by the user stay eliminated. As a precaution, missing necessary
     * candidates are added first. This will only work, if a solution has already
     * been set. It is the responsibility of the caller to ensure that.
     */
    public void switchToAllCandidates() {
        // first add necessary candidates (might not be necessary)
        for (int i = 0; i < userCells.length; i++) {
            if (values[i] == 0 && solution[i] != 0) {
                userCells[i] |= MASKS[solution[i]];
            }
        }
        // now simply copy the user candidates over
        System.arraycopy(userCells, 0, cells, 0, LENGTH);
        // rebuild internal data
        rebuildInternalData();
    }

    /**
     * Reset {@link #cells} to all possible candidates.
     */
    public void rebuildAllCandidates() {
        for (int i = 0; i < cells.length; i++) {
            if (values[i] != 0) {
                cells[i] = 0;
            } else {
                for (int cand = 1; cand <= 9; cand++) {
                    if (isValidValue(i, cand)) {
                        cells[i] |= MASKS[cand];
                    }
                }
            }
        }
        // rebuild internal data
        rebuildInternalData();
    }

    /**
     * Print the contents of the singles queues to stdout. For debugging
     * only.
     * 
     */
    public void printSinglesQueues() {
        System.out.println("Naked Singles:\r\n");
        System.out.println(nsQueue);
        System.out.println("Hidden Singles:\r\n");
        System.out.println(hsQueue);
    }

    /**
     * Calculates, which candidates are still present in the unset cells
     * of the sudoku.
     * @return 
     */
    public short getRemainingCandidates() {
        short result = 0;
        for (int i = 0; i < cells.length; i++) {
            if (values[i] == 0) {
                result |= cells[i];
            }
        }
        return result;
    }
}
