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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import solver.Als;
import solver.TableEntry;
import solver.TablingSolver;

/**
 * A chain consists of links, every link can be weak or strong, it can be a candidat in a cell,
 * a group node, an ALS, AUR...<br><br>
 *
 * Since more than one chain can share the same array, the chain starts with <code>chain[start]</code>
 * and it ends with <code>chain[end]</code>.
 * 
 * Every entry is a 32-bit Integer. Format of one entry:
 * <pre>
 * |       |       |       |       |       |       |       |       |
 * |x x|x x x x|x x x x x x x|x x x x x x x|x x x x x x x|x|x x x x|
 * | 7 |   6   |     5       |      4      |      3      |2|   1   |
 *
 *  1: candidate (entry candidate if node is ALS, AUR...)
 *  2: Strong (1 ... candidat is set) or weak (0 ... candidate is not set)
 *  3: normal node: cell index
 *     group node:  index of first cell
 *     als, aur...: index of the first cell that provides the entry
 *                  (e.g. turns an ALS into a LS)
 *  4: normal node: not used
 *     group node:  index of second cell or 0x7f if not used
 *     als, aur...: lower 7 bits of the index in appropriate array 
 *                  (stored outside the chain, normally in {@link SolutionStep})
 *  5: normal node: not used
 *     group node:  third cell or 0x7f if not used
 *     als, aur...: higher 7 bits of the index in appropriate array 
 *                  (stored outside the chain, normally in {@link SolutionStep})
 *  6: type of node:
 *       0 ... normal node
 *       1 ... group node
 *       2 ... ALS node
 *       3 ... AHS node (not yet implemented)
 *       4 ... AUR node (not yet implemented)
 *       5 ... Chain node
 *  7: reserved
 * </pre>
 * This format is used in chains and in {@link TableEntry table entries} (see {@link TablingSolver}).<br><br>
 *
 * An entry can be negativ to indicate that it is a starting point for a branch in a net.<br><br>
 *
 * The class contains a set of static methods to do the bit manipulations for a single
 * node and a corresponding set of methods that do the same for entries in {@link #chain}, that
 * delegate to the static methods.
 *
 * @author hobiwan
 */
public class Chain implements Cloneable, Serializable {

    private static final long serialVersionUID = 1L;
    /** Mask for isolating candidate and indices (used for comparisons). */
    private static final int EQUALS_MASK = 0x3fffffef;
    /** Mask for isolating the candidate. */
    private static final int CAND_MASK = 0xf;
    /** Mask for isolating the link type (weak or strong). */
    private static final int STRONG_MASK = 0x10;
    /** Every index consists of 7 bits (max: 128). To isolate an index using
     *  this mask the entry has to be shifted by the appropriate INDEXn_OFFSET.
     */
    private static final int INDEX_MASK = 0x7f;
    /** Mask for isolating the first index. */
    private static final int INDEX1_MASK = 0xfe0;
    /** Number of bits that the entry has to be right shifted to get the
     *  index1 to the right.
     */
    private static final int INDEX1_OFFSET = 5;
    /** Mask for isolating the second index. */
    private static final int INDEX2_MASK = 0x7f000;
    /** Number of bits that the entry has to be right shifted to get the
     *  index2 to the right.
     */
    private static final int INDEX2_OFFSET = 12;
    /** Mask for isolating the third index. */
    private static final int INDEX3_MASK = 0x3f80000;
    /** Number of bits that the entry has to be right shifted to get the
     *  index3 to the right.
     */
    private static final int INDEX3_OFFSET = 19;
    /** Mask isolating the ALS index (index into the accompaning als array). */
    private static final int ALS_INDEX_MASK = 0x3fff000;
    /** Number of bits the ALS index has to be moved to the right. */
    private static final int ALS_INDEX_OFFSET = 12;
    /** All bits set: index not used (only valid for third index in grouped node) */
    private static final int NO_INDEX = 0x7f;
    /** Mask for isolating the node type */
    private static final int MODE_MASK = 0x3c000000;
    /** Mask for deleting the node type */
    private static final int MODE_DEL_MASK = 0xc3ffffff;
    /** Number of bits for the node type field */
    private static final int MODE_OFFSET = 26;
    /** Node consists of a single cells thats not an ALS. */
    private static final int NORMAL_NODE_MASK = 0x0;
    /** Node is a group node. */
    private static final int GROUP_NODE_MASK = 0x4000000;
    /** Node is an ALS. */
    private static final int ALS_NODE_MASK = 0x8000000;
    /** Node is an AHS. */
    private static final int AHS_NODE_MASK = 0xC000000;
    /** Node is an AUR. */
    private static final int AUR_NODE_MASK = 0x10000000;
    /** Node is a Chain. */
    private static final int CHAIN_NODE_MASK = 0x14000000;
    /** Flag for normal nodes. */
    public static final int NORMAL_NODE = 0;
    /** Flag for group nodes. */
    public static final int GROUP_NODE = 1;
    /** Flag for ALS nodes. */
    public static final int ALS_NODE = 2;
    /** Flag for AHS nodes. */
    public static final int AHS_NODE = 3;
    /** Flag for AUR nodes. */
    public static final int AUR_NODE = 4;
    /** Flag for chain nodes. */
    public static final int CHAIN_NODE = 5;
    /** Names for node types (for {@link #toString() }). */
    public static final String[] TYPE_NAMES = new String[]{"NORMAL_NODE", "GROUP_NODE", "ALS_NODE", "AHS_NODE", "AUR_NODE", "CHAIN_NODE"};
    /** The index of the first entry belonging to the chain. */
    private int start;
    /** The index of the last entry belonging to the chain. */
    private int end;
    /** The length of the chain. For normal nodes it equals <code>(end - start + 1)</code>, for
     *  complex chains a penalty for the complex nodes is added (see {@link #calculateLength(java.util.List)}).
     *  A value of <code>-1</code> means, that the length has not been calculated yet.
     */
    private int length;
    /** Array holding the links for the current chain. */
    private int[] chain;

    /** Create an empty instance. */
    public Chain() {
    }

    /** Create and initialize a new chain.
     * @param start
     * @param end
     * @param chain  
     */
    public Chain(int start, int end, int[] chain) {
        this.start = start;
        this.end = end;
        this.chain = chain;
        this.length = -1;
    }

    /**
     * Makes a deep copy of the current chain.
     * @return
     */
    @Override
    public Object clone() {
        try {
            Chain newChain = (Chain) super.clone();
            newChain.start = start;
            newChain.end = end;
//        newChain.chain = chain.clone();
            newChain.chain = Arrays.copyOf(chain, end + 1);
            return newChain;
        } catch (CloneNotSupportedException ex) {
            return null;
        }
    }

    /**
     * Deletes the chain.
     */
    public void reset() {
        start = 0;
        end = 0;
        length = -1;
    }

    /**
     * Sets {@link #length} to trigger recalculation.
     */
    public void resetLength() {
        length = -1;
    }

    /**
     * Convenience method, delegates to {@link #getLength(java.util.List)}.
     * @return
     */
    public int getLength() {
        return getLength(null);
    }

    /**
     * Returns the length of the chain, recalculates it if necessary using
     * {@link #calculateLength(java.util.List)}.
     * @param alses An array containing all the ALSes of the current grid.
     * @return
     */
    public int getLength(List<AlsInSolutionStep> alses) {
        if (length == -1) {
            length = calculateLength(alses);
        }
        return length;
    }

    /**
     * Calculates the length of the chain: If the chain contains complex
     * nodes, a penalty is added (the chain is treated as if it were longer).
     * This is used in sorting. A chain containing fewer nodes but consisting
     * of large ALS would otherwise be preferred over a slightly larger simple chain.
     * @param alses
     * @return
     */
    private int calculateLength(List<AlsInSolutionStep> alses) {
        double tmpLength = 0;
        for (int i = start; i <= end; i++) {
            tmpLength++;
            if (getSNodeType(chain[i]) == Chain.ALS_NODE) {
                if (alses != null) {
                    int alsIndex = getSAlsIndex(chain[i]);
                    if (alses.size() > alsIndex) {
                        tmpLength += alses.get(alsIndex).getChainPenalty();
                    } else {
                        tmpLength += 5;
                    }
                } else {
                    tmpLength += 2.5;
                }
            }
        }
        return (int) tmpLength;
    }

    /**
     * Getter for {@link #start}.
     * @return
     */
    public int getStart() {
        return start;
    }

    /**
     * Setter for {@link #start}.
     * @param start
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Getter for {@link #end}.
     * @return
     */
    public int getEnd() {
        return end;
    }

    /**
     * Setter for {@link #end}.
     * @param end
     */
    public void setEnd(int end) {
        this.end = end;
    }

    /**
     * Getter for {@link #chain}.
     * @return
     */
    public int[] getChain() {
        return chain;
    }

    /**
     * Setter for {@link #chain}.
     * @param chain
     */
    public void setChain(int[] chain) {
        this.chain = chain;
    }

    /**
     * Creates a new node. Delegates to {@link #makeSEntry(int, int, int, int, boolean, int)}.
     * @param cellIndex
     * @param candidate
     * @param isStrong
     * @return
     */
    public static int makeSEntry(int cellIndex, int candidate, boolean isStrong) {
        return makeSEntry(cellIndex, 0, 0, candidate, isStrong, NORMAL_NODE);
    }

    /**
     * Creates a new node. Delegates to {@link #makeSEntry(int, int, int, int, boolean, int)}.
     * @param cellIndex
     * @param candidate
     * @param isStrong
     * @param nodeType
     * @return
     */
    public static int makeSEntry(int cellIndex, int candidate, boolean isStrong, int nodeType) {
        return makeSEntry(cellIndex, 0, 0, candidate, isStrong, nodeType);
    }

    /**
     * Creates a new node. Delegates to {@link #makeSEntry(int, int, int, int, boolean, int)}.
     * Before delegation the <code>alsIndex</code> is split in two parts that are then
     * stored separately.
     * @param cellIndex
     * @param alsIndex
     * @param candidate
     * @param isStrong
     * @param nodeType
     * @return
     */
    public static int makeSEntry(int cellIndex, int alsIndex, int candidate, boolean isStrong, int nodeType) {
        int tmpIndex = getSHigherAlsIndex(alsIndex);
        alsIndex = getSLowerAlsIndex(alsIndex);
        return makeSEntry(cellIndex, alsIndex, tmpIndex, candidate, isStrong, nodeType);
    }

    /**
     * Creates a new link. 
     * @param cellIndex1 Index of the first/only cell
     * @param cellIndex2 Index of the second cell in a group node, lower 7 bits
     *    of an ALS index or -1 if unused
     * @param cellIndex3 Index of the third cell in a group node, upper 7 bits
     *    of an ALS index or -1 if unused
     * @param candidate The exit candidate for the link
     * @param isStrong The type of the link (weak or strong)
     * @param nodeType The node type ({@link #NORMAL_NODE}, {@link #GROUP_NODE}
     *    or {@link #ALS_NODE}).
     * @return
     */
    public static int makeSEntry(int cellIndex1, int cellIndex2, int cellIndex3, int candidate, boolean isStrong, int nodeType) {
        int entry = (cellIndex1 << INDEX1_OFFSET) | candidate;
        if (isStrong) {
            entry |= STRONG_MASK;
        }

        if (nodeType != NORMAL_NODE) {
            switch (nodeType) {
                case GROUP_NODE:
                    entry |= GROUP_NODE_MASK;
                    break;
                case ALS_NODE:
                    entry |= ALS_NODE_MASK;
                    break;
                case AHS_NODE:
                    entry |= AHS_NODE_MASK;
                    break;
                case AUR_NODE:
                    entry |= AUR_NODE_MASK;
                    break;
                case CHAIN_NODE:
                    entry |= CHAIN_NODE_MASK;
                    break;
            }
        }

        if (cellIndex2 == -1) {
            if (nodeType == NORMAL_NODE) {
                cellIndex2 = 0;
            } else {
                cellIndex2 = NO_INDEX;
            }
        }
        if (cellIndex3 == -1) {
            if (nodeType == NORMAL_NODE) {
                cellIndex3 = 0;
            } else {
                cellIndex3 = NO_INDEX;
            }
        }
        entry |= (cellIndex2 << INDEX2_OFFSET);
        entry |= (cellIndex3 << INDEX3_OFFSET);
//        System.out.println("   " + toString(entry));
        return entry;
    }

    /**
     * Directly set the complete link contained in <code>entry</code> into
     * the chain slot denoted by <code>index</code>.
     * @param index
     * @param entry
     */
    public void setEntry(int index, int entry) {
        chain[index] = entry;
    }

    /**
     * Create a new node and store it at position <code>index</code>.
     * @param index
     * @param cellIndex
     * @param candidate
     * @param isStrong
     */
    public void setEntry(int index, int cellIndex, int candidate, boolean isStrong) {
        setEntry(index, makeSEntry(cellIndex, candidate, isStrong));
    }

    /**
     * Return the upper 7 bits of an index.
     * @param alsIndex
     * @return
     */
    public static int getSHigherAlsIndex(int alsIndex) {
        return (alsIndex >> 7) & INDEX_MASK;
    }

    /**
     * Return the lower 7 bits of an index.
     * @param alsIndex
     * @return
     */
    public static int getSLowerAlsIndex(int alsIndex) {
        return alsIndex &= INDEX_MASK;
    }

    /**
     * Get the first cell index of the node.
     * @param entry
     * @return
     */
    public static int getSCellIndex(int entry) {
        if (entry > 0) {
            return (entry >> INDEX1_OFFSET) & INDEX_MASK;
        } else {
            return ((-entry) >> INDEX1_OFFSET) & INDEX_MASK;
        }
    }

    /**
     * Get the second cell index of the node.
     * @param entry
     * @return
     */
    public static int getSCellIndex2(int entry) {
        int result = -1;
        if (entry > 0) {
            result = (entry >> INDEX2_OFFSET) & INDEX_MASK;
        } else {
            result = ((-entry) >> INDEX2_OFFSET) & INDEX_MASK;
        }
        if (result == INDEX_MASK) {
            result = -1;
        }
        return result;
    }

    /**
     * Get the third cell index of the node.
     * @param entry
     * @return
     */
    public static int getSCellIndex3(int entry) {
        int result = -1;
        if (entry > 0) {
            result = (entry >> INDEX3_OFFSET) & INDEX_MASK;
        } else {
            result = ((-entry) >> INDEX3_OFFSET) & INDEX_MASK;
        }
        if (result == INDEX_MASK) {
            result = -1;
        }
        return result;
    }

    /**
     * Retrieve the index in the als array for the <code>entry</code>.
     * @param entry
     * @return
     */
    public static int getSAlsIndex(int entry) {
        int result = -1;
        if (entry < 0) {
            entry = -entry;
        }
        result = (entry & ALS_INDEX_MASK) >> ALS_INDEX_OFFSET;
        return result;
    }

    /**
     * Replace the index of the als used in the link. Is needed when
     * chains with ALS nodes are stored in a {@link SolutionStep}: Only the
     * ALS, that are really needed by the step, are stored, which means, that
     * the index for the ALSes changes.
     * @param entry
     * @param newAlsIndex
     * @return
     */
    public static int replaceSAlsIndex(int entry, int newAlsIndex) {
        boolean isMin = false;
        if (entry < 0) {
            isMin = true;
            entry = -entry;
        }
        entry &= ~ALS_INDEX_MASK;
        newAlsIndex <<= ALS_INDEX_OFFSET;
        newAlsIndex &= ALS_INDEX_MASK;
        entry |= newAlsIndex;
        if (isMin) {
            entry = -entry;
        }
        return entry;
    }

    /**
     * See {@link #replaceSAlsIndex(int, int)}.
     * @param entryIndex
     * @param newAlsIndex
     */
    public void replaceAlsIndex(int entryIndex, int newAlsIndex) {
        chain[entryIndex] = replaceSAlsIndex(chain[entryIndex], newAlsIndex);
    }

    /**
     * Returns the first index of the current entry.
     * @param index
     * @return
     */
    public int getCellIndex(int index) {
        return getSCellIndex(chain[index]);
    }

    /**
     * Returns the link candidate contained in <code>entry</code>.
     * @param entry
     * @return
     */
    public static int getSCandidate(int entry) {
        if (entry > 0) {
            return entry & CAND_MASK;
        } else {
            return (-entry) & CAND_MASK;
        }
    }

    /**
     * Returns the link candidate of the node contained in {@link #chain}<code>[index]</code>.
     * @param index
     * @return
     */
    public int getCandidate(int index) {
        return getSCandidate(chain[index]);
    }

    /**
     * Tests if <code>entry</code> is a strong link.
     * @param entry
     * @return
     */
    public static boolean isSStrong(int entry) {
        if (entry > 0) {
            return (entry & STRONG_MASK) != 0;
        } else {
            return ((-entry) & STRONG_MASK) != 0;
        }
    }

    /**
     * Tests if {@link #chain}<code>[index]</code>
     * is a strong link.
     * @param index
     * @return
     */
    public boolean isStrong(int index) {
        return isSStrong(chain[index]);
    }

    /**
     * Retrieves the node type from <code>entry</code>.
     * @param entry
     * @return
     */
    public static int getSNodeType(int entry) {
        if (entry > 0) {
            return (entry & MODE_MASK) >> MODE_OFFSET;
        } else {
            return ((-entry) & MODE_MASK) >> MODE_OFFSET;
        }
    }

    /**
     * Retrieves the node type from {@link #chain}<code>[index]</code>.
     * @param index
     * @return
     */
    public int getNodeType(int index) {
        return getSNodeType(chain[index]);
    }

    /**
     * Changes the link type (weak/strong) of <code>entry</code>.
     * @param entry
     * @param strong
     * @return
     */
    public static int setSStrong(int entry, boolean strong) {
        if (strong) {
            entry |= STRONG_MASK;
        } else {
            entry &= ~STRONG_MASK;
        }
        return entry;
    }

    /**
     * Fills an instance of type {@link SudokuSetBase} with all cells, that
     * can see the complete node. Delegates to {@link #getSNodeBuddies(int, int, java.util.List, sudoku.SudokuSetBase)}.
     * @param index
     * @param set
     * @param alses
     */
    public void getNodeBuddies(int index, SudokuSetBase set, List<Als> alses) {
        getSNodeBuddies(chain[index], getCandidate(index), alses, set);
    }

    /**
     * Fills an instance of type {@link SudokuSetBase} with all cells, that
     * can see the complete node.<br>
     * <ul>
     * <li>for normal nodes just takes the buddies of the node</li>
     * <li>for group nodes takes the ANDed buddies of all node cells</li>
     * <li>for ALS takes the anded buddies of all ALS cells, that contain that candidate</li>
     * </ul>
     * 
     * @param entry The entry
     * @param candidate Only valid for als entries: the candidate for which the buddies should be
     *        calculated
     * @param alses A list with all alses for that chain (only valid for ALS nodes)
     * @param set The set containing the buddies
     */
    public static void getSNodeBuddies(int entry, int candidate, List<Als> alses, SudokuSetBase set) {
        if (getSNodeType(entry) == NORMAL_NODE) {
            set.set(Sudoku2.buddies[getSCellIndex(entry)]);
        } else if (getSNodeType(entry) == Chain.GROUP_NODE) {
            set.set(Sudoku2.buddies[getSCellIndex(entry)]);
            set.and(Sudoku2.buddies[getSCellIndex2(entry)]);
            if (getSCellIndex3(entry) != -1) {
                set.and(Sudoku2.buddies[getSCellIndex3(entry)]);
            }
        } else if (getSNodeType(entry) == Chain.ALS_NODE) {
            Als als = alses.get(getSAlsIndex(entry));
            set.set(als.buddiesPerCandidat[candidate]);
        } else {
            set.clear();
            Logger.getLogger(Chain.class.getName()).log(Level.SEVERE, "getSNodeBuddies() gesamt: invalid node type ({0})",
                    getSNodeType(entry));
        }
    }

//    public static boolean equalsIndexCandidate(int entry1, int entry2) {
//        return (entry1 & EQUALS_MASK) == (entry2 & EQUALS_MASK);
//    }
    /**
     * Returns a string representation for the node contained in <code>entry</code>. Mostely used
     * for testing.
     * @param entry
     * @return
     */
    public static String toString(int entry) {
        if (entry == Integer.MIN_VALUE) {
            return "MIN";
        }
        String sign = "";
        if (entry < 0) {
            sign = "-";
        }
        if (getSNodeType(entry) == ALS_NODE) {
            return sign + TYPE_NAMES[getSNodeType(entry)] + "/"
                    + getSAlsIndex(entry) + "/"
                    + SolutionStep.getCellPrint(getSCellIndex(entry))
                    + "/" + isSStrong(entry) + "/" + getSCandidate(entry);
        } else if (getSNodeType(entry) == GROUP_NODE) {
            return sign + TYPE_NAMES[getSNodeType(entry)] + "/"
                    + SolutionStep.getCompactCellPrint(getSCellIndex(entry), getSCellIndex2(entry), getSCellIndex3(entry)) + "/"
                    + isSStrong(entry) + "/" + getSCandidate(entry);
        } else {
            return sign + TYPE_NAMES[getSNodeType(entry)] + "/"
                    + SolutionStep.getCellPrint(getSCellIndex(entry)) + "/"
                    + isSStrong(entry) + "/" + getSCandidate(entry);
        }
    }

    /**
     * Returns a string representation of the chain.
     * @return
     */
    @Override
    public String toString() {
        StringBuilder tmp = new StringBuilder();
        for (int i = start; i <= end; i++) {
            tmp.append(toString(chain[i]));
            tmp.append(" ");
        }
        return tmp.toString();
    }

    public static void main(String[] args) {
        int entry = makeSEntry(0, 1, true);
        System.out.println("Entry: " + getSCellIndex(entry) + "/" + getSCandidate(entry) + "/" + isSStrong(entry));
    }
}
