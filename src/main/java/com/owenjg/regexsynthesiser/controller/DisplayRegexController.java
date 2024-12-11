package com.owenjg.regexsynthesiser.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class DisplayRegexController {

    @FXML private Label generatedRegexLabel;
    // Label to display the generated regex
    @FXML private Label titleText; // Label for the title
    @FXML private Label elapsedTimeText; // Label for the title

    private Stage stage;
    private String positiveExamples;
    private String negativeExamples;
    private String elapsedTime;
    private String generatedRegex;
    // Method to set the main stage
    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setTitle("Generated Regular Expression");
    }

    // Method to set examples from the previous form
    public void setValues(String positiveExamples, String negativeExamples, String elapsedTime, String generatedRegex) {
        elapsedTimeText.setText("Time taken " + elapsedTime + "s");
        generatedRegexLabel.setText("Generated regex: " + generatedRegex);
        this.generatedRegex = generatedRegex;
        this.positiveExamples = positiveExamples;
        this.negativeExamples = negativeExamples;
        this.elapsedTime = elapsedTime;
    }

    @FXML
    protected void onExportButtonClick() throws IOException {
        titleText.setText("test");
    }

    @FXML
    protected void onCopyButtonClick() throws IOException {
        titleText.setText("test2");
    }

}
