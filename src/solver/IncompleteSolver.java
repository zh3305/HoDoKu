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
import sudoku.SolutionType;

/**
 *
 * @author hobiwan
 */
public class IncompleteSolver extends AbstractSolver {
    
    /** Creates a new instance of IncompleteSolver
     * @param finder 
     */
    public IncompleteSolver(SudokuStepFinder finder) {
        super(finder);
    }

    @Override
    protected SolutionStep getStep(SolutionType type) {
        if (type == SolutionType.INCOMPLETE) {
            return null;
        }
        return null;
    }

    @Override
    protected boolean doStep(SolutionStep step) {
        boolean handled = false;
        switch (step.getType()) {
            case INCOMPLETE:
                handled = true;
                break;
            default:
                handled = false;
        }
        return handled;
    }
    
}
