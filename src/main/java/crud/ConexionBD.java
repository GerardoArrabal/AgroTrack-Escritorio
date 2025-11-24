package crud;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class ConexionBD {

    private static HikariDataSource dataSource;
    private static final Object lock = new Object();

    private ConexionBD() {
    }

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
                
                // URL de conexión con parámetros optimizados para rendimiento
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
                
                // Configuración del pool de conexiones
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

    public static Connection obtenerConexion() throws SQLException {
        if (dataSource == null) {
            inicializarDataSource();
        }
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

