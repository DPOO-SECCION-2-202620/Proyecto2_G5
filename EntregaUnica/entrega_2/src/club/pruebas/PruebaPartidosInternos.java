package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.Inscripciones.InscripcionEquipo;
import club.deportes.Baloncesto;
import club.deportes.Categoria;
import club.deportes.DeporteConjunto;
import club.deportes.Equipo;
import club.deportes.Futbol;
import club.partidos.PartidoInterno;
import club.personal.Entrenador;
import club.usuarios.Administrador;
import club.usuarios.Socio;

public class PruebaPartidosInternos {
    private Administrador administrador;
    private Entrenador entrenador;

    @BeforeEach
    void setUp() {
        Equipo.getEquiposDisponibles().clear();
        Equipo.getEquiposRegistrados().clear();
        PartidoInterno.getPartidosInternos().clear();
        administrador = new Administrador("admin-partido@club.co", "clave");
        entrenador = new Entrenador("entrenador-partido@club.co", "clave");
    }

    @Test
    void equipoTieneCupoMaximoConfigurado() {
        Equipo equipo = new Equipo("Equipo A", Categoria.SUB17, 15, new Futbol()) { };
        assertEquals(15, equipo.getCuposDisponibles());
    }

    @Test
    void partidoInternoExigeMismoDeporteYCategoria() {
        Equipo futbol = crearEquipo("Fútbol", Categoria.SUB15, new Futbol(), 7);
        Equipo baloncesto = crearEquipo("Baloncesto", Categoria.SUB15, new Baloncesto(), 5);

        assertThrows(IllegalArgumentException.class,
                () -> administrador.programarPartidoInterno(futbol, baloncesto,
                        LocalDateTime.now().plusDays(1), entrenador));
        assertTrue(PartidoInterno.getPartidosInternos().isEmpty());
    }

    @Test
    void rechazaEquipoConNominaInsuficiente() {
        Futbol futbol = new Futbol();
        Equipo completo = crearEquipo("Completo", Categoria.SUB15, futbol, 7);
        Equipo incompleto = crearEquipo("Incompleto", Categoria.SUB15, futbol, 6);

        assertThrows(IllegalStateException.class,
                () -> administrador.programarPartidoInterno(completo, incompleto,
                        LocalDateTime.now().plusDays(1), entrenador));
        assertTrue(PartidoInterno.getPartidosInternos().isEmpty());
    }

    @Test
    void rechazaCupoMaximoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> new Equipo("Inválido", Categoria.SUB15, -1, new Futbol()) { });
    }

    private Equipo crearEquipo(String nombre, Categoria categoria,
            DeporteConjunto deporte, int cantidadJugadores) {
        Equipo equipo = new Equipo(nombre, categoria, cantidadJugadores + 1, deporte) { };
        for (int i = 0; i < cantidadJugadores; i++) {
            Socio socio = new Socio(nombre + i + "@club.co", "clave");
            socio.setFechaNacimiento(LocalDate.now().minusYears(14));
            Enum<?> posicion = deporte instanceof Futbol
                    ? Futbol.Posicion.DELANTERO : Baloncesto.Posicion.ALERO;
            new InscripcionEquipo().inscribirSocio(socio, equipo, posicion, i + 1);
        }
        return equipo;
    }
}
