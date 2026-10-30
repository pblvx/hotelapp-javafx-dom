package com.hotelapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Punto de entrada de HotelApp.
 *
 * <p>Carga la vista FXML principal y configura la ventana. La aplicación usa
 * el tema Modena de JavaFX y estilos declarados en el propio FXML, sin hojas
 * CSS externas.</p>
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL vista = Main.class.getResource("/vista.fxml");
        if (vista == null) {
            throw new IOException("No se encontró el archivo vista.fxml en resources.");
        }

        FXMLLoader cargador = new FXMLLoader(vista);
        Parent raiz = cargador.load();

        Scene escena = new Scene(raiz, 1220, 800);

        stage.setTitle("HotelApp · Motor de Reservas y Recepción");
        stage.setMinWidth(1080);
        stage.setMinHeight(700);
        stage.setScene(escena);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
