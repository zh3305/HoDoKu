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

/*
 * Sudoku2-Grid mit Indices für Debugging:
 *
 *      1  2  3    4  5  6    7  8  9
 *   +----------+----------+----------+
 * 1 | 00 01 02 | 03 04 05 | 06 07 08 | 1
 * 2 | 09 10 11 | 12 13 14 | 15 16 17 | 2
 * 3 | 18 19 20 | 21 22 23 | 24 25 26 | 3
 *   +----------+----------+----------+
 * 4 | 27 28 29 | 30 31 32 | 33 34 35 | 4
 * 5 | 36 37 38 | 39 40 41 | 42 43 44 | 5
 * 6 | 45 46 47 | 48 49 50 | 51 52 53 | 6
 *   +----------+----------+----------+
 * 7 | 54 55 56 | 57 58 59 | 60 61 62 | 7
 * 8 | 63 64 65 | 66 67 68 | 69 70 71 | 8
 * 9 | 72 73 74 | 75 76 77 | 78 79 80 | 9
 *   +----------+----------+----------+
 *      1  2  3    4  5  6    7  8  9
 */

package solver;

import sudoku.SolutionStep;
import sudoku.SolutionType;
import sudoku.Sudoku2;

/**
 *
 * @author hobiwan
 */
public abstract class AbstractSolver {
    /** The {@link SudokuStepFinder} to which this specialized solver belongs. */
    protected SudokuStepFinder finder;
    /** Every solver needs the sudoku... */
    protected Sudoku2 sudoku;

//    private SudokuSet tmpSet = new SudokuSet();
    
    /** Creates a new instance of AbstractSolver
     * @param finder 
     */
    public AbstractSolver(SudokuStepFinder finder) {
        this.finder = finder;
    }

    /**
     * Method for finding a new instance of a specific technique.
     * @param type
     * @return
     */
    protected abstract SolutionStep getStep(SolutionType type);

    /**
     * Method for executing a specific technique.
     * @param step
     * @return
     */
    protected abstract boolean doStep(SolutionStep step);
    
    /**
     * Method is called in regular intervals to clean up
     * data structures. If a solver wants to use this functionality,
     * it has to override this method. If the method is overriden,
     * special care has to be taken to synchronize correctly.
     */
    protected void cleanUp() {
        // do nothing
    }
}
