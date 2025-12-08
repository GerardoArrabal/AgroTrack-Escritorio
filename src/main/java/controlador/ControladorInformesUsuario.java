package controlador;

import crud.ConexionBD;
import crud.FincaDAO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import modelo.Finca;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import util.Sesion;

public class ControladorInformesUsuario implements Initializable {

    private Connection conexion;
    private final FincaDAO fincaDAO = new FincaDAO();
    private Map<String, Object> parametros = new HashMap<>();
    private ObservableList<Finca> listaFincas = FXCollections.observableArrayList();
    private JasperPrint jasperPrintActual = null;

    @FXML
    private WebView webView;
    @FXML
    private ComboBox<Finca> comboFincas;
    @FXML
    private Button btnGenerarInforme;
    @FXML
    private Button btnDescargarPDF;
    @FXML
    private Button btnVolver;
    @FXML
    private VBox panelVacio;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            conexion = ConexionBD.obtenerConexion();
        } catch (SQLException e) {
            mostrarAlerta("Error", "Error de conexión: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }

        cargarFincasUsuario();
        inicializarComboFincas();

        Platform.runLater(() -> {
            Stage primaryStage = (Stage) (comboFincas != null ? comboFincas.getScene().getWindow() : null);
            if (primaryStage != null) {
                primaryStage.setOnCloseRequest(event -> {
                    try {
                        if (conexion != null && !conexion.isClosed()) {
                            conexion.close();
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(ControladorInformesUsuario.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }
        });
    }

    private void cargarFincasUsuario() {
        listaFincas.clear();
        if (Sesion.getInstancia().estaAutenticado()) {
            try {
                listaFincas.setAll(fincaDAO.listarPorUsuario(Sesion.getInstancia().getUsuarioActual().getId()));
            } catch (SQLException e) {
                mostrarAlerta("Error", "No se pudieron cargar las fincas: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    private void inicializarComboFincas() {
        comboFincas.setItems(listaFincas);
        comboFincas.setCellFactory(param -> new javafx.scene.control.ListCell<Finca>() {
            @Override
            protected void updateItem(Finca finca, boolean empty) {
                super.updateItem(finca, empty);
                if (empty || finca == null) {
                    setText(null);
                } else {
                    setText(finca.getNombre());
                }
            }
        });
        comboFincas.setButtonCell(new javafx.scene.control.ListCell<Finca>() {
            @Override
            protected void updateItem(Finca finca, boolean empty) {
                super.updateItem(finca, empty);
                if (empty || finca == null) {
                    setText(null);
                } else {
                    setText(finca.getNombre());
                }
            }
        });
    }

    @FXML
    public void generarInforme(ActionEvent event) throws IOException {
        Finca fincaSeleccionada = comboFincas.getValue();
        if (fincaSeleccionada == null) {
            mostrarAlerta("Aviso", "Debe seleccionar una finca para generar el informe", Alert.AlertType.WARNING);
            return;
        }

        parametros.clear();
        parametros.put("ParametroFincaId", fincaSeleccionada.getId());
        String rutaInforme = "/reports/InformeFiltra.jrxml";
        lanzarInforme(rutaInforme, parametros, true);
    }

    /**
     * GENERACIÓN DE INFORMES CON JASPERREPORTS
     * 
     * Este método genera informes usando la librería JasperReports, que permite:
     * 1. Diseñar informes visualmente (archivos .jrxml)
     * 2. Llenar el informe con datos de la base de datos
     * 3. Exportar a HTML (para mostrar en pantalla) o PDF (para descargar)
     * 
     * Proceso:
     * 1. Carga el archivo .jrxml (plantilla del informe)
     * 2. Lo compila (JasperCompileManager)
     * 3. Lo llena con datos de la BD usando parámetros (JasperFillManager)
     * 4. Lo exporta a HTML o PDF (JasperExportManager)
     * 
     * @param rutaInf Ruta del archivo .jrxml en src/main/resources/reports/
     * @param param Parámetros para el informe (ej: ID de finca)
     * @param incrustado Si true, muestra en el WebView; si false, abre nueva ventana
     */
    @FXML
    private void lanzarInforme(String rutaInf, Map<String, Object> param, boolean incrustado) {
        try {
            // Intentar diferentes rutas posibles (por si hay problemas con el classpath)
            InputStream reportStream = getClass().getResourceAsStream(rutaInf);
            if (reportStream == null) {
                // Intentar sin el slash inicial
                String rutaAlternativa = rutaInf.startsWith("/") ? rutaInf.substring(1) : "/" + rutaInf;
                reportStream = getClass().getResourceAsStream(rutaAlternativa);
            }
            if (reportStream == null) {
                // Intentar desde el classloader directamente
                reportStream = getClass().getClassLoader().getResourceAsStream(rutaInf.startsWith("/") ? rutaInf.substring(1) : rutaInf);
            }
            if (reportStream == null) {
                mostrarAlerta("Error", "No se pudo cargar el informe: " + rutaInf + "\nVerifica que el archivo existe en src/main/resources/reports/", Alert.AlertType.ERROR);
                return;
            }

            // PASO 1: Compilar el archivo .jrxml (convertirlo a formato binario)
            JasperReport report = JasperCompileManager.compileReport(reportStream);
            
            // PASO 2: Llenar el informe con datos de la base de datos
            // - report: La plantilla compilada
            // - param: Parámetros (ej: {"ParametroFincaId": 5})
            // - conexion: Conexión a MySQL para ejecutar las consultas SQL del informe
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, param, conexion);
            jasperPrintActual = jasperPrint; // Guardar para poder exportar a PDF después

            // PASO 3: Exportar el informe a HTML
            if (!jasperPrint.getPages().isEmpty()) {
                String outputHtmlFile = "informeFinca.html";
                // Exporta el informe a un archivo HTML temporal
                JasperExportManager.exportReportToHtmlFile(jasperPrint, outputHtmlFile);

                if (incrustado) {
                    // Mostrar el informe en el WebView de la misma ventana
                    if (panelVacio != null) {
                        panelVacio.setVisible(false);
                    }
                    // Cargar el HTML generado en el WebView
                    webView.getEngine().load(new File(outputHtmlFile).toURI().toString());
                } else {
                    // Abrir el informe en una nueva ventana
                    mostrarInformeEnNuevaVentana(outputHtmlFile, "Informe de Finca");
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Información");
                alert.setHeaderText("Alerta de Informe");
                alert.setContentText("La finca seleccionada no generó páginas en el informe");
                alert.showAndWait();
            }
        } catch (JRException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al generar el informe");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    private void mostrarInformeEnNuevaVentana(String archivoHTML, String titulo) {
        Stage stage = new Stage();
        stage.setTitle(titulo);
        WebView webView = new WebView();
        webView.getEngine().load(new File(archivoHTML).toURI().toString());
        StackPane stackPane = new StackPane(webView);
        Scene scene = new Scene(stackPane, 800, 600);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        try {
            Image icon = new Image(getClass().getResourceAsStream("/images/icono.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            // Si no hay icono, continuar sin él
        }
        stage.showAndWait();
    }

    /**
     * EXPORTACIÓN DEL INFORME A PDF
     * 
     * Permite al usuario descargar el informe generado como archivo PDF.
     * Usa el mismo JasperPrint que se generó al crear el informe en pantalla,
     * así que no necesita volver a consultar la base de datos.
     * 
     * Proceso:
     * 1. Verifica que haya un informe generado (jasperPrintActual)
     * 2. Abre un diálogo para que el usuario elija dónde guardar el PDF
     * 3. Exporta el informe a PDF usando JasperExportManager
     */
    @FXML
    private void descargarPDF(ActionEvent event) {
        if (jasperPrintActual == null) {
            mostrarAlerta("Aviso", "Primero debes generar el informe antes de descargarlo en PDF", Alert.AlertType.WARNING);
            return;
        }

        // Abrir diálogo para elegir dónde guardar el PDF
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar informe como PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        
        // Generar nombre de archivo sugerido basado en el nombre de la finca
        Finca fincaSeleccionada = comboFincas.getValue();
        String nombreArchivo = fincaSeleccionada != null 
            ? "Informe_Finca_" + fincaSeleccionada.getNombre().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf"
            : "Informe_Finca.pdf";
        fileChooser.setInitialFileName(nombreArchivo);

        Stage stage = (Stage) btnDescargarPDF.getScene().getWindow();
        File archivo = fileChooser.showSaveDialog(stage);

        if (archivo != null) {
            try {
                // Exportar el informe a PDF (mismo proceso que HTML, pero formato diferente)
                JasperExportManager.exportReportToPdfFile(jasperPrintActual, archivo.getAbsolutePath());
                mostrarAlerta("Éxito", "El informe se ha descargado correctamente en:\n" + archivo.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (JRException e) {
                mostrarAlerta("Error", "No se pudo exportar el informe a PDF:\n" + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void volver(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/main/vista/Dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) btnVolver.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgroTrack - Dashboard");
            stage.centerOnScreen();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo volver al dashboard: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        try {
            Stage stage = (Stage) alerta.getDialogPane().getScene().getWindow();
            Image icon = new Image(getClass().getResourceAsStream("/images/icono.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            // Si no hay icono, continuar sin él
        }
        alerta.showAndWait();
    }
}


