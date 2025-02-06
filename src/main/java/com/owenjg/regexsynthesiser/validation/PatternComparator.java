package com.owenjg.regexsynthesiser.validation;

import java.util.Arrays;

public class PatternComparator {
    public boolean areEquivalent(String regex1, String regex2) {
        // Convert both patterns to canonical form
        String canonical1 = canonicalize(regex1);
        String canonical2 = canonicalize(regex2);

        return canonical1.equals(canonical2);
    }

    private String canonicalize(String regex) {
        String canonical = regex;

        // Sort alternations
        canonical = sortAlternations(canonical);

        // Normalize quantifiers
        canonical = normalizeQuantifiers(canonical);

        // Remove unnecessary parentheses
        canonical = removeUnnecessaryParentheses(canonical);

        return canonical;
    }

    private String sortAlternations(String regex) {
        // Split on unescaped |
        String[] parts = regex.split("(?<!\\\\)\\|");
        Arrays.sort(parts);
        return String.join("|", parts);
    }

    private String normalizeQuantifiers(String regex) {
        // Convert {1} to no quantifier
        regex = regex.replaceAll("\\{1\\}", "");

        // Convert {0,1} to ?
        regex = regex.replaceAll("\\{0,1\\}", "?");

        // Convert {0,} to *
        regex = regex.replaceAll("\\{0,\\}", "*");

        // Convert {1,} to +
        regex = regex.replaceAll("\\{1,\\}", "+");

        return regex;
    }

    private String removeUnnecessaryParentheses(String regex) {
        // Remove parentheses around single characters
        regex = regex.replaceAll("\\(([^|()\\[\\]{}+*?\\\\])\\)", "$1");

        // Remove parentheses around alternations when they're the only group
        regex = regex.replaceAll("^\\(([^()]+)\\)$", "$1");

        return regex;
    }
}
