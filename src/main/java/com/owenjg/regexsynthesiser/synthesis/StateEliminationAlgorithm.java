package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;
import com.owenjg.regexsynthesiser.dfa.DFAState;
import com.owenjg.regexsynthesiser.dfa.DFATransition;

import java.util.*;

public class StateEliminationAlgorithm {
    private Map<DFATransition, String> regexLabels = new HashMap<>();

    public String convertToRegex(DFA dfa) {
        // Copy DFA for modification
        DFA workingDfa = dfa.copy();
        initializeRegexLabels(workingDfa);

        // Keep track of states to avoid infinite loop
        int previousSize = workingDfa.getStates().size();

        // First, eliminate all non-accepting, non-start states
        while (workingDfa.getStates().size() > 2) {
            DFAState stateToEliminate = chooseStateForElimination(workingDfa);
            if (stateToEliminate == null) {
                break; // No more states can be eliminated
            }
            eliminateState(workingDfa, stateToEliminate);

            // Check if we're actually reducing states
            if (workingDfa.getStates().size() >= previousSize) {
                break; // Prevent infinite loop
            }
            previousSize = workingDfa.getStates().size();
        }

        return extractFinalRegex(workingDfa);
    }

    private void initializeRegexLabels(DFA dfa) {
        regexLabels.clear();
        for (DFATransition transition : dfa.getTransitions()) {
            regexLabels.put(transition, String.valueOf(transition.getSymbol()));
        }
    }

    private DFAState chooseStateForElimination(DFA dfa) {
        // Choose state with fewest combined transitions to minimize regex complexity
        DFAState startState = dfa.getStartState();
        DFAState bestState = null;
        int minTransitions = Integer.MAX_VALUE;

        for (DFAState state : dfa.getStates()) {
            if (!state.equals(startState) && !state.isAccepting()) {
                int inCount = dfa.getTransitionsTo(state).size();
                int outCount = dfa.getTransitionsFrom(state).size();
                int totalTransitions = inCount * outCount;

                if (totalTransitions < minTransitions) {
                    minTransitions = totalTransitions;
                    bestState = state;
                }
            }
        }
        return bestState;
    }

    private void eliminateState(DFA dfa, DFAState state) {
        Set<DFATransition> incomingTransitions = dfa.getTransitionsTo(state);
        Set<DFATransition> outgoingTransitions = dfa.getTransitionsFrom(state);

        // Handle self-loops
        StringBuilder selfLoopRegex = new StringBuilder();
        Iterator<DFATransition> outgoingIter = outgoingTransitions.iterator();
        while (outgoingIter.hasNext()) {
            DFATransition t = outgoingIter.next();
            if (t.getDestination().equals(state)) {
                if (selfLoopRegex.length() > 0) {
                    selfLoopRegex.append("|");
                }
                selfLoopRegex.append(regexLabels.get(t));
            }
        }

        // If there's a self-loop, create the Kleene star expression
        String selfLoopPart = selfLoopRegex.length() > 0 ?
                "(" + selfLoopRegex.toString() + ")*" : "";

        // Create new transitions between incoming and outgoing states
        for (DFATransition inTrans : incomingTransitions) {
            if (inTrans.getSource().equals(state)) continue; // Skip self-loops

            for (DFATransition outTrans : outgoingTransitions) {
                if (outTrans.getDestination().equals(state)) continue; // Skip self-loops

                // Build the regex for this path
                String newRegex = concatenateRegex(
                        regexLabels.get(inTrans),
                        selfLoopPart,
                        regexLabels.get(outTrans)
                );

                // Check if a transition already exists between these states
                boolean transitionExists = false;
                DFATransition existingTransition = null;

                for (DFATransition existing : dfa.getTransitions()) {
                    if (existing.getSource().equals(inTrans.getSource()) &&
                            existing.getDestination().equals(outTrans.getDestination())) {
                        transitionExists = true;
                        existingTransition = existing;
                        break;
                    }
                }

                if (transitionExists) {
                    // Combine with OR
                    String existingRegex = regexLabels.get(existingTransition);
                    regexLabels.put(existingTransition,
                            "(" + existingRegex + "|" + newRegex + ")");
                } else {
                    // Create new transition with the first symbol as placeholder
                    DFATransition newTransition = new DFATransition(
                            inTrans.getSource(),
                            outTrans.getDestination(),
                            outTrans.getSymbol()
                    );
                    dfa.addTransition(newTransition);
                    regexLabels.put(newTransition, newRegex);
                }
            }
        }

        // Remove the eliminated state and its transitions
        dfa.removeState(state);
    }

    private String concatenateRegex(String... parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                if (result.length() > 0 && !isOperator(part.charAt(0))) {
                    result.append("");  // No explicit concatenation operator needed
                }
                result.append(part);
            }
        }
        return result.toString();
    }

    private boolean isOperator(char c) {
        return c == '*' || c == '+' || c == '?' || c == '|' || c == ')';
    }

    private String extractFinalRegex(DFA dfa) {
        DFAState startState = dfa.getStartState();
        List<String> patterns = new ArrayList<>();

        // Collect all paths from start state to accepting states
        for (DFATransition transition : dfa.getTransitions()) {
            if (transition.getSource().equals(startState) &&
                    transition.getDestination().isAccepting()) {
                String pattern = regexLabels.get(transition);
                if (pattern != null && !pattern.isEmpty()) {
                    patterns.add(pattern);
                }
            }
        }

        if (patterns.isEmpty()) {
            // Handle the case where start state is accepting
            if (startState.isAccepting()) {
                return "ε";  // Empty string
            }
            return "";  // No accepting paths
        } else if (patterns.size() == 1) {
            return patterns.get(0);
        } else {
            return "(" + String.join("|", patterns) + ")";
        }
    }
}