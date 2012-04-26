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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import sudoku.Candidate;
import sudoku.SolutionStep;
import sudoku.SolutionType;
import sudoku.Sudoku2;
import sudoku.SudokuSinglesQueue;
import sudoku.SudokuUtil;

/**
 *
 * @author hobiwan
 */
public class SimpleSolver extends AbstractSolver {
    /** Flags that indicate, if a search for all Hidden Singles was already successful. */
    private boolean[] singleFound = new boolean[Sudoku2.LENGTH];
    /** The list with all newly found steps. */
    private List<SolutionStep> steps;
    /** One global instance of {@link SolutionStep}. Reduces the number of unnecessarily created objects. */
    private SolutionStep globalStep = new SolutionStep();
    /** Buffer for checking for locked Subsets. */
    private boolean[] sameConstraint = new boolean[Sudoku2.CONSTRAINTS[0].length];
    /** Buffer for checking for locked Subsets. */
    private boolean[] foundConstraint = new boolean[Sudoku2.CONSTRAINTS[0].length];
    /** Buffer for checking for locked Subsets. */
    private int[] constraint = new int[Sudoku2.CONSTRAINTS[0].length];
    /** Buffer for subset check. */
    private int[] indices2 = new int[2];
    /** Buffer for subset check. */
    private int[] indices3 = new int[3];
    /** Buffer for subset check. */
    private int[] indices4 = new int[4];
    /** Cache for steps that were found but cannot be used right now */
    private List<SolutionStep> cachedSteps = new ArrayList<SolutionStep>();
    /** {@link SudokuStepFinder#stepNumber} that was valid when {@link #cachedSteps} was filled. */
    private int cachedStepsNumber = -1;
    /** Temporary array for holding cell indices */
    private int[] tmpArr1 = new int[9];
    /** Bitmaps for indices per candidate in one unit. */
    private short[] ipcMask = new short[10];

    /** Creates a new instance of SimpleSolver
     * @param finder 
     */
    protected SimpleSolver(SudokuStepFinder finder) {
        super(finder);
        steps = new ArrayList<SolutionStep>();
    }

    @Override
    protected SolutionStep getStep(SolutionType type) {
        SolutionStep result = null;
        sudoku = finder.getSudoku();
        switch (type) {
            case FULL_HOUSE: result = findFullHouse(false); break;
            case HIDDEN_SINGLE: result = findHiddenSingle(); break;
            case HIDDEN_PAIR: result = findHiddenXle(2); break;
            case HIDDEN_TRIPLE: result = findHiddenXle(3); break;
            case HIDDEN_QUADRUPLE: result = findHiddenXle(4); break;
            case NAKED_SINGLE: result = findNakedSingle(); break;
            case LOCKED_PAIR: result = findNakedXle(2, true); break;
            case NAKED_PAIR: result = findNakedXle(2, false); break;
            case LOCKED_TRIPLE: result = findNakedXle(3, true); break;
            case NAKED_TRIPLE: result = findNakedXle(3, false); break;
            case NAKED_QUADRUPLE: result = findNakedXle(4, false); break;
            case LOCKED_CANDIDATES:
            case LOCKED_CANDIDATES_1:
            case LOCKED_CANDIDATES_2: result = findLockedCandidates(type); break;
        }
        return result;
    }
    
    @Override
    protected boolean doStep(SolutionStep step) {
        boolean handled = true;
        sudoku = finder.getSudoku();
        switch (step.getType()) {
            case FULL_HOUSE:
            case HIDDEN_SINGLE:
            case NAKED_SINGLE:
                sudoku.setCell(step.getIndices().get(0), step.getValues().get(0));
                break;
            case HIDDEN_PAIR:
            case HIDDEN_TRIPLE:
            case HIDDEN_QUADRUPLE:
            case NAKED_PAIR:
            case NAKED_TRIPLE:
            case NAKED_QUADRUPLE:
            case LOCKED_PAIR:
            case LOCKED_TRIPLE:
            case LOCKED_CANDIDATES:
            case LOCKED_CANDIDATES_1:
            case LOCKED_CANDIDATES_2:
                for (Candidate cand : step.getCandidatesToDelete()) {
                    sudoku.delCandidate(cand.getIndex(), cand.getValue());
//                    SudokuCell cell = sudoku.getCell(cand.index);
//                    cell.delCandidate(candType, cand.value);
                }
                break;
            default:
                handled = false;
        }
        return handled;
    }

    /**
     * Finds and returns all Full Houses present in the grid.
     * @return
     */
    protected List<SolutionStep> findAllFullHouses() {
        sudoku = finder.getSudoku();
        List<SolutionStep> oldList = steps;
        List<SolutionStep> newList = new ArrayList<SolutionStep>();
        steps = newList;
        findFullHouse(true);
        Collections.sort(steps);
        steps = oldList;
        return newList;
    }
    
    /**
     * Full House: Last unset cell in an entity<br>
     * A Full House is always also a Naked Single. The method therefore
     * traverses the Naked Single queue and checks the number of candidates
     * for all other candidates in the constraint.
     * @param all If <code>true</code>, all Full houses are returned
     * @return
     */
    private SolutionStep findFullHouse(boolean all) {
        //SudokuUtil.clearStepList(steps);
        SolutionStep step = null;
        byte[][] free = sudoku.getFree();
        SudokuSinglesQueue nsQueue = sudoku.getNsQueue();
        int queueIndex = nsQueue.getFirstIndex();
        while (queueIndex != -1) {
            int index = nsQueue.getIndex(queueIndex);
            int value = nsQueue.getValue(queueIndex);
            if (sudoku.getValue(index) == 0) {
                // cell is still a valid Naked Single -> check constraints
                // the cell is a member of three constraints
                for (int i = 0; i < Sudoku2.CONSTRAINTS[index].length; i++) {
                    int constr = Sudoku2.CONSTRAINTS[index][i];
                    // check the other candidates in that constraint
                    boolean valid = true;
                    for (int j = 1; j <= 9; j++) {
                        // all candidates except value have to be 0
                        if (j != value && free[constr][j] != 0) {
                            // Naked Single cant be a Full House!
                            valid = false;
                            break;
                        }
                    }
                    if (valid) {
                        // ok, we have a Full House
                        step = new SolutionStep(SolutionType.FULL_HOUSE);
//                        step.setEntity(Sudoku2.CONSTRAINT_TYPE_FROM_CONSTRAINT[constr]);
//                        step.setEntityNumber(Sudoku2.CONSTRAINT_NUMBER_FROM_CONSTRAINT[constr]);
                        step.addValue(value);
                        step.addIndex(index);
                        if (all) {
                            steps.add(step);
                            // could be a Full House in more than one constraint -> record only once
                            break;
                        } else {
                            return step;
                        }
                    }
                }
            }
            queueIndex = nsQueue.getNextIndex();
        }
        return step;
    }
    
    /**
     * Finds the next Naked Single in the grid
     * @return
     */
    private SolutionStep findNakedSingle() {
        SolutionStep step = null;
        SudokuSinglesQueue nsQueue = sudoku.getNsQueue();
        int queueIndex = -1;
        while ((queueIndex = nsQueue.getSingle()) != -1) {
            int index = nsQueue.getIndex(queueIndex);
            int value = nsQueue.getValue(queueIndex);
            if (sudoku.getValue(index) == 0) {
                // cell is still a valid Naked Single
                step = new SolutionStep(SolutionType.NAKED_SINGLE);
                step.addValue(value);
                step.addIndex(index);
                break;
            }
        }
        return step;
    }

    /**
     * Find all Naked Singles but do not alter the queue!
     * @return
     */
    protected List<SolutionStep> findAllNakedSingles() {
        sudoku = finder.getSudoku();
        List<SolutionStep> oldList = steps;
        List<SolutionStep> newList = new ArrayList<SolutionStep>();
        steps = newList;

        // now look for the Naked Singles, but do NOT alter the queue
        SudokuSinglesQueue nsQueue = sudoku.getNsQueue();
        int queueIndex = nsQueue.getFirstIndex();
        while (queueIndex != -1) {
            int index = nsQueue.getIndex(queueIndex);
            int value = nsQueue.getValue(queueIndex);
            if (sudoku.getValue(index) == 0) {
                // cell is a valid Naked Single
                SolutionStep step = new SolutionStep(SolutionType.NAKED_SINGLE);
                step.addValue(value);
                step.addIndex(index);
                steps.add(step);
            }
            queueIndex = nsQueue.getNextIndex();
        }

        Collections.sort(steps);
        steps = oldList;
        return newList;
    }

    /**
     * Find the next Naked Subset of size <code>anz</code>. If <code>lockedOnly</code>
     * is <code>true</code>, only Locked Subsets are found, Naked Subsets are cached.<br>
     * If {@link SudokuStepFinder#stepNumber} has not changed since the last call to
     * this method, {@link #cachedSteps} is searched for a matching step.
     * @param anz
     * @param lockedOnly
     * @return
     */
    private SolutionStep findNakedXle(int anz, boolean lockedOnly) {
        SudokuUtil.clearStepList(steps);

        // check if cached steps can be used
        if (cachedSteps.size() > 0 && cachedStepsNumber == finder.getStepNumber()) {
            // steps were cached and are still valid -> try them
            SolutionType type = SolutionType.NAKED_PAIR;
            if (anz == 2 && lockedOnly) {
                type = SolutionType.LOCKED_PAIR;
            }
            if (anz == 3 && ! lockedOnly) {
                type = SolutionType.NAKED_TRIPLE;
            }
            if (anz == 3 && lockedOnly) {
                type = SolutionType.LOCKED_TRIPLE;
            }
            if (anz == 4) {
                type = SolutionType.NAKED_QUADRUPLE;
            }
            for (SolutionStep step : cachedSteps) {
                if (step.getType() == type) {
                    return step;
                }
            }
        }

        // cache was useless, try to find a step
        cachedSteps.clear();
        cachedStepsNumber = finder.getStepNumber();
        // try blocks first, guarantees to find all Locked Subsets in one method call
        SolutionStep step = findNakedXleInEntity(Sudoku2.BLOCKS, anz, lockedOnly, ! lockedOnly, true);
        if (step != null || lockedOnly) {
            return step;
        }
        step = findNakedXleInEntity(Sudoku2.LINES, anz, lockedOnly, ! lockedOnly, true);
        if (step != null) {
            return step;
        }
        step = findNakedXleInEntity(Sudoku2.COLS, anz, lockedOnly, ! lockedOnly, true);
        return step;
    }

    /**
     * Find all Naked and Locked Subsets.
     * @return
     */
    protected List<SolutionStep> findAllNakedXle() {
        sudoku = finder.getSudoku();
        List<SolutionStep> oldList = steps;
        List<SolutionStep> newList = new ArrayList<SolutionStep>();
        steps = newList;
        // first the Singles (uses queue)
        List<SolutionStep> tmpSteps = findAllNakedSingles();
        steps.addAll(tmpSteps);
        // now everything else
        for (int i = 2; i <= 4; i++) {
            findNakedXleInEntity(Sudoku2.BLOCKS, i, false, false, false);
            findNakedXleInEntity(Sudoku2.LINES, i, false, false, false);
            findNakedXleInEntity(Sudoku2.COLS, i, false, false, false);
        }
        Collections.sort(steps);
        steps = oldList;
        return newList;
    }

    /**
     * Searches for Naked Subsets in the group of units <code>indices</code>. Doesnt use
     * recursion to squeaze a few more microseconds out of the code.<br>
     * If the search is for Locked/Naked Subsets only, steps of the other type
     * are cached but not returned. They can be used as result in a subsequent search
     * if {@link SudokuStepFinder#stepNumber} has not changed.<br>
     * The search uses bitmaps for optimization.
     * @param indices
     * @param anz Must be between 2 and 4.
     * @param lockedOnly
     * @param nakedOnly
     * @param onlyOne
     * @return
     */
    private SolutionStep findNakedXleInEntity(int[][] indices, int anz, 
            boolean lockedOnly, boolean nakedOnly, boolean onlyOne) {
        SolutionStep step = null;
        // check all entities
        for (int entity = 0; entity < indices.length; entity++) {
            // get all cells from indices[entity] that hold no more than anz candidates
            // (since set cells have no candidates they are found automatically)
            int maxIndex = 0;
            for (int i = 0; i < indices[entity].length; i++) {
                int tmpAnz = Sudoku2.ANZ_VALUES[sudoku.getCell(indices[entity][i])];
                if (tmpAnz != 0 && tmpAnz <= anz) {
                    tmpArr1[maxIndex++] = indices[entity][i];
                }
            }
            if (maxIndex < anz) {
                // not enough cells with the correct number of candidates -> no Naked Subset possible
                continue;
            }
            // first level: take all cells with the correct number of indices
            for (int i1 = 0; i1 < maxIndex - anz + 1; i1++) {
                short cell1 = sudoku.getCell(tmpArr1[i1]);
                // second level: always necessary.
                for (int i2 = i1 + 1; i2 < maxIndex - anz + 2; i2++) {
                    short cell2 = (short)(cell1 | sudoku.getCell(tmpArr1[i2]));
                    // take cell only if the combination of candidates
                    // with the current result doesnt exceed anz
                    if (Sudoku2.ANZ_VALUES[cell2] > anz) {
                        continue;
                    }
                    if (anz == 2) {
                        if (Sudoku2.ANZ_VALUES[cell2] == anz) {
                            // step found!
                            step = createSubsetStep(tmpArr1[i1], tmpArr1[i2],
                                    -1, -1, cell2,
                                    SolutionType.NAKED_PAIR, lockedOnly, nakedOnly);
                            if (step != null && onlyOne) {
                                return step;
                            }
                        }
                    } else {
                        // third level
                        for (int i3 = i2 + 1; i3 < maxIndex - anz + 3; i3++) {
                            short cell3 = (short)(cell2 | sudoku.getCell(tmpArr1[i3]));
                            // take cell only if the combination of candidates
                            // with the current result doesnt exceed anz
                            if (Sudoku2.ANZ_VALUES[cell3] > anz) {
                                continue;
                            }
                            if (anz == 3) {
                                if (Sudoku2.ANZ_VALUES[cell3] == anz) {
                                    // step found!
                                    step = createSubsetStep(tmpArr1[i1], tmpArr1[i2],
                                            tmpArr1[i3], -1, cell3,
                                            SolutionType.NAKED_TRIPLE, lockedOnly, nakedOnly);
                                    if (step != null && onlyOne) {
                                        return step;
                                    }
                                }
                            } else {
                                // fourth level
                                for (int i4 = i3 + 1; i4 < maxIndex; i4++) {
                                    short cell4 = (short)(cell3 | sudoku.getCell(tmpArr1[i4]));
                                    // take cell only if the combination of candidates
                                    // with the current result doesnt exceed anz
                                    if (Sudoku2.ANZ_VALUES[cell4] > anz) {
                                        continue;
                                    }
                                    if (Sudoku2.ANZ_VALUES[cell4] == anz) {
                                        // step found!
                                        step = createSubsetStep(tmpArr1[i1], tmpArr1[i2],
                                                tmpArr1[i3], tmpArr1[i4], cell4,
                                                SolutionType.NAKED_QUADRUPLE, lockedOnly, nakedOnly);
                                        if (step != null && onlyOne) {
                                            return step;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Finds the next Hidden Single in the grid
     * @return
     */
    private SolutionStep findHiddenSingle() {
        SolutionStep step = null;
        byte[][] free = sudoku.getFree();
        SudokuSinglesQueue hsQueue = sudoku.getHsQueue();
        int queueIndex = -1;
        while ((queueIndex = hsQueue.getSingle()) != -1) {
            int index = hsQueue.getIndex(queueIndex);
            int value = hsQueue.getValue(queueIndex);
            if (sudoku.getValue(index) == 0) {
                // cell is still a valid Hidden Single; which constraint?
                for (int i = 0; i < Sudoku2.CONSTRAINTS[index].length; i++) {
                    if (free[Sudoku2.CONSTRAINTS[index][i]][value] == 1) {
                        step = new SolutionStep(SolutionType.HIDDEN_SINGLE);
                        step.addValue(value);
                        step.addIndex(index);
                        break;
                    }
                }
                break;
            }
        }
        return step;
    }

    /**
     * Iterates the Hidden Single queue without altering it. For every single
     * in the queue that is still valid, a hidden Single item is created.
     * {@link #singleFound} is used to ensure that only one single is
     * recorded per cell.
     * @return
     */
    protected List<SolutionStep> findAllHiddenSingles() {
        sudoku = finder.getSudoku();
        List<SolutionStep> oldList = steps;
        List<SolutionStep> newList = new ArrayList<SolutionStep>();
        steps = newList;
        Arrays.fill(singleFound, false);

        byte[][] free = sudoku.getFree();
        SudokuSinglesQueue hsQueue = sudoku.getHsQueue();
        int queueIndex = hsQueue.getFirstIndex();
        while (queueIndex != -1) {
            int index = hsQueue.getIndex(queueIndex);
            int value = hsQueue.getValue(queueIndex);
            if (sudoku.getValue(index) == 0 && ! singleFound[index]) {
                // cell is still a valid Hidden Single and was not found already; which constraint?
                for (int i = 0; i < Sudoku2.CONSTRAINTS[index].length; i++) {
                    if (free[Sudoku2.CONSTRAINTS[index][i]][value] == 1) {
                        SolutionStep step = new SolutionStep(SolutionType.HIDDEN_SINGLE);
                        step.addValue(value);
                        step.addIndex(index);
                        step.setEntity(i);
                        steps.add(step);
                        singleFound[index] = true;
                        break;
                    }
                }
            }
            queueIndex = hsQueue.getNextIndex();
        }

        Collections.sort(steps);
        steps = oldList;
        return newList;
    }

    /**
     * Find all Hidden Subsets in the grid.
     * @return
     */
    protected List<SolutionStep> findAllHiddenXle() {
        sudoku = finder.getSudoku();
        List<SolutionStep> oldList = steps;
        List<SolutionStep> newList = new ArrayList<SolutionStep>();
        steps = newList;
        List<SolutionStep> tmpSteps = findAllHiddenSingles();
        steps.addAll(tmpSteps);
        for (int i = 2; i <= 4; i++) {
            findHiddenXleInEntity(2 * Sudoku2.UNITS, Sudoku2.BLOCKS, i, false);
            findHiddenXleInEntity(0, Sudoku2.LINES, i, false);
            findHiddenXleInEntity(Sudoku2.UNITS, Sudoku2.COLS, i, false);
        }
        Collections.sort(steps);
        steps = oldList;
        return newList;
    }
    
    /**
     * Find the next Hidden Subset with size <code>anz</code>
     * @param anz
     * @return
     */
    private SolutionStep findHiddenXle(int anz) {
        SudokuUtil.clearStepList(steps);
        SolutionStep step = findHiddenXleInEntity(2 * Sudoku2.UNITS, Sudoku2.BLOCKS, anz, true);
        if (step != null) {
            return step;
        }
        step = findHiddenXleInEntity(0, Sudoku2.LINES, anz, true);
        if (step != null) {
            return step;
        }
        step = findHiddenXleInEntity(Sudoku2.UNITS, Sudoku2.COLS, anz, true);
        return step;
    }
    
    /**
     * Searches for Naked Subsets in the group of units <code>indices</code>. Doesnt use
     * recursion to squeaze a few more microseconds out of the code.
     * @param constraintBase
     * @param indices
     * @param anz
     * @param onlyOne
     * @return
     */
    private SolutionStep findHiddenXleInEntity(int constraintBase, int[][] indices, int anz, boolean onlyOne) {
        SolutionStep step = null;
        // check all entities
        for (int entity = 0; entity < indices.length; entity++) {
            // get the number of cells from indices[entity] that are not yet set
            int maxIndex = 0;
            for (int i = 0; i < indices[entity].length; i++) {
                if (sudoku.getCell(indices[entity][i]) != 0) {
                    maxIndex++;
                }
            }
            if (maxIndex <= anz) {
                // not enough cells with the correct number of candidates -> no Hidden Subset possible
                // (for a HS there must be more than anz cells!)
                continue;
            }
            // now check, if we have enough eligible candidates in this constraint, and collect them
            // build bitmaps per candidate too (bits indicate cells in unit, not candidates)
            short candMask = 0;
            byte[][] free = sudoku.getFree();
            for (int i = 1; i <= 9; i++) {
                int actFree = free[constraintBase + entity][i];
                if (actFree != 0 && actFree <= anz) {
                    // ok, candidate could be part of a Hidden Subset of size anz
                    candMask |= Sudoku2.MASKS[i];
                    ipcMask[i] = 0;
                    for (int j = 0; j < Sudoku2.UNITS; j++) {
                        if ((sudoku.getCell(indices[entity][j]) & Sudoku2.MASKS[i]) != 0) {
                            ipcMask[i] |= Sudoku2.MASKS[j + 1];
                        }
                    }
                }
            }
            if (Sudoku2.ANZ_VALUES[candMask] < anz) {
                // not enough candidates
                continue;
            }
            // now check all possible combinations of candidates
            int[] candArr = Sudoku2.POSSIBLE_VALUES[candMask];
            // first level: take all candidates
            for (int i1 = 0; i1 < candArr.length - anz + 1; i1++) {
                short cand1 = Sudoku2.MASKS[candArr[i1]];
                short cell1 = ipcMask[candArr[i1]];
                // second level: always necessary.
                for (int i2 = i1 + 1; i2 < candArr.length - anz + 2; i2++) {
                    short cand2 = (short)(cand1 | Sudoku2.MASKS[candArr[i2]]);
                    short cell2 = (short)(cell1 | ipcMask[candArr[i2]]);
                    if (anz == 2) {
                        if (Sudoku2.ANZ_VALUES[cell2] == anz) {
                            // step found!
                            int[] tmp = Sudoku2.POSSIBLE_VALUES[cell2];
                            step = createSubsetStep(indices[entity][tmp[0] - 1], indices[entity][tmp[1] - 1],
                                    -1, -1, cand2,
                                    SolutionType.HIDDEN_PAIR, onlyOne, onlyOne);
                            if (step != null && onlyOne) {
                                return step;
                            }
                        }
                    } else {
                        // third level
                        for (int i3 = i2 + 1; i3 < candArr.length - anz + 3; i3++) {
                            short cand3 = (short)(cand2 | Sudoku2.MASKS[candArr[i3]]);
                            short cell3 = (short)(cell2 | ipcMask[candArr[i3]]);
                            if (anz == 3) {
                                if (Sudoku2.ANZ_VALUES[cell3] == anz) {
                                    // step found!
                                    int[] tmp = Sudoku2.POSSIBLE_VALUES[cell3];
                                    step = createSubsetStep(indices[entity][tmp[0] - 1], indices[entity][tmp[1] - 1],
                                            indices[entity][tmp[2] - 1], -1, cand3,
                                            SolutionType.HIDDEN_TRIPLE, onlyOne, onlyOne);
                                    if (step != null && onlyOne) {
                                        return step;
                                    }
                                }
                            } else {
                                // fourth level
                                for (int i4 = i3 + 1; i4 < candArr.length; i4++) {
                                    short cand4 = (short)(cand3 | Sudoku2.MASKS[candArr[i4]]);
                                    short cell4 = (short)(cell3 | ipcMask[candArr[i4]]);
                                    if (Sudoku2.ANZ_VALUES[cell4] == anz) {
                                        // step found!
                                        int[] tmp = Sudoku2.POSSIBLE_VALUES[cell4];
                                        step = createSubsetStep(indices[entity][tmp[0] - 1], indices[entity][tmp[1] - 1],
                                                indices[entity][tmp[2] - 1], indices[entity][tmp[3] - 1], cand4,
                                                SolutionType.HIDDEN_QUADRUPLE, onlyOne, onlyOne);
                                        if (step != null && onlyOne) {
                                            return step;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the next LC step. Type 1 is found first.

     * @param type
     * @return
     */
    private SolutionStep findLockedCandidates(SolutionType type) {
        SudokuUtil.clearStepList(steps);
        SolutionStep step = null;
        if (type == SolutionType.LOCKED_CANDIDATES || type == SolutionType.LOCKED_CANDIDATES_1) {
            step = findLockedCandidatesInEntityN(18, Sudoku2.BLOCKS, true);
            if (step != null) {
                return step;
            }
        }
        if (type == SolutionType.LOCKED_CANDIDATES || type == SolutionType.LOCKED_CANDIDATES_2) {
            step = findLockedCandidatesInEntityN(0, Sudoku2.LINES, true);
            if (step != null) {
                return step;
            }
            step = findLockedCandidatesInEntityN(9, Sudoku2.COLS, true);
            if (step != null) {
                return step;
            }
        }
        return null;
    }

    /**
     * Get all LC steps.
     * @return
     */
    protected List<SolutionStep> findAllLockedCandidates() {
        sudoku = finder.getSudoku();
        List<SolutionStep> oldList = steps;
        List<SolutionStep> newList = new ArrayList<SolutionStep>();
        steps = newList;
        findLockedCandidatesInEntityN(18, Sudoku2.BLOCKS, false);
        findLockedCandidatesInEntityN(0, Sudoku2.LINES, false);
        findLockedCandidatesInEntityN(9, Sudoku2.COLS, false);
        Collections.sort(steps);
        steps = oldList;
        return newList;
    }
    
    /**
     * Find one or more Locked Candidates steps in the unit given by <code>indices</code>.
     * A LC move is present, if a candidate occurs exactely 2 or 3 times in a unit, all
     * instances share another unit and in that other unit additional candidates are present.<br>
     * If <code>constraintBase == 18</code> (search in blocks) a LC1 is possible, otherwise
     * it could be a LC2.
     * @param constraintBase
     * @param indices
     * @param onlyOne
     * @return
     */
    private SolutionStep findLockedCandidatesInEntityN(int constraintBase, int[][] indices, boolean onlyOne) {
        // look through all constraints in the set
        SolutionStep step = null;
        byte[][] free = sudoku.getFree();
        for (int constr = 0; constr < Sudoku2.UNITS; constr++) {
            // for every constraint check every candidate
            for (int cand = 1; cand <= 9; cand++) {
                int unitFree = free[constr + constraintBase][cand];
                if (unitFree == 2 || unitFree == 3) {
                    // possible Locked Candidates: check the cells
                    boolean first = true;
                    sameConstraint[0] = sameConstraint[1] = sameConstraint[2] = true;
                    for (int i = 0; i < indices[constr].length; i++) {
                        int index = indices[constr][i];
                        short cell = sudoku.getCell(index);
                        if ((cell & Sudoku2.MASKS[cand]) == 0) {
                            // candidate not present -> skip the cell
                            continue;
                        }
                        if (first) {
                            constraint[0] = Sudoku2.CONSTRAINTS[index][0];
                            constraint[1] = Sudoku2.CONSTRAINTS[index][1];
                            constraint[2] = Sudoku2.CONSTRAINTS[index][2];
                            first = false;
                        } else {
                            for (int j = 0; j < Sudoku2.CONSTRAINTS[0].length; j++) {
                                if (sameConstraint[j] && constraint[j] != Sudoku2.CONSTRAINTS[index][j]) {
                                    sameConstraint[j] = false;
                                }
                            }
                        }
                    }
                    // check it: distinguish between LC Type 1 and Type 2
                    int skipConstraint = constraintBase + constr;
                    int aktConstraint = -1;
                    if (constraintBase == 18) {
                        // we search blocks -> LC1 possible
                        if (sameConstraint[0] && free[constraint[0]][cand] > unitFree) {
                            aktConstraint = constraint[0];
                        } else if (sameConstraint[1] && free[constraint[1]][cand] > unitFree) {
                            aktConstraint = constraint[1];
                        } else {
                            // no LC1 possible
                            continue;
                        }
                        // LC Type 1 -> eliminations in line or col
                        step = createLockedCandidatesStep(SolutionType.LOCKED_CANDIDATES_1, cand,
                                skipConstraint, Sudoku2.ALL_UNITS[aktConstraint]);
                        if (onlyOne) {
                            return step;
                        } else {
                            steps.add(step);
                        }
                    } else {
                        // we search lines or cols -> LC2 possible
                        if (sameConstraint[2] && free[constraint[2]][cand] > unitFree) {
                            // LC Type 2 -> eliminations in block only
                            step = createLockedCandidatesStep(SolutionType.LOCKED_CANDIDATES_2, cand,
                                    skipConstraint, Sudoku2.ALL_UNITS[constraint[2]]);
                            if (onlyOne) {
                                return step;
                            } else {
                                steps.add(step);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Creates a new Locked Candidates step for candidate <code>cand</code>. All
     * candidates to delete are in the unit given by <code>indices</code>, all cells
     * that belong to constraint <code>skipConstraint</code> are part of the LC itself
     * and cannot be deleted.
     * @param type
     * @param cand
     * @param skipConstraint
     * @param indices
     * @return
     */
    private SolutionStep createLockedCandidatesStep(SolutionType type, int cand, int skipConstraint, int[] indices) {
        globalStep.reset();
        globalStep.setType(type);
        globalStep.addValue(cand);
        globalStep.setEntity(Sudoku2.CONSTRAINT_TYPE_FROM_CONSTRAINT[skipConstraint]);
        globalStep.setEntityNumber(Sudoku2.CONSTRAINT_NUMBER_FROM_CONSTRAINT[skipConstraint]);
        // check all cells in the unit
        for (int i = 0; i < indices.length; i++) {
            int index = indices[i];
            if ((sudoku.getCell(index) & Sudoku2.MASKS[cand]) != 0) {
                // cell holds the candidate
                if (Sudoku2.CONSTRAINTS[index][0] == skipConstraint ||
                        Sudoku2.CONSTRAINTS[index][1] == skipConstraint ||
                        Sudoku2.CONSTRAINTS[index][2] == skipConstraint) {
                    // cell belongs to the LC
                    globalStep.addIndex(index);
                } else {
                    // candidate can be deleted
                    globalStep.addCandidateToDelete(index, cand);
                }
            }
        }
        return (SolutionStep) globalStep.clone();
    }

    /**
     * Convenience method, delegates to {@link #createSubsetStep(int[], short, sudoku.SolutionType, boolean, boolean)}.
     * The valid indices are stored in an array with the correct size to make iterating them easier.
     * @param i1
     * @param i2
     * @param i3
     * @param i4
     * @param cands
     * @param type
     * @param lockedOnly
     * @param nakedOnly
     * @return
     */
    private SolutionStep createSubsetStep(int i1, int i2, int i3, int i4, short cands, SolutionType type, boolean lockedOnly, boolean nakedOnly) {
        if (i4 >= 0) {
            // Quadruple search
            indices4[0] = i1;
            indices4[1] = i2;
            indices4[2] = i3;
            indices4[3] = i4;
            return createSubsetStep(indices4, cands, type, lockedOnly, nakedOnly);
        } else if (i3 >= 0) {
            // Triple search
            indices3[0] = i1;
            indices3[1] = i2;
            indices3[2] = i3;
            return createSubsetStep(indices3, cands, type, lockedOnly, nakedOnly);
        } else {
            // Pair search
            indices2[0] = i1;
            indices2[1] = i2;
            return createSubsetStep(indices2, cands, type, lockedOnly, nakedOnly);
        }
    }

    /**
     * Check the subset in <code>indices</code> if it deletes any candidates. If
     * it does so, create the step and store it.<br>
     * The following steps have to be taken:
     * <ul>
     *  <li>Determine the common constraints of all cells in <code>indices</code> (used for Locked Set detection amongst others)</li>
     *  <li>Check the constraints for candidates that can be deleted</li>
     *  <li>If <code>lockedOnly</code> or <code>nakedHiddenOnly</code> is set, the search runs for
     *     the first step only. If the step doesnt match (Locked instead of Naked or vice versa) it is cached.
     *     If it does match, return it.</li>
     *  <li>If the search runs for all steps (no flag set), it is added tp {@link #steps}.</li>
     * </ul>
     * The method handles Naked and Hidden subsets (determined by <code>type</code>). For
     * Naked subsets all candidates in <code>cands</code> can be deleted from all cells that
     * see all cells in <code>indices</code>, for Hidden subsets all candidates except
     * <code>cands</code> can be deleted from the cells in <code>indices</code>.<br>
     * Templates are not used to avoid unnecessary template initializations.
     * @param indices
     * @param cands
     * @param type
     * @param lockedOnly
     * @param nakedHiddenOnly
     * @return
     */
    private SolutionStep createSubsetStep(int[] indices, short cands,
            SolutionType type, boolean lockedOnly, boolean nakedHiddenOnly) {
//        System.out.println("create SubsetStep: " + type + " (" + Arrays.toString(indices) + ")");
        globalStep.reset();
        globalStep.setType(type);

        // determine the constraints to which the cells belong
        sameConstraint[0] = sameConstraint[1] = sameConstraint[2] = true;
        constraint[0] = Sudoku2.CONSTRAINTS[indices[0]][0];
        constraint[1] = Sudoku2.CONSTRAINTS[indices[0]][1];
        constraint[2] = Sudoku2.CONSTRAINTS[indices[0]][2];
        for (int i = 1; i < indices.length; i++) {
            for (int j = 0; j < Sudoku2.CONSTRAINTS[0].length; j++) {
                if (sameConstraint[j] && constraint[j] != Sudoku2.CONSTRAINTS[indices[i]][j]) {
                    sameConstraint[j] = false;
                }
            }
        }
//        System.out.println("sameConstraint: " + Arrays.toString(sameConstraint));
//        System.out.println("constraint: " + Arrays.toString(constraint));
        int anzFoundConstraints = 0; // number of constraints, in which candidates will be deleted, outside the block

        // get candidates that can be deleted
        if (type.isHiddenSubset()) {
            // in the cells indices all candidates except cands can be deleted
            for (int i = 0; i < indices.length; i++) {
                short candsToDelete = (short)(sudoku.getCell(indices[i]) & ~cands);
                if (candsToDelete == 0) {
                    // nothing to delete
                    continue;
                }
                int[] candArray = Sudoku2.POSSIBLE_VALUES[candsToDelete];
                for (int k = 0; k < candArray.length; k++) {
                    globalStep.addCandidateToDelete(indices[i], candArray[k]);
                }
            }
        } else {
            // candidates have to be deleted in all other cells of all common constraints
            foundConstraint[0] = foundConstraint[1] = foundConstraint[2] = false;
            for (int i = 0; i < sameConstraint.length; i++) {
                if (! sameConstraint[i]) {
                    continue;
                }
                int[] cells = Sudoku2.ALL_UNITS[constraint[i]];
//                System.out.println("search cells " + Arrays.toString(cells));
                for (int j = 0; j < cells.length; j++) {
                    boolean skip = false;
                    for (int k = 0; k < indices.length; k++) {
                        if (cells[j] == indices[k]) {
                            skip = true;
                            break;
                        }
                    }
                    if (skip) {
                        // skip the cells themselves
                        continue;
                    }
//                    System.out.print("   cell: " + cells[j]);
                    short candsToDelete = (short)(sudoku.getCell(cells[j]) & cands);
//                    System.out.print("  candsToDelete: " + candsToDelete);
                    if (candsToDelete == 0) {
                        // nothing to do
//                        System.out.println();
                        continue;
                    }
                    int[] candArray = Sudoku2.POSSIBLE_VALUES[candsToDelete];
//                    System.out.println("  candArray: " + Arrays.toString(candArray));
                    for (int k = 0; k < candArray.length; k++) {
                        globalStep.addCandidateToDelete(cells[j], candArray[k]);
                        if (! foundConstraint[i] && ((i == 2) ||
                           (Sudoku2.CONSTRAINTS[cells[j]][2] != constraint[2]))) {
                            // we are in a line or a col and the cell is not in the block -> Locked Pair!
                            //if we are in the block -> count anyway
                            foundConstraint[i] = true;
                            anzFoundConstraints++;
                        }
                    }
                }
            }
        }
        // valid step?
        if (globalStep.getAnzCandidatesToDelete() == 0) {
            // nothing to do!
            return null;
        }

        // check for Locked Subsets
        boolean isLocked = false;
        if (indices.length < 4 && anzFoundConstraints > 1 && ! type.isHiddenSubset() &&
                (sameConstraint[2] && sameConstraint[0] || sameConstraint[2] && sameConstraint[1])) {
            isLocked = true;
        }

        // ok: assemble the step
        if (isLocked) {
            if (type == SolutionType.NAKED_PAIR) {
                globalStep.setType(SolutionType.LOCKED_PAIR);
            }
            if (type == SolutionType.NAKED_TRIPLE) {
                globalStep.setType(SolutionType.LOCKED_TRIPLE);
            }
        }
        for (int i = 0; i < indices.length; i++) {
            globalStep.addIndex(indices[i]);
        }
        int[] candArray = Sudoku2.POSSIBLE_VALUES[cands];
        for (int i = 0; i < candArray.length; i++) {
            globalStep.addValue(candArray[i]);
        }

        // what should we do with the step?
        SolutionStep step = (SolutionStep) globalStep.clone();
        if (lockedOnly && ! nakedHiddenOnly) {
            // search for Locked Subsets only
            if (! isLocked) {
                cachedSteps.add(step);
                step = null;
            }
        } else if (nakedHiddenOnly && ! lockedOnly) {
            if (isLocked) {
                cachedSteps.add(step);
                step = null;
            }
        } else  if (! lockedOnly && ! nakedHiddenOnly) {
            // search for ALL steps
            steps.add(step);
        }
        return step;
    }

    public static void main(String[] args) {
        Sudoku2 sudoku = new Sudoku2();
//        sudoku.setSudoku(":0110:28:.......+7+3.......464.6....95.2.+7.5+9.1...91.73...73..+5..7.543+9+6......7.3.9..91..+4.+7::214 215 216 224 225 226 236 237 814 815 816 824 825 826 832 836 837::");
//        sudoku.setSudoku(":0110:47:6.......8..2....7.94.........15.98..79....5.2.....4..916...5.2......2..5..3961...::477 479 487 488 491 498 777 779 787 792::");
//        sudoku.setSudoku(":0110:68:9862.......4...8...5......2.6..37.....51..29.1..4...3......3.2.57....6........15.::659 665 666 844 851 859 865 866::");
//        sudoku.setSudoku(":0110:48:97.5...6......71.38...4........8.9..685......1....3..2............6...57..8.9.2..::413 428 816 828::");
//        sudoku.setSudoku(":0110:67:8..21..+47.4..891......4........98..5.9..3......7..14.9...........3...2542.......8::674 675 676 681 682 684 694 695 696 774 775 776 781 782 784 794 795 796::");
//        sudoku.setSudoku(":0110:59:12..386.......7.8............7.........3.14..+34+1..+62956....432...........8..23.46::513 523 533 553 572 581 582 583 591 913 923 933 953 972 981 982 983 991::");
//        sudoku.setSudoku(":0110:56:+7....62....27.3..93.........49.+3..1..+37...9.856....3.2.1397.4...7.31.....+2.......::519 539 549 578 587 588 597 598 599 639 649 678 687 688 697 698 699::");
//        sudoku.setSudoku(":0110:27:+93..4.+6+58+6.59.+8+3..2..536..9.6.........+3+685.........763.19....363..4...91.........::216 246 266 274 275 285 294 295 296 716 746 774 775 785 794 795 796::");
//        sudoku.setSudoku(":0110:29:+6+8.+1+4+7.+35.7.65.....1+5.+8276.+1+26+59+43+788+947....15378+2+16...63.......+4+8.......+5+14.8...::277 278 279 287 288 289 291 299 977 978 979 987 988 989 991 999::");
//        sudoku.setSudoku(":0110:89:97.+8.62+4+3+6+82..3.1543.....6..46.......+1...2....+2.3...7415+3.8.4.+6.+9+46...3.2+6..+34.5.::847 848 849 851 853 857 867 947 948 949 953 954 955 957 967::");
        sudoku.setSudoku(":0110:38:.1.57.4..7+521+4.6..........5.+2...1........+7.2..7562+839.2.+7......569214....4.7.....::319 329 338 378 388 398 819 829 837 838 848 878 888 898::");
        // sollte kein Locked Pair sein!
//        sudoku.setSudoku(":0110:27:...5.+98.43+5+6+8+7+421+9.9...+2+7..9.7..6....1.+9+8+53+7.+5...27+9..1.+9+6+5+3+4.7..+3248.9...5+79+1+6..::213::");
//        sudoku.setSudoku("");
        SudokuSolver solver = SudokuSolverFactory.getDefaultSolverInstance();
        SolutionStep step = solver.getHint(sudoku, false);
        System.out.println(step);
        System.exit(0);
    }
}
