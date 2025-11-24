package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Tratamiento {

    public enum Tipo {
        FERTILIZANTE,
        HERBICIDA,
        FUNGICIDA,
        OTRO
    }

    private Integer id;
    private Integer cultivoId;
    private LocalDate fecha;
    private String producto;
    private Tipo tipo;
    private String dosis;
    private BigDecimal precioTratamiento;
    private String observaciones;

    private Cultivo cultivo;

    public Tratamiento() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCultivoId() {
        return cultivoId;
    }

    public void setCultivoId(Integer cultivoId) {
        this.cultivoId = cultivoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public String getDosis() {
        return dosis;
    }

    public void setDosis(String dosis) {
        this.dosis = dosis;
    }

    public BigDecimal getPrecioTratamiento() {
        return precioTratamiento;
    }

    public void setPrecioTratamiento(BigDecimal precioTratamiento) {
        this.precioTratamiento = precioTratamiento;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Cultivo getCultivo() {
        return cultivo;
    }

    public void setCultivo(Cultivo cultivo) {
        this.cultivo = cultivo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Tratamiento that = (Tratamiento) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return producto + " (" + tipo + ")";
    }
}

