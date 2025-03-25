package com.owenjg.regexsynthesiser.controller;

import com.owenjg.regexsynthesiser.validation.Examples;
import com.owenjg.regexsynthesiser.validation.RegexComparator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.awt.Toolkit;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class DisplayRegexController {

    @FXML private Label analyzerRegexLabel; // Label to display the pattern analyzer regex
    @FXML private Label dfaRegexLabel;      // Label to display the DFA-based regex
    @FXML private Label titleText;         // Label for the title
    @FXML private Label elapsedTimeText;   // Label for the elapsed time
    @FXML private Label statusText;        // Label for status updates
    @FXML private Label ratioLabel;        // Label for displaying the ratios

    private Stage stage;

    private String elapsedTime;
    private String analyzerRegex;
    private String dfaRegex;
    private Examples currentExamples;

    // Method to set the main stage
    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setTitle("Generated Regular Expressions");
    }

    // Method to set examples and a single regex (for backward compatibility)
    public void setValues(String elapsedTime, String generatedRegex, Examples ce) {
        currentExamples = ce;
        elapsedTimeText.setText("Time taken: " + elapsedTime + " seconds");

        // Check if the generatedRegex contains both regex patterns
        if (generatedRegex.contains("Pattern Analyzer:") && generatedRegex.contains("DFA-based:")) {
            // Parse the combined string
            parseRegexResults(generatedRegex);
        } else {
            // Fallback to setting both labels to the same regex
            analyzerRegexLabel.setText(generatedRegex);
            dfaRegexLabel.setText(generatedRegex);
            this.analyzerRegex = generatedRegex;
            this.dfaRegex = generatedRegex;
        }

        this.elapsedTime = elapsedTime;

        // Calculate and display the ratios
        updateRatios(analyzerRegex,dfaRegex);
    }

    //Method to calculate and display regex ratios
    private void updateRatios(String analyzerRegex, String dfaRegex) {
        if (analyzerRegex == null || dfaRegex == null ||
                analyzerRegex.equals("Not available") || dfaRegex.equals("Not available")) {
            statusText.setText("Ratio metrics not available");
            return;
        }

        // Calculate the ratios
        double lengthRatio = RegexComparator.getLengthRatio(analyzerRegex, dfaRegex);
        double complexityRatio = RegexComparator.getComplexityRatio(analyzerRegex, dfaRegex);

        // Format the ratios
        String formattedLengthRatio = String.format("%.2f:1", lengthRatio);
        String formattedComplexityRatio = String.format("%.2f:1", complexityRatio);

        // Create a concise metrics message for the status area
        String ratioMetrics = "Length ratio (PA:DFA): " + formattedLengthRatio +
                "\nComplexity ratio (PA:DFA): " + formattedComplexityRatio;

        // Update the status text
        ratioLabel.setText(ratioMetrics);
    }

    // Method to parse combined regex string

    @FXML
    protected void onExportButtonClick() throws IOException {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save positive and negative examples");

        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        try {
            File selectedFile = fc.showSaveDialog(stage);

            if (selectedFile != null) {
                try (FileWriter fw = new FileWriter(selectedFile)) {
                    writeToFile(fw);
                } catch (IOException e) {
                    updateStatusLabel("ERROR: Exporting the file");
                    e.printStackTrace();
                }
                updateStatusLabel("Successfully exported the file: " + selectedFile.getName());
            }
        } catch (Exception e) {
            updateStatusLabel("ERROR: Unexpected error during export");
            e.printStackTrace();
        }
    }

    private void writeToFile(FileWriter fw) throws IOException {
        List<String> positives = currentExamples.getPositiveExamples();
        List<String> negatives = currentExamples.getNegativeExamples();

        for (int i = 0; i < positives.size(); i++) {
            fw.write(positives.get(i));
            if (i < positives.size() - 1) {
                fw.write("|");
            }
        }

        fw.write("\n::\n");

        for (int i = 0; i < negatives.size(); i++) {
            fw.write(negatives.get(i));
            if (i < negatives.size() - 1) {
                fw.write("|");
            }
        }

        fw.write("\n::\n");
        fw.write("Pattern Analyzer Regex: " + analyzerRegex);
        fw.write("\nDFA-based Regex: " + dfaRegex);
        fw.write("\n\n" + ratioLabel.getText());
        fw.close();
    }

    @FXML
    protected void onCopyAnalyzerButtonClick() throws IOException {
        copyToClipboard(analyzerRegex);
        updateStatusLabel("Copied the Pattern Analyzer regex to clipboard!");
    }

    @FXML
    protected void onCopyDFAButtonClick() throws IOException {
        copyToClipboard(dfaRegex);
        updateStatusLabel("Copied the DFA-based regex to clipboard!");
    }

    @FXML
    protected void onCopyBothButtonClick() throws IOException {
        String combinedRegex = "Pattern Analyzer: " + analyzerRegex + "\nDFA-based: " + dfaRegex;
        copyToClipboard(combinedRegex);
        updateStatusLabel("Copied both regexes to clipboard!");
    }


    private void copyToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    @FXML
    protected void onBackButtonClick() throws IOException {
        // Load the previous input examples form
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/owenjg/regexsynthesiser/input-examples-view.fxml"));
        VBox previousRoot = fxmlLoader.load();

        // Get the controller for the previous form
        InputExamplesController previousController = fxmlLoader.getController();

        // Set the stage for the previous controller
        previousController.setStage(stage);

        // Create a new scene with the previous form
        Scene previousScene = new Scene(previousRoot, 800, 600);
        previousScene.getStylesheets().add(getClass().getResource("/com/owenjg/regexsynthesiser/styles.css").toExternalForm());

        stage.setScene(previousScene);
    }

    private void updateStatusLabel(String message) {
        // Clear previous styles
        statusText.getStyleClass().remove("error");

        // Check if message starts with "ERROR:" or contains specific error keywords
        if (message.toUpperCase().startsWith("ERROR:")) {
            // Add error style class
            statusText.getStyleClass().add("error");
        }

        // Set the message
        statusText.setText(message);
    }
    // Add this to your DisplayRegexController class
// This will update the blue status area with metrics

    /**
     * Set the status text with metrics information.
     *
     * @param analyzerRegex The pattern analyzer regex
     * @param dfaRegex The DFA-based regex
     */


    private void parseRegexResults(String combinedRegex) {
        // Initialize default values
        analyzerRegex = "Not available";
        dfaRegex = "Not available";

        if (combinedRegex != null) {
            String[] parts = combinedRegex.split("\n");

            for (String part : parts) {
                if (part.startsWith("Pattern Analyzer:")) {
                    analyzerRegex = part.substring("Pattern Analyzer:".length()).trim();
                } else if (part.startsWith("DFA-based:")) {
                    dfaRegex = part.substring("DFA-based:".length()).trim();
                }
            }
        }

        // Update UI
        analyzerRegexLabel.setText(analyzerRegex);
        dfaRegexLabel.setText(dfaRegex);

        // Update the status text with ratio metrics
        updateRatios(analyzerRegex, dfaRegex);
    }


}