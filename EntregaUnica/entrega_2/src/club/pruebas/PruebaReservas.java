package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.deportes.Modalidad;
import club.deportes.Padel;
import club.deportes.Tenis;
import club.instalaciones.Instalacion;
import club.instalaciones.Reserva;
import club.usuarios.Socio;

public class PruebaReservas {
    private Socio socio1;
    private Socio socio2;

    @BeforeEach
    void setUp() {
        Instalacion.getInstalaciones().clear();
        Reserva.getReservas().clear();
        socio1 = new Socio("socio1-reserva@club.co", "clave");
        socio2 = new Socio("socio2-reserva@club.co", "clave");
    }

    @Test
    void reservaValidaDevuelvePrimerSocioComoResponsable() {
        Instalacion cancha = new Instalacion(
                "Cancha de tenis", new Tenis(), List.of(Modalidad.SENCILLOS, Modalidad.DOBLES));

        Reserva reserva = new Reserva(List.of(socio1, socio2), cancha,
                LocalDateTime.now().plusDays(1), Duration.ofHours(1), Modalidad.SENCILLOS);

        assertSame(socio1, reserva.getResponsable());
        assertEquals(2, reserva.getCantidadJugadores());
    }

    @Test
    void padelNoAceptaSencillos() {
        Instalacion cancha = new Instalacion(
                "Cancha de pádel", new Padel(), List.of(Modalidad.DOBLES));

        assertThrows(IllegalArgumentException.class,
                () -> new Reserva(List.of(socio1, socio2), cancha,
                        LocalDateTime.now().plusDays(1), Duration.ofHours(1), Modalidad.SENCILLOS));
        assertTrue(Reserva.getReservas().isEmpty());
    }

    @Test
    void rechazaSuperposicionHoraria() {
        Instalacion cancha = new Instalacion(
                "Cancha", new Tenis(), List.of(Modalidad.SENCILLOS));
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        new Reserva(List.of(socio1, socio2), cancha, inicio,
                Duration.ofHours(1), Modalidad.SENCILLOS);

        assertThrows(IllegalStateException.class,
                () -> new Reserva(List.of(socio1, socio2), cancha,
                        inicio.plusMinutes(30), Duration.ofHours(1), Modalidad.SENCILLOS));
        assertEquals(1, Reserva.getReservas().size());
    }

    @Test
    void rechazaCupoSimultaneoAlcanzado() {
        Instalacion primera = new Instalacion(
                "Cancha 1", new Tenis(), List.of(Modalidad.SENCILLOS));
        Instalacion segunda = new Instalacion(
                "Cancha 2", new Tenis(), List.of(Modalidad.SENCILLOS));
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        new Reserva(List.of(socio1, socio2), primera, inicio,
                Duration.ofHours(1), Modalidad.SENCILLOS);

        assertDoesNotThrow(
                () -> new Reserva(List.of(socio1, socio2), segunda,
                        inicio, Duration.ofHours(1), Modalidad.SENCILLOS));
        assertEquals(2, Reserva.getReservas().size());
    }
}
