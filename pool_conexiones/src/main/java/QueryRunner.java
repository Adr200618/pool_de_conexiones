package main.java;

import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ejecuta una query en un hilo separado
 * Puede usar pool o crear conexiones nuevas
 */
public class QueryRunner implements Callable<String> {
    private final int queryId;
    private final boolean usePool;
    private final PoolManager poolManager;
    private final long startTime;
    private final DatabaseProvider provider;
    
    // Contadores estáticos para todas las queries
    private static final AtomicInteger completedQueries = new AtomicInteger(0);
    private static final AtomicInteger failedQueries = new AtomicInteger(0);
    
    public QueryRunner(int queryId, boolean usePool, PoolManager poolManager, 
                       long startTime, DatabaseProvider provider) {
        this.queryId = queryId;
        this.usePool = usePool;
        this.poolManager = poolManager;
        this.startTime = startTime;
        this.provider = provider;
    }
    
    public void run() {
        try {
            System.out.print(call());
        } catch (Exception e) {
            System.err.printf("[%s] Query %d inesperado: %s%n", provider.getName(), queryId, e.getMessage());
        }
    }
    
    @Override
    public String call() {
        if (usePool) {
            return runWithPool();
        } else {
            return runWithoutPool();
        }
    }
    
    /**
     * Ejecuta la query usando el pool de conexiones
     */
    private String runWithPool() {
        RealConnection conn = null;
        try {
            long waitStart = System.nanoTime();
            conn = poolManager.getConn(5000);
            long waitTime = (System.nanoTime() - waitStart) / 1_000_000;
            
            String sql = provider.getSelectSQL(queryId);
            conn.query(sql);
            
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000;
            
            completedQueries.incrementAndGet();
            return String.format("[%s - POOL] Query %d completada en %d ms (espera: %d ms)%n",
                    provider.getName(), queryId, duration, waitTime);
        } catch (InterruptedException | SQLException e) {
            failedQueries.incrementAndGet();
            return String.format("[%s - POOL] Query %d falló: %s%n", 
                    provider.getName(), queryId, e.getMessage());
        } finally {
            if (conn != null) {
                poolManager.releaseConn(conn);
            }
        }
    }
    
    /**
     * Ejecuta la query sin pool (crea una nueva conexión cada vez)
     */
    private String runWithoutPool() {
        RealConnection conn = null;
        try {
            long startConn = System.nanoTime();
            conn = new RealConnection(-1, provider);
            conn.connect();
            long connTime = (System.nanoTime() - startConn) / 1_000_000;
            
            String sql = provider.getSelectSQL(queryId);
            conn.query(sql);
            
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000;
            
            completedQueries.incrementAndGet();
            return String.format("[%s - NO POOL] Query %d completada en %d ms (conexión: %d ms)%n",
                    provider.getName(), queryId, duration, connTime);
        } catch (SQLException e) {
            failedQueries.incrementAndGet();
            return String.format("[%s - NO POOL] Query %d falló: %s%n", 
                    provider.getName(), queryId, e.getMessage());
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
    
    // Métodos estáticos para manejar los contadores
    public static int getCompletedCount() { 
        return completedQueries.get(); 
    }
    
    public static int getFailedCount() { 
        return failedQueries.get(); 
    }
    
    public static void resetCounters() { 
        completedQueries.set(0); 
        failedQueries.set(0);
    }
}