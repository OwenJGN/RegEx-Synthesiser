package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;
import java.util.*;
import java.util.stream.Collectors;

public class RegexGeneralizer {
    private static final int MIN_CHARS_FOR_PATTERN = 2;
    private static final int MIN_REPEAT_COUNT = 2;

    public DFA generalizeDFA(DFA dfa) {
        DFA generalizedDFA = new DFA(dfa.getStartState());

        // Copy accepting states
        for (int state : dfa.getStates()) {
            if (dfa.isAcceptingState(state)) {
                generalizedDFA.addAcceptingState(state);
            }
        }

        // Find and analyze transition patterns
        Map<Integer, List<TransitionPattern>> statePatterns = findTransitionPatterns(dfa);

        // Apply patterns to create generalized transitions
        applyTransitionPatterns(generalizedDFA, statePatterns);

        return generalizedDFA;
    }

    private Map<Integer, List<TransitionPattern>> findTransitionPatterns(DFA dfa) {
        Map<Integer, List<TransitionPattern>> patterns = new HashMap<>();

        for (int state : dfa.getStates()) {
            Map<Character, Integer> transitions = dfa.getTransitions().get(state);
            if (transitions == null) continue;

            // Group transitions by destination state
            Map<Integer, Set<Character>> transitionGroups = new HashMap<>();
            for (Map.Entry<Character, Integer> entry : transitions.entrySet()) {
                transitionGroups.computeIfAbsent(entry.getValue(), k -> new HashSet<>())
                        .add(entry.getKey());
            }

            // Analyze each group for patterns
            List<TransitionPattern> statePatterns = new ArrayList<>();
            for (Map.Entry<Integer, Set<Character>> group : transitionGroups.entrySet()) {
                TransitionPattern pattern = analyzeCharacterGroup(group.getKey(), group.getValue());
                if (pattern != null) {
                    statePatterns.add(pattern);
                }
            }

            // Look for repeating patterns
            TransitionPattern repeatPattern = findRepeatPattern(dfa, state);
            if (repeatPattern != null) {
                statePatterns.add(repeatPattern);
            }

            patterns.put(state, statePatterns);
        }

        return patterns;
    }

    private TransitionPattern analyzeCharacterGroup(int destState, Set<Character> chars) {
        if (chars.size() < MIN_CHARS_FOR_PATTERN) {
            return new TransitionPattern(destState, chars, PatternType.LITERAL);
        }

        // Check for character classes
        if (chars.stream().allMatch(Character::isDigit)) {
            return new TransitionPattern(destState, "\\d", PatternType.DIGIT);
        }
        if (chars.stream().allMatch(Character::isLowerCase)) {
            return new TransitionPattern(destState, "[a-z]", PatternType.LOWER);
        }
        if (chars.stream().allMatch(Character::isUpperCase)) {
            return new TransitionPattern(destState, "[A-Z]", PatternType.UPPER);
        }
        if (chars.stream().allMatch(Character::isLetterOrDigit)) {
            return new TransitionPattern(destState, "\\w", PatternType.WORD);
        }

        // Create custom character class
        return new TransitionPattern(destState,
                "[" + chars.stream().map(String::valueOf).sorted().collect(Collectors.joining()) + "]",
                PatternType.CUSTOM);
    }

    private TransitionPattern findRepeatPattern(DFA dfa, int state) {
        // Check for self-loops or cycles
        Map<Character, Integer> transitions = dfa.getTransitions().get(state);
        if (transitions == null) return null;

        Set<Character> selfLoops = transitions.entrySet().stream()
                .filter(e -> e.getValue() == state)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (!selfLoops.isEmpty()) {
            TransitionPattern pattern = analyzeCharacterGroup(state, selfLoops);
            if (pattern != null) {
                pattern.setRepeating(true);
            }
            return pattern;
        }

        return null;
    }

    private void applyTransitionPatterns(DFA targetDFA, Map<Integer, List<TransitionPattern>> patterns) {
        for (Map.Entry<Integer, List<TransitionPattern>> entry : patterns.entrySet()) {
            int fromState = entry.getKey();

            for (TransitionPattern pattern : entry.getValue()) {
                if (pattern.isRepeating()) {
                    // Add self-loop for repeating patterns
                    targetDFA.addTransition(fromState, pattern.getPatternChar(), fromState);
                    // Add transition to next state
                    targetDFA.addTransition(fromState, 'ε', pattern.getDestState());
                } else {
                    // Add normal transition
                    targetDFA.addTransition(fromState, pattern.getPatternChar(), pattern.getDestState());
                }
            }
        }
    }

    private static class TransitionPattern {
        private final int destState;
        private final String pattern;
        private final PatternType type;
        private boolean repeating;
        private Set<Character> literals;

        TransitionPattern(int destState, String pattern, PatternType type) {
            this.destState = destState;
            this.pattern = pattern;
            this.type = type;
            this.repeating = false;
        }

        TransitionPattern(int destState, Set<Character> chars, PatternType type) {
            this.destState = destState;
            this.literals = chars;
            this.pattern = chars.stream().map(String::valueOf).collect(Collectors.joining());
            this.type = type;
            this.repeating = false;
        }

        public int getDestState() { return destState; }
        public String getPattern() { return pattern; }
        public PatternType getType() { return type; }
        public boolean isRepeating() { return repeating; }
        public void setRepeating(boolean repeating) { this.repeating = repeating; }

        public char getPatternChar() {
            switch (type) {
                case DIGIT: return '\\';
                case WORD: return 'w';
                case LOWER: return 'a';
                case UPPER: return 'A';
                case CUSTOM:
                case LITERAL:
                    return literals.iterator().next();
                default:
                    return '?';
            }
        }
    }

    private enum PatternType {
        DIGIT, WORD, LOWER, UPPER, CUSTOM, LITERAL
    }
}