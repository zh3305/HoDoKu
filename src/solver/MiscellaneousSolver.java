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
 * Originale Beschreibung aus dem Player's Forum:
 *    Consider the set of unfilled cells C that lies at the intersection of Box B and Row (or Column) R.
 *    Suppose |C|>=2. Let V be the set of candidate values to occur in C. Suppose |V|>= |C|+2.
 *    The pattern requires that we find |V|-|C| cells in B and R, with at least one cell in each,
 *    with candidates drawn entirely from V. Label the sets of cells CB and CR and their candidates VB and VR.
 *    Crucially, no candidate is allowed to appear in VB and VR. Then C must contain V\(VB U VR) [possibly empty],
 *    |VB|-|CB| elements of VB and |VR|-|CR| elements of VR. The construction allows us to eliminate the
 *    candidates V\VR from B\(C U CB) and the candidates V\VB from R\(C U CR).
 *
 * Erweiterungen:
 *    - C muss nicht alle Zellen der Intersection enthalten
 *    - VB und VR dürfen zusätzlich Kandidaten enthalten, es muss allerdings dann eine zusätzliche Zelle
 *      pro zusätzlichem Kandidaten geben (Naked Subset)
 *
 * - Finde ungelöste Zellen am Schnittpunkt zwischen Zeile/Spalte und Block
 * - Für alle Kombinationen aus diesen Zellen:
 *     - Ermittle die Kandidaten -> Anzahl muss um mindestens 2 (N) größer sein als Anzahl Zellen
 *     - Finde N Zellen in der Zeile/Spalte und N Zellen im Block, die nicht Teil der Intersection sind,
 *       die jeweils nur Kandidaten enthalten, die in der Intersection enthalten sind
 *       (die Kandidaten-Sets aus Zeile/Spalte und Block dürfen sich nicht überschneiden)
 *     - Wenn in den N Zellen Kandidaten enthalten sind, die nicht Teil der Intersection sind, muss es
 *       eine zusätzliche Zelle pro zusätzlichem Kandidaten geben (am Besten alle Kombinationen durchgehen)
 *     - In den Zellen der Zeile/Spalte, die bis jetzt noch nicht verwendet wurden, können alle Kandidaten
 *       gelöscht werden, die in der Intersection, aber nicht im Set des Blocks vorkommen.
 *     - Analog im Block
 *
 * @author hobiwan
 */
public class MiscellaneousSolver extends AbstractSolver {

    /** One entry in the recursion stack for the unit search */
    private class StackEntry {

        /** The index of the cell that is currently tried */
        int aktIndex = 0;
        /** The indices of the cells in the current selection */
        SudokuSet indices = new SudokuSet();
        /** The candidates in the current selection */
        short candidates = 0;
    }
    /** All steps that were found in this search */
    private List<SolutionStep> steps;
    /** One global step for optimization */
    private SolutionStep globalStep = new SolutionStep(SolutionType.HIDDEN_SINGLE);
    /** All indices in the current row/col (only cells that are not set yet) */
    private SudokuSet nonBlockSet = new SudokuSet();
    /** All indices in the current block (only cells that are not set yet) */
    private SudokuSet blockSet = new SudokuSet();
    /** All indices in the current intersection (only cells that are not set yet) */
    private SudokuSet intersectionSet = new SudokuSet();
    /** All indices in row/col that can hold additional cells (row/col - set cells - intersection) */
    private SudokuSet nonBlockSourceSet = new SudokuSet();
    /** All indices in block that can hold additional cells (block - set cells - intersection) */
    private SudokuSet blockSourceSet = new SudokuSet();
    /** Stack for searching rows/cols */
    private StackEntry[] stack1 = new StackEntry[Sudoku2.UNITS];
    /** Stack for searching blocks */
    private StackEntry[] stack2 = new StackEntry[Sudoku2.UNITS];
    /** Cells of the current subset of the intersection */
    private SudokuSet intersectionActSet = new SudokuSet();
    /** Candidates of all cells in {@link #intersectionActSet}. */
    private short intersectionActCandSet = 0;
    /** Indices of the current additional cells in the row/col */
    private SudokuSet nonBlockActSet = new SudokuSet();
    /** Candidates of all cells in {@link #nonBlockActSet}. */
    private short nonBlockActCandSet = 0;
    /** Valid candidates for block */
    private short blockAllowedCandSet = 0;
    /** Indices of the current additional cells in the block */
    private SudokuSet blockActSet = new SudokuSet();
    /** Candidates of all cells in {@link #blockActSet}. */
    private short blockActCandSet = 0;
    /** For temporary calculations */
    private SudokuSet tmpSet = new SudokuSet();

    /** 
     * Creates a new instance of MiscellaneousSolver
     * @param finder 
     */
    public MiscellaneousSolver(SudokuStepFinder finder) {
        super(finder);
        for (int i = 0; i < stack1.length; i++) {
            stack1[i] = new StackEntry();
            stack2[i] = new StackEntry();
        }
    }

    @Override
    protected SolutionStep getStep(SolutionType type) {
        SolutionStep result = null;
        sudoku = finder.getSudoku();
        switch (type) {
            case SUE_DE_COQ:
                result = getSueDeCoq(true);
                break;
        }
        return result;
    }

    @Override
    protected boolean doStep(SolutionStep step) {
        boolean handled = true;
        sudoku = finder.getSudoku();
        switch (step.getType()) {
            case SUE_DE_COQ:
                for (Candidate cand : step.getCandidatesToDelete()) {
                    sudoku.delCandidate(cand.getIndex(), cand.getValue());
                }
                break;
            default:
                handled = false;
        }
        return handled;
    }

    protected List<SolutionStep> getAllSueDeCoqs() {
        sudoku = finder.getSudoku();
        List<SolutionStep> oldSteps = steps;
        steps = new ArrayList<SolutionStep>();
        getSueDeCoqInt(Sudoku2.LINE_TEMPLATES, Sudoku2.BLOCK_TEMPLATES, false);
        getSueDeCoqInt(Sudoku2.COL_TEMPLATES, Sudoku2.BLOCK_TEMPLATES, false);
        List<SolutionStep> result = steps;
        steps = oldSteps;
        return result;
    }

    private SolutionStep getSueDeCoq(boolean onlyOne) {
        SolutionStep step = getSueDeCoqInt(Sudoku2.LINE_TEMPLATES, Sudoku2.BLOCK_TEMPLATES, onlyOne);
        if (onlyOne && step != null) {
            return step;
        }
        return getSueDeCoqInt(Sudoku2.COL_TEMPLATES, Sudoku2.BLOCK_TEMPLATES, onlyOne);
    }

    /**
     * Builds all possible intersections of <code>blocks</code> and
     * <code>nonBlocks</code>. Delegates the check to
     * {@link #checkIntersection(boolean) }.
     * @param nonBlocks Sets containing all lines or all columns
     * @param blocks Sets containing all blocks
     * @param onlyOne
     * @return
     */
    private SolutionStep getSueDeCoqInt(SudokuSet[] nonBlocks, SudokuSet[] blocks, boolean onlyOne) {
        // get all possible intersections between blocks and nonBlocks
        SudokuSet emptyCells = finder.getEmptyCells();
        // for every row/col
        for (int i = 0; i < nonBlocks.length; i++) {
            nonBlockSet.setAnd(nonBlocks[i], emptyCells);
            // and every block
            for (int j = 0; j < blocks.length; j++) {
                blockSet.setAnd(blocks[j], emptyCells);
                // get the intersection
                intersectionSet.set(nonBlockSet);
                intersectionSet.and(blockSet);
                if (intersectionSet.isEmpty() || intersectionSet.size() < 2) {
                    // nothing to do
                    continue;
                }
                // check the intersection
                SolutionStep step = checkIntersection(onlyOne);
                if (onlyOne && step != null) {
                    return step;
                }
            }
        }
        return null;
    }

    /**
     * Checks all possible combinations of cells in the intersection. If a combination
     * holds 2 more candidates than cells, a SDC could possibly exist.<br>
     * The method doesnt use recursion. There can be only two or three cells in an
     * intersection for an SDC.
     * @param onlyOne
     * @return
     */
    private SolutionStep checkIntersection(boolean onlyOne) {
        int max = intersectionSet.size();
        int nPlus = 0;
        intersectionActSet.clear();
        for (int i1 = 0; i1 < max - 1; i1++) {
            // all candidates of the first cell
            int index1 = intersectionSet.get(i1);
            intersectionActSet.add(index1);
            short cand1 = sudoku.getCell(index1);
            // now try the second cell
            for (int i2 = i1 + 1; i2 < max; i2++) {
                int index2 = intersectionSet.get(i2);
                short cand2 = (short) (cand1 | sudoku.getCell(index2));
                intersectionActSet.add(index2);
                // we have two cells in the intersection
                nPlus = Sudoku2.ANZ_VALUES[cand2] - 2;
                if (nPlus >= 2) {
                    // possible SDC -> check
                    SolutionStep step = checkHouses(nPlus, cand2, onlyOne);
                    if (onlyOne && step != null) {
                        return step;
                    }
                }
                // and the third cell
                for (int i3 = i2 + 1; i3 < max; i3++) {
                    int index3 = intersectionSet.get(i3);
                    short cand3 = (short) (cand2 | sudoku.getCell(index3));
                    // now we have three cells in the intersection
                    nPlus = Sudoku2.ANZ_VALUES[cand3] - 3;
                    if (nPlus >= 2) {
                        // possible SDC -> check
                        intersectionActSet.add(index3);
                        SolutionStep step = checkHouses(nPlus, cand3, onlyOne);
                        if (onlyOne && step != null) {
                            return step;
                        }
                        intersectionActSet.remove(index3);
                    }
                }
                intersectionActSet.remove(index2);
            }
            intersectionActSet.remove(index1);
        }
        return null;
    }

    /**
     * Builds a set with all cells in the row/col that are not part of the
     * intersection and delegates the check to
     * {@link #checkHouses(int, sudoku.SudokuSet, short, short, boolean, boolean) }.
     * @param nPlus How many more candidates than cells
     * @param cand Candidates in the intersection
     * @param onlyOne
     * @return
     */
    private SolutionStep checkHouses(int nPlus, short cand, boolean onlyOne) {
        // store the candidates of the current intersection
        intersectionActCandSet = cand;
        // check nonBlocks: all cells not used in the intersection are valid
        nonBlockSourceSet.set(nonBlockSet);
        nonBlockSourceSet.andNot(intersectionActSet);
        // now check all possible combinations of cells in nonBlockSet
        return checkHouses(nPlus, nonBlockSourceSet, Sudoku2.MAX_MASK, onlyOne, false);
    }

    /**
     * Does a non recursive search: All possible combinations of indices in
     * <code>sourceSet</code> are tried.<br>
     * The method is used twice: The first run builds possible sets of cells
     * from the row/col. A set is valid if it contains candidates from the intersection,
     * has at least one cell more than extra candidates (candidates not contained in the
     * intersection) but leaves candidates in the intersection for the block search. If those
     * criteria are met, the method is called recursivly for the second run.<br>
     * The second run builds all possible sets of cells for the block. For every combination
     * that meets the SDC criteria a check for deleteable candidates is made.
     * @param nPlus
     * @param sourceSet
     * @param allowedCandSet
     * @param onlyOne
     * @param secondCheck
     * @return
     */
    private SolutionStep checkHouses(int nPlus, SudokuSet sourceSet,
            short allowedCandSet, boolean onlyOne, boolean secondCheck) {
        if (sourceSet.isEmpty()) {
            // nothing to do!
            return null;
        }
        StackEntry[] stack = secondCheck ? stack2 : stack1;
        int max = sourceSet.size();
        // level 0 is only a marker, we start with level 1
        int level = 1;
        stack[0].aktIndex = -1;
        stack[0].candidates = 0;
        stack[0].indices.clear();
        // get the first cell from sourceSet (there must be at least 1!)
        stack[1].aktIndex = -1;
        // check all possible combinations of cells
        while (true) {
            // fall back all levels where nothing can be done anymore
            while (stack[level].aktIndex >= max - 1) {
                level--;
                if (level <= 0) {
                    // ok, done
                    return null;
                }
            }
            // ok, calculate next try
            stack[level].aktIndex++;
            stack[level].indices.set(stack[level - 1].indices);
            stack[level].indices.add(sourceSet.get(stack[level].aktIndex));
            stack[level].candidates = (short) (stack[level - 1].candidates | sudoku.getCell(sourceSet.get(stack[level].aktIndex)));

            // the current cell combination must eliminate at least one candidate in the current
            // intersection or we dont have to look further
            // the cells must not contain candidates that are not in cand
            if ((stack[level].candidates & ~allowedCandSet) == 0) {
                short tmpCands = (short) (stack[level].candidates & intersectionActCandSet);
                int anzContained = Sudoku2.ANZ_VALUES[tmpCands]; // number of candidates from the intersection
                tmpCands = (short) (stack[level].candidates & ~intersectionActCandSet);
                int anzExtra = Sudoku2.ANZ_VALUES[tmpCands]; // number of candidates not drawn from the intersection

                // Here we must differentiate between first and second run:
                // In the first run we can switch over to the block if there are still candidates (and cells in the block) left
                // in the second run the number of candiates drawn from the intersection must equal nPlus
                if (!secondCheck) {
                    // level equals the number of current cells in the row/col
                    if (anzContained > 0 && level > anzExtra && level - anzExtra < nPlus) {
                        // The combination of cells contains candidates from the intersection, it has at least on
                        // cell more than the number of additional candidates (meaning: it eliminates at least one cell from
                        // the intersection) and there are uncovered candidates left in the intersection ->
                        // switch over to the block
                        // memorize current selection for second run
                        nonBlockActSet = stack[level].indices;
                        nonBlockActCandSet = stack[level].candidates;
                        blockSourceSet.set(blockSet);
                        blockSourceSet.andNot(intersectionActSet);
                        // exclude all cells that are already used in the roe/col
                        blockSourceSet.andNot(nonBlockActSet);
                        // candidates from the row/col set are not allowed anymore
                        blockAllowedCandSet = nonBlockActCandSet;
                        // 20090216 CAUTION: The extra candidates are allowed in both sets (still in tmpCands)
                        blockAllowedCandSet &= ~tmpCands;
                        blockAllowedCandSet = (short) ~blockAllowedCandSet;
                        // and the second run
                        SolutionStep step = checkHouses(nPlus - (nonBlockActSet.size() - anzExtra), blockSourceSet, blockAllowedCandSet, onlyOne, true);
                        if (onlyOne && step != null) {
                            return step;
                        }
                    }
                } else {
                    // now the number of candidates has to be exactly nPlus
                    if (anzContained > 0 && stack[level].indices.size() - anzExtra == nPlus) {
                        // It's a Sue de Coq! Can anything be eliminated?
                        // possible eliminations (special case "same extra candidate in block and non block" is not included below):
                        //  - (intersectionActCandSet + blockActCandSet) - nonBlockActCandSet in blockSet - blockActSet - intersectionActSet - nonBlockActSet
                        //  - (intersectionActCandSet + nonBlockActCandSet) - blockActCandSet in nonBlockSet - nonBlockActSet - intersectionActSet - blockActSet
                        // 20090216 If both sets hold the same extra candidates they can be eliminated from both sets!
                        globalStep.reset();
//                        System.out.println("===========================");
//                        System.out.println("intersectionSet: " + intersectionSet);
//                        System.out.println("intersectionActSet: " + intersectionActSet);
//                        System.out.println("intersectionActCandSet: " + intersectionActCandSet);
//                        System.out.println("blockSet: " + blockSet);
//                        System.out.println("blockActSet: " + blockActSet);
//                        System.out.println("blockActCandSet: " + blockActCandSet);
//                        System.out.println("nonBlockSet: " + nonBlockSet);
//                        System.out.println("nonBlockActSet: " + nonBlockActSet);
//                        System.out.println("nonBlockActCandSet: " + nonBlockActCandSet);
                        // get current data for block
                        blockActSet = stack[level].indices;
                        blockActCandSet = stack[level].candidates;
                        // 20090216: get the extra candidates that are in both sets
                        short tmpCandSet1 = (short) (blockActCandSet & nonBlockActCandSet);
                        // all cells in the block that dont belong to the SDC
                        tmpSet.set(blockSet);
                        tmpSet.andNot(blockActSet);
                        tmpSet.andNot(intersectionActSet);
//                        tmpSet.andNot(nonBlockActSet); // ???
                        // all candidates that can be eliminated in the block (including extra candidates contained in both sets)
                        short tmpCandSet = (short) (((intersectionActCandSet | blockActCandSet) & ~nonBlockActCandSet) | tmpCandSet1);
                        checkCandidatesToDelete(tmpSet, tmpCandSet);
                        // now the row/col
                        tmpSet.set(nonBlockSet);
                        tmpSet.andNot(nonBlockActSet);
                        tmpSet.andNot(intersectionActSet);
//                        tmpSet.andNot(blockActSet); // ???
                        // all candidates that can be eliminated in the row/col (including extra candidates contained in both sets)
                        tmpCandSet = (short) (((intersectionActCandSet | nonBlockActCandSet) & ~blockActCandSet) | tmpCandSet1);
                        checkCandidatesToDelete(tmpSet, tmpCandSet);
                        if (globalStep.getCandidatesToDelete().size() > 0) {
                            // FOUND ONE!
                            globalStep.setType(SolutionType.SUE_DE_COQ);
                            // intersection is written into indices and values
                            for (int j = 0; j < intersectionActSet.size(); j++) {
                                globalStep.addIndex(intersectionActSet.get(j));
                            }
                            int[] cands = Sudoku2.POSSIBLE_VALUES[intersectionActCandSet];
                            for (int j = 0; j < cands.length; j++) {
                                globalStep.addValue(cands[j]);
                            }
                            // Alle Kandidaten im nonBlockActSet (und die passenden im intersectionActSet) werden fins
                            // all candidates that occur in the intersection and in the row/col become fins (for display)
                            getSetCandidates(nonBlockActSet, intersectionActSet, nonBlockActCandSet, globalStep.getFins());
                            // all candidates that occur in the intersection and in the block become endo fins (for display)
                            getSetCandidates(blockActSet, intersectionActSet, blockActCandSet, globalStep.getEndoFins());

                            globalStep.addAls(intersectionActSet, intersectionActCandSet);
                            globalStep.addAls(blockActSet, blockActCandSet);
                            globalStep.addAls(nonBlockActSet, nonBlockActCandSet);
                            SolutionStep step = (SolutionStep) globalStep.clone();
                            if (onlyOne) {
                                return step;
                            } else {
                                steps.add(step);
                            }
                        }
                    }
                }
            }

            // on to the next level (if that is possible)
            if (stack[level].aktIndex < max - 1) {
                // ok, go to next level
                level++;
                stack[level].aktIndex = stack[level - 1].aktIndex;
            }
        }
    }

    /**
     * Write all candidates from <code>candSet</code> that are either in <code>srcSet1</code>
     * or in <code>srcSet2</code> into the list <code>dest</code>.<br>
     * Convenience method: Some candidates are written as fins/endo fins for display purposes.
     * @param srcSet1
     * @param srcSet2
     * @param candSet
     * @param dest
     */
    private void getSetCandidates(SudokuSet srcSet1, SudokuSet srcSet2, short candSet, List<Candidate> dest) {
        tmpSet.set(srcSet1);
        tmpSet.or(srcSet2);
        for (int i = 0; i < tmpSet.size(); i++) {
            int index = tmpSet.get(i);
            if ((sudoku.getCell(index) & candSet) != 0) {
                int[] cands = Sudoku2.POSSIBLE_VALUES[sudoku.getCell(index) & candSet];
                for (int j = 0; j < cands.length; j++) {
                    dest.add(new Candidate(index, cands[j]));
                }
            }
        }
    }

    /**
     * Checks if one of the cells in <code>tmpSet</code> contains candidates
     * from <code>tmpCandSet</code>. If so, they can be eliminated.
     * @param tmpSet
     * @param tmpCandSet
     */
    private void checkCandidatesToDelete(SudokuSet tmpSet, short tmpCandSet) {
        //System.out.println("checkCandidatesToDelete(" + tmpSet + ", " + tmpCandSet + ")");
        if (tmpSet.size() > 0 && Sudoku2.ANZ_VALUES[tmpCandSet] > 0) {
            for (int i = 0; i < tmpSet.size(); i++) {
                int index = tmpSet.get(i);
                short elimCandMask = (short) (sudoku.getCell(index) & tmpCandSet);
                if (elimCandMask == 0) {
                    // nothing to do!
                    continue;
                }
                int[] cands = Sudoku2.POSSIBLE_VALUES[elimCandMask];
                for (int j = 0; j < cands.length; j++) {
                    globalStep.addCandidateToDelete(index, cands[j]);
                    //System.out.println("   " + SolutionStep.getCellPrint(index) + "<>" + cands[j]);
                }
            }
        }
    }

    public static void main(String[] args) {
        Sudoku2 sudoku = new Sudoku2();
        // Sue de Coq: r7c45 - {123689} (r7c128 - {2389}, r9c6 - {16}) => r8c46,r9c4<>1, r8c456,r9c4<>6, r7c3<>2, r7c3<>9
        sudoku.setSudoku(":1101:12369:.....3+5+1+7+1+3+5+42+786+9867+91+54..+6+935+4+82+717183.+2.+54+2+54...........47.55......+4.....+5..9.::184 194 273 371 684 685 694 985::");
        SudokuSolver solver = SudokuSolverFactory.getDefaultSolverInstance();
        boolean singleHint = true;
        if (singleHint) {
            SolutionStep step = solver.getHint(sudoku, false);
            System.out.println(step);
        } else {
            List<SolutionStep> steps = solver.getStepFinder().getAllSueDeCoqs(sudoku);
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
