package com.owenjg.regexsynthesiser.simplification;

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

        // Get elimination order using complexity heuristic
        List<Integer> eliminationOrder = getSmartEliminationOrder(dfa);

        // Eliminate states
        for (Integer state : eliminationOrder) {
            eliminateState(dfa, state);
        }

        // Get final regex
        String regex = getFinalRegex(dfa);

        // Apply additional simplification for repetitions
        regex = simplifyRepetitions(regex);

        return regex;
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
                    regexTransitions.put(trans, combineAlternatives(existing, transStr));
                } else {
                    regexTransitions.put(trans, transStr);
                }
            }
        }
    }

    /**
     * Combines alternative patterns more efficiently than simple concatenation
     */
    private String combineAlternatives(String pattern1, String pattern2) {
        // Try to create character classes when possible
        if (pattern1.length() == 1 && pattern2.length() == 1) {
            char c1 = pattern1.charAt(0);
            char c2 = pattern2.charAt(0);

            // Check if both are letters or both are digits
            boolean bothLetters = Character.isLetter(c1) && Character.isLetter(c2);
            boolean bothDigits = Character.isDigit(c1) && Character.isDigit(c2);

            if (bothLetters || bothDigits) {
                return "[" + pattern1 + pattern2 + "]";
            }
        }

        // Check if either pattern already contains alternatives
        if (pattern1.startsWith("(") && pattern1.endsWith(")") && pattern1.contains("|")) {
            // Remove outer parentheses and add new alternative
            return "(" + pattern1.substring(1, pattern1.length() - 1) + "|" + pattern2 + ")";
        } else if (pattern2.startsWith("(") && pattern2.endsWith(")") && pattern2.contains("|")) {
            // Remove outer parentheses and add new alternative
            return "(" + pattern1 + "|" + pattern2.substring(1, pattern2.length() - 1) + ")";
        } else {
            return "(" + pattern1 + "|" + pattern2 + ")";
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
                    // Simplify self-loop expression
                    if (selfLoop.length() == 1) {
                        newRegex += selfLoop + "*";
                    } else {
                        newRegex += "(" + selfLoop + ")*";
                    }
                }
                newRegex += outgoing.getValue();

                StateTransition newTrans = new StateTransition(from, to);

                // Merge or add new transition
                if (regexTransitions.containsKey(newTrans)) {
                    String existing = regexTransitions.get(newTrans);
                    regexTransitions.put(newTrans, combineAlternatives(existing, newRegex));
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
                    if (selfLoop.length() == 1) {
                        incomingRegex += selfLoop + "*";
                    } else {
                        incomingRegex += "(" + selfLoop + ")*";
                    }
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

    /**
     * Smart elimination order based on transition complexity.
     * States with fewer/simpler transitions are eliminated first.
     */
    private List<Integer> getSmartEliminationOrder(DFA dfa) {
        // Create a map of states to their complexity scores
        Map<Integer, Integer> stateComplexity = new HashMap<>();

        // Calculate complexity for each state
        for (Integer state : dfa.getStates()) {
            if (state == dfa.getStartState()) {
                // Skip start state - we eliminate it last
                continue;
            }

            // Count incoming and outgoing transitions
            int inCount = 0;
            int outCount = 0;

            for (Map.Entry<StateTransition, String> entry : regexTransitions.entrySet()) {
                StateTransition trans = entry.getKey();
                if (trans.from == state) {
                    outCount++;
                }
                if (trans.to == state) {
                    inCount++;
                }
            }

            // Complexity score based on how many new transitions would be created
            int complexity = inCount * outCount;

            // Add bonus for self-loops (they're usually easier to eliminate)
            boolean hasSelfLoop = regexTransitions.containsKey(new StateTransition(state, state));
            if (hasSelfLoop) {
                complexity -= 1;
            }

            // Adjust for accepting states (prefer keeping them longer)
            if (dfa.isAcceptingState(state)) {
                complexity += 2;
            }

            stateComplexity.put(state, complexity);
        }

        // Sort states by complexity (lower first)
        List<Integer> order = new ArrayList<>(stateComplexity.keySet());
        order.sort(Comparator.comparing(stateComplexity::get));

        return order;
    }

    /**
     * Apply simplification for repetitions in the regex
     */
    private String simplifyRepetitions(String regex) {
        // This is a placeholder for regex-specific simplifications
        // that are better applied at the string level

        // Example: convert (a)* to a*
        regex = regex.replaceAll("\\(([a-zA-Z0-9])\\)\\*", "$1*");

        // Convert character class with single char to just the char
        regex = regex.replaceAll("\\[([a-zA-Z0-9])\\]", "$1");

        // Look for repeated groups
        regex = simplifyRepeatedGroups(regex);

        return regex;
    }

    /**
     * Simplify patterns with repeated groups like (ab)(ab)* to (ab)+
     */
    private String simplifyRepeatedGroups(String regex) {
        // This is a simplistic implementation
        // A more comprehensive solution would use a regex parser

        for (int i = 0; i < regex.length(); i++) {
            // Find closing parenthesis
            if (regex.charAt(i) == ')') {
                // Look for a group that repeats itself
                int start = findMatchingOpenParen(regex, i);
                if (start >= 0) {
                    String group = regex.substring(start, i + 1);

                    // Check if followed by the same group with a star
                    if (i + group.length() + 1 < regex.length() &&
                            regex.substring(i + 1).startsWith(group + "*")) {
                        // Replace with group+
                        regex = regex.substring(0, start) +
                                group + "+" +
                                regex.substring(i + 1 + group.length() + 1);
                    }
                }
            }
        }

        return regex;
    }

    private int findMatchingOpenParen(String regex, int closePos) {
        int count = 1;
        for (int i = closePos - 1; i >= 0; i--) {
            if (regex.charAt(i) == ')') {
                count++;
            } else if (regex.charAt(i) == '(') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
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