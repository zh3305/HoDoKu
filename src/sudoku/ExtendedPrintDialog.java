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

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;

/**
 * Booklet printer
 * 
 * @author hobiwan
 */
public class ExtendedPrintDialog extends javax.swing.JDialog {

    /** Images for Layout selection */
    private static final Icon[] images = new ImageIcon[]{
        new ImageIcon(Toolkit.getDefaultToolkit().getImage(ExtendedPrintDialog.class.getResource("/img/slh1th.png"))),
        new ImageIcon(Toolkit.getDefaultToolkit().getImage(ExtendedPrintDialog.class.getResource("/img/slh2th.png"))),
        new ImageIcon(Toolkit.getDefaultToolkit().getImage(ExtendedPrintDialog.class.getResource("/img/slh4th.png"))),
        new ImageIcon(Toolkit.getDefaultToolkit().getImage(ExtendedPrintDialog.class.getResource("/img/slq2th.png"))),
        new ImageIcon(Toolkit.getDefaultToolkit().getImage(ExtendedPrintDialog.class.getResource("/img/slq4th.png")))
    };
    /** The symbolic layout numbers used to choose the correct image */
    private static final Integer[] layouts = new Integer[]{
        0, 1, 2, 3, 4
    };
    /** the names of the {@link GameMode}s. */
    private static final String[] modeNames = new String[] {
        ResourceBundle.getBundle("intl/MainFrame").getString("MainFrame.playingMenuItem.text"),
        ResourceBundle.getBundle("intl/MainFrame").getString("MainFrame.learningMenuItem.text"),
        ResourceBundle.getBundle("intl/MainFrame").getString("MainFrame.practisingMenuItem.text")
    };
    private static final long serialVersionUID = 1L;
    /** The textfields with the number of puzzles for all sections */
    private JTextField[] numberTextFields;
    /** The levels of the puzzles for all sections */
    private JComboBox[] levelComboBoxes;
    /** The modes of the puzzles for all sections */
    private JComboBox[] modeComboBoxes;
    /** The candidate checkboxes for all sections */
    private JCheckBox[] candCheckBoxes;
    /** The page format for the print job */
    private PageFormat pageFormat = null;
    /** The current print job */
    private PrinterJob job = null;
    /** The total number of puzzles requested */
    private int totalNumberOfPuzzles = 0;


    /** Creates new form ExtendedPrintDialog
     * @param parent
     * @param modal  
     */
    @SuppressWarnings("unchecked")
    public ExtendedPrintDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        getRootPane().setDefaultButton(printButton);

        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);

        layoutList.setVisibleRowCount(1);
        layoutList.setCellRenderer(new MyCellRenderer());
        layoutList.setListData(layouts);
        layoutList.setSelectedIndex(0);

        // fill the section arrays
        numberTextFields = new JTextField[]{
            numberOfPuzzles1TextField, numberOfPuzzles2TextField,
            numberOfPuzzles3TextField, numberOfPuzzles4TextField,
            numberOfPuzzles5TextField
        };
        levelComboBoxes = new JComboBox[]{
            level1ComboBox, level2ComboBox, level3ComboBox,
            level4ComboBox, level5ComboBox
        };
        modeComboBoxes = new JComboBox[]{
            mode1ComboBox, mode2ComboBox, mode3ComboBox,
            mode4ComboBox, mode5ComboBox
        };
        candCheckBoxes = new JCheckBox[]{
            printCands1CheckBox, printCands2CheckBox, printCands3CheckBox,
            printCands4CheckBox, printCands5CheckBox
        };

        // only numbers in "number of puzzles" textfields
        for (int i = 0; i < numberTextFields.length; i++) {
            numberTextFields[i].setDocument(new NumbersOnlyDocument());
        }
        
        for (int i = 1; i < DifficultyType.values().length; i++) {
            for (int j = 0; j < levelComboBoxes.length; j++) {
                levelComboBoxes[j].addItem(Options.getInstance().getDifficultyLevels()[i].getName());
            }
        }
        
        for (int i = 0; i < modeNames.length; i++) {
            if (modeNames[i].endsWith("...")) {
                modeNames[i] = modeNames[i].substring(0, modeNames[i].length() - 3);
            }
            for (int j = 0; j < modeComboBoxes.length; j++) {
                modeComboBoxes[j].addItem(modeNames[i]);
            }
        }
        
        manualDuplexCheckBox.setEnabled(printBookletCheckBox.isSelected());
        
        job = SudokuUtil.getPrinterJob();
        pageFormat = SudokuUtil.getPageFormat();
        Paper paper = pageFormat.getPaper();
        double ind = 28.3464567;
        paper.setImageableArea(ind, ind, paper.getWidth() - 2 * ind, paper.getHeight() - 2 * ind);
        pageFormat.setPaper(paper);
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
        jScrollPane1 = new javax.swing.JScrollPane();
        layoutList = new javax.swing.JList();
        printRatingCheckBox = new javax.swing.JCheckBox();
        printAllBlackCheckBox = new javax.swing.JCheckBox();
        printBookletCheckBox = new javax.swing.JCheckBox();
        manualDuplexCheckBox = new javax.swing.JCheckBox();
        printButton = new javax.swing.JButton();
        pageSetupButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        sectionContainerPanel = new javax.swing.JPanel();
        section1Panel = new javax.swing.JPanel();
        numberOfPuzzles1Label = new javax.swing.JLabel();
        numberOfPuzzles1TextField = new javax.swing.JTextField();
        level1Label = new javax.swing.JLabel();
        level1ComboBox = new javax.swing.JComboBox();
        mode1Label = new javax.swing.JLabel();
        mode1ComboBox = new javax.swing.JComboBox();
        printCands1CheckBox = new javax.swing.JCheckBox();
        section2Panel = new javax.swing.JPanel();
        numberOfPuzzles2Label = new javax.swing.JLabel();
        numberOfPuzzles2TextField = new javax.swing.JTextField();
        level2Label = new javax.swing.JLabel();
        level2ComboBox = new javax.swing.JComboBox();
        mode2Label = new javax.swing.JLabel();
        mode2ComboBox = new javax.swing.JComboBox();
        printCands2CheckBox = new javax.swing.JCheckBox();
        section3Panel = new javax.swing.JPanel();
        numberOfPuzzles3Label = new javax.swing.JLabel();
        numberOfPuzzles3TextField = new javax.swing.JTextField();
        level3Label = new javax.swing.JLabel();
        level3ComboBox = new javax.swing.JComboBox();
        mode3Label = new javax.swing.JLabel();
        mode3ComboBox = new javax.swing.JComboBox();
        printCands3CheckBox = new javax.swing.JCheckBox();
        section4Panel = new javax.swing.JPanel();
        numberOfPuzzles4Label = new javax.swing.JLabel();
        numberOfPuzzles4TextField = new javax.swing.JTextField();
        level4Label = new javax.swing.JLabel();
        level4ComboBox = new javax.swing.JComboBox();
        mode4Label = new javax.swing.JLabel();
        mode4ComboBox = new javax.swing.JComboBox();
        printCands4CheckBox = new javax.swing.JCheckBox();
        section5Panel = new javax.swing.JPanel();
        numberOfPuzzles5Label = new javax.swing.JLabel();
        numberOfPuzzles5TextField = new javax.swing.JTextField();
        level5Label = new javax.swing.JLabel();
        level5ComboBox = new javax.swing.JComboBox();
        mode5Label = new javax.swing.JLabel();
        mode5ComboBox = new javax.swing.JComboBox();
        printCands5CheckBox = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("intl/ExtendedPrintDialog"); // NOI18N
        setTitle(bundle.getString("ExtendedPrintDialog.title")); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("ExtendedPrintDialog.jPanel1.border.title"))); // NOI18N

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        layoutList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        layoutList.setLayoutOrientation(javax.swing.JList.HORIZONTAL_WRAP);
        jScrollPane1.setViewportView(layoutList);

        printRatingCheckBox.setMnemonic(java.util.ResourceBundle.getBundle("intl/ExtendedPrintDialog").getString("ExtendedPrintDialog.printRatingCheckBox.mnemonic").charAt(0));
        printRatingCheckBox.setSelected(true);
        printRatingCheckBox.setText(bundle.getString("ExtendedPrintDialog.printRatingCheckBox.text")); // NOI18N

        printAllBlackCheckBox.setMnemonic(java.util.ResourceBundle.getBundle("intl/ExtendedPrintDialog").getString("ExtendedPrintDialog.printAllBlackCheckBox.mnemonic").charAt(0));
        printAllBlackCheckBox.setText(bundle.getString("ExtendedPrintDialog.printAllBlackCheckBox.text")); // NOI18N

        printBookletCheckBox.setMnemonic(java.util.ResourceBundle.getBundle("intl/ExtendedPrintDialog").getString("ExtendedPrintDialog.printBookletCheckBox.mnemonic").charAt(0));
        printBookletCheckBox.setText(bundle.getString("ExtendedPrintDialog.printBookletCheckBox.text")); // NOI18N
        printBookletCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printBookletCheckBoxActionPerformed(evt);
            }
        });

        manualDuplexCheckBox.setMnemonic(java.util.ResourceBundle.getBundle("intl/ExtendedPrintDialog").getString("ExtendedPrintDialog.manualDuplexCheckBox.mnemonic").charAt(0));
        manualDuplexCheckBox.setText(bundle.getString("ExtendedPrintDialog.manualDuplexCheckBox.text")); // NOI18N
        manualDuplexCheckBox.setEnabled(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(printRatingCheckBox)
                    .addComponent(printAllBlackCheckBox))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(manualDuplexCheckBox)
                    .addComponent(printBookletCheckBox))
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE)
                .addContainerGap(10, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(printRatingCheckBox)
                    .addComponent(printBookletCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(printAllBlackCheckBox)
                    .addComponent(manualDuplexCheckBox))
                .addContainerGap())
        );

        printButton.setMnemonic(java.util.ResourceBundle.getBundle("intl/ExtendedPrintDialog").getString("ExtendedPrintDialog.printButton.mnemonic").charAt(0));
        printButton.setText(bundle.getString("ExtendedPrintDialog.printButton.text")); // NOI18N
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printButtonActionPerformed(evt);
            }
        });

        pageSetupButton.setMnemonic(java.util.ResourceBundle.getBundle("intl/ExtendedPrintDialog").getString("ExtendedPrintDialog.pageSetupButton.mnemonic").charAt(0));
        pageSetupButton.setText(bundle.getString("ExtendedPrintDialog.pageSetupButton.text")); // NOI18N
        pageSetupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pageSetupButtonActionPerformed(evt);
            }
        });

        cancelButton.setMnemonic(java.util.ResourceBundle.getBundle("intl/ExtendedPrintDialog").getString("ExtendedPrintDialog.cancelButton.mnemonic").charAt(0));
        cancelButton.setText(bundle.getString("ExtendedPrintDialog.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        section1Panel.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("ExtendedPrintDialog.section1Panel.border.title"))); // NOI18N

        numberOfPuzzles1Label.setLabelFor(numberOfPuzzles1TextField);
        numberOfPuzzles1Label.setText(bundle.getString("ExtendedPrintDialog.numberOfPuzzles1Label.text")); // NOI18N

        numberOfPuzzles1TextField.setText(bundle.getString("ExtendedPrintDialog.numberOfPuzzles1TextField.text")); // NOI18N

        level1Label.setLabelFor(level1ComboBox);
        level1Label.setText(bundle.getString("ExtendedPrintDialog.level1Label.text")); // NOI18N

        mode1Label.setLabelFor(mode1ComboBox);
        mode1Label.setText(bundle.getString("ExtendedPrintDialog.mode1Label.text")); // NOI18N

        printCands1CheckBox.setText(bundle.getString("ExtendedPrintDialog.printCands1CheckBox.text")); // NOI18N

        javax.swing.GroupLayout section1PanelLayout = new javax.swing.GroupLayout(section1Panel);
        section1Panel.setLayout(section1PanelLayout);
        section1PanelLayout.setHorizontalGroup(
            section1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(section1PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(section1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(numberOfPuzzles1Label)
                    .addComponent(level1Label)
                    .addComponent(mode1Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(printCands1CheckBox)
                    .addComponent(numberOfPuzzles1TextField, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                    .addComponent(level1ComboBox, 0, 349, Short.MAX_VALUE)
                    .addComponent(mode1ComboBox, 0, 349, Short.MAX_VALUE))
                .addContainerGap())
        );
        section1PanelLayout.setVerticalGroup(
            section1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(section1PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(section1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(numberOfPuzzles1Label)
                    .addComponent(numberOfPuzzles1TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(level1Label)
                    .addComponent(level1ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mode1Label)
                    .addComponent(mode1ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(printCands1CheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        section2Panel.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("ExtendedPrintDialog.section2Panel.border.title"))); // NOI18N

        numberOfPuzzles2Label.setLabelFor(numberOfPuzzles2TextField);
        numberOfPuzzles2Label.setText(bundle.getString("ExtendedPrintDialog.numberOfPuzzles1Label.text")); // NOI18N

        numberOfPuzzles2TextField.setText(bundle.getString("ExtendedPrintDialog.numberOfPuzzles1TextField.text")); // NOI18N

        level2Label.setLabelFor(level2ComboBox);
        level2Label.setText(bundle.getString("ExtendedPrintDialog.level1Label.text")); // NOI18N

        mode2Label.setLabelFor(mode2ComboBox);
        mode2Label.setText(bundle.getString("ExtendedPrintDialog.mode1Label.text")); // NOI18N

        printCands2CheckBox.setText(bundle.getString("ExtendedPrintDialog.printCands1CheckBox.text")); // NOI18N

        javax.swing.GroupLayout section2PanelLayout = new javax.swing.GroupLayout(section2Panel);
        section2Panel.setLayout(section2PanelLayout);
        section2PanelLayout.setHorizontalGroup(
            section2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(section2PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(section2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(numberOfPuzzles2Label)
                    .addComponent(level2Label)
                    .addComponent(mode2Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(printCands2CheckBox)
                    .addComponent(numberOfPuzzles2TextField, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                    .addComponent(level2ComboBox, 0, 349, Short.MAX_VALUE)
                    .addComponent(mode2ComboBox, 0, 349, Short.MAX_VALUE))
                .addContainerGap())
        );
        section2PanelLayout.setVerticalGroup(
            section2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(section2PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(section2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(numberOfPuzzles2Label)
                    .addComponent(numberOfPuzzles2TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(level2Label)
                    .addComponent(level2ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mode2Label)
                    .addComponent(mode2ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(printCands2CheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        section3Panel.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("ExtendedPrintDialog.section3Panel.border.title"))); // NOI18N

        numberOfPuzzles3Label.setLabelFor(numberOfPuzzles3TextField);
        numberOfPuzzles3Label.setText(bundle.getString("ExtendedPrintDialog.numberOfPuzzles1Label.text")); // NOI18N

        numberOfPuzzles3TextField.setText(bundle.getString("ExtendedPrintDialog.numberOfPuzzles1TextField.text")); // NOI18N

        level3Label.setLabelFor(level3Label);
        level3Label.setText(bundle.getString("ExtendedPrintDialog.level1Label.text")); // NOI18N

        mode3Label.setLabelFor(mode3ComboBox);
        mode3Label.setText(bundle.getString("ExtendedPrintDialog.mode1Label.text")); // NOI18N

        printCands3CheckBox.setText(bundle.getString("ExtendedPrintDialog.printCands1CheckBox.text")); // NOI18N

        javax.swing.GroupLayout section3PanelLayout = new javax.swing.GroupLayout(section3Panel);
        section3Panel.setLayout(section3PanelLayout);
        section3PanelLayout.setHorizontalGroup(
            section3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(section3PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(section3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(numberOfPuzzles3Label)
                    .addComponent(level3Label)
                    .addComponent(mode3Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(printCands3CheckBox)
                    .addComponent(numberOfPuzzles3TextField, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                    .addComponent(level3ComboBox, 0, 349, Short.MAX_VALUE)
                    .addComponent(mode3ComboBox, 0, 349, Short.MAX_VALUE))
                .addContainerGap())
        );
        section3PanelLayout.setVerticalGroup(
            section3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(section3PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(section3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(numberOfPuzzles3Label)
                    .addComponent(numberOfPuzzles3TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(level3Label)
                    .addComponent(level3ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mode3Label)
                    .addComponent(mode3ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(printCands3CheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        section4Panel.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("ExtendedPrintDialog.section4Panel.border.title"))); // NOI18N

        numberOfPuzzles4Label.setLabelFor(numberOfPuzzles4TextField);
        numberOfPuzzles4Label.setText(bundle.getString("ExtendedPrintDialog.numberOfPuzzles1Label.text")); // NOI18N

        numberOfPuzzles4TextField.setText(bundle.getString("ExtendedPrintDialog.numberOfPuzzles1TextField.text")); // NOI18N

        level4Label.setLabelFor(level4ComboBox);
        level4Label.setText(bundle.getString("ExtendedPrintDialog.level1Label.text")); // NOI18N

        mode4Label.setLabelFor(mode4ComboBox);
        mode4Label.setText(bundle.getString("ExtendedPrintDialog.mode1Label.text")); // NOI18N

        printCands4CheckBox.setText(bundle.getString("ExtendedPrintDialog.printCands1CheckBox.text")); // NOI18N

        javax.swing.GroupLayout section4PanelLayout = new javax.swing.GroupLayout(section4Panel);
        section4Panel.setLayout(section4PanelLayout);
        section4PanelLayout.setHorizontalGroup(
            section4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(section4PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(section4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(numberOfPuzzles4Label)
                    .addComponent(level4Label)
                    .addComponent(mode4Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(printCands4CheckBox)
                    .addComponent(numberOfPuzzles4TextField, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                    .addComponent(level4ComboBox, 0, 349, Short.MAX_VALUE)
                    .addComponent(mode4ComboBox, 0, 349, Short.MAX_VALUE))
                .addContainerGap())
        );
        section4PanelLayout.setVerticalGroup(
            section4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(section4PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(section4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(numberOfPuzzles4Label)
                    .addComponent(numberOfPuzzles4TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(level4Label)
                    .addComponent(level4ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mode4Label)
                    .addComponent(mode4ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(printCands4CheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        section5Panel.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("ExtendedPrintDialog.section5Panel.border.title"))); // NOI18N

        numberOfPuzzles5Label.setLabelFor(numberOfPuzzles5TextField);
        numberOfPuzzles5Label.setText(bundle.getString("ExtendedPrintDialog.numberOfPuzzles1Label.text")); // NOI18N

        numberOfPuzzles5TextField.setText(bundle.getString("ExtendedPrintDialog.numberOfPuzzles1TextField.text")); // NOI18N

        level5Label.setLabelFor(level5ComboBox);
        level5Label.setText(bundle.getString("ExtendedPrintDialog.level1Label.text")); // NOI18N

        mode5Label.setLabelFor(mode5ComboBox);
        mode5Label.setText(bundle.getString("ExtendedPrintDialog.mode1Label.text")); // NOI18N

        printCands5CheckBox.setText(bundle.getString("ExtendedPrintDialog.printCands1CheckBox.text")); // NOI18N

        javax.swing.GroupLayout section5PanelLayout = new javax.swing.GroupLayout(section5Panel);
        section5Panel.setLayout(section5PanelLayout);
        section5PanelLayout.setHorizontalGroup(
            section5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(section5PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(section5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(numberOfPuzzles5Label)
                    .addComponent(level5Label)
                    .addComponent(mode5Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(printCands5CheckBox)
                    .addComponent(numberOfPuzzles5TextField, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                    .addComponent(level5ComboBox, 0, 349, Short.MAX_VALUE)
                    .addComponent(mode5ComboBox, 0, 349, Short.MAX_VALUE))
                .addContainerGap())
        );
        section5PanelLayout.setVerticalGroup(
            section5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(section5PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(section5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(numberOfPuzzles5Label)
                    .addComponent(numberOfPuzzles5TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(level5Label)
                    .addComponent(level5ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(section5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mode5Label)
                    .addComponent(mode5ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(printCands5CheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout sectionContainerPanelLayout = new javax.swing.GroupLayout(sectionContainerPanel);
        sectionContainerPanel.setLayout(sectionContainerPanelLayout);
        sectionContainerPanelLayout.setHorizontalGroup(
            sectionContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(section1Panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(section2Panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(section3Panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(section4Panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(section5Panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        sectionContainerPanelLayout.setVerticalGroup(
            sectionContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sectionContainerPanelLayout.createSequentialGroup()
                .addComponent(section1Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(section2Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(section3Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(section4Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(section5Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jScrollPane2.setViewportView(sectionContainerPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 496, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(printButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pageSetupButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, pageSetupButton, printButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 222, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(printButton)
                    .addComponent(pageSetupButton)
                    .addComponent(cancelButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void pageSetupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pageSetupButtonActionPerformed
        adjustOrientation();
        pageFormat = job.pageDialog(pageFormat);
    }//GEN-LAST:event_pageSetupButtonActionPerformed

    private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printButtonActionPerformed
        // if no puzzles have been set, nothing to do
        totalNumberOfPuzzles = 0;
        for (int i = 0; i < numberTextFields.length; i++) {
            totalNumberOfPuzzles += getNumberOfPuzzes(i);
        }
        if (totalNumberOfPuzzles == 0) {
            // nothing to do
            JOptionPane.showMessageDialog(this, 
                    ResourceBundle.getBundle("intl/ExtendedPrintDialog").getString("ExtendedPrintDialog.noPuzzles"), 
                    java.util.ResourceBundle.getBundle("intl/MainFrame").getString("MainFrame.error"), 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        int layout = layoutList.getSelectedIndex();
        boolean printRating = printRatingCheckBox.isSelected();
        boolean allBlack = printAllBlackCheckBox.isSelected();
        boolean printBooklet = printBookletCheckBox.isSelected();
        boolean manualDuplex = manualDuplexCheckBox.isSelected();
        if (printBooklet && layout < 3) {
            // booklets can only be printed with a LANDSCAPE format
            JOptionPane.showMessageDialog(this, 
                    ResourceBundle.getBundle("intl/ExtendedPrintDialog").getString("ExtendedPrintDialog.wrongOrientation"), 
                    java.util.ResourceBundle.getBundle("intl/MainFrame").getString("MainFrame.error"), 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        job = SudokuUtil.getPrinterJob();
        pageFormat = SudokuUtil.getPageFormat();
        adjustOrientation();
        ExtendedPrintProgressDialog dlg = new ExtendedPrintProgressDialog(null, true, 
                numberTextFields, levelComboBoxes, modeComboBoxes, candCheckBoxes,
                layout, printRating, allBlack, printBooklet, manualDuplex);
        job.setPrintable(dlg, pageFormat);
        if (job.printDialog()) {
            dlg.setJob(job);
            dlg.setVisible(true);
            setVisible(false);
        }
    }//GEN-LAST:event_printButtonActionPerformed

    private void printBookletCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printBookletCheckBoxActionPerformed
        manualDuplexCheckBox.setEnabled(printBookletCheckBox.isSelected());
    }//GEN-LAST:event_printBookletCheckBoxActionPerformed

    /**
     * Utility method: reads the contents of the textfield
     * {@link #numberTextFields}[<code>index</code>] and returns
     * it as integer. If the field is empty or an error occurs,
     * <code>0</code> is returned.
     * #
     * @param index
     * @return 
     */
    private int getNumberOfPuzzes(int index) {
        int ret = 0;
        try {
            ret = Integer.parseInt(numberTextFields[index].getText());
        } catch (NumberFormatException ex) {
            // simply assume 0 has been entered into the textfield
            ret = 0;
        }
        return ret;
    }
    
    /**
     * The orientation of the print pageis defined by the layout selected in
     * {@link #layoutList}. It has to be adjusted accordingly.
     */
    private void adjustOrientation() {
        job = SudokuUtil.getPrinterJob();
        pageFormat = SudokuUtil.getPageFormat();
        if (layoutList.getSelectedIndex() >= 3) {
            pageFormat.setOrientation(PageFormat.LANDSCAPE);
//            System.out.println("setting landscape!");
        } else {
            pageFormat.setOrientation(PageFormat.PORTRAIT);
        }
    }

    class MyCellRenderer extends JLabel implements ListCellRenderer {
        private static final long serialVersionUID = 1L;

        // This is the only method defined by ListCellRenderer.
        // We just reconfigure the JLabel each time we're called.
        @Override
        public Component getListCellRendererComponent(
                JList list,
                Object value, // value to display
                int index, // cell index
                boolean isSelected, // is the cell selected
                boolean cellHasFocus) // the list and the cell have the focus
        {
            setHorizontalAlignment(JLabel.CENTER);
            setText(null);
            if (!(value instanceof Integer)) {
                // do nothing!
                return this;
            }
            setIcon(images[(Integer) value]);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled());
//            setFont(list.getFont());
            setOpaque(true);
            return this;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ExtendedPrintDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ExtendedPrintDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ExtendedPrintDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ExtendedPrintDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                ExtendedPrintDialog dialog = new ExtendedPrintDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JList layoutList;
    private javax.swing.JComboBox level1ComboBox;
    private javax.swing.JLabel level1Label;
    private javax.swing.JComboBox level2ComboBox;
    private javax.swing.JLabel level2Label;
    private javax.swing.JComboBox level3ComboBox;
    private javax.swing.JLabel level3Label;
    private javax.swing.JComboBox level4ComboBox;
    private javax.swing.JLabel level4Label;
    private javax.swing.JComboBox level5ComboBox;
    private javax.swing.JLabel level5Label;
    private javax.swing.JCheckBox manualDuplexCheckBox;
    private javax.swing.JComboBox mode1ComboBox;
    private javax.swing.JLabel mode1Label;
    private javax.swing.JComboBox mode2ComboBox;
    private javax.swing.JLabel mode2Label;
    private javax.swing.JComboBox mode3ComboBox;
    private javax.swing.JLabel mode3Label;
    private javax.swing.JComboBox mode4ComboBox;
    private javax.swing.JLabel mode4Label;
    private javax.swing.JComboBox mode5ComboBox;
    private javax.swing.JLabel mode5Label;
    private javax.swing.JLabel numberOfPuzzles1Label;
    private javax.swing.JTextField numberOfPuzzles1TextField;
    private javax.swing.JLabel numberOfPuzzles2Label;
    private javax.swing.JTextField numberOfPuzzles2TextField;
    private javax.swing.JLabel numberOfPuzzles3Label;
    private javax.swing.JTextField numberOfPuzzles3TextField;
    private javax.swing.JLabel numberOfPuzzles4Label;
    private javax.swing.JTextField numberOfPuzzles4TextField;
    private javax.swing.JLabel numberOfPuzzles5Label;
    private javax.swing.JTextField numberOfPuzzles5TextField;
    private javax.swing.JButton pageSetupButton;
    private javax.swing.JCheckBox printAllBlackCheckBox;
    private javax.swing.JCheckBox printBookletCheckBox;
    private javax.swing.JButton printButton;
    private javax.swing.JCheckBox printCands1CheckBox;
    private javax.swing.JCheckBox printCands2CheckBox;
    private javax.swing.JCheckBox printCands3CheckBox;
    private javax.swing.JCheckBox printCands4CheckBox;
    private javax.swing.JCheckBox printCands5CheckBox;
    private javax.swing.JCheckBox printRatingCheckBox;
    private javax.swing.JPanel section1Panel;
    private javax.swing.JPanel section2Panel;
    private javax.swing.JPanel section3Panel;
    private javax.swing.JPanel section4Panel;
    private javax.swing.JPanel section5Panel;
    private javax.swing.JPanel sectionContainerPanel;
    // End of variables declaration//GEN-END:variables
}
