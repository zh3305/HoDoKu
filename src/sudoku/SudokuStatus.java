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
 * Describes, if the sudoku is valid or not.<br><br>
 * <ul>
 *  <li><b>EMPTY</b>: No cell has been set</li>
 *  <li><b>INVALID</b>: Cells have been set, but no solution exists</li>
 *  <li><b>VALUID</b>: The sudoku has exactly one solution</li>
 *  <li><b>MULTIPLE_SOLUTIONS</b>: The sudoku is solvable but has multiple solutions</li>
 * </ul>
 * The SudokuStatus doesnt say anything about givens.
 * 
 * @author hobiwan
 */
public enum SudokuStatus {
    EMPTY,
    INVALID,
    VALID,
    MULTIPLE_SOLUTIONS
}
