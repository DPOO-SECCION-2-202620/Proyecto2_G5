package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.tienda.BonoDescuento;
import club.usuarios.Socio;

public class PruebaBonosDescuento {
	private Socio jugador1;
	private BonoDescuento bono1;

	@BeforeEach
	void setUp() {
		BonoDescuento.limpiar();
		jugador1 = new Socio("jugador-bono@club.co", "clave");
		bono1 = new BonoDescuento(jugador1, 10000);
	}

	@Test
	void testGuardarBono() {
	    // HU-JUG-03: Guardar bono ganado
	    jugador1.guardarBono(bono1);
	
	    assertTrue(jugador1.getBonos().contains(bono1));
	}
	
	@Test
	void testConsultarBonos() {
	    // HU-JUG-03: Consultar bonos disponibles
	    jugador1.guardarBono(bono1);
	
	    assertTrue(jugador1.getBonosDisponibles().contains(bono1));
	    assertEquals(10000, bono1.getValor());
	}


}
