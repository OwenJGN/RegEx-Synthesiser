package com.owenjg.regexsynthesiser.controller;

import com.owenjg.regexsynthesiser.synthesis.Examples;
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

    @FXML private Label generatedRegexLabel;
    // Label to display the generated regex
    @FXML private Label titleText; // Label for the title
    @FXML private Label elapsedTimeText; // Label for the title
    @FXML private Label statusText; // Label for the title


    private Stage stage;

    private String elapsedTime;
    private String generatedRegex;
    private Examples currentExamples;
    // Method to set the main stage
    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setTitle("Generated Regular Expression");
    }

    // Method to set examples from the previous form
    public void setValues( String elapsedTime, String generatedRegex, Examples ce) {
        currentExamples = ce;
        elapsedTimeText.setText("Time taken " + elapsedTime + "s");
        generatedRegexLabel.setText("Generated regex: " + generatedRegex);
        this.generatedRegex = generatedRegex;
        this.elapsedTime = elapsedTime;
    }

    @FXML
    protected void onExportButtonClick() throws IOException {

        FileChooser fc = new FileChooser();
        fc.setTitle("Save positive and negative examples");

        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        try{
            File selectedFile = fc.showSaveDialog(stage);

            if(selectedFile != null){
                try (FileWriter fw = new FileWriter(selectedFile)){
                    writeToFile(fw);
                } catch (IOException e){
                    updateStatusLabel("ERROR: Exporting the file");
                    e.printStackTrace();
                }
            }
            updateStatusLabel("Successfully exported the file: " +selectedFile.getName());

        } catch (Exception e){
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
        fw.write("Generated Regex: " +generatedRegex);
        fw.close();
    }

    @FXML
    protected void onCopyButtonClick() throws IOException {
        StringSelection stringSelection = new StringSelection(generatedRegex);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
        updateStatusLabel("Copied the regular expression!");
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
        if (message.toUpperCase().startsWith("ERROR:")){
            // Add error style class
            statusText.getStyleClass().add("error");
        }

        // Set the message
        statusText.setText(message);
    }
}
