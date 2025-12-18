package com.ibsecurity.controller;

import com.ibsecurity.App;
import com.ibsecurity.dao.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Контроллер регистрации.
 */
public class RegisterController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtPasswordConfirm;
    @FXML private Label lblError;

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleRegister() {
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String password = txtPassword.getText();
        String passwordConfirm = txtPasswordConfirm.getText();

        if (username.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
            lblError.setText("Заполните все поля");
            return;
        }

        if (username.length() < 3) {
            lblError.setText("Логин минимум 3 символа");
            return;
        }

        if (password.length() < 6) {
            lblError.setText("Пароль минимум 6 символов");
            return;
        }

        if (!password.equals(passwordConfirm)) {
            lblError.setText("Пароли не совпадают");
            return;
        }

        if (userDAO.usernameExists(username)) {
            lblError.setText("Такой логин уже существует");
            return;
        }

        if (userDAO.register(username, password, fullName)) {
            showAlert("Успех", "Регистрация прошла успешно!\nТеперь вы можете войти в систему.");
            openLoginWindow();
        } else {
            lblError.setText("Ошибка регистрации");
        }
    }

    @FXML
    private void handleBackToLogin() {
        openLoginWindow();
    }

    private void openLoginWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 400, 500);

            Stage stage = new Stage();
            stage.setTitle("Авторизация - ИБ Система");
            stage.setScene(scene);
            stage.setResizable(false);
            if (App.getAppIcon() != null) {
                stage.getIcons().add(App.getAppIcon());
            }
            stage.show();

            Stage currentStage = (Stage) txtUsername.getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleKeyPress(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            handleRegister();
        }
    }
}
