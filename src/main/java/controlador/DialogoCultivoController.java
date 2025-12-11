package controlador;

import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import modelo.Cultivo;
import modelo.Finca;
import util.ValidacionUtil;

public class DialogoCultivoController {

    @FXML
    private Label lblTitulo;
    @FXML
    private TextField campoNombre;
    @FXML
    private TextField campoVariedad;
    @FXML
    private ComboBox<Cultivo.Estado> comboEstado;
    @FXML
    private DatePicker pickerSiembra;
    @FXML
    private DatePicker pickerCosecha;
    @FXML
    private TextField campoProduccion;
    @FXML
    private TextField campoRendEstimado;
    @FXML
    private TextField campoRendReal;
    @FXML
    private Label lblError;

    private Stage stage;
    private Cultivo cultivoResultado;
    private Finca finca;

    @FXML
    private void initialize() {
        comboEstado.setItems(FXCollections.observableArrayList(Cultivo.Estado.values()));
        comboEstado.getSelectionModel().select(Cultivo.Estado.ACTIVO);
        pickerSiembra.setValue(LocalDate.now());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setFinca(Finca finca) {
        this.finca = finca;
    }

    public void setCultivo(Cultivo cultivo) {
        if (cultivo == null) {
            lblTitulo.setText("Nuevo cultivo");
            return;
        }
        lblTitulo.setText("Editar cultivo");
        cultivoResultado = new Cultivo();
        cultivoResultado.setId(cultivo.getId());
        cultivoResultado.setFincaId(cultivo.getFincaId());

        campoNombre.setText(cultivo.getNombre());
        campoVariedad.setText(cultivo.getVariedad());
        comboEstado.getSelectionModel().select(cultivo.getEstado());
        pickerSiembra.setValue(cultivo.getFechaSiembra());
        pickerCosecha.setValue(cultivo.getFechaCosecha());
        campoProduccion.setText(cultivo.getProduccionKg() != null ? cultivo.getProduccionKg().toPlainString() : "");
        campoRendEstimado.setText(cultivo.getRendimientoEstimado() != null
            ? cultivo.getRendimientoEstimado().toPlainString() : "");
        campoRendReal.setText(cultivo.getRendimientoReal() != null
            ? cultivo.getRendimientoReal().toPlainString() : "");
    }

    @FXML
    private void guardar() {
        if (!validar()) {
            return;
        }
        if (cultivoResultado == null) {
            cultivoResultado = new Cultivo();
        }
        cultivoResultado.setFincaId(finca != null ? finca.getId() : cultivoResultado.getFincaId());
        String nombre = campoNombre.getText() != null ? campoNombre.getText().trim() : "";
        if (nombre.isEmpty()) {
            mostrarError("El nombre del cultivo es obligatorio.");
            return;
        }
        cultivoResultado.setNombre(nombre);
        cultivoResultado.setVariedad(campoVariedad.getText() != null ? campoVariedad.getText().trim() : null);
        // Asegurar que el estado no sea null, usar ACTIVO por defecto
        Cultivo.Estado estado = comboEstado.getValue() != null ? comboEstado.getValue() : Cultivo.Estado.ACTIVO;
        cultivoResultado.setEstado(estado);
        cultivoResultado.setFechaSiembra(pickerSiembra.getValue());
        cultivoResultado.setFechaCosecha(pickerCosecha.getValue());
        try {
            cultivoResultado.setProduccionKg(parseBigDecimal(campoProduccion));
            cultivoResultado.setRendimientoEstimado(parseBigDecimal(campoRendEstimado));
            cultivoResultado.setRendimientoReal(parseBigDecimal(campoRendReal));
        } catch (NumberFormatException e) {
            return;
        }
        cerrar();
    }

    private boolean validar() {
        ocultarError();
        boolean fincaAsignada = finca != null || (cultivoResultado != null && cultivoResultado.getFincaId() != null);
        if (!fincaAsignada) {
            mostrarError("No se ha especificado la finca.");
            return false;
        }
        boolean nombreValido = ValidacionUtil.textoRequerido(campoNombre);
        boolean estadoValido = ValidacionUtil.comboRequerido(comboEstado);
        boolean fechaSiembraValida = ValidacionUtil.fechaRequerida(pickerSiembra);

        if (!(nombreValido && estadoValido && fechaSiembraValida)) {
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
            mostrarError("Los campos numÃ©ricos deben ser vÃ¡lidos (usa punto como separador decimal).");
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
        cultivoResultado = null;
        cerrar();
    }

    private void cerrar() {
        if (stage != null) {
            stage.close();
        }
    }

    public Cultivo getCultivoResultado() {
        return cultivoResultado;
    }
}



