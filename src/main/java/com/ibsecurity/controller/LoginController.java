package com.ibsecurity.controller;

import com.ibsecurity.App;
import com.ibsecurity.dao.UserDAO;
import com.ibsecurity.model.User;
import com.ibsecurity.util.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Контроллер авторизации.
 */
public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        lblError.setText("");
        
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Введите логин и пароль");
            return;
        }

        User user = userDAO.authenticate(username, password);

        if (user != null) {
            Session.createSession(user);
            openMainWindow();
        } else {
            lblError.setText("Неверный логин или пароль");
            txtPassword.clear();
            txtPassword.requestFocus();
        }
    }

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/register.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 400, 550);

            Stage stage = new Stage();
            stage.setTitle("Регистрация - ИБ Система");
            stage.setScene(scene);
            stage.setResizable(false);
            if (App.getAppIcon() != null) {
                stage.getIcons().add(App.getAppIcon());
            }
            stage.show();

            Stage loginStage = (Stage) txtUsername.getScene().getWindow();
            loginStage.close();

        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("Ошибка открытия регистрации");
        }
    }

    private void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1150, 750);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());

            Stage stage = new Stage();
            User currentUser = Session.getCurrentUser();
            String title = "Система управления сотрудниками ИБ";
            if (currentUser != null) {
                title += " - " + currentUser.getFullName();
            }
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(600);
            if (App.getAppIcon() != null) {
                stage.getIcons().add(App.getAppIcon());
            }
            
            stage.setOnCloseRequest(event -> {
                Session.logout();
            });
            
            stage.show();

            Stage loginStage = (Stage) txtUsername.getScene().getWindow();
            loginStage.close();

        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("Ошибка загрузки главного окна");
        }
    }

    @FXML
    private void handleKeyPress(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            handleLogin();
        }
    }
}
