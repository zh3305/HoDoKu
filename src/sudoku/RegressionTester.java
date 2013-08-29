/*
 * Copyright (C) 2008-12  Bernhard Hobiger, MaNik-e Team
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import solver.SudokuSolverFactory;
import solver.SudokuStepFinder;

/**
 * A class that implements a Regression Tester for HoDoKu
 * 
 * Changes 20090910:
 *   - call specialised solvers for performance reasons
 *   - report unknown techniques
 *   - implement subvariants in techniques
 *   - allow techniques that set values in cells
 *   - allow fail cases (no step of the technique must be available)
 *
 * @author MaNik-e Team, hobiwan
 */
public class RegressionTester {

    private SudokuStepFinder stepFinder;
    private int anzTestCases = 0;
    private int anzGoodCases = 0;
    private int anzBadCases = 0;
    private int anzIgnoreCases = 0;
    private int anzNotImplementedCases = 0;
    private Map<String, Integer> ignoredTechniques = new TreeMap<String, Integer>();
    private Map<String, Integer> notImplementedTechniques = new TreeMap<String, Integer>();
    private Map<String, String> failedCases = new TreeMap<String, String>();
    private boolean fastMode = false;

    public RegressionTester() {
        stepFinder = SudokuSolverFactory.getDefaultSolverInstance().getStepFinder();
    }

    public void runTest(String testFile) {
        runTest(testFile, false);
    }

    public void runTest(String testFile, boolean fastMode) {
        this.fastMode = fastMode;
        String msg = "Starting test run for file " + testFile;
        if (fastMode) {
            msg += " (fast mode)";
        }
        msg += "...";
        System.out.println(msg);
        // reset everything
        anzTestCases = 0;
        anzGoodCases = 0;
        anzBadCases = 0;
        anzIgnoreCases = 0;
        ignoredTechniques.clear();
        failedCases.clear();

        int anzLines = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(testFile));
            String line = null;
            while ((line = in.readLine()) != null) {
                anzLines++;
                //System.out.println("line " + anzLines +": <" + line + ">");
                if ((anzLines % 10) == 0) {
                    System.out.print(".");
                }
                if ((anzLines % 400) == 0) {
                    System.out.println();
                }
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.isEmpty()) {
                    continue;
                }
                test(line);
            }
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "error reading test cases...", ex);
        }
        System.out.println();
        System.out.println("Test finished!");
        System.out.println((anzTestCases) + " cases total");
        System.out.println(anzGoodCases + " tests succeeded");
        System.out.println(anzBadCases + " tests failed");
        System.out.println(anzIgnoreCases + " tests were ignored");
        System.out.println(anzNotImplementedCases + " tests could not be run because the technique is not implemented");
        if (anzIgnoreCases != 0) {
            System.out.println("Ignored techniques:");
            Set<String> keys = ignoredTechniques.keySet();
            for (String key : keys) {
                System.out.println("  " + key + ": " + ignoredTechniques.get(key));
            }
        }
        if (anzNotImplementedCases != 0) {
            System.out.println("Test cases for techniques not implemented:");
            Set<String> keys = notImplementedTechniques.keySet();
            for (String key : keys) {
                System.out.println("  " + key + ": " + notImplementedTechniques.get(key));
            }
        }
        if (anzBadCases != 0) {
            System.out.println("Failed Cases:");
            Set<String> keys = failedCases.keySet();
            for (String key : keys) {
                System.out.println("  Should be:" + key);
                System.out.println("  Was:      " + failedCases.get(key));
            }
        }
    }

    /**
     * Extract the technique needed, the puzzle, the candidates, for which
     * the search should be made, all candidates, that should
     * be deleted, and all cells, that must be set; fail cases start with
     * a minus sign before the technique.
     *
     * Now search for all occurences of that technique in the grid and
     * compare the results.
     * 
     * @param testCase test case sudoku in library format
     */
    public void test(String testCase) {
        anzTestCases++;
//        System.out.println("testCase: " + testCase);
        String[] parts = testCase.split(":");
        // check for variants and fail cases (step must not be found!)
        int variant = 0;
        boolean failCase = false;
        if (parts[1].contains("-")) {
            int vIndex = parts[1].indexOf('-');
            if (parts[1].charAt(vIndex + 1) == 'x') {
                failCase = true;
            } else {
                try {
                    variant = Integer.parseInt(parts[1].substring(vIndex + 1));
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid variant: " + parts[1]);
                    addIgnoredTechnique(testCase);
                    return;
                }
            }
            parts[1] = parts[1].substring(0, vIndex);
            testCase = "";
            for (int i = 0; i < parts.length; i++) {
                testCase += parts[i];
                if (i < 7) {
                    testCase += ":";
                }
            }
            if (parts.length < 7) {
                testCase += ":";
            }
        }
        String start = ":" + parts[1] + ":" + parts[2] + ":";
        //System.out.println("   start: <" + start + ">");
        SolutionType type = SolutionType.getTypeFromLibraryType(parts[1]);
        if (type == null) {
            addIgnoredTechnique(testCase);
            return;
        }

        // Create and set a new Sudoku2
        Sudoku2 sudoku = new Sudoku2();
        //System.out.println(testCase);
        sudoku.setSudoku(testCase);
        //System.out.println("after set: " + sudoku.getSudoku(ClipboardMode.LIBRARY));

        // Find all steps for the technique at the current state
        List<SolutionStep> steps = null;
        List<SolutionStep> steps1 = null;
        boolean oldOption = false;
        boolean oldOption2 = false;
        boolean oldOption3 = false;
        switch (type) {
            case FULL_HOUSE:
                steps = stepFinder.findAllFullHouses(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case HIDDEN_SINGLE:
            case HIDDEN_PAIR:
            case HIDDEN_TRIPLE:
            case HIDDEN_QUADRUPLE:
                steps = stepFinder.findAllHiddenXle(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case NAKED_SINGLE:
            case NAKED_PAIR:
            case NAKED_TRIPLE:
            case NAKED_QUADRUPLE:
                steps = stepFinder.findAllNakedXle(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case LOCKED_PAIR:
            case LOCKED_TRIPLE:
                if (variant == 1) {
                    steps = stepFinder.findAllNakedXle(sudoku);
                    steps1 = stepFinder.findAllHiddenXle(sudoku);
                    steps.addAll(steps1);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzNotImplementedCases++;
                    notImplementedTechniques.put(testCase, 1);
                }
                break;
            case LOCKED_CANDIDATES_1:
            case LOCKED_CANDIDATES_2:
                steps = stepFinder.findAllLockedCandidates(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case SKYSCRAPER:
                oldOption = Options.getInstance().isAllowDualsAndSiamese();
                Options.getInstance().setAllowDualsAndSiamese(false);
                steps = stepFinder.findAllSkyScrapers(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setAllowDualsAndSiamese(oldOption);
                break;
            case TWO_STRING_KITE:
                oldOption = Options.getInstance().isAllowDualsAndSiamese();
                Options.getInstance().setAllowDualsAndSiamese(false);
                steps = stepFinder.findAllTwoStringKites(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setAllowDualsAndSiamese(oldOption);
                break;
            case DUAL_TWO_STRING_KITE:
                oldOption = Options.getInstance().isAllowDualsAndSiamese();
                Options.getInstance().setAllowDualsAndSiamese(true);
                steps = stepFinder.findAllTwoStringKites(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setAllowDualsAndSiamese(oldOption);
                break;
            case EMPTY_RECTANGLE:
                oldOption = Options.getInstance().isAllowErsWithOnlyTwoCandidates();
                if (variant == 1) {
                    Options.getInstance().setAllowErsWithOnlyTwoCandidates(true);
                }
                steps = stepFinder.findAllEmptyRectangles(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setAllowErsWithOnlyTwoCandidates(oldOption);
                break;
            case DUAL_EMPTY_RECTANGLE:
                oldOption = Options.getInstance().isAllowErsWithOnlyTwoCandidates();
                oldOption2 = Options.getInstance().isAllowDualsAndSiamese();
                Options.getInstance().setAllowErsWithOnlyTwoCandidates(true);
                Options.getInstance().setAllowDualsAndSiamese(true);
                steps = stepFinder.findAllEmptyRectangles(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setAllowErsWithOnlyTwoCandidates(oldOption);
                Options.getInstance().setAllowDualsAndSiamese(oldOption2);
                break;
            case SIMPLE_COLORS:
            case SIMPLE_COLORS_TRAP:
            case SIMPLE_COLORS_WRAP:
                steps = stepFinder.findAllSimpleColors(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case MULTI_COLORS:
            case MULTI_COLORS_1:
            case MULTI_COLORS_2:
                steps = stepFinder.findAllMultiColors(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case UNIQUENESS_1:
            case UNIQUENESS_2:
            case UNIQUENESS_3:
            case UNIQUENESS_4:
            case UNIQUENESS_5:
            case UNIQUENESS_6:
            case HIDDEN_RECTANGLE:
            case AVOIDABLE_RECTANGLE_1:
            case AVOIDABLE_RECTANGLE_2:
                oldOption = Options.getInstance().isAllowUniquenessMissingCandidates();
                if (variant == 1) {
                    Options.getInstance().setAllowUniquenessMissingCandidates(false);
                } else if (variant == 2) {
                    Options.getInstance().setAllowUniquenessMissingCandidates(true);
                }
                steps = stepFinder.getAllUniqueness(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setAllowUniquenessMissingCandidates(oldOption);
                break;
            case BUG_PLUS_1:
                steps = new ArrayList<SolutionStep>();
                stepFinder.setSudoku(sudoku);
                SolutionStep step = stepFinder.getStep(type);
                if (step != null) {
                    steps.add(step);
                }
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case XY_WING:
            case XYZ_WING:
            case W_WING:
                steps = stepFinder.getAllWings(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case TURBOT_FISH:
            case X_CHAIN:
            case XY_CHAIN:
            case REMOTE_PAIR:
                oldOption = Options.getInstance().isOnlyOneChainPerStep();
                Options.getInstance().setOnlyOneChainPerStep(false);
                steps = stepFinder.getAllChains(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setOnlyOneChainPerStep(oldOption);
                break;
            case CONTINUOUS_NICE_LOOP:
            case DISCONTINUOUS_NICE_LOOP:
            case AIC:
                oldOption = Options.getInstance().isOnlyOneChainPerStep();
                Options.getInstance().setOnlyOneChainPerStep(false);
                steps = stepFinder.getAllNiceLoops(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setOnlyOneChainPerStep(oldOption);
                break;
            case GROUPED_CONTINUOUS_NICE_LOOP:
            case GROUPED_DISCONTINUOUS_NICE_LOOP:
            case GROUPED_AIC:
                oldOption = Options.getInstance().isOnlyOneChainPerStep();
                oldOption2 = Options.getInstance().isAllowAlsInTablingChains();
                Options.getInstance().setOnlyOneChainPerStep(false);
                if ((type == SolutionType.GROUPED_CONTINUOUS_NICE_LOOP && variant == 2)
                        || (type == SolutionType.GROUPED_DISCONTINUOUS_NICE_LOOP && (variant == 3 || variant == 4))
                        || (type == SolutionType.GROUPED_AIC && (variant == 3 || variant == 4))) {
                    Options.getInstance().setAllowAlsInTablingChains(true);
                } else {
                    Options.getInstance().setAllowAlsInTablingChains(false);
                }
                steps = stepFinder.getAllGroupedNiceLoops(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setOnlyOneChainPerStep(oldOption);
                Options.getInstance().setAllowAlsInTablingChains(oldOption2);
                break;
            case X_WING:
            case FINNED_X_WING:
            case SASHIMI_X_WING:
                steps = findAllFishes(sudoku, 2, 0);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case FRANKEN_X_WING:
            case FINNED_FRANKEN_X_WING:
                steps = findAllFishes(sudoku, 2, 1);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case MUTANT_X_WING:
            case FINNED_MUTANT_X_WING:
                steps = findAllFishes(sudoku, 2, 2);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case SWORDFISH:
            case FINNED_SWORDFISH:
            case SASHIMI_SWORDFISH:
                oldOption = Options.getInstance().isAllowDualsAndSiamese();
                Options.getInstance().setAllowDualsAndSiamese(true);
                steps = findAllFishes(sudoku, 3, 0);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setAllowDualsAndSiamese(oldOption);
                break;
            case FRANKEN_SWORDFISH:
            case FINNED_FRANKEN_SWORDFISH:
                oldOption = Options.getInstance().isAllowDualsAndSiamese();
                Options.getInstance().setAllowDualsAndSiamese(true);
                if (!fastMode) {
                    steps = findAllFishes(sudoku, 3, 1);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzIgnoreCases++;
                    ignoredTechniques.put(testCase, 1);
                }
                Options.getInstance().setAllowDualsAndSiamese(oldOption);
                break;
            case MUTANT_SWORDFISH:
            case FINNED_MUTANT_SWORDFISH:
                oldOption = Options.getInstance().isAllowDualsAndSiamese();
                Options.getInstance().setAllowDualsAndSiamese(true);
                if (!fastMode) {
                    steps = findAllFishes(sudoku, 3, 2);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzIgnoreCases++;
                    ignoredTechniques.put(testCase, 1);
                }
                Options.getInstance().setAllowDualsAndSiamese(oldOption);
                break;
            case JELLYFISH:
            case FINNED_JELLYFISH:
            case SASHIMI_JELLYFISH:
                oldOption = Options.getInstance().isAllowDualsAndSiamese();
                Options.getInstance().setAllowDualsAndSiamese(true);
                steps = findAllFishes(sudoku, 4, 0);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setAllowDualsAndSiamese(oldOption);
                break;
            case FRANKEN_JELLYFISH:
            case FINNED_FRANKEN_JELLYFISH:
                oldOption = Options.getInstance().isAllowDualsAndSiamese();
                Options.getInstance().setAllowDualsAndSiamese(true);
                if (!fastMode) {
                    steps = findAllFishes(sudoku, 4, 1);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzIgnoreCases++;
                    ignoredTechniques.put(testCase, 1);
                }
                Options.getInstance().setAllowDualsAndSiamese(oldOption);
                break;
            case MUTANT_JELLYFISH:
            case FINNED_MUTANT_JELLYFISH:
                oldOption = Options.getInstance().isAllowDualsAndSiamese();
                Options.getInstance().setAllowDualsAndSiamese(true);
                if (!fastMode) {
                    steps = findAllFishes(sudoku, 4, 2);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzIgnoreCases++;
                    ignoredTechniques.put(testCase, 1);
                }
                Options.getInstance().setAllowDualsAndSiamese(oldOption);
                break;
            case SQUIRMBAG:
            case FINNED_SQUIRMBAG:
            case SASHIMI_SQUIRMBAG:
                steps = findAllFishes(sudoku, 5, 0);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case FRANKEN_SQUIRMBAG:
            case FINNED_FRANKEN_SQUIRMBAG:
                if (!fastMode) {
                    steps = findAllFishes(sudoku, 5, 1);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzIgnoreCases++;
                    ignoredTechniques.put(testCase, 1);
                }
                break;
            case MUTANT_SQUIRMBAG:
            case FINNED_MUTANT_SQUIRMBAG:
                if (!fastMode) {
                    steps = findAllFishes(sudoku, 5, 2);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzIgnoreCases++;
                    ignoredTechniques.put(testCase, 1);
                }
                break;
            case WHALE:
            case FINNED_WHALE:
            case SASHIMI_WHALE:
                if (!fastMode) {
                    steps = findAllFishes(sudoku, 6, 0);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzIgnoreCases++;
                    ignoredTechniques.put(testCase, 1);
                }
                break;
            case FRANKEN_WHALE:
            case FINNED_FRANKEN_WHALE:
                if (!fastMode) {
                    steps = findAllFishes(sudoku, 6, 1);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzIgnoreCases++;
                    ignoredTechniques.put(testCase, 1);
                }
                break;
            case MUTANT_WHALE:
            case FINNED_MUTANT_WHALE:
                if (!fastMode) {
                    steps = findAllFishes(sudoku, 6, 2);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzIgnoreCases++;
                    ignoredTechniques.put(testCase, 1);
                }
                break;
            case LEVIATHAN:
            case FINNED_LEVIATHAN:
            case SASHIMI_LEVIATHAN:
                if (!fastMode) {
                    steps = findAllFishes(sudoku, 7, 0);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzIgnoreCases++;
                    ignoredTechniques.put(testCase, 1);
                }
                break;
            case FRANKEN_LEVIATHAN:
            case FINNED_FRANKEN_LEVIATHAN:
                if (!fastMode) {
                    steps = findAllFishes(sudoku, 7, 1);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzIgnoreCases++;
                    ignoredTechniques.put(testCase, 1);
                }
                break;
            case MUTANT_LEVIATHAN:
            case FINNED_MUTANT_LEVIATHAN:
                if (!fastMode) {
                    steps = findAllFishes(sudoku, 7, 2);
                    checkResults(testCase, steps, sudoku, start, failCase);
                } else {
                    anzIgnoreCases++;
                    ignoredTechniques.put(testCase, 1);
                }
                break;
            case SUE_DE_COQ:
                steps = stepFinder.getAllSueDeCoqs(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case ALS_XZ:
            case ALS_XY_WING:
            case ALS_XY_CHAIN:
                oldOption = Options.getInstance().isOnlyOneAlsPerStep();
                oldOption2 = Options.getInstance().isAllowAlsOverlap();
                oldOption3 = Options.getInstance().isAllStepsAlsChainForwardOnly();
                int oldOption4 = Options.getInstance().getAllStepsAlsChainLength();
                Options.getInstance().setOnlyOneAlsPerStep(false);
                Options.getInstance().setAllowAlsOverlap(false);
                Options.getInstance().setAllStepsAlsChainForwardOnly(false);
                Options.getInstance().setAllStepsAlsChainLength(6);
                if ((type == SolutionType.ALS_XY_CHAIN && variant == 2)
                        || (type == SolutionType.ALS_XY_WING && variant == 2)) {
                    Options.getInstance().setAllowAlsOverlap(true);
                }
                steps = stepFinder.getAllAlsSteps(sudoku, type == SolutionType.ALS_XZ,
                        type == SolutionType.ALS_XY_WING,
                        type == SolutionType.ALS_XY_CHAIN);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setOnlyOneAlsPerStep(oldOption);
                Options.getInstance().setAllowAlsOverlap(oldOption2);
                Options.getInstance().setAllStepsAlsChainForwardOnly(oldOption3);
                Options.getInstance().setAllStepsAlsChainLength(oldOption4);
                break;
            case DEATH_BLOSSOM:
                oldOption = Options.getInstance().isOnlyOneAlsPerStep();
                oldOption2 = Options.getInstance().isAllowAlsOverlap();
                Options.getInstance().setOnlyOneAlsPerStep(false);
                Options.getInstance().setAllowAlsOverlap(false);
                if (variant == 2) {
                    Options.getInstance().setAllowAlsOverlap(true);
                }
                steps = stepFinder.getAllDeathBlossoms(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setOnlyOneAlsPerStep(oldOption);
                Options.getInstance().setAllowAlsOverlap(oldOption2);
                break;
            case TEMPLATE_SET:
            case TEMPLATE_DEL:
                steps = stepFinder.getAllTemplates(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                break;
            case FORCING_CHAIN_CONTRADICTION:
            case FORCING_CHAIN_VERITY:
                oldOption = Options.getInstance().isOnlyOneChainPerStep();
                oldOption2 = Options.getInstance().isAllowAlsInTablingChains();
                Options.getInstance().setOnlyOneChainPerStep(false);
                Options.getInstance().setAllowAlsInTablingChains(false);
                steps = stepFinder.getAllForcingChains(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setOnlyOneChainPerStep(oldOption);
                Options.getInstance().setAllowAlsInTablingChains(oldOption2);
                break;
            case FORCING_NET_CONTRADICTION:
            case FORCING_NET_VERITY:
                oldOption = Options.getInstance().isOnlyOneChainPerStep();
                oldOption2 = Options.getInstance().isAllowAlsInTablingChains();
                Options.getInstance().setOnlyOneChainPerStep(false);
                Options.getInstance().setAllowAlsInTablingChains(false);
                steps = stepFinder.getAllForcingNets(sudoku);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setOnlyOneChainPerStep(oldOption);
                Options.getInstance().setAllowAlsInTablingChains(oldOption2);
                break;
            case KRAKEN_FISH_TYPE_1:
            case KRAKEN_FISH_TYPE_2:
                oldOption = Options.getInstance().isOnlyOneFishPerStep();
                oldOption2 = Options.getInstance().isCheckTemplates();
                Options.getInstance().setOnlyOneFishPerStep(false);
                Options.getInstance().setCheckTemplates(true);
                steps = stepFinder.getAllKrakenFishes(sudoku, 2, 4,
                        Options.getInstance().getAllStepsMaxFins(),
                        Options.getInstance().getAllStepsMaxEndoFins(), null, -1, 1);
                checkResults(testCase, steps, sudoku, start, failCase);
                Options.getInstance().setOnlyOneFishPerStep(oldOption);
                Options.getInstance().setCheckTemplates(oldOption2);
                break;
            default:
                anzIgnoreCases++;
                addIgnoredTechnique(testCase);
                break;
        }
    }

    private List<SolutionStep> findAllFishes(Sudoku2 sudoku, int size, int type) {
        boolean oldOption = Options.getInstance().isOnlyOneFishPerStep();
        boolean oldOption2 = Options.getInstance().isCheckTemplates();
        Options.getInstance().setOnlyOneFishPerStep(false);
        Options.getInstance().setCheckTemplates(true);
        List<SolutionStep> steps = stepFinder.getAllFishes(sudoku, size, size,
                Options.getInstance().getAllStepsMaxFins(),
                Options.getInstance().getAllStepsMaxEndoFins(), null, -1, type);
        Options.getInstance().setOnlyOneFishPerStep(oldOption);
        Options.getInstance().setCheckTemplates(oldOption2);
        return steps;
    }

    /**
     * Checking the result of a test is a bit more complicated that it looks:
     * We have to assure, that the case has only one solution and that that
     * solution is correct. If a good and a bad result is present the test failes.
     *
     * With chains more than one chain with different chain lengths may exist. This
     * has to be tested separately.
     * 
     * @param testCase
     * @param steps
     * @param sudoku
     * @param start
     * @param failCase
     */
    private void checkResults(String testCase, List<SolutionStep> steps, Sudoku2 sudoku,
            String start, boolean failCase) {
        boolean found = false;
        boolean exactMatch = false;
        boolean good = true; // always be optimistic...
        for (SolutionStep step : steps) {
            String result = sudoku.getSudoku(ClipboardMode.LIBRARY, step);
            if (result.startsWith(start)) {
                found = true;
                // should match case!
                // careful: for chains one exact match has to be found,
                // but if a match already exists, a deviation in chain
                // length only does not give a fail case!
                if (!result.equals(testCase)) {
                    if (exactMatch == true) {
                        // test for everything but <comment>
                        int index1 = testCase.lastIndexOf(':');
                        int index2 = result.lastIndexOf(':');
                        if (testCase.substring(0, index1).equals(result.substring(0, index2))) {
                            // does not constitue a fail case!
                            continue;
                        }
                    }
                    good = false;
                    failedCases.put(testCase, result);
                } else {
                    exactMatch = true;
                }
            }
        }
        if (failCase) {
            if (found) {
                anzBadCases++;
                failedCases.put(testCase, "Step found for fail case!");
            } else {
                anzGoodCases++;
            }
        } else {
            if (!found) {
                anzBadCases++;
                failedCases.put(testCase, "No step found!");
            } else if (!good) {
                anzBadCases++;
            } else {
                anzGoodCases++;
            }
        }
    }

    private void addIgnoredTechnique(String technique) {
        int count = 1;
        if (ignoredTechniques.containsKey(technique)) {
            count = ignoredTechniques.get(technique);
            count++;
        }
        ignoredTechniques.put(technique, count);
        anzIgnoreCases++;
    }

    private void addNotImplementedTechnique(String technique) {
        int count = 1;
        if (notImplementedTechniques.containsKey(technique)) {
            count = notImplementedTechniques.get(technique);
            count++;
        }
        notImplementedTechniques.put(technique, count);
        anzNotImplementedCases++;
    }

    public static void main(String[] args) {
        RegressionTester tester = new RegressionTester();
//        boolean result = tester.test(":0100:3:.....4..9.49....2.172..9..5......8..3...7...6..5......4..5..698.9....7..6..39....::315 317 318:");
//        System.out.println("Result: " + result);
        tester.runTest("lib02.txt");
    }
}
