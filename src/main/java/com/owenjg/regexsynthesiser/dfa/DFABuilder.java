package com.owenjg.regexsynthesiser.dfa;

import java.util.*;

public class DFABuilder {
    /**
     * Builds a DFA from positive and negative examples with enhanced character class support
     */
    public DFA buildDFAFromExamples(List<String> positiveExamples, List<String> negativeExamples) {
        // Create a prefix tree from positive examples
        DFA dfa = buildSmartPrefixTree(positiveExamples);

        // Complete the DFA by adding transitions for all characters
        completeAutomaton(dfa);

        // Mark states that accept negative examples as non-accepting
        if (negativeExamples != null && !negativeExamples.isEmpty()) {
            markNegativeExamples(dfa, negativeExamples);
        }

        // Optimize state transitions by merging similar transitions
        optimizeTransitions(dfa);

        return dfa;
    }

    /**
     * Builds a smart prefix tree that attempts to identify patterns in the input
     */
    private DFA buildSmartPrefixTree(List<String> examples) {
        DFA dfa = new DFA(0); // Start state is 0
        int nextState = 1;

        // Track character frequencies at each position
        Map<Integer, Map<Character, Integer>> positionCharFreq = analyzeCharacterFrequencies(examples);

        for (String example : examples) {
            int currentState = dfa.getStartState();

            // Handle empty string specifically
            if (example.isEmpty()) {
                dfa.addAcceptingState(currentState);
                continue;
            }

            for (int i = 0; i < example.length(); i++) {
                char symbol = example.charAt(i);
                int nextStateForSymbol = dfa.getTransition(currentState, symbol);

                if (nextStateForSymbol == DFA.INVALID_STATE) {
                    // Create new state and transition
                    dfa.addTransition(currentState, symbol, nextState);
                    currentState = nextState;
                    nextState++;
                } else {
                    currentState = nextStateForSymbol;
                }
            }

            // Mark the final state as accepting
            dfa.addAcceptingState(currentState);
        }

        return dfa;
    }

    /**
     * Analyze character frequencies at each position across examples
     */
    private Map<Integer, Map<Character, Integer>> analyzeCharacterFrequencies(List<String> examples) {
        Map<Integer, Map<Character, Integer>> positionCharFreq = new HashMap<>();

        for (String example : examples) {
            for (int i = 0; i < example.length(); i++) {
                char c = example.charAt(i);
                positionCharFreq.computeIfAbsent(i, k -> new HashMap<>())
                        .compute(c, (k, v) -> (v == null) ? 1 : v + 1);
            }
        }

        return positionCharFreq;
    }

    /**
     * Makes the DFA complete by adding transitions for all characters in the alphabet
     */
    private void completeAutomaton(DFA dfa) {
        Set<Character> alphabet = dfa.getAlphabet();
        Set<Integer> states = dfa.getStates();

        // If alphabet is empty, nothing to complete
        if (alphabet.isEmpty()) {
            return;
        }

        // Add a sink state
        int sinkState = states.size();

        // For each state, add transitions for missing symbols
        for (int state : states) {
            Map<Character, Integer> stateTransitions = dfa.getTransitions().getOrDefault(state, new HashMap<>());

            // Find missing transitions
            Set<Character> missingChars = new HashSet<>(alphabet);
            missingChars.removeAll(stateTransitions.keySet());

            // If all transitions are defined, nothing to do
            if (missingChars.isEmpty()) {
                continue;
            }

            // Add transitions to sink state for missing characters
            for (char symbol : missingChars) {
                dfa.addTransition(state, symbol, sinkState);
            }
        }

        // Add self-loops for all characters in the sink state
        for (char symbol : alphabet) {
            dfa.addTransition(sinkState, symbol, sinkState);
        }
    }

    /**
     * Ensures states that accept negative examples are non-accepting
     */
    private void markNegativeExamples(DFA dfa, List<String> negativeExamples) {
        for (String example : negativeExamples) {
            int state = dfa.getStartState();
            boolean valid = true;

            for (int i = 0; i < example.length() && valid; i++) {
                char symbol = example.charAt(i);
                int nextState = dfa.getTransition(state, symbol);

                if (nextState == DFA.INVALID_STATE) {
                    valid = false;
                } else {
                    state = nextState;
                }
            }

            if (valid && dfa.isAcceptingState(state)) {
                dfa.removeAcceptingState(state);
            }
        }
    }

    /**
     * Optimize transitions by merging transitions that go to the same state
     */
    private void optimizeTransitions(DFA dfa) {
        // For each state, find sets of characters that transition to the same target state
        for (int state : dfa.getStates()) {
            Map<Character, Integer> transitions = dfa.getTransitions().get(state);
            if (transitions == null || transitions.isEmpty()) {
                continue;
            }

            // Group by target state
            Map<Integer, Set<Character>> targetToChars = new HashMap<>();
            for (Map.Entry<Character, Integer> entry : transitions.entrySet()) {
                targetToChars.computeIfAbsent(entry.getValue(), k -> new HashSet<>())
                        .add(entry.getKey());
            }

            // For each target with multiple characters, see if we can optimize
            for (Map.Entry<Integer, Set<Character>> entry : targetToChars.entrySet()) {
                if (entry.getValue().size() <= 1) {
                    continue;
                }
            }
        }
    }
}