package controlador;

import crud.UsuarioDAO;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import modelo.Usuario;
import util.SeguridadUtil;
import util.Sesion;
import util.ValidacionUtil;

public class DialogoPerfilUsuarioController {

    @FXML
    private TextField campoNombre;
    @FXML
    private TextField campoApellidos;
    @FXML
    private TextField campoEmail;
    @FXML
    private TextField campoUsername;
    @FXML
    private PasswordField campoPasswordActual;
    @FXML
    private PasswordField campoPasswordNueva;
    @FXML
    private PasswordField campoPasswordConfirmar;
    @FXML
    private Label lblMensaje;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private Usuario usuario;
    private Stage stage;

    @FXML
    private void initialize() {
        if (lblMensaje != null) {
            lblMensaje.setVisible(false);
        }
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
        if (usuario == null) {
            return;
        }
        if (campoNombre != null) {
            campoNombre.setText(valueOrEmpty(usuario.getNombre()));
        }
        if (campoApellidos != null) {
            campoApellidos.setText(valueOrEmpty(usuario.getApellidos()));
        }
        if (campoEmail != null) {
            campoEmail.setText(valueOrEmpty(usuario.getEmail()));
        }
        if (campoUsername != null) {
            campoUsername.setText(valueOrEmpty(usuario.getUsername()));
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void guardarCambios() {
        if (usuario == null) {
            mostrarMensaje("No se ha podido cargar el usuario.", true);
            return;
        }

        boolean nombreValido = ValidacionUtil.textoRequerido(campoNombre);
        boolean apellidosValido = ValidacionUtil.textoRequerido(campoApellidos);
        boolean emailValido = ValidacionUtil.textoRequerido(campoEmail);
        boolean usernameValido = ValidacionUtil.textoRequerido(campoUsername);

        if (!nombreValido || !apellidosValido || !emailValido || !usernameValido) {
            mostrarMensaje("Completa los campos obligatorios.", true);
            return;
        }

        String nuevoNombre = campoNombre.getText().trim();
        String nuevosApellidos = campoApellidos.getText().trim();
        String nuevoEmail = campoEmail.getText().trim();
        String nuevoUsername = campoUsername.getText().trim();

        String passwordActual = campoPasswordActual.getText() != null ? campoPasswordActual.getText() : "";
        String passwordNueva = campoPasswordNueva.getText() != null ? campoPasswordNueva.getText() : "";
        String passwordConfirmar = campoPasswordConfirmar.getText() != null ? campoPasswordConfirmar.getText() : "";

        boolean actualizarPassword = !passwordActual.isBlank() || !passwordNueva.isBlank() || !passwordConfirmar.isBlank();

        String nuevoPasswordPlano = null;
        if (actualizarPassword) {
            if (passwordActual.isBlank() || passwordNueva.isBlank() || passwordConfirmar.isBlank()) {
                mostrarMensaje("Para cambiar la contraseña completa los tres campos.", true);
                return;
            }
            if (!SeguridadUtil.verificarPassword(passwordActual, usuario.getPassword())) {
                mostrarMensaje("La contraseña actual no coincide.", true);
                return;
            }
            if (!passwordNueva.equals(passwordConfirmar)) {
                mostrarMensaje("La nueva contraseña no coincide en ambos campos.", true);
                return;
            }
            if (passwordNueva.length() < 6) {
                mostrarMensaje("La nueva contraseña debe tener al menos 6 caracteres.", true);
                return;
            }
            nuevoPasswordPlano = passwordNueva;
        }

        try {
            usuario.setNombre(nuevoNombre);
            usuario.setApellidos(nuevosApellidos);
            usuario.setEmail(nuevoEmail);
            usuario.setUsername(nuevoUsername);
            usuarioDAO.actualizar(usuario, nuevoPasswordPlano);
            Sesion.getInstancia().setUsuarioActual(usuario);
            mostrarMensaje("Perfil actualizado correctamente.", false);
            cerrar();
        } catch (SQLException e) {
            mostrarMensaje("No se pudieron guardar los cambios: " + e.getMessage(), true);
        }
    }

    @FXML
    private void cancelar() {
        cerrar();
    }

    private void cerrar() {
        if (stage != null) {
            stage.close();
        }
    }

    private void mostrarMensaje(String texto, boolean error) {
        if (lblMensaje != null) {
            lblMensaje.setText(texto);
            lblMensaje.setStyle(error ? "-fx-text-fill: #c62828;" : "-fx-text-fill: #2e7d32;");
            lblMensaje.setVisible(true);
        }
    }

    private String valueOrEmpty(String valor) {
        return valor == null ? "" : valor;
    }
}

