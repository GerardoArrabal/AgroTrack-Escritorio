package controlador;

import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import modelo.GestionFinanciera;
import util.ValidacionUtil;

public class DialogoGestionFinancieraController {

    @FXML
    private Label lblTitulo;
    @FXML
    private ComboBox<GestionFinanciera.Tipo> comboTipo;
    @FXML
    private TextField campoConcepto;
    @FXML
    private TextField campoMonto;
    @FXML
    private DatePicker pickerFecha;
    @FXML
    private TextArea areaObservaciones;
    @FXML
    private Label lblError;

    private Stage stage;
    private GestionFinanciera gestionResultado;
    private Integer fincaId;

    @FXML
    private void initialize() {
        comboTipo.setItems(FXCollections.observableArrayList(GestionFinanciera.Tipo.values()));
        comboTipo.getSelectionModel().select(GestionFinanciera.Tipo.INGRESO);
        pickerFecha.setValue(LocalDate.now());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setFincaId(Integer fincaId) {
        this.fincaId = fincaId;
    }

    public void setGestion(GestionFinanciera gestion) {
        if (gestion == null) {
            lblTitulo.setText("Nuevo registro financiero");
            return;
        }
        lblTitulo.setText("Editar registro");
        gestionResultado = new GestionFinanciera();
        gestionResultado.setId(gestion.getId());
        gestionResultado.setFincaId(gestion.getFincaId());

        comboTipo.getSelectionModel().select(gestion.getTipo());
        campoConcepto.setText(gestion.getConcepto());
        campoMonto.setText(gestion.getMonto() != null ? gestion.getMonto().toPlainString() : "");
        pickerFecha.setValue(gestion.getFecha());
        areaObservaciones.setText(gestion.getObservaciones());
    }

    @FXML
    private void guardar() {
        if (!validar()) {
            return;
        }
        if (gestionResultado == null) {
            gestionResultado = new GestionFinanciera();
        }
        gestionResultado.setFincaId(fincaId != null ? fincaId : gestionResultado.getFincaId());
        gestionResultado.setTipo(comboTipo.getValue());
        gestionResultado.setConcepto(campoConcepto.getText().trim());
        try {
            gestionResultado.setMonto(parseBigDecimal(campoMonto));
        } catch (NumberFormatException ex) {
            return;
        }
        gestionResultado.setFecha(pickerFecha.getValue());
        gestionResultado.setObservaciones(areaObservaciones.getText().trim());
        cerrar();
    }

    private boolean validar() {
        ocultarError();
        if ((fincaId == null) && (gestionResultado == null || gestionResultado.getFincaId() == null)) {
            mostrarError("No se ha especificado la finca del movimiento.");
            return false;
        }
        boolean tipoValido = ValidacionUtil.comboRequerido(comboTipo);
        boolean conceptoValido = ValidacionUtil.textoRequerido(campoConcepto);
        boolean montoValido = ValidacionUtil.textoRequerido(campoMonto);
        boolean fechaValida = ValidacionUtil.fechaRequerida(pickerFecha);

        if (!(tipoValido && conceptoValido && montoValido && fechaValida)) {
            mostrarError("Completa los campos obligatorios resaltados en rojo.");
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
            ValidacionUtil.marcar(campo, false);
            mostrarError("El monto es obligatorio.");
            throw new NumberFormatException("Monto vacÃ­o");
        }
        try {
            BigDecimal numero = new BigDecimal(value.trim());
            ValidacionUtil.marcar(campo, true);
            return numero;
        } catch (NumberFormatException e) {
            ValidacionUtil.marcar(campo, false);
            mostrarError("El monto debe ser un nÃºmero vÃ¡lido (usa punto como separador decimal).");
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
        gestionResultado = null;
        cerrar();
    }

    private void cerrar() {
        if (stage != null) {
            stage.close();
        }
    }

    public GestionFinanciera getGestionResultado() {
        return gestionResultado;
    }
}



