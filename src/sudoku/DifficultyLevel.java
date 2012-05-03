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

import java.awt.Color;

/**
 *
 * @author hobiwan
 */
public final class DifficultyLevel {
    private DifficultyType type;
    private int ordinal;
    private int maxScore;
    private String name;
    private Color backgroundColor;
    private Color foregroundColor;
    
    public DifficultyLevel() {
        // für XMLEncoder
    }
    
    public DifficultyLevel(DifficultyType type, int maxScore, String name, Color backgroundColor, Color foregroundColor) {
        setType(type);
        setMaxScore(maxScore);
        setName(name);
        setBackgroundColor(backgroundColor);
        setForegroundColor(foregroundColor);
    }
    
    public int getMaxScore() {
        return maxScore;
    }
    
    public String getName() {
        return name;
    }
    
    public Color getBackgroundColor() {
        return backgroundColor;
    }
    
    public Color getForegroundColor() {
        return foregroundColor;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public DifficultyType getType() {
        return type;
    }

    public void setType(DifficultyType type) {
        this.type = type;
        this.ordinal = type.ordinal();
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }
}
