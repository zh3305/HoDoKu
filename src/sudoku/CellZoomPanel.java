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

/* Default dimensions for:
 *
 *         System.out.println(getDimensions(setValueLabel));
 *         System.out.println(getDimensions(setValuePanel));
 *         System.out.println(getDimensions(toggleCandidatesLabel));
 *         System.out.println(getDimensions(toggleCandidatesPanel));
 *         System.out.println(getDimensions(colorCellsLabel));
 *         System.out.println(getDimensions(cellColorPanel));
 *         System.out.println(getDimensions(chooseCellColorPanel));
 *         System.out.println(getDimensions(chooseCandidateColorLabel));
 *         System.out.println(getDimensions(chooseCandidateColorPanel));
 * 
 * null: 10/33/java.awt.Dimension[width=220,height=14]
 * null: 10/53/java.awt.Dimension[width=103,height=94]
 * null: 10/165/java.awt.Dimension[width=220,height=14]
 * null: 10/185/java.awt.Dimension[width=103,height=95]
 * null: 10/298/java.awt.Dimension[width=220,height=14]
 * null: 10/318/java.awt.Dimension[width=45,height=54]
 * null: 65/318/java.awt.Dimension[width=115,height=54]
 * null: 10/390/java.awt.Dimension[width=220,height=14]
 * null: 65/410/java.awt.Dimension[width=115,height=56]
 */
package sudoku;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ResourceBundle;
import java.util.SortedMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 *
 * @author hobiwan
 */
@SuppressWarnings("serial")
public class CellZoomPanel extends javax.swing.JPanel {

    private static final int X_OFFSET = 10;
    private static final int Y_OFFSET = 33;
    private static final int SMALL_GAP = 6;
    private static final int LARGE_GAP = 14;
    private static final int COLOR_PANEL_MAX_HEIGHT = 50;
    private static final int DIFF_SIZE = 1;
    private static final String[] NUMBERS = new String[]{
        "1", "2", "3", "4", "5", "6", "7", "8", "9"
    };
    private MainFrame mainFrame;
    private Font buttonFont = null;
    private Font iconFont = null;
    private int buttonFontSize = -1;
    private int defaultButtonFontSize = -1;
    private int defaultButtonHeight = -1;
    private JButton[] setValueButtons = null;
    private JButton[] toggleCandidatesButtons = null;
    private JPanel[] cellPanels = null;
    private JPanel[] candidatePanels = null;
    private Color normButtonForeground = null;
    private Color normButtonBackground = null;
    private int aktColor = -1;
    private SudokuPanel sudokuPanel;
    private int colorImageHeight = -1;
    private Icon[] colorKuIcons = new Icon[9];

    /** Creates new form CellZoomPanel
     * @param mainFrame 
     */
    public CellZoomPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        initComponents();

        setValueButtons = new JButton[]{
            setValueButton1, setValueButton2, setValueButton3,
            setValueButton4, setValueButton5, setValueButton6,
            setValueButton7, setValueButton8, setValueButton9
        };
        toggleCandidatesButtons = new JButton[]{
            toggleCandidatesButton1, toggleCandidatesButton2, toggleCandidatesButton3,
            toggleCandidatesButton4, toggleCandidatesButton5, toggleCandidatesButton6,
            toggleCandidatesButton7, toggleCandidatesButton8, toggleCandidatesButton9
        };
        normButtonForeground = setValueButton1.getForeground();
        normButtonBackground = setValueButton1.getBackground();

        cellPanels = new JPanel[]{
            chooseCellColorM2Panel, chooseCellColorM1Panel, chooseCellColor0Panel,
            chooseCellColor1Panel, chooseCellColor2Panel, chooseCellColor3Panel,
            chooseCellColor4Panel, chooseCellColor5Panel, chooseCellColor6Panel,
            chooseCellColor7Panel, chooseCellColor8Panel, chooseCellColor9Panel
        };

        candidatePanels = new JPanel[]{
            chooseCandidateColorM2Panel, chooseCandidateColorM1Panel, chooseCandidateColor0Panel,
            chooseCandidateColor1Panel, chooseCandidateColor2Panel, chooseCandidateColor3Panel,
            chooseCandidateColor4Panel, chooseCandidateColor5Panel, chooseCandidateColor6Panel,
            chooseCandidateColor7Panel, chooseCandidateColor8Panel, chooseCandidateColor9Panel
        };

        jFontButton.setVisible(false);
        buttonFont = jFontButton.getFont();
//        buttonFontSize = buttonFont.getSize();
        buttonFontSize = 11;
        defaultButtonFontSize = buttonFontSize;
        // the height of the button is not adjusted when the font changes
        // -> calculate the height from the font height
//        FontMetrics metrics = getFontMetrics(buttonFont);
//        defaultButtonHeight = jFontButton.getHeight();
        defaultButtonHeight = 23;
        iconFont = new Font(buttonFont.getName(), buttonFont.getStyle(), defaultButtonFontSize - DIFF_SIZE);
//        System.out.println("jFOntButton: " + defaultButtonHeight + "/" + metrics.getHeight() + "/" + buttonFontSize + "/" + buttonFont + "/" + iconFont);

        int fontSize = 12;
        if (getFont().getSize() > 12) {
            fontSize = getFont().getSize();
        }
        Font font = titleLabel.getFont();
        titleLabel.setFont(new Font(font.getName(), Font.BOLD, fontSize));

        calculateLayout();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        titleLabel = new javax.swing.JLabel();
        setValueLabel = new javax.swing.JLabel();
        setValuePanel = new javax.swing.JPanel();
        setValueButton1 = new javax.swing.JButton();
        setValueButton2 = new javax.swing.JButton();
        setValueButton3 = new javax.swing.JButton();
        setValueButton4 = new javax.swing.JButton();
        setValueButton5 = new javax.swing.JButton();
        setValueButton6 = new javax.swing.JButton();
        setValueButton7 = new javax.swing.JButton();
        setValueButton8 = new javax.swing.JButton();
        setValueButton9 = new javax.swing.JButton();
        toggleCandidatesLabel = new javax.swing.JLabel();
        toggleCandidatesPanel = new javax.swing.JPanel();
        toggleCandidatesButton1 = new javax.swing.JButton();
        toggleCandidatesButton2 = new javax.swing.JButton();
        toggleCandidatesButton3 = new javax.swing.JButton();
        toggleCandidatesButton4 = new javax.swing.JButton();
        toggleCandidatesButton5 = new javax.swing.JButton();
        toggleCandidatesButton6 = new javax.swing.JButton();
        toggleCandidatesButton7 = new javax.swing.JButton();
        toggleCandidatesButton8 = new javax.swing.JButton();
        toggleCandidatesButton9 = new javax.swing.JButton();
        cellColorLabel = new javax.swing.JLabel();
        cellColorPanel = new javax.swing.JPanel();
        chooseCellColorPanel = new javax.swing.JPanel();
        chooseCellColor0Panel = new StatusColorPanel(0);
        chooseCellColor2Panel = new StatusColorPanel(2);
        chooseCellColor4Panel = new StatusColorPanel(4);
        chooseCellColor6Panel = new StatusColorPanel(6);
        chooseCellColor8Panel = new StatusColorPanel(8);
        chooseCellColorM1Panel = new StatusColorPanel(-1);
        chooseCellColor1Panel = new StatusColorPanel(1);
        chooseCellColor3Panel = new StatusColorPanel(3);
        chooseCellColor5Panel = new StatusColorPanel(5);
        chooseCellColor7Panel = new StatusColorPanel(7);
        chooseCellColor9Panel = new StatusColorPanel(9);
        chooseCellColorM2Panel = new StatusColorPanel(-2);
        chooseCandidateColorLabel = new javax.swing.JLabel();
        candidateColorPanel = new javax.swing.JPanel();
        chooseCandidateColorPanel = new javax.swing.JPanel();
        chooseCandidateColor0Panel = new StatusColorPanel(0);
        chooseCandidateColor2Panel = new StatusColorPanel(2);
        chooseCandidateColor4Panel = new StatusColorPanel(4);
        chooseCandidateColor6Panel = new StatusColorPanel(6);
        chooseCandidateColor8Panel = new StatusColorPanel(8);
        chooseCandidateColorM1Panel = new StatusColorPanel(-1);
        chooseCandidateColor1Panel = new StatusColorPanel(1);
        chooseCandidateColor3Panel = new StatusColorPanel(3);
        chooseCandidateColor5Panel = new StatusColorPanel(5);
        chooseCandidateColor7Panel = new StatusColorPanel(7);
        chooseCandidateColor9Panel = new StatusColorPanel(9);
        chooseCandidateColorM2Panel = new StatusColorPanel(-2);
        jFontButton = new javax.swing.JButton();

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        setLayout(null);

        titleLabel.setBackground(new java.awt.Color(0, 51, 255));
        titleLabel.setFont(new java.awt.Font("Tahoma", 1, 12));
        titleLabel.setForeground(new java.awt.Color(255, 255, 255));
        titleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("intl/CellZoomPanel"); // NOI18N
        titleLabel.setText(bundle.getString("CellZoomPanel.titleLabel.text")); // NOI18N
        titleLabel.setOpaque(true);
        add(titleLabel);
        titleLabel.setBounds(0, 0, 63, 15);

        setValueLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        setValueLabel.setText(bundle.getString("CellZoomPanel.setValueLabel.text")); // NOI18N
        add(setValueLabel);
        setValueLabel.setBounds(0, 0, 49, 14);

        setValuePanel.setLayout(new java.awt.GridLayout(3, 3));

        setValueButton1.setText("1");
        setValueButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setValueButton1ActionPerformed(evt);
            }
        });
        setValuePanel.add(setValueButton1);

        setValueButton2.setText("2");
        setValueButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setValueButton1ActionPerformed(evt);
            }
        });
        setValuePanel.add(setValueButton2);

        setValueButton3.setText("3");
        setValueButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setValueButton1ActionPerformed(evt);
            }
        });
        setValuePanel.add(setValueButton3);

        setValueButton4.setText("4");
        setValueButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setValueButton1ActionPerformed(evt);
            }
        });
        setValuePanel.add(setValueButton4);

        setValueButton5.setText("5");
        setValueButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setValueButton1ActionPerformed(evt);
            }
        });
        setValuePanel.add(setValueButton5);

        setValueButton6.setText("6");
        setValueButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setValueButton1ActionPerformed(evt);
            }
        });
        setValuePanel.add(setValueButton6);

        setValueButton7.setText("7");
        setValueButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setValueButton1ActionPerformed(evt);
            }
        });
        setValuePanel.add(setValueButton7);

        setValueButton8.setText("8");
        setValueButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setValueButton1ActionPerformed(evt);
            }
        });
        setValuePanel.add(setValueButton8);

        setValueButton9.setText("9");
        setValueButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setValueButton1ActionPerformed(evt);
            }
        });
        setValuePanel.add(setValueButton9);

        add(setValuePanel);
        setValuePanel.setBounds(0, 0, 117, 69);

        toggleCandidatesLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        toggleCandidatesLabel.setText(bundle.getString("CellZoomPanel.toggleCandidatesLabel.text")); // NOI18N
        add(toggleCandidatesLabel);
        toggleCandidatesLabel.setBounds(0, 0, 93, 14);

        toggleCandidatesPanel.setLayout(new java.awt.GridLayout(3, 3));

        toggleCandidatesButton1.setText("1");
        toggleCandidatesButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleCandidatesButton1ActionPerformed(evt);
            }
        });
        toggleCandidatesPanel.add(toggleCandidatesButton1);

        toggleCandidatesButton2.setText("2");
        toggleCandidatesButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleCandidatesButton1ActionPerformed(evt);
            }
        });
        toggleCandidatesPanel.add(toggleCandidatesButton2);

        toggleCandidatesButton3.setText("3");
        toggleCandidatesButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleCandidatesButton1ActionPerformed(evt);
            }
        });
        toggleCandidatesPanel.add(toggleCandidatesButton3);

        toggleCandidatesButton4.setText("4");
        toggleCandidatesButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleCandidatesButton1ActionPerformed(evt);
            }
        });
        toggleCandidatesPanel.add(toggleCandidatesButton4);

        toggleCandidatesButton5.setText("5");
        toggleCandidatesButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleCandidatesButton1ActionPerformed(evt);
            }
        });
        toggleCandidatesPanel.add(toggleCandidatesButton5);

        toggleCandidatesButton6.setText("6");
        toggleCandidatesButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleCandidatesButton1ActionPerformed(evt);
            }
        });
        toggleCandidatesPanel.add(toggleCandidatesButton6);

        toggleCandidatesButton7.setText("7");
        toggleCandidatesButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleCandidatesButton1ActionPerformed(evt);
            }
        });
        toggleCandidatesPanel.add(toggleCandidatesButton7);

        toggleCandidatesButton8.setText("8");
        toggleCandidatesButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleCandidatesButton1ActionPerformed(evt);
            }
        });
        toggleCandidatesPanel.add(toggleCandidatesButton8);

        toggleCandidatesButton9.setText("9");
        toggleCandidatesButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleCandidatesButton1ActionPerformed(evt);
            }
        });
        toggleCandidatesPanel.add(toggleCandidatesButton9);

        add(toggleCandidatesPanel);
        toggleCandidatesPanel.setBounds(0, 0, 117, 69);

        cellColorLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        cellColorLabel.setText(bundle.getString("CellZoomPanel.colorCellsLabel.text")); // NOI18N
        add(cellColorLabel);
        cellColorLabel.setBounds(0, 0, 105, 14);

        cellColorPanel.setBackground(new java.awt.Color(255, 255, 255));
        cellColorPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        javax.swing.GroupLayout cellColorPanelLayout = new javax.swing.GroupLayout(cellColorPanel);
        cellColorPanel.setLayout(cellColorPanelLayout);
        cellColorPanelLayout.setHorizontalGroup(
            cellColorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 41, Short.MAX_VALUE)
        );
        cellColorPanelLayout.setVerticalGroup(
            cellColorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        add(cellColorPanel);
        cellColorPanel.setBounds(0, 0, 45, 4);

        chooseCellColorPanel.setLayout(new java.awt.GridLayout(2, 6, 1, 1));

        chooseCellColor0Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCellColor0PanelLayout = new javax.swing.GroupLayout(chooseCellColor0Panel);
        chooseCellColor0Panel.setLayout(chooseCellColor0PanelLayout);
        chooseCellColor0PanelLayout.setHorizontalGroup(
            chooseCellColor0PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCellColor0PanelLayout.setVerticalGroup(
            chooseCellColor0PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        chooseCellColorPanel.add(chooseCellColor0Panel);

        chooseCellColor2Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCellColor2PanelLayout = new javax.swing.GroupLayout(chooseCellColor2Panel);
        chooseCellColor2Panel.setLayout(chooseCellColor2PanelLayout);
        chooseCellColor2PanelLayout.setHorizontalGroup(
            chooseCellColor2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCellColor2PanelLayout.setVerticalGroup(
            chooseCellColor2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        chooseCellColorPanel.add(chooseCellColor2Panel);

        chooseCellColor4Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCellColor4PanelLayout = new javax.swing.GroupLayout(chooseCellColor4Panel);
        chooseCellColor4Panel.setLayout(chooseCellColor4PanelLayout);
        chooseCellColor4PanelLayout.setHorizontalGroup(
            chooseCellColor4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCellColor4PanelLayout.setVerticalGroup(
            chooseCellColor4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        chooseCellColorPanel.add(chooseCellColor4Panel);

        chooseCellColor6Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCellColor6PanelLayout = new javax.swing.GroupLayout(chooseCellColor6Panel);
        chooseCellColor6Panel.setLayout(chooseCellColor6PanelLayout);
        chooseCellColor6PanelLayout.setHorizontalGroup(
            chooseCellColor6PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCellColor6PanelLayout.setVerticalGroup(
            chooseCellColor6PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        chooseCellColorPanel.add(chooseCellColor6Panel);

        chooseCellColor8Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCellColor8PanelLayout = new javax.swing.GroupLayout(chooseCellColor8Panel);
        chooseCellColor8Panel.setLayout(chooseCellColor8PanelLayout);
        chooseCellColor8PanelLayout.setHorizontalGroup(
            chooseCellColor8PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCellColor8PanelLayout.setVerticalGroup(
            chooseCellColor8PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        chooseCellColorPanel.add(chooseCellColor8Panel);

        chooseCellColorM1Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCellColorM1PanelLayout = new javax.swing.GroupLayout(chooseCellColorM1Panel);
        chooseCellColorM1Panel.setLayout(chooseCellColorM1PanelLayout);
        chooseCellColorM1PanelLayout.setHorizontalGroup(
            chooseCellColorM1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCellColorM1PanelLayout.setVerticalGroup(
            chooseCellColorM1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        chooseCellColorPanel.add(chooseCellColorM1Panel);

        chooseCellColor1Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCellColor1PanelLayout = new javax.swing.GroupLayout(chooseCellColor1Panel);
        chooseCellColor1Panel.setLayout(chooseCellColor1PanelLayout);
        chooseCellColor1PanelLayout.setHorizontalGroup(
            chooseCellColor1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCellColor1PanelLayout.setVerticalGroup(
            chooseCellColor1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        chooseCellColorPanel.add(chooseCellColor1Panel);

        chooseCellColor3Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCellColor3PanelLayout = new javax.swing.GroupLayout(chooseCellColor3Panel);
        chooseCellColor3Panel.setLayout(chooseCellColor3PanelLayout);
        chooseCellColor3PanelLayout.setHorizontalGroup(
            chooseCellColor3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCellColor3PanelLayout.setVerticalGroup(
            chooseCellColor3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        chooseCellColorPanel.add(chooseCellColor3Panel);

        chooseCellColor5Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCellColor5PanelLayout = new javax.swing.GroupLayout(chooseCellColor5Panel);
        chooseCellColor5Panel.setLayout(chooseCellColor5PanelLayout);
        chooseCellColor5PanelLayout.setHorizontalGroup(
            chooseCellColor5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCellColor5PanelLayout.setVerticalGroup(
            chooseCellColor5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        chooseCellColorPanel.add(chooseCellColor5Panel);

        chooseCellColor7Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCellColor7PanelLayout = new javax.swing.GroupLayout(chooseCellColor7Panel);
        chooseCellColor7Panel.setLayout(chooseCellColor7PanelLayout);
        chooseCellColor7PanelLayout.setHorizontalGroup(
            chooseCellColor7PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCellColor7PanelLayout.setVerticalGroup(
            chooseCellColor7PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        chooseCellColorPanel.add(chooseCellColor7Panel);

        chooseCellColor9Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCellColor9PanelLayout = new javax.swing.GroupLayout(chooseCellColor9Panel);
        chooseCellColor9Panel.setLayout(chooseCellColor9PanelLayout);
        chooseCellColor9PanelLayout.setHorizontalGroup(
            chooseCellColor9PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCellColor9PanelLayout.setVerticalGroup(
            chooseCellColor9PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        chooseCellColorPanel.add(chooseCellColor9Panel);

        chooseCellColorM2Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCellColorM2PanelLayout = new javax.swing.GroupLayout(chooseCellColorM2Panel);
        chooseCellColorM2Panel.setLayout(chooseCellColorM2PanelLayout);
        chooseCellColorM2PanelLayout.setHorizontalGroup(
            chooseCellColorM2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCellColorM2PanelLayout.setVerticalGroup(
            chooseCellColorM2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        chooseCellColorPanel.add(chooseCellColorM2Panel);

        add(chooseCellColorPanel);
        chooseCellColorPanel.setBounds(0, 0, 113, 3);

        chooseCandidateColorLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        chooseCandidateColorLabel.setText(bundle.getString("CellZoomPanel.chooseCandidateColorLabel.text")); // NOI18N
        add(chooseCandidateColorLabel);
        chooseCandidateColorLabel.setBounds(0, 0, 142, 14);

        candidateColorPanel.setBackground(new java.awt.Color(255, 255, 255));
        candidateColorPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        javax.swing.GroupLayout candidateColorPanelLayout = new javax.swing.GroupLayout(candidateColorPanel);
        candidateColorPanel.setLayout(candidateColorPanelLayout);
        candidateColorPanelLayout.setHorizontalGroup(
            candidateColorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 41, Short.MAX_VALUE)
        );
        candidateColorPanelLayout.setVerticalGroup(
            candidateColorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 40, Short.MAX_VALUE)
        );

        add(candidateColorPanel);
        candidateColorPanel.setBounds(0, 0, 45, 44);

        chooseCandidateColorPanel.setLayout(new java.awt.GridLayout(2, 5, 1, 1));

        chooseCandidateColor0Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCandidateColor0PanelLayout = new javax.swing.GroupLayout(chooseCandidateColor0Panel);
        chooseCandidateColor0Panel.setLayout(chooseCandidateColor0PanelLayout);
        chooseCandidateColor0PanelLayout.setHorizontalGroup(
            chooseCandidateColor0PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCandidateColor0PanelLayout.setVerticalGroup(
            chooseCandidateColor0PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        chooseCandidateColorPanel.add(chooseCandidateColor0Panel);

        chooseCandidateColor2Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCandidateColor2PanelLayout = new javax.swing.GroupLayout(chooseCandidateColor2Panel);
        chooseCandidateColor2Panel.setLayout(chooseCandidateColor2PanelLayout);
        chooseCandidateColor2PanelLayout.setHorizontalGroup(
            chooseCandidateColor2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCandidateColor2PanelLayout.setVerticalGroup(
            chooseCandidateColor2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        chooseCandidateColorPanel.add(chooseCandidateColor2Panel);

        chooseCandidateColor4Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCandidateColor4PanelLayout = new javax.swing.GroupLayout(chooseCandidateColor4Panel);
        chooseCandidateColor4Panel.setLayout(chooseCandidateColor4PanelLayout);
        chooseCandidateColor4PanelLayout.setHorizontalGroup(
            chooseCandidateColor4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCandidateColor4PanelLayout.setVerticalGroup(
            chooseCandidateColor4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        chooseCandidateColorPanel.add(chooseCandidateColor4Panel);

        chooseCandidateColor6Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCandidateColor6PanelLayout = new javax.swing.GroupLayout(chooseCandidateColor6Panel);
        chooseCandidateColor6Panel.setLayout(chooseCandidateColor6PanelLayout);
        chooseCandidateColor6PanelLayout.setHorizontalGroup(
            chooseCandidateColor6PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCandidateColor6PanelLayout.setVerticalGroup(
            chooseCandidateColor6PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        chooseCandidateColorPanel.add(chooseCandidateColor6Panel);

        chooseCandidateColor8Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCandidateColor8PanelLayout = new javax.swing.GroupLayout(chooseCandidateColor8Panel);
        chooseCandidateColor8Panel.setLayout(chooseCandidateColor8PanelLayout);
        chooseCandidateColor8PanelLayout.setHorizontalGroup(
            chooseCandidateColor8PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCandidateColor8PanelLayout.setVerticalGroup(
            chooseCandidateColor8PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        chooseCandidateColorPanel.add(chooseCandidateColor8Panel);

        chooseCandidateColorM1Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCandidateColorM1PanelLayout = new javax.swing.GroupLayout(chooseCandidateColorM1Panel);
        chooseCandidateColorM1Panel.setLayout(chooseCandidateColorM1PanelLayout);
        chooseCandidateColorM1PanelLayout.setHorizontalGroup(
            chooseCandidateColorM1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCandidateColorM1PanelLayout.setVerticalGroup(
            chooseCandidateColorM1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        chooseCandidateColorPanel.add(chooseCandidateColorM1Panel);

        chooseCandidateColor1Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCandidateColor1PanelLayout = new javax.swing.GroupLayout(chooseCandidateColor1Panel);
        chooseCandidateColor1Panel.setLayout(chooseCandidateColor1PanelLayout);
        chooseCandidateColor1PanelLayout.setHorizontalGroup(
            chooseCandidateColor1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCandidateColor1PanelLayout.setVerticalGroup(
            chooseCandidateColor1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        chooseCandidateColorPanel.add(chooseCandidateColor1Panel);

        chooseCandidateColor3Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCandidateColor3PanelLayout = new javax.swing.GroupLayout(chooseCandidateColor3Panel);
        chooseCandidateColor3Panel.setLayout(chooseCandidateColor3PanelLayout);
        chooseCandidateColor3PanelLayout.setHorizontalGroup(
            chooseCandidateColor3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCandidateColor3PanelLayout.setVerticalGroup(
            chooseCandidateColor3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        chooseCandidateColorPanel.add(chooseCandidateColor3Panel);

        chooseCandidateColor5Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCandidateColor5PanelLayout = new javax.swing.GroupLayout(chooseCandidateColor5Panel);
        chooseCandidateColor5Panel.setLayout(chooseCandidateColor5PanelLayout);
        chooseCandidateColor5PanelLayout.setHorizontalGroup(
            chooseCandidateColor5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCandidateColor5PanelLayout.setVerticalGroup(
            chooseCandidateColor5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        chooseCandidateColorPanel.add(chooseCandidateColor5Panel);

        chooseCandidateColor7Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCandidateColor7PanelLayout = new javax.swing.GroupLayout(chooseCandidateColor7Panel);
        chooseCandidateColor7Panel.setLayout(chooseCandidateColor7PanelLayout);
        chooseCandidateColor7PanelLayout.setHorizontalGroup(
            chooseCandidateColor7PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCandidateColor7PanelLayout.setVerticalGroup(
            chooseCandidateColor7PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        chooseCandidateColorPanel.add(chooseCandidateColor7Panel);

        chooseCandidateColor9Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCandidateColor9PanelLayout = new javax.swing.GroupLayout(chooseCandidateColor9Panel);
        chooseCandidateColor9Panel.setLayout(chooseCandidateColor9PanelLayout);
        chooseCandidateColor9PanelLayout.setHorizontalGroup(
            chooseCandidateColor9PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCandidateColor9PanelLayout.setVerticalGroup(
            chooseCandidateColor9PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        chooseCandidateColorPanel.add(chooseCandidateColor9Panel);

        chooseCandidateColorM2Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseCellColor0PanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chooseCandidateColorM2PanelLayout = new javax.swing.GroupLayout(chooseCandidateColorM2Panel);
        chooseCandidateColorM2Panel.setLayout(chooseCandidateColorM2PanelLayout);
        chooseCandidateColorM2PanelLayout.setHorizontalGroup(
            chooseCandidateColorM2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );
        chooseCandidateColorM2PanelLayout.setVerticalGroup(
            chooseCandidateColorM2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        chooseCandidateColorPanel.add(chooseCandidateColorM2Panel);

        add(chooseCandidateColorPanel);
        chooseCandidateColorPanel.setBounds(0, 0, 113, 43);

        jFontButton.setText("FontButton");
        jFontButton.setEnabled(false);
        add(jFontButton);
        jFontButton.setBounds(29, 130, 110, 23);
    }// </editor-fold>//GEN-END:initComponents

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        //System.out.println("CellZoomPanel resized!");
        calculateLayout();
        printSize();
    }//GEN-LAST:event_formComponentResized

    private void setValueButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setValueButton1ActionPerformed
        setValue((JButton) evt.getSource());
    }//GEN-LAST:event_setValueButton1ActionPerformed

    private void toggleCandidatesButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toggleCandidatesButton1ActionPerformed
        handleCandidateChange((JButton) evt.getSource());
    }//GEN-LAST:event_toggleCandidatesButton1ActionPerformed

    private void chooseCellColor0PanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_chooseCellColor0PanelMouseClicked
        handleColorChange((JPanel) evt.getSource());
    }//GEN-LAST:event_chooseCellColor0PanelMouseClicked

    private void handleCandidateChange(JButton button) {
        int candidate = -1;
        for (int i = 0; i < toggleCandidatesButtons.length; i++) {
            if (button == toggleCandidatesButtons[i]) {
                candidate = i + 1;
                break;
            }
        }
        if (sudokuPanel != null && candidate != -1) {
            if (aktColor == -1) {
                sudokuPanel.toggleOrRemoveCandidateFromCellZoomPanel(candidate);
            } else {
                sudokuPanel.handleColoring(candidate);
            }
        }
    }

    private void setValue(JButton button) {
        int number = -1;
        for (int i = 0; i < setValueButtons.length; i++) {
            if (button == setValueButtons[i]) {
                number = i + 1;
                break;
            }
        }
        if (sudokuPanel != null && number != -1) {
            sudokuPanel.setCellFromCellZoomPanel(number);
        }
    }

    private void handleColorChange(JPanel panel) {
        boolean found = false;
        boolean isCell = false;
        int colorNumber = -1;
        for (int i = 0; i < cellPanels.length; i++) {
            if (panel == cellPanels[i]) {
                colorNumber = i - 2; // adjust for -1 and -2
                isCell = true;
                found = true;
                break;
            }
        }
        if (!found) {
            for (int i = 0; i < candidatePanels.length; i++) {
                if (panel == candidatePanels[i]) {
                    colorNumber = i - 2; // adjust for -1 and -2
                    isCell = false;
                    found = true;
                    break;
                }
            }
        }
        if (found && mainFrame != null) {
            //System.out.println("setColoring(): " + colorNumber + "/" + isCell);
            mainFrame.setColoring(colorNumber, isCell);
        }
    }

    public final void calculateLayout() {
        if (defaultButtonHeight == -1) {
            // not yet initialized!
            return;
        }
        int width = getWidth();
        int height = getHeight();
        int y = Y_OFFSET;

        // adjust height and width for the labels
        FontMetrics metrics = getFontMetrics(getFont());
        int textHeight = metrics.getHeight();

        // calculate widths and height of components
        // how much vertical space is actually available?
//        int labelHeight = setValueLabel.getHeight();
//        labelHeight += toggleCandidatesLabel.getHeight();
//        labelHeight += cellColorLabel.getHeight();
//        labelHeight += chooseCandidateColorLabel.getHeight();
        int labelHeight = 4 * textHeight;
        int availableVert = height - Y_OFFSET - 4 * (SMALL_GAP + LARGE_GAP) - labelHeight;

        // try default sizes
        int buttonPanelHeight = availableVert * 2 / 6;
        int colorPanelHeight = availableVert / 6;
        if (colorPanelHeight > COLOR_PANEL_MAX_HEIGHT) {
            colorPanelHeight = COLOR_PANEL_MAX_HEIGHT;
        }
        if (buttonPanelHeight > (width - 2 * X_OFFSET)) {
            buttonPanelHeight = width - 2 * X_OFFSET;
        }
        if (buttonPanelHeight < 120) {
            // adjust color panels
            colorPanelHeight -= (120 - buttonPanelHeight);
            buttonPanelHeight = 120;
        }
        int colorPanelGesWidth = colorPanelHeight * 4;
        if (colorPanelGesWidth > width - 2 * X_OFFSET) {
            colorPanelHeight = (int) ((width - 2 * X_OFFSET) / 4.5);
        }
        colorPanelGesWidth = colorPanelHeight * 4;
        int newColorImageHeight = colorPanelHeight * 2 / 3;

        // ok, do the layout
//        titleLabel.setSize(width, titleLabel.getHeight());
        titleLabel.setSize(width, textHeight);
//        setValueLabel.setSize(width - 2 * X_OFFSET, setValueLabel.getHeight());
        setValueLabel.setSize(width - 2 * X_OFFSET, textHeight);
        setValueLabel.setLocation(X_OFFSET, y);
//        y += setValueLabel.getHeight();
        y += textHeight;
        y += SMALL_GAP;
        setValuePanel.setSize(buttonPanelHeight, buttonPanelHeight);
        setValuePanel.setLocation((width - buttonPanelHeight) / 2, y);
        setValuePanel.doLayout();
        y += buttonPanelHeight;
        y += LARGE_GAP;
//        toggleCandidatesLabel.setSize(width - 2 * X_OFFSET, toggleCandidatesLabel.getHeight());
        toggleCandidatesLabel.setSize(width - 2 * X_OFFSET, textHeight);
        toggleCandidatesLabel.setLocation(X_OFFSET, y);
//        y += toggleCandidatesLabel.getHeight();
        y += textHeight;
        y += SMALL_GAP;
        toggleCandidatesPanel.setSize(buttonPanelHeight, buttonPanelHeight);
        toggleCandidatesPanel.setLocation((width - buttonPanelHeight) / 2, y);
        toggleCandidatesPanel.doLayout();

        int cpx = (width - colorPanelGesWidth) / 2;
//        y = height - 2 * (SMALL_GAP + LARGE_GAP) - cellColorLabel.getHeight() -
//                chooseCandidateColorLabel.getHeight() - 2 * colorPanelHeight;
        y = height - 2 * (SMALL_GAP + LARGE_GAP) - textHeight
                - textHeight - 2 * colorPanelHeight;
//        cellColorLabel.setSize(width - 2 * X_OFFSET, cellColorLabel.getHeight());
        cellColorLabel.setSize(width - 2 * X_OFFSET, textHeight);
        cellColorLabel.setLocation(X_OFFSET, y);
//        y += cellColorLabel.getHeight();
        y += textHeight;
        y += SMALL_GAP;
        cellColorPanel.setSize(colorPanelHeight * 2 / 3, colorPanelHeight * 2 / 3);
        cellColorPanel.setLocation(cpx, y + colorPanelHeight / 6);
        cellColorPanel.doLayout();
        chooseCellColorPanel.setSize((3 * colorPanelHeight), colorPanelHeight);
        chooseCellColorPanel.setLocation(cpx + colorPanelHeight, y);
        chooseCellColorPanel.doLayout();
        y += colorPanelHeight;
        y += LARGE_GAP;
//        chooseCandidateColorLabel.setSize(width - 2 * X_OFFSET, chooseCandidateColorLabel.getHeight());
        chooseCandidateColorLabel.setSize(width - 2 * X_OFFSET, textHeight);
        chooseCandidateColorLabel.setLocation(X_OFFSET, y);
//        y += chooseCandidateColorLabel.getHeight();
        y += textHeight;
        y += SMALL_GAP;
        candidateColorPanel.setSize(colorPanelHeight * 2 / 3, colorPanelHeight * 2 / 3);
        candidateColorPanel.setLocation(cpx, y + colorPanelHeight / 6);
        candidateColorPanel.doLayout();
        chooseCandidateColorPanel.setSize((3 * colorPanelHeight), colorPanelHeight);
        chooseCandidateColorPanel.setLocation(cpx + colorPanelHeight, y);
        chooseCandidateColorPanel.doLayout();

        // set correct font size for buttons
        int newFontSize = defaultButtonFontSize * buttonPanelHeight / (defaultButtonHeight * 4);
        if (newFontSize > 0 && newFontSize != buttonFontSize) {
            //System.out.println("oldFontHeight: " + getFontMetrics(buttonFont).getAscent() + " (" + buttonFontSize + ")");
            buttonFontSize = newFontSize;
            buttonFont = new Font(buttonFont.getName(), buttonFont.getStyle(), buttonFontSize);
            iconFont = new Font(buttonFont.getName(), buttonFont.getStyle(), buttonFontSize - DIFF_SIZE);
            //System.out.println("newFontHeight: " + getFontMetrics(buttonFont).getAscent() + " (" + buttonFontSize + ")");
            for (int i = 0; i < setValueButtons.length; i++) {
                setValueButtons[i].setFont(buttonFont);
                toggleCandidatesButtons[i].setFont(buttonFont);
            }
        }
        // ColorKu icons should be the same size as the candidate numbers
        // icons are only created, if colorKu mode is active
        if (newColorImageHeight > 0 && Options.getInstance().isShowColorKuAct() && newColorImageHeight != colorImageHeight) {
            colorImageHeight = newColorImageHeight;
            for (int i = 0; i < colorKuIcons.length; i++) {
                colorKuIcons[i] = new ImageIcon(new ColorKuImage(colorImageHeight, Options.getInstance().getColorKuColor(i + 1)));
            }
        }
        repaint();
    }

    /**
     * Two modi: normal operation or coloring.<br>
     * Normal operation:
     * <ul>
     *      <li>activeColor has to be -1</li>
     *      <li>values and candidates contain valid choices (set of cells or single cell)</li>
     * </ul>
     * Coloring:
     * <ul>
     *      <li>activeColor holds the color, colorCellOrCandidate is true for coloring cells</li>
     *      <li>buttons are only available, if a single cell is selected<li>
     *      <li>if a set of cells is selected, coloredCells and coloredCandidates are null</li>
     * </ul>
     * @param values
     * @param candidates
     * @param aktColor
     * @param index
     * @param colorCellOrCandidate
     * @param singleCell 
     * @param coloredCells
     * @param coloredCandidates
     */
    public void update(SudokuSet values, SudokuSet candidates, int aktColor, int index,
            boolean colorCellOrCandidate, boolean singleCell, SortedMap<Integer, Integer> coloredCells,
            SortedMap<Integer, Integer> coloredCandidates) {
        // reset all buttons
        for (int i = 0; i < setValueButtons.length; i++) {
            setValueButtons[i].setText("");
            setValueButtons[i].setEnabled(false);
            setValueButtons[i].setForeground(normButtonForeground);
            setValueButtons[i].setBackground(normButtonBackground);
            setValueButtons[i].setIcon(null);
            toggleCandidatesButtons[i].setText("");
            toggleCandidatesButtons[i].setEnabled(false);
            toggleCandidatesButtons[i].setForeground(normButtonForeground);
            toggleCandidatesButtons[i].setBackground(normButtonBackground);
            toggleCandidatesButtons[i].setIcon(null);
        }
        cellColorPanel.setBackground(Options.getInstance().getDefaultCellColor());
        candidateColorPanel.setBackground(Options.getInstance().getDefaultCellColor());

        // now set accordingly
        this.aktColor = aktColor;
        if (aktColor == -1) {
            // no coloring -> buttons are available
            for (int i = 0; i < values.size(); i++) {
                int cand = values.get(i) - 1;
                if (cand >= 0 && cand <= 8) {
                    if (Options.getInstance().isShowColorKuAct()) {
                        setValueButtons[cand].setText(null);
                        setValueButtons[cand].setIcon(colorKuIcons[cand]);
                    } else {
                        setValueButtons[cand].setText(NUMBERS[cand]);
                        setValueButtons[cand].setIcon(null);
                    }
                    setValueButtons[cand].setEnabled(true);
                }
            }
            for (int i = 0; i < candidates.size(); i++) {
                int cand = candidates.get(i) - 1;
                if (cand >= 0 && cand <= 8) {
                    if (Options.getInstance().isShowColorKuAct()) {
                        toggleCandidatesButtons[cand].setText(null);
                        toggleCandidatesButtons[cand].setIcon(colorKuIcons[cand]);
                    } else {
                        toggleCandidatesButtons[cand].setText(NUMBERS[cand]);
                        toggleCandidatesButtons[cand].setIcon(null);
                    }
                    toggleCandidatesButtons[cand].setEnabled(true);
                }
            }
            if (singleCell) {
                toggleCandidatesLabel.setText(ResourceBundle.getBundle("intl/CellZoomPanel").getString("CellZoomPanel.toggleCandidatesLabel.text"));
                for (int i = 0; i < toggleCandidatesButtons.length; i++) {
                    toggleCandidatesButtons[i].setEnabled(true);
                }
            } else {
                toggleCandidatesLabel.setText(ResourceBundle.getBundle("intl/CellZoomPanel").getString("CellZoomPanel.toggleCandidatesLabel.text2"));
            }
        } else {
            // coloring
            if (colorCellOrCandidate) {
                cellColorPanel.setBackground(Options.getInstance().getColoringColors()[aktColor]);
            } else {
                candidateColorPanel.setBackground(Options.getInstance().getColoringColors()[aktColor]);
            }
            if (coloredCells != null) {
                // single cell is colored: set colors in buttons
//                if (coloredCells.containsKey(index)) {
//                    int color = coloredCells.get(index);
//                    Color valueColor = Options.getInstance().coloringColors[color];
//                    for (int i = 0; i < setValueButtons.length; i++) {
//                        setValueButtons[i].setForeground(valueColor);
//                    }
//                }
                if (colorCellOrCandidate == false) {
                    for (int i = 0; i < candidates.size(); i++) {
                        int cand = candidates.get(i);
                        if (coloredCandidates.containsKey(index * 10 + cand)) {
                            int candIndex = coloredCandidates.get(index * 10 + cand);
                            Color candColor = Options.getInstance().getColoringColors()[candIndex];
                            toggleCandidatesButtons[cand - 1].setForeground(candColor);
                            toggleCandidatesButtons[cand - 1].setBackground(candColor);
                            //toggleCandidatesButtons[cand - 1].setText(NUMBERS[cand - 1]);
                            toggleCandidatesButtons[cand - 1].setIcon(createImage(colorImageHeight, candIndex, cand));
                            toggleCandidatesButtons[cand - 1].setEnabled(true);
                        } else {
                            toggleCandidatesButtons[cand - 1].setText(NUMBERS[cand - 1]);
                            toggleCandidatesButtons[cand - 1].setEnabled(true);
                        }
                    }
                }
            }
        }

    }

    private ImageIcon createImage(int size, int colorIndex, int cand) {
        if (size > 0) {
            Image img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = (Graphics2D) img.getGraphics();
            Color color = Options.getInstance().getDefaultCellColor();
            if (colorIndex < Options.getInstance().getColoringColors().length) {
                color = Options.getInstance().getColoringColors()[colorIndex];
            }
            g.setColor(color);
            g.fillRect(0, 0, size, size);
            if (cand > 0) {
                if (Options.getInstance().isShowColorKuAct()) {
                    BufferedImage cImg = new ColorKuImage(size, Options.getInstance().getColorKuColor(cand));
                    g.drawImage(cImg, 0, 0, null);
                } else {
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.setFont(iconFont);
                    FontMetrics fm = g.getFontMetrics();
                    String str = String.valueOf(cand);
                    int strWidth = fm.stringWidth(str);
                    int strHeight = fm.getAscent();
                    g.setColor(normButtonForeground);
                    g.drawString(String.valueOf(cand), (size - strWidth) / 2, (size + strHeight - 2) / 2);
                }
            }
            return new ImageIcon(img);
        } else {
            return null;
        }
    }

//    private void initButton(JButton button, Color color) {
//        //button.setText(" ");
//        Image img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
//        Graphics g = img.getGraphics();
//        g.setColor(color);
//        g.fillRect(0, 0, 10, 10);
//        button.setIcon(new ImageIcon(img));
//        if (UIManager.getLookAndFeel().getName().equals("CDE/Motif")) {
//            button.setBackground(color);
//        }
//    }
//
    private void printSize() {
//        System.out.println(getDimensions(this, "cellZoomPanel"));
//
//        System.out.println(getDimensions(setValueLabel, "setValueLabel"));
//        System.out.println(getDimensions(setValuePanel, "setValuePanel"));
//        System.out.println(getDimensions(toggleCandidatesLabel, "toggleCandidatesLabel"));
//        System.out.println(getDimensions(toggleCandidatesPanel, "toggleCandidatesPanel"));
//        System.out.println(getDimensions(cellColorLabel, "colorCellsLabel"));
//        System.out.println(getDimensions(cellColorPanel, "cellColorPanel"));
//        System.out.println(getDimensions(chooseCellColorPanel, "chooseCellColorPanel"));
//        System.out.println(getDimensions(chooseCandidateColorLabel, "chooseCandidateColorLabel"));
//        System.out.println(getDimensions(chooseCandidateColorPanel, "chooseCandidateColorPanel"));
//        System.out.println(getDimensions(jFontButton, "jButton19"));
//        Font bFont = jFontButton.getFont();
//        FontMetrics fm = getFontMetrics(bFont);
//        int fHeight = fm.getAscent();
//        int cHeight = jFontButton.getHeight();
//        System.out.println(bFont + ": " + fHeight + "/" + cHeight);
    }

//    private String getDimensions(Component c, String name) {
//        return name + ": " + c.getX() + "/" + c.getY() + "/" + c.getSize();
//    }
    public void setTitleLabelColors(Color fore, Color back) {
        titleLabel.setBackground(back);
        titleLabel.setForeground(fore);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel candidateColorPanel;
    private javax.swing.JLabel cellColorLabel;
    private javax.swing.JPanel cellColorPanel;
    private javax.swing.JPanel chooseCandidateColor0Panel;
    private javax.swing.JPanel chooseCandidateColor1Panel;
    private javax.swing.JPanel chooseCandidateColor2Panel;
    private javax.swing.JPanel chooseCandidateColor3Panel;
    private javax.swing.JPanel chooseCandidateColor4Panel;
    private javax.swing.JPanel chooseCandidateColor5Panel;
    private javax.swing.JPanel chooseCandidateColor6Panel;
    private javax.swing.JPanel chooseCandidateColor7Panel;
    private javax.swing.JPanel chooseCandidateColor8Panel;
    private javax.swing.JPanel chooseCandidateColor9Panel;
    private javax.swing.JLabel chooseCandidateColorLabel;
    private javax.swing.JPanel chooseCandidateColorM1Panel;
    private javax.swing.JPanel chooseCandidateColorM2Panel;
    private javax.swing.JPanel chooseCandidateColorPanel;
    private javax.swing.JPanel chooseCellColor0Panel;
    private javax.swing.JPanel chooseCellColor1Panel;
    private javax.swing.JPanel chooseCellColor2Panel;
    private javax.swing.JPanel chooseCellColor3Panel;
    private javax.swing.JPanel chooseCellColor4Panel;
    private javax.swing.JPanel chooseCellColor5Panel;
    private javax.swing.JPanel chooseCellColor6Panel;
    private javax.swing.JPanel chooseCellColor7Panel;
    private javax.swing.JPanel chooseCellColor8Panel;
    private javax.swing.JPanel chooseCellColor9Panel;
    private javax.swing.JPanel chooseCellColorM1Panel;
    private javax.swing.JPanel chooseCellColorM2Panel;
    private javax.swing.JPanel chooseCellColorPanel;
    private javax.swing.JButton jFontButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton setValueButton1;
    private javax.swing.JButton setValueButton2;
    private javax.swing.JButton setValueButton3;
    private javax.swing.JButton setValueButton4;
    private javax.swing.JButton setValueButton5;
    private javax.swing.JButton setValueButton6;
    private javax.swing.JButton setValueButton7;
    private javax.swing.JButton setValueButton8;
    private javax.swing.JButton setValueButton9;
    private javax.swing.JLabel setValueLabel;
    private javax.swing.JPanel setValuePanel;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JButton toggleCandidatesButton1;
    private javax.swing.JButton toggleCandidatesButton2;
    private javax.swing.JButton toggleCandidatesButton3;
    private javax.swing.JButton toggleCandidatesButton4;
    private javax.swing.JButton toggleCandidatesButton5;
    private javax.swing.JButton toggleCandidatesButton6;
    private javax.swing.JButton toggleCandidatesButton7;
    private javax.swing.JButton toggleCandidatesButton8;
    private javax.swing.JButton toggleCandidatesButton9;
    private javax.swing.JLabel toggleCandidatesLabel;
    private javax.swing.JPanel toggleCandidatesPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * @param sudokuPanel the sudokuPanel to set
     */
    public void setSudokuPanel(SudokuPanel sudokuPanel) {
        this.sudokuPanel = sudokuPanel;
    }
}
