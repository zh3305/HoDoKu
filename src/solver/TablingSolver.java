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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sudoku.Candidate;
import sudoku.Chain;
import sudoku.Options;
import sudoku.SolutionStep;
import sudoku.SolutionType;
import sudoku.Sudoku2;
import sudoku.SudokuSet;
import sudoku.SudokuSetBase;

/**
 * Implements Trebors Tables for finding Nice Loops, AICs, Forcing Chains and
 * Forcing Nets. Also called by the {@link FishSolver} for finding suitable
 * chains for Kraken Fish.<br><br>
 *
 * The idea of tabling is simple: For all possible premises ("candidate n is set
 * in/eliminated from cell x") all possible outcomes are logged. The result is
 * then checked for contradictions or verities. For chains only direct outcomes
 * are stored, for nets every possible outcome triggers a new round of starting
 * conditions (up to a maximum recursion depth). For every premise a separate
 * table is used.<br><br>
 *
 * After the initial round all tables are expanded: For every possible outcome
 * all other outcomes from the other table are added. This results in a matrix
 * holding all possible conclusions. The method is simple but it uses a lot of
 * memory and computation time.<br><br>
 *
 * The real problem with Trebors Tables is to reconstruct the chains/nets that
 * led to the result.<br><br>
 *
 * Some tests currently implemented (every table holds an array with sets for
 * all cells than can be set to a certain candidate - onSets - and with set for
 * cells, where that candidate can be eliminated - offSets): 
 * <ol> 
 *      <li>only one chain:
 *      <ul> 
 *          <li>two values set in the same cell (AND onSets) -> premise
 *              was wrong </li> 
 *          <li>same value set twice in one house -> premise was
 *              wrong</li>
 *          <li>all candidates deleted from a cell -> premise was wrong</li>
 *          <li>candidate can be set in and deleted from a cell simultaneously ->
 *              premise was wrong</li> 
 *          <li>all candidates are deleted from a house -> premise
 *              was wrong</li>
 *      </ul></li> 
 *      <li>two chains for the same start candidate
 *          (candidate set and deleted):
 *      <ul> 
 *          <li>both chains lead to the same value in
 *              onSets -> value can be set</li> 
 *          <li>both chains lead to the same value in
 *              offSets -> candidate can be deleted</li>
 *      </ul></li> 
 *      <li>chains for all
 *          candidates in one house/cell set:
 *      <ul> 
 *          <li>all chains lead to the same value
 *              in onSets -> value can be set</li> 
 *          <li>all chains lead to the same value in
 *              offSets -> candidate can be deleted</li>
 *      </ul></li> 
 * </ol>
 *
 * 20081013: AIC added (combined with Nice Loops)<br><br> 
 * 
 * For every Nice Loop
 * that starts with a strong inference out of the start cell and ends with a
 * weak inference into the start cell the AIC (start cell - last strong
 * inference) is checked. If it gives more than one elimination, it is stored as
 * AIC instead of as Nice Loop. The check is done for discontinuous loops
 * only.<br><br>
 *
 * AIC eliminations: 
 * <ul> 
 *      <li>if the candidates of the endpoints are equal, all
 *          candidates can be eliminated that see both endpoints</li> 
 *      <li>if the candidates are not equal, cand A can be eliminated in 
 *          cell b and vice versa</li> 
 * </ul>
 *
 * @author hobiwan
 */
public class TablingSolver extends AbstractSolver {

    public int doDebugCounter = 0;
    private static final long CLEANUP_INTERVAL = 5 * 60 * 1000;
    /**
     * Enable additional output for debugging.
     */
    // TODO DEBUG
    private static boolean DEBUG = true;
    private static boolean doDebug = false;
    /**
     * Maximum recursion depth in buildung the tables.
     */
    private static final int MAX_REC_DEPTH = 50;
    /**
     * Maximum number of indices back to entries, that caused
     * the current entry. If more than MAX_RET_INDICES_PER_ENTRY
     * retIndices arepresent, they are ignored, leading to incomplete chains.
     */
    private static final int MAX_RET_INDICES_PER_ENTRY = 5;
    /**
     * A special comparator for comparing chains and nets.
     */
    private static TablingComparator tablingComparator = null;
    /**
     * A list with steps found in the current run.
     */
    private List<SolutionStep> steps; // gefundene Lösungsschritte
    /**
     * One global step for optimization.
     */
    private SolutionStep globalStep = new SolutionStep(SolutionType.HIDDEN_SINGLE);
    /**
     * All chains already found: eliminations + index in {@link #steps}.
     */
    private SortedMap<String, Integer> deletesMap = new TreeMap<String, Integer>();
    /**
     * Search only for chains, not for nets.
     */
    private boolean chainsOnly = true;
    /**
     * Include group nodes in search.
     */
    private boolean withGroupNodes = false;
    /**
     * Include ALS nodes in search.
     */
    private boolean withAlsNodes = false;
    /**
     * Accept steps only if they contain group nodes/ALS nodes.
     */
    private boolean onlyGroupedNiceLoops = false;
    /**
     * One table for every premise. Indices are in format "nnm" with "nn" the
     * index of the cell and "m" the candidate. This table holds all entries for
     * "candidate m set in cell nn".
     */
    private TableEntry[] onTable = null;
    /**
     * One table for every premise. Indices are in format "nnm" with "nn" the
     * index of the cell and "m" the candidate. This table holds all entries for
     * "candidate m deleted from cell nn".
     */
    private TableEntry[] offTable = null;
    /**
     * A list of all table entries for e specific candidate in a house or for
     * all candidates in a cell respectively. Used for Forcing chain/Net
     * checks.
     */
    private List<TableEntry> entryList = new ArrayList<TableEntry>(10);
    /**
     * For temporary checks.
     */
    private SudokuSet tmpSet = new SudokuSet();
    /**
     * For temporary checks.
     */
    private SudokuSet tmpSet1 = new SudokuSet();
    /**
     * For temporary checks.
     */
    private SudokuSet tmpSet2 = new SudokuSet();
    /**
     * For buildung chains.
     */
    private SudokuSet tmpSetC = new SudokuSet();
    /**
     * Used to check if all candidates in a house or cell set lead to the same
     * value in a cell.
     */
    private SudokuSet[] tmpOnSets = new SudokuSet[10];
    /**
     * Used to check if all candidates in a house or cell deleted lead to the
     * same canddiate deleted from a cell.
     */
    private SudokuSet[] tmpOffSets = new SudokuSet[10];
    /**
     * Map containing the new indices of all alses, that have already been
     * written to globalStep. They key is the old index into {@link #alses}, the
     * value is the new index of the ALS stored in the {@link SolutionStep}.
     */
    private TreeMap<Integer, Integer> chainAlses = new TreeMap<Integer, Integer>();
    /**
     * Indicates, if the internal data structures have already been
     * inititialized
     */
    private boolean initialized = false;
    /**
     * Time of the last call to
     */
    private long lastUsed = -1;
    /**
     * Original Sudoku before all operations; has to be used in Net searches:
     * The operations that lead to the current conclusion have to be taken
     * from the original and not from the current state of the sudoku.
     */
    private Sudoku2 savedSudoku;
    /**
     * Indices back to the table entries, that caused the current action.
     */
    private int[][] retIndices = new int[MAX_REC_DEPTH][MAX_RET_INDICES_PER_ENTRY];
    /**
     * A {@link SudokuStepFinder} containing only a {@link SimpleSolver}; used
     * for finding resulting singles after eliminations in net searches.
     */
    private SudokuStepFinder simpleFinder;
    /**
     * The singles found by the {@link #simpleFinder}.
     */
    private List<SolutionStep> singleSteps = new ArrayList<SolutionStep>();  // für Naked und Hidden Singles
    /**
     * Holds all entries, that are not {@link Chain#NORMAL_NODE} entries. The table is
     * not reset after a search run, but entries are reused in consecutive runs. Thats why
     * the position of the current entry is not determined by <code>extendedTable.size()</code>,
     * but by {@link #extendedTableIndex}.
     */
    private List<TableEntry> extendedTable = null;
    /**
     * Lookup map: can be used to determine the index of an extended entry in
     * {@link #extendedTable}.
     */
    private SortedMap<Integer, Integer> extendedTableMap = null;
    /**
     * Current index into {@link #extendedTable} (see there).
     */
    private int extendedTableIndex = 0;
    /**
     * A list with all group nodes for the sudoku.
     */
    private List<GroupNode> groupNodes = null;
    /**
     * A list with all available ALS for the sudoku
     */
    private List<Als> alses = null;
    /**
     * All cells with eliminations for an als using a specificentry candidate, 
     * sorted by candidate
     */
    private SudokuSet[] alsEliminations = new SudokuSet[10];
    /**
     * Global chain for {@link #buildChain(solver.TableEntry, int, int[], boolean, sudoku.SudokuSet)}.
     */
    private int[] chain = new int[Options.getInstance().getMaxTableEntryLength()];
    /**
     * Index of the next freeelement in {@link #chain}.
     */
    private int chainIndex = 0;
    /**
     * Chains for nets. Every new partial chain of anet is stored here.
     */
    private int[][] mins = new int[200][Options.getInstance().getMaxTableEntryLength()];
    /**
     * For every {@link #min} the index of the next free element.
     */
    private int[] minIndexes = new int[mins.length];
    /**
     * For every {@link #min} the entry of the first element in the min (the first element
     * in the min is the last element in the resulting subchain).
     */
    private TableEntry[] minEntries = new TableEntry[mins.length];
    /**
     * For every {@link #min} the index of the last entry in the
     * original chain (needed for check, if the chain has been reached)
     */
    private int[] minEndIndices = new int[mins.length];
    /**
     * the {@link #min}that is currently built.
     */
    private int actMin = 0;
    /**
     * One global buffer chain for useby {@link #addChain(solver.TableEntry, int, int, boolean, boolean, boolean) }.
     */
    private int[] tmpChain = new int[Options.getInstance().getMaxTableEntryLength()];
    /**
     * Up to nine chains for temporary storage.
     */
    private Chain[] tmpChains = new Chain[9];
    /**
     * Index of the next free element in the current {@link #tmpChain}.
     */
    private int tmpChainsIndex = 0;
    /**
     * Contains all cell indices of the current chain. Used by
     * {@link #addChain(solver.TableEntry, int, int, boolean, boolean, boolean)}
     * for lasso checks (a lasso is a chain, that links back to themiddleof the chain).
     */
    private SudokuSet lassoSet = new SudokuSet();

    /**
     * Creates a new instance of TablingSolver
     *
     * @param finder
     */
    public TablingSolver(SudokuStepFinder finder) {
        super(finder);

        simpleFinder = new SudokuStepFinder(true);

        for (int i = 0; i < tmpOnSets.length; i++) {
            tmpOnSets[i] = new SudokuSet();
            tmpOffSets[i] = new SudokuSet();
        }
        steps = new ArrayList<SolutionStep>();
        if (tablingComparator == null) {
            tablingComparator = new TablingComparator();
        }
        for (int i = 0; i < tmpChains.length; i++) {
            tmpChains[i] = new Chain();
            tmpChains[i].setChain(new int[Options.getInstance().getMaxTableEntryLength()]);
        }

        for (int i = 0; i < alsEliminations.length; i++) {
            alsEliminations[i] = new SudokuSet();
        }
    }

    /**
     * Late initialization for those internal data structures, that are very
     * memory intensive. This method <b>MUST</b> be called <b>every time</b> the
     * solver is actually used.<br>
     *
     * Calling this method also resets {@link #lastUsed} to the current time
     * (see {@link #cleanUp() }).
     */
    private void initialize() {
        if (!initialized) {
            onTable = new TableEntry[810];
            offTable = new TableEntry[810];
            for (int i = 0; i < onTable.length; i++) {
                onTable[i] = new TableEntry();
                offTable[i] = new TableEntry();
            }

            extendedTable = new ArrayList<TableEntry>();
            extendedTableMap = new TreeMap<Integer, Integer>();
            extendedTableIndex = 0;

            initialized = true;
        }
        lastUsed = System.currentTimeMillis();
    }

    /**
     * Releases memory, if the solver has not been used for more than
     * {@link #CLEANUP_INTERVAL} ms.<br>
     *
     * Please note, that this method is called from a seperate thread and must
     * therefore be synchronized. Calling this method while the solver is in
     * use, will result in Exceptions.
     */
    @Override
    protected void cleanUp() {
        synchronized (this) {
            if (initialized && (System.currentTimeMillis() - lastUsed) > CLEANUP_INTERVAL) {
                for (int i = 0; i < onTable.length; i++) {
                    onTable[i] = null;
                    offTable[i] = null;
                }
            }
            onTable = null;
            offTable = null;

            if (extendedTable != null) {
                for (int i = 0; i < extendedTableIndex; i++) {
                    extendedTable.set(i, null);
                }
                extendedTable = null;
            }
            if (extendedTableMap != null) {
                extendedTableMap.clear();
                extendedTableMap = null;
            }
            extendedTableIndex = 0;

            initialized = false;
        }
    }

    /**
     * Delete all temporary chains.
     */
    private void resetTmpChains() {
        for (int i = 0; i < tmpChains.length; i++) {
            tmpChains[i].reset();
        }
        tmpChainsIndex = 0;
    }

    @Override
    protected SolutionStep getStep(SolutionType type) {
        SolutionStep result = null;
        sudoku = finder.getSudoku();
        switch (type) {
            case NICE_LOOP:
            case CONTINUOUS_NICE_LOOP:
            case DISCONTINUOUS_NICE_LOOP:
            case AIC:
                withGroupNodes = false;
                withAlsNodes = false;
                result = getNiceLoops();
                break;
            case GROUPED_NICE_LOOP:
            case GROUPED_CONTINUOUS_NICE_LOOP:
            case GROUPED_DISCONTINUOUS_NICE_LOOP:
            case GROUPED_AIC:
                withGroupNodes = true;
                withAlsNodes = Options.getInstance().isAllowAlsInTablingChains();
                result = getNiceLoops();
                break;
            case FORCING_CHAIN:
            case FORCING_CHAIN_CONTRADICTION:
            case FORCING_CHAIN_VERITY:
                steps.clear();
                withGroupNodes = true;
                withAlsNodes = Options.getInstance().isAllowAlsInTablingChains();
                getForcingChains();
                if (steps.size() > 0) {
                    Collections.sort(steps, tablingComparator);
                    result = steps.get(0);
                }
                break;
            case FORCING_NET:
            case FORCING_NET_CONTRADICTION:
            case FORCING_NET_VERITY:
                steps.clear();
                withGroupNodes = true;
                withAlsNodes = Options.getInstance().isAllowAlsInTablingChains();
                getForcingNets();
                if (steps.size() > 0) {
                    Collections.sort(steps, tablingComparator);
                    result = steps.get(0);
                }
                break;
        }
        return result;
    }

    @Override
    protected boolean doStep(SolutionStep step) {
        boolean handled = true;
        sudoku = finder.getSudoku();
        switch (step.getType()) {
            case NICE_LOOP:
            case CONTINUOUS_NICE_LOOP:
            case DISCONTINUOUS_NICE_LOOP:
            case AIC:
            case GROUPED_NICE_LOOP:
            case GROUPED_CONTINUOUS_NICE_LOOP:
            case GROUPED_DISCONTINUOUS_NICE_LOOP:
            case GROUPED_AIC:
                for (Candidate cand : step.getCandidatesToDelete()) {
                    sudoku.delCandidate(cand.getIndex(), cand.getValue());
                }
                break;
            case FORCING_CHAIN:
            case FORCING_CHAIN_CONTRADICTION:
            case FORCING_CHAIN_VERITY:
            case FORCING_NET:
            case FORCING_NET_CONTRADICTION:
            case FORCING_NET_VERITY:
                if (step.getValues().size() > 0) {
                    for (int i = 0; i < step.getValues().size(); i++) {
                        int value = step.getValues().get(i);
                        int index = step.getIndices().get(i);
                        sudoku.setCell(index, value);
                    }
                } else {
                    for (Candidate cand : step.getCandidatesToDelete()) {
                        sudoku.delCandidate(cand.getIndex(), cand.getValue());
                    }
                }
                break;
            default:
                handled = false;
        }
        if (handled) {
            // solver has been used
            lastUsed = System.currentTimeMillis();
        }
        return handled;
    }

    /**
     * Finds all Nice Loops/AICs contained in the current sudoku.
     *
     * @return
     */
    protected synchronized List<SolutionStep> getAllNiceLoops() {
        initialize();
        sudoku = finder.getSudoku();
        long ticks = System.currentTimeMillis();
        steps = new ArrayList<SolutionStep>();
        withGroupNodes = false;
        withAlsNodes = false;
        doGetNiceLoops();
        Collections.sort(this.steps);
        ticks = System.currentTimeMillis() - ticks;
        if (DEBUG) {
            System.out.println("getAllNiceLoops() gesamt: " + ticks + "ms");
        }
        return steps;
    }

    /**
     * Finds all Grouped Nice Loops/Grouped AICs contained in the current
     * sudoku.
     *
     * @return
     */
    protected synchronized List<SolutionStep> getAllGroupedNiceLoops() {
        initialize();
        sudoku = finder.getSudoku();
        long ticks = System.currentTimeMillis();
        steps = new ArrayList<SolutionStep>();
        withGroupNodes = true;
        withAlsNodes = Options.getInstance().isAllowAlsInTablingChains();
        onlyGroupedNiceLoops = true;
        doGetNiceLoops();
        onlyGroupedNiceLoops = false;
        Collections.sort(this.steps);
        ticks = System.currentTimeMillis() - ticks;
        if (DEBUG) {
            System.out.println("getAllGroupedNiceLoops() gesamt: " + ticks + "ms");
        }
        return steps;
    }

    /**
     * Finds all Forcing Chains contained in the current sudoku.
     *
     * @return
     */
    protected synchronized List<SolutionStep> getAllForcingChains() {
        initialize();
        sudoku = finder.getSudoku();
        List<SolutionStep> oldSteps = steps;
        steps = new ArrayList<SolutionStep>();
        long millis1 = System.currentTimeMillis();
        withGroupNodes = true;
        withAlsNodes = Options.getInstance().isAllowAlsInTablingChains();
        getForcingChains();
        Collections.sort(steps, tablingComparator);
        millis1 = System.currentTimeMillis() - millis1;
        if (DEBUG) {
            System.out.println("getAllForcingChains() gesamt: " + millis1 + "ms");
        }
        List<SolutionStep> result = steps;
        steps = oldSteps;
        return result;
    }

    /**
     * Finds all Forcing Nets contained in the current sudoku.
     *
     * @return
     */
    protected synchronized List<SolutionStep> getAllForcingNets() {
        initialize();
        sudoku = finder.getSudoku();
        List<SolutionStep> oldSteps = steps;
        steps = new ArrayList<SolutionStep>();
        long millis1 = System.currentTimeMillis();
        //withGroupNodes = true;
        withGroupNodes = true;
        withAlsNodes = Options.getInstance().isAllowAlsInTablingChains();
        getForcingNets();
        Collections.sort(steps, tablingComparator);
        millis1 = System.currentTimeMillis() - millis1;
        if (DEBUG) {
            System.out.println("getAllForcingNets() gesamt: " + millis1 + "ms");
        }
        List<SolutionStep> result = steps;
        steps = oldSteps;
        return result;
    }

    /**
     * Fills and expands the tables for a Kraken Fish search. This method is
     * called by the fish finder before the fish search starts. For every fish
     * {@link #checkKrakenTypeOne(sudoku.SudokuSet, int, int)} or
     * {@link #checkKrakenTypeTwo(sudoku.SudokuSet, sudoku.SudokuSet, int, int)}
     * is called to do the actual search.
     */
    protected void initForKrakenSearch() {
        initialize();
        sudoku = finder.getSudoku();
        deletesMap.clear();
        // fill tables
        long ticks = System.currentTimeMillis();
        chainsOnly = true;
        // search for everything
        fillTables();
        fillTablesWithGroupNodes();
        if (Options.getInstance().isAllowAlsInTablingChains()) {
            fillTablesWithAls();
        }
        ticks = System.currentTimeMillis() - ticks;
        if (DEBUG) {
            System.out.println("fillTables(): " + ticks + "ms");
        }
        printTableAnz();
        //printTable("r1c6=6 fill", onTable[56]);
        //printTable("r3c2<>8 fill", offTable[198]);

        // expand tables
        ticks = System.currentTimeMillis();
        expandTables();
        ticks = System.currentTimeMillis() - ticks;
        if (DEBUG) {
            System.out.println("expandTables(): " + ticks + "ms");
        }
        printTableAnz();
        //printTable("r1c6=6 expand", onTable[56]);
        //printTable("r3c2<>8 expand", offTable[198]);
    }

    /**
     * Search for Kraken Fish Type 1: if a chain starting and ending with a
     * weak link exists from every cell in fins to candidate in index, a KF Type
     * 1 exists.
     *
     * @param fins Set with all fins
     * @param index Index of destination cell
     * @param candidate Candidate in destination cell
     * @return true if a KF exists, false otherwise
     */
    protected boolean checkKrakenTypeOne(SudokuSet fins, int index, int candidate) {
        for (int i = 0; i < fins.size(); i++) {
            int tableIndex = fins.get(i) * 10 + candidate;
            if (!onTable[tableIndex].offSets[candidate].contains(index)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check for Kraken Fish Type 2: If for all cells in indices chains starting
     * and ending in a weak link exist to a candidate, a Kraken Fish Type 2
     * exists. A set with all cells holding a target for the KF is returned.
     *
     * @param indices Set with all starting cells
     * @param result Set that contains possible targets for Kraken Fishes
     * @param startCandidate The fish candidate
     * @param endCandidate The candidate for which the search is made
     * @return true if a KF exists, false otherwise
     */
    protected boolean checkKrakenTypeTwo(SudokuSet indices, SudokuSet result, int startCandidate, int endCandidate) {
        result.set(finder.getCandidates()[endCandidate]);
        result.andNot(indices);
        for (int i = 0; i < indices.size(); i++) {
            int tableIndex = indices.get(i) * 10 + startCandidate;
            result.and(onTable[tableIndex].offSets[endCandidate]);
        }
        return !result.isEmpty();
    }

    /**
     * Retrieve the chain for a Kraken Fish.
     *
     * @param startIndex
     * @param startCandidate
     * @param endIndex
     * @param endCandidate
     * @return
     */
    protected Chain getKrakenChain(int startIndex, int startCandidate, int endIndex, int endCandidate) {
        globalStep.reset();
        resetTmpChains();
        addChain(onTable[startIndex * 10 + startCandidate], endIndex, endCandidate, false);
        return globalStep.getChains().get(0);
    }

    /**
     * Get the shortest NiceLoop/AIC in the grid. Delegates to
     * {@link #doGetNiceLoops()}.
     *
     * @return
     */
    private synchronized SolutionStep getNiceLoops() {
        initialize();
        steps = new ArrayList<SolutionStep>();
        doGetNiceLoops();
        if (steps.size() > 0) {
            Collections.sort(steps);
            return steps.get(0);
        }
        return null;
    }

    /**
     * Find all Forcing Chains. Delegates to {@link #doGetForcingChains()}.
     */
    private synchronized void getForcingChains() {
        initialize();
        chainsOnly = true;
        doGetForcingChains();
    }

    /**
     * Find all Forcing Nets. Delegates to {@link #doGetForcingChains()}.
     */
    private synchronized void getForcingNets() {
        initialize();
        chainsOnly = false;
        doGetForcingChains();
    }

    /**
     * This is the method that actually searches for all types of NiceLoops and
     * AICs.
     */
    private void doGetNiceLoops() {
        deletesMap.clear();
        // fill tables
        long ticks = System.currentTimeMillis();
        chainsOnly = true;
        fillTables();
        if (withGroupNodes) {
            fillTablesWithGroupNodes();
        }
        if (withAlsNodes) {
            fillTablesWithAls();
        }
        ticks = System.currentTimeMillis() - ticks;
        if (DEBUG) {
            System.out.println("fillTables(): " + ticks + "ms");
        }
        printTableAnz();
        //printTable("r5c6=2 fill", onTable[412]);
        //printTable("r8c6<>4 fill", offTable[684]);

        // expand the tables
        ticks = System.currentTimeMillis();
        expandTables();
        ticks = System.currentTimeMillis() - ticks;
        if (DEBUG) {
            System.out.println("expandTables(): " + ticks + "ms");
        }
        printTableAnz();
        //printTable("r5c6=2 expand", onTable[412]);
        //printTable("r8c6<>4 expand", offTable[684]);

        // ok, here it starts!
        ticks = System.currentTimeMillis();
        checkNiceLoops(onTable);
        checkNiceLoops(offTable);
        checkAics(offTable);
        ticks = System.currentTimeMillis() - ticks;
        if (DEBUG) {
            System.out.println("checkNiceLoops(): " + ticks + "ms");
        }
    }

    /**
     * This is the method that actually searches for all types of Forcing Chains
     * and Nets.
     */
    private void doGetForcingChains() {
        deletesMap.clear();
        // fill tables
        long nanos = System.nanoTime();
        fillTables();
        if (withGroupNodes) {
            fillTablesWithGroupNodes();
        }
        if (withAlsNodes) {
            fillTablesWithAls();
        }
        nanos = System.nanoTime() - nanos;
        if (DEBUG) {
            System.out.println("fillTables(): " + (nanos / 1000000l) + "ms");
            //printTables("after fillTables()");
            //printTable("after fillTables() r4c3=5", onTable[285]);
        }
        printTableAnz();
        //printTable("r7c1=8 fill", onTable[548], alses);
        //printTable("r3c8=7 fill", onTable[257], alses);
        //printTable("r8c3=6 fill", onTable[656], alses);
        printTable("r7c5=4 fill", onTable[584], alses);
        printTable("r2c1=8 fill", onTable[98], alses);

        // expand tables
        nanos = System.nanoTime();
        expandTables();
        nanos = System.nanoTime() - nanos;
        if (DEBUG) {
            System.out.println("expandTables(): " + (nanos / 1000000l) + "ms");
            //printTables("after expandTables()");
            //printTable("r7c1=8 expand", onTable[548], alses);
            //printTable("r3c8=7 expand", onTable[257], alses);
            printTable("r7c5=4 expand", onTable[584], alses);
            printTable("r2c1=8 expand", onTable[98], alses);
        }
        printTableAnz();
        //printTable("r6c8=1 expand", onTable[521]);
        //printTable("r6c8<>1 expand", offTable[521]);

        if (chainsOnly == true) {
            // ok, hier beginnt der Spass!
            nanos = System.nanoTime();
            checkForcingChains();
            nanos = System.nanoTime() - nanos;
            if (DEBUG) {
                System.out.println("checkChainsFC(): " + (nanos / 1000000l) + "ms");
            }
        } else {
            // now nets
            // try new net nodes
//            System.out.println("======== CREATE NET1 =========");
            nanos = System.nanoTime();
            createAllNets();
            nanos = System.nanoTime() - nanos;
            if (DEBUG) {
                System.out.println("createAllNets(): " + (nanos / 1000000l) + "ms");
                //printTable("after createNets() r4c3=5", onTable[285]);
                //printTable("r7c1=8 nets", onTable[548], alses);
                //printTable("r3c8=7 nets", onTable[257], alses);
                printTable("r7c5=4 nets", onTable[584], alses);
                printTable("r2c1=8 nets", onTable[98], alses);
            }
            printTableAnz();

            // ok, hier beginnt der Spass!
            nanos = System.nanoTime();
            checkForcingChains();
            nanos = System.nanoTime() - nanos;
            if (DEBUG) {
                System.out.println("checkChainsFN(): " + (nanos / 1000000l) + "ms");
            }

            //TODO: DEBUG
            // lets find out, how many candidates (and combination of candidates)
            // exist in the grid as by now
            for (int c = 1; c <= 9; c++) {
                List<SudokuSet> candSets = new ArrayList<SudokuSet>();
                Map<SudokuSet, Integer> candSetAnzMap = new TreeMap<SudokuSet, Integer>();
                for (int i = 0; i < onTable.length; i++) {
                    if (onTable[i].index != 0) {
                    }
                }
            }
        }

        if (DEBUG) {
//            for (SolutionStep step : steps) {
//                if (step.getCandidatesToDelete().get(0).getIndex() == 3 && step.getCandidatesToDelete().get(0).getValue() == 5) {
//                    System.out.println("==================================");
//                    System.out.println("   " + step.toString(2));
//                    List<Chain> chains = step.getChains();
//                    for (Chain debugChain : chains) {
//                        System.out.println("   chain: " + debugChain);
//                    }
//                    System.out.println("==================================");
//                }
//            }
        }
    }

    /**
     * Starting point for the real checks for Forcing Chains/Nets. The checks
     * are delegated to {@link #checkOneChain(solver.TableEntry)},
     * {@link #checkTwoChains(solver.TableEntry, solver.TableEntry)} and
     * {@link #checkAllChainsForHouse(sudoku.SudokuSet[])}.
     */
    private void checkForcingChains() {
        // all possible solutions using one chain only
        for (int i = 0; i < onTable.length; i++) {
            checkOneChain(onTable[i]);
            checkOneChain(offTable[i]);
        }
        // all possible solutions for two chains originating
        // from the same cell
        for (int i = 0; i < onTable.length; i++) {
            checkTwoChains(onTable[i], offTable[i]);
        }
        // all possible solutions for chains originating
        // in the same house.
        checkAllChainsForHouse(null);
        checkAllChainsForHouse(Sudoku2.LINE_TEMPLATES);
        checkAllChainsForHouse(Sudoku2.COL_TEMPLATES);
        checkAllChainsForHouse(Sudoku2.BLOCK_TEMPLATES);
    }

    /**
     * Collects all tables for a specific candidate in one house (for all
     * candidates in every cell if
     * <code>houseSets</code> is
     * <code>null</code>) and stores them in {@link #entryList}. The list is
     * then used in {@link #checkEntryList(java.util.List)} to find chains that
     * have the same outcome.
     *
     * @param houseSets
     */
    private void checkAllChainsForHouse(SudokuSet[] houseSets) {
        if (houseSets == null) {
            // make checks for cells
            for (int i = 0; i < Sudoku2.LENGTH; i++) {
                if (sudoku.getValue(i) != 0) {
                    continue;
                }
                // collect table entries for all candidates in the cell
                entryList.clear();
                int[] cands = sudoku.getAllCandidates(i);
                for (int j = 0; j < cands.length; j++) {
                    entryList.add(onTable[i * 10 + cands[j]]);
                }
                // do the checks
                checkEntryList(entryList);
            }
        } else {
            // collect all table entries for every candidate in every house
            // for every house
            for (int i = 0; i < houseSets.length; i++) {
                // and every possible candidate
                for (int j = 1; j < finder.getCandidates().length; j++) {
                    // check if the candidate is still valid in the house
                    tmpSet.set(houseSets[i]);
                    tmpSet.and(finder.getCandidates()[j]);
                    if (!tmpSet.isEmpty()) {
                        // get the table entries
                        entryList.clear();
                        for (int k = 0; k < tmpSet.size(); k++) {
                            entryList.add(onTable[tmpSet.get(k) * 10 + j]);
                        }
                        // do the checks
                        checkEntryList(entryList);
                    }
                }
            }
        }
    }

    /**
     * Used by {@link #checkAllChainsForHouse(sudoku.SudokuSet[])} to check
     * outcomes from "all candidates set in one house or one cell".
     * <code>entryList</code> contains the necessary table entries for one
     * check. If the same value is set/deleted in all chains, it can be
     * set/deleted.<br><br>
     *
     * Note: The destinations candidate must not be one of the source
     * candidates.
     *
     * @param entryList
     */
    private void checkEntryList(List<TableEntry> entryList) {
        // AND all onSets and all Offset and see,
        // if something remains.
        for (int i = 0; i < entryList.size(); i++) {
            TableEntry entry = entryList.get(i);
            for (int j = 1; j < tmpOnSets.length; j++) {
                if (i == 0) {
                    tmpOnSets[j].set(entry.onSets[j]);
                    tmpOffSets[j].set(entry.offSets[j]);
                } else {
                    tmpOnSets[j].and(entry.onSets[j]);
                    tmpOffSets[j].and(entry.offSets[j]);
                }
            }
        }
        // check if something is still left
        for (int j = 1; j < tmpOnSets.length; j++) {
            if (!tmpOnSets[j].isEmpty()) {
                // found a verity -> cell(s) can be set
                for (int k = 0; k < tmpOnSets[j].size(); k++) {
                    if (DEBUG && k > 0) {
                        //System.out.println("More than one chain/net found 1");
                    }
                    globalStep.reset();
                    globalStep.setType(SolutionType.FORCING_CHAIN_VERITY);
                    globalStep.addIndex(tmpOnSets[j].get(k));
                    globalStep.addValue(j);
                    resetTmpChains();
                    for (int l = 0; l < entryList.size(); l++) {
                        addChain(entryList.get(l), tmpOnSets[j].get(k), j, true);
                    }
                    replaceOrCopyStep();
                }
            }
            if (!tmpOffSets[j].isEmpty()) {
                // found a verity -> candidate(s) can be deleted
                for (int k = 0; k < tmpOffSets[j].size(); k++) {
                    if (DEBUG && k > 0) {
                        //System.out.println("More than one chain/net found 2");
                    }
                    globalStep.reset();
                    globalStep.setType(SolutionType.FORCING_CHAIN_VERITY);
                    globalStep.addCandidateToDelete(tmpOffSets[j].get(k), j);
                    resetTmpChains();
                    for (int l = 0; l < entryList.size(); l++) {
                        addChain(entryList.get(l), tmpOffSets[j].get(k), j, false);
                    }
                    replaceOrCopyStep();
                }
            }
        }
    }

    /**
     * Steps are created as "Forcing Chain" always. Here the chains are checked
     * for signs of a net. If nets are found, the type is corrected to "Forcing
     * Net".
     *
     * @param step
     */
    private void adjustType(SolutionStep step) {
        if (step.isNet()) {
            if (step.getType() == SolutionType.FORCING_CHAIN_CONTRADICTION) {
                step.setType(SolutionType.FORCING_NET_CONTRADICTION);
            }
            if (step.getType() == SolutionType.FORCING_CHAIN_VERITY) {
                step.setType(SolutionType.FORCING_NET_VERITY);
            }
        }
    }

    /**
     * Chains that contain ALS_NODEs have to be handled carefully: The ALS for
     * every ALS_NODE must be added to globalStep, the index of the ALS in the
     * chain entry has to be adjusted and all candidates for the entry have
     * to be put as endo fins
     *
     * @param step
     */
    protected void adjustChains(SolutionStep step) {
        // step can contain ALS already -> they are ignored
        int alsIndex = step.getAlses().size();
        chainAlses.clear();
        // check every chain contained in step
        for (int i = 0; i < step.getChainAnz(); i++) {
            Chain adjChain = step.getChains().get(i);
            // check every link in the chain
            for (int j = adjChain.getStart(); j <= adjChain.getEnd(); j++) {
                if (Chain.getSNodeType(adjChain.getChain()[j]) == Chain.ALS_NODE) {
                    // link is an ALS_NODE -> get the index into alses
                    int which = Chain.getSAlsIndex(adjChain.getChain()[j]);
                    if (chainAlses.containsKey(which)) {
                        // ALS has already been used -> adjust the als index in the chain
                        int newIndex = chainAlses.get(which);
                        adjChain.replaceAlsIndex(j, newIndex);
                    } else {
                        // new als -> add it to step and adjust the index.
                        step.addAls(alses.get(which).indices, alses.get(which).candidates);
                        // store the new index
                        chainAlses.put(which, alsIndex);
                        adjChain.replaceAlsIndex(j, alsIndex);
                        alsIndex++;
                    }
                }
            }
        }
    }

    /**
     * Replace
     * <code>dest</code> with
     * <code>src</code>. Used to overwrite a longer chain/net already found
     * with a shorter one that provides the same outcome.<br><br>
     *
     * Note: Doesn't clone chains, so src must be an already cloned step.
     *
     * @param src
     * @param dest
     */
    private void replaceStep(SolutionStep src, SolutionStep dest) {
        // chain or net?
        adjustType(src);
        dest.setType(src.getType());
        // copy the result
        if (src.getIndices().size() > 0) {
            for (int i = 0; i < src.getIndices().size(); i++) {
                dest.getIndices().set(i, src.getIndices().get(i));
                dest.getValues().set(i, src.getValues().get(i));
            }
        } else {
            dest.getCandidatesToDelete().clear();
            for (int i = 0; i < src.getCandidatesToDelete().size(); i++) {
                dest.getCandidatesToDelete().add(src.getCandidatesToDelete().get(i));
            }
        }
        // copy all ALS
        if (src.getAlses().size() > 0) {
            dest.getAlses().clear();
            for (int i = 0; i < src.getAlses().size(); i++) {
                dest.addAls(src.getAlses().get(i));
            }
        }
        dest.getEndoFins().clear();
        for (int i = 0; i < src.getEndoFins().size(); i++) {
            dest.getEndoFins().add(src.getEndoFins().get(i));
        }
        dest.setEntity(src.getEntity());
        dest.setEntityNumber(src.getEntityNumber());
        int i;
        // copy the chains. if a chain already exists in dest
        // that can hold the chain from source, copy it. if not
        // create a new one.
        for (i = 0; i < src.getChains().size(); i++) {
            // get the new chain
            Chain localTmpChain = src.getChains().get(i);
            // there is a chain with index i in dest but it is too short
            boolean toShort = dest.getChains().size() > i && dest.getChains().get(i).getChain().length < (localTmpChain.getEnd() + 1);
            if (i >= dest.getChains().size() || toShort) {
                // either no suitable chain in dest or chain is too short -> create a new one
                int[] tmp = new int[localTmpChain.getEnd() + 1];
                for (int j = 0; j <= localTmpChain.getEnd(); j++) {
                    tmp[j] = localTmpChain.getChain()[j];
                }
                if (toShort) {
                    // chain with index i exists in dest -> replace it
                    Chain destChain = dest.getChains().get(i);
                    destChain.setChain(tmp);
                    destChain.setStart(localTmpChain.getStart());
                    destChain.setEnd(localTmpChain.getEnd());
                    destChain.resetLength();
                } else {
                    // no chain with index i exists in dest -> add it
                    dest.addChain(0, localTmpChain.getEnd(), tmp);
                }
            } else {
                // chain with index i exists in dest and it is long enough to hold the new chain ->
                // replace it
                Chain destChain = dest.getChains().get(i);
                for (int j = 0; j <= localTmpChain.getEnd(); j++) {
                    destChain.getChain()[j] = localTmpChain.getChain()[j];
                }
                destChain.setStart(localTmpChain.getStart());
                destChain.setEnd(localTmpChain.getEnd());
                destChain.resetLength();
            }
        }
        // there are unused chains left in dest -> remove them
        while (i < dest.getChains().size()) {
            // ignore warning: call to remove(int index) not to remove(Object o).
            dest.getChains().remove(i);
        }
    }

    /**
     * Checks if a step with the same effect is already contained in
     * {@link #steps}. If not, the new step is added. If it is already there,
     * the old step is replaced with the new one if the chains in the new step
     * are shorter. If they are longer, the new step is discarded.<br><br>
     *
     * Chains stored in {@link #globalStep} must be cloned before storing the
     * step.
     */
    private void replaceOrCopyStep() {
        adjustType(globalStep);
        if (!chainsOnly && (globalStep.getType() == SolutionType.FORCING_CHAIN_CONTRADICTION
                || globalStep.getType() == SolutionType.FORCING_CHAIN_VERITY)) {
            // we only want nets but got a chain (no caching possible!)
            return;
        }
        // adjust the ALS nodes
        adjustChains(globalStep);
//        System.out.println("replaceorcopystep: " + globalStep.toString(2));

        // all steps use the same chains -> they have to be cloned
        List<Chain> oldChains = globalStep.getChains();
        int chainAnz = oldChains.size();
        oldChains.clear();
        for (int i = 0; i < chainAnz; i++) {
            oldChains.add((Chain) tmpChains[i].clone());
        }

        String del;
        if (globalStep.getCandidatesToDelete().size() > 0) {
            // candidates can be deleted
            del = globalStep.getCandidateString();
        } else {
            // cells can be set
            del = globalStep.getSingleCandidateString();
        }
        if (Options.getInstance().isOnlyOneChainPerStep()) {
            Integer oldIndex = deletesMap.get(del);
            SolutionStep actStep = null;
            if (oldIndex != null) {
                actStep = steps.get(oldIndex.intValue());
            }
            if (actStep != null) {
                if (actStep.getChainLength() > globalStep.getChainLength()) {
                    // new chain is short -> replace
                    replaceStep(globalStep, actStep);
                }
                // done!
                return;
            }
        }
        // new step -> write it
        steps.add((SolutionStep) globalStep.clone());
        if (del != null) {
            // "only one chain" is set -> store the new step
            deletesMap.put(del, steps.size() - 1);
        }
    }

    /**
     * Print all table entries from
     * <code>entrylist</code> (for debugging only).
     *
     * @param entryList
     * @return
     */
    private String printEntryList(List<TableEntry> entryList) {
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < entryList.size(); i++) {
            if (i != 0) {
                tmp.append(", ");
            }
            tmp.append(printTableEntry(entryList.get(i).entries[0], alses));
        }
        return tmp.toString();
    }

    /**
     * <code>on</code> and <code>off</code> lead to the same conclusion. 
     * This is a verity and the
     * conclusion has to be always true.<br><br>
     *
     * Note: If one of the chains gets back to the originating cell, the other
     * chain is only one element long. The whole thing really is a Nice
     * Loop and has already been handled by
     * {@link #checkOneChain(solver.TableEntry)}. It is ignored here.
     *
     * @param on
     * @param off
     */
    private void checkTwoChains(TableEntry on, TableEntry off) {
        if (on.index == 0 || off.index == 0) {
            // one of the tables is empty -> nothing to do
            return;
        }
        // if both tables lead to the same on value that value can be set
        // AND the onSets of both tables
        for (int i = 1; i < on.onSets.length; i++) {
            tmpSet.set(on.onSets[i]);
            tmpSet.and(off.onSets[i]);
            tmpSet.remove(on.getCellIndex(0));
            if (!tmpSet.isEmpty()) {
                // we have found at least one
                for (int j = 0; j < tmpSet.size(); j++) {
                    globalStep.reset();
                    globalStep.setType(SolutionType.FORCING_CHAIN_VERITY);
                    globalStep.addIndex(tmpSet.get(j));
                    globalStep.addValue(i);
                    resetTmpChains();
                    addChain(on, tmpSet.get(j), i, true);
                    addChain(off, tmpSet.get(j), i, true);
                    replaceOrCopyStep();
                }
            }
        }
        // if both tables lead to the same off value that value can be deleted
        // AND the offSets of both tables
        for (int i = 1; i < on.offSets.length; i++) {
            tmpSet.set(on.offSets[i]);
            tmpSet.and(off.offSets[i]);
            tmpSet.remove(on.getCellIndex(0));
            if (!tmpSet.isEmpty()) {
                // found a few
                for (int j = 0; j < tmpSet.size(); j++) {
                    globalStep.reset();
                    globalStep.setType(SolutionType.FORCING_CHAIN_VERITY);
                    globalStep.addCandidateToDelete(tmpSet.get(j), i);
                    resetTmpChains();
                    addChain(on, tmpSet.get(j), i, false);
                    addChain(off, tmpSet.get(j), i, false);
                    replaceOrCopyStep();
                }
            }
        }
    }

    /**
     * Checks
     * <code>entry</code> for all combinations that lead to a conclusion.
     * <ul>
     *      <li>setting/deleting a candidate in/from a cell leades to that candidate
     *          beeing deleted from/set in that very cell -> original assumption was
     *          false.</li>
     *      <li>two chains from the same start lead to a candidate set in and deleted
     *          from the same cell -> assumption is false.</li>
     *      <li>two chains from the same start lead to two different values set in
     *          the same cell -> assumption is false.</li>
     *      <li>two chains from the same start lead to the same value set twice in
     *          one house -> assumption is false.</li>
     *      <li>chains from the same start lead to all instances of a candidate
     *          beeing removed from a cell -> assumption is false.</li>
     *      <li>chains from the same start lead to all instances of a candidate
     *          beeing removed from a house -> assumption is false.</li>
     * </ul>
     *
     * @param entry
     */
    private void checkOneChain(TableEntry entry) {
        if (entry.index == 0) {
            // table is empty -> nothing to do
            return;
        }
        if (DEBUG) {
            System.out.println("checkOneChain(): " + Chain.toString(entry.entries[0]));
        }
        // chain contains the invers of the assumption -> assumption is false
        if ((entry.isStrong(0) && entry.offSets[entry.getCandidate(0)].contains(entry.getCellIndex(0)))
                || (!entry.isStrong(0) && entry.onSets[entry.getCandidate(0)].contains(entry.getCellIndex(0)))) {
            if (DEBUG) {
//                System.out.println("  1");
            }
            globalStep.reset();
            globalStep.setType(SolutionType.FORCING_CHAIN_CONTRADICTION);
            if (entry.isStrong(0)) {
                globalStep.addCandidateToDelete(entry.getCellIndex(0), entry.getCandidate(0));
            } else {
                globalStep.addIndex(entry.getCellIndex(0));
                globalStep.addValue(entry.getCandidate(0));
            }
            globalStep.setEntity(Sudoku2.CELL);
            globalStep.setEntityNumber(tmpSet.get(0));
            resetTmpChains();
            addChain(entry, entry.getCellIndex(0), entry.getCandidate(0), !entry.isStrong(0));
            replaceOrCopyStep();
        }
        // same candidate set in and deleted from a cell -> assumption is false
        for (int i = 0; i < entry.onSets.length; i++) {
            if (DEBUG) {
//                System.out.println("  2");
            }
            // check all candidates
            tmpSet.set(entry.onSets[i]);
            tmpSet.and(entry.offSets[i]);
            if (!tmpSet.isEmpty()) {
                globalStep.reset();
                globalStep.setType(SolutionType.FORCING_CHAIN_CONTRADICTION);
                if (entry.isStrong(0)) {
                    globalStep.addCandidateToDelete(entry.getCellIndex(0), entry.getCandidate(0));
                } else {
                    globalStep.addIndex(entry.getCellIndex(0));
                    globalStep.addValue(entry.getCandidate(0));
                }
                globalStep.setEntity(Sudoku2.CELL);
                globalStep.setEntityNumber(tmpSet.get(0));
                resetTmpChains();
                addChain(entry, tmpSet.get(0), i, false);
                addChain(entry, tmpSet.get(0), i, true);
                replaceOrCopyStep();
            }
        }
        // two different values set in one and the same cell -> assumption is false
        for (int i = 1; i < entry.onSets.length; i++) {
            for (int j = i + 1; j < entry.onSets.length; j++) {
                if (DEBUG) {
//                    System.out.println("  3");
                }
                tmpSet.set(entry.onSets[i]);
                tmpSet.and(entry.onSets[j]);
                if (!tmpSet.isEmpty()) {
                    globalStep.reset();
                    globalStep.setType(SolutionType.FORCING_CHAIN_CONTRADICTION);
                    if (entry.isStrong(0)) {
                        globalStep.addCandidateToDelete(entry.getCellIndex(0), entry.getCandidate(0));
                    } else {
                        globalStep.addIndex(entry.getCellIndex(0));
                        globalStep.addValue(entry.getCandidate(0));
                    }
                    globalStep.setEntity(Sudoku2.CELL);
                    globalStep.setEntityNumber(tmpSet.get(0));
                    resetTmpChains();
                    addChain(entry, tmpSet.get(0), i, true);
                    addChain(entry, tmpSet.get(0), j, true);
                    replaceOrCopyStep();
                }
            }
        }
        // one value set twice in one house
        if (DEBUG) {
//            System.out.println("  4");
        }
        checkHouseSet(entry, Sudoku2.LINE_TEMPLATES, Sudoku2.LINE);
        if (DEBUG) {
//            System.out.println("  5");
        }
        checkHouseSet(entry, Sudoku2.COL_TEMPLATES, Sudoku2.COL);
        if (DEBUG) {
//            System.out.println("  6");
        }
        checkHouseSet(entry, Sudoku2.BLOCK_TEMPLATES, Sudoku2.BLOCK);

        // cell without candidates -> assumption false
        // chain creates a cell without candidates (delete sets OR ~allowedPositions, AND all
        // together, AND with ~set sets -> must not be 1
        // CAUTION: exclude all cells in which a value is already set
        tmpSet.setAll();
        for (int i = 1; i < entry.offSets.length; i++) {
            // all candidates, that can be deleted
            tmpSet1.set(entry.offSets[i]);
            // CAUTION: the candidate might not be in that cell anymore,
            // ANDing all candidates goes wrong then.
            // SOLUTION: WE fake a delete by setting all cells 1 where
            //           that candidate is not valid anymore
            tmpSet1.orNot(finder.getCandidates()[i]);
            tmpSet.and(tmpSet1);
        }
        // if a candidate can be set in a cell deleting all candidates
        // from that cell is impossible -> ignore those cells
        for (int i = 0; i < entry.onSets.length; i++) {
            tmpSet.andNot(entry.onSets[i]);
        }
        //cells that have a value set already are irrevelant
        tmpSet2.clear();
        for (int i = 1; i < finder.getPositions().length; i++) {
            tmpSet2.or(finder.getPositions()[i]);
        }
        tmpSet.andNot(tmpSet2);
        // tempSet now holds a one only for cells, where all candidates can be eliminated
        if (!tmpSet.isEmpty()) {
            for (int i = 0; i < tmpSet.size(); i++) {
                globalStep.reset();
                globalStep.setType(SolutionType.FORCING_CHAIN_CONTRADICTION);
                if (entry.isStrong(0)) {
                    globalStep.addCandidateToDelete(entry.getCellIndex(0), entry.getCandidate(0));
                } else {
                    globalStep.addIndex(entry.getCellIndex(0));
                    globalStep.addValue(entry.getCandidate(0));
                }
                globalStep.setEntity(Sudoku2.CELL);
                globalStep.setEntityNumber(tmpSet.get(i));
                resetTmpChains();
                int[] cands = sudoku.getAllCandidates(tmpSet.get(i));
                for (int j = 0; j < cands.length; j++) {
                    addChain(entry, tmpSet.get(i), cands[j], false);
                }
                if (entry.isStrong(0)) {
                    replaceOrCopyStep();
                } else {
                    replaceOrCopyStep();
                }
            }
        }
        // all instances of a candidate delete from a house -> assumption is false
        checkHouseDel(entry, Sudoku2.LINE_TEMPLATES, Sudoku2.LINE);
        checkHouseDel(entry, Sudoku2.COL_TEMPLATES, Sudoku2.COL);
        checkHouseDel(entry, Sudoku2.BLOCK_TEMPLATES, Sudoku2.BLOCK);
    }

    /**
     * Check, if all instances of a candidate are deleted from one house. If so,
     * the assumption was invalid: 
     * 
     * <ul> 
     *      <li>Get all instances of the candidate
     *          in the house</li> 
     *      <li>If there are candidates and the set equals the
     *          offSet, step was found</li>
     * </ul>
     *
     * @param entry
     * @param houseSets
     * @param entityTyp
     */
    private void checkHouseDel(TableEntry entry, SudokuSet[] houseSets, int entityTyp) {
        // check all candidates
        for (int i = 1; i < entry.offSets.length; i++) {
            // in all houses
            for (int j = 0; j < houseSets.length; j++) {
                tmpSet.set(houseSets[j]);
                tmpSet.and(finder.getCandidatesAllowed()[i]);
                if (!tmpSet.isEmpty() && tmpSet.andEquals(entry.offSets[i])) {
                    globalStep.reset();
                    globalStep.setType(SolutionType.FORCING_CHAIN_CONTRADICTION);
                    if (entry.isStrong(0)) {
                        globalStep.addCandidateToDelete(entry.getCellIndex(0), entry.getCandidate(0));
                    } else {
                        globalStep.addIndex(entry.getCellIndex(0));
                        globalStep.addValue(entry.getCandidate(0));
                    }
                    globalStep.setEntity(entityTyp);
                    globalStep.setEntityNumber(j);
                    resetTmpChains();
                    for (int k = 0; k < tmpSet.size(); k++) {
                        addChain(entry, tmpSet.get(k), i, false);
                    }
                    if (entry.isStrong(0)) {
                        replaceOrCopyStep();
                    } else {
                        replaceOrCopyStep();
                    }
                }
            }
        }
    }

    /**
     * Checks, if an assumption leads to the same value set at least twice in one house.
     *
     * @param entry
     * @param houseSets
     * @param entityTyp
     */
    private void checkHouseSet(TableEntry entry, SudokuSet[] houseSets, int entityTyp) {
        for (int i = 1; i < entry.onSets.length; i++) {
            for (int j = 0; j < houseSets.length; j++) {
                tmpSet.setAnd(houseSets[j], entry.onSets[i]);
                if (tmpSet.size() > 1) {
                    globalStep.reset();
                    globalStep.setType(SolutionType.FORCING_CHAIN_CONTRADICTION);
                    if (entry.isStrong(0)) {
                        globalStep.addCandidateToDelete(entry.getCellIndex(0), entry.getCandidate(0));
                    } else {
                        globalStep.addIndex(entry.getCellIndex(0));
                        globalStep.addValue(entry.getCandidate(0));
                    }
                    globalStep.setEntity(entityTyp);
                    globalStep.setEntityNumber(j);
                    resetTmpChains();
                    for (int k = 0; k < tmpSet.size(); k++) {
                        addChain(entry, tmpSet.get(k), i, true);
                    }
                    if (entry.isStrong(0)) {
                        replaceOrCopyStep();
                    } else {
                        replaceOrCopyStep();
                    }
                }
            }
        }
    }

    /**
     * For every table check, if it contains a link, that goes back to the
     * originating cell of the table entry. If so, a possible Nice Loop exists.
     * A Nice Loop in this implementation always starts and ends with a
     * {@link Chain#NORMAL_NODE}.
     *
     * @param tables
     */
    private void checkNiceLoops(TableEntry[] tables) {
        // check all table entries
        for (int i = 0; i < tables.length; i++) {
            int startIndex = tables[i].getCellIndex(0);
            for (int j = 1; j < tables[i].index; j++) {
                if (tables[i].getNodeType(j) == Chain.NORMAL_NODE
                        && tables[i].getCellIndex(j) == startIndex) {
                    // ok - direct loop
                    checkNiceLoop(tables[i], j);
                }
            }
        }
    }

    /**
     * AICs are checked separately: The end of the chain has to be: 
     * 
     * <ul>
     *      <li>on-entry for the same candidate as the start cell (Type 1), if the
     *          combined buddies of start and end cell can eliminate more than one
     *          candidate</li> 
     *      <li>on-entry for a different candidate if the end cell
     *          sees the start cell and if the start cell contains a candidate of the
     *          chain end and the end cell contains a candidate of the chain
     *          start</li>
     * </ul>
     *
     * @param tables Only offTables are allowed (AICs start with a strong link)
     */
    private void checkAics(TableEntry[] tables) {
        for (int i = 0; i < tables.length; i++) {
            int startIndex = tables[i].getCellIndex(0);
            int startCandidate = tables[i].getCandidate(0);
            SudokuSetBase buddies = Sudoku2.buddies[startIndex];
            for (int j = 1; j < tables[i].index; j++) {
                if (tables[i].getNodeType(j) != Chain.NORMAL_NODE
                        || !tables[i].isStrong(j) || tables[i].getCellIndex(j) == startIndex) {
                    // not now
                    continue;
                }
                if (startCandidate == tables[i].getCandidate(j)) {
                    // check Type 1
                    tmpSet.set(buddies);
                    tmpSet.and(Sudoku2.buddies[tables[i].getCellIndex(j)]);
                    tmpSet.and(finder.getCandidates()[startCandidate]);
                    if (!tmpSet.isEmpty() && tmpSet.size() >= 2) {
                        // everything else is already covered by a Nice Loop
                        checkAic(tables[i], j);
                    }
                } else {
                    if (!buddies.contains(tables[i].getCellIndex(j))) {
                        // cant be Type 2
                        continue;
                    }
                    // it should be enough, that the end cell holds the start
                    // candidate. Only the start cell holding the end candidate
                    // is useless here, is covered as Nice Loop
//                    if (sudoku.isCandidate(tables[i].getCellIndex(j), startCandidate)
//                            && sudoku.isCandidate(startIndex, tables[i].getCandidate(j))) {
                    if (sudoku.isCandidate(tables[i].getCellIndex(j), startCandidate)) {
                        checkAic(tables[i], j);
                    }
                }
            }
        }
    }

    /**
     * If the first and the last cell of the chain are identical, the
     * chain is a Nice Loop.<br><br>
     *
     * Discontinuous Nice Loop: 
     * 
     * <dl> 
     *  <dt>First and last link are weak for the
     *      same candidate:</dt> 
     *  <dd>Candidate can be eliminated from the start
     *      cell</dd> 
     *  <dt>First and last link are strong for the same candidate:</dt>
     *  <dd>Candidate can be set in the start cell (in the step all other
     *      candidates are eliminated from the cell, leads to a naked single)</dd>
     *  <dt>One link is weak and the other strong, they are for different
     *      candidates:</dt> 
     *  <dd>The candidate from the weak link can be eliminated
     *      from the start cell</dd> 
     * </dl>
     *
     * Continuous Nice Loop: 
     * 
     * <dl> 
     *  <dt>Two weak links:</dt> 
     *  <dd>First cell must be bivalue, candidates must be different</dd> 
     *  <dt>Two strong links:</dt>
     *  <dd>Candidates must be different</dd> 
     *  <dt>One link strong, the other weak:</dt> 
     *  <dd>Both links must be for the same candidate</dd> 
     * </dl>
     *
     * If a Continuous Nice Loop is present, the following eliminations are
     * possible: 
     * 
     * <dl> 
     *  <dt>One cell reached and left with a strong link:</dt>
     *  <dd>All candidates not present in the strong links can be eliminated from
     *      the cell</dd> 
     *  <dt>Weak link between cells:</dt> 
     *  <dd>Link candidate can be eliminated from all cells, that see both cells 
     *      of the link</dd> 
     * </dl>
     *
     * Chains are created backwards. We cant be sure, if the first link really
     * leaves the cell before we have created the actual chain. All chains,
     * which first link remains in the start cell, are ignored.
     *
     * @param entry TableEntry of the start link
     * @param entryIndex Index of the second to last link in the chain 
     *      (the last link links back to the start cell and is not present 
     *      in the table)
     */
    private void checkNiceLoop(TableEntry entry, int entryIndex) {
        // A Nice Loop must be at least 3 links long
        if (entry.getDistance(entryIndex) <= 2) {
            // Chain too short -> no eliminations possible
            if (DEBUG) {
//                System.out.println("Chain too short: " + printTableEntry(entry.entries[0], alses) + "/"
//                        + printTableEntry(entry.entries[entryIndex], alses) + "/" + entry.getDistance(entryIndex));
            }
            return;
        }
        if (DEBUG) {
//            System.out.println("Longer chain found");
        }

        // check loop type
        globalStep.reset();
        globalStep.setType(SolutionType.DISCONTINUOUS_NICE_LOOP);
        resetTmpChains();
        addChain(entry, entry.getCellIndex(entryIndex), entry.getCandidate(entryIndex),
                entry.isStrong(entryIndex), true);
        if (globalStep.getChains().isEmpty()) {
            // invalid chain -> builds a lasso somewhere -> ignore it!
            if (DEBUG) {
//                System.out.println("Chain is a lasso -> ignored");
            }
            return;
        }
        Chain localTmpChain = globalStep.getChains().get(0);
        if (localTmpChain.getCellIndex(0) == localTmpChain.getCellIndex(1)) {
            // invalid for a Nice Loop, first link must leave start cell!
            return;
        }
        int[] nlChain = localTmpChain.getChain();
        int nlChainIndex = localTmpChain.getEnd();
        int nlChainLength = localTmpChain.getLength();

        boolean firstLinkStrong = entry.isStrong(1);
        boolean lastLinkStrong = entry.isStrong(entryIndex);
        int startCandidate = entry.getCandidate(0);
        int endCandidate = entry.getCandidate(entryIndex);
        int startIndex = entry.getCellIndex(0);

        if (!firstLinkStrong && !lastLinkStrong && startCandidate == endCandidate) {
            // Discontinuous -> eliminate startCandidate in startIndex
            globalStep.addCandidateToDelete(startIndex, startCandidate);
//            // auf mögliche AIC prüfen: die strong links müssen normale links sein
//            if (Chain.getSNodeType(nlChain[1]) == Chain.NORMAL_NODE && Chain.getSNodeType(nlChain[nlChainIndex - 1]) == Chain.NORMAL_NODE) {
//                tmpSet.set(Sudoku2.buddies[Chain.getSCellIndex(nlChain[1])]);
//                tmpSet.and(Sudoku2.buddies[Chain.getSCellIndex(nlChain[nlChainIndex - 1])]);
//                tmpSet.and(finder.getCandidates()[startCandidate]);
//                if (tmpSet.size() > 1) {
//                    globalStep.setType(SolutionType.AIC);
//                    for (int i = 0; i < tmpSet.size(); i++) {
//                        if (tmpSet.get(i) != startIndex) {
//                            globalStep.addCandidateToDelete(tmpSet.get(i), startCandidate);
//                        }
//                    }
//                    localTmpChain.start++;
//                    localTmpChain.end--;
//                }
//            }
        } else if (firstLinkStrong && lastLinkStrong && startCandidate == endCandidate) {
            // Discontinuous -> eliminate all candidates from startIndex except startCandidate
            int[] cands = sudoku.getAllCandidates(startIndex);
            for (int i = 0; i < cands.length; i++) {
                if (cands[i] != startCandidate) {
                    globalStep.addCandidateToDelete(startIndex, cands[i]);
                }
            }
        } else if (firstLinkStrong != lastLinkStrong && startCandidate != endCandidate) {
            // Discontinuous -> eliminate weak link
            if (!firstLinkStrong) {
                globalStep.addCandidateToDelete(startIndex, startCandidate);
//                if (Chain.getSNodeType(nlChain[1]) == Chain.NORMAL_NODE &&
//                        sudoku.isCandidate(Chain.getSCellIndex(nlChain[1]), endCandidate)) {
//                    globalStep.setType(SolutionType.AIC);
//                    globalStep.addCandidateToDelete(Chain.getSCellIndex(nlChain[1]), endCandidate);
//                    localTmpChain.start++;
//                }
            } else {
                globalStep.addCandidateToDelete(startIndex, endCandidate);
//                if (Chain.getSNodeType(nlChain[nlChainIndex - 1]) == Chain.NORMAL_NODE &&
//                        sudoku.isCandidate(Chain.getSCellIndex(nlChain[nlChainIndex - 1]), startCandidate)) {
//                    globalStep.setType(SolutionType.AIC);
//                    globalStep.addCandidateToDelete(Chain.getSCellIndex(nlChain[nlChainIndex - 1]), startCandidate);
//                    localTmpChain.end--;
//                }
            }
        } else if ((!firstLinkStrong && !lastLinkStrong && sudoku.getAnzCandidates(startIndex) == 2 && startCandidate != endCandidate)
                || (firstLinkStrong && lastLinkStrong && startCandidate != endCandidate)
                || (firstLinkStrong != lastLinkStrong && startCandidate == endCandidate)) {
            // Continuous -> check possible eliminations
            globalStep.setType(SolutionType.CONTINUOUS_NICE_LOOP);
            // cell entered and left with a strong link: strong between cells, then weak link within cell, then strong link again
            // weak link between cells: obvious
            // CAUTION: startCell can also be reached and left with a strong link
            for (int i = 0; i <= nlChainIndex; i++) {
                if ((i == 0 && (firstLinkStrong && lastLinkStrong))
                        || (i > 0 && (Chain.isSStrong(nlChain[i]) && i <= nlChainIndex - 2
                        && Chain.getSCellIndex(nlChain[i - 1]) != Chain.getSCellIndex(nlChain[i])))) {
                    // possible cell with two strong links: check the next links
                    if (i == 0
                            || (!Chain.isSStrong(nlChain[i + 1]) && Chain.getSCellIndex(nlChain[i]) == Chain.getSCellIndex(nlChain[i + 1])
                            && Chain.isSStrong(nlChain[i + 2]) && Chain.getSCellIndex(nlChain[i + 1]) != Chain.getSCellIndex(nlChain[i + 2]))) {
                        // we are save here: group nodes and ALS cannot provide weak links in the cells through which they are reached
                        // eliminate all candidates except the strong link candidates from nlChain[i]
                        int c1 = Chain.getSCandidate(nlChain[i]);
                        int c2 = Chain.getSCandidate(nlChain[i + 2]);
                        if (i == 0) {
                            c1 = startCandidate;
                            c2 = endCandidate;
                        }
                        int[] cands = sudoku.getAllCandidates(Chain.getSCellIndex(nlChain[i]));
                        for (int j = 0; j < cands.length; j++) {
                            if (cands[j] != c1 && cands[j] != c2) {
                                globalStep.addCandidateToDelete(Chain.getSCellIndex(nlChain[i]), cands[j]);
                            }
                        }
                    }
                }
                // this condition is nonsens (I have no idea what I thought when I wrote it)
                // a weak link to the start cell will be the last item in the chain; a weak link to the second cell will be the second item
                // in the chain -> no special cases needed here
//                if ((i == 0 && (i == -1) ||
//                        (i > 0) && (!Chain.isSStrong(nlChain[i]) && Chain.getSCellIndex(nlChain[i - 1]) != Chain.getSCellIndex(nlChain[i])))) {
                if ((i > 0) && (!Chain.isSStrong(nlChain[i]) && Chain.getSCellIndex(nlChain[i - 1]) != Chain.getSCellIndex(nlChain[i]))) {
                    // weak link between cells
                    // CAUTION:  If one of the cells is entry point for an ALS, nothing can be eliminated;
                    //           if one or both cells are group nodes, only candidates, that see all of the 
                    //           group node cells, can be eliminated
                    // 20090224: entries to ALS can be treated like normal group nodes: all candidates in the
                    //           same house that dont belong to the node or the ALS can be eliminated
                    //           plus: all ALS candidates that are not entry/exit candidates eliminate all
                    //           candidates they can see
                    // 20100218: If an ALS node forces a digit (ALS left via more than one candidate -> all
                    //           candidates except one are eliminated in another cell) the leaving weak link is
                    //           missing (next link is strong to forced cell); in that case all other candidates
                    //           in the forced cell are exit candidates and may not be eliminated
                    int actCand = Chain.getSCandidate(nlChain[i]);
                    Chain.getSNodeBuddies(nlChain[i - 1], actCand, alses, tmpSet);
                    Chain.getSNodeBuddies(nlChain[i], actCand, alses, tmpSet1);
                    tmpSet.and(tmpSet1);
                    tmpSet.andNot(tmpSetC);
                    tmpSet.remove(startIndex);
                    tmpSet.and(finder.getCandidates()[actCand]);
                    if (!tmpSet.isEmpty()) {
                        for (int j = 0; j < tmpSet.size(); j++) {
                            globalStep.addCandidateToDelete(tmpSet.get(j), actCand);
                        }
                    }
                    if (Chain.getSNodeType(nlChain[i]) == Chain.ALS_NODE) {
                        // there could be more than one exit candidate (the node following an ALS node
                        // must be weak; if it is strong, the weak link contains more than one
                        // candidate and was omitted
                        boolean isForceExit = i < nlChainIndex && Chain.isSStrong(nlChain[i + 1]);
                        int nextCellIndex = Chain.getSCellIndex(nlChain[i + 1]);
                        tmpSet2.clear();
                        if (isForceExit) {
                            // all candidates in the next cell (except the one providing the strong link)
                            // are exit candidates
                            int forceCand = Chain.getSCandidate(nlChain[i + 1]);
                            sudoku.getCandidateSet(nextCellIndex, tmpSet2);
                            tmpSet2.remove(forceCand);
                        } else {
                            if (i < nlChainIndex) {
                                tmpSet2.add(Chain.getSCandidate(nlChain[i + 1]));
                            }
                        }
                        Als als = alses.get(Chain.getSAlsIndex(nlChain[i]));
                        for (int j = 1; j < als.buddiesPerCandidat.length; j++) {
                            if (j == actCand || tmpSet2.contains(j) || als.buddiesPerCandidat[j] == null) {
                                // RC -> handled from code above
                                // or exit candidate (handled by the next link or below)
                                // or candidate not in ALS
                                continue;
                            }
                            tmpSet.set(als.buddiesPerCandidat[j]);
                            //tmpSet.andNot(tmpSetC); not exactely sure, but I think cannibalism is allowed here
                            //tmpSet.remove(startIndex);
                            tmpSet.and(finder.getCandidates()[j]);
                            if (!tmpSet.isEmpty()) {
                                for (int k = 0; k < tmpSet.size(); k++) {
                                    globalStep.addCandidateToDelete(tmpSet.get(k), j);
                                }
                            }
                        }
                        // special case forced next cell: exit candidates have to be handled here
                        if (isForceExit) {
                            // for all exit candidates: eliminate everything that sees all instances
                            // of that cand in the als and in the next cell
                            tmpSet1.set(Sudoku2.buddies[nextCellIndex]);
                            for (int j = 0; j < tmpSet2.size(); j++) {
                                int actExitCand = tmpSet2.get(j);
                                tmpSet.set(als.buddiesPerCandidat[actExitCand]);
                                tmpSet.and(tmpSet1);
                                //tmpSet.andNot(tmpSetC);
                                //tmpSet.remove(startIndex);
                                tmpSet.and(finder.getCandidates()[actExitCand]);
                                if (!tmpSet.isEmpty()) {
                                    for (int k = 0; k < tmpSet.size(); k++) {
                                        globalStep.addCandidateToDelete(tmpSet.get(k), actExitCand);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!globalStep.getCandidatesToDelete().isEmpty()) {
            // ok, this is a valid step!
            // check for group or ALS nodes
            boolean grouped = false;
            Chain newChain = globalStep.getChains().get(0);
            for (int i = newChain.getStart(); i <= newChain.getEnd(); i++) {
                if (Chain.getSNodeType(newChain.getChain()[i]) != Chain.NORMAL_NODE) {
                    grouped = true;
                    break;
                }
            }
            if (grouped) {
                if (globalStep.getType() == SolutionType.DISCONTINUOUS_NICE_LOOP) {
                    globalStep.setType(SolutionType.GROUPED_DISCONTINUOUS_NICE_LOOP);
                }
                if (globalStep.getType() == SolutionType.CONTINUOUS_NICE_LOOP) {
                    globalStep.setType(SolutionType.GROUPED_CONTINUOUS_NICE_LOOP);
                }
                if (globalStep.getType() == SolutionType.AIC) {
                    globalStep.setType(SolutionType.GROUPED_AIC);
                }
            }
            if (onlyGroupedNiceLoops && !grouped) {
                return;
            }
            replaceOrCopyStep();
//            // only one Nice Loop per set of eliminations
//            String del = globalStep.getCandidateString();
//            Integer oldIndex = deletesMap.get(del);
//            if (oldIndex != null && steps.get(oldIndex.intValue()).getChainLength() <= nlChainLength) {
//                // an eqivalent does exist and is shorter than the new one
//                return;
//            }
//            deletesMap.put(del, steps.size());
//            // the chain has to be copied
//            newChain = (Chain) globalStep.getChains().get(0).clone();
//            globalStep.getChains().clear();
//            globalStep.getChains().add(newChain);
//            adjustChains(globalStep);
//            steps.add((SolutionStep) globalStep.clone());
        }
    }

    /**
     * Checks whether the AIC does make an elimination; if so builds the step
     * and adds it to steps.
     *
     * @param entry The entry for the start cell
     * @param entryIndex index of the end cell of the AIC
     */
    private void checkAic(TableEntry entry, int entryIndex) {
        // minimum length: 3 links
        if (entry.getDistance(entryIndex) <= 2) {
            // chain too short -> no eliminations possible
            return;
        }

        globalStep.reset();
        globalStep.setType(SolutionType.AIC);

        // check whether eliminations are possible
        int startCandidate = entry.getCandidate(0);
        int endCandidate = entry.getCandidate(entryIndex);
        int startIndex = entry.getCellIndex(0);
        int endIndex = entry.getCellIndex(entryIndex);
        if (startCandidate == endCandidate) {
            // type 1 AIC: delete all candidates that can see both ends of the chain
            tmpSet.set(Sudoku2.buddies[startIndex]);
            tmpSet.and(Sudoku2.buddies[endIndex]);
            tmpSet.and(finder.getCandidates()[startCandidate]);
            if (!tmpSet.isEmpty()) {
                for (int i = 0; i < tmpSet.size(); i++) {
                    globalStep.addCandidateToDelete(tmpSet.get(i), startCandidate);
                }
            }
        } else {
            // Type 2 AIC: Delete start candidate in end cell and vice versa
            if (sudoku.isCandidate(startIndex, endCandidate)) {
                globalStep.addCandidateToDelete(startIndex, endCandidate);
            }
            if (sudoku.isCandidate(endIndex, startCandidate)) {
                globalStep.addCandidateToDelete(endIndex, startCandidate);
            }
            if (globalStep.getAnzCandidatesToDelete() < 2) {
                // only one elimination, could conflict with type 1
                return;
            }
        }
        if (globalStep.getAnzCandidatesToDelete() == 0) {
            // nothing to do
            return;
        }
        // build the chain
        resetTmpChains();
        addChain(entry, entry.getCellIndex(entryIndex), entry.getCandidate(entryIndex), entry.isStrong(entryIndex), false, true);
        if (globalStep.getChains().isEmpty()) {
            // something is wrong with that chain
            if (DEBUG) {
//                System.out.println("checkAIC(): Found eliminations, but no suitable chain!");
            }
            return;
        }
        // check for group nodes
        boolean grouped = false;
        Chain newChain = globalStep.getChains().get(0);
        for (int i = newChain.getStart(); i <= newChain.getEnd(); i++) {
            if (Chain.getSNodeType(newChain.getChain()[i]) != Chain.NORMAL_NODE) {
                grouped = true;
                break;
            }
        }
        if (grouped) {
            if (globalStep.getType() == SolutionType.DISCONTINUOUS_NICE_LOOP) {
                globalStep.setType(SolutionType.GROUPED_DISCONTINUOUS_NICE_LOOP);
            }
            if (globalStep.getType() == SolutionType.CONTINUOUS_NICE_LOOP) {
                globalStep.setType(SolutionType.GROUPED_CONTINUOUS_NICE_LOOP);
            }
            if (globalStep.getType() == SolutionType.AIC) {
                globalStep.setType(SolutionType.GROUPED_AIC);
            }
        }
        if (onlyGroupedNiceLoops && !grouped) {
            return;
        }
        // check for steps with the same eliminations
        replaceOrCopyStep();
//        String del = globalStep.getCandidateString();
//        Integer oldIndex = deletesMap.get(del);
//        if (oldIndex != null && steps.get(oldIndex.intValue()).getChainLength() <= globalStep.getChains().get(0).getLength()) {
//            // a similar chain already exists and is shorter than the new one -> ignore it
//            return;
//        }
//        deletesMap.put(del, steps.size());
//        // chain must be copied
//        newChain = (Chain) globalStep.getChains().get(0).clone();
//        globalStep.getChains().clear();
//        globalStep.getChains().add(newChain);
//        adjustChains(globalStep);
//        steps.add((SolutionStep) globalStep.clone());
    }

    /**
     * Fills the tables with all initial consequences. One table exists for
     * every outcome (set/not set) of every candidate in the sudoku. If
     * {@link #chainsOnly} is set, only direct dependencies are recorded. If it
     * is not set, {@link #getTableEntry(solver.TableEntry, int, int, boolean) }
     * is used to dig a little deeper.<br><br>
     *
     * All consequences depend on the original sudoku. Especially when searching
     * for nets this can be confusing: the current result (e.g. a Hidden Single)
     * comes from eliminating one candidate in a house. But if there were more
     * than two candidates initially in that house, it depends on all of them.
     */
    private void fillTables() {
        // initalize tables
        for (int i = 0; i < onTable.length; i++) {
            onTable[i].reset();
            offTable[i].reset();
        }
        extendedTableMap.clear();
        extendedTableIndex = 0;

        if (chainsOnly) {
            // collect only direct links -> should create only chains, not nets
            for (int i = 0; i < sudoku.getCells().length; i++) {
                if (sudoku.getValue(i) != 0) {
                    // cell not empty -> ignore
                    continue;
                }
                for (int j = 1; j <= 9; j++) {
                    if (!sudoku.isCandidate(i, j)) {
                        // not a candidate -> ignore
                        continue;
                    }
                    // ok, valid candidate: collect the links
                    int cand = j;

                    onTable[i * 10 + cand].addEntry(i, cand, true);
                    offTable[i * 10 + cand].addEntry(i, cand, false);
                    // candidate ON deletes all other candidates from the cell and
                    // the candidate itself from all other cells in the houses
                    // candidate OFF sets all resulting singles (hidden und naked)
                    // all collected results depend directly on the premise, so
                    // retIndex is always 0

                    // first the candidates in the cell itself
                    int[] cands = sudoku.getAllCandidates(i);
                    for (int k = 0; k < cands.length; k++) {
                        int otherCand = cands[k];
                        if (otherCand == cand) {
                            // not cand itself
                            continue;
                        }
                        // if cand is ON, otherCand has to be OFF
                        onTable[i * 10 + cand].addEntry(i, otherCand, false);
//                        if (sudoku.getAnzCandidates(i) == 2) {
                        if (cands.length == 2) {
                            // only two candidates in cell -> if
                            // cand is OFF, otherCand has to be ON
                            offTable[i * 10 + cand].addEntry(i, otherCand, true);
                        }
                    }
                    tmpSet1.set(finder.getCandidates()[cand]);
                    tmpSet1.remove(i);
                    for (int constrIndex = 0; constrIndex < Sudoku2.CONSTRAINTS[i].length; constrIndex++) {
                        // number of candidates remaining in the current house: 1 - only
                        // cand itself; 2 - strong link; > 2 - weak links
                        int constr = Sudoku2.CONSTRAINTS[i][constrIndex];
                        int anzCands = sudoku.getFree()[constr][cand];
                        if (anzCands < 2) {
                            // nothing to do
                            continue;
                        }
                        // get the candidates
                        tmpSet.setAnd(tmpSet1, Sudoku2.ALL_CONSTRAINTS_TEMPLATES[constr]);
                        if (tmpSet.isEmpty()) {
                            // no candidates left...
                            continue;
                        }
                        for (int k = 0; k < tmpSet.size(); k++) {
                            // if cand is ON, all other candidates are OFF
                            onTable[i * 10 + cand].addEntry(tmpSet.get(k), cand, false);
                        }
                        if (anzCands == 2) {
                            // strong link: if cand is OFF, the other candidate has to be ON
                            offTable[i * 10 + cand].addEntry(tmpSet.get(0), cand, true);
                        }
                    }
                }
            }
        } else {
            // we are looking for nets!
            // iterate through all cells and candidates: set and delete the
            // candidate and record all dependencies (look ahead more than one iteration).
            // one copy is enough, Sudoku2.set() copies the contents of the sudoku
            if (DEBUG) {
                System.out.println("BEGIN fillTables()");
            }
            savedSudoku = sudoku.clone();
            simpleFinder.setSudoku(savedSudoku);
            for (int i = 0; i < savedSudoku.getCells().length; i++) {
//            if (i != 52) {
//                // debugging only
//                continue;
//            }
                if (savedSudoku.getValue(i) != 0) {
                    // cell is already set -> ignore it
                    continue;
                }
                int[] cands = savedSudoku.getAllCandidates(i);
                for (int j = 0; j < cands.length; j++) {
                    // once for every candidate
                    int cand = cands[j];
                    // candidate is ON
                    sudoku.set(savedSudoku);
                    simpleFinder.setSudoku(sudoku);
                    getTableEntry(onTable[i * 10 + cand], i, cand, true);
                    // candidate is OFF
                    sudoku.set(savedSudoku);
                    simpleFinder.setSudoku(sudoku);
                    getTableEntry(offTable[i * 10 + cand], i, cand, false);
                }
            }
            sudoku.set(savedSudoku);
            if (DEBUG) {
                System.out.println("END fillTables()");
            }
        }
    }

    /**
     * Fills {@link #extendedTable } with all group nodes. Group nodes are
     * always handled as chains - only direct implications are stored.<br><br>
     *
     * Collect all group nodes. For every group node do: 
     * 
     * <ul> 
     *  <li>make a table for every group node (on and off);</li> 
     *  <li>write the index in extendedTable into extendedTableMap 
     *      (together with the group node entry)</li> 
     *  <li>
     *  <dl> 
     *      <dt>for ON entries:</dt> 
     *      <dd>every candidate that can see all group node cells is turned OFF; 
     *          every other group node that can see (and doesnt overlap) the 
     *          actual group node is turned OFF</dd>
     *      <dt>for OFF entries:</dt> 
     *      <dd>if a single candidate in one of the houses
     *          of the group node exists, it is turned ON; if only one other
     *          non-overlapping group node (without extra non-group nodes) 
     *          exists in one of the houses, it is turned ON</dd> 
     *  </dl></li> 
     * </ul> 
     * 
     * Links to the group nodes have to be added in normal tables, that trigger 
     * the group node: 
     * 
     * <ul>
     *  <li>    
     *  <dl> 
     *      <dt>for ON entries:</dt> 
     *      <dd>if only one additional candidate exists in one of the houses, 
     *          the entry is added to that candidate's offTable</dd> 
     *      <dt>for OFF entries:</dt> 
     *      <dd>the entry is added to the onTable of every candidate, that 
     *          sees the group node</dd> 
     *     
     *  </dl></li> 
     * </ul>
     *
     * <b>CAUTION:</b> Must be called AFTER {@link #fillTables() } or the
     * attributes {@link #extendedTableMap } and {@link #extendedTableIndex }
     * will not be properly initialized; the initialization cannot be moved
     * here, because it must be possible to call 
     * {@link #fillTablesWithGroupNodes()} and {@link #fillTablesWithAls() } 
     * in arbitrary order.
     */
    private void fillTablesWithGroupNodes() {
        // get all the group nodes
        groupNodes = finder.getGroupNodes();
        // now handle them
        for (int i = 0; i < groupNodes.size(); i++) {
            GroupNode gn = groupNodes.get(i);
            // one table for ON
            TableEntry onEntry = getNextExtendedTableEntry(extendedTableIndex);
            onEntry.addEntry(gn.index1, gn.index2, gn.index3, Chain.GROUP_NODE, gn.cand, true, 0, 0, 0, 0, 0, 0);
            extendedTableMap.put(onEntry.entries[0], extendedTableIndex);
            extendedTableIndex++;
            if (DEBUG) {
                //System.out.println("GN: " + Chain.toString(onEntry.entries[0]) + "(" + (extendedTableIndex - 1) + "/" + onEntry.entries[0] + ")");
            }
            // and one for OFF
            TableEntry offEntry = getNextExtendedTableEntry(extendedTableIndex);
            offEntry.addEntry(gn.index1, gn.index2, gn.index3, Chain.GROUP_NODE, gn.cand, false, 0, 0, 0, 0, 0, 0);
            extendedTableMap.put(offEntry.entries[0], extendedTableIndex);
            extendedTableIndex++;
            if (DEBUG) {
                //System.out.println("GN: " + Chain.toString(offEntry.entries[0]) + "(" + (extendedTableIndex - 1) + "/" + offEntry.entries[0] + ")");
            }

            // ok: collect candidates that can see the group node
            tmpSet.setAnd(finder.getCandidates()[gn.cand], gn.buddies);
            if (!tmpSet.isEmpty()) {
                // every candidate that can see the group node is turned of by the on-entry
                // every candidate's onTable triggers the offEntry
                for (int j = 0; j < tmpSet.size(); j++) {
                    int index = tmpSet.get(j);
                    onEntry.addEntry(index, gn.cand, false);
                    TableEntry tmp = onTable[index * 10 + gn.cand];
                    tmp.addEntry(gn.index1, gn.index2, gn.index3, Chain.GROUP_NODE, gn.cand, false, 0, 0, 0, 0, 0, 0);
                }
                // if in a given house only one additional candidate exists, it is turned on by the off-entry
                // the candidates offTable triggers the onEntry
                tmpSet1.setAnd(tmpSet, Sudoku2.BLOCK_TEMPLATES[gn.block]);
                if (!tmpSet1.isEmpty() && tmpSet1.size() == 1) {
                    offEntry.addEntry(tmpSet1.get(0), gn.cand, true);
                    TableEntry tmp = offTable[tmpSet1.get(0) * 10 + gn.cand];
                    tmp.addEntry(gn.index1, gn.index2, gn.index3, Chain.GROUP_NODE, gn.cand, true, 0, 0, 0, 0, 0, 0);
                }
                tmpSet1.set(tmpSet);
                if (gn.line != -1) {
                    tmpSet1.and(Sudoku2.LINE_TEMPLATES[gn.line]);
                } else {
                    tmpSet1.and(Sudoku2.COL_TEMPLATES[gn.col]);
                }
                if (!tmpSet1.isEmpty() && tmpSet1.size() == 1) {
                    offEntry.addEntry(tmpSet1.get(0), gn.cand, true);
                    TableEntry tmp = offTable[tmpSet1.get(0) * 10 + gn.cand];
                    tmp.addEntry(gn.index1, gn.index2, gn.index3, Chain.GROUP_NODE, gn.cand, true, 0, 0, 0, 0, 0, 0);
                }
            }

            // next: a group node can of course be connected to another group node
            // check all other group nodes for the same candidate: if they share one of
            // the houses but don't overlap, they are connected
            // NOTE: there cant be more than three group nodes in one house
            int lineAnz = 0;
            int line1Index = -1;
            int colAnz = 0;
            int col1Index = -1;
            int blockAnz = 0;
            int block1Index = -1;
            GroupNode gn2;
            for (int j = 0; j < groupNodes.size(); j++) {
                gn2 = groupNodes.get(j);
                if (j == i) {
                    // thats us, skip
                    continue;
                }
                if (gn.cand != gn2.cand) {
                    // wrong candidate -> skip
                    continue;
                }
                // check for overlap
                tmpSet2.set(gn.indices);
                if (!tmpSet2.andEmpty(gn2.indices)) {
                    // group nodes do overlap -> skip
                    continue;
                }
                if (gn.line != -1 && gn.line == gn2.line) {
                    // store it for later use
                    lineAnz++;
                    if (lineAnz == 1) {
                        line1Index = j;
                    }
                    // group node is in the same line -> on-entry turns it off
                    onEntry.addEntry(gn2.index1, gn2.index2, gn2.index3, Chain.GROUP_NODE, gn.cand, false, 0, 0, 0, 0, 0, 0);
                }
                if (gn.col != -1 && gn.col == gn2.col) {
                    // store it for later use
                    colAnz++;
                    if (colAnz == 1) {
                        col1Index = j;
                    }
                    // group node is in the same col -> on-entry turns it off
                    onEntry.addEntry(gn2.index1, gn2.index2, gn2.index3, Chain.GROUP_NODE, gn.cand, false, 0, 0, 0, 0, 0, 0);
                }
                if (gn.block == gn2.block) {
                    // store it for later use
                    blockAnz++;
                    if (blockAnz == 1) {
                        block1Index = j;
                    }
                    // group node is in the same block -> on-entry turns it off
                    onEntry.addEntry(gn2.index1, gn2.index2, gn2.index3, Chain.GROUP_NODE, gn.cand, false, 0, 0, 0, 0, 0, 0);
                }
            }
            // if in one house was only one additional group node and if there is no additional single candidate
            // in that same house -> group node is turned on by off-entry
            // has to be done in seperate blocks,sincemore than one condition can be true!
            if (lineAnz == 1) {
                gn2 = groupNodes.get(line1Index);
                tmpSet.set(Sudoku2.LINE_TEMPLATES[gn.line]);
                tmpSet.and(finder.getCandidates()[gn.cand]);
                tmpSet.andNot(gn.indices);
                tmpSet.andNot(gn2.indices);
                if (tmpSet.isEmpty()) {
                    // no additional candidates -> write it
                    offEntry.addEntry(gn2.index1, gn2.index2, gn2.index3, Chain.GROUP_NODE, gn.cand, true, 0, 0, 0, 0, 0, 0);
                }
            }
            if (colAnz == 1) {
                gn2 = groupNodes.get(col1Index);
                tmpSet.set(Sudoku2.COL_TEMPLATES[gn.col]);
                tmpSet.and(finder.getCandidates()[gn.cand]);
                tmpSet.andNot(gn.indices);
                tmpSet.andNot(gn2.indices);
                if (tmpSet.isEmpty()) {
                    // no additional candidates -> write it
                    offEntry.addEntry(gn2.index1, gn2.index2, gn2.index3, Chain.GROUP_NODE, gn.cand, true, 0, 0, 0, 0, 0, 0);
                }
            }
            if (blockAnz == 1) {
                gn2 = groupNodes.get(block1Index);
                tmpSet.set(Sudoku2.BLOCK_TEMPLATES[gn.block]);
                tmpSet.and(finder.getCandidates()[gn.cand]);
                tmpSet.andNot(gn.indices);
                tmpSet.andNot(gn2.indices);
                if (tmpSet.isEmpty()) {
                    // no additional candidates -> write it
                    offEntry.addEntry(gn2.index1, gn2.index2, gn2.index3, Chain.GROUP_NODE, gn.cand, true, 0, 0, 0, 0, 0, 0);
                }
            }
        }
    }

    /**
     * Collect all ALS and handle them correctly.<br><br>
     *
     * ALS can only be reached over weak links (single or multiple candidates),
     * and they can be left via weak or strong links. Turning the candidate(s)
     * off changes the ALS into a locked set that can provide eliminations or
     * force a cell to a certain value (the candidate eliminations that force
     * the cell are not stored in the chain, since we can't handle links
     * with more than one candidate).<br><br>
     *
     * Since every ALS can trigger different sets of eliminations depending on
     * how it is reached, every ALS can have more than one table entry. The weak
     * link that provides the locked set is not stored in the chain (it can
     * affect multiple candidates, that don't form a group node, which we can't
     * handle). Eliminations caused by locked sets can trigger other
     * ALSes.<br><br>
     *
     * For every ALS do: 
     * 
     * <ul> 
     *  <li>check all possible entries; if an entry provides eliminations or 
     *      forces cells, make a table for that entry (only off)</li> 
     *  <li>write the index in {@link #extendedTable } into 
     *      {@link #extendedTableMap} (together with the ALS entry)</li> 
     *  <li>add the ALS entry to the onTable of the candidate/group node/als 
     *      that provides the entry</li> 
     *  <li>every candidate/group node deleted by the resulting locked set is 
     *      added to the ALS's table as is every newly triggered ALS</li> 
     * </ul>
     *
     * The ALS entry has the index of the first candidate that provides the
     * entry set as index1, the index in the ALS-array set as index2.<br><br>
     *
     * More detailed: for every ALS do 
     * 
     * <ul> 
     *  <li>for every candidate (or group of candidates) of the als find all 
     *      remaining buddies in the grid: they are all valid entries</li> 
     *  <li>if one of the entries from above is a member of a group node, that 
     *      doesn't overlap the als, the group node is an additional entry</li> 
     *  <li>if the remaining locked set provides eliminations, record
     *      them and check for possible forcings; note that the eliminations could
     *      provide an entry for another als (think of them as resctricd common);
     *      also, the eliminations could form a group node</li> 
     * </ul>
     *
     * <b>20090220:</b> BUG - alsBuddies contains only cells, that can see all
     * cells of the ALS, its then used for finding possible entries and
     * eliminations; this is incomplete: entries and eliminations only have to
     * see all cells of the ALS that contain a certain candidate!<br><br>
     *
     * <b>CAUTION:</b> Must be called AFTER {@link #fillTables() } or the
     * attributes {@link #extendedTableMap } and {@link #extendedTableIndex }
     * will not be properly initialized; the initialization cannot be moved
     * here, because it must be possible to call {@link #fillTablesWithGroupNodes()} 
     * and {@link #fillTablesWithAls() } in arbitrary order.
     */
    private void fillTablesWithAls() {
        // get all ALSes,but exclude single cells with only two remaining candidates
        alses = finder.getAlses(true);
        // handle them
        for (int alsIndex = 0; alsIndex < alses.size(); alsIndex++) {
            Als als = alses.get(alsIndex);
            if (als.indices.size() == 1) {
                // alses with size one (= nodes with two candidates) are ignored
                continue;
            }
            // for every possible entry candidate find all remaining candidates in the grid
            for (int entryCand = 1; entryCand <= 9; entryCand++) {
                // first check, if there are possible eliminations (nothing to do if not):
                // for all other candidates get all cells, that contain that 
                // candidate and can see all cells of the ALS;
                // any such candidate can be eliminated
                // 20090220: a candidiate doesnt have to see all cells of the ALS, only the cells
                //    that contain that candidate
                if (als.indicesPerCandidat[entryCand] == null || als.indicesPerCandidat[entryCand].isEmpty()) {
                    // nothing to do -> next candidate
                    continue;
                }
                // find all candidates, that can provide an entry into the als
                // 20090220: use the correct buddies
                tmpSet.setAnd(finder.getCandidates()[entryCand], als.buddiesPerCandidat[entryCand]);
                if (tmpSet.isEmpty()) {
                    // there is no possible entry candidate outside the ALS ->
                    // the ALS cant be triggered by that candidate
                    continue;
                }

                // check for eliminations
                boolean eliminationsPresent = false;
                for (int actCand = 1; actCand <= 9; actCand++) {
                    alsEliminations[actCand].clear();
                    if (actCand == entryCand) {
                        // that candidate is not in the als anymore
                        continue;
                    }
                    if (als.indicesPerCandidat[actCand] != null) {
                        // 20090220: use the correct buddies
                        alsEliminations[actCand].setAnd(finder.getCandidates()[actCand], als.buddiesPerCandidat[actCand]);
                        if (!alsEliminations[actCand].isEmpty()) {
                            // possible eliminations found
                            eliminationsPresent = true;
                        }
                    }
                }
                if (!eliminationsPresent) {
                    // nothing to do -> next candidate
                    continue;
                }
                // Eliminations are possible and possible entries exist, 
                // create a table for the als with that entry
                int entryIndex = als.indicesPerCandidat[entryCand].get(0);
                TableEntry alsEntry;
                if ((alsEntry = getAlsTableEntry(entryIndex, alsIndex, entryCand)) == null) {
                    alsEntry = getNextExtendedTableEntry(extendedTableIndex);
                    alsEntry.addEntry(entryIndex, alsIndex, Chain.ALS_NODE, entryCand, false, 0);
                    extendedTableMap.put(alsEntry.entries[0], extendedTableIndex);
                    extendedTableIndex++;
                }
                // put the ALS into the onTables of all entry candidates:
                // tmpSet already contains all possible entry candidates
                for (int i = 0; i < tmpSet.size(); i++) {
                    int actIndex = tmpSet.get(i);
                    TableEntry tmp = onTable[actIndex * 10 + entryCand];
                    // "false" because the ALS is triggered by an elimination
                    // the following "true" would be that the ALS becomes a LS
                    tmp.addEntry(entryIndex, alsIndex, Chain.ALS_NODE, entryCand, false, 0);
                }
                // every group node that doesnt overlap with the ALS and contains
                // at least two possible entry candidates, is a valid entry into
                for (int i = 0; i < groupNodes.size(); i++) {
                    GroupNode gAct = groupNodes.get(i);
                    if (gAct.cand != entryCand) {
                        // wrong candidate -> nothing to do
                        continue;
                    }
                    // tmpSet contains all remaining buddies of the ALS; two of 
                    // them have to be in the group node too
                    tmpSet1.setAnd(tmpSet, gAct.indices);
                    if (tmpSet1.isEmpty() || tmpSet1.size() < 2) {
                        // nothing to do
                        continue;
                    }
                    // now check overlapping
                    tmpSet1.set(als.indices);
                    if (!tmpSet1.andEmpty(gAct.indices)) {
                        // group node overlaps als -> ignore
                        continue;
                    }
                    // add the ALS to the group node
                    int entry = Chain.makeSEntry(gAct.index1, gAct.index2, gAct.index3, entryCand, true, Chain.GROUP_NODE);
                    TableEntry gTmp = extendedTable.get(extendedTableMap.get(entry));
                    if (gTmp != null) {
                        // if fillTables WithGroupNodes() has not yet been called, the
                        // entry doesnt exist!
                        gTmp.addEntry(entryIndex, alsIndex, Chain.ALS_NODE, entryCand, false, 0);
                    }
                }
                // now for the eliminations: candidates and group nodes
                for (int actCand = 1; actCand <= 9; actCand++) {
                    if (alsEliminations[actCand].isEmpty()) {
                        // no eliminations
                        continue;
                    }
                    // every single elimination must be recorded
                    for (int i = 0; i < alsEliminations[actCand].size(); i++) {
                        // 20090213: add ALS penalty to distance
                        alsEntry.addEntry(alsEliminations[actCand].get(i), actCand, als.getChainPenalty(), false);
                    }
                    // if a group node is a subset of the eliminations, it is turned off as well
                    for (int j = 0; j < groupNodes.size(); j++) {
                        GroupNode gAct = groupNodes.get(j);
                        if (gAct.cand != actCand) {
                            // group node is for wrong candidate
                            continue;
                        }
                        tmpSet1.set(gAct.indices);
                        if (!tmpSet1.andEquals(alsEliminations[actCand])) {
                            // not all group node cells are eliminated
                            continue;
                        }
                        // 20090213: adjust penalty for ALS
                        alsEntry.addEntry(gAct.index1, gAct.index2, gAct.index3, Chain.GROUP_NODE,
                                actCand, false, 0, 0, 0, 0, 0, als.getChainPenalty());//                        offEntry.addEntry(gAct.index1, gAct.index2, gAct.index3, Chain.GROUP_NODE, k, false, 0, 0, 0, 0, 0);
                    }
                }
                // now als: if the eliminations for one candidate cover all cells with
                // that candidate in another non-overlapping als, that als is triggered
                // we do that here for performance reasons
                for (int actAlsIndex = 0; actAlsIndex < alses.size(); actAlsIndex++) {
                    if (actAlsIndex == alsIndex) {
                        // not for ourself
                        continue;
                    }
                    Als tmpAls = alses.get(actAlsIndex);
                    tmpSet1.set(als.indices);
                    if (!tmpSet1.andEmpty(tmpAls.indices)) {
                        // overlapping -> ignore
                        continue;
                    }
                    for (int actCand = 1; actCand <= 9; actCand++) {
                        if (alsEliminations[actCand] == null || alsEliminations[actCand].isEmpty()
                                || tmpAls.indicesPerCandidat[actCand] == null
                                || tmpAls.indicesPerCandidat[actCand].isEmpty()) {
                            // nothing to do
                            continue;
                        }
                        // 20090220: tmpAls has not to be equal to alsEliminations, alsEliminations
                        //   must contain tmpAls!
                        //tmpSet1.set(tmpAls.indicesPerCandidat[l]);
                        //if (!tmpSet1.andEquals(alsEliminations[l])) {
                        tmpSet1.set(alsEliminations[actCand]);
                        if (!tmpSet1.contains(tmpAls.indicesPerCandidat[actCand])) {
                            // no entry
                            continue;
                        }
                        // create the table for the triggered als (if it does not produce
                        // valid eliminations it would be missing later on)
                        int tmpAlsIndex = tmpAls.indicesPerCandidat[actCand].get(0);
                        if (getAlsTableEntry(tmpAlsIndex, actAlsIndex, actCand) == null) {
                            TableEntry tmpAlsEntry = getNextExtendedTableEntry(extendedTableIndex);
                            tmpAlsEntry.addEntry(tmpAlsIndex, actAlsIndex, Chain.ALS_NODE, actCand, false, 0);
                            extendedTableMap.put(tmpAlsEntry.entries[0], extendedTableIndex);
                            extendedTableIndex++;
                        }
                        // 20090213: adjust for ALS penalty
                        alsEntry.addEntry(tmpAlsIndex, actAlsIndex, Chain.ALS_NODE, actCand, false, als.getChainPenalty());
                    }
                }
                // last but not least: forcings
                // if one of the als's buddies has only one candidate left
                // after the eliminations, it is forced
                // 20090220: use the correct buddies
                // only necessary, if the cell contains more than 2 candidates (its
                // handled correctly with only two candidates)
                for (int i = 0; i < als.buddies.size(); i++) {
                    int cellIndex = als.buddies.get(i);
                    if (sudoku.getValue(cellIndex) != 0 || sudoku.getAnzCandidates(cellIndex) == 2) {
                        // cell already set or handled elsewhere
                        continue;
                    }
                    sudoku.getCandidateSet(cellIndex, tmpSet1);
                    for (int actCand = 1; actCand <= 9; actCand++) {
                        if (alsEliminations[actCand] != null && alsEliminations[actCand].contains(cellIndex)) {
                            // delete candidate
                            tmpSet1.remove(actCand);
                        }
                    }
                    if (tmpSet1.size() == 1) {
                        // forcing!
                        // 20090213: adjust for ALS penalty (plus the extra omitted link)
                        alsEntry.addEntry(cellIndex, tmpSet1.get(0), als.getChainPenalty() + 1, true);
                    }
                }
            }
        }
    }

    /**
     * Tries to find an extended table entry for a given als with the given
     * entry candidate; if none can be found, null is returned.
     *
     * @param entryCellIndex
     * @param alsIndex
     * @param cand
     * @return
     */
    private TableEntry getAlsTableEntry(int entryCellIndex, int alsIndex, int cand) {
        int entry = Chain.makeSEntry(entryCellIndex, alsIndex, cand, false, Chain.ALS_NODE);
        if (extendedTableMap.containsKey(entry)) {
            return extendedTable.get(extendedTableMap.get(entry));
        }
        return null;
    }

    /**
     * Returns the next free {@link TableEntry } from {@link #extendedTable }
     * (reuse of entries in multiple search runs). If no entry is left, a new
     * one is created and added to extendedTable.
     *
     * @param tableIndex
     * @return
     */
    private TableEntry getNextExtendedTableEntry(int tableIndex) {
        TableEntry entry;
        if (tableIndex >= extendedTable.size()) {
            entry = new TableEntry();
            extendedTable.add(entry);
        } else {
            entry = extendedTable.get(tableIndex);
            entry.reset();
        }
        return entry;
    }

    /**
     * Collects all dependencies on one specific action (cell is set/candidate
     * is deleted). To detect nets, the whole operation is repeated
     * {@link Options#anzTableLookAhead} times.<br><br>
     *
     * All operations have to be done on a copy of the original sudoku. The
     * candidates in the {@link #finder} are not updated (they are not used and
     * after the operation the sudoku has not changed).<br><br>
     *
     * If <code>set</code> is <code>true</code>, the cell is set and all newly 
     * created Hidden and Naked Singles are collected and executed. If it is
     * <code>false</code>, it is eliminated. If that creates single(s), they are
     * executed and handled as well.<br><br>
     *
     * If a cell is set, this is delegated to 
     * {@link #setCell(int, int, solver.TableEntry, boolean, boolean, int)}.
     *
     * @param entry The {@link TableEntry}
     * @param cellIndex the index of the current cell
     * @param cand The current candidate
     * @param set <code>true</code> if the candidate is to be set, else
     * <code>false</code>
     */
    private void getTableEntry(TableEntry entry, int cellIndex, int cand, boolean set) {
        if (DEBUG) {
            //System.out.println("getTableEntry() 1: " + cellIndex + "/" + cand + "/" + set);
        }
        if (set) {
            // set the cell and record all dependencies
            setCell(cellIndex, cand, entry, false, false);
        } else {
            // eliminate the candidate and set the cell if necessary
            sudoku.delCandidate(cellIndex, cand);
            entry.addEntry(cellIndex, cand, false, 0);
            if (sudoku.getAnzCandidates(cellIndex) == 1) {
                int setCand = sudoku.getAllCandidates(cellIndex)[0];
                // getRetIndices == false causes retIndex == 0
                if (DEBUG) {
                    //System.out.println("getTableEntry() 2: " + cellIndex + "/" + setCand);
                }
                setCell(cellIndex, setCand, entry, false, true);
            }
        }
        // now look ahead
        for (int j = 0; j < Options.getInstance().getAnzTableLookAhead(); j++) {
            singleSteps.clear();
            List<SolutionStep> dummyList = simpleFinder.findAllNakedSingles(sudoku);
            singleSteps.addAll(dummyList);
            dummyList = simpleFinder.findAllHiddenSingles(sudoku);
            singleSteps.addAll(dummyList);
            for (int i = 0; i < singleSteps.size(); i++) {
                SolutionStep step = singleSteps.get(i);
                int index = step.getIndices().get(0);
                if (DEBUG) {
                    //System.out.println("getTableEntry() 3: " + index + "/" + step.getValues().get(0) + "/" + step.getType().getStepName());
                }
                setCell(index, step.getValues().get(0), entry, true,
                        step.getType() == SolutionType.NAKED_SINGLE);
            }
        }
    }

    /**
     * Setting a value in a cell is surprisingly complicated: Not only must all
     * consequences be found but the sources of all actions have to be recorded
     * as well (from the ORIGINAL sudoku!).
     *
     * @param cellIndex Index of the cell, where the value should be set
     * @param cand Candidate to set in cell
     * @param entry {@link TableEntry} for the operation
     * @param getRetIndices <code>true</code>, if <code>retIndices</code> should be recorded
     * @param nakedSingle <code>true</code> if Named Single,
     *          <code>false</code> if Hidden Single
     */
    private void setCell(int cellIndex, int cand, TableEntry entry,
            boolean getRetIndices, boolean nakedSingle) {
        if (sudoku.getValue(cellIndex) != 0) {
            // nothing to do
            if (DEBUG) {
                //System.out.println("TablungSolver.setCell(): Cell already set (" + cellIndex + "/" + cand + "/" + SolutionStep.getCellPrint(cellIndex) + ")");
            }
            return;
        }
        // find all candidates that are eliminated by the set operation (dont forget
        // the candidates in the cell itself). The reason for the elimination is the
        // ON entry.
        // finder.getCandidates() gets the original candidates (even in a net search)
        tmpSet.set(finder.getCandidates()[cand]);
        tmpSet.remove(cellIndex);
        tmpSet.and(Sudoku2.buddies[cellIndex]);
        int[] cands = sudoku.getAllCandidates(cellIndex);
        // The candidates from the original Sudoku (for Naked Singles)
        int[] cellCands = savedSudoku.getAllCandidates(cellIndex);
        // get the house with the smallest number of original candidates (needed for ret indices,
        // but must be done before the cell is set)
        // CAUTION: This cant be right! We are called from FillTables; we can only use a
        //          house, where all other candidates have been removed by the original
        //          TableEntry;
        //          So: What are we really looking for? A house with free == 1 (or
        //          it wouldnt be a single), that has had the least number of candidates removed
//        int entityType = Sudoku2.LINE;
        int entityNumberFree = 100; // impossibly high
        tmpSet2 = SudokuSetBase.EMPTY_SET;
        if (getRetIndices && !nakedSingle) {
//            if (DEBUG) {
//                if (cellIndex == 55 && cand == 4) {
//                    printTable("", entry);
//                }
//            }
            for (int i = 0; i < Sudoku2.CONSTRAINTS[cellIndex].length; i++) {
                int constrNumber = Sudoku2.CONSTRAINTS[cellIndex][i];
                if (DEBUG) {
//                    if (cellIndex == 63 && cand == 5) {
//                        System.out.println("63/5: " + constrNumber + "/" + sudoku.getFree()[constrNumber][cand]);
//                        tmpSet1.setAnd(entry.offSets[cand],
//                                Sudoku2.ALL_CONSTRAINTS_TEMPLATES[constrNumber]);
//                        System.out.println("    : " + tmpSet1);
//                        System.out.print("    : " + entry.offSets[cand]);
//                        for (int h = 0; h < entry.offSets[cand].size(); h++) {
//                            System.out.print("  " + SolutionStep.getCellPrint(entry.offSets[cand].get(h)));
//                        }
//                        System.out.println("");
//                    }
                }
                if (sudoku.getFree()[constrNumber][cand] <= 1) {
                    // get all other candidates in the house
                    tmpSet1.setAnd(finder.getCandidates()[cand], Sudoku2.ALL_CONSTRAINTS_TEMPLATES[constrNumber]);
                    tmpSet1.remove(cellIndex);
                    // check, if they are all removed
//                    if (DEBUG) {
//                        if (cellIndex == 63 && cand == 5) {
//                            System.out.print("x   : " + tmpSet1);
//                            for (int h = 0; h < tmpSet1.size(); h++) {
//                                System.out.print("  " + SolutionStep.getCellPrint(tmpSet1.get(h)));
//                            }
//                            System.out.println("");
//                            System.out.print("y   : " + entry.offSets[cand]);
//                            for (int h = 0; h < entry.offSets[cand].size(); h++) {
//                                System.out.print("  " + SolutionStep.getCellPrint(entry.offSets[cand].get(h)));
//                            }
//                            System.out.println("");
//                        }
//                    }
                    if (tmpSet1.andEquals(entry.offSets[cand])) {
                        int dummy = tmpSet1.size();
                        if (dummy < entityNumberFree) {
                            entityNumberFree = dummy;
                            // store for later use (candidate eliminations, that caused the Hidden Single)
                            tmpSet2.set(tmpSet1);
                        }
                    }
                }
            }
            if (entityNumberFree == 100 && cellCands.length > 1) {
                // its not a Hidden Single, but a Naked Single
                if (DEBUG) {
                    System.out.println("TablingSolver.setCell(): Switching to Naked Single (" + cellIndex + "/" + cand + ")");
                }
                nakedSingle = true;
            }
            if (DEBUG) {
                if (getRetIndices && !nakedSingle && entityNumberFree == 100) {
                    // only relevant for Hidden Singles
                    System.out.println("TablingSolver.setCell(): No house found! " + cellIndex + "/" + cand + "/" + nakedSingle + "/" + getRetIndices);
                }
            }
        }
        // now set the cell
        if (DEBUG) {
            //System.out.println("   setCell(): " + cellIndex + "/" + cand + " (" + SolutionStep.getCellPrint(cellIndex) + ")");
        }
        sudoku.setCell(cellIndex, cand);
        int retIndex = entry.index;
        if (getRetIndices) {
            // find the candidate(s) that are responsible for the ON operation
            for (int i = 0; i < retIndices[0].length; i++) {
                retIndices[0][i] = 0;
            }
            if (nakedSingle) {
                // all other candidates in the cell
                // +1 because the set candidate is still present here
                if (cellCands.length > retIndices[0].length + 1) {
                    if (DEBUG) {
                        System.out.println("Too many candidates (setCell() - Naked Single");
                    }
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Too many candidates (setCell() - Naked Single");
                }
                int ri = 0;
                for (int i = 0; i < cellCands.length && ri < retIndices[0].length; i++) {
                    if (cellCands[i] == cand) {
                        continue;
                    }
                    retIndices[0][ri++] = entry.getEntryIndex(cellIndex, false, cellCands[i]);
                    if (DEBUG) {
                        //System.out.println("     NS - retIndices[0][" + (ri - 1) + "] = " + retIndices[0][ri - 1] + " (" + cellIndex + "/" + cellCands[i] + ")");
                    }
                }
            } else {
                // all other candidates in the house with the smallest number of original candidates
                // already stored in tmpSet2
                if (tmpSet2.size() > retIndices[0].length) {
                    if (DEBUG) {
                        System.out.println("Too many candidates (setCell() - Hidden Single");
                    }
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Too many candidates (setCell() - Hidden Single");
                }
                int ri = 0;
                for (int i = 0; i < tmpSet2.size() && ri < retIndices[0].length; i++) {
                    retIndices[0][ri++] = entry.getEntryIndex(tmpSet2.get(i), false, cand);
                    if (DEBUG) {
                        //System.out.println("     HS - retIndices[0][" + (ri - 1) + "] = " + retIndices[0][ri - 1] + " (" + tmpSet2.get(i) + "/" + cand + ")");
                    }
                }
//                if (entityType == Sudoku2.LINE) {
//                    getRetIndicesForHouse(cellIndex, cand, Sudoku2.LINE_TEMPLATES[Sudoku2.getLine(cellIndex)], entry);
//                } else if (entityType == Sudoku2.COL) {
//                    getRetIndicesForHouse(cellIndex, cand, Sudoku2.COL_TEMPLATES[Sudoku2.getCol(cellIndex)], entry);
//                } else {
//                    getRetIndicesForHouse(cellIndex, cand, Sudoku2.BLOCK_TEMPLATES[Sudoku2.getBlock(cellIndex)], entry);
//                }
            }
            // ON entry for set operation including retIndices
            entry.addEntry(cellIndex, cand, true, retIndices[0][0], retIndices[0][1], retIndices[0][2],
                    retIndices[0][3], retIndices[0][4]);
        } else {
            // ON entry for set operation without retIndices
            entry.addEntry(cellIndex, cand, true);
        }
        // OFF entries for all candidates that can see cellIndex
        for (int i = 0; i < tmpSet.size(); i++) {
            entry.addEntry(tmpSet.get(i), cand, false, retIndex);
        }
        // OFF entries for all other candidates in the cell
        for (int i = 0; i < cands.length; i++) {
            if (cands[i] != cand) {
                entry.addEntry(cellIndex, cands[i], false, retIndex);
            }
        }
    }

    /**
     * Collect the entries for all candidates in a given house. All those
     * canddiates have to be eliminated before the cell can be set. Used by 
     * {@link #setCell(int, int, solver.TableEntry, boolean, boolean)}.
     *
     * @param cellIndex
     * @param cand
     * @param houseSet
     * @param entry
     */
//    private void getRetIndicesForHouse(int cellIndex, int cand, SudokuSet houseSet, TableEntry entry) {
//        // get all original candidates in the house (cell itself excluded)
//        tmpSet1.set(finder.getCandidates()[cand]);
//        tmpSet1.remove(cellIndex);
//        tmpSet1.and(houseSet);
//        if (tmpSet1.size() > retIndices[0].length + 1) {
//            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Too many candidates (setCell() - Hidden Single");
//        }
//        int ri = 0;
//        for (int i = 0; i < tmpSet1.size() && ri < retIndices[0].length; i++) {
//            retIndices[0][ri++] = entry.getEntryIndex(tmpSet1.get(i), false, cand);
//        }
//    }
    /**
     * Expand all the tables. The real work is delegated to
     * {@link #expandTable(solver.TableEntry, int, int, boolean) }.
     */
    private void expandTables() {
        // for every entry in all tables do...
        for (int i = 0; i < onTable.length; i++) {
            if (onTable[i].index == 0) {
                // cell is set -> no implications
                continue;
            }
            // expand it
            expandTable(onTable[i], i / 10, i % 10, true, 1, -1);
        }
        for (int i = 0; i < offTable.length; i++) {
            if (offTable[i].index == 0) {
                // cell is set -> no implications
                continue;
            }
            // expand it
            expandTable(offTable[i], i / 10, i % 10, false, 1, -1);
        }
    }

    /**
     * Expands the tables: every {@link TableEntry } contains all direct
     * implications for a given premise. Now every implication is expanded with
     * all implication from its own
     * <code>TableEntry</code>.<br><br>
     *
     * For every entry in
     * <code>table[i].entries</code> all new implications are added. that is
     * done till no implications are left or till
     * <code>table[i].entries</code> is full.<br><br>
     *
     * If an entry is added, a reference is set to the originating table. If an
     * entry already exists, the path length is checked: if the new entry gives
     * a shorter chain, the old entry is overridden.<br><br>
     *
     * Group node table entries are never expanded (since we dont start or end
     * with a group node, that wouldnt make any sense). They are however used as
     * possible implications.
     *
     * @param dest The table that should be expanded
     * @param index The cell index of the premise
     * @param cand The candidate of the premise
     * @param isOn <code>true</code>, if the candidate is set in the premise
     * @param startIndex The index of the first entry to expand (1 to expand the
     * whole table)
     * @param singleEntry If not -1 the index of one single entry that should be
     * expanded (is used to adjust distances when a single node has been
     * replaced by a shorter net)
     */
    private void expandTable(TableEntry dest, int index, int cand, boolean isOn,
            int startIndex, int singleEntry) {
        boolean isFromOnTable = false;
        boolean isFromExtendedTable = false;
        // check every entry except the first (thats the premise)
        int end = dest.entries.length;
        if (singleEntry != -1) {
            startIndex = singleEntry;
            end = singleEntry + 1;
        }
        for (int destIndex = startIndex; destIndex < end; destIndex++) {
            if (dest.entries[destIndex] == 0) {
                // ok -> done
                break;
            }
            if (dest.isFull()) {
                if (DEBUG) {
                    System.out.println("WARNING: TableEntry full (" + SolutionStep.getCellPrint(index) + (isOn ? "=" : "<>") + cand);
                }
                // nothing left to do...
                Logger.getLogger(getClass().getName()).log(Level.WARNING, "TableEntry full!");
                break;
            }
            // table for the current entry -> all entries in src have to be written into dest
            TableEntry src = null;

            // find the table, where the current implication is the premise
            int srcTableIndex = dest.getCellIndex(destIndex) * 10 + dest.getCandidate(destIndex);
            isFromExtendedTable = false;
            isFromOnTable = false;
            if (Chain.getSNodeType(dest.entries[destIndex]) != Chain.NORMAL_NODE) {
                Integer tmpSI = extendedTableMap.get(dest.entries[destIndex]);
                if (tmpSI == null) {
                    if (DEBUG) {
                        System.out.println("WARNING: Table for " + printTableEntry(dest.entries[destIndex], alses) + " not found!");
                    }
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Table for {0} not found!", printTableEntry(dest.entries[destIndex], alses));
                    continue;
                }
                srcTableIndex = tmpSI.intValue();
                src = extendedTable.get(srcTableIndex);
                isFromExtendedTable = true;
            } else {
                isFromOnTable = dest.isStrong(destIndex);
                if (isFromOnTable) {
                    src = onTable[srcTableIndex];
                } else {
                    src = offTable[srcTableIndex];
                }
            }
            if (src.index == 0) {
                // should not be possible
                StringBuilder tmpBuffer = new StringBuilder();
                tmpBuffer.append("TableEntry for ").append(dest.entries[destIndex]).append(" not found!\r\n");
                tmpBuffer.append("index == ").append(index).append(", j == ").append(destIndex).append(", dest.entries[j] == ").append(dest.entries[destIndex]).append(": ");
                tmpBuffer.append(printTableEntry(dest.entries[destIndex], alses));
                if (DEBUG) {
                    System.out.println("WARNING: " + tmpBuffer);
                }
                Logger.getLogger(getClass().getName()).log(Level.WARNING, tmpBuffer.toString());
                continue;
            }
            // ok -> expand it
            int srcBaseDistance = dest.getDistance(destIndex);
            // check all entries from src
            for (int srcIndex = 1; srcIndex < src.index; srcIndex++) {
                // we take only entries, that have not been expanded themselves
                if (src.isExpanded(srcIndex)) {
                    // ignore it!
                    continue;
                }
                int srcDistance = src.getDistance(srcIndex);
                if (dest.indices.containsKey(src.entries[srcIndex])) {
                    // entry from src already exists in dest -> check path length
                    int orgIndex = dest.getEntryIndex(src.entries[srcIndex]);
                    // 20090213: prefer normal nodes to group nodes or als
//                        if (dest.isExpanded(orgIndex) && dest.getDistance(orgIndex) > (srcBaseDistance + srcDistance)) {
                    if (dest.isExpanded(orgIndex)
                            && (dest.getDistance(orgIndex) > (srcBaseDistance + srcDistance)
                            || dest.getDistance(orgIndex) == (srcBaseDistance + srcDistance)
                            && dest.getNodeType(orgIndex) > src.getNodeType(srcIndex))) {
                        // old entry had a longer path or was more complicated -> rewrite
                        dest.retIndices[orgIndex] = TableEntry.makeSRetIndex(srcTableIndex, 0, 0, 0, 0);
                        // expanded flag was lost -> set it again
                        dest.setExpanded(orgIndex);
                        if (isFromExtendedTable) {
                            dest.setExtendedTable(orgIndex);
                        } else if (isFromOnTable) {
                            dest.setOnTable(orgIndex);
                        }
                        dest.setDistance(orgIndex, srcBaseDistance + srcDistance);
                    }
                } else {
                    // new entry
                    int srcCellIndex = src.getCellIndex(srcIndex);
                    int srcCand = src.getCandidate(srcIndex);
                    boolean srcStrong = src.isStrong(srcIndex);
                    if (Chain.getSNodeType(src.entries[srcIndex]) == Chain.NORMAL_NODE) {
                        dest.addEntry(srcCellIndex, srcCand, srcStrong, srcTableIndex);
                    } else {
                        int tmp = src.entries[srcIndex];
                        dest.addEntry(Chain.getSCellIndex(tmp), Chain.getSCellIndex2(tmp),
                                Chain.getSCellIndex3(tmp), Chain.getSNodeType(tmp),
                                srcCand, srcStrong, srcTableIndex, 0, 0, 0, 0, 0);
                    }
                    dest.setExpanded(dest.index - 1);
                    if (isFromExtendedTable) {
                        dest.setExtendedTable(dest.index - 1);
                    } else if (isFromOnTable) {
                        dest.setOnTable(dest.index - 1);
                    }
                    dest.setDistance(dest.index - 1, srcBaseDistance + srcDistance);
                }
            }
        }
    }

    /**
     * Expands chains to nets as long as possible.
     * The real work is done by {@link #createNets()}.
     */
    private void createAllNets() {
        int entryAnz = getTableAnz();
        int count = 0;
        int newEntryAnz = entryAnz;
        long nanos = System.nanoTime();
        do {
            count++;
            // TODO
//            System.out.println("createNets() start: " + count + "/" + entryAnz);
            entryAnz = newEntryAnz;
            createNets();
            newEntryAnz = getTableAnz();
//            System.out.println("createNets() end: " + count + "/" + newEntryAnz);
        } while (newEntryAnz > entryAnz);
        if (DEBUG) {
            nanos = System.nanoTime() - nanos;
            System.out.println("createAllNets(): " + (nanos / 1000000l) + "ms (" + count + " iterations)");
        }
    }

    /**
     * Tries to find nets in existing tables. Delegates to 
     * {@link #createNet(solver.TableEntry)}.
     */
    private void createNets() {
        if (DEBUG) {
//            printTable("r3c4=3", onTable[213], alses);
//            printTable("r7c1=8", onTable[548], alses);
        }
        for (int i = 0; i < onTable.length; i++) {
            if (onTable[i].index == 0) {
                continue;
            }
            createNet(onTable[i], i / 10, i % 10, true);
        }
        for (int i = 0; i < offTable.length; i++) {
            if (offTable[i].index == 0) {
                continue;
            }
            createNet(offTable[i], i / 10, i % 10, false);
        }
    }

    /**
     * Goes through all outcomes in
     * <code>src</code> and tries to determine, if a combination of outcomes can
     * produce a new one.<br><br>
     *
     * Currently the following patterns are checked: 
     * 
     * <ul> 
     *  <li>Entries deleting all but one candiates in a house</li> 
     *  <li>Entries deleting all but one candidate in a cell</li> 
     *  <li>Entries deleting all candidates of a group node</li>
     *  <li>Entries setting all candidates of a group node</li>
     *  <li>Entries deleting all instances of a candidate in an ALS</li>
     * </ul> 
     * 
     * The search is done via the {@link TableEntry#offSets} of
     * <code>src</code>.<br><br>
     *
     * If a new outcome is found, it is added to
     * <code>src</code> and all predecessors are recorded in the entries
     * {@link TableEntry#retIndices return index}.
     *
     * @param src The {@link TableEntry} which is currently handled
     * @param srcIndex The cell index of the premise of <code>src</code>
     * @param srcCand The candidate of the premise of <code>src</code>
     * @param isOn <code>srcCand</code> is on/off in the premise
     */
    private void createNet(TableEntry src, int srcIndex, int srcCand, boolean isOn) {
        if (DEBUG) {
            System.out.println("createNet(): " + Chain.toString(src.entries[0]));
        }
        // store the current amount of entries in the table (for expansion)
        int currentTableIndex = src.index;
        // houses first: check all 9 candidates in all 27 houses (time consuming...)
        for (int cand = 1; cand < src.offSets.length; cand++) {
            for (int constr = 0; constr < Sudoku2.ALL_CONSTRAINTS_TEMPLATES.length; constr++) {
                // check all candidates in that house
                if (finder.getSudoku().getFree()[constr][cand] < 3) {
                    // there have to be at least three candidates in that house; if there
                    // are less, its either a normal implication (has already been handled)
                    // or there is nothing to do
                    continue;
                }
                // get all implications (candidate off) for that candidate and house
                tmpSet.setAnd(src.offSets[cand], Sudoku2.ALL_CONSTRAINTS_TEMPLATES[constr]);
                if (tmpSet.isEmpty()) {
                    // nothing to do
                    continue;
                }
                // get all candidates in the house, for which no implications exist
                tmpSet1.setAnd(finder.getCandidates()[cand], Sudoku2.ALL_CONSTRAINTS_TEMPLATES[constr]);
                tmpSet1.andNot(tmpSet);
                if (tmpSet1.size() == 1) {
                    // there is exactly one candidate left -> is set by a net
                    int index = tmpSet1.get(0);
                    makeNetEntry(src, srcIndex, srcCand, isOn, index, -1, -1,
                            cand, tmpSet, (short) 0, true, Chain.NORMAL_NODE,
                            null, null);
                } else if (tmpSet1.isEmpty()) {
                    // no candidate left: every candidate in the house can be set by a net consisting
                    // of all the other candidates
                    for (int i = 0; i < tmpSet.size(); i++) {
                        int index = tmpSet.get(i);
                        makeNetEntry(src, srcIndex, srcCand, isOn, index, -1, -1,
                                cand, tmpSet, (short) 0, true, Chain.NORMAL_NODE,
                                null, null);
                    }
                }
            }
        }
        // now all cells: if all but one candidates are removed from a cell, the
        // last candidate can be set in the cell; if all candidates are removed,
        // every candidate can be set; only applicable, if more than two
        // candidates are still valid in the cell
        Sudoku2 actSudoku = finder.getSudoku();
        for (int index = 0; index < Sudoku2.LENGTH; index++) {
            if (actSudoku.getValue(index) != 0) {
                // cell already set -> ignore
                continue;
            }
            if (actSudoku.getAnzCandidates(index) < 3) {
                // no net possible
                continue;
            }
            int cand = -1;
            int anz = 0;
            int[] cands = actSudoku.getAllCandidates(index);
            for (int i = 0; i < cands.length; i++) {
                if (!src.offSets[cands[i]].contains(index)) {
                    anz++;
                    if (anz > 1) {
                        // thats the second missing candidate -> no net possible
                        break;
                    }
                    // "left over" -> could be set in cell later
                    cand = cands[i];
                }
            }
            short mask = actSudoku.getCell(index);
            if (anz == 1) {
                // cand can be set due to the net
                makeNetEntry(src, srcIndex, srcCand, isOn, index, -1, -1,
                        cand, null, mask, true, Chain.NORMAL_NODE,
                        null, null);
            } else if (anz == 0) {
                // every candidate is possible
                for (int i = 0; i < cands.length; i++) {
                    cand = cands[i];
                    makeNetEntry(src, srcIndex, srcCand, isOn, index, -1, -1,
                            cand, null, mask, true, Chain.NORMAL_NODE,
                            null, null);
                }
            } else {
                // no net possible
                continue;
            }
        }
        // group nodes: if all candidates outside of a of a group node are 
        // deleted/set, the group node is triggered
        if (withGroupNodes) {
            if (DEBUG) {
                System.out.println("  start on group nodes");
            }
            groupNodes = finder.getGroupNodes();
            for (int i = 0; i < groupNodes.size(); i++) {
                GroupNode gn = groupNodes.get(i);
                // in the block or line/col containing the group node more
                // than one candidate outside the group node itself must be set/deleted

                // block
                checkGroupNodeNetEntry(src, srcIndex, srcCand,
                        isOn, gn, true, Sudoku2.BLOCK_TEMPLATES[gn.block]);
                checkGroupNodeNetEntry(src, srcIndex, srcCand,
                        isOn, gn, false, Sudoku2.BLOCK_TEMPLATES[gn.block]);
                // now line or col
                if (gn.line != -1) {
                    // group node is in a line
                    checkGroupNodeNetEntry(src, srcIndex, srcCand,
                            isOn, gn, true, Sudoku2.LINE_TEMPLATES[gn.line]);
                    checkGroupNodeNetEntry(src, srcIndex, srcCand,
                            isOn, gn, false, Sudoku2.LINE_TEMPLATES[gn.line]);
                } else {
                    checkGroupNodeNetEntry(src, srcIndex, srcCand,
                            isOn, gn, true, Sudoku2.COL_TEMPLATES[gn.col]);
                    checkGroupNodeNetEntry(src, srcIndex, srcCand,
                            isOn, gn, false, Sudoku2.COL_TEMPLATES[gn.col]);
                }
            }
        }
        // ALS: If all instances of a candidate are eliminated from the ALS,
        // the ALS becomes a locked set
//        if (withAlsNodes) {
//            alses = finder.getAlses(true);
//            for (int i = 0; i < alses.size(); i++) {
//                Als als = alses.get(i);
//                for (int cand = 1; cand <= 9; cand++) {
//                    if ((als.candidates & Sudoku2.MASKS[cand]) == 0) {
//                        // candidate not in ALS -> nothing to do
//                        continue;
//                    }
//                    if (src.offSets[cand] == null || als.indicesPerCandidat[cand] == null) {
//                        // cant do anything
//                        continue;
//                    }
//                    if (src.offSets[cand].contains(als.indicesPerCandidat[cand])) {
//                        // all candidates are eliminated, the als becomes a locked set!
//                        makeNetEntry(src, srcIndex, srcCand, isOn, als.indices.get(0),
//                                i, -1, cand, null, (short) 0, false,
//                                Chain.ALS_NODE, null, als);
//                    }
//                }
//            }
//        }
        // finally expand the new entries
        expandTable(src, srcIndex, srcCand, isOn, currentTableIndex, -1);
        if (DEBUG) {
            if (srcIndex == 54 && srcCand == 8 && isOn == true) {
//                System.out.println("expanding table:");
//                printTable("r7c1=8", onTable[548], alses);
            }
        }
    }

    /**
     * Make a single new entry in <code>src</code> for setting <code>cand</code> 
     * in <code>index</code>.<br><br>
     * 
     * <code>entries</code> contains all other cells, which lead to the new net
     * entry. If <code>entries</code> is <code>null</code>, a cell is set due to 
     * all but one candidate deleted from it and <code>cands</code> contains the 
     * candidates that lead to the new entry.
     *
     * @param src Table for which the new entry should be created
     * @param srcIndex The cell index of the premise of <code>src</code>
     * @param srcCand The candidate of the premise of <code>src</code>
     * @param isOn <code>srcCand</code> is on/off in the premise
     * @param index The index of the cell
     * @param index2 The index2 of a group node or the alsIndex; -1 if not applicable
     * @param index3 The index3 of agroup node; -1 if not applicable
     * @param cand The candidate that can be set due to the net
     * @param entries All other cells, that lead to the new entry
     * @param cands All other candidates in the cell
     * @param newOnOff New entry is an on or an off entry
     * @param nodeType The type of the new node
     * @param gn The group node if available
     * @param als The ALS if available
     */
    private void makeNetEntry(TableEntry src, int srcIndex, int srcCand, boolean isOn,
            int index, int index2, int index3, int cand, SudokuSet entries,
            short cands, boolean newOnOff, int nodeType, GroupNode gn, Als als) {
        if (DEBUG) {
            if (nodeType == Chain.NORMAL_NODE && index == 11 && index2 == -1 && index3 == -1 && cand == 7 && newOnOff == false) {
                System.out.println("      Creating entry for " + gn + " " + newOnOff + " (Entry: " + Chain.toString(src.entries[0]) + ")");
            }
        }
        // check, if it already exists
        boolean alreadyThere = false;
        int oldDistance = 0;
        int atIndex = -1;
        int entry = 0;
        if (nodeType == Chain.ALS_NODE) {
            entry = Chain.makeSEntry(index, index2, cand, newOnOff, nodeType);
        } else {
            entry = Chain.makeSEntry(index, index2, index3, cand, newOnOff, nodeType);
            if (DEBUG) {
                if (nodeType == Chain.NORMAL_NODE && index == 11 && index2 == -1 && index3 == -1 && cand == 7 && newOnOff == false) {
                    System.out.println("       entry: " + Chain.toString(entry));
                }
            }
        }
        if (src.indices.containsKey(entry)) {
            alreadyThere = true;
            atIndex = src.getEntryIndex(entry);
            oldDistance = src.getDistance(atIndex);
            if (DEBUG) {
                if (nodeType == Chain.NORMAL_NODE && index == 11 && index2 == -1 && index3 == -1 && cand == 7 && newOnOff == false) {
                    System.out.println("       already there: distance = " + oldDistance + " (" + atIndex + ")");
                }
            }
        } else {
            if (DEBUG) {
                if (nodeType == Chain.NORMAL_NODE && index == 11 && index2 == -1 && index3 == -1 && cand == 7 && newOnOff == false) {
                    System.out.println("       new entry");
                }
            }
        }
        //make a new net entry
        int retIndex = 0;
        int distance = 0;
        // entries is set for normal nodes and group nodes
//        if (entries != null && nodeType == Chain.NORMAL_NODE) {
        if (entries != null) {
            // normal node  or group node, entries contains all the cells, that
            // can be set by a net
            for (int i = 0; i < entries.size(); i++) {
                int actIndex = entries.get(i);
                if (actIndex == index) {
                    // is destination-> ignore
                    continue;
                }
                entry = Chain.makeSEntry(actIndex, cand, !newOnOff);
                if (!src.indices.containsKey(entry)) {
                    if (DEBUG) {
                        System.out.println("makeNetEntry: Entry for net not in table (1 - " + actIndex + "/" + cand);
                    }
                    continue;
                }
                int ri = src.getEntryIndex(entry);
                if (retIndex < retIndices[0].length) {
                    retIndices[0][retIndex++] = ri;
                } else if (DEBUG) {
                    System.out.println("makeNetEntry: Too many retindices");
                }
                distance += (src.getDistance(ri) + 1);
            }
            if (DEBUG) {
                if (nodeType == Chain.NORMAL_NODE && index == 11 && index2 == -1 && index3 == -1 && cand == 7 && newOnOff == false) {
                    System.out.println("       new distance = " + distance);
                }
            }
        } else if (nodeType == Chain.ALS_NODE) {
            // entry for als: collect the ret indices
            // the als is triggered by onSets of the source,
            // that can see the candidates in the als.
            tmpSet.clear();
            for (int i = 0; i < als.indicesPerCandidat[cand].size(); i++) {
                // candidates have to be eliminated to trigger the als,
                // therefore onSets has to be used
                int actIndex = als.indicesPerCandidat[cand].get(i);
                tmpSet1.setAnd(src.onSets[cand], Sudoku2.buddies[actIndex]);
                if (tmpSet1.isEmpty()) {
                    if (DEBUG) {
                        System.out.println("makeNetEntry: No buddies found for als node (" + als + "/" + cand + ")");
                    }
                    return;
                }
                int checkDistance = 1000;
                for (int j = 0; j < tmpSet1.size(); j++) {
                    entry = Chain.makeSEntry(tmpSet1.get(j), cand, true);
                    if (!src.indices.containsKey(entry)) {
                        if (DEBUG) {
                            System.out.println("makeNetEntry: Entry for net not in table (als node - " + als + "/" + cand);
                        }
                        // cant do anything!
                        return;
                    }
                    int ri = src.getEntryIndex(entry);
                    int actDistance = src.getDistance(ri);
                    if (actDistance < checkDistance) {
                        if (checkDistance == 1000) {
                            // first time around -> new ret index
                            retIndices[0][retIndex++] = ri;
                        } else {
                            // replace it
                            retIndices[0][retIndex - 1] = ri;
                        }
                        checkDistance = actDistance;
                    }
                }
                distance += (checkDistance + 1);
            }
        } else {
            // single cell
            int[] candsArr = Sudoku2.POSSIBLE_VALUES[cands];
            for (int i = 0; i < candsArr.length; i++) {
                int cand1 = candsArr[i];
                if (cand1 == cand) {
                    // is destination-> ignore
                    continue;
                }
                entry = Chain.makeSEntry(index, cand1, false);
                if (!src.indices.containsKey(entry)) {
                    if (DEBUG) {
                        System.out.println("ERROR: Entry for net not in table (2 - " + index + "/" + cand1);
                    }
                    continue;
                }
                int ri = src.getEntryIndex(entry);
                if (retIndex < retIndices[0].length) {
                    retIndices[0][retIndex++] = ri;
                } else if (DEBUG) {
                    System.out.println("makeNetEntry: Too many ret indices");
                }
                distance += (src.getDistance(ri) + 1);
            }
        }

        // build the new entry; if there are more than 5 retIndices, they are ignored
        for (int i = retIndex; i < 5; i++) {
            // reset unused indices
            retIndices[0][i] = 0;
        }
        // write the entry
//        System.out.println("   new distance: " + distance);
        if (alreadyThere) {
            if (distance < oldDistance) {
//                System.out.println("   overwrite old entry");
                // shorter distance when reached as net -> recode it
                // in expanded nodes the first ret index points to the
                // other Table -> must be retained
                long newRetIndices;
                if (src.isExpanded(atIndex)) {
                    int oldRetIndex = src.getRetIndex(atIndex, 0);
                    newRetIndices = TableEntry.makeSRetIndex(oldRetIndex,
                            retIndices[0][0], retIndices[0][1], retIndices[0][2],
                            retIndices[0][3]);
                } else {
                    newRetIndices = TableEntry.makeSRetIndex(retIndices[0][0],
                            retIndices[0][1], retIndices[0][2], retIndices[0][3],
                            retIndices[0][4]);
                }
                src.retIndices[atIndex] = newRetIndices;
                src.setDistance(atIndex, distance);
                if (DEBUG) {
                    if (nodeType == Chain.NORMAL_NODE && index == 11 && index2 == -1 && index3 == -1 && cand == 7 && newOnOff == false) {
                        System.out.println("       set new distance: " + distance);
                    }
                }
                // expand the single entry: recalculates distances of all depending entries
                expandTable(src, srcIndex, srcCand, isOn, 0, atIndex);
                if (DEBUG) {
                    if (nodeType == Chain.NORMAL_NODE && index == 11 && index2 == -1 && index3 == -1 && cand == 7 && newOnOff == false) {
                        System.out.println("       expanding...");
                        printTable("??", onTable[548], alses);
                    }
                }
            } else {
                // do nothing
                if (nodeType == Chain.NORMAL_NODE && index == 11 && index2 == -1 && index3 == -1 && cand == 7 && newOnOff == false) {
                    System.out.println("       ignored: " + distance + "/" + oldDistance);
                }
            }
        } else {
//            System.out.println("   add new entry");
            if (nodeType == Chain.ALS_NODE) {
                // index2 contains the ALS index
                src.addEntry(index, Chain.getSLowerAlsIndex(index2),
                        Chain.getSHigherAlsIndex(index2), nodeType, cand, newOnOff,
                        retIndices[0][0], retIndices[0][1], retIndices[0][2],
                        retIndices[0][3], retIndices[0][4], als.getChainPenalty());
            } else {
                src.addEntry(index, index2, index3, nodeType, cand, true,
                        retIndices[0][0], retIndices[0][1], retIndices[0][2],
                        retIndices[0][3], retIndices[0][4], 0);
            }
            src.setDistance(src.index - 1, distance);
            if (DEBUG) {
//                if (nodeType == Chain.NORMAL_NODE && index == 11 && index2 == -1 && index3 == -1 && cand == 7 && newOnOff == false) {
                if (nodeType == Chain.GROUP_NODE) {
                    System.out.println("       entry added: " + Chain.toString(src.entries[src.index - 1]) + " to " + Chain.toString(src.entries[0]));
                    System.out.println("       distance: " + distance);
//                    printTable("??", onTable[548], alses);
                }
            }
        }
    }

    /**
     * Checks, if a {@link GroupNode} entry can be turned on or off due to a net.<br><br>
     * 
     * A <code>GroupNode<code> can be turned on, when all other candidates in the
     * containing house are deleted (there must be more than one candidate for a
     * net to be possible), and it can be turned off, if all other candidates
     * are set.
     * 
     * @param src The {@link TableEntry} of the source.
     * @param srcIndex The current index in <code>entry</code>.
     * @param srcCand The current candidate in <code>entry</code>.
     * @param isOn The current state of the <code>entry</code>.
     * @param gn The {@link GroupNode} itself.
     * @param checkOn <code>true</code>, if checking for {@link GroupNode} is
     *      turned on, <code>false</code> otherwise.
     * @param template A {@link SudokuSet} containing all cells of the house to check.
     */
    private void checkGroupNodeNetEntry(TableEntry src, int srcIndex, int srcCand,
            boolean isOn, GroupNode gn, boolean checkOn, SudokuSet template) {
        int cand = gn.cand;
        SudokuSet srcSet = checkOn ? src.offSets[cand] : src.onSets[cand];
        // collect all candidates, that are deleted from/set in the house
        // (exclude the group node itself)
        tmpSet.setAnd(srcSet, template);
        tmpSet.andNot(gn.indices);
        // collect all candidates, that are still in the house
        // (exclude the group node itself)
        tmpSet1.setAnd(finder.getCandidates()[cand], template);
        tmpSet1.andNot(gn.indices);
        if (tmpSet.equals(tmpSet1) && !tmpSet.isEmpty() && tmpSet.size() > 1) {
            // group node is turned on/turned off by the candidates in tmpSet
            makeNetEntry(src, srcIndex, srcCand, isOn, gn.index1, gn.index2,
                    gn.index3, cand, tmpSet, (short) 0, checkOn,
                    Chain.GROUP_NODE, gn, null);
        }
    }

    /**
     * Convenience method, delegates to 
     * {@link #addChain(solver.TableEntry, int, int, boolean, boolean, boolean)}.
     *
     * @param entry
     * @param cellIndex
     * @param cand
     * @param set
     */
    private void addChain(TableEntry entry, int cellIndex, int cand, boolean set) {
        addChain(entry, cellIndex, cand, set, false);
    }

    /**
     * Convenience method, delegates to 
     * {@link #addChain(solver.TableEntry, int, int, boolean, boolean, boolean)}.
     *
     * @param entry
     * @param cellIndex
     * @param cand
     * @param set
     * @param isNiceLoop
     */
    private void addChain(TableEntry entry, int cellIndex, int cand, boolean set, boolean isNiceLoop) {
        addChain(entry, cellIndex, cand, set, isNiceLoop, false);
    }

    /**
     * Construct the chain for a premise and an implication. Since we have
     * to build the chain from back to start via the retIndices, the
     * chain must be reversed before it can be written into a 
     * {@link SolutionStep}.
     *
     * @param entry Premise for the chain (first step in the chain)
     * @param cellIndex Index of the cell of the implication (last step in the chain)
     * @param cand Candidate of the implication
     * @param set Last link in chain is strong or weak
     * @param isNiceLoop Like <code>isAic</code>, but the first link must leave
     *          the cell and the last link may point to the start cell.
     * @param isAic No element in the chain may link to the middle of the
     *          chain (but it is allowed for two consecutive links to share the same
     *          cell). If the chain is invalid, the method aborts. Links to the
     *          start cell are invalid too for AICs.
     */
    private void addChain(TableEntry entry, int cellIndex, int cand, boolean set, boolean isNiceLoop, boolean isAic) {
//        if (cellIndex != 79 || cand != 6 || entry.getCellIndex(0) != 73 || entry.getCandidate(0) != 1) {
//            return;
//        }
        // construct the new chain
        buildChain(entry, cellIndex, cand, set);

        // now check it and add it to the step if possible
        int newChainIndex = 0;
        if (isNiceLoop && Chain.getSCellIndex(chain[0]) == Chain.getSCellIndex(chain[1])) {
            // the last link must be outside the cell
            return;
        }

        lassoSet.clear();
        int lastCellIndex = -1;
        int lastCellEntry = -1;
        int firstCellIndex = Chain.getSCellIndex(chain[chainIndex - 1]);
        // reverse the chain and check for lassos
        for (int i = chainIndex - 1; i >= 0; i--) {
            int oldEntry = chain[i];
            int newCellIndex = Chain.getSCellIndex(oldEntry);
            if (isNiceLoop || isAic) {
                // no entry is allowed to link back to the chain. we always check
                // the last but one entry (the last may be in the cell)
                if (lassoSet.contains(newCellIndex)) {
                    // forbidden, chain is a lasso
                    return;
                }
                // for Nice Loops a reference to the first cell is valid, for AICs it is not!
                if (lastCellIndex != -1 && (lastCellIndex != firstCellIndex || isAic)) {
                    lassoSet.add(lastCellIndex);
                    // with group nodes: add all cells (nice loop may not cross a group node or als)
                    if (Chain.getSNodeType(lastCellEntry) == Chain.GROUP_NODE) {
                        int tmp = Chain.getSCellIndex2(lastCellEntry);
                        if (tmp != -1) {
                            lassoSet.add(tmp);
                        }
                        tmp = Chain.getSCellIndex3(lastCellEntry);
                        if (tmp != -1) {
                            lassoSet.add(tmp);
                        }
                    } else if (Chain.getSNodeType(lastCellEntry) == Chain.ALS_NODE) {
                        lassoSet.or(alses.get(Chain.getSAlsIndex(lastCellEntry)).indices);
                    }
                }
            }
            lastCellIndex = newCellIndex;
            lastCellEntry = oldEntry;
            if (newChainIndex < tmpChain.length) {
                tmpChain[newChainIndex++] = oldEntry;
            }
            // "min" stands for "multiple implications" - the chain is a net
            // check for mins
            for (int k = 0; k < actMin; k++) {
                if (mins[k][minIndexes[k] - 1] == oldEntry) {
                    // is a min for the current entry -> add it (the first
                    // entry is skipped, it is already in the chain)
                    for (int l = minIndexes[k] - 2; l >= 0; l--) {
                        if (newChainIndex < tmpChain.length) {
                            tmpChain[newChainIndex++] = -mins[k][l];
                        }
                    }
                    if (newChainIndex < tmpChain.length) {
                        tmpChain[newChainIndex++] = Integer.MIN_VALUE;
                    }
                }
            }
        }
        // do we have a chain?
        if (newChainIndex > 0) {
            // add the new chain(s); tmpChains is reused for every step,
            // this is allowed, since the chains are copied, if the globalStep
            // is really added to the steps array
            System.arraycopy(tmpChain, 0, tmpChains[tmpChainsIndex].getChain(), 0, newChainIndex);
            tmpChains[tmpChainsIndex].setStart(0);
            tmpChains[tmpChainsIndex].setEnd(newChainIndex - 1);
            // trigger a recalculation of the chain length, including
            // possible als penalties
            tmpChains[tmpChainsIndex].resetLength();
            globalStep.addChain(tmpChains[tmpChainsIndex]);
            tmpChainsIndex++;
        }
    }

    /**
     * Constructs a chain for a given premise and a given implication. It
     * looks up the correct entry in
     * <code>entry</code> and delegates the real work to     
     * {@link #buildChain(solver.TableEntry, int, int[], boolean, sudoku.SudokuSet)}. 
     * If the chain is a net, the net parts are constructed as
     * well.<br><br>
     *
     * The main chain is written to {@link #chain}, the net parts are
     * written to {@link #mins}. The chain is from back to front, it is
     * reversed by    
     * {@link #addChain(solver.TableEntry, int, int, boolean, boolean, boolean)}.
     *
     * @param entry The table entry holding the premise and the implication
     * @param cellIndex The index of the implication
     * @param cand The candidate of the implication
     * @param set The link type of the implication
     */
    private void buildChain(TableEntry entry, int cellIndex, int cand, boolean set) {
        if (DEBUG) {
            if (Chain.getSCellIndex(entry.entries[0]) == 54 && Chain.getSCandidate(entry.entries[0]) == 8 && Chain.isSStrong(entry.entries[0])
                    && cellIndex == 54 && cand == 8 && !set) {
                int eEntry = Chain.makeSEntry(cellIndex, cand, set);
                System.out.println("buildChain() START (" + Chain.toString(entry.entries[0]) + " - " + Chain.toString(eEntry) + ")");
                doDebug = true;
            }
        }
        // find the entry for the implication in the TableEntry
        chainIndex = 0;
        int chainEntry = Chain.makeSEntry(cellIndex, cand, set);
        if (!entry.indices.containsKey(chainEntry)) {
            if (DEBUG) {
                System.out.println("No chain entry for " + cellIndex + "/" + SolutionStep.getCellPrint(cellIndex) + "/" + cand + "/" + set);
                Logger.getLogger(getClass().getName()).log(Level.WARNING, "No chain entry for {0}/{1}/{2}/{3}", new Object[]{cellIndex, SolutionStep.getCellPrint(cellIndex), cand, set});
            }
            return;
        }
        int index = entry.getEntryIndex(chainEntry);
        if (DEBUG) {
            if (Chain.getSCellIndex(entry.entries[0]) == 54 && Chain.getSCandidate(entry.entries[0]) == 8 && Chain.isSStrong(entry.entries[0])
                    && cellIndex == 54 && cand == 8 && !set) {
                //System.out.println("   1: " + index + ": " + Chain.toString(chainEntry));
            }
        }

        // reset the data structures for multiples inferences (nets)
        actMin = 0;
        for (int i = 0; i < minIndexes.length; i++) {
            minIndexes[i] = 0;
        }
        // construct the main chain
        tmpSetC.clear();
        chainIndex = buildChain(entry, index, chain, false, null, -1, tmpSetC);
        // now build the net parts
        int minIndex = 0;
        while (minIndex < actMin) {
            if (DEBUG) {
                if (Chain.getSCellIndex(entry.entries[0]) == 54 && Chain.getSCandidate(entry.entries[0]) == 8 && Chain.isSStrong(entry.entries[0])
                        && cellIndex == 54 && cand == 8 && !set) {
                    System.out.println("buildChain() start new min: " + minIndex + ": " + Chain.toString(mins[minIndex][0]));
                }
            }
            minIndexes[minIndex] = buildChain(entry, entry.getEntryIndex(mins[minIndex][0]), mins[minIndex], true, minEntries[minIndex], minIndex, tmpSetC);
            minIndex++;
        }
        if (DEBUG) {
            if (Chain.getSCellIndex(entry.entries[0]) == 54 && Chain.getSCandidate(entry.entries[0]) == 8 && Chain.isSStrong(entry.entries[0])
                    && cellIndex == 54 && cand == 8 && !set) {
                int eEntry = Chain.makeSEntry(cellIndex, cand, set);
                System.out.println("buildChain() END (" + Chain.toString(entry.entries[0]) + " - " + Chain.toString(eEntry) + ")");
                doDebug = false;
            }
        }
    }

    /**
     * <i>Really</i> constructs the chain for a given premise and a given
     * implication :-).<br><br> 
     * 
     * <ul> 
     *  <li>Add the implication as first step in the chain</li> 
     *  <li>retIndex1 points to the entry that caused the implication -&gt; 
     *      jump to it and handle it next</li> 
     *  <li>if there are more than one retIndices, the first is treated 
     *      normally; the others are stored in {@link #mins}/{@link #minIndexes}, 
     *      they are evaluated at a later time</li>
     * </ul>
     * 
     * The method returns, when the first entry in <code>entry</code> is 
     * reached.<br><br>
     *
     * All cells of the main chain are stored in <code>chainSet</code>. When the 
     * method is called for a min (multiple inference - the net part of a chain -
     * <code>isMin</code> is <code>true</code>), the method runs until a cell 
     * from the main chain is reached.
     *
     * <b>CAUTION:</b> The chain is stored in
     * <code>actChain</code> in reverse order!<br><br>
     * 
     * 20130902: In normal chains expanded entries are not expanded again. If an
     * expanded entry is met, we jump to the table holding the original and 
     * follow it back to entry 0. Then we jump back to the original table and
     * go on. In Forcing Nets entries are added <i>after the expansion</i>. That
     * means, that an expanded entry in the original table could have an expanded
     * predecessor. If that is the case, we have to take that expanded predecessor
     * from the original table or all goes wrong.
     *
     * @param entry The table entry that holds the premise of the chain at index 0.
     * @param entryIndex The index of the implication in <code>entry</code>.
     * @param actChain The array in which the current chain or min is stored.
     * @param isMin <code>true</code> if we are constructing part of a net.
     * @param minEntry If <code>isMin</code> is <code>true</code>, the 
     *          {@link TableEntry} where the first element (implication) of 
     *          the min came from.
     * @param minIndex The index into {@link #mins} or <code>-1</code>, if the current
     *          chain is not a min.
     * @param chainSet A set that holds all cells of the main chain.
     * @return
     */
    private int buildChain(TableEntry entry, int entryIndex, int[] actChain, boolean isMin,
            TableEntry minEntry, int minIndex, SudokuSet chainSet) {
        if (DEBUG) {
            doDebugCounter++;
            int dEntry = entry.entries[entryIndex];
            if (Chain.getSCellIndex(entry.entries[0]) == 58 && Chain.getSCandidate(entry.entries[0]) == 4 && Chain.isSStrong(entry.entries[0])
                    && Chain.getSCellIndex(dEntry) == 54 && Chain.getSCandidate(dEntry) == 6 && !Chain.isSStrong(dEntry)) {
                System.out.println("  doDebugCounter == " + doDebugCounter);
            }
        }
        int actChainIndex = 0;
        actChain[actChainIndex++] = entry.entries[entryIndex];
        if (DEBUG) {
            if (doDebug) {
                int e = entry.entries[entryIndex];
                System.out.println("added to chain: " + Chain.toString(e) + " - " + isMin + " (Table: " + Chain.toString(entry.entries[0]) + ")");
            }
        }
        int firstEntryIndex = entryIndex;
        boolean expanded = false;
        TableEntry orgEntry = entry;
        while (firstEntryIndex != 0 && actChainIndex < actChain.length) {
            if (entry.isExpanded(firstEntryIndex) || (isMin && minEntry
                    != null && orgEntry != minEntry)) {
                // current entry comes from a different table -> jump to it!
                // an entry can only be expanded, when it comes from the original table orgEntry
                // first ret index contains the table where the node came from
                // or the index in extendedTable if the entry is a group or als node
                if (DEBUG) {
                    if (expanded) {
                        System.out.println("buildChain(): expanded node in expanded entry (should not be possible)!");
                        System.out.println("doDebugCounter = " + doDebugCounter);
                    }
                }
                if (DEBUG) {
                    if (doDebug) {
//                        System.out.println("   3aa: " + entry.isExtendedTable(firstEntryIndex) + "/" + entry.isOnTable(firstEntryIndex) + "/" + Chain.toString(entry.entries[firstEntryIndex]));
//                        if (chainIndex > 0) {
//                            System.out.println("        " + Chain.toString(chain[chainIndex - 1]));
//                        }
//                        if (chainIndex > 1) {
//                            System.out.println("        " + Chain.toString(chain[chainIndex - 2]));
//                        }
                        //printTable("0", orgEntry, alses);
                        //printTable("1", entry, alses);
                    }
                }
                if (isMin && minEntry != null && orgEntry != minEntry) {
                    // first entry in a min comes from a different table!
                    entry = minEntry;
                    minEntry = null;
                } else {
                    // must be taken from orgEntry, because entry contains the real ret index
                    int newEntryIndex = orgEntry.getRetIndex(firstEntryIndex, 0);
                    if (orgEntry.isExtendedTable(firstEntryIndex)) {
                        entry = extendedTable.get(newEntryIndex);
                    } else if (orgEntry.isOnTable(firstEntryIndex)) {
                        entry = onTable[newEntryIndex];
                    } else {
                        entry = offTable[newEntryIndex];
                    }
                }
                expanded = true;
                if (DEBUG) {
                    if (doDebug) {
                        //System.out.println("   3a: xxx: " + Chain.toString(orgEntry.entries[firstEntryIndex]));
                        //printTable("2", entry, alses);
                    }
                }
                firstEntryIndex = entry.getEntryIndex(orgEntry.entries[firstEntryIndex]);
                if (DEBUG) {
                    if (doDebug) {
                        //System.out.println("   3: " + firstEntryIndex + ": " + Chain.toString(entry.entries[firstEntryIndex]));
                    }
                }
            }
            int tmpEntryIndex = firstEntryIndex;
            for (int i = 0; i < 5; i++) {
                entryIndex = entry.getRetIndex(tmpEntryIndex, i);
                if (i == 0) {
                    // the first retIndex points to the next element -> store it
                    // and set it in the chainSet if isMin is false.
                    firstEntryIndex = entryIndex;
                    actChain[actChainIndex++] = entry.entries[entryIndex];
                    if (DEBUG) {
                        if (doDebug) {
                            int e = entry.entries[entryIndex];
                            System.out.println("added to chain: " + Chain.toString(e) + " - " + isMin + " (Table: " + Chain.toString(entry.entries[0]) + ")");
                        }
                    }
                    if (!isMin) {
                        // record all cells of the main chain
                        chainSet.add(entry.getCellIndex(entryIndex));
                        // group nodes
                        int actTableEntry = entry.entries[entryIndex];
                        if (Chain.getSNodeType(actTableEntry) == Chain.GROUP_NODE) {
                            int tmp = Chain.getSCellIndex2(actTableEntry);
                            if (tmp != -1) {
                                chainSet.add(tmp);
                            }
                            tmp = Chain.getSCellIndex3(actTableEntry);
                            if (tmp != -1) {
                                chainSet.add(tmp);
                            }
                        } else if (Chain.getSNodeType(entry.entries[entryIndex]) == Chain.ALS_NODE) {
                            if (Chain.getSAlsIndex(actTableEntry) == -1) {
                                Logger.getLogger(getClass().getName()).log(Level.WARNING, "INVALID ALS_NODE: {0}", Chain.toString(entry.entries[entryIndex]));
                            }
                            chainSet.or(alses.get(Chain.getSAlsIndex(actTableEntry)).indices);
                        }
                    } else {
                        // if the current chain is a min, check if we have reached the main chain
                        // 20130906: We cant search the entire chain, or we could find a start point,
                        // that has not been reached yet. Therefore the index in the main chain, where the min
                        // ends, is recorded in minEndIndices. The search has to start there.
                        if (chainSet.contains(entry.getCellIndex(entryIndex))) {
                            // preselection: the current cell is part of the main chain -> search the main chain
                            // the entry might be later in the chain (smaller index - chain is reversed) than the end
                            // of the min, which is invalid
                            for (int j = minEndIndices[minIndex]; j < chainIndex; j++) {
                                //for (int j = 0; j < chainIndex; j++) {
                                if (chain[j] == entry.entries[entryIndex]) {
                                    // done!
                                    if (DEBUG) {
                                        int e = entry.entries[entryIndex];
                                        if (doDebug) {
                                            System.out.println("  min ended in: " + Chain.toString(e) + " - " + Chain.toString(orgEntry.entries[0]) + " - " + Chain.toString(chain[0]) + " (Table: " + Chain.toString(entry.entries[0]) + ")");
                                        }
                                    }
                                    return actChainIndex;
                                }
                            }
                        }
                    }
                } else {
                    // its a multiple inference entry: store the start entry for later use
                    // we dont show nets in nets; they can exist, but are not spelled out
                    if (entryIndex != 0 && !isMin) {
                        // 0 is not allowed, only possible for first retIndex!
                        if (actMin < mins.length) {
                            mins[actMin][0] = entry.entries[entryIndex];
                            minEntries[actMin] = entry;
                            // the current chain index has to be stored
                            minEndIndices[actMin] = actChainIndex;
                            minIndexes[actMin++] = 1;
                            if (DEBUG) {
                                if (doDebug) {
                                    System.out.println("   added min start: " + Chain.toString(entry.entries[entryIndex]) + " (" + (actMin - 1) + ")");
                                }
                            }
                        }
                    }
                }
            }
            // CAUTION 20130902: In Forcing Nets the predecessor of an
            // expanded node can be an expanded node itself. In that case we
            // have to jump back to the original table as well
            // 20130906: The predecessor of an expaned node can have a larger
            // distance than a possible corresponding (not expanded) node in
            // the original table -> in that case, jump back
            boolean orgIsShorter = false;
            int retEntry = entry.entries[firstEntryIndex];
            if (expanded && orgEntry.containsEntry(retEntry)) {
                // check, if there is a shorter path in the original table
                int actDistance = entry.getDistance(firstEntryIndex);
                if (actDistance > orgEntry.getDistance(orgEntry.getEntryIndex(retEntry))) {
                    orgIsShorter = true;
                }
            }
            if (expanded && (firstEntryIndex == 0 || entry.isExpanded(firstEntryIndex) || orgIsShorter)) {
                // we jumped to another TableEntry and have reached its start
                // or the new entry was expanded itself ->
                // jump back to the original
//                int retEntry = entry.entries[firstEntryIndex];
                entry = orgEntry;
                firstEntryIndex = entry.getEntryIndex(retEntry);
                if (DEBUG) {
                    if (doDebug) {
                        //System.out.println("   4: " + firstEntryIndex + ": " + Chain.toString(retEntry));
                    }
                }
                expanded = false;
            }
        }
        return actChainIndex;
    }

    /**
     * Print the contents of ALL tables (debugging only)
     * @param title
     * @param onTable
     * @param offTable
     * @param alses  
     */
    public static void printTables(String title, TableEntry[] onTable, TableEntry[] offTable, List<Als> alses) {
        if (!DEBUG) {
            return;
        }
        System.out.println("==========================================");
        System.out.println(title);
        System.out.println("==========================================");
        for (int i = 0; i < onTable.length; i++) {
            int index = i / 10;
            int candidate = i % 10;
            if (candidate < 1 || candidate > 9) {
                continue;
            }
            String cell = "r" + (Sudoku2.getLine(index) + 1) + "c" + (Sudoku2.getCol(index) + 1);
            if (onTable[i].index == 0) {
                System.out.println(cell + "=" + candidate + " is EMPTY!");
            } else {
                printTable(cell + "=" + candidate, onTable[i], alses);
                System.out.println();
            }
            if (offTable[i].index == 0) {
                System.out.println(cell + "<>" + candidate + " is EMPTY!");
            } else {
                printTable(cell + "<>" + candidate, offTable[i], alses);
                System.out.println();
            }
        }
    }

    /**
     * Show the contents of one {@link TableEntry} (for debugging).
     *
     * @param title
     * @param entry
     * @param alses  
     */
    public static void printTable(String title, TableEntry entry, List<Als> alses) {
        if (!DEBUG) {
            return;
        }
        System.out.println(title + ": ");
        int anz = 0;
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < entry.index; i++) {
            tmp.append(printTableEntry(entry.entries[i], alses));
            tmp.append("/").append(entry.getDistance(i));
            for (int j = 0; j < entry.getRetIndexAnz(i); j++) {
                int retIndex = entry.getRetIndex(i, j);
                tmp.append(" (");
                if (entry.isExpanded(i)) {
                    tmp.append("EX:").append(retIndex).append(":").append(entry.isExtendedTable(i)).append("/").append(entry.isOnTable(i)).append(")");
                } else {
                    tmp.append(retIndex).append("/").append(printTableEntry(entry.entries[retIndex], alses)).append("/").append(entry.getDistance(retIndex)).append(")");
                }
            }
            tmp.append(" ");
            anz++;
            if ((anz % 5) == 0) {
                tmp.append("\r\n");
            }
        }
        System.out.println(tmp.toString());
//        for (int i = 1; i < entry.onSets.length; i++) {
//            System.out.println(i + " on:  " + entry.onSets[i]);
//            System.out.println(i + " off: " + entry.offSets[i]);
//        }
    }

    /**
     * Show one {@link TableEntry} (for debugging).
     *
     * @param entry
     * @param alses 
     * @return
     */
    public static String printTableEntry(int entry, List<Als> alses) {
        if (!DEBUG) {
            return "";
        }
        int index = Chain.getSCellIndex(entry);
        int candidate = Chain.getSCandidate(entry);
        boolean set = Chain.isSStrong(entry);
        String cell = SolutionStep.getCellPrint(index, false);
        if (Chain.getSNodeType(entry) == Chain.GROUP_NODE) {
            cell = SolutionStep.getCompactCellPrint(index, Chain.getSCellIndex2(entry), Chain.getSCellIndex3(entry));
        } else if (Chain.getSNodeType(entry) == Chain.ALS_NODE) {
            String alsStr = alses != null ? SolutionStep.getAls(alses.get(Chain.getSAlsIndex(entry))) : "UNKNOWN";
            cell = "ALS: " + alsStr;
        }
        if (set) {
            return cell + "=" + candidate;
        } else {
            return cell + "<>" + candidate;
        }
    }

    /**
     * Calculates the total amount of entries in all tables; used for net
     * creation
     *
     * @return
     */
    public int getTableAnz() {
        int entryAnz = 0;
        for (int i = 0; i < onTable.length; i++) {
            if (onTable[i] != null) {
                entryAnz += onTable[i].index;
            }
            if (offTable[i] != null) {
                entryAnz += offTable[i].index;
            }
        }
        return entryAnz;
    }

    /**
     * Show the number of tables and entries in tables (debugging only).
     */
    public void printTableAnz() {
        if (!DEBUG) {
            return;
        }
        int onAnz = 0;
        int offAnz = 0;
        int entryAnz = 0;
        int maxEntryAnz = 0;
        for (int i = 0; i < onTable.length; i++) {
            if (onTable[i] != null) {
                onAnz++;
                entryAnz += onTable[i].index;
                if (onTable[i].index > maxEntryAnz) {
                    maxEntryAnz = onTable[i].index;
                }
            }
            if (offTable[i] != null) {
                offAnz++;
                entryAnz += offTable[i].index;
                if (offTable[i].index > maxEntryAnz) {
                    maxEntryAnz = offTable[i].index;
                }
            }
        }
        System.out.println("Tables: " + onAnz + " onTableEntries, " + offAnz + " offTableEntries, "
                + entryAnz + " Implikationen (" + maxEntryAnz + " max)");










    }

    /**
     * Compares two {@link SolutionStep SolutionSteps} that hold steps found by
     * tabling. The sort order:
     *
     * <ol> 
     *  <li>steps that set cells beat steps that delete candidates</li>
     *  <li>if both steps set cells:
     *  <ul> 
     *      <li>number of cells that can be set</li>
     *      <li>equivalency (same cells?)</li> 
     *      <li>cells with lower indices go first</li> 
     *      <li>chain length in all chains</li>
     *  </ul></li> 
     *  <li>if both steps eliminate candidates:
     *  <ul> 
     *      <li>number of candidates that can be deleted</li> 
     *      <li>equivalency (same cells affected?)</li> 
     *      <li>lower cells and lower candidates first</li> 
     *      <li>chain length in all chains</li>
     *  </ul></li> 
     * </ol>
     */
    class TablingComparator implements Comparator<SolutionStep> {

        /**
         * Compares two {@link SolutionStep SolutionSteps} obtained by tabling.
         * For details see description of the class itself.
         *
         * @param o1
         * @param o2
         * @return
         */
        @Override
        public int compare(SolutionStep o1, SolutionStep o2) {
            int sum1 = 0, sum2 = 0;

            // set cell or delete candidates?
            if (o1.getIndices().size() > 0 && o2.getIndices().isEmpty()) {
                return -1;
            }
            if (o1.getIndices().isEmpty() && o2.getIndices().size() > 0) {
                return +1;
            }
            // different algorithm for setting and eliminating
            if (o1.getIndices().size() > 0) {
                // set cell
                // number of cells that can be set (descending)
                int result = o2.getIndices().size() - o1.getIndices().size();
                if (result != 0) {
                    return result;
                }
                // equivalency (same cells affected)
                if (!o1.isEquivalent(o2)) {
                    // not equivalent: lower cells first
                    sum1 = o1.getSumme(o1.getIndices());
                    sum2 = o1.getSumme(o2.getIndices());
                    return sum1 == sum2 ? 1 : sum1 - sum2;
                }

                // chain length (descending)
                result = o1.getChainLength() - o2.getChainLength();
                if (result != 0) {
                    return result;
                }
            } else {
                // eliminate candidates
                // number of candidates to eliminate (descending)
                int result = o2.getCandidatesToDelete().size() - o1.getCandidatesToDelete().size();
                if (result != 0) {
                    return result;
                }
                // equivalency (same cells affected)
                if (!o1.isEquivalent(o2)) {
                    // not equivalent: lower cells first, lower candidates first
                    result = o1.compareCandidatesToDelete(o2);
                    if (result != 0) {
                        return result;
                    }
                }

                // chain length (descending)
                result = o1.getChainLength() - o2.getChainLength();
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        }
    }

    public static void main(String[] args) {
        SudokuStepFinder finder = new SudokuStepFinder();
        TablingSolver.DEBUG = true;
        Sudoku2 sudoku = new Sudoku2();
        //sudoku.setSudoku(":0100:1:....7.94..7..9...53....5.7..874..1..463.8.........7.8.8..7.....7......28.5.268...:::");
        //sudoku.setSudoku(":0000:x:....7.94..7..9...53....5.7..874..1..463.8.........7.8.8..7.....7......28.5.268...:613 623 233 633 164 165 267 269 973 377 378 379 983 387::");
        // Originalbeispiel
        //sudoku.setSudoku(":0000:x:2.4.857...15.2..4..98..425.8.2..61.79...7.5.257.2....4.29..147..5.....2..87.326..:618 358 867 368 968 381 681 183::");
        // #39462
        //sudoku.setSudoku(":0000:x:.4..1..........5.6......3.15.38.2...7......2..........6..5.7....2.....1....3.14..:211 213 214 225 235 448 465 366 566 468 469::");
        // Another puzzle for your consideration
        //sudoku.setSudoku(":0000:x:61.......9.37.62..27..3.6.9.......85....1....79.......8.769..32..62.879..29....6.:517 419 819 138 141 854 756 459 863 169 469 391::");
        //sudoku.setSudoku(":0702:9:.62143.5.1..5.8.3.5..7.9....28.154..4.56.2..3.16.8.52.6.9851..225...6..1..123.695:711 817 919 422 727 729 929 837 438 838 639 757 957 758 961 772 787 788 792:944 964 985:");
        // Nice Loop tutorial:
        // group node example 1
        //sudoku.setSudoku(":0000:x:..1.5794...961.8.5....8.1.3..279...8..3.....77...3.6..4.7.2....52...17...3.57.2..:632 633 651 863 469 672 872 691 891 699::");
        // example 2
        //sudoku.setSudoku(":0000:x:2....9.5..358...6...42....1.6.....7.5....2..4.8...3.96721...64..4...1.2..5.42...9:713 931 735 736 337 837 752 155 881 984 985 693 398::");
        // bug in Grouped Continuous Nice Loop
        // r7c3<>1 -> falsch! (Group Node)
        //sudoku.setSudoku(":0000:x:.....1.2...4+29.......576+4.8+735.+2.6.1..87....+44......7.56.......3......49.+49.325+6+7:912 814 122 233 555 162 263 874 875 876 182 282 885 887::");
        // r7c378<>1 -> falsch! (ALS)
        //sudoku.setSudoku(":0000:x:.....1.2...4+29.......576+4.8+735.+2.6.1..87....+44......7.56.......3..+6...49.+49.325+6+7:812 912 814 122 233 555 162 263 874 875 876 182 282 885 887::");
        // Beispiel daj
        //sudoku.setSudoku(":0000:x:4..1..8.9....3.54.8....46.1..34.1..8.74....5.98.5.34..749.....5.6..4....3.8..9..4:512 715 735 648 668 378 388 795::");
        // Grouped AIC with 4 eliminations
        //sudoku.setSudoku(":0000:x:....6.+83..36.8..94.2.+3496.....2..5..95.7...8.....+583.......1....65........4..+57.8:164 664 979 286 786 989::");
        // Grouped AIC that touches the beginning of the loop (-> lasso!)
        // Grouped AIC 5- r6c9 -6- r6c4 =6= r13c4 -6- r2c56 =6= r2c9 -6- r6c9 -5- r5c789 =5= r5c4 -5 => r5c789,r6c456<>5
        //sudoku.setSudoku(":0711:5:4+8..+12.391+953..28......+9+4...+1.4..9.886+4.+9+1....79...1+4..+5123.+8.4..+89........1.8...:248 269 369:557 558 559 564 565 566:");
        // Continuous Nice Loop 2- r4c4 =2= r5c6 -2- ALS:r156c7(2|78|4) -4- ALS:r3c49(4|7|2) -2 => r2c4 <> 2, r2c789<>4, r1c9<> 4, r3c8<>4, r3c128<>7, r289c7<>7, r28c7<>8
        sudoku.setSudoku(":0000:x:9...6..2............1.893.......65..41.8...96..24.......352.1..1.........8..1...5:316 716 221 521 621 721 325 725 326 726 741 344 744 944 345 348 748 848 349 749 849 361 861 362 365 366 384 784 985 394 794::");
        // Wrong elminations in grouped continuous nice loop (issue 2795464)
        // 1/2/3/4/6/7/9 3= r2c4 =5= r2c9 -5- ALS:r13c7,r3c9 =7= r6c7 -7- ALS:r4c3,r56c2 -3- r4c4 =3= r2c4 =5 => r2c28,r3456c1,r46c7<>1, r12c9<>2, r4c18<>3, r456c1<>4, r2c4<>6, r6c19<>7, r1c9,r468c7<>9
        // r1c9<>9, r6c7<>9 are invalid
        sudoku.setSudoku(":0709:1234679:5.81...6.....9.4...39.8..7..6...5.....27.95....58...2..8..5134..51.3.....9...8651:221 224 231 743 445 349 666 793:122 128 131 141 147 151 161 167 219 229 341 348 441 451 461 624 761 769 919 947 967 987::11");
        // sollte 2 Grouped AICs für 59 geben:
        //  Grouped AIC: 5/9 9- r7c8 =9= r2c8 =7= r2c5 -7- ALS:r78c6,r9c5 =5= r7c4 -5 => r7c8<>5, r7c4<>9
        //  Grouped AIC: 5/9 9- r7c8 =9= r2c8 =7= r2c5 -7- ALS:r36c4 =5= r7c4 -5 => r7c8<>5, r7c4<>9
        // es wird in 2.1beta nur die 1. gefunden
        sudoku.setSudoku(":0711-4:59:...65+4+328+2458.31.+6+63+8....+459+7+31+4+5+86+2+42+1+38+6..+9+8+56..74+13.84.....7.......+8..6...+8.3.:175 275 975 185 285 785 985:578 974::7");
        //da geht gar nichts...
        //sudoku.setSudoku(":0000:x:.......123......6+4+1...4..+8+59+1...+45+2......1+67..2....+1+4....35+64+9+1..14..8.+6.6....+2.+7:::");
        //sollte chain r4c2=5 -> r7c3=2 / r4c2=5 -> r7c3<>2 geben
        //sudoku.setSudoku("100200300040030050003006007300600800010090060006004009500900700070050020008007005");
        // Easter monster: r5c9=6 -> r1c7=9 / r5c9<>6 -> r1c7<>9
        //sudoku.setSudoku("1.......2.9.4...5...6...7...5.9.3.......7.......85..4.7.....6...3...9.8...2.....1");
        finder.setSudoku(sudoku);
        List<SolutionStep> steps = new ArrayList<SolutionStep>();
        long ticks = System.currentTimeMillis();
        int anzLoops = 1;
        for (int i = 0; i < anzLoops; i++) {
            //steps = finder.getAllForcingChains(sudoku);
            //steps = finder.getAllForcingNets(sudoku);
            steps = finder.getAllGroupedNiceLoops(sudoku);
            TablingSolver ts = finder.getTablingSolver();
            for (int c = 1; c < 10; c++) {
                System.out.println("==== Eliminations for candidate " + c + " ==============");
                for (int j = 0; j < ts.onTable.length; j++) {
                    if (ts.onTable[j].index > 0 && !ts.onTable[j].offSets[c].isEmpty()) {
                        System.out.println(SolutionStep.getCellPrint(j / 10) + "/" + (j % 10) + "/on:  " + ts.onTable[j].offSets[c]);
                    }
                    if (ts.offTable[j].index > 0 && !ts.offTable[j].offSets[c].isEmpty()) {
                        System.out.println(SolutionStep.getCellPrint(j / 10) + "/" + (j % 10) + "/off: " + ts.offTable[j].offSets[c]);
                    }
                }
            }
            //steps = ts.getAllNiceLoops(sudoku);
            //steps = finder.getAllGroupedNiceLoops(sudoku);
        }
        ticks = System.currentTimeMillis() - ticks;
        System.out.println("Dauer: " + (ticks / anzLoops) + "ms");
        System.out.println("Anzahl Steps: " + steps.size());
        for (int i = 0; i < steps.size(); i++) {
            //System.out.println(steps.get(i).getCandidateString());
            System.out.println(steps.get(i).toString(2));
        }
    }
}
