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
public enum SolutionType {

    FULL_HOUSE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Full_House"), "0000", "fh"),
    HIDDEN_SINGLE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Hidden_Single"), "0002", "h1"),
    HIDDEN_PAIR(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Hidden_Pair"), "0210", "h2"),
    HIDDEN_TRIPLE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Hidden_Triple"), "0211", "h3"),
    HIDDEN_QUADRUPLE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Hidden_Quadruple"), "0212", "h4"),
    NAKED_SINGLE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Naked_Single"), "0003", "n1"),
    NAKED_PAIR(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Naked_Pair"), "0200", "n2"),
    NAKED_TRIPLE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Naked_Triple"), "0201", "n3"),
    NAKED_QUADRUPLE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Naked_Quadruple"), "0202", "n4"),
    LOCKED_PAIR(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Locked_Pair"), "0110", "l2"),
    LOCKED_TRIPLE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Locked_Triple"), "0111", "l3"),
    LOCKED_CANDIDATES(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Locked_Candidates"), "xxxx", "lc"),
    LOCKED_CANDIDATES_1(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Locked_Candidates_Type_1_(Pointing)"), "0100", "lc1"),
    LOCKED_CANDIDATES_2(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Locked_Candidates_Type_2_(Claiming)"), "0101", "lc2"),
    SKYSCRAPER(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Skyscraper"), "0400", "sk"),
    TWO_STRING_KITE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("2-String_Kite"), "0401", "2sk"),
    UNIQUENESS_1(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Uniqueness_Test_1"), "0600", "u1"),
    UNIQUENESS_2(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Uniqueness_Test_2"), "0601", "u2"),
    UNIQUENESS_3(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Uniqueness_Test_3"), "0602", "u3"),
    UNIQUENESS_4(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Uniqueness_Test_4"), "0603", "u4"),
    UNIQUENESS_5(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Uniqueness_Test_5"), "0604", "u5"),
    UNIQUENESS_6(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Uniqueness_Test_6"), "0605", "u6"),
    BUG_PLUS_1(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Bivalue_Universal_Grave_+_1"), "0610", "bug1"),
    XY_WING(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("XY-Wing"), "0800", "xy"),
    XYZ_WING(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("XYZ-Wing"), "0801", "xyz"),
    W_WING(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("W-Wing"), "0803", "w"),
    X_CHAIN(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("X-Chain"), "0701", "x"),
    XY_CHAIN(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("XY-Chain"), "0702", "xyc"),
    REMOTE_PAIR(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Remote_Pair"), "0703", "rp"),
    NICE_LOOP(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Nice_Loop/AIC"), "xxxx", "nl"),
    CONTINUOUS_NICE_LOOP(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Continuous_Nice_Loop"), "0706", "cnl"),
    DISCONTINUOUS_NICE_LOOP(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Discontinuous_Nice_Loop"), "0707", "dnl"),
    X_WING(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("X-Wing"), "0300", "bf2"),
    SWORDFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Swordfish"), "0301", "bf3"),
    JELLYFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Jellyfish"), "0302", "bf4"),
    SQUIRMBAG(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Squirmbag"), "0303", "bf5"),
    WHALE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Whale"), "0304", "bf6"),
    LEVIATHAN(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Leviathan"), "0305", "bf7"),
    FINNED_X_WING(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_X-Wing"), "0310", "fbf2"),
    FINNED_SWORDFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Swordfish"), "0311", "fbf3"),
    FINNED_JELLYFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Jellyfish"), "0312", "fbf4"),
    FINNED_SQUIRMBAG(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Squirmbag"), "0313", "fbf5"),
    FINNED_WHALE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Whale"), "0314", "fbf6"),
    FINNED_LEVIATHAN(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Leviathan"), "0315", "fbf7"),
    SASHIMI_X_WING(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Sashimi_X-Wing"), "0320", "sbf2"),
    SASHIMI_SWORDFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Sashimi_Swordfish"), "0321", "sbf3"),
    SASHIMI_JELLYFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Sashimi_Jellyfish"), "0322", "sbf4"),
    SASHIMI_SQUIRMBAG(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Sashimi_Squirmbag"), "0323", "sbf5"),
    SASHIMI_WHALE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Sashimi_Whale"), "0324", "sbf6"),
    SASHIMI_LEVIATHAN(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Sashimi_Leviathan"), "0325", "sbf7"),
    FRANKEN_X_WING(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Franken_X-Wing"), "0330", "ff2"),
    FRANKEN_SWORDFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Franken_Swordfish"), "0331", "ff3"),
    FRANKEN_JELLYFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Franken_Jellyfish"), "0332", "ff4"),
    FRANKEN_SQUIRMBAG(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Franken_Squirmbag"), "0333", "ff5"),
    FRANKEN_WHALE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Franken_Whale"), "0334", "ff6"),
    FRANKEN_LEVIATHAN(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Franken_Leviathan"), "0335", "ff7"),
    FINNED_FRANKEN_X_WING(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Franken_X-Wing"), "0340", "fff2"),
    FINNED_FRANKEN_SWORDFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Franken_Swordfish"), "0341", "fff3"),
    FINNED_FRANKEN_JELLYFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Franken_Jellyfish"), "0342", "fff4"),
    FINNED_FRANKEN_SQUIRMBAG(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Franken_Squirmbag"), "0343", "fff5"),
    FINNED_FRANKEN_WHALE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Franken_Whale"), "0344", "fff6"),
    FINNED_FRANKEN_LEVIATHAN(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Franken_Leviathan"), "0345", "fff7"),
    MUTANT_X_WING(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Mutant_X-Wing"), "0350", "mf2"),
    MUTANT_SWORDFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Mutant_Swordfish"), "0351", "mf3"),
    MUTANT_JELLYFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Mutant_Jellyfish"), "0352", "mf4"),
    MUTANT_SQUIRMBAG(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Mutant_Squirmbag"), "0353", "mf5"),
    MUTANT_WHALE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Mutant_Whale"), "0354", "mf6"),
    MUTANT_LEVIATHAN(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Mutant_Leviathan"), "0355", "mf7"),
    FINNED_MUTANT_X_WING(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Mutant_X-Wing"), "0360", "fmf2"),
    FINNED_MUTANT_SWORDFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Mutant_Swordfish"), "0361", "fmf3"),
    FINNED_MUTANT_JELLYFISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Mutant_Jellyfish"), "0362", "fmf4"),
    FINNED_MUTANT_SQUIRMBAG(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Mutant_Squirmbag"), "0363", "fmf5"),
    FINNED_MUTANT_WHALE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Mutant_Whale"), "0364", "fmf6"),
    FINNED_MUTANT_LEVIATHAN(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Finned_Mutant_Leviathan"), "0365", "fmf7"),
    SUE_DE_COQ(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Sue_de_Coq"), "1101", "sdc"),
    ALS_XZ(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Almost_Locked_Set_XZ-Rule"), "0901", "axz"),
    ALS_XY_WING(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Almost_Locked_Set_XY-Wing"), "0902", "axy"),
    ALS_XY_CHAIN(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Almost_Locked_Set_XY-Chain"), "0903", "ach"),
    DEATH_BLOSSOM(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Death_Blossom"), "0904", "db"),
    TEMPLATE_SET(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Template_Set"), "1201", "ts"),
    TEMPLATE_DEL(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Template_Delete"), "1202", "td"),
    FORCING_CHAIN(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Forcing_Chain"), "xxxx", "fc"),
    FORCING_CHAIN_CONTRADICTION(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Forcing_Chain_Contradiction"), "1301", "fcc"),
    FORCING_CHAIN_VERITY(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Forcing_Chain_Verity"), "1302", "fcv"),
    FORCING_NET(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Forcing_Net"), "xxxx", "fn"),
    FORCING_NET_CONTRADICTION(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Forcing_Net_Contradiction"), "1303", "fnc"),
    FORCING_NET_VERITY(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Forcing_Net_Verity"), "1304", "fnv"),
    BRUTE_FORCE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Brute_Force"), "xxxx", "bf"),
    INCOMPLETE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Incomplete_Solution"), "xxxx", "in"),
    GIVE_UP(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Give_Up"), "xxxx", "gu"),
    GROUPED_NICE_LOOP(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Grouped_Nice_Loop/AIC"), "xxxx", "gnl"),
    GROUPED_CONTINUOUS_NICE_LOOP(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Grouped_Continuous_Nice_Loop"), "0709", "gcnl"),
    GROUPED_DISCONTINUOUS_NICE_LOOP(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Grouped_Discontinuous_Nice_Loop"), "0710", "gdnl"),
    EMPTY_RECTANGLE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Empty_Rectangle"), "0402", "er"),
    HIDDEN_RECTANGLE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Hidden_Rectangle"), "0606", "hr"),
    AVOIDABLE_RECTANGLE_1(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Avoidable_Rectangle_Type_1"), "0607", "ar1"),
    AVOIDABLE_RECTANGLE_2(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Avoidable_Rectangle_Type_2"), "0608", "ar2"),
    AIC(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("AIC"), "0708", "aic"),
    GROUPED_AIC(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Grouped_AIC"), "0711", "gaic"),
    SIMPLE_COLORS(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Simple_Colors"), "xxxx", "sc"),
    MULTI_COLORS(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Multi_Colors"), "xxxx", "mc"),
    KRAKEN_FISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Kraken_Fish"), "xxxx", "kf"),    
    TURBOT_FISH(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Turbot_Fish"), "0403", "tf"),
    KRAKEN_FISH_TYPE_1(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Kraken_Fish_Type_1"), "0371", "kf1"),
    KRAKEN_FISH_TYPE_2(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Kraken_Fish_Type_2"), "0372", "kf2"),
    DUAL_TWO_STRING_KITE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Dual_2-String_Kite"), "0404", "d2sk"),
    DUAL_EMPTY_RECTANGLE(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Dual_Empty_Rectangle"), "0405", "der"),
    SIMPLE_COLORS_TRAP(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Simple_Colors_Trap"), "0500", "sc1"),
    SIMPLE_COLORS_WRAP(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Simple_Colors_Wrap"), "0501", "sc2"),
    MULTI_COLORS_1(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Multi_Colors_1"), "0502", "mc1"),
    MULTI_COLORS_2(java.util.ResourceBundle.getBundle("intl/SolutionType").getString("Multi_Colors_2"), "0503", "mc2");
    private String stepName;
    private String libraryType;
    private String argName;

    SolutionType() {
        // für XMLEncoder
    }

    SolutionType(String stepName, String libraryType, String argName) {
        this.setStepName(stepName);
        this.setLibraryType(libraryType);
        this.setArgName(argName);
    }

    @Override
    public String toString() {
        return "enum SolutionType: " + stepName + " (" + libraryType + "|" + argName + ")";
    }

    /**
     * compareTo() is final and can't be overridden (uses ordinal() for comparison);
     * Custom compare order: the order of steps matches the order configured in the
     * solver;
     * exception: fishes are sorted for (size, fin status, type)
     * fin status: finned & sashimi are treated separately
     * @param t The SolutionType which should be compared with this
     * @return &lt; 0 for this &lt; t, == 0 for this == t, &gt; 0 for this &gt; t
     */
    public int compare(SolutionType t) {
        StepConfig s1 = getStepConfig();
        StepConfig s2 = getStepConfig(t);
        if (isFish() && t.isFish()) {
            SolutionCategory c1 = s1.getCategory();
            SolutionCategory c2 = s2.getCategory();
            if (c1.ordinal() != c2.ordinal()) {
                // different categories -> category defines order
                return c1.ordinal() - c2.ordinal();
            } else {
                // same category -> type.ordinal can be used
                // unfortunately not!
                //return ordinal() - t.ordinal();
                int size = getFishSize() - t.getFishSize();
                if (size != 0) {
                    return size;
                }
                // same category and same size: check for Finned/Sashimi
                boolean sl = isSashimiFish();
                boolean sr = t.isSashimiFish();
                if (sl && sr || ! sl && ! sr) {
                    // both are sashimi or both are not sashimi -> equal
                    return 0;
                } else if (sl) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
        // for non-fishes use the sort order of the solver
        return s1.getIndex() - s2.getIndex();
    }

    public int getFishSize() {
        switch (this) {
            case X_WING:
            case FINNED_X_WING:
            case SASHIMI_X_WING:
            case FRANKEN_X_WING:
            case FINNED_FRANKEN_X_WING:
            case MUTANT_X_WING:
            case FINNED_MUTANT_X_WING:
                return 2;
            case SWORDFISH:
            case FINNED_SWORDFISH:
            case SASHIMI_SWORDFISH:
            case FRANKEN_SWORDFISH:
            case FINNED_FRANKEN_SWORDFISH:
            case MUTANT_SWORDFISH:
            case FINNED_MUTANT_SWORDFISH:
                return 3;
            case JELLYFISH:
            case FINNED_JELLYFISH:
            case SASHIMI_JELLYFISH:
            case FRANKEN_JELLYFISH:
            case FINNED_FRANKEN_JELLYFISH:
            case MUTANT_JELLYFISH:
            case FINNED_MUTANT_JELLYFISH:
                return 4;
            case SQUIRMBAG:
            case FINNED_SQUIRMBAG:
            case SASHIMI_SQUIRMBAG:
            case FRANKEN_SQUIRMBAG:
            case FINNED_FRANKEN_SQUIRMBAG:
            case MUTANT_SQUIRMBAG:
            case FINNED_MUTANT_SQUIRMBAG:
                return 5;
            case WHALE:
            case FINNED_WHALE:
            case SASHIMI_WHALE:
            case FRANKEN_WHALE:
            case FINNED_FRANKEN_WHALE:
            case MUTANT_WHALE:
            case FINNED_MUTANT_WHALE:
                return 6;
            default:
                return 7;
        }
    }
    
    public static boolean isSingle(SolutionType type) {
        if (type == HIDDEN_SINGLE || type == NAKED_SINGLE || type == FULL_HOUSE) {
            return true;
        }
        return false;
    }

    public boolean isSingle() {
        return isSingle(this);
    }

    public static boolean isSSTS(SolutionType type) {
        if (type.isSingle() ||
                type == HIDDEN_PAIR || type == HIDDEN_TRIPLE || type == HIDDEN_QUADRUPLE ||
                type == NAKED_PAIR || type == NAKED_TRIPLE || type == NAKED_QUADRUPLE ||
                type == LOCKED_PAIR || type == LOCKED_TRIPLE ||
                type == LOCKED_CANDIDATES || type == LOCKED_CANDIDATES_1 || type == LOCKED_CANDIDATES_2 ||
                type == X_WING || type == SWORDFISH || type == JELLYFISH ||
                type == XY_WING || type == SIMPLE_COLORS || type == MULTI_COLORS) {
            return true;
        }
        return false;
    }

    public boolean isSSTS() {
        return isSSTS(this);
    }

    public static boolean isHiddenSubset(SolutionType type) {
        if (type.isSingle() ||
                type == HIDDEN_PAIR || type == HIDDEN_TRIPLE || type == HIDDEN_QUADRUPLE) {
            return true;
        }
        return false;
    }

    public boolean isHiddenSubset() {
        return isHiddenSubset(this);
    }

    public StepConfig getStepConfig() {
        return getStepConfig(this);
    }

    /**
     * Don't forget SolutionTypes tat don't have StepConfigs
     * (e.g. DISCONTINUOUS_NICE_LOOP or DUAL_TWO_STRING_KITE)
     * 
     * @param type The SolutionType for which the StepConfig should be retrieved
     * @return The StepConfig appropriate for type
     */
    public static StepConfig getStepConfig(SolutionType type) {
        // Split Locked Candidates in two
//        if (type == SolutionType.LOCKED_CANDIDATES_1 || type == SolutionType.LOCKED_CANDIDATES_2) {
//            type = SolutionType.LOCKED_CANDIDATES;
//        }
        if (type == SolutionType.CONTINUOUS_NICE_LOOP || type == SolutionType.DISCONTINUOUS_NICE_LOOP ||
                type == SolutionType.AIC) {
            type = SolutionType.NICE_LOOP;
        }
        if (type == SolutionType.GROUPED_CONTINUOUS_NICE_LOOP || type == SolutionType.GROUPED_DISCONTINUOUS_NICE_LOOP ||
                type == SolutionType.GROUPED_AIC) {
            type = SolutionType.GROUPED_NICE_LOOP;
        }
        if (type == SolutionType.FORCING_CHAIN_CONTRADICTION || type == SolutionType.FORCING_CHAIN_VERITY) {
            type = SolutionType.FORCING_CHAIN;
        }
        if (type == SolutionType.FORCING_NET_CONTRADICTION || type == SolutionType.FORCING_NET_VERITY) {
            type = SolutionType.FORCING_NET;
        }
        if (type == SolutionType.KRAKEN_FISH_TYPE_1 || type == SolutionType.KRAKEN_FISH_TYPE_2) {
            type = SolutionType.KRAKEN_FISH;
        }
        if (type == SolutionType.DUAL_TWO_STRING_KITE) {
            type = SolutionType.TWO_STRING_KITE;
        }
        if (type == SolutionType.DUAL_EMPTY_RECTANGLE) {
            type = SolutionType.EMPTY_RECTANGLE;
        }
        if (type == SolutionType.SIMPLE_COLORS_TRAP || type == SolutionType.SIMPLE_COLORS_WRAP) {
            type = SolutionType.SIMPLE_COLORS;
        }
        if (type == SolutionType.MULTI_COLORS_1 || type == SolutionType.MULTI_COLORS_2) {
            type = SolutionType.MULTI_COLORS;
        }

        StepConfig[] configs = Options.getInstance().solverSteps;
        for (int i = 0; i < configs.length; i++) {
            if (configs[i].getType() == type) {
                return configs[i];
            }
        }
        return null;
    }

    public static boolean isFish(SolutionType type) {
        StepConfig config = getStepConfig(type);
        if (config != null) {
            if (config.getCategory() == SolutionCategory.BASIC_FISH ||
                    config.getCategory() == SolutionCategory.FINNED_BASIC_FISH ||
                    config.getCategory() == SolutionCategory.FRANKEN_FISH ||
                    config.getCategory() == SolutionCategory.FINNED_FRANKEN_FISH ||
                    config.getCategory() == SolutionCategory.MUTANT_FISH ||
                    config.getCategory() == SolutionCategory.FINNED_MUTANT_FISH) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean isFish() {
        return isFish(this);
    }
    
    public static boolean isBasicFish(SolutionType type) {
        StepConfig config = getStepConfig(type);
        if (config != null) {
            if (config.getCategory() == SolutionCategory.BASIC_FISH ||
                    config.getCategory() == SolutionCategory.FINNED_BASIC_FISH) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    
    public boolean isBasicFish() {
        return isBasicFish(this);
    }
    
    public static boolean isFrankenFish(SolutionType type) {
        StepConfig config = getStepConfig(type);
        if (config != null) {
            if (config.getCategory() == SolutionCategory.FRANKEN_FISH ||
                    config.getCategory() == SolutionCategory.FINNED_FRANKEN_FISH) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    
    public boolean isFrankenFish() {
        return isFrankenFish(this);
    }
    
    public static boolean isMutantFish(SolutionType type) {
        StepConfig config = getStepConfig(type);
        if (config != null) {
            if (config.getCategory() == SolutionCategory.MUTANT_FISH ||
                    config.getCategory() == SolutionCategory.FINNED_MUTANT_FISH) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    
    public boolean isMutantFish() {
        return isMutantFish(this);
    }
    
    public static boolean isKrakenFish(SolutionType type) {
        if (type == KRAKEN_FISH || type == KRAKEN_FISH_TYPE_1 || type == KRAKEN_FISH_TYPE_2) {
            return true;
        }
        return false;
    }
    
    public boolean isKrakenFish() {
        return isKrakenFish(this);
    }

    public static boolean isSashimiFish(SolutionType type) {
        if (type == SASHIMI_X_WING || type == SASHIMI_SWORDFISH || type == SASHIMI_JELLYFISH ||
                type == SASHIMI_SQUIRMBAG || type == SASHIMI_LEVIATHAN || type == SASHIMI_WHALE) {
            return true;
        }
        return false;
    }

    public boolean isSashimiFish() {
        return isSashimiFish(this);
    }

    public static boolean isSimpleChainOrLoop(SolutionType type) {
        if (type == NICE_LOOP || type == DISCONTINUOUS_NICE_LOOP || type == CONTINUOUS_NICE_LOOP ||
                type == GROUPED_NICE_LOOP || type == GROUPED_DISCONTINUOUS_NICE_LOOP ||
                type == GROUPED_CONTINUOUS_NICE_LOOP ||
                type == X_CHAIN || type == XY_CHAIN || type == REMOTE_PAIR ||
                type == AIC || type == GROUPED_AIC) {
            return true;
        }
        return false;
    }

    public boolean isSimpleChainOrLoop() {
        return isSimpleChainOrLoop(this);
    }

    public static boolean useCandToDelInLibraryFormat(SolutionType type) {
        boolean ret = false;
        if (type == NICE_LOOP || type == CONTINUOUS_NICE_LOOP || type == DISCONTINUOUS_NICE_LOOP ||
                type == GROUPED_NICE_LOOP || type == GROUPED_CONTINUOUS_NICE_LOOP ||
                type == GROUPED_DISCONTINUOUS_NICE_LOOP || type == AIC ||
                type == GROUPED_AIC ||
                type == FORCING_CHAIN_CONTRADICTION || type == FORCING_NET_CONTRADICTION ||
                type == ALS_XZ || type == ALS_XY_WING || type == ALS_XY_CHAIN ||
                type == DEATH_BLOSSOM || type == SUE_DE_COQ) {
            ret = true;
        }
        return ret;
    }

    public boolean useCandToDelInLibraryFormat() {
        return useCandToDelInLibraryFormat(this);
    }

    public static int getNonSinglesAnz() {
        int anz = 0;
        for (SolutionType tmp : values()) {
            if (!tmp.isSingle()) {
                anz++;
            }
        }
        return anz;
    }

    public static int getNonSSTSAnz() {
        int anz = 0;
        for (SolutionType tmp : values()) {
            if (!tmp.isSingle() && !tmp.isSSTS()) {
                anz++;
            }
        }
        return anz;
    }
    
    public static SolutionType getTypeFromArgName(String argName) {
        for (int i = 0; i < values().length; i++) {
            if (argName.compareToIgnoreCase(values()[i].argName) == 0) {
                return values()[i];
            }
        }
        return null;
    }

    public static SolutionType getTypeFromLibraryType(String libraryType) {
        SolutionType ret = getTypeFromLibraryTypeInternal(libraryType);
        if (ret == null) {
            // could be a siamese fish: if the last character is '1' try without it
            if (libraryType.charAt(libraryType.length() - 1) == '1') {
                ret = getTypeFromLibraryTypeInternal(libraryType.substring(0, libraryType.length() - 1));
            }
        }
        return ret;
    }

    private static SolutionType getTypeFromLibraryTypeInternal(String libraryType) {
        for (int i = 0; i < values().length; i++) {
            if (libraryType.compareToIgnoreCase(values()[i].libraryType) == 0) {
                return values()[i];
            }
        }
        return null;
    }

    public String getLibraryType() {
        return libraryType;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public void setLibraryType(String libraryType) {
        this.libraryType = libraryType;
    }

    public String getArgName() {
        return argName;
    }

    public void setArgName(String argName) {
        this.argName = argName;
    }
}
