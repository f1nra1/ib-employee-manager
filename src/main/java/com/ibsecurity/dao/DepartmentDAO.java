package com.ibsecurity.dao;

import com.ibsecurity.model.Department;
import com.ibsecurity.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {

    public List<Department> findAll() {
        List<Department> departments = new ArrayList<>();
        String sql = "SELECT * FROM departments ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Department department = new Department();
                department.setId(rs.getInt("id"));
                department.setName(rs.getString("name"));
                department.setCode(rs.getString("code"));
                department.setDescription(rs.getString("description"));
                departments.add(department);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return departments;
    }

    public Department findById(int id) {
        String sql = "SELECT * FROM departments WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Department department = new Department();
                department.setId(rs.getInt("id"));
                department.setName(rs.getString("name"));
                department.setCode(rs.getString("code"));
                department.setDescription(rs.getString("description"));
                return department;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int insert(Department department) {
        String sql = "INSERT INTO departments (name, code, description) VALUES (?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, department.getName());
            // Генерируем код из названия (первые буквы слов)
            String code = generateCode(department.getName());
            stmt.setString(2, code);
            stmt.setString(3, department.getDescription() != null ? department.getDescription() : "");

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private String generateCode(String name) {
        StringBuilder code = new StringBuilder();
        String[] words = name.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                code.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        // Добавляем случайное число для уникальности
        code.append(System.currentTimeMillis() % 1000);
        return code.toString();
    }
}