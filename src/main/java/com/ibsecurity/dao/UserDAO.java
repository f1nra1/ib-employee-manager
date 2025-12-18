package com.ibsecurity.dao;

import com.ibsecurity.model.User;
import com.ibsecurity.util.DatabaseConnection;
import com.ibsecurity.util.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Access Object для работы с пользователями.
 */
public class UserDAO {
    
    private static final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private static final Map<String, Long> lockoutTime = new ConcurrentHashMap<>();
    
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000;
    
    private static boolean initialized = false;

    public UserDAO() {
        initDefaultUsers();
    }
    
    private synchronized void initDefaultUsers() {
        if (initialized) return;
        initialized = true;
        
        try {
            if (!usernameExists("admin")) {
                String sql = "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, "admin");
                    stmt.setString(2, PasswordUtils.hashPassword("admin123"));
                    stmt.setString(3, "Администратор");
                    stmt.setString(4, "admin");
                    stmt.executeUpdate();
                }
            }
            if (!usernameExists("user")) {
                String sql = "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, "user");
                    stmt.setString(2, PasswordUtils.hashPassword("user123"));
                    stmt.setString(3, "Пользователь");
                    stmt.setString(4, "user");
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            // Игнорируем - пользователи уже существуют
        }
    }

    /**
     * Аутентификация пользователя.
     */
    public User authenticate(String username, String password) {
        if (isAccountLocked(username)) {
            return null;
        }

        String sql = "SELECT * FROM users WHERE username = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                
                if (PasswordUtils.verifyPassword(password, storedHash)) {
                    resetFailedAttempts(username);
                    User user = mapResultSetToUser(rs);
                    logAccess(user.getId(), "LOGIN", "Вход в систему");
                    return user;
                }
            }
            
            incrementFailedAttempts(username);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Регистрация нового пользователя.
     */
    public boolean register(String username, String password, String fullName) {
        if (username == null || username.trim().length() < 3) {
            return false;
        }
        if (password == null || password.length() < 6) {
            return false;
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            return false;
        }
        if (usernameExists(username)) {
            return false;
        }
        
        String sql = "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, 'user')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim().toLowerCase());
            stmt.setString(2, PasswordUtils.hashPassword(password));
            stmt.setString(3, fullName.trim());

            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Проверка существования пользователя.
     */
    public boolean usernameExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    /**
     * Получение списка всех пользователей.
     */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, full_name, role, is_active, created_at FROM users ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setFullName(rs.getString("full_name"));
                user.setRole(rs.getString("role"));
                user.setActive(rs.getBoolean("is_active"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) {
                    user.setCreatedAt(ts.toLocalDateTime());
                }
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Назначение прав администратора.
     */
    public boolean setAdmin(int userId, boolean isAdmin) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, isAdmin ? "admin" : "user");
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Активация/блокировка учётной записи.
     */
    public boolean setActive(int userId, boolean isActive) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, isActive);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Удаление пользователя.
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isAccountLocked(String username) {
        Long lockTime = lockoutTime.get(username);
        if (lockTime == null) {
            return false;
        }
        if (System.currentTimeMillis() - lockTime > LOCKOUT_DURATION_MS) {
            lockoutTime.remove(username);
            failedAttempts.remove(username);
            return false;
        }
        return true;
    }

    private void incrementFailedAttempts(String username) {
        int attempts = failedAttempts.getOrDefault(username, 0) + 1;
        failedAttempts.put(username, attempts);
        
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            lockoutTime.put(username, System.currentTimeMillis());
        }
    }

    private void resetFailedAttempts(String username) {
        failedAttempts.remove(username);
        lockoutTime.remove(username);
    }

    /**
     * Запись в журнал.
     */
    public void logAccess(int userId, String actionType, String description) {
        String sql = "INSERT INTO access_log (employee_id, action_type, action_description) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (userId > 0) {
                stmt.setInt(1, userId);
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, actionType);
            stmt.setString(3, description);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            // Игнорируем ошибки логирования
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    }
    
    public String hashPassword(String password) {
        return PasswordUtils.hashPassword(password);
    }
}
