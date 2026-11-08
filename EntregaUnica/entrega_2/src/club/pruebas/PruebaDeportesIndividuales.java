package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.Inscripciones.InscripcionAcademia;
import club.deportes.Deporte;
import club.deportes.Nivel;
import club.deportes.Modalidad;
import club.deportes.Tenis;
import club.partidos.PartidoPractica;
import club.personal.Entrenador;
import club.personal.Turno;
import club.usuarios.Administrador;
import club.usuarios.Socio;
import club.usuarios.Usuario;
import club.torneos.InscripcionTorneo;
import club.torneos.CatalogoTorneos;
import club.torneos.TipoTorneo;
import club.torneos.TorneoInterno;
import club.torneos.Torneo;

public class PruebaDeportesIndividuales {
	private TorneoInterno torneo;
	private TorneoInterno torneoDobles;
	private TorneoInterno torneoCompetitivo;
	private Socio jugador1;
	private Socio jugador2;
	private Socio jugadorRanking;
	private Entrenador empleadoDeTurno;
	private Entrenador empleadoLibre;
	private Tenis tenis;

    @BeforeEach
    void setUp() {
		CatalogoTorneos.limpiar();
        Deporte.getDeportesRegistrados().clear();
        InscripcionAcademia.getInscripciones().clear();
        PartidoPractica.getPartidos().clear();
		Administrador administrador = new Administrador("admin-individual@club.co", "clave");
		tenis = new Tenis();
		jugador1 = socioInscrito("jugador1@club.co", tenis);
		jugador2 = socioInscrito("jugador2@club.co", tenis);
		jugadorRanking = socioInscrito("ranking@club.co", tenis);
		InscripcionAcademia.buscar(jugador1, tenis).actualizarElo(90);
		InscripcionAcademia.buscar(jugador2, tenis).actualizarElo(90);
		InscripcionAcademia.buscar(jugadorRanking, tenis).actualizarElo(200);
		empleadoDeTurno = new Entrenador("empleado-turno@club.co", "clave");
		empleadoLibre = new Entrenador("empleado-libre@club.co", "clave");
		LocalDateTime fechaTorneo = LocalDateTime.now();
		administrador.getPlantillaTurnos().agregarTurno(new Turno(empleadoDeTurno,
				fechaTorneo.minusHours(1), fechaTorneo.plusHours(1)));
		torneo = administrador.crearTorneo("Tenis", "Avanzado", "INDIVIDUAL",
				10, TipoTorneo.RECREATIVO, 0, Torneo.Formato.LIGA, fechaTorneo);
		torneoDobles = administrador.crearTorneo("Tenis", "Avanzado", "DOBLES",
				10, TipoTorneo.RECREATIVO, 0, Torneo.Formato.LIGA, fechaTorneo);
		torneoCompetitivo = administrador.crearTorneo("Tenis", "Avanzado", "INDIVIDUAL",
				10, TipoTorneo.COMPETITIVO, 30000, Torneo.Formato.LIGA, fechaTorneo);
    }

    @Test void inscripcionIniciaConNivelPrincipianteYEloCero() {
        Socio socio = new Socio("socio@club.co", "clave");
        InscripcionAcademia inscripcion = InscripcionAcademia.inscribirSocio(socio, new Tenis());

        assertEquals(Nivel.PRINCIPIANTE, inscripcion.getNivel());
        assertEquals(0.0, inscripcion.getElo(), 0.00001);
    }
    @Test
    void permiteParticipantesDeNivelesAdyacentes() {
        Tenis tenis = new Tenis();
        Socio principiante = socioInscrito("principiante@club.co", tenis);
        Socio intermedio = socioInscrito("intermedio@club.co", tenis);
        InscripcionAcademia.buscar(intermedio, tenis).actualizarElo(30);

        PartidoPractica partido = new PartidoPractica(
                tenis, Modalidad.SENCILLOS, List.of(principiante, intermedio));

        assertEquals(2, partido.getParticipantes().size());
    }

    @Test
    void rechazaPrincipianteContraAvanzado() {
        Tenis tenis = new Tenis();
        Socio principiante = socioInscrito("principiante@club.co", tenis);
        Socio avanzado = socioInscrito("avanzado@club.co", tenis);
        InscripcionAcademia.buscar(avanzado, tenis).actualizarElo(90);

        assertThrows(IllegalArgumentException.class,
                () -> new PartidoPractica(
                        tenis, Modalidad.SENCILLOS, List.of(principiante, avanzado)));
    }

    @Test
    void permiteExcepcionDeEntrenadorJugador() {
        Tenis tenis = new Tenis();
        Socio avanzado = socioInscrito("avanzado@club.co", tenis);
        InscripcionAcademia.buscar(avanzado, tenis).actualizarElo(90);
        Entrenador entrenador = new Entrenador("entrenador-jugador@club.co", "clave");

        PartidoPractica partido = new PartidoPractica(tenis, Modalidad.SENCILLOS,
                List.<Usuario>of(entrenador, avanzado));

        assertTrue(partido.getParticipantes().contains(entrenador));
    }

    @Test
    void actualizaEloUnaSolaVezAlFinalizarPartido() {
        Tenis tenis = new Tenis();
        Socio ganador = socioInscrito("ganador@club.co", tenis);
        Socio perdedor = socioInscrito("perdedor@club.co", tenis);
        PartidoPractica partido = new PartidoPractica(
                tenis, Modalidad.SENCILLOS, List.of(ganador, perdedor));

        partido.finalizar(10, 5);

        assertEquals(15, InscripcionAcademia.buscar(ganador, tenis).getElo(), 0.00001);
        assertEquals(3, InscripcionAcademia.buscar(perdedor, tenis).getElo(), 0.00001);
        assertThrows(IllegalStateException.class, () -> partido.finalizar(10, 5));
        assertEquals(15, InscripcionAcademia.buscar(ganador, tenis).getElo(), 0.00001);
    }

    private Socio socioInscrito(String email, Tenis tenis) {
        Socio socio = new Socio(email, "clave");
        InscripcionAcademia.inscribirSocio(socio, tenis);
        return socio;
    }

    // Nuevos Tests
    
	
	@Test
	void testInscripcionIndividual() {
	    // HU-JUG-01: Inscripcion valida
	    torneo.inscribir(jugador1);
	
	    assertTrue(torneo.estaInscrito(jugador1));
	}
	
	@Test
	void testInscripcionDobles() {
	    // HU-JUG-01: Una pareja ocupa un cupo
	    int cupos = torneoDobles.getCuposDisponibles();
	
	    torneoDobles.inscribirPareja(jugador1, jugador2);
	
	    assertEquals(cupos - 1, torneoDobles.getCuposDisponibles());
	    assertTrue(torneoDobles.estaInscrito(jugador1));
	    assertTrue(torneoDobles.estaInscrito(jugador2));
	}
	
	@Test
	void testInscripcionPreferencial() {
	    // HU-JUG-01: Usar cupo preferencial
	    torneo.inscribirPreferencial(jugadorRanking);
	
	    assertTrue(torneo.estaInscrito(jugadorRanking));
	    assertTrue(torneo.getInscripcion(jugadorRanking).esPreferencial());
	}
	
	@Test
	void testInscripcionDuplicada() {
	    // HU-JUG-01: Rechazar jugador repetido
	    torneo.inscribir(jugador1);
	
	    assertThrows(IllegalStateException.class, () ->{torneo.inscribir(jugador1);});
	}
	
	@Test
	void testEmpleadoDeTurno() {
	    // HU-JUG-01: Empleado trabajando no puede inscribirse
	    assertThrows(IllegalStateException.class, () ->{torneo.inscribir(empleadoDeTurno);});
	}
	
	@Test
	void testEmpleadoSinTurno() {
	    // HU-JUG-01: Empleado sin tarifa ni premio
	    torneoCompetitivo.inscribir(empleadoLibre);
	
	    InscripcionTorneo inscripcion = torneoCompetitivo.getInscripcion(empleadoLibre);
	
	    assertEquals(0, inscripcion.getTarifaPagada());
	    assertFalse(inscripcion.puedeRecibirPremio());
	}
	
	@Test
	void testInscripcionTorneoIniciado() {
	    // HU-JUG-01: Inscripcion fuera de plazo
	    torneo.iniciar();
	
	    assertThrows(IllegalStateException.class, () ->{torneo.inscribir(jugador1);});
	}
	

	@Test
	void testRetirarJugador() {
	    // HU-JUG-02: Retiro exitoso
	    torneo.inscribir(jugador1);
	    torneo.retirar(jugador1);
	
	    assertFalse(torneo.estaInscrito(jugador1));
	}
	
	@Test
	void testRetiroLiberaCupo() {
	    // HU-JUG-02: Liberar cupo
	    int cupos = torneo.getCuposDisponibles();
	
	    torneo.inscribir(jugador1);
	    torneo.retirar(jugador1);
	
	    assertEquals(cupos, torneo.getCuposDisponibles());
	}
	
	@Test
	void testRetiroTorneoIniciado() {
	    // HU-JUG-02: No retirar despues del inicio
	    torneo.inscribir(jugador1);
	    torneo.iniciar();
	
	    assertThrows(IllegalStateException.class, () ->{torneo.retirar(jugador1);});
	}

}
