package util;

/**
 * Utilidad para generar hashes BCrypt de contraseñas.
 * 
 * ¿Para qué sirve este archivo?
 * Cuando necesitas crear usuarios directamente en la base de datos (por ejemplo,
 * el usuario administrador inicial), no puedes guardar la contraseña en texto plano.
 * Este programa genera el hash BCrypt de la contraseña para que puedas copiarlo
 * y pegarlo en tu script SQL.
 * 
 * Cómo usarlo:
 * 1. Ejecuta este main (Run -> Run File)
 * 2. Copia el hash que aparece en la consola
 * 3. Pégalo en tu script SQL INSERT
 * 
 * IMPORTANTE: Cada vez que ejecutes esto, obtendrás un hash DIFERENTE
 * (por el salt aleatorio de BCrypt), pero todos son válidos para la misma contraseña.
 */
public class GenerarHashes {
    
    public static void main(String[] args) {
        System.out.println("=== Hashes BCrypt para insertar en la base de datos ===\n");
        
        // Genera el hash BCrypt de la contraseña "admin"
        // Este hash es lo que se guarda en la base de datos, NO la contraseña en texto plano
        String hashAdmin = SeguridadUtil.hashPassword("admin");
        System.out.println("Usuario: admin");
        System.out.println("Contraseña: admin");
        System.out.println("Hash BCrypt: " + hashAdmin);
        System.out.println();
        
        // Genera el SQL completo listo para copiar y pegar
        System.out.println("=== SQL para insertar usuario administrador ===");
        System.out.println();
        System.out.println("INSERT INTO usuario (USU_NOMBRE, USU_APELLIDOS, USU_EMAIL, USU_USERNAME, USU_PASSWORD, USU_ROL, USU_FECHA_REGISTRO, USU_ACTIVO)");
        System.out.println("VALUES (");
        System.out.println("    'Administrador',");
        System.out.println("    'Sistema',");
        System.out.println("    'admin@agrotrack.com',");
        System.out.println("    'admin',");
        System.out.println("    '" + hashAdmin + "',");  // Aquí va el hash, NO la contraseña
        System.out.println("    'admin',");
        System.out.println("    CURDATE(),");
        System.out.println("    TRUE");
        System.out.println(");");
    }
}

