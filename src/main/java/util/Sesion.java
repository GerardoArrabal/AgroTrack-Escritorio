package util;

import modelo.Finca;
import modelo.Usuario;

/**
 * GESTOR DE SESIÓN DE USUARIO (PATRÓN SINGLETON)
 * 
 * Esta clase mantiene el estado de la sesión del usuario autenticado durante
 * toda la ejecución de la aplicación. Usa el patrón Singleton para asegurar
 * que solo existe UNA instancia en toda la aplicación.
 * 
 * ¿Qué es el patrón Singleton?
 * - Solo puede existir UNA instancia de esta clase
 * - Se accede siempre a través de: Sesion.getInstancia()
 * - Útil para datos globales que necesitas en toda la app (usuario logueado, etc.)
 * 
 * ¿Qué datos guarda?
 * - Usuario actual: El usuario que está logueado
 * - Finca seleccionada: La finca que el usuario está viendo/editando
 * - Tab destino: Para navegación entre ventanas
 * 
 * IMPORTANTE: Cuando el usuario cierra sesión, se limpian todos estos datos.
 */
public class Sesion {
    
    private static Sesion instancia;  // La única instancia que existirá
    private Usuario usuarioActual;
    private Finca fincaSeleccionada;
    private String tabDestino;
    
    /**
     * Constructor privado: Evita que se pueda crear con "new Sesion()"
     * Solo se puede obtener la instancia con getInstancia()
     */
    private Sesion() {
        // Constructor privado para singleton
    }
    
    /**
     * Obtiene la única instancia de Sesion (patrón Singleton).
     * 
     * Si no existe, la crea. Si ya existe, devuelve la misma.
     * Esto garantiza que siempre trabajas con la misma instancia.
     * 
     * @return La única instancia de Sesion
     */
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

