package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.dfa.DFABuilder;
import com.owenjg.regexsynthesiser.minimisation.DFAMinimiser;
import com.owenjg.regexsynthesiser.simplification.RegexSimplifier;
import com.owenjg.regexsynthesiser.validation.RegexComparator;
import com.owenjg.regexsynthesiser.dfa.DFA;
import com.owenjg.regexsynthesiser.exceptions.RegexSynthesisException;
import com.owenjg.regexsynthesiser.simplification.StateEliminationAlgorithm;
import com.owenjg.regexsynthesiser.validation.ExampleValidator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.*;

/**
 * The main synthesiser class for regular expressions. This class orchestrates the entire
 * process of generating regular expressions from positive and negative example strings.
 * It provides two approaches: one using pattern analysis heuristics and another using
 * DFA (Deterministic Finite Automaton) construction and minimisation techniques.
 */
public class RegexSynthesiser {
    private volatile boolean cancelRequested = false;
    private ProgressCallback progressCallback;
    private final DFAMinimiser dfaMinimiser;
    private final StateEliminationAlgorithm stateElimination;
    private final ExampleValidator exampleValidator;
    private final DFABuilder dfaBuilder;
    private List<String> positiveExamples;
    private List<String> negativeExamples;

    private final PatternAnalyser patternAnalyser;
    @FXML
    private Label currentStatusLabel;

    /**
     * Constructs a new RegexSynthesiser with a status label for UI updates.
     * Initialises all necessary components for regex synthesis.
     *
     * @param statusLabel The label used to display current status in the UI (can be null)
     */
    public RegexSynthesiser(Label statusLabel) {
        this.positiveExamples = new ArrayList<>();
        this.negativeExamples = new ArrayList<>();
        this.dfaBuilder = new DFABuilder();
        this.patternAnalyser = new PatternAnalyser();
        this.dfaMinimiser = new DFAMinimiser();
        this.stateElimination = new StateEliminationAlgorithm();
        this.exampleValidator = new ExampleValidator();
        this.currentStatusLabel = statusLabel;
    }

    /**
     * Interface for monitoring progress and receiving results of the synthesis process.
     * Provides callbacks for progress updates, completion, cancellation, and errors.
     */
    public interface ProgressCallback {
        /**
         * Called periodically to report progress of the synthesis operation.
         *
         * @param elapsedTime Time elapsed since the start of the operation (in seconds)
         * @param status Current status message describing the synthesis stage
         */
        void onProgress(long elapsedTime, String status);

        /**
         * Called when synthesis has successfully completed.
         *
         * @param generatedRegex The resulting regular expression(s)
         */
        void onComplete(String generatedRegex);

        /**
         * Called when the synthesis operation is cancelled by the user.
         */
        void onCancel();

        /**
         * Called when an error occurs during synthesis.
         *
         * @param errorMessage A description of the error that occurred
         */
        void onError(String errorMessage);
    }

    /**
     * The main entry point for regex synthesis. Takes positive and negative examples
     * and generates optimised regular expressions using two different approaches.
     *
     * @param positiveExamples List of strings that should match the pattern
     * @param negativeExamples List of strings that should NOT match the pattern
     */
    public void synthesise(List<String> positiveExamples, List<String> negativeExamples) {
        try {
            validateInputExamples(positiveExamples, negativeExamples);

            this.positiveExamples = positiveExamples;
            this.negativeExamples = negativeExamples;

            // Generate regex using pattern analysis approach
            String analyserRegex = createRegexFromAnalyser();

            // Generate regex using DFA-based approach
            String dfaRegex = createRegexFromDFA();

            // Validate both regexes against the provided examples
            boolean analyserValid = analyserRegex != null &&
                    exampleValidator.validateExamples(analyserRegex, positiveExamples, negativeExamples);
            boolean dfaValid = dfaRegex != null &&
                    exampleValidator.validateExamples(dfaRegex, positiveExamples, negativeExamples);

            // Mark invalid regexes clearly
            if (!analyserValid) {
                analyserRegex = "INVALID: " + analyserRegex;
            }
            if (!dfaValid) {
                dfaRegex = "INVALID: " + dfaRegex;
            }

            // Create comparison string with both regexes
            String comparison = RegexComparator.compareRegexes(analyserRegex, dfaRegex);
            updateStatus("Comparing regex patterns....");

            // Notify caller of completion with the results
            if (progressCallback != null) {
                progressCallback.onComplete(comparison);
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

    /**
     * Creates a regular expression using the pattern analysis approach.
     * This approach examines common patterns in the examples and builds a regex
     * based on identified patterns, without constructing a complete automaton.
     *
     * @return A regular expression that matches the positive examples
     */
    private String createRegexFromAnalyser() {
        updateStatus("Analysing patterns in examples...");
        String regex = patternAnalyser.generalisePattern(positiveExamples, negativeExamples);

        String simplifiedRegex = RegexSimplifier.simplify(regex);
        return simplifiedRegex;
    }

    /**
     * Creates a regular expression using the DFA-based approach.
     * This approach constructs a DFA from the examples, minimises it,
     * and then converts it to a regular expression.
     *
     * @return A regular expression derived from the DFA
     */
    private String createRegexFromDFA() {
        updateStatus("Building DFA from examples...");
        DFA dfa = dfaBuilder.buildDFAFromExamples(positiveExamples, negativeExamples);

        updateStatus("Minimising DFA...");
        DFA minimisedDFA = dfaMinimiser.minimiseDFA(dfa);

        updateStatus("Generating regex from DFA...");
        String regex = stateElimination.eliminateStates(minimisedDFA);

        updateStatus("Simplifying DFA-based regex...");
        String simplifiedRegex = RegexSimplifier.simplify(regex);

        return simplifiedRegex;
    }

    /**
     * Validates the provided examples to ensure they meet the minimum requirements.
     *
     * @param positiveExamples List of strings that should match the pattern
     * @param negativeExamples List of strings that should NOT match the pattern
     * @throws RegexSynthesisException If the examples do not meet requirements
     */
    private void validateInputExamples(List<String> positiveExamples, List<String> negativeExamples)
            throws RegexSynthesisException {
        if (positiveExamples == null || positiveExamples.isEmpty()) {
            throw new RegexSynthesisException("At least one positive example is required");
        }
    }

    /**
     * Updates the status label in the UI and logs to console.
     *
     * @param status The status message to display
     */
    private void updateStatus(String status) {
        System.out.println(status);
        if (currentStatusLabel != null) {
            Platform.runLater(() -> currentStatusLabel.setText(status));
        }
    }

    /**
     * Handles an error that occurred during synthesis.
     *
     * @param errorMessage The error message to report
     */
    private void handleError(String errorMessage) {
        if (progressCallback != null) {
            progressCallback.onError("Error during synthesis: " + errorMessage);
        }
    }

    /**
     * Sets the callback for progress and result notification.
     *
     * @param callback The callback to be notified of progress and results
     */
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    /**
     * Requests cancellation of the current synthesis operation.
     */
    public void cancelGeneration() {
        cancelRequested = true;
    }
}