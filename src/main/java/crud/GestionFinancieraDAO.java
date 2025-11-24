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
import modelo.GestionFinanciera;

public class GestionFinancieraDAO {

    private static final String SELECT_BASE =
        "SELECT GES_ID, GES_FIN_ID, GES_TIPO, GES_CONCEPTO, GES_MONTO, GES_FECHA, GES_OBSERVACIONES "
            + "FROM gestion_financiera ";

    public List<GestionFinanciera> listarPorFinca(int fincaId) throws SQLException {
        String sql = SELECT_BASE + "WHERE GES_FIN_ID = ? ORDER BY GES_FECHA DESC";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fincaId);
            ResultSet rs = ps.executeQuery();
            List<GestionFinanciera> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        }
    }

    public List<GestionFinanciera> listarRecientes(int limite) throws SQLException {
        String sql = SELECT_BASE + "ORDER BY GES_FECHA DESC LIMIT ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            List<GestionFinanciera> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        }
    }

    public List<GestionFinanciera> listarRecientesPorFincas(List<Integer> fincaIds, int limite) throws SQLException {
        if (fincaIds == null || fincaIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Construir la lista de placeholders para IN clause
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < fincaIds.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }
        
        String sql = SELECT_BASE + "WHERE GES_FIN_ID IN (" + placeholders + ") ORDER BY GES_FECHA DESC LIMIT ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            for (Integer fincaId : fincaIds) {
                ps.setInt(paramIndex++, fincaId);
            }
            ps.setInt(paramIndex, limite);
            ResultSet rs = ps.executeQuery();
            List<GestionFinanciera> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        }
    }

    public GestionFinanciera guardar(GestionFinanciera gestion) throws SQLException {
        String sql = "INSERT INTO gestion_financiera (GES_FIN_ID, GES_TIPO, GES_CONCEPTO, GES_MONTO, GES_FECHA, "
            + "GES_OBSERVACIONES) VALUES (?,?,?,?,?,?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParametros(ps, gestion);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                gestion.setId(keys.getInt(1));
            }
            return gestion;
        }
    }

    public void actualizar(GestionFinanciera gestion) throws SQLException {
        String sql = "UPDATE gestion_financiera SET GES_FIN_ID=?, GES_TIPO=?, GES_CONCEPTO=?, GES_MONTO=?, "
            + "GES_FECHA=?, GES_OBSERVACIONES=? WHERE GES_ID=?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParametros(ps, gestion);
            ps.setInt(7, gestion.getId());
            ps.executeUpdate();
        }
    }

    public boolean eliminar(int gestionId) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM gestion_financiera WHERE GES_ID = ?")) {
            ps.setInt(1, gestionId);
            return ps.executeUpdate() > 0;
        }
    }

    private void setParametros(PreparedStatement ps, GestionFinanciera gestion) throws SQLException {
        ps.setInt(1, gestion.getFincaId());
        ps.setString(2, gestion.getTipo().name().toLowerCase());
        ps.setString(3, gestion.getConcepto());
        ps.setBigDecimal(4, gestion.getMonto());
        ps.setDate(5, toSqlDate(gestion.getFecha()));
        ps.setString(6, gestion.getObservaciones());
    }

    private Date toSqlDate(LocalDate fecha) {
        return fecha != null ? Date.valueOf(fecha) : null;
    }

    private GestionFinanciera mapear(ResultSet rs) throws SQLException {
        GestionFinanciera gestion = new GestionFinanciera();
        gestion.setId(rs.getInt("GES_ID"));
        gestion.setFincaId(rs.getInt("GES_FIN_ID"));
        gestion.setTipo(GestionFinanciera.Tipo.valueOf(rs.getString("GES_TIPO").toUpperCase()));
        gestion.setConcepto(rs.getString("GES_CONCEPTO"));
        gestion.setMonto(rs.getBigDecimal("GES_MONTO"));
        Date fecha = rs.getDate("GES_FECHA");
        if (fecha != null) {
            gestion.setFecha(fecha.toLocalDate());
        }
        gestion.setObservaciones(rs.getString("GES_OBSERVACIONES"));
        return gestion;
    }
}

