package club.consola;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import club.acceso.SesionActual;
import club.personal.Empleado;
import club.personal.PlantillaTurnos;
import club.personal.SolicitudCambioTurno;
import club.personal.Turno;
import club.usuarios.Administrador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;
import club.usuarios.Usuario;

// Pantallas del administrador para turnos del personal y gestión de socios
class ConsolaAdminPersonal {

    private final EntradaConsola entrada;
    private final SesionActual sesion;

    ConsolaAdminPersonal(EntradaConsola entrada, SesionActual sesion) {
        this.entrada = entrada;
        this.sesion = sesion;
    }

    // HU Administrador 5: crear y modificar turnos semanales
    void gestionarTurnos() {
        PlantillaTurnos plantilla = ((Administrador) sesion.getUsuarioActual()).getPlantillaTurnos();
        List<Turno> turnos = plantilla.getTurnos();
        turnos.sort(Comparator.comparing(Turno::getInicio));
        entrada.mostrarMensaje("Turnos de la plantilla:");
        if (turnos.isEmpty()) entrada.mostrarMensaje("  (sin turnos)");
        for (Turno turno : turnos) entrada.mostrarMensaje("  " + FormatoAdministrador.turno(turno));
        entrada.mostrarMensaje("Turnos asignados: " + plantilla.getEntrenadoresDisponibles() + " de entrenadores y "
                + plantilla.getFisioterapeutasDisponibles() + " de fisioterapeutas (mínimo en atención: 2 y 1)");
        entrada.mostrarMensaje("1. Crear turno\n2. Revisar solicitudes de cambio\n0. Volver");
        int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 2);
        if (opcion == 1) crearTurno(plantilla);
        else if (opcion == 2) revisarSolicitudes(plantilla);
    }

    private void crearTurno(PlantillaTurnos plantilla) {
        List<Empleado> empleados = new ArrayList<>();
        for (Usuario usuario : RepositorioUsuarios.getRepositorioUsuarios().values()) {
            if (usuario instanceof Empleado && usuario.isActivo()) empleados.add((Empleado) usuario);
        }
        empleados.sort(Comparator.comparing(Usuario::getEmail));
        Empleado empleado = entrada.seleccionar("Empleado", empleados, Usuario::getEmail);
        if (empleado == null) return;
        LocalDateTime inicio = entrada.leerFechaHora("Inicio del turno");
        if (inicio == null) return;
        LocalDateTime fin = entrada.leerFechaHora("Fin del turno");
        if (fin == null) return;
        if (!fin.isAfter(inicio)) {
            entrada.mostrarMensaje("El fin del turno debe ser posterior al inicio.");
            return;
        }
        if (!entrada.confirmar("¿Asignar el turno a " + empleado.getEmail() + "?")) return;
        plantilla.agregarTurno(new Turno(empleado, inicio, fin));
        entrada.mostrarMensaje("Turno asignado.");
    }

    private void revisarSolicitudes(PlantillaTurnos plantilla) {
        List<SolicitudCambioTurno> pendientes = new ArrayList<>();
        for (SolicitudCambioTurno solicitud : plantilla.getSolicitudes()) if (!solicitud.isProcesada()) pendientes.add(solicitud);
        SolicitudCambioTurno solicitud = entrada.seleccionar("Solicitudes pendientes", pendientes,
                s -> FormatoAdministrador.turno(s.getTurnoAnterior()) + "  →  " + s.getTurnoNuevo().getInicio()
                        + " hasta " + s.getTurnoNuevo().getFin());
        if (solicitud == null || !entrada.confirmar("¿Aprobar el cambio? Se rechazará si no mantiene la cobertura mínima")) return;
        try {
            sesion.aprobarCambioTurno(solicitud);
            entrada.mostrarMensaje("Solicitud aprobada. El turno fue actualizado.");
        } catch (IllegalStateException e) {
            entrada.mostrarMensaje("Solicitud rechazada: " + e.getMessage());
        }
    }

    // Registro y mantenimiento de socios
    void gestionarSocios() {
        entrada.mostrarMensaje("Socios");
        entrada.mostrarMensaje("1. Consultar socios\n2. Registrar socio\n3. Corregir fecha de nacimiento\n4. Dar de baja socio\n0. Volver");
        int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 4);
        if (opcion == 1) {
            List<Socio> socios = socios(false);
            if (socios.isEmpty()) entrada.mostrarMensaje("No hay socios registrados.");
            for (Socio socio : socios) entrada.mostrarMensaje(FormatoAdministrador.usuario(socio)
                    + " - Nacimiento: " + (socio.getFechaNacimiento() == null ? "sin registrar" : socio.getFechaNacimiento()));
        } else if (opcion == 2) {
            registrarSocio();
        } else if (opcion == 3) {
            Socio socio = entrada.seleccionar("Socios", socios(true), FormatoAdministrador::usuario);
            if (socio == null) return;
            entrada.mostrarMensaje("Fecha actual: " + sesion.consultarFechaNacimiento(socio));
            LocalDate fecha = entrada.leerFecha("Nueva fecha de nacimiento");
            if (fecha == null || !entrada.confirmar("¿Guardar la nueva fecha?")) return;
            sesion.editarFechaNacimiento(socio, fecha);
            entrada.mostrarMensaje("Fecha actualizada.");
        } else if (opcion == 4) {
            Socio socio = entrada.seleccionar("Socios activos", socios(true), FormatoAdministrador::usuario);
            if (socio == null || !entrada.confirmar("¿Dar de baja a " + socio.getEmail() + "?")) return;
            sesion.darDeBaja(socio);
            entrada.mostrarMensaje("Socio dado de baja.");
        }
    }

    private void registrarSocio() {
        String correo = entrada.leerTexto("Correo del nuevo socio: ");
        if (!correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            entrada.mostrarMensaje("El correo no tiene un formato válido.");
            return;
        }
        if (RepositorioUsuarios.buscarPorEmail(correo) != null) {
            entrada.mostrarMensaje("Ya existe un usuario con ese correo.");
            return;
        }
        String contrasenia = entrada.leerLinea("Contraseña (mínimo 6 caracteres): ");
        if (contrasenia.length() < 6) {
            entrada.mostrarMensaje("La contraseña debe tener al menos 6 caracteres.");
            return;
        }
        if (!entrada.confirmar("¿Registrar al socio " + correo + "?")) return;
        sesion.registrarSocio(correo, contrasenia);
        entrada.mostrarMensaje("Socio registrado. Podrá registrar su fecha de nacimiento al iniciar sesión.");
    }

    private List<Socio> socios(boolean soloActivos) {
        List<Socio> socios = new ArrayList<>();
        for (Usuario usuario : RepositorioUsuarios.getRepositorioUsuarios().values()) {
            if (usuario instanceof Socio && (!soloActivos || usuario.isActivo())) socios.add((Socio) usuario);
        }
        socios.sort(Comparator.comparing(Usuario::getEmail));
        return socios;
    }
}
