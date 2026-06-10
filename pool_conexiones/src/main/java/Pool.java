package main.java;


import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

public class Pool {
    private static Pool instance;
    private final BlockingQueue<RealConnection> availableConnections;
    private final List<RealConnection> allConnections;
    private final int maxSize;
    private final AtomicInteger activeCount = new AtomicInteger(0);
    private final DatabaseProvider provider;
    
    private Pool(int initialSize, int maxSize, DatabaseProvider provider) throws SQLException {
        this.maxSize = maxSize;
        this.provider = provider;
        this.availableConnections = new LinkedBlockingQueue<>(maxSize);
        this.allConnections = new ArrayList<>(maxSize);
        
        for (int i = 0; i < initialSize; i++) {
            RealConnection conn = new RealConnection(i, provider);
            availableConnections.add(conn);
            allConnections.add(conn);
        }
    }
    
    public static synchronized Pool getInstance(int initialSize, int maxSize, DatabaseProvider provider) 
            throws SQLException {
        if (instance == null) {
            instance = new Pool(initialSize, maxSize, provider);
        }
        return instance;
    }
    
    public static Pool getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Pool no inicializado.");
        }
        return instance;
    }
    
    public RealConnection getConn(long timeoutMillis) throws InterruptedException, SQLException {
        RealConnection conn = availableConnections.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        if (conn == null) {
            throw new SQLException("Timeout: No available connections after " + timeoutMillis + "ms");
        }
        // Si la conexión está cerrada, la reemplazamos
        if (conn.isClosed()) {
            conn = new RealConnection(conn.getId(), provider);
            // Reemplazar en la lista allConnections (opcional, pero recomendado)
            int index = allConnections.indexOf(conn);
            if (index >= 0) allConnections.set(index, conn);
        }
        conn.connect();
        activeCount.incrementAndGet();
        return conn;
    }
    
    public void releaseConn(RealConnection conn) {
        if (conn != null) {
            conn.reset();        // Limpia el estado, NO cierra
            availableConnections.add(conn);
            activeCount.decrementAndGet();
        }
    }
    
    public int getActiveCount() { return activeCount.get(); }
    public int getAvailableCount() { return availableConnections.size(); }
    
    public void closeAll() {
        for (RealConnection conn : allConnections) {
            conn.close();
        }
    }
}