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
 *
 * @author hobiwan
 */
public class SudokuSetBase implements Cloneable, Serializable, Comparable<SudokuSetBase> {
    private static final long serialVersionUID = 1L;

    public static final SudokuSet EMPTY_SET = new SudokuSet();
    public static final long[] MASKS = {
        0x0000000000000001L, 0x0000000000000002L, 0x0000000000000004L, 0x0000000000000008L,
        0x0000000000000010L, 0x0000000000000020L, 0x0000000000000040L, 0x0000000000000080L,
        0x0000000000000100L, 0x0000000000000200L, 0x0000000000000400L, 0x0000000000000800L,
        0x0000000000001000L, 0x0000000000002000L, 0x0000000000004000L, 0x0000000000008000L,
        0x0000000000010000L, 0x0000000000020000L, 0x0000000000040000L, 0x0000000000080000L,
        0x0000000000100000L, 0x0000000000200000L, 0x0000000000400000L, 0x0000000000800000L,
        0x0000000001000000L, 0x0000000002000000L, 0x0000000004000000L, 0x0000000008000000L,
        0x0000000010000000L, 0x0000000020000000L, 0x0000000040000000L, 0x0000000080000000L,
        0x0000000100000000L, 0x0000000200000000L, 0x0000000400000000L, 0x0000000800000000L,
        0x0000001000000000L, 0x0000002000000000L, 0x0000004000000000L, 0x0000008000000000L,
        0x0000010000000000L, 0x0000020000000000L, 0x0000040000000000L, 0x0000080000000000L,
        0x0000100000000000L, 0x0000200000000000L, 0x0000400000000000L, 0x0000800000000000L,
        0x0001000000000000L, 0x0002000000000000L, 0x0004000000000000L, 0x0008000000000000L,
        0x0010000000000000L, 0x0020000000000000L, 0x0040000000000000L, 0x0080000000000000L,
        0x0100000000000000L, 0x0200000000000000L, 0x0400000000000000L, 0x0800000000000000L,
        0x1000000000000000L, 0x2000000000000000L, 0x4000000000000000L, 0x8000000000000000L
    };
    public static final long MAX_MASK1 = 0xFFFFFFFFFFFFFFFFL;
    public static final long MAX_MASK2 = 0x1FFFFL;
    protected long mask1 = 0; //  0 - 63

    protected long mask2 = 0; // 64 - 80

    protected boolean initialized = true;

    /** Creates a new instance of SudokuSetBase */
    public SudokuSetBase() {
    }

    public SudokuSetBase(SudokuSetBase init) {
        set(init);
    }

    public SudokuSetBase(boolean full) {
        if (full) {
            setAll();
        }
    }

    @Override
    public SudokuSetBase clone() {
        SudokuSetBase newSet = null;
        try {
            newSet = (SudokuSetBase) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error while cloning", ex);
        }
        return newSet;
    }

    @Override
    public int compareTo(SudokuSetBase o) {
        long lRet = mask2 - o.mask2;
        if (lRet == 0) {
            lRet = mask1 - o.mask1;
        }
        int ret = 0;
        if (lRet < 0) {
            ret = -1;
        } else if (lRet > 0) {
            ret = 1;
        }
        return ret;
    }

    public boolean isEmpty() {
        return (mask1 == 0) && (mask2 == 0);
    }

    public void add(int value) {
        // Bitmap
        if (value >= 64) {
            mask2 |= MASKS[value - 64];
        } else {
            mask1 |= MASKS[value];
        }
        initialized = false;
    }

    public void remove(int value) {
        // Bitmap
        if (value >= 64) {
            mask2 &= ~MASKS[value - 64];
        } else {
            mask1 &= ~MASKS[value];
        }
        initialized = false;
    }

    public final void set(SudokuSetBase set) {
        mask1 = set.mask1;
        mask2 = set.mask2;
        initialized = false;
    }

    public void set(int[] data) {
        mask1 = 0;
        mask2 = 0;
        initialized = false;

        for (int i = 0; i < data.length; i++) {
            add(data[i]);
        }
    }

    public void set(long m1, long m2) {
        mask1 = m1;
        mask2 = m2;
        initialized = false;
    }

    /**
     * Sets only the first 32 bits of the set.
     * 
     * @param data
     */
    public void set(int data) {
        mask1 = data;
        mask2 = 0;
        initialized = false;
    }

    public boolean contains(int value) {
        if (value >= 64) {
            return (mask2 & MASKS[value - 64]) != 0;
        } else {
            return (mask1 & MASKS[value]) != 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (! (o instanceof SudokuSetBase)) {
            return false;
        }
        SudokuSetBase s = (SudokuSetBase) o;
        return mask1 == s.mask1 && mask2 == s.mask2;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + (int) (this.mask1 ^ (this.mask1 >>> 32));
        hash = 71 * hash + (int) (this.mask2 ^ (this.mask2 >>> 32));
        return hash;
    }

    public void clear() {
        mask1 = mask2 = 0;
        initialized = false;
    }

    public final void setAll() {
        mask1 = MAX_MASK1;
        mask2 = MAX_MASK2;
        initialized = false;
    }

    public boolean intersects(SudokuSetBase b) {
        if ((mask1 & b.mask1) != 0 || (mask2 & b.mask2) != 0) {
            return true;
        }
        return false;
    }

    /**
     * Wenn this und b sich überschneiden, werden die gemeinsamen Kandidaten in c hinzugefügt
     * @param b
     * @param c
     * @return  
     */
    public boolean intersects(SudokuSetBase b, SudokuSetBase c) {
        boolean result = false;
        long mask = mask1 & b.mask1;
        if (mask != 0) {
            result = true;
            c.mask1 |= mask;
            c.initialized = false;
        }
        mask = mask2 & b.mask2;
        if (mask != 0) {
            result = true;
            c.mask2 |= mask;
            c.initialized = false;
        }
        return result;
    }

    /**
     * Gibt true zurück, wenn b zur Gänze in this enthalten ist
     * @param b
     * @return  
     */
    public boolean contains(SudokuSetBase b) {
        return (b.mask1 & ~mask1) == 0 && (b.mask2 & ~mask2) == 0;
    }

    public void or(SudokuSetBase b) {
        mask1 |= b.mask1;
        mask2 |= b.mask2;
        initialized = false;
    }

    public void orNot(SudokuSetBase set) {
        mask1 |= ~set.mask1;
        mask2 |= ~set.mask2;
        initialized = false;
    }

    public void and(SudokuSetBase set) {
        mask1 &= set.mask1;
        mask2 &= set.mask2;
        initialized = false;
    }

    public void andNot(SudokuSetBase set) {
        mask1 &= ~set.mask1;
        mask2 &= ~set.mask2;
        initialized = false;
    }

    /**
     * gibt ((this & set) == this) zurück
     * @param set
     * @return  
     */
    public boolean andEquals(SudokuSetBase set) {
        long m1 = mask1 & set.mask1;
        long m2 = mask2 & set.mask2;
        return (m1 == mask1 && m2 == mask2);
    }

    /**
     * gibt ((this & ~set) == this) zurück
     * @param set
     * @return  
     */
    public boolean andNotEquals(SudokuSetBase set) {
        long m1 = mask1 & ~set.mask1;
        long m2 = mask2 & ~set.mask2;
        return (m1 == mask1 && m2 == mask2);
    }

    /**
     * gibt ((this & set) == 0) zurück
     * @param set
     * @return  
     */
    public boolean andEmpty(SudokuSetBase set) {
        long m1 = mask1 & set.mask1;
        long m2 = mask2 & set.mask2;
        return (m1 == 0 && m2 == 0);
    }

//    /**
//     * gibt ((this & ~set) == 0) zurück
//     */
//    public boolean andNotEmpty(SudokuSetBase set) {
//        long m1 = mask1 & ~set.mask1;
//        long m2 = mask2 & ~set.mask2;
//        return (m1 == 0 && m2 == 0);
//    }
    public void not() {
        mask1 = ~mask1;
        mask2 = (~mask2 & MAX_MASK2);
        initialized = false;
    }

    /**
     * Calculates this | (s1 & s2);
     * 
     * @param s1
     * @param s2
     */
    public void orAndAnd(SudokuSetBase s1, SudokuSetBase s2) {
        mask1 |= (s1.mask1 & s2.mask1);
        mask2 |= (s1.mask2 & s2.mask2);
        initialized = false;
    }

    /**
     * Calculates this = (s1 & s2)
     * @param s1
     * @param s2
     */
    public void setAnd(SudokuSetBase s1, SudokuSetBase s2) {
        mask1 = (s1.mask1 & s2.mask1);
        mask2 = (s1.mask2 & s2.mask2);
        initialized = false;
    }

    /**
     * Calculates this = (s1 & s2) and returns this.isEmpty()
     * @param s1
     * @param s2
     * @return  
     */
    public static boolean andEmpty(SudokuSetBase s1, SudokuSetBase s2) {
        return ((s1.mask1 & s2.mask1) == 0 && (s1.mask2 & s2.mask2) == 0);
    }

    /**
     * Calculates this = (s1 | s2)
     * @param s1
     * @param s2
     */
    public void setOr(SudokuSetBase s1, SudokuSetBase s2) {
//        System.out.println("    setOr");
//        System.out.println("    " + pM(s1.mask1));
//        System.out.println("    " + pM(s2.mask1));
//        System.out.println("    " + pM(s1.mask1|s2.mask1));
//        System.out.println("    " + pM(s1.mask2));
//        System.out.println("    " + pM(s2.mask2));
//        System.out.println("    " + pM(s1.mask2|s2.mask2));
        mask1 = (s1.mask1 | s2.mask1);
        mask2 = (s1.mask2 | s2.mask2);
        initialized = false;
    }

    protected String pM(long mask) {
        return Long.toHexString(mask);
    }

    @Override
    public String toString() {
        int[] values = new int[81];
        int anz = 0;
        int index = 0;
        for (int i = 0; i < 64; i++) {
            if ((mask1 & MASKS[i]) != 0) {
                values[index++] = i;
            }
        }
        for (int i = 0; i < 17; i++) {
            if ((mask2 & MASKS[i]) != 0) {
                values[index++] = i + 64;
            }
        }
        initialized = true;
        anz = index;
        if (anz == 0) {
            return "empty!";
        }
        StringBuilder tmp = new StringBuilder();
        tmp.append(Integer.toString(values[0]));
        for (int i = 1; i < anz; i++) {
            tmp.append(" ").append(Integer.toString(values[i]));
        }
        tmp.append(" ").append(pM(mask1)).append("/").append(pM(mask2));
        return tmp.toString();
    }

    public static void main(String[] args) {
        SudokuSetBase set = new SudokuSetBase();
        for (int i = 0; i < 81; i++) {
            set.add(i);
        }
        System.out.println(set);
    }

    public long getMask1() {
        return mask1;
    }

    public void setMask1(long mask1) {
        this.mask1 = mask1;
    }

    public long getMask2() {
        return mask2;
    }

    public void setMask2(long mask2) {
        this.mask2 = mask2;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
