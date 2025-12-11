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
import modelo.Finca;
import modelo.Usuario;

public class FincaDAO {

    private static final String SELECT_BASE =
        "SELECT f.FIN_ID, f.FIN_USU_ID, f.FIN_NOMBRE, f.FIN_UBICACION, f.FIN_SUPERFICIE, f.FIN_TIPO_SUELO, "
            + "f.FIN_COORD_POLIGONO, f.FIN_SISTEMA_RIEGO, f.FIN_ESTADO, f.FIN_FECHA_REGISTRO, "
            + "u.USU_ID, u.USU_NOMBRE, u.USU_APELLIDOS "
            + "FROM finca f LEFT JOIN usuario u ON f.FIN_USU_ID = u.USU_ID ";

    public List<Finca> listarTodas() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + "ORDER BY f.FIN_FECHA_REGISTRO DESC")) {
            ResultSet rs = ps.executeQuery();
            List<Finca> fincas = new ArrayList<>();
            while (rs.next()) {
                fincas.add(mapear(rs));
            }
            return fincas;
        }
    }

    public List<Finca> listarPorUsuario(int usuarioId) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + "WHERE f.FIN_USU_ID = ?")) {
            ps.setInt(1, usuarioId);
            ResultSet rs = ps.executeQuery();
            List<Finca> fincas = new ArrayList<>();
            while (rs.next()) {
                fincas.add(mapear(rs));
            }
            return fincas;
        }
    }

    public Finca guardar(Finca finca) throws SQLException {
        String sql = "INSERT INTO finca (FIN_USU_ID, FIN_NOMBRE, FIN_UBICACION, FIN_SUPERFICIE, FIN_TIPO_SUELO, "
            + "FIN_COORD_POLIGONO, FIN_SISTEMA_RIEGO, FIN_ESTADO, FIN_FECHA_REGISTRO) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, finca.getUsuarioId());
            ps.setString(2, finca.getNombre());
            ps.setString(3, finca.getUbicacion());
            ps.setBigDecimal(4, finca.getSuperficie());
            ps.setString(5, finca.getTipoSuelo());
            ps.setString(6, finca.getCoordenadasPoligono());
            ps.setString(7, finca.getSistemaRiego());
            ps.setString(8, finca.getEstado().name().toLowerCase());
            ps.setDate(9, Date.valueOf(finca.getFechaRegistro() != null ? finca.getFechaRegistro() : LocalDate.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                finca.setId(keys.getInt(1));
            }
            return finca;
        }
    }

    public void actualizar(Finca finca) throws SQLException {
        String sql = "UPDATE finca SET FIN_USU_ID=?, FIN_NOMBRE=?, FIN_UBICACION=?, FIN_SUPERFICIE=?, "
            + "FIN_TIPO_SUELO=?, FIN_COORD_POLIGONO=?, FIN_SISTEMA_RIEGO=?, FIN_ESTADO=?, FIN_FECHA_REGISTRO=? "
            + "WHERE FIN_ID=?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Validar que el usuarioId no sea null
            if (finca.getUsuarioId() == null) {
                throw new SQLException("El ID del usuario propietario es obligatorio");
            }
            ps.setInt(1, finca.getUsuarioId());
            // Validar que el nombre no sea null o vacío
            String nombre = finca.getNombre();
            if (nombre == null || nombre.trim().isEmpty()) {
                throw new SQLException("El nombre de la finca es obligatorio");
            }
            ps.setString(2, nombre);
            ps.setString(3, finca.getUbicacion());
            ps.setBigDecimal(4, finca.getSuperficie());
            ps.setString(5, finca.getTipoSuelo());
            ps.setString(6, finca.getCoordenadasPoligono());
            ps.setString(7, finca.getSistemaRiego());
            ps.setString(8, finca.getEstado().name().toLowerCase());
            ps.setDate(9, Date.valueOf(
                finca.getFechaRegistro() != null ? finca.getFechaRegistro() : LocalDate.now()));
            ps.setInt(10, finca.getId());
            ps.executeUpdate();
        }
    }

    public boolean eliminar(int fincaId) throws SQLException {
        String sql = "DELETE FROM finca WHERE FIN_ID = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fincaId);
            return ps.executeUpdate() > 0;
        }
    }

    private Finca mapear(ResultSet rs) throws SQLException {
        Finca finca = new Finca();
        finca.setId(rs.getInt("FIN_ID"));
        finca.setUsuarioId(rs.getInt("FIN_USU_ID"));
        finca.setNombre(rs.getString("FIN_NOMBRE"));
        finca.setUbicacion(rs.getString("FIN_UBICACION"));
        BigDecimal superficie = rs.getBigDecimal("FIN_SUPERFICIE");
        finca.setSuperficie(superficie);
        finca.setTipoSuelo(rs.getString("FIN_TIPO_SUELO"));
        finca.setCoordenadasPoligono(rs.getString("FIN_COORD_POLIGONO"));
        finca.setSistemaRiego(rs.getString("FIN_SISTEMA_RIEGO"));
        finca.setEstado(Finca.Estado.valueOf(rs.getString("FIN_ESTADO").toUpperCase()));
        Date fecha = rs.getDate("FIN_FECHA_REGISTRO");
        if (fecha != null) {
            finca.setFechaRegistro(fecha.toLocalDate());
        }

        int propietarioId = rs.getInt("USU_ID");
        if (propietarioId > 0) {
            Usuario propietario = new Usuario();
            propietario.setId(propietarioId);
            propietario.setNombre(rs.getString("USU_NOMBRE"));
            propietario.setApellidos(rs.getString("USU_APELLIDOS"));
            finca.setPropietario(propietario);
        }
        return finca;
    }
}

