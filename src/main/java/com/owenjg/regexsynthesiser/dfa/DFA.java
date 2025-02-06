package com.owenjg.regexsynthesiser.dfa;

import java.util.*;
import java.util.stream.Collectors;

public class DFA {
    private static DFAState startState;
    private Set<DFAState> states;
    private static Set<DFATransition> transitions;

    public DFA() {
        this.states = new HashSet<>();
        this.transitions = new HashSet<>();
    }

    public DFA copy() {
        DFA newDfa = new DFA();

        // Create map to track state correspondence
        Map<DFAState, DFAState> stateMap = new HashMap<>();

        // Copy all states
        for (DFAState oldState : states) {
            DFAState newState = new DFAState(oldState.getId());
            newState.setAccepting(oldState.isAccepting());
            stateMap.put(oldState, newState);
            newDfa.addState(newState);

            // Set start state if this is the start state
            if (oldState.equals(startState)) {
                newDfa.setStartState(newState);
            }
        }

        // Copy all transitions using the state map
        for (DFATransition oldTransition : transitions) {
            DFAState newSource = stateMap.get(oldTransition.getSource());
            DFAState newDestination = stateMap.get(oldTransition.getDestination());
            DFATransition newTransition = new DFATransition(newSource, newDestination, oldTransition.getSymbol());
            newDfa.addTransition(newTransition);
        }

        return newDfa;
    }

    public boolean accepts(String input) {
        if (startState == null) {
            return false;
        }

        DFAState currentState = startState;

        // Process each character in the input string
        for (char symbol : input.toCharArray()) {
            DFAState nextState = getNextState(currentState, symbol);

            // If no valid transition exists, reject
            if (nextState == null) {
                return false;
            }

            currentState = nextState;
        }

        // Accept only if final state is accepting
        return currentState.isAccepting();
    }

    // Helper methods that should be in your DFA class

    public void removeState(DFAState state) {
        states.remove(state);
        transitions.removeIf(t ->
                t.getSource().equals(state) || t.getDestination().equals(state));
    }

    public Set<DFATransition> getTransitionsTo(DFAState state) {
        return transitions.stream()
                .filter(t -> t.getDestination().equals(state))
                .collect(Collectors.toSet());
    }

    public static Set<DFATransition> getTransitionsFrom(DFAState state) {
        return transitions.stream()
                .filter(t -> t.getSource().equals(state))
                .collect(Collectors.toSet());
    }

    private DFAState getNextState(DFAState currentState, char symbol) {
        return transitions.stream()
                .filter(t -> t.getSource().equals(currentState) &&
                        t.getSymbol() == symbol)
                .map(DFATransition::getDestination)
                .findFirst()
                .orElse(null);
    }

    // Getters and basic mutation methods

    public void setStartState(DFAState state) {
        this.startState = state;
        this.states.add(state);
    }

    public static DFAState getStartState() {
        return startState;
    }

    public void addState(DFAState state) {
        states.add(state);
    }

    public void addTransition(DFATransition transition) {
        transitions.add(transition);
    }

    public Set<DFAState> getStates() {
        return Collections.unmodifiableSet(states);
    }

    public Set<DFATransition> getTransitions() {
        return Collections.unmodifiableSet(transitions);
    }
}
