package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Finca {

    public enum Estado {
        ACTIVA,
        ARRENDADA,
        INACTIVA
    }

    private Integer id;
    private Integer usuarioId;
    private String nombre;
    private String ubicacion;
    private BigDecimal superficie;
    private String tipoSuelo;
    private String coordenadasPoligono;
    private String sistemaRiego;
    private Estado estado;
    private LocalDate fechaRegistro;

    private Usuario propietario;
    private final List<Cultivo> cultivos = new ArrayList<>();
    private final List<GestionFinanciera> gestiones = new ArrayList<>();

    public Finca() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public BigDecimal getSuperficie() {
        return superficie;
    }

    public void setSuperficie(BigDecimal superficie) {
        this.superficie = superficie;
    }

    public String getTipoSuelo() {
        return tipoSuelo;
    }

    public void setTipoSuelo(String tipoSuelo) {
        this.tipoSuelo = tipoSuelo;
    }

    public String getCoordenadasPoligono() {
        return coordenadasPoligono;
    }

    public void setCoordenadasPoligono(String coordenadasPoligono) {
        this.coordenadasPoligono = coordenadasPoligono;
    }

    public String getSistemaRiego() {
        return sistemaRiego;
    }

    public void setSistemaRiego(String sistemaRiego) {
        this.sistemaRiego = sistemaRiego;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Usuario getPropietario() {
        return propietario;
    }

    public void setPropietario(Usuario propietario) {
        this.propietario = propietario;
    }

    public List<Cultivo> getCultivos() {
        return cultivos;
    }

    public List<GestionFinanciera> getGestiones() {
        return gestiones;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Finca finca = (Finca) o;
        return Objects.equals(id, finca.id);
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

