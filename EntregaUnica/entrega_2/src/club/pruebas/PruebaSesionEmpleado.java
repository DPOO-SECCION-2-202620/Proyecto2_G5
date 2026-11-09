package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.acceso.SesionActual;
import club.deportes.Deporte;
import club.deportes.Tenis;
import club.personal.Entrenador;
import club.personal.Fisioterapeuta;
import club.personal.PlantillaTurnos;
import club.personal.SolicitudCambioTurno;
import club.personal.Turno;
import club.tienda.GestorTienda;
import club.tienda.TipoSugerencia;
import club.torneos.CatalogoTorneos;
import club.torneos.TipoTorneo;
import club.torneos.Torneo;
import club.torneos.TorneoInterno;
import club.usuarios.Administrador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;

public class PruebaSesionEmpleado {

    private Entrenador entrenador;
    private Fisioterapeuta fisioterapeuta;
    private Administrador administrador;
    private SesionActual sesion;
    private Turno propio;
    private Turno ajeno;
    private LocalDateTime inicio;
    private GestorTienda gestor;

    // Prepara empleados con turnos y un gestor compartido
    @BeforeEach
    void preparar() {
        RepositorioUsuarios.getRepositorioUsuarios().clear();
        Deporte.getDeportesRegistrados().clear();
        CatalogoTorneos.limpiar();
        entrenador = new Entrenador("entrenador-sesion@club.co", "clave");
        fisioterapeuta = new Fisioterapeuta("fisio-sesion@club.co", "clave");
        administrador = new Administrador("admin-empleado@club.co", "clave");
        sesion = new SesionActual(entrenador);
        inicio = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
        propio = new Turno(entrenador, inicio, inicio.plusHours(4));
        ajeno = new Turno(fisioterapeuta, inicio, inicio.plusHours(4));
        administrador.getPlantillaTurnos().agregarTurno(propio);
        administrador.getPlantillaTurnos().agregarTurno(ajeno);
        gestor = new GestorTienda();
        new Tenis();
    }

    // Consulta solamente los turnos del empleado autenticado
    @Test
    void consultaSoloTurnosPropios() {
        assertEquals(List.of(propio), sesion.consultarMisTurnos());
        assertEquals(List.of(ajeno), new SesionActual(fisioterapeuta).consultarMisTurnos());
    }

    // Conserva el turno original mientras la solicitud esta pendiente
    @Test
    void solicitudPendienteNoModificaHorario() {
        SolicitudCambioTurno solicitud = sesion.solicitarCambioTurno(
                propio, inicio.plusHours(1), inicio.plusHours(5));
        assertFalse(solicitud.isProcesada());
        assertFalse(solicitud.isAprobada());
        assertSame(entrenador, solicitud.getTurnoNuevo().getEmpleado());
        assertEquals(List.of(propio), sesion.consultarMisTurnos());
        assertEquals(List.of(solicitud), administrador.getPlantillaTurnos().getSolicitudes());
    }

    // Impide solicitar cambios sobre turnos de otro empleado
    @Test
    void rechazaTurnoAjeno() {
        assertThrows(IllegalStateException.class,
                () -> sesion.solicitarCambioTurno(ajeno, inicio.plusHours(1), inicio.plusHours(5)));
        assertTrue(administrador.getPlantillaTurnos().getSolicitudes().isEmpty());
    }

    // Rechaza horarios invalidos sin registrar una solicitud
    @Test
    void rechazaHorarioInvalido() {
        assertThrows(IllegalArgumentException.class, () -> sesion.solicitarCambioTurno(propio, inicio, inicio));
        assertTrue(sesion.consultarMisSolicitudesCambio().isEmpty());
    }

    // Rechaza turnos que no pertenecen a una plantilla registrada
    @Test
    void rechazaTurnoNoRegistrado() {
        Turno inexistente = new Turno(entrenador, inicio.plusDays(1), inicio.plusDays(1).plusHours(4));
        assertThrows(IllegalArgumentException.class,
                () -> sesion.solicitarCambioTurno(inexistente, inicio.plusHours(1), inicio.plusHours(5)));
        assertTrue(sesion.consultarMisSolicitudesCambio().isEmpty());
    }

    // Muestra solamente las solicitudes del empleado autenticado
    @Test
    void consultaSoloSolicitudesPropias() {
        SolicitudCambioTurno propia = sesion.solicitarCambioTurno(propio, inicio.plusHours(1), inicio.plusHours(5));
        new SesionActual(fisioterapeuta).solicitarCambioTurno(ajeno, inicio.plusHours(1), inicio.plusHours(5));
        assertEquals(List.of(propia), sesion.consultarMisSolicitudesCambio());
    }

    // Permite al administrador aprobar una solicitud registrada por el empleado
    @Test
    void administradorApruebaSolicitudConCobertura() {
        PlantillaTurnos plantilla = administrador.getPlantillaTurnos();
        plantilla.agregarTurno(new Turno(new Entrenador("segundo@club.co", "clave"), inicio, inicio.plusHours(4)));
        SolicitudCambioTurno solicitud = sesion.solicitarCambioTurno(propio, inicio, inicio.plusHours(4));
        administrador.aprobarCambioTurno(solicitud);
        assertTrue(solicitud.isAprobada());
        assertEquals(List.of(solicitud.getTurnoNuevo()), sesion.consultarMisTurnos());
    }

    // Registra la sugerencia a nombre de quien tiene la sesion
    @Test
    void sugerenciasConservanSolicitanteYPrivacidad() {
        sesion.registrarSugerencia(gestor, "Balón", "Balón de entrenamiento", TipoSugerencia.ARTICULO);
        new SesionActual(fisioterapeuta).registrarSugerencia(
                gestor, "Agua", "Bebida fría", TipoSugerencia.BEBIDA);
        assertEquals(2, gestor.getSugerencias().size());
        assertEquals(1, sesion.consultarMisSugerencias(gestor).size());
        assertSame(entrenador, sesion.consultarMisSugerencias(gestor).get(0).getSolicitante());
    }

    // Rechaza funcionalidades de empleado para cuentas de socio
    @Test
    void rechazaSocioEnOperacionesDeEmpleado() {
        SesionActual socio = new SesionActual(new Socio("socio-rol@club.co", "clave"));
        assertThrows(IllegalStateException.class, socio::consultarMisTurnos);
        assertThrows(IllegalStateException.class,
                () -> socio.registrarSugerencia(gestor, "Agua", "Bebida", TipoSugerencia.BEBIDA));
        assertTrue(gestor.getSugerencias().isEmpty());
    }

    // Rechaza operaciones cuando la cuenta queda inactiva o cierra sesion
    @Test
    void rechazaEmpleadoInactivoYSesionCerrada() {
        entrenador.setActivo(false);
        assertThrows(IllegalStateException.class, sesion::consultarMisTurnos);
        entrenador.setActivo(true);
        sesion.cerrarSesion();
        assertThrows(IllegalStateException.class,
                () -> sesion.registrarSugerencia(gestor, "Agua", "Bebida", TipoSugerencia.BEBIDA));
    }

    // Impide al fisioterapeuta usar operaciones exclusivas del entrenador
    @Test
    void fisioterapeutaNoPuedeProgramarEntrenamientos() {
        SesionActual fisio = new SesionActual(fisioterapeuta);
        assertThrows(IllegalStateException.class,
                () -> fisio.programarSesionEntrenamiento(new Tenis(), null, inicio, java.time.Duration.ofHours(1)));
        assertThrows(IllegalStateException.class, () -> fisio.puedeDisputarPartido(null));
    }

    // Rechaza la inscripcion de empleados que estan de turno
    @Test
    void torneoRechazaEmpleadoDeTurno() {
        TorneoInterno torneo = administrador.crearTorneo("Tenis", "PRINCIPIANTE", "INDIVIDUAL",
                10, TipoTorneo.COMPETITIVO, 10000, Torneo.Formato.LIGA, inicio.plusHours(1));
        assertThrows(IllegalStateException.class, () -> sesion.inscribirEnTorneo(torneo));
        assertFalse(torneo.estaInscrito(entrenador));
    }

    // Inscribe gratis al fisioterapeuta libre sin premio en efectivo
    @Test
    void torneoCompetitivoNoCobraNiPremiaAlEmpleado() {
        TorneoInterno torneo = administrador.crearTorneo("Tenis", "PRINCIPIANTE", "INDIVIDUAL",
                10, TipoTorneo.COMPETITIVO, 10000, Torneo.Formato.LIGA, inicio.plusDays(1));
        new SesionActual(fisioterapeuta).inscribirEnTorneo(torneo);
        assertEquals(0, torneo.getInscripcion(fisioterapeuta).getTarifaPagada());
        assertFalse(torneo.getInscripcion(fisioterapeuta).puedeRecibirPremio());
    }
}
