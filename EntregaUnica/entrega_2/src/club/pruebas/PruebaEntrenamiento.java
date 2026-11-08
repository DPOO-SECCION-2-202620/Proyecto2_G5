package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.acceso.SesionActual;
import club.Inscripciones.InscripcionEquipo;
import club.deportes.Categoria;
import club.deportes.Equipo;
import club.deportes.Futbol;
import club.entrenamientos.EstadoAsistencia;
import club.entrenamientos.RegistroEntrenamiento;
import club.entrenamientos.SesionEntrenamiento;
import club.instalaciones.Instalacion;
import club.personal.Entrenador;
import club.usuarios.Socio;

public class PruebaEntrenamiento {

    @BeforeEach
    void setUp() {
        Instalacion.getInstalaciones().clear();
        SesionEntrenamiento.getSesiones().clear();
        Equipo.getEquiposDisponibles().clear();
    }

    @Test
    void sesionSeCreaConConvocadosYDuracion() {
        Entrenador entrenador = new Entrenador("entrenador@club.co", "clave");
        Futbol futbol = new Futbol();
        Instalacion cancha = new Instalacion("Cancha principal");
        LocalDateTime fechaHora = LocalDateTime.now().plusDays(1);
        Duration duracion = Duration.ofMinutes(90);
        Equipo equipo = new Equipo("Tigres", Categoria.SUB15, 2, futbol) { };
        Socio socio1 = crearSocio("socio1@club.co");
        Socio socio2 = crearSocio("socio2@club.co");
        new InscripcionEquipo().inscribirSocio(
                socio1, equipo, Futbol.Posicion.DEFENSA, 4);
        new InscripcionEquipo().inscribirSocio(
                socio2, equipo, Futbol.Posicion.DELANTERO, 10);
        SesionActual sesionActual = new SesionActual(entrenador);

        SesionEntrenamiento sesion = sesionActual
                .programarSesionEntrenamiento(futbol, cancha, fechaHora, duracion);
        sesionActual.convocarEquipo(sesion, equipo);

        assertNotNull(sesion);
        assertSame(entrenador, sesion.getEntrenador());
        assertSame(futbol, sesion.getDeporte());
        assertSame(cancha, sesion.getInstalacion());
        assertEquals(fechaHora, sesion.getFechaHora());
        assertEquals(duracion, sesion.getDuracion());
        assertEquals(2, sesion.getConvocados().size());
        assertTrue(sesion.getConvocados().contains(socio1));
        assertTrue(sesion.getConvocados().contains(socio2));
    }

    @Test
    void registraAsistenciaYEvaluacionOpcional() {
        ContextoSesion contexto = crearSesionConConvocado();

        RegistroEntrenamiento registro = contexto.sesionActual.registrarEntrenamiento(
                contexto.sesion, contexto.socio, EstadoAsistencia.ASISTIO, null, null);

        assertEquals(EstadoAsistencia.ASISTIO, registro.getEstado());
        assertNull(registro.getCalificacion());
        assertNull(registro.getComentario());

        RegistroEntrenamiento actualizado = contexto.sesionActual.registrarEntrenamiento(
                contexto.sesion, contexto.socio, EstadoAsistencia.TARDE, 5, "Buen trabajo");
        assertSame(registro, actualizado);
        assertEquals(EstadoAsistencia.TARDE, actualizado.getEstado());
        assertEquals(5, actualizado.getCalificacion());
        assertEquals("Buen trabajo", actualizado.getComentario());
    }

    @Test
    void rechazaNotaFueraDeUnoACinco() {
        ContextoSesion contexto = crearSesionConConvocado();

        assertThrows(IllegalArgumentException.class,
                () -> contexto.sesionActual.registrarEntrenamiento(
                        contexto.sesion, contexto.socio, EstadoAsistencia.ASISTIO, 6, null));
        assertTrue(contexto.sesion.getRegistros().isEmpty());
    }

    @Test
    void rechazaJugadorNoConvocado() {
        ContextoSesion contexto = crearSesionConConvocado();
        Socio noConvocado = crearSocio("no-convocado@club.co");

        assertThrows(IllegalArgumentException.class,
                () -> contexto.sesionActual.registrarEntrenamiento(
                        contexto.sesion, noConvocado, EstadoAsistencia.FALTO, null, null));
        assertTrue(contexto.sesion.getRegistros().isEmpty());
    }

    private Socio crearSocio(String email) {
        Socio socio = new Socio(email, "clave");
        socio.setFechaNacimiento(java.time.LocalDate.now().minusYears(14));
        return socio;
    }

    private ContextoSesion crearSesionConConvocado() {
        Entrenador entrenador = new Entrenador("entrenador-registro@club.co", "clave");
        Futbol futbol = new Futbol();
        Instalacion cancha = new Instalacion("Cancha de registro");
        Equipo equipo = new Equipo("Equipo de registro", Categoria.SUB15, 1, futbol) { };
        Socio socio = crearSocio("convocado@club.co");
        new InscripcionEquipo().inscribirSocio(
                socio, equipo, Futbol.Posicion.DELANTERO, 10);
        SesionActual sesionActual = new SesionActual(entrenador);
        SesionEntrenamiento sesion = sesionActual.programarSesionEntrenamiento(
                futbol, cancha, LocalDateTime.now().plusDays(1), Duration.ofMinutes(90));
        sesionActual.convocarEquipo(sesion, equipo);
        return new ContextoSesion(sesionActual, sesion, socio);
    }

    private static class ContextoSesion {
        private SesionActual sesionActual;
        private SesionEntrenamiento sesion;
        private Socio socio;

        ContextoSesion(SesionActual sesionActual, SesionEntrenamiento sesion, Socio socio) {
            this.sesionActual = sesionActual;
            this.sesion = sesion;
            this.socio = socio;
        }
    }
}
