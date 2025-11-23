package com.example.webbrowser;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.Objects;
import javafx.scene.image.Image;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {

        CookieHandler.setDefault(new CookieManager());

        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1080, 720);

        String css = Objects.requireNonNull(Application.class.getResource("style.css")).toExternalForm();
        scene.getStylesheets().add(css);

        Image icon = new Image(Objects.requireNonNull(Application.class.getResourceAsStream("atlas-logo.png")));
        stage.getIcons().add(icon);

        stage.setTitle("Browser");
        stage.setScene(scene);
        stage.show();
    }
}
