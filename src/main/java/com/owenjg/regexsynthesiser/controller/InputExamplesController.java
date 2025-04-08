package com.owenjg.regexsynthesiser.controller;

import com.owenjg.regexsynthesiser.synthesis.RegexSynthesiser;
import com.owenjg.regexsynthesiser.validation.Examples;
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

/**
 * Controller for the input examples view where users provide positive and negative examples
 * for regex synthesis.
 *
 * This controller allows users to input examples directly through text fields or
 * load them from a file, then initiates the regex generation process.
 */
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

    /**
     * Initialises the controller after its root element has been processed.
     * Sets up the buttons and creates a new RegexSynthesiser instance.
     */
    @FXML
    protected void initialize() {
        setupButtons();
        synthesiser = new RegexSynthesiser(currentStatusLabel);
    }

    /**
     * Sets up the initial button text values.
     */
    private void setupButtons() {
        generateButtonFile.setText("Generate from File");
        generateButtonInput.setText("Generate from Input");
        statusLabel.setText("");
    }

    /**
     * Sets the main application stage for this controller.
     *
     * @param stage The primary stage for this JavaFX application
     */
    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setTitle("Enter Examples");
    }

    /**
     * Handles the click event for the Select File button.
     * Allows users to select a file with examples or clear a previous selection.
     *
     * @throws IOException If there is an error accessing the file
     */
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

    /**
     * Clears the current file selection and resets related UI elements.
     */
    private void clearFileSelection() {
        filePath = null;
        fileLabel.setText("Import examples from a text file:");
        selectFileButton.setText("Select File");
    }

    /**
     * Displays a file chooser dialogue for selecting a text file.
     *
     * @return The selected file or null if cancelled
     */
    private File showFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        return fileChooser.showOpenDialog(stage);
    }

    /**
     * Updates the UI to reflect the selected file.
     *
     * @param selectedFile The file selected by the user
     */
    private void updateFileSelection(File selectedFile) {
        filePath = selectedFile.getAbsolutePath();
        fileLabel.setText("Selected the file: " + selectedFile.getName());
        selectFileButton.setText("Remove File");
    }

    /**
     * Handles the click event for the Generate from File button.
     * Initiates regex generation from examples in the selected file.
     *
     * @throws IOException If there is an error reading the file
     */
    @FXML
    protected void onGenerateButtonFileClick() throws IOException {
        processGeneration(true);
    }

    /**
     * Handles the click event for the Generate from Input button.
     * Initiates regex generation from examples provided in the text areas.
     *
     * @throws IOException If there is an error processing the input
     */
    @FXML
    protected void onGenerateButtonInputClick() throws IOException {
        processGeneration(false);
    }

    /**
     * Processes the generation request based on the source of examples.
     *
     * @param isFileMode True if examples come from a file, false if from text inputs
     * @throws IOException If there is an error accessing the examples
     */
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

    /**
     * Cancels the current generation process and resets UI.
     *
     * @param isFileMode True if working in file mode, false for input mode
     */
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

    /**
     * Starts a new generation process based on the provided examples.
     *
     * @param isFileMode True if working in file mode, false for input mode
     * @param positiveExamples List of positive examples for regex synthesis
     * @param negativeExamples List of negative examples for regex synthesis
     */
    private void startNewGeneration(boolean isFileMode, List<String> positiveExamples, List<String> negativeExamples){
        Button activeButton = isFileMode ? generateButtonFile : generateButtonInput;
        Button disabledButton = isFileMode ? generateButtonInput : generateButtonFile;
        String buttonText = isFileMode ? "Generate from File" : "Generate from Input";

        disabledButton.setDisable(true);
        activeButton.setText("Cancel");
        startGeneration(activeButton, positiveExamples, negativeExamples, buttonText);
    }

    /**
     * Initiates the generation process with the provided examples and configures callbacks.
     *
     * @param genButton The button that initiated the generation
     * @param positiveExamples List of positive examples for regex synthesis
     * @param negativeExamples List of negative examples for regex synthesis
     * @param buttonText The original text of the generation button
     */
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

    /**
     * Resets the generation state and UI elements.
     *
     * @param genButton The button that initiated the generation
     * @param buttonText The original text of the generation button
     */
    private void resetGenerationState(Button genButton, String buttonText) {
        isGenerating = false;
        genButton.setText(buttonText);
        selectFileButton.setDisable(false);
    }

    /**
     * Resets the generation state, UI elements, and displays a status message.
     *
     * @param genButton The button that initiated the generation
     * @param buttonText The original text of the generation button
     * @param message The status message to display
     */
    private void resetGenerationStateWithMessage(Button genButton, String buttonText, String message) {
        isGenerating = false;
        genButton.setText(buttonText);
        selectFileButton.setDisable(false);
        updateStatusLabel(message);
    }

    /**
     * Creates a progress callback to handle synthesis events.
     *
     * @param genButton The button that initiated the generation
     * @param buttonText The original text of the generation button
     * @return A configured ProgressCallback for the synthesiser
     */
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

    /**
     * Navigates to the regex display scene to show the generated results.
     *
     * @param generatedRegex The regex string(s) generated by the synthesiser
     * @throws IOException If there is an error loading the next view
     */
    private void navigateToRegexDisplayScene(String generatedRegex) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/owenjg/regexsynthesiser/display-regex-view.fxml"));
        VBox thirdRoot = fxmlLoader.load();

        DisplayRegexController thirdController = fxmlLoader.getController();
        thirdController.setValues(String.valueOf((System.currentTimeMillis() - startTime) / 1000), generatedRegex, examples);
        thirdController.setStage(stage);

        Scene thirdScene = new Scene(thirdRoot, 850, 775);
        thirdScene.getStylesheets().add(getClass().getResource("/com/owenjg/regexsynthesiser/styles.css").toExternalForm());

        stage.setScene(thirdScene);
    }

    /**
     * Cancels the current generation process and updates the UI.
     *
     * @param genButton The button that initiated the generation
     * @param buttonText The original text of the generation button
     */
    private void cancelGeneration(Button genButton, String buttonText) {
        genButton.setText(buttonText);
        updateStatusLabel("Generation cancelled.");
        selectFileButton.setDisable(false);
        isGenerating = false;
    }

    /**
     * Updates the status label with a message and applies appropriate styling.
     *
     * @param message The message to display in the status label
     */
    private void updateStatusLabel(String message) {
        // Clear previous styles
        statusLabel.getStyleClass().remove("error");

        // Check if message starts with "ERROR:" or contains specific error keywords
        if (message.toUpperCase().startsWith("ERROR:") ||
                message.contains("Generation cancelled.") ||
                message.contains("empty")) {
            // Add error style class
            statusLabel.getStyleClass().add("error");
        }

        // Set the message
        statusLabel.setText(message);
    }
}