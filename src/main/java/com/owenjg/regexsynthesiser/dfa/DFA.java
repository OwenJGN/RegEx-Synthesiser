package com.owenjg.regexsynthesiser.dfa;

import java.util.*;

public class DFA {
    public static final int INVALID_STATE = -1;

    private int startState;
    private Set<Integer> acceptingStates;
    private Map<Integer, Map<Character, Integer>> transitions;

    public DFA(int startState) {
        this.startState = startState;
        this.acceptingStates = new HashSet<>();
        this.transitions = new HashMap<>();
    }

    public void addTransition(int fromState, char symbol, int toState) {
        transitions.computeIfAbsent(fromState, k -> new HashMap<>()).put(symbol, toState);
    }

    public int getTransition(int state, char symbol) {
        return transitions.getOrDefault(state, Collections.emptyMap()).getOrDefault(symbol, INVALID_STATE);
    }

    public void addAcceptingState(int state) {
        acceptingStates.add(state);
    }

    public void removeAcceptingState(int state) {
        acceptingStates.remove(state);
    }

    public boolean isAcceptingState(int state) {
        return acceptingStates.contains(state);
    }

    public int getStartState() {
        return startState;
    }

    public int getNumStates() {
        Set<Integer> states = new HashSet<>();
        states.add(startState);
        states.addAll(acceptingStates);
        states.addAll(transitions.keySet());
        for (Map<Character, Integer> transitionMap : transitions.values()) {
            states.addAll(transitionMap.values());
        }
        return states.size();
    }

    public Set<Character> getAlphabet() {
        Set<Character> alphabet = new HashSet<>();
        for (Map<Character, Integer> transitionMap : transitions.values()) {
            alphabet.addAll(transitionMap.keySet());
        }
        return alphabet;
    }
    public void setStartState(int state) {
        this.startState = state;
    }
    public Set<Integer> getStates() {
        Set<Integer> states = new HashSet<>();
        states.add(startState);
        states.addAll(acceptingStates);
        states.addAll(transitions.keySet());
        for (Map<Character, Integer> transitionMap : transitions.values()) {
            states.addAll(transitionMap.values());
        }
        return states;
    }

    public Map<Integer, Map<Character, Integer>> getTransitions() {
        return transitions;
    }

}