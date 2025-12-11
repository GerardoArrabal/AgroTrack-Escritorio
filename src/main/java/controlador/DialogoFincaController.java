package controlador;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import modelo.Finca;
import modelo.Usuario;
import netscape.javascript.JSObject;
import util.ValidacionUtil;

public class DialogoFincaController {

    @FXML
    private Label lblTitulo;
    @FXML
    private ComboBox<Usuario> comboPropietario;
    @FXML
    private TextField campoNombre;
    @FXML
    private TextField campoUbicacion;
    @FXML
    private TextField campoSuperficie;
    @FXML
    private ComboBox<String> comboTipoSuelo;
    @FXML
    private TextField campoSistemaRiego;
    @FXML
    private ComboBox<Finca.Estado> comboEstado;
    @FXML
    private DatePicker pickerFechaRegistro;
    @FXML
    private Label lblError;
    @FXML
    private StackPane panelMapa;
    @FXML
    private WebView webViewMapa;
    @FXML
    private ProgressIndicator loadingMapa;

    private WebEngine webEngine;
    private String coordenadasPoligono = "";
    private boolean mapaListo = false;
    private boolean poligonoRequerido = false;
    private boolean listenerAñadido = false; // Flag para evitar añadir múltiples listeners
    private boolean mapaCargado = false; // Flag para evitar cargar el mapa múltiples veces

    private Stage stage;
    private Finca fincaResultado;

    @FXML
    private void initialize() {
        comboEstado.setItems(FXCollections.observableArrayList(Finca.Estado.values()));
        comboEstado.getSelectionModel().select(Finca.Estado.ACTIVA);
        comboTipoSuelo.setItems(FXCollections.observableArrayList("Arcilloso", "Arenoso", "Franco", "Calcáreo", "Limoso"));
        comboTipoSuelo.getSelectionModel().selectFirst();
        pickerFechaRegistro.setValue(LocalDate.now());

        inicializarMapa();
    }

    /**
     * INICIALIZACIÓN DEL MAPA INTERACTIVO
     * 
     * Este método carga el archivo HTML del mapa (mapa.html) en un WebView de JavaFX
     * y establece la comunicación bidireccional entre Java y JavaScript.
     * 
     * ¿Cómo funciona la comunicación?
     * 1. Java carga el HTML en el WebView
     * 2. Java expone este controlador a JavaScript: window.javaController = this
     * 3. JavaScript puede llamar métodos Java: window.javaController.guardarCoordenadasDesdeMapa()
     * 4. Java puede llamar funciones JavaScript: webEngine.executeScript("inicializarMapa()")
     * 
     * IMPORTANTE: Hay mucha lógica de "espera" porque:
     * - El HTML se carga de forma asíncrona
     * - Leaflet.js dentro del HTML también se carga de forma asíncrona
     * - Si intentas usar el mapa antes de que esté listo, falla
     */
    private void inicializarMapa() {
        if (webViewMapa == null) {
            return;
        }
        
        // Si ya se cargó el mapa, no hacer nada (evitar cargar múltiples veces)
        if (webEngine != null && webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            return;
        }
        
        webEngine = webViewMapa.getEngine();
        webViewMapa.managedProperty().bind(webViewMapa.visibleProperty());

        if (loadingMapa != null) {
            loadingMapa.managedProperty().bind(loadingMapa.visibleProperty());
            loadingMapa.setVisible(false);
        }

        // Agregar listener solo una vez (evitar múltiples listeners)
        if (!listenerAñadido) {
            // Escuchar cuando el HTML termine de cargar
            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    mapaListo = true;
                    mostrarIndicadorCarga(false);
                    try {
                        // PASO CRÍTICO: Exponer este controlador Java a JavaScript
                        // Ahora JavaScript puede llamar: window.javaController.guardarCoordenadasDesdeMapa()
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        window.setMember("javaController", this);
                        
                        // Llamar a JavaScript para inicializar el mapa
                        // Usamos setTimeout porque Leaflet.js puede tardar en cargar
                        // Este código JavaScript se ejecuta dentro del WebView
                        webEngine.executeScript(
                            "setTimeout(function() { " +
                            "  console.log('Verificando Leaflet...'); " +
                            "  console.log('L existe: ' + (typeof L !== 'undefined')); " +
                            "  console.log('L.map existe: ' + (typeof L !== 'undefined' && typeof L.map !== 'undefined')); " +
                            "  if (typeof inicializarMapaDesdeJava === 'function') { " +
                            "    console.log('Llamando inicializarMapaDesdeJava...'); " +
                            "    inicializarMapaDesdeJava(); " +
                            "  } else if (typeof inicializarMapa === 'function') { " +
                            "    console.log('Llamando inicializarMapa...'); " +
                            "    inicializarMapa(); " +
                            "  } else { " +
                            "    console.error('Funciones de inicialización no encontradas'); " +
                            "  } " +
                            "}, 500);"
                        );
                        
                        // Si estamos editando una finca existente, cargar sus coordenadas en el mapa
                        if (!coordenadasPoligono.isBlank()) {
                            // Esperar más tiempo (1500ms) para asegurar que el mapa esté completamente listo
                            // IMPORTANTE: Escapamos las comillas y barras para evitar errores en JavaScript
                            webEngine.executeScript(
                                "setTimeout(function() { " +
                                "  if (typeof cargarCoordenadasDesdeJava === 'function') { " +
                                "    cargarCoordenadasDesdeJava('" + coordenadasPoligono.replace("\\", "\\\\").replace("'", "\\'") + "'); " +
                                "  } " +
                                "}, 1500);"
                            );
                        }
                    } catch (Exception e) {
                        System.err.println("No se pudo inicializar el mapa: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (newValue == Worker.State.RUNNING) {
                    mapaListo = false;
                    mostrarIndicadorCarga(true);
                }
            });
            listenerAñadido = true;
        }

        // Cargar el HTML solo si no está cargado o está en estado inicial
        if (!mapaCargado) {
            Worker.State estado = webEngine.getLoadWorker().getState();
            if (estado == Worker.State.READY || estado == Worker.State.FAILED || estado == Worker.State.CANCELLED) {
                try {
                    // Usar el mapa simple mejorado que muestra tiles reales
                    System.out.println("Cargando mapa simple mejorado...");
                    String html = leerRecursoComoString("/mapa/mapa_simple.html");
                    if (html != null && !html.isEmpty()) {
                        System.out.println("Mapa simple leído: " + html.length() + " caracteres");
                        webEngine.loadContent(html);
                        mapaCargado = true;
                        System.out.println("Mapa cargado correctamente");
                    } else {
                        throw new Exception("No se pudo leer el archivo del mapa");
                    }
                } catch (Exception e) {
                    System.err.println("Error al cargar mapa: " + e.getMessage());
                    e.printStackTrace();
                    mostrarAlerta(Alert.AlertType.WARNING, "Error de mapa",
                        "No se pudo cargar el mapa. Verifique la consola para más detalles.");
                }
            }
        }
    }

    private void mostrarIndicadorCarga(boolean mostrar) {
        if (loadingMapa != null) {
            loadingMapa.setVisible(mostrar);
        }
    }

    private void refrescarMapaWeb() {
        if (webEngine == null || !mapaListo) {
            return;
        }
        try {
            webEngine.executeScript("window.refrescarMapaDesdeJava && window.refrescarMapaDesdeJava();");
        } catch (Exception e) {
            System.err.println("No se pudo refrescar el mapa: " + e.getMessage());
        }
    }

    private void cargarCoordenadasEnMapa() {
        if (!mapaListo || webEngine == null || coordenadasPoligono == null || coordenadasPoligono.isBlank()) {
            return;
        }
        try {
            String datos = coordenadasPoligono.replace("\\", "\\\\").replace("'", "\\'");
            webEngine.executeScript("window.cargarCoordenadasDesdeJava('" + datos + "');");
            refrescarMapaWeb();
        } catch (Exception e) {
            System.err.println("Error al cargar coordenadas en el mapa: " + e.getMessage());
        }
    }

    /**
     * MÉTODO LLAMADO DESDE JAVASCRIPT
     * 
     * Este método es llamado desde el mapa HTML cuando el usuario hace clic en
     * "Guardar coordenadas". JavaScript ejecuta: window.javaController.guardarCoordenadasDesdeMapa(json)
     * 
     * @param coordenadasJson String JSON con las coordenadas del polígono
     *                        Formato: "[[lat1, lng1], [lat2, lng2], ...]"
     * 
     * IMPORTANTE: Este método solo guarda las coordenadas en memoria (variable coordenadasPoligono).
     * Para guardarlas en la base de datos, el usuario debe hacer clic en "Guardar" del formulario.
     */
    public void guardarCoordenadasDesdeMapa(String coordenadasJson) {
        this.coordenadasPoligono = coordenadasJson != null ? coordenadasJson : "";
        mostrarAlerta(Alert.AlertType.INFORMATION, "Coordenadas guardadas",
            "El polígono se guardó en el formulario. Recuerda guardar la finca.");
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

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setUsuarios(List<Usuario> usuarios) {
        comboPropietario.setDisable(false);
        comboPropietario.setItems(FXCollections.observableArrayList(usuarios));
    }

    public void configurarPropietarioFijo(Usuario usuario) {
        if (usuario == null) {
            return;
        }
        comboPropietario.setItems(FXCollections.observableArrayList(usuario));
        comboPropietario.getSelectionModel().select(usuario);
        comboPropietario.setDisable(true);
    }

    public void setPoligonoRequerido(boolean requerido) {
        this.poligonoRequerido = requerido;
    }

    public void setFinca(Finca finca) {
        if (finca == null) {
            lblTitulo.setText("Nueva finca");
            fincaResultado = null;
            coordenadasPoligono = "";
            return;
        }
        lblTitulo.setText("Editar finca");
        fincaResultado = new Finca();
        fincaResultado.setId(finca.getId());

        campoNombre.setText(finca.getNombre());
        campoUbicacion.setText(finca.getUbicacion());
        campoSuperficie.setText(finca.getSuperficie() != null ? finca.getSuperficie().toPlainString() : "");
        comboTipoSuelo.getSelectionModel().select(finca.getTipoSuelo());
        campoSistemaRiego.setText(finca.getSistemaRiego());
        comboEstado.getSelectionModel().select(finca.getEstado());
        pickerFechaRegistro.setValue(finca.getFechaRegistro());
        coordenadasPoligono = finca.getCoordenadasPoligono() != null ? finca.getCoordenadasPoligono() : "";
        if (finca.getPropietario() != null) {
            comboPropietario.getSelectionModel().select(finca.getPropietario());
        }
        if (mapaListo) {
            cargarCoordenadasEnMapa();
        }
    }

    @FXML
    private void guardar() {
        if (!validar()) {
            return;
        }
        BigDecimal superficie;
        try {
            superficie = parseBigDecimal(campoSuperficie.getText());
        } catch (NumberFormatException e) {
            return;
        }
        if (fincaResultado == null) {
            fincaResultado = new Finca();
        }
        Usuario propietario = comboPropietario.getValue();
        if (propietario == null || propietario.getId() == null) {
            mostrarError("Debes seleccionar un propietario para la finca.");
            return;
        }
        fincaResultado.setUsuarioId(propietario.getId());
        fincaResultado.setPropietario(propietario);
        String nombre = campoNombre.getText() != null ? campoNombre.getText().trim() : "";
        if (nombre.isEmpty()) {
            mostrarError("El nombre de la finca es obligatorio.");
            return;
        }
        fincaResultado.setNombre(nombre);
        fincaResultado.setUbicacion(campoUbicacion.getText().trim());
        fincaResultado.setSuperficie(superficie);
        fincaResultado.setTipoSuelo(comboTipoSuelo.getValue());
        fincaResultado.setSistemaRiego(campoSistemaRiego.getText().trim());
        fincaResultado.setEstado(comboEstado.getValue());
        fincaResultado.setFechaRegistro(
            pickerFechaRegistro.getValue() != null ? pickerFechaRegistro.getValue() : LocalDate.now());
        fincaResultado.setCoordenadasPoligono(coordenadasPoligono);
        cerrar();
    }

    private boolean validar() {
        ocultarError();
        boolean propietarioValido = ValidacionUtil.comboRequerido(comboPropietario);
        boolean nombreValido = ValidacionUtil.textoRequerido(campoNombre);
        boolean ubicacionValida = ValidacionUtil.textoRequerido(campoUbicacion);
        boolean superficieValida = ValidacionUtil.textoRequerido(campoSuperficie);
        boolean tipoSueloValido = ValidacionUtil.comboRequerido(comboTipoSuelo);
        boolean estadoValido = ValidacionUtil.comboRequerido(comboEstado);
        boolean fechaValida = ValidacionUtil.fechaRequerida(pickerFechaRegistro);

        if (!(propietarioValido && nombreValido && ubicacionValida && superficieValida
            && tipoSueloValido && estadoValido && fechaValida)) {
            mostrarError("Completa los campos marcados en rojo.");
            return false;
        }
        if (poligonoRequerido && (coordenadasPoligono == null || coordenadasPoligono.isBlank())) {
            mostrarError("Dibuja y guarda el polígono de la finca.");
            return false;
        }
        return true;
    }

    private BigDecimal parseBigDecimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            BigDecimal resultado = new BigDecimal(valor.trim());
            ValidacionUtil.marcar(campoSuperficie, true);
            return resultado;
        } catch (NumberFormatException e) {
            ValidacionUtil.marcar(campoSuperficie, false);
            mostrarError("La superficie debe ser un número válido.");
            throw e;
        }
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
        fincaResultado = null;
        cerrar();
    }

    private void cerrar() {
        if (stage != null) {
            stage.close();
        }
    }

    public Finca getFincaResultado() {
        return fincaResultado;
    }
    
    /**
     * Lee un archivo de recursos (HTML, CSS, JS) y lo devuelve como String.
     * 
     * ¿Por qué leer el HTML como String en lugar de cargarlo directamente?
     * - Permite modificar el contenido antes de cargarlo (inyectar CSS/JS si es necesario)
     * - Más control sobre el proceso de carga
     * - Útil para debugging (puedes ver el HTML completo antes de cargarlo)
     * 
     * @param ruta Ruta del recurso dentro de src/main/resources (ej: "/mapa/mapa.html")
     * @return El contenido completo del archivo como String
     * @throws Exception Si el archivo no existe o no se puede leer
     */
    private String leerRecursoComoString(String ruta) throws Exception {
        // getResourceAsStream busca el archivo en src/main/resources
        try (InputStream is = getClass().getResourceAsStream(ruta)) {
            if (is == null) {
                throw new Exception("No se encontró el recurso: " + ruta);
            }
            // Leer el archivo línea por línea y unirlo con saltos de línea
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}

