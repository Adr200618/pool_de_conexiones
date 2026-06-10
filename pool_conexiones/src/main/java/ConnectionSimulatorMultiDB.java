package main.java;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Clase principal que ejecuta las pruebas con múltiples bases de datos
 * Compara PostgreSQL vs MySQL con y sin pool de conexiones
 */
public class ConnectionSimulatorMultiDB {
    // Configuración de la prueba
    private static final int NUM_QUERIES = 50;
    private static final int NUM_THREADS = 10;
    private static final int POOL_INITIAL_SIZE = 5;
    private static final int POOL_MAX_SIZE = 10;
    
    // Configuración PostgreSQL (Windows)
    // Cambia estos valores según tu instalación
    private static final String PG_URL = "jdbc:postgresql://localhost:5432/testdb";
    private static final String PG_USER = "postgres";
    private static final String PG_PASSWORD = "32082324"; // Cambia por tu contraseña real de PostgreSQL
    
    // Configuración MySQL (Windows)
    // Cambia estos valores según tu instalación
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/testdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "32082324"; // Cambia por tu contraseña real de MySQL
    
    // Almacenar resultados para comparativa final
    private static long pgTimeWithoutPool = 0;
    private static long pgTimeWithPool = 0;
    private static long mysqlTimeWithoutPool = 0;
    private static long mysqlTimeWithPool = 0;
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║     SIMULADOR DE CONEXIONES CON MÚLTIPLES BASES DE DATOS      ║");
        System.out.println("║              PostgreSQL vs MySQL - WINDOWS                    ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        
        // Probar con PostgreSQL
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📊 POSTGRESQL");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            PostgreSQLProvider pgProvider = new PostgreSQLProvider(PG_URL, PG_USER, PG_PASSWORD);
            testDatabase(pgProvider);
        } catch (Exception e) {
            System.err.println("❌ Error con PostgreSQL: " + e.getMessage());
            System.err.println("   Asegúrate de que PostgreSQL esté corriendo y la BD 'testdb' exista");
            System.err.println("   En Windows: Services -> PostgreSQL -> Iniciar");
        }
        
        // Probar con MySQL
        System.out.println("\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📊 MYSQL");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            MySQLProvider mysqlProvider = new MySQLProvider(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
            testDatabase(mysqlProvider);
        } catch (Exception e) {
            System.err.println("❌ Error con MySQL: " + e.getMessage());
            System.err.println("   Asegúrate de que MySQL esté corriendo y la BD 'testdb' exista");
            System.err.println("   En Windows: Services -> MySQL -> Iniciar");
        }
        
        // Mostrar comparativa final
        showFinalComparison();
    }
    
    /**
     * Prueba una base de datos específica
     */
    private static void testDatabase(DatabaseProvider provider) {
        try {
            // Configurar base de datos
            setupDatabase(provider);
            insertTestData(provider);
            
            // Prueba sin pool
            System.out.println("\n📍 MODO SIN POOL");
            long timeWithoutPool = runTest(provider, false);
            
            // Prueba con pool
            System.out.println("\n📍 MODO CON POOL");
            long timeWithPool = runTest(provider, true);
            
            // Guardar resultados para comparativa
            if (provider.getName().equals("PostgreSQL")) {
                pgTimeWithoutPool = timeWithoutPool;
                pgTimeWithPool = timeWithPool;
            } else if (provider.getName().equals("MySQL")) {
                mysqlTimeWithoutPool = timeWithoutPool;
                mysqlTimeWithPool = timeWithPool;
            }
            
            // Mostrar resultados para esta BD
            System.out.println("\n📈 RESULTADOS PARA " + provider.getName());
            System.out.printf("  Sin Pool: %d ms%n", timeWithoutPool);
            System.out.printf("  Con Pool: %d ms%n", timeWithPool);
            
            if (timeWithoutPool > 0) {
                double improvement = (timeWithoutPool - timeWithPool) * 100.0 / timeWithoutPool;
                System.out.printf("  Mejora: %.1f%% más rápido con pool%n", improvement);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error probando " + provider.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crea la tabla si no existe
     */
    private static void setupDatabase(DatabaseProvider provider) throws SQLException {
        try (Connection conn = provider.createConnection()) {
            conn.createStatement().execute(provider.getCreateTableSQL());
            System.out.println("✅ Tabla 'usuarios' verificada/creada en " + provider.getName());
        }
    }
    
    /**
     * Inserta datos de prueba
     */
    private static void insertTestData(DatabaseProvider provider) throws SQLException {
        try (Connection conn = provider.createConnection()) {
            // Verificar cuántos registros hay
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM usuarios");
            rs.next();
            int count = rs.getInt(1);
            
            if (count < NUM_QUERIES) {
                System.out.println("📝 Insertando " + NUM_QUERIES + " registros de prueba en " + provider.getName() + "...");
                
                // Usar batch para mejor rendimiento
                conn.setAutoCommit(false);
                try (Statement stmt = conn.createStatement()) {
                    for (int i = 1; i <= NUM_QUERIES; i++) {
                        String insertSQL = provider.getInsertSQL(i, "Usuario" + i, "usuario" + i + "@test.com");
                        stmt.addBatch(insertSQL);
                        
                        // Ejecutar cada 1000 registros
                        if (i % 1000 == 0) {
                            stmt.executeBatch();
                        }
                    }
                    stmt.executeBatch();
                    conn.commit();
                }
                System.out.println("✅ Datos insertados en " + provider.getName());
            } else {
                System.out.println("✅ Datos ya existentes en " + provider.getName());
            }
        }
    }
    
    /**
     * Ejecuta las pruebas con o sin pool
     */
    private static long runTest(DatabaseProvider provider, boolean usePool) 
            throws InterruptedException, SQLException {
        
        QueryRunner.resetCounters();
        PoolManager poolManager = null;
        
        if (usePool) {
            poolManager = new PoolManager(POOL_INITIAL_SIZE, POOL_MAX_SIZE, provider);
            System.out.println("  Pool creado: " + POOL_INITIAL_SIZE + " conexiones iniciales");
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        List<Future<String>> futures = new ArrayList<>();
        long testStartTime = System.nanoTime();
        
        // Crear y ejecutar todas las queries
        for (int i = 1; i <= NUM_QUERIES; i++) {
            final int queryId = i;
            final long startTime = System.nanoTime();
            futures.add(executor.submit(new QueryRunner(queryId, usePool, poolManager, startTime, provider)));
        }
        
        executor.shutdown();

        // Imprimir resultados en orden de query
        for (Future<String> future : futures) {
            try {
                System.out.print(future.get());
            } catch (ExecutionException e) {
                System.err.println("Error en la ejecución de una query: " + e.getCause().getMessage());
            }
        }

        boolean finished = executor.awaitTermination(60, TimeUnit.SECONDS);
        
        long totalTime = (System.nanoTime() - testStartTime) / 1_000_000;
        
        if (!finished) {
            System.err.println("⚠️ Timeout en " + provider.getName());
            executor.shutdownNow();
        }
        
        // Mostrar estadísticas
        System.out.printf("  Queries completadas: %d/%d%n", 
                QueryRunner.getCompletedCount(), NUM_QUERIES);
        System.out.printf("  Queries fallidas: %d%n", QueryRunner.getFailedCount());
        System.out.printf("  Tiempo total: %d ms%n", totalTime);
        
        return totalTime;
    }
    
    /**
     * Muestra la comparativa final entre ambas bases de datos
     */
    private static void showFinalComparison() {
        System.out.println("\n\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    COMPARATIVA FINAL                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        
        System.out.println("\n┌───────────────────────┬──────────────┬──────────────┬─────────────┐");
        System.out.println("│ Base de Datos         │   Sin Pool   │   Con Pool   │   Mejora    │");
        System.out.println("├───────────────────────┼──────────────┼──────────────┼─────────────┤");
        
        // PostgreSQL
        if (pgTimeWithoutPool > 0) {
            double pgImprovement = (pgTimeWithoutPool - pgTimeWithPool) * 100.0 / pgTimeWithoutPool;
            System.out.printf("│ PostgreSQL            │ %10d ms │ %10d ms │ %9.1f%% │%n", 
                    pgTimeWithoutPool, pgTimeWithPool, pgImprovement);
        } else {
            System.out.println("│ PostgreSQL            │   ERROR      │   ERROR      │     -       │");
        }
        
        // MySQL
        if (mysqlTimeWithoutPool > 0) {
            double mysqlImprovement = (mysqlTimeWithoutPool - mysqlTimeWithPool) * 100.0 / mysqlTimeWithoutPool;
            System.out.printf("│ MySQL                 │ %10d ms │ %10d ms │ %9.1f%% │%n", 
                    mysqlTimeWithoutPool, mysqlTimeWithPool, mysqlImprovement);
        } else {
            System.out.println("│ MySQL                 │   ERROR      │   ERROR      │     -       │");
        }
        
        System.out.println("└───────────────────────┴──────────────┴──────────────┴─────────────┘");
        
        // Comparativa entre BDs con pool
        if (pgTimeWithPool > 0 && mysqlTimeWithPool > 0) {
            System.out.println("\n📊 Comparativa usando POOL de conexiones:");
            if (pgTimeWithPool < mysqlTimeWithPool) {
                double faster = (mysqlTimeWithPool - pgTimeWithPool) * 100.0 / mysqlTimeWithPool;
                System.out.printf("  ✅ PostgreSQL es %.1f%% más RÁPIDO que MySQL con pool%n", faster);
            } else if (mysqlTimeWithPool < pgTimeWithPool) {
                double faster = (pgTimeWithPool - mysqlTimeWithPool) * 100.0 / pgTimeWithPool;
                System.out.printf("  ✅ MySQL es %.1f%% más RÁPIDO que PostgreSQL con pool%n", faster);
            } else {
                System.out.println("  ⚖️ Ambas bases de datos tienen rendimiento similar");
            }
        }
        
        System.out.println("\n💡 Conclusión: El pool de conexiones mejora significativamente el rendimiento");
        System.out.println("   al reutilizar conexiones en lugar de crearlas para cada consulta.");
    }
}