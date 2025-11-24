package util;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextInputControl;

/**
 * Utilidades simples para validar campos obligatorios y resaltar en rojo
 * aquellos que falten por completar.
 */
public final class ValidacionUtil {

    private static final String ERROR_STYLE = "-fx-border-color: #c62828; -fx-border-width: 2; "
        + "-fx-border-radius: 6; -fx-focus-color: #c62828;";

    private ValidacionUtil() {
    }

    public static boolean textoRequerido(TextInputControl control) {
        boolean valido = control != null && control.getText() != null && !control.getText().trim().isEmpty();
        marcar(control, valido);
        return valido;
    }

    public static boolean comboRequerido(ComboBox<?> combo) {
        boolean valido = combo != null && combo.getValue() != null;
        marcar(combo, valido);
        return valido;
    }

    public static boolean fechaRequerida(DatePicker picker) {
        boolean valido = picker != null && picker.getValue() != null;
        marcar(picker, valido);
        return valido;
    }

    public static boolean checkRequerido(CheckBox check) {
        boolean valido = check != null && check.isSelected();
        if (check != null) {
            check.setStyle(valido ? "" : "-fx-text-fill: #c62828; -fx-border-color: #c62828; "
                + "-fx-border-width: 1; -fx-border-radius: 4; -fx-padding: 2;");
        }
        return valido;
    }

    public static void marcar(Control control, boolean valido) {
        if (control == null) {
            return;
        }
        control.setStyle(valido ? "" : ERROR_STYLE);
    }
}
