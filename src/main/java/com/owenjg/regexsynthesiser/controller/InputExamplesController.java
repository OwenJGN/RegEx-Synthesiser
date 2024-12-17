package com.owenjg.regexsynthesiser.controller;

import com.owenjg.regexsynthesiser.synthesis.Examples;
import com.owenjg.regexsynthesiser.synthesis.RegexSynthesiser;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InputExamplesController {

    @FXML
    private TextArea positiveExamplesField;
    @FXML
    private TextArea negativeExamplesField;

    @FXML
    private Button generateButtonInput;

    @FXML
    private Button generateButtonFile;
    @FXML
    private Button selectFileButton; // Button for file selection
    @FXML
    private Label statusLabel; // Label to display status message and elapsed time
    @FXML
    private Label fileLabel;

    private Boolean isGenerating = false;
    private Stage stage;
    private long startTime; // To track the start time for elapsed time calculation
    private String generatedRegEx;
    private String filePath;
    private RegexSynthesiser synthesiser;

    @FXML
    protected void initialize() {
        generateButtonFile.setText("Generate from File");
        generateButtonInput.setText("Generate from Input");
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

        if(filePath != null){
            filePath = null;
            fileLabel.setText("Import examples from a text file:");
            selectFileButton.setText("Select File");
            return;
        }
        // Create a new FileChooser to select a file
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        // Show the file chooser and get the selected file
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            // If a file is selected, set the file path in the text field
            filePath = selectedFile.getAbsolutePath();
            fileLabel.setText("Selected the file: " + selectedFile.getName());
            selectFileButton.setText("Remove File");
        } else {
            statusLabel.setText("Error getting import text file");
        }
    }

    @FXML
    protected void onGenerateButtonFileClick() throws IOException {
        Examples examples = new Examples();
        List<List<String>> exs;
        List<String> positiveExamples;
        List<String> negativeExamples;
        if (this.filePath != null && !this.filePath.isEmpty()) {
            if(examples.validateFileContent(filePath)){
                exs = examples.splitPositiveAndNegativeFile(filePath);
            }
            else {
                statusLabel.setText("ERROR: Invalid File");
                return;
            }

            positiveExamples = exs.get(0);
            negativeExamples = exs.get(1);

            if(positiveExamples.isEmpty() && negativeExamples.isEmpty()){
                statusLabel.setText("Both positive and negative examples are empty!");
                return;
            }
        }
        else {
            statusLabel.setText("ERROR: No File Selected");
            return;
        }



        if (isGenerating) {
            synthesiser.cancelGeneration();
            cancelGeneration(generateButtonFile, "Generate from File");
            generateButtonInput.setDisable(false);
        } else {
            generateButtonInput.setDisable(true);
            generateButtonFile.setText("Cancel");
            startGeneration(generateButtonFile, positiveExamples, negativeExamples,"Generate from File");
        }
    }
    @FXML
    protected void onGenerateButtonInputClick() throws IOException {
        Examples examples = new Examples();
        List<List<String>> exs;

        exs = examples.splitPositiveAndNegativeInput(
                positiveExamplesField.getText(),
                negativeExamplesField.getText()
        );

        List<String> positiveExamples = exs.get(0);
        List<String> negativeExamples = exs.get(1);

        if(positiveExamples.isEmpty() && negativeExamples.isEmpty()){
            statusLabel.setText("Both positive and negative examples are empty!");
            return;
        }


        if (isGenerating) {
            synthesiser.cancelGeneration();
            cancelGeneration(generateButtonInput, "Generate from Input");
            generateButtonFile.setDisable(false);
        } else {
            generateButtonFile.setDisable(true);
            generateButtonInput.setText("Cancel");
            startGeneration(generateButtonInput, positiveExamples, negativeExamples, "Generate from Input");
        }
    }

    private void startGeneration(Button genButton, List<String> positiveExamples, List<String> negativeExamples, String buttonText) throws IOException {

        isGenerating = true;
        Boolean cancelRequested = false;
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

                    resetGenerationState(genButton, buttonText);
                });
            }

            @Override
            public void onCancel() {
                Platform.runLater(() -> {
                    cancelGeneration(genButton, buttonText);
                });
            }
        });

        // Run the generation in a separate thread
        new Thread(() -> {
            try {
                // Start the regex generation process
                synthesiser.synthesise(positiveExamples, negativeExamples);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void cancelGeneration(Button genButton, String buttonText) {
        genButton.setText(buttonText);
        statusLabel.setText("Generation canceled.");
        selectFileButton.setDisable(false);
        isGenerating = false;
    }

    private void resetGenerationState(Button genButton, String buttonText) {
        isGenerating = false;
        genButton.setText(buttonText);
        statusLabel.setText("Generation complete.");
        selectFileButton.setDisable(false);
    }


}
