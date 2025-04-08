package com.owenjg.regexsynthesiser.simplification;

import java.util.*;

/**
 * A utility class that simplifies regular expressions by removing redundancies
 * and improving clarity.
 *
 * This class focuses primarily on removing duplicate alternations and
 * unnecessary parentheses from regular expressions.
 */
public class RegexSimplifier {

    /**
     * Simplifies a regular expression by removing redundancies.
     *
     * @param regex The regular expression to simplify
     * @return A simplified version of the input regular expression
     */
    public static String simplify(String regex) {
        // First, normalise the regex by removing whitespace
        String normalised = regex.trim();

        // Extract patterns, handling both parenthesised and non-parenthesised input
        List<String> patterns;
        if (normalised.startsWith("(") && normalised.endsWith(")") && isMatchingParentheses(normalised)) {
            // Remove outer parentheses first if they're matching
            patterns = extractTopLevelAlternations(normalised.substring(1, normalised.length() - 1));
        } else {
            patterns = extractTopLevelAlternations(normalised);
        }

        // If all patterns are identical, return just one instance
        if (arePatternsSame(patterns)) {
            return patterns.get(0);
        }

        // Remove duplicates while preserving order
        List<String> uniquePatterns = new ArrayList<>(new LinkedHashSet<>(patterns));

        // If we only have one pattern after removing duplicates, return it
        if (uniquePatterns.size() == 1) {
            return uniquePatterns.get(0);
        }

        // Join patterns with alternation
        String result = String.join("|", uniquePatterns);
        return result.contains("|") ? "(" + result + ")" : result;
    }

    /**
     * Checks if the parentheses in the regex are properly matched.
     *
     * @param regex The regular expression to check
     * @return true if all parentheses are properly matched, false otherwise
     */
    private static boolean isMatchingParentheses(String regex) {
        int count = 0;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (!isEscaped(regex, i)) {
                if (c == '(') count++;
                if (c == ')') count--;
                if (count < 0) return false;
            }
        }
        return count == 0;
    }

    /**
     * Extracts top-level alternations from a regular expression.
     * This splits a regex on unescaped pipe symbols (|) that are not inside
     * parentheses or character classes.
     *
     * @param regex The regular expression to process
     * @return A list of alternation components
     */
    private static List<String> extractTopLevelAlternations(String regex) {
        List<String> patterns = new ArrayList<>();
        StringBuilder currentPattern = new StringBuilder();
        int parenthesesCount = 0;
        boolean inCharacterClass = false;

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);

            // Handle character class brackets
            if (c == '[' && !isEscaped(regex, i)) {
                inCharacterClass = true;
            } else if (c == ']' && !isEscaped(regex, i)) {
                inCharacterClass = false;
            }

            // Only count unescaped parentheses outside character classes
            if (!inCharacterClass) {
                if (c == '(' && !isEscaped(regex, i)) {
                    parenthesesCount++;
                } else if (c == ')' && !isEscaped(regex, i)) {
                    parenthesesCount--;
                }
            }

            // Split on unescaped pipe symbols at top level
            if (c == '|' && parenthesesCount == 0 && !inCharacterClass && !isEscaped(regex, i)) {
                patterns.add(currentPattern.toString());
                currentPattern = new StringBuilder();
            } else {
                currentPattern.append(c);
            }
        }

        if (!currentPattern.isEmpty()) {
            patterns.add(currentPattern.toString());
        }

        return patterns;
    }

    /**
     * Checks if a character at the specified position is escaped with a backslash.
     *
     * @param regex The regular expression string
     * @param index The position to check
     * @return true if the character is escaped, false otherwise
     */
    private static boolean isEscaped(String regex, int index) {
        if (index <= 0) return false;

        int count = 0;
        int i = index - 1;
        while (i >= 0 && regex.charAt(i) == '\\') {
            count++;
            i--;
        }
        return count % 2 == 1;
    }

    /**
     * Checks if all patterns in a list are identical.
     *
     * @param patterns The list of patterns to compare
     * @return true if all patterns are the same, false otherwise
     */
    private static boolean arePatternsSame(List<String> patterns) {
        if (patterns.isEmpty()) return true;
        String first = patterns.get(0);
        return patterns.stream().allMatch(p -> p.equals(first));
    }

}