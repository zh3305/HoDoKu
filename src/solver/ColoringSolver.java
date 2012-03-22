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
import sudoku.Candidate;
import sudoku.SolutionStep;
import sudoku.SolutionType;
import sudoku.Sudoku2;
import sudoku.SudokuSet;

/**
 *
 * @author hobiwan
 */
public class ColoringSolver extends AbstractSolver {
    /** First color of a coloring pair. Index in {@link #sets} */
    private static final int C1 = 0;
    /** Second color of a coloring pair. Index in {@link #sets} */
    private static final int C2 = 1;
    /** Maximum number of color pairs. */
    private static final int MAX_COLOR = 20;

    /** Sets containing the indices of colored cells for all candidates. sets[x]
     * contains the color pairs for candidate x, sets[x][y] contains one color pair for candidate x. */
    private SudokuSet[][][] sets = new SudokuSet[10][MAX_COLOR][2];
    /** Number of color pairs for each candidate. */
    private int[] anzColorPairs = new int[sets.length];
    /** Step number of the sudoku for which coloring was calculated. -1 means "data invalid". */
    private int[] stepNumbers = new int[sets.length];
    /** contains all candidates, that are part of at least one conjugate pair. */
    private SudokuSet startSet = new SudokuSet();
    /** Set for temporary calculations. */
    private SudokuSet tmpSet1 = new SudokuSet();
    /** Contains cells where a candidate can be eliminated. */
    private SudokuSet deleteSet = new SudokuSet();
    /** All steps found. */
    private List<SolutionStep> steps = new ArrayList<SolutionStep>();
    /** One global step for optimization. */
    private SolutionStep globalStep = new SolutionStep();

    public ColoringSolver(SudokuStepFinder finder) {
        super(finder);

        // create coloring sets
        for (int i = 0; i < sets.length; i++) {
            for (int j = 0; j < sets[i].length; j++) {
                for (int k = 0; k < sets[i][j].length; k++) {
                    sets[i][j][k] = new SudokuSet();
                }
            }
            anzColorPairs[i] = 0;
            stepNumbers[i] = -1;
        }
    }

    @Override
    protected SolutionStep getStep(SolutionType type) {
        SolutionStep result = null;
        sudoku = finder.getSudoku();
        switch (type) {
            case SIMPLE_COLORS:
            case SIMPLE_COLORS_TRAP:
            case SIMPLE_COLORS_WRAP:
                result = findSimpleColorStep(true);
                break;
            case MULTI_COLORS:
            case MULTI_COLORS_1:
            case MULTI_COLORS_2:
                result = findMultiColorStep(true);
                break;
        }
        return result;
    }

    @Override
    protected boolean doStep(SolutionStep step) {
        boolean handled = true;
        sudoku = finder.getSudoku();
        switch (step.getType()) {
            case SIMPLE_COLORS:
            case SIMPLE_COLORS_TRAP:
            case SIMPLE_COLORS_WRAP:
            case MULTI_COLORS:
            case MULTI_COLORS_1:
            case MULTI_COLORS_2:
                for (Candidate cand : step.getCandidatesToDelete()) {
                    sudoku.delCandidate(cand.getIndex(), cand.getValue());
                }
                break;
            default:
                handled = false;
        }
        return handled;
    }

    protected List<SolutionStep> findAllSimpleColors() {
        sudoku = finder.getSudoku();
        List<SolutionStep> oldList = steps;
        List<SolutionStep> newList = new ArrayList<SolutionStep>();
        steps = newList;
//        long ticks = System.currentTimeMillis();
        findSimpleColorSteps(false);
//        ticks = System.currentTimeMillis() - ticks;
//        Logger.getLogger(getClass().getName()).log(Level.FINE, "end of findAllSimpleColors() (" + ticks + "ms)");
        Collections.sort(steps);
        steps = oldList;
        return newList;
    }

    protected List<SolutionStep> findAllMultiColors() {
        sudoku = finder.getSudoku();
        List<SolutionStep> oldList = steps;
        List<SolutionStep> newList = new ArrayList<SolutionStep>();
        steps = newList;
//        long ticks = System.currentTimeMillis();
        findMultiColorSteps(false);
//        ticks = System.currentTimeMillis() - ticks;
//        Logger.getLogger(getClass().getName()).log(Level.FINE, "end of findAllMultiColors() (" + ticks + "ms)");
        Collections.sort(steps);
        steps = oldList;
        return newList;
    }

    /**
     * Finds the next SC step.
     * @param onlyOne
     * @return
     */
    private SolutionStep findSimpleColorStep(boolean onlyOne) {
        steps.clear();
        SolutionStep step = findSimpleColorSteps(onlyOne);
        if (onlyOne && step != null) {
            return step;
        } else if (steps.size() > 0) {
            Collections.sort(steps);
            return steps.get(0);
        }
        return null;
    }

    /**
     * Iterates over all candidates and finds SC steps.
     * @param onlyOne
     * @return
     */
    private SolutionStep findSimpleColorSteps(boolean onlyOne) {
        for (int i = 1; i <= 9; i++) {
            SolutionStep step = findSimpleColorStepsForCandidate(i, onlyOne);
            if (onlyOne && step != null) {
                return step;
            }
        }
        return null;
    }

    /**
     * Tries to find all Simple Colors steps for the given candidate.
     * 
     * @param cand candidate to check
     * @param onlyOne
     * @return
     */
    private SolutionStep findSimpleColorStepsForCandidate(int cand, boolean onlyOne) {
        int anzColors = doColoring(cand);
        // now check for eliminations
        for (int i = 0; i < anzColors; i++) {
            SudokuSet set1 = sets[cand][i][C1];
            SudokuSet set2 = sets[cand][i][C2];

            // first: color wrap - if two cells with the same color can see each other,
            // all candidates with that color can be removed
            globalStep.reset();
            if (checkColorWrap(set1)) {
                for (int j = 0; j < set1.size(); j++) {
                    globalStep.addCandidateToDelete(set1.get(j), cand);
                    //System.out.println("add: " + onSet.get(j) + "/" + cand);
                }
            }
            if (checkColorWrap(set2)) {
                for (int j = 0; j < set2.size(); j++) {
                    globalStep.addCandidateToDelete(set2.get(j), cand);
                }
            }
            if (!globalStep.getCandidatesToDelete().isEmpty()) {
                globalStep.setType(SolutionType.SIMPLE_COLORS_WRAP);
                globalStep.addValue(cand);
                globalStep.addColorCandidates(set1, 0);
                globalStep.addColorCandidates(set2, 1);
                //System.out.println("onSet: " + onSet);
                //System.out.println("offSet: " + offSet);
                SolutionStep step = (SolutionStep) globalStep.clone();
                if (onlyOne) {
                    return step;
                } else {
                    steps.add(step);
                }
            }

            // second: color trap - any candidate, that can see two cells with
            // opposite colors, can be removed
            globalStep.reset();
            checkCandidateToDelete(set1, set2, cand);
            if (!globalStep.getCandidatesToDelete().isEmpty()) {
                globalStep.setType(SolutionType.SIMPLE_COLORS_TRAP);
                globalStep.addValue(cand);
                globalStep.addColorCandidates(set1, 0);
                globalStep.addColorCandidates(set2, 1);
                SolutionStep step = (SolutionStep) globalStep.clone();
                if (onlyOne) {
                    return step;
                } else {
                    steps.add(step);
                }
            }
        }
        return null;
    }

    /**
     * Checks, if two cells in set can see each other. If so, all candidates
     * in set can be removed.
     * @param set The set to check
     */
    private boolean checkColorWrap(SudokuSet set) {
        for (int i = 0; i < set.size() - 1; i++) {
            for (int j = i + 1; j < set.size(); j++) {
                if (Sudoku2.buddies[set.get(i)].contains(set.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Utility function: All candidates, that can see a cell in set1 and a cell in
     * set2, can be eliminated.
     * 20090414: Eliminations are recorded more than once, that leads to wrong sorting;
     *           Collect all eliminations in a set first
     * @param set1 The first set to check
     * @param set2 The second set to check
     * @param cand Candidate to check
     */
    private void checkCandidateToDelete(SudokuSet set1, SudokuSet set2, int cand) {
        deleteSet.clear();
        for (int i = 0; i < set1.size(); i++) {
            for (int j = 0; j < set2.size(); j++) {
                tmpSet1.set(Sudoku2.buddies[set1.get(i)]);
                tmpSet1.and(Sudoku2.buddies[set2.get(j)]);
                tmpSet1.and(finder.getCandidates()[cand]);
                deleteSet.or(tmpSet1);
//                if (!tmpSet1.isEmpty()) {
//                    for (int k = 0; k < tmpSet1.size(); k++) {
//                        globalStep.addCandidateToDelete(tmpSet1.get(k), cand);
//                    }
//                }
            }
        }
        if (!deleteSet.isEmpty()) {
            for (int i = 0; i < deleteSet.size(); i++) {
                globalStep.addCandidateToDelete(deleteSet.get(i), cand);
            }
        }
    }

    /**
     * Find the next MC step.
     * @param onlyOne
     * @return
     */
    private SolutionStep findMultiColorStep(boolean onlyOne) {
        steps.clear();
        SolutionStep step = findMultiColorSteps(onlyOne);
        if (onlyOne && step != null) {
            return step;
        } else if (steps.size() > 0) {
            Collections.sort(steps);
            return steps.get(0);
        }
        return null;
    }

    /**
     * Iterates over all candidates and tries to find steps.
     * @param onlyOne
     * @return
     */
    private SolutionStep findMultiColorSteps(boolean onlyOne) {
        for (int i = 1; i <= 9; i++) {
            SolutionStep step = findMultiColorStepsForCandidate(i, onlyOne);
            if (onlyOne && step != null) {
                return step;
            }
        }
        return null;
    }

    /**
     * Does the real work...
     * @param cand
     * @param onlyOne
     * @return
     */
    private SolutionStep findMultiColorStepsForCandidate(int cand, boolean onlyOne) {
        int anzColors = doColoring(cand);
        // first check whether cells of one color can see opposite cells of another color pair.
        // if so, all cells with that color can be eliminated
        // NOTE: a->b is not equal b->a, so ALL combinations have to be checked
        for (int i = 0; i < anzColors; i++) {
            for (int j = 0; j < anzColors; j++) {
                if (i == j) {
                    // color pairs have to be different
                    continue;
                }
                SudokuSet set11 = sets[cand][i][C1];
                SudokuSet set12 = sets[cand][i][C2];
                SudokuSet set21 = sets[cand][j][C1];
                SudokuSet set22 = sets[cand][j][C2];
                globalStep.reset();
                if (checkMultiColor1(set11, set21, set22)) {
                    for (int k = 0; k < set11.size(); k++) {
                        globalStep.addCandidateToDelete(set11.get(k), cand);
                    }
                }
                if (checkMultiColor1(set12, set21, set22)) {
                    for (int k = 0; k < set12.size(); k++) {
                        globalStep.addCandidateToDelete(set12.get(k), cand);
                    }
                }
                if (!globalStep.getCandidatesToDelete().isEmpty()) {
                    globalStep.setType(SolutionType.MULTI_COLORS_2);
                    globalStep.addValue(cand);
                    globalStep.addColorCandidates(set11, 0);
                    globalStep.addColorCandidates(set12, 1);
                    globalStep.addColorCandidates(set21, 2);
                    globalStep.addColorCandidates(set22, 3);
                    SolutionStep step = (SolutionStep) globalStep.clone();
                    if (onlyOne) {
                        return step;
                    } else {
                        steps.add(step);
                    }
                }
                
                // now check, if a two cells of different color pairs can see each other. If so,
                // all candidates, that can see cells of the two other colors, can be eliminated
                globalStep.reset();
                if (checkMultiColor2(set11, set21)) {
                    checkCandidateToDelete(set12, set22, cand);
                }
                if (checkMultiColor2(set11, set22)) {
                    checkCandidateToDelete(set12, set21, cand);
                }
                if (checkMultiColor2(set12, set21)) {
                    checkCandidateToDelete(set11, set22, cand);
                }
                if (checkMultiColor2(set12, set22)) {
                    checkCandidateToDelete(set11, set21, cand);
                }
                if (!globalStep.getCandidatesToDelete().isEmpty()) {
                    globalStep.setType(SolutionType.MULTI_COLORS_1);
                    globalStep.addValue(cand);
                    globalStep.addColorCandidates(set11, 0);
                    globalStep.addColorCandidates(set12, 1);
                    globalStep.addColorCandidates(set21, 2);
                    globalStep.addColorCandidates(set22, 3);
                    SolutionStep step = (SolutionStep) globalStep.clone();
                    if (onlyOne) {
                        return step;
                    } else {
                        steps.add(step);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks, whether cells in set can see cells of s21 and s22. If so, all candidates
     * in set can be eliminated.
     * @param set Set to be checked
     * @param s21 First color of other color pair
     * @param s22 Second color of other color pair
     * @return
     */
    private boolean checkMultiColor1(SudokuSet set, SudokuSet s21, SudokuSet s22) {
        boolean seeS21 = false;
        boolean seeS22 = false;
        for (int i = 0; i < set.size(); i++) {
            tmpSet1.set(Sudoku2.buddies[set.get(i)]);
            if (!tmpSet1.andEmpty(s21)) {
                seeS21 = true;
            }
            if (!tmpSet1.andEmpty(s22)) {
                seeS22 = true;
            }
            if (seeS21 && seeS22) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks, whether some cell in set1 can see a cell in set2.
     * @param set1 First set
     * @param set2 Second set
     * @return
     */
    private boolean checkMultiColor2(SudokuSet set1, SudokuSet set2) {
        for (int i = 0; i < set1.size(); i++) {
            for (int j = 0; j < set2.size(); j++) {
                if (Sudoku2.buddies[set1.get(i)].contains(set2.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Actually colors the grid. "Coloring the grid" means,
     * that every candidate, that is part of at least on conjugate pair, is
     * aasigned a color. "Assigned a color" means, that the candidate is added
     * to one of the {@link #sets}.<br>
     * 
     * The algorithm is easy:<ul>
     * <li>first eliminate all candidates, that are not part of at least one conjugate pair</li>
     * <li>for every remaining uncolored candidate do the coloring</li></ul>
     * Colored candidates are removed from {@link #startSet}.<br>
     * Coloring sets are buffered: If a set has already been calculated for a given candidate
     * and a certain state of a sudoku the method does nothing.
     * @param cand
     * @return
     */
    private int doColoring(int cand) {
        // caching
        if (stepNumbers[cand] == finder.getStepNumber()) {
            // sudoku has not changed since last calculation
            return anzColorPairs[cand];
        }
        // reset everything
        anzColorPairs[cand] = 0;
        stepNumbers[cand] = finder.getStepNumber();
        // first: remove all candidates, that are not part of at least one conjugate pair
        startSet.set(finder.getCandidates()[cand]);
        int[] values = startSet.getValues();
        int size = startSet.size();
        byte[][] free = sudoku.getFree();
        for (int i = 0; i < size; i++) {
            int index = values[i];
            if (free[Sudoku2.CONSTRAINTS[index][0]][cand] != 2 &&
                    free[Sudoku2.CONSTRAINTS[index][1]][cand] != 2 &&
                    free[Sudoku2.CONSTRAINTS[index][2]][cand] != 2) {
                // cannot be part of a conjugate pair
                startSet.remove(values[i]);
                continue;
            }
        }
        // now do the coloring; startSet is changed during the process, so don't try to loop!
        while (!startSet.isEmpty()) {
            // reset the sets for the new color
            SudokuSet[] actSets = sets[cand][anzColorPairs[cand]];
            actSets[C1].clear();
            actSets[C2].clear();
            int index = startSet.get(0);
            doColoringForColorRecursive(index, cand, true);
            // a color chain has to consist of two cells at least (one on, one off)
            // single candidates are discarded
            if (actSets[C1].isEmpty() || actSets[C2].isEmpty()) {
                actSets[C1].clear();
                actSets[C2].clear();
            } else {
                anzColorPairs[cand]++;
            }
        }
        return anzColorPairs[cand];
    }

    /**
     * Colors the cell index with color anzColorPairs[cand]/C1 and tries to
     * find all conjugate pairs. Every colored candidate is removed from startSet.
     * 
     * @param index index of the cell to color. -1 means stop the coloring
     * @param cand Candidate for which the coloring is performed
     * @param on true: use {@link #C1}, false: use {@link #C2}
     */
    private void doColoringForColorRecursive(int index, int cand, boolean on) {
        if (index == -1 || ! startSet.contains(index)) {
            return;
        }
        if (on) {
            sets[cand][anzColorPairs[cand]][C1].add(index);
        } else {
            sets[cand][anzColorPairs[cand]][C2].add(index);
        }
        startSet.remove(index);
        // recursion
        doColoringForColorRecursive(getConjugateIndex(index, cand, Sudoku2.CONSTRAINTS[index][0]), cand, !on);
        doColoringForColorRecursive(getConjugateIndex(index, cand, Sudoku2.CONSTRAINTS[index][1]), cand, !on);
        doColoringForColorRecursive(getConjugateIndex(index, cand, Sudoku2.CONSTRAINTS[index][2]), cand, !on);
    }

    /**
     * Checks whether cell <code>index</code> belongs to a conjugate
     * pair for candidate <code>cand</code> in constraint
     * <code>constraint</code>. Returns the other cell or -1, if there
     * is no conjugate pair.
     * @param index Index of cell for which a conjugate link is tried to find
     * @param cand The candidate for which the search is performed
     * @param constraint The constraint to check against
     * @return An index, if the house has only one cell left, or -1
     */
    private int getConjugateIndex(int index, int cand, int constraint) {
        if (sudoku.getFree()[constraint][cand] != 2) {
            // no conjugate pair!
            return -1;
        }
        // must be a conjugate pair, find the other index
        tmpSet1.set(finder.getCandidates()[cand]);
        tmpSet1.and(Sudoku2.ALL_CONSTRAINTS_TEMPLATES[constraint]);
        int result = tmpSet1.get(0);
        if (result == index) {
            result = tmpSet1.get(1);
        }
        return result;
    }
}
