package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;
import com.owenjg.regexsynthesiser.dfa.DFAState;
import com.owenjg.regexsynthesiser.dfa.DFATransition;

import java.util.List;

public class PrefixTreeBuilder {
    private DFA dfa;
    private int stateCounter;

    public PrefixTreeBuilder() {
        this.dfa = new DFA();
        this.stateCounter = 0;
    }

    public DFA buildPrefixTree(List<String> positiveExamples) {
        // Initialize DFA with start state
        DFAState startState = new DFAState(stateCounter++);
        dfa.setStartState(startState);

        for (String example : positiveExamples) {
            addStringToTree(example);
        }

        return dfa;
    }

    private void addStringToTree(String example) {
        DFAState currentState = dfa.getStartState();

        // Process each character in the example string
        for (int i = 0; i < example.length(); i++) {
            char symbol = example.charAt(i);
            DFAState nextState = findNextState(currentState, symbol);

            if (nextState == null) {
                // Create new state and transition if it doesn't exist
                nextState = new DFAState(stateCounter++);
                DFATransition transition = new DFATransition(currentState, nextState, symbol);
                dfa.addState(nextState);
                dfa.addTransition(transition);
            }

            currentState = nextState;
        }

        // Mark the final state as accepting
        currentState.setAccepting(true);
    }

    private DFAState findNextState(DFAState current, char symbol) {
        // Find existing transition for the current symbol
        for (DFATransition transition : dfa.getTransitionsFrom(current)) {
            if (transition.getSymbol() == symbol) {
                return transition.getDestination();
            }
        }
        return null;
    }

    public void reset() {
        this.dfa = new DFA();
        this.stateCounter = 0;
    }
}
