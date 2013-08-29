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

/**
 *
 * @author hobiwan
 */
public class Candidate implements Cloneable, Comparable<Candidate>, Serializable {

    private static final long serialVersionUID = 1L;
    private int value;
    private int index;

    public Candidate() {
    }

    public Candidate(int index, int value) {
        this.index = index;
        this.value = value;
    }

    @Override
    public int compareTo(Candidate o) {
        int ret = value - o.value;
        if (ret == 0) {
            ret = index - o.index;
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Candidate)) {
            return false;
        }
        Candidate c = (Candidate) o;
        if (index == c.index && value == c.value) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.value;
        hash = 29 * hash + this.index;
        return hash;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return index + "/" + value;
    }
}
