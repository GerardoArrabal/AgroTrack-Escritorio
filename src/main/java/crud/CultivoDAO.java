package crud;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import modelo.Cultivo;
import modelo.Finca;

public class CultivoDAO {

    private static final String SELECT_BASE =
        "SELECT CUL_ID, CUL_FIN_ID, CUL_NOMBRE, CUL_VARIEDAD, CUL_FECHA_SIEMBRA, CUL_FECHA_COSECHA, "
            + "CUL_ESTADO, CUL_PRODUCCION_KG, CUL_REND_ESTIMADO, CUL_REND_REAL "
            + "FROM cultivo ";

    public List<Cultivo> listarPorFinca(int fincaId) throws SQLException {
        String sql = SELECT_BASE + "WHERE CUL_FIN_ID = ? ORDER BY CUL_FECHA_SIEMBRA DESC";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fincaId);
            ResultSet rs = ps.executeQuery();
            List<Cultivo> cultivos = new ArrayList<>();
            while (rs.next()) {
                cultivos.add(mapear(rs));
            }
            return cultivos;
        }
    }

    public Cultivo guardar(Cultivo cultivo) throws SQLException {
        String sql = "INSERT INTO cultivo (CUL_FIN_ID, CUL_NOMBRE, CUL_VARIEDAD, CUL_FECHA_SIEMBRA, "
            + "CUL_FECHA_COSECHA, CUL_ESTADO, CUL_PRODUCCION_KG, CUL_REND_ESTIMADO, CUL_REND_REAL) "
            + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParametros(ps, cultivo);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                cultivo.setId(keys.getInt(1));
            }
            return cultivo;
        }
    }

    public void actualizar(Cultivo cultivo) throws SQLException {
        String sql = "UPDATE cultivo SET CUL_FIN_ID=?, CUL_NOMBRE=?, CUL_VARIEDAD=?, CUL_FECHA_SIEMBRA=?, "
            + "CUL_FECHA_COSECHA=?, CUL_ESTADO=?, CUL_PRODUCCION_KG=?, CUL_REND_ESTIMADO=?, CUL_REND_REAL=? "
            + "WHERE CUL_ID=?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParametros(ps, cultivo);
            ps.setInt(10, cultivo.getId());
            ps.executeUpdate();
        }
    }

    public boolean eliminar(int cultivoId) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM cultivo WHERE CUL_ID=?")) {
            ps.setInt(1, cultivoId);
            return ps.executeUpdate() > 0;
        }
    }

    private void setParametros(PreparedStatement ps, Cultivo cultivo) throws SQLException {
        ps.setInt(1, cultivo.getFincaId());
        // Validar que el nombre no sea null o vacío
        String nombre = cultivo.getNombre();
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new SQLException("El nombre del cultivo es obligatorio");
        }
        ps.setString(2, nombre);
        ps.setString(3, cultivo.getVariedad());
        ps.setDate(4, toSqlDate(cultivo.getFechaSiembra()));
        ps.setDate(5, toSqlDate(cultivo.getFechaCosecha()));
        // Validar que el estado no sea null, usar ACTIVO por defecto
        Cultivo.Estado estado = cultivo.getEstado() != null ? cultivo.getEstado() : Cultivo.Estado.ACTIVO;
        ps.setString(6, estado.name().toLowerCase());
        ps.setBigDecimal(7, cultivo.getProduccionKg());
        ps.setBigDecimal(8, cultivo.getRendimientoEstimado());
        ps.setBigDecimal(9, cultivo.getRendimientoReal());
    }

    private Date toSqlDate(LocalDate date) {
        return date != null ? Date.valueOf(date) : null;
    }

    private Cultivo mapear(ResultSet rs) throws SQLException {
        Cultivo cultivo = new Cultivo();
        cultivo.setId(rs.getInt("CUL_ID"));
        cultivo.setFincaId(rs.getInt("CUL_FIN_ID"));
        cultivo.setNombre(rs.getString("CUL_NOMBRE"));
        cultivo.setVariedad(rs.getString("CUL_VARIEDAD"));
        Date siembra = rs.getDate("CUL_FECHA_SIEMBRA");
        if (siembra != null) {
            cultivo.setFechaSiembra(siembra.toLocalDate());
        }
        Date cosecha = rs.getDate("CUL_FECHA_COSECHA");
        if (cosecha != null) {
            cultivo.setFechaCosecha(cosecha.toLocalDate());
        }
        cultivo.setEstado(Cultivo.Estado.valueOf(rs.getString("CUL_ESTADO").toUpperCase()));
        cultivo.setProduccionKg(rs.getBigDecimal("CUL_PRODUCCION_KG"));
        cultivo.setRendimientoEstimado(rs.getBigDecimal("CUL_REND_ESTIMADO"));
        cultivo.setRendimientoReal(rs.getBigDecimal("CUL_REND_REAL"));
        return cultivo;
    }
}

