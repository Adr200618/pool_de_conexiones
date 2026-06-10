package main.java;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interfaz que define el contrato para cualquier proveedor de base de datos
 * Permite agregar nuevos motores de BD fácilmente (Oracle, SQL Server, etc.)
 */
public interface DatabaseProvider {
    
    /**
     * Crea una nueva conexión a la base de datos
     */
    Connection createConnection() throws SQLException;
    
    /**
     * Retorna el nombre de la clase del driver JDBC
     */
    String getDriverClass();
    
    /**
     * Query de prueba para verificar la conexión
     */
    String getTestQuery();
    
    /**
     * SQL para crear la tabla de usuarios
     */
    String getCreateTableSQL();
    
    /**
     * SQL para insertar un usuario (con manejo de conflictos)
     */
    String getInsertSQL(int id, String nombre, String email);
    
    /**
     * SQL para seleccionar un usuario por ID
     */
    String getSelectSQL(int id);
    
    /**
     * Nombre del proveedor (para mostrar en logs)
     */
    String getName();
}