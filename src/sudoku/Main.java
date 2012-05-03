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

import generator.BackgroundGeneratorThread;
import generator.SudokuGenerator;
import generator.SudokuGeneratorFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import solver.SudokuSolver;
import solver.SudokuSolverFactory;

/**
 *
 * @author hobiwan
 */
public class Main {

    /** Maintain strong references to Loggers that have been configured manually */
    private static List<Logger> loggers = new ArrayList<Logger>();
    /** Value of the property "os.name" */
    public static String OS_NAME = "";

    /** Creates a new instance of Main */
    public Main() {
    }

    public String getSrcDir() {
        String path = getClass().getClassLoader().getResource("sudoku").toExternalForm().toLowerCase();
        if (path.startsWith("jar")) {
            path = path.substring(10, path.indexOf("hodoku.jar"));
        } else {
            path = path.substring(6, path.indexOf("build"));
        }
        return path;
    }

    @SuppressWarnings("empty-statement")
    void searchForType(List<StepType> typeList, DifficultyLevel level, String outFile) {
        //Logger.getLogger(getClass().getName()).log(Level.INFO, "Starting search for " + type.getStepName());
        System.out.println("Starting search for:");
        if (typeList.size() > 0) {
            for (StepType tmpType : typeList) {
                System.out.println("   " + tmpType);
            }
        }
        if (level != null) {
            System.out.println("   " + level.getName());
        }
        SearchForTypeThread thread = new SearchForTypeThread(this, typeList, level, outFile);
        thread.start();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            //20120112: Pressing <ctrl><c> makes readLine() return null
            // which leads to a NullPointerException
//            while (in.readLine().compareTo("q") != 0) {
//                ;
//            }
            String line = null;
            while ((line = in.readLine()) != null) {
                if (line.compareTo("q") == 0) {
                    break;
                }
            }
        } catch (IOException ex) {
            System.out.println("Error reading from console");
        }
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException ex) {
            System.out.println("Interrupted waiting for search thread");
        }
        System.out.println("Gesamt: " + thread.getAnz() + " Sudoku erzeugt (" + thread.getAnzFound() + " Treffer)");
    }

    public void batchSolve(String fileName, String puzzleString, boolean printSolution, boolean printSolutionPath,
            boolean printStatistic,
            ClipboardMode cMode, Set<SolutionType> types, String outFile, boolean findAllSteps) {
        batchSolve(fileName, puzzleString, printSolution, printSolutionPath, printStatistic, cMode, types, outFile, findAllSteps, false, null);
    }

    @SuppressWarnings("CallToThreadDumpStack")
    public void batchSolve(String fileName, String puzzleString, boolean printSolution, boolean printSolutionPath,
            boolean printStatistic, ClipboardMode cMode, Set<SolutionType> types, String outFile, boolean findAllSteps,
            boolean bruteForceTest, List<SolutionType> testTypes) {
        BatchSolveThread thread = new BatchSolveThread(fileName, puzzleString, printSolution, printSolutionPath, printStatistic,
                cMode, types, outFile, findAllSteps, bruteForceTest, testTypes);
        thread.start();
        ShutDownThread st = new ShutDownThread(thread);
        Runtime.getRuntime().addShutdownHook(st);
        try {
            thread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "join interrupted...", ex);
        }
        try {
            Runtime.getRuntime().removeShutdownHook(st);
        } catch (Exception ex) {
        }
        int min = (int) (thread.getTicks() / 60000);
        int sec = (int) (thread.getTicks() % 60000);
        int ms = sec % 1000;
        sec /= 1000;
        int hours = min / 60;
        min -= (hours * 60);
        System.out.printf("%d puzzles in %dms (%d:%02d:%02d.%03d)\r\n", thread.getCount(), thread.getTicks(), hours, min, sec, ms);
//        System.out.println(thread.getCount() + " puzzles in " + thread.getTicks() + "ms (" + hours + ":" + min + ":" + sec + "." + ms + ")");
        System.out.printf("%.03f ms per puzzle\r\n", (thread.getTicks() / (double) thread.getCount()));
        System.out.println(thread.getBruteForceAnz() + " puzzles require guessing!");
        System.out.println(thread.getTemplateAnz() + " puzzles require templates!");
        System.out.println(thread.getGivenUpAnz() + " puzzles unsolved!");
        System.out.println(thread.getUnsolvedAnz() + " puzzles not solved logically!");
        System.out.println();
        for (int i = 1; i < thread.getResultLength(); i++) {
            System.out.println("   " + Options.DEFAULT_DIFFICULTY_LEVELS[i].getName() + ": " + thread.getResult(i));
        }
        if (printStatistic) {
            System.out.println();
            try {
                thread.printStatistic(null, false);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            SudokuSolverFactory.getDefaultSolverInstance().getStepFinder().printStatistics();
        }
    }

    void sortPuzzleFile(String fileName, List<StepType> typeList, String outFileName) {
        try {
            if (typeList.size() > 0) {
                System.out.println("Filter:");
                for (StepType tmpType : typeList) {
                    System.out.println("   " + tmpType);
                }
            }
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            BufferedWriter out = null;
            if (outFileName == null) {
                outFileName = fileName + ".out.txt";
            }
            if (!outFileName.equals("stdout")) {
                out = new BufferedWriter(new FileWriter(outFileName));
            }
            List<String> puzzleList = new ArrayList<String>();
            String line = null;
            int gesAnz = 0;
            while ((line = in.readLine()) != null) {
                gesAnz++;
                boolean includePuzzle = true;
                if (line.contains("#") && typeList.size() > 0) {
                    includePuzzle = false;
                    // determine puzzle type
                    String inputStr = line.substring(line.indexOf('#') + 1).trim();
                    int puzzleType = 3;
                    String[] types = inputStr.split(" ");
                    for (int i = 0; i < types.length; i++) {
                        if (types[i].equals("x")) {
                            puzzleType = 0;
                            break;
                        }
                    }
//                    if (inputStr.contains("x")) {
//                        puzzleType = 0;
                    if (puzzleType == 3) {
                        if (inputStr.startsWith("ssts")) {
                            puzzleType = 2;
                        }
                        if (inputStr.endsWith("ssts")) {
                            puzzleType = 1;
                        }
                    }
                    String typeStr = null;
                    int compAnz = 0;
                    String[] parts = inputStr.split(" ");
                    if (parts.length > 1) {
                        typeStr = parts[1];
                    } else {
                        // invalid type in input -> nothing to apply
                        continue;
                    }
                    int index1 = typeStr.indexOf('(');
                    int index2 = typeStr.indexOf(')');
                    String orgTypeStr = typeStr;
                    if (index1 != -1) {
                        typeStr = typeStr.substring(0, index1);
                        if (index2 != -1) {
                            String anzStr = orgTypeStr.substring(index1 + 1, index2);
                            if (anzStr.length() > 0) {
                                compAnz = Integer.parseInt(anzStr);
                            }
                        }
                    }
                    // apply filter
                    for (StepType actType : typeList) {
                        if (typeStr.equals(actType.type.getArgName()) && puzzleType >= actType.puzzleType) {
                            // filter fits, do comparison
                            switch (actType.compType) {
                                case StepType.EQUAL:
                                    if (compAnz == actType.compAnz) {
                                        includePuzzle = true;
                                    }
                                    break;
                                case StepType.LT:
                                    if (compAnz < actType.compAnz) {
                                        includePuzzle = true;
                                    }
                                    break;
                                case StepType.GT:
                                    if (compAnz > actType.compAnz) {
                                        includePuzzle = true;
                                    }
                                    break;
                                default:
                                    includePuzzle = true;
                                    break;
                            }
                        }
                        if (includePuzzle) {
                            break;
                        }
                    }
                }
                if (includePuzzle) {
                    puzzleList.add(line);
                }
            }
            in.close();
            int anz = puzzleList.size();
            Collections.sort(puzzleList, new Comparator<String>() {

                @Override
                public int compare(String s1, String s2) {
                    int index1 = s1.indexOf('#');
                    int index2 = s2.indexOf('#');
                    if (index1 == -1 && index2 == -1) {
                        return s1.compareTo(s2);
                    } else if (index1 == -1 && index2 != -1) {
                        return -1;
                    } else if (index1 != -1 && index2 == -1) {
                        return 1;
                    } else {
                        return s1.substring(index1).compareTo(s2.substring(index2));
                    }
                }
            });
            for (String key : puzzleList) {
                if (out != null) {
                    out.write(key);
                    out.newLine();
                } else {
                    System.out.println(key);
                }
            }
            if (out != null) {
                out.close();
            }
            System.out.println(anz + " puzzles sorted (" + gesAnz + ")!");
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Error sorting puzzle file", ex);
        }
    }

    /**
     * @param args the command line arguments
     * @throws IOException  
     */
    public static void main(String[] args) throws IOException {
        // Logging: Standardmäßig auf die Console, ins Logfile nur Exceptions
        Handler fh = new FileHandler("%t/hodoku.log", false);
        fh.setFormatter(new SimpleFormatter());
        fh.setLevel(Level.SEVERE);
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(fh);
        rootLogger.setLevel(Level.CONFIG);
//        rootLogger.setLevel(Level.ALL);
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(Level.ALL);
                //handler.setLevel(Level.CONFIG);
                //handler.setLevel(Level.SEVERE);
                //handler.setLevel(Level.FINER);
            }
            handler.setLevel(Level.ALL);
        }
        // When configuring a logger we need a strong reference or
        // it may be garbage collected and the configuration will be lost
        Logger logger = null;
        //Logger.getLogger(Sudoku2.class.getName()).setLevel(Level.FINER);
        //Logger.getLogger(FishSolver.class.getName()).setLevel(Level.FINER);
        //Logger.getLogger(TablingSolver.class.getName()).setLevel(Level.FINER);
        loggers.add(logger = Logger.getLogger(SudokuSolver.class.getName()));
        //logger.setLevel(Level.FINER);

        Logger.getLogger(Main.class.getName()).log(Level.CONFIG, "java.io.tmpdir={0}", System.getProperty("java.io.tmpdir"));
        Logger.getLogger(Main.class.getName()).log(Level.CONFIG, "user.dir={0}", System.getProperty("user.dir"));
        Logger.getLogger(Main.class.getName()).log(Level.CONFIG, "user.home={0}", System.getProperty("user.home"));
        Logger.getLogger(Main.class.getName()).log(Level.CONFIG, "launch4j.exedir={0}", System.getProperty("launch4j.exedir"));

        // Determine OS name
        OS_NAME = System.getProperty("os.name");
        if (OS_NAME != null) {
            OS_NAME = OS_NAME.toLowerCase();
        }

        // Optionen lesen (macht getInstance())
        // if a file hodoku.hcfg exists in the directory from where the program
        // was started, it is loaded automatically
        Options.getInstance();
        String path = System.getProperty("launch4j.exedir");
        if (path == null) {
            URL startURL = Main.class.getResource("/sudoku/Main.class");
            path = startURL.getPath();
            Logger.getLogger(Main.class.getName()).log(Level.CONFIG, "Startup path: {0}", path);
            // 20120116: CAUTION - jar file might be renamed, dont rely on "hodoku.jar"!
            if (path.contains(".jar!")) {
                // started from jar file: find path only
                // format is: file:/C:/Sudoku/hodoku.jar!/sudoku/Main.class
                // format on Ubuntu: file:/home/ubuntu/Desktop/hodoku.jar!/sudoku/Main.class
                int startIndex = 5;
                if (OS_NAME.startsWith("windows")) {
                    startIndex = 6;
                }
                String tmp = path.substring(startIndex, path.indexOf(".jar!"));
                int index = tmp.lastIndexOf('/');
                if (index > 0) {
                    path = tmp.substring(0, index);
                }
            } else if (path.contains("/build/classes")) {
                // format is (Netbeans only): /C:/Sudoku/Alles%20rund%20um%20HoDoKu/HoDoKu/build/classes/sudoku/Main.class
                int startIndex = 0;
                if (OS_NAME.startsWith("windows")) {
                    startIndex = 1;
                }
                path = path.substring(startIndex, path.indexOf("/build/classes"));
            } else if (path.contains("/bin/sudoku/Main")) {
                // format (Eclipse. Ubuntu): /home/ubuntu/HoDoKuEclipse/bin/sudoku/Main.class
                int startIndex = 0;
                if (OS_NAME.startsWith("windows")) {
                    startIndex = 1;
                }
                path = path.substring(startIndex, path.indexOf("/bin/sudoku/Main"));
            }
            path = path.replaceAll("%20", " ");
        }
        File configFile = new File(path + File.separator + Options.FILE_NAME);
        boolean needToResetPuzzles = false;
        if (configFile.exists()) {
            Logger.getLogger(Main.class.getName()).log(Level.CONFIG, "Reading options from {0}", configFile.getPath());
            Options.readOptions(configFile.getPath());
            needToResetPuzzles = true;
        } else {
            Logger.getLogger(Main.class.getName()).log(Level.CONFIG, "No config file found: <{0}>", configFile.getPath());
        }

        // set locale
        if (!Options.getInstance().getLanguage().isEmpty()) {
            Locale.setDefault(new Locale(Options.getInstance().getLanguage()));
        }
        // adjust names of difficulty levels
        Options.getInstance().resetDifficultyLevelStrings();

        // set laf; if a laf is set in options, check if it exists
        // change font sizes if needed
        // the LaF is set here because the console window of the
        // exe version should use the correct laF too
//        SudokuUtil.printFontDefaults();
        SudokuUtil.setLookAndFeel();
//        SudokuUtil.printFontDefaults();

        // to detect whether launch4j is used, a constant command line parameter
        // "/launch4j" is used for the exe version; detect it
        // "/gui" is used to start the GUI with a hcfg or hsol file
        boolean launch4jUsed = false;
        boolean launchGui = false;
        String launchFile = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("/launch4j")) {
                launch4jUsed = true;
            }
            if (args[i].equalsIgnoreCase("/gui")) {
                launchGui = true;
            }
            if (args[i].toLowerCase().endsWith("hsol") || args[i].toLowerCase().endsWith("hcfg")) {
                launchFile = args[i];
            }
        }
        // handle command line arguments
        SudokuConsoleFrame consoleFrame = null;
        if (!launchGui && (launch4jUsed && args.length > 1 || !launch4jUsed && args.length > 0)) {
//            for (int i = 0; i < args.length; i++) {
//                System.out.println("args[" + i + "]: <" + args[i] + ">");
//            }
            // open a fake console when HoDoKu is started as exe file
            // problem: console is null when using pipeing or input redirection
//            if (System.console() == null) {
            if (launch4jUsed) {
                //JOptionPane.showMessageDialog(null, "Program has no console!", "Console error", JOptionPane.ERROR_MESSAGE);
                //System.out.println("no console!");
                consoleFrame = new SudokuConsoleFrame();
                consoleFrame.setVisible(true);
            }
//            System.out.println(path);
            // copyright notice
            System.out.println(MainFrame.VERSION + " - " + MainFrame.BUILD);
            System.out.println("Copyright (C) 2008-12  Bernhard Hobiger\r\n"
                    + "\r\n"
                    + "HoDoKu is free software: you can redistribute it and/or modify\r\n"
                    + "it under the terms of the GNU General Public License as published by\r\n"
                    + "the Free Software Foundation, either version 3 of the License, or\r\n"
                    + "(at your option) any later version.\r\n\r\n");
            // all options are stored in a list, if /f is present, the list is
            // expanded accordingly (/f may be present more than once)
            List<String> options = new ArrayList<String>();
            for (int i = 0; i < args.length; i++) {
                //System.out.println("args original: " + args[i]);
                if (args[i].equals("/launch4j")) {
                    // ignore it
                    continue;
                }
                if (args[i].equals("/f")) {
                    if (i + 1 >= args.length) {
                        System.out.println("No options file given: /f ignored");
                    } else {
                        try {
                            System.out.println("reading options from file '" + args[i + 1] + "'");
                            BufferedReader in = new BufferedReader(new FileReader(args[i + 1]));
                            StringBuilder tmpOptions = new StringBuilder();
                            String line = null;
                            while ((line = in.readLine()) != null) {
                                tmpOptions.append(line.trim()).append(" ");
                            }
                            in.close();
                            //String[] tmpOptionsArray = tmpOptions.toString().split(" ");
                            String[] tmpOptionsArray = getOptionsFromStringBuilder(tmpOptions);
                            for (int j = 0; j < tmpOptionsArray.length; j++) {
                                String opt = tmpOptionsArray[j].trim();
                                if (!opt.isEmpty()) {
                                    options.add(tmpOptionsArray[j]);
                                }
                            }
                        } catch (Exception ex) {
                            System.out.println("Can't read from file '" + args[i + 1] + "': /f ignored");
                        }
                        i++;
                    }
                } else if (args[i].equals("/stdin")) {
                    // read options from stdin
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                        StringBuilder tmpOptions = new StringBuilder();
                        String line = null;
                        while ((line = in.readLine()) != null) {
                            //System.out.println("Line: <" + line + ">");
                            tmpOptions.append(line.trim()).append(" ");
                        }
                        in.close();
                        //String[] tmpOptionsArray = tmpOptions.toString().split(" ");
                        String[] tmpOptionsArray = getOptionsFromStringBuilder(tmpOptions);
                        for (int j = 0; j < tmpOptionsArray.length; j++) {
                            String opt = tmpOptionsArray[j].trim();
                            if (!opt.isEmpty()) {
                                options.add(tmpOptionsArray[j]);
                            }
                        }
                    } catch (Exception ex) {
                        System.out.println("Can't read from stdin: /stdin ignored");
                    }
                } else {
                    options.add(args[i]);
                }
            }
//            for (String opt : options) {
//                System.out.println("option: " + opt);
//            }
            // if a custom console is used, it is safe now to redirect input
            if (consoleFrame != null) {
                consoleFrame.setIn();
            }
            // store all args in a map (except puzzle string)
            String puzzleString = null;
            Map<String, String> argMap = new TreeMap<String, String>();
            args = null; // safe guard against refactoring error
            for (int i = 0; i < options.size(); i++) {
                String arg = options.get(i).trim().toLowerCase();
                if (arg.equals("/bs") || arg.equals("/vg") || arg.equals("/sc")
                        || arg.equals("/sl")
                        || arg.equals("/so") || arg.equals("/c") || arg.equals("/o")
                        || arg.equals("/bsaf") || arg.equals("/bts") || arg.equals("/bt")
                        || arg.equals("/test") || arg.equals("/testf") || arg.equals("/vf")
                        || (arg.equals("/s") && (i + 1 < options.size()) && options.get(i + 1).trim().charAt(0) != '/')) {
                    // args with parameters (only one parameter per arg permitted)
                    if (i + 1 >= options.size() || options.get(i + 1).trim().charAt(0) == '/') {
                        System.out.println("No value for parameter: '" + arg + "' ignored!");
                    } else {
                        if (arg.equals("/s")) {
                            argMap.put("/s", null);
                            argMap.put("/sc", options.get(i + 1));
                        } else {
                            argMap.put(arg, options.get(i + 1));
                        }
                        i++;
                    }
                } else {
                    // args without parameters (could be puzzle)
                    if (arg.charAt(0) == '/') {
                        argMap.put(arg, null);
                    } else {
                        // has to be puzzle
                        puzzleString = arg;
                        // could be PM - format check is impossible
//                        if (puzzleString.length() != 81) {
//                            System.out.println("Puzzle string is not 81 characters long - ignored!");
//                            puzzleString = null;
//                        } else {
//                            for (int j = 0; j < puzzleString.length(); j++) {
//                                if (!Character.isDigit(arg.charAt(j)) && arg.charAt(j) != '.') {
//                                    System.out.println("Invalid character in puzzle string (" +
//                                            puzzleString.charAt(j) + ") - puzzle ignored!");
//                                    puzzleString = null;
//                                }
//                            }
//                        }
                    }
                }
            }
            // now check for args
            String helpArg = null;
            if (argMap.containsKey("/h")) {
                helpArg = "/h";
            }
            if (argMap.containsKey("/?")) {
                helpArg = "/?";
            }
            if (helpArg != null) {
                printHelpScreen();
                printIgnoredOptions(helpArg, argMap);
                if (consoleFrame == null) {
                    System.exit(0);
                }
                return;
            }
            if (argMap.containsKey("/testf")) {
                RegressionTester tester = new RegressionTester();
                tester.runTest(argMap.get("/testf"), true);
                if (consoleFrame == null) {
                    System.exit(0);
                }
                return;
            }
            if (argMap.containsKey("/test")) {
                RegressionTester tester = new RegressionTester();
                tester.runTest(argMap.get("/test"));
                if (consoleFrame == null) {
                    System.exit(0);
                }
                return;
            }
            if (argMap.containsKey("/lt")) {
                printIgnoredOptions("/lt", argMap);
                SortedMap<String, String> tmpMap = new TreeMap<String, String>();
                for (SolutionType tmpType : SolutionType.values()) {
                    tmpMap.put(tmpType.getStepName(), tmpType.getArgName());
                }
                System.out.println("List of Techniques:");
                for (String stepName : tmpMap.keySet()) {
                    System.out.printf("%6s:%s\r\n", tmpMap.get(stepName), stepName);
                }
                System.out.println("Done!");
                if (consoleFrame == null) {
                    System.exit(0);
                }
                return;
            }
            if (argMap.containsKey("/c")) {
                String fileName = argMap.get("/c");
                if (fileName.toLowerCase().equals("default")) {
                    System.out.println("Using default config!");
                    Options.resetAll();
                    Options.getInstance();
                } else {
                    System.out.println("Using configuration file '" + fileName + "'");
                    Options.readOptions(fileName);
                }
                argMap.remove("/c");
            }
            String outFile = null;
            if (argMap.containsKey("/o")) {
                outFile = argMap.get("/o");
                argMap.remove("/o");
                if (outFile.equals("stdout")) {
                    System.out.println("Writing output to console");
                } else {
                    System.out.println("Using output file '" + outFile + "'");
                }
            }
            List<StepType> typeList = new ArrayList<StepType>();
            if (argMap.containsKey("/sc")) {
                String[] steps = argMap.get("/sc").toLowerCase().split(",");
                for (int i = 0; i < steps.length; i++) {
                    StepType.parseTypeStr(typeList, steps[i]);
                }
                argMap.remove("/sc");
            }
            DifficultyLevel actLevel = null;
            if (argMap.containsKey("/sl")) {
                int levelOrd = -1;
                try {
                    levelOrd = Integer.parseInt(argMap.get("/sl"));
                    actLevel = Options.getInstance().getDifficultyLevel(levelOrd + 1);
                    // if a level is given together with a step list, the level must be
                    // greater or equal the most difficult step in the list
                    for (StepType type : typeList) {
                        if (type.type.getStepConfig().getLevel() > (levelOrd + 1)) {
                            System.out.println("Invalid argument for option /sl: " + type.type.getStepName()
                                    + " requires at least difficulty level " + (levelOrd + 1));
                            if (consoleFrame == null) {
                                System.exit(0);
                            }
                            return;
                        }
                    }
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid argument for option /sl: " + argMap.get("/sl") + " - option ignored!");
                    if (consoleFrame == null) {
                        System.exit(0);
                    }
                    return;
                }
                argMap.remove("/sl");
            }
            if (argMap.containsKey("/so")) {
                printIgnoredOptions("/so", argMap);
                new Main().sortPuzzleFile(argMap.get("/so"), typeList, outFile);
                if (consoleFrame == null) {
                    System.exit(0);
                }
                return;
            }
            if (argMap.containsKey("/s")) {
                printIgnoredOptions("/s", argMap);
                if (typeList.isEmpty() && actLevel == null) {
                    System.out.println("No step name given and no difficulty level set!");
                    if (consoleFrame == null) {
                        System.exit(0);
                    }
                    return;
                }
                new Main().searchForType(typeList, actLevel, outFile);
                if (consoleFrame == null) {
                    System.exit(0);
                }
                return;
            }
            boolean printSolution = false;
            if (argMap.containsKey("/vs")) {
                printSolution = true;
                argMap.remove("/vs");
            }
            boolean printSolutionPath = false;
            if (argMap.containsKey("/vp")) {
                printSolutionPath = true;
                argMap.remove("/vp");
            }
            boolean printStatistics = false;
            if (argMap.containsKey("/vst")) {
                printStatistics = true;
                argMap.remove("/vst");
            }
            if (argMap.containsKey("/vf")) {
                String arg = argMap.get("/vf");
                int fishFormat = 0;
                try {
                    fishFormat = Integer.parseInt(arg);
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid argument for /vf ('" + arg + "'): '0' used instead!");
                }
                Options.getInstance().setFishDisplayMode(fishFormat);
                argMap.remove("/vf");
            }
            ClipboardMode clipboardMode = null;
            Set<SolutionType> outTypes = null;
            if (argMap.containsKey("/vg") && printSolutionPath) {
                // [l|c|s]:<type>[,<type>...]
                String types = argMap.get("/vg").toLowerCase();
                if (types.charAt(1) == ':') {
                    switch (types.charAt(0)) {
                        case 'l':
                            clipboardMode = ClipboardMode.LIBRARY;
                            break;
                        case 'c':
                            clipboardMode = ClipboardMode.PM_GRID;
                            break;
                        case 's':
                            clipboardMode = ClipboardMode.PM_GRID_WITH_STEP;
                            break;
                        default:
                            System.out.println("Invalid argument ('" + types.charAt(1) + "'): 'c' used instead!");
                            clipboardMode = ClipboardMode.PM_GRID;
                            break;
                    }
                    types = types.substring(2);
                } else {
                    System.out.println("No output mode set for '/vg': 'c' used as default!");
                    clipboardMode = ClipboardMode.PM_GRID;
                }
                String[] typesArr = types.split(",");
                for (int i = 0; i < typesArr.length; i++) {
                    StepConfig[] steps = Options.getInstance().solverSteps;
                    boolean typeFound = false;
                    for (int j = 0; j < steps.length; j++) {
                        if (steps[j].getType().getArgName().equals(typesArr[i])) {
                            if (outTypes == null) {
                                //outTypes = new TreeSet<SolutionType>();
                                outTypes = EnumSet.noneOf(SolutionType.class);
                            }
                            outTypes.add(steps[j].getType());
                            typeFound = true;
                            break;
                        }
                    }
                    if (!typeFound) {
                        System.out.println("Invalid solution type set for '/vg' (" + typesArr[i] + "): ignored!");
                    }
                }
                if (outTypes == null || outTypes.isEmpty()) {
                    System.out.println("No solution type set for '/vg': option ignored!");
                    clipboardMode = null;
                    outTypes = null;
                }
                argMap.remove("/vg");
            }
            if (argMap.containsKey("/bs")) {
                printIgnoredOptions("/bs", argMap);
                String fileName = argMap.get("/bs");
                new Main().batchSolve(fileName, null, printSolution, printSolutionPath, printStatistics,
                        clipboardMode, outTypes, outFile, false);
                if (consoleFrame == null) {
                    System.exit(0);
                }
                return;
            }
            if (argMap.containsKey("/bsaf")) {
                printIgnoredOptions("/bsaf", argMap);
                String fileName = argMap.get("/bsaf");
                new Main().batchSolve(fileName, null, printSolution, printSolutionPath, printStatistics,
                        clipboardMode, outTypes, outFile, true);
                if (consoleFrame == null) {
                    System.exit(0);
                }
                return;
            }
            if (argMap.containsKey("/bsa")) {
                printIgnoredOptions("/bsa", argMap);
                System.out.println("bsa: started");
                if (puzzleString == null) {
                    System.out.println("No puzzle given with /bsa - ignored!");
                    if (consoleFrame == null) {
                        System.exit(0);
                    }
                    return;
                }
                new Main().batchSolve(null, puzzleString, printSolution, printSolutionPath, printStatistics,
                        clipboardMode, outTypes, outFile, true);
                if (consoleFrame == null) {
                    System.exit(0);
                }
                return;
            }
            List<SolutionType> testTypes = new ArrayList<SolutionType>();
            if (argMap.containsKey("/bts")) {
                String testArgType = argMap.get("/bts");
                String[] tmpTypes = testArgType.split(",");
                for (int i = 0; i < tmpTypes.length; i++) {
                    SolutionType tmp = SolutionType.getTypeFromArgName(tmpTypes[i]);
                    if (tmp == null) {
                        System.out.println("Invalid step: " + tmpTypes[i] + " with /bts - ignored!");
                    } else {
                        testTypes.add(tmp);
                    }
                }
                if (testTypes.isEmpty()) {
                    System.out.println("Invalid step(s): <" + testArgType + "> with /bts - ignored!");
                }
                argMap.remove("/bts");
            }
            if (argMap.containsKey("/bt")) {
                printIgnoredOptions("/bt", argMap);
                String fileName = argMap.get("/bt");
                if (testTypes.isEmpty()) {
                    System.out.println("/bt: nothing to do!");
                    if (consoleFrame == null) {
                        System.exit(0);
                    }
                    return;
                }
                new Main().batchSolve(fileName, null, false, false, true,
                        clipboardMode, outTypes, outFile, false, true, testTypes);
                if (consoleFrame == null) {
                    System.exit(0);
                }
                return;
            }
            if (puzzleString != null) {
                printIgnoredOptions("", argMap);
                new Main().batchSolve(null, puzzleString, printSolution, printSolutionPath, printStatistics,
                        clipboardMode, outTypes, outFile, false);
                if (consoleFrame == null) {
                    System.exit(0);
                }
                return;
            }
            printIgnoredOptions("", argMap);
            System.out.println("Don't know what to do...");
            printHelpScreen();
            if (consoleFrame == null) {
                System.exit(0);
            }
            return;
        }

        // ok - no console operation, start GUI
        if (needToResetPuzzles) {
            BackgroundGeneratorThread.getInstance().resetAll();
        }
        final String lf = launchFile;
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new MainFrame(lf).setVisible(true);
            }
        });
    }

    /**
     * Dynamically loads the Nimbus LaF and resets the default font to a 
     * larger size. This is necessary for two reasons:
     * <ul>
     * <li>The Nimbus LaF is initialized dynamically, so setting it in
     *      UIManager and changing the defaults wont work (instance has not
     *      yet been created, so the changes wont work)</li>
     * <li>The package of the Nimbus LaF changed from Java 1.6 to 1.7. Simply
     *      subclassing NimusLookAndFeel and overriding getDefaults()
     *      throws an ClassLoaderException on some Java versions.</li>
     * </ul>
     * @param className
     * @param fontName
     * @param fontStyle
     * @param fontSize
     * @return 
     */
//    private static boolean initializeNimbusLaF(String className, String fontName, int fontStyle, int fontSize) {
//        ClassLoader classLoader = Main.class.getClassLoader();
//
//        try {
//            @SuppressWarnings("unchecked")
//            Class<LookAndFeel> aClass = (Class<LookAndFeel>)classLoader.loadClass(className);
//            LookAndFeel laf = aClass.newInstance();
//            UIManager.setLookAndFeel(laf);
//            laf.getDefaults().put("defaultFont",
//                    new FontUIResource(fontName, fontStyle, fontSize)); // supersize me
//            return true;
//        } catch (Exception ex) {
//            return false;
//        }
//    }

    /**
     * Prints all remaining (unused) options in argMap except "option"
     * @param Option option that is currently worked on
     * @param argMap All options from the command line
     */
    private static void printIgnoredOptions(String option, Map<String, String> argMap) {
        StringBuilder tmp = new StringBuilder();
        boolean found = false;
        for (String key : argMap.keySet()) {
            if (!key.equals(option)) {
                found = true;
                tmp.append(key);
                tmp.append(" ");
            }
        }
        if (found) {
            System.out.println("The following options were ignored: " + tmp.toString().trim());
        }
    }

    /**
     * Parses a command line string that comes from a file or from stdin.
     * This used to be a String.split(" "), but we have to support text qualifiers.
     * 
     * Allowed qualifiers: " or '
     * 
     * Qualifiers within the string have to be doubled
     * 
     * @param in The command line string
     * @return Array with options
     */
    private static String[] getOptionsFromStringBuilder(StringBuilder in) {
        List<String> options = new ArrayList<String>();
        char qualifier = '"';
        boolean qualifierSeen = false;
        int startIndex = -1;
        for (int i = 0; i < in.length(); i++) {
            char ch = in.charAt(i);
            if (ch == ' ') {
                if (qualifierSeen) {
                    // we are in the middle of an option -> ignore it
                    continue;
                }
                if (startIndex != -1) {
                    // we reached the end of an option -> copy it
                    options.add(in.substring(startIndex, i));
                    startIndex = -1;
                }
            }
            if (qualifierSeen && ch == qualifier) {
                // could be a stuffed character or the end of an option
                if (i < in.length() - 1 && in.charAt(i + 1) == qualifier) {
                    // stuffed character -> delete the superfluous one and
                    // skip the correct one to avoid confusion
                    in.delete(i, i);
                    i++;
                } else {
                    // end of qualified option reached -> store it
                    options.add(in.substring(startIndex, i));
                    startIndex = -1;
                    qualifierSeen = false;
                }
            }
            if (!qualifierSeen && (ch == '"' || ch == '\'')) {
                // if we are in the middle of an option -> store
                // what we have so far
                if (startIndex != -1) {
                    options.add(in.substring(startIndex, i));
                }
                // start a new qualified option
                startIndex = i + 1;
                qualifier = ch;
                qualifierSeen = true;
            }
            if (ch != ' ' && startIndex == -1) {
                // a new option starts
                startIndex = i;
            }
        }
        if (startIndex != -1 && startIndex < in.length()) {
            options.add(in.substring(startIndex, in.length()));
        }
        return options.toArray(new String[0]);
    }

    private static void printHelpScreen() {
        System.out.println("Usage: java -Xmx512m -jar hodoku.jar [options] [puzzle]\r\n"
                + "\r\n"
                + "Options:\r\n"
                + "  /h, /?: print this help screen\r\n"
                + "  /f <file>: read options from file <file>\r\n"
                + "  /c <hcfg file | 'default'>: use <file> for this console run\r\n"
                + "      (current config of GUI program is not changed)\r\n"
                + "  /lt: list internal names of techniques\r\n"
                + "  /so <file>: sort puzzle file created with /s, write output to <file>.out.txt\r\n"
                + "      or to a file given by /o; a filter can be applied with /sc\r\n"
                + "  /s: create puzzles which contain steps according to /sc and/or /sl\r\n"
                + "      and write them to <step>[_<step>...].txt or a file given by /o\r\n"
                + "      (for compatibility reasons steps can be defined directly with /s)\r\n"
                + "  /sc <step>[:0|1|2|3][+[e|l|g]n][,[-]<step>[:0|1|2|3][+[e|l|g]n]...]: define\r\n"
                + "      puzzle properties for /s or /so\r\n"
                + "      <step> is an internal name according to /lt or \"all\" (all steps except\r\n"
                + "          singles), \"nssts\" (all steps except SSTS: singles, h2, h3, h4, n2,\r\n"
                + "          n3, n4, l2, l3, lc1, lc2, bf2, bf3, bf4, xy, sc, mc) or \r\n"
                + "          \"nssts1\" (nssts minus 2sk, sk, bug1, er, w, u1, xyz, rp)\r\n"
                + "      -: exclude the step (not allowed with first <step> definition)\r\n"
                + "      0: x <step> x (default)\r\n"
                + "      1: ssts <step> ssts\r\n"
                + "      2: ssts <step> s\r\n"
                + "      3: s <step> s\r\n"
                + "      with: 'x' - arbitrary steps, 'ssts' - SSTS, 's' - singles\r\n"
                + "      +[e|l|g]n: number of candidates, <step> has to eliminate, equals |\r\n"
                + "          is less than | is greater than n\r\n"
                + "  /sl <level>: create only puzzles with difficulty level <level>\r\n"
                + "      0: easy; 1: medium; 2: hard; 3: unfair; 4: extreme\r\n"
                + "  /bs <file>: batch solve puzzles in <file> (output written to <file>.out.txt\r\n"
                + "       or a file given by /o)\r\n"
                + "  /bsaf <file>: batch process puzzles in <file> (output as in /bs);\r\n"
                + "       for each puzzle \"Find all Steps\" is executed\r\n"
                + "  /bsa: execute \"Find all Steps\" for [puzzle] (output written to\r\n"
                + "       <file>.out.txt or a file given by /o)\r\n"
                + "  /bt <file>: batch test using puzzle collection in <file> (output as in /bs)\r\n"
                + "  /bts <step>[,<step>...]: find all occurences of <step> after any non single\r\n"
                + "      step and check all eliminations against the solution of the puzzle\r\n"
                + "  /vs: print solution in output file (only valid with /bs)\r\n"
                + "  /vp: print complete solution for each puzzle (only valid with /bs)\r\n"
                + "  /vst: print statistics (only valid with /bs)\r\n"
                + "  /vf <0|1|2>: set fish output format (default, numbers, cells)\r\n"
                + "  /vg [l|c|s:]<step>[,<step>...]: print pm before every <step> in the solution\r\n"
                + "      (only valid with /bs and /vp)\r\n"
                + "      l: print library format\r\n"
                + "      c: print candidate grid\r\n"
                + "      s: print candidate grid with step highlighted\r\n"
                + "  /o <file>: write output to <file>; if <file> is \"stdout\", all output is\r\n"
                + "      written to the console\r\n"
                + "  /stdin: read options from stdin\r\n"
                + "  /test <file>: run regression tester against test cases in <file>\r\n"
                + "  /testf <file>: same as /test, but long running tests are ommitted\r\n"
                + "\r\n"
                + "Puzzle: If a puzzle is given it is solved as if it was read from a file with\r\n"
                + "      /bs; if a PM is given it must be delimited by \" or '");
    }
}

class SearchForTypeThread extends Thread {

    private final class PuzzleType {

        StepType type;
        boolean typeSeen = false;
        boolean immediatelyFollowed = false; // set, when type is seen
        int isPuzzleMode1 = -1;
        int isPuzzleMode2 = -1;
        String puzzleString = "";
        int anzCandDel = 0;

        PuzzleType(StepType type) {
            reset();
            this.type = type;
        }

        void reset() {
            typeSeen = false;
            immediatelyFollowed = false;
            isPuzzleMode1 = -1;
            isPuzzleMode2 = -1;
            puzzleString = "";
            anzCandDel = 0;
        }
    }
    private Main m;
    private List<StepType> typeList;
    private DifficultyLevel level;
    private int anz = 0;
    private int anzFound = 0;
    private String outFile = null;

    SearchForTypeThread(Main m, List<StepType> typeList,
            DifficultyLevel level, String outFile) {
        this.m = m;
        this.typeList = typeList;
        this.level = level;
        this.outFile = outFile;
    }

    private void appendPuzzleString(PuzzleType pType, boolean mode1) {
        int mode = mode1 ? pType.isPuzzleMode1 : pType.isPuzzleMode2;
        switch (mode) {
            case -1:
                // first step is type!
                break;
            case 3:
                pType.puzzleString += " s";
                break;
            case 2:
            case 1:
                pType.puzzleString += " ssts";
                break;
            case 0:
                pType.puzzleString += " x";
                break;
        }
    }

    @Override
    @SuppressWarnings({"ResultOfObjectAllocationIgnored", "CallToThreadDumpStack"})
    public void run() {
        //String path = m.getSrcDir() + "ar.txt";
        PuzzleType[] puzzleTypes = new PuzzleType[typeList.size()];
        StringBuilder pathBuffer = new StringBuilder();
        int index = 0;
        for (StepType tmpType : typeList) {
            pathBuffer.append(tmpType.type.getArgName()).append("_");
            puzzleTypes[index] = new PuzzleType(tmpType);
            index++;
        }
        if (level == null) {
            pathBuffer.deleteCharAt(pathBuffer.length() - 1);
        } else {
            pathBuffer.append(level.getName());
        }
        if (pathBuffer.length() > 50) {
            pathBuffer.delete(50, pathBuffer.length() - 1);
        }
        pathBuffer.append(".txt");
        if (outFile != null) {
            pathBuffer = new StringBuilder(outFile);
        }
        anz = 0;
        anzFound = 0;
        try {
            BufferedWriter out = null;
            if (!pathBuffer.toString().equals("stdout")) {
                out = new BufferedWriter(new FileWriter(pathBuffer.toString(), true));
            }
            SudokuGenerator generator = SudokuGeneratorFactory.getDefaultGeneratorInstance();
            SudokuSolver solver = SudokuSolverFactory.getDefaultSolverInstance();
            // einmal ein leeres Sudoku2 erzeugen, damit alles richtig initialisiert wird
            new Sudoku2();

            //System.out.println("level: " + level.getName());
            while (!isInterrupted()) {
                Sudoku2 newSudoku = generator.generateSudoku(false);
                Sudoku2 clonedSudoku = newSudoku.clone();
                solver.setSudoku(clonedSudoku);
                solver.solve();
                //System.out.println("result: " + clonedSudoku.isSolved() + "/" + clonedSudoku.getLevel().getName());
                if (level != null) {
                    if (!clonedSudoku.isSolved()) {
                        // invalid: if a level is set, the sudoku must be solved
                        //System.out.println("INVALID: Sudoku not solved");
                        continue;
                    }
                    if (clonedSudoku.getLevel().getOrdinal() != level.getOrdinal()) {
                        // sudoku to difficult -> reject
//                        System.out.println("INVALID: difficulty level " + clonedSudoku.getLevel().getName());
                        continue;
                    }
                }
//                System.out.println("VALID: difficulty level " + clonedSudoku.getLevel().getName());
                if (puzzleTypes.length == 0) {
                    // no types, only level: this puzzle is acceptable
                    String txt = newSudoku.getSudoku(ClipboardMode.CLUES_ONLY);
                    if (out != null) {
                        out.write(txt + " #" + level.getName());
                        out.newLine();
                        out.flush();
                    }
                    System.out.println(txt + " #" + level.getName());
                    anzFound++;
                }
                for (int i = 0; i < puzzleTypes.length; i++) {
                    puzzleTypes[i].reset();
                }
                List<SolutionStep> steps = solver.getSteps();
                for (int i = 0; i < steps.size(); i++) {
                    SolutionType type = steps.get(i).getType();
                    for (int j = 0; j < puzzleTypes.length; j++) {
                        if (type.equals(puzzleTypes[j].type.type)) {
                            int anzCandDel = steps.get(i).getAnzCandidatesToDelete();
                            if (puzzleTypes[j].anzCandDel < anzCandDel) {
                                puzzleTypes[j].anzCandDel = anzCandDel;
                            }
                            StringBuilder stepName = new StringBuilder(" " + type.getArgName());
                            if (type.isFish()) {
                                if (steps.get(i).getEndoFins().size() > 0) {
                                    stepName.append("e");
                                }
                                if (steps.get(i).getCannibalistic().size() > 0) {
                                    stepName.append("c");
                                }
                            }
                            stepName.append("(").append(anzCandDel).append(")");
                            if (puzzleTypes[j].immediatelyFollowed) {
                                // nothing between two occurences of type
                                // nothing special has to be done
                            } else {
                                // what was before type?
                                if (!puzzleTypes[j].typeSeen) {
                                    appendPuzzleString(puzzleTypes[j], true);
                                } else {
                                    // we are between two steps of <type>
                                    appendPuzzleString(puzzleTypes[j], false);
                                    // start from scratch
                                    puzzleTypes[j].isPuzzleMode2 = -1;
                                }
                                puzzleTypes[j].typeSeen = true;
                                puzzleTypes[j].immediatelyFollowed = true;
                            }
                            puzzleTypes[j].puzzleString += stepName.toString();
                        } else {
                            puzzleTypes[j].immediatelyFollowed = false;
                            if (type.isSingle()) {
                                // best case
                                if (puzzleTypes[j].typeSeen) {
                                    if (puzzleTypes[j].isPuzzleMode2 == -1) {
                                        puzzleTypes[j].isPuzzleMode2 = 3;
                                    }
                                }
                                // has to be done in both cases
                                if (puzzleTypes[j].isPuzzleMode1 == -1) {
                                    puzzleTypes[j].isPuzzleMode1 = 3;
                                }
                            } else if (type.isSSTS()) {
                                // step is SSTS -> can only be 2 or 1
                                if (puzzleTypes[j].typeSeen) {
                                    if (puzzleTypes[j].isPuzzleMode2 == -1
                                            || puzzleTypes[j].isPuzzleMode2 > 2) {
                                        puzzleTypes[j].isPuzzleMode2 = 1;
                                    }
                                    if (puzzleTypes[j].isPuzzleMode1 > 1) {
                                        puzzleTypes[j].isPuzzleMode1 = 1;
                                    }
                                } else {
                                    if (puzzleTypes[j].isPuzzleMode1 == 3
                                            || puzzleTypes[j].isPuzzleMode1 == -1) {
                                        puzzleTypes[j].isPuzzleMode1 = 2;
                                    }
                                }
                            } else {
                                // worst case -> 'X'
                                if (puzzleTypes[j].typeSeen) {
                                    puzzleTypes[j].isPuzzleMode2 = 0;
                                }
                                puzzleTypes[j].isPuzzleMode1 = 0;
                            }
                        }
                    }
                }
                // now check, whether the puzzle fits the specification
                for (int i = 0; i < puzzleTypes.length; i++) {
                    String txt = null;
                    if (puzzleTypes[i].typeSeen
                            && puzzleTypes[i].isPuzzleMode1 >= puzzleTypes[i].type.puzzleType) {
                        // found a suitable sudoku, check candidates
                        if (puzzleTypes[i].type.compType != StepType.UNDEFINED) {
                            switch (puzzleTypes[i].type.compType) {
                                case StepType.EQUAL:
                                    if (puzzleTypes[i].anzCandDel != puzzleTypes[i].type.compAnz) {
                                        continue;
                                    }
                                    break;
                                case StepType.LT:
                                    if (puzzleTypes[i].anzCandDel >= puzzleTypes[i].type.compAnz) {
                                        continue;
                                    }
                                    break;
                                case StepType.GT:
                                    if (puzzleTypes[i].anzCandDel <= puzzleTypes[i].type.compAnz) {
                                        continue;
                                    }
                                    break;
                            }
                        }
                        appendPuzzleString(puzzleTypes[i], false);
                        if (txt == null) {
                            txt = newSudoku.getSudoku(ClipboardMode.CLUES_ONLY);
                        }
                        if (out != null) {
                            out.write(txt + " #" + puzzleTypes[i].puzzleString);
                            out.newLine();
                            out.flush();
                        }
                        System.out.println(txt + " #" + puzzleTypes[i].puzzleString);
                        anzFound++;
                    }
                }
                anz++;
//                if ((getAnz() % 10) == 0) {
//                    System.out.println(".");
//                }
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException ex) {
            System.out.println("Error writing sudoku file");
            ex.printStackTrace();
        }
    }

    public int getAnz() {
        return anz;
    }

    public int getAnzFound() {
        return anzFound;
    }
}

class BatchSolveThread extends Thread {

    private String fileName;
    private String puzzleString;
    private boolean printSolution;
    private boolean printSolutionPath;
    private boolean printStatistic;
    private int[] results;
    private int bruteForceAnz;
    private int templateAnz;
    private int unsolvedAnz = 0;
    private int givenUpAnz = 0;
    private int count;
    private long ticks;
    private SudokuGenerator generator = SudokuGeneratorFactory.getDefaultGeneratorInstance();
    private ClipboardMode clipboardMode;
    private Set<SolutionType> types;
    private boolean outputGrid = false;
    private String outFileName = null;
    private boolean findAllSteps = false;
    private boolean bruteForceTest = false;
    private List<SolutionType> testTypes = null;
    private StepStatistic[] stepStatistics;
    private StepStatistic[] singleStepStatistics;
    private FindAllSteps findAllStepsInstance = null;

    BatchSolveThread(String fn, String pStr, boolean ps, boolean pp, boolean pst,
            ClipboardMode cm, Set<SolutionType> t,
            String ofn, boolean fas, boolean bft, List<SolutionType> tt) {
        fileName = fn;
        puzzleString = pStr;
        printSolution = ps;
        printSolutionPath = pp;
        printStatistic = pst;
        clipboardMode = cm;
        types = t;
        if (clipboardMode != null && types != null) {
            outputGrid = true;
        }
        outFileName = ofn;
        findAllSteps = fas;
        bruteForceTest = bft;
        testTypes = tt;
        if (bruteForceTest) {
            findAllStepsInstance = new FindAllSteps();
        }

        if (printStatistic) {
            stepStatistics = new StepStatistic[SolutionType.values().length];
            singleStepStatistics = new StepStatistic[SolutionType.values().length];
            for (int i = 0; i < stepStatistics.length; i++) {
                stepStatistics[i] = new StepStatistic(SolutionType.values()[i]);
                singleStepStatistics[i] = new StepStatistic(SolutionType.values()[i]);
            }
        }
    }

    private void adjustStatistics(SolutionStep step) {
        int anzCand = step.getAnzCandidatesToDelete();
        int anzSet = step.getAnzSet();
        stepStatistics[step.getType().ordinal()].anzSteps++;
        stepStatistics[step.getType().ordinal()].anzCandDel += anzCand;
        stepStatistics[step.getType().ordinal()].anzSet += anzSet;
        singleStepStatistics[step.getType().ordinal()].anzSteps++;
        singleStepStatistics[step.getType().ordinal()].anzCandDel += anzCand;
        singleStepStatistics[step.getType().ordinal()].anzSet += anzSet;
    }

    private void clearSingleStepStatistics() {
        for (int i = 0; i < singleStepStatistics.length; i++) {
            singleStepStatistics[i].anzCandDel = 0;
            singleStepStatistics[i].anzSet = 0;
            singleStepStatistics[i].anzSteps = 0;
        }
    }

    public void printStatistic(PrintWriter out, boolean single) throws IOException {
        if (out != null) {
            if (!single) {
                out.println();
                out.println("Statistics total:");
            } else {
                out.println("    Statistics:");
            }
        } else {
            if (!single) {
                System.out.println();
                System.out.println("Statistics total:");
            } else {
                System.out.println("    Statistics:");
            }
        }
        if (single) {
            printStatistic(out, singleStepStatistics, false);
        } else {
            printStatistic(out, stepStatistics, true);
        }
    }

    private void printStatistic(PrintWriter out, StepStatistic[] stat, boolean total) throws IOException {
        int anzSteps = 0;
        int anzSet = 0;
        int anzCandDel = 0;
        int anzInvalidSteps = 0;
        int anzInvalidSet = 0;
        int anzInvalidCandDel = 0;
        for (int i = 0; i < stat.length; i++) {
            if (stat[i].anzSteps > 0) {
                if (out != null) {
                    out.printf("      %8d - %8d/%8d: %s", stat[i].anzSteps,
                            stat[i].anzSet, stat[i].anzCandDel, stat[i].type.getStepName());
//                    out.write("      " + stat[i].type.getStepName() + ": " + stat[i].anzSteps + " - " + stat[i].anzSet + "/" + stat[i].anzCandDel);
                    out.println();
                    if (bruteForceTest) {
                        out.printf("      Invalid: %3d - %3d/%3d",
                                stat[i].anzInvalidSteps, stat[i].anzInvalidSet, stat[i].anzInvalidCandDel);
//                        out.write("        Invalid: " + stat[i].anzInvalidSteps + " - " + stat[i].anzInvalidSet + "/" + stat[i].anzInvalidCandDel);
                        out.println();
                    }
                } else {
//                    System.out.println("      " + stat[i].type.getStepName() + ": " + stat[i].anzSteps + " - " + stat[i].anzSet + "/" + stat[i].anzCandDel);
                    System.out.printf("      %8d - %8d/%8d: %s", stat[i].anzSteps,
                            stat[i].anzSet, stat[i].anzCandDel, stat[i].type.getStepName());
                    System.out.println();
                    if (bruteForceTest) {
//                        System.out.println("        Invalid: " + stat[i].anzInvalidSteps + " - " + stat[i].anzInvalidSet + "/" + stat[i].anzInvalidCandDel);
                        System.out.printf("        Invalid: %3d - %3d/%3d",
                                stat[i].anzInvalidSteps, stat[i].anzInvalidSet, stat[i].anzInvalidCandDel);
                        System.out.println();
                    }
                }
            }
            anzSteps += stat[i].anzSteps;
            anzSet += stat[i].anzSet;
            anzCandDel += stat[i].anzCandDel;
            anzInvalidSteps += stat[i].anzInvalidSteps;
            anzInvalidSet += stat[i].anzInvalidSet;
            anzInvalidCandDel += stat[i].anzInvalidCandDel;
        }
        if (total) {
            if (out != null) {
                out.println("      ---------------------------------------------------");
                out.printf("      %8d - %8d/%8d", anzSteps, anzSet, anzCandDel);
                out.println();
                if (bruteForceTest) {
                    out.printf("      %8d - %8d/%8d", anzInvalidSteps, anzInvalidSet, anzInvalidCandDel);
                    out.println();
                }
            } else {
                System.out.println("      ---------------------------------------------------");
                System.out.printf("      %8d - %8d/%8d", anzSteps, anzSet, anzCandDel);
                if (bruteForceTest) {
                    System.out.printf("      %8d - %8d/%8d", anzInvalidSteps, anzInvalidSet, anzInvalidCandDel);
                }
            }
            // timing
            if (out != null) {
                SudokuSolverFactory.getDefaultSolverInstance().printStatistics(out);
            } else {
//                System.out.println("System.out!");
                SudokuSolverFactory.getDefaultSolverInstance().printStatistics(System.out);
            }
        }

    }

    @Override
    @SuppressWarnings("CallToThreadDumpStack")
    public void run() {
        System.out.println("Starting batch solve...");
        results = new int[Options.DEFAULT_DIFFICULTY_LEVELS.length];
        bruteForceAnz = 0;
        templateAnz = 0;
        unsolvedAnz = 0;
        givenUpAnz = 0;
        BufferedReader inFile = null;
        PrintWriter outFile = null;
        ticks = System.currentTimeMillis();
        count = 0;
        try {
            if (fileName != null) {
                inFile = new BufferedReader(new FileReader(fileName));
            }
            if (outFileName == null) {
                outFileName = fileName + ".out.txt";
            }
            if (outFileName.equals("stdout")) {
                outFile = null;
            } else {
                outFile = new PrintWriter(new BufferedWriter(new FileWriter(outFileName)));
            }
            String line = null;
            SudokuSolver solver = SudokuSolverFactory.getDefaultSolverInstance();
            //Sudoku2 sudoku = new Sudoku2(true);
            Sudoku2 sudoku = new Sudoku2();
            Sudoku2 tmpSudoku = null;
            Sudoku2 solvedSudoku = null;
            List<SolutionStep> allSteps = null;
            if (bruteForceTest) {
                allSteps = new ArrayList<SolutionStep>();
            }
            long outTicks = 0;
            while (!isInterrupted()
                    && (inFile != null && (line = inFile.readLine()) != null)
                    || (puzzleString != null)) {
                if (puzzleString != null) {
                    line = puzzleString;
                    puzzleString = null;
                }
//                System.out.println("solving: " + line);
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
//                System.out.println(line);
                sudoku.setSudoku(line);
//                System.out.println("Sudoku: " + sudoku.getSudoku(ClipboardMode.VALUES_ONLY));
                if (outputGrid || bruteForceTest) {
                    tmpSudoku = sudoku.clone();
                }
                if (bruteForceTest) {
                    solvedSudoku = sudoku.clone();
                    generator.validSolution(solvedSudoku);
                }
                count++;
                boolean needsGuessing = false;
                boolean needsTemplates = false;
                boolean givenUp = false;
                boolean unsolved = false;
                List<SolutionStep> steps = null;
                if (findAllSteps) {
                    steps = new ArrayList<SolutionStep>();
                    Thread thread = new Thread(new FindAllSteps(steps, sudoku, null));
                    thread.start();
                    thread.join();
                    //System.out.println("fas: " + steps.size());
                } else {
                    // only for now: check the solution
                    generator.validSolution(sudoku);
                    solver.setSudoku(sudoku);
                    solver.solve();
//                    System.out.println("solved: " + sudoku.getSudoku(ClipboardMode.VALUES_ONLY));
                    steps = solver.getSteps();
                    for (int i = 0; i < steps.size(); i++) {
//                        System.out.println("      " + steps.get(i).toString(2));
                        if (steps.get(i).getType() == SolutionType.BRUTE_FORCE && !needsGuessing) {
                            needsGuessing = true;
                            unsolved = true;
                            bruteForceAnz++;
                        }
                        if ((steps.get(i).getType() == SolutionType.TEMPLATE_DEL
                                || steps.get(i).getType() == SolutionType.TEMPLATE_SET) && !needsTemplates) {
                            needsTemplates = true;
                            unsolved = true;
                            templateAnz++;
                        }
                        if (steps.get(i).getType() == SolutionType.GIVE_UP && !givenUp) {
                            givenUp = true;
                            unsolved = true;
                            givenUpAnz++;
                        }
                    }
                    if (unsolved) {
                        unsolvedAnz++;
                    }
                    // only for now: check the solution!
                    for (int i = 0; i < sudoku.getValues().length; i++) {
                        if (sudoku.getValue(i) != sudoku.getSolution(i)) {
                            System.out.println("Invalid solution: ");
                            System.out.println("   Sudoku: " + line);
                            System.out.println("   Solution:      " + Arrays.toString(sudoku.getValues()));
                            System.out.println("   True Solution: " + Arrays.toString(sudoku.getSolution()));
                        }
                    }
//                    System.out.println("solved!");
                }
                String guess = needsGuessing ? " " + SolutionType.BRUTE_FORCE.getArgName() : "";
                String template = needsTemplates ? " " + SolutionType.TEMPLATE_DEL.getArgName() : "";
                String giveUp = givenUp ? " " + SolutionType.GIVE_UP.getArgName() : "";
                if (printSolution || bruteForceTest) {
                    solvedSudoku = sudoku.clone();
                    if (sudoku.isSolved()) {
                        line = sudoku.getSudoku(ClipboardMode.VALUES_ONLY);
                    } else {
                        //System.out.println("Sudoku2: " + sudoku.getSudoku(ClipboardMode.PM_GRID));
                        //System.out.println("SolvedSudoku: " + solvedSudoku.getSudoku(ClipboardMode.PM_GRID));
                        generator.validSolution(solvedSudoku);
                        //System.out.println("SolvedSudoku2: " + solvedSudoku.getSudoku(ClipboardMode.PM_GRID));
                        line = solvedSudoku.getSudoku(ClipboardMode.VALUES_ONLY);
                        //System.out.println("line: " + line);
                    }
                }
                String out = line + " #" + count;
                if (!findAllSteps) {
                    out += " " + solver.getLevel().getName() + " (" + solver.getScore() + ")"
                            + guess + template + giveUp;
                    results[solver.getLevel().getOrdinal()]++;
                }
                if (outFile != null) {
                    outFile.println(out);
                } else {
                    System.out.println(out);
                }

                if (printSolutionPath || findAllSteps || printStatistic || bruteForceTest) {
                    steps = new ArrayList<SolutionStep>(steps);
                    for (int i = 0; i < steps.size(); i++) {
                        if (outputGrid || bruteForceTest) {
                            if (types != null && clipboardMode != null && types.contains(steps.get(i).getType())
                                    && (printSolutionPath || findAllSteps)) {
                                String grid = tmpSudoku.getSudoku(clipboardMode, steps.get(i));
                                String[] gridLines = grid.split("\r\n");
                                int end = clipboardMode == ClipboardMode.PM_GRID_WITH_STEP ? gridLines.length - 2 : gridLines.length;
                                for (int j = 0; j < end; j++) {
                                    if (outFile != null) {
                                        outFile.println("   " + gridLines[j]);
                                    } else {
                                        System.out.println("   " + gridLines[j]);
                                    }
                                }
                            }
                            if (bruteForceTest && !steps.get(i).getType().isSingle()) {
                                // get all steps for testType
//                                System.out.println("Running: " + tmpSudoku.getSudoku(ClipboardMode.LIBRARY));
                                allSteps.clear();
                                findAllStepsInstance.setSteps(allSteps);
                                findAllStepsInstance.setSudoku(tmpSudoku);
                                findAllStepsInstance.setTestType(testTypes);
                                findAllStepsInstance.run();
                                // check them
                                for (SolutionStep act : allSteps) {
//                                    System.out.println("   " + act);
                                    if (!testTypes.contains(act.getType())) {
                                        continue;
                                    }
                                    boolean invalid = false;
                                    adjustStatistics(act);
                                    if (!act.getValues().isEmpty()) {
                                        // Set
                                        for (int index : act.getIndices()) {
                                            if (sudoku.getValue(index) != solvedSudoku.getValue(index)) {
                                                invalid = true;
                                                stepStatistics[act.getType().ordinal()].anzInvalidSet++;
                                            }
                                        }
                                    }
                                    for (Candidate cand : act.getCandidatesToDelete()) {
                                        if (cand.getValue() == solvedSudoku.getValue(cand.getIndex())) {
                                            invalid = true;
                                            stepStatistics[act.getType().ordinal()].anzInvalidCandDel++;
                                        }
                                    }
                                    if (invalid) {
                                        stepStatistics[act.getType().ordinal()].anzInvalidSteps++;
                                        if (outFile != null) {
                                            outFile.println("INVALID:");
                                            outFile.println(sudoku.getSudoku(ClipboardMode.LIBRARY, act));
                                        } else {
                                            System.out.println("INVALID:");
                                            System.out.println(sudoku.getSudoku(ClipboardMode.LIBRARY, act));
                                        }
                                    }
                                }
                            }
                            solver.doStep(tmpSudoku, steps.get(i));
                        }
                        if (printStatistic && !bruteForceTest) {
                            adjustStatistics(steps.get(i));
                        }
                        if (printSolutionPath || findAllSteps) {
                            if (outFile != null) {
                                outFile.write("   ");
                                if (printStatistic) {
                                    outFile.write(steps.get(i).getCandidateString(false, true) + ": ");
                                }
                                outFile.println(steps.get(i).toString(2));
                            } else {
                                System.out.print("   ");
                                if (printStatistic) {
                                    System.out.print(steps.get(i).getCandidateString(false, true) + ": ");
                                }
                                System.out.println(steps.get(i).toString(2));
                            }
                        }
                    }
                    if (printStatistic && (printSolutionPath || findAllSteps)) {
                        printStatistic(outFile, true);
                        clearSingleStepStatistics();
                    }
                }
//                    if (printStatistic) {
//                        System.out.print(count + " -");
//                        printStatistic(null, true);
//                        clearSingleStepStatistics();
//                    }

                if ((count % 100) == 0) {
                    if (System.currentTimeMillis() - outTicks > 2000) {
                        outTicks = System.currentTimeMillis();
                        double ticks2 = outTicks - getTicks();
//                        System.out.println(count + " (" + (ticks2 / count) + "ms per puzzle)");
                        System.out.printf("%d (%.03fms per puzzle\r\n", count, (ticks2 / count));
                    }
                }
            }
            if (printStatistic) {
                printStatistic(outFile, false);
            }
        } catch (Exception ex) {
            System.out.println("Error in batch solve:");
            ex.printStackTrace();
        } finally {
            try {
                if (inFile != null) {
                    inFile.close();
                }
                if (outFile != null) {
                    outFile.close();
                }
            } catch (Exception ex) {
                System.out.println("Error closing files:");
                ex.printStackTrace();
            }
        }
        if (isInterrupted()) {
            System.out.println("Interrupted, shutting down...");
        } else {
            System.out.println("Done!");
        }
        ticks = System.currentTimeMillis() - getTicks();
    }

    public int getBruteForceAnz() {
        return bruteForceAnz;
    }

    public int getTemplateAnz() {
        return templateAnz;
    }

    public int getUnsolvedAnz() {
        return unsolvedAnz;
    }

    public int getGivenUpAnz() {
        return givenUpAnz;
    }

    public long getTicks() {
        return ticks;
    }

    public int getResult(int index) {
        return results[index];
    }

    public int getResultLength() {
        return results.length;
    }

    public int getCount() {
        return count;
    }

    public StepStatistic[] getStepStatistics() {
        return stepStatistics;
    }
}

class ShutDownThread extends Thread {

    private Thread thread;

    ShutDownThread(Thread thread) {
        this.thread = thread;
    }

    @Override
    public void run() {
        thread.interrupt();
    }
}

class StepType {

    static final int UNDEFINED = -1;
    static final int EQUAL = 0;
    static final int LT = 1;
    static final int GT = 2;
    SolutionType type;
    int puzzleType = 0;
    boolean isRemove = false;
    int compType = UNDEFINED;
    int compAnz = 0;

    private StepType(SolutionType type, int puzzleType, boolean isRemove, int compType, int compAnz) {
        this.type = type;
        this.puzzleType = puzzleType;
        this.isRemove = isRemove;
        this.compType = compType;
        this.compAnz = compAnz;
    }

    @Override
    public String toString() {
        char compChar = '-';
        switch (compType) {
            case EQUAL:
                compChar = '=';
                break;
            case LT:
                compChar = '<';
                break;
            case GT:
                compChar = '>';
                break;
        }
        if (compType != UNDEFINED) {
            return type.getStepName() + " (" + puzzleType + ", " + compChar + compAnz + ")";
        } else {
            return type.getStepName() + " (" + puzzleType + ", -)";
        }
    }

    public static void parseTypeStr(List<StepType> stepList, String inputStr) {
        SolutionType type;
        int puzzleType = 0;
        boolean isRemove = false;
        int compType = UNDEFINED;
        int compAnz = 0;

        inputStr = inputStr.toLowerCase();
        if (inputStr.startsWith("-")) {
            isRemove = true;
            inputStr = inputStr.substring(1);
        }
        String compStr = null;
        String typeStr = null;
        int compIndex = -1;
        compIndex = inputStr.indexOf('+');
        int typeIndex = inputStr.indexOf(':');
        if (typeIndex == -1 && compIndex != -1) {
            compStr = inputStr.substring(compIndex + 1);
            inputStr = inputStr.substring(0, compIndex);
        } else if (typeIndex != -1 && compIndex == -1) {
            typeStr = inputStr.substring(typeIndex);
            inputStr = inputStr.substring(0, typeIndex);
        } else if (typeIndex != -1 && compIndex != -1) {
            if (typeIndex < compIndex) {
                compStr = inputStr.substring(compIndex + 1);
                typeStr = inputStr.substring(typeIndex, compIndex);
                inputStr = inputStr.substring(0, typeIndex);
            } else {
                typeStr = inputStr.substring(typeIndex);
                compStr = inputStr.substring(compIndex + 1, typeIndex);
                inputStr = inputStr.substring(0, compIndex);
            }
        }
        puzzleType = 0;
        if (typeStr != null) {
            if (typeStr.length() < 2) {
                System.out.println("Puzzle type missing (assuming '0')!");
            } else {
                char typeModeChar = typeStr.charAt(1);
                switch (typeModeChar) {
                    case '0':
                        puzzleType = 0; // step must be in puzzle, nothing else required
                        break;
                    case '1':
                        puzzleType = 1; // SSTS + step + SSTS
                        break;
                    case '2':
                        puzzleType = 2; // SSTS + step + Singles
                        break;
                    case '3':
                        puzzleType = 3; // singles + step + singles
                        break;
                    default:
                        System.out.println("Invalid puzzle type: " + typeModeChar + " (assuming '0')");
                        break;
                }
            }
        }
        if (compStr != null) {
            // now comparison
            if (compStr.length() < 2) {
                System.out.println("Invalid comparison spec - ignored!");
            } else {
                switch (compStr.charAt(0)) {
                    case 'e':
                        compType = EQUAL;
                        break;
                    case 'l':
                        compType = LT;
                        break;
                    case 'g':
                        compType = GT;
                        break;
                    default:
                        System.out.println("Invalid comparison mode: " + compStr.charAt(0) + " (ignored)");
                        break;
                }
                if (compType != UNDEFINED) {
                    String compAnzStr = compStr.substring(1);
                    try {
                        compAnz = Integer.parseInt(compAnzStr);
                    } catch (NumberFormatException ex) {
                        System.out.println("Invalid comparison digit: " + compAnzStr + " (comparison ignored)");
                        compType = UNDEFINED;
                    }
                }
            }
        }
        type = null;
        SolutionType[] values = SolutionType.values();
        for (int j = 0; j < values.length; j++) {
            if (values[j].getArgName().equals(inputStr)) {
                type = values[j];
            }
        }
        if (type == null) {
            if (inputStr.equals("all")) {
                for (SolutionType tmpType : SolutionType.values()) {
                    if (!tmpType.isSingle()) {
                        addDeleteStepInList(stepList, new StepType(tmpType, puzzleType, isRemove, compType, compAnz));
                    }
                }
            } else if (inputStr.equals("nssts")) {
                for (SolutionType tmpType : SolutionType.values()) {
                    if (!tmpType.isSingle() && !tmpType.isSSTS()) {
                        addDeleteStepInList(stepList, new StepType(tmpType, puzzleType, isRemove, compType, compAnz));
                    }
                }
            } else if (inputStr.equals("nssts1")) {
                for (SolutionType tmpType : SolutionType.values()) {
                    if (!tmpType.isSingle() && !tmpType.isSSTS()
                            && !tmpType.equals(SolutionType.TWO_STRING_KITE)
                            && !tmpType.equals(SolutionType.SKYSCRAPER)
                            && !tmpType.equals(SolutionType.BUG_PLUS_1)
                            && !tmpType.equals(SolutionType.EMPTY_RECTANGLE)
                            && !tmpType.equals(SolutionType.W_WING)
                            && !tmpType.equals(SolutionType.UNIQUENESS_1)
                            && !tmpType.equals(SolutionType.XYZ_WING)
                            && !tmpType.equals(SolutionType.REMOTE_PAIR)) {
                        addDeleteStepInList(stepList, new StepType(tmpType, puzzleType, isRemove, compType, compAnz));
                    }
                }
            } else {
                System.out.println("Invalid step name: " + inputStr + " (ignored!)");
            }
        } else {
            addDeleteStepInList(stepList, new StepType(type, puzzleType, isRemove, compType, compAnz));
        }
    }

    private static void addDeleteStepInList(List<StepType> stepList, StepType step) {
        if (step.type == null) {
            return;
        }
        boolean found = false;
        for (int i = 0; i < stepList.size(); i++) {
            StepType tmpStep = stepList.get(i);
            if (tmpStep.type == step.type && tmpStep.puzzleType == step.puzzleType) {
                found = true;
                if (step.isRemove) {
                    stepList.remove(i);
                    i--;
                } else {
                    // allow multiple instances with same puzzleType
//                    tmpStep.compType = step.compType;
//                    tmpStep.compAnz = step.compAnz;
                }
            }
        }
        if (step.isRemove) {
            if (!found) {
                System.out.println("Could not remove step " + step.type.getArgName() + ":" + step.puzzleType + ": was not set yet.");
            }
        } else {
            stepList.add(step);
        }
    }
}

class StepStatistic {

    SolutionType type;
    int anzSet;
    int anzCandDel;
    int anzSteps;
    int anzInvalidSteps;
    int anzInvalidSet;
    int anzInvalidCandDel;

    StepStatistic(SolutionType type) {
        this.type = type;
    }
}
