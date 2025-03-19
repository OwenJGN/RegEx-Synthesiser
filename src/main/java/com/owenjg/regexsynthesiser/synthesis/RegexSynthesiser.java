package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFA;
import com.owenjg.regexsynthesiser.dfa.DFABuilder;
import com.owenjg.regexsynthesiser.exceptions.RegexSynthesisException;
import com.owenjg.regexsynthesiser.minimisation.DFAMinimiser;
import com.owenjg.regexsynthesiser.simplification.RegexSimplifier;
import com.owenjg.regexsynthesiser.validation.ExampleValidator;
import com.owenjg.regexsynthesiser.validation.RegexComparator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.*;

public class RegexSynthesiser {
    private volatile boolean cancelRequested = false;
    private ProgressCallback progressCallback;
    private final DFAMinimiser dfaMinimiser;
    private final StateEliminationAlgorithm stateElimination;
    private final RegexSimplifier regexSimplifier;
    private final ExampleValidator exampleValidator;
    private final RegexGeneralizer patternGeneralizer;
    private final DFABuilder dfaBuilder;
    private final RegexComparator regexComparator;
    private List<String> positiveExamples;
    private List<String> negativeExamples;

    private final PatternAnalyzer patternAnalyzer;
    @FXML
    private Label currentStatusLabel;


    public RegexSynthesiser(Label statusLabel) {
        this.regexComparator = new RegexComparator();
        this.positiveExamples = new ArrayList<>();
        this.negativeExamples = new ArrayList<>();
        this.dfaBuilder = new DFABuilder();
        this.patternAnalyzer = new PatternAnalyzer();
        this.patternGeneralizer = new RegexGeneralizer();
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

            this.positiveExamples = positiveExamples;
            this.negativeExamples = negativeExamples;

            // Generate regex from pattern analyzer
            String analyzerRegex = createRegexFromAnalyser();

            // Generate regex from DFA
            String dfaRegex = createRegexFromDFA();

            // Display both regexes
            System.out.println("Pattern Analyzer Regex: " + analyzerRegex);
            System.out.println("DFA-based Regex: " + dfaRegex);

            // Validate both regexes
            boolean analyzerValid = analyzerRegex != null &&
                    exampleValidator.validateExamples(analyzerRegex, positiveExamples, negativeExamples);
            boolean dfaValid = dfaRegex != null &&
                    exampleValidator.validateExamples(dfaRegex, positiveExamples, negativeExamples);

            System.out.println("Validation results:");
            System.out.println("Pattern Analyzer regex valid: " + analyzerValid);
            System.out.println("DFA-based regex valid: " + dfaValid);

            // Compare the regexes if both are valid
            if (analyzerValid && dfaValid) {
                updateStatus("Comparing regex patterns...");
                String comparison = RegexComparator.compareRegexes(analyzerRegex, dfaRegex);
                System.out.println(comparison);

                // Update the UI with comparison info
                if (progressCallback != null) {
                    progressCallback.onComplete(comparison);
                }
            } else {
                // Format a simple string containing both regexes and validation results only
                StringBuilder resultsBuilder = new StringBuilder();
                resultsBuilder.append("GENERATED REGEXES\n\n");

                if (progressCallback != null) {
                    progressCallback.onComplete(resultsBuilder.toString());
                }

                // Log any validation issues
                if (!analyzerValid && !dfaValid) {
                    System.out.println("Warning: Generated regexes failed validation");
                    if (progressCallback != null) {
                        progressCallback.onError("Both approaches failed to generate a valid regex");
                    }
                }
            }

            updateStatus("Synthesis complete!");
        } catch (Exception e) {
            System.err.println("Error during synthesis: " + e.getMessage());
            e.printStackTrace();
            if (progressCallback != null) {
                progressCallback.onError("Synthesis failed: " + e.getMessage());
            }
        }
    }

    private String createRegexFromAnalyser() {
        updateStatus("Analyzing patterns in examples...");
        String regex = patternAnalyzer.generalizePattern(positiveExamples, negativeExamples);
        System.out.println("Initial pattern analyzer result: " + regex);

        String simplifiedRegex = RegexSimplifier.simplify(regex);
        System.out.println("Simplified pattern analyzer regex: " + simplifiedRegex);
        return simplifiedRegex;
    }

    private String createRegexFromDFA() {
        updateStatus("Building DFA from examples...");
        DFA dfa = dfaBuilder.buildDFAFromExamples(positiveExamples, negativeExamples);

        updateStatus("Minimizing DFA...");
        DFA minimisedDFA = dfaMinimiser.minimizeDFA(dfa);

        updateStatus("Generating regex from DFA...");
        String regex = stateElimination.eliminateStates(minimisedDFA);
        System.out.println("Raw DFA-based regex: " + regex);

        updateStatus("Simplifying DFA-based regex...");
        String simplifiedRegex = RegexSimplifier.simplify(regex);
        System.out.println("Simplified DFA-based regex: " + simplifiedRegex);


        return simplifiedRegex;
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