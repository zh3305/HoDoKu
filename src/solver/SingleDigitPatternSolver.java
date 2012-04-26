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

/**********
 Dual Skyscraper?
 |-----------+-----------+-----------|
 |  .  .  .  |  .  .  .  |  3  .  3  |
 |  .  .  3  |  .  .  .  |  .  .  .  |
 |  .  .  .  |  .  3  .  |  .  .  .  |
 |-----------+-----------+-----------|
 |  .  .  .  |  .  .  .  |  .  3  .  |
 |  .  3@ .  |  3@ .  .  |  .  .  .  |
 |  3  .  .  |  .  .  3# |  .  .  .  |
 |-----------+-----------+-----------|
 |  3  .  .  |  3# .  .  |  .  .  3  |
 |  3  .  .  |  .  .  .  |  3  .  3  |
 |  3  3@ .  |  .  .  3# |  3  .  .  |
 +-----------------------------------+ 
 */

package solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sudoku.Candidate;
import sudoku.Options;
import sudoku.SolutionStep;
import sudoku.SolutionType;
import sudoku.Sudoku2;
import sudoku.SudokuSet;

/**
 * Empty Rectangles:
 * 
 * Every box can hold nine different empty rectangles ('X' means 'candidate not 
 * present', digits below are lines/cols):
 * 
 * + - - - +   + - - - +   + - - - +
 * | X X . |   | X . X |   | . X X |
 * | X X . |   | X . X |   | . X X |
 * | . . . |   | . . . |   | . . . |
 * + - - - +   + - - - +   + - - - +
 *    2 2         2 1         2 0
 * + - - - +   + - - - +   + - - - +
 * | X X . |   | X . X |   | . X X |
 * | . . . |   | . . . |   | . . . |
 * | X X . |   | X . X |   | . X X |
 * + - - - +   + - - - +   + - - - +
 *    1 2         1 1         1 0
 * + - - - +   + - - - +   + - - - +
 * | . . . |   | . . . |   | . . . |
 * | X X . |   | X . X |   | . X X |
 * | X X . |   | X . X |   | . X X |
 * + - - - +   + - - - +   + - - - +
 *    0 2         0 1         0 0
 * 
 * The '.' cells must contain at least three candidates, at least one exclusively
 * within the row/col (with two candidates the basic ER move degenerates into an 
 * X-Chain, with all three candidates only in a row/col it doesn't work at all).
 * 
 * For easy comparison SudokuSets with all possible combinations of empty cells
 * for all blocks are created at startup.
 *
 * @author hobiwan
 */
public class SingleDigitPatternSolver extends AbstractSolver {

    /** empty rectangles: all possible empty cells relative to cell 0 */
    private static final int[][] erOffsets = new int[][]{
        {0, 1, 9, 10},
        {0, 2, 9, 11},
        {1, 2, 10, 11},
        {0, 1, 18, 19},
        {0, 2, 18, 20},
        {1, 2, 19, 20},
        {9, 10, 18, 19},
        {9, 11, 18, 20},
        {10, 11, 19, 20}
    };
    /** empty rectangles: all possible ER lines relative to line 0, synchronized with {@link #erOffsets} */
    private static final int[] erLineOffsets = new int[]{2, 2, 2, 1, 1, 1, 0, 0, 0};
    /** empty rectangles: all possible ER cols relative to col 0, synchronized with {@link #erOffsets} */
    private static final int[] erColOffsets = new int[]{2, 1, 0, 2, 1, 0, 2, 1, 0};
    /** Bitmaps for all possible ERs for all blocks (all cells set except those that
     * have to be empty; if anded with the availble candidates in a block the result has to
     * be empty too) */
    private static final SudokuSet[][] erSets = new SudokuSet[9][9];
    /** All possible ER lines for all blocks, synchronized with {@link #erSets} */
    private static final int[][] erLines = new int[9][9];
    /** All possible ER cols for all blocks, synchronized with {@link #erSets} */
    private static final int[][] erCols = new int[9][9];
    /** All candidates in a block (for ER search) */
    private SudokuSet blockCands = new SudokuSet();
    /** A set for various checks */
    private SudokuSet tmpSet = new SudokuSet();
    /** A list with all steps found */
    private List<SolutionStep> steps = new ArrayList<SolutionStep>();
    /** One global instance for optimization */
    private SolutionStep globalStep = new SolutionStep();
    /** For all entries in {@link #only2Constraints} the indices of the two cells */
    private int[][] only2Indices = new int[2 * Sudoku2.UNITS][2];
    /** A set to check for eliminations */
    private SudokuSet firstUnit = new SudokuSet();

    /** Creates a new instance of SimpleSolver
     * @param finder 
     */
    protected SingleDigitPatternSolver(SudokuStepFinder finder) {
        super(finder);
    }
    

    static {
        // initialize erSets, erLines, erCols
        int indexOffset = 0;
        int lineOffset = 0;
        int colOffset = 0;
        for (int i = 0; i < Sudoku2.BLOCKS.length; i++) {
            for (int j = 0; j < erOffsets.length; j++) {
                erSets[i][j] = new SudokuSet();
                for (int k = 0; k < erOffsets[j].length; k++) {
                    erSets[i][j].add(erOffsets[j][k] + indexOffset);
                }
            }
            erLines[i] = new int[9];
            erCols[i] = new int[9];
            for (int j = 0; j < erLineOffsets.length; j++) {
                erLines[i][j] = erLineOffsets[j] + lineOffset;
                erCols[i][j] = erColOffsets[j] + colOffset;
            }
            // on to the next block
            indexOffset += 3;
            colOffset += 3;
            if ((i % 3) == 2) {
                indexOffset += 18;
                lineOffset += 3;
                colOffset = 0;
            }
            
        }
    }

    @Override
    protected SolutionStep getStep(SolutionType type) {
        SolutionStep result = null;
        sudoku = finder.getSudoku();
        switch (type) {
            case SKYSCRAPER:
                result = findSkyscraper();
                break;
            case TWO_STRING_KITE:
                result = findTwoStringKite();
                break;
            case EMPTY_RECTANGLE:
                result = findEmptyRectangle();
                break;
        }
        return result;
    }

    @Override
    protected boolean doStep(SolutionStep step) {
        boolean handled = true;
        sudoku = finder.getSudoku();
        switch (step.getType()) {
            case SKYSCRAPER:
            case TWO_STRING_KITE:
            case DUAL_TWO_STRING_KITE:
            case EMPTY_RECTANGLE:
            case DUAL_EMPTY_RECTANGLE:
                for (Candidate cand : step.getCandidatesToDelete()) {
                    sudoku.delCandidate(cand.getIndex(), cand.getValue());
                }
                break;
            default:
                handled = false;
        }
        return handled;
    }

    /**
     * Finds all Empty Rectangles
     * @return
     */
    protected List<SolutionStep> findAllEmptyRectangles() {
        sudoku = finder.getSudoku();
        List<SolutionStep> oldList = steps;
        List<SolutionStep> newList = new ArrayList<SolutionStep>();
        steps = newList;
        findEmptyRectangles(false);
        findDualEmptyRectangles(steps);
        Collections.sort(steps);
        steps = oldList;
        return newList;
    }

    /**
     * Find a single ER. If {@link Options#allowDualsAndSiamese} is set, Dual ERs
     * are found as well.
     * @return
     */
    protected SolutionStep findEmptyRectangle() {
        steps.clear();
        SolutionStep step = findEmptyRectangles(true);
        if (step != null && ! Options.getInstance().isAllowDualsAndSiamese()) {
            return step;
        }
        if (steps.size() > 0 && Options.getInstance().isAllowDualsAndSiamese()) {
            findDualEmptyRectangles(steps);
            Collections.sort(steps);
            return steps.get(0);
        }
        return null;
    }

    /**
     * Finds all empty rectangles that provide eliminations (only simple case
     * with one conjugate pair). The search is actually delegated to
     * {@link #findEmptyRectanglesForCandidate(int)}.
     * @param onlyOne
     * @return
     */
    private SolutionStep findEmptyRectangles(boolean onlyOne) {
        for (int i = 1; i <= 9; i++) {
            SolutionStep step = findEmptyRectanglesForCandidate(i, onlyOne);
            if (step != null && onlyOne && ! Options.getInstance().isAllowDualsAndSiamese()) {
                return step;
            }
        }
        return null;
    }

    /**
     * Try all blocks: for every block check whether all the cells in erSets[block][i]
     * don't have the candidate in question. If this is true neither the ER line nor
     * the ER col may be empty (without crossing point!) and at least one of them
     * has to hold at least two candidates.
     * 
     * For any ER try to find a conjugate pair with one candidate in the row/col
     * of the ER, and one single candidate in ther intersection of the second
     * ca didate of the conjugate pair and the col/row of the ER.
     * 
     * @param cand candidate for which the grid is searched
     * @param onlyOne
     * @return
     */
    private SolutionStep findEmptyRectanglesForCandidate(int cand, boolean onlyOne) {
        // scan all blocks
        byte[][] free = sudoku.getFree();
        for (int i = 0; i < Sudoku2.BLOCK_TEMPLATES.length; i++) {
            // if the block holds less than two or more than five candidates,
            // it cant be a ER
            if (free[18 + i][cand] < 2 || free[18 + i][cand] > 5) {
                // impossible
                continue;
            }
            // get all occurrencies for cand in block i
            blockCands.set(finder.getCandidates()[cand]);
            blockCands.and(Sudoku2.BLOCK_TEMPLATES[i]);
            // check all possible ERs for that block
            for (int j = 0; j < erSets[i].length; j++) {
                int erLine = 0;
                int erCol = 0;
                boolean notEnoughCandidates = true;
                // are the correct cells empty?
                tmpSet.setAnd(blockCands, erSets[i][j]);
                if (!tmpSet.isEmpty()) {
                    // definitely not this type of ER
                    continue;
                }
                // now check the candidates in the lines
                tmpSet.setAnd(blockCands, Sudoku2.LINE_TEMPLATES[erLines[i][j]]);
                if (tmpSet.size() >= 2) {
                    notEnoughCandidates = false;
                }
                tmpSet.andNot(Sudoku2.COL_TEMPLATES[erCols[i][j]]);
                if (tmpSet.isEmpty()) {
                    // not valid!
                    continue;
                }
                erLine = erLines[i][j];
                // and the candidates in the cols
                tmpSet.setAnd(blockCands, Sudoku2.COL_TEMPLATES[erCols[i][j]]);
                if (tmpSet.size() >= 2) {
                    notEnoughCandidates = false;
                }
                tmpSet.andNot(Sudoku2.LINE_TEMPLATES[erLines[i][j]]);
                if (tmpSet.isEmpty()) {
                    // not valid!
                    continue;
                }
                erCol = erCols[i][j];
                if (notEnoughCandidates && Options.getInstance().isAllowErsWithOnlyTwoCandidates() == false) {
                    // both row and col have only one candidate -> invalid
                    continue;
                }
                // empty rectangle found: erLine and erCol hold the lineNumbers
                // try all cells in indices erLine; if a cell that is not part of the ER holds
                // a candidate, check whether it forms a conjugate pair in the respective col
                SolutionStep step = checkEmptyRectangle(cand, i, blockCands, Sudoku2.LINES[erLine], Sudoku2.LINE_TEMPLATES,
                        Sudoku2.COL_TEMPLATES, erCol, false, onlyOne);
                if (onlyOne && step != null && ! Options.getInstance().isAllowDualsAndSiamese()) {
                    return step;
                }
                step = checkEmptyRectangle(cand, i, blockCands, Sudoku2.COLS[erCol], Sudoku2.COL_TEMPLATES,
                        Sudoku2.LINE_TEMPLATES, erLine, true, onlyOne);
                if (onlyOne && step != null && ! Options.getInstance().isAllowDualsAndSiamese()) {
                    return step;
                }
            }
        }
        return null;
    }

    /**
     * Checks possible eliminations for a given ER. The names of the parameters
     * are chosen for a conjugate pair search in the columns, but it works for
     * the lines too, if all indices/col parameters are exchanged in the
     * method call.
     * 
     * The method tries to find a conjugate pair in a column where one of the
     * candidates is in indices firstLine. If so all candidates in the indices of the
     * second cell of the conjugate pair are checked. If one of them lies in
     * column firstCol, it can be eliminated.
     * 
     * @param cand The candidate for which the check is made
     * @param block The index of the block holding the ER
     * @param blockCands All Candidates that comprise the ER
     * @param indices Indices of all cells in firstLine/firstCol
     * @param LINE_TEMPLATES Sudoku2.LINE_TEMPLATES/Sudoku2.COL_TEMPLATES
     * @param COL_TEMPLATES Sudoku2.COL_TEMPLATES/Sudoku2.LINE_TEMPLATES
     * @param firstCol Index of the col/indices of the ER
     * @param lineColReversed If <code>true</code>, all lines/columns are interchanged
     * @param onlyOne
     * @return
     */
    private SolutionStep checkEmptyRectangle(int cand, int block, SudokuSet blockCands,
            int[] indices, SudokuSet[] lineTemplates,
            SudokuSet[] colTemplates, int firstCol, boolean lineColReversed,
            boolean onlyOne) {
        for (int i = 0; i < indices.length; i++) {
            int index = indices[i];
            if (sudoku.getValue(index) != 0) {
                // cell already set
                continue;
            }
            if (Sudoku2.getBlock(index) == block) {
                // cell part of the ER
                continue;
            }
            if (sudoku.isCandidate(index, cand)) {
                // possible conjugate pair -> check
                tmpSet.set(finder.getCandidates()[cand]);
                int actCol = Sudoku2.getCol(index);
                if (lineColReversed) {
                    actCol = Sudoku2.getLine(index);
                }
                tmpSet.and(colTemplates[actCol]);
                if (tmpSet.size() == 2) {
                    // conjugate pair found
                    int index2 = tmpSet.get(0);
                    if (index2 == index) {
                        index2 = tmpSet.get(1);
                    }
                    // now check, whether a candidate in the row of index2
                    // sees the col of the ER
                    int actLine = Sudoku2.getLine(index2);
                    if (lineColReversed) {
                        actLine = Sudoku2.getCol(index2);
                    }
                    tmpSet.set(finder.getCandidates()[cand]);
                    tmpSet.and(lineTemplates[actLine]);
                    for (int j = 0; j < tmpSet.size(); j++) {
                        int indexDel = tmpSet.get(j);
                        if (Sudoku2.getBlock(indexDel) == block) {
                            // cannot eliminate an ER candidate
                            continue;
                        }
                        int colDel = Sudoku2.getCol(indexDel);
                        if (lineColReversed) {
                            colDel = Sudoku2.getLine(indexDel);
                        }
                        if (colDel == firstCol) {
                            // elimination found!
                            globalStep.reset();
                            globalStep.setType(SolutionType.EMPTY_RECTANGLE);
                            globalStep.setEntity(Sudoku2.BLOCK);
                            globalStep.setEntityNumber(block + 1);
                            globalStep.addValue(cand);
                            globalStep.addIndex(index);
                            globalStep.addIndex(index2);
                            for (int k = 0; k < blockCands.size(); k++) {
                                globalStep.addFin(blockCands.get(k), cand);
                            }
                            globalStep.addCandidateToDelete(indexDel, cand);
                            SolutionStep step = (SolutionStep) globalStep.clone();
                            // only one elimination per conjugate pair possible
                            if (onlyOne && ! Options.getInstance().isAllowDualsAndSiamese()) {
                                return step;
                            } else {
                                steps.add(step);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * A dual Empty Rectangle consists of two ERs, that have the same candidates
     * in the ER box but lead to different eliminations.
     * 
     * Try all combinations of steps:
     *   - entity and entityNumber have to be the same
     *   - box candidiates have to be the same (fins!)
     *   - elimination has to be different
     * Create new step with indices/eliminations from both, fins from first, add to ers
     * 
     * @param kites All available 2-String-Kites
     */
    private void findDualEmptyRectangles(List<SolutionStep> ers) {
        if (! Options.getInstance().isAllowDualsAndSiamese()) {
            // do nothing
            return;
        }
        // read current size (list can be changed by DUALS)
        int maxIndex = ers.size();
        for (int i = 0; i < maxIndex - 1; i++) {
            for (int j = i + 1; j < maxIndex; j++) {
                SolutionStep step1 = ers.get(i);
                SolutionStep step2 = ers.get(j);
                if (step1.getEntity() != step2.getEntity() ||
                        step1.getEntityNumber() != step2.getEntityNumber()) {
                    // different boxes -> cant be a dual
                    continue;
                }
                if (step1.getFins().size() != step2.getFins().size()) {
                    // different number of candidates in box -> cant be a dual
                    continue;
                }
                boolean finsEqual = true;
                for (int k = 0; k < step1.getFins().size(); k++) {
                    if (! step1.getFins().get(k).equals(step2.getFins().get(k))) {
//                        System.out.println("  " + step1.getFins().get(k) + " - " + step2.getFins().get(k));
                        finsEqual = false;
                        break;
                    }
                }
                if (! finsEqual) {
                    // not the same ER -> cant be a dual
                    continue;
                }
                // possible dual ER; different eliminations?
                if (step1.getCandidatesToDelete().get(0).equals(step2.getCandidatesToDelete().get(0))) {
                    // same step twice -> no dual
                    continue;
                }
                // ok: dual!
                SolutionStep dual = (SolutionStep) step1.clone();
                dual.setType(SolutionType.DUAL_EMPTY_RECTANGLE);
                dual.addIndex(step2.getIndices().get(0));
                dual.addIndex(step2.getIndices().get(1));
                dual.addCandidateToDelete(step2.getCandidatesToDelete().get(0));
                ers.add(dual);
            }
        }
    }

    /**
     * Search for all Skyscrapers
     * @return
     */
    protected List<SolutionStep> findAllSkyscrapers() {
        sudoku = finder.getSudoku();
        List<SolutionStep> oldList = steps;
        List<SolutionStep> newList = new ArrayList<SolutionStep>();
        steps = newList;
        findSkyscraper(true, false);
        findSkyscraper(false, false);
        Collections.sort(steps);
        steps = oldList;
        return newList;
    }

    /**
     * Search the grid for Skyscrapers
     * @return
     */
    protected SolutionStep findSkyscraper() {
        steps.clear();
        SolutionStep step = findSkyscraper(true, true);
        if (step != null) {
            return step;
        }
        return findSkyscraper(false, true);
    }

    /**
     * Search for Skyscrapers in the lines or in the columns. Two calls are
     * necessary to get all possible steps.<br>
     * The search:
     * <ul>
     * <li>Iterate over all candidates</li>
     * <li>For each candidate look at all lines (cols) and check which have only two candidates left</li>
     * <li></li>
     * <li></li>
     * </ul>
     * @param lines
     * @param onlyOne
     * @return
     */
    private SolutionStep findSkyscraper(boolean lines, boolean onlyOne) {
        // indices in free
        int cStart = 0;
        int cEnd = 9;
        if (! lines) {
            // adjust for columns
            cStart += 9;
            cEnd += 9;
        }
        byte[][] free = sudoku.getFree();
        // try every candidate
        for (int cand = 1; cand <= 9; cand++) {
            // get all constraints with only two candidates and the indices of the cells
            int constrCount = 0;
            for (int constr = cStart; constr < cEnd; constr++) {
                if (free[constr][cand] == 2) {
                    // constraint has only two candidates left -> get the indices of the cells
                    int[] indices = Sudoku2.ALL_UNITS[constr];
                    int candIndex = 0;
                    for (int i = 0; i < indices.length; i++) {
                        if (sudoku.isCandidate(indices[i], cand)) {
                            only2Indices[constrCount][candIndex++] = indices[i];
                            if (candIndex >= 2) {
                                break;
                            }
                        }
                    }
                    constrCount++;
                }
            }
            // ok: now try all combinations of those constraints
            for (int i = 0; i < constrCount; i++) {
                for (int j = i + 1; j < constrCount; j++) {
                    // one end has to be in the same line/col
                    boolean found = false;
                    int otherIndex = 1;
                    if (lines) {
                        // must be in the same col
                        if (Sudoku2.getCol(only2Indices[i][0]) == Sudoku2.getCol(only2Indices[j][0])) {
                            found = true;
                        }
                        if (! found && Sudoku2.getCol(only2Indices[i][1]) == Sudoku2.getCol(only2Indices[j][1])) {
                            found = true;
                            otherIndex = 0;
                        }
                    } else {
                        // must be in the same line
                        if (Sudoku2.getLine(only2Indices[i][0]) == Sudoku2.getLine(only2Indices[j][0])) {
                            found = true;
                        }
                        if (! found && Sudoku2.getLine(only2Indices[i][1]) == Sudoku2.getLine(only2Indices[j][1])) {
                            found = true;
                            otherIndex = 0;
                        }
                    }
                    if (! found) {
                        // invalid combination
                        continue;
                    }
                    // the "free ends" must not be in the same unit or it would be an X-Wing
                    if (lines && Sudoku2.getCol(only2Indices[i][otherIndex]) == Sudoku2.getCol(only2Indices[j][otherIndex]) ||
                            ! lines && Sudoku2.getLine(only2Indices[i][otherIndex]) == Sudoku2.getLine(only2Indices[j][otherIndex])) {
                        // step is X-Wing -> ignore
                        continue;
                    }
                    // can something be eliminated?
                    firstUnit.setAnd(finder.getCandidates()[cand], Sudoku2.buddies[only2Indices[i][otherIndex]]);
                    firstUnit.and(Sudoku2.buddies[only2Indices[j][otherIndex]]);
                    if (! firstUnit.isEmpty()) {
                        // Skyscraper found!
                        SolutionStep step = new SolutionStep(SolutionType.SKYSCRAPER);
                        step.addValue(cand);
                        if (otherIndex == 0) {
                            step.addIndex(only2Indices[i][0]);
                            step.addIndex(only2Indices[j][0]);
                            step.addIndex(only2Indices[i][1]);
                            step.addIndex(only2Indices[j][1]);
                        } else {
                            step.addIndex(only2Indices[i][1]);
                            step.addIndex(only2Indices[j][1]);
                            step.addIndex(only2Indices[i][0]);
                            step.addIndex(only2Indices[j][0]);
                        }
                        for (int k = 0; k < firstUnit.size(); k++) {
                            step.addCandidateToDelete(firstUnit.get(k), cand);
                        }
//                        if (onlyOne && ! Options.getInstance().isAllowDualsAndSiamese()) {
                        if (onlyOne) {
                            return step;
                        } else {
                            steps.add(step);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * FIns all 2-String-Kites in the grid
     * @return
     */
    protected List<SolutionStep> findAllTwoStringKites() {
        sudoku = finder.getSudoku();
        List<SolutionStep> oldList = steps;
        List<SolutionStep> newList = new ArrayList<SolutionStep>();
        steps = newList;
        findTwoStringKite(false);
        if (Options.getInstance().isAllowDualsAndSiamese()) {
            findDualTwoStringKites(steps);
        }
        Collections.sort(steps);
        steps = oldList;
        return newList;
    }

    /**
     * Find the next 2-String-Kite
     * @return
     */
    protected SolutionStep findTwoStringKite() {
        steps.clear();
        SolutionStep step = findTwoStringKite(true);
        if (step != null && ! Options.getInstance().isAllowDualsAndSiamese()) {
            return step;
        }
        findDualTwoStringKites(steps);
        if (steps.size() > 0) {
            Collections.sort(steps);
            return steps.get(0);
        } else {
            return null;
        }
    }

    /**
     * Search for 2-String-Kites: We need a strong link in a line and one in a col.
     * The two strong links must be connected by a box and the "free ends" must see
     * a candidate.
     * @param onlyOne
     * @return
     */
    private SolutionStep findTwoStringKite(boolean onlyOne) {
        // search for lines and columns with exactly two candidates
        byte[][] free = sudoku.getFree();
        // try every candidate
        for (int cand = 1; cand <= 9; cand++) {
            // get all constraints with only two candidates and the indices of the cells
            // all lines are in only2Indices[0 .. constr1Count - 1], all cols
            // are in only2Indices[constr1Count .. constr2Count - 1]
            int constr1Count = 0;
            int constr2Count = 0;
            for (int constr = 0; constr < 18; constr++) {
                if (free[constr][cand] == 2) {
                    // constraint has only two candidates left -> get the indices of the cells
                    int[] indices = Sudoku2.ALL_UNITS[constr];
                    int candIndex = 0;
                    for (int i = 0; i < indices.length; i++) {
                        if (sudoku.isCandidate(indices[i], cand)) {
                            only2Indices[constr1Count + constr2Count][candIndex++] = indices[i];
                            if (candIndex >= 2) {
                                break;
                            }
                        }
                    }
                    if (constr < 9) {
                        constr1Count++;
                    } else {
                        constr2Count++;
                    }
                }
            }
            // ok: now try all combinations of those constraints
            for (int i = 0; i < constr1Count; i++) {
                for (int j = constr1Count; j < constr1Count + constr2Count; j++) {
                    // one end has to be in the same line/col, but: all 4 combinations are possible
                    // the indices in the same block end up in only2Indices[][0], the "free ends"
                    // in only2indices[][1]
                    if (Sudoku2.getBlock(only2Indices[i][0]) == Sudoku2.getBlock(only2Indices[j][0])) {
                        // everything is as it should be -> do nothing
                    } else if (Sudoku2.getBlock(only2Indices[i][0]) == Sudoku2.getBlock(only2Indices[j][1])) {
                        int tmp = only2Indices[j][0];
                        only2Indices[j][0] = only2Indices[j][1];
                        only2Indices[j][1] = tmp;
                    } else if (Sudoku2.getBlock(only2Indices[i][1]) == Sudoku2.getBlock(only2Indices[j][0])) {
                        int tmp = only2Indices[i][0];
                        only2Indices[i][0] = only2Indices[i][1];
                        only2Indices[i][1] = tmp;
                    } else if (Sudoku2.getBlock(only2Indices[i][1]) == Sudoku2.getBlock(only2Indices[j][1])) {
                        int tmp = only2Indices[j][0];
                        only2Indices[j][0] = only2Indices[j][1];
                        only2Indices[j][1] = tmp;
                        tmp = only2Indices[i][0];
                        only2Indices[i][0] = only2Indices[i][1];
                        only2Indices[i][1] = tmp;
                    } else {
                        // nothing found -> continue with next column
                        continue;
                    }
                    // the indices within the connecting box could be the same -> not a 2-String-Kite
                    if (only2Indices[i][0] == only2Indices[j][0] || only2Indices[i][0] == only2Indices[j][1] ||
                            only2Indices[i][1] == only2Indices[j][0] || only2Indices[i][1] == only2Indices[j][1]) {
                        // invalid!
                        continue;
                    }
                    // ok: two strong links, connected in a box; can anything be deleted?
                    int crossIndex = Sudoku2.getIndex(Sudoku2.getLine(only2Indices[j][1]), Sudoku2.getCol(only2Indices[i][1]));
                    if (sudoku.isCandidate(crossIndex, cand)) {
                        // valid 2-String-Kite!
                        SolutionStep step = new SolutionStep(SolutionType.TWO_STRING_KITE);
                        step.addValue(cand);
                        step.addIndex(only2Indices[i][1]);
                        step.addIndex(only2Indices[j][1]);
                        step.addIndex(only2Indices[i][0]);
                        step.addIndex(only2Indices[j][0]);
                        step.addCandidateToDelete(crossIndex, cand);
                        // the candidates in the connecting block are added as fins (will be painted
                        // in a different color)
                        step.addFin(only2Indices[i][0], cand);
                        step.addFin(only2Indices[j][0], cand);
                        if (onlyOne && ! Options.getInstance().isAllowDualsAndSiamese()) {
                            return step;
                        } else {
                            steps.add(step);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * A dual 2-String-Kite consists of two kites, that have the same candidates
     * in the connecting box but lead to different eliminations.
     * 
     * Try all combinations of steps:
     *   - box candidates have to be the same (fins!)
     *   - elimination has to be different
     * Create new step with indices/eliminations from both, fins from first, add to kites
     * 
     * @param kites All available 2-String-Kites
     */
    private void findDualTwoStringKites(List<SolutionStep> kites) {
        if (! Options.getInstance().isAllowDualsAndSiamese()) {
            // do nothing
            return;
        }
        // read current size (list can be changed by DUALS)
        int maxIndex = kites.size();
        for (int i = 0; i < maxIndex - 1; i++) {
            for (int j = i + 1; j < maxIndex; j++) {
                SolutionStep step1 = kites.get(i);
                SolutionStep step2 = kites.get(j);
                int b11 = step1.getIndices().get(2);
                int b12 = step1.getIndices().get(3);
                int b21 = step2.getIndices().get(2);
                int b22 = step2.getIndices().get(3);
                if ((b11 == b21 && b12 == b22) || (b12 == b21 && b11 == b22)) {
                    // possible dual kite; different eliminations?
                    if (step1.getCandidatesToDelete().get(0).equals(step2.getCandidatesToDelete().get(0))) {
                        // same step twice -> no dual
                        continue;
                    }
                    // ok: dual!
                    SolutionStep dual = (SolutionStep) step1.clone();
                    dual.setType(SolutionType.DUAL_TWO_STRING_KITE);
                    dual.addIndex(step2.getIndices().get(0));
                    dual.addIndex(step2.getIndices().get(1));
                    dual.addIndex(step2.getIndices().get(2));
                    dual.addIndex(step2.getIndices().get(3));
                    dual.addCandidateToDelete(step2.getCandidatesToDelete().get(0));
                    kites.add(dual);
                }
            }
        }
    }
    
    public static void main(String[] args) {
        Sudoku2 sudoku = new Sudoku2();
        // 2-String Kite: 3 in r2c1,r8c5 (verbunden durch r2c6,r3c5) => r8c1<>3
        sudoku.setSudoku(":0401:3:+156+87+49+3+2.4+762.+18+528....+4+7+6....8.+5+9.73....618+8.5...+32.........+3.7.5...49....487.1::381::");
        // 2-String Kite: 3 in r2c3,r6c8 (verbunden durch r5c3,r6c1) => r2c8<>3
        sudoku.setSudoku(":0401:3:9.567.1..61.5+4...+9.849+3+15+6....8.39.....+2.+9....+987.4...+5+61.+9782.+8+7+9.+26.51..2+1857+96:249 261 165 367 369:328::");

        SudokuSolver solver = SudokuSolverFactory.getDefaultSolverInstance();
        SolutionStep step = solver.getHint(sudoku, false);
        System.out.println(step);
        System.exit(0);
    }
}
