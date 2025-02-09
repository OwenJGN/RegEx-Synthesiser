// StateEliminationAlgorithm.java
package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;

import java.util.*;
import java.util.stream.Collectors;

public class StateEliminationAlgorithm {
    public String eliminateStates(DFA dfa) {
        System.out.println("Total States: " + dfa.getNumStates());
        System.out.println("Start State: " + dfa.getStartState());
        System.out.println("Accepting States: " + dfa.getStates().stream()
                .filter(dfa::isAcceptingState)
                .collect(Collectors.toSet()));
        System.out.println("Alphabet: " + dfa.getAlphabet());

        // Identify the key characteristics of the input strings
        List<String> paths = extractGeneralizedPaths(dfa);
        System.out.println("Extracted Paths: " + paths);
        // If no paths found, return empty string
        if (paths.isEmpty()) {
            return "";
        }

        // Remove duplicates and create generalized regex
        List<String> uniquePaths = new ArrayList<>(new LinkedHashSet<>(paths));

        // If only one unique path, return it
        if (uniquePaths.size() == 1) {
            return uniquePaths.get(0);
        }

        // Create alternation of paths
        return "(" + String.join("|", uniquePaths) + ")";
    }

    private List<String> extractGeneralizedPaths(DFA dfa) {
        List<String> fullPaths = new ArrayList<>();
        Set<Integer> visitedStates = new HashSet<>();

        // Trace paths from start state
        tracePaths(dfa, dfa.getStartState(), new StringBuilder(), fullPaths, visitedStates);

        // Generalize the paths
        return generalizePaths(fullPaths);
    }

    private void tracePaths(DFA dfa, int currentState, StringBuilder currentPath,
                            List<String> fullPaths, Set<Integer> visitedStates) {
        // Prevent infinite loops
        if (visitedStates.contains(currentState)) {
            return;
        }
        visitedStates.add(currentState);

        // If this is an accepting state, add the current path
        if (dfa.isAcceptingState(currentState)) {
            fullPaths.add(currentPath.toString());
        }

        // Explore all possible transitions
        for (char symbol : dfa.getAlphabet()) {
            int nextState = dfa.getTransition(currentState, symbol);
            if (nextState != DFA.INVALID_STATE) {
                StringBuilder newPath = new StringBuilder(currentPath);
                newPath.append(symbol);

                // Recursive exploration with a copy of visited states
                Set<Integer> newVisitedStates = new HashSet<>(visitedStates);
                tracePaths(dfa, nextState, newPath, fullPaths, newVisitedStates);
            }
        }
    }

    private List<String> generalizePaths(List<String> paths) {
        if (paths.isEmpty()) {
            return paths;
        }

        // Find common patterns
        String[] commonPatternParts = findCommonPatternParts(paths);

        // If we can create a generalized pattern
        if (commonPatternParts != null) {
            return Collections.singletonList(createGeneralizedRegex(commonPatternParts));
        }

        // If no common pattern, return original paths with some generalization
        return paths.stream()
                .map(this::generalizeIndividualPath)
                .collect(Collectors.toList());
    }

    private String[] findCommonPatternParts(List<String> paths) {
        if (paths.isEmpty()) {
            return null;
        }

        // Start with first path as reference
        String firstPath = paths.get(0);

        // Check if all paths have similar structure
        boolean allPathsMatch = paths.stream()
                .allMatch(path -> path.length() == firstPath.length());

        if (!allPathsMatch) {
            return null;
        }

        // Analyze each character position
        String[] commonParts = new String[firstPath.length()];
        for (int i = 0; i < firstPath.length(); i++) {
            char firstChar = firstPath.charAt(i);
            int finalI = i;
            boolean sameCharAtPosition = paths.stream()
                    .allMatch(path -> path.charAt(finalI) == firstChar);

            if (sameCharAtPosition) {
                commonParts[i] = String.valueOf(firstChar);
            } else {
                // Determine if we can generalize this position
                commonParts[i] = generalizeCharPosition(paths, i);
            }
        }

        return commonParts;
    }

    private String generalizeCharPosition(List<String> paths, int position) {
        // Analyze the characters at this position
        Set<Character> uniqueChars = paths.stream()
                .map(path -> path.charAt(position))
                .collect(Collectors.toSet());

        // If it's a mix of letters, use a character class
        if (uniqueChars.stream().allMatch(Character::isLetter)) {
            return "[A-Za-z]";
        }

        // If it's a mix of digits, use digit class
        if (uniqueChars.stream().allMatch(Character::isDigit)) {
            return "\\d";
        }

        // If it's a mix of special characters, use a more generic pattern
        if (uniqueChars.stream().allMatch(c -> !Character.isLetterOrDigit(c))) {
            return "\\W";
        }

        // Default to a more permissive pattern
        return ".";
    }

    private String createGeneralizedRegex(String[] commonPatternParts) {
        return String.join("", commonPatternParts);
    }

    private String generalizeIndividualPath(String path) {
        // Add more sophisticated generalization logic here
        // For now, we'll do some basic generalization
        return path.replaceAll("\\d+", "\\\\d+")  // Replace consecutive digits with \d+
                .replaceAll("[A-Za-z]+", "[A-Za-z]+");  // Replace consecutive letters
    }
}