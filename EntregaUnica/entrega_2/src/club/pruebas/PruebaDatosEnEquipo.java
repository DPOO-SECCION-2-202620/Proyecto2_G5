package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.Inscripciones.InscripcionEquipo;
import club.deportes.Baloncesto;
import club.deportes.Categoria;
import club.deportes.Equipo;
import club.deportes.Futbol;
import club.usuarios.Socio;

public class PruebaDatosEnEquipo {
    private Equipo equipo;
    private Socio socio;

    @BeforeEach
    void setUp() {
        Equipo.getEquiposDisponibles().clear();
        equipo = new Equipo("Tigres", Categoria.SUB15, 20, new Futbol()) { };
        socio = crearSocio("socio@club.co");
    }

    @Test
    void asignarCamisetaYPosicion() {
        InscripcionEquipo inscripcion = new InscripcionEquipo();
        inscripcion.inscribirSocio(socio, equipo, Futbol.Posicion.DELANTERO, 10);

        assertEquals(10, inscripcion.getNumeroCamiseta());
        assertEquals(Futbol.Posicion.DELANTERO, inscripcion.getPosicion());
    }

    @Test
    void rechazarNumeroYaEnUso() {
        new InscripcionEquipo().inscribirSocio(
                socio, equipo, Futbol.Posicion.DELANTERO, 10);

        assertThrows(IllegalArgumentException.class,
                () -> new InscripcionEquipo().inscribirSocio(
                        crearSocio("otro@club.co"), equipo, Futbol.Posicion.PORTERO, 10));
    }

    @Test
    void rechazarPosicionIncompatible() {
        assertThrows(IllegalArgumentException.class,
                () -> new InscripcionEquipo().inscribirSocio(
                        socio, equipo, Baloncesto.Posicion.ALERO, 10));
    }

    private Socio crearSocio(String email) {
        Socio nuevoSocio = new Socio(email, "clave");
        nuevoSocio.setFechaNacimiento(LocalDate.now().minusYears(14));
        return nuevoSocio;
    }
}
