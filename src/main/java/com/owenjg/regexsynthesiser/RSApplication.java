package com.owenjg.regexsynthesiser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.owenjg.regexsynthesiser.controller.MenuController;

import java.io.IOException;

public class RSApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(RSApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        // Get the controller for the main view
        MenuController controller = fxmlLoader.getController();

        // Pass the Stage to the controller
        controller.setStage(stage);

        stage.setTitle("Regular Expression Synthesiser");
        stage.setScene(scene);
        stage.setResizable(false); // Fixed size
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
