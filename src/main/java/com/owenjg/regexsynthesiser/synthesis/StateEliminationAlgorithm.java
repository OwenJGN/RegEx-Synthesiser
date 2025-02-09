// StateEliminationAlgorithm.java
package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;

import java.util.*;
import java.util.stream.Collectors;

public class StateEliminationAlgorithm {

    public String eliminateStates(DFA dfa) {
        // Collect full paths to accepting states
        List<String> fullPaths = new ArrayList<>();

        // Comprehensive path tracing
        traceFullPaths(dfa, dfa.getStartState(), "", fullPaths);

        // Debug: print extracted paths
        System.out.println("Extracted Paths: " + fullPaths);

        // If no paths found, return empty string
        if (fullPaths.isEmpty()) {
            return "";
        }

        // Remove duplicates while preserving order
        List<String> uniquePaths = new ArrayList<>(new LinkedHashSet<>(fullPaths));

        // If only one unique path, return it
        if (uniquePaths.size() == 1) {
            return uniquePaths.get(0);
        }

        // Create alternation of paths
        return "(" + String.join("|", uniquePaths) + ")";
    }

    private void traceFullPaths(DFA dfa, int currentState, String currentPath,
                                List<String> fullPaths) {
        // If this is an accepting state, add the current path
        if (dfa.isAcceptingState(currentState)) {
            fullPaths.add(currentPath);
            return;
        }

        // Track to prevent infinite recursion
        Set<Integer> visitedStates = new HashSet<>();
        tracePaths(dfa, currentState, currentPath, fullPaths, visitedStates);
    }

    private void tracePaths(DFA dfa, int currentState, String currentPath,
                            List<String> fullPaths, Set<Integer> visitedStates) {
        // Prevent infinite loops
        if (visitedStates.contains(currentState)) {
            return;
        }
        visitedStates.add(currentState);

        // Explore all possible transitions
        for (char symbol : dfa.getAlphabet()) {
            int nextState = dfa.getTransition(currentState, symbol);
            if (nextState != DFA.INVALID_STATE) {
                // Create a new set of visited states for each branch
                Set<Integer> newVisitedStates = new HashSet<>(visitedStates);

                String newPath = currentPath + symbol;

                // If next state is an accepting state, add the path
                if (dfa.isAcceptingState(nextState)) {
                    fullPaths.add(newPath);
                }

                // Continue tracing
                tracePaths(dfa, nextState, newPath, fullPaths, newVisitedStates);
            }
        }
    }


    private List<String> cleanAndFilterPaths(List<String> paths) {
        // Filter and clean paths to capture the structural pattern
        return paths.stream()
                .filter(path -> {
                    // Ensure path has key structural elements
                    return path.matches("^[A-Z][a-z]+#\\d+@[a-z]+\\.[a-z]+$");
                })
                .collect(Collectors.toList());
    }

    private Map<Integer, String> initializeStateRegexMap(DFA dfa) {
        Map<Integer, String> stateRegexMap = new HashMap<>();

        for (int state = 0; state < dfa.getNumStates(); state++) {
            stateRegexMap.put(state, "");
        }

        return stateRegexMap;
    }

    private Set<Integer> getStatesToEliminate(DFA dfa) {
        Set<Integer> statesToEliminate = new HashSet<>();

        for (int state = 0; state < dfa.getNumStates(); state++) {
            if (state != dfa.getStartState() && !dfa.isAcceptingState(state)) {
                statesToEliminate.add(state);
            }
        }

        return statesToEliminate;
    }

    private void eliminateState(DFA dfa, int state, Map<Integer, String> stateRegexMap) {
        String selfLoopRegex = getSelfLoopRegex(dfa, state);
        System.out.println("Initial DFA States: " + dfa.getStates());
        System.out.println("Alphabet: " + dfa.getAlphabet());
        System.out.println("Accepting States: " +
                dfa.getStates().stream()
                        .filter(dfa::isAcceptingState)
                        .collect(Collectors.toSet())
        );

        for (int fromState = 0; fromState < dfa.getNumStates(); fromState++) {
            if (fromState == state) {
                continue;
            }

            String transitionRegex = getTransitionRegex(dfa, fromState, state);

            if (!transitionRegex.isEmpty()) {
                for (int toState = 0; toState < dfa.getNumStates(); toState++) {
                    if (toState == state) {
                        continue;
                    }

                    String outgoingRegex = getTransitionRegex(dfa, state, toState);

                    if (!outgoingRegex.isEmpty()) {
                        String newRegex = transitionRegex + selfLoopRegex + outgoingRegex;
                        updateStateRegexMap(stateRegexMap, fromState, toState, newRegex);
                    }
                }
            }
        }
    }

    private String getSelfLoopRegex(DFA dfa, int state) {
        StringBuilder sb = new StringBuilder();

        for (char symbol : dfa.getAlphabet()) {
            if (dfa.getTransition(state, symbol) == state) {
                sb.append(symbol);
            }
        }

        String selfLoopSymbols = sb.toString();

        if (selfLoopSymbols.isEmpty()) {
            return "";
        } else if (selfLoopSymbols.length() == 1) {
            return selfLoopSymbols;
        } else {
            return "(" + selfLoopSymbols + ")*";
        }
    }

    private String getTransitionRegex(DFA dfa, int fromState, int toState) {
        StringBuilder sb = new StringBuilder();
        boolean foundTransition = false;

        for (char symbol : dfa.getAlphabet()) {
            if (dfa.getTransition(fromState, symbol) == toState) {
                sb.append(symbol);
                foundTransition = true;
            }
        }

        // If multiple transitions, wrap in character class or group
        if (sb.length() > 1) {
            return "[" + sb.toString() + "]";
        }

        return foundTransition ? sb.toString() : "";
    }

    private void updateStateRegexMap(Map<Integer, String> stateRegexMap, int fromState, int toState, String newRegex) {
        String currentRegex = stateRegexMap.get(fromState);

        // Preserve full sequences, avoid breaking into tiny fragments
        if (currentRegex.isEmpty()) {
            stateRegexMap.put(fromState, newRegex);
        } else {
            // Ensure meaningful alternation
            stateRegexMap.put(fromState,
                    newRegex.length() > currentRegex.length() ?
                            newRegex :
                            "(" + currentRegex + "|" + newRegex + ")"
            );
        }
    }
}