package controlador;

import crud.UsuarioDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import modelo.Usuario;
import util.ValidacionUtil;

public class ControladorRegistro {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private TextField campoNombre;
    @FXML
    private TextField campoApellidos;
    @FXML
    private TextField campoCorreo;
    @FXML
    private TextField campoUsername;
    @FXML
    private PasswordField campoContrasena;
    @FXML
    private PasswordField campoContrasenaConfirmacion;
    @FXML
    private CheckBox checkTerminos;
    @FXML
    private Button btnRegistrar;
    @FXML
    private Button btnVolverLogin;

    @FXML
    private void registrarUsuario(ActionEvent event) {
        if (!validarFormulario()) {
            return;
        }

        try {
            if (usuarioDAO.existeUsername(campoUsername.getText().trim())) {
                mostrarAlerta(Alert.AlertType.WARNING, "Nombre de usuario en uso",
                    "El nombre de usuario ya está registrado. Elige otro.");
                return;
            }
            if (usuarioDAO.existeEmail(campoCorreo.getText().trim())) {
                mostrarAlerta(Alert.AlertType.WARNING, "Correo en uso",
                    "Ya existe una cuenta asociada a ese correo.");
                return;
            }

            Usuario nuevo = new Usuario();
            nuevo.setNombre(campoNombre.getText().trim());
            nuevo.setApellidos(campoApellidos.getText().trim());
            nuevo.setEmail(campoCorreo.getText().trim());
            nuevo.setUsername(campoUsername.getText().trim());
            nuevo.setPassword(campoContrasena.getText());
            nuevo.setRol(Usuario.Rol.USUARIO);
            nuevo.setFechaRegistro(LocalDate.now());
            nuevo.setActivo(true);
            usuarioDAO.guardar(nuevo);

            mostrarAlerta(Alert.AlertType.INFORMATION, "Registro exitoso",
                "El usuario se registró correctamente. Ya puedes iniciar sesión.");
            navegar("/main/vista/Login.fxml", "Inicio de sesión");
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al registrar",
                "No se pudo completar el registro. Intenta nuevamente.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validarFormulario() {
        boolean nombreValido = ValidacionUtil.textoRequerido(campoNombre);
        boolean apellidosValido = ValidacionUtil.textoRequerido(campoApellidos);
        boolean correoValido = ValidacionUtil.textoRequerido(campoCorreo);
        boolean usernameValido = ValidacionUtil.textoRequerido(campoUsername);
        boolean passValido = ValidacionUtil.textoRequerido(campoContrasena);
        boolean passConfirmValido = ValidacionUtil.textoRequerido(campoContrasenaConfirmacion);
        boolean terminosValido = checkTerminos != null && checkTerminos.isSelected();
        ValidacionUtil.checkRequerido(checkTerminos);

        if (!(nombreValido && apellidosValido && correoValido && usernameValido && passValido
            && passConfirmValido && terminosValido)) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos incompletos", "Completa toda la información requerida.");
            return false;
        }

        if (!campoContrasena.getText().equals(campoContrasenaConfirmacion.getText())) {
            ValidacionUtil.marcar(campoContrasena, false);
            ValidacionUtil.marcar(campoContrasenaConfirmacion, false);
            mostrarAlerta(Alert.AlertType.WARNING, "Contraseñas distintas", "Las contraseñas no coinciden.");
            return false;
        }
        ValidacionUtil.marcar(campoContrasena, true);
        ValidacionUtil.marcar(campoContrasenaConfirmacion, true);

        return true;
    }

    @FXML
    private void volverAlLogin(ActionEvent event) {
        navegar("/main/vista/Login.fxml", "Inicio de sesión");
    }

    private void navegar(String ruta, String tituloVentana) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent root = loader.load();
            Stage stage = (Stage) btnRegistrar.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(tituloVentana);
            stage.centerOnScreen();
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de navegación",
                "No se pudo abrir la ventana solicitada.\nDetalle: " + e.getMessage());
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

