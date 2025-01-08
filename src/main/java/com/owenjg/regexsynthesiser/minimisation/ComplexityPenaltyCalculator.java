package com.owenjg.regexsynthesiser.minimisation;

import com.owenjg.regexsynthesiser.dfa.DFA;

public class ComplexityPenaltyCalculator {
    private static final double STATE_PENALTY = 1.0;
    private static final double TRANSITION_PENALTY = 0.5;

    public double calculatePenalty(DFA dfa) {
        int stateCount = dfa.getStates().size();
        int transitionCount = dfa.getTransitions().size();

        return (stateCount * STATE_PENALTY) + (transitionCount * TRANSITION_PENALTY);
    }
}
