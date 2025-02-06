package com.owenjg.regexsynthesiser.simplification;

public class RegexSimplifier {
    private RegexPostProcessor postProcessor;

    public RegexSimplifier() {
        this.postProcessor = new RegexPostProcessor();
    }

    public String simplify(String regex) {
        String simplified = regex;

        // Remove redundant patterns
        simplified = removeRedundancy(simplified);

        // Apply post-processing rules
        simplified = postProcessor.process(simplified);

        return simplified;
    }

    // In RegexSimplifier.java
    private String removeRedundancy(String regex) {
        String simplified = regex;

        // Remove repeated groups
        simplified = simplified.replaceAll("\\(([^|()]+)\\)\\1+", "($1)+");

        // Simplify alternations with common prefixes/suffixes
        simplified = simplified.replaceAll("([a-zA-Z0-9]+)\\|\\1([a-zA-Z0-9]+)", "$1$2");

        // Remove redundant character classes
        simplified = simplified.replaceAll("\\[([a-zA-Z0-9])\\]", "$1");

        // Combine adjacent quantifiers
        simplified = simplified.replaceAll("([a-zA-Z0-9])\\+\\+", "$1+");
        simplified = simplified.replaceAll("([a-zA-Z0-9])\\*\\*", "$1*");

        // Simplify nested groups
        simplified = simplified.replaceAll("\\(\\(([^()]+)\\)\\)", "($1)");

        return simplified;
    }
}