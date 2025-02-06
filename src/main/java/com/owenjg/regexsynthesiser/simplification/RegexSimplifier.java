package com.owenjg.regexsynthesiser.simplification;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexSimplifier {
    //TODO Use more symbols that just () and |
    private RegexPostProcessor postProcessor;

    public RegexSimplifier() {
        this.postProcessor = new RegexPostProcessor();
    }

    public String simplify(String regex) {
        String simplified = regex;

        // Combine alternations with common prefixes
        simplified = simplified.replaceAll("([^|()]+)\\|\\1([^|()]+)", "$1$2");

        // Remove duplicate alternations
        simplified = simplified.replaceAll("\\((.*?)\\)\\|\\1", "($1)");

        // Combine or-patterns with common elements
        simplified = simplified.replaceAll("\\(([^|()]+)\\|([^|()]+)\\)\\|\\(\\1\\|([^|()]+)\\)", "($1|$2|$3)");

        // Optimize alternations with common parts
        simplified = optimizeAlternations(simplified);

        return simplified;
    }

    private String optimizeAlternations(String regex) {
        // Find common prefixes in alternations
        String pattern = "\\((.*?)\\|.*?\\)";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(regex);

        StringBuffer result = new StringBuffer();
        while (m.find()) {
            String group = m.group();
            String[] parts = group.substring(1, group.length()-1).split("\\|");

            String commonPrefix = findCommonPrefix(parts);
            if (!commonPrefix.isEmpty()) {
                String simplified = commonPrefix + "(" +
                        String.join("|", Arrays.stream(parts)
                                .map(s -> s.substring(commonPrefix.length()))
                                .filter(s -> !s.isEmpty())
                                .toArray(String[]::new)) + ")";
                m.appendReplacement(result, simplified);
            }
        }
        m.appendTail(result);

        return result.toString();
    }

    private String findCommonPrefix(String[] strings) {
        if (strings.length == 0) return "";
        String first = strings[0];
        int prefixLen = first.length();

        for (int i = 1; i < strings.length; i++) {
            prefixLen = Math.min(prefixLen, strings[i].length());
            for (int j = 0; j < prefixLen; j++) {
                if (first.charAt(j) != strings[i].charAt(j)) {
                    prefixLen = j;
                    break;
                }
            }
        }

        return first.substring(0, prefixLen);
    }
}