package com.ibsecurity.controller;

import com.ibsecurity.dao.DepartmentDAO;
import com.ibsecurity.dao.EmployeeDAO;
import com.ibsecurity.dao.PositionDAO;
import com.ibsecurity.model.Department;
import com.ibsecurity.model.Employee;
import com.ibsecurity.model.Position;
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
import javafx.util.StringConverter;
import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;
import com.ibsecurity.util.ExcelExporter;
import com.ibsecurity.util.ExcelImporter;
import javafx.stage.FileChooser;
import java.io.File;

public class MainController implements Initializable {

    @FXML private Label lblCurrentUser;
    @FXML private Button btnLogout;
    @FXML private Button btnAdmin;
    @FXML private Button btnExport;
    @FXML private Button btnImport;
    @FXML private Button btnDelete;
    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, String> colNumber;
    @FXML private TableColumn<Employee, String> colLastName;
    @FXML private TableColumn<Employee, String> colFirstName;
    @FXML private TableColumn<Employee, String> colMiddleName;
    @FXML private TableColumn<Employee, String> colPosition;
    @FXML private TableColumn<Employee, String> colDepartment;
    @FXML private TableColumn<Employee, String> colEmail;
    @FXML private TableColumn<Employee, String> colPhone;
    @FXML private TableColumn<Employee, Integer> colClearance;
    @FXML private TableColumn<Employee, Boolean> colActive;

    @FXML private TextField txtSearch;
    @FXML private TextField txtEmployeeNumber;
    @FXML private TextField txtLastName;
    @FXML private TextField txtFirstName;
    @FXML private TextField txtMiddleName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private DatePicker dpBirthDate;
    @FXML private DatePicker dpHireDate;
    @FXML private ComboBox<Position> cbPosition;
    @FXML private ComboBox<Department> cbDepartment;
    @FXML private ComboBox<String> cbClearance;
    @FXML private CheckBox chkActive;

    private EmployeeDAO employeeDAO = new EmployeeDAO();
    private PositionDAO positionDAO = new PositionDAO();
    private DepartmentDAO departmentDAO = new DepartmentDAO();
    private ObservableList<Employee> employeeList = FXCollections.observableArrayList();
    private Employee selectedEmployee = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadComboBoxes();
        loadEmployees();
        setupTableSelection();

        // Отображаем имя пользователя
        if (Session.getCurrentUser() != null) {
            lblCurrentUser.setText("Пользователь: " + Session.getCurrentUser().getFullName());
            
            // Показываем кнопки только для администраторов
            btnAdmin.setVisible(Session.isAdmin());
            btnAdmin.setManaged(Session.isAdmin());
            btnExport.setVisible(Session.isAdmin());
            btnExport.setManaged(Session.isAdmin());
            btnImport.setVisible(Session.isAdmin());
            btnImport.setManaged(Session.isAdmin());
            btnDelete.setVisible(Session.isAdmin());
            btnDelete.setManaged(Session.isAdmin());
        }
    }

    private void setupTable() {
        colNumber.setCellValueFactory(new PropertyValueFactory<>("employeeNumber"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colMiddleName.setCellValueFactory(new PropertyValueFactory<>("middleName"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("positionTitle"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colClearance.setCellValueFactory(new PropertyValueFactory<>("clearanceLevel"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        // Отображение уровня допуска с описанием
        colClearance.setCellFactory(column -> new TableCell<Employee, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String clearanceText = switch (item) {
                        case 1 -> "1 - Открытый";
                        case 2 -> "2 - ДСП";
                        case 3 -> "3 - Секретно";
                        case 4 -> "4 - Сов. секретно";
                        case 5 -> "5 - Особой важности";
                        default -> item.toString();
                    };
                    setText(clearanceText);
                }
            }
        });

        // Отображение статуса
        colActive.setCellFactory(column -> new TableCell<Employee, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if (item) {
                        setText("Активный");
                        setStyle("-fx-text-fill: #34a853;");
                    } else {
                        setText("Неактивный");
                        setStyle("-fx-text-fill: #ea4335;");
                    }
                }
            }
        });

        employeeTable.setItems(employeeList);
    }

    private void loadComboBoxes() {
        // Настройка ComboBox для должностей (редактируемый)
        cbPosition.setItems(FXCollections.observableArrayList(positionDAO.findAll()));
        cbPosition.setEditable(true);
        cbPosition.setConverter(new StringConverter<Position>() {
            @Override
            public String toString(Position position) {
                return position != null ? position.getTitle() : "";
            }

            @Override
            public Position fromString(String string) {
                if (string == null || string.trim().isEmpty()) {
                    return null;
                }
                // Ищем существующую должность
                for (Position p : cbPosition.getItems()) {
                    if (p.getTitle().equalsIgnoreCase(string.trim())) {
                        return p;
                    }
                }
                // Создаём новую должность
                Position newPosition = new Position();
                newPosition.setId(-1);
                newPosition.setTitle(string.trim());
                return newPosition;
            }
        });

        // Настройка ComboBox для отделов (редактируемый)
        cbDepartment.setItems(FXCollections.observableArrayList(departmentDAO.findAll()));
        cbDepartment.setEditable(true);
        cbDepartment.setConverter(new StringConverter<Department>() {
            @Override
            public String toString(Department department) {
                return department != null ? department.getName() : "";
            }

            @Override
            public Department fromString(String string) {
                if (string == null || string.trim().isEmpty()) {
                    return null;
                }
                // Ищем существующий отдел
                for (Department d : cbDepartment.getItems()) {
                    if (d.getName().equalsIgnoreCase(string.trim())) {
                        return d;
                    }
                }
                // Создаём новый отдел
                Department newDepartment = new Department();
                newDepartment.setId(-1);
                newDepartment.setName(string.trim());
                return newDepartment;
            }
        });

        cbClearance.setItems(FXCollections.observableArrayList(
            "1 - Открытый",
            "2 - ДСП",
            "3 - Секретно",
            "4 - Сов. секретно",
            "5 - Особой важности"
        ));
        cbClearance.setValue("1 - Открытый");
        chkActive.setSelected(true);
    }

    private void loadEmployees() {
        employeeList.clear();
        employeeList.addAll(employeeDAO.findAll());
    }

    private void setupTableSelection() {
        employeeTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectedEmployee = newSelection;
                    fillForm(newSelection);
                }
            }
        );
    }

    private void fillForm(Employee employee) {
        txtEmployeeNumber.setText(employee.getEmployeeNumber());
        txtLastName.setText(employee.getLastName());
        txtFirstName.setText(employee.getFirstName());
        txtMiddleName.setText(employee.getMiddleName());
        txtEmail.setText(employee.getEmail());
        txtPhone.setText(employee.getPhone());
        dpBirthDate.setValue(employee.getBirthDate());
        dpHireDate.setValue(employee.getHireDate());
        String clearanceText = switch (employee.getClearanceLevel()) {
            case 1 -> "1 - Открытый";
            case 2 -> "2 - ДСП";
            case 3 -> "3 - Секретно";
            case 4 -> "4 - Сов. секретно";
            case 5 -> "5 - Особой важности";
            default -> "1 - Открытый";
        };
        cbClearance.setValue(clearanceText);
        chkActive.setSelected(employee.isActive());

        for (Position p : cbPosition.getItems()) {
            if (p.getId() == employee.getPositionId()) {
                cbPosition.setValue(p);
                break;
            }
        }
        for (Department d : cbDepartment.getItems()) {
            if (d.getId() == employee.getDepartmentId()) {
                cbDepartment.setValue(d);
                break;
            }
        }
    }

    @FXML
    private void handleAdd() {
        if (!validateForm()) return;

        Employee employee = new Employee();
        if (!fillEmployeeFromForm(employee)) return;

        if (employeeDAO.insert(employee)) {
            showAlert(Alert.AlertType.INFORMATION, "Успех", "Сотрудник добавлен");
            refreshData();
            clearForm();
        } else {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось добавить сотрудника");
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedEmployee == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите сотрудника");
            return;
        }
        if (!validateForm()) return;

        try {
            if (!fillEmployeeFromForm(selectedEmployee)) return;

            if (employeeDAO.update(selectedEmployee)) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Данные обновлены");
                int updatedId = selectedEmployee.getId();
                refreshData();
                // Восстанавливаем выделение после обновления
                for (Employee emp : employeeList) {
                    if (emp.getId() == updatedId) {
                        employeeTable.getSelectionModel().select(emp);
                        break;
                    }
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось обновить данные");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка при обновлении: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedEmployee == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите сотрудника");
            return;
        }

        Optional<ButtonType> result = showConfirmation(
            "Подтверждение", 
            "Удалить сотрудника " + selectedEmployee.getFullName() + "?"
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (employeeDAO.delete(selectedEmployee.getId())) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Сотрудник удалён");
                refreshData();
                clearForm();
                selectedEmployee = null;
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось удалить сотрудника");
            }
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = txtSearch.getText().trim();
        employeeList.clear();
        if (searchTerm.isEmpty()) {
            employeeList.addAll(employeeDAO.findAll());
        } else {
            employeeList.addAll(employeeDAO.search(searchTerm));
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
        selectedEmployee = null;
        employeeTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleExport() {
        if (employeeList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Нет данных для экспорта");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить файл Excel");
        fileChooser.setInitialFileName("сотрудники_иб.xlsx");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel файлы (*.xlsx)", "*.xlsx")
        );

        Stage stage = (Stage) employeeTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            if (ExcelExporter.exportEmployees(employeeList, file.getAbsolutePath())) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Данные экспортированы в:\n" + file.getAbsolutePath());
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось экспортировать данные");
            }
        }
    }

    @FXML
    private void handleImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите Excel-файл для импорта");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel файлы (*.xlsx)", "*.xlsx"),
            new FileChooser.ExtensionFilter("Все файлы (*.*)", "*.*")
        );

        Stage stage = (Stage) employeeTable.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // Спрашиваем про заголовок
            Alert headerAlert = new Alert(Alert.AlertType.CONFIRMATION);
            headerAlert.setTitle("Заголовок файла");
            headerAlert.setHeaderText(null);
            headerAlert.setContentText("Первая строка файла содержит заголовки столбцов?");

            ButtonType btnYes = new ButtonType("Да, пропустить");
            ButtonType btnNo = new ButtonType("Нет, импортировать всё");
            headerAlert.getButtonTypes().setAll(btnYes, btnNo);

            Optional<ButtonType> headerChoice = headerAlert.showAndWait();
            boolean skipHeader = headerChoice.isPresent() && headerChoice.get() == btnYes;

            // Выполняем импорт
            ExcelImporter importer = new ExcelImporter();
            ExcelImporter.ImportResult result = importer.importFromExcel(file, skipHeader);

            // Показываем результат
            Alert.AlertType alertType = result.hasErrors() ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION;
            showAlert(alertType, "Результат импорта", result.getDetailedReport());

            // Обновляем таблицу
            if (result.getSuccessCount() > 0) {
                refreshData();
            }
        }
    }

    @FXML
    private void handleAdminPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());

            Stage stage = (Stage) btnAdmin.getScene().getWindow();
            stage.setTitle("Админ-панель - ИБ Система");
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось открыть админ-панель");
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
                
                Stage loginStage = new Stage();
                loginStage.setTitle("Авторизация - ИБ Система");
                loginStage.setScene(scene);
                loginStage.setResizable(false);
                loginStage.show();
                
                // Закрываем текущее окно
                Stage currentStage = (Stage) btnLogout.getScene().getWindow();
                currentStage.close();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean fillEmployeeFromForm(Employee employee) {
        // Обработка должности
        Position position = cbPosition.getValue();
        if (position == null) {
            String posText = cbPosition.getEditor().getText();
            if (posText != null && !posText.trim().isEmpty()) {
                position = cbPosition.getConverter().fromString(posText);
            }
        }
        
        if (position == null) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Выберите должность");
            return false;
        }
        
        if (position.getId() == -1) {
            int newId = positionDAO.insert(position);
            if (newId > 0) {
                position.setId(newId);
                cbPosition.getItems().add(position);
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось создать должность");
                return false;
            }
        }

        // Обработка отдела
        Department department = cbDepartment.getValue();
        if (department == null) {
            String depText = cbDepartment.getEditor().getText();
            if (depText != null && !depText.trim().isEmpty()) {
                department = cbDepartment.getConverter().fromString(depText);
            }
        }
        
        if (department == null) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Выберите отдел");
            return false;
        }
        
        if (department.getId() == -1) {
            int newId = departmentDAO.insert(department);
            if (newId > 0) {
                department.setId(newId);
                cbDepartment.getItems().add(department);
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось создать отдел");
                return false;
            }
        }

        employee.setEmployeeNumber(getTextSafe(txtEmployeeNumber));
        employee.setLastName(getTextSafe(txtLastName));
        employee.setFirstName(getTextSafe(txtFirstName));
        employee.setMiddleName(getTextSafe(txtMiddleName));
        employee.setEmail(getTextSafe(txtEmail));
        employee.setPhone(getTextSafe(txtPhone));
        employee.setBirthDate(dpBirthDate.getValue());
        employee.setHireDate(dpHireDate.getValue());
        employee.setPositionId(position.getId());
        employee.setDepartmentId(department.getId());
        int clearanceLevel = Integer.parseInt(cbClearance.getValue().substring(0, 1));
        employee.setClearanceLevel(clearanceLevel);
        employee.setActive(chkActive.isSelected());
        
        return true;
    }
    
    private String getTextSafe(TextField field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().trim();
    }

    private void refreshData() {
        loadEmployees();
        cbPosition.setItems(FXCollections.observableArrayList(positionDAO.findAll()));
        cbDepartment.setItems(FXCollections.observableArrayList(departmentDAO.findAll()));
    }

    private void clearForm() {
        txtEmployeeNumber.clear();
        txtLastName.clear();
        txtFirstName.clear();
        txtMiddleName.clear();
        txtEmail.clear();
        txtPhone.clear();
        dpBirthDate.setValue(null);
        dpHireDate.setValue(LocalDate.now());
        cbPosition.setValue(null);
        cbPosition.getEditor().clear();
        cbDepartment.setValue(null);
        cbDepartment.getEditor().clear();
        cbClearance.setValue("1 - Открытый");
        chkActive.setSelected(true);
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (txtEmployeeNumber.getText().trim().isEmpty()) 
            errors.append("- Табельный номер обязателен\n");
        if (txtLastName.getText().trim().isEmpty()) 
            errors.append("- Фамилия обязательна\n");
        if (txtFirstName.getText().trim().isEmpty()) 
            errors.append("- Имя обязательно\n");
        if (dpBirthDate.getValue() == null) 
            errors.append("- Дата рождения обязательна\n");
        if (dpHireDate.getValue() == null) 
            errors.append("- Дата приёма обязательна\n");
        
        Position pos = cbPosition.getValue();
        String posText = cbPosition.getEditor().getText();
        if (pos == null && (posText == null || posText.trim().isEmpty())) {
            errors.append("- Должность обязательна\n");
        }
        
        Department dep = cbDepartment.getValue();
        String depText = cbDepartment.getEditor().getText();
        if (dep == null && (depText == null || depText.trim().isEmpty())) {
            errors.append("- Отдел обязателен\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Ошибки валидации", errors.toString());
            return false;
        }
        return true;
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