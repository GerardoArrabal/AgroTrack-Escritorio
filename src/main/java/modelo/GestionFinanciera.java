package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class GestionFinanciera {

    public enum Tipo {
        INGRESO,
        GASTO
    }

    private Integer id;
    private Integer fincaId;
    private Tipo tipo;
    private String concepto;
    private BigDecimal monto;
    private LocalDate fecha;
    private String observaciones;

    private Finca finca;

    public GestionFinanciera() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFincaId() {
        return fincaId;
    }

    public void setFincaId(Integer fincaId) {
        this.fincaId = fincaId;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Finca getFinca() {
        return finca;
    }

    public void setFinca(Finca finca) {
        this.finca = finca;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GestionFinanciera that = (GestionFinanciera) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return tipo + " - " + concepto;
    }
}

