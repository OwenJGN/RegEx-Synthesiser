package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;
import com.owenjg.regexsynthesiser.exceptions.RegexSynthesisException;
import com.owenjg.regexsynthesiser.minimisation.DFAMinimiser;
import com.owenjg.regexsynthesiser.simplification.RegexSimplifier;
import com.owenjg.regexsynthesiser.validation.ExampleValidator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class RegexSynthesiser {
    private volatile boolean cancelRequested = false;
    private ProgressCallback progressCallback;
    private final PrefixTreeBuilder prefixTreeBuilder;
    private final DFAMinimiser dfaMinimiser;
    private final StateEliminationAlgorithm stateElimination;
    private final RegexSimplifier regexSimplifier;
    private final ExampleValidator exampleValidator;
    private final RegexGeneralizer patternGeneralizer;
    @FXML
    private Label currentStatusLabel;


    public RegexSynthesiser(Label statusLabel) {
        this.patternGeneralizer = new RegexGeneralizer();// Modified constructor
        this.prefixTreeBuilder = new PrefixTreeBuilder();
        this.dfaMinimiser = new DFAMinimiser();
        this.stateElimination = new StateEliminationAlgorithm();
        this.regexSimplifier = new RegexSimplifier();
        this.exampleValidator = new ExampleValidator();
        this.currentStatusLabel = statusLabel;
    }


    // Interface for progress callback
    public interface ProgressCallback {
        void onProgress(long elapsedTime, String status);
        void onComplete(String generatedRegex);
        void onCancel();
        void onError(String errorMessage);

    }

    public void synthesise(List<String> positiveExamples, List<String> negativeExamples) {
        try {
            validateInputExamples(positiveExamples, negativeExamples);

            // First analyze patterns in positive examples
            PatternAnalyzer analyzer = new PatternAnalyzer();
            String generalizedPattern = analyzer.generalizePattern(positiveExamples);

            // Build DFA from generalized pattern
            updateStatus("Building prefix tree...");
            DFA pta = prefixTreeBuilder.buildPrefixTree(Collections.singletonList(generalizedPattern));

            updateStatus("Minimizing DFA...");
            DFA minimizedDFA = dfaMinimiser.minimizeDFA(pta);

            // Skip generalization step since we've already generalized
            updateStatus("Refining DFA with negative examples...");
            DFA refinedDFA = refineWithNegativeExamples(minimizedDFA, negativeExamples);

            updateStatus("Extracting regex from DFA...");
            String regex = stateElimination.eliminateStates(refinedDFA);
            System.out.println("Raw regex: " + regex);

            updateStatus("Simplifying regex...");
            String simplifiedRegex = RegexSimplifier.simplify(regex);
            System.out.println("Simplified regex: " + simplifiedRegex);

            updateStatus("Validating regex...");
            // First validate against the generalized pattern
            boolean isValid = validatePattern(simplifiedRegex, positiveExamples, negativeExamples);

            if (isValid) {
                System.out.println("Synthesis complete!");
                if (progressCallback != null) {
                    progressCallback.onComplete(simplifiedRegex);
                }
            } else {
                System.out.println("Warning: Generated regex failed validation");
                if (progressCallback != null) {
                    progressCallback.onError("Generated regex failed validation");
                }
            }
        } catch (Exception e) {
            System.err.println("Error during synthesis: " + e.getMessage());
            e.printStackTrace();
            if (progressCallback != null) {
                progressCallback.onError("Synthesis failed: " + e.getMessage());
            }
        }
    }

    private DFA refineWithNegativeExamples(DFA dfa, List<String> negativeExamples) {
        DFA refinedDFA = new DFA(dfa.getStartState());
        Map<Integer, Integer> stateMap = new HashMap<>();
        int nextState = dfa.getNumStates();

        // Copy initial DFA structure
        for (int state : dfa.getStates()) {
            stateMap.put(state, state);
            if (dfa.isAcceptingState(state)) {
                refinedDFA.addAcceptingState(state);
            }
        }

        // Process transitions
        for (Map.Entry<Integer, Map<Character, Integer>> entry : dfa.getTransitions().entrySet()) {
            int from = entry.getKey();
            for (Map.Entry<Character, Integer> transition : entry.getValue().entrySet()) {
                refinedDFA.addTransition(stateMap.get(from), transition.getKey(),
                        stateMap.get(transition.getValue()));
            }
        }

        // Handle negative examples
        for (String example : negativeExamples) {
            int currentState = refinedDFA.getStartState();
            List<Integer> visitedStates = new ArrayList<>();

            for (char c : example.toCharArray()) {
                int nextStateTemp = refinedDFA.getTransition(currentState, c);
                if (nextStateTemp == DFA.INVALID_STATE) {
                    break;
                }
                visitedStates.add(currentState);
                currentState = nextStateTemp;
            }

            // Check if the negative example is accepted by the DFA
            if (refinedDFA.isAcceptingState(currentState)) {
                // Create a new non-accepting state for the last visited state
                int lastState = visitedStates.isEmpty() ? currentState : visitedStates.get(visitedStates.size() - 1);
                int newState = nextState++;
                refinedDFA.removeAcceptingState(lastState);
                refinedDFA.addTransition(lastState, example.charAt(visitedStates.size()), newState);
            }
        }

        return refinedDFA;
    }

    private boolean validatePattern(String regex, List<String> positiveExamples, List<String> negativeExamples) {
        try {
            Pattern pattern = Pattern.compile(regex);

            // Check all positive examples match
            boolean allPositiveMatch = positiveExamples.stream()
                    .allMatch(ex -> pattern.matcher(ex).matches());

            // Check no negative examples match
            boolean noNegativeMatch = negativeExamples == null || negativeExamples.isEmpty() ||
                    negativeExamples.stream()
                            .noneMatch(ex -> pattern.matcher(ex).matches());

            return allPositiveMatch && noNegativeMatch;
        } catch (Exception e) {
            return false;
        }
    }

//    private String tryAlternativePatterns(List<String> positiveExamples, List<String> negativeExamples) {
//        // Try splitting into subgroups if the examples have different patterns
//        List<List<String>> subgroups = findSimilarExamples(positiveExamples);
//
//        if (subgroups.size() > 1) {
//            // Generate pattern for each subgroup and combine with OR
//            List<String> patterns = subgroups.stream()
//                    .map(group -> patternGeneralizer.generalizePattern(group))
//                    .collect(Collectors.toList());
//            return "(" + String.join("|", patterns) + ")";
//        }
//
//        // If no subgroups work, fall back to exact matching only as last resort
//        return createExactMatchPattern(positiveExamples);
//    }

    private List<List<String>> findSimilarExamples(List<String> examples) {
        Map<Integer, List<String>> lengthGroups = examples.stream()
                .collect(Collectors.groupingBy(String::length));

        // If we have different lengths, group by length
        if (lengthGroups.size() > 1) {
            return new ArrayList<>(lengthGroups.values());
        }

        // If same length, try to group by pattern similarity
        return Collections.singletonList(examples);
    }

    private String createExactMatchPattern(List<String> examples) {
        return examples.stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
    }

    private void validateInputExamples(List<String> positiveExamples, List<String> negativeExamples)
            throws RegexSynthesisException {
        if (positiveExamples == null || positiveExamples.isEmpty()) {
            throw new RegexSynthesisException("At least one positive example is required");
        }
    }

    private void updateStatus(String status) {
        System.out.println(status);
        if (currentStatusLabel != null) {
            Platform.runLater(() -> currentStatusLabel.setText(status));
        }
    }

    private void handleError(String errorMessage) {
        if (progressCallback != null) {
            progressCallback.onError("Error during synthesis: " + errorMessage);
        }
    }

    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    public void cancelGeneration() {
        cancelRequested = true;
    }
}