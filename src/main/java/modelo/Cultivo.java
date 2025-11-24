package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Cultivo {

    public enum Estado {
        ACTIVO,
        COSECHADO,
        EN_PREPARACION
    }

    private Integer id;
    private Integer fincaId;
    private String nombre;
    private String variedad;
    private LocalDate fechaSiembra;
    private LocalDate fechaCosecha;
    private Estado estado;
    private BigDecimal produccionKg;
    private BigDecimal rendimientoEstimado;
    private BigDecimal rendimientoReal;

    private Finca finca;
    private final List<Tratamiento> tratamientos = new ArrayList<>();

    public Cultivo() {
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getVariedad() {
        return variedad;
    }

    public void setVariedad(String variedad) {
        this.variedad = variedad;
    }

    public LocalDate getFechaSiembra() {
        return fechaSiembra;
    }

    public void setFechaSiembra(LocalDate fechaSiembra) {
        this.fechaSiembra = fechaSiembra;
    }

    public LocalDate getFechaCosecha() {
        return fechaCosecha;
    }

    public void setFechaCosecha(LocalDate fechaCosecha) {
        this.fechaCosecha = fechaCosecha;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public BigDecimal getProduccionKg() {
        return produccionKg;
    }

    public void setProduccionKg(BigDecimal produccionKg) {
        this.produccionKg = produccionKg;
    }

    public BigDecimal getRendimientoEstimado() {
        return rendimientoEstimado;
    }

    public void setRendimientoEstimado(BigDecimal rendimientoEstimado) {
        this.rendimientoEstimado = rendimientoEstimado;
    }

    public BigDecimal getRendimientoReal() {
        return rendimientoReal;
    }

    public void setRendimientoReal(BigDecimal rendimientoReal) {
        this.rendimientoReal = rendimientoReal;
    }

    public Finca getFinca() {
        return finca;
    }

    public void setFinca(Finca finca) {
        this.finca = finca;
    }

    public List<Tratamiento> getTratamientos() {
        return tratamientos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Cultivo cultivo = (Cultivo) o;
        return Objects.equals(id, cultivo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nombre;
    }
}

