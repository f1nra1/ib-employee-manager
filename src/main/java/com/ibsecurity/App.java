package com.ibsecurity;

import com.ibsecurity.util.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {
    
    private static Image appIcon;
    
    public static Image getAppIcon() {
        if (appIcon == null) {
            try {
                appIcon = new Image(App.class.getResourceAsStream("/icon.png"));
            } catch (Exception e) {
            }
        }
        return appIcon;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/views/login.fxml"));
        
        Scene scene = new Scene(root, 400, 500);
        
        primaryStage.setTitle("Авторизация - ИБ Система");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        
        if (getAppIcon() != null) {
            primaryStage.getIcons().add(getAppIcon());
        }
        
        primaryStage.show();
    }

    @Override
    public void stop() {
        DatabaseConnection.closeDataSource();
    }

    public static void main(String[] args) {
        launch(args);
    }
}