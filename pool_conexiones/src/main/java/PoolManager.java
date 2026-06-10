package main.java;

import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Administrador del pool que permite operaciones como grow/shrink
 * Actúa como una capa de abstracción sobre Pool
 */
public class PoolManager {
    private final Pool pool;
    private final ReentrantLock lock = new ReentrantLock();
    
    public PoolManager(int initialSize, int maxSize, DatabaseProvider provider) throws SQLException {
        this.pool = Pool.getInstance(initialSize, maxSize, provider);
    }
    
    /**
     * Obtiene una conexión del pool
     */
    public RealConnection getConn(long timeoutMillis) throws InterruptedException, SQLException {
        return pool.getConn(timeoutMillis);
    }
    
    /**
     * Devuelve una conexión al pool
     */
    public void releaseConn(RealConnection conn) {
        pool.releaseConn(conn);
    }
    
    /**
     * Intenta aumentar el tamaño del pool (demostración)
     */
    public void grow() {
        lock.lock();
        try {
            System.out.println("⚠️ Pool grow: Operación demostrativa - no se implementa crecimiento dinámico");
            // En una implementación real, aquí se agregarían nuevas conexiones
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Intenta reducir el tamaño del pool (demostración)
     */
    public void shrink() {
        lock.lock();
        try {
            System.out.println("⚠️ Pool shrink: Operación demostrativa - no se implementa reducción dinámica");
            // En una implementación real, aquí se removerían conexiones inactivas
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene estadísticas del pool
     */
    public void printStats() {
        System.out.printf("Pool Stats - Activas: %d, Disponibles: %d%n",
                pool.getActiveCount(), pool.getAvailableCount());
    }
}