package controlador;

import crud.FincaDAO;
import crud.UsuarioDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import modelo.Finca;
import modelo.Usuario;
import util.Sesion;

public class ControladorAdmin {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final FincaDAO fincaDAO = new FincaDAO();

    @FXML
    private TextField campoBuscarUsuarios;
    @FXML
    private TableView<Usuario> tablaUsuarios;
    @FXML
    private TableColumn<Usuario, String> colUsuarioNombre;
    @FXML
    private TableColumn<Usuario, String> colUsuarioCorreo;
    @FXML
    private TableColumn<Usuario, String> colUsuarioUsername;
    @FXML
    private TableColumn<Usuario, String> colUsuarioRol;
    @FXML
    private TableColumn<Usuario, String> colUsuarioActivo;

    @FXML
    private TextField campoBuscarFincas;
    @FXML
    private TableView<Finca> tablaFincas;
    @FXML
    private TableColumn<Finca, String> colFincaNombre;
    @FXML
    private TableColumn<Finca, String> colFincaPropietario;
    @FXML
    private TableColumn<Finca, String> colFincaSuperficie;
    @FXML
    private TableColumn<Finca, String> colFincaEstado;
    @FXML
    private TableColumn<Finca, LocalDate> colFincaRegistrada;

    @FXML
    private Label lblTotalUsuarios;
    @FXML
    private Label lblTotalFincas;
    @FXML
    private Label lblFincasActivas;

    private final ObservableList<Usuario> usuarios = FXCollections.observableArrayList();
    private final ObservableList<Finca> fincas = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        try {
            inicializarTablaUsuarios();
            inicializarTablaFincas();
            actualizarResumen();
        } catch (Exception e) {
            // Si hay un error durante la inicialización, mostrar mensaje pero permitir que la ventana se cargue
            System.err.println("Error durante la inicialización: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void inicializarTablaUsuarios() {
        colUsuarioNombre.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getNombre() + " " + data.getValue().getApellidos()));
        colUsuarioCorreo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colUsuarioUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        colUsuarioRol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRol().name()));
        colUsuarioActivo.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().isActivo() ? "Sí" : "No"));

        tablaUsuarios.setItems(usuarios);
        if (campoBuscarUsuarios != null) {
            campoBuscarUsuarios.textProperty().addListener((obs, oldV, newV) -> aplicarFiltroUsuarios());
        }
        refrescarUsuarios();
    }

    private void inicializarTablaFincas() {
        colFincaNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colFincaPropietario.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPropietario() != null
                ? data.getValue().getPropietario().toString()
                : "Sin asignar"));
        colFincaSuperficie.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getSuperficie() == null
                ? "-"
                : data.getValue().getSuperficie() + " ha"));
        colFincaEstado.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEstado().name()));
        colFincaRegistrada.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getFechaRegistro()));

        tablaFincas.setItems(fincas);
        if (campoBuscarFincas != null) {
            campoBuscarFincas.textProperty().addListener((obs, oldV, newV) -> aplicarFiltroFincas());
        }
        refrescarFincas();
    }

    private void refrescarUsuarios() {
        try {
            usuarios.setAll(usuarioDAO.listarTodos());
            aplicarFiltroUsuarios();
            actualizarResumen();
        } catch (SQLException e) {
            String mensaje = esErrorDeConexion(e) 
                ? "No se puede conectar a la base de datos MySQL.\n\n" +
                  "Verifica que:\n" +
                  "• Docker Desktop esté ejecutándose\n" +
                  "• El contenedor MySQL esté corriendo\n" +
                  "• El puerto 3309 esté disponible\n" +
                  "• Las credenciales en bbdd.properties sean correctas\n\n" +
                  "Detalle técnico: " + e.getMessage()
                : "No se pudieron obtener los usuarios.\nDetalle: " + e.getMessage();
            
            mostrarAlerta(Alert.AlertType.ERROR, "Error de conexión", mensaje);
            e.printStackTrace();
        }
    }

    private void refrescarFincas() {
        try {
            fincas.setAll(fincaDAO.listarTodas());
            aplicarFiltroFincas();
            actualizarResumen();
        } catch (SQLException e) {
            String mensaje = esErrorDeConexion(e)
                ? "No se puede conectar a la base de datos MySQL.\n\n" +
                  "Detalle técnico: " + e.getMessage()
                : "No se pudieron obtener las fincas.\nDetalle: " + e.getMessage();
            
            mostrarAlerta(Alert.AlertType.ERROR, "Error de conexión", mensaje);
            e.printStackTrace();
        }
    }

    private boolean esErrorDeConexion(SQLException e) {
        String mensaje = e.getMessage().toLowerCase();
        return mensaje.contains("communications link failure") ||
               mensaje.contains("connection refused") ||
               mensaje.contains("could not create connection") ||
               e.getSQLState() != null && e.getSQLState().startsWith("08");
    }

    private void actualizarResumen() {
        lblTotalUsuarios.setText("Total usuarios: " + usuarios.size());
        lblTotalFincas.setText("Total fincas: " + fincas.size());
        long activas = fincas.stream().filter(f -> f.getEstado() == Finca.Estado.ACTIVA).count();
        lblFincasActivas.setText("Fincas activas: " + activas);
    }

    @FXML
    private void crearUsuario(ActionEvent event) {
        Optional<UsuarioFormResult> resultado = mostrarDialogoUsuario(null);
        resultado.ifPresent(data -> {
            try {
                usuarioDAO.guardar(data.usuario());
                refrescarUsuarios();
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar usuario",
                    "No se pudo crear el usuario.\nDetalle: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void editarUsuario(ActionEvent event) {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona un usuario.");
            return;
        }
        Optional<UsuarioFormResult> resultado = mostrarDialogoUsuario(seleccionado);
        resultado.ifPresent(data -> {
            try {
                usuarioDAO.actualizar(data.usuario(), data.passwordPlano());
                refrescarUsuarios();
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al actualizar",
                    "No se pudo actualizar el usuario.\nDetalle: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void desactivarUsuario(ActionEvent event) {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona un usuario.");
            return;
        }
        try {
            usuarioDAO.desactivar(seleccionado.getId(), false);
            refrescarUsuarios();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Cuenta desactivada",
                "El usuario " + seleccionado.getUsername() + " ha sido desactivado.");
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al desactivar",
                "No fue posible actualizar la cuenta.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void eliminarUsuario(ActionEvent event) {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona un usuario.");
            return;
        }
        try {
            usuarioDAO.eliminar(seleccionado.getId());
            refrescarUsuarios();
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar",
                "No se pudo eliminar el usuario.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void crearFinca(ActionEvent event) {
        if (usuarios.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin usuarios",
                "Debes tener al menos un usuario para asignarlo como propietario.");
            return;
        }
        Optional<Finca> resultado = mostrarDialogoFinca(null);
        resultado.ifPresent(finca -> {
            try {
                fincaDAO.guardar(finca);
                refrescarFincas();
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar finca",
                    "No se pudo crear la finca.\nDetalle: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void editarFinca(ActionEvent event) {
        Finca seleccionada = tablaFincas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona una finca.");
            return;
        }
        abrirDetalleFinca(seleccionada, true);
    }

    @FXML
    private void verDetalleFinca(ActionEvent event) {
        Finca seleccionada = tablaFincas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona una finca.");
            return;
        }
        abrirDetalleFinca(seleccionada, true);
    }

    @FXML
    private void eliminarFinca(ActionEvent event) {
        Finca seleccionada = tablaFincas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona una finca.");
            return;
        }
        try {
            fincaDAO.eliminar(seleccionada.getId());
            refrescarFincas();
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar finca",
                "No se pudo eliminar la finca seleccionada.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    private void abrirInformes(ActionEvent event) {
        navegar("/main/vista/InformesAdmin.fxml", "Informes de Administración");
    }

    @FXML
    private void cerrarSesion(ActionEvent event) {
        Sesion.getInstancia().cerrarSesion();
        navegar("/main/vista/Login.fxml", "Inicio de sesión");
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
        }
        alert.showAndWait();
    }

    private void aplicarFiltroUsuarios() {
        if (campoBuscarUsuarios == null || tablaUsuarios == null) {
            return;
        }
        String filtro = campoBuscarUsuarios.getText();
        if (filtro == null || filtro.isBlank()) {
            tablaUsuarios.setItems(usuarios);
            return;
        }
        String criterio = filtro.toLowerCase();
        ObservableList<Usuario> filtrados = usuarios.filtered(u ->
            u.getNombre().toLowerCase().contains(criterio)
                || u.getApellidos().toLowerCase().contains(criterio)
                || u.getEmail().toLowerCase().contains(criterio)
                || u.getUsername().toLowerCase().contains(criterio));
        tablaUsuarios.setItems(filtrados);
    }

    private void aplicarFiltroFincas() {
        if (campoBuscarFincas == null || tablaFincas == null) {
            return;
        }
        String filtro = campoBuscarFincas.getText();
        if (filtro == null || filtro.isBlank()) {
            tablaFincas.setItems(fincas);
            return;
        }
        String criterio = filtro.toLowerCase();
        ObservableList<Finca> filtradas = fincas.filtered(f ->
            f.getNombre().toLowerCase().contains(criterio)
                || (f.getPropietario() != null && f.getPropietario().toString().toLowerCase().contains(criterio))
                || f.getEstado().name().toLowerCase().contains(criterio));
        tablaFincas.setItems(filtradas);
    }

    private void abrirDetalleFinca(Finca finca, boolean volverAAdmin) {
        try {
            Sesion.getInstancia().setFincaSeleccionada(finca);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/vista/DetalleFinca.fxml"));
            Parent root = loader.load();
            ControladorDetalleFinca controller = loader.getController();
            controller.setFinca(finca);
            controller.setVolverAAdmin(volverAAdmin);
            Stage stage = (Stage) tablaFincas.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Detalle de finca");
            stage.centerOnScreen();
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al abrir detalle",
                "No se pudo cargar la vista DetalleFinca.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navegar(String ruta, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent root = loader.load();
            Stage stage = (Stage) tablaUsuarios.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(titulo);
            stage.centerOnScreen();
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de navegación",
                "No se pudo cargar la vista solicitada.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Optional<UsuarioFormResult> mostrarDialogoUsuario(Usuario usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/vista/UsuarioForm.fxml"));
            Parent root = loader.load();
            DialogoUsuarioController controller = loader.getController();
            Stage dialogStage = crearDialogoStage(usuario == null ? "Nuevo usuario" : "Editar usuario", root);
            controller.setStage(dialogStage);
            controller.setUsuario(usuario);
            dialogStage.showAndWait();
            Usuario resultado = controller.getUsuarioResultado();
            if (resultado != null) {
                return Optional.of(new UsuarioFormResult(resultado, controller.getPasswordPlano()));
            }
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al abrir formulario",
                "No se pudo abrir la ventana de usuario.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private Optional<Finca> mostrarDialogoFinca(Finca finca) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/vista/FincaForm.fxml"));
            Parent root = loader.load();
            DialogoFincaController controller = loader.getController();
            Stage dialogStage = crearDialogoStage(finca == null ? "Nueva finca" : "Editar finca", root);
            controller.setStage(dialogStage);
            controller.setUsuarios(usuarios);
            controller.setFinca(finca);
            dialogStage.showAndWait();
            return Optional.ofNullable(controller.getFincaResultado());
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al abrir formulario",
                "No se pudo abrir la ventana de finca.\nDetalle: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Stage crearDialogoStage(String titulo, Parent root) {
        Stage stage = new Stage();
        stage.setTitle(titulo);
        stage.initOwner(tablaUsuarios.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        
        try {
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icono.png")));
        } catch (Exception e) {
        }
        return stage;
    }

    private record UsuarioFormResult(Usuario usuario, String passwordPlano) {
    }
}

