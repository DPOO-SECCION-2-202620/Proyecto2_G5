package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.Inscripciones.InscripcionEquipo;
import club.competencias.Competencia;
import club.competencias.FormatoCompetencia;
import club.competencias.PartidoCompetencia;
import club.deportes.Categoria;
import club.deportes.Equipo;
import club.deportes.Futbol;
import club.partidos.EstadisticaFutbol;
import club.partidos.Partido;
import club.personal.Entrenador;
import club.usuarios.Administrador;
import club.usuarios.Socio;

public class PruebaCompetencias {
    private Administrador administrador;
    private Entrenador entrenador;
    private Futbol futbol;

    @BeforeEach
    void setUp() {
        Competencia.getCompetencias().clear();
        Equipo.getEquiposDisponibles().clear();
        Equipo.getEquiposRegistrados().clear();
        Partido.getPartidos().clear();
        administrador = new Administrador("admin-competencia@club.co", "clave");
        entrenador = new Entrenador("entrenador-competencia@club.co", "clave");
        futbol = new Futbol();
    }

    @Test
    void permiteEquipoConNominaYCategoriaValidas() {
        Competencia competencia = competencia("ASCUN", FormatoCompetencia.LIGA);
        Equipo equipo = equipoConJugador("Tigres", Categoria.SUB15, 1);

        administrador.inscribirEquipoCompetencia(competencia, equipo);

        assertTrue(competencia.getEquipos().contains(equipo));
    }

    @Test
    void rechazaEquipoConCategoriaIncompatible() {
        Competencia competencia = competencia("ASCUN", FormatoCompetencia.LIGA);
        Equipo equipo = equipoConJugador("Mayores", Categoria.MAYORES, 2);

        assertThrows(IllegalArgumentException.class,
                () -> administrador.inscribirEquipoCompetencia(competencia, equipo));
        assertTrue(competencia.getEquipos().isEmpty());
    }

    @Test
    void cerrosOrdenaTablaDePosiciones() {
        Competencia competencia = competencia("CERROS", FormatoCompetencia.LIGA);
        Equipo ganador = equipoConJugador("Ganador", Categoria.SUB15, 3);
        Equipo perdedor = equipoConJugador("Perdedor", Categoria.SUB15, 4);
        administrador.inscribirEquipoCompetencia(competencia, ganador);
        administrador.inscribirEquipoCompetencia(competencia, perdedor);
        PartidoCompetencia partido = administrador.programarPartidoCompetencia(
                competencia, ganador, perdedor, LocalDateTime.now().plusDays(1), entrenador);

        partido.registrarResultado(entrenador, 2, 1);

        assertEquals(ganador, competencia.getTablaPosiciones().get(0));
        assertEquals(3, competencia.getPuntos(ganador));
    }

    @Test
    void ascunGeneraEnfrentamientosDeEliminacion() {
        Competencia competencia = competencia("ASCUN", FormatoCompetencia.FASES);
        administrador.inscribirEquipoCompetencia(competencia, equipoConJugador("A", Categoria.SUB15, 5));
        administrador.inscribirEquipoCompetencia(competencia, equipoConJugador("B", Categoria.SUB15, 6));

        List<PartidoCompetencia> enfrentamientos = competencia.generarEnfrentamientos(entrenador);

        assertEquals(1, enfrentamientos.size());
        assertSame(competencia, enfrentamientos.get(0).getCompetencia());
    }

    @Test
    void rankingAcumulaEstadisticasPorCompetencia() {
        Competencia competencia = competencia("CERROS", FormatoCompetencia.LIGA);
        Equipo local = equipoConJugador("Local", Categoria.SUB15, 7);
        Equipo visitante = equipoConJugador("Visitante", Categoria.SUB15, 8);
        administrador.inscribirEquipoCompetencia(competencia, local);
        administrador.inscribirEquipoCompetencia(competencia, visitante);
        PartidoCompetencia partido = administrador.programarPartidoCompetencia(
                competencia, local, visitante, LocalDateTime.now().plusDays(1), entrenador);
        Socio jugador = local.getNomina().get(0).getSocio();

        partido.registrarEstadistica(entrenador, new EstadisticaFutbol(jugador, 1, 0, 0, false));

        assertEquals(1, competencia.getRankingEstadistica().size());
        assertSame(jugador, competencia.getRankingEstadistica().get(0).getSocio());
    }

    private Competencia competencia(String nombre, FormatoCompetencia formato) {
        return new Competencia(nombre, "Universidad", 2026, "2026-2", formato,
                futbol, List.of(Categoria.SUB15), 1);
    }

    private Equipo equipoConJugador(String nombre, Categoria categoria, int camiseta) {
        Equipo equipo = new Equipo(nombre, categoria, 2, futbol) { };
        Socio socio = new Socio(nombre.toLowerCase() + "@club.co", "clave");
        socio.setFechaNacimiento(LocalDate.now().minusYears(categoria == Categoria.MAYORES ? 20 : 14));
        new InscripcionEquipo().inscribirSocio(
                socio, equipo, Futbol.Posicion.DELANTERO, camiseta);
        return equipo;
    }
}
