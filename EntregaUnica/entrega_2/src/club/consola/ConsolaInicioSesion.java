package club.consola;

import club.acceso.Autenticador;
import club.acceso.SesionActual;
import club.usuarios.Usuario;

public class ConsolaInicioSesion {

    private final EntradaConsola entrada;
    private final Autenticador autenticador;

    // Recibe la entrada y el autenticador compartidos
    public ConsolaInicioSesion(EntradaConsola entrada, Autenticador autenticador) {
        if (entrada == null || autenticador == null) {
            throw new IllegalArgumentException("La entrada y el autenticador son obligatorios");
        }
        this.entrada = entrada;
        this.autenticador = autenticador;
    }

    // Solicita credenciales hasta obtener el rol esperado o salir
    public SesionActual iniciarSesion(Class<? extends Usuario> tipoEsperado) {
        if (tipoEsperado == null) throw new IllegalArgumentException("El tipo de usuario es obligatorio");
        while (true) {
            entrada.mostrarMensaje("Club Deportivo Séneca - Inicio de sesión");
            entrada.mostrarMensaje("1. Iniciar sesión\n0. Salir");
            if (entrada.leerEntero("Seleccione una opción: ", 0, 1) == 0) return null;
            String correo = entrada.leerTexto("Correo: ");
            String contrasenia = entrada.leerLinea("Contraseña: ");
            try {
                SesionActual sesion = autenticador.autenticar(correo, contrasenia);
                if (!tipoEsperado.isInstance(sesion.getUsuarioActual())) {
                    sesion.cerrarSesion();
                    entrada.mostrarMensaje("La cuenta no corresponde al tipo de usuario de esta consola.");
                    continue;
                }
                entrada.mostrarMensaje("Inicio de sesión exitoso.");
                return sesion;
            } catch (IllegalArgumentException | IllegalStateException e) {
                entrada.mostrarMensaje("No fue posible iniciar sesión: " + e.getMessage());
            }
        }
    }
}
