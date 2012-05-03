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
 * 
 * This code is actually a Java port of code posted by Glenn Fowler
 * in the Sudoku Player's Forum (http://www.setbb.com/sudoku).
 * Many thanks for letting me use it!
 */
package generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import sudoku.Options;
import sudoku.Sudoku2;
import sudoku.SudokuSinglesQueue;
import sudoku.SudokuStatus;

/**
 * Bit based backtracking solver.
 * 
 * @author hobiwan
 */
public class SudokuGenerator {
    /** Debug flag */
    private static final boolean DEBUG = false;
    
    /** Maximum number of tries when generating a puzzle using a pattern */
    private static final int MAX_TRIES = 1000000;

    /** Empty sudoku for initialization */
    private static Sudoku2 EMPTY_GRID = new Sudoku2();

    /**
     * One entry in recursion stack
     */
    private class RecursionStackEntry {

        /** The current state of the sudoku */
        Sudoku2 sudoku = new Sudoku2();
        /** The index of the cell thats being tried */
        int index;
        /** The candidates for cells {@link #index}. */
        int[] candidates;
        /** The index of the last tried candidate in {@link #candidates}. */
        int candIndex;
    }
    /** The cells values of the first solution */
    private int[] solution = new int[81];
    /** Number of solutions already found */
    private int solutionCount = 0;
    /** The recursion stack */
    private RecursionStackEntry[] stack = new RecursionStackEntry[82];
    /** The order in which cells are set when generating a full grid. */
    private int[] generateIndices = new int[81];
    /** The cells of a newly generated sudoku (full board) */
    private int[] newFullSudoku = new int[81];
    /** The cells of a newly generated valid sudoku. */
    private int[] newValidSudoku = new int[81];
    /** A random generator for creating new puzzles. */
    private Random rand = new Random();

    private int anzTries = 0;
    private int anzNS = 0;
    private int anzHS = 0;
    private int anzTriesGen = 0;
    private int anzClues = 0;
    private long nanos = 0;
    private long setNanos = 0;
    private long actSetNanos = 0;

    /** Creates a new instance of SudokuGenerator */
    protected SudokuGenerator() {
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new RecursionStackEntry();
        }
    }

    /**
     * Checks if <code>sudoku</code> has exactly one solution. If it
     * has, the solution is stored in the sudoku.
     * 
     * @param sudoku
     * @return 0 (invalid), 1 (valid), or 2 (multiple solutions)
     */
    public int getNumberOfSolutions(Sudoku2 sudoku) {
        long ticks = System.currentTimeMillis();
        solve(sudoku);
        if (solutionCount == 1) {
            sudoku.setSolution(Arrays.copyOf(solution, solution.length));
        }
        ticks = System.currentTimeMillis() - ticks;
        Logger.getLogger(getClass().getName()).log(Level.FINE, "validSolution() {0}ms", ticks);
        return solutionCount;
    }
    
    /**
     * Checks if <code>sudoku</code> has exactly one solution. If it
     * has, the solution is stored in the sudoku.
     * 
     * @param sudoku
     * @return
     */
    public boolean validSolution(Sudoku2 sudoku) {
        long ticks = System.currentTimeMillis();
        solve(sudoku);
        boolean unique = solutionCount == 1;
        if (unique) {
            sudoku.setSolution(Arrays.copyOf(solution, solution.length));
        }
        ticks = System.currentTimeMillis() - ticks;
        Logger.getLogger(getClass().getName()).log(Level.FINE, "validSolution() {0}ms", ticks);
        return unique;
    }

    /**
     * Solves <code>sudoku</code>.
     * @param sudoku
     */
    private void solve(Sudoku2 sudoku) {
        // start with the current state of the sudoku
        stack[0].sudoku.set(sudoku);
        stack[0].index = 0;
        stack[0].candidates = null;
        stack[0].candIndex = 0;

        // solve it
        solve();
    }

    /**
     * Solves a sudoku given by a 81 character string.
     * @param sudokuString
     */
    public void solve(String sudokuString) {
        // start with an empty sudoku
        stack[0].sudoku.set(EMPTY_GRID);
        stack[0].candidates = null;
        stack[0].candIndex = 0;

        // set up the sudoku
        for (int i = 0; i < sudokuString.length() && i < Sudoku2.LENGTH; i++) {
            int value = sudokuString.charAt(i) - '0';
            if (value >= 1 && value <= 9) {
                stack[0].sudoku.setCell(i, value, false, false);
                setAllExposedSingles(stack[0].sudoku);
            }
        }
        // solve it
        solve();
    }

    /**
     * Solves a sudoku given by a 81 int array.
     * @param cellValues 
     */
    public void solve(int[] cellValues) {
//        System.out.println("start solving " + getSolutionAsString(cellValues));
//        actSetNanos = System.nanoTime();
        // start with an empty sudoku
        stack[0].sudoku.set(EMPTY_GRID);
        stack[0].candidates = null;
        stack[0].candIndex = 0;

//        // set up the sudoku
//        for (int i = 0; i < cellValues.length; i++) {
//            int value = cellValues[i];
//            if (value >= 1 && value <= 9) {
//                stack[0].sudoku.setCell(i, value, false, false);
//                setAllExposedSingles(stack[0].sudoku);
//            }
//        }
        // set up the sudoku
//        System.out.println("setting up sudoku...");
        for (int i = 0; i < cellValues.length; i++) {
            int value = cellValues[i];
            if (value >= 1 && value <= 9) {
                stack[0].sudoku.setCellBS(i, value);
            }
        }
        stack[0].sudoku.rebuildInternalData();
        setAllExposedSingles(stack[0].sudoku);
//        System.out.println("and solve...");

        // solve it
        solve();
    }

    /**
     * The real backtracking solver: Recursion is simulated by
     * a recursion stack ({@link #stack}), if Singles are exposed
     * during solving, they are set.
     */
    private void solve() {
        anzTries = 0;
        anzNS = 0;
        anzHS = 0;
        solutionCount = 0;
        // first set all Singles exposed by building up the Sudoku grid
        if (DEBUG) {
            System.out.println("solve start:");
        }
        if (!setAllExposedSingles(stack[0].sudoku)) {
            // puzzle was invalid all along
            if (DEBUG) {
                System.out.println("  puzzle was invalid!");
            }
            return;
        }
//        setNanos += System.nanoTime() - actSetNanos;
//        System.out.println("solve: " + getSolutionAsString(stack[0].sudoku.getValues()));
//        System.out.println("unsolvedCellsAnz = " + stack[0].sudoku.getUnsolvedCellsAnz());
        if (stack[0].sudoku.getUnsolvedCellsAnz() == 0) {
            // already solved, nothing to do
            solution = Arrays.copyOf(stack[0].sudoku.getValues(), Sudoku2.LENGTH);
            solutionCount++;
            if (DEBUG) {
                System.out.println("  puzzle was already solved!");
            }
            return;
        }
        int level = 0;
        while (true) {
            // get the next unsolved cells with the fewest number of candidates
            if (stack[level].sudoku.getUnsolvedCellsAnz() == 0) {
                // sudoku is solved
                solutionCount++;
                // count the solutions
                if (solutionCount == 1) {
                    // first solution is recorded
                    solution = Arrays.copyOf(stack[level].sudoku.getValues(), Sudoku2.LENGTH);
                } else if (solutionCount > 1) {
                    // but not more than 1000
                    if (DEBUG) {
                        System.out.println("  puzzle has more than one solution (" + solutionCount + ")!");
                    }
                    return;
                }
            } else {
                int index = -1;
                int anzCand = 9;
                Sudoku2 sudoku = stack[level].sudoku;
                for (int i = 0; i < Sudoku2.LENGTH; i++) {
//                    if (sudoku.getCell(i) != 0) {
//                        System.out.println("cell[" + i + "] = " + Sudoku2.ANZ_VALUES[sudoku.getCell(i)]);
//                    }
                    if (sudoku.getCell(i) != 0 && Sudoku2.ANZ_VALUES[sudoku.getCell(i)] < anzCand) {
                        index = i;
                        anzCand = Sudoku2.ANZ_VALUES[sudoku.getCell(i)];
                    }
                }
                level++;
                // missing candidates lead to exception -> avoid that
                if (index < 0) {
                    solutionCount = 0;
                    return;
                }
                stack[level].index = (short) index;
                stack[level].candidates = Sudoku2.POSSIBLE_VALUES[stack[level - 1].sudoku.getCell(index)];
                stack[level].candIndex = 0;
            }

            // go to the next level
            boolean done = false;
            do {
                // this loop runs as long as the next candidate tried produces an
                // invalid sudoku or until all possibilities have been tried

                // fall back all levels, where nothing is to do anymore
                while (stack[level].candIndex >= stack[level].candidates.length) {
                    level--;
                    if (level <= 0) {
                        // no level with candidates left
                        done = true;
                        break;
                    }
                }
                if (done) {
                    break;
                }
                // try the next candidate
                int nextCand = stack[level].candidates[stack[level].candIndex++];
                // start with a fresh sudoku
                anzTries++;
                stack[level].sudoku.setBS(stack[level - 1].sudoku);
                if (!stack[level].sudoku.setCell(stack[level].index, nextCand, false, false)) {
                    // invalid -> try next candidate
                    continue;
                }
                if (setAllExposedSingles(stack[level].sudoku)) {
                    // valid move, break from the inner loop to advance to the next level
                    break;
                }
            } while (true);
            if (done) {
                break;
            }
        }
        if (DEBUG) {
            System.out.println("  puzzle has " + solutionCount + " solution!");
        }
    }

    /**
     * Generates a new valid sudoku. If a pattern is set in
     * {@link Options} and it has already be checked for validity, 
     * it is used automatically.<br><br>
     * 
     * This method is used by the {@link BackgroundGenerator}.
     * 
     * @param symmetric
     * @return
     */
    public Sudoku2 generateSudoku(boolean symmetric) {
        int index = Options.getInstance().getGeneratorPatternIndex();
        boolean[] pattern = null;
        ArrayList<GeneratorPattern> patterns = Options.getInstance().getGeneratorPatterns();
        if (index != -1 && index < patterns.size() && patterns.get(index).isValid()) {
            pattern = patterns.get(index).getPattern();
        }
        return generateSudoku(symmetric, pattern);
    }
    
    /**
     * Generates a new valid sudoku. If <code>pattern</code> is not <code>null</code>,
     * it is used to determine the positions of the givens. If no sudoku could be generated
     * (only possible if a <code>pattern</code> is applied), the method returns 
     * <code>null</code>.<br><br>
     * 
     * This method is used by the validity checker in the {@link ConfigGeneratorPanel}.
     * 
     * @param symmetric
     * @param pattern
     * @return 
     */
    public Sudoku2 generateSudoku(boolean symmetric, boolean[] pattern) {
        generateFullGrid();
        if (pattern == null) {
            generateInitPos(symmetric);
        } else {
            boolean ok = false;
            System.out.println("Trying with pattern!");
            for (int i = 0; i < MAX_TRIES; i++) {
                if ((ok = generateInitPos(pattern)) == true) {
                    break;
                }
                if ((i % 1000) == 0) {
                    System.out.println("  try: " + i);
                }
            }
            if (! ok) {
                // no puzzle found in MAX_TRIES iterations
                System.out.println("nothing found!");
                return null;
            }
            System.out.println("puzzle found!");
        }
        // construct the new sudoku
        Sudoku2 sudoku = new Sudoku2();
        for (int i = 0; i < newValidSudoku.length; i++) {
            if (newValidSudoku[i] != 0) {
                sudoku.setCell(i, newValidSudoku[i]);
                sudoku.setIsFixed(i, true);
                anzClues++;
            }
        }
        //this sudoku is valid and has givens
        sudoku.setStatus(SudokuStatus.VALID);
        sudoku.setStatusGivens(SudokuStatus.VALID);
        return sudoku;
    }

    /**
     * Generates a new valid randomized grid. The real work is
     * done by {@link #doGenerateFullGrid()}, but since this
     * method can fail, we have to check for errors.
     */
    @SuppressWarnings("empty-statement")
    private void generateFullGrid() {
        while (doGenerateFullGrid() == false);
    }

    /**
     * Generates a new valid full sudoku grid. Works exactly like the
     * backtracking solver ({@link #solve()}), the cells are set in
     * random order.<br>
     * The method works very well most of the times, but somtimes 
     * (about 1.5% of all cases) it can take extremely long to get a
     * solution. It is then better to abort and try with a new randomized
     * index set.
     */
    private boolean doGenerateFullGrid() {
        anzTries = 0;
        anzNS = 0;
        anzHS = 0;
        // limit the number of tries
        int actTries = 0;
        // generate a random order for setting the cells
        int max = generateIndices.length;
        for (int i = 0; i < max; i++) {
            generateIndices[i] = i;
        }
        for (int i = 0; i < max; i++) {
            int index1 = rand.nextInt(max);
            int index2 = rand.nextInt(max);
            while (index1 == index2) {
                index2 = rand.nextInt(max);
            }
            int dummy = generateIndices[index1];
            generateIndices[index1] = generateIndices[index2];
            generateIndices[index2] = dummy;
        }
        // first set a new empty Sudoku
        stack[0].sudoku.set(EMPTY_GRID);
        int level = 0;
        stack[0].index = -1;
        while (true) {
            // get the next unsolved cell according to generateIndices
            if (stack[level].sudoku.getUnsolvedCellsAnz() == 0) {
                // generation is complete
                System.arraycopy(stack[level].sudoku.getValues(), 0, newFullSudoku, 0, newFullSudoku.length);
                return true;
            } else {
                int index = -1;
                int[] actValues = stack[level].sudoku.getValues();
                for (int i = 0; i < Sudoku2.LENGTH; i++) {
                    int actTry = generateIndices[i];
                    if (actValues[actTry] == 0) {
                        index = actTry;
                        break;
                    }
                }
                level++;
                stack[level].index = (short) index;
                stack[level].candidates = Sudoku2.POSSIBLE_VALUES[stack[level - 1].sudoku.getCell(index)];
                stack[level].candIndex = 0;
            }

            // not too many tries...
            actTries++;
            if (actTries > 100) {
                return false;
            }

            // go to the next level
            boolean done = false;
            do {
                // this loop runs as long as the next candidate tried produces an
                // invalid sudoku or until all possibilities have been tried

                // fall back all levels, where nothing is to do anymore
                while (stack[level].candIndex >= stack[level].candidates.length) {
                    level--;
                    if (level <= 0) {
                        // no level with candidates left
                        done = true;
                        break;
                    }
                }
                if (done) {
                    break;
                }
                // try the next candidate
                int nextCand = stack[level].candidates[stack[level].candIndex++];
                // start with a fresh sudoku
                anzTries++;
                stack[level].sudoku.setBS(stack[level - 1].sudoku);
                if (!stack[level].sudoku.setCell(stack[level].index, nextCand, false, false)) {
                    // invalid -> try next candidate
                    continue;
                }
                if (setAllExposedSingles(stack[level].sudoku)) {
                    // valid move, break from the inner loop to advance to the next level
                    break;
                }
            } while (true);
            if (done) {
                break;
            }
        }
        // we should never get till here...
        return false;
    }

    /**
     * Takes a full sudoku from {@link #newFullSudoku} and generates a valid
     * puzzle by deleting the cells indicated by <code>pattern</code>. If the
     * resulting puzzle is invalid, <code>false</code> is returned and the caller
     * is responsible for continuing the search.
     * 
     * @param pattern
     * @return 
     */
    private boolean generateInitPos(boolean[] pattern) {
        // we start with the full board
        System.arraycopy(newFullSudoku, 0, newValidSudoku, 0, newFullSudoku.length);
        // delete all cells indicated by pattern
//        System.out.println("Full:  " + Arrays.toString(newFullSudoku));
        for (int i = 0; i < pattern.length; i++) {
            if (pattern[i] == false) {
                newValidSudoku[i] = 0;
            }
        }
//        System.out.println("Valid: " + Arrays.toString(newValidSudoku));
//        long actNanos = System.nanoTime();
        solve(newValidSudoku);
//        nanos += System.nanoTime() - actNanos;
        if (solutionCount > 1) {
            return false;
        } else {
            System.out.println("!!!! FOUND ONE !!!!");
            return true;
        }
    }
    
    /**
     * Takes a full sudoku from {@link #newFullSudoku} and generates a valid
     * puzzle by deleting cells. If a deletion produces a grid with more
     * than one solution it is of course undone.<br><br>
     * 
     * @param isSymmetric
     * @param pattern
     */
    private void generateInitPos(boolean isSymmetric) {
        int maxPosToFill = 17; // no less than 17 givens
        boolean[] used = new boolean[81]; // try every cell only once
        int usedCount = used.length;
        Arrays.fill(used, false);

        // we start with the full board
        System.arraycopy(newFullSudoku, 0, newValidSudoku, 0, newFullSudoku.length);
        int remainingClues = newValidSudoku.length;

        // do until we have only 17 clues left or until all cells have been tried
        while (remainingClues > maxPosToFill && usedCount > 1) {
            // get the next position to try
            int i = rand.nextInt(81);
            do {
                if( i < 80 ) {
                    i++;
                } else {
                    i = 0;
                }
            } while (used[i]);
            used[i] = true;
            usedCount--;

            if (newValidSudoku[i] == 0) {
                // already deleted (symmetry)
                continue;
            }
            if (isSymmetric && (i/9 != 4 || i%9 != 4 ) && newValidSudoku[9 * (8 - i / 9) + (8 - i % 9)] == 0) {
                // the other end of our symmetric puzzle is already deleted
                continue;
            }
            // delete cell
            newValidSudoku[i] = 0;
            remainingClues--;
            int symm = 0;
            if (isSymmetric && (i/9 != 4 || i%9 != 4 )) {
                symm = 9 * (8 - i / 9) + (8 - i % 9);
                newValidSudoku[symm] = 0;
                used[symm] = true;
                usedCount--;
                remainingClues--;
            }
//            long actNanos = System.nanoTime();
            solve(newValidSudoku);
//            nanos += System.nanoTime() - actNanos;
            anzTriesGen++;
            if (solutionCount > 1) {
                newValidSudoku[i] = newFullSudoku[i];
                remainingClues++;
                if (isSymmetric && (i/9 != 4 || i%9 != 4 )) {
                    newValidSudoku[symm] = newFullSudoku[symm];
                    remainingClues++;
                }
            }
        }
    }

    /**
     * Sets all Singles that have been exposed by a previous operation. All Singles
     * exposed by the method itself are set too.
     * @param sudoku
     * @return <code>false</code>, if the puzzle has become invalid.
     */
    private boolean setAllExposedSingles(Sudoku2 sudoku) {
        boolean valid = true;
        SudokuSinglesQueue nsQueue = sudoku.getNsQueue();
        SudokuSinglesQueue hsQueue = sudoku.getHsQueue();
        do {
            int singleIndex = 0;
            // first all Naked Singles
            while (valid && (singleIndex = nsQueue.getSingle()) != -1) {
                int index = nsQueue.getIndex(singleIndex);
                int value = nsQueue.getValue(singleIndex);
                if ((sudoku.getCell(index) & Sudoku2.MASKS[value]) != 0) {
                    // only set the cell if the Single is still valid
                    anzNS++;
                    valid = sudoku.setCell(index, value, false, false);
                    if (DEBUG && ! valid) {
                        System.out.println("   NS " + index + "/" + value + "/" + valid);
                    }
                }
            }
            // then all Hidden Singles
            while (valid && (singleIndex = hsQueue.getSingle()) != -1) {
                int index = hsQueue.getIndex(singleIndex);
                int value = hsQueue.getValue(singleIndex);
                if ((sudoku.getCell(index) & Sudoku2.MASKS[value]) != 0) {
                    // only set the cell if the Single is still valid
                    anzHS++;
                    valid = sudoku.setCell(index, value, false, false);
                    if (DEBUG && ! valid) {
                        System.out.println("   HS " + index + "/" + value + "/" + valid);
                    }
                }
            }
        } while (valid && !(nsQueue.isEmpty() && hsQueue.isEmpty()));
        return valid;
    }

    public int getSolutionCount() {
        return solutionCount;
    }

    public int[] getSolution() {
        return solution;
    }

    public String getSolutionAsString() {
        return getSolutionAsString(solution);
    }

    public String getSolutionAsString(int[] array) {
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            temp.append("").append(array[i]);
        }
        //temp.deleteCharAt(0);
        return temp.toString();
    }

    private String getGridStr(Sudoku2 sudoku) {
        return getSolutionAsString(sudoku.getValues());
    }

    public String printStat() {
        return "anzTries: " + anzTries + ", anzNS: " + anzNS + ", anzHS: " + anzHS;
    }

    public static void main(String[] args) {
        System.out.println("Sudoku2!");
//        ..15............32...............2.9.5...3......7..8..27.....4.3...9.......6..5..
//        .1.....2....8..6.......3........43....2.1....8......9.4...7.5.3...2...........4..
//        ...87..3.52.......4..........3.9..7......54...8.......2.....5.....3....9...1.....
//        .51..........2.4...........64....2.....5.1..7...3..6..4...3.......8...5.2........
//        17.....4....62....5...3....84....1.....3....6......9....6.....3.....1..........5.
//        ...7...1...6.......4.......7..5.1.....8...4..2...........24.6...3..8....1.......9
//        3.....7.....1..4.....2.........5.61..82...........6....1.....287...3...........3.
//        64.7............53.......1.7.86........4.9...5.........6....4......5.2......1....
        SudokuGenerator bs = SudokuGeneratorFactory.getDefaultGeneratorInstance();
//        bs.solve("..15............32...............2.9.5...3......7..8..27.....4.3...9.......6..5..");
//        System.out.println(bs.getSolutionAsString() + " (" + bs.getSolutionCount() + ")");
//        bs.solve(".1.....2....8..6.......3........43....2.1....8......9.4...7.5.3...2...........4..");
//        System.out.println(bs.getSolutionAsString() + " (" + bs.getSolutionCount() + ")");
//        bs.solve("...87..3.52.......4..........3.9..7......54...8.......2.....5.....3....9...1.....");
//        System.out.println(bs.getSolutionAsString() + " (" + bs.getSolutionCount() + ")");
//        bs.solve(".51..........2.4...........64....2.....5.1..7...3..6..4...3.......8...5.2........");
//        System.out.println(bs.getSolutionAsString() + " (" + bs.getSolutionCount() + ")");
//        bs.solve("17.....4....62....5...3....84....1.....3....6......9....6.....3.....1..........5.");
//        System.out.println(bs.getSolutionAsString() + " (" + bs.getSolutionCount() + ")");
//        bs.solve("...7...1...6.......4.......7..5.1.....8...4..2...........24.6...3..8....1.......9");
//        System.out.println(bs.getSolutionAsString() + " (" + bs.getSolutionCount() + ")");
//        bs.solve("3.....7.....1..4.....2.........5.61..82...........6....1.....287...3...........3.");
//        System.out.println(bs.getSolutionAsString() + " (" + bs.getSolutionCount() + ")");
//        bs.solve("64.7............53.......1.7.86........4.9...5.........6....4......5.2......1....");
//        System.out.println(bs.getSolutionAsString() + " (" + bs.getSolutionCount() + ")");
        //bs.solve("1..2..3...2.1.........3...1....1.2.3..1...4....2.4...........1.2.........1.......");
        //System.out.println(bs.getSolutionAsString() + " (" + bs.getSolutionCount() + ")");

        int anzRuns = 10000;
        long ticks = System.currentTimeMillis();
        for (int i = 0; i < anzRuns; i++) {
            //bs.solve("..15............32...............2.9.5...3......7..8..27.....4.3...9.......6..5..");
            //bs.solve(".1.756.........2..4...9.57...95...21.........76...14...91.7...3..7.........635.9.");
            //  2 Easter Monster
            //bs.solve("1.......2.9.4...5...6...7...5.9.3.......7.......85..4.7.....6...3...9.8...2.....1");
            //  2
            //bs.solve("..............3.85..1.2.......5.7.....4...1...9.......5......73..2.1........4...9");
            // 95
            //bs.solve(".1.....2....8..6.......3........43....2.1....8......9.4...7.5.3...2...........4..");
            //  1
            //bs.solve("...87..3.52.......4..........3.9..7......54...8.......2.....5.....3....9...1.....");
            // 73
            //bs.solve("..15............32...............2.9.5...3......7..8..27.....4.3...9.......6..5..");
            //  0
            //bs.solve("7.....4...2..7..8...3..8..9...5..3...6..2..9...1..7..6...3..9...3..4..6...9..1..5");
            //  1 bs.solve("...7..8......4..3......9..16..5......1..3..4...5..1..75..2..6...3..8..9...7.....2");
            //bs.solve("536020900008000000000000000600285009000903000800761004000000000004000000201000007");
            //bs.solve("000000490020100000000000500800400300609000000000200000000069070040050000000000001");
            //bs.solve(".1.....2....8..6.......3........43....2.1....8......9.4...7.5.3...2...........4..");
            //bs.solve("...87..3.52.......4..........3.9..7......54...8.......2.....5.....3....9...1.....");
            //bs.solve("..1..4.......6.3.5...9.....8.....7.3.......285...7.6..3...8...6..92......4...1...");
            //bs.solve("7..4......5....3........1..368..........2..5....7........5...7213...8...6........");
        }
        ticks = System.currentTimeMillis() - ticks;
        System.out.println(bs.getSolutionAsString() + " (" + bs.getSolutionCount() + ")");
        System.out.println("Time: " + ((double) ticks / anzRuns) + "ms");
        System.out.println(bs.printStat());

        ticks = System.currentTimeMillis();
        for (int i = 0; i < anzRuns; i++) {
            Sudoku2 sudoku = bs.generateSudoku(true);
//            bs.generateFullGrid();
//            System.out.println("New Grid1: " + bs.getSolutionAsString(bs.newFullSudoku));
//            System.out.println("New Grid2: " + bs.getSolutionAsString(bs.newValidSudoku));
//            System.out.println("New Grid3: " + sudoku.getSudoku(ClipboardMode.CLUES_ONLY));
//            System.out.println("New Grid4: " + sudoku.getSudoku(ClipboardMode.VALUES_ONLY));
        }
        ticks = System.currentTimeMillis() - ticks;
        //System.out.println("New Grid: " + bs.getSolutionAsString(bs.newFullSudoku));
        long faktor = 1000000;
        long nanos = bs.nanos / faktor;
        long setNanos = bs.setNanos / faktor;
        System.out.println("Time: " + ((double) ticks / anzRuns) + "ms " + (bs.anzTriesGen / anzRuns) + "/" + (bs.anzClues / anzRuns) + "/" + 
                nanos + "/" + setNanos + "/" + (nanos - setNanos));
        System.out.println(bs.printStat());
    }
}
