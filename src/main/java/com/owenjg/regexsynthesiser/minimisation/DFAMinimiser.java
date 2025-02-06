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

    private DFA simulateMerge(DFA original, DFAState state1, DFAState state2) {
        // Create a copy of the original DFA to work with
        DFA mergedDfa = new DFA();

        // Create a mapping of old states to new states
        Map<DFAState, DFAState> stateMap = new HashMap<>();

        // Create the merged state
        DFAState mergedState = new DFAState(Math.min(state1.getId(), state2.getId()));
        mergedState.setAccepting(state1.isAccepting() || state2.isAccepting());

        // Add all states except state1 and state2 to the new DFA
        for (DFAState oldState : original.getStates()) {
            if (oldState.equals(state1) || oldState.equals(state2)) {
                stateMap.put(oldState, mergedState);
            } else {
                DFAState newState = new DFAState(oldState.getId());
                newState.setAccepting(oldState.isAccepting());
                stateMap.put(oldState, newState);
                mergedDfa.addState(newState);
            }
        }

        // Add the merged state to the new DFA
        mergedDfa.addState(mergedState);

        // Set the start state
        if (original.getStartState().equals(state1) || original.getStartState().equals(state2)) {
            mergedDfa.setStartState(mergedState);
        } else {
            mergedDfa.setStartState(stateMap.get(original.getStartState()));
        }

        // Add transitions to the new DFA
        for (DFATransition oldTransition : original.getTransitions()) {
            DFAState newSource = stateMap.get(oldTransition.getSource());
            DFAState newDest = stateMap.get(oldTransition.getDestination());

            // Create new transition with mapped states
            DFATransition newTransition = new DFATransition(newSource, newDest, oldTransition.getSymbol());

            // Add transition, avoiding duplicates
            if (!hasEquivalentTransition(mergedDfa, newTransition)) {
                mergedDfa.addTransition(newTransition);
            }
        }

        return mergedDfa;
    }

    private boolean hasEquivalentTransition(DFA dfa, DFATransition transition) {
        return dfa.getTransitions().stream().anyMatch(t ->
                t.getSource().equals(transition.getSource()) &&
                        t.getDestination().equals(transition.getDestination()) &&
                        t.getSymbol() == transition.getSymbol()
        );
    }

    private boolean isValidMerge(DFA mergedDfa, List<String> negativeExamples) {
        if (negativeExamples == null || negativeExamples.isEmpty()) {
            return true;
        }

        // Check if the merged DFA accepts any negative examples
        for (String example : negativeExamples) {
            if (mergedDfa.accepts(example)) {
                return false;
            }
        }

        return true;
    }

    public DFA minimise(DFA dfa, List<String> negativeExamples) {
        // Initialize UnionFind with all states
        unionFind.initialize(dfa.getStates());

        // Find mergeable states while respecting negative examples
        List<StatePair> statePairs = findMergeablePairs(dfa);

        // Sort pairs by merge priority (e.g., number of shared transitions)
        Collections.sort(statePairs, this::comparePairsByPriority);

        DFA currentDfa = dfa;

        // Try merging each pair of states
        for (StatePair pair : statePairs) {
            DFA mergedDfa = simulateMerge(currentDfa, pair.state1, pair.state2);

            if (isValidMerge(mergedDfa, negativeExamples)) {
                currentDfa = mergedDfa;
                unionFind.union(pair.state1, pair.state2);
            }
        }

        return currentDfa;
    }

    private List<StatePair> findMergeablePairs(DFA dfa) {
        List<StatePair> pairs = new ArrayList<>();
        List<DFAState> states = new ArrayList<>(dfa.getStates());

        for (int i = 0; i < states.size(); i++) {
            for (int j = i + 1; j < states.size(); j++) {
                DFAState state1 = states.get(i);
                DFAState state2 = states.get(j);

                // Only add pairs that have potential for merging
                if (canPotentiallyMerge(state1, state2)) {
                    pairs.add(new StatePair(state1, state2));
                }
            }
        }

        return pairs;
    }

    private boolean canPotentiallyMerge(DFAState state1, DFAState state2) {
        // States with different accepting status cannot be merged
        if (state1.isAccepting() != state2.isAccepting()) {
            return false;
        }

        // Don't merge if one is start state and other isn't
        boolean isStart1 = state1.equals(DFA.getStartState());
        boolean isStart2 = state2.equals(DFA.getStartState());
        if (isStart1 != isStart2) {
            return false;
        }

        // Check for compatible transitions
        Set<Character> symbols1 = getTransitionSymbols(state1);
        Set<Character> symbols2 = getTransitionSymbols(state2);

        // States should have at least one symbol in common to be worth merging
        Set<Character> intersection = new HashSet<>(symbols1);
        intersection.retainAll(symbols2);

        return !intersection.isEmpty();
    }


    // In DFAMinimiser.java
    private int comparePairsByPriority(StatePair pair1, StatePair pair2) {
        // Calculate shared transitions for pair1
        int sharedTransitions1 = countSharedTransitions(pair1.state1, pair1.state2);

        // Calculate shared transitions for pair2
        int sharedTransitions2 = countSharedTransitions(pair2.state1, pair2.state2);

        // Higher number of shared transitions means higher priority (lower value)
        return Integer.compare(sharedTransitions2, sharedTransitions1);
    }

    private int countSharedTransitions(DFAState state1, DFAState state2) {
        Set<Character> symbols1 = getTransitionSymbols(state1);
        Set<Character> symbols2 = getTransitionSymbols(state2);

        // Count symbols that both states have transitions for
        Set<Character> intersection = new HashSet<>(symbols1);
        intersection.retainAll(symbols2);

        return intersection.size();
    }

    private Set<Character> getTransitionSymbols(DFAState state) {
        return DFA.getTransitionsFrom(state)
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
