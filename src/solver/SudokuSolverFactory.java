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
import java.util.Iterator;
import java.util.List;

/**
 * HoDoKu uses one instance of class {@link SudokuSolver} from within the
 * GUI. This instance is called the <b>defaultSolver</b>. For use in background
 * threads an arbitrary number of additional solver instances can be gotten
 * from this factory.<br>
 * Solvers have to be released after they are used.
 * 
 * @author hobiwan
 */
public class SudokuSolverFactory {
    /** The <b>defaultSolver</b> for use by the GUI. */
    private static final SudokuSolver defaultSolver = new SudokuSolver();
    /** All SudokuSolver instances created by this factory. */
    private static List<SolverInstance> instances = new ArrayList<SolverInstance>();
    /** A background thread that cleans up unused SudokuSolver instances. */
    private static final Thread thread = new Thread( new Runnable() {
        @Override
        @SuppressWarnings("SleepWhileInLoop")
        public void run() {
            while (true) {
                synchronized (thread) {
                    // cleanup for defaultSolver
                    defaultSolver.getStepFinder().cleanUp();
                    
                    // now check all other solvers
                    Iterator<SolverInstance> iterator = instances.iterator();
                    while (iterator.hasNext()) {
                        SolverInstance act = iterator.next();
                        if (act.inUse == false && (System.currentTimeMillis() - act.lastUsedAt) > SOLVER_TIMEOUT) {
                            iterator.remove();
                        } else {
                            act.instance.getStepFinder().cleanUp();
                        }
                    }
                }
                try {
                    Thread.sleep(SOLVER_TIMEOUT);
                } catch (InterruptedException ex) {
                    // do nothing
                }
            }
        }
    });
    /** The default cleanup time for SudokuSolver instances. */
    private static final long SOLVER_TIMEOUT = 5 * 60 * 1000;

    /**
     * One entry in {@link #instances}.
     */
    private static class SolverInstance {
        /** The solver held in this entry. */
        SudokuSolver instance = null;
        /** <code>true</code>, if the solver has been handed out by the factory. */
        boolean inUse = true;
        /** Last time the solver was returned to the factory. */
        long lastUsedAt = -1;

        /**
         * Create a new entry for {@link #instances}.
         * @param instance
         */
        private SolverInstance(SudokuSolver instance) {
            this.instance = instance;
        }
    }

    /** Start the thread */
    static {
        thread.start();
    }

    /**
     * This class is a utility class that cannot be instantiated.
     */
    private SudokuSolverFactory() { /* class cannot be instantiated! */ }

    /** Get the {@link #defaultSolver}.
     * @return 
     */
    public static SudokuSolver getDefaultSolverInstance() {
        return defaultSolver;
    }

    /**
     * Hand out an ununsed solver or create a new one if necessary.
     * @return
     */
    public static SudokuSolver getInstance() {
        SudokuSolver ret = null;
        synchronized (thread) {
            for (SolverInstance act : instances) {
                if (act.inUse == false) {
                    act.inUse = true;
                    ret = act.instance;
                    break;
                }
            }
            if (ret == null) {
                ret = new SudokuSolver();
                instances.add(new SolverInstance(ret));
            }
        }
        return ret;
    }

    /**
     * Gives a solver back to the factory.
     * @param solver
     */
    public static void giveBack(SudokuSolver solver) {
        synchronized (thread) {
            for (SolverInstance act : instances) {
                if (act.instance == solver) {
                    act.inUse = false;
                    act.lastUsedAt = System.currentTimeMillis();
                    break;
                }
            }
        }
    }
}
