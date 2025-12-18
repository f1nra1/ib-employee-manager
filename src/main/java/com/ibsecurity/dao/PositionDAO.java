package com.ibsecurity.dao;

import com.ibsecurity.model.Position;
import com.ibsecurity.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PositionDAO {

    public List<Position> findAll() {
        List<Position> positions = new ArrayList<>();
        String sql = "SELECT * FROM positions ORDER BY title";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Position position = new Position();
                position.setId(rs.getInt("id"));
                position.setTitle(rs.getString("title"));
                position.setDescription(rs.getString("description"));
                position.setSecurityLevel(rs.getInt("security_level"));
                positions.add(position);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return positions;
    }

    public Position findById(int id) {
        String sql = "SELECT * FROM positions WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Position position = new Position();
                position.setId(rs.getInt("id"));
                position.setTitle(rs.getString("title"));
                position.setDescription(rs.getString("description"));
                position.setSecurityLevel(rs.getInt("security_level"));
                return position;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int insert(Position position) {
        String sql = "INSERT INTO positions (title, description, security_level) VALUES (?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, position.getTitle());
            stmt.setString(2, position.getDescription() != null ? position.getDescription() : "");
            stmt.setInt(3, position.getSecurityLevel() > 0 ? position.getSecurityLevel() : 1);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}