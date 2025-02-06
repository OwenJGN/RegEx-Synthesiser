package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;
import com.owenjg.regexsynthesiser.dfa.DFAState;
import com.owenjg.regexsynthesiser.dfa.DFATransition;

import java.util.*;
import java.util.stream.Collectors;

public class StateEliminationAlgorithm {
    private Map<DFATransition, String> regexLabels = new HashMap<>();

    public String convertToRegex(DFA dfa) {
        System.out.println("\nStarting state elimination...");
        System.out.println("Initial states: " + dfa.getStates().size());
        System.out.println("Initial transitions: " + dfa.getTransitions().size());

        DFA workingDfa = dfa.copy();
        initializeRegexLabels(workingDfa);

        while (workingDfa.getStates().size() > 2) {
            DFAState stateToEliminate = chooseStateForElimination(workingDfa);
            if (stateToEliminate == null) {
                System.out.println("No more states to eliminate");
                break;
            }

            System.out.println("Eliminating state: " + stateToEliminate.getId());
            eliminateState(workingDfa, stateToEliminate);
            System.out.println("Remaining states: " + workingDfa.getStates().size());
            System.out.println("Remaining transitions: " + workingDfa.getTransitions().size());
        }

        String regex = extractFinalRegex(workingDfa);
        System.out.println("Final regex: " + regex);
        return regex;
    }

    private void initializeRegexLabels(DFA dfa) {
        regexLabels.clear();
        for (DFATransition transition : dfa.getTransitions()) {
            regexLabels.put(transition, String.valueOf(transition.getSymbol()));
        }
    }

    // In StateEliminationAlgorithm.java
    private DFAState chooseStateForElimination(DFA dfa) {
        DFAState startState = dfa.getStartState();
        DFAState bestState = null;
        int minTransitions = Integer.MAX_VALUE;

        // First, identify accepting states
        Set<DFAState> acceptingStates = dfa.getStates().stream()
                .filter(DFAState::isAccepting)
                .collect(Collectors.toSet());

        // Don't eliminate if we only have start and accepting states left
        if (dfa.getStates().size() <= acceptingStates.size() + 1) {
            return null;
        }

        for (DFAState state : dfa.getStates()) {
            // Skip start state and accepting states
            if (!state.equals(startState) && !state.isAccepting()) {
                int inCount = dfa.getTransitionsTo(state).size();
                int outCount = dfa.getTransitionsFrom(state).size();

                // Skip states with no incoming or outgoing transitions
                if (inCount == 0 || outCount == 0) continue;

                int totalTransitions = inCount * outCount;
                if (totalTransitions < minTransitions) {
                    minTransitions = totalTransitions;
                    bestState = state;
                }
            }
        }
        return bestState;
    }

    private String extractFinalRegex(DFA dfa) {
        DFAState startState = dfa.getStartState();
        List<String> patterns = new ArrayList<>();

        // Handle case where start state is accepting
        boolean startIsAccepting = startState.isAccepting();

        // Collect all paths from start state to accepting states
        for (DFATransition transition : dfa.getTransitions()) {
            if (transition.getSource().equals(startState)) {
                String pattern = regexLabels.get(transition);
                if (pattern != null && !pattern.isEmpty()) {
                    if (transition.getDestination().isAccepting()) {
                        patterns.add(pattern);
                    }
                }
            }
        }

        if (patterns.isEmpty()) {
            if (startIsAccepting) {
                return "ε";  // Empty string
            }
            // If no patterns but we have transitions, something went wrong
            if (!dfa.getTransitions().isEmpty()) {
                return ".+"; // Default to any non-empty string
            }
            return "";
        }

        return patterns.size() == 1 ? patterns.get(0) : "(" + String.join("|", patterns) + ")";
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


}