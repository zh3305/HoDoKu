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

import java.util.logging.Level;
import java.util.logging.Logger;
import sudoku.DifficultyLevel;
import sudoku.DifficultyType;
import sudoku.GameMode;
import sudoku.Options;
import sudoku.StepConfig;

/**
 * One instance of this class is used to control the background creation
 * of sudokus. The following puzzles are created automatically and
 * stored in {@link Options}:
 * <ul>
 *  <li>10 sudokus for every difficulty level</li>
 *  <li>10 sudokus for {@link GameMode#LEARNING}</li>
 *  <li>10 sudokus for {@link GameMode#PRACTISING}</li>
 * </ul>
 * The puzzle creation is triggered by the following events:
 * <ul>
 *  <li>At program startup: missing puzzles are created (starting with the current {@link DifficultyLevel})</li>
 *  <li>When the step configuration has been changed: all types of puzzles are created again.</li>
 *  <li>When the training steps configuration has changed: <code>LEARNING</code> and <code>PRACTISING</code> puzzles are redone</li>
 *  <li>When the {@link DifficultyLevel} is changed in the GUI: <code>PRACTISING</code> puzzles are redone</li>
 *  <li>When a configuration is loaded from a file, all puzzles are redone</li>
 * </ul>
 * This class is a singleton.
 * 
 * @author hobiwan
 */
public class BackgroundGeneratorThread implements Runnable {
    /** Debug flag */
    private static final boolean DEBUG = false;
    /** The singleton instance */
    private static BackgroundGeneratorThread instance = null;
    /** the actual creator */
    private BackgroundGenerator generator;
    /** The creation thread */
    private final Thread thread;
    /** a flag that indicates, that a new sudoku has been passed in */
    private boolean newRequest = false;
    /** a flag that indicates, if the thread has been started yet */
    private boolean threadStarted = false;
    
    /**
     * Creates an instance.
     */
    private BackgroundGeneratorThread() {
        thread = new Thread(this);
        generator = new BackgroundGenerator();
    }
    
    /**
     * Retrieves the singleton instance, generates it if necessary.
     * 
     * @return 
     */
    public static BackgroundGeneratorThread getInstance() {
        if (instance == null) {
            instance = new BackgroundGeneratorThread();
        }
        return instance;
    }

    /**
     * Checks, if a puzzle matching the requirements is available.
     * 
     * @param level
     * @param mode
     * @return 
     */
    public synchronized String getSudoku(DifficultyLevel level, GameMode mode) {
        // get the correct puzzles from Options
        String[] puzzles = getPuzzleArray(level, mode);
        // check if a puzzle is available
        String newPuzzle = null;
        if (puzzles[0] != null) {
            newPuzzle = puzzles[0];
            for (int i = 1; i < puzzles.length; i++) {
                puzzles[i - 1] = puzzles[i];
            }
            puzzles[puzzles.length - 1] = null;
        }
        if (DEBUG) {
            System.out.println("Got puzzle from cache: " + level.getName() + "/" + mode.name() + "/" + newPuzzle);
        }
        // start a new run
        startCreation();
        // and give it back
        return newPuzzle;
    }
    
    /**
     * Writes a new sudoku into the cache.
     * 
     * @param level
     * @param mode
     * @param sudoku 
     */
    private synchronized void setSudoku(DifficultyLevel level, GameMode mode, String sudoku) {
        // get the correct puzzles from Options
        String[] puzzles = getPuzzleArray(level, mode);
        for (int i = 0; i < puzzles.length; i++) {
            if (puzzles[i] == null) {
                puzzles[i] = sudoku;
                break;
            }
        }
    }
    
    /**
     * The step configuration has been changed:
     * reset everything and start over.
     */
    public synchronized void resetAll() {
        String[][] puzzles = Options.getInstance().getNormalPuzzles();
        for (int i = 0; i < puzzles.length; i++) {
            for (int j = 0; j < puzzles[i].length; j++) {
                puzzles[i][j] = null;
            }
        }
        resetTrainingPractising();
    }
    
    /**
     * The training configuration has changed: recreate the
     * LEARNING and PRACTISING puzzles and start over.
     */
    public synchronized void resetTrainingPractising() {
        String[] puzzles1 = Options.getInstance().getLearningPuzzles();
        for (int i = 0; i < puzzles1.length; i++) {
            puzzles1[i] = null;
        }
        puzzles1 = Options.getInstance().getPractisingPuzzles();
        for (int i = 0; i < puzzles1.length; i++) {
            puzzles1[i] = null;
        }
        startCreation();
    }
    
    /**
     * The level has been changed, check if the PRACTISING puzzles
     * have to be recreated.
     * 
     * @param newLevel 
     */
    public synchronized void setNewLevel(int newLevel) {
        int maxTrainingLevel = getTrainingLevel();
        if (maxTrainingLevel == -1 || newLevel < maxTrainingLevel) {
            // we cant create suitable puzzles -> ignore
            return;
        }
        if (newLevel == Options.getInstance().getPractisingPuzzlesLevel()) {
            // nothing to do!
            return;
        }
        String[] puzzles = Options.getInstance().getPractisingPuzzles();
        for (int i = 0; i < puzzles.length; i++) {
            puzzles[i] = null;
        }
        Options.getInstance().setPractisingPuzzlesLevel(newLevel);
        startCreation();
    }
    
    /**
     * Schedules a new creation run. If the thread is not yet running,
     * it is started. The thread is signalled.
     * Since it is possible, that the thread is still busy with another
     * run, a flag is set as well.
     */
    public void startCreation() {
        if (thread == null) {
            return;
        }
        if (! threadStarted) {
            thread.start();
            threadStarted = true;
            if (DEBUG) {
                System.out.println("BackgroundCreationThread started!");
            }
        }
        synchronized(thread) {
            // set a flag indicating a newly scheduled check
            newRequest = true;
            if (DEBUG) {
                System.out.println("new creation request scheduled!");
            }
            // wake up the thread, if it is sleeping
            thread.notify();
        }
    }
    
    /**
     * The main thread: If it is signalled it checks, which type of puzzle
     * is missing. As long as a missing puzzle type is found, the creation is
     * continued.
     */
    @Override
    public void run() {
        while (!thread.isInterrupted()) {
            try {
                synchronized (thread) {
                    if (newRequest == false) {
                        thread.wait();
                    }
                    if (newRequest) {
                        newRequest = false;
                    } else {
                        continue;
                    }
                }
                if (DEBUG) {
                    System.out.println("Creation starting...");
                }
                DifficultyLevel level = null;
                GameMode mode = null;
                while (level == null && !thread.isInterrupted()) {
                    // get the next puzzle
                    synchronized (this) {
                        // find out, what to do
                        String[][] puzzles = Options.getInstance().getNormalPuzzles();
                        for (int i = 0; i < puzzles.length; i++) {
                            for (int j = 0; j < puzzles[i].length; j++) {
                                if (puzzles[i][j] == null) {
                                    if (DEBUG) {
                                        System.out.println("found level: "+ (i + 1));
                                    }
                                    level = Options.getInstance().getDifficultyLevel(i + 1);
                                    mode = GameMode.PLAYING;
                                    if (DEBUG) {
                                        System.out.println("   " + level.getName()+ "/" + mode.name());
                                    }
                                    break;
                                }
                            }
                            if (level != null) {
                                break;
                            }
                        }
                        int trLevel = getTrainingLevel();
                        String[] puzzles1 = Options.getInstance().getLearningPuzzles();
                        if (level == null && trLevel != -1) {
                            for (int i = 0; i < puzzles1.length; i++) {
                                if (puzzles1[i] == null) {
                                    if (DEBUG) {
                                        System.out.println("found level: "+ (i + 1));
                                    }
                                    level = Options.getInstance().getDifficultyLevel(DifficultyType.EXTREME.ordinal());
                                    mode = GameMode.LEARNING;
                                    if (DEBUG) {
                                        System.out.println("   " + level.getName()+ "/" + mode.name());
                                    }
                                    break;
                                }
                            }
                        }
                        if (trLevel != -1 && Options.getInstance().getPractisingPuzzlesLevel() == -1) {
                            setNewLevel(Options.getInstance().getActLevel());
                        }
                        puzzles1 = Options.getInstance().getPractisingPuzzles();
                        if (DEBUG) {
                            System.out.println("looking for pract: " + level + "/" + trLevel + "/" + Options.getInstance().getActLevel() + "/" + Options.getInstance().getPractisingPuzzlesLevel());
                        }
                        if (level == null && trLevel != -1 && Options.getInstance().getActLevel() >= trLevel) {
                            for (int i = 0; i < puzzles1.length; i++) {
                                if (puzzles1[i] == null) {
                                    if (DEBUG) {
                                        System.out.println("found level: " + (i + 1));
                                    }
                                    level = Options.getInstance().getDifficultyLevel(Options.getInstance().getPractisingPuzzlesLevel());
                                    mode = GameMode.PRACTISING;
                                    if (DEBUG) {
                                        System.out.println("   " + level.getName()+ "/" + mode.name());
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    //new puzzle type found?
                    if (level == null) {
                        // we are done for now
                        if (DEBUG) {
                            System.out.println("creation: nothing to do!");
                        }
                        break;
                    }
                    if (DEBUG) {
                        System.out.println("  creating " + level.getName() + "/" + mode.name());
                    }
                    // ok, create the puzzle
                    String puzzle = generator.generate(level, mode);
                    if (puzzle == null) {
                        //couldnt create one -> stop for now
                        // BUG: dont give up just now!
                        if (DEBUG) {
                            System.out.println("couldnt find suitable puzzles, retrying!");
                        }
                        break;
                    }
                    // store it
                    setSudoku(level, mode, puzzle);
                    if (DEBUG) {
                        System.out.println("  created in background: " + level.getName() + "/" + mode.name() + "/" + puzzle);
                    }
                    // and try again
                    level = null;
                    mode = null;
                }
                if (DEBUG) {
                    System.out.println("Done (level = " + level + ", isInterrupted() = " + thread.isInterrupted() + ")!");
                }
            } catch (InterruptedException ex) {
                thread.interrupt();
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error checking progress", ex);
            }
        }
    }
    
    /**
     * Gets the correct cache array from Options.
     * 
     * @param level
     * @param mode
     * @return 
     */
    private String[] getPuzzleArray(DifficultyLevel level, GameMode mode) {
        String[] puzzles = null;
        switch (mode) {
            case PLAYING:
                puzzles = Options.getInstance().getNormalPuzzles()[level.getOrdinal() - 1];
                break;
            case LEARNING:
                puzzles = Options.getInstance().getLearningPuzzles();
                break;
            case PRACTISING:
                puzzles = Options.getInstance().getPractisingPuzzles();
                break;
        }
        return puzzles;
    }
    
    /**
     * Utility method: gets the {@link DifficultyLevel} of the most difficult
     * training step. If no training step is set, -1 is returned.<br>
     * This method is used in two ways: To decide, if LEARNING/PRACTISING steps
     * should be created at all (if no trainig step is enabled, creation will
     * be impossible), and to decide, if PRACTISING steps have to be
     * redone after a change of the games current DifficultyLevel (if the
     * current level is lower than the level of the hardest training step,
     * no new PRACTISING puzzles have to be created).
     * 
     * @return 
     */
    private int getTrainingLevel() {
        StepConfig[] conf = Options.getInstance().getOrgSolverSteps();
        int level = -1;
        for (StepConfig act : conf) {
            if (act.isEnabledTraining()) {
                int actLevel = act.getLevel();
                if (actLevel > level) {
                    level = actLevel;
                }
            }
        }
        return level;
    }
}
