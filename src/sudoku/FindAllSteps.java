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

import java.awt.EventQueue;
import java.util.List;
import solver.SudokuSolverFactory;
import solver.SudokuStepFinder;

/**
 *
 * @author hobiwan
 */
public class FindAllSteps implements Runnable {
    private FindAllStepsProgressDialog dlg = null;
    private List<SolutionStep> steps;
    private List<SolutionType> testTypes = null;
    private Sudoku2 sudoku;
//    private int minSize;
//    private int maxSize;
//    private int maxFins;
//    private int maxEndoFins;
//    private boolean forcingChains;
//    private boolean forcingNets;
//    private boolean krakenFish;

    private SudokuStepFinder stepFinder;
    
    public FindAllSteps() {
        stepFinder = SudokuSolverFactory.getDefaultSolverInstance().getStepFinder();
    }
    
    public FindAllSteps(List<SolutionStep> steps, Sudoku2 sudoku, FindAllStepsProgressDialog dlg) {
        this();
        
        this.sudoku = sudoku;
        // sometimes the internal chaching data can become invalid -> better save than sorry
        sudoku.rebuildInternalData();
        
        this.steps = steps;
        this.dlg = dlg;
    }

    private void updateProgress(final String label, final int step) {
        if (dlg != null) {
            dlg.updateProgress(label, step);
        }
    }

    /**
     * The class can be called by FindAllStepsProgressDialog in which case the
     * configuration from Options.solverSteps has to be read. If it is called
     * from BatchSolveThread only all Steps for testType have to be found
     * 
     * @param type 
     * @return
     */
    private boolean isAllStepsEnabled(SolutionType type) {
        if (testTypes == null) {
            StepConfig[] tmpSteps = Options.getInstance().solverSteps;
            for (int i = 0; i < tmpSteps.length; i++) {
                if (tmpSteps[i].getType() == type) {
                    return tmpSteps[i].isAllStepsEnabled();
                }
            }
            return false;
        } else {
            return testTypes.contains(type);
        }
    }
    
    private boolean isFishTestTypes() {
        for (int i = 0; i < testTypes.size(); i++) {
            if (! testTypes.get(i).isFish()) {
                return false;
            }
        }
        return true;
    }
    
    private void filterSteps(List<SolutionStep> steps) {
        for (int i = 0; i < steps.size(); i++) {
            if (! isAllStepsEnabled(steps.get(i).getType())) {
                steps.remove(i);
                i--;
            }
        }
    }
    
    @Override
    public void run() {
        int actStep = 0;
        //boolean[] tmpCands = new boolean[9];
        List<SolutionStep> steps1 = null;
        //while (! Thread.currentThread().isInterrupted()) {
        while (! Thread.interrupted()) {
            switch (actStep) {
                case 0:
                    updateProgress(java.util.ResourceBundle.getBundle("intl/FindAllStepsProgressDialog").getString("FindAllStepsProgressDialog.simple_solutions"), actStep);
                    steps1 = stepFinder.findAllFullHouses(sudoku);
                    steps.addAll(steps1);
                    steps1 = stepFinder.findAllHiddenXle(sudoku);
                    steps.addAll(steps1);
                    steps1 = stepFinder.findAllNakedXle(sudoku);
                    steps.addAll(steps1);
                    filterSteps(steps);
                    if (isAllStepsEnabled(SolutionType.LOCKED_CANDIDATES_1) && isAllStepsEnabled(SolutionType.LOCKED_CANDIDATES_2)) {
                        steps1 = stepFinder.findAllLockedCandidates(sudoku);
                        steps.addAll(steps1);
                    } else if (isAllStepsEnabled(SolutionType.LOCKED_CANDIDATES_1)) {
                        steps1 = stepFinder.findAllLockedCandidates1(sudoku);
                        steps.addAll(steps1);
                    } else if (isAllStepsEnabled(SolutionType.LOCKED_CANDIDATES_2)) {
                        steps1 = stepFinder.findAllLockedCandidates2(sudoku);
                        steps.addAll(steps1);
                    }
                    if (isAllStepsEnabled(SolutionType.SKYSCRAPER)) {
                        steps1 = stepFinder.findAllSkyScrapers(sudoku);
                        steps.addAll(steps1);
                    }
                    if (isAllStepsEnabled(SolutionType.EMPTY_RECTANGLE)) {
                        steps1 = stepFinder.findAllEmptyRectangles(sudoku);
                        steps.addAll(steps1);
                    }
                    if (isAllStepsEnabled(SolutionType.TWO_STRING_KITE)) {
                        steps1 = stepFinder.findAllTwoStringKites(sudoku);
                        steps.addAll(steps1);
                    }
                    if (isAllStepsEnabled(SolutionType.SUE_DE_COQ)) {
                        steps1 = stepFinder.getAllSueDeCoqs(sudoku);
                        steps.addAll(steps1);
                    }
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                    //System.out.println("Fish search cand " + (actStep) + ": " + Options.getInstance().allStepsFishCandidates.charAt(actStep - 1));
                    updateProgress(java.util.ResourceBundle.getBundle("intl/FindAllStepsProgressDialog").getString("FindAllStepsProgressDialog.fish") + " " + actStep, actStep);
                    if ((testTypes == null && Options.getInstance().isAllStepsSearchFish() && 
                            Options.getInstance().getAllStepsFishCandidates().charAt(actStep - 1) == '1') ||
                            testTypes != null && isFishTestTypes()) {
                        boolean oldCheckTemplates = Options.getInstance().isCheckTemplates();
                        Options.getInstance().setCheckTemplates(Options.getInstance().isAllStepsCheckTemplates());
                        steps1 = stepFinder.getAllFishes(sudoku, Options.getInstance().getAllStepsMinFishSize(),
                                Options.getInstance().getAllStepsMaxFishSize(), 
                                Options.getInstance().getAllStepsMaxFins(), 
                                Options.getInstance().getAllStepsMaxEndoFins(), dlg, actStep,
                                Options.getInstance().getAllStepsMaxFishType());
                        steps.addAll(steps1);
                        Options.getInstance().setCheckTemplates(oldCheckTemplates);
                    }
                    break;
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                    //System.out.println("Kraken Fish search cand " + (actStep - 9) + ": " + Options.getInstance().allStepsFishCandidates.charAt(actStep - 10));
                    if (isAllStepsEnabled(SolutionType.KRAKEN_FISH) && 
                            Options.getInstance().getAllStepsKrakenFishCandidates().charAt(actStep - 10) == '1') {
                        updateProgress(java.util.ResourceBundle.getBundle("intl/FindAllStepsProgressDialog").getString("FindAllStepsProgressDialog.kraken_fish") + " " + (actStep - 9), actStep);
                        steps1 = stepFinder.getAllKrakenFishes(sudoku, Options.getInstance().getAllStepsKrakenMinFishSize(),
                                Options.getInstance().getAllStepsKrakenMaxFishSize(), 
                                Options.getInstance().getAllStepsMaxKrakenFins(), 
                                Options.getInstance().getAllStepsMaxKrakenEndoFins(), dlg, actStep - 9,
                                Options.getInstance().getAllStepsKrakenMaxFishType());
                        steps.addAll(steps1);
                    }
                    break;
                case 19:
                    updateProgress(java.util.ResourceBundle.getBundle("intl/FindAllStepsProgressDialog").getString("FindAllStepsProgressDialog.uniqueness"), actStep);
                    if (isAllStepsEnabled(SolutionType.UNIQUENESS_1) || 
                            isAllStepsEnabled(SolutionType.UNIQUENESS_2) ||
                            isAllStepsEnabled(SolutionType.UNIQUENESS_3) ||
                            isAllStepsEnabled(SolutionType.UNIQUENESS_4) ||
                            isAllStepsEnabled(SolutionType.UNIQUENESS_5) ||
                            isAllStepsEnabled(SolutionType.UNIQUENESS_6) ||
                            isAllStepsEnabled(SolutionType.HIDDEN_RECTANGLE) ||
                            isAllStepsEnabled(SolutionType.AVOIDABLE_RECTANGLE_1) ||
                            isAllStepsEnabled(SolutionType.AVOIDABLE_RECTANGLE_2)) {
                        steps1 = stepFinder.getAllUniqueness(sudoku);
                        filterSteps(steps1);
                        steps.addAll(steps1);
                    }
                    if (isAllStepsEnabled(SolutionType.BUG_PLUS_1)) {
                        stepFinder.setSudoku(sudoku);
                        SolutionStep result = stepFinder.getStep(SolutionType.BUG_PLUS_1);
                        if (result != null) {
                            steps.add(result);
                        }
                    }
                    steps1 = stepFinder.getAllWings(sudoku);
                    filterSteps(steps1);
                    steps.addAll(steps1);
                    if (isAllStepsEnabled(SolutionType.SIMPLE_COLORS)) {
                        steps1 = stepFinder.findAllSimpleColors(sudoku);
                        steps.addAll(steps1);
                    }
                    if (isAllStepsEnabled(SolutionType.MULTI_COLORS)) {
                        steps1 = stepFinder.findAllMultiColors(sudoku);
                        steps.addAll(steps1);
                    }
                    break;
                case 20:
                    updateProgress(java.util.ResourceBundle.getBundle("intl/FindAllStepsProgressDialog").getString("FindAllStepsProgressDialog.chains"), actStep);
                    if (isAllStepsEnabled(SolutionType.X_CHAIN) || isAllStepsEnabled(SolutionType.XY_CHAIN) ||
                            isAllStepsEnabled(SolutionType.REMOTE_PAIR) || isAllStepsEnabled(SolutionType.TURBOT_FISH)) {
                        steps1 = stepFinder.getAllChains(sudoku);
                        filterSteps(steps1);
                        steps.addAll(steps1);
                    }
                    break;
                case 21:
                    updateProgress(java.util.ResourceBundle.getBundle("intl/FindAllStepsProgressDialog").getString("FindAllStepsProgressDialog.nice_loops"), actStep);
                    if (isAllStepsEnabled(SolutionType.NICE_LOOP)) {
                        steps1 = stepFinder.getAllNiceLoops(sudoku);
                        steps.addAll(steps1);
                    }
                    break;
                case 22:
                    updateProgress(java.util.ResourceBundle.getBundle("intl/FindAllStepsProgressDialog").getString("FindAllStepsProgressDialog.grouped_nice_loops"), actStep);
                    if (isAllStepsEnabled(SolutionType.GROUPED_NICE_LOOP)) {
                        steps1 = stepFinder.getAllGroupedNiceLoops(sudoku);
                        steps.addAll(steps1);
                    }
                    break;
                case 23:
                    updateProgress(java.util.ResourceBundle.getBundle("intl/FindAllStepsProgressDialog").getString("FindAllStepsProgressDialog.templates"), actStep);
                    if (isAllStepsEnabled(SolutionType.TEMPLATE_DEL) || isAllStepsEnabled(SolutionType.TEMPLATE_SET)) {
                        steps1 = stepFinder.getAllTemplates(sudoku);
                        filterSteps(steps1);
                        steps.addAll(steps1);
                    }
                    break;
                case 24:
                    updateProgress(java.util.ResourceBundle.getBundle("intl/FindAllStepsProgressDialog").getString("FindAllStepsProgressDialog.als"), actStep);
                    if (isAllStepsEnabled(SolutionType.ALS_XZ) || isAllStepsEnabled(SolutionType.ALS_XY_WING) ||
                            isAllStepsEnabled(SolutionType.ALS_XY_CHAIN)) {
                        steps1 = stepFinder.getAllAlsSteps(sudoku, isAllStepsEnabled(SolutionType.ALS_XZ),
                                isAllStepsEnabled(SolutionType.ALS_XY_WING),
                                isAllStepsEnabled(SolutionType.ALS_XY_CHAIN));
                        filterSteps(steps1);
                        steps.addAll(steps1);
                    }
                    if (isAllStepsEnabled(SolutionType.DEATH_BLOSSOM)) {
                        steps1 = stepFinder.getAllDeathBlossoms(sudoku);
                        filterSteps(steps1);
                        steps.addAll(steps1);
                    }
                    break;
                case 25:
                    if (isAllStepsEnabled(SolutionType.FORCING_CHAIN)) {
                        updateProgress(java.util.ResourceBundle.getBundle("intl/FindAllStepsProgressDialog").getString("FindAllStepsProgressDialog.forcing_Chains"), actStep);
                        steps1 = stepFinder.getAllForcingChains(sudoku);
                        steps.addAll(steps1);
                    }
                    break;
                case 26:
                    if (isAllStepsEnabled(SolutionType.FORCING_NET)) {
                        updateProgress(java.util.ResourceBundle.getBundle("intl/FindAllStepsProgressDialog").getString("FindAllStepsProgressDialog.forcing_Nets"), actStep);
                        steps1 = stepFinder.getAllForcingNets(sudoku);
                        steps.addAll(steps1);
                    }
                    break;
                case 27:
                    updateProgress(java.util.ResourceBundle.getBundle("intl/FindAllStepsProgressDialog").getString("FindAllStepsProgressDialog.progress_Score"), actStep);
                    // calculate progress measure
                    SudokuSolverFactory.getDefaultSolverInstance().getProgressScore(sudoku, steps, dlg);
                    break;
                default:
                    if (testTypes == null) {
                        Thread.currentThread().interrupt();
                    } else {
                        // called directly -> dont interrupt!
                        return;
                    }
                    break;
            }
            actStep++;
        }
        // done!
        if (dlg != null) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (dlg != null) {
                        dlg.setVisible(false);
                    }
                }
            });
        }
    }

    public List<SolutionStep> getSteps() {
        return steps;
    }

    public void setSteps(List<SolutionStep> steps) {
        this.steps = steps;
    }

    public List<SolutionType> getTestType() {
        return testTypes;
    }

    public void setTestType(List<SolutionType> testStep) {
        this.testTypes = testStep;
    }

    public Sudoku2 getSudoku() {
        return sudoku;
    }

    public void setSudoku(Sudoku2 sudoku) {
        this.sudoku = sudoku;
    }
}
