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
import java.util.List;
import sudoku.Candidate;
import sudoku.ClipboardMode;
import sudoku.SolutionStep;
import sudoku.SolutionType;
import sudoku.Sudoku2;
import sudoku.SudokuSet;

/**
 *
 * @author hobiwan
 */
public class WingSolver extends AbstractSolver {

    /** One global step for eliminations */
    private SolutionStep globalStep = new SolutionStep(SolutionType.FULL_HOUSE);
    /** A list for all steps found in one search */
    private List<SolutionStep> steps = new ArrayList<SolutionStep>();
    /** A set for elimination checks */
    private SudokuSet preCalcSet1 = new SudokuSet();
    /** A set for elimination checks */
    private SudokuSet preCalcSet2 = new SudokuSet();
    /** A set for elimination checks */
    private SudokuSet elimSet = new SudokuSet();
    /** The indices of all bivalue cells in the current sudoku (for XY-Wing) */
    private int[] biCells = new int[Sudoku2.LENGTH];
    /** The indices of all trivalue cells in the current sudoku (for XYZ-Wing) */
    private int[] triCells = new int[Sudoku2.LENGTH];
    /** The first index of the strong link for W-Wings */
    private int wIndex1 = -1;
    /** The second index of the strong link for W-Wings */
    private int wIndex2 = -1;
    
    /** Creates a new instance of WingSolver
     * @param finder 
     */
    public WingSolver(SudokuStepFinder finder) {
        super(finder);
    }
    
    @Override
    protected SolutionStep getStep(SolutionType type) {
        SolutionStep result = null;
        sudoku = finder.getSudoku();
        switch (type) {
            case XY_WING:
                result = getXYWing();
                break;
            case XYZ_WING:
                result = getXYZWing();
                break;
            case W_WING:
                result = getWWing(true);
                break;
        }
        return result;
    }
    
    @Override
    protected boolean doStep(SolutionStep step) {
        boolean handled = true;
        sudoku = finder.getSudoku();
        switch (step.getType()) {
            case XY_WING:
            case W_WING:
            case XYZ_WING:
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
     * get the next XY-Wing
     * @return
     */
    private SolutionStep getXYWing() {
        return getWing(false, true);
    }

    /**
     * get the next XYZ-Wing
     * @return
     */
    private SolutionStep getXYZWing() {
        return getWing(true, true);
    }

    /**
     * Searches for all types of wings.
     * @return
     */
    protected List<SolutionStep> getAllWings() {
        sudoku = finder.getSudoku();
        List<SolutionStep> newSteps = new ArrayList<SolutionStep>();
        List<SolutionStep> oldSteps = steps;
        steps = newSteps;

        getWing(true, false);
        getWing(false, false);
        getWWing(false);

        steps = oldSteps;
        return newSteps;
    }

    /**
     * Wir suchen das Grid ab nach Zellen mit nur 2(3) Kandidaten xy (xyz). Wenn wir eine finden,
     * versuchen wir eine weitere Zelle mit zwei Kandidaten im gleichen Block oder in der gleichen
     * Spalte (im gleichen Block) zu finden, bei der ein Kandidat gleich, der zweite aber verschieden
     * (beide gleich) ist - xz (xz). Wenn wir eine solche Zelle finden, suchen wir in der gleichen Zeile oder
     * Spalte des ersten Elements (zweites Element in Spalte: nur Zeile) eine dritte Bivalue-Zelle, die
     * den neuen Kandidaten mit der zweiten Zelle gemeinsam hat und deren zweiter Kandidat der
     * fehlende Kandidat der ersten Zelle ist - yz (yz). Alle Zellen die die zweite und dritte Zelle
     * (alle drei Zellen) sehen können, können z nicht als Kandidaten haben.
     */
    /**
     * Try all combinations of three bivalue cells (for xyz: one trivalue
     * and two bivalue cells). The following restrictions are in place:
     * <ul>
     * <li>The three cells must have exactly three candidates together</li>
     * <li>The first cell (pivot) must see both other cells (pincers)</li>
     * <li>The pincers must have exactly one candidate that is the same (candidate z)</li>
     * <li>z can be excluded from all cells that see both pincers (for xyz they must see the pivot as well)</li>
     * </ul>
     * @param xyz
     * @param onlyOne
     * @return
     */
    private SolutionStep getWing(boolean xyz, boolean onlyOne) {
        // first get all bivalue/trivalue cells
        int biValueCount = 0;
        int triValueCount = 0;
        for (int i = 0; i < Sudoku2.LENGTH; i++) {
            if (sudoku.getAnzCandidates(i) == 2) {
                biCells[biValueCount++] = i;
            }
            if (xyz && sudoku.getAnzCandidates(i) == 3) {
                triCells[triValueCount++] = i;
            }
        }
        // now iterate them; use local variables to cover xy and xyz
        int endIndex = xyz ? triValueCount : biValueCount;
        int[] biTri = xyz ? triCells : biCells;
        // we check all combinations of bivalue cells (one tri + 2 bi for xyz)
        for (int i = 0; i < endIndex; i++) {
            for (int j = xyz ? 0 : i + 1; j < biValueCount; j++) {
                // any given combination of two cells must give exactly three
                // candidates; if that is not the case, skip it right away
                if (Sudoku2.ANZ_VALUES[sudoku.getCell(biTri[i]) | sudoku.getCell(biCells[j])] != 3) {
                    // cant become a wing
                    continue;
                }
                for (int k = j + 1; k < biValueCount; k++) {
                    int index1 = biTri[i];
                    int index2 = biCells[j];
                    int index3 = biCells[k];
                    int cell1 = sudoku.getCell(index1);
                    int cell2 = sudoku.getCell(index2);
                    int cell3 = sudoku.getCell(index3);
                    // all three cells combined must have exactly three candidates
                    if (Sudoku2.ANZ_VALUES[cell1 | cell2 | cell3] != 3) {
                        // incorrect number of candidates
                        continue;
                    }
                    // none of the cells may be equal
                    if (cell1 == cell2 || cell2 == cell3 || cell3 == cell1) {
                        // cant be a wing
                        continue;
                    }
                    // three possibilities for XY-Wing: each cell could be the pincer
                    // XYZ-Wing exits the loop after the first iteration
                    int maxTries = xyz ? 1 : 3;
                    for (int tries = 0; tries < maxTries; tries++) {
                        // swap cells accordingly
                        if (tries == 1) {
                            index1 = biCells[j];
                            index2 = biTri[i];
                            cell1 = sudoku.getCell(index1);
                            cell2 = sudoku.getCell(index2);
                        } else if (tries == 2) {
                            index1 = biCells[k];
                            index2 = biCells[j];
                            index3 = biTri[i];
                            cell1 = sudoku.getCell(index1);
                            cell2 = sudoku.getCell(index2);
                            cell3 = sudoku.getCell(index3);
                        }
                        // the pivot must see the pincers
                        if (! Sudoku2.buddies[index1].contains(index2) || ! Sudoku2.buddies[index1].contains(index3)) {
                            // doesnt see them -> try another
                            continue;
                        }
                        // the pincers must have exactly one candidate that is the same in both cells
                        short cell = (short)(cell2 & cell3);
                        if (Sudoku2.ANZ_VALUES[cell] != 1) {
                            // no wing, sorry
                            continue;
                        }
                        int candZ = Sudoku2.CAND_FROM_MASK[cell];
                        // are there candidates that can see the pincers?
                        elimSet.setAnd(Sudoku2.buddies[index2], Sudoku2.buddies[index3]);
                        elimSet.and(finder.getCandidates()[candZ]);
                        if (xyz) {
                            // the pivot as well
                            elimSet.and(Sudoku2.buddies[index1]);
                        }
                        if (elimSet.isEmpty()) {
                            // no candidates to delete
                            continue;
                        }
                        // ok, wing found!
                        globalStep.reset();
                        if (xyz) {
                            globalStep.setType(SolutionType.XYZ_WING);
                        } else {
                            globalStep.setType(SolutionType.XY_WING);
                        }
                        int[] cands = sudoku.getAllCandidates(index1);
                        globalStep.addValue(cands[0]);
                        globalStep.addValue(cands[1]);
                        if (xyz) {
                            globalStep.addValue(cands[2]);
                        } else {
                            globalStep.addValue(candZ);
                        }
                        globalStep.addIndex(index1);
                        globalStep.addIndex(index2);
                        globalStep.addIndex(index3);
                        if (xyz) {
                            globalStep.addFin(index1, candZ);
                        }
                        globalStep.addFin(index2, candZ);
                        globalStep.addFin(index3, candZ);
                        for (int l = 0; l < elimSet.size(); l++) {
                            globalStep.addCandidateToDelete(elimSet.get(l), candZ);
                        }
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
        return null;
    }
    
    /**
     * Searches for W-Wings: look for all combinations of bivalue
     * cells with the same candidates. If one is found and it could theoretically
     * eliminate something, a connecting strong link is searched for.
     * @param onlyOne
     * @return
     */
    private SolutionStep getWWing(boolean onlyOne) {
        for (int i = 0; i < sudoku.getCells().length; i++) {
            if (sudoku.getValue(i) != 0 || sudoku.getAnzCandidates(i) != 2) {
                continue;
            }
            // bivalue cell found
            short cell1 = sudoku.getCell(i);
            int cand1 = sudoku.getAllCandidates(i)[0];
            int cand2 = sudoku.getAllCandidates(i)[1];
            // prepare for elimination checks
            preCalcSet1.setAnd(Sudoku2.buddies[i], finder.getCandidates()[cand1]);
            preCalcSet2.setAnd(Sudoku2.buddies[i], finder.getCandidates()[cand2]);
            // check all other cells
            for (int j = i + 1; j < sudoku.getCells().length; j++) {
                if (sudoku.getCell(j) != cell1) {
                    // doesnt fit!
                    continue;
                }
                // ok, we have a pair; can anything be eliminated?
                elimSet.setAnd(preCalcSet1, Sudoku2.buddies[j]);
                if (! elimSet.isEmpty()) {
                    // check for W-Wing for candidate cand1
                    SolutionStep step = checkLink(cand1, cand2, i, j, elimSet, onlyOne);
                    if (onlyOne && step != null) {
                        return step;
                    }
                }
                elimSet.setAnd(preCalcSet2, Sudoku2.buddies[j]);
                if (! elimSet.isEmpty()) {
                    // check for W-Wing for candidate cand2
                    SolutionStep step = checkLink(cand2, cand1, i, j, elimSet, onlyOne);
                    if (onlyOne && step != null) {
                        return step;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Searches for a strong link for candidate <code>cand</code> that
     * connects <code>index1</code> and <code>index2</code> (both indices
     * are seen by the strong link).
     * @param cand1
     * @param cand2
     * @param index1
     * @param index2
     * @param elimSet
     * @param onlyOne
     * @return
     */
    private SolutionStep checkLink(int cand1, int cand2, int index1, int index2, SudokuSet elimSet, boolean onlyOne) {
        byte[][] free = sudoku.getFree();
        for (int constr = 0; constr < free.length; constr++) {
            if (free[constr][cand2] == 2) {
                // strong link; does it fit?
                boolean sees1 = false;
                boolean sees2 = false;
                int[] indices = Sudoku2.ALL_UNITS[constr];
                for (int i = 0; i < indices.length; i++) {
                    int aktIndex = indices[i];
                    if (aktIndex != index1 && aktIndex != index2 && sudoku.isCandidate(aktIndex, cand2)) {
                        // CAUTION: one cell of the strong link can see both bivalue cells -> forbidden
                        if (Sudoku2.buddies[aktIndex].contains(index1)) {
                            sees1 = true;
                            wIndex1 = aktIndex;
                        } else if (Sudoku2.buddies[aktIndex].contains(index2)) {
                            sees2 = true;
                            wIndex2 = aktIndex;
                        }
                    }
                    if (sees1 && sees2) {
                        // done
                        break;
                    }
                }
                if (sees1 && sees2) {
                    // valid W-Wing!
                    SolutionStep step = createWWingStep(cand1, cand2, index1, index2, elimSet, onlyOne);
                    if (onlyOne && step != null) {
                        return step;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Creates a step for a W-Wing. <code>cand1</code> is the candidate for which
     * eliminations can be made, <code>cand2</code> is the connecting candidate.
     * <code>index1</code> and <code>index2</code> are the bivalue cells,
     * {@link #wIndex1} and {@link #wIndex2} are the strong link. <code>elimSet</code>
     * holds all cells where candidates can be eliminated.
     * @param cand1
     * @param cand2
     * @param index1
     * @param index2
     * @param elimSet
     * @param onlyOne
     * @return
     */
    private SolutionStep createWWingStep(int cand1, int cand2, int index1, int index2, SudokuSet elimSet, boolean onlyOne) {
        globalStep.reset();
        globalStep.setType(SolutionType.W_WING);
        globalStep.addValue(cand1);
        globalStep.addValue(cand2);
        globalStep.addIndex(index1);
        globalStep.addIndex(index2);
        globalStep.addFin(index1, cand2);
        globalStep.addFin(index2, cand2);
        globalStep.addFin(wIndex1, cand2);
        globalStep.addFin(wIndex2, cand2);
        for (int i = 0; i < elimSet.size(); i++) {
            globalStep.addCandidateToDelete(elimSet.get(i), cand1);
        }
        SolutionStep step = (SolutionStep) globalStep.clone();
        if (onlyOne) {
            return step;
        } else {
            steps.add(step);
        }
        return null;
    }

    public static void main(String[] args) {
        Sudoku2 sudoku = new Sudoku2();
        // W-Wing: 4/1 in r1c9,r8c7 verbunden durch 1 in r18c3 => r123c7,r89c9<>4
        sudoku.setSudoku(":0803:14:6..+9+5..7...+9.2.....58.+31...+1+64+3+8+9+7+52...1+7+59+46597+24+6..892+54+1+76+8+3...5+6+2.....68+93...::417 427 437 489 499::");
        // XY-Wing: 1/2/3 in r2c7,r3c18 => r3c7<>3
        sudoku.setSudoku(":0800:123:..+8+2..57.+7.+54....+8..9+8+57...4+5+17+2+98+6+3+2765+83+94+1+9+8+3+6+1+4+7+526+9+23+4+5+1+8+7537+168...+81+4+9+726+3+5::337::");

        SudokuSolver solver = SudokuSolverFactory.getDefaultSolverInstance();
        SolutionStep step = solver.getHint(sudoku, false);
        System.out.println(step);
        System.out.println(sudoku.getSudoku(ClipboardMode.LIBRARY, step));
        System.exit(0);
    }
}
