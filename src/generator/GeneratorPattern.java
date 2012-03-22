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
package generator;

import java.util.Arrays;
import sudoku.Sudoku2;

/**
 * A pattern, that indicates, which cells should contain givens
 * when generating new puzzles.<br><br>
 * 
 * <b>Caution:</b> The setter for {@link #pattern} only sets
 * the reference, the constructor actually makes a copy of
 * the pattern, that has been passed in. When working with
 * new patterns, only the constructore should be used, the
 * setter is used internally by <code>XmlDecoder</code>.
 * 
 * @author hobiwan
 */
public class GeneratorPattern implements Cloneable {
    /** One entry per cell; if it is <code>true</code>, the cell must be a given. */
    private boolean[] pattern = new boolean[Sudoku2.LENGTH];
    /** The name of the pattern. */
    private String name = "";
    /** Patterns must be tested befor they can be applied. the result of the test is stored here. */
    private boolean valid = false;
    
    /**
     * Default constructor.
     */
    public GeneratorPattern() {
        // does nothing, only for XmlDecoder
    }
    
    /**
     * Constructor: Makes a new pattern with a given name.
     * 
     * @param name
     */
    public GeneratorPattern(String name) {
        this.name = name;
    }

    /**
     * Constructor: Makes a copy of the pattern and sets it.
     * 
     * @param name
     * @param pattern 
     */
    public GeneratorPattern(String name, boolean[] pattern) {
        this.name = name;
        this.pattern = Arrays.copyOf(pattern, pattern.length);
    }

    /**
     * Makes a copy of a GeneratorPattern.
     * @return 
     */
    @Override
    public GeneratorPattern clone() {
        GeneratorPattern newPattern = null;
//        try {
            // clone() would result in to many arrays created for nothing,
            // so we just do it ourselves
//            newPattern = (GeneratorPattern) super.clone();
            newPattern = new GeneratorPattern();
            // no deep copy required for name, it is immutable
            newPattern.setName(name);
            newPattern.setValid(valid);
            System.arraycopy(pattern, 0, newPattern.pattern, 0, pattern.length);
//        } catch (CloneNotSupportedException ex) {
//            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error while cloning", ex);
//        }
        return newPattern;
    }
    
    /**
     * Return a printable representation of the pattern.
     * 
     * @return 
     */
    @Override
    public String toString() {
        return name + ": " + Arrays.toString(pattern);
    }

    /**
     * Returns the number of entries in {@link #pattern}, that are <code>true</code>.
     * @return 
     */
    public int getAnzGivens() {
        int anz = 0;
        for (int i = 0; i < pattern.length; i++) {
            if (pattern[i]) {
                anz++;
            }
        }
        return anz;
    }
    
    /**
     * @return the pattern
     */
    public boolean[] getPattern() {
        return pattern;
    }

    /**
     * @param pattern the pattern to set
     */
    public void setPattern(boolean[] pattern) {
        this.pattern = pattern;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @param valid the valid to set
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
}
