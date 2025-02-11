package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;
import java.util.*;


import java.util.*;
import java.util.stream.Collectors;

public class PatternAnalyzer {
    public String generalizePattern(List<String> examples) {
        if (examples.isEmpty()) {
            return "";
        }

        // First find common prefix and suffix
        String commonPrefix = findCommonPrefix(examples);
        List<String> withoutPrefix = examples.stream()
                .map(s -> s.substring(commonPrefix.length()))
                .collect(Collectors.toList());

        String commonSuffix = findCommonSuffix(withoutPrefix);
        List<String> middle = withoutPrefix.stream()
                .map(s -> s.substring(0, s.length() - commonSuffix.length()))
                .collect(Collectors.toList());

        // Analyze the middle part
        String middlePattern = analyzeMiddlePattern(middle);

        return commonPrefix + middlePattern + commonSuffix;
    }

    private String findCommonPrefix(List<String> examples) {
        if (examples.isEmpty()) return "";

        String firstStr = examples.get(0);
        StringBuilder prefix = new StringBuilder();

        for (int i = 0; i < firstStr.length(); i++) {
            char currentChar = firstStr.charAt(i);
            int finalI = i;
            if (examples.stream().allMatch(s -> s.length() > finalI && s.charAt(finalI) == currentChar)) {
                prefix.append(currentChar);
            } else {
                break;
            }
        }

        return prefix.toString();
    }

    private String findCommonSuffix(List<String> examples) {
        if (examples.isEmpty()) return "";

        List<String> reversed = examples.stream()
                .map(s -> new StringBuilder(s).reverse().toString())
                .collect(Collectors.toList());

        return new StringBuilder(findCommonPrefix(reversed)).reverse().toString();
    }

    private String analyzeMiddlePattern(List<String> examples) {
        if (examples.isEmpty() || examples.stream().allMatch(String::isEmpty)) {
            return "";
        }

        // First check if all strings are the same length
        boolean sameLengths = examples.stream().allMatch(s -> s.length() == examples.get(0).length());
        if (sameLengths) {
            // Find positions where characters differ
            String firstExample = examples.get(0);
            int length = firstExample.length();
            List<Integer> differentPositions = new ArrayList<>();

            for (int i = 0; i < length; i++) {
                final int pos = i;
                Set<Character> charsAtPosition = examples.stream()
                        .map(s -> s.charAt(pos))
                        .collect(Collectors.toSet());

                if (charsAtPosition.size() > 1) {
                    differentPositions.add(i);
                }
            }

            // If we only have a few positions that differ
            if (!differentPositions.isEmpty() && differentPositions.size() <= 3) {
                StringBuilder pattern = new StringBuilder();
                int lastPos = 0;

                // Build pattern with character classes at differing positions
                for (int pos : differentPositions) {
                    // Add common characters before this position
                    if (pos > lastPos) {
                        pattern.append(firstExample.substring(lastPos, pos));
                    }

                    // Add character class for this position
                    final int finalPos = pos;
                    Set<Character> chars = examples.stream()
                            .map(s -> s.charAt(finalPos))
                            .collect(Collectors.toSet());
                    pattern.append(generalizeCharacterClass(chars));

                    lastPos = pos + 1;
                }

                // Add remaining common characters
                if (lastPos < length) {
                    pattern.append(firstExample.substring(lastPos));
                }

                return pattern.toString();
            }

            return analyzeFixedLengthPattern(examples);
        }

        // Check for optional repeating pattern
        String optionalRepeatingPattern = findOptionalRepeatingPattern(examples);
        if (optionalRepeatingPattern != null) {
            return optionalRepeatingPattern;
        }

        // Check for repeating pattern
        String repeatingPattern = findRepeatingPattern(examples);
        if (repeatingPattern != null) {
            return repeatingPattern;
        }

        // Check for variable length patterns
        String variablePattern = analyzeVariableLengthPattern(examples);
        if (variablePattern != null) {
            return variablePattern;
        }

        // If no clear pattern, analyze as complex pattern
        return analyzeComplexPattern(examples);
    }
    private String findOptionalRepeatingPattern(List<String> examples) {
        // If any string is empty, we might have an optional pattern
        boolean hasEmpty = examples.stream().anyMatch(String::isEmpty);

        // Get all non-empty strings
        List<String> nonEmpty = examples.stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (nonEmpty.isEmpty()) return "";

        // Check if all non-empty strings consist of the same character
        char repeatingChar = nonEmpty.get(0).charAt(0);
        boolean allSameChar = nonEmpty.stream()
                .allMatch(s -> s.chars().allMatch(c -> c == repeatingChar));

        if (allSameChar) {
            // If we found empty strings, use * (zero or more)
            // Otherwise use + (one or more)
            return "(" + repeatingChar + (hasEmpty ? "*" : "+") + ")";
        }

        return null;
    }
    private String findRepeatingPattern(List<String> examples) {
        // Find the shortest example to use as potential pattern
        String shortest = examples.stream()
                .min(Comparator.comparingInt(String::length))
                .orElse("");

        if (shortest.isEmpty()) return null;

        // Check if other strings are repetitions of this pattern
        for (int len = 1; len <= shortest.length(); len++) {
            if (shortest.length() % len != 0) continue;

            String pattern = shortest.substring(0, len);
            int finalLen = len;
            boolean isRepeating = examples.stream().allMatch(s -> {
                if (s.isEmpty()) return true;
                if (s.length() % finalLen != 0) return false;
                for (int i = 0; i < s.length(); i += finalLen) {
                    if (!s.substring(i, i + finalLen).equals(pattern)) {
                        return false;
                    }
                }
                return true;
            });

            if (isRepeating) {
                return "(" + pattern + ")+";
            }
        }

        // Check for single character repetition
        if (examples.stream().allMatch(s -> s.isEmpty() || s.chars().allMatch(c -> c == s.charAt(0)))) {
            return "(" + shortest.charAt(0) + ")*";
        }

        return null;
    }

    private String analyzeFixedLengthPattern(List<String> examples) {
        StringBuilder pattern = new StringBuilder();
        int length = examples.get(0).length();

        for (int i = 0; i < length; i++) {
            int finalI = i;
            Set<Character> chars = examples.stream()
                    .map(s -> s.charAt(finalI))
                    .collect(Collectors.toSet());

            pattern.append(generalizeCharacterClass(chars));
        }

        return pattern.toString();
    }

    private String analyzeVariableLengthPattern(List<String> examples) {
        // Get all unique characters across all strings
        Set<Character> allChars = examples.stream()
                .flatMap(s -> s.chars().mapToObj(c -> (char)c))
                .collect(Collectors.toSet());

        // Check if all characters are of same type
        String charClassPattern = generalizeCharacterClass(allChars);
        if (!charClassPattern.startsWith("[") || charClassPattern.length() <= 3) {
            return charClassPattern + "+";
        }

        return null;
    }

    private String analyzeComplexPattern(List<String> examples) {
        // Group similar strings
        Map<Integer, List<String>> lengthGroups = examples.stream()
                .collect(Collectors.groupingBy(String::length));

        if (lengthGroups.size() == 1) {
            return analyzeFixedLengthPattern(examples);
        }

        // Try to find character class patterns within groups
        List<String> patterns = new ArrayList<>();
        for (List<String> group : lengthGroups.values()) {
            String groupPattern = analyzeFixedLengthPattern(group);
            if (!patterns.contains(groupPattern)) {
                patterns.add(groupPattern);
            }
        }

        // If we found multiple patterns, combine them
        if (patterns.size() > 1) {
            return "(" + String.join("|", patterns) + ")";
        } else if (patterns.size() == 1) {
            return patterns.get(0);
        }

        // Fallback to character class if possible
        Set<Character> allChars = examples.stream()
                .flatMap(s -> s.chars().mapToObj(c -> (char)c))
                .collect(Collectors.toSet());

        String charClass = generalizeCharacterClass(allChars);
        if (!charClass.equals(examples.get(0))) {
            return charClass + "+";
        }

        // Last resort: alternation
        return "(" + examples.stream().distinct().collect(Collectors.joining("|")) + ")";
    }

    private String generalizeCharacterClass(Set<Character> chars) {
        if (chars.size() == 1) {
            return String.valueOf(chars.iterator().next());
        }

        boolean allDigits = chars.stream().allMatch(Character::isDigit);
        boolean allLower = chars.stream().allMatch(c -> Character.isLowerCase(c));
        boolean allUpper = chars.stream().allMatch(c -> Character.isUpperCase(c));
        boolean allLetters = chars.stream().allMatch(Character::isLetter);
        boolean allAlphanumeric = chars.stream().allMatch(Character::isLetterOrDigit);

        if (allDigits) return "\\d";
        if (allLower) return "[a-z]";
        if (allUpper) return "[A-Z]";
        if (allLetters) return "[a-zA-Z]";
        if (allAlphanumeric) return "\\w";

        return "[" + chars.stream()
                .map(c -> {
                    if (c == '-' || c == '^' || c == '\\' || c == ']' || c == '[') {
                        return "\\" + c;
                    }
                    return String.valueOf(c);
                })
                .sorted()
                .collect(Collectors.joining()) + "]";
    }
}