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
    private final PatternGeneralizer patternGeneralizer;
    @FXML
    private Label currentStatusLabel;


    public RegexSynthesiser(Label statusLabel) {
        this.patternGeneralizer = new PatternGeneralizer();// Modified constructor
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

            updateStatus("Building prefix tree...");
            DFA pta = prefixTreeBuilder.buildPrefixTree(positiveExamples);

            updateStatus("Minimizing DFA...");
            DFA minimizedDFA = dfaMinimiser.minimizeDFA(pta);

            updateStatus("Refining DFA with negative examples...");
            DFA refinedDFA = refineWithNegativeExamples(minimizedDFA, negativeExamples);

            updateStatus("Extracting regex from DFA...");
            String regex = stateElimination.eliminateStates(refinedDFA);

            updateStatus("Simplifying regex...");
            String simplifiedRegex = RegexSimplifier.simplify(regex);
            System.out.println(simplifiedRegex);

            updateStatus("Validating regex...");
            boolean isValid = exampleValidator.validateExamples(simplifiedRegex, positiveExamples, negativeExamples);

            if (isValid) {
                if (progressCallback != null) {
                    progressCallback.onComplete(simplifiedRegex);
                }
            } else {
                updateStatus("Trying alternative patterns...");
                String alternativeRegex = tryAlternativePatterns(positiveExamples, negativeExamples);
                alternativeRegex = RegexSimplifier.simplify((alternativeRegex));
                if (progressCallback != null) {
                    progressCallback.onComplete(alternativeRegex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            handleError(e.getMessage());
        }
    }

    private DFA refineWithNegativeExamples(DFA dfa, List<String> negativeExamples) {
        for (String example : negativeExamples) {
            int currentState = dfa.getStartState();
            for (char c : example.toCharArray()) {
                currentState = dfa.getTransition(currentState, c);
                if (currentState == DFA.INVALID_STATE) {
                    break;
                }
            }
            if (dfa.isAcceptingState(currentState)) {
                dfa.removeAcceptingState(currentState);
            }
        }
        return dfa;
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

    private String tryAlternativePatterns(List<String> positiveExamples, List<String> negativeExamples) {
        // Try splitting into subgroups if the examples have different patterns
        List<List<String>> subgroups = findSimilarExamples(positiveExamples);

        if (subgroups.size() > 1) {
            // Generate pattern for each subgroup and combine with OR
            List<String> patterns = subgroups.stream()
                    .map(group -> patternGeneralizer.generalizePattern(group))
                    .collect(Collectors.toList());
            return "(" + String.join("|", patterns) + ")";
        }

        // If no subgroups work, fall back to exact matching only as last resort
        return createExactMatchPattern(positiveExamples);
    }

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