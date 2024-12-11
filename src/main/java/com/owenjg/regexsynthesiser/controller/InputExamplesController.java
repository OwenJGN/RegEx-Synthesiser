package com.owenjg.regexsynthesiser.controller;

import com.owenjg.regexsynthesiser.synthesis.Examples;
import com.owenjg.regexsynthesiser.synthesis.RegexSynthesiser;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class InputExamplesController {

    @FXML
    private TextField positiveExamplesField;
    @FXML
    private TextField negativeExamplesField;

    @FXML
    private Button generateButton;
    @FXML
    private Button selectFileButton; // Button for file selection
    @FXML
    private Label statusLabel; // Label to display status message and elapsed time

    private Boolean isGenerating = false;
    private Boolean cancelRequested = false;
    private Stage stage;
    private long startTime; // To track the start time for elapsed time calculation
    private String generatedRegEx;
    private String filePath;
    private RegexSynthesiser synthesiser;

    @FXML
    protected void initialize() {
        generateButton.setText("Generate");
        statusLabel.setText(""); // Initially, no status message
        synthesiser = new RegexSynthesiser();
    }

    // Method to set the main stage
    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setTitle("Enter Examples");
    }

    // New method to handle file selection
    @FXML
    protected void onSelectFileButtonClick() {
        // Create a new FileChooser to select a file
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        // Show the file chooser and get the selected file
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            // If a file is selected, set the file path in the text field
            filePath = selectedFile.getAbsolutePath();
        } else {
            filePath = "empty";
            statusLabel.setText("Error getting import text file");
        }
    }

    @FXML
    protected void onGenerateButtonClick() throws IOException {
        if (isGenerating) {
            synthesiser.cancelGeneration();
            cancelGeneration();
        } else {
            startGeneration();
        }
    }

    private void startGeneration() {
        isGenerating = true;
        cancelRequested = false;
        generateButton.setText("Cancel");
        selectFileButton.setDisable(true);

        // Record the start time to calculate elapsed time
        startTime = System.currentTimeMillis();

        // Create and set progress callback
        synthesiser.setProgressCallback(new RegexSynthesiser.ProgressCallback() {
            @Override
            public void onProgress(long elapsedTime, String status) {
                Platform.runLater(() -> {
                    statusLabel.setText("Generating... Elapsed time: " + elapsedTime + " seconds");
                });
            }

            @Override
            public void onComplete(String generatedRegex) {
                Platform.runLater(() -> {
                    generatedRegEx = generatedRegex;
                    try {
                        // Load the next scene to display the generated regex
                        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/owenjg/regexsynthesiser/display-regex-view.fxml"));
                        VBox thirdRoot = fxmlLoader.load();

                        DisplayRegexController thirdController = fxmlLoader.getController();
                        thirdController.setValues(String.valueOf((System.currentTimeMillis() - startTime) / 1000), generatedRegEx);
                        thirdController.setStage(stage);

                        Scene thirdScene = new Scene(thirdRoot, 800, 600);
                        stage.setScene(thirdScene);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    resetGenerationState();
                });
            }

            @Override
            public void onCancel() {
                Platform.runLater(() -> {
                    cancelGeneration();
                });
            }
        });

        // Run the generation in a separate thread
        new Thread(() -> {
            try {
                Examples examples = new Examples();
                List<List<String>> exs = examples.splitPositiveAndNegativeInput(
                        positiveExamplesField.getText(),
                        negativeExamplesField.getText()
                );

                // If file path is provided, read examples from the file
                if (!filePath.equals("empty")) {
                    exs = examples.splitPositiveAndNegativeFile(filePath);
                }

                List<String> positiveExamples = exs.get(0);
                List<String> negativeExamples = exs.get(1);

                // Start the regex generation process
                synthesiser.synthesise(positiveExamples, negativeExamples);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void cancelGeneration() {
        generateButton.setText("Generate");
        statusLabel.setText("Generation canceled.");
        selectFileButton.setDisable(false);
        isGenerating = false;
    }

    private void resetGenerationState() {
        isGenerating = false;
        generateButton.setText("Generate");
        statusLabel.setText("Generation complete.");
        selectFileButton.setDisable(false);
    }
}
