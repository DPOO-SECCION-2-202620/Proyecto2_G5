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
	private Equipo equipoFutbol;
	private Equipo equipoBaloncesto;
	private Socio jugadorSancionado;

    @BeforeEach
    void setUp() {
        Equipo.getEquiposDisponibles().clear();
		Equipo.getEquiposRegistrados().clear();
        equipo = new Equipo("Tigres", Categoria.SUB15, 20, new Futbol()) { };
        socio = crearSocio("socio@club.co");
		Futbol futbol = new Futbol();
		equipoFutbol = new Equipo("Fútbol habilitados", Categoria.SUB15, 8, futbol) { };
		for (int i = 0; i < 7; i++) {
			Socio jugador = crearSocio("futbol-habilitado-" + i + "@club.co");
			new InscripcionEquipo().inscribirSocio(
					jugador, equipoFutbol, Futbol.Posicion.DELANTERO, i + 1);
		}
		jugadorSancionado = equipoFutbol.getNomina().get(0).getSocio();
		Baloncesto baloncesto = new Baloncesto();
		equipoBaloncesto = new Equipo("Baloncesto habilitados", Categoria.SUB15, 6, baloncesto) { };
		for (int i = 0; i < 5; i++) {
			Socio jugador = crearSocio("baloncesto-habilitado-" + i + "@club.co");
			new InscripcionEquipo().inscribirSocio(
					jugador, equipoBaloncesto, Baloncesto.Posicion.ALERO, i + 1);
		}
		equipoBaloncesto.getNomina().get(0).getSocio().sancionar("Sanción vigente");
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
    
    
    // Nuevos Tests
    

	@Test
	void testFutbolMinimoJugadores()
	{
	    // HU-ENT-02 CA-1: Futbol con 7 habilitados
	    assertEquals(7, equipoFutbol.getJugadoresHabilitados().size());

	    assertTrue(equipoFutbol.puedeDisputarPartido());
	}

	@Test
	void testBaloncestoJugadoresInsuficientes()
	{
	    // HU-ENT-02 CA-2: Baloncesto con 4 habilitados
	    assertEquals(4, equipoBaloncesto.getJugadoresHabilitados().size());

	    assertFalse(equipoBaloncesto.puedeDisputarPartido());

	    assertFalse(equipoBaloncesto.getJugadoresNoDisponibles().isEmpty());
	}

	@Test
	void testJugadorSancionadoNoHabilitado()
	{
	    // HU-ENT-02 CA-3: Excluir sancionados
	    jugadorSancionado.sancionar("Sancion vigente");

	    assertFalse(equipoFutbol.getJugadoresHabilitados()
	            .contains(jugadorSancionado));
	}

    private Socio crearSocio(String email) {
        Socio nuevoSocio = new Socio(email, "clave");
        nuevoSocio.setFechaNacimiento(LocalDate.now().minusYears(14));
        return nuevoSocio;
    }
}
