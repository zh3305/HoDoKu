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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * <p>Einfacher Font-Auswahldialog (der Xte). Wird über
 * {@link #showDialog(Dialog,String,Font) showDialog()}
 * aufgerufen.</p>
 *
 * <p>Der Dialog enthält ein einfaches Preview-Panel, in dem
 * das Ergebnis der Auswahl in konstanter Größe angezeigt wird.</p>
 *
 * <p>Der Dialog verwendet NullLayout, es kann daher zu Anzeigeproblemen
 * kommen, wenn nicht das aktuelle Windows-PlugAndFeel des JRE 1.5
 * verwendet wird.</p>
 * 
 * @author hobiwan
 */
@SuppressWarnings("serial")
public class MyFontChooser extends javax.swing.JDialog implements ListSelectionListener {

    /**
     * Nur eine Instanz für alle Aufrufe.
     */
    static MyFontChooser chooser = null;
    private static final long serialVersionUID = 1L;
    /**
     * Der aktuelle gewählte Font. Wird im Konstruktor mit einem vorgegebenen Font
     * initialisiert. Wird "Abbrechen" gedrückt, wird er mit <CODE>null</CODE> überschrieben.
     */
    private Font font;
    /**
     * Statisches Array, das die Fontnamen aller im System installierten Schriften enthält.
     * Das Array wird befüllt, wenn der Dialog zum ersten Mal aufgerufen wird.
     */
    static private String[] fontNames = null;
    /**
     * Array mit den vorhandenen Schriftstilen. Dient zur Initialisierung der
     * entsprechenden Liste.
     */
    static private String[] styles = {
        java.util.ResourceBundle.getBundle("intl/MyFontChooser").getString("MyFontChooser.regular"),
        java.util.ResourceBundle.getBundle("intl/MyFontChooser").getString("MyFontChooser.italic"),
        java.util.ResourceBundle.getBundle("intl/MyFontChooser").getString("MyFontChooser.bold"),
        java.util.ResourceBundle.getBundle("intl/MyFontChooser").getString("MyFontChooser.bold_italic")
    };
    /**
     * Array mit vorgegebenen Größen. Derzeit kann nur aus diesen Größen gewählt werden.
     */
    static private String[] size = {"6", "7", "8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "36", "40", "50", "60"};

    /**
     * Erzeugt den FontChooser.
     * @param parent Referenz auf den aufrufenden Dialog (darf nicht <CODE>null</CODE> sein).
     * @param modal <CODE>true</CODE>, wenn der Dialog modal sein soll, sonst <CODE>false</CODE>.
     */
    @SuppressWarnings("LeakingThisInConstructor")
    private MyFontChooser(Frame owner, boolean modal) {
        super(owner, modal);
        if (fontNames == null) {
            fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        }

        initComponents();

        jliSchriftart.addListSelectionListener(this);
        jliSchriftschnitt.addListSelectionListener(this);
        jliSchriftgrad.addListSelectionListener(this);
        getRootPane().setDefaultButton(jbOK);

        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                jbCancelActionPerformed(e);
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
                put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
    }

    /**
     * Initialisiert die statische Instanz {@link chooser} mit den Werten des
     * übergebenen Fonts und zeigt den Dialog an.
     * @param font Font, der beim Öffnen des Dialogs angezeigt werden soll.
     */
    private void showFontChooser(Font font) {
        this.font = font;
        int index = Arrays.binarySearch(fontNames, font.getName());
        if (index >= 0) {
            jliSchriftart.setSelectedIndex(index);
            jliSchriftart.ensureIndexIsVisible(index);
        }
        int style = font.getStyle();
        if (style == Font.PLAIN) {
            index = 0;
        } else if (style == Font.ITALIC) {
            index = 1;
        } else if (style == Font.BOLD) {
            index = 2;
        } else {
            index = 3;
        }
        jliSchriftschnitt.setSelectedIndex(index);
        jliSchriftschnitt.ensureIndexIsVisible(index);
        int actSize = font.getSize();
        index = Arrays.binarySearch(size, String.valueOf(actSize));
        if (index < 0) {
            index = 6;
        }
        jliSchriftgrad.setSelectedIndex(index);
        jliSchriftgrad.ensureIndexIsVisible(index);
        setVisible(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jlSchriftart = new javax.swing.JLabel();
        jspSchriftart = new javax.swing.JScrollPane();
        jliSchriftart = new javax.swing.JList(fontNames);
        jlSchriftschnitt = new javax.swing.JLabel();
        jspSchriftschnitt = new javax.swing.JScrollPane();
        jliSchriftschnitt = new javax.swing.JList(styles);
        jlSchriftgrad = new javax.swing.JLabel();
        jspSchriftgrad = new javax.swing.JScrollPane();
        jliSchriftgrad = new javax.swing.JList(size);
        jpDemo = new javax.swing.JPanel() {
            protected void paintComponent(Graphics g) {
                updateDemoPanel(g);
            }
        };
        jbCancel = new javax.swing.JButton();
        jbOK = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("intl/MyFontChooser"); // NOI18N
        setTitle(bundle.getString("MyFontChooser.title")); // NOI18N

        jlSchriftart.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("intl/MyFontChooser").getString("MyFontChooser.jlSchriftart.mnemonic").charAt(0));
        jlSchriftart.setLabelFor(jliSchriftart);
        jlSchriftart.setText(bundle.getString("MyFontChooser.jlSchriftart.text")); // NOI18N

        jspSchriftart.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jliSchriftart.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jspSchriftart.setViewportView(jliSchriftart);

        jlSchriftschnitt.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("intl/MyFontChooser").getString("MyFontChooser.jlSchriftSchnitt.mnemonic").charAt(0));
        jlSchriftschnitt.setLabelFor(jliSchriftschnitt);
        jlSchriftschnitt.setText(bundle.getString("MyFontChooser.jlSchriftschnitt.text")); // NOI18N

        jspSchriftschnitt.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jspSchriftschnitt.setViewportView(jliSchriftschnitt);

        jlSchriftgrad.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("intl/MyFontChooser").getString("MyFontChooser.jlSchriftgrad.mnemonic").charAt(0));
        jlSchriftgrad.setLabelFor(jliSchriftgrad);
        jlSchriftgrad.setText(bundle.getString("MyFontChooser.jlSchriftgrad.text")); // NOI18N

        jspSchriftgrad.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jspSchriftgrad.setViewportView(jliSchriftgrad);

        jpDemo.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("MyFontChooser.jpDemo.border.title"))); // NOI18N

        jbCancel.setMnemonic(java.util.ResourceBundle.getBundle("intl/MyFontChooser").getString("MyFontChooser.jbCancel.mnemonic").charAt(0));
        jbCancel.setText(bundle.getString("MyFontChooser.jbCancel.text")); // NOI18N
        jbCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbCancelActionPerformed(evt);
            }
        });

        jbOK.setMnemonic(java.util.ResourceBundle.getBundle("intl/MyFontChooser").getString("MyFontChooser.jbOK.mnemonic").charAt(0));
        jbOK.setText(bundle.getString("MyFontChooser.jbOK.text")); // NOI18N
        jbOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbOKActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jlSchriftart)
                                .addComponent(jspSchriftart, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(5, 5, 5)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jlSchriftschnitt)
                                .addComponent(jspSchriftschnitt, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(5, 5, 5)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jlSchriftgrad)
                                .addComponent(jspSchriftgrad, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE))
                            .addContainerGap())
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addComponent(jpDemo, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                            .addGap(10, 10, 10)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jbOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jbCancel)
                        .addContainerGap())))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jbCancel, jbOK});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jlSchriftart)
                    .addComponent(jlSchriftschnitt)
                    .addComponent(jlSchriftgrad))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jspSchriftart, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jspSchriftschnitt, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jspSchriftgrad, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpDemo, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbCancel, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
                    .addComponent(jbOK, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Gibt die Resourcen des Dialogs wieder frei.
     * @param evt Auslösendes Event.
     */
    private void jbOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbOKActionPerformed
        setVisible(false);
    }//GEN-LAST:event_jbOKActionPerformed

    /**
     * Setzt den aktuell gewählten {@link #font} auf <CODE>null</CODE> und gibt die
     * Dialog-Resourcen wieder frei.
     * @param evt Auslösendes Event.
     */
    private void jbCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbCancelActionPerformed
        font = null;
        setVisible(false);
    }//GEN-LAST:event_jbCancelActionPerformed

    /**
     * <p>Über diese Methode wird ein MyFontChooser angezeigt. Da das Konstruieren eines
     * JFonChooser-Objekts zeitaufwendig ist, existiert pro Prozess nur eine Instanz,
     * die beim ersten Aufruf erzeugt wird.</p>
     * 
     * @param owner 
     * @param title Titel des Dialogs.
     * @param initialFont Font, der beim Öffnen des Dialogs angezeigt werden soll. Wird <CODE>null</CODE>
     * übergeben, wird der Standard-Font für Buttons verwendet.
     * @return Der gewählte Font bzw. <CODE>null</CODE>, wenn "Abbrechen" gedrückt wurde.
     */
    public static Font showDialog(Frame owner, String title, Font initialFont) {
        if (initialFont == null) {
            initialFont = UIManager.getFont("Button.font");
        }
        if (chooser == null) {
            chooser = new MyFontChooser(owner, true);
        }
        chooser.setTitle(title);
        chooser.showFontChooser(initialFont);
        return chooser.font;
    }

    /**
     * Passt {@link #font} und das Demo-Panel an, wenn in einer Liste eine Selektion
     * vorgenommen wurde.
     * @param e Auslösendes Event.
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        String name = (String) jliSchriftart.getSelectedValue();
        int styleIndex = jliSchriftschnitt.getSelectedIndex();
        String value = (String) jliSchriftgrad.getSelectedValue();
        int tmpSize = 12;
        if (value != null) {
            tmpSize = Integer.parseInt(value);
        }
        int style = Font.PLAIN;
        switch (styleIndex) {
            case 0:
                style = Font.PLAIN;
                break;
            case 1:
                style = Font.ITALIC;
                break;
            case 2:
                style = Font.BOLD;
                break;
            case 3:
                style = Font.ITALIC + Font.BOLD;
                break;
        }
        //System.out.println("font: " + font + ", name: " + name);
        if (name != null && font != null && (!name.equals(font.getName()) || tmpSize != font.getSize() || style != font.getStyle())) {
            font = new Font(name, style, tmpSize);
            jpDemo.repaint();
        }
    }

    /**
     * <p>{@link jpDemoPanel} wird mit einer anonymen Klasse instantiiert, die
     * <CODE>paintComponent()</CODE> so überschreibt, das <CODE>updateDemoPanel()</CODE>
     * aufgerufen wird, wenn das Demo-Panel neu gezeichnet werden soll.</p>
     *
     * <p>Zeichnet eine Linie, die die Baseline des aktuellen Fonts repräsentiert, und
     * gibt den Namen des Fonts im aktuell gewählten Schriftschnitt aus.</p>
     * @param g <CODE>Graphics2D</CODE> zum Zeichnen des Panels.
     */
    private void updateDemoPanel(Graphics g) {
        Graphics2D gr = (Graphics2D) g;
        gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        Insets insets = jpDemo.getInsets();
        Rectangle dim = jpDemo.getBounds();
        Color tmpCol = gr.getColor();
        gr.setColor(jpDemo.getBackground());
        gr.fillRect(0, 0, jpDemo.getWidth(), jpDemo.getHeight());
        gr.setColor(tmpCol);
        gr.setFont(new Font(font.getName(), font.getStyle(), 24));
        FontMetrics metrics = gr.getFontMetrics();
        int yBase = (dim.height + metrics.getHeight()) / 2;
        gr.drawLine(insets.left, yBase, dim.width - insets.right, yBase);
        //String text = "    " + font.getName() + "    ";
        String text = "    " + "123456789" + "    ";
        Shape oldClip = gr.getClip();
        gr.setClip(insets.left, insets.top, dim.width - insets.left - insets.right, dim.height - insets.top - insets.bottom);
        gr.drawString(text, (jpDemo.getWidth() - metrics.stringWidth(text)) / 2, yBase);
        gr.setClip(oldClip);
    }

    /**
     * Zum Testen...
     * @param args the command line arguments
     */
    public static void main(String args[]) {
//        Font font = MyFontChooser.showDiaJFontChoosersttitel", null);
        Font font = MyFontChooser.showDialog(null, "Testtitel", new Font("Arial", Font.ITALIC, 14));
        System.out.println(font);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jbCancel;
    private javax.swing.JButton jbOK;
    private javax.swing.JLabel jlSchriftart;
    private javax.swing.JLabel jlSchriftgrad;
    private javax.swing.JLabel jlSchriftschnitt;
    private javax.swing.JList jliSchriftart;
    private javax.swing.JList jliSchriftgrad;
    private javax.swing.JList jliSchriftschnitt;
    private javax.swing.JPanel jpDemo;
    private javax.swing.JScrollPane jspSchriftart;
    private javax.swing.JScrollPane jspSchriftgrad;
    private javax.swing.JScrollPane jspSchriftschnitt;
    // End of variables declaration//GEN-END:variables
}
