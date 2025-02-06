package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;
import com.owenjg.regexsynthesiser.dfa.DFAState;
import com.owenjg.regexsynthesiser.dfa.DFATransition;
import javafx.util.Pair;

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
        Set<String> patterns = new HashSet<>(); // Use Set to avoid duplicates

        // Handle paths from start to accepting states
        for (DFATransition transition : dfa.getTransitions()) {
            if (transition.getSource().equals(startState)) {
                String pattern = regexLabels.get(transition);
                if (pattern != null && !pattern.isEmpty() &&
                        transition.getDestination().isAccepting()) {
                    patterns.add(pattern);
                }
            }
        }

        if (patterns.isEmpty()) {
            return startState.isAccepting() ? "ϵ" : "";
        }

        List<String> sortedPatterns = new ArrayList<>(patterns);
        Collections.sort(sortedPatterns); // Sort for consistent output
        return sortedPatterns.size() == 1 ? sortedPatterns.get(0) :
                "(" + String.join("|", sortedPatterns) + ")";
    }

    private void eliminateState(DFA dfa, DFAState state) {
        Set<DFATransition> incomingTransitions = dfa.getTransitionsTo(state);
        Set<DFATransition> outgoingTransitions = dfa.getTransitionsFrom(state);
        Map<Pair<DFAState, DFAState>, String> newTransitions = new HashMap<>();

        // Handle self-loops
        String selfLoopRegex = getSelfLoopRegex(state, outgoingTransitions);
        String selfLoopPart = !selfLoopRegex.isEmpty() ? "(" + selfLoopRegex + ")*" : "";

        // Create new transitions
        for (DFATransition inTrans : incomingTransitions) {
            if (inTrans.getSource().equals(state)) continue;

            for (DFATransition outTrans : outgoingTransitions) {
                if (outTrans.getDestination().equals(state)) continue;

                String newRegex = concatenateRegex(
                        regexLabels.get(inTrans),
                        selfLoopPart,
                        regexLabels.get(outTrans)
                );

                Pair<DFAState, DFAState> statePair = new Pair<>(
                        inTrans.getSource(), outTrans.getDestination());

                String existingRegex = newTransitions.get(statePair);
                if (existingRegex != null) {
                    newRegex = "(" + existingRegex + "|" + newRegex + ")";
                }
                newTransitions.put(statePair, newRegex);
            }
        }

        // Remove old state and transitions
        dfa.removeState(state);

        // Add new transitions
        for (Map.Entry<Pair<DFAState, DFAState>, String> entry : newTransitions.entrySet()) {
            DFATransition newTrans = new DFATransition(
                    entry.getKey().first, entry.getKey().second, 'ε');
            dfa.addTransition(newTrans);
            regexLabels.put(newTrans, entry.getValue());
        }
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

    private String getSelfLoopRegex(DFAState state, Set<DFATransition> outgoing) {
        return outgoing.stream()
                .filter(t -> t.getDestination().equals(state))
                .map(t -> regexLabels.get(t))
                .collect(Collectors.joining("|"));
    }

    private static class Pair<T, U> {
        final T first;
        final U second;

        Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pair)) return false;
            Pair<?, ?> p = (Pair<?, ?>) o;
            return Objects.equals(first, p.first) && Objects.equals(second, p.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }

}