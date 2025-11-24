package controlador;

import crud.ConexionBD;
import crud.CultivoDAO;
import crud.FincaDAO;
import crud.GestionFinancieraDAO;
import crud.TratamientoDAO;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Modality;
import modelo.Cultivo;
import modelo.Finca;
import modelo.GestionFinanciera;
import modelo.Tratamiento;
import modelo.Usuario;
import util.Sesion;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import java.util.HashMap;
import java.util.Map;

public class ControladorDetalleFinca {

    private final CultivoDAO cultivoDAO = new CultivoDAO();
    private final TratamientoDAO tratamientoDAO = new TratamientoDAO();
    private final GestionFinancieraDAO gestionDAO = new GestionFinancieraDAO();
    private final FincaDAO fincaDAO = new FincaDAO();
    private Finca fincaActual;
    private JasperPrint jasperPrintActual = null;

    @FXML
    private TabPane tabPaneFinca;
    @FXML
    private Label lblNombreFinca;
    @FXML
    private Label lblEstadoFinca;
    @FXML
    private Label lblSuperficie;
    @FXML
    private Label lblPropietario;
    @FXML
    private Label lblUltimoCultivo;
    @FXML
    private Label lblBalanceAnual;
    @FXML
    private Button btnVolver;
    @FXML
    private Button btnEditarFinca;
    @FXML
    private Button btnGenerarInforme;
    @FXML
    private Button btnDescargarPDF;

    // Cultivos
    @FXML
    private TableView<Cultivo> tablaCultivos;
    @FXML
    private TableColumn<Cultivo, String> colCultivoNombre;
    @FXML
    private TableColumn<Cultivo, String> colCultivoVariedad;
    @FXML
    private TableColumn<Cultivo, String> colCultivoEstado;
    @FXML
    private TableColumn<Cultivo, String> colCultivoProduccion;
    @FXML
    private TextField campoBuscarCultivos;
    @FXML
    private Label lblCultivoNombre;
    @FXML
    private Label lblCultivoVariedad;
    @FXML
    private Label lblCultivoEstado;
    @FXML
    private Label lblCultivoSiembra;
    @FXML
    private Label lblCultivoCosecha;
    @FXML
    private TextArea areaCultivoNotas;

    // Tratamientos
    @FXML
    private TableView<Tratamiento> tablaTratamientos;
    @FXML
    private TableColumn<Tratamiento, String> colTratamientoCultivo;
    @FXML
    private TableColumn<Tratamiento, LocalDate> colTratamientoFecha;
    @FXML
    private TableColumn<Tratamiento, String> colTratamientoProducto;
    @FXML
    private TableColumn<Tratamiento, String> colTratamientoTipo;
    @FXML
    private TableColumn<Tratamiento, String> colTratamientoCosto;
    @FXML
    private TextField campoBuscarTratamientos;

    // Finanzas
    @FXML
    private TableView<GestionFinanciera> tablaFinanzas;
    @FXML
    private TableColumn<GestionFinanciera, LocalDate> colFinanzaFecha;
    @FXML
    private TableColumn<GestionFinanciera, String> colFinanzaTipo;
    @FXML
    private TableColumn<GestionFinanciera, String> colFinanzaConcepto;
    @FXML
    private TableColumn<GestionFinanciera, String> colFinanzaMonto;
    @FXML
    private TextField campoBuscarFinanzas;
    @FXML
    private Label lblFinanzaConcepto;
    @FXML
    private Label lblFinanzaTipo;
    @FXML
    private Label lblFinanzaMonto;
    @FXML
    private Label lblFinanzaFecha;
    @FXML
    private TextArea areaFinanzaObservaciones;

    // Mapa
    @FXML
    private StackPane panelMapaFinca;
    @FXML
    private Button btnEditarPoligono;
    @FXML
    private ProgressIndicator loadingMapaFinca;

    private WebView webView;
    private WebEngine webEngine;
    private boolean volverAAdmin = false;

    private final ObservableList<Cultivo> cultivos = FXCollections.observableArrayList();
    private final ObservableList<Tratamiento> tratamientos = FXCollections.observableArrayList();
    private final ObservableList<GestionFinanciera> finanzas = FXCollections.observableArrayList();
    private BigDecimal costoTratamientos = BigDecimal.ZERO;

    @FXML
    private void initialize() {
        inicializarTablaCultivos();
        inicializarTablaTratamientos();
        inicializarTablaFinanzas();
        inicializarMapa();
        
        // Intentar obtener la finca de la sesión si no se estableció explícitamente
        if (fincaActual == null) {
            Finca fincaSesion = Sesion.getInstancia().getFincaSeleccionada();
            if (fincaSesion != null) {
                setFinca(fincaSesion);
            }
        }
    }

    private void inicializarMapa() {
        if (panelMapaFinca == null) {
            return;
        }
        
        // Limpiar WebViews anteriores si existen
        panelMapaFinca.getChildren().removeIf(node -> node instanceof WebView);
        
        // Si ya existe un WebView, reutilizarlo
        if (webView == null) {
            webView = new WebView();
            webEngine = webView.getEngine();
            webView.setPrefWidth(800);
            webView.setPrefHeight(600);
            
            // Ocultar loading cuando cargue y configurar bridge Java-JS
            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    if (loadingMapaFinca != null) {
                        loadingMapaFinca.setVisible(false);
                    }
                    // Configurar el bridge Java-JS
                    try {
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        window.setMember("javaController", this);
                        // Cargar coordenadas después de un pequeño delay para asegurar que el mapa esté listo
                        Platform.runLater(() -> {
                            try {
                                Thread.sleep(300);
                                Platform.runLater(() -> cargarCoordenadasEnMapa());
                            } catch (InterruptedException e) {
                                cargarCoordenadasEnMapa();
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("Error al configurar bridge Java-JS: " + e.getMessage());
                    }
                }
            });
        }
        
        // Cargar el HTML del mapa solo si no está cargado
        if (webEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED 
            && webEngine.getLoadWorker().getState() != Worker.State.RUNNING) {
            try {
                // Usar el mapa simple mejorado
                String html = leerRecursoComoString("/mapa/mapa_simple.html");
                if (html != null && !html.isEmpty()) {
                    webEngine.loadContent(html);
                } else {
                    URL mapaUrl = getClass().getResource("/mapa/mapa_simple.html");
                    if (mapaUrl != null) {
                        webEngine.load(mapaUrl.toExternalForm());
                    } else {
                        mostrarAlerta(Alert.AlertType.WARNING, "Error de mapa",
                            "No se pudo cargar el archivo del mapa.");
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al cargar mapa: " + e.getMessage());
                URL mapaUrl = getClass().getResource("/mapa/mapa_simple.html");
                if (mapaUrl != null) {
                    webEngine.load(mapaUrl.toExternalForm());
                } else {
                    mostrarAlerta(Alert.AlertType.WARNING, "Error de mapa",
                        "No se pudo cargar el archivo del mapa.");
                    return;
                }
            }
        }
        
        // Añadir el WebView al panel solo si no está ya añadido
        if (!panelMapaFinca.getChildren().contains(webView)) {
            panelMapaFinca.getChildren().add(0, webView);
        }
    }

    private void cargarCoordenadasEnMapa() {
        if (fincaActual == null || fincaActual.getCoordenadasPoligono() == null 
            || fincaActual.getCoordenadasPoligono().trim().isEmpty()) {
            return;
        }
        
        String coordenadas = fincaActual.getCoordenadasPoligono();
        webEngine.executeScript("window.cargarCoordenadasDesdeJava('" + coordenadas + "');");
    }

    @FXML
    private void editarPoligono(ActionEvent event) {
        if (fincaActual == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin finca", "No hay una finca seleccionada.");
            return;
        }
        if (webEngine == null) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Mapa no disponible",
                "El mapa aún no ha terminado de cargar. Intenta nuevamente en unos segundos.");
            return;
        }
        try {
            webEngine.executeScript("window.iniciarEdicionDesdeJava && window.iniciarEdicionDesdeJava();");
            mostrarAlerta(Alert.AlertType.INFORMATION, "Modo edición activado",
                "Ya puedes dibujar o ajustar el polígono.\n" +
                "1. Añade puntos con clic izquierdo.\n" +
                "2. Pulsa \"Finalizar polígono\" cuando cierres el perímetro.\n" +
                "3. Guarda las coordenadas antes de salir.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al activar edición",
                "No se pudo iniciar el modo de edición del mapa.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void guardarCoordenadasDesdeMapa(String coordenadasJson) {
        if (fincaActual == null) {
            return;
        }
        
        try {
            fincaActual.setCoordenadasPoligono(coordenadasJson);
            fincaDAO.actualizar(fincaActual);
            mostrarAlerta(Alert.AlertType.INFORMATION, "Coordenadas guardadas",
                "El polígono de la finca se ha guardado correctamente.");
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar",
                "No se pudieron guardar las coordenadas.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setFinca(Finca finca) {
        this.fincaActual = finca;
        if (finca == null) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Detalle de finca",
                "No se ha proporcionado una finca para mostrar.");
            return;
        }
        poblarEncabezado();
        cargarCultivos();
        cargarFinanzas();
        // Recargar coordenadas en el mapa si el WebView ya está cargado
        if (webEngine != null && webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            cargarCoordenadasEnMapa();
        }
        aplicarTabDestino();
    }

    private void poblarEncabezado() {
        lblNombreFinca.setText(fincaActual.getNombre());
        if (fincaActual.getEstado() != null) {
            lblEstadoFinca.setText(capitalizar(fincaActual.getEstado().name()));
            String estiloEstado = fincaActual.getEstado() == Finca.Estado.ACTIVA
                ? "-fx-background-color: #c8e6c9; -fx-text-fill: #0d4528;"
                : "-fx-background-color: #ffe0b2; -fx-text-fill: #a84300;";
            lblEstadoFinca.setStyle(estiloEstado + " -fx-padding: 4 12; -fx-background-radius: 12;");
        } else {
            lblEstadoFinca.setText("-");
            lblEstadoFinca.setStyle("-fx-background-color: transparent; -fx-text-fill: #5f6b6f;");
        }
        lblSuperficie.setText(fincaActual.getSuperficie() == null ? "-" : fincaActual.getSuperficie() + " ha");
        lblPropietario.setText(fincaActual.getPropietario() != null ? fincaActual.getPropietario().toString() : "-");
        lblUltimoCultivo.setText("-");
        lblBalanceAnual.setText("-");
    }

    private String capitalizar(String texto) {
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }

    private void inicializarTablaCultivos() {
        colCultivoNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colCultivoVariedad.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVariedad()));
        colCultivoEstado.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEstado().name()));
        colCultivoProduccion.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getProduccionKg() == null ? "-" : data.getValue().getProduccionKg() + " kg"));

        tablaCultivos.setItems(cultivos);
        tablaCultivos.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldSel, newSel) -> {
                mostrarDetalleCultivo(newSel);
                cargarTratamientos(newSel);
            });
    }

    private void cargarCultivos() {
        cultivos.clear();
        if (fincaActual == null) {
            return;
        }
        try {
            cultivos.setAll(cultivoDAO.listarPorFinca(fincaActual.getId()));
            if (!cultivos.isEmpty()) {
                tablaCultivos.getSelectionModel().selectFirst();
                lblUltimoCultivo.setText(cultivos.get(0).getNombre());
            } else {
                lblUltimoCultivo.setText("-");
            }
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar cultivos",
                "No se pudieron obtener los cultivos de la finca.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void mostrarDetalleCultivo(Cultivo cultivo) {
        if (cultivo == null) {
            lblCultivoNombre.setText("-");
            lblCultivoVariedad.setText("-");
            lblCultivoEstado.setText("-");
            lblCultivoSiembra.setText("-");
            lblCultivoCosecha.setText("-");
            areaCultivoNotas.clear();
            return;
        }
        lblCultivoNombre.setText(cultivo.getNombre());
        lblCultivoVariedad.setText(cultivo.getVariedad());
        lblCultivoEstado.setText(cultivo.getEstado().name());
        lblCultivoSiembra.setText(cultivo.getFechaSiembra() == null ? "-" : cultivo.getFechaSiembra().toString());
        lblCultivoCosecha.setText(cultivo.getFechaCosecha() == null ? "-" : cultivo.getFechaCosecha().toString());
        areaCultivoNotas.setText("Notas pendientes para " + cultivo.getNombre());
    }

    private void inicializarTablaTratamientos() {
        colTratamientoCultivo.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCultivo() != null ? data.getValue().getCultivo().getNombre() : "-"));
        colTratamientoFecha.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getFecha()));
        colTratamientoProducto.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProducto()));
        colTratamientoTipo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipo().name()));
        colTratamientoCosto.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getPrecioTratamiento() == null ? "-" : data.getValue().getPrecioTratamiento() + " €"));

        tablaTratamientos.setItems(tratamientos);
    }

    private void cargarTratamientos(Cultivo cultivo) {
        tratamientos.clear();
        if (cultivo == null) {
            return;
        }
        try {
            List<Tratamiento> lista = tratamientoDAO.listarPorCultivo(cultivo.getId());
            lista.forEach(t -> t.setCultivo(cultivo));
            tratamientos.setAll(lista);
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar tratamientos",
                "No se pudieron obtener los tratamientos del cultivo.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void inicializarTablaFinanzas() {
        colFinanzaFecha.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getFecha()));
        colFinanzaTipo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipo().name()));
        colFinanzaConcepto.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getConcepto()));
        colFinanzaMonto.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getMonto() == null ? "-" : data.getValue().getMonto() + " €"));

        tablaFinanzas.setItems(finanzas);
        tablaFinanzas.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldSel, newSel) -> mostrarDetalleFinanza(newSel));
    }

    private void cargarFinanzas() {
        finanzas.clear();
        if (fincaActual == null) {
            return;
        }
        try {
            finanzas.setAll(gestionDAO.listarPorFinca(fincaActual.getId()));
            costoTratamientos = tratamientoDAO.obtenerCostoTotalPorFinca(fincaActual.getId());
            actualizarBalance();
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar finanzas",
                "No se pudieron obtener las gestiones financieras.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void actualizarBalance() {
        BigDecimal balanceGestiones = finanzas.stream()
            .map(gestion -> {
                if (gestion.getMonto() == null) {
                    return BigDecimal.ZERO;
                }
                return gestion.getTipo() == GestionFinanciera.Tipo.INGRESO
                    ? gestion.getMonto()
                    : gestion.getMonto().negate();
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTratamientos = costoTratamientos != null ? costoTratamientos : BigDecimal.ZERO;
        BigDecimal balanceTotal = balanceGestiones.subtract(totalTratamientos);
        actualizarEstiloBalance(lblBalanceAnual, balanceTotal.doubleValue());
    }

    public void setVolverAAdmin(boolean volverAAdmin) {
        this.volverAAdmin = volverAAdmin;
        if (btnVolver != null) {
            btnVolver.setText(volverAAdmin ? "Volver al panel admin" : "Volver");
        }
    }

    @FXML
    private void volver(ActionEvent event) {
        if (volverAAdmin) {
            navegar("/main/vista/AdminPanel.fxml", "Panel de administración");
        } else {
            navegar("/main/vista/Dashboard.fxml", "AgroTrack - Dashboard");
        }
    }

    @FXML
    private void generarInforme(ActionEvent event) {
        if (fincaActual == null || fincaActual.getId() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin finca seleccionada",
                "No hay una finca seleccionada para generar el informe.");
            return;
        }

        try {
            Connection conexion = ConexionBD.obtenerConexion();
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("ParametroFincaId", fincaActual.getId());
            String rutaInforme = "/reports/InformeFiltra.jrxml";
            
            lanzarInforme(rutaInforme, parametros, conexion);
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de conexión",
                "No se pudo conectar a la base de datos.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void lanzarInforme(String rutaInf, Map<String, Object> param, Connection conexion) {
        try {
            // Intentar diferentes rutas posibles
            InputStream reportStream = getClass().getResourceAsStream(rutaInf);
            if (reportStream == null) {
                // Intentar sin el slash inicial
                String rutaAlternativa = rutaInf.startsWith("/") ? rutaInf.substring(1) : "/" + rutaInf;
                reportStream = getClass().getResourceAsStream(rutaAlternativa);
            }
            if (reportStream == null) {
                // Intentar desde el classloader
                reportStream = getClass().getClassLoader().getResourceAsStream(rutaInf.startsWith("/") ? rutaInf.substring(1) : rutaInf);
            }
            if (reportStream == null) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo cargar el informe: " + rutaInf + "\nVerifica que el archivo existe en src/main/resources/reports/");
                return;
            }

            JasperReport report = JasperCompileManager.compileReport(reportStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, param, conexion);
            jasperPrintActual = jasperPrint; // Guardar para exportar a PDF

            if (!jasperPrint.getPages().isEmpty()) {
                String outputHtmlFile = "informeFinca_" + fincaActual.getId() + ".html";
                JasperExportManager.exportReportToHtmlFile(jasperPrint, outputHtmlFile);
                mostrarInformeEnNuevaVentana(outputHtmlFile, "Informe de Finca - " + fincaActual.getNombre());
            } else {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Información",
                    "La finca seleccionada no generó páginas en el informe.");
            }
        } catch (JRException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al generar el informe",
                "No se pudo generar el informe.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrarInformeEnNuevaVentana(String archivoHTML, String titulo) {
        Stage stage = new Stage();
        stage.setTitle(titulo);
        // Configurar icono
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icono.png")));
        } catch (Exception e) {
            // Si no se puede cargar el icono, continuar sin él
        }
        WebView webView = new WebView();
        webView.getEngine().load(new File(archivoHTML).toURI().toString());
        StackPane stackPane = new StackPane(webView);
        Scene scene = new Scene(stackPane, 800, 600);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.show();
    }

    @FXML
    private void descargarPDF(ActionEvent event) {
        if (jasperPrintActual == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso",
                "Primero debes generar el informe antes de descargarlo en PDF.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar informe como PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        
        String nombreArchivo = fincaActual != null && fincaActual.getNombre() != null
            ? "Informe_Finca_" + fincaActual.getNombre().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf"
            : "Informe_Finca.pdf";
        fileChooser.setInitialFileName(nombreArchivo);

        Stage stage = (Stage) (btnDescargarPDF != null ? btnDescargarPDF.getScene().getWindow() : null);
        if (stage == null && lblNombreFinca != null) {
            stage = (Stage) lblNombreFinca.getScene().getWindow();
        }
        
        if (stage != null) {
            File archivo = fileChooser.showSaveDialog(stage);

            if (archivo != null) {
                try {
                    JasperExportManager.exportReportToPdfFile(jasperPrintActual, archivo.getAbsolutePath());
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito",
                        "El informe se ha descargado correctamente en:\n" + archivo.getAbsolutePath());
                } catch (JRException e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error",
                        "No se pudo exportar el informe a PDF:\n" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    @FXML
    private void editarFinca(ActionEvent event) {
        if (fincaActual == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin finca",
                "No hay una finca seleccionada para editar.");
            return;
        }
        mostrarDialogoFinca(fincaActual).ifPresent(fincaEditada -> {
            try {
                fincaDAO.actualizar(fincaEditada);
                setFinca(fincaEditada);
                Sesion.getInstancia().setFincaSeleccionada(fincaEditada);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Finca actualizada",
                    "Los cambios de la finca se guardaron correctamente.");
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al actualizar finca",
                    "No se pudieron guardar los cambios.\nDetalle: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void verTratamientosCultivo(ActionEvent event) {
        Cultivo cultivo = tablaCultivos.getSelectionModel().getSelectedItem();
        if (cultivo == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona un cultivo.");
            return;
        }
        mostrarAlerta(Alert.AlertType.INFORMATION, "Tratamientos",
            "Tratamientos registrados para " + cultivo.getNombre() + ": " + tratamientos.size());
    }

    @FXML
    private void nuevoCultivo(ActionEvent event) {
        if (fincaActual == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin finca",
                "No hay una finca seleccionada para registrar cultivos.");
            return;
        }
        Optional<Cultivo> resultado = mostrarDialogoCultivo(null);
        resultado.ifPresent(cultivo -> {
            try {
                cultivo.setFincaId(fincaActual.getId());
                cultivoDAO.guardar(cultivo);
                cargarCultivos();
                seleccionarCultivoEnTabla(cultivo.getId());
                cargarFinanzas();
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar cultivo",
                    "No se pudo registrar el cultivo.\nDetalle: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void editarCultivo(ActionEvent event) {
        Cultivo seleccionado = tablaCultivos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona un cultivo para editar.");
            return;
        }
        Optional<Cultivo> resultado = mostrarDialogoCultivo(seleccionado);
        resultado.ifPresent(cultivo -> {
            try {
                cultivoDAO.actualizar(cultivo);
                cargarCultivos();
                seleccionarCultivoEnTabla(cultivo.getId());
                cargarFinanzas();
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al actualizar cultivo",
                    "No se pudo actualizar el cultivo.\nDetalle: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void eliminarCultivo(ActionEvent event) {
        Cultivo seleccionado = tablaCultivos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona un cultivo para eliminar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar cultivo");
        confirm.setHeaderText(null);
        confirm.setContentText("Esta acción eliminará el cultivo \"" + seleccionado.getNombre()
            + "\" y sus tratamientos asociados. ¿Deseas continuar?");
        confirm.initOwner(lblNombreFinca.getScene().getWindow());
        // Configurar icono
        try {
            Stage stage = (Stage) confirm.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icono.png")));
        } catch (Exception e) {
            // Si no se puede cargar el icono, continuar sin él
        }
        Optional<ButtonType> respuesta = confirm.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }
        try {
            cultivoDAO.eliminar(seleccionado.getId());
            cargarCultivos();
            tratamientos.clear();
            tablaTratamientos.getSelectionModel().clearSelection();
            cargarFinanzas();
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "No se puede eliminar",
                "No fue posible eliminar el cultivo. Asegúrate de que no tenga información dependiente.\nDetalle: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void nuevoTratamiento(ActionEvent event) {
        Cultivo cultivoSeleccionado = tablaCultivos.getSelectionModel().getSelectedItem();
        if (cultivoSeleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona un cultivo antes de registrar un tratamiento.");
            return;
        }
        Optional<Tratamiento> resultado = mostrarDialogoTratamiento(null, cultivoSeleccionado);
        resultado.ifPresent(tratamiento -> {
            try {
                tratamientoDAO.guardar(tratamiento);
                if (tratamiento.getCultivo() != null) {
                    tablaCultivos.getSelectionModel().select(tratamiento.getCultivo());
                }
                cargarTratamientos(tablaCultivos.getSelectionModel().getSelectedItem());
                seleccionarTratamientoEnTabla(tratamiento.getId());
                cargarFinanzas();
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar tratamiento",
                    "No se pudo registrar el tratamiento.\nDetalle: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void editarTratamiento(ActionEvent event) {
        Tratamiento seleccionado = tablaTratamientos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona un tratamiento para editar.");
            return;
        }
        Cultivo cultivoBase = seleccionado.getCultivo() != null
            ? seleccionado.getCultivo()
            : tablaCultivos.getSelectionModel().getSelectedItem();
        Optional<Tratamiento> resultado = mostrarDialogoTratamiento(seleccionado, cultivoBase);
        resultado.ifPresent(tratamiento -> {
            try {
                tratamientoDAO.actualizar(tratamiento);
                if (tratamiento.getCultivo() != null) {
                    tablaCultivos.getSelectionModel().select(tratamiento.getCultivo());
                }
                cargarTratamientos(tablaCultivos.getSelectionModel().getSelectedItem());
                seleccionarTratamientoEnTabla(tratamiento.getId());
                cargarFinanzas();
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al actualizar tratamiento",
                    "No se pudo actualizar el tratamiento.\nDetalle: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void eliminarTratamiento(ActionEvent event) {
        Tratamiento seleccionado = tablaTratamientos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona un tratamiento para eliminar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar tratamiento");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar el tratamiento \"" + seleccionado.getProducto() + "\"?");
        confirm.initOwner(lblNombreFinca.getScene().getWindow());
        // Configurar icono
        try {
            Stage stage = (Stage) confirm.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icono.png")));
        } catch (Exception e) {
            // Si no se puede cargar el icono, continuar sin él
        }
        Optional<ButtonType> respuesta = confirm.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }
        try {
            tratamientoDAO.eliminar(seleccionado.getId());
            Cultivo cultivoContexto = tablaCultivos.getSelectionModel().getSelectedItem();
            cargarTratamientos(cultivoContexto);
            cargarFinanzas();
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar tratamiento",
                "No se pudo eliminar el tratamiento.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void nuevaGestion(ActionEvent event) {
        if (fincaActual == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin finca", "Selecciona una finca para registrar movimientos.");
            return;
        }
        Optional<GestionFinanciera> resultado = mostrarDialogoGestion(null);
        resultado.ifPresent(gestion -> {
            try {
                gestion.setFincaId(fincaActual.getId());
                gestionDAO.guardar(gestion);
                cargarFinanzas();
                seleccionarGestionEnTabla(gestion.getId());
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar movimiento",
                    "No se pudo registrar el movimiento financiero.\nDetalle: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void editarGestion(ActionEvent event) {
        GestionFinanciera seleccionada = tablaFinanzas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona un movimiento para editar.");
            return;
        }
        Optional<GestionFinanciera> resultado = mostrarDialogoGestion(seleccionada);
        resultado.ifPresent(gestion -> {
            try {
                gestionDAO.actualizar(gestion);
                cargarFinanzas();
                seleccionarGestionEnTabla(gestion.getId());
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al actualizar movimiento",
                    "No se pudo actualizar el movimiento.\nDetalle: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void eliminarGestion(ActionEvent event) {
        GestionFinanciera seleccionada = tablaFinanzas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", "Selecciona un movimiento para eliminar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar movimiento");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar el movimiento \"" + seleccionada.getConcepto() + "\"?");
        confirm.initOwner(lblNombreFinca.getScene().getWindow());
        // Configurar icono
        try {
            Stage stage = (Stage) confirm.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icono.png")));
        } catch (Exception e) {
            // Si no se puede cargar el icono, continuar sin él
        }
        Optional<ButtonType> respuesta = confirm.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }
        try {
            gestionDAO.eliminar(seleccionada.getId());
            cargarFinanzas();
            mostrarDetalleFinanza(null);
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar movimiento",
                "No se pudo eliminar el movimiento seleccionado.\nDetalle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Optional<Cultivo> mostrarDialogoCultivo(Cultivo cultivo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/vista/CultivoForm.fxml"));
            Parent root = loader.load();
            DialogoCultivoController controller = loader.getController();
            Stage dialogStage = crearDialogoStage(cultivo == null ? "Nuevo cultivo" : "Editar cultivo", root);
            controller.setStage(dialogStage);
            controller.setFinca(fincaActual);
            controller.setCultivo(cultivo);
            dialogStage.showAndWait();
            return Optional.ofNullable(controller.getCultivoResultado());
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al abrir formulario",
                "No se pudo cargar el formulario de cultivo.\nDetalle: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Optional<Tratamiento> mostrarDialogoTratamiento(Tratamiento tratamiento, Cultivo cultivoPorDefecto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/vista/TratamientoForm.fxml"));
            Parent root = loader.load();
            DialogoTratamientoController controller = loader.getController();
            Stage dialogStage = crearDialogoStage(
                tratamiento == null ? "Nuevo tratamiento" : "Editar tratamiento", root);
            controller.setStage(dialogStage);
            controller.setCultivos(new ArrayList<>(cultivos));
            if (tratamiento != null) {
                controller.setTratamiento(tratamiento);
            }
            if (cultivoPorDefecto != null) {
                controller.seleccionarCultivo(cultivoPorDefecto);
            }
            dialogStage.showAndWait();
            return Optional.ofNullable(controller.getTratamientoResultado());
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al abrir formulario",
                "No se pudo cargar el formulario de tratamiento.\nDetalle: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Stage crearDialogoStage(String titulo, Parent root) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(titulo);
        dialogStage.initOwner(lblNombreFinca.getScene().getWindow());
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setScene(new Scene(root));
        dialogStage.setResizable(false);
        // Configurar icono
        try {
            dialogStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icono.png")));
        } catch (Exception e) {
            // Si no se puede cargar el icono, continuar sin él
        }
        return dialogStage;
    }

    private void seleccionarCultivoEnTabla(Integer cultivoId) {
        if (cultivoId == null) {
            return;
        }
        cultivos.stream()
            .filter(c -> cultivoId.equals(c.getId()))
            .findFirst()
            .ifPresent(c -> tablaCultivos.getSelectionModel().select(c));
    }

    private void seleccionarTratamientoEnTabla(Integer tratamientoId) {
        if (tratamientoId == null) {
            return;
        }
        tratamientos.stream()
            .filter(t -> tratamientoId.equals(t.getId()))
            .findFirst()
            .ifPresent(t -> tablaTratamientos.getSelectionModel().select(t));
    }

    private Optional<GestionFinanciera> mostrarDialogoGestion(GestionFinanciera gestion) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/vista/GestionFinancieraForm.fxml"));
            Parent root = loader.load();
            DialogoGestionFinancieraController controller = loader.getController();
            Stage dialogStage = crearDialogoStage(
                gestion == null ? "Nuevo registro financiero" : "Editar registro", root);
            controller.setStage(dialogStage);
            controller.setFincaId(fincaActual != null ? fincaActual.getId() : null);
            controller.setGestion(gestion);
            dialogStage.showAndWait();
            return Optional.ofNullable(controller.getGestionResultado());
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al abrir formulario",
                "No se pudo cargar el formulario financiero.\nDetalle: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Optional<Finca> mostrarDialogoFinca(Finca finca) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/vista/FincaForm.fxml"));
            Parent root = loader.load();
            DialogoFincaController controller = loader.getController();
            Stage dialogStage = crearDialogoStage("Editar finca", root);
            controller.setStage(dialogStage);
            controller.setPoligonoRequerido(false);

            Usuario propietario = finca.getPropietario();
            if (propietario == null) {
                propietario = Sesion.getInstancia().getUsuarioActual();
            }
            if (propietario != null) {
                controller.setUsuarios(List.of(propietario));
                controller.configurarPropietarioFijo(propietario);
            }

            controller.setFinca(finca);
            dialogStage.showAndWait();
            return Optional.ofNullable(controller.getFincaResultado());
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al abrir formulario",
                "No se pudo cargar el formulario de finca.\nDetalle: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private void mostrarDetalleFinanza(GestionFinanciera gestion) {
        if (gestion == null) {
            lblFinanzaConcepto.setText("-");
            lblFinanzaTipo.setText("-");
            lblFinanzaMonto.setText("-");
            lblFinanzaFecha.setText("-");
            areaFinanzaObservaciones.clear();
            return;
        }
        lblFinanzaConcepto.setText(gestion.getConcepto());
        lblFinanzaTipo.setText(gestion.getTipo().name());
        lblFinanzaMonto.setText(gestion.getMonto() == null ? "-" : gestion.getMonto() + " €");
        lblFinanzaFecha.setText(gestion.getFecha() == null ? "-" : gestion.getFecha().toString());
        areaFinanzaObservaciones.setText(gestion.getObservaciones());
    }

    private void seleccionarGestionEnTabla(Integer gestionId) {
        if (gestionId == null) {
            return;
        }
        finanzas.stream()
            .filter(g -> gestionId.equals(g.getId()))
            .findFirst()
            .ifPresent(g -> tablaFinanzas.getSelectionModel().select(g));
    }

    private void navegar(String ruta, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent root = loader.load();
            Stage stage = (Stage) lblNombreFinca.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(titulo);
            stage.centerOnScreen();
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de navegación",
                "No se pudo cargar la vista solicitada: " + e.getMessage());
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

    private void aplicarTabDestino() {
        if (tabPaneFinca == null) {
            return;
        }
        Sesion sesion = Sesion.getInstancia();
        String destino = sesion.getTabDestino();
        if (destino == null || destino.isBlank()) {
            return;
        }
        tabPaneFinca.getTabs().stream()
            .filter(tab -> tab.getText().equalsIgnoreCase(destino))
            .findFirst()
            .ifPresent(tab -> tabPaneFinca.getSelectionModel().select(tab));
        sesion.setTabDestino(null);
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

    private void actualizarEstiloBalance(Label etiqueta, double valor) {
        if (etiqueta == null) {
            return;
        }
        etiqueta.setText(String.format("%+.2f €", valor));
        if (valor < 0) {
            etiqueta.setStyle("-fx-text-fill: #c62828; -fx-font-size: 24px; -fx-font-weight: bold;");
        } else if (valor > 0) {
            etiqueta.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 24px; -fx-font-weight: bold;");
        } else {
            etiqueta.setStyle("-fx-text-fill: #37474f; -fx-font-size: 24px; -fx-font-weight: bold;");
        }
    }

}

