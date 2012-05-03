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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A bitset class that can hold candidates. It can contain digits 1 to 9. For easier
 * iteration the bitset is backed by an array.
 * 
 * @author hobiwan
 */
public final class SudokuSetShort implements Cloneable, Serializable {

    private static final long serialVersionUID = 1L;
    public static final SudokuSetShort EMPTY_SET = new SudokuSetShort();
    public static final short[] MASKS = {
        0x0000,
        0x0001, 0x0002, 0x0004, 0x0008,
        0x0010, 0x0020, 0x0040, 0x0080,
        0x0100
    };
    private static final short MAX_MASK = 0x01ff;
    // für jede der 256 möglichen Kombinationen von Bits das entsprechende Array
    private static final int[][] possibleValues = new int[0x200][];
    // und zu jeder Zahl die Länge des Arrays
    private static int[] anzValues = new int[0x200];
    private short mask = 0; //  0 - 9
    boolean initialized = true;
    private int[] values = null;
    private int anz = 0;

    static {
        // possibleValues initialisieren
        possibleValues[0] = new int[0];
        anzValues[0] = 0;
        int[] temp = new int[9];
        for (int i = 1; i <= 0x1ff; i++) {
            int index = 0;
            int mask = 1;
            for (int j = 1; j <= 0x1ff; j++) {
                if ((i & mask) != 0) {
                    temp[index++] = j;
                }
                mask <<= 1;
            }
            possibleValues[i] = new int[index];
//            for (int k = 0; k < index; k++) {
//                possibleValues[i][k] = temp[k];
//            }
            System.arraycopy(temp, 0, possibleValues[i], 0, index);
            anzValues[i] = index;
        }
    }

    /** Creates a new instance of SudokuSetBase */
    public SudokuSetShort() {
    }

    public SudokuSetShort(SudokuSetShort init) {
        set(init);
    }

    public SudokuSetShort(boolean full) {
        if (full) {
            setAll();
        }
    }

    @Override
    public SudokuSetShort clone() {
        SudokuSetShort newSet = null;
        try {
            newSet = (SudokuSetShort) super.clone();
            // dont clone the array (for performance reasons - might not be necessary)
            values = null;
            initialized = false;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error while cloning", ex);
        }
        return newSet;
    }

    public int get(int index) {
        if (! isInitialized()) {
            initialize();
        }
        return values[index];
    }

    public int size() {
        if (! isInitialized()) {
            initialize();
        }
        return anz;
    }

    public boolean isEmpty() {
        return (mask == 0);
    }

    public void add(int value) {
        // Bitmap
        mask |= MASKS[value];
        initialized = false;
    }

    public void remove(int value) {
        // Bitmap
        mask = (short) (mask & ~MASKS[value]);
        initialized = false;
    }

    public void set(SudokuSetShort set) {
        mask = set.mask;
        initialized = false;
    }

    public void set(int data) {
        mask = (short) data;
        initialized = false;
    }

    public boolean contains(int value) {
        return (mask & MASKS[value]) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (! (o instanceof SudokuSetShort)) {
            return false;
        }
        SudokuSetShort s = (SudokuSetShort) o;
        return mask == s.mask;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + this.mask;
        return hash;
    }

    public void clear() {
        mask = 0;
        anz = 0;
        initialized = false;
    }

    public void setAll() {
        mask = MAX_MASK;
        initialized = false;
    }

    public void or(SudokuSetShort b) {
        mask |= b.mask;
        initialized = false;
    }

    public void orNot(SudokuSetShort set) {
        mask = (short) (mask | ~set.mask);
        initialized = false;
    }

    public void and(SudokuSetShort set) {
        mask &= set.mask;
        initialized = false;
    }

    public void andNot(SudokuSetShort set) {
        mask = (short) (mask & ~set.mask);
        initialized = false;
    }

    /**
     * gibt ((this & set) == this) zur�ck
     * @param set
     * @return  
     */
    public boolean andEquals(SudokuSetShort set) {
        short m = (short) (mask & set.mask);
        return (m == mask);
    }

    /**
     * gibt ((this & ~set) == this) zur�ck
     * @param set
     * @return  
     */
    public boolean andNotEquals(SudokuSetShort set) {
        short m = (short) (mask & ~set.mask);
        return (m == mask);
    }

    /**
     * gibt ((this & set) == 0) zur�ck
     * @param set
     * @return  
     */
    public boolean andEmpty(SudokuSetShort set) {
        short m = (short) (mask & set.mask);
        return (m == 0);
    }

    public void not() {
        mask = (short) (~mask);
        initialized = false;
    }

    String pM(long mask) {
        return Long.toHexString(mask);
    }

    private void initialize() {
        values = possibleValues[mask];
        anz = anzValues[mask];
        initialized = true;
    }

    @Override
    public String toString() {
        if (! initialized) {
            initialize();
        }
        if (anz == 0) {
            return "empty!";
        }
        StringBuilder tmp = new StringBuilder();
        tmp.append(Integer.toString(values[0]));
        for (int i = 1; i < anz; i++) {
            tmp.append(" ").append(Integer.toString(values[i]));
        }
        return tmp.toString();
    }

    public long getMask1() {
        return mask;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * @return the mask
     */
    public short getMask() {
        return mask;
    }

    /**
     * @param mask the mask to set
     */
    public void setMask(short mask) {
        this.mask = mask;
    }

    /**
     * @return the values
     */
    public int[] getValues() {
        if (! initialized) {
            initialize();
        }
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(int[] values) {
        this.values = values;
    }

    /**
     * @return the anz
     */
    public int getAnz() {
        return anz;
    }

    /**
     * @param anz the anz to set
     */
    public void setAnz(int anz) {
        this.anz = anz;
    }

    public static void main(String[] args) {
        SudokuSetShort set = new SudokuSetShort();
        for (int i = 1; i <= 9; i++) {
            set.add(i);
        }
        System.out.println(set);
        set.clear();
        set.add(2);
        set.add(5);
        set.add(6);
        System.out.println(set);
    }
}
