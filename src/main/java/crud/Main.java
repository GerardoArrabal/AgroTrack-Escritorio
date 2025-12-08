package crud;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * PUNTO DE ENTRADA DE LA APLICACIÓN AGROTRACK
 * 
 * Esta es la clase principal que inicia la aplicación JavaFX.
 * Extiende Application, que es la clase base de todas las aplicaciones JavaFX.
 * 
 * Flujo de inicio:
 * 1. Se ejecuta main(String[] args)
 * 2. main() llama a launch(args) que inicia JavaFX
 * 3. JavaFX llama automáticamente a start(Stage) cuando está listo
 * 4. start() carga la ventana de Login y la muestra
 * 
 * IMPORTANTE: Cuando la aplicación se cierra, se cierra el pool de conexiones
 * para liberar recursos correctamente.
 */
public class Main extends Application {

    /**
     * Método llamado por JavaFX cuando la aplicación está lista para iniciar.
     * 
     * Carga la ventana de Login (el primer archivo FXML que ve el usuario)
     * y configura la ventana principal (título, icono, etc.).
     * 
     * @param primaryStage La ventana principal de la aplicación (creada por JavaFX)
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Cargar el archivo FXML de Login
            // FXMLLoader lee el archivo XML y crea los componentes JavaFX
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/vista/Login.fxml"));
            Parent root = loader.load();

            // Crear la escena (contenedor de todos los elementos visuales)
            Scene scene = new Scene(root);
            primaryStage.setTitle("AgroTrack");
            primaryStage.setScene(scene);
            
            // Configurar el icono de la aplicación (aparece en la barra de tareas)
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icono.png")));
            
            // IMPORTANTE: Cerrar el pool de conexiones cuando se cierre la aplicación
            // Esto libera las conexiones a MySQL correctamente
            primaryStage.setOnCloseRequest(event -> {
                ConexionBD.cerrarPool();
            });
            
            // Mostrar la ventana
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Método llamado cuando la aplicación se está cerrando.
     * 
     * Se asegura de cerrar el pool de conexiones incluso si la aplicación
     * se cierra de forma inesperada (no solo con el botón X).
     */
    @Override
    public void stop() {
        // Asegurar que el pool se cierre al cerrar la aplicación
        ConexionBD.cerrarPool();
    }

    /**
     * Punto de entrada principal de la aplicación.
     * 
     * Este método es el primero que se ejecuta cuando inicias la aplicación.
     * launch() es un método estático de Application que:
     * - Inicializa JavaFX
     * - Crea el Stage (ventana principal)
     * - Llama automáticamente a start(Stage)
     * 
     * @param args Argumentos de línea de comandos (no se usan en esta app)
     */
    public static void main(String[] args) {
        launch(args);
    }
}

