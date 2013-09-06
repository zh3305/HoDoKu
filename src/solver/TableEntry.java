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

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sudoku.Chain;
import sudoku.Options;
import sudoku.SudokuSet;

/**
 * The format of an entry is described in class {@link Chain}.<br><br>
 *
 * An instance of <code>TableEntry</code> holds all possible outcomes for
 * one single premise. The premise itself is the first element in the <code>TableEntry</code>.
 * The {@link TablingSolver} holds two arrays of <code>TableEntries</code>. The index of
 * the entry decides cell and candidate (<code>cell * 10 + candidate</code>), one
 * table is for "candidate is deleted from cell" and one is for "cell is set to that number".<br><br>
 *
 * A <code>TableEntry</code> consists mainly of two synchronized arrays: {@link #entries} contains
 * the possible conclusions, {@link #retIndices} contains up to five indices of {@link #entries}, that
 * have to be true to reach that conclusion (used to reconstruct the chain from the table). If
 * an entry has more than one return index, the result has to be a net instead of a chain.<br><br>
 *
 * Two arrays of sets, {@link #onSets} and {@link #offSets}, hold bitmaps that summarize all
 * possible conclusions for the premise. A set bit in <code>onSets[cand]</code> means that
 * that cell can be set to a value of <code>cand</code>, a set bit in <code>offSets[cand]</code>,
 * that <code>cand</code> can be eliminated from that cell. The sets are used to check for possible
 * outcomes.<br><br>
 *
 * In {@link #retIndices} references are stored to the chain elements that made
 * the current chain entry possible (index in the current table). Each retIndex 
 * can hold a maximum of five references to predecessors thus limiting
 * the complexity of networks.<br><br>
 * 
 * Format of entries in retIndices:
 * <pre>
 *   bit  0 .. 11: Index of first predecessor (indices in extendedTable
 *                 can be longer than 10 bits)
 *   bit 12 .. 21: Index of second predecessor (only for nets)
 *   bit 22 .. 31: Index of third predecessor (only for nets)
 *   bit 32 .. 41: Index of fourth predecessor (only for nets)
 *   bit 42 .. 51: Index of fifth predecessor (only for nets)
 *   bit 52 .. 60: Distance to the root element of the chain
 *   bit 61: set if entry is expanded (comes from another table)
 *   bit 62: source table is on- or off-table
 *   bit 63: source table is extended-table
 * </pre>
 *
 * Please note:
 * <ul>
 *   <li>"distance" is used in finding the shortest possible chain
 *       (number of links used to get to the current entry)</li>
 *   <li>if more than one index is present, index 1 is always the largest
 *       numerical value (should make the shortest chain the "main"
 *       chain in networks)</li>
 *   <li>if "expanded" (61) is set, the index1 is an index in the table
 *       from where the entry was expanded. Which table this is depends on
 *       the flags "onTable" and "extended":<ul>
 *          <li>onTable 0 extended 0: {@link TablingSolver#offTable}</li>
 *          <li>onTable 1 extended 0: {@link TablingSolver#onTable}</li>
 *          <li>onTable 0 extended 1: {@link TablingSolver#extendedTable}</li>
 *          <li>onTable 1 extended 1: invalid</li></ul>
 * </ul>
 *
 * @author hobiwan
 */
public class TableEntry {

    /** Debug flag */
    private static final boolean DEBUG = true;
    /** Entry has been expanded from another table. */
    private static final long EXPANDED = 0x2000000000000000L;
    /** Bitmap indicating that the entry comes from {@link TablingSolver#onTable}. */
    private static final long ON_TABLE = 0x4000000000000000L;
    /** Bitmap indicating that the entry comes from {@link TablingSolver#extendedTable}. */
    private static final long EXTENDED_TABLE = 0x8000000000000000L;
//    private static final long RAW_ENTRY      = 0x1fffffffffffffffL;
    /** Index into {@link #entries} and {@link #retIndices}. */
    int index = 0;
    /** The actual table, holding all resulting links. Synchronized with {@link #retIndices}. */
    int[] entries = new int[Options.getInstance().getMaxTableEntryLength()];
    /** Contains up to 5 reverse indices plus the distance of the entry to the root assumption. Synchronized with {@link #entries}. */
    long[] retIndices = new long[Options.getInstance().getMaxTableEntryLength()];
    /** Array of sets holding all cells for every candidate that can be set as a result of the assumption. */
    SudokuSet[] onSets = new SudokuSet[10];
    /** Array of sets holding all cells for every candidate that can be deleted as a result of the assumption. */
    SudokuSet[] offSets = new SudokuSet[10];
    /** Reverse lookup cache: hold the index in {@link #entries} for every entry. Used when constructing the chain from the result
     * and when expanding tables. */
    SortedMap<Integer, Integer> indices = new TreeMap<Integer, Integer>();

    /** Creates a new instance. */
    TableEntry() {
        for (int i = 0; i < onSets.length; i++) {
            onSets[i] = new SudokuSet();
            offSets[i] = new SudokuSet();
        }
    }

    /**
     * Clears the whole table.
     */
    void reset() {
        index = 0;
        entries[0] = 0;
        retIndices[0] = 0;
        indices.clear();
        for (int i = 0; i < onSets.length; i++) {
            onSets[i].clear();
            offSets[i].clear();
        }
        for (int i = 0; i < entries.length; i++) {
            entries[i] = 0;
            retIndices[i] = 0;
        }
    }

    /**
     * Adds an entry for cell <code>cellIndex</code>, candidate <code>cand</code> using
     * an ALS penalty.
     * @param cellIndex
     * @param cand
     * @param penalty
     * @param set
     */
    void addEntry(int cellIndex, int cand, int penalty, boolean set) {
        addEntry(cellIndex, -1, -1, Chain.NORMAL_NODE, cand, set, 0, 0, 0, 0, 0, penalty);
    }

    /**
     * Adds a simple node without a reverse index. Used for filling the initial tables.
     * @param cellIndex
     * @param cand
     * @param set
     */
    void addEntry(int cellIndex, int cand, boolean set) {
        addEntry(cellIndex, -1, -1, Chain.NORMAL_NODE, cand, set, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Adds a simple node with a reverse index. Used for expanding tables.
     * @param cellIndex
     * @param cand
     * @param set
     * @param reverseIndex
     */
    void addEntry(int cellIndex, int cand, boolean set, int reverseIndex) {
        addEntry(cellIndex, -1, -1, Chain.NORMAL_NODE, cand, set, reverseIndex, 0, 0, 0, 0, 0);
    }

    /**
     * Adds a simple node with up to 5 reverse indices.
     * @param cellIndex
     * @param cand
     * @param set
     * @param ri1
     * @param ri2
     * @param ri3
     * @param ri4
     * @param ri5
     */
    void addEntry(int cellIndex, int cand, boolean set, int ri1,
            int ri2, int ri3, int ri4, int ri5) {
        addEntry(cellIndex, -1, -1, Chain.NORMAL_NODE, cand, set, ri1, ri2, ri3, ri4, ri5, 0);
    }

    /**
     * Adds an ALS node with a possible penalty.
     * @param cellIndex1
     * @param alsIndex
     * @param nodeType
     * @param cand
     * @param set
     * @param penalty
     */
    void addEntry(int cellIndex1, int alsIndex, int nodeType, int cand, boolean set, int penalty) {
        addEntry(cellIndex1, Chain.getSLowerAlsIndex(alsIndex), Chain.getSHigherAlsIndex(alsIndex),
                nodeType, cand, set, 0, 0, 0, 0, 0, penalty);
    }

    /**
     * Adds an ALS node with a possible penalty and up to five ret indices.
     * @param cellIndex1
     * @param alsIndex
     * @param nodeType
     * @param cand
     * @param set
     * @param ri1
     * @param ri2
     * @param ri3
     * @param ri4
     * @param ri5
     * @param penalty
     */
    void addEntry(int cellIndex1, int alsIndex, int nodeType, int cand, boolean set, int ri1,
            int ri2, int ri3, int ri4, int ri5, int penalty) {
        addEntry(cellIndex1, Chain.getSLowerAlsIndex(alsIndex), Chain.getSHigherAlsIndex(alsIndex),
                nodeType, cand, set, ri1, ri2, ri3, ri4, ri5, penalty);
    }

    /**
     * Adds entries to the table.
     * @param cellIndex1 The index of the cell for {@link Chain#NORMAL_NODE}; the index of the first
     *   cell for {@link Chain#GROUP_NODE}; the index of the cell that provides entry into an
     *   ALS for {@link Chain#ALS_NODE}.
     * @param cellIndex2 -1 for {@link Chain#NORMAL_NODE}, the index of the second cell for
     *   {@link Chain#GROUP_NODE}; the lower 7 bits of the ALS index for {@link Chain#ALS_NODE}.
     * @param cellIndex3 -1 for {@link Chain#NORMAL_NODE}; the index of the third cell or -1, if no
     *   third cell exists for {@link Chain#GROUP_NODE}; the higher 7 bits of the ALS index
     *   for {@link Chain#ALS_NODE}.
     * @param nodeType {@link Chain#NORMAL_NODE}, {@link Chain#GROUP_NODE} or {@link Chain#ALS_NODE}.
     * @param cand The candidate for the link or the entry candidate for an ALS link.
     * @param set <code>true</code> if the cell can be set to <code>cand</code>, <code>false</code>, if
     *   <code>cand</code> can be eliminated from the cell.
     * @param ri1 The first reverse index or 0 for entries that depend directly on the initial assumption.
     *   For entries that have been expanded from another table the index of that table.
     * @param ri2 The second reverse index for a net or 0 if no reverse index is present.
     * @param ri3 The third reverse index for a net or 0 if no reverse index is present.
     * @param ri4 The fourth reverse index for a net or 0 if no reverse index is present.
     * @param ri5 The fifth reverse index for a net or 0 if no reverse index is present.
     * @param penalty A possible penalty for complex ALS nodes.
     */
    void addEntry(int cellIndex1, int cellIndex2, int cellIndex3, int nodeType, int cand, boolean set, int ri1,
            int ri2, int ri3, int ri4, int ri5, int penalty) {
        if (index >= entries.length) {
            // already full, some possible outcomes will be missed...
            if (DEBUG) {
                System.out.println("WARNING: addEntry(): TableEntry is already full (" + cellIndex1 + ", " + cellIndex2 + ", "
                        + cellIndex3 + ", " + nodeType + ", " + cand + ", " + set + ", " + ri1 + ", " + ri2 + ", "
                        + ri3 + ", " + ri4 + ", " + ri5 + ", " + penalty);
            }
//            Logger.getLogger(getClass().getName()).log(Level.WARNING, "addEntry(): TableEntry is already full!");
            return;
        }
        // check only for single cells -> group nodes, ALS etc. can not be start or end point
        // of a chain (in this implementation)
        /*K*///What about shorter paths?
        if (nodeType == Chain.NORMAL_NODE) {
            if ((set && onSets[cand].contains(cellIndex1)) || (!set && offSets[cand].contains(cellIndex1))) {
                // already there
                return;
            }
        }
        // construct the entry and store it
        int entry = Chain.makeSEntry(cellIndex1, cellIndex2, cellIndex3, cand, set, nodeType);
        entries[index] = entry;
        retIndices[index] = makeSRetIndex(ri1, ri2, ri3, ri4, ri5);
        // when expanding ri1 is the index of the original table for the entry;
        // setting the distance doesn't make any sense in this context (distance
        // is set by the expansion routine). Since we don't know here, whether we
        // are expanding or not, we just try to avoid exceptions
        // NOTE: for initial entries the code works correctly; for expanded entries
        //       the distance is overridden immediately by the expansion code.
        if (ri1 < retIndices.length) {
            setDistance(index, getDistance(ri1) + 1);
        }

        // chains end only in normal links (in this implementation)
        if (nodeType == Chain.NORMAL_NODE) {
            if (set) {
                onSets[cand].add(cellIndex1);
            } else {
                offSets[cand].add(cellIndex1);
            }
        }

        // 20090213: Adjust chain penalty for ALS
        int distance = getDistance(index);
        distance += penalty;
        setDistance(index, distance);

        if (DEBUG) {
            if (nodeType == Chain.GROUP_NODE && this.toString().equals("solver.TableEntry@c5384d")) {
                System.out.println("GN added to table " + this + " (" + index + "/" + Chain.toString(entry) + ")");
            }
        }

        indices.put(entry, index);
        index++;
    }

    /**
     * Returns the entry with index <code>index</code>.
     * @param index
     * @return
     */
    int getEntry(int index) {
        return entries[index];
    }

    /**
     * Checks, if the entry is in the table.
     * 
     * @param entry
     * @return <code>true</code>: table contains <code>entry</code>, else <code>false</code>.
     */
    boolean containsEntry(int entry) {
        Integer ret = indices.get(entry);
        if (ret == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Tries to find an entry for cell <code>cellIndex</code>, candidate
     * <code>cand</code> and type <code>set</code>. If it can be found,
     * the index is returned. Used to construct net dependencies.<br><br>
     * 
     * It is possible, that the entry doesnt yet exist.
     * @param cellIndex
     * @param set
     * @param cand
     * @return
     */
    int getEntryIndex(int cellIndex, boolean set, int cand) {
        ///*K*/ returns null???
        Integer ret = indices.get(Chain.makeSEntry(cellIndex, cand, set));
        if (ret == null) {
            if (DEBUG) {
                System.out.println("TableEntry.getEntryIndex() - entry not found: " + cellIndex + ", " + cand + ", " + set);
            }
//            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "entry not found: {0}, {1}, {2}", new Object[]{cellIndex, cand, set});
            return 0;
        } else {
            return ret.intValue();
        }
    }

    /**
     * Tries to find <code>entry</code> in {@link #entries} using {@link #indices}.
     * The index into {@link #entries} is returned.
     * @param entry
     * @return
     */
    int getEntryIndex(int entry) {
        Integer tmp = indices.get(entry);
        if (tmp == null) {
            if (DEBUG) {
                System.out.println("TableEntry.getEntryIndex() - tmp == null: " + Chain.toString(entry) + " (" + entry + ") (" + Chain.getSCellIndex(entry) + "/" + Chain.getSCellIndex2(entry) + "/" + Chain.getSCellIndex3(entry) + ")");
                TablingSolver.printTable("tmp == null", this, null);
            }
//            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "tmp == null: {0}", entry);
            return 0;
        }
        return indices.get(entry);
    }

    /**
     * Checks if the table is already full.
     * @return
     */
    boolean isFull() {
        return index == entries.length;
    }

    /**
     * Returns the cell index contained in <code>entries[index]</code>.
     * @param index
     * @return
     */
    public int getCellIndex(int index) {
        return Chain.getSCellIndex(entries[index]);
    }

    /**
     * Checks if <code>entries[index]</code> is a strong link or not.
     * @param index
     * @return
     */
    public boolean isStrong(int index) {
        return Chain.isSStrong(entries[index]);
    }

    /**
     * Returns the candidate contained in <code>entries[index]</code>.
     * @param index
     * @return
     */
    public int getCandidate(int index) {
        return Chain.getSCandidate(entries[index]);
    }

    /**
     * Constructs the data for {@link #retIndices}: Every entry contains up to
     * 5 reverse indices (indices of entries on which the entry depends). For every
     * index 10 bits are reserved, which makes the largest value for every index 1023.
     * The first index has 12 bits, allowing for a maximum of 4095.<br><br>
     *
     * The largest value has to be first (I have forgotten why!).
     * @param index1
     * @param index2
     * @param index3
     * @param index4
     * @param index5
     * @return
     */
    public static long makeSRetIndex(long index1, long index2, long index3,
            long index4, long index5) {
        // lets try without sorting
        long tmp = 0;
        if (index1 > 4096) {
            index1 = 0;
        }
        if (index2 > 1023) {
            index2 = 0;
        }
        if (index3 > 1023) {
            index3 = 0;
        }
        if (index4 > 1023) {
            index4 = 0;
        }
        if (index5 > 1023) {
            index5 = 0;
        }
        if (index2 > index1) {
            tmp = index2;
            index2 = index1;
            index1 = tmp;
        }
        if (index3 > index1) {
            tmp = index3;
            index3 = index1;
            index1 = tmp;
        }
        if (index4 > index1) {
            tmp = index4;
            index4 = index1;
            index1 = tmp;
        }
        if (index5 > index1) {
            tmp = index5;
            index5 = index1;
            index1 = tmp;
        }
        // construct the entry
        return (index5 << 42) + (index4 << 32) + (index3 << 22)
                + (index2 << 12) + index1;
    }

    /**
     * Calculates the number of reverse indices contained in <code>retIndex</code>.
     * The first reverse index is always set, even if it is 0.
     * @param retIndex
     * @return
     */
    public static int getSRetIndexAnz(long retIndex) {
        int anz = 1;
        retIndex >>= 12;
        for (int i = 0; i < 4; i++) {
            if ((retIndex & 0x3ff) != 0) {
                anz++;
            }
            retIndex >>= 10;
        }
        return anz;
    }

    /**
     * Calculate the number of reverse indices contained in <code>retIndices[index]</code>.
     * Delegates to {@link #getSRetIndexAnz(long)}.
     * @param index
     * @return
     */
    public int getRetIndexAnz(int index) {
        return getSRetIndexAnz(retIndices[index]);
    }

    /**
     * Gets the reverse index <code>which</code> from entry
     * <code>retIndex</code>.
     * @param retIndex A valid reverse index entry.
     * @param which 0 to 4 for reverse indices, 5 for distance.
     * @return
     */
    public static int getSRetIndex(long retIndex, int which) {
        if (which == 0) {
            return (int) (retIndex & 0xfff);
        } else {
            int ret = (int) ((retIndex >> (which * 10 + 2)) & 0x3ff);
            if (which == 5) {
                // distance has only 9 bit!
                ret &= 0x1ff;
            }
            return ret;
        }
    }

    /**
     * Gets the reverse index <code>which</code> from entry
     * <code>retIndices[index]</code>. Delegates to {@link #getSRetIndex(long, int)}.
     * @param index
     * @param which
     * @return
     */
    public int getRetIndex(int index, int which) {
        return getSRetIndex(retIndices[index], which);
    }

    /**
     * Sets the distance in <code>retIndices[index]</code>.
     * @param index
     * @param distance
     */
    public void setDistance(int index, int distance) {
        // delete old distance (52 times 1)
        long tmp = distance & 0x1ff;
        retIndices[index] &= 0xE00FFFFFFFFFFFFFL;
        retIndices[index] |= (tmp << 52);
    }

    /**
     * Retrieves the distance from <code>retIndices[index]</code>.
     * @param index
     * @return
     */
    public int getDistance(int index) {
        return getSRetIndex(retIndices[index], 5) & 0x1ff;
    }

    /**
     * Prüft, ob der Eintrag aus einer anderen Tabelle stammt. Wenn ja,
     * ist getRetIndex( index, 0) der Index der Tabelle und ON_TABLE bestimmt,
     * ob es aus onTables oder aus offTables kommt
     */
    /**
     * Checks if the entry comes from another table. If <code>true</code>,
     * {@link #isOnTable(int) isOnTable(index)} returns if the source of the entry is
     * {@link TablingSolver#onTable} or {@link TablingSolver#offTable} and
     * {@link #getRetIndex(int, int) getRetIndex(index, 0)} is the index in
     * the source table.
     * @param index
     * @return
     */
    public boolean isExpanded(int index) {
        return (retIndices[index] & EXPANDED) != 0;
    }

    /**
     * Entry comes from another table.
     * @param index
     */
    public void setExpanded(int index) {
        retIndices[index] |= EXPANDED;
    }

    /**
     * Checks if the source of the expanded entry <code>entries[index]</code>
     * was {@link TablingSolver#onTable} or {@link TablingSolver#offTable}.
     * @param index
     * @return
     */
    public boolean isOnTable(int index) {
        return (retIndices[index] & ON_TABLE) != 0;
    }

    /**
     * The source of the expanded entry <code>entries[index]</code>
     * was {@link TablingSolver#onTable}.
     * @param index
     */
    public void setOnTable(int index) {
        retIndices[index] |= ON_TABLE;
    }

    /**
     * Checks if the source of <code>entries[index]</code> was
     * {@link TablingSolver#extendedTable}.
     * @param index
     * @return
     */
    public boolean isExtendedTable(int index) {
        return (retIndices[index] & EXTENDED_TABLE) != 0;
    }

    /**
     * The source of <code>entries[index]</code> was
     * {@link TablingSolver#extendedTable}.
     * @param index
     */
    public void setExtendedTable(int index) {
        retIndices[index] |= EXTENDED_TABLE;
    }

    /**
     * The source of the last entry added to the table was
     * {@link TablingSolver#extendedTable}.
     */
    public void setExtendedTable() {
        retIndices[index - 1] |= EXTENDED_TABLE;
    }

    /**
     * Retrieves the node type of the entry <code>entries[index]</code>.
     * @param index
     * @return  
     */
    public int getNodeType(int index) {
        return Chain.getSNodeType(entries[index]);
    }
}
