package com.ibsecurity.util;

import com.ibsecurity.model.Employee;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExporter {

    public static boolean exportEmployees(List<Employee> employees, String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Сотрудники ИБ");

            // Стиль заголовка
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Стиль данных
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Заголовки
            String[] headers = {
                "Таб. номер", "Фамилия", "Имя", "Отчество", 
                "Дата рождения", "Дата приёма", "Должность", "Отдел",
                "Email", "Телефон", "Уровень допуска", "Статус"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Данные
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            int rowNum = 1;

            for (Employee emp : employees) {
                Row row = sheet.createRow(rowNum++);

                createCell(row, 0, emp.getEmployeeNumber(), dataStyle);
                createCell(row, 1, emp.getLastName(), dataStyle);
                createCell(row, 2, emp.getFirstName(), dataStyle);
                createCell(row, 3, emp.getMiddleName(), dataStyle);
                createCell(row, 4, emp.getBirthDate() != null ? emp.getBirthDate().format(dateFormatter) : "", dataStyle);
                createCell(row, 5, emp.getHireDate() != null ? emp.getHireDate().format(dateFormatter) : "", dataStyle);
                createCell(row, 6, emp.getPositionTitle(), dataStyle);
                createCell(row, 7, emp.getDepartmentName(), dataStyle);
                createCell(row, 8, emp.getEmail(), dataStyle);
                createCell(row, 9, emp.getPhone(), dataStyle);
                createCell(row, 10, getClearanceText(emp.getClearanceLevel()), dataStyle);
                createCell(row, 11, emp.isActive() ? "Активный" : "Неактивный", dataStyle);
            }

            // Автоширина колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Сохранение файла
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private static String getClearanceText(int level) {
        return switch (level) {
            case 1 -> "1 - Открытый";
            case 2 -> "2 - ДСП";
            case 3 -> "3 - Секретно";
            case 4 -> "4 - Сов. секретно";
            case 5 -> "5 - Особой важности";
            default -> String.valueOf(level);
        };
    }
}
