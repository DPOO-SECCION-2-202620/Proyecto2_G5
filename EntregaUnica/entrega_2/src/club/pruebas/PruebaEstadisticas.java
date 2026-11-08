package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.deportes.Futbol;
import club.deportes.Tenis;
import club.deportes.TenisDeMesa;
import club.partidos.EstadisticaFutbol;
import club.partidos.EstadisticaTenis;
import club.partidos.EstadisticaTenisDeMesa;
import club.partidos.Partido;
import club.personal.Entrenador;
import club.usuarios.Socio;

public class PruebaEstadisticas {
    private Entrenador entrenador;
    private Socio socio;

    @BeforeEach
    void setUp() {
        Partido.getPartidos().clear();
        entrenador = new Entrenador("entrenador-estadisticas@club.co", "clave");
        socio = new Socio("jugador@club.co", "clave");
    }

    @Test
    void futbolRegistraGolesAsistenciasYTarjetas() {
        Partido partido = new Partido(entrenador, new Futbol(), List.of(socio));
        EstadisticaFutbol estadistica = new EstadisticaFutbol(socio, 2, 1, 1, false);

        partido.registrarEstadistica(entrenador, estadistica);

        assertEquals(2, estadistica.getGoles());
        assertEquals(1, estadistica.getAsistencias());
        assertEquals(1, estadistica.getAmarillas());
        assertFalse(estadistica.isRoja());
    }

    @Test
    void tenisPermiteSetsGamesAcesYErrores() {
        Partido partido = new Partido(entrenador, new Tenis(), List.of(socio));
        EstadisticaTenis estadistica = new EstadisticaTenis(socio, 2, 12, 4, 3);

        partido.registrarEstadistica(entrenador, estadistica);

        assertEquals(2, estadistica.getSetsGanados());
        assertEquals(12, estadistica.getGamesGanados());
        assertEquals(4, estadistica.getAces());
        assertEquals(3, estadistica.getErroresNoForzados());
    }

    @Test
    void tenisMesaPermiteSetsYPuntosTotales() {
        Partido partido = new Partido(entrenador, new TenisDeMesa(), List.of(socio));
        EstadisticaTenisDeMesa estadistica = new EstadisticaTenisDeMesa(socio, 3, 33);

        partido.registrarEstadistica(entrenador, estadistica);

        assertEquals(3, estadistica.getSetsGanados());
        assertEquals(33, estadistica.getPuntosTotales());
    }

    @Test
    void rechazaEstadisticaDeOtroDeporte() {
        Partido partido = new Partido(entrenador, new Futbol(), List.of(socio));

        assertThrows(IllegalArgumentException.class,
                () -> partido.registrarEstadistica(
                        entrenador, new EstadisticaTenis(socio, 2, 12, 4, 3)));
        assertTrue(partido.getEstadisticas().isEmpty());
    }

    @Test
    void dosAmarillasSuspendenSiguienteEncuentro() {
        Partido partido = new Partido(entrenador, new Futbol(), List.of(socio));

        partido.registrarEstadistica(
                entrenador, new EstadisticaFutbol(socio, 0, 0, 2, false));

        assertFalse(socio.estaHabilitado());
        assertEquals(1, socio.getSuspensionRestante());
    }

    @Test
    void tarjetaRojaSuspendeSiguienteEncuentro() {
        Partido partido = new Partido(entrenador, new Futbol(), List.of(socio));

        partido.registrarEstadistica(
                entrenador, new EstadisticaFutbol(socio, 0, 0, 0, true));

        assertFalse(socio.estaHabilitado());
        assertEquals(1, socio.getSuspensionesTotales());
    }
}
