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

public class RegexSynthesiser {
    private volatile boolean cancelRequested = false;
    private ProgressCallback progressCallback;
    private final PrefixTreeBuilder prefixTreeBuilder;
    private final DFAMinimiser dfaMinimiser;
    private final StateEliminationAlgorithm stateElimination;
    private final RegexSimplifier regexSimplifier;
    private final ExampleValidator exampleValidator;
    private final RegexGeneralizer patternGeneralizer;

    private final PatternAnalyzer patternAnalyzer;
    @FXML
    private Label currentStatusLabel;


    public RegexSynthesiser(Label statusLabel) {
        this.patternAnalyzer = new PatternAnalyzer();
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

            // Step 1: Generate initial pattern from examples
            updateStatus("Analyzing patterns in examples...");
            String initialPattern = patternAnalyzer.generalizePattern(positiveExamples, negativeExamples);
            System.out.println("Initial pattern: " + initialPattern);

            // Step 2: Create DFA from the pattern
            updateStatus("Converting pattern to DFA...");
            DFA patternDFA = prefixTreeBuilder.buildPrefixTree(Collections.singletonList(initialPattern));

            // Step 3: Minimize DFA
            updateStatus("Minimizing DFA...");
            DFA minimizedDFA = dfaMinimiser.minimizeDFA(patternDFA);

            // Step 5: Convert back to regex
            updateStatus("Converting DFA back to regex...");
            String regex = stateElimination.eliminateStates(minimizedDFA);
            System.out.println("Raw regex: " + regex);

            // Step 6: Simplify final regex
            updateStatus("Simplifying final regex...");
            String simplifiedRegex = RegexSimplifier.simplify(regex);
            System.out.println("Simplified regex: " + simplifiedRegex);

            // Validate final result
            updateStatus("Validating regex...");
            boolean isValid = exampleValidator.validateExamples(simplifiedRegex, positiveExamples, negativeExamples);

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