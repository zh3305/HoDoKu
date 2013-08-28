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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import sudoku.FindAllStepsProgressDialog;
import sudoku.SolutionStep;
import sudoku.SolutionType;
import sudoku.StepConfig;
import sudoku.Sudoku2;
import sudoku.SudokuSet;
import sudoku.SudokuSetBase;

/**
 * This class has two purposes:
 * <ol>
 * <li>It holds all configuration data for the specializes solvers and
 * handles lazy initialization</li>
 * <li>It caches data needed by more than one solver (e.g. ALS and RCs)</li>
 * <li>It exposes the public API of the specialized solvers to the
 * rest of the program.</li>
 * </ol>
 * 
 * @author hobiwan
 */
public class SudokuStepFinder {

    /** The specialized solver for Singles, Intersections and Subsets. */
    private SimpleSolver simpleSolver;
    /** The specialized solver for all kinds of Fish. */
    private FishSolver fishSolver;
    /** The specialized solver for single digit patterns. */
    private SingleDigitPatternSolver singleDigitPatternSolver;
    /** The specialized solver for all kinds of Uniqueness techniques. */
    private UniquenessSolver uniquenessSolver;
    /** The specialized solver for Wings. */
    private WingSolver wingSolver;
    /** The specialized solver for Coloring. */
    private ColoringSolver coloringSolver;
    /** The specialized solver for simple chains. */
    private ChainSolver chainSolver;
    /** The specialized solver for ALS moves. */
    private AlsSolver alsSolver;
    /** The specialized solver for SDC. */
    private MiscellaneousSolver miscellaneousSolver;
    /** The specialized solver for complicated chains. */
    private TablingSolver tablingSolver;
    /** The specialized solver for Templates. */
    private TemplateSolver templateSolver;
    /** The specialized solver for guessing. */
    private BruteForceSolver bruteForceSolver;
    /** The specialized solver for Incomplete Solutions. */
    private IncompleteSolver incompleteSolver;
    /** The specialized solver for giving up. */
    private GiveUpSolver giveUpSolver;
    /** An array for all specialized solvers. Makes finding steps easier. */
    private AbstractSolver[] solvers;
    /** The sudoku for which steps should be found. */
    private Sudoku2 sudoku;
    /** The step configuration for searches. */
    private StepConfig[] stepConfigs;
    /** A status counter that changes every time a new step has been found. Specialized
     *  solvers can use this counter to use cached steps instead of searching for them
     *  if no step was found since the last search.
     */
    private int stepNumber = 0;
    /** for timing */
    private long templateNanos;
    /** for timing */
    private int templateAnz;
    /** Lazy initialization: The solvers are only created when they are used. */
    private boolean initialized = false;
    /** If set to <code>true</code>, the StepFinder contains only one {@link SimpleSolver} instance. */
    private boolean simpleOnly = false;
    // Data that is used by more than one specialized solver
    /** One set with all positions left for each candidate. */
    private SudokuSet[] candidates = new SudokuSet[10];
    /** Dirty flag for candidates. */
    private boolean candidatesDirty = true;
    /** One set with all set cells for each candidate. */
    private SudokuSet[] positions = new SudokuSet[10];
    /** Dirty flag for positions. */
    private boolean positionsDirty = true;
    /** One set with all cells where a candidate is still possible */
    private SudokuSet[] candidatesAllowed = new SudokuSet[10];
    /** Dirty flag for candidatesAllowed. */
    private boolean candidatesAllowedDirty = true;
    /** A set for all cells that are not set yet */
    private SudokuSet emptyCells = new SudokuSet();
    /** One template per candidate with all positions that can be set immediately. */
    private SudokuSet[] setValueTemplates = new SudokuSet[10];
    /** One template per candidate with all positions from which the candidate can be eliminated immediately. */
    private SudokuSet[] delCandTemplates = new SudokuSet[10];
    /** The lists with all valid templates for each candidate. */
    private List<List<SudokuSetBase>> candTemplates;
    /** Dirty flag for templates (without refinements). */
    private boolean templatesDirty = true;
    /** Dirty flag for templates (with refinements). */
    private boolean templatesListDirty = true;
    /** Cache for group nodes. */
    private List<GroupNode> groupNodes = null;
    /** Step number for which {@link #groupNodes} was computed. */
    private int groupNodesStepNumber = -1;
    /** Cache for ALS entries (only ALS with more than one cell). */
    private List<Als> alsesOnlyLargerThanOne = null;
    /** Step number for which {@link #alsesOnlyLargerThanOne} was computed. */
    private int alsesOnlyLargerThanOneStepNumber = -1;
    /** Cache for ALS entries (ALS with one cell allowed). */
    private List<Als> alsesWithOne = null;
    /** Step number for which {@link #alsesWithOne} was computed. */
    private int alsesWithOneStepNumber = -1;
    /** Cache for RC entries. */
    private List<RestrictedCommon> restrictedCommons = null;
    /** start indices into {@link #restrictedCommons} for all ALS. */
    private int[] startIndices = null;
    /** end indices into {@link #restrictedCommons} for all ALS. */
    private int[] endIndices = null;
    /** Overlap status at last RC search. */
    private boolean lastRcAllowOverlap;
    /** Step number for which {@link #restrictedCommons} was computed. */
    private int lastRcStepNumber = -1;
    /** ALS list for which RCs were calculated. */
    private List<Als> lastRcAlsList = null;
    /** Was last RC search only for forward references? */
    private boolean lastRcOnlyForward = true;
    /** Collect RCs for forward search only */
    private boolean rcOnlyForward = true;
    // temporary varibles for calculating ALS and RC
    /** Temporary set for recursion: all cells of each try */
    private SudokuSet indexSet = new SudokuSet();
    /** Temporary set for recursion: all numbers contained in {@link #indexSet}. */
    private short[] candSets = new short[10];
    /** statistics: total time for all calls */
    private long alsNanos;
    /** statistics: number of calls */
    private int anzAlsCalls;
    /** statistics: number of ALS found */
    private int anzAls;
    /** statistics: number of ALS found more than once */
    private int doubleAls;
    /** All candidates common to two ALS. */
    private short possibleRestrictedCommonsSet = 0;
    /** Holds all buddies of all candidate cells for one RC (including the candidate cells themselves). */
    private SudokuSet restrictedCommonBuddiesSet = new SudokuSet();
    /** All cells containing a specific candidate in two ALS. */
    private SudokuSet restrictedCommonIndexSet = new SudokuSet();
    /** Contains the indices of all overlapping cells in two ALS. */
    private SudokuSet intersectionSet = new SudokuSet();
    /** statistics: total time for all calls */
    private long rcNanos;
    /** statistics: number of calls */
    private int rcAnzCalls;
    /** statistics: number of RCs found */
    private int anzRcs;

    /**
     * Creates an instance of the class.
     */
    public SudokuStepFinder() {
        this(false);
    }

    /**
     * Creates an instance of the class.
     * @param simpleOnly If set, the StepFinder contains only an instance of SimpleSolver
     */
    public SudokuStepFinder(boolean simpleOnly) {
        this.simpleOnly = simpleOnly;
        initialized = false;
    }

    private void initialize() {
        if (initialized) {
            return;
        }
        // Create all Sets
        for (int i = 0; i < candidates.length; i++) {
            candidates[i] = new SudokuSet();
            positions[i] = new SudokuSet();
            candidatesAllowed[i] = new SudokuSet();
        }
        // Create all templates
        candTemplates = new ArrayList<List<SudokuSetBase>>(10);
        for (int i = 0; i < setValueTemplates.length; i++) {
            setValueTemplates[i] = new SudokuSet();
            delCandTemplates[i] = new SudokuSet();
            candTemplates.add(i, new LinkedList<SudokuSetBase>());
        }
        // Create the solvers
        simpleSolver = new SimpleSolver(this);
        if (!simpleOnly) {
            fishSolver = new FishSolver(this);
            singleDigitPatternSolver = new SingleDigitPatternSolver(this);
            uniquenessSolver = new UniquenessSolver(this);
            wingSolver = new WingSolver(this);
            coloringSolver = new ColoringSolver(this);
            chainSolver = new ChainSolver(this);
            alsSolver = new AlsSolver(this);
            miscellaneousSolver = new MiscellaneousSolver(this);
            tablingSolver = new TablingSolver(this);
            templateSolver = new TemplateSolver(this);
            bruteForceSolver = new BruteForceSolver(this);
            incompleteSolver = new IncompleteSolver(this);
            giveUpSolver = new GiveUpSolver(this);
            solvers = new AbstractSolver[]{
                simpleSolver, fishSolver, singleDigitPatternSolver, uniquenessSolver,
                wingSolver, coloringSolver, chainSolver, alsSolver, miscellaneousSolver,
                tablingSolver, templateSolver, bruteForceSolver,
                incompleteSolver, giveUpSolver
            };
        } else {
            solvers = new AbstractSolver[]{simpleSolver};
        }
        initialized = true;
    }

    /**
     * Calls the {@link AbstractSolver#cleanUp() } method for every
     * specialized solver. This method is called from an extra
     * thread from within {@link SudokuSolverFactory}. No synchronization
     * is done here to speed things up, if the functionality is not used.<br>
     * 
     * Specialized solvers, that use cleanup, have to implement synchronization
     * themselves.
     */
    public void cleanUp() {
        if (solvers == null) {
            return;
        }
        for (AbstractSolver solver : solvers) {
            solver.cleanUp();
        }
    }

    /**
     * Gets the next step of type <code>type</code>.
     * @param type
     * @return
     */
    public SolutionStep getStep(SolutionType type) {
        initialize();
        SolutionStep result = null;
        for (int i = 0; i < solvers.length; i++) {
            if ((result = solvers[i].getStep(type)) != null) {
                // step has been found!
                stepNumber++;
                return result;
            }
        }
        return result;
    }

    /**
     * Executes a step.
     * @param step
     */
    public void doStep(SolutionStep step) {
        initialize();
        for (int i = 0; i < solvers.length; i++) {
            if (solvers[i].doStep(step)) {
                setSudokuDirty();
                return;
            }
        }
        throw new RuntimeException("Invalid solution step in doStep() (" + step.getType() + ")");
    }

    /**
     * The sudoku has been changed, all precalculated data is now invalid.
     */
    public void setSudokuDirty() {
        candidatesDirty = true;
        candidatesAllowedDirty = true;
        positionsDirty = true;
        templatesDirty = true;
        templatesListDirty = true;
        stepNumber++;
    }

    /**
     * Stes a new sudoku.
     * @param sudoku
     */
    public void setSudoku(Sudoku2 sudoku) {
        if (sudoku != null && this.sudoku != sudoku) {
            this.sudoku = sudoku;
        }
        // even if the reference is the same, the content could have been changed
        setSudokuDirty();
    }

    /**
     * Gets the sudoku.
     * @return
     */
    public Sudoku2 getSudoku() {
        return sudoku;
    }

    /**
     * Sets the stepConfigs.
     * @param stepConfigs
     */
    public void setStepConfigs(StepConfig[] stepConfigs) {
        this.stepConfigs = stepConfigs;
    }

    /**
     * Get the {@link TablingSolver}.
     * @return
     */
    protected TablingSolver getTablingSolver() {
        return tablingSolver;
    }

    /******************************************************************************************************************/
    /* EXPOSE PUBLIC APIs                                                                                             */
    /******************************************************************************************************************/
    /**
     * Finds all Full Houses for a given sudoku.
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllFullHouses(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = simpleSolver.findAllFullHouses();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all Naked Singles for a given sudoku.
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllNakedSingles(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = simpleSolver.findAllNakedSingles();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all Naked Subsets for a given sudoku.
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllNakedXle(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = simpleSolver.findAllNakedXle();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all Hidden Singles for a given sudoku.
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllHiddenSingles(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = simpleSolver.findAllHiddenSingles();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Find all hidden Subsets.
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllHiddenXle(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = simpleSolver.findAllHiddenXle();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all Locked Candidates for a given sudoku.
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllLockedCandidates(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = simpleSolver.findAllLockedCandidates();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all Locked Candidates Type 1 for a given sudoku.
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllLockedCandidates1(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = simpleSolver.findAllLockedCandidates();
        setSudoku(oldSudoku);
        // filter the steps
        List<SolutionStep> resultList = new ArrayList<SolutionStep>();
        for (SolutionStep step : steps) {
            if (step.getType().equals(SolutionType.LOCKED_CANDIDATES_1)) {
                resultList.add(step);
            }
        }
        return resultList;
    }

    /**
     * Finds all Locked Candidates Type 2 for a given sudoku.
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllLockedCandidates2(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = simpleSolver.findAllLockedCandidates();
        setSudoku(oldSudoku);
        // filter the steps
        List<SolutionStep> resultList = new ArrayList<SolutionStep>();
        for (SolutionStep step : steps) {
            if (step.getType().equals(SolutionType.LOCKED_CANDIDATES_2)) {
                resultList.add(step);
            }
        }
        return resultList;
    }

    /**
     * Finds all fishes of a given size and shape.
     * @param newSudoku
     * @param minSize
     * @param maxSize
     * @param maxFins
     * @param maxEndoFins
     * @param dlg
     * @param forCandidate
     * @param type
     * @return
     */
    public List<SolutionStep> getAllFishes(Sudoku2 newSudoku, int minSize, int maxSize,
            int maxFins, int maxEndoFins, FindAllStepsProgressDialog dlg, int forCandidate, int type) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = fishSolver.getAllFishes(minSize, maxSize, maxFins, maxEndoFins, dlg, forCandidate, type);
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all kraken fishes of a given size and shape.
     * @param newSudoku
     * @param minSize
     * @param maxSize
     * @param maxFins
     * @param maxEndoFins
     * @param dlg
     * @param forCandidate
     * @param type
     * @return
     */
    public List<SolutionStep> getAllKrakenFishes(Sudoku2 newSudoku, int minSize, int maxSize,
            int maxFins, int maxEndoFins, FindAllStepsProgressDialog dlg, int forCandidate, int type) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = fishSolver.getAllKrakenFishes(minSize, maxSize, maxFins, maxEndoFins, dlg, forCandidate, type);
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all Empty Rectangles
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllEmptyRectangles(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = singleDigitPatternSolver.findAllEmptyRectangles();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all Skyscrapers
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllSkyScrapers(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = singleDigitPatternSolver.findAllSkyscrapers();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all Two String Kites
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllTwoStringKites(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = singleDigitPatternSolver.findAllTwoStringKites();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all instances of all types of Uniqueness techniques
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> getAllUniqueness(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = uniquenessSolver.getAllUniqueness();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Find all kinds of Wings
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> getAllWings(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = wingSolver.getAllWings();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Find all Simple Colors
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllSimpleColors(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = coloringSolver.findAllSimpleColors();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Find all Multi Colors
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> findAllMultiColors(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = coloringSolver.findAllMultiColors();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Find all simple chains (X-Chain, XY-Chain, Remote Pairs, Turbot Fish).
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> getAllChains(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = chainSolver.getAllChains();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all ALS-XZ, ALS-XY and ALS-Chains.
     * @param newSudoku
     * @param doXz 
     * @param doXy 
     * @param doChain 
     * @return
     */
    public List<SolutionStep> getAllAlsSteps(Sudoku2 newSudoku, boolean doXz, boolean doXy, boolean doChain) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = alsSolver.getAllAlses(doXz, doXy, doChain);
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Get all Death Blossoms
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> getAllDeathBlossoms(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = alsSolver.getAllDeathBlossoms();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all Sue de Coqs
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> getAllSueDeCoqs(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = miscellaneousSolver.getAllSueDeCoqs();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all normal Nice Loops/AICs
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> getAllNiceLoops(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = tablingSolver.getAllNiceLoops();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Find all Grouped Nice Loops/AICs
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> getAllGroupedNiceLoops(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = tablingSolver.getAllGroupedNiceLoops();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all Forcing Chains
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> getAllForcingChains(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = tablingSolver.getAllForcingChains();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all Forcing Nets
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> getAllForcingNets(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = tablingSolver.getAllForcingNets();
        setSudoku(oldSudoku);
        return steps;
    }

    /**
     * Finds all Templates steps
     * @param newSudoku
     * @return
     */
    public List<SolutionStep> getAllTemplates(Sudoku2 newSudoku) {
        initialize();
        Sudoku2 oldSudoku = getSudoku();
        setSudoku(newSudoku);
        List<SolutionStep> steps = templateSolver.getAllTemplates();
        setSudoku(oldSudoku);
        return steps;
    }

    /******************************************************************************************************************/
    /* END EXPOSE PUBLIC APIs                                                                                         */
    /******************************************************************************************************************/
    /******************************************************************************************************************/
    /* SETS                                                                                                           */
    /******************************************************************************************************************/
    /**
     * Returns the {@link #candidates}. Recalculates them if they are dirty.
     * @return
     */
    public SudokuSet[] getCandidates() {
        if (candidatesDirty) {
            initCandidates();
        }
        return candidates;
    }

    /**
     * Returns the {@link #positions}. Recalculates them if they are dirty.
     * @return
     */
    public SudokuSet[] getPositions() {
        if (positionsDirty) {
            initPositions();
        }
        return positions;
    }

    /**
     * Create the sets that contain all cells, in which a specific candidate is still present.
     */
    private void initCandidates() {
        if (candidatesDirty) {
            for (int i = 1; i < candidates.length; i++) {
                candidates[i].clear();
            }
            short[] cells = sudoku.getCells();
            for (int i = 0; i < cells.length; i++) {
                int[] cands = Sudoku2.POSSIBLE_VALUES[cells[i]];
                for (int j = 0; j < cands.length; j++) {
                    candidates[cands[j]].add(i);
                }
            }
            candidatesDirty = false;
        }
    }

    /**
     * Create the sets that contain all cells, in which a specific candidate is already set.
     */
    private void initPositions() {
        if (positionsDirty) {
            for (int i = 1; i < positions.length; i++) {
                positions[i].clear();
            }
            int[] values = sudoku.getValues();
            for (int i = 0; i < values.length; i++) {
                if (values[i] != 0) {
                    positions[values[i]].add(i);
                }
            }
            positionsDirty = false;
        }
    }

    /**
     * Returns the {@link #candidatesAllowed}. Recalculates them if they are dirty.
     * @return
     */
    public SudokuSet[] getCandidatesAllowed() {
        if (candidatesAllowedDirty) {
            initCandidatesAllowed();
        }
        return candidatesAllowed;
    }

    /**
     * Returns the {@link #emptyCells}. Recalculates them if they are dirty.
     * @return
     */
    public SudokuSet getEmptyCells() {
        if (candidatesAllowedDirty) {
            initCandidatesAllowed();
        }
        return emptyCells;
    }

    /**
     * Create the sets that contain all cells, in which a specific candidate is still valid.
     */
    private void initCandidatesAllowed() {
        if (candidatesAllowedDirty) {
            emptyCells.setAll();
            for (int i = 1; i < candidatesAllowed.length; i++) {
                candidatesAllowed[i].setAll();
            }
            int[] values = sudoku.getValues();
            for (int i = 0; i < values.length; i++) {
                if (values[i] != 0) {
                    candidatesAllowed[values[i]].andNot(Sudoku2.buddies[i]);
                    emptyCells.remove(i);
                }
            }
            for (int i = 1; i < candidatesAllowed.length; i++) {
                candidatesAllowed[i].and(emptyCells);
            }
            candidatesAllowedDirty = false;
        }
    }

    /******************************************************************************************************************/
    /* END SETS                                                                                                       */
    /******************************************************************************************************************/
    /******************************************************************************************************************/
    /* TEMPLATES                                                                                                      */
    /******************************************************************************************************************/
    /**
     * Returns delCandTemplates.
     * @param initLists
     * @return
     */
    protected SudokuSet[] getDelCandTemplates(boolean initLists) {
        if ((initLists && templatesListDirty) || (!initLists && templatesDirty)) {
            initCandTemplates(initLists);
        }
        return delCandTemplates;
    }

    /**
     * Returns setValueTemplates.
     * @param initLists
     * @return
     */
    protected SudokuSet[] getSetValueTemplates(boolean initLists) {
        if ((initLists && templatesListDirty) || (!initLists && templatesDirty)) {
            initCandTemplates(initLists);
        }
        return setValueTemplates;
    }

    /**
     * Initializiation of templates:
     *
     * The following templates are forbidden and will be ignored:
     *   All templates which have no 1 at at least one already set position
     *    (positions & template) != positions
     *   All templats which have at least one 1 at a position thats already forbidden
     *    (~(positions | allowedPositions) & template) != 0
     *
     * When the valid templates are known:
     *   All valid templates OR: Candidate can be eliminated from all positions that are 0
     *   All templates AND: Candidate can be set in all cells that have a 1 left
     *   Calculate all valid combinations of templates for two different candidates (OR),
     *      AND all results: Gives Hidden Pairs (eliminate all candidates from the result,
     *      that dont belong to the two start candidates). - not implemented yet
     *
     * If <code>initLists</code> is set make the following additions (for {@link TemplateSolver}):
     * All templates, that have a one at the result of an AND of all templates of another candidate, are forbidden
     * All templates, that dont have at least one non overlapping combination with at least one template
     *    of another candidate, are forbidden.
     * @param initLists
     */
    private void initCandTemplates(boolean initLists) {
///*K*/ Not here!!!
//        if (! Options.getInstance().checkTemplates) {
//            return;
//        }
        templateAnz++;
        long nanos = System.nanoTime();
        if ((initLists && templatesListDirty) || (!initLists && templatesDirty)) {
            SudokuSetBase[] allowedPositions = getCandidates();
            SudokuSet[] setPositions = getPositions();
            SudokuSetBase[] templates = Sudoku2.templates;
            SudokuSetBase[] forbiddenPositions = new SudokuSetBase[10]; // eine 1 an jeder Position, an der Wert nicht mehr sein darf

//        SudokuSetBase setMask = new SudokuSetBase();
//        SudokuSetBase delMask = new SudokuSetBase();
//        SudokuSetBase temp = new SudokuSetBase();
            for (int i = 1; i <= 9; i++) {
                setValueTemplates[i].setAll();
                delCandTemplates[i].clear();
                candTemplates.get(i).clear();

                // eine 1 an jeder verbotenen Position ~(positions | allowedPositions)
                forbiddenPositions[i] = new SudokuSetBase();
                forbiddenPositions[i].set(setPositions[i]);
                forbiddenPositions[i].or(allowedPositions[i]);
                forbiddenPositions[i].not();
            }
            for (int i = 0; i < templates.length; i++) {
                for (int j = 1; j <= 9; j++) {
                    if (!setPositions[j].andEquals(templates[i])) {
                        // Template hat keine 1 an einer bereits gesetzten Position
                        continue;
                    }
                    if (!forbiddenPositions[j].andEmpty(templates[i])) {
                        // Template hat eine 1 an einer verbotenen Position
                        continue;
                    }
                    // Template ist für Kandidaten erlaubt!
                    setValueTemplates[j].and(templates[i]);
                    delCandTemplates[j].or(templates[i]);
                    if (initLists) {
                        candTemplates.get(j).add(templates[i]);
                    }
                }
            }

            // verfeinern
            if (initLists) {
                int removals = 0;
                do {
                    removals = 0;
                    for (int j = 1; j <= 9; j++) {
                        setValueTemplates[j].setAll();
                        delCandTemplates[j].clear();
                        ListIterator<SudokuSetBase> it = candTemplates.get(j).listIterator();
                        while (it.hasNext()) {
                            SudokuSetBase template = it.next();
                            boolean removed = false;
                            for (int k = 1; k <= 9; k++) {
                                if (k != j && !template.andEmpty(setValueTemplates[k])) {
                                    it.remove();
                                    removed = true;
                                    removals++;
                                    break;
                                }
                            }
                            if (!removed) {
                                setValueTemplates[j].and(template);
                                delCandTemplates[j].or(template);
                            }
                        }
                    }
                } while (removals > 0);
            }

            for (int i = 1; i <= 9; i++) {
                delCandTemplates[i].not();
            }
            templatesDirty = false;
            if (initLists) {
                templatesListDirty = false;
            }
        }
        templateNanos += System.nanoTime() - nanos;
    }

    /**
     * @return the stepNumber
     */
    public int getStepNumber() {
        return stepNumber;
    }

    /******************************************************************************************************************/
    /* END TEMPLATES                                                                                                  */
    /******************************************************************************************************************/
    /******************************************************************************************************************/
    /* GROUP NODE CACHE                                                                                               */
    /******************************************************************************************************************/
    /**
     * Gets all group nodes from {@link #sudoku}.
     * The list is cached in {@link #gourpNodes} and only recomputed if necessary.
     * @return
     */
    public List<GroupNode> getGroupNodes() {
        if (groupNodesStepNumber == stepNumber) {
            return groupNodes;
        } else {
            groupNodes = GroupNode.getGroupNodes(this);
            groupNodesStepNumber = stepNumber;
            return groupNodes;
        }
    }

    /******************************************************************************************************************/
    /* END GROUP NODE CACHE                                                                                           */
    /******************************************************************************************************************/
    /******************************************************************************************************************/
    /* ALS AND RC CACHE                                                                                               */
    /******************************************************************************************************************/
    /**
     * Convenience method for {@link #getAlses(boolean) }.
     * @return
     */
    public List<Als> getAlses() {
        return getAlses(false);
    }

    /**
     * Gets all ALS from {@link #sudoku}.
     * If <code>onlyLargerThanOne</code>
     * is set, ALS of size 1 (cells containing two candidates) are ignored.<br>
     * The work is delegated to {@link #collectAllAlsesForHouse(int[][], sudoku.Sudoku2, java.util.List, boolean)}.<br><br>
     * The list is cached in {@link #alsesOnlyLargerThanOne} or
     * {@link #alsesWithOne} respectively and only recomputed if necessary.
     * @param onlyLargerThanOne
     * @return
     */
    public List<Als> getAlses(boolean onlyLargerThanOne) {
        if (onlyLargerThanOne) {
            if (alsesOnlyLargerThanOneStepNumber == stepNumber) {
                return alsesOnlyLargerThanOne;
            } else {
                alsesOnlyLargerThanOne = doGetAlses(onlyLargerThanOne);
                alsesOnlyLargerThanOneStepNumber = stepNumber;
                return alsesOnlyLargerThanOne;
            }
        } else {
            if (alsesWithOneStepNumber == stepNumber) {
                return alsesWithOne;
            } else {
                alsesWithOne = doGetAlses(onlyLargerThanOne);
                alsesWithOneStepNumber = stepNumber;
                return alsesWithOne;
            }
        }
    }

    /**
     * Does some statistics and starts the recursive search for every house.
     * 
     * @param onlyLargerThanOne
     * @return
     */
    private List<Als> doGetAlses(boolean onlyLargerThanOne) {
        long actNanos = System.nanoTime();

        // this is the list we will be working with
        List<Als> alses = new ArrayList<Als>(300);
        alses.clear();

        // recursion is started once for every cell in every house
        for (int i = 0; i < Sudoku2.ALL_UNITS.length; i++) {
            for (int j = 0; j < Sudoku2.ALL_UNITS[i].length; j++) {
                indexSet.clear();
                candSets[0] = 0;
                checkAlsRecursive(0, j, Sudoku2.ALL_UNITS[i], alses, onlyLargerThanOne);
            }
        }

        // compute fields
        for (Als als : alses) {
            als.computeFields(this);
        }

        alsNanos += (System.nanoTime() - actNanos);
        anzAlsCalls++;

        return alses;
    }

    /**
     * Does a recursive ALS search over one house (<code>indexe</code>).
     * @param anzahl Number of cells already contained in {@link #indexSet}.
     * @param startIndex First index in <code>indexe</code> to check.
     * @param indexe Array with all the cells of the current house.
     * @param alses List for all newly found ALS
     * @param onlyLargerThanOne Allow ALS with only one cell (bivalue cells)
     */
    private void checkAlsRecursive(int anzahl, int startIndex, int[] indexe,
            List<Als> alses, boolean onlyLargerThanOne) {
        anzahl++;
        if (anzahl > indexe.length - 1) {
            // end recursion (no more than 8 cells in an ALS possible)
            return;
        }
        for (int i = startIndex; i < indexe.length; i++) {
            int houseIndex = indexe[i];
            if (sudoku.getValue(houseIndex) != 0) {
                // cell already set -> ignore
                continue;
            }
            indexSet.add(houseIndex);
            candSets[anzahl] = (short) (candSets[anzahl - 1] | sudoku.getCell(houseIndex));

            // if the number of candidates is excatly one larger than the number
            // of cells, an ALS was found
            if (Sudoku2.ANZ_VALUES[candSets[anzahl]] - anzahl == 1) {
                if (!onlyLargerThanOne || indexSet.size() > 1) {
                    // found one -> save it if it doesnt exist already
                    anzAls++;
                    Als newAls = new Als(indexSet, candSets[anzahl]);
                    if (!alses.contains(newAls)) {
                        alses.add(newAls);
                    } else {
                        doubleAls++;
                    }
                }
            }

            // continue recursion
            checkAlsRecursive(anzahl, i + 1, indexe, alses, onlyLargerThanOne);

            // remove current cell
            indexSet.remove(houseIndex);
        }
    }

    /**
     * Do some statistics.
     * @return
     */
    public String getAlsStatistics() {
        return "Statistic for getAls(): number of calls: " + anzAlsCalls + ", total time: "
                + (alsNanos / 1000) + "us, average: " + (alsNanos / anzAlsCalls / 1000) + "us\r\n"
                + "    anz: " + anzAls + "/" + (anzAls / anzAlsCalls)
                + ", double: " + doubleAls + "/" + (doubleAls / anzAlsCalls)
                + " res: " + (anzAls - doubleAls) + "/" + ((anzAls - doubleAls) / anzAlsCalls);
    }

    /**
     * Lists of all RCs of the current sudoku are needed by more than one solver,
     * but caching them can greatly increase performance.
     *
     * @param alses
     * @param allowOverlap
     * @return
     */
    public List<RestrictedCommon> getRestrictedCommons(List<Als> alses, boolean allowOverlap) {
        if (lastRcStepNumber != stepNumber || lastRcAllowOverlap != allowOverlap
                || lastRcAlsList != alses || lastRcOnlyForward != rcOnlyForward) {
            // recompute
            if (startIndices == null || startIndices.length < alses.size()) {
                startIndices = new int[(int) (alses.size() * 1.5)];
                endIndices = new int[(int) (alses.size() * 1.5)];
            }
            restrictedCommons = doGetRestrictedCommons(alses, allowOverlap);
            // store caching flags
            lastRcStepNumber = stepNumber;
            lastRcAllowOverlap = allowOverlap;
            lastRcOnlyForward = rcOnlyForward;
            lastRcAlsList = alses;
        }
        return restrictedCommons;
    }

    /**
     * Getter for {@link #startIndices}.
     * @return
     */
    public int[] getStartIndices() {
        return startIndices;
    }

    /**
     * Getter for {@link #endIndices}.
     * @return
     */
    public int[] getEndIndices() {
        return endIndices;
    }

    /**
     * Setter for {@link #rcOnlyForward}.
     * @param rof
     */
    public void setRcOnlyForward(boolean rof) {
        rcOnlyForward = rof;
    }

    /**
     * Getter for {@link #rcOnlyForward}.
     * @return
     */
    public boolean isRcOnlyForward() {
        return rcOnlyForward;
    }

    /**
     * For all combinations of two ALS check whether they have one or two RC(s). An
     * RC is a candidate that is common to both ALS and where all instances of that
     * candidate in both ALS see each other.<br>
     * ALS with RC(s) may overlap as long as the overlapping area doesnt contain an RC.<br>
     * Two ALS can have a maximum of two RCs.<br>
     * The index of the first RC for {@link #alses}[i] is written to {@link #startIndices}[i],
     * the index of the last RC + 1 is written to {@link #endIndices}[i] (needed for chain search).<br><br>
     * 
     * If {@link #rcOnlyForward} is set to <code>true</code>, only RCs with references to ALS with a greater
     * index are collected. For ALS-XZ und ALS-XY-Wing this is irrelevant. For ALS-Chains
     * it greatly improves performance, but not all chains are found. This is the default
     * when solving puzzles, {@link #rcOnlyForward} <code>false</code> is the default for
     * search for all steps.
     *
     * @param withOverlap If <code>false</code> overlapping ALS are not allowed
     */
    private List<RestrictedCommon> doGetRestrictedCommons(List<Als> alses, boolean withOverlap) {
        rcAnzCalls++;
        long actNanos = 0;
        actNanos = System.nanoTime();
        // store the calculation mode
        lastRcOnlyForward = rcOnlyForward;
        // delete all RCs from the last run
        List<RestrictedCommon> rcs = new ArrayList<RestrictedCommon>(2000);
        // Try all combinations of alses
        for (int i = 0; i < alses.size(); i++) {
            Als als1 = alses.get(i);
            startIndices[i] = rcs.size();
            //if (DEBUG) System.out.println("als1: " + SolutionStep.getAls(als1));
            int start = 0;
            if (rcOnlyForward) {
                start = i + 1;
            }
            for (int j = start; j < alses.size(); j++) {
                if (i == j) {
                    continue;
                }
                Als als2 = alses.get(j);
                // check whether the ALS overlap (intersectionSet is needed later on anyway)
                intersectionSet.set(als1.indices);
                intersectionSet.and(als2.indices);
                if (!withOverlap && !intersectionSet.isEmpty()) {
                    // overlap is not allowed!
                    continue;
                }
                //if (DEBUG) System.out.println("als2: " + SolutionStep.getAls(als2));
                // restricted common: all buddies + the positions of the candidates themselves ANDed
                // check whether als1 and als2 have common candidates
                possibleRestrictedCommonsSet = als1.candidates;
                possibleRestrictedCommonsSet &= als2.candidates;
                // possibleRestrictedCommons now contains all candidates common to both ALS
                if (possibleRestrictedCommonsSet == 0) {
                    // nothing to do!
                    continue;
                }
                // number of RC candidates found for this ALS combination
                int rcAnz = 0;
                RestrictedCommon newRC = null;
                int[] prcs = Sudoku2.POSSIBLE_VALUES[possibleRestrictedCommonsSet];
                for (int k = 0; k < prcs.length; k++) {
                    int cand = prcs[k];
                    // Get all positions of cand in both ALS
                    restrictedCommonIndexSet.set(als1.indicesPerCandidat[cand]);
                    restrictedCommonIndexSet.or(als2.indicesPerCandidat[cand]);
                    // non of these positions may be in the overlapping area of the two ALS
                    if (!restrictedCommonIndexSet.andEmpty(intersectionSet)) {
                        // at least on occurence of cand is in overlap -> forbidden
                        continue;
                    }
                    // now check if all those candidates see each other
                    restrictedCommonBuddiesSet.setAnd(als1.buddiesAlsPerCandidat[cand],
                            als2.buddiesAlsPerCandidat[cand]);
                    // we now know all common buddies, all common candidates must be in that set
                    if (restrictedCommonIndexSet.andEquals(restrictedCommonBuddiesSet)) {
                        // found -> cand is RC
                        if (rcAnz == 0) {
                            newRC = new RestrictedCommon(i, j, cand);
                            rcs.add(newRC);
                            anzRcs++;
                        } else {
                            newRC.setCand2(cand);
                        }
                        rcAnz++;
                    }
                }
                if (rcAnz > 0) {
                    //if (DEBUG) System.out.println(newRC + ": " + rcAnz + " RCs for ALS " + SolutionStep.getAls(als1) + "/" + SolutionStep.getAls(als2));
                }
            }
            endIndices[i] = rcs.size();
        }
        actNanos = System.nanoTime() - actNanos;
        rcNanos += actNanos;
        return rcs;
    }

    /**
     * Do some statistics.
     * @return
     */
    public String getRCStatistics() {
        return "Statistic for getRestrictedCommons(): number of calls: " + rcAnzCalls + ", total time: "
                + (rcNanos / 1000) + "us, average: " + (rcNanos / rcAnzCalls / 1000) + "us\r\n"
                + "    anz: " + anzRcs + "/" + (anzRcs / rcAnzCalls);
    }

    /******************************************************************************************************************/
    /* END ALS AND RC CACHE                                                                                           */
    /******************************************************************************************************************/
    public void printStatistics() {
//        double per = ((double)templateNanos) / templateAnz;
//        per /= 1000.0;
//        double total = ((double)templateNanos) / 1000000.0;
//        System.out.printf("Templates: %d calls, %.2fus per call, %.2fms total%n", templateAnz, per, total);
//        fishSolver.printStatistics();
//        chainSolver.printStatistics();
    }
}
