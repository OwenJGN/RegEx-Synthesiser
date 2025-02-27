package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;
import java.util.*;

public class StateEliminationAlgorithm {
    private Map<StateTransition, String> regexTransitions = new HashMap<>();

    public String eliminateStates(DFA dfa) {
        // Initialize transitions
        initializeRegexTransitions(dfa);

        if (regexTransitions.isEmpty()) {
            return "";
        }

        // Get elimination order
        List<Integer> eliminationOrder = getEliminationOrder(dfa);

        // Eliminate states
        for (Integer state : eliminationOrder) {
            eliminateState(dfa, state);
        }

        // Get final regex
        return getFinalRegex(dfa);
    }

    private void initializeRegexTransitions(DFA dfa) {
        regexTransitions.clear();

        // Get all transitions from DFA
        Map<Integer, Map<Character, Integer>> dfaTransitions = dfa.getTransitions();

        // Convert each DFA transition to regex transition
        for (Map.Entry<Integer, Map<Character, Integer>> fromState : dfaTransitions.entrySet()) {
            int from = fromState.getKey();
            for (Map.Entry<Character, Integer> transition : fromState.getValue().entrySet()) {
                char symbol = transition.getKey();
                int to = transition.getValue();

                StateTransition trans = new StateTransition(from, to);
                String transStr = String.valueOf(symbol);

                // If transition already exists, merge with OR
                if (regexTransitions.containsKey(trans)) {
                    String existing = regexTransitions.get(trans);
                    regexTransitions.put(trans, "(" + existing + "|" + transStr + ")");
                } else {
                    regexTransitions.put(trans, transStr);
                }
            }
        }
    }

    private void eliminateState(DFA dfa, int state) {
        // Create maps for incoming and outgoing transitions
        Map<Integer, String> incomingTransitions = new HashMap<>();
        Map<Integer, String> outgoingTransitions = new HashMap<>();
        String selfLoop = null;

        // Collect transitions
        for (Map.Entry<StateTransition, String> entry : new HashMap<>(regexTransitions).entrySet()) {
            StateTransition trans = entry.getKey();
            String regex = entry.getValue();

            if (trans.from == state && trans.to == state) {
                selfLoop = regex;
            } else if (trans.from == state) {
                outgoingTransitions.put(trans.to, regex);
            } else if (trans.to == state) {
                incomingTransitions.put(trans.from, regex);
            }
        }

        // Remove transitions involving this state
        regexTransitions.entrySet().removeIf(entry ->
                entry.getKey().from == state || entry.getKey().to == state);

        // Create new transitions
        for (Map.Entry<Integer, String> incoming : incomingTransitions.entrySet()) {
            for (Map.Entry<Integer, String> outgoing : outgoingTransitions.entrySet()) {
                int from = incoming.getKey();
                int to = outgoing.getKey();

                String newRegex = incoming.getValue();
                if (selfLoop != null) {
                    newRegex += "(" + selfLoop + ")*";
                }
                newRegex += outgoing.getValue();

                StateTransition newTrans = new StateTransition(from, to);

                // Merge or add new transition
                if (regexTransitions.containsKey(newTrans)) {
                    String existing = regexTransitions.get(newTrans);
                    regexTransitions.put(newTrans, "(" + existing + "|" + newRegex + ")");
                } else {
                    regexTransitions.put(newTrans, newRegex);
                }
            }
        }

        // Preserve accepting state transitions
        if (dfa.isAcceptingState(state)) {
            for (Map.Entry<Integer, String> incoming : incomingTransitions.entrySet()) {
                StateTransition acceptingTrans = new StateTransition(incoming.getKey(), state);
                String incomingRegex = incoming.getValue();
                if (selfLoop != null) {
                    incomingRegex += "(" + selfLoop + ")*";
                }
                regexTransitions.put(acceptingTrans, incomingRegex);
            }
        }
    }

    private String getFinalRegex(DFA dfa) {
        List<String> patterns = new ArrayList<>();
        int startState = dfa.getStartState();

        // Collect all patterns from start state to accepting states
        for (Map.Entry<StateTransition, String> entry : regexTransitions.entrySet()) {
            StateTransition trans = entry.getKey();
            if (trans.from == startState && dfa.isAcceptingState(trans.to)) {
                patterns.add(entry.getValue());
            }
        }

        // Handle case where start state is accepting (empty string)
        if (dfa.isAcceptingState(startState)) {
            patterns.add("ε"); // Empty string representation
        }

        if (patterns.isEmpty()) {
            return "";
        }

        // Sort patterns for consistency
        Collections.sort(patterns);

        // Join patterns with OR
        String regex = String.join("|", patterns);
        // Replace ε with empty string after combination
        regex = regex.replace("ε", "");
        if (regex.equals("|")) {
            regex = "";
        } else if (regex.startsWith("|")) {
            regex = regex.substring(1);
        } else if (regex.endsWith("|")) {
            regex = regex.substring(0, regex.length() - 1);
        }

        return patterns.size() > 1 ? "(" + regex + ")" : regex;
    }

    private List<Integer> getEliminationOrder(DFA dfa) {
        List<Integer> order = new ArrayList<>();
        Set<Integer> states = dfa.getStates();
        int startState = dfa.getStartState();

        // Add non-accepting, non-start states first
        for (Integer state : states) {
            if (state != startState && !dfa.isAcceptingState(state)) {
                order.add(state);
            }
        }

        // Then add accepting states (except if start state)
        for (Integer state : states) {
            if (state != startState && dfa.isAcceptingState(state)) {
                order.add(state);
            }
        }

        return order;
    }

    private static class StateTransition {
        final int from;
        final int to;

        StateTransition(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StateTransition that = (StateTransition) o;
            return from == that.from && to == that.to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }

        @Override
        public String toString() {
            return from + "->" + to;
        }
    }
}