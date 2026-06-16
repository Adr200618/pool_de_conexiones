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
    private static final int NUM_QUERIES = 10000;
    private static final int NUM_THREADS = 10000;
    private static final int POOL_INITIAL_SIZE = 10;
    private static final int POOL_MAX_SIZE = 30;
    
    // Configuración PostgreSQL (Windows)
    // Cambia estos valores según tu instalación
    // Desactivar SSL si el servidor no lo soporta y añadir timeouts para evitar bloqueos
    private static final String PG_URL = "jdbc:postgresql://localhost:5432/testdb?sslmode=disable&connectTimeout=10&socketTimeout=30";
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
    private static int pgCompletedWithoutPool = 0;
    private static int pgFailedWithoutPool = 0;
    private static int pgCompletedWithPool = 0;
    private static int pgFailedWithPool = 0;

    private static long mysqlTimeWithoutPool = 0;
    private static long mysqlTimeWithPool = 0;
    private static int mysqlCompletedWithoutPool = 0;
    private static int mysqlFailedWithoutPool = 0;
    private static int mysqlCompletedWithPool = 0;
    private static int mysqlFailedWithPool = 0;
    
    public static void main(String[] args) {
        
        // Probar con PostgreSQL
        System.out.println("===========================================================");
        System.out.println(" POSTGRESQL");
        System.out.println("===========================================================");
        
        try {
            PostgreSQLProvider pgProvider = new PostgreSQLProvider(PG_URL, PG_USER, PG_PASSWORD);
            testDatabase(pgProvider);
        } catch (Exception e) {
            System.err.println(" Error con PostgreSQL: " + e.getMessage());
            System.err.println("   Asegúrate de que PostgreSQL esté corriendo y la BD 'testdb' exista");
            System.err.println("   En Windows: Services -> PostgreSQL -> Iniciar");
        }
        
        // Probar con MySQL
        System.out.println("\n\n===========================================================");
        System.out.println(" MYSQL");
        System.out.println("===========================================================");
        
        try {
            MySQLProvider mysqlProvider = new MySQLProvider(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
            testDatabase(mysqlProvider);
        } catch (Exception e) {
            System.err.println(" Error con MySQL: " + e.getMessage());
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
            System.out.println("\n MODO SIN POOL");
            TestResult resultWithoutPool = runTest(provider, false);
            
            // Prueba con pool
            System.out.println("\n MODO CON POOL");
            TestResult resultWithPool = runTest(provider, true);
            
            // Guardar resultados para comparativa
            if (provider.getName().equals("PostgreSQL")) {
                pgTimeWithoutPool = resultWithoutPool.timeMs;
                pgTimeWithPool = resultWithPool.timeMs;
                pgCompletedWithoutPool = resultWithoutPool.completed;
                pgFailedWithoutPool = resultWithoutPool.failed;
                pgCompletedWithPool = resultWithPool.completed;
                pgFailedWithPool = resultWithPool.failed;
            } else if (provider.getName().equals("MySQL")) {
                mysqlTimeWithoutPool = resultWithoutPool.timeMs;
                mysqlTimeWithPool = resultWithPool.timeMs;
                mysqlCompletedWithoutPool = resultWithoutPool.completed;
                mysqlFailedWithoutPool = resultWithoutPool.failed;
                mysqlCompletedWithPool = resultWithPool.completed;
                mysqlFailedWithPool = resultWithPool.failed;
            }
            
            // Mostrar resultados para esta BD
            System.out.println("\nRESULTADOS PARA " + provider.getName());
            System.out.printf("  Sin Pool: %d ms | Completadas: %d | Fallidas: %d%n", 
                    resultWithoutPool.timeMs, resultWithoutPool.completed, resultWithoutPool.failed);
            System.out.printf("  Con Pool: %d ms | Completadas: %d | Fallidas: %d%n", 
                    resultWithPool.timeMs, resultWithPool.completed, resultWithPool.failed);
            
            if (resultWithoutPool.timeMs > 0) {
                double improvement = (resultWithoutPool.timeMs - resultWithPool.timeMs) * 100.0 / resultWithoutPool.timeMs;
                System.out.printf("  Mejora: %.1f%% más rápido con pool%n", improvement);
            }
            
        } catch (Exception e) {
            System.err.println("Error probando " + provider.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crea la tabla si no existe
     */
    private static void setupDatabase(DatabaseProvider provider) throws SQLException {
        try (Connection conn = provider.createConnection()) {
            conn.createStatement().execute(provider.getCreateTableSQL());
            System.out.println(" Tabla 'usuarios' verificada/creada en " + provider.getName());
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
                System.out.println(" Insertando " + NUM_QUERIES + " registros de prueba en " + provider.getName() + "...");
                
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
                System.out.println(" Datos insertados en " + provider.getName());
            } else {
                System.out.println(" Datos ya existentes en " + provider.getName());
            }
        }
    }
    
    /**
     * Ejecuta las pruebas con o sin pool
     */
    private static TestResult runTest(DatabaseProvider provider, boolean usePool) 
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
                // Si la tarea lanzó una excepción, contabilizarla como fallo
                QueryRunner.incrementFailedCount();
                System.err.println("Error en la ejecucion de una query: " + e.getCause().getMessage());
            }
        }

        boolean finished = executor.awaitTermination(60, TimeUnit.SECONDS);

        long totalTime = (System.nanoTime() - testStartTime) / 1_000_000;

        if (!finished) {
            System.err.println(" Timeout en " + provider.getName());
            executor.shutdownNow();
        }

        // Mostrar estadísticas: incluir fracción total de queries finalizadas y luego separarlas
        int completed = QueryRunner.getCompletedCount();
        int failed = QueryRunner.getFailedCount();
        int finishedCount = completed + failed;

        System.out.printf("  Queries finalizadas: %d/%d%n", finishedCount, NUM_QUERIES);
        System.out.printf("  Queries completadas: %d/%d%n", completed, NUM_QUERIES);
        System.out.printf("  Queries fallidas: %d/%d%n", failed, NUM_QUERIES);
        System.out.printf("  Tiempo total: %d ms%n", totalTime);
        
        return new TestResult(totalTime, completed, failed);
    }
    
    /**
     * Representa los resultados de una ejecución de prueba
     */
    private static class TestResult {
        private final long timeMs;
        private final int completed;
        private final int failed;

        private TestResult(long timeMs, int completed, int failed) {
            this.timeMs = timeMs;
            this.completed = completed;
            this.failed = failed;
        }
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
                System.out.printf("   PostgreSQL es %.1f%% mas rapido que MySQL con pool%n", faster);
            } else if (mysqlTimeWithPool < pgTimeWithPool) {
                double faster = (pgTimeWithPool - mysqlTimeWithPool) * 100.0 / pgTimeWithPool;
                System.out.printf("   MySQL es %.1f%% mas rapido que PostgreSQL con pool%n", faster);
            } else {
                System.out.println("  Ambas bases de datos tienen rendimiento similar");
            }
        }

        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║               RESUMEN FINAL DE QUERIES POR MODO              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.printf(" PostgreSQL Sin Pool:   %d completadas, %d fallidas%n", pgCompletedWithoutPool, pgFailedWithoutPool);
        System.out.printf(" PostgreSQL Con Pool:   %d completadas, %d fallidas%n", pgCompletedWithPool, pgFailedWithPool);
        System.out.printf(" MySQL      Sin Pool:   %d completadas, %d fallidas%n", mysqlCompletedWithoutPool, mysqlFailedWithoutPool);
        System.out.printf(" MySQL      Con Pool:   %d completadas, %d fallidas%n", mysqlCompletedWithPool, mysqlFailedWithPool);
    }
}