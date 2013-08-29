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
import java.util.ArrayList;
import java.util.List;
import solver.Als;

/**
 *
 * @author hobiwan
 */
public class AlsInSolutionStep implements Cloneable, Serializable {

    private static final long serialVersionUID = 1L;
    private List<Integer> indices = new ArrayList<Integer>();
    private List<Integer> candidates = new ArrayList<Integer>();
    private int chainPenalty = -1;

    public AlsInSolutionStep() {
    }

    public void addIndex(int index) {
        indices.add(index);
    }

    public void addCandidate(int cand) {
        candidates.add(cand);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object clone()
            throws CloneNotSupportedException {
        AlsInSolutionStep newAls = (AlsInSolutionStep) super.clone();
        newAls.indices = (List<Integer>) ((ArrayList<Integer>) indices).clone();
        newAls.candidates = (List<Integer>) ((ArrayList<Integer>) candidates).clone();
        return newAls;
    }

    public List<Integer> getIndices() {
        return indices;
    }

    public void setIndices(List<Integer> indices) {
        this.indices = indices;
    }

    public List<Integer> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Integer> candidates) {
        this.candidates = candidates;
    }

    public int getChainPenalty() {
        if (chainPenalty == -1) {
            chainPenalty = Als.getChainPenalty(indices.size());
        }
        return chainPenalty;
    }

    public void setChainPenalty(int chainPenalty) {
        this.chainPenalty = chainPenalty;
    }
}
