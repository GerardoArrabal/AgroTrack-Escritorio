package crud;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import modelo.Tratamiento;

public class TratamientoDAO {

    private static final String SELECT_BASE =
        "SELECT TRA_ID, TRA_CUL_ID, TRA_FECHA, TRA_PRODUCTO, TRA_TIPO, TRA_DOSIS, TRA_PRECIO_TRAT, TRA_OBSERVACIONES "
            + "FROM tratamiento ";

    public List<Tratamiento> listarPorCultivo(int cultivoId) throws SQLException {
        String sql = SELECT_BASE + "WHERE TRA_CUL_ID = ? ORDER BY TRA_FECHA DESC";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cultivoId);
            ResultSet rs = ps.executeQuery();
            List<Tratamiento> tratamientos = new ArrayList<>();
            while (rs.next()) {
                tratamientos.add(mapear(rs));
            }
            return tratamientos;
        }
    }

    public Tratamiento guardar(Tratamiento tratamiento) throws SQLException {
        String sql = "INSERT INTO tratamiento (TRA_CUL_ID, TRA_FECHA, TRA_PRODUCTO, TRA_TIPO, TRA_DOSIS, "
            + "TRA_PRECIO_TRAT, TRA_OBSERVACIONES) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParametros(ps, tratamiento);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                tratamiento.setId(keys.getInt(1));
            }
            return tratamiento;
        }
    }

    public void actualizar(Tratamiento tratamiento) throws SQLException {
        String sql = "UPDATE tratamiento SET TRA_CUL_ID=?, TRA_FECHA=?, TRA_PRODUCTO=?, TRA_TIPO=?, TRA_DOSIS=?, "
            + "TRA_PRECIO_TRAT=?, TRA_OBSERVACIONES=? WHERE TRA_ID=?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParametros(ps, tratamiento);
            ps.setInt(8, tratamiento.getId());
            ps.executeUpdate();
        }
    }

    public boolean eliminar(int tratamientoId) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM tratamiento WHERE TRA_ID=?")) {
            ps.setInt(1, tratamientoId);
            return ps.executeUpdate() > 0;
        }
    }

    private void setParametros(PreparedStatement ps, Tratamiento tratamiento) throws SQLException {
        ps.setInt(1, tratamiento.getCultivoId());
        ps.setDate(2, toSqlDate(tratamiento.getFecha()));
        ps.setString(3, tratamiento.getProducto());
        ps.setString(4, tratamiento.getTipo().name().toLowerCase());
        ps.setString(5, tratamiento.getDosis());
        ps.setBigDecimal(6, tratamiento.getPrecioTratamiento());
        ps.setString(7, tratamiento.getObservaciones());
    }

    private Date toSqlDate(LocalDate fecha) {
        return fecha != null ? Date.valueOf(fecha) : null;
    }

    public BigDecimal obtenerCostoTotalPorFinca(int fincaId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(t.TRA_PRECIO_TRAT), 0) AS total "
            + "FROM tratamiento t JOIN cultivo c ON t.TRA_CUL_ID = c.CUL_ID "
            + "WHERE c.CUL_FIN_ID = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fincaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total");
                return total != null ? total : BigDecimal.ZERO;
            }
            return BigDecimal.ZERO;
        }
    }

    private Tratamiento mapear(ResultSet rs) throws SQLException {
        Tratamiento t = new Tratamiento();
        t.setId(rs.getInt("TRA_ID"));
        t.setCultivoId(rs.getInt("TRA_CUL_ID"));
        Date fecha = rs.getDate("TRA_FECHA");
        if (fecha != null) {
            t.setFecha(fecha.toLocalDate());
        }
        t.setProducto(rs.getString("TRA_PRODUCTO"));
        t.setTipo(Tratamiento.Tipo.valueOf(rs.getString("TRA_TIPO").toUpperCase()));
        t.setDosis(rs.getString("TRA_DOSIS"));
        t.setPrecioTratamiento(rs.getBigDecimal("TRA_PRECIO_TRAT"));
        t.setObservaciones(rs.getString("TRA_OBSERVACIONES"));
        return t;
    }
}

