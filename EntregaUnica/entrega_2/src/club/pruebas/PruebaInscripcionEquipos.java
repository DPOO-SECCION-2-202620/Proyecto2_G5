package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.Inscripciones.InscripcionEquipo;
import club.Inscripciones.InscripcionAcademia;
import club.deportes.Categoria;
import club.deportes.Equipo;
import club.deportes.Futbol;
import club.deportes.Tenis;
import club.personal.Entrenador;
import club.personal.Turno;
import club.usuarios.Administrador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;
import club.torneos.InscripcionTorneo;
import club.torneos.CatalogoTorneos;
import club.torneos.TipoTorneo;
import club.torneos.TorneoInterno;
import club.torneos.Torneo;

import java.time.LocalDateTime;

public class PruebaInscripcionEquipos {
    private Equipo equipo;
    private Futbol futbol;
    private Socio socio;
	private TorneoInterno torneo;
	private TorneoInterno torneoDobles;
	private TorneoInterno torneoCompetitivo;
	private Socio jugador1;
	private Socio jugador2;
	private Socio jugadorRanking;
	private Entrenador empleadoDeTurno;
	private Entrenador empleadoLibre;
	private Equipo equipo1;
	private Equipo equipo2;
	private Equipo equipoSub13;
	private Socio socio1;
	private Socio socio2;
	private Socio socioMayor;

    @BeforeEach
    void setUp() {
		CatalogoTorneos.limpiar();
        Equipo.getEquiposDisponibles().clear();
		Equipo.getEquiposRegistrados().clear();
        RepositorioUsuarios.getRepositorioUsuarios().clear();
        futbol = new Futbol();
        equipo = crearEquipo("Tigres", Categoria.SUB15, 20, futbol);
        socio = new Socio("socio@club.co", "clave");
        socio.setFechaNacimiento(LocalDate.now().minusYears(14));
		InscripcionAcademia.getInscripciones().clear();
		Administrador administrador = new Administrador("admin-equipos@club.co", "clave");
		Tenis tenis = new Tenis();
		jugador1 = inscribir("jugador1-equipos@club.co", tenis);
		jugador2 = inscribir("jugador2-equipos@club.co", tenis);
		jugadorRanking = inscribir("ranking-equipos@club.co", tenis);
		InscripcionAcademia.buscar(jugador1, tenis).actualizarElo(90);
		InscripcionAcademia.buscar(jugador2, tenis).actualizarElo(90);
		InscripcionAcademia.buscar(jugadorRanking, tenis).actualizarElo(200);
		empleadoDeTurno = new Entrenador("empleado-turno-equipos@club.co", "clave");
		empleadoLibre = new Entrenador("empleado-libre-equipos@club.co", "clave");
		LocalDateTime fechaTorneo = LocalDateTime.now();
		administrador.getPlantillaTurnos().agregarTurno(new Turno(empleadoDeTurno,
				fechaTorneo.minusHours(1), fechaTorneo.plusHours(1)));
		torneo = administrador.crearTorneo("Tenis", "Avanzado", "INDIVIDUAL",
				10, TipoTorneo.RECREATIVO, 0, Torneo.Formato.LIGA, fechaTorneo);
		torneoDobles = administrador.crearTorneo("Tenis", "Avanzado", "DOBLES",
				10, TipoTorneo.RECREATIVO, 0, Torneo.Formato.LIGA, fechaTorneo);
		torneoCompetitivo = administrador.crearTorneo("Tenis", "Avanzado", "INDIVIDUAL",
				10, TipoTorneo.COMPETITIVO, 30000, Torneo.Formato.LIGA, fechaTorneo);
		Futbol futbolHistorias = new Futbol();
		equipo1 = crearEquipo("Equipo HU SOC 1", Categoria.SUB15, 20, futbolHistorias);
		equipo2 = crearEquipo("Equipo HU SOC 2", Categoria.SUB15, 20, futbolHistorias);
		equipoSub13 = crearEquipo("Equipo HU SUB13", Categoria.SUB13, 20, futbolHistorias);
		socio1 = new Socio("socio1-hu@club.co", "clave");
		socio1.setFechaNacimiento(LocalDate.now().minusYears(14));
		socio2 = new Socio("socio2-hu@club.co", "clave");
		socio2.setFechaNacimiento(LocalDate.now().minusYears(14));
		socioMayor = new Socio("socio-mayor-hu@club.co", "clave");
		socioMayor.setFechaNacimiento(LocalDate.now().minusYears(20));
    }

    @Test
    void equipoNuevoTieneCupos() {
        assertEquals(20, equipo.getCuposDisponibles());
    }

    @Test
    void rechazaEdadIncompatible() {
        assertFalse(equipo.getCategoria().admiteEdad(LocalDate.of(1990, 1, 1)));
    }

    @Test
    void equipoIniciaSinInscripciones() {
        assertTrue(equipo.getNomina().isEmpty());
    }

    @Test
    void consultaDisponibilidadCamisetaEnEquipoVacio() {
        assertTrue(equipo.getNomina().stream()
                .noneMatch(inscripcion -> inscripcion.getNumeroCamiseta() == 10));
    }

    @Test
    void inscribeSocioConEdadYCupo() {
        new InscripcionEquipo().inscribirSocio(
                socio, equipo, Futbol.Posicion.DELANTERO, 10);

        assertEquals(19, equipo.getCuposDisponibles());
        assertEquals(1, equipo.getNomina().size());
    }

    @Test
    void rechazaSegundoEquipoMismoDeporte() {
        Equipo segundoEquipo = crearEquipo("Leones", Categoria.SUB15, 20, new Futbol());
        new InscripcionEquipo().inscribirSocio(
                socio, equipo, Futbol.Posicion.DELANTERO, 10);

        assertThrows(IllegalStateException.class,
                () -> new InscripcionEquipo().inscribirSocio(
                        socio, segundoEquipo, Futbol.Posicion.PORTERO, 1));
    }

    @Test
    void rechazaEquipoSinCupos() {
        Equipo equipoLleno = crearEquipo("Lleno", Categoria.SUB15, 0, futbol);

        assertThrows(IllegalStateException.class,
                () -> new InscripcionEquipo().inscribirSocio(
                        socio, equipoLleno, Futbol.Posicion.DEFENSA, 4));
    }

    private Equipo crearEquipo(String nombre, Categoria categoria, int cupos, Futbol deporte) {
        return new Equipo(nombre, categoria, cupos, deporte) { };
    }

	private Socio inscribir(String email, Tenis tenis) {
		Socio jugador = new Socio(email, "clave");
		InscripcionAcademia.inscribirSocio(jugador, tenis);
		return jugador;
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
	void testInscribirSocioEquipo()
	{
	    // HU-SOC-02 CA-1: Inscripcion valida
	    int cupos = equipo1.getCuposDisponibles();

	    equipo1.inscribirSocio(socio1, 10, "Delantero");

	    assertTrue(equipo1.contieneJugador(socio1));
	    assertEquals(cupos - 1, equipo1.getCuposDisponibles());
	    assertEquals(10, equipo1.getDorsal(socio1));
	    assertEquals("Delantero", equipo1.getPosicion(socio1));
	}

	@Test
	void testInscripcionCategoriaIncorrecta()
	{
	    // HU-SOC-02 CA-2: Edad incompatible
	    assertThrows(IllegalArgumentException.class, () ->
	    {
	        equipoSub13.inscribirSocio(socioMayor, 8, "Defensa");
	    });
	}

	@Test
	void testInscripcionDorsalOcupado()
	{
	    // HU-SOC-02 CA-3: Dorsal repetido
	    equipo1.inscribirSocio(socio1, 10, "Delantero");

	    assertThrows(IllegalArgumentException.class, () ->
	    {
	        equipo1.inscribirSocio(socio2, 10, "Defensa");
	    });
	}

	@Test
	void testInscripcionOtroEquipoMismoDeporte()
	{
	    // HU-SOC-02 CA-4: Un equipo por disciplina
	    equipo1.inscribirSocio(socio1, 10, "Delantero");

	    assertThrows(IllegalStateException.class, () ->
	    {
	        equipo2.inscribirSocio(socio1, 7, "Mediocampista");
	    });
	}

}
