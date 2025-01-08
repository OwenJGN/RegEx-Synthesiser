package com.owenjg.regexsynthesiser.minimisation;

import com.owenjg.regexsynthesiser.dfa.DFAState;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UnionFind {
    private Map<DFAState, DFAState> parent;
    private Map<DFAState, Integer> rank;

    public void initialize(Set<DFAState> states) {
        parent = new HashMap<>();
        rank = new HashMap<>();

        for (DFAState state : states) {
            parent.put(state, state);
            rank.put(state, 0);
        }
    }

    public DFAState find(DFAState state) {
        if (!parent.get(state).equals(state)) {
            parent.put(state, find(parent.get(state)));
        }
        return parent.get(state);
    }

    public void union(DFAState state1, DFAState state2) {
        DFAState root1 = find(state1);
        DFAState root2 = find(state2);

        if (!root1.equals(root2)) {
            if (rank.get(root1) < rank.get(root2)) {
                parent.put(root1, root2);
            } else if (rank.get(root1) > rank.get(root2)) {
                parent.put(root2, root1);
            } else {
                parent.put(root2, root1);
                rank.put(root1, rank.get(root1) + 1);
            }
        }
    }
}