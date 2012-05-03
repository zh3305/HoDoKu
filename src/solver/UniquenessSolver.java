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

package solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import sudoku.Candidate;
import sudoku.ClipboardMode;
import sudoku.Options;
import sudoku.SolutionStep;
import sudoku.SolutionType;
import sudoku.Sudoku2;
import sudoku.SudokuSet;
import sudoku.SudokuStatus;

/**
 *
 * @author hobiwan
 */
public class UniquenessSolver extends AbstractSolver {

    /** An array vor caching constraint indices while searching for BUG +1 */
    private int[] bugConstraints = new int[3];
    /** One global step for optimization */
    private SolutionStep globalStep = new SolutionStep(SolutionType.FULL_HOUSE);
    /** All steps that were found in this search */
    private List<SolutionStep> steps = new ArrayList<SolutionStep>();
    /** All already checked rectangles: One int aabbccdd with "aa"... the indices of the corners (sorted ascending) */
    private int[] rectangles = new int[400];
    /** Current index in {@link #rectangles}. */
    private int rectAnz = 0;
    /** Contains the indices of the current rectangle */
    private int[] indexe = new int[4];
    /** Temporary array for sorting */
    private int[] tmpRect = new int[4];
    /** The first candidate of the UR */
    private int cand1;
    /** The second candidate of the UR */
    private int cand2;
    /** A list of cached steps */
    private List<SolutionStep> cachedSteps = new ArrayList<SolutionStep>();
    /** The {@link SudokuStepFinder#stepNumber} under which the cached steps were found */
    private int stepNumber = -1;
    /** A set with all cells that hold only {@link #cand1} and/or {@link #cand2}. */
    private SudokuSet twoCandidates = new SudokuSet();
    /** A set with all cells that hold additional candidates */
    private SudokuSet additionalCandidates = new SudokuSet();
    /** A set for various checks */
    private SudokuSet tmpSet = new SudokuSet();
    /** A set for various checks */
    private SudokuSet tmpSet1 = new SudokuSet();
    /** A flag that indicates if the last search was for URs */
    private boolean lastSearchWasUR = false;
    /** A flag that indicates if the last search was for ARs */
    private boolean lastSearchWasAR = false;

    /** Creates a new instance of SimpleSolver
     * @param finder 
     */
    public UniquenessSolver(SudokuStepFinder finder) {
        super(finder);
    }

    @Override
    protected SolutionStep getStep(SolutionType type) {
        SolutionStep result = null;
        sudoku = finder.getSudoku();
        if (sudoku.getStatus() != SudokuStatus.VALID) {
            // Uniqueness only for sudokus, that are strictly valid!
            return null;
        }
        switch (type) {
            case UNIQUENESS_1:
            case UNIQUENESS_2:
            case UNIQUENESS_3:
            case UNIQUENESS_4:
            case UNIQUENESS_5:
            case UNIQUENESS_6:
            case HIDDEN_RECTANGLE:
                result = getUniqueness(type);
                break;
            case AVOIDABLE_RECTANGLE_1:
            case AVOIDABLE_RECTANGLE_2:
                if (sudoku.getStatusGivens() != SudokuStatus.VALID) {
                    //only, if the givens themselves have only one solution!
                    return null;
                }
                result = getAvoidableRectangle(type);
                break;
            case BUG_PLUS_1:
                result = getBugPlus1();
                break;
        }
        return result;
    }

    @Override
    protected boolean doStep(SolutionStep step) {
        boolean handled = true;
        sudoku = finder.getSudoku();
        switch (step.getType()) {
            case UNIQUENESS_1:
            case UNIQUENESS_2:
            case UNIQUENESS_3:
            case UNIQUENESS_4:
            case UNIQUENESS_5:
            case UNIQUENESS_6:
            case HIDDEN_RECTANGLE:
            case AVOIDABLE_RECTANGLE_1:
            case AVOIDABLE_RECTANGLE_2:
            case BUG_PLUS_1:
                if (step.getCandidatesToDelete().isEmpty()) {
                    System.out.println("ERROR: No candidate to delete!");
                    System.out.println(step.toString(2));
                    System.out.println(sudoku.getSudoku(ClipboardMode.LIBRARY));
                }
                for (Candidate cand : step.getCandidatesToDelete()) {
                    if (! sudoku.isCandidate(cand.getIndex(), cand.getValue())) {
                        System.out.println("ERROR: " + cand.getIndex() + "/" + cand.getValue());
                        System.out.println(step.toString(2));
                        System.out.println(sudoku.getSudoku(ClipboardMode.LIBRARY));
                    }
                    sudoku.delCandidate(cand.getIndex(), cand.getValue());
                }
                break;
            default:
                handled = false;
        }
        return handled;
    }

    /**
     * Try to get an appropriate step from the cache {@link #cachedSteps}. If none is
     * available or the sudoku has changed since the last call find a new one. If no
     * step is found but the sudoku has not changed the already found and stored URs
     * ({@link #rectangles}) are not reset (they have already been searched and if a step
     * was in them it would have been cached).
     * @param type
     * @return
     */
    private SolutionStep getUniqueness(SolutionType type) {
        if (finder.getStepNumber() == stepNumber && lastSearchWasUR) {
            if (cachedSteps.size() > 0) {
                // try to find the step in cachedSteps
                for (SolutionStep step : cachedSteps) {
                    if (step.getType() == type) {
                        return step;
                    }
                }
            }
        } else {
            stepNumber = finder.getStepNumber();
            cachedSteps.clear();
            rectAnz = 0;
        }
        lastSearchWasUR = true;
        lastSearchWasAR = false;
        return getAllUniquenessInternal(type, true);
    }

    /**
     * More or less equal to {@link #getUniqueness(sudoku.SolutionType)}.
     * @param type
     * @return
     */
    private SolutionStep getAvoidableRectangle(SolutionType type) {
        if (finder.getStepNumber() == stepNumber && lastSearchWasAR) {
            if (cachedSteps.size() > 0) {
                // try to find the step in cachedSteps
                for (SolutionStep step : cachedSteps) {
                    if (step.getType() == type) {
                        return step;
                    }
                }
            }
        } else {
            stepNumber = finder.getStepNumber();
            cachedSteps.clear();
            rectAnz = 0;
        }
        lastSearchWasUR = false;
        lastSearchWasAR = true;
        return getAllAvoidableRectangles(type, true);
    }

    /**
     * Find all Uniqueness steps except BUG+1
     * @return
     */
    protected List<SolutionStep> getAllUniqueness() {
        stepNumber = -1;
        cachedSteps.clear();
        rectAnz = 0;
        lastSearchWasAR = false;
        lastSearchWasUR = false;
        sudoku = finder.getSudoku();
        List<SolutionStep> tmpSteps = new ArrayList<SolutionStep>();
        List<SolutionStep> oldSteps = steps;
        steps = tmpSteps;
        getAllUniquenessInternal(null, false);
        getAllAvoidableRectangles(null, false);
        steps = oldSteps;
        return tmpSteps;
    }

    /**
     * Find all available Avoidable Rectangles. If <code>onlyone</code> is
     * <code>true</code>, the first one found is returned.
     * @param type
     * @param onlyOne
     * @return
     */
    private SolutionStep getAllAvoidableRectangles(SolutionType type, boolean onlyOne) {
        // check for solved cells that are not givens
        for (int i = 0; i < Sudoku2.LENGTH; i++) {
            if (sudoku.getValue(i) == 0 || sudoku.isFixed(i)) {
                // cell is either not solved or a given
                continue;
            }
            cand1 = sudoku.getValue(i);
            SolutionStep step = findUniquenessForStartCell(i, true, type, onlyOne);
            if (step != null && onlyOne) {
                return step;
            }
            step = findUniquenessForStartCell(i, true, type, onlyOne);
            if (step != null && onlyOne) {
                return step;
            }
        }
        return null;
    }

    /**
     * Find all bivalue cells and take them as starting point for a search.
     * the search itself is delegated to ....
     * @param type
     * @param onlyOne
     * @return
     */
    private SolutionStep getAllUniquenessInternal(SolutionType type, boolean onlyOne) {
        // get bivalue cells
        for (int i = 0; i < Sudoku2.LENGTH; i++) {
            if (sudoku.getAnzCandidates(i) == 2) {
                int[] cands = sudoku.getAllCandidates(i);
                cand1 = cands[0];
                cand2 = cands[1];
                SolutionStep step = findUniquenessForStartCell(i, false, type, onlyOne);
                if (step != null && onlyOne) {
                    return step;
                }
            }
        }
        return null;
    }

    /**
     * Only one cell exists with three candidates, all other cells have two candidates.
     * All candidates appear in all units exactly twice, except one of the candidates
     * in the cell with the three candidates.
     * @return
     */
    private SolutionStep getBugPlus1() {
        // check the number of candidates in all cells
        int index3 = -1;
        for (int i = 0; i < Sudoku2.LENGTH; i++) {
            int anz = sudoku.getAnzCandidates(i);
            if (anz > 3) {
                // no BUG+1!
                return null;
            } else if (anz == 3) {
                if (index3 != -1) {
                    // second cell with three candidates -> no BUG+1!
                    return null;
                }
                index3 = i;
            }
        }
        if (index3 == -1) {
            // no cell with three candidates exists -> no BUG+1
            return null;
        }
        // all cells have two candidates except one and that cell is index3
        // -> check for possible BUG+1
        int cand3 = -1;
        bugConstraints[0] = -1;
        bugConstraints[1] = -1;
        bugConstraints[2] = -1;
        byte[][] free = sudoku.getFree();
        for (int constr = 0; constr < free.length; constr++) {
            for (int cand = 1; cand <= 9; cand++) {
                int anz = free[constr][cand];
                if (anz > 3) {
                    // no BUG+1
                    return null;
                } else if (anz == 3) {
                    // must be for same candidate and only once per type of constraint
                    if (bugConstraints[constr / 9] != -1 || (cand3 != -1 && cand3 != cand)) {
                        // cant be BUG+1
                        return null;
                    }
                    cand3 = cand;
                    bugConstraints[constr / 9] = constr;
                }
            }
        }
        // still here?
        if (sudoku.isCandidate(index3, cand3) && Sudoku2.CONSTRAINTS[index3][0] == bugConstraints[0] &&
                Sudoku2.CONSTRAINTS[index3][1] == bugConstraints[1] &&
                Sudoku2.CONSTRAINTS[index3][2] == bugConstraints[2]) {
            // ok it IS a BUG+1
            // don't call initStep()!
            globalStep.reset();
            globalStep.setType(SolutionType.BUG_PLUS_1);
            int[] candArr = sudoku.getAllCandidates(index3);
            for (int i = 0; i < candArr.length; i++) {
                if (candArr[i] != cand3) {
                    globalStep.addCandidateToDelete(index3, candArr[i]);
                }
            }
            return (SolutionStep) globalStep.clone();
        }
        return null;
    }

    /**
     * - Feststellen, in welcher Unit1 sich die Zelle befindet
     * - alle anderen Zellen dieser Unit1 anschauen, die im selben Block sind
     * - wenn eine Zelle die selben zwei Kandidaten (plus beliebig viele andere) hat, für beide Zellen
     *   die Unit2 finden
     * - Alle Zellen dieser Unit2(s) durchgehen, die nicht im selben Block wie die Unit1(s) sind
     * - Wenn beide Zellen die selben beiden Kandidaten haben -> mögliches Unique Rectangle, SolutionStep prüfen
     * 
     * when checking for avoidable rectangles some rules change: index11/index12 have
     * to be solved cells, they designate the candidates; the other side of the
     * avoidable rectangle has to have at least one not solved cell. All solved cells
     * must not be givens.
     */
    private SolutionStep findUniquenessForStartCell(int index11, boolean avoidable, 
            SolutionType type, boolean onlyOne) {
        boolean allowMissing = Options.getInstance().isAllowUniquenessMissingCandidates();
//        System.out.println("index11 = " + index11 + ", cand1 = " + cand1 + ", cand2 = " + cand2);
        
        // find a second cell within the same block that contains the same two candidates
        // and is either in the same row or in the same column
        // start the search with index11 to avoid double findings
        int line11 = Sudoku2.getLine(index11);
        int col11 = Sudoku2.getCol(index11);
        int block11 = Sudoku2.getBlock(index11);
        int cell11 = sudoku.getCell(index11);
        SudokuSet allowedCand1 = finder.getCandidatesAllowed()[cand1];
        SudokuSet allowedCand2 = finder.getCandidatesAllowed()[cand2];
//        System.out.println("allowedCand1: " + allowedCand1);
//        System.out.println("allowedCand2: " + allowedCand2);
        int[] blockIndices = Sudoku2.BLOCKS[Sudoku2.getBlock(index11)];
        for (int i = 0; i < blockIndices.length; i++) {
            if (blockIndices[i] == index11) {
                continue;
            }
            int index12 = blockIndices[i];
            if (line11 != Sudoku2.getLine(index12) && col11 != Sudoku2.getCol(index12)) {
                // not in the same row or col -> cannot become a rectangle
                continue;
            }
            // check it
            int cell12 = sudoku.getCell(index12);
            if ((!avoidable && (sudoku.getValue(index12) == 0 &&
                    ((! allowMissing && (cell11 & cell12) == cell11) ||
                    (allowMissing && allowedCand1.contains(index12) && allowedCand2.contains(index12))))) ||
                    (avoidable && (sudoku.getValue(index12) != 0 && ! sudoku.isFixed(index12)))) {
                if (avoidable) {
                    cand2 = sudoku.getValue(index12);
                }
                // possible second endpoint: check for rectangles
                boolean isLines = line11 == Sudoku2.getLine(index12);
                // get the units that have to hold the opposite side of the rectangle
                int[] unit11 = Sudoku2.ALL_UNITS[isLines ? Sudoku2.getCol(index11) + 9 : Sudoku2.getLine(index11)];
                int[] unit12 = Sudoku2.ALL_UNITS[isLines ? Sudoku2.getCol(index12) + 9 : Sudoku2.getLine(index12)];
                // check the units: two adequat cells at the same indices in a different block
                for (int j = 0; j < unit11.length; j++) {
                    if (Sudoku2.getBlock(unit11[j]) == block11) {
                        // must be another block!
                        continue;
                    }
                    int index21 = unit11[j];
                    int index22 = unit12[j];
                    int cell21 = sudoku.getCell(index21);
                    int cell22 = sudoku.getCell(index22);

                    if ((!avoidable && (! allowMissing && (cell21 & cell11) == cell11 && (cell22 & cell11) == cell11)) ||
                            (allowMissing && allowedCand1.contains(index21) && allowedCand1.contains(index22) &&
                            allowedCand2.contains(index22) && allowedCand2.contains(index22)) ||
                            (avoidable && ((sudoku.getValue(index21) == cand2 && !sudoku.isFixed(index21) && sudoku.getValue(index22) == 0 && sudoku.isCandidate(index22, cand1) && sudoku.getAnzCandidates(index22) == 2) ||
                            (sudoku.getValue(index22) == cand1 && !sudoku.isFixed(index22) && sudoku.getValue(index21) == 0 && sudoku.isCandidate(index21, cand2) && sudoku.getAnzCandidates(index21) == 2) ||
                            (sudoku.getValue(index21) == 0 && sudoku.isCandidate(index21, cand2) && sudoku.getAnzCandidates(index21) == 2 &&
                            sudoku.getValue(index22) == 0 && sudoku.isCandidate(index22, cand1) && sudoku.getAnzCandidates(index22) == 2)))) {
                        // ok, could be a UR: didi we have it alreyd?
                        if (checkRect(index11, index12, index21, index22)) {
                            // check for UR Type and get candidates that can be deleted
                            indexe[0] = index11;
                            indexe[1] = index12;
                            indexe[2] = index21;
                            indexe[3] = index22;
                            SolutionStep step = null;
                            if (avoidable) {
                                step = checkAvoidableRectangle(index21, index22, type, onlyOne);
                            } else {
                                step = checkURForStep(type, onlyOne);
                            }
                            if (step != null && onlyOne) {
                                return step;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks the UR in {@link #indexe} for a possible UR. All UR types are checked
     * at once, if <code>onlyOne</code> is set and a UR of type <code>searchType</code>
     * could be found it is returned immediately.
     * @param searchType
     * @param onlyOne
     * @return
     */
    private SolutionStep checkURForStep(SolutionType searchType, boolean onlyOne) {
        // collects all cells that contain only cand1 and/or cand2 in twoCandidates
        // and all canddiates in the UR except cand1 and cand2 in additionalCandidates
        initCheck(indexe);
        
        // TODO: Delete code before making a release
        // ignore all steps, where not all cells hold both candidates
//        boolean missing = false;
//        for (int i = 0; i < indexe.length; i++) {
//            if (! sudoku.isCandidate(indexe[i], cand1) || ! sudoku.isCandidate(indexe[i], cand2)) {
//                // UR has missing candidates
//                missing = true;
//            }
//        }
//        if(! missing) {
//            return null;
//        }
        // END TODO

        SolutionStep step = null;
        int twoSize = twoCandidates.size();
        short urMask = (short)(Sudoku2.MASKS[cand1] | Sudoku2.MASKS[cand2]);

        // Uniqueness Test 1: 3 cells have only 2 candidates -> delete cand1 and cand2 from the
        // fourth cell
        if (twoSize == 3) {
            // yes, it is a Uniqueness Type 1
            initStep(SolutionType.UNIQUENESS_1);
            int delIndex = additionalCandidates.get(0);
            if (sudoku.isCandidate(delIndex, cand1)) {
                globalStep.addCandidateToDelete(delIndex, cand1);
            }
            if (sudoku.isCandidate(delIndex, cand2)) {
                globalStep.addCandidateToDelete(delIndex, cand2);
            }
            if (globalStep.getCandidatesToDelete().size() > 0) {
                step = (SolutionStep) globalStep.clone();
                if (onlyOne) {
                    if (searchType == step.getType()) {
                        return step;
                    } else {
                        cachedSteps.add(step);
                    }
                } else {
                    steps.add(step);
                }
            }
        }

        // Uniqueness Test 2/5/5A: one or two cells have 2 candidates, the other have exactly
        // one additional candidate, which is the same for all cells: This candidate can be deleted
        // from all cells that can see all UR cells with that candidate.
        // If there are only two additional candidate cells and if they are in the same cell or row
        // it is a UR 2, else a UR 5
        if (twoSize == 2 || twoSize == 1) {
            short addMask = 0;
            tmpSet.setAll();
            for (int i = 0; i < additionalCandidates.size(); i++) {
                int index3 = additionalCandidates.get(i);
                addMask |= (short)(sudoku.getCell(index3) & ~urMask);
                if (Sudoku2.ANZ_VALUES[addMask] > 1) {
                    break;
                }
                tmpSet.and(Sudoku2.buddies[index3]);
            }
            if (Sudoku2.ANZ_VALUES[addMask] == 1) {
                // ok, valid UR 2 or UR 5
                // get cells that can see all cells with the additional candidate
                int addCand = Sudoku2.CAND_FROM_MASK[addMask];
                tmpSet.and(finder.getCandidates()[addCand]);
                if (!tmpSet.isEmpty()) {
                    // ok, valid step
                    SolutionType type = SolutionType.UNIQUENESS_2;
                    int i1 = additionalCandidates.get(0);
                    int i2 = additionalCandidates.get(1);
                    if (additionalCandidates.size() == 3 || 
                            (Sudoku2.getLine(i1) != Sudoku2.getLine(i2) &&
                            Sudoku2.getCol(i1) != Sudoku2.getCol(i2))) {
                        type = SolutionType.UNIQUENESS_5;
                    }
                    initStep(type);
                    for (int i = 0; i < tmpSet.size(); i++) {
                        globalStep.addCandidateToDelete(tmpSet.get(i), addCand);
                    }
                    step = (SolutionStep) globalStep.clone();
                    if (onlyOne) {
                        if (searchType == step.getType()) {
                            return step;
                        } else {
                            cachedSteps.add(step);
                        }
                    } else {
                        steps.add(step);
                    }
                }
            }
        }

        // Uniqueness Test 3: If two UR cells have additional candidates, find in one of their houses
        // additional (k - 1) cells, so that all cells contain exactly k candidates (cand1 and cand2
        // may not appear in any of these cells). If we find such cells, the k candidates can be
        // deleted from all other cells in the house
        if (twoSize == 2) {
            short u3Cands = 0;
            // get all additional candidates
            for (int i = 0; i < additionalCandidates.size(); i++) {
                int index3 = additionalCandidates.get(i);
                u3Cands |= (short)(sudoku.getCell(index3) & ~urMask);
            }
            // check the houses
            int i1 = additionalCandidates.get(0);
            int i2 = additionalCandidates.get(1);
            if (Sudoku2.getLine(i1) == Sudoku2.getLine(i2)) {
                step = checkUniqueness3(Sudoku2.LINE, Sudoku2.LINES[Sudoku2.getLine(i1)], u3Cands,
                        urMask, searchType, onlyOne);
                if (step != null && onlyOne) {
                    // if steps should be cached that was already done
                    return step;
                }
            }
            if (Sudoku2.getCol(i1) == Sudoku2.getCol(i2)) {
                step = checkUniqueness3(Sudoku2.COL, Sudoku2.COLS[Sudoku2.getCol(i1)], u3Cands,
                        urMask, searchType, onlyOne);
                if (step != null && onlyOne) {
                    // if steps should be cached that was already done
                    return step;
                }
            }
            if (Sudoku2.getBlock(i1) == Sudoku2.getBlock(i2)) {
                step = checkUniqueness3(Sudoku2.BLOCK, Sudoku2.BLOCKS[Sudoku2.getBlock(i1)], u3Cands,
                        urMask, searchType, onlyOne);
                if (step != null && onlyOne) {
                    // if steps should be cached that was already done
                    return step;
                }
            }
        }

        // Uniqueness Test 4: Two cells with additional candidates; in all cells seen by
        // both cells one of the UR candidates is missing: the other UR candidate can be
        // eliminated from the cells with the additional candidates
        // the cells with additional candidates have to be in one row or one column
        if (twoSize == 2) {
//            System.out.println("check UR4 " + additionalCandidates + "/" + cand1 + "/" + cand2);
            int i1 = additionalCandidates.get(0);
            int i2 = additionalCandidates.get(1);
            if ((Sudoku2.getLine(i1) == Sudoku2.getLine(i2)) || (Sudoku2.getCol(i1) == Sudoku2.getCol(i2))) {
                // get all cells that can see both cells with additional candidates
                tmpSet.setAnd(Sudoku2.buddies[i1], Sudoku2.buddies[i2]);
//                System.out.println("   " + tmpSet);
                // now check
                int delCand = -1;
                tmpSet1.setAnd(tmpSet, finder.getCandidates()[cand1]);
//                System.out.println("   " + tmpSet1);
                if (tmpSet1.isEmpty()) {
                    delCand = cand2;
                } else {
                    tmpSet1.setAnd(tmpSet, finder.getCandidates()[cand2]);
//                    System.out.println("   " + tmpSet1);
                    if (tmpSet1.isEmpty()) {
                        delCand = cand1;
                    }
                }
//                System.out.println("   delCand: " + delCand);
                if (delCand != -1) {
                    initStep(SolutionType.UNIQUENESS_4);
                    if (sudoku.isCandidate(i1, delCand)) {
                        globalStep.addCandidateToDelete(i1, delCand);
                    }
                    if (sudoku.isCandidate(i2, delCand)) {
                        globalStep.addCandidateToDelete(i2, delCand);
                    }
                    if (globalStep.getCandidatesToDelete().size() > 0) {
                        step = (SolutionStep) globalStep.clone();
                        if (onlyOne) {
                            if (searchType == step.getType()) {
                                return step;
                            } else {
                                cachedSteps.add(step);
                            }
                        } else {
                            steps.add(step);
                        }
                    }
                }
            }
        }

        // Uniqueness Test 6: Two cells with additional candidates located diagonally; if in both lines and cols none of
        // the other candidates contain cand1 -> cand2 can be deleted from the diagonal cells
        if (twoSize == 2) {
            int i1 = additionalCandidates.get(0);
            int i2 = additionalCandidates.get(1);
            if ((Sudoku2.getLine(i1) != Sudoku2.getLine(i2)) && (Sudoku2.getCol(i1) != Sudoku2.getCol(i2))) {
                // get all cells in both lines and cols but without the UR itself
                tmpSet.set(Sudoku2.LINE_TEMPLATES[Sudoku2.getLine(i1)]);
                tmpSet.or(Sudoku2.COL_TEMPLATES[Sudoku2.getCol(i1)]);
                tmpSet.or(Sudoku2.LINE_TEMPLATES[Sudoku2.getLine(i2)]);
                tmpSet.or(Sudoku2.COL_TEMPLATES[Sudoku2.getCol(i2)]);
                tmpSet.andNot(additionalCandidates);
                tmpSet.andNot(twoCandidates);
                // now check for additional candidates
                int delCand = -1;
                tmpSet1.setAnd(tmpSet, finder.getCandidates()[cand1]);
                if (tmpSet1.isEmpty()) {
                    delCand = cand1;
                } else {
                    tmpSet1.setAnd(tmpSet, finder.getCandidates()[cand2]);
                    if (tmpSet1.isEmpty()) {
                        delCand = cand2;
                    }
                }
                if (delCand != -1) {
                    initStep(SolutionType.UNIQUENESS_6);
                    if (sudoku.isCandidate(i1, delCand)) {
                        globalStep.addCandidateToDelete(i1, delCand);
                    }
                    if (sudoku.isCandidate(i2, delCand)) {
                        globalStep.addCandidateToDelete(i2, delCand);
                    }
                    if (globalStep.getCandidatesToDelete().size() > 0) {
                        step = (SolutionStep) globalStep.clone();
                        if (onlyOne) {
                            if (searchType == step.getType()) {
                                return step;
                            } else {
                                cachedSteps.add(step);
                            }
                        } else {
                            steps.add(step);
                        }
                    }
                }
            }
        }

        // Hidden Rectangle: If one or two cells contain only two candidates (if two they
        // have to be aligned diagonally) one of the cells with only two cands is the corner; if
        // the line and the col through the other three cells contain only two occurences
        // of one of the uniqueness candidates each, the other uniqueness candidate
        // can be deleted
        if (twoSize == 2 || twoSize == 1) {
            int i1 = twoCandidates.get(0);
            int i2 = twoCandidates.get(1);
            boolean doCheck = true;
            if (twoSize == 2) {
                // must be aligned diagonally -> must not be in the same row or column
                if (Sudoku2.getLine(i1) == Sudoku2.getLine(i2) ||
                        Sudoku2.getCol(i1) == Sudoku2.getCol(i2)) {
                    doCheck = false;
                }
            }
            if (doCheck) {
                step = checkHiddenRectangle(i1, searchType, onlyOne);
                if (step != null && onlyOne) {
                    return step;
                }
                if (twoCandidates.size() == 2) {
                    step = checkHiddenRectangle(i2, searchType, onlyOne);
                    if (step != null && onlyOne) {
                        return step;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Avoidable Rectangle: If only one of cell21/cell22 is not set,
     * cand1/cand2 can be deleted from that cell. If both cell21 and cell22
     * are not set, cell21 must contain cand2, cell22 must contain cand1
     * and both cells have to have the same additional candidate. This
     * candidate can be deleted from all cells that see both cell21 and cell22.
     * @param index21
     * @param index22
     * @param type
     * @param onlyOne
     * @return
     */
    private SolutionStep checkAvoidableRectangle(int index21, int index22, SolutionType type, boolean onlyOne) {
        SolutionStep step = null;
        if (sudoku.getValue(index21) != 0 || sudoku.getValue(index22) != 0) {
            // first type: cand1/cand2 can be deleted
            initStep(SolutionType.AVOIDABLE_RECTANGLE_1);
            if (sudoku.getValue(index21) != 0) {
                if (sudoku.isCandidate(index22, cand1)) {
                    globalStep.addCandidateToDelete(index22, cand1);
                }
            } else {
                if (sudoku.isCandidate(index21, cand2)) {
                    globalStep.addCandidateToDelete(index21, cand2);
                }
            }
            if (globalStep.getCandidatesToDelete().size() > 0) {
                step = (SolutionStep) globalStep.clone();
                if (onlyOne) {
                    if (type == SolutionType.AVOIDABLE_RECTANGLE_1) {
                        return step;
                    } else {
                        cachedSteps.add(step);
                    }
                } else {
                    steps.add(step);
                }
            }
        } else {
            // the additional candidate in index21 and index22 has to be the same
//            System.out.println("check AR " + index21 + "/" + index22);
            int[] cands = sudoku.getAllCandidates(index21);
            int additionalCand = cands[0];
            if (additionalCand == cand2) {
                additionalCand = cands[1];
            }
            if (! sudoku.isCandidate(index22, additionalCand)) {
                // wrong candidate -> do nothing
                return null;
            }
            // check for deletions
            tmpSet.set(Sudoku2.buddies[index21]);
            tmpSet.and(Sudoku2.buddies[index22]);
            tmpSet.and(finder.getCandidates()[additionalCand]);
            if (tmpSet.isEmpty()) {
                // no eliminations possible -> do nothing
                return null;
            }
            initStep(SolutionType.AVOIDABLE_RECTANGLE_2);
            for (int i = 0; i < tmpSet.size(); i++) {
                globalStep.addCandidateToDelete(tmpSet.get(i), additionalCand);
            }
            globalStep.addEndoFin(index21, additionalCand);
            globalStep.addEndoFin(index22, additionalCand);
            step = (SolutionStep) globalStep.clone();
            if (onlyOne) {
                if (type == SolutionType.AVOIDABLE_RECTANGLE_2) {
                    return step;
                } else {
                    cachedSteps.add(step);
                }
            } else {
                steps.add(step);
            }
        }
        return null;
    }

    /**
     *  Hidden Rectangle: If one or two cells contain only two candidates (if two they
     *  have to be aligned diagonally) one of the cells with only two cands is the corner; if
     *  the line and the col through the other three cells contain only two occurences
     *  of one of the uniqueness candidates each, the other uniqueness candidate
     *  can be deleted
     *
     * @param cornerIndex
     */
    private SolutionStep checkHiddenRectangle(int cornerIndex, SolutionType type, boolean onlyOne) {
        // whether there is only one cell or two cells with two candidates,
        // checking two additional cells is enough to get the line and col 
        // that have to be checked
        int lineC = Sudoku2.getLine(cornerIndex);
        int colC = Sudoku2.getCol(cornerIndex);
        int i1 = additionalCandidates.get(0);
        int i2 = additionalCandidates.get(1);
        int line1 = Sudoku2.getLine(i1);
        if (line1 == lineC) {
            line1 = Sudoku2.getLine(i2);
        }
        int col1 = Sudoku2.getCol(i1);
        if (col1 == colC) {
            col1 = Sudoku2.getCol(i2);
        }

        SolutionStep step = checkCandForHiddenRectangle(line1, col1, cand1, cand2, type, onlyOne);
        if (step != null && onlyOne) {
            return step;
        }
        step = checkCandForHiddenRectangle(line1, col1, cand2, cand1, type, onlyOne);
        if (step != null && onlyOne) {
            return step;
        }
        return null;
    }

    /**
     * In line <code>line</code> and col <code>col</code> the candidate
     * <code>cand1</code> may appear only twice. If that is the case
     * candidate <code>cand2</code> may be deleted from the cell at the
     * intersection of <code>line</code> and <code>col</code>.<br>
     * CAUTION: the condition "only twice" is invalid if a UR candidate
     * is missing from the additional cell itself!
     * 
     * @param line
     * @param col
     * @param cand1
     * @param cand2
     */
    private SolutionStep checkCandForHiddenRectangle(int line, int col, int cand1, int cand2,
            SolutionType type, boolean onlyOne) {
//        System.out.println("check " + line + "/" + col + "/" + cand1 + "/" + cand2);
        tmpSet1.setOr(twoCandidates, additionalCandidates);
        tmpSet.set(finder.getCandidates()[cand1]);
        tmpSet.and(Sudoku2.LINE_TEMPLATES[line]);
        tmpSet.andNot(tmpSet1);
//        System.out.println(tmpSet);
        if (! tmpSet.isEmpty()) {
            return null;
        }
        tmpSet.set(finder.getCandidates()[cand1]);
        tmpSet.and(Sudoku2.COL_TEMPLATES[col]);
        tmpSet.andNot(tmpSet1);
//        System.out.println(tmpSet);
        if (! tmpSet.isEmpty()) {
            return null;
        }
        // ok ->hidden rectangle; delete cand2 from cell at the intersection
        int delIndex = Sudoku2.getIndex(line, col);

        initStep(SolutionType.HIDDEN_RECTANGLE);
        if (sudoku.isCandidate(delIndex, cand2)) {
            globalStep.addCandidateToDelete(delIndex, cand2);
        }
        if (globalStep.getCandidatesToDelete().size() > 0) {
            SolutionStep step = (SolutionStep) globalStep.clone();
            if (onlyOne) {
                if (type == step.getType()) {
                    return step;
                } else {
                    cachedSteps.add(step);
                }
            } else {
                steps.add(step);
            }
        }
        return null;
    }

    /**
     * Collect all cells that have to be checked and call the recursive check method.
     * @param unitType
     * @param unit
     * @param u3Cands
     * @param urMask
     * @param searchType
     * @param onlyOne
     * @return
     */
    private SolutionStep checkUniqueness3(int unitType, int[] unit, short u3Cands, 
            short urMask, SolutionType searchType, boolean onlyOne) {
//        System.out.println("search U3: " + Arrays.toString(Sudoku2.POSSIBLE_VALUES[u3Cands]) + "/" + Arrays.toString(Sudoku2.POSSIBLE_VALUES[urMask]));
        SudokuSet u3Indices = new SudokuSet();
        tmpSet.set(twoCandidates);
        tmpSet.or(additionalCandidates);
        for (int i = 0; i < unit.length; i++) {
            // a cell has to be checked, if it is not set, not part of the UR without additional
            // candidates and doesnt contain cand1 or cand2
            short cell = sudoku.getCell(unit[i]);
            if (cell != 0 && (cell & urMask) == 0 && !tmpSet.contains(unit[i])) {
                u3Indices.add(unit[i]);
            }
        }
        // now check all combinations: the additional cells have to
        // be in the set always
        if (! u3Indices.isEmpty()) {
//            System.out.println("   " + u3Indices);
            SolutionStep step = checkUniqueness3Recursive(unitType, unit, u3Indices, 
                    u3Cands, new SudokuSet(additionalCandidates), 0, searchType, onlyOne);
            if (step != null && onlyOne) {
                return step;
            }
        }
        return null;
    }

    /**
     * Do the recursive check for UR 3.
     * @param type
     * @param unit
     * @param u3Indices
     * @param candsIncluded
     * @param indicesIncluded
     * @param startIndex
     * @param searchType
     * @param onlyOne
     * @return
     */
    private SolutionStep checkUniqueness3Recursive(int type, int[] unit, SudokuSet u3Indices,
            short candsIncluded, SudokuSet indicesIncluded, int startIndex,
            SolutionType searchType, boolean onlyOne) {
        SolutionStep step = null;
        for (int i = startIndex; i < u3Indices.size(); i++) {
            short aktCands = candsIncluded;
            SudokuSet aktIndices = indicesIncluded.clone();
            aktIndices.add(u3Indices.get(i));
            // collect all additional candidates
            aktCands |= sudoku.getCell(u3Indices.get(i));
            // if we search for blocks, a check for deletable candidates is only done,
            // if the cells are not all in the same line or column (in that case the
            // step has already been checked)
            if (type != Sudoku2.BLOCK || !isSameLineOrCol(aktIndices)) {
                // if the number of additional candidates is one smaller as the number of cells
                // in aktIndices, it is a UR 3 -> check if candidates can be deleted
                if (Sudoku2.ANZ_VALUES[aktCands] == (aktIndices.size() - 1)) {
//                    System.out.println("UR3 found (" + type + "):");
//                    System.out.println("  cands: " + Arrays.toString(Sudoku2.POSSIBLE_VALUES[aktCands]));
//                    System.out.println("  cells: " + aktIndices + "/" + isSameLineOrCol(aktIndices));
                    // candidates can be deleted in all cells of the same unit, which are not contained in
                    // aktIndices (that includes the UR candidates). All candidates belonging
                    // to the Naked Subset are written as fins (for display only)
                    initStep(SolutionType.UNIQUENESS_3);
                    for (int j = 0; j < unit.length; j++) {
                        if (sudoku.getValue(unit[j]) == 0 && !aktIndices.contains(unit[j])) {
                            short delCands = (short) (sudoku.getCell(unit[j]) & aktCands);
//                            System.out.println("   unit[j] = " + unit[j] + ", value = " + sudoku.getValue(unit[j]) + ", delCands = " + Integer.toBinaryString(delCands));
                            if (Sudoku2.ANZ_VALUES[delCands] == 0) {
                                // no candidates to delete
                                continue;
                            }
                            int[] delCandsArray = Sudoku2.POSSIBLE_VALUES[delCands];
                            for (int k = 0; k < delCandsArray.length; k++) {
                                globalStep.addCandidateToDelete(unit[j], delCandsArray[k]);
//                                System.out.println("      ctd: " + unit[j] + "/" + delCandsArray[k]);
                            }
                        }
                    }
                    // do we have a step?
                    if (globalStep.getCandidatesToDelete().size() > 0) {
                        // write the fins
                        int[] aktCandsArray = Sudoku2.POSSIBLE_VALUES[aktCands];
                        for (int k = 0; k < aktCandsArray.length; k++) {
                            int cTmp = aktCandsArray[k];
                            for (int l = 0; l < aktIndices.size(); l++) {
                                if (sudoku.isCandidate(aktIndices.get(l), cTmp)) {
                                    globalStep.addFin(aktIndices.get(l), cTmp);
                                }
                            }
//                        for (int l = 0; l < additionalCandidates.size(); l++) {
//                            if (sudoku.isCandidate(additionalCandidates.get(l), cTmp)) {
//                                globalStep.addFin(additionalCandidates.get(l), cTmp);
//                            }
//                        }
                        }
                        if (type == Sudoku2.LINE || type == Sudoku2.COL) {
                            // could be Locked Subset
                            int block = getBlockForCheck3(aktIndices);
                            if (block != -1) {
                                int[] unit1 = Sudoku2.BLOCKS[block];
                                for (int j = 0; j < unit1.length; j++) {
                                    if (sudoku.getValue(unit1[j]) == 0 && !aktIndices.contains(unit1[j])) {
                                        short delCands = (short) (sudoku.getCell(unit1[j]) & aktCands);
//                                        System.out.println("   unit1[j] = " + unit1[j] + ", value = " + sudoku.getValue(unit1[j]) + ", delCands = " + Integer.toBinaryString(delCands));
                                        if (Sudoku2.ANZ_VALUES[delCands] == 0) {
                                            // no candidates to delete
                                            continue;
                                        }
                                        int[] delCandsArray = Sudoku2.POSSIBLE_VALUES[delCands];
                                        for (int k = 0; k < delCandsArray.length; k++) {
                                            globalStep.addCandidateToDelete(unit1[j], delCandsArray[k]);
//                                            System.out.println("      ctd: " + unit1[j] + "/" + delCandsArray[k]);
                                        }
                                    }
                                }
                            }
                        }
                        step = (SolutionStep) globalStep.clone();
                        if (onlyOne) {
                            if (searchType == step.getType()) {
                                return step;
                            } else {
                                cachedSteps.add(step);
                            }
                        } else {
                            steps.add(step);
                        }
                    }
                }
            }
            // and on to the next level
            step = checkUniqueness3Recursive(type, unit, u3Indices, aktCands, aktIndices, i + 1, searchType, onlyOne);
            if (step != null && onlyOne) {
                return step;
            }
        }
        return null;
    }

    /**
     * If all cells in <code>aktIndices</code> belong to the same block,
     * return that block (for UR3 check)
     * @param aktIndices
     * @return
     */
    private int getBlockForCheck3(SudokuSet aktIndices) {
        if (aktIndices.isEmpty()) {
            return -1;
        }
        int block = Sudoku2.getBlock(aktIndices.get(0));
        for (int i = 1; i < aktIndices.size(); i++) {
            if (Sudoku2.getBlock(aktIndices.get(i)) != block) {
                return -1;
            }
        }
        return block;
    }

    /**
     * Checks, if all indices in <code>aktIndices</code> are in the same line
     * or column (for UR check).
     * @param aktIndices
     * @return
     */
    private boolean isSameLineOrCol(SudokuSet aktIndices) {
        if (aktIndices.isEmpty()) {
            return false;
        }
        boolean sameLine = true;
        boolean sameCol = true;
        int line = Sudoku2.getLine(aktIndices.get(0));
        int col = Sudoku2.getCol(aktIndices.get(0));
        for (int i = 1; i < aktIndices.size(); i++) {
            if (Sudoku2.getLine(aktIndices.get(i)) != line) {
                sameLine = false;
            }
            if (Sudoku2.getCol(aktIndices.get(i)) != col) {
                sameCol = false;
            }
        }
        return sameLine || sameCol;
    }

    /**
     * Initialize {@link #globalStep} for a Unique Rectangle.
     * @param type
     */
    private void initStep(SolutionType type) {
        globalStep.reset();
        globalStep.setType(type);
        if (indexe != null) {
            globalStep.addValue(cand1);
            globalStep.addValue(cand2);
            globalStep.addIndex(indexe[0]);
            globalStep.addIndex(indexe[1]);
            globalStep.addIndex(indexe[2]);
            globalStep.addIndex(indexe[3]);
        }
    }

    /**
     * Get all cells that hold only {@link #cand1} and {@link #cand2} (or possibly
     * only one of them). Collect all additional candidates in {@link #additionalCandidates}.
     * @param indices
     */
    private void initCheck(int[] indices) {
        twoCandidates.clear();
        additionalCandidates.clear();
        short mask = (short)~(Sudoku2.MASKS[cand1] | Sudoku2.MASKS[cand2]);
        for (int i = 0; i < indices.length; i++) {
            short addTemp = (short)(sudoku.getCell(indices[i]) & mask);
            if (addTemp == 0) {
                twoCandidates.add(indices[i]);
            } else {
                additionalCandidates.add(indices[i]);
            }
        }
    }

    /**
     * Checks if the UR formed by the four indices is already contained in {@link #rectangles}.
     * If the buffer is full, the UR is treated as if it were not already handled.
     * @param i11
     * @param i12
     * @param i21
     * @param i22
     * @return
     */
    private boolean checkRect(int i11, int i12, int i21, int i22) {
        tmpRect[0] = i11;
        tmpRect[1] = i12;
        tmpRect[2] = i21;
        tmpRect[3] = i22;
        // sort the indices (BubblSort is sufficient for 4 indices only)
        for (int i = tmpRect.length; i > 1; i--) {
            boolean changed = false;
            for (int j = 1; j < i; j++) {
                if (tmpRect[j - 1] > tmpRect[j]) {
                    int tmp = tmpRect[j - 1];
                    tmpRect[j - 1] = tmpRect[j];
                    tmpRect[j] = tmp;
                    changed = true;
                }
            }
            if (changed == false) {
                break;
            }
        }
        // build the new rectangle number
        int rect = (((tmpRect[0] * 10) + tmpRect[1]) * 10 + tmpRect[2]) * 10 + tmpRect[3];
        // new search for it in rectangles
        for (int i = 0; i < rectAnz; i++) {
            if (rectangles[i] == rect) {
                // allready contained in recangles -> dont check anymore
                return false;
            }
        }
        // a new UR -> put it in rectangles if there is any space left
        if (rectAnz < rectangles.length) {
            rectangles[rectAnz++] = rect;
        } else {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Find Uniqueness: Kein Platz mehr in rectangles!");
        }
        // check it
        return true;
    }

    public static void main(String[] args) {
        Sudoku2 sudoku = new Sudoku2();
        //sudoku.setSudoku(":0000:x:2.7.86.5.8169..2.79.572.8.6..4..7.....2.3.56....5..7..6.3.19475.....43924.9.7.6..:141 342 844 348 149 949 854 362 368 892 894 896::");
        //sudoku.setSudoku(":0000:x:837159246.426837.96.9742.38.2..169..37..95.62.96.27...9..2683..28357469176.93182.:443 449 453 469::");
        //sudoku.setSudoku(":0000:x:.78.....6.19.5.4.8.53...29.....76...761.4.9253..59.....2....7838.7.3.1..13.....4.:241 441 344 844 866 471 671 694 695 596::");
        sudoku.setSudoku(":0000:x:..513.9.2..1..97..89..2.4..45.29.136962351847.1...6..9.349...78...8..39..89.736..:625 271 571 485::");
        sudoku.setSudoku(":0000:x:+41+67+8.5...35..4+8+178+7.+13+5.+46..4+5716..+1.3+4..7.+5+6+5789+34+2+15+4..1+7.+683.+82+5.17+4+7.1.+48.5.:942 948 252 297 399::");
        // Bivalue Universal Grave + 1 => r6c5<>5, r6c5<>9
        sudoku.setSudoku(":0610::8+4+9325+6+1+77+3+2+8..9+4+5+5+6+1+7..+32+8+49327+85+61+1+5+7.3.8+9+2+2+86...+4+7+3+9+7+8..+213+4+3+149+87+2566+2+5..+3+7+8+9::565 965::");
        // Bivalue Universal Grave + 1 => r1c8<>3, r1c8<>5
        sudoku.setSudoku(":0610::+1+4.+7+8...+9+2+8.+4+5.1.73+7.6+1...+89+5+38+7+1..+2+72+4+9+6+581+3+86+1+324+9+7+5+6+18+2+3759+4+5+971+4+832+64+3+2596+7+8+1::318 518::");
        // Uniqueness Test 1: 1/3 in r5c89,r8c89 => r8c9<>1, r8c9<>3
        sudoku.setSudoku(":0600:13:+5+2+3+9+7+8+1466+1+832+4...+4+7+9+5+6+1+38+29+5+1+6+32.+7.+86+2+49+7+5..+7+34+18526+9+38+7+21....295+8+4....+14+67+5..+2.::189 389::");
        // Uniqueness Test 2: 1/2 in r2c45,r7c45 => r1c46,r2c689,r3c45<>5
        sudoku.setSudoku(":0601:12:....6..2.394...7..2.+6..41....2....4..6.8........6.9....4...739...1.8...2.+2....51.:114 116 819 829 839 849 869 574 575 581 582 495:514 516 526 528 529 534 535::");
        //Uniqueness Test 3: 1/5 in r2c78,r9c78 => r9c1<>3
        sudoku.setSudoku(":0602:15:+7+1+3+4+5+862+9+4927+63..+8+8+6+51+9+2374+67158+9+4+3+2+9+5+42+3+7+8+612+386+4+1+7+9+5.8..+1.+24+71+47.+2.....+2..+74...::391::");
        // Uniqueness Test 3: 1/6 in r4c37,r5c37 => r389c3,r4c12,r5c12<>2, r3c3,r45c2<>9
        sudoku.setSudoku(":0602:16:..3.+1+6.9...78....6.6......1....+8.......45....45..61..7..81.926.9....3........8.1.:514 225 425 226 334 534 934 235 435 735 236 736 244 744 347 447 547 947 248 348 249 349 949 357 857 957 858 859 264 284 784 294 794:233 241 242 251 252 283 293 933 942 952::");
        // Uniqueness Test 3: 5/7 in r3c45,r6c45 => r5c56,r6c1238<>3, r5c456,r6c23<>8
        sudoku.setSudoku(":0602:57:+54.........72+4.5..9.....+463...964.....5...+6........1.281...7........5+7867+5.3.....:213 613 714 715 626 134 834 135 835 151 251 252 663 568 373 195 196:355 356 361 362 363 368 854 855 856 862 863:");
        // Uniqueness Test 4: 4/8 in r4c27,r6c27 => r4c27<>4
        sudoku.setSudoku(":0603:48:+3+9+6+17+52+8481+4+29+63..5+27.+8.+1+6+9..+5+9.2..1+2.38.19..9.+16.+7...+1.+2+7+6.5+98..8.2+9+7137.9.1+8...:448 465 468 492 692 494:442 447::");
        // Avoidable Rectangle Type 2: 1/2 in r3c45,r7c45 => r1c456,r3c236<>7
        sudoku.setSudoku(":0608:12:......+531.96...7+425.....+6+9+8......8..7.94..+1.+3....329..+9..+1+28+3+65+832.5.+41+9+6+51+3+49+287:814 815 141 142:714 715 716 732 733 736::");
        // BUG: INVALID ELIMINATION: Hidden Rectangle: 3/4 in r3c45,r9c45 => r3c5<>4
        sudoku.setSudoku(":0606:34:.51.+8..4+2....2.3...........3..8.+29.....5.4......1.+9+4.....7+5+6+214.6.+29....2........:432 732 832 932 433 633 733 833 933 334 335 635 735 336 881 383 883 392 492 892 992 393 493 893 993 395:435::");
        SudokuSolver solver = SudokuSolverFactory.getDefaultSolverInstance();
        SudokuStepFinder finder = solver.getStepFinder();
        boolean singleHint = false;
        if (singleHint) {
            finder.setSudoku(sudoku);
            SolutionStep step = finder.getStep(SolutionType.BUG_PLUS_1);
            System.out.println(step);
        } else {
            List<SolutionStep> steps = solver.getStepFinder().getAllUniqueness(sudoku);
            solver.getStepFinder().printStatistics();
            if (steps.size() > 0) {
                Collections.sort(steps);
                for (SolutionStep actStep : steps) {
                    System.out.println(actStep);
                }
            }
        }
        System.exit(0);
    }
}
