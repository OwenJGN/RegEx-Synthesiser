package com.owenjg.regexsynthesiser.controller;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Controller for the main menu of the Regular Expression Synthesiser application.
 *
 * This controller manages the initial landing page that allows users to start
 * the synthesiser process or quit the application.
 */
public class MenuController {

    private Stage stage;

    /**
     * Sets the main application stage for this controller.
     *
     * @param stage The primary stage for this JavaFX application
     */
    public void setStage(Stage stage) {
        this.stage = stage;
        // Set window title
        stage.setTitle("Regular Expression Synthesiser");
    }

    /**
     * Handles the click event for the Start button.
     * Navigates to the examples input form where users can enter test cases.
     *
     * @throws IOException If there is an error loading the next view
     */
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
        secondScene.getStylesheets().add(getClass().getResource("/com/owenjg/regexsynthesiser/styles.css").toExternalForm());

        stage.setScene(secondScene);
    }

    /**
     * Handles the click event for the Quit button.
     * Exits the application immediately.
     */
    @FXML
    protected void onQuitButtonClick() {
        System.exit(0);
    }
}