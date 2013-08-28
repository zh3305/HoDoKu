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

import sudoku.SolutionStep;
import sudoku.Sudoku2;
import sudoku.SudokuSet;

/**
 * An ALS is a number cells, that together contain one candidate more than the
 * number of cells. A single cell with only two candidates left is by this
 * definition a calid ALS.<br><br>
 * 
 * The search for ALSes is done by {@link SudokuStepFinder#getAlses()} which
 * can restrict the search to exclude single cells.
 * 
 * @author hobiwan
 */
public class Als {

    /** 
     * All indices that belong to the ALS 
     */
    public SudokuSet indices;
    /** 
     * All numbers that are contained in the ALS (only the numbers, not the actual candidates!) 
     */
    public short candidates;
    /** 
     * For every number contained in the ALS all cells containing that number as candidate 
     */
    public SudokuSet[] indicesPerCandidat = new SudokuSet[10];
    /** 
     * For every number contained in the ALS all cells outside the ALS that are 
     * buddies to all ALS cells holding that candidate 
     */
    public SudokuSet[] buddiesPerCandidat = new SudokuSet[10];
    /** 
     * Like {@link #buddiesPerCandidat} but including the ALS cells holding 
     * that candidate (for RC search). 
     */
    public SudokuSet[] buddiesAlsPerCandidat = new SudokuSet[10];
    /** 
     * All cells outside the als, that contain at least one candidate, 
     * that is a buddy to the ALS 
     */
    public SudokuSet buddies;
    /** 
     * The penalty for the ALS (used when calculating chain length) 
     */
    public int chainPenalty = -1;

    /**
     * Creates a new ALS.<br><br>
     * <b>Note:</b> An ALS created with this constructor cannot be
     * used unless {@link #computeFields(solver.SudokuStepFinder) }
     * has been called.
     * @param indices
     * @param candidates
     */
    public Als(SudokuSet indices, short candidates) {
        this.indices = new SudokuSet(indices);
        this.candidates = candidates;
    }

    /**
     * Computes all the additional fields; is done after the initial search
     * to optimize finding doubles.
     * 
     * @param finder
     */
    public void computeFields(SudokuStepFinder finder) {
        this.buddies = new SudokuSet();
        for (int i = 1; i <= 9; i++) {
            if ((candidates & Sudoku2.MASKS[i]) != 0) {
                SudokuSet sudokuCandidates = finder.getCandidates()[i];
                indicesPerCandidat[i] = new SudokuSet(indices);
                indicesPerCandidat[i].and(sudokuCandidates);
                buddiesPerCandidat[i] = new SudokuSet();
                Sudoku2.getBuddies(indicesPerCandidat[i], buddiesPerCandidat[i]);
                buddiesPerCandidat[i].andNot(indices);
                buddiesPerCandidat[i].and(finder.getCandidates()[i]);
                buddiesAlsPerCandidat[i] = new SudokuSet(buddiesPerCandidat[i]);
                buddiesAlsPerCandidat[i].or(indicesPerCandidat[i]);
                buddies.or(buddiesPerCandidat[i]);
            }
        }
    }

    /**
     * ALS in chains count as one link. This prefers chains containing large
     * ALS over slightly longer chains with smaller (or non at all) ALS. The
     * penalty is added to the chain length to suppress that behaviour.
     * @param candSize Number of candidates in the ALS
     * @return Number of links to be added to the chain size
     */
    public static int getChainPenalty(int candSize) {
        //return 0;
        if (candSize == 0 || candSize == 1) {
            return 0;
        } else if (candSize == 2) {
            return candSize - 1;
        } else {
            return (candSize - 1) * 2;
        }
    }

    /**
     * Returns the chain penalty of the ALS (see {@link #getChainPenalty(int)}).
     * @return Number of links to be added to the chain size
     */
    public int getChainPenalty() {
        if (chainPenalty == -1) {
            chainPenalty = getChainPenalty(Sudoku2.ANZ_VALUES[candidates]);
        }
        return chainPenalty;
    }

    /**
     * Two ALS are equal if they contain the same indices
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Als)) {
            return false;
        }
        Als a = (Als) o;
        return indices.equals(a.indices);
    }

    /**
     * Fitting for {@link #equals(java.lang.Object) }.
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (this.indices != null ? this.indices.hashCode() : 0);
        return hash;
    }

    /**
     * Human readable for for the ALS.
     * @return 
     */
    @Override
    public String toString() {
        //return "ALS: " + candidates.toString() + " - " + indices.toString();
        return "ALS: " + SolutionStep.getAls(this);
    }
}
