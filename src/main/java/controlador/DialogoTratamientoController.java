package controlador;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import modelo.Cultivo;
import modelo.Tratamiento;
import util.ValidacionUtil;

public class DialogoTratamientoController {

    @FXML
    private Label lblTitulo;
    @FXML
    private ComboBox<Cultivo> comboCultivo;
    @FXML
    private DatePicker pickerFecha;
    @FXML
    private TextField campoProducto;
    @FXML
    private ComboBox<Tratamiento.Tipo> comboTipo;
    @FXML
    private TextField campoDosis;
    @FXML
    private TextField campoCosto;
    @FXML
    private TextArea areaObservaciones;
    @FXML
    private Label lblError;

    private Stage stage;
    private Tratamiento tratamientoResultado;
    private Cultivo cultivoPreseleccionado;

    @FXML
    private void initialize() {
        comboTipo.setItems(FXCollections.observableArrayList(Tratamiento.Tipo.values()));
        comboTipo.getSelectionModel().select(Tratamiento.Tipo.OTRO);
        pickerFecha.setValue(LocalDate.now());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setCultivos(List<Cultivo> cultivos) {
        comboCultivo.setItems(FXCollections.observableArrayList(cultivos));
        seleccionarCultivoEnCombo();
    }

    public void seleccionarCultivo(Cultivo cultivo) {
        this.cultivoPreseleccionado = cultivo;
        seleccionarCultivoEnCombo();
    }

    private void seleccionarCultivoEnCombo() {
        if (comboCultivo.getItems() == null || cultivoPreseleccionado == null) {
            return;
        }
        if (comboCultivo.getItems().contains(cultivoPreseleccionado)) {
            comboCultivo.getSelectionModel().select(cultivoPreseleccionado);
        }
    }

    public void setTratamiento(Tratamiento tratamiento) {
        if (tratamiento == null) {
            lblTitulo.setText("Nuevo tratamiento");
            return;
        }
        lblTitulo.setText("Editar tratamiento");
        tratamientoResultado = new Tratamiento();
        tratamientoResultado.setId(tratamiento.getId());
        tratamientoResultado.setCultivoId(tratamiento.getCultivoId());

        pickerFecha.setValue(tratamiento.getFecha());
        campoProducto.setText(tratamiento.getProducto());
        comboTipo.getSelectionModel().select(tratamiento.getTipo());
        campoDosis.setText(tratamiento.getDosis());
        campoCosto.setText(tratamiento.getPrecioTratamiento() != null
            ? tratamiento.getPrecioTratamiento().toPlainString() : "");
        areaObservaciones.setText(tratamiento.getObservaciones());

        this.cultivoPreseleccionado = tratamiento.getCultivo();
        seleccionarCultivoEnCombo();
    }

    @FXML
    private void guardar() {
        if (!validar()) {
            return;
        }
        if (tratamientoResultado == null) {
            tratamientoResultado = new Tratamiento();
        }
        Cultivo cultivoSeleccionado = comboCultivo.getValue();
        tratamientoResultado.setCultivoId(cultivoSeleccionado.getId());
        tratamientoResultado.setCultivo(cultivoSeleccionado);
        tratamientoResultado.setFecha(pickerFecha.getValue());
        tratamientoResultado.setProducto(campoProducto.getText().trim());
        tratamientoResultado.setTipo(comboTipo.getValue());
        tratamientoResultado.setDosis(campoDosis.getText().trim());
        try {
            tratamientoResultado.setPrecioTratamiento(parseBigDecimal(campoCosto));
        } catch (NumberFormatException ex) {
            return;
        }
        tratamientoResultado.setObservaciones(areaObservaciones.getText().trim());
        cerrar();
    }

    private boolean validar() {
        ocultarError();
        boolean cultivoValido = ValidacionUtil.comboRequerido(comboCultivo);
        boolean fechaValida = ValidacionUtil.fechaRequerida(pickerFecha);
        boolean productoValido = ValidacionUtil.textoRequerido(campoProducto);
        boolean tipoValido = ValidacionUtil.comboRequerido(comboTipo);
        boolean dosisValida = ValidacionUtil.textoRequerido(campoDosis);

        if (!(cultivoValido && fechaValida && productoValido && tipoValido && dosisValida)) {
            mostrarError("Revisa los campos resaltados en rojo.");
            return false;
        }
        return true;
    }

    private BigDecimal parseBigDecimal(TextField campo) {
        if (campo == null) {
            return null;
        }
        String value = campo.getText();
        if (value == null || value.isBlank()) {
            ValidacionUtil.marcar(campo, true);
            return null;
        }
        try {
            BigDecimal numero = new BigDecimal(value.trim());
            ValidacionUtil.marcar(campo, true);
            return numero;
        } catch (NumberFormatException e) {
            ValidacionUtil.marcar(campo, false);
            mostrarError("El costo debe ser un nÃºmero vÃ¡lido (usa punto como separador decimal).");
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
        tratamientoResultado = null;
        cerrar();
    }

    private void cerrar() {
        if (stage != null) {
            stage.close();
        }
    }

    public Tratamiento getTratamientoResultado() {
        return tratamientoResultado;
    }
}



