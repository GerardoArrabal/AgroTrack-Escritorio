package util;

import modelo.Finca;
import modelo.Usuario;

/**
 * Gestor de sesión de usuario para mantener el estado autenticado
 * y pasar datos entre controladores.
 */
public class Sesion {
    
    private static Sesion instancia;
    private Usuario usuarioActual;
    private Finca fincaSeleccionada;
    private String tabDestino;
    
    private Sesion() {
        // Constructor privado para singleton
    }
    
    public static Sesion getInstancia() {
        if (instancia == null) {
            instancia = new Sesion();
        }
        return instancia;
    }
    
    public Usuario getUsuarioActual() {
        return usuarioActual;
    }
    
    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
    }
    
    public Finca getFincaSeleccionada() {
        return fincaSeleccionada;
    }
    
    public void setFincaSeleccionada(Finca finca) {
        this.fincaSeleccionada = finca;
    }
    
    public String getTabDestino() {
        return tabDestino;
    }
    
    public void setTabDestino(String tabDestino) {
        this.tabDestino = tabDestino;
    }
    
    public boolean estaAutenticado() {
        return usuarioActual != null;
    }
    
    public boolean esAdministrador() {
        return estaAutenticado() && usuarioActual.getRol() == Usuario.Rol.ADMIN;
    }
    
    public void cerrarSesion() {
        usuarioActual = null;
        fincaSeleccionada = null;
        tabDestino = null;
    }
}

