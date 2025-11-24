package controlador;

import crud.ConexionBD;
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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import util.Sesion;

public class ControladorInformesAdmin implements Initializable {

    private Connection conexion;
    private Map<String, Object> parametros = new HashMap<>();
    private JasperPrint jasperPrintActual = null;
    private String tituloInformeActual = "";

    @FXML
    private WebView webView;
    @FXML
    private RadioButton radioUsuarios;
    @FXML
    private RadioButton radioFincas;
    @FXML
    private Button btnGenerarInforme;
    @FXML
    private Button btnDescargarPDF;
    @FXML
    private Button btnVolver;
    @FXML
    private VBox panelVacio;
    @FXML
    private ToggleGroup tipoInforme;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            conexion = ConexionBD.obtenerConexion();
        } catch (SQLException e) {
            mostrarAlerta("Error", "Error de conexión: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }

        tipoInforme = new ToggleGroup();
        radioUsuarios.setToggleGroup(tipoInforme);
        radioFincas.setToggleGroup(tipoInforme);
        radioUsuarios.setSelected(true);

        Platform.runLater(() -> {
            Stage primaryStage = (Stage) (btnVolver != null ? btnVolver.getScene().getWindow() : null);
            if (primaryStage != null) {
                primaryStage.setOnCloseRequest(event -> {
                    try {
                        if (conexion != null && !conexion.isClosed()) {
                            conexion.close();
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(ControladorInformesAdmin.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }
        });
    }

    @FXML
    public void generarInforme(ActionEvent event) throws IOException {
        parametros.clear();
        String rutaInforme;
        String tituloInforme;

        if (radioUsuarios.isSelected()) {
            rutaInforme = "/reports/InformeGeneral.jrxml";
            tituloInforme = "Informe de Usuarios";
        } else {
            rutaInforme = "/reports/InformeCompuesto2.jrxml";
            tituloInforme = "Informe de Fincas";
        }

        lanzarInforme(rutaInforme, parametros, true, tituloInforme);
    }

    @FXML
    private void lanzarInforme(String rutaInf, Map<String, Object> param, boolean incrustado, String titulo) {
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
                mostrarAlerta("Error", "No se pudo cargar el informe: " + rutaInf + "\nVerifica que el archivo existe en src/main/resources/reports/", Alert.AlertType.ERROR);
                return;
            }

            JasperReport report = JasperCompileManager.compileReport(reportStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, param, conexion);
            jasperPrintActual = jasperPrint; // Guardar para exportar a PDF
            tituloInformeActual = titulo; // Guardar título para el nombre del archivo

            if (!jasperPrint.getPages().isEmpty()) {
                String outputHtmlFile = "informeAdmin.html";
                JasperExportManager.exportReportToHtmlFile(jasperPrint, outputHtmlFile);

                if (incrustado) {
                    if (panelVacio != null) {
                        panelVacio.setVisible(false);
                    }
                    webView.getEngine().load(new File(outputHtmlFile).toURI().toString());
                } else {
                    mostrarInformeEnNuevaVentana(outputHtmlFile, titulo);
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Información");
                alert.setHeaderText("Alerta de Informe");
                alert.setContentText("El informe no generó páginas");
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

    @FXML
    private void descargarPDF(ActionEvent event) {
        if (jasperPrintActual == null) {
            mostrarAlerta("Aviso", "Primero debes generar el informe antes de descargarlo en PDF", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar informe como PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        
        String nombreArchivo = tituloInformeActual.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
        fileChooser.setInitialFileName(nombreArchivo);

        Stage stage = (Stage) btnDescargarPDF.getScene().getWindow();
        File archivo = fileChooser.showSaveDialog(stage);

        if (archivo != null) {
            try {
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
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/main/vista/AdminPanel.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) btnVolver.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Panel de administración");
            stage.centerOnScreen();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo volver al panel de administración: " + e.getMessage(), Alert.AlertType.ERROR);
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


