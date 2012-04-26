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

import java.awt.Toolkit;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 *
 * @author hobiwan
 */
public class NumbersOnlyDocument extends PlainDocument {

    private static final long serialVersionUID = 1L;

    @Override
    public void insertString(int offs, String str, AttributeSet a)
            throws BadLocationException {

        char[] source = str.toCharArray();
        char[] result = new char[source.length];
        int j = 0;

        for (int i = 0; i < result.length; i++) {
            if (Character.isDigit(source[i])) {
                result[j++] = source[i];
            } else {
                Toolkit.getDefaultToolkit().beep();
                //System.err.println("insertString: " + source[i]);
            }
        }
        super.insertString(offs, new String(result, 0, j), a);
    }
}
