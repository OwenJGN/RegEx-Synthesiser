package com.owenjg.regexsynthesiser.controller;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MenuController {

    private Stage stage;

    // Method to set the main stage
    public void setStage(Stage stage) {
        this.stage = stage;
        // Set window title
        stage.setTitle("Regular Expression Synthesiser");
    }

    @FXML
    protected void onStartButtonClick() throws IOException {
        // Load second form (Examples input form)
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/owenjg/regexsynthesiser/input-examples-view.fxml"));
        VBox secondRoot = fxmlLoader.load();

        // Pass the stage to the second controller
        InputExamplesController inputController = fxmlLoader.getController();
        inputController.setStage(stage);

        // Create and set the scene for the second form
        Scene secondScene = new Scene(secondRoot, 800, 600);
        stage.setScene(secondScene);
    }

    @FXML
    protected void onQuitButtonClick() {
        System.exit(0);
    }
}
