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
package sudoku;

import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import solver.SudokuSolver;
import solver.SudokuSolverFactory;

/**
 * Checks the actual progress a sudoku has made. It should only be
 * used for sudokus, that are valid (have a unique solution).<br>
 * 
 * The progress checker runs in its own background thread. It is
 * invoked every time, the sudoku changes in the GUI. It then solves
 * the sudoku and sets the current level and score in the {@link MainFrame}.
 * 
 * @author hobiwan
 */
public class ProgressChecker implements Runnable {
    /** A copy of the sudoku that should be checked. */
    private Sudoku2 sudoku = new Sudoku2();
    /** The sudoku that has been passedin */
    private Sudoku2 passedInSudoku = new Sudoku2();
    /** a flag that indicates, that a new sudoku has been passed in */
    private boolean passedIn = false;
    /** the background Thread */
    private Thread thread;
    /** a flag that indicates, if the thread has been started yet */
    private boolean threadStarted = false;
    /** a reference to the {@link MainFrame}. */
    private MainFrame mainFrame = null;
    /** the solver to be used for the check */
    private SudokuSolver solver = null;
    
    /**
     * Creates a new instance of ProgressChecker. The thread is
     * created, but not started yet.
     * 
     * @param mainFrame 
     */
    public ProgressChecker(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        thread = new Thread(this);
    }

    /**
     * Schedules a new check. If the thread is not yet running,
     * it is started. The sudoku is stored and the thread is signalled.
     * Since it is possible, that the thread is still busy with another
     * check run, a flag is set as well.
     * 
     * @param actSudoku 
     */
    public void startCheck(Sudoku2 actSudoku) {
        if (thread == null) {
            return;
        }
        if (! threadStarted) {
            thread.start();
            threadStarted = true;
        }
        synchronized(thread) {
            // store the contents of the sudoku to check
            passedInSudoku.set(actSudoku);
//            System.out.println("ProgressChecker: pass in \r\n"+passedInSudoku.getSudoku(ClipboardMode.PM_GRID));
            // set a flag indicating a newly scheduled check
            passedIn = true;
            // wake up the thread, if it is sleeping
            thread.notify();
        }
    }
    
    /**
     * The background thread for the checks: If a new check is
     * scheduled, a {@link SudokuSolver} instance is obtained and
     * the sudoku is solved. The result is stored directly in the
     * {@link #mainFrame}.
     */
    @Override
    public void run() {
        while (! thread.isInterrupted()) {
            try {
                synchronized(thread) {
                    if (passedIn == false) {
                        thread.wait();
                    }
                    if (passedIn) {
                        sudoku.set(passedInSudoku);
                        sudoku.checkSudoku();
                        passedIn = false;
                    } else {
                        continue;
                    }
                }
//                System.out.println("ProgressChecker: run\r\n"+ sudoku.getSudoku(ClipboardMode.PM_GRID));
                // ok,a new check is to be made; is the sudoku valid?
                if (sudoku.getStatus() != SudokuStatus.VALID) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Progress check scheduled for invalid sudoku ({0})",sudoku.getSudoku(ClipboardMode.LIBRARY));
//                    System.out.println("   INVALID!");
                    continue;
                }
                // get a solver and do it
                if (solver == null) {
                    solver = SudokuSolverFactory.getInstance();
                }
                solver.setSudoku(sudoku);
                if (solver.solve()) {
                    mainFrame.setCurrentLevel(sudoku.getLevel());
                    mainFrame.setCurrentScore(sudoku.getScore());
//                    System.out.println("   " + sudoku.getLevel().getName() + ", " + sudoku.getScore());
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            mainFrame.setProgressLabel();
                        }
                    });
                } else {
                    //System.out.println("ProgressChecker: No solution found");
                }
                SudokuSolverFactory.giveBack(solver);
                solver = null;
            } catch (InterruptedException ex) {
                thread.interrupt();
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error checking progress", ex);
            }
        }
    }
}
