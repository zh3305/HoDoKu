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

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import solver.Als;
import solver.RestrictedCommon;

/**
 *
 * @author hobiwan
 */
public class SolutionStep implements Comparable<SolutionStep>, Cloneable, Serializable {

    // TODO Debug
    private static final boolean DEBUG = true;
    private static final String[] entityNames = {
        java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.block"),
        java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.line"),
        java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.col"),
        java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.cell")
    };
    private static final String[] entityShortNames = {"b", "r", "c", ""};
    private static final DecimalFormat FISH_FORMAT = new DecimalFormat("#00");
    private static final long serialVersionUID = 1L;
    private SolutionType type;
    private SolutionType subType; // for kraken fish: holds the underlying fish type
    private int entity;
    private int entityNumber;
    private int entity2;        // für LOCKED_CANDIDATES_X
    private int entity2Number;  // für LOCKED_CANDIDATES_X
    private boolean isSiamese;  // für Siamese Fish
    private int progressScoreSingles = -1; // number of singles that this step unlocks in the sudoku
    private int progressScoreSinglesOnly = -1; // direct unlocked singles
    private int progressScore = -1;  // the resulting score (only no single steps)
    private List<Integer> values = new ArrayList<Integer>();
    private List<Integer> indices = new ArrayList<Integer>();
    private List<Candidate> candidatesToDelete = new ArrayList<Candidate>();
    private List<Candidate> cannibalistic = new ArrayList<Candidate>();
    private List<Candidate> fins = new ArrayList<Candidate>();     // für Finned Fische
    private List<Candidate> endoFins = new ArrayList<Candidate>(); // für Finned Fische
    private List<Entity> baseEntities = new ArrayList<Entity>();   // für Fisch
    private List<Entity> coverEntities = new ArrayList<Entity>();  // für Fisch
    private List<Chain> chains = new ArrayList<Chain>();           // Für alle Arten Chains und Loops
    private List<AlsInSolutionStep> alses = new ArrayList<AlsInSolutionStep>();
    private SortedMap<Integer, Integer> colorCandidates = new TreeMap<Integer, Integer>(); // coloring moves
    private List<RestrictedCommon> restrictedCommons = new ArrayList<RestrictedCommon>(); // ALS Chains
    private SudokuSet potentialCannibalisticEliminations = new SudokuSet(); // for fish only
    private SudokuSet potentialEliminations = new SudokuSet(); // for fish only

    public SolutionStep() {
    }

    /** Creates a new instance of SolutionStep
     * @param type 
     */
    public SolutionStep(SolutionType type) {
        setType(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object clone() {
        SolutionStep newStep = null;
        try {
            newStep = (SolutionStep) super.clone();
            newStep.type = type;
            newStep.entity = entity;
            newStep.entityNumber = entityNumber;
            newStep.entity2 = entity2;
            newStep.entity2Number = entity2Number;
            newStep.isSiamese = isSiamese;
            newStep.progressScoreSingles = progressScoreSingles;
            newStep.progressScoreSinglesOnly = progressScoreSinglesOnly;
            newStep.progressScore = progressScore;
            newStep.values = (List<Integer>) ((ArrayList) values).clone();
            newStep.indices = (List<Integer>) ((ArrayList) indices).clone();
            newStep.candidatesToDelete = (List<Candidate>) ((ArrayList) candidatesToDelete).clone();
            newStep.cannibalistic = (List<Candidate>) ((ArrayList) cannibalistic).clone();
            newStep.fins = (List<Candidate>) ((ArrayList) fins).clone();
            newStep.endoFins = (List<Candidate>) ((ArrayList) endoFins).clone();
            newStep.baseEntities = (List<Entity>) ((ArrayList) baseEntities).clone();
            newStep.coverEntities = (List<Entity>) ((ArrayList) coverEntities).clone();
            newStep.chains = (List<Chain>) ((ArrayList) chains).clone();
            newStep.alses = (List<AlsInSolutionStep>) ((ArrayList) alses).clone();
            newStep.colorCandidates = (SortedMap<Integer, Integer>) ((TreeMap) getColorCandidates()).clone();
            newStep.restrictedCommons = (List<RestrictedCommon>) ((ArrayList) restrictedCommons).clone();
            newStep.potentialCannibalisticEliminations = potentialCannibalisticEliminations.clone();
            newStep.potentialEliminations = potentialEliminations.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error while cloning", ex);
        }

        return newStep;
    }

    public void reset() {
        type = SolutionType.HIDDEN_SINGLE;
        entity = 0;
        entityNumber = 0;
        entity2 = 0;
        entity2Number = 0;
        isSiamese = false;
        progressScoreSingles = -1;
        progressScoreSinglesOnly = -1;
        progressScore = -1;
        values.clear();
        indices.clear();
        candidatesToDelete.clear();
        cannibalistic.clear();
        fins.clear();
        endoFins.clear();
        baseEntities.clear();
        coverEntities.clear();
        chains.clear();
        alses.clear();
        colorCandidates.clear();
        restrictedCommons.clear();
        potentialCannibalisticEliminations.clear();
        potentialEliminations.clear();
    }

    public StringBuffer getForcingChainString(Chain chain) {
//        return getForcingChainString(chain.getChain(), chain.getStart(), chain.getEnd(), false);
        return getForcingChainString(chain.getChain(), chain.getStart(), chain.getEnd(), true);
    }

    /**
     * Chain wird als Forcing Chain ausgegeben. Wenn weakLinks nicht gesetzt ist,
     * werden die weak links übersprungen. Der erste und der letzte Link werden
     * immer ausgegeben (egal ob strong oder weak und auch innerhalb von Klammern)
     * @param chain
     * @param start
     * @param end
     * @param weakLinks
     * @return  
     */
    public StringBuffer getForcingChainString(int[] chain, int start, int end, boolean weakLinks) {
        StringBuffer tmp = new StringBuffer();
        boolean inMin = false;
        appendForcingChainEntry(tmp, chain[start]);
        for (int i = start + 1; i <= end - 1; i++) {
            boolean blank = true;
            if (chain[i] == Integer.MIN_VALUE) {
                tmp.append(")");
                inMin = false;
                continue;
            }
            if (!weakLinks && !Chain.isSStrong(chain[i])
                    && (chain[i] > 0 || chain[i] < 0 && chain[i + 1] < 0 && chain[i + 1] != Integer.MIN_VALUE)) {
                // weak link überspringen, wenn er nicht am Ende eines inMins ist
                // es gibt immer ein Element chain[i + 1], weil die Schleife nur bis zum
                // vorletzten Element geht
                // group nodes, als etc. nicht überspringen (sonst kennt man sich gar nicht mehr aus
                if (Chain.getSNodeType(chain[i]) == Chain.NORMAL_NODE) {
                    continue;
                }
            }
            if (chain[i] < 0 && !inMin) {
                tmp.append(" (");
                inMin = true;
                blank = false;
            }
            if (chain[i] > 0 && inMin) {
                tmp.append(")");
                inMin = false;
            }
            if (blank) {
                tmp.append(" ");
            }
            appendForcingChainEntry(tmp, chain[i]);
        }
        tmp.append(" ");
        appendForcingChainEntry(tmp, chain[end]);
        return tmp;
    }

    public void appendForcingChainEntry(StringBuffer buf, int chainEntry) {
        int entry = chainEntry < 0 ? -chainEntry : chainEntry;
        //buf.append(getCellPrint((entry / 10) % 100, false));
        switch (Chain.getSNodeType(entry)) {
            case Chain.NORMAL_NODE:
                buf.append(getCellPrint(Chain.getSCellIndex(entry), false));
                break;
            case Chain.GROUP_NODE:
                buf.append(getCompactCellPrint(Chain.getSCellIndex(entry), Chain.getSCellIndex2(entry), Chain.getSCellIndex3(entry)));
                break;
            case Chain.ALS_NODE:
//                int alsIndex = Chain.getSCellIndex2(entry);
                int alsIndex = Chain.getSAlsIndex(entry);
                if (alsIndex >= 0 && alsIndex < alses.size()) {
                    buf.append("ALS:");
                    getAls(buf, alsIndex, false);
                } else {
                    buf.append("UNKNOWN ALS");
                    if (DEBUG) {
                        System.out.println("SolutionStep.appendForcingChainEntry: UNKNOWN (" + alsIndex + "/" + alses.size() + ")");
                    }
                }
                break;
        }
        if (!Chain.isSStrong(entry)) {
            buf.append("<>");
        } else {
            buf.append("=");
        }
        buf.append(Chain.getSCandidate(entry));
    }

    public StringBuffer getChainString(Chain chain) {
        return getChainString(chain.getChain(), chain.getStart(), chain.getEnd(), false, true, true, false);
    }

    public StringBuffer getChainString(Chain chain, boolean internalFormat) {
        return getChainString(chain.getChain(), chain.getStart(), chain.getEnd(), true, true, true, internalFormat);
    }

    public StringBuffer getChainString(int[] chain, int start, int end, boolean alternate, boolean up) {
        return getChainString(chain, start, end, alternate, up, true, false);
    }

    public StringBuffer getChainString(int[] chain, int start, int end, boolean alternate, boolean up,
            boolean asNiceLoop, boolean internalFormat) {
        StringBuffer tmp = new StringBuffer();
        boolean isStrong = false;
        int lastIndex = -1;
        if (up) {
            for (int i = start; i <= end; i++) {
                if (internalFormat) {
                    if (i > start) {
                        tmp.append("-");
                    }
                    tmp.append(chain[i]);
                } else {
                    if (i == start + 1) {
                        isStrong = Chain.isSStrong(chain[i]);
                    } else {
                        isStrong = !isStrong;
                    }
                    if (asNiceLoop && Chain.getSCellIndex(chain[i]) == lastIndex) {
                        continue;
                    } else {
                        lastIndex = Chain.getSCellIndex(chain[i]);
                    }
                    if (i > start) {
                        int cand = Chain.getSCandidate(chain[i]);
                        if (!Chain.isSStrong(chain[i]) || (alternate && !isStrong)) {
                            tmp.append(" -");
                            tmp.append(cand);
                            tmp.append("- ");
                        } else {
                            tmp.append(" =");
                            tmp.append(cand);
                            tmp.append("= ");
                        }
                    }
                    switch (Chain.getSNodeType(chain[i])) {
                        case Chain.NORMAL_NODE:
                            tmp.append(getCellPrint(Chain.getSCellIndex(chain[i]), false));
                            break;
                        case Chain.GROUP_NODE:
                            tmp.append(getCompactCellPrint(Chain.getSCellIndex(chain[i]), Chain.getSCellIndex2(chain[i]), Chain.getSCellIndex3(chain[i])));
                            break;
                        case Chain.ALS_NODE:
//                            int alsIndex = Chain.getSCellIndex2(chain[i]);
                            int alsIndex = Chain.getSAlsIndex(chain[i]);
                            if (alsIndex < alses.size()) {
                                tmp.append("ALS:");
                                getAls(tmp, alsIndex, false);
                            } else {
                                tmp.append("UNKNOWN ALS");
                                if (DEBUG) {
                                    System.out.println("SolutionStep.getChainString: UNKNOWN (" + alsIndex + "/" + alses.size() + ")");
                                }
                            }
                            break;
                        default:
                            tmp.append("INV");
                    }
                }
            }
        } else {
            for (int i = end; i >= start; i--) {
                if (internalFormat) {
                    if (i > start) {
                        tmp.append("-");
                    }
                    tmp.append(chain[i]);
                } else {
                    if (i == end - 1) {
                        isStrong = Chain.isSStrong(chain[i + 1]);
                    } else {
                        isStrong = !isStrong;
                    }
                    if (Chain.getSCellIndex(chain[i + 1]) == lastIndex) {
                        continue;
                    } else {
                        lastIndex = Chain.getSCellIndex(chain[i + 1]);
                    }
                    if (i < end) {
                        int cand = Chain.getSCandidate(chain[i]);
                        if (!Chain.isSStrong(chain[i + 1]) || (alternate && !isStrong)) {
                            tmp.append(" -");
                            tmp.append(cand);
                            tmp.append("- ");
                        } else {
                            tmp.append(" =");
                            tmp.append(cand);
                            tmp.append("= ");
                        }
                    }
                    switch (Chain.getSNodeType(chain[i])) {
                        case Chain.NORMAL_NODE:
                            tmp.append(getCellPrint(Chain.getSCellIndex(chain[i]), false));
                            break;
                        case Chain.GROUP_NODE:
                            tmp.append(getCompactCellPrint(Chain.getSCellIndex(chain[i]), Chain.getSCellIndex2(chain[i]), Chain.getSCellIndex3(chain[i])));
                            break;
                        case Chain.ALS_NODE:
//                            int alsIndex = Chain.getSCellIndex2(chain[i]);
                            int alsIndex = Chain.getSAlsIndex(chain[i]);
                            if (alsIndex < alses.size()) {
                                tmp.append("ALS:");
                                getAls(tmp, alsIndex, false);
                            } else {
                                tmp.append("UNKNOWN ALS");
                                if (DEBUG) {
                                    System.out.println("SolutionStep.getChainString: UNKNOWN (" + alsIndex + "/" + alses.size() + ")");
                                }
                            }
                            break;
                    }
                }
            }
        }
        return tmp;
    }

    /**
     * indices and values hold candidates, that should be marked or set; the
     * two lists are not necessarily of the same length. The method has to
     * return all combinations of values and indices.
     *
     * @return A string containing all combinations of values and indices in
     * library format
     */
    public String getValueIndexString() {
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            int value = values.get(i);
            for (int j = 0; j < indices.size(); j++) {
                int index = indices.get(j);
                tmp.append(value);
                tmp.append(Integer.toString(Sudoku2.getLine(index) + 1));
                tmp.append(Integer.toString(Sudoku2.getCol(index) + 1));
                tmp.append(" ");
            }
        }
        return tmp.toString().trim();
    }

    public String getSingleCandidateString() {
        return getStepName() + ": " + getCompactCellPrint(indices) + "=" + values.get(0);
    }

    public String getCandidateString() {
        return getCandidateString(false, false);
    }

    public String getCandidateString(boolean library) {
        return getCandidateString(library, false);
    }

    public String getCandidateString(boolean library, boolean statistics) {
        Collections.sort(candidatesToDelete);
        eliminateDoubleCandidatesToDelete();
        StringBuilder candBuff = new StringBuilder();
        int lastCand = -1;
        StringBuffer delPos = new StringBuffer();
        for (Candidate cand : candidatesToDelete) {
            if (cand.getValue() != lastCand) {
                if (lastCand != -1) {
                    candBuff.append("/");
                }
                candBuff.append(cand.getValue());
                lastCand = cand.getValue();
            }
            delPos.append(" ");
            if (library) {
                delPos.append(Integer.toString(cand.getValue())).append(Integer.toString(Sudoku2.getLine(cand.getIndex()) + 1)).
                        append(Integer.toString(Sudoku2.getCol(cand.getIndex()) + 1));
            }
        }
        if (library) {
            return delPos.toString().trim();
        } else {
            delPos = new StringBuffer();
            getCandidatesToDelete(delPos);
            delPos.delete(0, 4); // " => " entfernen
            if (statistics) {
                //return candBuff.toString() + " (" + candidatesToDelete.size() + ")" + " (0/0)";
                return candBuff.toString() + " (" + getAnzCandidatesToDelete() + ")" + " (0/0)";
            } else {
                String tmpStepName = getStepName();
                if (isSiamese) {
                    tmpStepName = java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.siamese")
                            + " " + getStepName();
                }
                //return candBuff.toString() + " (" + candidatesToDelete.size() + "):" + delPos.toString() + " (" + tmpStepName + ")";
                return candBuff.toString() + " (" + getAnzCandidatesToDelete() + "):" + delPos.toString() + " (" + tmpStepName + ")";
            }
        }
    }

    private void eliminateDoubleCandidatesToDelete() {
        Set<Candidate> candSet = new TreeSet<Candidate>();
        for (int i = 0; i < candidatesToDelete.size(); i++) {
            candSet.add(candidatesToDelete.get(i));
        }
        candidatesToDelete.clear();
        for (Candidate cand : candSet) {
            candidatesToDelete.add(cand);
        }
    }

    public static String getCellPrint(int index) {
        return getCellPrint(index, true);
    }

    public static String getCellPrint(int index, boolean withParen) {
        if (index == -1) {
            return "-1";
        }
        if (withParen) {
            return "[r" + (Sudoku2.getLine(index) + 1) + "c" + (Sudoku2.getCol(index) + 1) + "]";
        } else {
            return "r" + (Sudoku2.getLine(index) + 1) + "c" + (Sudoku2.getCol(index) + 1);
        }
    }

    public static String getCompactCellPrint(int index1, int index2, int index3) {
        TreeSet<Integer> tmpSet = new TreeSet<Integer>();
        tmpSet.add(index1);
        tmpSet.add(index2);
        if (index3 != -1) {
            tmpSet.add(index3);
        }
        String result = getCompactCellPrint(tmpSet);
        return result;
    }

    public static String getCompactCellPrint(SudokuSet set) {
        TreeSet<Integer> tmpSet = new TreeSet<Integer>();
        for (int i = 0; i < set.size(); i++) {
            tmpSet.add(set.get(i));
        }
        String result = getCompactCellPrint(tmpSet);
        return result;
    }

    public static String getCompactCellPrint(List<Integer> indices) {
        return getCompactCellPrint(indices, 0, indices.size() - 1);
    }

    public static String getCompactCellPrint(List<Integer> indices, int start, int end) {
        // Duplikate entfernen!
        TreeSet<Integer> tmpSet = new TreeSet<Integer>();
        for (int i = start; i <= end; i++) {
            tmpSet.add(indices.get(i));
        }
        return getCompactCellPrint(tmpSet);
    }

    public static String getCompactCellPrint(TreeSet<Integer> tmpSet) {
        StringBuilder tmp = new StringBuilder();
        boolean first = true;
        while (tmpSet.size() > 0) {
            int index = tmpSet.pollFirst();
            int line = Sudoku2.getLine(index);
            int col = Sudoku2.getCol(index);
            int anzLines = 1;
            int anzCols = 1;
            if (first) {
                first = false;
            } else {
                tmp.append(",");
            }
            tmp.append(getCellPrint(index));
            Iterator<Integer> it = tmpSet.iterator();
            while (it.hasNext()) {
                int i1 = it.next();
                int l1 = Sudoku2.getLine(i1);
                int c1 = Sudoku2.getCol(i1);
                if (l1 == line && anzLines == 1) {
                    // Spalte hinzufügen
                    int pIndex = tmp.lastIndexOf("]");
                    tmp.insert(pIndex, c1 + 1);
                    it.remove();
                    anzCols++;
                } else if (c1 == col && anzCols == 1) {
                    // Zeile hinzufügen
                    int pIndex = tmp.lastIndexOf("c");
                    tmp.insert(pIndex, l1 + 1);
                    it.remove();
                    anzLines++;
                }
            }
        }
        int index = 0;
        while ((index = tmp.indexOf("[")) != -1) {
            tmp.deleteCharAt(index);
        }
        while ((index = tmp.indexOf("]")) != -1) {
            tmp.deleteCharAt(index);
        }
        return tmp.toString();
    }

    public final void setType(SolutionType type) {
        boolean found = false;
        for (SolutionType t : SolutionType.values()) {
            if (t == type) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new RuntimeException(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.invalid_setType") + " (" + type + ")");
        }
        this.type = type;
    }

    public void addValue(int value) {
        if (value < 1 || value > 9) {
            throw new RuntimeException(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.invalid_setValue") + " (" + value + ")");
        }
        values.add(value);
    }

    public void addIndex(int index) {
        if (index < 0 || index > 80) {
            throw new RuntimeException(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.invalid_setIndex") + " (" + index + ")");
        }
        indices.add(index);
    }

    public void addCandidateToDelete(Candidate cand) {
        candidatesToDelete.add(cand);
    }

    public void addCandidateToDelete(int index, int candidate) {
        candidatesToDelete.add(new Candidate(index, candidate));
    }

    public void addCannibalistic(Candidate cand) {
        cannibalistic.add(cand);
    }

    public void addCannibalistic(int index, int candidate) {
        cannibalistic.add(new Candidate(index, candidate));
    }

    public void addFin(int index, int candidate) {
        addFin(new Candidate(index, candidate));
    }

    public void addFin(Candidate fin) {
        fins.add(fin);
    }

    public void addEndoFin(int index, int candidate) {
        endoFins.add(new Candidate(index, candidate));
    }

    public int getAnzCandidatesToDelete() {
        SortedSet<Candidate> tmpSet = new TreeSet<Candidate>();
        for (int i = 0; i < candidatesToDelete.size(); i++) {
            tmpSet.add(candidatesToDelete.get(i));
        }
        int anz = tmpSet.size();
        tmpSet.clear();
        tmpSet = null;
        return anz;
    }

    public int getAnzSet() {
        if (type.isSingle()) {
            return 1;
        }
        if (type == SolutionType.FORCING_CHAIN || type == SolutionType.FORCING_CHAIN_CONTRADICTION || type == SolutionType.FORCING_CHAIN_VERITY
                || type == SolutionType.FORCING_NET || type == SolutionType.FORCING_NET_CONTRADICTION || type == SolutionType.FORCING_NET_VERITY) {
            if (indices.size() > 0) {
                return 1;
            }
        }
        if (type == SolutionType.TEMPLATE_SET) {
            return indices.size();
        }
        return 0;
    }

    public SolutionType getType() {
        return type;
    }

    public List<Integer> getValues() {
        return values;
    }

    public List<Integer> getIndices() {
        return indices;
    }

    public List<Candidate> getCandidatesToDelete() {
        return candidatesToDelete;
    }

    public List<Candidate> getCannibalistic() {
        return cannibalistic;
    }

    public List<Candidate> getFins() {
        return fins;
    }

    public List<Candidate> getEndoFins() {
        return endoFins;
    }

    public String getStepName() {
        return type.getStepName();
    }

    public static String getStepName(SolutionType type) {
        return type.getStepName();
    }

    public static String getStepName(int type) {
        return SolutionType.values()[type].getStepName();
    }

    public String getEntityName(int name) {
        return entityNames[name];
    }

    public String getEntityShortName(int name) {
        return entityShortNames[name];
    }

    public String getEntityName() {
        return entityNames[entity];
    }

    public String getEntityName2() {
        return entityNames[entity2];
    }

    public String getEntityShortName() {
        return entityShortNames[entity];
    }

    public String getEntityShortNameNumber() {
        if (entity == Sudoku2.CELL) {
            return getCellPrint(entityNumber, false);
        } else {
            return entityShortNames[entity] + Integer.toString(entityNumber + 1);
        }
    }

    public String getEntityShortName2() {
        return entityShortNames[entity2];
    }

    @Override
    public String toString() {
        return toString(2);
    }

    /**
     * art == 0: Kurzform
     * art == 1: Mittellang
     * art == 2: ausführlich
     * @param art
     * @return  
     */
    public String toString(int art) {
        String str = null;
        int index = 0;
        StringBuffer tmp;
        switch (type) {
            case FULL_HOUSE:
            case HIDDEN_SINGLE:
            case NAKED_SINGLE:
                index = indices.get(0);
                str = getStepName();
                if (art == 1) {
                    str += ": " + values.get(0);
                } else if (art == 2) {
                    str += ": " + getCellPrint(index, false) + "=" + values.get(0);
                }
                break;
            case HIDDEN_QUADRUPLE:
            case NAKED_QUADRUPLE:
            case HIDDEN_TRIPLE:
            case NAKED_TRIPLE:
            case LOCKED_TRIPLE:
            case HIDDEN_PAIR:
            case NAKED_PAIR:
            case LOCKED_PAIR:
                index = indices.get(0);
                str = getStepName();
                tmp = new StringBuffer(str);
                if (art >= 1) {
                    tmp.append(": ");
                    if (type == SolutionType.HIDDEN_PAIR || type == SolutionType.NAKED_PAIR || type == SolutionType.LOCKED_PAIR) {
                        tmp.append(values.get(0));
                        tmp.append(",");
                        tmp.append(values.get(1));
                    } else if (type == SolutionType.HIDDEN_TRIPLE || type == SolutionType.NAKED_TRIPLE || type == SolutionType.LOCKED_TRIPLE) {
                        tmp.append(values.get(0));
                        tmp.append(",");
                        tmp.append(values.get(1));
                        tmp.append(",");
                        tmp.append(values.get(2));
                    } else if (type == SolutionType.HIDDEN_QUADRUPLE || type == SolutionType.NAKED_QUADRUPLE) {
                        tmp.append(values.get(0));
                        tmp.append(",");
                        tmp.append(values.get(1));
                        tmp.append(",");
                        tmp.append(values.get(2));
                        tmp.append(",");
                        tmp.append(values.get(3));
                    }
                }
                if (art >= 2) {
                    tmp.append(" ");
                    tmp.append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.in"));
                    tmp.append(" ");
                    tmp.append(getCompactCellPrint(indices));
                    getCandidatesToDelete(tmp);
                }
                str = tmp.toString();
                break;
            case LOCKED_CANDIDATES:
            case LOCKED_CANDIDATES_1:
            case LOCKED_CANDIDATES_2:
                str = getStepName();
                if (art >= 1) {
                    str += ": " + values.get(0);
                }
                if (art >= 2) {
                    str += " "
                            + java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.in")
                            + " " + getEntityShortName() + getEntityNumber();
                    tmp = new StringBuffer(str);
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case SKYSCRAPER:
            case TWO_STRING_KITE:
            case DUAL_TWO_STRING_KITE:
                str = getStepName();
                if (art >= 1) {
                    str += ": " + values.get(0);
                }
                if (art >= 2) {
                    str += " " + java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.in") + " "
                            + getCompactCellPrint(indices, 0, 1);
                    if (type == SolutionType.DUAL_TWO_STRING_KITE) {
                        str += "/" + java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.in") + " "
                                + getCompactCellPrint(indices, 4, 5);
                    }
                    str += " ("
                            + java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.connected_by") + " "
                            + getCompactCellPrint(indices, 2, 3) + ")";
                    tmp = new StringBuffer(str);
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case EMPTY_RECTANGLE:
            case DUAL_EMPTY_RECTANGLE:
                str = getStepName();
                if (art >= 1) {
                    str += ": " + values.get(0);
                }
                if (art >= 2) {
                    str += " " + java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.in") + " " + getEntityShortName() + getEntityNumber()
                            + " (" + getCompactCellPrint(indices, 0, 1);
                    if (type == SolutionType.DUAL_EMPTY_RECTANGLE) {
                        str += "/" + getCompactCellPrint(indices, 2, 3);
                    }
                    str += ")";
                    tmp = new StringBuffer(str);
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case W_WING:
                str = getStepName();
                if (art >= 1) {
                    str += ": " + values.get(0) + "/" + values.get(1);
                }
                if (art >= 2) {
                    tmp = new StringBuffer(str);
                    tmp.append(" ");
                    tmp.append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.in"));
                    tmp.append(" ");
                    tmp.append(getCompactCellPrint(indices, 0, 1));
                    tmp.append(" ");
                    tmp.append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.connected_by"));
                    tmp.append(" ");
                    tmp.append(values.get(1));
                    tmp.append(" ");
                    tmp.append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.in"));
                    tmp.append(" ");
                    getFinSet(tmp, fins, false);
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case XY_WING:
            case XYZ_WING:
                str = getStepName();
                if (art >= 1) {
                    str += ": " + values.get(0) + "/" + values.get(1);
                }
                if (art >= 2) {
                    str += "/" + values.get(2) + " " + java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.in") + " " + getCompactCellPrint(indices);
                    tmp = new StringBuffer(str);
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case SIMPLE_COLORS:
            case SIMPLE_COLORS_TRAP:
            case SIMPLE_COLORS_WRAP:
            case MULTI_COLORS:
            case MULTI_COLORS_1:
            case MULTI_COLORS_2:
                str = getStepName();
                if (art >= 1) {
                    str += ": " + values.get(0);
                }
                if (art >= 2) {
                    tmp = new StringBuffer(str);
                    getColorCellPrint(tmp);
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case X_CHAIN:
            case XY_CHAIN:
            case REMOTE_PAIR:
            case TURBOT_FISH:
            case NICE_LOOP:
            case CONTINUOUS_NICE_LOOP:
            case DISCONTINUOUS_NICE_LOOP:
            case GROUPED_NICE_LOOP:
            case GROUPED_CONTINUOUS_NICE_LOOP:
            case GROUPED_DISCONTINUOUS_NICE_LOOP:
            case AIC:
            case GROUPED_AIC:
                str = getStepName();
                if (art >= 1) {
                    if (type == SolutionType.REMOTE_PAIR) {
                        str += ": " + values.get(0) + "/" + values.get(1);
                    } else {
                        str += ": " + getCandidatesToDeleteDigits();
                    }
//                    if (type == SolutionType.REMOTE_PAIR) {
//                        str += ": " + values.get(0) + "/" + values.get(1);
//                    } else if (type == SolutionType.X_CHAIN || type == SolutionType.XY_CHAIN) {
//                    //} else if (type == SolutionType.X_CHAIN) {
//                        //str += ": " + values.get(0);
//                        str += ": " + candidatesToDelete.get(0).value;
//                    }
                }
                if (art >= 2) {
                    List<Chain> dummy1 = getChains();
                    StringBuffer tmpChain = getChainString(getChains().get(0));
                    // adjust nice loop notation
                    if (type == SolutionType.CONTINUOUS_NICE_LOOP || type == SolutionType.GROUPED_CONTINUOUS_NICE_LOOP) {
                        Chain ch = getChains().get(0);
                        int start = ch.getStart();
                        int cellIndex = ch.getCellIndex(start);
                        while (ch.getCellIndex(start) == cellIndex) {
                            start++;
                        }
                        int end = ch.getEnd();
                        cellIndex = ch.getCellIndex(end);
                        while (ch.getCellIndex(end) == cellIndex) {
                            end--;
                        }
                        end++;
                        tmpChain.insert(0, ch.getCandidate(end) + "= ");
                        tmpChain.append(" =").append(ch.getCandidate(start));
                        //System.out.println(Chain.toString(ch.chain[start]) + "/" + Chain.toString(ch.chain[ch.end]));
                    }
                    if (type == SolutionType.AIC || type == SolutionType.GROUPED_AIC || type == SolutionType.XY_CHAIN) {
                        Chain ch = getChains().get(0);
                        //System.out.println(Chain.toString(ch.chain[ch.start]) + "/" + Chain.toString(ch.chain[ch.end]));
                        tmpChain.insert(0, ch.getCandidate(ch.getStart()) + "- ");
                        tmpChain.append(" -").append(ch.getCandidate(ch.getEnd()));
                    }
                    //str += " " + getChainString(getChains().get(0));
                    str += " " + tmpChain;
                    tmp = new StringBuffer(str);
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case FORCING_CHAIN:
            case FORCING_CHAIN_CONTRADICTION:
            case FORCING_CHAIN_VERITY:
            case FORCING_NET:
            case FORCING_NET_CONTRADICTION:
            case FORCING_NET_VERITY:
                str = getStepName();
                if (art >= 1) {
                    // Keine dezenten Hinweise bei Forcing Chains...
                }
                if (art >= 2) {
                    if (type == SolutionType.FORCING_CHAIN_CONTRADICTION
                            || type == SolutionType.FORCING_NET_CONTRADICTION) {
                        str += " " + java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.in") + " " + getEntityShortNameNumber();
                    } else {
                        //str += " Verity";
                    }
                    if (indices.size() > 0) {
                        str += " => " + getCellPrint(indices.get(0), false) + "=" + values.get(0);
                    } else {
                        tmp = new StringBuffer(str);
                        getCandidatesToDelete(tmp);
                        str = tmp.toString();
                    }
                    for (int i = 0; i < chains.size(); i++) {
                        str += "\r\n  " + getForcingChainString(getChains().get(i));
                    }
                }
                break;
            case UNIQUENESS_1:
            case UNIQUENESS_2:
            case UNIQUENESS_3:
            case UNIQUENESS_4:
            case UNIQUENESS_5:
            case UNIQUENESS_6:
            case HIDDEN_RECTANGLE:
            case AVOIDABLE_RECTANGLE_1:
            case AVOIDABLE_RECTANGLE_2:
                str = getStepName();
                if (art >= 1) {
                    str += ": " + values.get(0) + "/" + values.get(1);
                }
                if (art >= 2) {
                    str += " in " + getCompactCellPrint(indices);
                    tmp = new StringBuffer(str);
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case BUG_PLUS_1:
                str = getStepName();
                if (art >= 2) {
                    tmp = new StringBuffer(str);
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case X_WING:
            case SWORDFISH:
            case JELLYFISH:
            case SQUIRMBAG:
            case WHALE:
            case LEVIATHAN:
            case FINNED_X_WING:
            case FINNED_SWORDFISH:
            case FINNED_JELLYFISH:
            case FINNED_SQUIRMBAG:
            case FINNED_WHALE:
            case FINNED_LEVIATHAN:
            case SASHIMI_X_WING:
            case SASHIMI_SWORDFISH:
            case SASHIMI_JELLYFISH:
            case SASHIMI_SQUIRMBAG:
            case SASHIMI_WHALE:
            case SASHIMI_LEVIATHAN:
            case FRANKEN_X_WING:
            case FRANKEN_SWORDFISH:
            case FRANKEN_JELLYFISH:
            case FRANKEN_SQUIRMBAG:
            case FRANKEN_WHALE:
            case FRANKEN_LEVIATHAN:
            case FINNED_FRANKEN_X_WING:
            case FINNED_FRANKEN_SWORDFISH:
            case FINNED_FRANKEN_JELLYFISH:
            case FINNED_FRANKEN_SQUIRMBAG:
            case FINNED_FRANKEN_WHALE:
            case FINNED_FRANKEN_LEVIATHAN:
            case MUTANT_X_WING:
            case MUTANT_SWORDFISH:
            case MUTANT_JELLYFISH:
            case MUTANT_SQUIRMBAG:
            case MUTANT_WHALE:
            case MUTANT_LEVIATHAN:
            case FINNED_MUTANT_X_WING:
            case FINNED_MUTANT_SWORDFISH:
            case FINNED_MUTANT_JELLYFISH:
            case FINNED_MUTANT_SQUIRMBAG:
            case FINNED_MUTANT_WHALE:
            case FINNED_MUTANT_LEVIATHAN:
            case KRAKEN_FISH:
            case KRAKEN_FISH_TYPE_1:
            case KRAKEN_FISH_TYPE_2:
                tmp = new StringBuffer();
                if (isSiamese) {
                    tmp.append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.siamese")).append(" ");
                }
                tmp.append(getStepName());
                if (art >= 1) {
                    if (type.isKrakenFish()) {
                        tmp.append(": ");
                        getCandidatesToDelete(tmp);
                        tmp.append("\r\n  ").append(subType.getStepName());
                    }
                    tmp.append(": ").append(values.get(0));
                }
                if (art >= 2) {
                    tmp.append(" ");
                    getEntities(tmp, baseEntities, true, false);
                    tmp.append(" ");
                    getEntities(tmp, coverEntities, true, true);
                    //tmp.append(" Positionen: ");
                    int displayMode = Options.getInstance().getFishDisplayMode();
                    if (type.isKrakenFish()) {
                        // no statistics
                        displayMode = 0;
                    }
                    switch (displayMode) {
                        case 0:
                            if (fins.size() > 0) {
                                tmp.append(" ");
                                getFins(tmp, false, true);
                            }
                            if (endoFins.size() > 0) {
                                tmp.append(" ");
                                getFins(tmp, true, true);
                            }
                            break;
                        case 1:
                            getFishStatistics(tmp, false);
                            break;
                        case 2:
                            getFishStatistics(tmp, true);
                            break;
                    }
                    if (!type.isKrakenFish()) {
                        getCandidatesToDelete(tmp);
                    }
                }
                if (type.isKrakenFish()) {
                    for (int i = 0; i < chains.size(); i++) {
                        tmp.append("\r\n  ").append(getChainString(chains.get(i)));
                    }
                }
                str = tmp.toString();
                break;
            case SUE_DE_COQ:
                str = getStepName();
                tmp = new StringBuffer(str + ": ");
                if (art >= 1) {
                    getIndexValueSet(tmp);
                    str = tmp.toString();
                }
                if (art >= 2) {
                    tmp.append(" (");
                    getFinSet(tmp, fins);
                    tmp.append(", ");
                    getFinSet(tmp, endoFins);
                    tmp.append(")");
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case ALS_XZ:
                // Sets A und B stecken in AlsInSolutionStep, X ist eine 2-Elemente lange Chain, alle Z stecken in fins
                str = getStepName();
                tmp = new StringBuffer(str + ": ");
                if (art >= 1) {
                    tmp.append("A=");
                    getAls(tmp, 0);
                    str = tmp.toString();
                }
                if (art >= 2) {
                    tmp.append(", B=");
                    getAls(tmp, 1);
                    tmp.append(", X=");
                    getAlsXorZ(tmp, true);
                    if (!fins.isEmpty()) {
                        tmp.append(", Z=");
                        getAlsXorZ(tmp, false);
                    }
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case ALS_XY_WING:
                // Sets A, B und C stecken in AlsInSolutionStep, alle Y und Z stecken in endoFins, alle X stecken in fins
                str = getStepName();
                if (art == 1) {
                    tmp = new StringBuffer(str + ": ");
                    tmp.append("C=");
                    getAls(tmp, 2);
                    str = tmp.toString();
                }
                if (art >= 2) {
                    tmp = new StringBuffer(str + ": ");
                    tmp.append("A=");
                    getAls(tmp, 0);
                    tmp.append(", B=");
                    getAls(tmp, 1);
                    tmp.append(", C=");
                    getAls(tmp, 2);
                    tmp.append(", X,Y=");
                    getAlsXorZ(tmp, true);
                    tmp.append(", Z=");
                    getAlsXorZ(tmp, false);
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case ALS_XY_CHAIN:
                str = getStepName();
                if (restrictedCommons.isEmpty()) {
                    // old code -> has to remain for correctly displaying saved files
                    if (art == 1) {
                        tmp = new StringBuffer(str + ": ");
                        tmp.append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.start")).append("=");
                        getAls(tmp, 0);
                        tmp.append(", ").append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.end")).append("=");
                        getAls(tmp, alses.size() - 1);
                        str = tmp.toString();
                    }
                    if (art >= 2) {
                        tmp = new StringBuffer(str + ": ");
                        char alsChar = 'A';
                        boolean first = true;
                        for (int i = 0; i < alses.size(); i++) {
                            if (first) {
                                first = false;
                            } else {
                                tmp.append(", ");
                            }
                            tmp.append(alsChar++);
                            tmp.append("=");
                            getAls(tmp, i);
                        }
                        tmp.append(", RCs=");
                        getAlsXorZ(tmp, true);
                        tmp.append(", X=");
                        getAlsXorZ(tmp, false);
                        getCandidatesToDelete(tmp);
                        str = tmp.toString();
                    }
                } else {
                    if (art == 1) {
                        tmp = new StringBuffer(str + ": ");
                        tmp.append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.start")).append("=");
                        getAls(tmp, 0);
                        tmp.append(", ").append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.end")).append("=");
                        getAls(tmp, alses.size() - 1);
                        str = tmp.toString();
                    }
                    if (art >= 2) {
                        tmp = new StringBuffer(str + ": ");
                        getCandidatesToDeleteDigits(tmp);
                        tmp.append("- ");
                        for (int i = 0; i < alses.size(); i++) {
                            getAls(tmp, i);
                            if (i < restrictedCommons.size()) {
                                getRestrictedCommon(restrictedCommons.get(i), tmp);
                            }
                        }
                        tmp.append(" -");
                        getCandidatesToDeleteDigits(tmp);
                        getCandidatesToDelete(tmp);
                        str = tmp.toString();
                    }
                }
                break;
            case DEATH_BLOSSOM:
                str = getStepName();
                tmp = new StringBuffer(str + ": ");
                if (art >= 1) {
                    tmp.append(getCellPrint(indices.get(0)));
                    str = tmp.toString();
                }
                if (art >= 2) {
                    for (int i = 0; i < alses.size(); i++) {
                        tmp.append(", ");
                        getRestrictedCommon(restrictedCommons.get(i), tmp);
                        getAls(tmp, i);
                    }
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case TEMPLATE_SET:
                str = getStepName();
                if (art == 1) {
                    str += ": " + values.get(0);
                }
                if (art >= 2) {
                    tmp = new StringBuffer(str + ": ");
                    tmp.append(getCompactCellPrint(indices)).append("=").append(values.get(0));
                    str = tmp.toString();
                }
                break;
            case TEMPLATE_DEL:
                str = getStepName();
                if (art >= 1) {
                    // nichts zusätzlich ausgeben
                }
                if (art >= 2) {
                    tmp = new StringBuffer(str + ": ");
                    getCandidatesToDelete(tmp);
                    str = tmp.toString();
                }
                break;
            case BRUTE_FORCE:
                str = getStepName();
                if (art == 1) {
                    str += ": " + values.get(0);
                }
                if (art >= 2) {
                    tmp = new StringBuffer(str + ": ");
                    tmp.append(getCompactCellPrint(indices)).append("=").append(values.get(0));
                    str = tmp.toString();
                }
                break;
            case INCOMPLETE:
                str = java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.incomplete_solution");
                break;
            case GIVE_UP:
                tmp = new StringBuffer();
                tmp.append(getStepName());
                if (art >= 1) {
                    tmp.append(": ").append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.dont_know"));
                }
                str = tmp.toString();
                break;
            default:
                throw new RuntimeException(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.invalid_type") + " (" + type + ")!");
        }
        return str;
    }

    /**
     * Gets information about vertices, fins, eliminations...
     * @param tmp
     * @param cells
     */
    private void getFishStatistics(StringBuffer tmp, boolean cells) {
        tmp.append(" ");
        SudokuSet set = new SudokuSet();
        // Vertices: all indices minus potential cannibalistic eliminations
        for (int i = 0; i < indices.size(); i++) {
            set.add(indices.get(i));
        }
        set.andNot(potentialCannibalisticEliminations);
        appendFishData(tmp, set, "V", cells);
        // exo fins
        set.clear();
        for (int i = 0; i < fins.size(); i++) {
            set.add(fins.get(i).getIndex());
        }
        for (int i = 0; i < endoFins.size(); i++) {
            set.remove(endoFins.get(i).getIndex());
        }
        appendFishData(tmp, set, "XF", cells);
        // endo fins
        set.clear();
        for (int i = 0; i < endoFins.size(); i++) {
            set.add(endoFins.get(i).getIndex());
        }
        appendFishData(tmp, set, "NF", cells);
        // eventual eliminations
        set.clear();
        for (int i = 0; i < candidatesToDelete.size(); i++) {
            set.add(candidatesToDelete.get(i).getIndex());
        }
        appendFishData(tmp, set, "EE", cells);
        // cannibalistic eventual eliminations
        set.clear();
        for (int i = 0; i < cannibalistic.size(); i++) {
            set.add(cannibalistic.get(i).getIndex());
        }
        appendFishData(tmp, set, "CE", cells);
        // potential eliminations
        set.set(potentialEliminations);
        set.or(potentialCannibalisticEliminations);
        appendFishData(tmp, set, "PE", cells);
    }

    private void appendFishData(StringBuffer tmp, SudokuSet set, String prefix, boolean cells) {
        tmp.append(prefix);
        tmp.append("(");
        if (cells) {
            tmp.append(getCompactCellPrint(set));
        } else {
            tmp.append(FISH_FORMAT.format(set.size()));
        }
        tmp.append(") ");
    }

    private void getColorCellPrint(StringBuffer tmp) {
        tmp.append(" ");
        StringBuffer[] bufs = new StringBuffer[Options.getInstance().getColoringColors().length];
        for (int index : getColorCandidates().keySet()) {
            int color = getColorCandidates().get(index);
            if (bufs[color] == null) {
                bufs[color] = new StringBuffer();
                bufs[color].append("(");
            } else {
                bufs[color].append(",");
            }
            bufs[color].append(getCellPrint(index, false));
        }
        for (int i = 0; i < bufs.length; i++) {
            if (bufs[i] != null) {
                bufs[i].append(")");
                if ((i % 2) != 0) {
                    tmp.append(" / ");
                } else if (i > 0) {
                    tmp.append(", ");
                }
                tmp.append(bufs[i]);
            }
        }
    }

    private void getAlsXorZ(StringBuffer tmp, boolean x) {
        // gemeinsame Kandidaten für AlsInSolutionStep-XZ stehen in fins,
        // restricted commons in endoFins
        List<Candidate> list = x ? endoFins : fins;
        TreeSet<Integer> cands = new TreeSet<Integer>();
        for (int i = 0; i < list.size(); i++) {
            cands.add(list.get(i).getValue());
        }
        boolean first = true;
        for (int cand : cands) {
            if (first) {
                first = false;
            } else {
                tmp.append(",");
            }
            tmp.append(cand);
        }
    }

    public static String getAls(Als als) {
        return getAls(als, true);
    }

    public static String getAls(Als als, boolean withCandidates) {
        StringBuilder tmp = new StringBuilder();
        TreeSet<Integer> set = new TreeSet<Integer>();
        for (int i = 0; i < als.indices.size(); i++) {
            set.add(als.indices.get(i));
        }
        tmp.append(getCompactCellPrint(set));
        if (withCandidates) {
            //tmp.append(" - {");
            tmp.append(" {");
            int[] cands = Sudoku2.POSSIBLE_VALUES[als.candidates];
            for (int i = 0; i < cands.length; i++) {
                tmp.append(cands[i]);
            }
//            for (int i = 0; i < als.candidates.size(); i++) {
//                tmp.append(als.candidates.get(i));
//            }
            tmp.append("}");
        }
        return tmp.toString();
    }

    public void getAls(StringBuffer tmp, int alsIndex) {
        getAls(tmp, alsIndex, true);
    }

    public void getAls(StringBuffer tmp, int alsIndex, boolean withCandidates) {
        AlsInSolutionStep als = alses.get(alsIndex);
        tmp.append(getCompactCellPrint(als.getIndices()));
        if (withCandidates) {
            //tmp.append(" - {");
            tmp.append(" {");
            for (Integer cand : als.getCandidates()) {
                tmp.append(cand);
            }
            tmp.append("}");
        }
    }

    private void getIndexValueSet(StringBuffer tmp) {
        tmp.append(getCompactCellPrint(indices));
        tmp.append(" - {");
        for (Integer value : values) {
            tmp.append(value);
        }
        tmp.append("}");
    }

    /**
     * Ein Eintrag pro betroffener Zelle und pro betroffenem Kandidaten -> beinhart Set verwenden!
     */
    private void getFinSet(StringBuffer tmp, List<Candidate> fins) {
        getFinSet(tmp, fins, true);
    }

    private void getFinSet(StringBuffer tmp, List<Candidate> fins, boolean withCandidates) {
        TreeSet<Integer> indexes = new TreeSet<Integer>();
        TreeSet<Integer> candidates = new TreeSet<Integer>();
        for (Candidate cand : fins) {
            indexes.add(cand.getIndex());
            candidates.add(cand.getValue());
        }
        // Alle indexe ausschließen, die in indices enthalten sind
        for (int index : indices) {
            indexes.remove(index);
        }
        tmp.append(getCompactCellPrint(indexes));
        if (withCandidates) {
            tmp.append(" - {");
            for (int value : candidates) {
                tmp.append(value);
            }
            tmp.append("}");
        }
    }

    public void getEntities(StringBuffer tmp, List<Entity> entities) {
        getEntities(tmp, entities, false);
    }

    public void getEntities(StringBuffer tmp, List<Entity> entities, boolean library) {
        getEntities(tmp, entities, library, false);
    }

    public void getEntities(StringBuffer tmp, List<Entity> entities, boolean library, boolean checkSiamese) {
        boolean first = true;
        if (!library) {
            tmp.append("(");
        }
        int siameseIndex = entities.size() / 2 - 1;
        int lastEntityName = -1;
        int index = 0;
        for (Entity act : entities) {
            if (first) {
                first = false;
            } else {
                if (!library) {
                    tmp.append(", ");
                }
            }
            if (library) {
                if (lastEntityName != act.getEntityName()) {
                    tmp.append(getEntityShortName(act.getEntityName()));
                }
                tmp.append(act.getEntityNumber());
            } else {
                tmp.append(getEntityName(act.getEntityName())).append(" ").append(act.getEntityNumber());
            }
            lastEntityName = act.getEntityName();
            if (checkSiamese && isSiamese && index == siameseIndex) {
                tmp.append("/");
                lastEntityName = -1;
            }
            index++;
        }
        if (!library) {
            tmp.append(")");
        }
    }

    private void getIndexes(StringBuffer tmp) {
        boolean first = true;
        for (int index : indices) {
            if (first) {
                first = false;
            } else {
                tmp.append(", ");
            }
            tmp.append(getCellPrint(index, false));
        }
    }

    /**
     * Calculates the String representation of an RC: -ARC-
     * ARC are the actual RCs depending on the value of actualRC.
     * 
     * @param rc The Restricted Common to be displayed
     * @param tmp Result is appended to tmp
     */
    private void getRestrictedCommon(RestrictedCommon rc, StringBuffer tmp) {
        int anz = 0;
        tmp.append(" -");
        if (rc.getActualRC() == 1 || rc.getActualRC() == 3) {
            tmp.append(rc.getCand1());
            anz++;
        }
        if (rc.getActualRC() == 2 || rc.getActualRC() == 3) {
            tmp.append(rc.getCand2());
            anz++;
        }
//        if (rc.cand2 != 0 && anz < 2) {
//            tmp.append("(!!!");
//            tmp.append(rc.cand1);
//            tmp.append(rc.cand2);
//            tmp.append("!!!)");
//        }
        tmp.append("- ");
    }

    /**
     * Returns all candidates that are deleted in this. Is needed for
     * displaying ALS Chains (chain should start and end with the
     * deleted candidates (no indices").
     * 
     * @param tmp Result is appended to tmp
     */
    private void getCandidatesToDeleteDigits(StringBuffer tmp) {
        SortedSet<Integer> candSet = new TreeSet<Integer>();
        for (int i = 0; i < candidatesToDelete.size(); i++) {
            candSet.add(candidatesToDelete.get(i).getValue());
        }
        for (int value : candSet) {
            tmp.append(value);
        }
    }

    /**
     * Similar to {@link #getCandidatesToDeleteDigits(java.lang.StringBuffer) },
     * but inserts slashes between the digits
     */
    private String getCandidatesToDeleteDigits() {
        StringBuffer tmp = new StringBuffer();
        getCandidatesToDeleteDigits(tmp);
        int compactLength = tmp.length();
        for (int i = 0; i < compactLength - 1; i++) {
            tmp.insert(i * 2 + 1, "/");
        }
        return tmp.toString();
    }

    private void getCandidatesToDelete(StringBuffer tmp) {
        tmp.append(" => ");
        @SuppressWarnings("unchecked")
        ArrayList<Candidate> tmpList = (ArrayList<Candidate>) ((ArrayList<Candidate>) candidatesToDelete).clone();
        boolean first = true;
        ArrayList<Integer> candList = new ArrayList<Integer>();
        while (tmpList.size() > 0) {
            Candidate firstCand = tmpList.remove(0);
            candList.clear();
            candList.add(firstCand.getIndex());
            Iterator<Candidate> it = tmpList.iterator();
            while (it.hasNext()) {
                Candidate c1 = it.next();
                if (c1.getValue() == firstCand.getValue()) {
                    candList.add(c1.getIndex());
                    it.remove();
                }
            }
            if (first) {
                first = false;
            } else {
                tmp.append(", ");
            }
            tmp.append(getCompactCellPrint(candList));
            tmp.append("<>");
            tmp.append(firstCand.getValue());
        }
    }

    public void getFins(StringBuffer tmp, boolean endo) {
        getFins(tmp, endo, false);
    }

    public void getFins(StringBuffer tmp, boolean endo, boolean library) {
        List<Candidate> list = endo ? endoFins : fins;
        if (list.isEmpty()) {
            return;
        }
        if (!library) {
            if (list.size() == 1) {
                if (endo) {
                    tmp.append(" ").append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.endofin_in")).append(" ");
                } else {
                    tmp.append(" ").append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.fin_in")).append(" ");
                }
            } else {
                if (endo) {
                    tmp.append(" ").append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.endofins_in")).append(" ");
                } else {
                    tmp.append(" ").append(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.fins_in")).append(" ");
                }
            }
        }
        String finStr = endo ? "ef" : "f";
        boolean first = true;
        for (Candidate cand : list) {
            if (first) {
                first = false;
            } else {
                if (library) {
                    tmp.append(" ");
                } else {
                    tmp.append(", ");
                }
            }
            if (library) {
                tmp.append(finStr).append(getCellPrint(cand.getIndex(), false));
            } else {
                tmp.append(getCellPrint(cand.getIndex(), false));
            }
        }
    }

    public int getEntity() {
        return entity;
    }

    public void setEntity(int entity) {
        if (entity != Sudoku2.BLOCK && entity != Sudoku2.LINE && entity != Sudoku2.COL && entity != Sudoku2.CELL) {
            throw new RuntimeException(java.util.ResourceBundle.getBundle("intl/SolutionStep").getString("SolutionStep.invalid_setEntity") + " (" + entity + java.util.ResourceBundle.getBundle("intl/SolutionStep").getString(")"));
        }
        this.entity = entity;
    }

    public int getEntityNumber() {
        return entityNumber;
    }

    public void setEntityNumber(int entityNumber) {
        this.entityNumber = entityNumber;
    }

    public int getEntity2() {
        return entity2;
    }

    public void setEntity2(int entity2) {
        this.entity2 = entity2;
    }

    public int getEntity2Number() {
        return entity2Number;
    }

    public void setEntity2Number(int entity2Number) {
        this.entity2Number = entity2Number;
    }

    public void addBaseEntity(int name, int number) {
        baseEntities.add(new Entity(name, number));
    }

    public void addBaseEntity(Entity e) {
        baseEntities.add(e);
    }

    public void addCoverEntity(int name, int number) {
        coverEntities.add(new Entity(name, number));
    }

    public void addCoverEntity(Entity e) {
        coverEntities.add(e);
    }

    public void addChain(int start, int end, int[] chain) {
        chains.add(new Chain(start, end, chain));
    }

    public void addChain(Chain chain) {
        chain.resetLength();
        chains.add(chain);
    }

    public List<Chain> getChains() {
        return chains;
    }

//    public boolean containsChain(int start, int end, int[] chain) {
//        int i = 0, j = 0;
//        for (int m = 0; m < chains.size(); m++) {
//            Chain akt = chains.get(m);
//            // chains können nur gleich sein, wenn sie gleich lang sind
//            if (akt.end - akt.start != end - start) {
//                continue;
//            }
//
//            // einmal hin...
//            for (i = akt.start, j =
//                            start;
//                    j <= end; i++, j++) {
//                if (akt.chain[i] != chain[j]) {
//                    break;
//                }
//            }
//            if (j == end + 1) {
//                return true;
//            }
//            // und einmal her...
//            for (i = akt.start, j =
//                            end;
//                    j >= start; i++, j--) {
//                // die Zellen und Kandidaten müssen gleich sein
//                if (!Chain.equalsIndexCandidate(akt.chain[i], chain[j])) {
//                    break;
//                }
//                // um strong oder weak kümmere ich mich einmal nicht...
//            }
//            if (j == start - 1) {
//                return true;
//            }
//        }
//        return false;
//    }
    public int getChainLength() {
        int length = 0;
        for (int i = 0; i < chains.size(); i++) {
            //length += (chains.get(i).end + 1);
            length += chains.get(i).getLength(alses);
        }
        return length;
    }

    public int getChainAnz() {
        return chains.size();
    }

    public boolean isNet() {
        if (chains.size() > 0) {
            for (int i = 0; i < chains.size(); i++) {
                Chain tmp = chains.get(i);
                for (int j = tmp.getStart(); j <= tmp.getEnd(); j++) {
                    if (tmp.getChain()[j] < 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int getAlsesIndexCount() {
        int count = 0;
        for (AlsInSolutionStep als : alses) {
            count += als.getIndices().size();
        }
        return count;
    }

    public List<AlsInSolutionStep> getAlses() {
        return alses;
    }

    public AlsInSolutionStep getAls(int index) {
        return alses.get(index);
    }

    public void addAls(AlsInSolutionStep newAls) {
        alses.add(newAls);
    }

    public void addAls(SudokuSet indices, SudokuSet candidates) {
        AlsInSolutionStep als = new AlsInSolutionStep();
        for (int i = 0; i < indices.size(); i++) {
            als.addIndex(indices.get(i));
        }
        for (int i = 0; i < candidates.size(); i++) {
            als.addCandidate(candidates.get(i));
        }
        alses.add(als);
    }

    public void addAls(SudokuSet indices, short candidates) {
        AlsInSolutionStep als = new AlsInSolutionStep();
        for (int i = 0; i < indices.size(); i++) {
            als.addIndex(indices.get(i));
        }
        int[] cands = Sudoku2.POSSIBLE_VALUES[candidates];
        for (int i = 0; i < cands.length; i++) {
            als.addCandidate(cands[i]);
        }
        alses.add(als);
    }

    public void addRestrictedCommon(RestrictedCommon rc) {
        restrictedCommons.add(rc);
    }

    /**
     * Prüft, ob Index index in einem AlsInSolutionStep enthalten ist. Wenn ja, wird der
     * index in alses zurückgegeben, sonst -1;
     * 
     * Doesnt work: if a step has more than one chain, a cell can be part of more than one
     * ALS; index has to be checked against a chain (only when a certain chain is requested,
     * which means if chainIndex != -1)!
     * @param index
     * @param chainIndex
     * @return  
     */
    public int getAlsIndex(int index, int chainIndex) {
        if (chainIndex == -1) {
            for (int i = 0; i < alses.size(); i++) {
                if (alses.get(i).getIndices().contains(index)) {
                    return i;
                }
            }
        } else {
            Chain chain = chains.get(chainIndex);
            for (int i = chain.getStart(); i <= chain.getEnd(); i++) {
                if (chain.getNodeType(i) == Chain.ALS_NODE) {
                    int alsIndex = Chain.getSAlsIndex(chain.getChain()[i]);
                    AlsInSolutionStep als = alses.get(alsIndex);
                    if (als.getIndices().contains(index)) {
                        return alsIndex;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Adds a new colored candidate
     * @param index
     * @param color  
     */
    public void addColorCandidate(int index, int color) {
        getColorCandidates().put(index, color);
    }

    public void addColorCandidates(SudokuSet indices, int color) {
        for (int i = 0; i < indices.size(); i++) {
            addColorCandidate(indices.get(i), color);
        }
    }

    /**
     * Zwei Steps sind gleich, wenn sie die gleichen zu löschenden Kandidaten
     * bewirken und wenn alle betroffenen Kandidaten (inkl. Fins) gleich sind.
     *
     * Es wurde absichtlich nicht equals() überschrieben, weil es ein ganz anderer
     * Gleichheitsbegriff ist.
     * @param s
     * @return  
     */
    public boolean isEqual(SolutionStep s) {
        if (!isEquivalent(s)) {
            return false;
        }

        // ok: gleiche zu löschende Kandidaten -> weiter schauen
        if (!isEqualInteger(values, s.values)) {
            return false;
        }
        if (!isEqualInteger(indices, s.indices)) {
            return false;
        }
        if (!isEqualCandidate(fins, s.fins)) {
            return false;
        }

        return true;
    }

    /**
     * Zwei Steps sind äquivalent, wenn sie die gleichen zu löschenden
     * Kandidaten bewirken (oder die gleichen Kandidaten setzen).
     * 
     * 20081013: Problems with AllStepsPanel, so new try:
     *    two steps cannot be equal, if they have not the same SolutionType
     *    Exception: both steps are fish
     * 20120112: All steps handled specially in compareTo() are
     *    to be treated as equivalent!
     * @param s
     * @return  
     */
    public boolean isEquivalent(SolutionStep s) {
        // Special steps first:
//        if (getType() != s.getType() && (!SolutionType.isFish(getType()) ||
//                !SolutionType.isFish(s.getType()))) {
//            return false;
//        }
        if (type.isFish() && s.getType().isFish()) {
            return true;
        }
        if (type.isKrakenFish() && s.getType().isKrakenFish()) {
            return true;
        }
        if (getType() != s.getType()) {
            return false;
        }

        if (candidatesToDelete.size() > 0) {
            return isEqualCandidate(candidatesToDelete, s.candidatesToDelete);
        }
        return isEqualInteger(indices, s.indices);
    }

    /**
     * Der aktuelle Step ist eun Substep des übergebenen Steps, wenn alle
     * zu löschenden Kandidaten auch im übergebenen Step enthalten sind.
     * @param s
     * @return  
     */
    public boolean isSubStep(SolutionStep s) {
        if (s.candidatesToDelete.size() < candidatesToDelete.size()) {
            // hat weniger Kandidaten -> kann nicht sein
            return false;
        }
        for (Candidate cand : candidatesToDelete) {
            if (!s.candidatesToDelete.contains(cand)) {
                return false;
            }
        }
        return true;
    }

    public boolean isSingle() {
        return isSingle(type);
    }

    public boolean isSingle(SolutionType type) {
        return (type == SolutionType.FULL_HOUSE || type == SolutionType.HIDDEN_SINGLE || type == SolutionType.NAKED_SINGLE
                || type == SolutionType.TEMPLATE_SET);
    }

    public boolean isForcingChainSet() {
        if ((type == SolutionType.FORCING_CHAIN || type == SolutionType.FORCING_CHAIN_CONTRADICTION
                || type == SolutionType.FORCING_CHAIN_VERITY) && indices.size() > 0) {
            return true;
        }
        if ((type == SolutionType.FORCING_NET || type == SolutionType.FORCING_NET_CONTRADICTION
                || type == SolutionType.FORCING_NET_VERITY) && indices.size() > 0) {
            return true;
        }
        return false;
    }

    public int compareChainLengths(SolutionStep o) {
        return getChainLength() - o.getChainLength();
    }

    /**
     * Sortierreihenfolge:
     *
     *   - Die Steps mit den meisten zu löschenden Kandidaten zuerst
     *   - Dann nach betroffenen Kandidaten
     *   - dann nach Äquvalenz (wenn nicht äquivalent, nach Summe der Indexe der zu löschenden Kandidaten
     *   - dann nach betroffenen Kandidaten und Fins
     *   - innerhalb der gleichen Steps nach type
     */
    @Override
    public int compareTo(SolutionStep o) {
        int sum1 = 0, sum2 = 0;

        // Steps, die einen Kandidaten setzen, immer zuerst
        if (isSingle(type) && !isSingle(o.type)) {
            return -1;
        } else if (!isSingle(type) && isSingle(o.type)) {
            return 1;
        }

        // zuerst nach Anzahl zu löschende Kandidaten (absteigend!)
        int result = o.candidatesToDelete.size() - candidatesToDelete.size();
        if (result != 0) {
            return result;
        }

        // nach Äquivalenz (gleiche zu löschende Kandidaten)
        if (!isEquivalent(o)) {
//            // change 20110512: short chains first!
//            int chainDiff = compareChainLengths(o);
//            if (chainDiff != 0) {
//                return chainDiff;
//            }

            // nicht äquivalent: nach Indexsumme der zu löschenden Kandidaten
            sum1 = getIndexSumme(candidatesToDelete);
            sum2 = getIndexSumme(o.candidatesToDelete);
            // BUG 20110512: Sort order is intransitiv - doesnt work with Java7 anymore
//            return sum1 == sum2 ? 1 : sum1 - sum2;
            return (sum1 - sum2);
        }

        // SPECIAL STEPS
        // fish general: sort for
        //  - fish type
        //  - fish size
        //  - cannibalism
        //  - number of endo fins
        //  - number of fins
        if (type.isFish() && o.getType().isFish()) {
            int ret = type.compare(o.getType());
            if (ret != 0) {
                // different type or different size
                return ret;
            }
            ret = getCannibalistic().size() - o.getCannibalistic().size();
            if (ret != 0) {
                return ret;
            }
            ret = getEndoFins().size() - o.getEndoFins().size();
            if (ret != 0) {
                return ret;
            }
            ret = getFins().size() - o.getFins().size();
            if (ret != 0) {
                return ret;
            }
            if (!isEqualInteger(values, o.values)) {
                sum1 = getSumme(values);
                sum2 = getSumme(o.values);
                //return sum1 == sum2 ? 1 : sum1 - sum2;
                return sum1 - sum2;
            }
            return 0;
        }

        // kraken fish: sort for (fish type, chain length)
//        System.out.println("c: " + getType()+ "/"+ getSubType()+" - " + o.getType() + "/" + o.getSubType());
        if (type.isKrakenFish() && o.getType().isKrakenFish()) {
            //if (type == SolutionType.KRAKEN_FISH && o.getType() == SolutionType.KRAKEN_FISH) {
            int ret = subType.compare(o.getSubType());
//            System.out.println("ck1: " + ret);
            if (ret != 0) {
                return ret;
            }
//            System.out.println("ck2: " + compareChainLengths(o));
            return compareChainLengths(o);
        }

        // GENERAL STEPS
        // Neu: Chains - nach Länge der Chains (gesamt)
        int chainDiff = compareChainLengths(o);
        if (chainDiff != 0) {
            return chainDiff;
        }

        // jetzt nach betroffenen Kandidaten
        // wenn alle betroffenen Kandidaten gleich sind, sind die Steps gleich, sonst
        // zählt die Summe
        if (!isEqualInteger(values, o.values)) {
            sum1 = getSumme(values);
            sum2 = getSumme(o.values);
//            return sum1 == sum2 ? 1 : sum1 - sum2;
            return sum1 - sum2;
        }

        // Neuer Versuch: Nach Kandidaten, Fins und Typ
        // Zuerst Kandidaten
        if (!isEqualInteger(indices, o.indices)) {
            // zuerst nach Anzahl
            if (indices.size() != o.indices.size()) {
                return indices.size() - o.indices.size();
            }
            // dann nach Indexsumme (wieder aufsteigend)
            sum1 = getSumme(indices);
            sum2 = getSumme(o.indices);
//            return sum1 == sum2 ? 1 : sum2 - sum1;
            return sum2 - sum1;
        }

//        // Kandidaten sind gleich: nach Fins (je weniger desto besser)
//        if (!isEqualCandidate(fins, o.fins)) {
//            // zuerst nach Anzahl fins
//            if (fins.size() != o.fins.size()) {
//                return fins.size() - o.fins.size();
//            }
//            // gleich Anzahl Fins -> nach Indexsumme
//            sum1 = getIndexSumme(fins);
//            sum2 = getIndexSumme(o.fins);
////            return sum1 == sum2 ? 1 : sum2 - sum1;
//            return sum2 - sum1;
//        }

        // zuletzt nach Typ
        //return type.ordinal() - o.type.ordinal();
        return type.compare(o.getType());
    }

    public boolean isEqualValues(SolutionStep s) {
        return isEqualInteger(values, s.getValues());
    }

    private boolean isEqualInteger(List<Integer> l1, List<Integer> l2) {
        if (l1.size() != l2.size()) {
            return false;
        }
        int anz = l1.size();
        for (int i = 0; i < anz; i++) {
            int i1 = l1.get(i);
            boolean found = false;
            for (int j = 0; j < anz; j++) {
                int i2 = l2.get(j);
                if (i1 == i2) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public boolean isEqualCandidate(SolutionStep s) {
        return isEqualCandidate(candidatesToDelete, s.getCandidatesToDelete());
    }

    private boolean isEqualCandidate(List<Candidate> l1, List<Candidate> l2) {
        if (l1.size() != l2.size()) {
            return false;
        }
        int anz = l1.size();
        for (int i = 0; i < anz; i++) {
            Candidate c1 = l1.get(i);
            boolean found = false;
            for (int j = 0; j < anz; j++) {
                Candidate c2 = l2.get(j);
                if (c1.getIndex() == c2.getIndex() && c1.getValue() == c2.getValue()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * The sum of the indices of a collection of candidates
     * is used as a sorting criteria. For this to work, the
     * indices have to be weighted or else two combinations of
     * different indices could lead to the same sum.<br><br>
     * 
     * @param list
     * @return 
     */
    public int getIndexSumme(List<Candidate> list) {
        int sum = 0;
        int offset = 1;
        for (int i = 0; i < list.size(); i++) {
            sum += list.get(i).getIndex() * offset + list.get(i).getValue();
            offset += 80;
        }
        return sum;
    }

    public int getSumme(List<Integer> list) {
        int sum = 0;
        for (int i = 0; i < list.size(); i++) {
            sum += list.get(i);
        }
        return sum;
    }

    public int compareCandidatesToDelete(SolutionStep o) {
        int size1 = candidatesToDelete.size();
        int size2 = o.candidatesToDelete.size();
        if (size1 != size2) {
            // absteigend!
            return size2 - size1;
        }
        // gleich viele Kandidaten -> einzeln vergleichen
        int result = 0;
        for (int i = 0; i < size1; i++) {
            Candidate c1 = candidatesToDelete.get(i);
            Candidate c2 = o.candidatesToDelete.get(i);
            result = (c1.getIndex() * 10 + c1.getValue()) - (c2.getIndex() * 10 + c2.getValue());
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    public List<Entity> getBaseEntities() {
        return baseEntities;
    }

    public List<Entity> getCoverEntities() {
        return coverEntities;
    }

    public void setValues(List<Integer> values) {
        this.values = values;
    }

    public void setIndices(List<Integer> indices) {
        this.indices = indices;
    }

    public void setCandidatesToDelete(List<Candidate> candidatesToDelete) {
        this.candidatesToDelete = candidatesToDelete;
    }

    public void setCannibalistic(List<Candidate> cannibalistic) {
        this.cannibalistic = cannibalistic;
    }

    public void setFins(List<Candidate> fins) {
        this.fins = fins;
    }

    public void setEndoFins(List<Candidate> endoFins) {
        this.endoFins = endoFins;
    }

    public void setBaseEntities(List<Entity> baseEntities) {
        this.baseEntities = baseEntities;
    }

    public void setCoverEntities(List<Entity> coverEntities) {
        this.coverEntities = coverEntities;
    }

    public void setChains(List<Chain> chains) {
        this.chains = chains;
    }

    public void setAlses(List<AlsInSolutionStep> alses) {
        this.alses = alses;
    }

    public SortedMap<Integer, Integer> getColorCandidates() {
        return colorCandidates;
    }

    public void setColorCandidates(SortedMap<Integer, Integer> colorCandidates) {
        this.colorCandidates = colorCandidates;
    }

    public SolutionType getSubType() {
        return subType;
    }

    public void setSubType(SolutionType subType) {
        this.subType = subType;
    }

    public boolean isIsSiamese() {
        return isSiamese;
    }

    public void setIsSiamese(boolean isSiamese) {
        this.isSiamese = isSiamese;
    }

    public List<RestrictedCommon> getRestrictedCommons() {
        return restrictedCommons;
    }

    public void setRestrictedCommons(List<RestrictedCommon> restrictedCommons) {
        this.restrictedCommons = restrictedCommons;
    }

    /**
     * @return the progressScoreSingles
     */
    public int getProgressScoreSingles() {
        return progressScoreSingles;
    }

    /**
     * @param progressScoreSingles the progressScoreSingles to set
     */
    public void setProgressScoreSingles(int progressScoreSingles) {
        this.progressScoreSingles = progressScoreSingles;
    }

    /**
     * @return the progressScoreSingles
     */
    public int getProgressScoreSinglesOnly() {
        return progressScoreSinglesOnly;
    }

    /**
     * @param progressScoreSinglesOnly 
     */
    public void setProgressScoreSinglesOnly(int progressScoreSinglesOnly) {
        this.progressScoreSinglesOnly = progressScoreSinglesOnly;
    }

    /**
     * @return the progressScore
     */
    public int getProgressScore() {
        return progressScore;
    }

    /**
     * @param progressScore the progressScore to set
     */
    public void setProgressScore(int progressScore) {
        this.progressScore = progressScore;
    }

    /**
     * @return the potentialCannibalisticEliminations
     */
    public SudokuSet getPotentialCannibalisticEliminations() {
        return potentialCannibalisticEliminations;
    }

    /**
     * @param potentialCannibalisticEliminations the potentialCannibalisticEliminations to set
     */
    public void setPotentialCannibalisticEliminations(SudokuSet potentialCannibalisticEliminations) {
        this.potentialCannibalisticEliminations = potentialCannibalisticEliminations;
    }

    /**
     * @return the potentialEliminations
     */
    public SudokuSet getPotentialEliminations() {
        return potentialEliminations;
    }

    /**
     * @param potentialEliminations the potentialEliminations to set
     */
    public void setPotentialEliminations(SudokuSet potentialEliminations) {
        this.potentialEliminations = potentialEliminations;
    }
}
