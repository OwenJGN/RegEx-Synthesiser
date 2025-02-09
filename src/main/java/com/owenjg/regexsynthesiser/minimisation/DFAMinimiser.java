// DFAMinimiser.java
package com.owenjg.regexsynthesiser.minimisation;

import com.owenjg.regexsynthesiser.dfa.DFA;

import java.util.*;

public class DFAMinimiser {
    public DFA minimizeDFA(DFA dfa) {
        Map<Integer, Set<Integer>> partitions = initializePartitions(dfa);
        boolean changed = true;

        while (changed) {
            changed = false;
            Map<Integer, Set<Integer>> newPartitions = new HashMap<>();

            for (Set<Integer> partition : partitions.values()) {
                Map<Integer, Set<Integer>> splitPartitions = splitPartition(dfa, partition);
                if (splitPartitions.size() > 1) {
                    changed = true;
                    newPartitions.putAll(splitPartitions);
                } else {
                    newPartitions.put(partition.iterator().next(), partition);
                }
            }

            partitions = newPartitions;
        }

        return buildMinimizedDFA(dfa, partitions);
    }

    private Map<Integer, Set<Integer>> initializePartitions(DFA dfa) {
        Map<Integer, Set<Integer>> partitions = new HashMap<>();
        Set<Integer> acceptingStates = new HashSet<>();
        Set<Integer> nonAcceptingStates = new HashSet<>();

        for (int state = 0; state < dfa.getNumStates(); state++) {
            if (dfa.isAcceptingState(state)) {
                acceptingStates.add(state);
            } else {
                nonAcceptingStates.add(state);
            }
        }

        partitions.put(0, acceptingStates);
        partitions.put(1, nonAcceptingStates);

        return partitions;
    }

    private Map<Integer, Set<Integer>> splitPartition(DFA dfa, Set<Integer> partition) {
        Map<Integer, Set<Integer>> splitPartitions = new HashMap<>();

        for (int state : partition) {
            String transitionKey = getTransitionKey(dfa, state, partition);
            splitPartitions.computeIfAbsent(transitionKey.hashCode(), k -> new HashSet<>()).add(state);
        }

        return splitPartitions;
    }

    private String getTransitionKey(DFA dfa, int state, Set<Integer> partition) {
        StringBuilder sb = new StringBuilder();

        for (char symbol : dfa.getAlphabet()) {
            int nextState = dfa.getTransition(state, symbol);
            sb.append(partition.contains(nextState) ? "1" : "0");
        }

        return sb.toString();
    }

    private DFA buildMinimizedDFA(DFA dfa, Map<Integer, Set<Integer>> partitions) {
        DFA minimizedDFA = new DFA(0);
        Map<Set<Integer>, Integer> stateMapping = new HashMap<>();
        int stateCounter = 0;

        for (Set<Integer> partition : partitions.values()) {
            int representativeState = partition.iterator().next();
            stateMapping.put(partition, stateCounter);

            if (partition.contains(dfa.getStartState())) {
                minimizedDFA.setStartState(stateCounter);
            }

            if (dfa.isAcceptingState(representativeState)) {
                minimizedDFA.addAcceptingState(stateCounter);
            }

            stateCounter++;
        }

        for (Set<Integer> partition : partitions.values()) {
            int representativeState = partition.iterator().next();
            int fromState = stateMapping.get(partition);

            for (char symbol : dfa.getAlphabet()) {
                int nextState = dfa.getTransition(representativeState, symbol);
                Set<Integer> nextPartition = findPartition(partitions, nextState);

                if (nextPartition != null) {
                    int toState = stateMapping.get(nextPartition);
                    minimizedDFA.addTransition(fromState, symbol, toState);
                }
            }
        }

        return minimizedDFA;
    }

    private Set<Integer> findPartition(Map<Integer, Set<Integer>> partitions, int state) {
        for (Set<Integer> partition : partitions.values()) {
            if (partition.contains(state)) {
                return partition;
            }
        }
        return null;
    }
}