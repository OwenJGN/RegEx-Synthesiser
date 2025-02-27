package com.owenjg.regexsynthesiser.synthesis;

import com.owenjg.regexsynthesiser.synthesis.RegexSynthesiser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RegexSynthesiserController {
    @FXML
    private TextArea positiveExamplesArea;
    @FXML
    private TextArea negativeExamplesArea;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField patternAnalyzerRegexField;
    @FXML
    private TextField dfaRegexField;
    @FXML
    private Button generateButton;

    private RegexSynthesiser regexSynthesiser;

    @FXML
    public void initialize() {
        regexSynthesiser = new RegexSynthesiser(statusLabel);
        regexSynthesiser.setProgressCallback(new RegexSynthesiser.ProgressCallback() {
            @Override
            public void onProgress(long elapsedTime, String status) {
                Platform.runLater(() -> statusLabel.setText(status));
            }

            @Override
            public void onComplete(String generatedRegex) {
                Platform.runLater(() -> {
                    statusLabel.setText("Regex generation complete");

                    // Parse the combined results
                    String patternAnalyzerRegex = "N/A";
                    String dfaRegex = "N/A";

                    if (generatedRegex.contains("Pattern Analyzer:")) {
                        String[] parts = generatedRegex.split("\n");

                        for (String part : parts) {
                            if (part.startsWith("Pattern Analyzer:")) {
                                patternAnalyzerRegex = part.substring("Pattern Analyzer:".length()).trim();
                            } else if (part.startsWith("DFA-based:")) {
                                dfaRegex = part.substring("DFA-based:".length()).trim();
                            }
                        }
                    } else {
                        // Handle legacy format (just in case)
                        patternAnalyzerRegex = generatedRegex;
                        dfaRegex = generatedRegex;
                    }

                    // Display both regexes
                    patternAnalyzerRegexField.setText(patternAnalyzerRegex);
                    dfaRegexField.setText(dfaRegex);

                    // Re-enable the generate button
                    generateButton.setDisable(false);
                });
            }

            @Override
            public void onCancel() {
                Platform.runLater(() -> {
                    statusLabel.setText("Regex generation cancelled");
                    generateButton.setDisable(false);
                });
            }

            @Override
            public void onError(String errorMessage) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + errorMessage);
                    generateButton.setDisable(false);
                });
            }
        });
    }

    @FXML
    public void handleGenerateRegex() {
        // Disable the generate button during generation
        generateButton.setDisable(true);

        // Clear previous results
        patternAnalyzerRegexField.clear();
        dfaRegexField.clear();

        // Get examples from text areas
        List<String> positiveExamples = Arrays.stream(positiveExamplesArea.getText().split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<String> negativeExamples = Arrays.stream(negativeExamplesArea.getText().split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // Start regex generation in a background thread
        new Thread(() -> {
            regexSynthesiser.synthesise(positiveExamples, negativeExamples);
        }).start();
    }

    @FXML
    public void handleCancelGeneration() {
        regexSynthesiser.cancelGeneration();
    }
}