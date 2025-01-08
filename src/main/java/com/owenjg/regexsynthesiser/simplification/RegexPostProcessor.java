package com.owenjg.regexsynthesiser.simplification;

public class RegexPostProcessor {
    public String process(String regex) {
        String simplified = regex;

        // Apply various simplification rules
        simplified = removeRedundantGroups(simplified);
        simplified = optimizeQuantifiers(simplified);
        simplified = convertToShorthand(simplified);

        return simplified;
    }

    private String removeRedundantGroups(String regex) {
        // Remove unnecessary grouping
        return regex.replaceAll("\\(([^|()]+)\\)", "$1");
    }

    private String optimizeQuantifiers(String regex) {
        // Convert repeated patterns to appropriate quantifiers
        return regex
                .replaceAll("(\\w)\\1{3,}", "$1{4,}")
                .replaceAll("(\\w)\\1\\1", "$1{3}")
                .replaceAll("(\\w)\\1", "$1{2}");
    }

    private String convertToShorthand(String regex) {
        // Convert character classes to shorthand notations
        return regex
                .replaceAll("[0-9]", "\\d")
                .replaceAll("[a-zA-Z]", "\\w")
                .replaceAll("[ \\t\\n\\r]", "\\s");
    }
}
