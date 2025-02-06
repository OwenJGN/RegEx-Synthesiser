package com.owenjg.regexsynthesiser.minimisation;

import com.owenjg.regexsynthesiser.dfa.DFA;
import com.owenjg.regexsynthesiser.dfa.DFAState;
import com.owenjg.regexsynthesiser.dfa.DFATransition;

import java.util.*;
import java.util.stream.Collectors;

public class DFAMinimiser {
    private UnionFind unionFind;
    private ComplexityPenaltyCalculator penaltyCalculator;

    public DFAMinimiser() {
        this.unionFind = new UnionFind();
        this.penaltyCalculator = new ComplexityPenaltyCalculator();
    }

    public DFA minimise(DFA dfa, List<String> negativeExamples) {
        DFA currentDfa = dfa.copy();
        boolean changed;

        do {
            changed = false;
            List<StatePair> pairs = findMergeablePairs(currentDfa);

            for (StatePair pair : pairs) {
                DFA mergedDfa = simulateMerge(currentDfa, pair.state1, pair.state2);

                if (mergedDfa != null && isValidMerge(mergedDfa, negativeExamples)) {
                    currentDfa = mergedDfa;
                    changed = true;
                    break;
                }
            }
        } while (changed);

        return currentDfa;
    }

    private DFA simulateMerge(DFA original, DFAState state1, DFAState state2) {
        System.out.println("Attempting to merge states: " + state1.getId() + " and " + state2.getId());

        DFA mergedDfa = new DFA();
        Map<DFAState, DFAState> stateMap = new HashMap<>();

        // Create merged state
        DFAState mergedState = new DFAState(Math.min(state1.getId(), state2.getId()));
        mergedState.setAccepting(state1.isAccepting() || state2.isAccepting());
        stateMap.put(state1, mergedState);
        stateMap.put(state2, mergedState);
        mergedDfa.addState(mergedState);

        // Copy other states
        for (DFAState oldState : original.getStates()) {
            if (!oldState.equals(state1) && !oldState.equals(state2)) {
                DFAState newState = new DFAState(oldState.getId());
                newState.setAccepting(oldState.isAccepting());
                stateMap.put(oldState, newState);
                mergedDfa.addState(newState);
            }
        }

        // Set start state
        DFAState oldStart = original.getStartState();
        DFAState newStart = stateMap.get(oldStart);
        if (newStart != null) {
            mergedDfa.setStartState(newStart);
            System.out.println("Set start state: " + newStart.getId());
        }

        // Copy transitions
        System.out.println("Original transitions:");
        for (DFATransition t : original.getTransitions()) {
            System.out.println(t.getSource().getId() + " --" + t.getSymbol() + "--> " + t.getDestination().getId());
        }

        for (DFATransition oldTrans : original.getTransitions()) {
            DFAState newSource = stateMap.get(oldTrans.getSource());
            DFAState newDest = stateMap.get(oldTrans.getDestination());

            if (newSource != null && newDest != null) {
                DFATransition newTrans = new DFATransition(newSource, newDest, oldTrans.getSymbol());
                mergedDfa.addTransition(newTrans);
                System.out.println("Added transition: " + newSource.getId() + " --" + newTrans.getSymbol() + "--> " + newDest.getId());
            }
        }

        return mergedDfa;
    }


    // Add this for debugging
    private boolean isValidMerge(DFA mergedDfa, List<String> negativeExamples) {
        System.out.println("Validating merge - States: " + mergedDfa.getStates().size() +
                " Transitions: " + mergedDfa.getTransitions().size());

        if (negativeExamples == null || negativeExamples.isEmpty()) {
            return true;
        }

        for (String example : negativeExamples) {
            if (mergedDfa.accepts(example)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasEquivalentTransition(DFA dfa, DFATransition transition) {
        return dfa.getTransitions().stream().anyMatch(t ->
                t.getSource().equals(transition.getSource()) &&
                        t.getDestination().equals(transition.getDestination()) &&
                        t.getSymbol() == transition.getSymbol()
        );
    }

    private List<StatePair> findMergeablePairs(DFA dfa) {
        List<StatePair> pairs = new ArrayList<>();
        List<DFAState> states = new ArrayList<>(dfa.getStates());

        for (int i = 0; i < states.size(); i++) {
            for (int j = i + 1; j < states.size(); j++) {
                DFAState state1 = states.get(i);
                DFAState state2 = states.get(j);

                // Only add pairs that have potential for merging
                if (canPotentiallyMerge(state1, state2, dfa)) {
                    pairs.add(new StatePair(state1, state2));
                }
            }
        }

        return pairs;
    }

    private boolean canPotentiallyMerge(DFAState state1, DFAState state2, DFA dfa) {
        // States with different accepting status cannot be merged
        if (state1.isAccepting() != state2.isAccepting()) {
            return false;
        }

        // Don't merge if one is start state and other isn't
        boolean isStart1 = state1.equals(dfa.getStartState());
        boolean isStart2 = state2.equals(dfa.getStartState());
        if (isStart1 != isStart2) {
            return false;
        }

        // Check for compatible transitions
        Set<Character> symbols1 = getTransitionSymbols(state1, dfa);
        Set<Character> symbols2 = getTransitionSymbols(state2, dfa);

        // States should have at least one symbol in common to be worth merging
        Set<Character> intersection = new HashSet<>(symbols1);
        intersection.retainAll(symbols2);

        return !intersection.isEmpty();
    }


    // In DFAMinimiser.java
    private int comparePairsByPriority(StatePair pair1, StatePair pair2, DFA dfa) {
        // Calculate shared transitions for pair1
        int sharedTransitions1 = countSharedTransitions(pair1.state1, pair1.state2, dfa);

        // Calculate shared transitions for pair2
        int sharedTransitions2 = countSharedTransitions(pair2.state1, pair2.state2, dfa);

        // Higher number of shared transitions means higher priority (lower value)
        return Integer.compare(sharedTransitions2, sharedTransitions1);
    }

    private int countSharedTransitions(DFAState state1, DFAState state2, DFA dfa) {
        Set<Character> symbols1 = getTransitionSymbols(state1, dfa);
        Set<Character> symbols2 = getTransitionSymbols(state2, dfa);

        // Count symbols that both states have transitions for
        Set<Character> intersection = new HashSet<>(symbols1);
        intersection.retainAll(symbols2);

        return intersection.size();
    }

    private Set<Character> getTransitionSymbols(DFAState state, DFA dfa) {
        return dfa.getTransitionsFrom(state)
                .stream()
                .map(DFATransition::getSymbol)
                .collect(Collectors.toSet());
    }

    private static class StatePair {
        DFAState state1;
        DFAState state2;

        StatePair(DFAState state1, DFAState state2) {
            this.state1 = state1;
            this.state2 = state2;
        }
    }
}
