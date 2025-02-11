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
            int partitionCounter = 0;  // Counter for new partition IDs

            // For each current partition
            for (Set<Integer> partition : partitions.values()) {
                // Try to split based on transitions
                Map<String, Set<Integer>> splits = splitPartitionByTransitions(dfa, partition, partitions);

                if (splits.size() > 1) {
                    changed = true;
                    // Assign new partition IDs to each split
                    for (Set<Integer> split : splits.values()) {
                        newPartitions.put(partitionCounter++, split);
                    }
                } else {
                    newPartitions.put(partitionCounter++, partition);
                }
            }

            partitions = newPartitions;
        }

        return buildMinimizedDFA(dfa, partitions);
    }

    private Map<String, Set<Integer>> splitPartitionByTransitions(DFA dfa, Set<Integer> partition,
                                                                  Map<Integer, Set<Integer>> currentPartitions) {
        Map<String, Set<Integer>> splits = new HashMap<>();

        for (int state : partition) {
            StringBuilder signature = new StringBuilder();
            signature.append(dfa.isAcceptingState(state) ? "1" : "0");

            // Add transition information
            for (char symbol : dfa.getAlphabet()) {
                int nextState = dfa.getTransition(state, symbol);
                if (nextState != DFA.INVALID_STATE) {
                    signature.append("|").append(symbol).append("->")
                            .append(getPartitionId(currentPartitions, nextState))
                            .append(":").append(nextState);
                }
            }

            splits.computeIfAbsent(signature.toString(), k -> new HashSet<>()).add(state);
        }

        return splits;
    }


    private int getPartitionId(Map<Integer, Set<Integer>> partitions, int state) {
        for (Map.Entry<Integer, Set<Integer>> entry : partitions.entrySet()) {
            if (entry.getValue().contains(state)) {
                return entry.getKey();
            }
        }
        return -1;
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

        // Add caching for transition keys
        Map<Integer, String> transitionKeyCache = new HashMap<>();

        for (int state : partition) {
            String transitionKey = transitionKeyCache.computeIfAbsent(state,
                    k -> getTransitionKey(dfa, k, partition));
            splitPartitions.computeIfAbsent(transitionKey.hashCode(),
                    k -> new HashSet<>()).add(state);
        }

        // Only return splits if we actually split something
        return splitPartitions.size() > 1 ? splitPartitions :
                Collections.singletonMap(partition.iterator().next(), partition);
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
        Map<Set<Integer>, Integer> partitionToState = new HashMap<>();
        int stateCounter = 0;

        // Create states for each partition
        for (Set<Integer> partition : partitions.values()) {
            int representativeState = partition.iterator().next();
            partitionToState.put(partition, stateCounter);

            // Preserve accepting state information
            if (dfa.isAcceptingState(representativeState)) {
                minimizedDFA.addAcceptingState(stateCounter);
            }

            // Set start state
            if (partition.contains(dfa.getStartState())) {
                minimizedDFA.setStartState(stateCounter);
            }

            stateCounter++;
        }

        // Add transitions
        for (Set<Integer> partition : partitions.values()) {
            int fromState = partitionToState.get(partition);
            int representativeState = partition.iterator().next();

            for (char symbol : dfa.getAlphabet()) {
                int nextState = dfa.getTransition(representativeState, symbol);
                if (nextState != DFA.INVALID_STATE) {
                    Set<Integer> targetPartition = findPartition(partitions, nextState);
                    if (targetPartition != null) {
                        int toState = partitionToState.get(targetPartition);
                        minimizedDFA.addTransition(fromState, symbol, toState);
                    }
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