package com.ibsecurity.controller;

import com.ibsecurity.dao.UserDAO;
import com.ibsecurity.model.User;
import com.ibsecurity.util.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    @FXML private Label lblCurrentUser;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, Boolean> colStatus;
    @FXML private TableColumn<User, LocalDateTime> colCreatedAt;

    @FXML private Button btnMakeAdmin;
    @FXML private Button btnRemoveAdmin;
    @FXML private Button btnBlock;
    @FXML private Button btnUnblock;
    @FXML private Button btnDelete;

    private UserDAO userDAO = new UserDAO();
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private User selectedUser = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadUsers();
        setupTableSelection();
        updateButtonStates();

        if (Session.getCurrentUser() != null) {
            lblCurrentUser.setText("Администратор: " + Session.getCurrentUser().getFullName());
        }
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        // Отображение роли
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if ("admin".equals(item)) {
                        setText("Администратор");
                        setStyle("-fx-text-fill: #1a73e8; -fx-font-weight: bold;");
                    } else {
                        setText("Пользователь");
                        setStyle("");
                    }
                }
            }
        });

        // Отображение статуса
        colStatus.setCellValueFactory(new PropertyValueFactory<>("active"));
        colStatus.setCellFactory(column -> new TableCell<User, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if (item) {
                        setText("Активен");
                        setStyle("-fx-text-fill: #34a853;");
                    } else {
                        setText("Заблокирован");
                        setStyle("-fx-text-fill: #ea4335;");
                    }
                }
            }
        });

        // Отображение даты
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colCreatedAt.setCellFactory(column -> new TableCell<User, LocalDateTime>() {
            private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });

        userTable.setItems(userList);
    }

    private void loadUsers() {
        userList.clear();
        userList.addAll(userDAO.findAll());
    }

    private void setupTableSelection() {
        userTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                selectedUser = newSelection;
                updateButtonStates();
            }
        );
    }

    private void updateButtonStates() {
        boolean noSelection = selectedUser == null;
        boolean isSelf = selectedUser != null && selectedUser.getId() == Session.getCurrentUser().getId();
        boolean isAdmin = selectedUser != null && selectedUser.isAdmin();
        boolean isActive = selectedUser != null && selectedUser.isActive();

        btnMakeAdmin.setDisable(noSelection || isSelf || isAdmin);
        btnRemoveAdmin.setDisable(noSelection || isSelf || !isAdmin);
        btnBlock.setDisable(noSelection || isSelf || !isActive);
        btnUnblock.setDisable(noSelection || isSelf || isActive);
        btnDelete.setDisable(noSelection || isSelf);
    }

    @FXML
    private void handleMakeAdmin() {
        if (selectedUser == null) return;

        Optional<ButtonType> result = showConfirmation("Назначить администратором",
            "Назначить пользователя \"" + selectedUser.getFullName() + "\" администратором?");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (userDAO.setAdmin(selectedUser.getId(), true)) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Права администратора назначены");
                loadUsers();
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось назначить права");
            }
        }
    }

    @FXML
    private void handleRemoveAdmin() {
        if (selectedUser == null) return;

        Optional<ButtonType> result = showConfirmation("Снять права администратора",
            "Снять права администратора у пользователя \"" + selectedUser.getFullName() + "\"?");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (userDAO.setAdmin(selectedUser.getId(), false)) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Права администратора сняты");
                loadUsers();
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось снять права");
            }
        }
    }

    @FXML
    private void handleBlock() {
        if (selectedUser == null) return;

        Optional<ButtonType> result = showConfirmation("Заблокировать пользователя",
            "Заблокировать пользователя \"" + selectedUser.getFullName() + "\"?\nОн не сможет войти в систему.");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (userDAO.setActive(selectedUser.getId(), false)) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Пользователь заблокирован");
                loadUsers();
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось заблокировать пользователя");
            }
        }
    }

    @FXML
    private void handleUnblock() {
        if (selectedUser == null) return;

        Optional<ButtonType> result = showConfirmation("Разблокировать пользователя",
            "Разблокировать пользователя \"" + selectedUser.getFullName() + "\"?");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (userDAO.setActive(selectedUser.getId(), true)) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Пользователь разблокирован");
                loadUsers();
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось разблокировать пользователя");
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedUser == null) return;

        Optional<ButtonType> result = showConfirmation("Удалить пользователя",
            "Удалить пользователя \"" + selectedUser.getFullName() + "\"?\nЭто действие необратимо!");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (userDAO.deleteUser(selectedUser.getId())) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Пользователь удалён");
                loadUsers();
                selectedUser = null;
                updateButtonStates();
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось удалить пользователя");
            }
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1150, 750);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());

            Stage stage = (Stage) userTable.getScene().getWindow();
            stage.setTitle("Система управления сотрудниками ИБ - " + Session.getCurrentUser().getFullName());
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        Optional<ButtonType> result = showConfirmation("Выход", "Вы действительно хотите выйти из системы?");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Session.logout();

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
                Parent root = loader.load();

                Scene scene = new Scene(root, 400, 500);

                Stage stage = new Stage();
                stage.setTitle("Авторизация - ИБ Система");
                stage.setScene(scene);
                stage.setResizable(false);
                stage.show();

                Stage currentStage = (Stage) userTable.getScene().getWindow();
                currentStage.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Optional<ButtonType> showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait();
    }
}
