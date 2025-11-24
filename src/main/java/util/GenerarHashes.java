package util;

/**
 * Utilidad para generar hashes BCrypt de contraseñas.
 * Ejecuta este main para obtener los hashes que necesitas para el SQL.
 */
public class GenerarHashes {
    
    public static void main(String[] args) {
        System.out.println("=== Hashes BCrypt para insertar en la base de datos ===\n");
        
        // Hash para contraseña "admin"
        String hashAdmin = SeguridadUtil.hashPassword("admin");
        System.out.println("Usuario: admin");
        System.out.println("Contraseña: admin");
        System.out.println("Hash BCrypt: " + hashAdmin);
        System.out.println();
        
        // Hash para contraseña "test123"
        String hashTest = SeguridadUtil.hashPassword("test123");
        System.out.println("Usuario: test");
        System.out.println("Contraseña: test123");
        System.out.println("Hash BCrypt: " + hashTest);
        System.out.println();
        
        System.out.println("=== SQL para insertar usuarios ===");
        System.out.println();
        System.out.println("-- Usuario Administrador");
        System.out.println("INSERT INTO usuario (USU_NOMBRE, USU_APELLIDOS, USU_EMAIL, USU_USERNAME, USU_PASSWORD, USU_ROL, USU_FECHA_REGISTRO, USU_ACTIVO)");
        System.out.println("VALUES (");
        System.out.println("    'Administrador',");
        System.out.println("    'Sistema',");
        System.out.println("    'admin@agrotrack.com',");
        System.out.println("    'admin',");
        System.out.println("    '" + hashAdmin + "',");
        System.out.println("    'admin',");
        System.out.println("    CURDATE(),");
        System.out.println("    TRUE");
        System.out.println(");");
        System.out.println();
        System.out.println("-- Usuario de Prueba");
        System.out.println("INSERT INTO usuario (USU_NOMBRE, USU_APELLIDOS, USU_EMAIL, USU_USERNAME, USU_PASSWORD, USU_ROL, USU_FECHA_REGISTRO, USU_ACTIVO)");
        System.out.println("VALUES (");
        System.out.println("    'Juan',");
        System.out.println("    'Pérez García',");
        System.out.println("    'juan.perez@agrotrack.com',");
        System.out.println("    'test',");
        System.out.println("    '" + hashTest + "',");
        System.out.println("    'usuario',");
        System.out.println("    CURDATE(),");
        System.out.println("    TRUE");
        System.out.println(");");
    }
}

