package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;

import com.owenjg.regexsynthesiser.dfa.DFA;

import java.util.List;
import java.util.stream.Collectors;

public class PrefixTreeBuilder {
    public DFA buildPrefixTree(List<String> examples) {
        DFA pta = new DFA(0);
        int stateCounter = 1;

        for (String example : examples) {
            int currentState = pta.getStartState();

            for (int i = 0; i < example.length(); i++) {
                char c = example.charAt(i);
                int nextState = pta.getTransition(currentState, c);

                if (nextState == DFA.INVALID_STATE) {
                    nextState = stateCounter++;
                    pta.addTransition(currentState, c, nextState);
                }

                currentState = nextState;
            }

            // Mark the final state of each example as accepting
            pta.addAcceptingState(currentState);
        }

        return pta;
    }
}
