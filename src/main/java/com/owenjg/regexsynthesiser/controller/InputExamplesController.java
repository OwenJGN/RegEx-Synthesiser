package com.owenjg.regexsynthesiser.controller;

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class InputExamplesController {

    @FXML private TextField positiveExamplesField;
    @FXML private TextField negativeExamplesField;
    @FXML private TextField filePathField;

    @FXML private Button generateButton;
    @FXML private Button selectFileButton; // Button for file selection
    @FXML private Label statusLabel; // Label to display status message and elapsed time

    private Boolean isGenerating = false;
    private Boolean cancelRequested = false;
    private Stage stage;
    private long startTime; // To track the start time for elapsed time calculation
    private String generatedRegEx;

    @FXML
    protected void initialize() {
        generateButton.setText("Generate");
        statusLabel.setText(""); // Initially, no status message
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
            filePathField.setText(selectedFile.getAbsolutePath());
        }
        else {
            statusLabel.setText("Error getting import text file");
        }
    }

    @FXML
    protected void onGenerateButtonClick() throws IOException {
        if (isGenerating) {
            // If already generating, cancel the generation
            cancelRequested = true;
            generateButton.setText("Generate");
            statusLabel.setText("Generation canceled.");
            selectFileButton.setDisable(false);
            isGenerating = false;
        } else {
            // Start generating regex
            isGenerating = true;
            cancelRequested = false;
            generateButton.setText("Cancel");
            statusLabel.setText("Generating...");
            selectFileButton.setDisable(true);

            // Record the start time to calculate elapsed time
            startTime = System.currentTimeMillis();

            // Run the regex generation in a separate thread to avoid blocking the UI
            new Thread(() -> {
                try {
                    // Get examples from text fields
                    String positiveExamples = positiveExamplesField.getText();
                    String negativeExamples = negativeExamplesField.getText();

                    // If file path is provided, read examples from the file
                    if (!filePathField.getText().isEmpty()) {
                        String filePath = filePathField.getText();
                        List<String> examplesFromFile = Files.readAllLines(Paths.get(filePath));
                        // Add examples to the fields if the file has content
                        positiveExamples = examplesFromFile.get(0); // Assuming first line is for positive
                        negativeExamples = examplesFromFile.size() > 1 ? examplesFromFile.get(1) : "";
                    }

                    // Simulate a time-consuming regex generation process (can be replaced with actual logic)
                    for (int i = 0; i < 10; i++) {
                        if (cancelRequested) {
                            // If cancel is requested, stop the generation
                            break;
                        }

                        // Simulate regex generation work (replace with actual logic)
                        Thread.sleep(500);  // Simulate delay
                        generatedRegEx = "A REGEX";
                        // Update the elapsed time and status every second
                        if (!cancelRequested) {
                            long elapsedTime = (System.currentTimeMillis() - startTime) / 1000; // Elapsed time in seconds
                            Platform.runLater(() -> {
                                statusLabel.setText("Generating... Elapsed time: " + elapsedTime + " seconds");
                            });
                        }
                    }

                    // Once finished (or cancelled), update UI on the main thread
                    String finalPositiveExamples = positiveExamples;
                    String finalNegativeExamples = negativeExamples;
                    Platform.runLater(() -> {
                        if (!cancelRequested) {
                            // Proceed to the next scene to display the generated regular expression
                            try {
                                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/owenjg/regexsynthesiser/display-regex-view.fxml"));
                                VBox thirdRoot = fxmlLoader.load();

                                // Pass the examples to the third controller
                                DisplayRegexController thirdController = fxmlLoader.getController();
                                thirdController.setValues(finalPositiveExamples, finalNegativeExamples, String.valueOf((System.currentTimeMillis() - startTime) / 1000), generatedRegEx);
                                thirdController.setStage(stage);

                                // Create and set the scene for the third form
                                Scene thirdScene = new Scene(thirdRoot, 800, 600);
                                stage.setScene(thirdScene);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        // Reset the button and state after process ends (on JavaFX thread)
                        isGenerating = false;
                        generateButton.setText("Generate");
                        statusLabel.setText("Generation complete.");
                    });
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }).start(); // Start the thread
        }
    }
}
