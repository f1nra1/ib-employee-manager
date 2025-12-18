package com.ibsecurity.util;

import com.ibsecurity.dao.DepartmentDAO;
import com.ibsecurity.dao.EmployeeDAO;
import com.ibsecurity.dao.PositionDAO;
import com.ibsecurity.model.Department;
import com.ibsecurity.model.Employee;
import com.ibsecurity.model.Position;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Класс для импорта данных сотрудников из Excel-файлов (.xlsx).
 * Формат: табельный_номер | фамилия | имя | отчество | дата_рождения | дата_приёма | должность | отдел | email | телефон | уровень_допуска
 */
public class ExcelImporter {

    private EmployeeDAO employeeDAO = new EmployeeDAO();
    private PositionDAO positionDAO = new PositionDAO();
    private DepartmentDAO departmentDAO = new DepartmentDAO();

    /**
     * Результат импорта с подробной статистикой.
     */
    public static class ImportResult {
        private int totalRows;
        private int successCount;
        private int errorCount;
        private List<String> errors;

        public ImportResult() {
            this.errors = new ArrayList<>();
        }

        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

        public int getSuccessCount() { return successCount; }
        public void incrementSuccess() { this.successCount++; }

        public int getErrorCount() { return errorCount; }

        public List<String> getErrors() { return errors; }
        public void addError(String error) { 
            this.errors.add(error); 
            this.errorCount++;
        }

        public boolean hasErrors() { return errorCount > 0; }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Импорт завершён.\n");
            sb.append("Всего строк: ").append(totalRows).append("\n");
            sb.append("Успешно: ").append(successCount).append("\n");
            sb.append("Ошибок: ").append(errorCount);
            return sb.toString();
        }

        public String getDetailedReport() {
            StringBuilder sb = new StringBuilder(getSummary());
            if (!errors.isEmpty()) {
                sb.append("\n\nОшибки:\n");
                for (int i = 0; i < Math.min(errors.size(), 10); i++) {
                    sb.append("• ").append(errors.get(i)).append("\n");
                }
                if (errors.size() > 10) {
                    sb.append("... и ещё ").append(errors.size() - 10).append(" ошибок");
                }
            }
            return sb.toString();
        }
    }

    /**
     * Импортирует сотрудников из Excel-файла.
     * @param file Excel-файл для импорта (.xlsx)
     * @param skipHeader пропустить первую строку (заголовок)
     * @return результат импорта
     */
    public ImportResult importFromExcel(File file, boolean skipHeader) {
        ImportResult result = new ImportResult();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int startRow = skipHeader ? 1 : 0;

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                result.setTotalRows(result.getTotalRows() + 1);
                int rowNum = i + 1; // Номер строки для пользователя (начиная с 1)

                try {
                    Employee employee = parseRow(row, rowNum);
                    if (employee != null) {
                        // Находим или создаём должность
                        int positionId = findOrCreatePosition(employee.getPositionTitle());
                        if (positionId == -1) {
                            result.addError("Строка " + rowNum + ": не удалось создать должность");
                            continue;
                        }
                        employee.setPositionId(positionId);

                        // Находим или создаём отдел
                        int departmentId = findOrCreateDepartment(employee.getDepartmentName());
                        if (departmentId == -1) {
                            result.addError("Строка " + rowNum + ": не удалось создать отдел");
                            continue;
                        }
                        employee.setDepartmentId(departmentId);

                        // Сохраняем сотрудника
                        if (employeeDAO.insert(employee)) {
                            result.incrementSuccess();
                        } else {
                            result.addError("Строка " + rowNum + ": ошибка сохранения (возможно, дубликат табельного номера или email)");
                        }
                    }
                } catch (Exception e) {
                    result.addError("Строка " + rowNum + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            result.addError("Ошибка чтения файла: " + e.getMessage());
        }

        return result;
    }

    /**
     * Проверяет, пустая ли строка.
     */
    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < 3; i++) { // Проверяем первые 3 ячейки
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellStringValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Парсит строку Excel и создаёт объект Employee.
     */
    private Employee parseRow(Row row, int rowNum) throws Exception {
        Employee employee = new Employee();

        // Колонка 0: Табельный номер (обязательно)
        String employeeNumber = getCellStringValue(row.getCell(0));
        if (employeeNumber == null || employeeNumber.trim().isEmpty()) {
            throw new Exception("табельный номер не может быть пустым");
        }
        employee.setEmployeeNumber(employeeNumber.trim());

        // Колонка 1: Фамилия (обязательно)
        String lastName = getCellStringValue(row.getCell(1));
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new Exception("фамилия не может быть пустой");
        }
        employee.setLastName(lastName.trim());

        // Колонка 2: Имя (обязательно)
        String firstName = getCellStringValue(row.getCell(2));
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new Exception("имя не может быть пустым");
        }
        employee.setFirstName(firstName.trim());

        // Колонка 3: Отчество (необязательно)
        String middleName = getCellStringValue(row.getCell(3));
        employee.setMiddleName(middleName != null ? middleName.trim() : "");

        // Колонка 4: Дата рождения (обязательно)
        LocalDate birthDate = getCellDateValue(row.getCell(4));
        if (birthDate == null) {
            throw new Exception("дата рождения обязательна");
        }
        employee.setBirthDate(birthDate);

        // Колонка 5: Дата приёма (обязательно)
        LocalDate hireDate = getCellDateValue(row.getCell(5));
        if (hireDate == null) {
            throw new Exception("дата приёма обязательна");
        }
        employee.setHireDate(hireDate);

        // Колонка 6: Должность (обязательно)
        String positionTitle = getCellStringValue(row.getCell(6));
        if (positionTitle == null || positionTitle.trim().isEmpty()) {
            throw new Exception("должность не может быть пустой");
        }
        employee.setPositionTitle(positionTitle.trim());

        // Колонка 7: Отдел (обязательно)
        String departmentName = getCellStringValue(row.getCell(7));
        if (departmentName == null || departmentName.trim().isEmpty()) {
            throw new Exception("отдел не может быть пустым");
        }
        employee.setDepartmentName(departmentName.trim());

        // Колонка 8: Email (необязательно)
        String email = getCellStringValue(row.getCell(8));
        employee.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);

        // Колонка 9: Телефон (необязательно)
        String phone = getCellStringValue(row.getCell(9));
        employee.setPhone(phone != null && !phone.trim().isEmpty() ? phone.trim() : null);

        // Колонка 10: Уровень допуска (по умолчанию 1)
        int clearance = getCellIntValue(row.getCell(10), 1);
        if (clearance < 1 || clearance > 5) {
            throw new Exception("уровень допуска должен быть от 1 до 5");
        }
        employee.setClearanceLevel(clearance);

        // Колонка 11: Статус (по умолчанию активен)
        boolean isActive = getCellBooleanValue(row.getCell(11), true);
        employee.setActive(isActive);

        return employee;
    }
    
    /**
     * Получает булево значение из ячейки.
     * Распознаёт: "Активный", "Да", "Yes", "1", "true" как true
     * "Неактивный", "Нет", "No", "0", "false" как false
     */
    private boolean getCellBooleanValue(Cell cell, boolean defaultValue) {
        if (cell == null) {
            return defaultValue;
        }
        
        try {
            if (cell.getCellType() == CellType.BOOLEAN) {
                return cell.getBooleanCellValue();
            } else if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue() != 0;
            } else if (cell.getCellType() == CellType.STRING) {
                String str = cell.getStringCellValue().trim().toLowerCase();
                if (str.isEmpty()) {
                    return defaultValue;
                }
                // Проверяем на "неактивный" и подобные
                if (str.contains("неактив") || str.contains("нет") || str.equals("no") || 
                    str.equals("false") || str.equals("0")) {
                    return false;
                }
                // Проверяем на "активный" и подобные
                if (str.contains("актив") || str.contains("да") || str.equals("yes") || 
                    str.equals("true") || str.equals("1")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ошибка парсинга
        }
        return defaultValue;
    }

    /**
     * Получает строковое значение ячейки.
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return null; // Дата обрабатывается отдельно
                }
                // Для чисел убираем десятичную часть если она .0
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    /**
     * Получает дату из ячейки.
     */
    private LocalDate getCellDateValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                String dateStr = cell.getStringCellValue().trim();
                return parseDate(dateStr);
            }
        } catch (Exception e) {
            // Ошибка парсинга даты
        }
        return null;
    }

    /**
     * Парсит дату из строки.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        // Пробуем разные форматы
        String[] patterns = {"dd.MM.yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy"};
        
        for (String pattern : patterns) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception e) {
                // Пробуем следующий формат
            }
        }
        return null;
    }

    /**
     * Получает целочисленное значение ячейки.
     */
    private int getCellIntValue(Cell cell, int defaultValue) {
        if (cell == null) {
            return defaultValue;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String str = cell.getStringCellValue().trim();
                if (!str.isEmpty()) {
                    // Извлекаем первую цифру (для случаев типа "2 - ДСП")
                    if (Character.isDigit(str.charAt(0))) {
                        return Integer.parseInt(str.substring(0, 1));
                    }
                }
            }
        } catch (Exception e) {
            // Ошибка парсинга
        }
        return defaultValue;
    }

    /**
     * Находит должность по названию или создаёт новую.
     */
    private int findOrCreatePosition(String title) {
        List<Position> positions = positionDAO.findAll();
        for (Position p : positions) {
            if (p.getTitle().equalsIgnoreCase(title)) {
                return p.getId();
            }
        }

        // Создаём новую должность
        Position newPosition = new Position();
        newPosition.setTitle(title);
        newPosition.setSecurityLevel(1);
        return positionDAO.insert(newPosition);
    }

    /**
     * Находит отдел по названию или создаёт новый.
     */
    private int findOrCreateDepartment(String name) {
        List<Department> departments = departmentDAO.findAll();
        for (Department d : departments) {
            if (d.getName().equalsIgnoreCase(name)) {
                return d.getId();
            }
        }

        // Создаём новый отдел
        Department newDepartment = new Department();
        newDepartment.setName(name);
        return departmentDAO.insert(newDepartment);
    }
}
