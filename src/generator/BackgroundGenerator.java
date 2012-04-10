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

package generator;

import java.awt.EventQueue;
import java.util.List;
import solver.SudokuSolver;
import solver.SudokuSolverFactory;
import sudoku.ClipboardMode;
import sudoku.DifficultyLevel;
import sudoku.GameMode;
import sudoku.GenerateSudokuProgressDialog;
import sudoku.Options;
import sudoku.SolutionStep;
import sudoku.Sudoku2;

/**
 * A BackgroundGenerator generates sudokus with a given {@link DifficultyLevel} 
 * and for a given {@link GameMode}. An instance of this class can be contained 
 * within a {@link BackgroundGeneratorThread} or within a {@link GenerateSudokuProgressDialog}.<br>
 * If it is called from a {@link GenerateSudokuProgressDialog}, it uses the
 * default solver and reports the progress to the dialog. If a puzzle has been
 * found, the dialog is closed. The creation process can be aborted at any time.<br>
 * If it is called from a {@link BackgroundGeneratorThread}, it simply delivers
 * the generated puzzle or <code>null</code>, if no puzzle could be found.
 * 
 * @author hobiwan
 */
public class BackgroundGenerator {
    /** Maximal number of tries, when called from a {@link BackgroundGeneratorThread}. */
    private static final int MAX_TRIES = 20000;
    /** Current number of tries when called from {@link GenerateSudokuProgressDialog}. */
    private int anz = 0;
    /** Progress dialog when called from GUI. */
    private GenerateSudokuProgressDialog progressDialog = null;

    /**
     * Generates a new instance.
     */
    public BackgroundGenerator() {
        // nothing to do!
    }
    
    /**
     * Creates a sudoku without responses to the GUI. Delegates to 
     * {@link #generate(sudoku.DifficultyLevel, sudoku.GameMode, sudoku.GenerateSudokuProgressDialog) }.
     * 
     * @param level
     * @param mode
     * @return 
     */
    public String generate(DifficultyLevel level, GameMode mode) {
        Sudoku2 sudoku = generate(level, mode, null);
        if (sudoku != null) {
            return sudoku.getSudoku(ClipboardMode.CLUES_ONLY);
        }
        return null;
    }
    
    /**
     * Generates a new sudoku: If <code>dlg</code> is <code>null</code>, the progress is
     * reported and the dialog is closed, when puzzle has been found. The
     * current thread is checked for interruptions.<br>
     * If <code>dlg</code> is not <code>null</code>, the creation process goes on
     * until a puzzle has been found or until {@link #MAX_TRIES} tries have been
     * run.
     * 
     * @param level
     * @param mode
     * @param dlg
     * @return 
     */
    public Sudoku2 generate(DifficultyLevel level, GameMode mode, GenerateSudokuProgressDialog dlg) {
        long actMillis = System.currentTimeMillis();
        progressDialog = dlg;
        Sudoku2 sudoku = null;
        SudokuGenerator creator = null; 
        SudokuSolver solver = null;
        setAnz(0);
        if (dlg == null) {
            // get any instance
            solver = SudokuSolverFactory.getInstance();
            creator = SudokuGeneratorFactory.getInstance();
        } else {
            // use the default solver
            solver = SudokuSolverFactory.getDefaultSolverInstance();
            creator = SudokuGeneratorFactory.getDefaultGeneratorInstance();
        }
        while (dlg == null || ! Thread.currentThread().isInterrupted()) {
            sudoku = creator.generateSudoku(true);
            if (sudoku == null) {
                // impossible to create sudoku due to an invalid pattern
                return null;
            }
            Sudoku2 solvedSudoku = sudoku.clone();
            boolean ok = solver.solve(level, solvedSudoku, true, null, false, 
                    Options.getInstance().solverSteps, mode);
            boolean containsTrainingStep = true;
            if (mode != GameMode.PLAYING) {
                containsTrainingStep = false;
                List<SolutionStep> steps = solver.getSteps();
                for (SolutionStep step : steps) {
                    if (step.getType().getStepConfig().isEnabledTraining()) {
                        containsTrainingStep = true;
                        break;
                    }
                }
            }
            if (ok && containsTrainingStep && 
                    (solvedSudoku.getLevel().getOrdinal() == level.getOrdinal()
                    || mode == GameMode.LEARNING)) {
                sudoku.setLevel(solvedSudoku.getLevel());
                sudoku.setScore(solvedSudoku.getScore());
                break;
            }
            setAnz(getAnz() + 1);
            if (dlg != null) {
                if ((System.currentTimeMillis() - actMillis) > 500) {
                    actMillis = System.currentTimeMillis();
                    EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            progressDialog.updateProgressLabel();
                            //progressLabel.setText(Integer.toString(getAnz()));
                        }
                    });
                }
            } else {
                if (getAnz() > MAX_TRIES) {
                    // give up...
                    sudoku = null;
                    break;
                }
            }
        }
        if (dlg == null) {
            // give everything back
            SudokuGeneratorFactory.giveBack(creator);
            SudokuSolverFactory.giveBack(solver);
        }
        return sudoku;
    }

    /**
     * @return the anz
     */
    public synchronized int getAnz() {
        return anz;
    }

    /**
     * @param anz the anz to set
     */
    public synchronized void setAnz(int anz) {
        this.anz = anz;
    }
}
