package com.ibsecurity.dao;

import com.ibsecurity.model.Employee;
import com.ibsecurity.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    public List<Employee> findAll() {
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT e.*, p.title as position_title, d.name as department_name " +
                     "FROM employees e " +
                     "JOIN positions p ON e.position_id = p.id " +
                     "JOIN departments d ON e.department_id = d.id " +
                     "ORDER BY e.last_name, e.first_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return employees;
    }

    public Employee findById(int id) {
        String sql = "SELECT e.*, p.title as position_title, d.name as department_name " +
                     "FROM employees e " +
                     "JOIN positions p ON e.position_id = p.id " +
                     "JOIN departments d ON e.department_id = d.id " +
                     "WHERE e.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEmployee(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insert(Employee employee) {
        String sql = "INSERT INTO employees (employee_number, last_name, first_name, middle_name, " +
                     "birth_date, hire_date, position_id, department_id, email, phone, clearance_level, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, employee.getEmployeeNumber());
            stmt.setString(2, employee.getLastName());
            stmt.setString(3, employee.getFirstName());
            stmt.setString(4, employee.getMiddleName());
            stmt.setDate(5, Date.valueOf(employee.getBirthDate()));
            stmt.setDate(6, Date.valueOf(employee.getHireDate()));
            stmt.setInt(7, employee.getPositionId());
            stmt.setInt(8, employee.getDepartmentId());
            
            // Email: пустая строка -> NULL (для корректной работы UNIQUE constraint)
            String email = employee.getEmail();
            if (email == null || email.trim().isEmpty()) {
                stmt.setNull(9, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(9, email.trim());
            }
            
            // Phone: пустая строка -> NULL
            String phone = employee.getPhone();
            if (phone == null || phone.trim().isEmpty()) {
                stmt.setNull(10, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(10, phone.trim());
            }
            
            stmt.setInt(11, employee.getClearanceLevel());
            stmt.setBoolean(12, employee.isActive());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(Employee employee) {
        String sql = "UPDATE employees SET employee_number = ?, last_name = ?, first_name = ?, " +
                     "middle_name = ?, birth_date = ?, hire_date = ?, position_id = ?, " +
                     "department_id = ?, email = ?, phone = ?, clearance_level = ?, is_active = ? " +
                     "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, employee.getEmployeeNumber());
            stmt.setString(2, employee.getLastName());
            stmt.setString(3, employee.getFirstName());
            stmt.setString(4, employee.getMiddleName());
            stmt.setDate(5, Date.valueOf(employee.getBirthDate()));
            stmt.setDate(6, Date.valueOf(employee.getHireDate()));
            stmt.setInt(7, employee.getPositionId());
            stmt.setInt(8, employee.getDepartmentId());
            
            // Email: пустая строка -> NULL
            String email = employee.getEmail();
            if (email == null || email.trim().isEmpty()) {
                stmt.setNull(9, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(9, email.trim());
            }
            
            // Phone: пустая строка -> NULL
            String phone = employee.getPhone();
            if (phone == null || phone.trim().isEmpty()) {
                stmt.setNull(10, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(10, phone.trim());
            }
            
            stmt.setInt(11, employee.getClearanceLevel());
            stmt.setBoolean(12, employee.isActive());
            stmt.setInt(13, employee.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM employees WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Employee> search(String searchTerm) {
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT e.*, p.title as position_title, d.name as department_name " +
                     "FROM employees e " +
                     "JOIN positions p ON e.position_id = p.id " +
                     "JOIN departments d ON e.department_id = d.id " +
                     "WHERE LOWER(e.last_name) LIKE ? OR LOWER(e.first_name) LIKE ? " +
                     "OR LOWER(e.employee_number) LIKE ? OR LOWER(e.email) LIKE ? " +
                     "ORDER BY e.last_name, e.first_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String pattern = "%" + searchTerm.toLowerCase() + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return employees;
    }

    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        Employee employee = new Employee();
        employee.setId(rs.getInt("id"));
        employee.setEmployeeNumber(rs.getString("employee_number"));
        employee.setLastName(rs.getString("last_name"));
        employee.setFirstName(rs.getString("first_name"));
        employee.setMiddleName(rs.getString("middle_name"));
        employee.setBirthDate(rs.getDate("birth_date").toLocalDate());
        employee.setHireDate(rs.getDate("hire_date").toLocalDate());
        employee.setPositionId(rs.getInt("position_id"));
        employee.setDepartmentId(rs.getInt("department_id"));
        employee.setEmail(rs.getString("email"));
        employee.setPhone(rs.getString("phone"));
        employee.setClearanceLevel(rs.getInt("clearance_level"));
        employee.setActive(rs.getBoolean("is_active"));
        employee.setPositionTitle(rs.getString("position_title"));
        employee.setDepartmentName(rs.getString("department_name"));
        return employee;
    }
}
