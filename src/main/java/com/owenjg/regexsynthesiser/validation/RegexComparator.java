package com.owenjg.regexsynthesiser.validation;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A regex comparator that calculates numerical ratios between two regexes.
 */
public class RegexComparator {

    /**
     * Compare regexes and return only the patterns in the correct format
     */
    public static String compareRegexes(String analyzerRegex, String dfaRegex) {
        if (analyzerRegex == null || dfaRegex == null) {
            return "Pattern Analyzer: " + (analyzerRegex != null ? analyzerRegex : "N/A") + "\n" +
                    "DFA-based: " + (dfaRegex != null ? dfaRegex : "N/A");
        }

        // Return only the regex patterns for the UI fields
        return "Pattern Analyzer: " + analyzerRegex + "\n" +
                "DFA-based: " + dfaRegex;
    }

    /**
     * Calculate the length ratio between two regexes.
     * Returns the ratio as Pattern Analyzer : DFA
     * @return A double representing the ratio (e.g., 0.75 means PA is 75% the length of DFA)
     */
    public static double getLengthRatio(String analyzerRegex, String dfaRegex) {
        if (analyzerRegex == null || dfaRegex == null || dfaRegex.length() == 0) {
            return 0.0;
        }

        return (double) analyzerRegex.length() / dfaRegex.length();
    }

    /**
     * Calculate the complexity ratio between two regexes based on special characters.
     * Returns the ratio as Pattern Analyzer : DFA
     * @return A double representing the ratio (e.g., 0.75 means PA has 75% the special chars of DFA)
     */
    public static double getComplexityRatio(String analyzerRegex, String dfaRegex) {
        if (analyzerRegex == null || dfaRegex == null) {
            return 0.0;
        }

        int analyzerComplexity = countSpecialChars(analyzerRegex);
        int dfaComplexity = countSpecialChars(dfaRegex);

        if (dfaComplexity == 0) {
            return analyzerComplexity == 0 ? 1.0 : Double.POSITIVE_INFINITY;
        }

        return (double) analyzerComplexity / dfaComplexity;
    }

    /**
     * Checks if a regex pattern is valid.
     */
    private static boolean isValidRegex(String regex) {
        try {
            Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /**
     * Counts the special characters in a regex.
     */
    private static int countSpecialChars(String regex) {
        int count = 0;
        String specialChars = "[](){}*+?.|^$\\";

        for (char c : regex.toCharArray()) {
            if (specialChars.indexOf(c) >= 0) {
                count++;
            }
        }

        return count;
    }
}