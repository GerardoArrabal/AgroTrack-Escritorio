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
import java.util.Optional;
import modelo.Usuario;
import util.SeguridadUtil;

public class UsuarioDAO {

    private static final String SELECT_BASE =
        "SELECT USU_ID, USU_NOMBRE, USU_APELLIDOS, USU_EMAIL, USU_USERNAME, USU_PASSWORD, USU_ROL, "
            + "USU_FECHA_REGISTRO, USU_ACTIVO FROM usuario ";

    public Optional<Usuario> autenticar(String usuarioOCorreo, String password) throws SQLException {
        String sql = SELECT_BASE
            + "WHERE (USU_USERNAME = ? OR USU_EMAIL = ?) AND USU_ACTIVO = TRUE";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuarioOCorreo);
            ps.setString(2, usuarioOCorreo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Usuario usuario = mapear(rs);
                if (SeguridadUtil.verificarPassword(password, usuario.getPassword())) {
                    return Optional.of(usuario);
                }
                // Compatibilidad con contraseñas antiguas en texto plano
                if (password.equals(usuario.getPassword())) {
                    String nuevoHash = SeguridadUtil.hashPassword(password);
                    actualizarPassword(usuario.getId(), nuevoHash);
                    usuario.setPassword(nuevoHash);
                    return Optional.of(usuario);
                }
            }
            return Optional.empty();
        }
    }

    public List<Usuario> listarTodos() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + "ORDER BY USU_FECHA_REGISTRO DESC")) {
            ResultSet rs = ps.executeQuery();
            List<Usuario> usuarios = new ArrayList<>();
            while (rs.next()) {
                usuarios.add(mapear(rs));
            }
            return usuarios;
        }
    }

    public Usuario guardar(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuario (USU_NOMBRE, USU_APELLIDOS, USU_EMAIL, USU_USERNAME, "
            + "USU_PASSWORD, USU_ROL, USU_FECHA_REGISTRO, USU_ACTIVO) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getApellidos());
            ps.setString(3, usuario.getEmail());
            ps.setString(4, usuario.getUsername());
            String hash = SeguridadUtil.hashPassword(usuario.getPassword());
            ps.setString(5, hash);
            ps.setString(6, usuario.getRol().name().toLowerCase());
            ps.setDate(7, Date.valueOf(
                usuario.getFechaRegistro() != null ? usuario.getFechaRegistro() : LocalDate.now()));
            ps.setBoolean(8, usuario.isActivo());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                usuario.setId(keys.getInt(1));
            }
            usuario.setPassword(hash);
            return usuario;
        }
    }

    public void actualizar(Usuario usuario, String nuevoPasswordPlano) throws SQLException {
        if (usuario.getFechaRegistro() == null) {
            usuario.setFechaRegistro(LocalDate.now());
        }
        StringBuilder sql = new StringBuilder("UPDATE usuario SET USU_NOMBRE=?, USU_APELLIDOS=?, "
            + "USU_EMAIL=?, USU_USERNAME=?, USU_ROL=?, USU_FECHA_REGISTRO=?, USU_ACTIVO=?");
        boolean actualizarPassword = nuevoPasswordPlano != null && !nuevoPasswordPlano.isBlank();
        if (actualizarPassword) {
            sql.append(", USU_PASSWORD=?");
        }
        sql.append(" WHERE USU_ID=?");

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, usuario.getNombre());
            ps.setString(idx++, usuario.getApellidos());
            ps.setString(idx++, usuario.getEmail());
            ps.setString(idx++, usuario.getUsername());
            // Validar que el rol no sea null, usar USUARIO por defecto
            Usuario.Rol rol = usuario.getRol() != null ? usuario.getRol() : Usuario.Rol.USUARIO;
            ps.setString(idx++, rol.name().toLowerCase());
            ps.setDate(idx++, Date.valueOf(usuario.getFechaRegistro()));
            ps.setBoolean(idx++, usuario.isActivo());
            if (actualizarPassword) {
                String hash = SeguridadUtil.hashPassword(nuevoPasswordPlano);
                ps.setString(idx++, hash);
                usuario.setPassword(hash);
            }
            ps.setInt(idx, usuario.getId());
            ps.executeUpdate();
        }
    }

    public boolean existeUsername(String username) throws SQLException {
        return existeCampo("USU_USERNAME", username);
    }

    public boolean existeEmail(String email) throws SQLException {
        return existeCampo("USU_EMAIL", email);
    }

    private boolean existeCampo(String campo, String valor) throws SQLException {
        String sql = "SELECT COUNT(1) FROM usuario WHERE " + campo + " = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    public boolean desactivar(int usuarioId, boolean activo) throws SQLException {
        String sql = "UPDATE usuario SET USU_ACTIVO = ? WHERE USU_ID = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, usuarioId);
            return ps.executeUpdate() > 0;
        }
    }

    private void actualizarPassword(int usuarioId, String hash) throws SQLException {
        String sql = "UPDATE usuario SET USU_PASSWORD = ? WHERE USU_ID = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, usuarioId);
            ps.executeUpdate();
        }
    }

    public boolean eliminar(int usuarioId) throws SQLException {
        String sql = "DELETE FROM usuario WHERE USU_ID = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            return ps.executeUpdate() > 0;
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("USU_ID"));
        usuario.setNombre(rs.getString("USU_NOMBRE"));
        usuario.setApellidos(rs.getString("USU_APELLIDOS"));
        usuario.setEmail(rs.getString("USU_EMAIL"));
        usuario.setUsername(rs.getString("USU_USERNAME"));
        usuario.setPassword(rs.getString("USU_PASSWORD"));
        usuario.setRol(Usuario.Rol.valueOf(rs.getString("USU_ROL").toUpperCase()));
        usuario.setFechaRegistro(rs.getDate("USU_FECHA_REGISTRO").toLocalDate());
        usuario.setActivo(rs.getBoolean("USU_ACTIVO"));
        return usuario;
    }
}

