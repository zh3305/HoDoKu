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
 *
 * @author hobiwan
 */
public enum SolutionCategory {
    SINGLES("Singles"),
    INTERSECTIONS("Intersections"),
    SUBSETS("Subsets"),
    BASIC_FISH("Basic Fish"),
    FINNED_BASIC_FISH("(Sashimi) Finned Fish"),
    FRANKEN_FISH("Franken Fish"),
    FINNED_FRANKEN_FISH("Finned Franken Fish"),
    MUTANT_FISH("Mutant Fish"),
    FINNED_MUTANT_FISH("Finned Mutant Fish"),
    SINGLE_DIGIT_PATTERNS("Single Digit Patterns"),
    COLORING("Coloring"),
    UNIQUENESS("Uniqueness"),
    CHAINS_AND_LOOPS("Chains and Loops"),
    WINGS("Wings"),
    ALMOST_LOCKED_SETS("Almost Locked Sets"),
    ENUMERATIONS("Enumerations"),
    MISCELLANEOUS("Miscellaneous"),
    LAST_RESORT("Last Resort")
    ;
    
    private String categoryName;
    
    SolutionCategory() {
        // für XMLEncoder
    }
    
    SolutionCategory(String catName) {
        categoryName = catName;
    }

    @Override
    public String toString() {
        return "enum SolutionCategory: " + categoryName;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String name) {
        categoryName = name;
    }
    
    public boolean isFish() {
        if (this == BASIC_FISH || this == FINNED_BASIC_FISH ||
                this == FRANKEN_FISH || this == FINNED_FRANKEN_FISH ||
                this == MUTANT_FISH || this == FINNED_MUTANT_FISH) {
            return true;
        }
        return false;
    }
}
