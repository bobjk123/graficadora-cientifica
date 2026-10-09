package com.graficadora;

import com.graficadora.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada principal de la Graficadora Científica FX.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainWindow mainWindow = new MainWindow(primaryStage);
        Scene scene = new Scene(mainWindow, 1200, 750);

        // Estilos CSS base oscuros para controles estándar
        scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());

        primaryStage.setTitle("Graficadora Científica FX");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
