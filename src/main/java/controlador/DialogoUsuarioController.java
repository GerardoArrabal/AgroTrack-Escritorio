package controlador;

import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import modelo.Usuario;
import util.ValidacionUtil;

public class DialogoUsuarioController {

    @FXML
    private Label lblTitulo;
    @FXML
    private TextField campoNombre;
    @FXML
    private TextField campoApellidos;
    @FXML
    private TextField campoCorreo;
    @FXML
    private TextField campoUsername;
    @FXML
    private PasswordField campoPassword;
    @FXML
    private ComboBox<Usuario.Rol> comboRol;
    @FXML
    private DatePicker pickerFechaRegistro;
    @FXML
    private CheckBox checkActivo;
    @FXML
    private Label lblError;

    private Stage stage;
    private Usuario usuarioResultado;
    private String passwordPlano;

    @FXML
    private void initialize() {
        comboRol.setItems(FXCollections.observableArrayList(Usuario.Rol.values()));
        comboRol.getSelectionModel().select(Usuario.Rol.USUARIO);
        pickerFechaRegistro.setValue(LocalDate.now());
        checkActivo.setSelected(true);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setUsuario(Usuario usuario) {
        if (usuario == null) {
            lblTitulo.setText("Nuevo usuario");
            return;
        }
        lblTitulo.setText("Editar usuario");
        usuarioResultado = new Usuario();
        usuarioResultado.setId(usuario.getId());
        usuarioResultado.setPassword(usuario.getPassword());

        campoNombre.setText(usuario.getNombre());
        campoApellidos.setText(usuario.getApellidos());
        campoCorreo.setText(usuario.getEmail());
        campoUsername.setText(usuario.getUsername());
        comboRol.getSelectionModel().select(usuario.getRol());
        pickerFechaRegistro.setValue(usuario.getFechaRegistro());
        checkActivo.setSelected(usuario.isActivo());
    }

    @FXML
    private void guardar() {
        if (!validar()) {
            return;
        }
        if (usuarioResultado == null) {
            usuarioResultado = new Usuario();
        }
        usuarioResultado.setNombre(campoNombre.getText().trim());
        usuarioResultado.setApellidos(campoApellidos.getText().trim());
        usuarioResultado.setEmail(campoCorreo.getText().trim());
        usuarioResultado.setUsername(campoUsername.getText().trim());
        // Asegurar que el rol no sea null, usar USUARIO por defecto
        Usuario.Rol rol = comboRol.getValue() != null ? comboRol.getValue() : Usuario.Rol.USUARIO;
        usuarioResultado.setRol(rol);
        usuarioResultado.setFechaRegistro(
            pickerFechaRegistro.getValue() != null ? pickerFechaRegistro.getValue() : LocalDate.now());
        usuarioResultado.setActivo(checkActivo.isSelected());

        passwordPlano = campoPassword.getText();
        if (passwordPlano != null && !passwordPlano.isBlank()) {
            usuarioResultado.setPassword(passwordPlano);
        }
        cerrar();
    }

    private boolean validar() {
        ocultarError();
        boolean nombreValido = ValidacionUtil.textoRequerido(campoNombre);
        boolean apellidosValido = ValidacionUtil.textoRequerido(campoApellidos);
        boolean correoValido = ValidacionUtil.textoRequerido(campoCorreo);
        boolean usernameValido = ValidacionUtil.textoRequerido(campoUsername);
        boolean rolValido = ValidacionUtil.comboRequerido(comboRol);
        boolean fechaValida = ValidacionUtil.fechaRequerida(pickerFechaRegistro);
        boolean passwordValido = usuarioResultado != null
            ? true
            : ValidacionUtil.textoRequerido(campoPassword);
        if (usuarioResultado != null && (campoPassword == null || campoPassword.getText().isBlank())) {
            ValidacionUtil.marcar(campoPassword, true);
        }

        if (!(nombreValido && apellidosValido && correoValido && usernameValido && rolValido && fechaValida
            && passwordValido)) {
            mostrarError(usuarioResultado == null
                ? "Completa todos los campos obligatorios para crear el usuario."
                : "Revisa los campos marcados en rojo.");
            return false;
        }
        if (usuarioResultado != null && !campoPassword.getText().isBlank()) {
            passwordValido = ValidacionUtil.textoRequerido(campoPassword);
            if (!passwordValido) {
                mostrarError("La nueva contraseña no puede estar vacía.");
                return false;
            }
        }
        return true;
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
    }

    private void ocultarError() {
        lblError.setVisible(false);
        lblError.setText("");
    }

    @FXML
    private void cancelar() {
        usuarioResultado = null;
        passwordPlano = null;
        cerrar();
    }

    private void cerrar() {
        if (stage != null) {
            stage.close();
        }
    }

    public Usuario getUsuarioResultado() {
        return usuarioResultado;
    }

    public String getPasswordPlano() {
        return passwordPlano;
    }
}

