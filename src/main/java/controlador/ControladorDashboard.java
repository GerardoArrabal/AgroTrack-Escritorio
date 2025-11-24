package controlador;

import crud.CultivoDAO;
import crud.FincaDAO;
import crud.GestionFinancieraDAO;
import crud.TratamientoDAO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import modelo.Cultivo;
import modelo.Finca;
import modelo.GestionFinanciera;
import modelo.GestionFinanciera.Tipo;
import modelo.Usuario;
import util.Sesion;

@SuppressWarnings("unchecked")
public class ControladorDashboard {

    private final FincaDAO fincaDAO = new FincaDAO();
    private final GestionFinancieraDAO gestionDAO = new GestionFinancieraDAO();
    private final TratamientoDAO tratamientoDAO = new TratamientoDAO();
    private final CultivoDAO cultivoDAO = new CultivoDAO();

    @FXML
    private Label lblFincasActivas;
    @FXML
    private Label lblSuperficieTotal;
    @FXML
    private Label lblBalanceMensual;

    @FXML
    private TextField campoBuscar;
    @FXML
    private Button btnNuevaFinca;
    @FXML
    private Button btnPerfilUsuario;
    @FXML
    private Button btnCerrarSesion;
    @FXML
    private ToggleButton btnNavFincas;
    @FXML
    private ToggleButton btnNavCultivos;
    @FXML
    private ToggleButton btnNavTratamientos;
    @FXML
    private ToggleButton btnNavFinanzas;
    @FXML
    private ToggleButton btnNavInformes;

    @FXML
    private TableView<GestionFinanciera> tablaGestiones;
    @FXML
    private TableColumn<GestionFinanciera, LocalDate> colGestionFecha;
    @FXML
    private TableColumn<GestionFinanciera, String> colGestionTipo;
    @FXML
    private TableColumn<GestionFinanciera, String> colGestionConcepto;
    @FXML
    private TableColumn<GestionFinanciera, String> colGestionMonto;

    @FXML
    private ListView<Finca> listaFincas;
    @FXML
    private AreaChart<String, Number> chartProduccion;
    @FXML
    private StackPane panelMapa;
    @FXML
    private ProgressIndicator loadingMapa;

    private final ObservableList<GestionFinanciera> gestiones = FXCollections.observableArrayList();
    private final ObservableList<Finca> fincasUsuario = FXCollections.observableArrayList();
    private WebView webViewMapa;
    private WebEngine mapaEngine;
    private ToggleGroup menuToggleGroup;

    @FXML
    private void initialize() {
        inicializarTablaGestiones();
        poblarIndicadores();
        inicializarListaFincas();
        poblarGraficaProduccion();
        inicializarMenuLateral();
        inicializarMapaDashboard();
        configurarBotonPerfil();
    }

    private void inicializarTablaGestiones() {
        if (tablaGestiones == null) {
            return;
        }
        colGestionFecha.setCellValueFactory(data -> new SimpleObjectProperty<LocalDate>(data.getValue().getFecha()));
        colGestionTipo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipo().name()));
        colGestionConcepto.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getConcepto()));
        colGestionMonto.setCellValueFactory(data ->
            new SimpleStringProperty(String.format("%.2f €", data.getValue().getMonto())));

        tablaGestiones.setItems(gestiones);
        cargarGestionesRecientes();
    }

    private void cargarGestionesRecientes() {
        try {
            Sesion sesion = Sesion.getInstancia();
            List<GestionFinanciera> todasGestiones;
            
            if (sesion.esAdministrador()) {
                todasGestiones = gestionDAO.listarRecientes(10);
            } else if (sesion.estaAutenticado() && sesion.getUsuarioActual().getId() != null) {
                // Obtener fincas del usuario y luego sus gestiones usando consulta optimizada
                List<Finca> fincasUsuario = fincaDAO.listarPorUsuario(sesion.getUsuarioActual().getId());
                List<Integer> idsFincas = fincasUsuario.stream()
                    .map(Finca::getId)
                    .filter(id -> id != null)
                    .toList();
                
                if (idsFincas.isEmpty()) {
                    gestiones.clear();
                    actualizarBalanceMensual();
                    return;
                }
                
                // Usar método optimizado que filtra en SQL en lugar de en memoria
                todasGestiones = gestionDAO.listarRecientesPorFincas(idsFincas, 10);
            } else {
                todasGestiones = List.of();
            }
            
            gestiones.setAll(todasGestiones);
            actualizarBalanceMensual();
        } catch (SQLException e) {
            String mensaje = esErrorDeConexion(e)
                ? "No se puede conectar a la base de datos MySQL.\n\n" +
                  "Verifica que Docker Desktop y el contenedor MySQL estén ejecutándose.\n\n" +
                  "Detalle técnico: " + e.getMessage()
                : "No se pudieron obtener las gestiones financieras.\nDetalle: " + e.getMessage();
            
            // No mostrar alerta aquí para evitar spam, solo log
            System.err.println("Error al cargar gestiones: " + mensaje);
            e.printStackTrace();
        }
    }

    private void actualizarBalanceMensual() {
        BigDecimal balanceGestiones = gestiones.stream()
            .map(gestion -> {
                if (gestion.getMonto() == null) {
                    return BigDecimal.ZERO;
                }
                return gestion.getTipo() == Tipo.INGRESO
                    ? gestion.getMonto()
                    : gestion.getMonto().negate();
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costoTratamientos = obtenerCostoTratamientosUsuario();
        BigDecimal balanceTotal = balanceGestiones.subtract(costoTratamientos);
        actualizarEstiloBalance(lblBalanceMensual, balanceTotal.doubleValue());
    }

    private void poblarIndicadores() {
        try {
            List<Finca> fincas = obtenerFincasDeSesion();
            long activas = fincas.stream().filter(f -> f.getEstado() == Finca.Estado.ACTIVA).count();
            double superficie = fincas.stream()
                .map(Finca::getSuperficie)
                .filter(s -> s != null)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
            lblFincasActivas.setText(String.valueOf(activas));
            lblSuperficieTotal.setText(String.format("%.2f ha", superficie));
            actualizarListaFincas(fincas);
        } catch (SQLException e) {
            lblFincasActivas.setText("-");
            lblSuperficieTotal.setText("-");
            lblBalanceMensual.setText("-");
            
            String mensaje = esErrorDeConexion(e)
                ? "No se puede conectar a la base de datos MySQL.\n\n" +
                  "Verifica que:\n" +
                  "• Docker Desktop esté ejecutándose\n" +
                  "• El contenedor MySQL esté corriendo\n" +
                  "• El puerto 3309 esté disponible\n\n" +
                  "Detalle técnico: " + e.getMessage()
                : "No se pudieron obtener los datos de fincas.\nDetalle: " + e.getMessage();
            
            mostrarAlerta(Alert.AlertType.ERROR, "Error de conexión", mensaje);
            e.printStackTrace();
        }
    }

    private boolean esErrorDeConexion(SQLException e) {
        String mensaje = e.getMessage().toLowerCase();
        return mensaje.contains("communications link failure") ||
               mensaje.contains("connection refused") ||
               mensaje.contains("could not create connection") ||
               (e.getSQLState() != null && e.getSQLState().startsWith("08"));
    }

    private void inicializarListaFincas() {
        if (listaFincas == null) {
            return;
        }
        listaFincas.setItems(fincasUsuario);
        listaFincas.setPlaceholder(new Label("Aún no has registrado fincas."));
        listaFincas.setCellFactory(lv -> new ListCell<Finca>() {
            @Override
            protected void updateItem(Finca item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String superficie = item.getSuperficie() == null
                        ? "-"
                        : String.format("%.1f ha", item.getSuperficie());
                    String estado = item.getEstado() != null ? item.getEstado().name() : "-";
                    setText(item.getNombre() + " · " + superficie + " · " + estado);
                }
            }
        });
        listaFincas.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                abrirFincaSeleccionada(null);
            }
        });
        listaFincas.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldSel, nueva) -> {
                if (nueva != null) {
                    // Guardar la finca seleccionada en la sesión
                    Sesion.getInstancia().setFincaSeleccionada(nueva);
                }
                actualizarMapaConFinca(nueva);
            });
        poblarListaFincasUsuario();
    }

    private void poblarListaFincasUsuario() {
        if (listaFincas == null) {
            return;
        }
        try {
            actualizarListaFincas(obtenerFincasDeSesion());
        } catch (SQLException e) {
            fincasUsuario.clear();
            listaFincas.setPlaceholder(new Label("No se pudieron cargar tus fincas."));
            System.err.println("Error al cargar lista de fincas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void poblarGraficaProduccion() {
        if (chartProduccion == null) {
            return;
        }
        chartProduccion.getData().clear();
        try {
            List<Finca> fincas = obtenerFincasDeSesion();
            Map<Integer, BigDecimal> estimadoPorMes = new HashMap<>();
            Map<Integer, BigDecimal> realPorMes = new HashMap<>();

            for (Finca finca : fincas) {
                if (finca.getId() == null) {
                    continue;
                }
                List<Cultivo> cultivos = cultivoDAO.listarPorFinca(finca.getId());
                for (Cultivo cultivo : cultivos) {
                    int mes = obtenerMesReferencia(cultivo);
                    if (mes < 1) {
                        continue;
                    }
                    if (cultivo.getRendimientoEstimado() != null) {
                        estimadoPorMes.merge(mes, cultivo.getRendimientoEstimado(), BigDecimal::add);
                    }
                    BigDecimal real = cultivo.getRendimientoReal() != null
                        ? cultivo.getRendimientoReal()
                        : cultivo.getProduccionKg();
                    if (real != null) {
                        realPorMes.merge(mes, real, BigDecimal::add);
                    }
                }
            }

            XYChart.Series<String, Number> serieEstimado = new XYChart.Series<>();
            serieEstimado.setName("Rend. estimado");
            XYChart.Series<String, Number> serieReal = new XYChart.Series<>();
            serieReal.setName("Rend. real");

            for (Month mes : Month.values()) {
                String etiqueta = mes.getDisplayName(TextStyle.SHORT, Locale.getDefault());
                BigDecimal estimado = estimadoPorMes.getOrDefault(mes.getValue(), BigDecimal.ZERO);
                BigDecimal real = realPorMes.getOrDefault(mes.getValue(), BigDecimal.ZERO);
                serieEstimado.getData().add(new XYChart.Data<String, Number>(etiqueta, estimado));
                serieReal.getData().add(new XYChart.Data<String, Number>(etiqueta, real));
            }
            chartProduccion.getData().addAll(serieEstimado, serieReal);
        } catch (SQLException e) {
            chartProduccion.getData().clear();
            System.err.println("Error al poblar gráfica de producción: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void crearFinca(ActionEvent event) {
        Sesion sesion = Sesion.getInstancia();
        if (!sesion.estaAutenticado() || sesion.getUsuarioActual() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sesión requerida",
                "Debes iniciar sesión para registrar una finca.");
            return;
        }

        Usuario propietario = sesion.getUsuarioActual();
        Optional<Finca> resultado = mostrarDialogoFinca(propietario);
        resultado.ifPresent(finca -> {
            try {
                fincaDAO.guardar(finca);
                poblarIndicadores();
                cargarGestionesRecientes();
                poblarListaFincasUsuario();
                mostrarAlerta(Alert.AlertType.INFORMATION, "Finca registrada",
                    "La finca \"" + finca.getNombre() + "\" se ha guardado correctamente.");
            } catch (SQLException e) {
                String mensaje = esErrorDeConexion(e)
                    ? "No se pudo conectar a la base de datos MySQL.\nDetalle: " + e.getMessage()
                    : "No se pudo registrar la finca.\nDetalle: " + e.getMessage();
                mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar finca", mensaje);
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void abrirPerfilUsuario(ActionEvent event) {
        Sesion sesion = Sesion.getInstancia();
        if (!sesion.estaAutenticado() || sesion.getUsuarioActual() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sesión requerida",
                "Debes iniciar sesión para gestionar tu perfil.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/vista/PerfilUsuarioDialog.fxml"));
            Parent root = loader.load();
            DialogoPerfilUsuarioController controller = loader.getController();
            controller.setUsuario(sesion.getUsuarioActual());

            Stage dialog = new Stage();
            dialog.setTitle("Mi perfil");
            dialog.initOwner(btnPerfilUsuario.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setScene(new Scene(root));
            dialog.getIcons().add(new Image(getClass().getResourceAsStream("/images/icono.png")));
            controller.setStage(dialog);
            dialog.showAndWait();
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Perfil",
                "No se pudo abrir la configuración de perfil.\nDetalle: " + e.getMessage());
        }
    }

    @FXML
    private void abrirFincaSeleccionada(ActionEvent event) {
        if (listaFincas == null) {
            return;
        }
        Finca seleccionada = listaFincas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Elige una finca para visualizarla.");
            return;
        }
        abrirDetalleFinca(seleccionada, null);
    }

    private void abrirDetalleFinca(Finca finca) {
        abrirDetalleFinca(finca, null);
    }

    private void abrirDetalleFinca(Finca finca, String tabDestino) {
        try {
            Sesion.getInstancia().setFincaSeleccionada(finca);
            Sesion.getInstancia().setTabDestino(tabDestino);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/vista/DetalleFinca.fxml"));
            Parent root = loader.load();
            ControladorDetalleFinca controller = loader.getController();
            controller.setFinca(finca);
            Stage stage = (Stage) lblFincasActivas.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Detalle de finca");
            stage.centerOnScreen();
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al abrir detalle",
                "No se pudo cargar la vista DetalleFinca.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void filtrarFincas(KeyEvent event) {
        if (listaFincas == null) {
            return;
        }
        String filtro = campoBuscar != null ? campoBuscar.getText() : "";
        if (filtro == null || filtro.isBlank()) {
            listaFincas.setItems(fincasUsuario);
            return;
        }
        String criterio = filtro.toLowerCase(Locale.getDefault());
        listaFincas.setItems(fincasUsuario.filtered(finca ->
            finca.getNombre() != null && finca.getNombre().toLowerCase(Locale.getDefault()).contains(criterio)));
    }

    @FXML
    private void navegarMenu(ActionEvent event) {
        Object source = event.getSource();
        if (source == btnNavFincas) {
            if (listaFincas != null) {
                listaFincas.requestFocus();
            }
        } else if (source == btnNavCultivos) {
            abrirSeccionDetalle("Cultivos");
        } else if (source == btnNavTratamientos) {
            abrirSeccionDetalle("Tratamientos");
        } else if (source == btnNavFinanzas) {
            abrirSeccionDetalle("Finanzas");
        } else if (source == btnNavInformes) {
            navegar("/main/vista/InformesUsuario.fxml", "AgroTrack - Informes");
        }
    }

    @FXML
    private void cerrarSesion(ActionEvent event) {
        Sesion.getInstancia().cerrarSesion();
        navegar("/main/vista/Login.fxml", "Inicio de sesión");
    }

    private void navegar(String ruta, String tituloVentana) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent root = loader.load();
            Stage stage = (Stage) lblFincasActivas.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(tituloVentana);
            stage.centerOnScreen();
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al navegar",
                "No se pudo cargar la vista: " + e.getMessage());
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

    private void actualizarEstiloBalance(Label etiqueta, double valor) {
        if (etiqueta == null) {
            return;
        }
        etiqueta.setText(String.format("%+.2f €", valor));
        if (valor < 0) {
            etiqueta.setStyle("-fx-text-fill: #c62828; -fx-font-size: 32px; -fx-font-weight: bold;");
        } else if (valor > 0) {
            etiqueta.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 32px; -fx-font-weight: bold;");
        } else {
            etiqueta.setStyle("-fx-text-fill: #37474f; -fx-font-size: 32px; -fx-font-weight: bold;");
        }
    }

    private void inicializarMenuLateral() {
        if (btnNavFincas == null) {
            return;
        }
        menuToggleGroup = new ToggleGroup();
        btnNavFincas.setToggleGroup(menuToggleGroup);
        btnNavCultivos.setToggleGroup(menuToggleGroup);
        btnNavTratamientos.setToggleGroup(menuToggleGroup);
        btnNavFinanzas.setToggleGroup(menuToggleGroup);
        btnNavInformes.setToggleGroup(menuToggleGroup);
        btnNavFincas.setSelected(true);
    }

    private void configurarBotonPerfil() {
        if (btnPerfilUsuario == null) {
            return;
        }
        Sesion sesion = Sesion.getInstancia();
        boolean visible = sesion.estaAutenticado() && !sesion.esAdministrador();
        btnPerfilUsuario.setVisible(visible);
        btnPerfilUsuario.setManaged(visible);
    }

    private void inicializarMapaDashboard() {
        if (panelMapa == null) {
            return;
        }
        
        // Limpiar WebViews anteriores si existen
        panelMapa.getChildren().removeIf(node -> node instanceof WebView);
        
        if (webViewMapa == null) {
            webViewMapa = new WebView();
            mapaEngine = webViewMapa.getEngine();
            
            if (loadingMapa != null) {
                loadingMapa.managedProperty().bind(loadingMapa.visibleProperty());
                loadingMapa.setVisible(true);
            }
            
            // Añadir listener solo una vez
            mapaEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    if (loadingMapa != null) {
                        loadingMapa.setVisible(false);
                    }
                    ocultarControlesMapa();
                    refrescarMapaWeb();
                    actualizarMapaConFinca(listaFincas != null
                        ? listaFincas.getSelectionModel().getSelectedItem()
                        : null);
                } else if (newState == Worker.State.RUNNING && loadingMapa != null) {
                    loadingMapa.setVisible(true);
                }
            });
        }
        
        // Cargar el HTML del mapa solo si no está cargado
        if (mapaEngine != null && mapaEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED 
            && mapaEngine.getLoadWorker().getState() != Worker.State.RUNNING) {
            try {
                // Usar el mapa simple mejorado
                String html = leerRecursoComoString("/mapa/mapa_simple.html");
                if (html != null && !html.isEmpty()) {
                    mapaEngine.loadContent(html);
                } else {
                    URL mapaUrl = getClass().getResource("/mapa/mapa_simple.html");
                    if (mapaUrl != null) {
                        mapaEngine.load(mapaUrl.toExternalForm());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al cargar mapa: " + e.getMessage());
                URL mapaUrl = getClass().getResource("/mapa/mapa_simple.html");
                if (mapaUrl != null) {
                    mapaEngine.load(mapaUrl.toExternalForm());
                }
            }
        }
        
        // Añadir el WebView al panel solo si no está ya añadido
        if (!panelMapa.getChildren().contains(webViewMapa)) {
            panelMapa.getChildren().add(0, webViewMapa);
        }
    }

    private void actualizarMapaConFinca(Finca finca) {
        if (mapaEngine == null) {
            return;
        }
        if (finca == null || finca.getCoordenadasPoligono() == null
            || finca.getCoordenadasPoligono().isBlank()) {
            mapaEngine.executeScript("window.limpiar ? window.limpiar() : null;");
            refrescarMapaWeb();
            return;
        }
        String coords = finca.getCoordenadasPoligono()
            .replace("\\", "\\\\")
            .replace("'", "\\'");
        mapaEngine.executeScript(
            "window.cargarCoordenadasDesdeJava && window.cargarCoordenadasDesdeJava('"
                + coords + "');");
        ocultarControlesMapa();
        refrescarMapaWeb();
    }

    private void ocultarControlesMapa() {
        if (mapaEngine == null) {
            return;
        }
        mapaEngine.executeScript(
            "var c = document.querySelector('.controls'); if (c) { c.style.display = 'none'; }");
    }

    private void refrescarMapaWeb() {
        if (mapaEngine == null) {
            return;
        }
        mapaEngine.executeScript(
            "window.refrescarMapaDesdeJava && window.refrescarMapaDesdeJava();");
    }

    private void abrirSeccionDetalle(String tabDestino) {
        if (listaFincas == null) {
            return;
        }
        Finca seleccionada = listaFincas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección",
                "Selecciona una finca para acceder a " + tabDestino.toLowerCase(Locale.getDefault()) + ".");
            return;
        }
        abrirDetalleFinca(seleccionada, tabDestino);
    }

    private int obtenerMesReferencia(Cultivo cultivo) {
        LocalDate referencia = cultivo.getFechaCosecha() != null
            ? cultivo.getFechaCosecha()
            : cultivo.getFechaSiembra();
        return referencia != null ? referencia.getMonthValue() : -1;
    }

    private List<Finca> obtenerFincasDeSesion() throws SQLException {
        Sesion sesion = Sesion.getInstancia();
        if (sesion.esAdministrador()) {
            return fincaDAO.listarTodas();
        } else if (sesion.estaAutenticado() && sesion.getUsuarioActual().getId() != null) {
            return fincaDAO.listarPorUsuario(sesion.getUsuarioActual().getId());
        }
        return List.of();
    }

    private void actualizarListaFincas(List<Finca> fincas) {
        if (listaFincas == null) {
            return;
        }
        fincasUsuario.setAll(fincas);
        if (fincasUsuario.isEmpty()) {
            listaFincas.setPlaceholder(new Label("Aún no has registrado fincas."));
            listaFincas.getSelectionModel().clearSelection();
            actualizarMapaConFinca(null);
            return;
        }
        if (campoBuscar == null || campoBuscar.getText() == null || campoBuscar.getText().isBlank()) {
            listaFincas.setItems(fincasUsuario);
        } else {
            filtrarFincas(null);
        }
        
        // Intentar restaurar la finca seleccionada de la sesión
        Finca fincaSeleccionada = Sesion.getInstancia().getFincaSeleccionada();
        if (fincaSeleccionada != null && fincaSeleccionada.getId() != null) {
            // Buscar la finca en la lista por ID
            fincasUsuario.stream()
                .filter(f -> f.getId() != null && f.getId().equals(fincaSeleccionada.getId()))
                .findFirst()
                .ifPresent(f -> listaFincas.getSelectionModel().select(f));
        }
        
        // Si aún no hay selección, seleccionar la primera
        if (listaFincas.getSelectionModel().getSelectedItem() == null) {
            listaFincas.getSelectionModel().selectFirst();
        }
        actualizarMapaConFinca(listaFincas.getSelectionModel().getSelectedItem());
    }

    private BigDecimal obtenerCostoTratamientosUsuario() {
        try {
            List<Finca> fincas = obtenerFincasDeSesion();
            BigDecimal total = BigDecimal.ZERO;
            for (Finca finca : fincas) {
                if (finca.getId() != null) {
                    total = total.add(tratamientoDAO.obtenerCostoTotalPorFinca(finca.getId()));
                }
            }
            return total;
        } catch (SQLException e) {
            System.err.println("Error al obtener costo de tratamientos: " + e.getMessage());
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }

    private Optional<Finca> mostrarDialogoFinca(Usuario propietario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/vista/FincaForm.fxml"));
            Parent root = loader.load();
            DialogoFincaController controller = loader.getController();
            Stage dialogStage = crearDialogoStage("Registrar finca", root);
            controller.setStage(dialogStage);
            controller.setUsuarios(List.of(propietario));
            controller.configurarPropietarioFijo(propietario);
            controller.setPoligonoRequerido(true);
            dialogStage.showAndWait();
            return Optional.ofNullable(controller.getFincaResultado());
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al abrir formulario",
                "No se pudo cargar el formulario de fincas.\nDetalle: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Stage crearDialogoStage(String titulo, Parent root) {
        Stage stage = new Stage();
        stage.setTitle(titulo);
        stage.initOwner(lblFincasActivas.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        // Configurar icono
        try {
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icono.png")));
        } catch (Exception e) {
            // Si no se puede cargar el icono, continuar sin él
        }
        return stage;
    }
    
    private String leerRecursoComoString(String ruta) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(ruta)) {
            if (is == null) {
                throw new Exception("No se encontró el recurso: " + ruta);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}

