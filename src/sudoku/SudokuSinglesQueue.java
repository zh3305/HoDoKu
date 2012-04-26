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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A queue that can hold possible Singles (naked or hidden). For Hidden Singles
 * the unit is not stored, Full houses are treated as Naked Singles. <br>
 * Since every (Hidden) Single can appear in three houses, the maximum size
 * of the queue is three times the number of cells in the Sudoku.<br>
 * The queue is not built as ring buffer, but the indices are reset to 0
 * every time the queue is empty.<br>
 * It is possible to delete Singles from the queue. This is used by class
 * {@link Sudoku2} when setting candidates or deleting values from cells.
 *
 * @author hobiwan
 */
public class SudokuSinglesQueue implements Cloneable {
    /** The indices of newly detected Singles */
    private int[] indices = new int[Sudoku2.LENGTH * 3];
    /** The values of newly detected Singles */
    private int[] values = new int[Sudoku2.LENGTH * 3];
    /** The index of the next Single to put into the queue. */
    private int putIndex = 0;
    /** The index of the next Single to get from the queue. */
    private int getIndex = 0;
    /** An index for iterating the queue without removing Singles. */
    private int iterateIndex = 0;

    /** Construct a new SinglesQueue. */
    public SudokuSinglesQueue() {
        // do nothing...
    }

    /**
     * Clones a SudokuSinglesQueue. Makes a valid deep copy.
     *
     * @return
     */
    @Override
    public SudokuSinglesQueue clone() {
        SudokuSinglesQueue newSudokuSinglesQueue = null;
        try {
            newSudokuSinglesQueue = (SudokuSinglesQueue) super.clone();
            newSudokuSinglesQueue.indices = indices.clone();
            newSudokuSinglesQueue.values = values.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error while cloning", ex);
        }
        return newSudokuSinglesQueue;
    }

    /**
     * Set the queue with the values of queue <code>src</code>.
     * @param src
     */
    public void set(SudokuSinglesQueue src) {
        System.arraycopy(src.indices, 0, indices, 0, indices.length);
        System.arraycopy(src.values, 0, values, 0, values.length);
        getIndex = src.getIndex;
        putIndex = src.putIndex;
        iterateIndex = src.iterateIndex;
    }

    /**
     * Checks if the queue is empty.
     * @return
     */
    public boolean isEmpty() {
        return getIndex >= putIndex;
    }

    /**
     * Add a new Single to the queue
     * @param index
     * @param value
     */
    public void addSingle(int index, int value) {
//        System.out.println("    add Single " + index + "/" + value + " at pos " + putIndex);
        indices[putIndex] = index;
        values[putIndex++] = value;
    }

    /**
     * Gets the index of the next Single or -1, if the
     * queue is empty. The index is incremented, which
     * removes the oldest Single from the queue.
     * @return
     */
    public int getSingle() {
        if (getIndex >= putIndex) {
            // queue is empty
            return -1;
        }
        int ret = getIndex++;
        if (getIndex >= putIndex) {
            getIndex = putIndex = 0;
        }
        return ret;
    }

    /**
     * Returns the cell index at queue position <code>queueIndex</code>.
     * @param queueIndex
     * @return
     */
    public int getIndex(int queueIndex) {
        return indices[queueIndex];
    }

    /**
     * Returns the cell value at queue position <code>queueIndex</code>.
     * @param queueIndex
     * @return
     */
    public int getValue(int queueIndex) {
        return values[queueIndex];
    }

    /**
     * Used together with {@link #getNextIndex() } to iterate the queue
     * without removing elements. If the queue is empty, -1 is returned.
     * @return
     */
    public int getFirstIndex() {
        iterateIndex = getIndex;
        return getNextIndex();
    }

    /**
     * Used together with {@link #getFirstIndex() } to iterate the queue
     * without removing elements. If the queue is empty, -1 is returned.
     * The method does not check for concurrent modifications of the queue!
     * @return
     */
    public int getNextIndex() {
        if (iterateIndex >= putIndex) {
            return -1;
        }
        return iterateIndex++;
    }

    /**
     * Checks, if the queue contains an entry for the cell with index
     * <code>index</code>. If an entry is found, it is deleted.
     *
     * @param index
     */
    public void deleteNakedSingle(int index) {
        for (int i = getIndex; i < putIndex; i++) {
            if (indices[i] == index) {
                for (int j = i + 1; j < putIndex; j++) {
                    indices[j - 1] = indices[j];
                    values[j - 1] = values[j];
                }
                putIndex--;
                break;
            }
        }
    }

    /**
     * Checks, if the queue contains an entry for a
     * cell within constraint <code>constraint</code> for candidate
     * <code>value</code>. If it does, that entry is removed.
     *
     * @param constraint
     * @param value  
     */
    public void deleteHiddenSingle(int constraint, int value) {
        for (int i = getIndex; i < putIndex; i++) {
            int actIndex = indices[i];
            if (values[i] == value && (Sudoku2.CONSTRAINTS[actIndex][0] == constraint ||
                    Sudoku2.CONSTRAINTS[actIndex][1] == constraint ||
                    Sudoku2.CONSTRAINTS[actIndex][2] == constraint)) {
                for (int j = i + 1; j < putIndex; j++) {
                    indices[j - 1] = indices[j];
                    values[j - 1] = values[j];
                }
                putIndex--;
                break;
            }
        }
    }

    /**
     * Deletes all entries in the queue.
     */
    public void clear() {
//        System.out.println("  Queue cleared!");
        getIndex = putIndex = 0;
    }

    /**
     * Return a formatted String containing the contents of the queue.
     * For debugging only.
     * 
     * @return 
     */
    @Override
    public String toString() {
        StringBuilder tmp = new StringBuilder();
        tmp.append("Singles Queue START\r\n");
        for (int i = getIndex; i < putIndex; i++) {
            tmp.append("   ").append(indices[i]).append("/").append(values[i]).append("\r\n");
        }
        tmp.append("Singles Queue END\r\n");
        return tmp.toString();
    }
}
