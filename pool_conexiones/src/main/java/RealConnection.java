package main.java;


import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class RealConnection {
    private Connection sqlConnection;
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
    
    public void connect() throws SQLException {
        if (sqlConnection == null || sqlConnection.isClosed()) {
            // Recrear la conexión si está cerrada
            this.sqlConnection = provider.createConnection();
            this.sqlConnection.setAutoCommit(true);
        }
        inUse = true;
    }
    
    public void reset() {
        try {
            if (sqlConnection != null && !sqlConnection.isClosed()) {
                if (!sqlConnection.getAutoCommit()) {
                    sqlConnection.rollback();
                    sqlConnection.setAutoCommit(true);
                }
                // Limpiar cualquier otro estado (por ejemplo, cerrar statements pendientes)
            }
        } catch (SQLException e) {
            System.err.println("Error resetting connection: " + e.getMessage());
        }
    }
    
    public void query(String sql) throws SQLException {
        try (Statement stmt = sqlConnection.createStatement()) {
            stmt.executeQuery(sql);
        }
    }
    
    public void update(String sql) throws SQLException {
        try (Statement stmt = sqlConnection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
    
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
    
    public boolean isInUse() { return inUse; }
    public int getId() { return id; }
    public boolean isClosed() throws SQLException { 
        return sqlConnection == null || sqlConnection.isClosed(); 
    }
    public DatabaseProvider getProvider() { return provider; }
}