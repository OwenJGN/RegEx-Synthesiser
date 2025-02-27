package com.owenjg.regexsynthesiser.dfa;

import java.util.*;

public class DFABuilder {
    /**
     * Builds a DFA from positive and negative examples
     */
    public DFA buildDFAFromExamples(List<String> positiveExamples, List<String> negativeExamples) {
        // Create a prefix tree from positive examples
        DFA dfa = buildPrefixTree(positiveExamples);

        // Complete the DFA by adding transitions for all characters
        completeAutomaton(dfa);

        // Mark states that accept negative examples as non-accepting
        if (negativeExamples != null && !negativeExamples.isEmpty()) {
            markNegativeExamples(dfa, negativeExamples);
        }

        return dfa;
    }

    /**
     * Builds a prefix tree (trie) DFA from a list of strings
     */
    public DFA buildPrefixTree(List<String> examples) {
        DFA dfa = new DFA(0); // Start state is 0
        int nextState = 1;

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

        // Add transitions for all characters in all states
        for (int state : states) {
            for (char symbol : alphabet) {
                if (dfa.getTransition(state, symbol) == DFA.INVALID_STATE) {
                    dfa.addTransition(state, symbol, sinkState);
                }
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
}