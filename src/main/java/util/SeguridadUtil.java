package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilidad para el manejo seguro de contraseñas usando el algoritmo BCrypt.
 * 
 * IMPORTANTE: BCrypt es un algoritmo de hash unidireccional diseñado específicamente
 * para contraseñas. A diferencia de algoritmos como MD5 o SHA, BCrypt:
 * - Es lento intencionalmente (protege contra ataques de fuerza bruta)
 * - Incluye un "salt" (sal) aleatorio en cada hash (mismo password = diferentes hashes)
 * - El "COSTO" determina cuántas iteraciones se hacen (mayor = más seguro pero más lento)
 * 
 * ¿Por qué no guardamos las contraseñas en texto plano?
 * Si alguien accede a la base de datos, no podrá ver las contraseñas reales,
 * solo los hashes. Y como BCrypt es unidireccional, no se puede "descifrar".
 */
public final class SeguridadUtil {

    /**
     * Factor de costo para BCrypt. Determina cuántas iteraciones del algoritmo se realizan.
     * - Valor 12 = ~2^12 = 4096 iteraciones (buen equilibrio entre seguridad y velocidad)
     * - Mayor valor = más seguro pero más lento al generar/verificar
     * - Recomendado: entre 10 y 14 para aplicaciones normales
     */
    private static final int COSTO = 12;

    private SeguridadUtil() {
        // Clase de utilidad, no se puede instanciar
    }

    /**
     * Convierte una contraseña en texto plano a un hash BCrypt seguro.
     * 
     * Ejemplo: "admin" -> "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyY..."
     * 
     * Cada vez que llamas a esta función con la misma contraseña, obtienes
     * un hash DIFERENTE (por el salt aleatorio), pero ambos son válidos para verificar.
     * 
     * @param passwordPlano La contraseña en texto plano que el usuario ingresó
     * @return Un hash BCrypt que se puede guardar de forma segura en la base de datos
     */
    public static String hashPassword(String passwordPlano) {
        if (passwordPlano == null) {
            throw new IllegalArgumentException("La contraseña no puede ser nula");
        }
        // BCrypt.gensalt(COSTO) genera un salt aleatorio y lo incluye en el hash
        // El hash resultante contiene: algoritmo + costo + salt + hash real
        return BCrypt.hashpw(passwordPlano, BCrypt.gensalt(COSTO));
    }

    /**
     * Verifica si una contraseña en texto plano coincide con un hash BCrypt.
     * 
     * Cómo funciona:
     * 1. Toma la contraseña en texto plano que el usuario ingresó
     * 2. Toma el hash guardado en la base de datos (que incluye el salt)
     * 3. BCrypt extrae el salt del hash, aplica el algoritmo a la contraseña
     *    con ese mismo salt, y compara el resultado
     * 
     * @param passwordPlano La contraseña que el usuario acaba de ingresar
     * @param passwordHash El hash guardado en la base de datos
     * @return true si la contraseña es correcta, false en caso contrario
     */
    public static boolean verificarPassword(String passwordPlano, String passwordHash) {
        if (passwordPlano == null || passwordHash == null) {
            return false;
        }
        // BCrypt.checkpw hace toda la magia: extrae el salt del hash,
        // aplica el algoritmo y compara
        return BCrypt.checkpw(passwordPlano, passwordHash);
    }
}

