package com.owenjg.regexsynthesiser.simplification;

import java.util.*;

public class RegexSimplifier {



    public static String simplify(String regex) {
        // First, normalize the regex by removing whitespace
        String normalized = regex.trim();

        // Extract patterns, handling both parenthesized and non-parenthesized input
        List<String> patterns;
        if (normalized.startsWith("(") && normalized.endsWith(")") && isMatchingParentheses(normalized)) {
            // Remove outer parentheses first if they're matching
            patterns = extractTopLevelAlternations(normalized.substring(1, normalized.length() - 1));
        } else {
            patterns = extractTopLevelAlternations(normalized);
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

        if (currentPattern.length() > 0) {
            patterns.add(currentPattern.toString());
        }

        return patterns;
    }

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

    private static boolean arePatternsSame(List<String> patterns) {
        if (patterns.isEmpty()) return true;
        String first = patterns.get(0);
        return patterns.stream().allMatch(p -> p.equals(first));
    }

}