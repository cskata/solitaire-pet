package com.codecool.klondike;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Klondike extends Application {
    private static Stage primaryStage;
    private static final double WINDOW_WIDTH = 1400;
    private static final double WINDOW_HEIGHT = 900;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Card.loadCardImages();
        setPrimaryStage(primaryStage);
        Scene scene = startGame();

        primaryStage.setTitle("Klondike Solitaire");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static Scene startGame() {
        Game game = new Game("ads");
        game.setStyle("-fx-background-color: green");
        return new Scene(game, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    private static void setPrimaryStage(Stage stage) {
        Klondike.primaryStage = stage;
    }

}
