package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;
import com.owenjg.regexsynthesiser.exceptions.RegexSynthesisException;
import com.owenjg.regexsynthesiser.minimisation.DFAMinimiser;
import com.owenjg.regexsynthesiser.simplification.RegexSimplifier;
import com.owenjg.regexsynthesiser.validation.ExampleValidator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexSynthesiser {
    private volatile boolean cancelRequested = false;
    private ProgressCallback progressCallback;
    private final PrefixTreeBuilder prefixTreeBuilder;
    private final DFAMinimiser dfaMinimiser;
    private final StateEliminationAlgorithm stateElimination;
    private final RegexSimplifier regexSimplifier;
    private final ExampleValidator exampleValidator;
    @FXML
    private Label currentStatusLabel;


    public RegexSynthesiser(Label statusLabel) {  // Modified constructor
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
            System.out.println("Positive examples: " + positiveExamples);

            updateStatus("Building prefix tree...");
            DFA prefixTree = prefixTreeBuilder.buildPrefixTree(positiveExamples);
            System.out.println("Prefix tree states: " + prefixTree.getStates().size());
            System.out.println("Prefix tree transitions: " + prefixTree.getTransitions().size());

            updateStatus("Minimizing automaton...");
            DFA minimizedDfa = dfaMinimiser.minimise(prefixTree, negativeExamples);
            System.out.println("Minimized DFA states: " + minimizedDfa.getStates().size());
            System.out.println("Minimized DFA transitions: " + minimizedDfa.getTransitions().size());

            updateStatus("Converting to regex...");
            String regex = stateElimination.convertToRegex(minimizedDfa);
            System.out.println("Raw regex: " + regex);

            String simplifiedRegex = regexSimplifier.simplify(regex);
            System.out.println("Simplified regex: " + simplifiedRegex);

            if (progressCallback != null) {
                progressCallback.onComplete(simplifiedRegex);
            }
        } catch (Exception e) {
            e.printStackTrace();
            handleError(e.getMessage());
        }
    }

    // Add this helper method to safely update the status label
    private void updateStatus(String status) {
        if (currentStatusLabel != null) {
            Platform.runLater(() -> currentStatusLabel.setText(status));
        }
    }


    private boolean checkCancellationAndUpdateProgress(long startTime, String message) {
        try {
            // Add a small delay to make progress visible
            Thread.sleep(500);

            if (cancelRequested) {
                if (progressCallback != null) {
                    progressCallback.onCancel();
                }
                return true;
            }

            if (progressCallback != null) {
                long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                progressCallback.onProgress(elapsedTime, message);
            }
            return false;
        } catch (InterruptedException e) {
            if (progressCallback != null) {
                progressCallback.onCancel();
            }
            return true;
        }
    }

    private void validateInputExamples(List<String> positiveExamples, List<String> negativeExamples)
            throws RegexSynthesisException {
        if (positiveExamples == null || positiveExamples.isEmpty()) {
            throw new RegexSynthesisException("At least one positive example is required");
        }

        for (String example : positiveExamples) {
            if (example == null || example.isEmpty()) {
                throw new RegexSynthesisException("Positive examples cannot be null or empty");
            }
        }

        if (negativeExamples != null) {
            for (String example : negativeExamples) {
                if (example == null) {
                    throw new RegexSynthesisException("Negative examples cannot be null");
                }
            }

            // Check for overlap between positive and negative examples
            Set<String> positiveSet = new HashSet<>(positiveExamples);
            Set<String> negativeSet = new HashSet<>(negativeExamples);
            Set<String> intersection = new HashSet<>(positiveSet);
            intersection.retainAll(negativeSet);

            if (!intersection.isEmpty()) {
                throw new RegexSynthesisException(
                        "Found overlapping examples in positive and negative sets: " + intersection);
            }
        }
    }

    private void validateResult(String regex, List<String> positiveExamples,
                                List<String> negativeExamples) throws RegexSynthesisException {
        try {
            Pattern pattern = Pattern.compile(regex);

            for (String example : positiveExamples) {
                if (!pattern.matcher(example).matches()) {
                    throw new RegexSynthesisException(
                            "Generated regex fails to match positive example: " + example);
                }
            }

            if (negativeExamples != null) {
                for (String example : negativeExamples) {
                    if (pattern.matcher(example).matches()) {
                        throw new RegexSynthesisException(
                                "Generated regex incorrectly matches negative example: " + example);
                    }
                }
            }
        } catch (PatternSyntaxException e) {
            throw new RegexSynthesisException("Generated invalid regex pattern: " + e.getMessage());
        }
    }

    private void handleError(String errorMessage) {
        if (progressCallback != null) {
            progressCallback.onError("Error during synthesis: " + errorMessage);
        }
    }

    // Method to set progress callback
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    // Method to cancel the generation
    public void cancelGeneration() {
        cancelRequested = true;
    }
}