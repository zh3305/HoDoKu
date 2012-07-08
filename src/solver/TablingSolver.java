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
 * cell where that candidate can be eliminated - offSets): <ol> <li>only one
 * chain:<ul> <li>two values set in the same cell (AND onSets) -> premise was
 * wrong </li> <li>same value set twice in one house -> premise was wrong</li>
 * <li>all candidates deleted from a cell -> premise was wrong</li>
 * <li>candidate cand be set in and deleted from a cell simultaneously ->
 * premise was wrong</li> <li>all candidates are deleted from a cell -> premise
 * was wrong</li></ul></li> <li>two chains for the same start candidate
 * (candidate set and deleted):<ul> <li>both chains lead to the same value in
 * onSets -> value can be set</li> <li>both chains lead to the same value in
 * offSets -> candidate can be deleted</li></ul></li> <li>chains for all
 * candidates in one house/cell set:<ul> <li>both chains lead to the same value
 * in onSets -> value can be set</li> <li>both chains lead to the same value in
 * offSets -> candidate can be deleted</li></ul></li> </ol>
 *
 * 20081013: AIC added (combined with Nice Loops)<br><br> For every Nice Loop
 * that starts with a strong inference out of the start cell and ends with a
 * weak inference into the start cell the AIC (start cell - last strong
 * inference) is checked. If it gives more than one elimination, it is stored as
 * AIC instead of as Nice Loop. The check is done for discontinuous loops
 * only.<br><br>
 *
 * AIC eliminations: <ul> <li>if the candidates of the endpoints are equal, all
 * candidates can be eliminated that see both endpoints</li> <li>if the
 * candidates are not equal, cand A can be eliminated in cell b and vice
 * versa</li> </ul>
 *
 * @author hobiwan
 */
public class TablingSolver extends AbstractSolver {

    private static final long CLEANUP_INTERVAL = 5 * 60 * 1000;
    /**
     * Enable additional output for debugging.
     */
    private static boolean DEBUG = false;
    /**
     * Maximum recursion depth in buildung the tables.
     */
    private static final int MAX_REC_DEPTH = 50;
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
     * all candidates in a cell respectively. Used for Forcing chain/Net checks.
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
    private Sudoku2 savedSudoku;            // Sudoku2 im Ausgangszustand (für Erstellen der Tables)
    private int[][] retIndices = new int[MAX_REC_DEPTH][5]; // indices ermitteln
//    private int[][] retIndices1 = new int[MAX_REC_DEPTH][5]; // indices ermitteln
    private List<GroupNode> groupNodes = null;  // a list with all group nodes for a given sudoku
    private List<Als> alses = null; // a list with all available ALS for a given sudoku
//    private SudokuSet alsBuddies = new SudokuSet(); // cells that can see all the cells of the als
    private SudokuSet[] alsEliminations = new SudokuSet[10]; // all cells with elminations for an als, sorted by candidate
    private SudokuStepFinder simpleFinder;
    private List<SolutionStep> singleSteps = new ArrayList<SolutionStep>();  // für Naked und Hidden Singles
    private int[] chain = new int[Options.getInstance().getMaxTableEntryLength()]; // globale chain für buildChain()
    private int chainIndex = 0; // Index des nächsten Elements in chain[]
    private int[][] mins = new int[200][Options.getInstance().getMaxTableEntryLength()]; // globale chains für networks
    private int[] minIndexes = new int[mins.length]; // Indexe der nächsten Elemente in mins[]
    private int actMin = 0;                          // derzeit aktuelles min
    private int[] tmpChain = new int[Options.getInstance().getMaxTableEntryLength()]; // globale chain für addChain()
    private Chain[] tmpChains = new Chain[9];
    private int tmpChainsIndex = 0;
    private SudokuSet lassoSet = new SudokuSet();  // für addChain: enthält alle Zellen-Indices der Chain
    private List<TableEntry> extendedTable = null; // Tables for group nodes, ALS, AUR...
    private SortedMap<Integer, Integer> extendedTableMap = null; // entry -> index in extendedTable
    private int extendedTableIndex = 0; // current index in extendedTable
    private boolean initialized = false;
    private long lastUsed = -1;

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
     * Releases memory, if the solver has not been used for more than {@link #CLEANUP_INTERVAL}
     * ms.<br>
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
     * called by the fish finder before the fish search starts. For every fish {@link #checkKrakenTypeOne(sudoku.SudokuSet, int, int)}
     * or
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
        expandTables(onTable);
        expandTables(offTable);
        ticks = System.currentTimeMillis() - ticks;
        if (DEBUG) {
            System.out.println("expandTables(): " + ticks + "ms");
        }
        printTableAnz();
        //printTable("r1c6=6 expand", onTable[56]);
        //printTable("r3c2<>8 expand", offTable[198]);
    }

    /**
     * Search for Kraken Fish Type 1: if a chain starting and ending with a weak
     * link exists from every cell in fins to candidate in index, a KF Type 1
     * exists.
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
     * Get the shortest NiceLoop/AIC in the grid. Delegates to {@link #doGetNiceLoops()}.
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
        expandTables(onTable);
        expandTables(offTable);
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
        long ticks = System.currentTimeMillis();
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
        //printTable("r6c8=1 fill", onTable[521]);
        //printTable("r6c8<>1 fill", offTable[521]);

        // expand tables
        ticks = System.currentTimeMillis();
        expandTables(onTable);
        expandTables(offTable);
        ticks = System.currentTimeMillis() - ticks;
        if (DEBUG) {
            System.out.println("expandTables(): " + ticks + "ms");
        }
        printTableAnz();
        //printTable("r6c8=1 expand", onTable[521]);
        //printTable("r6c8<>1 expand", offTable[521]);

        // ok, hier beginnt der Spass!
        ticks = System.currentTimeMillis();
        checkForcingChains();
//        // TODO: DEBUG
//        for (SolutionStep step : steps) {
//            if (step.getCandidatesToDelete().get(0).getIndex() == 3 && step.getCandidatesToDelete().get(0).getValue() == 5) {
//                System.out.println("==================================");
//                System.out.println("   " + step.toString(2));
//                List<Chain> chains = step.getChains();
//                for (Chain chain : chains) {
//                    System.out.println("   chain: " + chain);
//                }
//                System.out.println("==================================");
//            }
//        }
        ticks = System.currentTimeMillis() - ticks;
        if (DEBUG) {
            System.out.println("checkChains(): " + ticks + "ms");
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
                        System.out.println("More than one chein/net found 1");
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
                        System.out.println("More than one chein/net found 2");
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
     * chain entry has to be adjusted and all candidates for the entry have to
     * be put as endo fins
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
     * <code>src</code>. Used to overwrite a longer chain/net already found with
     * a shorter one that provides the same outcome.
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
            for (int i = 0; i < src.getCandidatesToDelete().size(); i++) {
                dest.getCandidatesToDelete().set(i, src.getCandidatesToDelete().get(i));
            }
        }
        // copy al ALS
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
        int i = 0;
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
     * Checks if a step with the same effect is already contained in {@link #steps}.
     * If not, the new step is added. If it is already there, the old step is
     * replaced with the new one if the chains in the new step are shorter. If
     * they are longer, the new step is discarded.
     */
    private void replaceOrCopyStep() {
        adjustType(globalStep);
        if (!chainsOnly && (globalStep.getType() == SolutionType.FORCING_CHAIN_CONTRADICTION
                || globalStep.getType() == SolutionType.FORCING_CHAIN_VERITY)) {
            // we only want chains but got a net (no caching possible!)
            return;
        }
        // adjust the ALS nodes
        adjustChains(globalStep);
//        System.out.println("replaceorcopystep: " + globalStep.toString(2));
        String del = null;
        if (Options.getInstance().isOnlyOneChainPerStep()) {
            if (globalStep.getCandidatesToDelete().size() > 0) {
                // candidates can be deleted
                del = globalStep.getCandidateString();
            } else {
                // cells can be set
                del = globalStep.getSingleCandidateString();
            }
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
        // all steps use the same chains -> they have to be cloned when copying
        List<Chain> oldChains = globalStep.getChains();
        int chainAnz = oldChains.size();
        oldChains.clear();
        for (int i = 0; i < chainAnz; i++) {
            oldChains.add((Chain) tmpChains[i].clone());
        }
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
            tmp.append(printTableEntry(entryList.get(i).entries[0]));
        }
        return tmp.toString();
    }

    /**
     * <code>on</code> and
     * <code>off</code> lead to the same conclusion. This is a verity and the
     * conclusion has to be always true.<br><br>
     *
     * Note: If one of the chains gets back to the originating cell, the other
     * chain is only one element long. The whole thing really is a Nice Loop and
     * has already been handled by {@link #checkOneChain(solver.TableEntry)}. It
     * is ignored here.
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
     * <code>entry</code> for all combinations that lead to a conclusion. <ul>
     * <li>setting/deleting a candidate in/from a cell leades to that candidate
     * beeing deletedfrom/set in that very cell -> original assumption was
     * false.</li> <li>two chains from the same start lead to a candidate set in
     * and deleted from the same cell -> assumption is false.</i> <li>two chains
     * from the same start lead to two different values set in the same cell ->
     * assumption is false.</li> <li>two chains from the same start lead to the
     * same value set twice in one house -> assumption is false.</li> <li>chains
     * from the same start lead to all instances of a candidate beeing removed
     * from a cell -> assumption is false.</li> <li>chains from the same start
     * lead to all instances of a candidate beeing removed from a house ->
     * assumption is false.</li> </ul>
     *
     * @param entry
     */
    private void checkOneChain(TableEntry entry) {
        if (entry.index == 0) {
            // table is empty -> nothing to do
            return;
        }
        // chain contains the invers of the assumption -> assumption is false
        if ((entry.isStrong(0) && entry.offSets[entry.getCandidate(0)].contains(entry.getCellIndex(0)))
                || (!entry.isStrong(0) && entry.onSets[entry.getCandidate(0)].contains(entry.getCellIndex(0)))) {
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
        checkHouseSet(entry, Sudoku2.LINE_TEMPLATES, Sudoku2.LINE);
        checkHouseSet(entry, Sudoku2.COL_TEMPLATES, Sudoku2.COL);
        checkHouseSet(entry, Sudoku2.BLOCK_TEMPLATES, Sudoku2.BLOCK);

        // cell without candidates -> assumption false
        // chain creates a cell without candidates (delete sets OR ~allowedPositions, AND all
        // together, AND with ~set sets -> must not be 1
        // CAUTION: exclude all cells in which a value is already set
        tmpSet.setAll();
        for (int i = 1; i < entry.offSets.length; i++) {
            tmpSet1.set(entry.offSets[i]);
            tmpSet1.orNot(finder.getCandidates()[i]);
            tmpSet.and(tmpSet1);
        }
        for (int i = 0; i < entry.onSets.length; i++) {
            tmpSet.andNot(entry.onSets[i]);
        }
        tmpSet2.clear();
        for (int i = 1; i < finder.getPositions().length; i++) {
            tmpSet2.or(finder.getPositions()[i]);
        }
        tmpSet.andNot(tmpSet2);
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
     * Check, if all instances of a canddiate are delete from one house. If so,
     * the assumption was invalid: <ul> <li>Get all instances of the candidate
     * in the house</li> <li>If there are canddiates and the set equals the
     * offSet, step was found</li> </ul>
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
     * Checks, if an assumptions leads to the same vaule set twice in one house.
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
     * A Nice Loop in this implementation always starts and ends with a {@link Chain#NORMAL_NODE}.
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
     * AICs are checked separately: The end of the chain has to be: <ul>
     * <li>on-entry for the same candidate as the start cell (Type 1), if the
     * combined buddies of start and end cell can eliminate more than one
     * candidate</li> <li>on-entry for a different candidate if the end cell
     * sees the start cell and if the start cell contains a candidate of the
     * chain end and the end cell contains a candidate of the chain start</li>
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
                    if (sudoku.isCandidate(tables[i].getCellIndex(j), startCandidate)
                            && sudoku.isCandidate(startIndex, tables[i].getCandidate(j))) {
                        // Type 2
                        checkAic(tables[i], j);
                    }
                }
            }
        }
    }

    /**
     * If the first and the last cell of the chain are identical, the chain is a
     * Nice Loop.<br><br>
     *
     * Discontinuous Nice Loop: <dl> <dt>First and last link are weak for the
     * same candidate:</dt> <dd>Candidate can be eliminated from the start
     * cell</dd> <dt>First and last link are strong for the same candidate:</dt>
     * <dd>Candidate can be set in the start cell (in the step all other
     * candidates are eliminated from the cell, leads to a naked single)</dd>
     * <dt>One link is weak and the other strong, they are for different
     * candidates:</dt> <dd>The candidate from the weak link can be eliminated
     * from the start cell</dd> </dl>
     *
     * Continuous Nice Loop: <dl> <dt>Two weak links:</dt> <dd>First cell must
     * be bivalue, candidates must be different</dd> <dt>Two strong links:</dt>
     * <dd>Candidates must be different</dd> <dt>One link strong, the other
     * weak:</dt> <dd>Both links must be for the same candidate</dd> </dl>
     *
     * If a Continuous nice Loop is present, the following eliminations are
     * possible: <dl> <dt>One cell reached and left with a strong link:</dt>
     * <dd>All candidates not present in the strong links can be eliminated from
     * the cell</dd> <dt>Weak link between cells:</dt> <dd>Link candidate can be
     * eliminated from all cells, that see both cells of the link</dd> </dl>
     *
     * Chains are created backwards. We cant be sure, if the first link really
     * leaves the cell before we have created the actual chain. All chains,
     * which first link remains in the start cell, are ignored.
     *
     * @param entry TableEntry für den Start-Link
     * @param entryIndex Index auf den vorletzten Link des Nice Loops (ist
     * letzter Eintrag, der in der Table noch enthalten ist).
     */
    private void checkNiceLoop(TableEntry entry, int entryIndex) {
        // A Nice Loop must be at least 3 links long
        if (entry.getDistance(entryIndex) <= 2) {
            // Chain too short -> no eliminations possible
            return;
        }

        // check loop type
        globalStep.reset();
        globalStep.setType(SolutionType.DISCONTINUOUS_NICE_LOOP);
        resetTmpChains();
        addChain(entry, entry.getCellIndex(entryIndex), entry.getCandidate(entryIndex), entry.isStrong(entryIndex), true);
        if (globalStep.getChains().isEmpty()) {
            // invalid chain -> build a lasso somewhere -> ignore it!
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
            // Discontinous -> eliminate weak link
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
                    // CAUTION: If one of the cells is entry point for an ALS, nothing can be eliminated;
                    //          if one or both cells are group nodes, only candidates, that see all of the group node cells,
                    //          can be eliminated
                    // 20090224: entries to ALS can be treated like normal group nodes: all candidates in the
                    //          same house that dont belong to the node or the ALS can be eliminated
                    //          plus: all ALS candidates that are not entry/exit candidates eliminate all
                    //          candidates they can see
                    // 20100218: If an ALS node forces a digit (ALS left via more than one candidate -> all
                    //          candidates except one are eliminated in another cell) the leaving weak link is
                    //          missing (next link is strong to forced cell); in that case all other candidates
                    //          in the forced cell are exit candidates and may not be eliminated
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

        if (globalStep.getCandidatesToDelete().size() > 0) {
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
            // only one Nice Loop per set of eliminations
            String del = globalStep.getCandidateString();
            Integer oldIndex = deletesMap.get(del);
            if (oldIndex != null && steps.get(oldIndex.intValue()).getChainLength() <= nlChainLength) {
                // an eqivalent does exist and is shorter than the new one
                return;
            }
            deletesMap.put(del, steps.size());
            // the chain has to be copied
            newChain = (Chain) globalStep.getChains().get(0).clone();
            globalStep.getChains().clear();
            globalStep.getChains().add(newChain);
            adjustChains(globalStep);
            steps.add((SolutionStep) globalStep.clone());
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
            if (tmpSet.size() > 1) {
                for (int i = 0; i < tmpSet.size(); i++) {
                    if (tmpSet.get(i) != startIndex) {
                        globalStep.addCandidateToDelete(tmpSet.get(i), startCandidate);
                    }
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
        String del = globalStep.getCandidateString();
        Integer oldIndex = deletesMap.get(del);
        if (oldIndex != null && steps.get(oldIndex.intValue()).getChainLength() <= globalStep.getChains().get(0).getLength()) {
            // a similar chain already exists and is shorter than the new one -> ignore it
            return;
        }
        deletesMap.put(del, steps.size());
        // chain must be copied
        newChain = (Chain) globalStep.getChains().get(0).clone();
        globalStep.getChains().clear();
        globalStep.getChains().add(newChain);
        adjustChains(globalStep);
        steps.add((SolutionStep) globalStep.clone());
    }

    /**
     * Fills the tables with all initial consequences. One table exists for
     * every outcome (set/not set) of every candidate in the sudoku. If {@link #chainsOnly}
     * is set, only direct dependencies are recorded. If it is not set,
     * {@link #getTableEntry(solver.TableEntry, int, int, boolean) } is used to
     * dig a little deeper.<br><br>
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
                    // candidate ON deletes all other canddiates from the cell and
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
                        tmpSet.set(tmpSet1);
                        tmpSet.and(Sudoku2.ALL_CONSTRAINTS_TEMPLATES[constr]);
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
        }
    }

    /**
     * Fills {@link #extendedTable } with all group nodes. Group nodes are
     * always handled as chains - only direct implications are stored.<br><br>
     *
     * Collect all group nodes. For every group node do: <ul> <li>make a table
     * for every group node (on and off);</li> <li>write the index in
     * extendedTable into extendedTableMap (together with the group node
     * entry)</li> <li><dl> <dt>for ON entries:</dt> <dd>every candidate that
     * can see all group node cells is turned OFF; every other group node that
     * can see (and doesnt overlap) the actual group node is turned OFF</dd>
     * <dt>for OFF entries:</dt> <dd>if a single candidate in one of the houses
     * of the group node exists, it is turned ON; if only one other
     * non-overlapping group node (without extra non-group nodes) exists in one
     * of the houses, it is turned ON</dd> </dl></li> </ul> Links to the group
     * nodes have to be added in normal tables that trigger the group node: <ul>
     * <li><dl> <dt>for ON entries:</dt> <dd>if only one additional candidate
     * exists in one of the houses, the entry is added to that candidate's
     * offTable</dd> <dt>for OFF entries:</dt> <dd>the entry is added to the
     * onTable of every candidate that sees the group node</dd> </dl></li> </ul>
     *
     * <b>CAUTION:</b> Must be called AFTER {@link #fillTables() } or the
     * attributes
     * {@link #extendedTableMap } and {@link #extendedTableIndex }
     * will not be properly initialized; the initialization cannot be moved
     * here, because it must be possible to call {@link #fillTablesWithGroupNodes()
     * }
     * and {@link #fillTablesWithAls() } in arbitrary order.
     */
    private void fillTablesWithGroupNodes() {
        // get all the group nodes
        groupNodes = GroupNode.getGroupNodes(finder);
        // now handle them
        for (int i = 0; i < groupNodes.size(); i++) {
            GroupNode gn = groupNodes.get(i);
            // one table for ON
            TableEntry onEntry = getNextExtendedTableEntry(extendedTableIndex);
            onEntry.addEntry(gn.index1, gn.index2, gn.index3, Chain.GROUP_NODE, gn.cand, true, 0, 0, 0, 0, 0, 0);
            extendedTableMap.put(onEntry.entries[0], extendedTableIndex);
            extendedTableIndex++;
            // and one for OFF
            TableEntry offEntry = getNextExtendedTableEntry(extendedTableIndex);
            offEntry.addEntry(gn.index1, gn.index2, gn.index3, Chain.GROUP_NODE, gn.cand, false, 0, 0, 0, 0, 0, 0);
            extendedTableMap.put(offEntry.entries[0], extendedTableIndex);
            extendedTableIndex++;

            // ok: collect candidates that can see the group node
            tmpSet.set(finder.getCandidates()[gn.cand]);
            tmpSet.and(gn.buddies);
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
                // the candidates offTable triggers the offEntry
                tmpSet1.set(tmpSet);
                tmpSet1.and(Sudoku2.BLOCK_TEMPLATES[gn.block]);
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
            GroupNode gn2 = null;
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
     * the cell are not stored in the chain, since we can't handle links with
     * more than one candidate).<br><br>
     *
     * Since every ALS can trigger different sets of eliminations depending on
     * how it is reached, every ALS can have more than one table entry. The weak
     * link that provides the locked set is not stored in the chain (it can
     * affect multiple candidates, that don't form a group node, which we can't
     * handle). Eliminations caused by locked sets can trigger other
     * ALSes.<br><br>
     *
     * For every ALS do: <ul> <li>check all possible entries; if an entry
     * provides eliminations or forces cells make a table for that entry (only
     * off)</li> <li>write the index in {@link #extendedTable } into {@link #extendedTableMap
     * }
     * (together with the ALS entry)</li> <li>add the ALS entry to the onTable
     * of the candidate/group node/als that provides the entry</li> <li>every
     * candidate/group node deleted by the resulting locked set is added to the
     * ALS's table as is every newly triggered ALS</li> </ul>
     *
     * The ALS entry has the index of the first candidate that provides the
     * entry set as index1, the index in the ALS-array set as index2.<br><br>
     *
     * More detailed: for every ALS do <ul> <li>for every candidate of the als
     * find all remaining candidates in the grid: they are all valid
     * entries</li> <li>if one of the entries from above is a member of a group
     * node, that doesn't overlap the als, the group node is an additional
     * entry</li> <li>if the remaining locked set provides eliminations, record
     * them and check for possible forcings; note that the eliminations could
     * provide an entry for another als; also, the eliminations could form a
     * group node</li> </ul>
     *
     * <b>20090220:</b> BUG - alsBuddies contains only cells, that can see all
     * cells of the ALS, its then used for finding possible entries and
     * eliminations; this is incomplete: entries and eliminations only have to
     * see all cells of the ALS that contain a certain candidate!<br><br>
     *
     * <b>CAUTION:</b> Must be called AFTER {@link #fillTables() } or the
     * attributes
     * {@link #extendedTableMap } and {@link #extendedTableIndex } will not be
     * properly initialized; the initialization cannot be moved here, because it
     * must be possible to call {@link #fillTablesWithGroupNodes() } and
     * {@link #fillTablesWithAls() } in arbitrary order.
     */
    private void fillTablesWithAls() {
        // get all ALSes
        alses = finder.getAlses(true);
        // handle them
        for (int i = 0; i < alses.size(); i++) {
            Als als = alses.get(i);
            if (als.indices.size() == 1) {
                // alses with size one (= nodes with two candidates) are ignored
                continue;
            }
//            als.getBuddies(alsBuddies);
            // for every candidate find all remaining candidates in the grid
            for (int j = 1; j <= 9; j++) {
                // first check, if there are possible eliminations (nothing to do if not):
                // for all other candidates get all cells, that contain that 
                // candidate and can see all cells of the ALS;
                // any such candidate can be eliminated
                // 20090220: a candidiate doesnt have to see all cells of the ALS, only the cells
                //    that contain that candidate
                if (als.indicesPerCandidat[j] == null || als.indicesPerCandidat[j].isEmpty()) {
                    // nothing to do -> next candidate
                    continue;
                }
                boolean eliminationsPresent = false;
                for (int k = 1; k <= 9; k++) {
                    alsEliminations[k].clear();
                    if (k == j) {
                        // that candidate is not in the als anymore
                        continue;
                    }
                    if (als.indicesPerCandidat[k] != null) {
                        alsEliminations[k].set(finder.getCandidates()[k]);
                        // 20090220: use the correct buddies
                        //alsEliminations[k].and(alsBuddies);
                        alsEliminations[k].and(als.buddiesPerCandidat[k]);
                        if (!alsEliminations[k].isEmpty()) {
                            // possible eliminations found
                            eliminationsPresent = true;
                        }
                    }
                }
                if (!eliminationsPresent) {
                    // nothing to do -> next candidate
                    continue;
                }
                // Eliminations are possible, create a table for the als with that entry
                int entryIndex = als.indicesPerCandidat[j].get(0);
                TableEntry offEntry = null;
                if ((offEntry = getAlsTableEntry(entryIndex, i, j)) == null) {
                    offEntry = getNextExtendedTableEntry(extendedTableIndex);
                    offEntry.addEntry(entryIndex, i, Chain.ALS_NODE, j, false, 0);
                    extendedTableMap.put(offEntry.entries[0], extendedTableIndex);
                    extendedTableIndex++;
                }
                // put the ALS into the onTables of all entry candidates:
                // find all candidates, that can provide an entry into the als
                tmpSet.set(finder.getCandidates()[j]);
                // 20090220: use the correct buddies
                //tmpSet.and(alsBuddies);
                tmpSet.and(als.buddiesPerCandidat[j]);
                int alsEntry = Chain.makeSEntry(entryIndex, i, j, false, Chain.ALS_NODE);
                for (int k = 0; k < tmpSet.size(); k++) {
                    int actIndex = tmpSet.get(k);
                    TableEntry tmp = onTable[actIndex * 10 + j];
                    tmp.addEntry(entryIndex, i, Chain.ALS_NODE, j, false, 0);
                    // every group node in which the candidate is a member and which doesn't overlap
                    // the als is a valid entry too; since we look for an on entry for the group
                    // node all group node cells have to see the appropriate cells of the als
                    for (int l = 0; l < groupNodes.size(); l++) {
                        GroupNode gAct = groupNodes.get(l);
                        if (gAct.cand == j && gAct.indices.contains(actIndex)) {
                            // first check overlapping
                            tmpSet1.set(als.indices);
                            if (!tmpSet1.andEmpty(gAct.indices)) {
                                // group node overlaps als -> ignore
                                continue;
                            }
                            // now check visibility: all group node cells have to be
                            // buddies of the als cells that hold the entry candidate
                            tmpSet1.set(als.indicesPerCandidat[j]);
                            if (!tmpSet1.andEquals(gAct.buddies)) {
                                // invalid
                                continue;
                            }
                            // the same group node could be found more than once
                            int entry = Chain.makeSEntry(gAct.index1, gAct.index2, gAct.index3, j, true, Chain.GROUP_NODE);
                            // if we had had that node already, it's onTable contained the als
                            TableEntry gTmp = extendedTable.get(extendedTableMap.get(entry));
                            if (gTmp.indices.containsKey(alsEntry)) {
                                // already present -> ignore
                                continue;
                            }
                            // new group node -> add the als
                            gTmp.addEntry(entryIndex, i, Chain.ALS_NODE, j, false, 0);
                        }
                    }
                }
                // now for the eliminations: candidates and group nodes
                for (int k = 1; k <= 9; k++) {
                    if (alsEliminations[k].isEmpty()) {
                        // no eliminations
                        continue;
                    }
                    // every single elimination must be recorded
                    for (int l = 0; l < alsEliminations[k].size(); l++) {
                        // 20090213: add ALS penalty to distance
                        offEntry.addEntry(alsEliminations[k].get(l), k, als.getChainPenalty(), false);
//                        offEntry.addEntry(alsEliminations[k].get(l), k, false);
                    }
                    // if a group node is a subset of the eliminations, it is turned off as well
                    for (int l = 0; l < groupNodes.size(); l++) {
                        GroupNode gAct = groupNodes.get(l);
                        if (gAct.cand != k) {
                            // group node is for wrong candidate
                            continue;
                        }
                        tmpSet1.set(gAct.indices);
                        if (!tmpSet1.andEquals(alsEliminations[k])) {
                            // not all group node cells are eliminated
                            continue;
                        }
                        // 20090213: adjust penalty for ALS
                        offEntry.addEntry(gAct.index1, gAct.index2, gAct.index3, Chain.GROUP_NODE,
                                k, false, 0, 0, 0, 0, 0, als.getChainPenalty());
//                        offEntry.addEntry(gAct.index1, gAct.index2, gAct.index3, Chain.GROUP_NODE, k, false, 0, 0, 0, 0, 0);
                    }
                }
                // now als: if the eliminations for one candidate cover all cells with
                // that candidate in another non-overlapping als, that als is triggered
                // we do that here for performance reasons
                for (int k = 0; k < alses.size(); k++) {
                    if (k == i) {
                        // not for ourself
                        continue;
                    }
                    Als tmpAls = alses.get(k);
                    tmpSet1.set(als.indices);
                    if (!tmpSet1.andEmpty(tmpAls.indices)) {
                        // overlapping -> ignore
                        continue;
                    }
                    for (int l = 1; l <= 9; l++) {
                        if (alsEliminations[l] == null || alsEliminations[l].isEmpty()
                                || tmpAls.indicesPerCandidat[l] == null
                                || tmpAls.indicesPerCandidat[l].isEmpty()) {
                            // nothing to do
                            continue;
                        }
                        // 20090220: tmpAls has not to be equal to alsEliminations, alsEliminations
                        //   must contain tmpAls!
                        //tmpSet1.set(tmpAls.indicesPerCandidat[l]);
                        //if (!tmpSet1.andEquals(alsEliminations[l])) {
                        tmpSet1.set(alsEliminations[l]);
                        if (!tmpSet1.contains(tmpAls.indicesPerCandidat[l])) {
                            // no entry
                            continue;
                        }
                        // create the table for the triggered als (if it does not produce
                        // valid eliminations it would be missing later on)
                        int tmpAlsIndex = tmpAls.indicesPerCandidat[l].get(0);
                        if (getAlsTableEntry(tmpAlsIndex, k, l) == null) {
                            TableEntry tmpAlsEntry = getNextExtendedTableEntry(extendedTableIndex);
                            tmpAlsEntry.addEntry(tmpAlsIndex, k, Chain.ALS_NODE, l, false, 0);
                            extendedTableMap.put(tmpAlsEntry.entries[0], extendedTableIndex);
                            extendedTableIndex++;
                        }
                        // 20090213: adjust for ALS penalty
                        offEntry.addEntry(tmpAlsIndex, k, Chain.ALS_NODE, l, false, als.getChainPenalty());
//                        offEntry.addEntry(tmpAlsIndex, k, Chain.ALS_NODE, l, false);
                    }
                }
                // last but not least: forcings
                // if one of the als's buddies has only one candidate left
                // after the eliminations, it is forced
                // 20090220: use the correct buddies
                // only necessary, if the cell contains more than 2 candidates (its
                // handled correctly with only two candidates)
                for (int k = 0; k < als.buddies.size(); k++) {
                    int cellIndex = als.buddies.get(k);
                    if (sudoku.getValue(cellIndex) != 0 || sudoku.getAnzCandidates(cellIndex) == 2) {
                        // cell already set
                        continue;
                    }
                    sudoku.getCandidateSet(cellIndex, tmpSet1);
                    for (int l = 1; l <= 9; l++) {
                        if (alsEliminations[l] != null && alsEliminations[l].contains(cellIndex)) {
                            // delete candidate
                            tmpSet1.remove(l);
                        }
                    }
                    if (tmpSet1.size() == 1) {
                        // forcing!
                        // 20090213: adjust for ALS penalty (plus the extra omitted link)
                        offEntry.addEntry(cellIndex, tmpSet1.get(0), als.getChainPenalty() + 1, true);
//                        offEntry.addEntry(cellIndex, tmpSet1.get(0), true);
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
        TableEntry entry = null;
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
     * is deleted). To detect nets, the whole operation is repeated {@link Options#anzTableLookAhead}
     * times.<br>
     *
     * All operations have to be done on a copy of the original sudoku. The
     * candidates in the {@link #finder} are not updated (they are not used and
     * after the operation the sudoku has not changed).
     *
     * If
     * <code>set</code> is
     * <code>true</code>, the cell is set and all newly created Hidden and Naked
     * Singles are collected and executed. If it is
     * <code>false</code>, it is eliminated. If that creates single(s), they are
     * executed and handled as well.<br>
     *
     * If a cell is set, this is delegated to {@link #setCell(int, int, solver.TableEntry, boolean, boolean, int)
     * }.
     *
     * @param entry The {@link TableEntry}
     * @param cellIndex the index of the current cell
     * @param cand The current candidate
     * @param set
     * <code>true</code> if the candidate is to be set, else
     * <code>false</code>
     */
    private void getTableEntry(TableEntry entry, int cellIndex, int cand, boolean set) {
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
     * @param cellIndex
     * @param cand
     * @param entry
     * @param getRetIndices
     * @param nakedSingle
     */
    private void setCell(int cellIndex, int cand, TableEntry entry, boolean getRetIndices, boolean nakedSingle) {
        // find all candidates that are eliminated by the set operation (dont forget
        // the candidates in the cell itself). The reason for the elimination is the
        // ON entry.
        // finder.getCandidates() gets the original candidates (even in a net search)
        tmpSet.set(finder.getCandidates()[cand]);
        tmpSet.remove(cellIndex);
        tmpSet.and(Sudoku2.buddies[cellIndex]);
        int[] cands = sudoku.getAllCandidates(cellIndex);
        // get the house with the smallest number of original candidates (needed for ret indices,
        // but must be done before the cell is set)
        int entityType = Sudoku2.LINE;
        int entityNumberFree = sudoku.getFree()[Sudoku2.CONSTRAINTS[cellIndex][0]][cand];
        int dummy = sudoku.getFree()[Sudoku2.CONSTRAINTS[cellIndex][1]][cand];
        if (dummy < entityNumberFree) {
            entityType = Sudoku2.COL;
            entityNumberFree = dummy;
        }
        dummy = sudoku.getFree()[Sudoku2.CONSTRAINTS[cellIndex][2]][cand];
        if (dummy < entityNumberFree) {
            entityType = Sudoku2.BLOCK;
            entityNumberFree = dummy;
        }
        // now set the cell
        sudoku.setCell(cellIndex, cand);
        int retIndex = entry.index;
        if (getRetIndices) {
            // find the candidate(s) that are responsible for the ON operation
            for (int i = 0; i < retIndices[0].length; i++) {
                retIndices[0][i] = 0;
            }
            if (nakedSingle) {
                // all other candidates in the cell
                int[] cellCands = savedSudoku.getAllCandidates(cellIndex);
                if (cellCands.length > retIndices[0].length + 1) {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Too many candidates (setCell() - Naked Single");
                }
                int ri = 0;
                for (int i = 0; i < cellCands.length && ri < retIndices[0].length; i++) {
                    if (cellCands[i] == cand) {
                        continue;
                    }
                    retIndices[0][ri++] = entry.getEntryIndex(cellIndex, false, cellCands[i]);
                }
            } else {
                // all other candidates in the house with the smallest number of original candidates
                if (entityType == Sudoku2.LINE) {
                    getRetIndicesForHouse(cellIndex, cand, Sudoku2.LINE_TEMPLATES[Sudoku2.getLine(cellIndex)], entry);
                } else if (entityType == Sudoku2.COL) {
                    getRetIndicesForHouse(cellIndex, cand, Sudoku2.COL_TEMPLATES[Sudoku2.getCol(cellIndex)], entry);
                } else {
                    getRetIndicesForHouse(cellIndex, cand, Sudoku2.BLOCK_TEMPLATES[Sudoku2.getBlock(cellIndex)], entry);
                }
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
     * canddiates have to be eliminated before the cell can be set. Used by {@link #setCell(int, int, solver.TableEntry, boolean, boolean)
     * }.
     *
     * @param cellIndex
     * @param cand
     * @param houseSet
     * @param entry
     */
    private void getRetIndicesForHouse(int cellIndex, int cand, SudokuSet houseSet, TableEntry entry) {
        // get all original candidates in the house (cell itself excluded)
        tmpSet1.set(finder.getCandidates()[cand]);
        tmpSet1.remove(cellIndex);
        tmpSet1.and(houseSet);
        if (tmpSet1.size() > retIndices[0].length + 1) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Too many candidates (setCell() - Hidden Single");
        }
        int ri = 0;
        for (int i = 0; i < tmpSet1.size() && ri < retIndices[0].length; i++) {
            retIndices[0][ri++] = entry.getEntryIndex(tmpSet1.get(i), false, cand);
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
     * @param table
     */
    private void expandTables(TableEntry[] table) {
        // for every entry in tables do...
        for (int i = 0; i < table.length; i++) {
//            if (i != 521) {
//                continue;
//            }
            if (table[i].index == 0) {
                // cell is set -> no implications
                continue;
            }
            // table that should be expanded
            TableEntry dest = table[i];

            boolean isFromOnTable = false;
            boolean isFromExtendedTable = false;
            // check every entry except the first (thats the premise)
            for (int j = 1; j < dest.entries.length; j++) {
                if (dest.entries[j] == 0) {
                    // ok -> done
                    break;
                }
                if (dest.isFull()) {
                    // nothing left to do...
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "TableEntry full!");
                    break;
                }
                // table for the current entry -> all entries in src have to be written into dest
                TableEntry src = null;

                // find the table, where the current implication is the premise
                int srcTableIndex = dest.getCellIndex(j) * 10 + dest.getCandidate(j);
                isFromExtendedTable = false;
                isFromOnTable = false;
                if (Chain.getSNodeType(dest.entries[j]) != Chain.NORMAL_NODE) {
                    Integer tmpSI = extendedTableMap.get(dest.entries[j]);
                    if (tmpSI == null) {
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Table for {0} not found!", printTableEntry(dest.entries[j]));
                        continue;
                    }
                    srcTableIndex = tmpSI.intValue();
                    src = extendedTable.get(srcTableIndex);
                    isFromExtendedTable = true;
                } else {
                    if (dest.isStrong(j)) {
                        src = onTable[srcTableIndex];
                    } else {
                        src = offTable[srcTableIndex];
                    }
                    isFromOnTable = dest.isStrong(j);
                }
                if (src.index == 0) {
                    // should not be possible
                    StringBuilder tmpBuffer = new StringBuilder();
                    tmpBuffer.append("TableEntry for ").append(dest.entries[j]).append(" not found!\r\n");
                    tmpBuffer.append("i == ").append(i).append(", j == ").append(j).append(", dest.entries[j] == ").append(dest.entries[j]).append(": ");
                    tmpBuffer.append(printTableEntry(dest.entries[j]));
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, tmpBuffer.toString());
                    continue;
                }
                // ok -> expand it
                int srcBaseDistance = dest.getDistance(j);
                // check all entries from src
                for (int k = 1; k < src.index; k++) {
                    // we take only entries, that have not been expanded themselves
                    if (src.isExpanded(k)) {
                        // ignore it!
                        continue;
                    }
                    int srcDistance = src.getDistance(k);
                    if (dest.indices.containsKey(src.entries[k])) {
                        // entry from src already exists in dest -> check path length
                        int orgIndex = dest.getEntryIndex(src.entries[k]);
                        // 20090213: prefer normal nodes to group nodes or als
//                        if (dest.isExpanded(orgIndex) && dest.getDistance(orgIndex) > (srcBaseDistance + srcDistance)) {
                        if (dest.isExpanded(orgIndex)
                                && (dest.getDistance(orgIndex) > (srcBaseDistance + srcDistance)
                                || dest.getDistance(orgIndex) == (srcBaseDistance + srcDistance)
                                && dest.getNodeType(orgIndex) > src.getNodeType(k))) {
                            // Alter Eintrag war länger oder komplizierter als neuer -> umschreiben
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
                        int srcCellIndex = src.getCellIndex(k);
                        int srcCand = src.getCandidate(k);
                        boolean srcStrong = src.isStrong(k);
                        if (Chain.getSNodeType(src.entries[k]) == Chain.NORMAL_NODE) {
                            dest.addEntry(srcCellIndex, srcCand, srcStrong, srcTableIndex);
                        } else {
                            int tmp = src.entries[k];
                            dest.addEntry(Chain.getSCellIndex(tmp), Chain.getSCellIndex2(tmp), Chain.getSCellIndex3(tmp),
                                    Chain.getSNodeType(tmp), srcCand, srcStrong, srcTableIndex, 0, 0, 0, 0, 0);
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
    }

    /**
     * Convenience method, delegates to {@link #addChain(solver.TableEntry, int, int, boolean, boolean, boolean)
     * }.
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
     * Convenience method, delegates to {@link #addChain(solver.TableEntry, int, int, boolean, boolean, boolean)
     * }.
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
     * Construct the chain for a premise and an implication. Since we have to
     * build the chain from back to start via the retIndices, the chain must be
     * reversed before it can be written into a {@link SolutionStep }.
     *
     * @param entry premise for the chain (first step in the chain)
     * @param cellIndex index of the cell of the implication (last step in the
     * chain)
     * @param cand candidate of the implication
     * @param set last link in chain is strong or weak
     * @param isNiceLoop like
     * <code>isAic</code>, but the first link must leave the cell; the last link
     * may point to the start cell.
     * @param isAic no element in the chain may link to the middleof the chain
     * (but it is allowed for two censecutive links to share the same cell). If
     * the chain is invalid, the method aborts. Links to the start cell are
     * invalid too for AICs.
     */
    private void addChain(TableEntry entry, int cellIndex, int cand, boolean set, boolean isNiceLoop, boolean isAic) {
//        if (cellIndex != 79 || cand != 6 || entry.getCellIndex(0) != 73 || entry.getCandidate(0) != 1) {
//            return;
//        }
        // construct the new chain
        buildChain(entry, cellIndex, cand, set);

        // now check it and add it to the step if plssible
        int j = 0;
        if (isNiceLoop || isAic) {
            lassoSet.clear();
            // for Nice Loops the last chain entry must link to the start cell, but it
            // must not be in the start cell itself (would result in double chains that
            // cannot be detected correctly)
            if (isNiceLoop && Chain.getSCellIndex(chain[0]) == Chain.getSCellIndex(chain[1])) {
                // a shorter version will come soon...
                return;
            }
        }
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
            tmpChain[j++] = oldEntry;
            // "min" stands for "multiple implications" - the chain is a net
            // check for mins
            for (int k = 0; k < actMin; k++) {
                if (mins[k][minIndexes[k] - 1] == oldEntry) {
                    // is a min for the current entry -> add it (the first
                    // entry is skipped, it is already in the chain)
                    for (int l = minIndexes[k] - 2; l >= 0; l--) {
                        tmpChain[j++] = -mins[k][l];
                    }
                    tmpChain[j++] = Integer.MIN_VALUE;
                }
            }
        }
        // do we have a chain?
        if (j > 0) {
//            for (int i = 0; i < j; i++) {
//                tmpChains[tmpChainsIndex].chain[i] = tmpChain[i];
//            }
            // add the new chain(s); tmpChains is reused for every step,
            // this is allowed, since the chains are copied if the globalStep
            // is really added to the steps array
            System.arraycopy(tmpChain, 0, tmpChains[tmpChainsIndex].getChain(), 0, j);
            tmpChains[tmpChainsIndex].setStart(0);
            tmpChains[tmpChainsIndex].setEnd(j - 1);
            tmpChains[tmpChainsIndex].resetLength();
            globalStep.addChain(tmpChains[tmpChainsIndex]);
            tmpChainsIndex++;
        }
    }

    /**
     * Constructs a chain for a given premise and a given implication. It looks
     * up the correct entry in
     * <code>entry</code> and delegates the real work to
     * {@link #buildChain(solver.TableEntry, int, int[], boolean, sudoku.SudokuSet)
     * }. If the chain is a net, the net parts are constructed as well.<br><br>
     *
     * The main chain is written to {@link #chain}, the net parts are written to {@link #mins}.
     * The chain is from back to front, it is reversed by
     * {@link #addChain(solver.TableEntry, int, int, boolean, boolean, boolean)
     * }.
     *
     * @param entry
     * @param cellIndex
     * @param cand
     * @param set
     */
    private void buildChain(TableEntry entry, int cellIndex, int cand, boolean set) {
        // find the entry for the implication in the TableEntry
        chainIndex = 0;
        int chainEntry = Chain.makeSEntry(cellIndex, cand, set);
        int index = -1;
        for (int i = 0; i < entry.entries.length; i++) {
            if (entry.entries[i] == chainEntry) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "No chain entry for {0}/{1}/{2}/{3}", new Object[]{cellIndex, SolutionStep.getCellPrint(cellIndex), cand, set});
            return;
        }
        // reset the data structures for multiples inferences (nets)
        actMin = 0;
        for (int i = 0; i < minIndexes.length; i++) {
            minIndexes[i] = 0;
        }
        // construct the main chain
        tmpSetC.clear();
        chainIndex = buildChain(entry, index, chain, false, tmpSetC);
        // now build the net parts
        int minIndex = 0;
        while (minIndex < actMin) {
            minIndexes[minIndex] = buildChain(entry, entry.getEntryIndex(mins[minIndex][0]), mins[minIndex], true, tmpSetC);
            minIndex++;
        }
    }

    /**
     * <i>Really</i> constructs the chain for a given premise and a given
     * implication :-).<br><br> <ul> <li>Add the implication as first step in
     * the chain</li> <li>retIndex1 points to the entry that caused the
     * implication -&gt; jump to it and handle it next</li> <li>if there are
     * more than 1 retIndices, the first is treated normally; the others are
     * stored in {@link #mins}/{@link #minIndexes], they are
     *  evaluated at a later time</li>
     * </ul>
     * The method returns, when the first entry in
     * <code>entry</code> is reached.<br><br>
     *
     * All cells of the main chain are stored in
     * <code>chainSet</code>. When the method is called for a min (multiple
     * inference - the net part of a chain -
     * <code>isMin</code> is
     * <code>true</code>), the method runs until a cell from the main chain is
     * reached.
     *
     * <b>CAUTION:</b> The chain is stored in
     * <code>actChain</code> in reverse order!
     *
     * @param entry
     * @param entryIndex
     * @param actChain
     * @param isMin
     * @param chainSet
     * @return
     */
    private int buildChain(TableEntry entry, int entryIndex, int[] actChain, boolean isMin, SudokuSet chainSet) {
        int actChainIndex = 0;
        actChain[actChainIndex++] = entry.entries[entryIndex];
        int firstEntryIndex = entryIndex;
        boolean expanded = false;
        TableEntry orgEntry = entry;
        while (firstEntryIndex != 0 && actChainIndex < actChain.length) {
            if (entry.isExpanded(firstEntryIndex)) {
                // current entry comes from a different table -> jump to it!
                if (entry.isExtendedTable(firstEntryIndex)) {
                    entry = extendedTable.get(orgEntry.getRetIndex(firstEntryIndex, 0));
                } else if (entry.isOnTable(firstEntryIndex)) {
                    entry = onTable[orgEntry.getRetIndex(firstEntryIndex, 0)];
                } else {
                    entry = offTable[orgEntry.getRetIndex(firstEntryIndex, 0)];
                }
                expanded = true;
                firstEntryIndex = entry.getEntryIndex(orgEntry.entries[firstEntryIndex]);
            }
            int tmpEntryIndex = firstEntryIndex;
            for (int i = 0; i < 5; i++) {
                entryIndex = entry.getRetIndex(tmpEntryIndex, i);
                if (i == 0) {
                    // the first retIndex points to the next element -> store it
                    // and set it in the chainSet if isMin is false.
                    firstEntryIndex = entryIndex;
                    actChain[actChainIndex++] = entry.entries[entryIndex];
                    if (!isMin) {
                        // record all cells of the main chain
                        chainSet.add(entry.getCellIndex(entryIndex));
                        // group nodes
                        if (Chain.getSNodeType(entry.entries[entryIndex]) == Chain.GROUP_NODE) {
                            int tmp = Chain.getSCellIndex2(entry.entries[entryIndex]);
                            if (tmp != -1) {
                                chainSet.add(tmp);
                            }
                            tmp = Chain.getSCellIndex3(entry.entries[entryIndex]);
                            if (tmp != -1) {
                                chainSet.add(tmp);
                            }
                        } else if (Chain.getSNodeType(entry.entries[entryIndex]) == Chain.ALS_NODE) {
                            if (Chain.getSAlsIndex(entry.entries[entryIndex]) == -1) {
                                Logger.getLogger(getClass().getName()).log(Level.WARNING, "INVALID ALS_NODE: {0}", Chain.toString(entry.entries[entryIndex]));
                            }
                            chainSet.or(alses.get(Chain.getSAlsIndex(entry.entries[entryIndex])).indices);
                        }
                    } else {
                        // if the current chain is a min, check if we have reached the main chain
                        if (chainSet.contains(entry.getCellIndex(entryIndex))) {
                            // preselection: the current cell is part of the main chain -> search the main chain
                            for (int j = 0; j < chainIndex; j++) {
                                if (chain[j] == entry.entries[entryIndex]) {
                                    // done!
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
                        mins[actMin][0] = entry.entries[entryIndex];
                        minIndexes[actMin++] = 1;
                    }
                }
            }
            if (expanded && firstEntryIndex == 0) {
                // we jumped to another TableEntry and have reached its start ->
                // jump back to the original
                int retEntry = entry.entries[0];
                entry = orgEntry;
                firstEntryIndex = entry.getEntryIndex(retEntry);
                expanded = false;
            }
        }
        return actChainIndex;
    }

    /**
     * Show the contents of one {@link TableEntry} (for debugging).
     *
     * @param title
     * @param entry
     */
    private void printTable(String title, TableEntry entry) {
        System.out.println(title + ": ");
        int anz = 0;
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < entry.index; i++) {
            if (!entry.isStrong(i)) {
                //continue;
            }
            tmp.append(printTableEntry(entry.entries[i]));
            for (int j = 0; j < entry.getRetIndexAnz(i); j++) {
                int retIndex = entry.getRetIndex(i, j);
                tmp.append(" (");
                if (entry.isExpanded(i)) {
                    tmp.append("EX:").append(retIndex).append(":").append(entry.isExtendedTable(i)).append("/").append(entry.isOnTable(i)).append("/");
//                    TableEntry actEntry = entry.isOnTable(i) ? onTable[retIndex] : offTable[retIndex];
//                    int index1 = actEntry.getEntryIndex(entry.entries[i]);
//                    // go back one level
//                    for (int k = 0; k < actEntry.getRetIndexAnz(index1); k++) {
//                        int retIndex1 = actEntry.getRetIndex(index1, k);
//                        if (actEntry.isExpanded(index1)) {
//                            tmp.append("EEX/");
//                        }
//                        tmp.append(retIndex1 + "/" + printTableEntry(actEntry.entries[retIndex1]) + ")");
//                    }
                } else {
                    tmp.append(retIndex).append("/").append(printTableEntry(entry.entries[retIndex])).append(")");
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
     * @return
     */
    private String printTableEntry(int entry) {
        int index = Chain.getSCellIndex(entry);
        int candidate = Chain.getSCandidate(entry);
        boolean set = Chain.isSStrong(entry);
        String cell = SolutionStep.getCellPrint(index, false);
        if (Chain.getSNodeType(entry) == Chain.GROUP_NODE) {
            cell = SolutionStep.getCompactCellPrint(index, Chain.getSCellIndex2(entry), Chain.getSCellIndex3(entry));
        } else if (Chain.getSNodeType(entry) == Chain.ALS_NODE) {
            cell = "ALS:" + SolutionStep.getAls(alses.get(Chain.getSAlsIndex(entry)));
        }
        if (set) {
            return cell + "=" + candidate;
        } else {
            return cell + "<>" + candidate;
        }
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
     * <ol> <li>steps that set cells beat steps that delete candidates</li>
     * <li>if both steps set cells:<ul> <li>number of cells that can be set</li>
     * <li>equivalency (same cells?)</li> <li>cells with lower indices go
     * first</li> <li>chain length in all chains</li></ul></li> <li>if both
     * steps eliminate candidates:<ul> <li>number of candidates that can be
     * deleted</li> <li>equivalency (same cells affected?)</li> <li>lower cells
     * and lower candidates first</li> <li>chain length in all
     * chains</li></ul></li> </ol>
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
        sudoku.setSudoku(":0000:x:.......123......6+4+1...4..+8+59+1...+45+2......1+67..2....+1+4....35+64+9+1..14..8.+6.6....+2.+7:::");
        finder.setSudoku(sudoku);
        List<SolutionStep> steps = null;
        long ticks = System.currentTimeMillis();
        int anzLoops = 1;
        for (int i = 0; i < anzLoops; i++) {
            steps = finder.getAllForcingChains(sudoku);
            //steps = ts.getAllForcingNets(sudoku);
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
