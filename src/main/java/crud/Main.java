package crud;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/vista/Login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setTitle("AgroTrack");
            primaryStage.setScene(scene);
            // Configurar icono antes de mostrar
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icono.png")));
            
            // Cerrar el pool de conexiones cuando se cierre la aplicación
            primaryStage.setOnCloseRequest(event -> {
                ConexionBD.cerrarPool();
            });
            
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Asegurar que el pool se cierre al cerrar la aplicación
        ConexionBD.cerrarPool();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

