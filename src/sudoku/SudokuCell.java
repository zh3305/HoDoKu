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

/**
 * Compatibility class for loading Sudoku files that were stored with 
 * an older version of HoDoKu (2.0 or older). It contains only the 
 * public contract for XMLEncoder.
 * 
 * @author hobiwan
 */
public class SudokuCell {
    public static final int USER = 0;
    public static final int PLAY = 1;
    public static final int ALL = 2;

    private static final short M_1 = 0x0001;
    private static final short M_2 = 0x0002;
    private static final short M_3 = 0x0004;
    private static final short M_4 = 0x0008;
    private static final short M_5 = 0x0010;
    private static final short M_6 = 0x0020;
    private static final short M_7 = 0x0040;
    private static final short M_8 = 0x0080;
    private static final short M_9 = 0x0100;
    private static final short M_ALL = 0x01FF;
    private static final short[] masks = { M_1, M_2, M_3, M_4, M_5, M_6, M_7, M_8, M_9 };
    
    private byte value = 0;
    private boolean isFixed = false; // vorgegebene Zahl, kann nicht verändert werden!
    private short[] candidates = new short[3];
    
    /** Creates a new instance of SudokuCell */
    public SudokuCell() {
        // do nothing
    }
    
    public SudokuCell(byte value) {
        this.value = value;
    }
    
//    @Override
//    public SudokuCell clone() throws CloneNotSupportedException {
//        SudokuCell newCell = (SudokuCell) super.clone();
//        newCell.candidates = new short[candidates.length];
//        for (int i = 0; i < candidates.length; i++) {
//            newCell.candidates[i] = candidates[i];
//        }
//        return newCell;
//    }

    public void setValue(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
    
//    public void setValue(int value, boolean isFixed) {
//        this.value = (byte) value;
//        this.isFixed = isFixed;
//    }
    
    public void setIsFixed(boolean isFixed) {
        this.isFixed = isFixed;
    }
    
    public boolean isIsFixed() {
        return isFixed;
    }

    public short[] getCandidates() {
        return candidates;
    }

    public void setCandidates(short[] candidates) {
        this.candidates = candidates;
    }
    
    public String getCandidateString(int type) {
        StringBuilder tmp = new StringBuilder();
        for (int i = 1; i <= 9; i++) {
            if (isCandidate(type, i)) {
                tmp.append(i);
            }
        }
        return tmp.toString();
    }
    
    protected boolean isCandidate(int type, int value) {
        short mask = masks[value - 1];
        return (candidates[type] & mask) != 0 ? true : false;
    }
}
