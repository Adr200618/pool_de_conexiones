package main.java;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Representa una conexión real a la base de datos
 * Encapsula la conexión JDBC y su estado
 */
public class RealConnection {
    private final Connection sqlConnection;
    private final int id;
    private boolean inUse;
    private final DatabaseProvider provider;
    
    public RealConnection(int id, DatabaseProvider provider) throws SQLException {
        this.id = id;
        this.provider = provider;
        this.sqlConnection = provider.createConnection();
        this.sqlConnection.setAutoCommit(true);
        this.inUse = false;
    }
    
    /**
     * Marca la conexión como en uso
     */
    public void connect() throws SQLException {
        inUse = true;
        if (sqlConnection.isClosed()) {
            throw new SQLException("Connection is closed");
        }
    }
    
    /**
     * Ejecuta una query SELECT
     */
    public void query(String sql) throws SQLException {
        try (Statement stmt = sqlConnection.createStatement()) {
            stmt.executeQuery(sql);
        }
    }
    
    /**
     * Ejecuta INSERT, UPDATE o DELETE
     */
    public void update(String sql) throws SQLException {
        try (Statement stmt = sqlConnection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
    
    /**
     * Cierra la conexión y la libera
     */
    public void close() {
        try {
            if (sqlConnection != null && !sqlConnection.isClosed()) {
                sqlConnection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
        inUse = false;
    }
    
    // Getters
    public boolean isInUse() { return inUse; }
    public int getId() { return id; }
    public boolean isClosed() throws SQLException { return sqlConnection.isClosed(); }
    public DatabaseProvider getProvider() { return provider; }
}