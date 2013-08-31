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
import java.util.List;
import sudoku.SolutionStep;
import sudoku.Sudoku2;
import sudoku.SudokuSet;

/**
 * A class for finding and storing Group Nodes: A group node consists of
 * up to three candidates, that occupy a single row or a single column
 * within a single block.<br><br>
 * 
 * Every block can hold multiple group nodes, group nodes are allowed to
 * overlap.<br><br>
 * 
 * Group nodes are created by the factory method {@link #getGroupNodes(solver.SudokuStepFinder)}.
 * Creating a group node in any other way is not possible.
 *
 * @author hobiwan
 */
public class GroupNode {

    /** Debug flag */
    private static final boolean DEBUG = false;
    /**
     * 
     * Indices as bit mask.
     */
    public SudokuSet indices = new SudokuSet();
    /**
     * All buddies that can see all cells in the group node.
     */
    public SudokuSet buddies = new SudokuSet();
    /**
     * Candidate for the group node.
     */
    public int cand;
    /**
     * The line of the group node (index in {@link Sudoku2#ROWS}), -1 if not applicable
     */
    public int line = -1;
    /**
     * The column of the group node (index in {@link Sudoku2#COLS}), -1 if not applicable
     */
    public int col = -1;
    /**
     * The block of the group node (index in {@link Sudoku2#BLOCKS}); every
     * group node must have a block.
     */
    public int block;
    /**
     * Index of the first cell in the group node.
     */
    public int index1;
    /**
     * Index of the second cell in the group node.
     */
    public int index2;
    /**
     * Index of the third cell in the group node or -1, if the group node 
     * consists only of two cells (which is a valid case)
     */
    public int index3;
    /**
     * All positions for a given candidate in a given house
     */
    private static SudokuSet candInHouse = new SudokuSet();
    /**
     * For checks with blocks.
     */
    private static SudokuSet tmpSet = new SudokuSet();

    /**
     * Creates a new instance of GroupNode. Group nodes are only
     * created by the static factory methods.
     * 
     * @param cand The candidateof the group node.
     * @param indices  The cells of the group node.
     */
    private GroupNode(int cand, SudokuSet indices) {
        this.cand = cand;
        this.indices.set(indices);
        index1 = indices.get(0);
        index2 = indices.get(1);
        index3 = -1;
        if (indices.size() > 2) {
            index3 = indices.get(2);
        }
        block = Sudoku2.getBlock(index1);
        if (Sudoku2.getLine(index1) == Sudoku2.getLine(index2)) {
            line = Sudoku2.getLine(index1);
        }
        if (Sudoku2.getCol(index1) == Sudoku2.getCol(index2)) {
            col = Sudoku2.getCol(index1);
        }
        // calculate the buddies
        buddies.set(Sudoku2.buddies[index1]);
        buddies.and(Sudoku2.buddies[index2]);
        if (index3 >= 0) {
            buddies.and(Sudoku2.buddies[index3]);
        }
    }

    /**
     * Presents a group node in human readable form.
     * 
     * @return 
     */
    @Override
    public String toString() {
        return "GroupNode: " + cand + " - " + SolutionStep.getCompactCellPrint(index1, index2, index3) + "  - " + index1 + "/" + index2 + "/" + index3
                + " (" + line + "/" + col + "/" + block + ")";
    }

    /**
     * Gets all group nodes from the given sudoku and puts them in an ArrayList.
     *
     * For all candidates in all lines and all cols do:
     * <ul>
     *  <li>check if they have a candidate left</li>
     *  <li>if so, check if an intersection of line/col and a block contains
     *     more than one candidate; if yes -> group node found</li>
     * </ul
     * @param finder A current instance of {@link SudokuStepFinder} for the
     *          sudoku. Used to find remaining candidates.
     * @return  
     */
    public static List<GroupNode> getGroupNodes(SudokuStepFinder finder) {
        List<GroupNode> groupNodes = new ArrayList<GroupNode>();

        getGroupNodesForHouseType(groupNodes, finder, Sudoku2.LINE_TEMPLATES, true);
        getGroupNodesForHouseType(groupNodes, finder, Sudoku2.COL_TEMPLATES, false);

        return groupNodes;
    }

    /**
     * Does the real work in finding group nodes. If a line or a column has candidates
     * left and two or more of them are confined to a block, a group node is added
     * to <code>groupNodes</code>.
     * 
     * @param groupNodes A list with all group nodes.
     * @param finder The finder for the current sudoku.
     * @param houses Templates for all lines/cols
     * @param isLines <code>true</code> if <code>houses</code> holds
     *          lines, <code>false</code> for cols.
     */
    private static void getGroupNodesForHouseType(List<GroupNode> groupNodes,
            SudokuStepFinder finder, SudokuSet[] houses, boolean isLines) {
        for (int i = 0; i < houses.length; i++) {
            int[] blocks = isLines ? Sudoku2.BLOCKS_FROM_LINES[i] : Sudoku2.BLOCKS_FROM_COLS[i];
            for (int cand = 1; cand <= 9; cand++) {
                candInHouse.setAnd(houses[i], finder.getCandidates()[cand]);
                if (candInHouse.isEmpty()) {
                    // no candidates left in this house -> proceed
                    continue;
                }

                // candidates left in house -> check blocks
                for (int j = 0; j < blocks.length; j++) {
                    tmpSet.setAnd(candInHouse, Sudoku2.BLOCK_TEMPLATES[blocks[j]]);
                    // isEmpty() is much faster than size()
                    if (!tmpSet.isEmpty() && tmpSet.size() >= 2) {
                        // group node found
                        groupNodes.add(new GroupNode(cand, tmpSet));
                        if (DEBUG) {
                            System.out.println("GroupNode found: " + groupNodes.get(groupNodes.size() - 1));
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        Sudoku2 sudoku = new Sudoku2();
        sudoku.setSudoku(":0000:x:.4..1..........5.6......3.15.38.2...7......2..........6..5.7....2.....1....3.14..:211 213 214 225 235 448 465 366 566 468 469::");
        long ticks = System.currentTimeMillis();
        List<GroupNode> groupNodes = GroupNode.getGroupNodes(null);
        ticks = System.currentTimeMillis() - ticks;
        System.out.println("getGroupNodes(): " + ticks + "ms, " + groupNodes.size() + " group nodes");
        for (GroupNode node : groupNodes) {
            System.out.println("  " + node);
        }
    }
}
