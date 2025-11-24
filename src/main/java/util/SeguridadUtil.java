package util;

import org.mindrot.jbcrypt.BCrypt;

public final class SeguridadUtil {

    private static final int COSTO = 12;

    private SeguridadUtil() {
    }

    public static String hashPassword(String passwordPlano) {
        if (passwordPlano == null) {
            throw new IllegalArgumentException("La contraseña no puede ser nula");
        }
        return BCrypt.hashpw(passwordPlano, BCrypt.gensalt(COSTO));
    }

    public static boolean verificarPassword(String passwordPlano, String passwordHash) {
        if (passwordPlano == null || passwordHash == null) {
            return false;
        }
        return BCrypt.checkpw(passwordPlano, passwordHash);
    }
}

