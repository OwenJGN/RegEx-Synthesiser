package com.owenjg.regexsynthesiser.controller;

import com.owenjg.regexsynthesiser.synthesis.Examples;
import com.owenjg.regexsynthesiser.synthesis.RegexSynthesiser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class InputExamplesController {
    @FXML private TextArea positiveExamplesField;
    @FXML private TextArea negativeExamplesField;
    @FXML private Button generateButtonInput;
    @FXML private Button generateButtonFile;
    @FXML private Button selectFileButton;
    @FXML private Label statusLabel;
    @FXML private Label currentStatusLabel;
    @FXML private Label fileLabel;

    private Stage stage;
    private RegexSynthesiser synthesiser;
    private String filePath;
    private boolean isGenerating = false;
    private long startTime;
    private Examples examples;

    @FXML
    protected void initialize() {
        setupButtons();
        synthesiser = new RegexSynthesiser(currentStatusLabel);
    }

    private void setupButtons() {
        generateButtonFile.setText("Generate from File");
        generateButtonInput.setText("Generate from Input");
        statusLabel.setText("");
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setTitle("Enter Examples");
    }

    @FXML
    protected void onSelectFileButtonClick() {
        if (filePath != null) {
            clearFileSelection();
            return;
        }

        File selectedFile = showFileChooser();
        if (selectedFile != null) {
            updateFileSelection(selectedFile);
        }
    }

    private void clearFileSelection() {
        filePath = null;
        fileLabel.setText("Import examples from a text file:");
        selectFileButton.setText("Select File");
    }

    private File showFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        return fileChooser.showOpenDialog(stage);
    }

    private void updateFileSelection(File selectedFile) {
        filePath = selectedFile.getAbsolutePath();
        fileLabel.setText("Selected the file: " + selectedFile.getName());
        selectFileButton.setText("Remove File");
    }

    @FXML
    protected void onGenerateButtonFileClick() throws IOException {
        processGeneration(true);
    }

    @FXML
    protected void onGenerateButtonInputClick() throws IOException {
        processGeneration(false);
    }

    private void processGeneration(boolean isFileMode) throws IOException {
        examples = new Examples();
        List<String> positiveExamples;
        List<String> negativeExamples;

        // Retrieve examples based on mode
        if (isFileMode) {
            if (filePath == null || filePath.isEmpty()) {
                updateStatusLabel("ERROR: No File Selected");
                return;
            }

            if (!examples.validateFileContent(filePath)) {
                updateStatusLabel("ERROR: Invalid File");
                return;
            }

            List<List<String>> exs = examples.splitPositiveAndNegativeFile(filePath);
            positiveExamples = exs.get(0);
            negativeExamples = exs.get(1);
        } else {
            List<List<String>> exs = examples.splitPositiveAndNegativeInput(
                    positiveExamplesField.getText(),
                    negativeExamplesField.getText()
            );
            positiveExamples = exs.get(0);
            negativeExamples = exs.get(1);
        }

        // Validate examples
        if (positiveExamples.isEmpty() && negativeExamples.isEmpty()) {
            updateStatusLabel("ERROR: Both positive and negative examples are empty!");
            return;
        }

        // Toggle generation state
        if (isGenerating) {
            cancelCurrentGeneration(isFileMode);
        } else {
            startNewGeneration(isFileMode, positiveExamples, negativeExamples);
        }
    }

    private void cancelCurrentGeneration(boolean isFileMode) {
        synthesiser.cancelGeneration();

        if (isFileMode) {
            cancelGeneration(generateButtonFile, "Generate from File");
            generateButtonInput.setDisable(false);
        } else {
            cancelGeneration(generateButtonInput, "Generate from Input");
            generateButtonFile.setDisable(false);
        }
    }

    private void startNewGeneration(boolean isFileMode, List<String> positiveExamples, List<String> negativeExamples){
        Button activeButton = isFileMode ? generateButtonFile : generateButtonInput;
        Button disabledButton = isFileMode ? generateButtonInput : generateButtonFile;
        String buttonText = isFileMode ? "Generate from File" : "Generate from Input";

        disabledButton.setDisable(true);
        activeButton.setText("Cancel");
        startGeneration(activeButton, positiveExamples, negativeExamples, buttonText);
    }

    private void startGeneration(Button genButton, List<String> positiveExamples, List<String> negativeExamples, String buttonText) {
        isGenerating = true;
        selectFileButton.setDisable(true);
        startTime = System.currentTimeMillis();

        synthesiser.setProgressCallback(createProgressCallback(genButton, buttonText));

        new Thread(() -> {
            try {
                synthesiser.synthesise(positiveExamples, negativeExamples);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void resetGenerationState(Button genButton, String buttonText) {
        isGenerating = false;
        genButton.setText(buttonText);
        selectFileButton.setDisable(false);
    }

    private void resetGenerationStateWithMessage(Button genButton, String buttonText, String message) {
        isGenerating = false;
        genButton.setText(buttonText);
        selectFileButton.setDisable(false);
        updateStatusLabel(message);
    }

    private RegexSynthesiser.ProgressCallback createProgressCallback(Button genButton, String buttonText) {
        return new RegexSynthesiser.ProgressCallback() {
            @Override
            public void onProgress(long elapsedTime, String status) {
                Platform.runLater(() ->
                        updateStatusLabel("Generating... Elapsed time: " + elapsedTime + " seconds")
                );
            }

            @Override
            public void onComplete(String generatedRegex) {
                Platform.runLater(() -> {
                    try {
                        navigateToRegexDisplayScene(generatedRegex);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    resetGenerationStateWithMessage(genButton, buttonText, "Generation complete.");
                });
            }

            @Override
            public void onCancel() {
                Platform.runLater(() ->
                        cancelGeneration(genButton, buttonText)
                );
            }

            @Override
            public void onError(String errorMessage) {
                Platform.runLater(() -> {
                    resetGenerationState(genButton, buttonText);
                    updateStatusLabel("ERROR: " + errorMessage);
                    generateButtonInput.setDisable(false);
                    generateButtonFile.setDisable(false);
                });
            }
        };
    }

    private void navigateToRegexDisplayScene(String generatedRegex) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/owenjg/regexsynthesiser/display-regex-view.fxml"));
        VBox thirdRoot = fxmlLoader.load();

        DisplayRegexController thirdController = fxmlLoader.getController();
        thirdController.setValues(String.valueOf((System.currentTimeMillis() - startTime) / 1000), generatedRegex, examples);
        thirdController.setStage(stage);

        Scene thirdScene = new Scene(thirdRoot, 800, 600);
        thirdScene.getStylesheets().add(getClass().getResource("/com/owenjg/regexsynthesiser/styles.css").toExternalForm());

        stage.setScene(thirdScene);
    }

    private void cancelGeneration(Button genButton, String buttonText) {
        genButton.setText(buttonText);
        updateStatusLabel("Generation canceled.");
        selectFileButton.setDisable(false);
        isGenerating = false;
    }


    private void updateStatusLabel(String message) {
        // Clear previous styles
        statusLabel.getStyleClass().remove("error");

        // Check if message starts with "ERROR:" or contains specific error keywords
        if (message.toUpperCase().startsWith("ERROR:") ||
                message.contains("Generation canceled.") ||
                message.contains("empty")) {
            // Add error style class
            statusLabel.getStyleClass().add("error");
        }

        // Set the message
        statusLabel.setText(message);
    }

}