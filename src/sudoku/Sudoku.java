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

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Compatibility class for loading Sudoku files that were stored with 
 * an older version of HoDoKu (2.0 or older). It contains only the contract
 * for XMLEncoder.
 * 
 * @author hobiwan
 */
public class Sudoku {
    private static final int[][] COLS = {
        {0, 9, 18, 27, 36, 45, 54, 63, 72},
        {1, 10, 19, 28, 37, 46, 55, 64, 73},
        {2, 11, 20, 29, 38, 47, 56, 65, 74},
        {3, 12, 21, 30, 39, 48, 57, 66, 75},
        {4, 13, 22, 31, 40, 49, 58, 67, 76},
        {5, 14, 23, 32, 41, 50, 59, 68, 77},
        {6, 15, 24, 33, 42, 51, 60, 69, 78},
        {7, 16, 25, 34, 43, 52, 61, 70, 79},
        {8, 17, 26, 35, 44, 53, 62, 71, 80}
    };
    private static final int CPL = 9;
    
    // Ein Template pro Kandidat mit allen prinzipiell noch möglichen Positionen
    // (ohne Berücksichtigung der gesetzten Zellen)

    private SudokuSetBase[] possiblePositions = new SudokuSetBase[10];
    // Ein Template pro Kandidat mit allen noch möglichen Positionen
    private SudokuSetBase[] allowedPositions = new SudokuSetBase[10];
    // Ein Template pro Kandidat mit allen gesetzten Positionen
    private SudokuSet[] positions = new SudokuSet[10];    // 9x9 Sudoku, linearer Zugriff (ist leichter)
    private SudokuCell[] cells = new SudokuCell[81];    // Schwierigkeits-Level dieses Sudokus
    private DifficultyLevel level;
    private int score;    // Ausgangspunkt für dieses Sudoku (für reset)
    private String initialState = null;

    /** Creates a new instance of Sudoku */
    public Sudoku() {
        for (int i = 0; i <= 9; i++) {
            allowedPositions[i] = new SudokuSet(true);
            allowedPositions[i].setAll();
            possiblePositions[i] = new SudokuSet(true);
            possiblePositions[i].setAll();
            positions[i] = new SudokuSet();
        }
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new SudokuCell();
        }
    }

    public SudokuCell[] getCells() {
        return cells;
    }

    public void setCells(SudokuCell[] cells) {
        this.cells = cells;
    }

    public DifficultyLevel getLevel() {
        return level;
    }

    public void setLevel(DifficultyLevel level) {
        this.level = level;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public SudokuSetBase[] getPossiblePositions() {
        return possiblePositions;
    }

    public void setPossiblePositions(SudokuSetBase[] possiblePositions) {
        this.possiblePositions = possiblePositions;
    }

    public SudokuSetBase[] getAllowedPositions() {
        return allowedPositions;
    }

    public void setAllowedPositions(SudokuSetBase[] allowedPositions) {
        this.allowedPositions = allowedPositions;
    }

    public SudokuSet[] getPositions() {
        return positions;
    }

    public void setPositions(SudokuSet[] positions) {
        this.positions = positions;
    }

    public String getInitialState() {
        return initialState;
    }

    public void setInitialState(String initialState) {
        this.initialState = initialState;
    }

    /**
     * Used to get a representation of the sudoku that can be fed directly into
     * a Sudoku2.
     * 
     * @param mode
     * @param step
     * @return 
     */
    public String getSudoku(ClipboardMode mode, SolutionStep step) {
        String dot = Options.getInstance().isUseZeroInsteadOfDot() ? "0" : ".";
        StringBuilder out = new StringBuilder();
        if (mode == ClipboardMode.LIBRARY) {
            if (step == null) {
                out.append(":0000:x:");
            } else {
                String type = step.getType().getLibraryType();
                if (step.getType().isFish() && step.isIsSiamese()) {
                    type += "1";
                }
                out.append(":").append(type).append(":");
//                for (int i = 0; i < step.getValues().size(); i++) {
//                    out.append(step.getValues().get(i));
//                }
                // append the candidates, that can be deleted
                SortedSet<Integer> candToDeleteSet = new TreeSet<Integer>();
                if (step.getType().useCandToDelInLibraryFormat()) {
                    for (Candidate cand : step.getCandidatesToDelete()) {
                        candToDeleteSet.add(cand.getValue());
                    }
                }
                // if nothing can be deleted, append the cells, that can be set
                if (candToDeleteSet.isEmpty()) {
                    for (int i = 0; i < step.getValues().size(); i++) {
                        candToDeleteSet.add(step.getValues().get(i));
                    }
                }
                for (int cand : candToDeleteSet) {
                    out.append(cand);
                }
                out.append(":");
            }
        }
        if (mode == ClipboardMode.CLUES_ONLY || mode == ClipboardMode.VALUES_ONLY
                || mode == ClipboardMode.LIBRARY) {
            for (SudokuCell cell : cells) {
                if (cell.getValue() == 0 || (mode == ClipboardMode.CLUES_ONLY && !cell.isIsFixed())) {
                    //out.append(".");
                    out.append(dot);
                } else {
                    if (mode == ClipboardMode.LIBRARY && !cell.isIsFixed()) {
                        out.append("+");
                    }
                    out.append(Integer.toString(cell.getValue()));
                }
            }
        }
        if (mode == ClipboardMode.PM_GRID || mode == ClipboardMode.PM_GRID_WITH_STEP) {
            // new: create one StringBuffer per cell with all candidates/values; add
            // special characters for step if necessary; if a '*' is added to a cell, 
            // insert a blank in all other cells of that col that don't have a '*';
            // calculate fieldLength an write it
            StringBuilder[] cellBuffers = new StringBuilder[cells.length];
            for (int i = 0; i < cells.length; i++) {
                cellBuffers[i] = new StringBuilder();
                if (cells[i].getValue() != 0) {
                    cellBuffers[i].append(String.valueOf(cells[i].getValue()));
                } else {
                    //cellBuffers[i].append(String.valueOf(cells[i].getCandidateString(SudokuCell.PLAY)));
                    String candString = cells[i].getCandidateString(SudokuCell.PLAY);
                    if (candString.isEmpty()) {
                        candString = dot;
                    }
                    cellBuffers[i].append(candString);
                }
            }

            // now add markings for step
            if (mode == ClipboardMode.PM_GRID_WITH_STEP && step != null) {
                boolean[] cellsWithExtraChar = new boolean[cells.length];
                // indices
                for (int index : step.getIndices()) {
                    insertOrReplaceChar(cellBuffers[index], '*');
                    cellsWithExtraChar[index] = true;
                }
                // fins and endo-fins
                if (SolutionType.isFish(step.getType())
                        || step.getType() == SolutionType.W_WING) {
                    for (Candidate cand : step.getFins()) {
                        int index = cand.getIndex();
                        insertOrReplaceChar(cellBuffers[index], '#');
                        cellsWithExtraChar[index] = true;
                    }
                }
                if (SolutionType.isFish(step.getType())) {
                    for (Candidate cand : step.getEndoFins()) {
                        int index = cand.getIndex();
                        insertOrReplaceChar(cellBuffers[index], '@');
                        cellsWithExtraChar[index] = true;
                    }
                }
                // chains
                for (Chain chain : step.getChains()) {
                    for (int i = chain.getStart(); i <= chain.getEnd(); i++) {
                        if (chain.getNodeType(i) == Chain.ALS_NODE) {
                            // ALS are handled separately
                            continue;
                        }
                        int index = chain.getCellIndex(i);
                        insertOrReplaceChar(cellBuffers[index], '*');
                        cellsWithExtraChar[index] = true;
                        if (chain.getNodeType(i) == Chain.GROUP_NODE) {
                            index = Chain.getSCellIndex2(chain.getChain()[i]);
                            if (index != -1) {
                                insertOrReplaceChar(cellBuffers[index], '*');
                                cellsWithExtraChar[index] = true;
                            }
                            index = Chain.getSCellIndex3(chain.getChain()[i]);
                            if (index != -1) {
                                insertOrReplaceChar(cellBuffers[index], '*');
                                cellsWithExtraChar[index] = true;
                            }
                        }
                    }
                }

                // ALS
                char alsChar = 'A';
                for (AlsInSolutionStep als : step.getAlses()) {
                    for (int index : als.getIndices()) {
                        insertOrReplaceChar(cellBuffers[index], alsChar);
                        cellsWithExtraChar[index] = true;
                    }
                    alsChar++;
                }

                // candidates to delete
                for (Candidate cand : step.getCandidatesToDelete()) {
                    int index = cand.getIndex();
                    char candidate = Character.forDigit(cand.getValue(), 10);
                    for (int i = 0; i < cellBuffers[index].length(); i++) {
                        if (cellBuffers[index].charAt(i) == candidate && (i == 0 || (i > 0 && cellBuffers[index].charAt(i - 1) != '-'))) {
                            cellBuffers[index].insert(i, '-');
                            if (i == 0) {
                                cellsWithExtraChar[index] = true;
                            }
                        }
                    }
                }

                // now adjust columns, where a character was added
                for (int i = 0; i < cellsWithExtraChar.length; i++) {
                    if (cellsWithExtraChar[i]) {
                        int[] indices = Sudoku.COLS[Sudoku.getCol(i)];
                        for (int j = 0; j < indices.length; j++) {
                            if (Character.isDigit(cellBuffers[indices[j]].charAt(0))) {
                                cellBuffers[indices[j]].insert(0, ' ');
                            }
                        }
                    }
                }
            }

            int[] fieldLengths = new int[COLS.length];
            for (int i = 0; i < cellBuffers.length; i++) {
                int col = getCol(i);
                if (cellBuffers[i].length() > fieldLengths[col]) {
                    fieldLengths[col] = cellBuffers[i].length();
                }
            }
            for (int i = 0; i < fieldLengths.length; i++) {
                fieldLengths[i] += 2;
            }
            for (int i = 0; i < 9; i++) {
                if ((i % 3) == 0) {
                    writeLine(out, i, fieldLengths, null, true);
                }
                writeLine(out, i, fieldLengths, cellBuffers, false);
            }
            writeLine(out, 9, fieldLengths, null, true);

            if (mode == ClipboardMode.PM_GRID_WITH_STEP && step != null) {
                //out.append("\r\n");
                out.append(step.toString(2));
            }
        }
        if (mode == ClipboardMode.LIBRARY) {
            // gelöschte Kandidaten anhängen
            int type = SudokuCell.PLAY;
            boolean first = true;
            out.append(":");
            for (int i = 0; i < cells.length; i++) {
                SudokuCell cell = cells[i];
                if (cell.getValue() == 0) {
                    for (int j = 1; j <= 9; j++) {
                        if (cell.isCandidate(SudokuCell.ALL, j) && !cell.isCandidate(type, j)) {
                            if (first) {
                                first = false;
                            } else {
                                out.append(" ");
                            }
                            out.append(Integer.toString(j)).append(Integer.toString((i / 9) + 1)).append(Integer.toString((i % 9) + 1));
                        }
                    }
                }
            }
            if (step == null) {
                out.append("::");
            } else {
                String candString = step.getCandidateString(true);
                out.append(":").append(candString).append(":");
//                if (SolutionType.isFish(step.getType())) {
//                    step.getEntities(out, step.getBaseEntities(), true);
//                    out.append(" ");
//                    step.getEntities(out, step.getCoverEntities(), true);
//                    if (step.getFins().size() > 0) {
//                        out.append(" ");
//                        step.getFins(out, false, true);
//                    }
//                    if (step.getEndoFins().size() > 0) {
//                        out.append(" ");
//                        step.getFins(out, true, true);
//                    }
//                }
                if (candString.isEmpty()) {
                    out.append(step.getValueIndexString());
                }
                out.append(":");
                if (step.getType().isSimpleChainOrLoop()) {
                    out.append((step.getChainLength() - 1));
                }
            }
        }
        return out.toString();
    }

    private void insertOrReplaceChar(StringBuilder buffer, char ch) {
        if (Character.isDigit(buffer.charAt(0))) {
            buffer.insert(0, ch);
        } else {
            buffer.replace(0, 1, Character.toString(ch));
        }
    }

    private void writeLine(StringBuilder out, int line, int[] fieldLengths,
            StringBuilder[] cellBuffers, boolean drawOutline) {
        if (drawOutline) {
            char leftRight = '.';
            char middle = '.';
            if (line == 3 || line == 6) {
                leftRight = ':';
                middle = '+';
            } else if (line == 9) {
                leftRight = '\'';
                middle = '\'';
            }
            out.append(leftRight);
            //for (int i = 0; i < 3 * fieldLength; i++) {
            for (int i = 0; i < fieldLengths[0] + fieldLengths[1] + fieldLengths[2]; i++) {
                out.append('-');
            }
            out.append(middle);
            for (int i = 0; i < fieldLengths[3] + fieldLengths[4] + fieldLengths[5]; i++) {
                out.append('-');
            }
            out.append(middle);
            for (int i = 0; i < fieldLengths[6] + fieldLengths[7] + fieldLengths[8]; i++) {
                out.append('-');
            }
            out.append(leftRight);
        } else {
            for (int i = line * 9; i < (line + 1) * 9; i++) {
                if ((i % 3) == 0) {
                    out.append("|");
                    if ((i % 9) != 8) {
                        out.append(' ');
                    }
                } else {
                    out.append(' ');
                }
                int tmp = fieldLengths[getCol(i)];
                out.append(cellBuffers[i]);
                tmp -= cellBuffers[i].length();
                for (int j = 0; j < tmp - 1; j++) {
                    out.append(' ');
                }
            }
            out.append('|');
        }
        out.append("\r\n");
    }

    private static int getCol(int index) {
        return index % CPL;
    }
}
