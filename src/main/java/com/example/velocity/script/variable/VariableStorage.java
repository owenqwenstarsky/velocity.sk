package com.example.velocity.script.variable;

import org.slf4j.Logger;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class VariableStorage {
    private final Logger logger;
    private final File databaseFile;
    private Connection connection;

    public VariableStorage(Logger logger, File databaseFile) {
        this.logger = logger;
        this.databaseFile = databaseFile;
    }

    public void initialize() throws SQLException {
        try {
            // Ensure parent directory exists
            if (!databaseFile.getParentFile().exists()) {
                databaseFile.getParentFile().mkdirs();
            }

            // Explicitly load the SQLite JDBC driver
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not found. Make sure sqlite-jdbc is bundled.", e);
            }

            // Connect to SQLite database
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            // Create variables table if it doesn't exist
            createTable();
            
            logger.info("Variable storage initialized at: {}", databaseFile.getAbsolutePath());
        } catch (SQLException e) {
            logger.error("Failed to initialize variable storage", e);
            throw e;
        }
    }

    private void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS variables (
                name TEXT PRIMARY KEY,
                value TEXT,
                type TEXT,
                updated_at INTEGER
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void saveVariable(String name, String value, String type) throws SQLException {
        String sql = "INSERT OR REPLACE INTO variables (name, value, type, updated_at) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, value);
            pstmt.setString(3, type);
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.executeUpdate();
        }
    }

    public String loadVariable(String name) throws SQLException {
        String sql = "SELECT value FROM variables WHERE name = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        }
        
        return null;
    }

    public boolean variableExists(String name) throws SQLException {
        String sql = "SELECT 1 FROM variables WHERE name = ? LIMIT 1";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void deleteVariable(String name) throws SQLException {
        String sql = "DELETE FROM variables WHERE name = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }

    public Map<String, String> loadAllVariables() throws SQLException {
        Map<String, String> variables = new HashMap<>();
        String sql = "SELECT name, value FROM variables";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                variables.put(rs.getString("name"), rs.getString("value"));
            }
        }
        
        return variables;
    }

    public Map<String, String> loadVariablesByPrefix(String prefix) throws SQLException {
        Map<String, String> variables = new HashMap<>();
        String sql = "SELECT name, value FROM variables WHERE name LIKE ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, prefix + "%");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    variables.put(rs.getString("name"), rs.getString("value"));
                }
            }
        }
        
        return variables;
    }

    public void deleteVariablesByPrefix(String prefix) throws SQLException {
        String sql = "DELETE FROM variables WHERE name LIKE ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, prefix + "%");
            pstmt.executeUpdate();
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Variable storage closed");
            } catch (SQLException e) {
                logger.error("Error closing variable storage", e);
            }
        }
    }

    public int getVariableCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM variables";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        return 0;
    }
}

