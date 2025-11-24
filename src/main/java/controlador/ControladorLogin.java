package controlador;

import crud.UsuarioDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import util.ValidacionUtil;
import javafx.stage.Stage;
import modelo.Usuario;
import util.Sesion;

public class ControladorLogin {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private TextField campoUsuario;
    @FXML
    private PasswordField campoContrasena;
    @FXML
    private Hyperlink linkRecuperar;
    @FXML
    private Button btnIniciarSesion;
    @FXML
    private Button btnRegistrarse;

    @FXML
    private void iniciarSesion(ActionEvent event) {
        if (campoUsuario == null || campoContrasena == null) {
            return;
        }

        String usuario = campoUsuario.getText().trim();
        String contrasena = campoContrasena.getText();

        boolean usuarioValido = ValidacionUtil.textoRequerido(campoUsuario);
        boolean contrasenaValida = ValidacionUtil.textoRequerido(campoContrasena);
        if (!usuarioValido || !contrasenaValida) {
            mostrarAlerta(Alert.AlertType.WARNING, "Datos incompletos", "Introduzca usuario y contraseña para continuar.");
            return;
        }

        if (esAdministrador(usuario, contrasena)) {
            // Crear usuario admin temporal para la sesiÃ³n
            Usuario adminTemp = new Usuario();
            adminTemp.setId(0);
            adminTemp.setUsername("admin");
            adminTemp.setNombre("Administrador");
            adminTemp.setApellidos("Sistema");
            adminTemp.setEmail("admin@agrotrack.com");
            adminTemp.setRol(Usuario.Rol.ADMIN);
            adminTemp.setFechaRegistro(LocalDate.now());
            adminTemp.setActivo(true);
            Sesion.getInstancia().setUsuarioActual(adminTemp);
            navegar("/main/vista/AdminPanel.fxml", "Panel de administración");
            return;
        }

        try {
            Optional<Usuario> usuarioOpt = usuarioDAO.autenticar(usuario, contrasena);
            if (usuarioOpt.isEmpty()) {
                mostrarAlerta(Alert.AlertType.ERROR, "Credenciales inválidas",
                    "Usuario o contraseña incorrectos, o la cuenta está inactiva.");
                return;
            }
            Usuario autenticado = usuarioOpt.get();
            Sesion.getInstancia().setUsuarioActual(autenticado);
            if (autenticado.getRol() == Usuario.Rol.ADMIN) {
                navegar("/main/vista/AdminPanel.fxml", "Panel de administración");
            } else {
                navegar("/main/vista/Dashboard.fxml", "AgroTrack - Dashboard");
            }
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de conexión",
                "No fue posible validar las credenciales. Intenta nuevamente.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean esAdministrador(String usuario, String contrasena) {
        return "admin".equalsIgnoreCase(usuario) && "admin".equals(contrasena);
    }

    @FXML
    private void irARegistro(ActionEvent event) {
        navegar("/main/vista/registro.fxml", "Registro de usuario");
    }

    @FXML
    private void recuperarCuenta(ActionEvent event) {
        mostrarAlerta(Alert.AlertType.INFORMATION, "Recuperación no implementada",
            "Contacta con un administrador para restablecer tus credenciales.");
    }

    private void navegar(String ruta, String tituloVentana) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent root = loader.load();
            Stage stage = (Stage) btnIniciarSesion.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(tituloVentana);
            stage.centerOnScreen();
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de navegación",
                "No se pudo cargar la vista solicitada.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        // Configurar icono
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icono.png")));
        } catch (Exception e) {
            // Si no se puede cargar el icono, continuar sin él
        }
        alert.showAndWait();
    }
}


