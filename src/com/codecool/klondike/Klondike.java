package com.codecool.klondike;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Klondike extends Application {

    private static final double WINDOW_WIDTH = 1400;
    private static final double WINDOW_HEIGHT = 900;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Card.loadCardImages();
        Game game = new Game();
        game.setStyle("-fx-background-color: green");

        Scene scene = new Scene(game, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setTitle("Klondike Solitaire");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
