package crud;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * GESTIÓN DE CONEXIONES A LA BASE DE DATOS
 * 
 * Esta clase maneja todas las conexiones a MySQL usando HikariCP, que es un
 * "pool de conexiones" (connection pool).
 * 
 * ¿Qué es un pool de conexiones y por qué lo usamos?
 * - Abrir/cerrar conexiones a la base de datos es LENTO y costoso
 * - En lugar de crear una nueva conexión cada vez, reutilizamos conexiones existentes
 * - El pool mantiene varias conexiones "listas" y las presta cuando las necesitas
 * - Cuando terminas, la conexión vuelve al pool (no se cierra realmente)
 * 
 * Ventajas:
 * - Mucho más rápido (no hay que esperar a crear conexiones)
 * - Controla cuántas conexiones simultáneas hay (evita sobrecargar la BD)
 * - Detecta conexiones "perdidas" (que no se cerraron correctamente)
 * 
 * Configuración:
 * - Mínimo 2 conexiones siempre activas
 * - Máximo 10 conexiones simultáneas
 * - Las conexiones se cierran automáticamente después de 30 minutos
 */
public final class ConexionBD {

    private static HikariDataSource dataSource;  // El pool de conexiones
    private static final Object lock = new Object();  // Para sincronización (thread-safe)

    private ConexionBD() {
        // Clase de utilidad, no se puede instanciar
    }

    /**
     * Inicializa el pool de conexiones leyendo la configuración de bbdd.properties.
     * 
     * Usa "double-checked locking" para asegurar que solo se inicialice una vez,
     * incluso si múltiples hilos intentan inicializarlo al mismo tiempo.
     * 
     * @throws SQLException Si no se puede leer la configuración o crear el pool
     */
    private static void inicializarDataSource() throws SQLException {
        if (dataSource != null) {
            return;
        }

        synchronized (lock) {
            if (dataSource != null) {
                return;
            }

            Properties properties = new Properties();
            
            try (InputStream input = ConexionBD.class.getClassLoader().getResourceAsStream("bbdd.properties")) {
                if (input == null) {
                    throw new SQLException("No se encontró el archivo bbdd.properties");
                }
                
                properties.load(input);
                
                String IP = properties.getProperty("IP", "127.0.0.1");
                String PORT = properties.getProperty("PORT", "3309");
                String BBDD = properties.getProperty("BBDD");
                String USER = properties.getProperty("USER");
                String PWD = properties.getProperty("PWD");
                
                if (BBDD == null || USER == null || PWD == null) {
                    throw new SQLException("Faltan propiedades requeridas en bbdd.properties (BBDD, USER, PWD)");
                }
                
                HikariConfig config = new HikariConfig();
                
                /**
                 * URL DE CONEXIÓN CON OPTIMIZACIONES
                 * 
                 * Los parámetros en la URL mejoran el rendimiento:
                 * - useServerPrepStmts: Reutiliza consultas preparadas (más rápido)
                 * - cachePrepStmts: Guarda consultas preparadas en caché
                 * - rewriteBatchedStatements: Optimiza inserciones múltiples
                 * - useUnicode + characterEncoding: Soporte para caracteres especiales (ñ, acentos, etc.)
                 * - serverTimezone=UTC: Evita problemas con zonas horarias
                 */
                String jdbcUrl = "jdbc:mysql://" + IP + ":" + PORT + "/" + BBDD
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&useServerPrepStmts=true"
                    + "&cachePrepStmts=true"
                    + "&prepStmtCacheSize=250"
                    + "&prepStmtCacheSqlLimit=2048"
                    + "&rewriteBatchedStatements=true"
                    + "&cacheResultSetMetadata=true"
                    + "&cacheServerConfiguration=true"
                    + "&elideSetAutoCommits=true"
                    + "&maintainTimeStats=false"
                    + "&useLocalSessionState=true"
                    + "&useUnicode=true"
                    + "&characterEncoding=UTF-8"
                    + "&serverTimezone=UTC";
                
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(USER);
                config.setPassword(PWD);
                
                /**
                 * CONFIGURACIÓN DEL POOL DE CONEXIONES
                 * 
                 * - MinimumIdle: Conexiones que siempre están listas (aunque no se usen)
                 * - MaximumPoolSize: Límite de conexiones simultáneas (evita sobrecargar MySQL)
                 * - ConnectionTimeout: Tiempo máximo para obtener una conexión del pool
                 * - IdleTimeout: Si una conexión no se usa en 10 min, se cierra
                 * - MaxLifetime: Las conexiones se renuevan cada 30 min (MySQL tiene límite de 8 horas)
                 * - LeakDetectionThreshold: Detecta si olvidaste cerrar una conexión (útil para debug)
                 */
                config.setMinimumIdle(2);           // Mínimo de conexiones inactivas
                config.setMaximumPoolSize(10);     // Máximo de conexiones en el pool
                config.setConnectionTimeout(30000); // 30 segundos timeout para obtener conexión
                config.setIdleTimeout(600000);      // 10 minutos antes de cerrar conexiones inactivas
                config.setMaxLifetime(1800000);     // 30 minutos máximo de vida de una conexión
                config.setLeakDetectionThreshold(60000); // Detectar conexiones que no se cierran (60 segundos)
                
                // Optimizaciones adicionales
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                
                dataSource = new HikariDataSource(config);
                
            } catch (IOException e) {
                throw new SQLException("Error al leer el archivo bbdd.properties: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Obtiene una conexión del pool.
     * 
     * IMPORTANTE: Siempre cierra la conexión cuando termines:
     *   try (Connection conn = ConexionBD.obtenerConexion()) {
     *       // usar la conexión
     *   } // Se cierra automáticamente aquí
     * 
     * Si no cierras la conexión, el pool se quedará sin conexiones disponibles.
     * 
     * @return Una conexión a la base de datos MySQL
     * @throws SQLException Si no se puede obtener una conexión (pool lleno, BD caída, etc.)
     */
    public static Connection obtenerConexion() throws SQLException {
        if (dataSource == null) {
            inicializarDataSource();
        }
        // HikariCP maneja todo: busca una conexión disponible, espera si es necesario, etc.
        return dataSource.getConnection();
    }

    public static void cerrarPool() {
        synchronized (lock) {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                dataSource = null;
            }
        }
    }
}

