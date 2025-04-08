package com.owenjg.regexsynthesiser.dfa;

import java.util.*;

/**
 * Represents a Deterministic Finite Automaton (DFA) for regex pattern matching.
 * This class provides the core data structure and operations for working with DFAs
 * in the context of regular expression synthesis.
 */
public class DFA {
    /**
     * Constant representing an invalid or non-existent state in the DFA.
     */
    public static final int INVALID_STATE = -1;

    /**
     * The initial state of the DFA where processing begins.
     */
    private int startState;

    /**
     * Set of states that represent successful pattern matching.
     */
    private Set<Integer> acceptingStates;

    /**
     * Maps states to their transitions, with characters as keys and destination states as values.
     * This structure represents the state transition function of the DFA.
     */
    private Map<Integer, Map<Character, Integer>> transitions;

    /**
     * Constructs a new DFA with the specified start state.
     *
     * @param startState The initial state of the DFA
     */
    public DFA(int startState) {
        this.startState = startState;
        this.acceptingStates = new HashSet<>();
        this.transitions = new HashMap<>();
    }

    /**
     * Adds a transition from one state to another on a specific input symbol.
     *
     * @param fromState The source state
     * @param symbol The input character triggering the transition
     * @param toState The destination state
     */
    public void addTransition(int fromState, char symbol, int toState) {
        transitions.computeIfAbsent(fromState, k -> new HashMap<>()).put(symbol, toState);
    }

    /**
     * Retrieves the next state based on the current state and input symbol.
     *
     * @param state The current state
     * @param symbol The input character
     * @return The next state, or INVALID_STATE if no transition exists
     */
    public int getTransition(int state, char symbol) {
        return transitions.getOrDefault(state, Collections.emptyMap()).getOrDefault(symbol, INVALID_STATE);
    }

    /**
     * Designates a state as accepting (a final state).
     *
     * @param state The state to mark as accepting
     */
    public void addAcceptingState(int state) {
        acceptingStates.add(state);
    }

    /**
     * Removes a state from the set of accepting states.
     *
     * @param state The state to no longer consider as accepting
     */
    public void removeAcceptingState(int state) {
        acceptingStates.remove(state);
    }

    /**
     * Checks if a state is an accepting state.
     *
     * @param state The state to check
     * @return true if the state is accepting, false otherwise
     */
    public boolean isAcceptingState(int state) {
        return acceptingStates.contains(state);
    }

    /**
     * Gets the starting state of the DFA.
     *
     * @return The start state
     */
    public int getStartState() {
        return startState;
    }

    /**
     * Calculates the total number of states in the DFA.
     * This includes the start state, accepting states, and any states
     * that are part of transitions.
     *
     * @return The total number of states
     */
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

    /**
     * Gets the set of all input symbols used in the DFA transitions.
     *
     * @return A set containing all the characters used in transitions
     */
    public Set<Character> getAlphabet() {
        Set<Character> alphabet = new HashSet<>();
        for (Map<Character, Integer> transitionMap : transitions.values()) {
            alphabet.addAll(transitionMap.keySet());
        }
        return alphabet;
    }

    /**
     * Sets the starting state of the DFA.
     *
     * @param state The new start state
     */
    public void setStartState(int state) {
        this.startState = state;
    }

    /**
     * Gets the complete set of all states in the DFA.
     * This includes the start state, accepting states, and any states
     * that are part of transitions.
     *
     * @return A set containing all states in the DFA
     */
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

    /**
     * Gets all transitions in the DFA.
     *
     * @return A map representing the transition function of the DFA
     */
    public Map<Integer, Map<Character, Integer>> getTransitions() {
        return transitions;
    }
}