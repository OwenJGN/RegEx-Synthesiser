package com.owenjg.regexsynthesiser.synthesis;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The PatternAnalyser class analyses input examples and generates regular expressions
 * that match positive examples while excluding negative examples.
 * It uses a variety of pattern recognition techniques to create optimal regex patterns.
 */
public class PatternAnalyser {
    /**
     * Generates a regular expression pattern that matches all positive examples
     * and excludes all negative examples.
     *
     * @param positiveExamples List of strings that should match the pattern
     * @param negativeExamples List of strings that should not match the pattern
     * @return A regular expression that satisfies the given examples
     */
    public String generalisePattern(List<String> positiveExamples, List<String> negativeExamples) {
        // First try the original pattern based only on positive examples
        String initialPattern = generaliseFromPositive(positiveExamples);

        // Check if it already excludes all negative examples
        if (negativeExamples.isEmpty() || isPatternValid(initialPattern, positiveExamples, negativeExamples)) {
            return initialPattern;
        }

        // If not, refine the pattern to handle negative examples
        return refinePattern(initialPattern, positiveExamples, negativeExamples);
    }

    /**
     * Finds the longest common prefix shared by all example strings.
     *
     * @param examples List of strings to analyse
     * @return The common prefix or empty string if none exists
     */
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

    /**
     * Finds the longest common suffix shared by all example strings.
     *
     * @param examples List of strings to analyse
     * @return The common suffix or empty string if none exists
     */
    private String findCommonSuffix(List<String> examples) {
        if (examples.isEmpty()) return "";

        // Reverse all strings and find their common prefix, then reverse the result
        List<String> reversed = examples.stream()
                .map(s -> new StringBuilder(s).reverse().toString())
                .collect(Collectors.toList());

        return new StringBuilder(findCommonPrefix(reversed)).reverse().toString();
    }

    /**
     * Analyses the middle pattern after removing common prefix and suffix.
     * This is the core pattern recognition algorithm that handles various cases.
     *
     * @param examples List of strings to analyse (with prefix/suffix removed)
     * @return A regex pattern for the middle section
     */
    private String analyseMiddlePattern(List<String> examples) {
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
                    pattern.append(generaliseCharacterClass(chars));

                    lastPos = pos + 1;
                }

                // Add remaining common characters
                if (lastPos < length) {
                    pattern.append(firstExample.substring(lastPos));
                }

                return pattern.toString();
            }

            return analyseFixedLengthPattern(examples);
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
        String variablePattern = analyseVariableLengthPattern(examples);
        if (variablePattern != null) {
            return variablePattern;
        }

        // If no clear pattern, analyse as complex pattern
        return analyseComplexPattern(examples);
    }

    /**
     * Detects optional repeating patterns, such as characters that may appear
     * zero or more times in the examples.
     *
     * @param examples List of strings to analyse
     * @return A regex pattern for optional repetition or null if none found
     */
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

    /**
     * Detects repeating patterns in the examples, such as sequences that
     * repeat one or more times.
     *
     * @param examples List of strings to analyse
     * @return A regex pattern for repetition or null if none found
     */
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

    /**
     * Analyses patterns where all examples have the same length.
     * Creates character classes at each position where needed.
     *
     * @param examples List of fixed-length strings to analyse
     * @return A regex pattern for fixed-length strings
     */
    private String analyseFixedLengthPattern(List<String> examples) {
        StringBuilder pattern = new StringBuilder();
        int length = examples.get(0).length();

        for (int i = 0; i < length; i++) {
            int finalI = i;
            Set<Character> chars = examples.stream()
                    .map(s -> s.charAt(finalI))
                    .collect(Collectors.toSet());

            pattern.append(generaliseCharacterClass(chars));
        }

        return pattern.toString();
    }

    /**
     * Analyses patterns where examples have variable lengths.
     * Attempts to find a common character class that applies to all.
     *
     * @param examples List of variable-length strings to analyse
     * @return A regex pattern for variable-length strings or null if no pattern found
     */
    private String analyseVariableLengthPattern(List<String> examples) {
        // Get all unique characters across all strings
        Set<Character> allChars = examples.stream()
                .flatMap(s -> s.chars().mapToObj(c -> (char)c))
                .collect(Collectors.toSet());

        // Check if all characters are of same type
        String charClassPattern = generaliseCharacterClass(allChars);
        if (!charClassPattern.startsWith("[") || charClassPattern.length() <= 3) {
            return charClassPattern + "+";
        }

        return null;
    }

    /**
     * Handles complex patterns that don't fit simpler categories.
     * This is a fallback method for when other pattern recognition fails.
     *
     * @param examples List of strings to analyse
     * @return A regex pattern that covers the complex patterns
     */
    private String analyseComplexPattern(List<String> examples) {
        // Group similar strings
        Map<Integer, List<String>> lengthGroups = examples.stream()
                .collect(Collectors.groupingBy(String::length));

        if (lengthGroups.size() == 1) {
            return analyseFixedLengthPattern(examples);
        }

        // Try to find character class patterns within groups
        List<String> patterns = new ArrayList<>();
        for (List<String> group : lengthGroups.values()) {
            String groupPattern = analyseFixedLengthPattern(group);
            if (!patterns.contains(groupPattern)) {
                patterns.add(groupPattern);
            }
        }

        // If found multiple patterns, combine them
        if (patterns.size() > 1) {
            return "(" + String.join("|", patterns) + ")";
        } else if (patterns.size() == 1) {
            return patterns.get(0);
        }

        // Fallback to character class if possible
        Set<Character> allChars = examples.stream()
                .flatMap(s -> s.chars().mapToObj(c -> (char)c))
                .collect(Collectors.toSet());

        String charClass = generaliseCharacterClass(allChars);
        if (!charClass.equals(examples.get(0))) {
            return charClass + "+";
        }

        // Last resort: alternation
        return "(" + examples.stream().distinct().collect(Collectors.joining("|")) + ")";
    }

    /**
     * Generates a regex pattern from positive examples by finding
     * common prefixes, suffixes, and analysing the middle pattern.
     *
     * @param examples List of positive examples
     * @return A regex pattern that matches the positive examples
     */
    private String generaliseFromPositive(List<String> examples) {
        // Find common prefix among all examples
        String commonPrefix = findCommonPrefix(examples);
        List<String> withoutPrefix = examples.stream()
                .map(s -> s.substring(commonPrefix.length()))
                .collect(Collectors.toList());

        // Find common suffix in the remainder
        String commonSuffix = findCommonSuffix(withoutPrefix);
        List<String> middle = withoutPrefix.stream()
                .map(s -> s.substring(0, s.length() - commonSuffix.length()))
                .collect(Collectors.toList());

        // Analyse the middle pattern after removing prefix and suffix
        String middlePattern = analyseMiddlePattern(middle);
        return commonPrefix + middlePattern + commonSuffix;
    }

    /**
     * Refines a regex pattern to ensure it excludes negative examples
     * while still matching all positive examples.
     *
     * @param initialPattern The pattern generated from positive examples
     * @param positiveExamples List of strings that should match
     * @param negativeExamples List of strings that should not match
     * @return A refined regex pattern
     */
    private String refinePattern(String initialPattern, List<String> positiveExamples, List<String> negativeExamples) {
        // Strategy 1: Try to make character classes more specific
        String refinedPattern = refineCharacterClasses(initialPattern, positiveExamples, negativeExamples);
        if (isPatternValid(refinedPattern, positiveExamples, negativeExamples)) {
            return refinedPattern;
        }

        // Strategy 2: Add length constraints if needed
        refinedPattern = addLengthConstraints(refinedPattern, positiveExamples, negativeExamples);
        if (isPatternValid(refinedPattern, positiveExamples, negativeExamples)) {
            return refinedPattern;
        }

        // Strategy 3: Convert to alternation if needed
        return createAlternationPattern(positiveExamples, negativeExamples);
    }

    /**
     * Refines character classes in a pattern to make them more specific
     * based on actual characters in positive and negative examples.
     *
     * @param pattern The initial regex pattern
     * @param positiveExamples List of strings that should match
     * @param negativeExamples List of strings that should not match
     * @return A refined regex pattern with more specific character classes
     */
    private String refineCharacterClasses(String pattern, List<String> positiveExamples, List<String> negativeExamples) {
        // Replace generic character classes with more specific ones
        Map<String, Set<Character>> actualChars = new HashMap<>();

        // Collect actual characters used in positive examples for each position
        for (String example : positiveExamples) {
            for (int i = 0; i < example.length(); i++) {
                String pos = String.valueOf(i);
                actualChars.computeIfAbsent(pos, k -> new HashSet<>()).add(example.charAt(i));
            }
        }

        // Remove characters that appear in negative examples at same positions
        for (String negative : negativeExamples) {
            for (int i = 0; i < negative.length(); i++) {
                String pos = String.valueOf(i);
                if (actualChars.containsKey(pos)) {
                    Set<Character> chars = actualChars.get(pos);
                    if (chars.size() > 1) { // Don't remove if only one char left
                        chars.remove(negative.charAt(i));
                    }
                }
            }
        }

        // Replace \w, \d, [a-z], etc. with specific character classes
        String refined = pattern;
        for (Map.Entry<String, Set<Character>> entry : actualChars.entrySet()) {
            refined = refined.replace("\\w", generaliseCharacterClass(entry.getValue()))
                    .replace("\\d", generaliseCharacterClass(entry.getValue()))
                    .replace("[a-z]", generaliseCharacterClass(entry.getValue()))
                    .replace("[A-Z]", generaliseCharacterClass(entry.getValue()))
                    .replace("[a-zA-Z]", generaliseCharacterClass(entry.getValue()));
        }

        return refined;
    }

    /**
     * Adds length constraints to a pattern to exclude negative examples
     * that differ in length from positive examples.
     *
     * @param pattern The initial regex pattern
     * @param positiveExamples List of strings that should match
     * @param negativeExamples List of strings that should not match
     * @return A refined regex pattern with length constraints
     */
    private String addLengthConstraints(String pattern, List<String> positiveExamples, List<String> negativeExamples) {
        int minLength = positiveExamples.stream().mapToInt(String::length).min().orElse(0);
        int maxLength = positiveExamples.stream().mapToInt(String::length).max().orElse(0);

        // If there's a length difference in negative examples, add constraints
        boolean needsConstraint = negativeExamples.stream()
                .anyMatch(s -> s.length() < minLength || s.length() > maxLength);

        if (needsConstraint) {
            if (minLength == maxLength) {
                return "^" + pattern + "$";
            } else {
                return "^" + pattern + "{" + minLength + "," + maxLength + "}$";
            }
        }

        return pattern;
    }

    /**
     * Creates an alternation pattern as a last resort when other strategies fail.
     * This creates a pattern with specific alternatives for each length group.
     *
     * @param positiveExamples List of strings that should match
     * @param negativeExamples List of strings that should not match
     * @return A regex pattern using alternation
     */
    private String createAlternationPattern(List<String> positiveExamples, List<String> negativeExamples) {
        // Group positive examples by length
        Map<Integer, List<String>> lengthGroups = positiveExamples.stream()
                .collect(Collectors.groupingBy(String::length));

        // Create patterns for each length group
        List<String> patterns = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : lengthGroups.entrySet()) {
            String groupPattern = analyseFixedLengthPattern(entry.getValue());
            patterns.add(groupPattern);
        }

        // Combine patterns with alternation
        return "(" + String.join("|", patterns) + ")";
    }

    /**
     * Validates if a pattern correctly matches all positive examples
     * and excludes all negative examples.
     *
     * @param pattern The regex pattern to validate
     * @param positiveExamples List of strings that should match
     * @param negativeExamples List of strings that should not match
     * @return True if the pattern is valid, false otherwise
     */
    private boolean isPatternValid(String pattern, List<String> positiveExamples, List<String> negativeExamples) {
        try {
            // Convert pattern to regex
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);

            // Check all positive examples match
            boolean allPositiveMatch = positiveExamples.stream()
                    .allMatch(ex -> regex.matcher(ex).matches());

            // Check no negative examples match
            boolean noNegativeMatch = negativeExamples.stream()
                    .noneMatch(ex -> regex.matcher(ex).matches());

            return allPositiveMatch && noNegativeMatch;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates an appropriate character class based on a set of characters.
     * Optimises by using predefined classes like \d, [a-z], etc. when possible.
     *
     * @param chars Set of characters to generalise
     * @return A regex character class or single character if only one
     */
    private String generaliseCharacterClass(Set<Character> chars) {
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