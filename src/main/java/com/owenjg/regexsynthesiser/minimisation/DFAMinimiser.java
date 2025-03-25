package com.owenjg.regexsynthesiser.minimisation;

import com.owenjg.regexsynthesiser.dfa.DFA;

import java.util.*;

public class DFAMinimiser {
    public DFA minimizeDFA(DFA dfa) {
        // Handle special case - empty DFA or no accepting states
        if (dfa.getNumStates() <= 1) {
            return copyDFA(dfa);
        }

        Map<Integer, Set<Integer>> partitions = initializePartitions(dfa);

        // Special case - all states are accepting or all are non-accepting
        if (partitions.size() <= 1) {
            // Create a partition with just the start state to handle this case
            partitions.clear();
            Set<Integer> allStates = new HashSet<>();
            allStates.add(dfa.getStartState());
            partitions.put(0, allStates);

            // Add other states to a different partition if any
            Set<Integer> otherStates = new HashSet<>(dfa.getStates());
            otherStates.remove(dfa.getStartState());
            if (!otherStates.isEmpty()) {
                partitions.put(1, otherStates);
            }

            return buildMinimizedDFA(dfa, partitions);
        }

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
                } else if (!splits.isEmpty()) {
                    // Add the unsplit partition
                    newPartitions.put(partitionCounter++, splits.values().iterator().next());
                }
            }

            // If we have new partitions, update
            if (!newPartitions.isEmpty()) {
                partitions = newPartitions;
            } else {
                // Safety check to avoid infinite loops
                break;
            }
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

        // Add all the states from DFA's state set as well
        for (int state : dfa.getStates()) {
            if (dfa.isAcceptingState(state)) {
                acceptingStates.add(state);
            } else {
                nonAcceptingStates.add(state);
            }
        }

        // Only add non-empty partitions
        int partitionId = 0;
        if (!acceptingStates.isEmpty()) {
            partitions.put(partitionId++, acceptingStates);
        }
        if (!nonAcceptingStates.isEmpty()) {
            partitions.put(partitionId, nonAcceptingStates);
        }

        return partitions;
    }

    private DFA buildMinimizedDFA(DFA dfa, Map<Integer, Set<Integer>> partitions) {
        if (partitions.isEmpty()) {
            // Handle edge case - return a copy of the original DFA
            return copyDFA(dfa);
        }

        DFA minimizedDFA = new DFA(0);
        Map<Set<Integer>, Integer> partitionToState = new HashMap<>();
        int stateCounter = 0;

        // Create states for each partition
        for (Map.Entry<Integer, Set<Integer>> entry : partitions.entrySet()) {
            Set<Integer> partition = entry.getValue();

            if (partition.isEmpty()) {
                continue; // Skip empty partitions
            }

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
        for (Map.Entry<Integer, Set<Integer>> entry : partitions.entrySet()) {
            Set<Integer> partition = entry.getValue();

            if (partition.isEmpty() || !partitionToState.containsKey(partition)) {
                continue; // Skip empty partitions
            }

            int fromState = partitionToState.get(partition);
            int representativeState = partition.iterator().next();

            for (char symbol : dfa.getAlphabet()) {
                int nextState = dfa.getTransition(representativeState, symbol);
                if (nextState != DFA.INVALID_STATE) {
                    Set<Integer> targetPartition = findPartition(partitions, nextState);
                    if (targetPartition != null && !targetPartition.isEmpty() && partitionToState.containsKey(targetPartition)) {
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

    private DFA copyDFA(DFA original) {
        DFA copy = new DFA(original.getStartState());

        // Copy accepting states
        for (int state : original.getStates()) {
            if (original.isAcceptingState(state)) {
                copy.addAcceptingState(state);
            }
        }

        // Copy transitions
        for (Map.Entry<Integer, Map<Character, Integer>> stateEntry : original.getTransitions().entrySet()) {
            int fromState = stateEntry.getKey();
            for (Map.Entry<Character, Integer> transEntry : stateEntry.getValue().entrySet()) {
                char symbol = transEntry.getKey();
                int toState = transEntry.getValue();
                copy.addTransition(fromState, symbol, toState);
            }
        }

        return copy;
    }
}