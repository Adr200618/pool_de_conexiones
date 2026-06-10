package main.java;

import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Pool de conexiones implementado como Singleton
 * Mantiene un conjunto de conexiones reutilizables
 */
public class Pool {
    private static Pool instance;
    private final BlockingQueue<RealConnection> availableConnections;
    private final List<RealConnection> allConnections;
    private final int maxSize;
    private final AtomicInteger activeCount = new AtomicInteger(0);
    private final DatabaseProvider provider;
    
    /**
     * Constructor privado (patrón Singleton)
     */
    private Pool(int initialSize, int maxSize, DatabaseProvider provider) throws SQLException {
        this.maxSize = maxSize;
        this.provider = provider;
        this.availableConnections = new LinkedBlockingQueue<>(maxSize);
        this.allConnections = new ArrayList<>(maxSize);
        
        // Crear las conexiones iniciales
        for (int i = 0; i < initialSize; i++) {
            RealConnection conn = new RealConnection(i, provider);
            availableConnections.add(conn);
            allConnections.add(conn);
        }
    }
    
    /**
     * Obtiene la instancia única del pool (Singleton)
     */
    public static synchronized Pool getInstance(int initialSize, int maxSize, DatabaseProvider provider) 
            throws SQLException {
        if (instance == null) {
            instance = new Pool(initialSize, maxSize, provider);
        }
        return instance;
    }
    
    /**
     * Obtiene la instancia existente (debe haberse inicializado antes)
     */
    public static Pool getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Pool no inicializado. Llame a getInstance con parámetros primero.");
        }
        return instance;
    }
    
    /**
     * Obtiene una conexión del pool, esperando hasta timeoutMillis
     */
    public RealConnection getConn(long timeoutMillis) throws InterruptedException, SQLException {
        RealConnection conn = availableConnections.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        if (conn == null) {
            throw new SQLException("Timeout: No available connections after " + timeoutMillis + "ms");
        }
        conn.connect();
        activeCount.incrementAndGet();
        return conn;
    }
    
    /**
     * Devuelve una conexión al pool
     */
    public void releaseConn(RealConnection conn) {
        if (conn != null) {
            conn.close();
            availableConnections.add(conn);
            activeCount.decrementAndGet();
        }
    }
    
    /**
     * Obtiene el número de conexiones activas
     */
    public int getActiveCount() { 
        return activeCount.get(); 
    }
    
    /**
     * Obtiene el número de conexiones disponibles
     */
    public int getAvailableCount() { 
        return availableConnections.size(); 
    }
    
    /**
     * Cierra todas las conexiones del pool
     */
    public void closeAll() {
        for (RealConnection conn : allConnections) {
            conn.close();
        }
    }
}