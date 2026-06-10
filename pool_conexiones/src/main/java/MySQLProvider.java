package main.java;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Implementación para MySQL
 */
public class MySQLProvider implements DatabaseProvider {
    private final String url;
    private final String user;
    private final String password;
    
    public MySQLProvider(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        
        // Registrar el driver automáticamente
        try {
            Class.forName(getDriverClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver MySQL no encontrado", e);
        }
    }
    
    @Override
    public Connection createConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
    
    @Override
    public String getDriverClass() {
        return "com.mysql.cj.jdbc.Driver";
    }
    
    @Override
    public String getTestQuery() {
        return "SELECT 1";
    }
    
    @Override
    public String getCreateTableSQL() {
        return """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INT PRIMARY KEY,
                nombre VARCHAR(100),
                email VARCHAR(100) UNIQUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
    }
    
    @Override
    public String getInsertSQL(int id, String nombre, String email) {
        return String.format(
            "INSERT IGNORE INTO usuarios (id, nombre, email) VALUES (%d, '%s', '%s')",
            id, nombre, email
        );
    }
    
    @Override
    public String getSelectSQL(int id) {
        return "SELECT * FROM usuarios WHERE id = " + id;
    }
    
    @Override
    public String getName() {
        return "MySQL";
    }
}