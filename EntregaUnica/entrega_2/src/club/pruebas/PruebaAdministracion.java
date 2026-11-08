package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.deportes.Categoria;
import club.deportes.Equipo;
import club.deportes.Futbol;
import club.deportes.Deporte;
import club.deportes.Tenis;
import club.Inscripciones.InscripcionAcademia;
import club.personal.Entrenador;
import club.personal.Fisioterapeuta;
import club.personal.PlantillaTurnos;
import club.personal.SolicitudCambioTurno;
import club.personal.Turno;
import club.tienda.Articulo;
import club.tienda.EstadoSugerencia;
import club.tienda.GestorTienda;
import club.tienda.Inventario;
import club.tienda.SugerenciaProducto;
import club.tienda.TipoSugerencia;
import club.tienda.UbicacionInventario;
import club.usuarios.Administrador;
import club.usuarios.Socio;
import club.torneos.EstadoTorneo;
import club.torneos.CatalogoTorneos;
import club.torneos.PersonaExterna;
import club.torneos.TipoTorneo;
import club.torneos.TorneoInterno;

public class PruebaAdministracion {
	private Administrador administrador;
	private TorneoInterno torneo;
	private PersonaExterna externo1;
	private Tenis tenis;

	@BeforeEach
	void prepararTorneos() {
		CatalogoTorneos.limpiar();
		Deporte.getDeportesRegistrados().clear();
		administrador = new Administrador("admin-torneos@club.co", "clave");
		tenis = new Tenis();
		torneo = administrador.crearTorneo("Tenis", "Avanzado", "INDIVIDUAL",
				10, TipoTorneo.COMPETITIVO, 30000);
		externo1 = new PersonaExterna("EXT-1", "Participante externo", "Avanzado");
	}
	@Test void daDeBajaEquipo() {
		Administrador admin = new Administrador("admin-baja-equipo@club.co", "clave");
		Equipo equipo = new Equipo("Equipo de baja", Categoria.MAYORES, 10, new Futbol()) { };
		admin.darDeBaja(equipo);
		assertFalse(equipo.isActivo());
		assertTrue(Equipo.getEquiposRegistrados().containsValue(equipo));
	}

	@Test void daDeBajaSocio() {
		Administrador admin = new Administrador("admin-baja-socio@club.co", "clave");
		Socio socio = new Socio("socio-baja@club.co", "clave");
		admin.darDeBaja(socio);
		assertFalse(socio.isActivo());
	}

	@Test void trasladaExistenciaEntreUbicacionesSinAlterarTotal() {
		ContextoInventario contexto = inventario();
		contexto.gestor.trasladar(contexto.origen, contexto.destino, 4);
		assertEquals(10, contexto.origen.consultarCantidad() + contexto.destino.consultarCantidad());
		assertEquals(6, contexto.origen.consultarCantidad());
		assertEquals(4, contexto.destino.consultarCantidad());
	}

	@Test void rechazaTrasladoConStockInsuficiente() {
		ContextoInventario contexto = inventario();
		assertThrows(IllegalStateException.class,
				() -> contexto.gestor.trasladar(contexto.origen, contexto.destino, 11));
		assertEquals(10, contexto.origen.consultarCantidad());
		assertEquals(0, contexto.destino.consultarCantidad());
	}

	@Test void soloAdministradorApruebaSugerencias() {
		GestorTienda gestor = new GestorTienda();
		SugerenciaProducto sugerencia = new SugerenciaProducto("Producto", "Nuevo",
				TipoSugerencia.ARTICULO, new Entrenador("sugerente@club.co", "clave"));
		gestor.registrarSugerencia(sugerencia);
		assertThrows(IllegalArgumentException.class, () -> gestor.aprobarSugerencia(sugerencia, null));
		gestor.aprobarSugerencia(sugerencia, new Administrador("admin-sugerencia@club.co", "clave"));
		assertEquals(EstadoSugerencia.APROBADA, sugerencia.getEstado());
	}

	@Test void cambioDeTurnoNoPuedeReducirCoberturaBajoDosEntrenadoresYUnFisio() {
		ContextoTurnos contexto = turnos();
		Turno reducido = new Turno(contexto.primero.getEmpleado(),
				contexto.inicio.plusHours(1), contexto.fin);
		SolicitudCambioTurno solicitud = contexto.plantilla.solicitarCambio(contexto.primero, reducido);
		assertThrows(IllegalStateException.class,
				() -> contexto.plantilla.aprobarCambio(contexto.admin, solicitud));
		assertTrue(contexto.plantilla.getTurnos().contains(contexto.primero));
	}

	@Test void cambioConCoberturaSuficienteSeAprueba() {
		ContextoTurnos contexto = turnos();
		Turno reemplazo = new Turno(new Entrenador("reemplazo@club.co", "clave"), contexto.inicio, contexto.fin);
		SolicitudCambioTurno solicitud = contexto.plantilla.solicitarCambio(contexto.primero, reemplazo);
		contexto.plantilla.aprobarCambio(contexto.admin, solicitud);
		assertTrue(solicitud.isAprobada());
		assertTrue(contexto.plantilla.getTurnos().contains(reemplazo));
	}

	private ContextoInventario inventario() {
		GestorTienda gestor = new GestorTienda();
		Articulo articulo = new Articulo("Balón inventario", "Balón", 20, "Deporte");
		UbicacionInventario bodega = new UbicacionInventario("bodega-admin", "Bodega");
		UbicacionInventario tienda = new UbicacionInventario("tienda-admin", "Tienda");
		gestor.registrarProducto(articulo); gestor.registrarUbicacion(bodega); gestor.registrarUbicacion(tienda);
		Inventario origen = new Inventario(articulo, bodega, 0);
		Inventario destino = new Inventario(articulo, tienda, 0);
		gestor.reabastecer(origen, 10); gestor.reabastecer(destino, 1); destino.descontar(1);
		return new ContextoInventario(gestor, origen, destino);
	}

	private ContextoTurnos turnos() {
		LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
		LocalDateTime fin = inicio.plusHours(8);
		PlantillaTurnos plantilla = new PlantillaTurnos();
		Turno primero = new Turno(new Entrenador("entrenador1-turno@club.co", "clave"), inicio, fin);
		plantilla.agregarTurno(primero);
		plantilla.agregarTurno(new Turno(new Entrenador("entrenador2-turno@club.co", "clave"), inicio, fin));
		plantilla.agregarTurno(new Turno(new Fisioterapeuta("fisio-turno@club.co", "clave"), inicio, fin));
		return new ContextoTurnos(plantilla, primero, inicio, fin,
				new Administrador("admin-turno@club.co", "clave"));
	}

	private static class ContextoInventario {
		private GestorTienda gestor; private Inventario origen; private Inventario destino;
		private ContextoInventario(GestorTienda gestor, Inventario origen, Inventario destino) {
			this.gestor = gestor; this.origen = origen; this.destino = destino;
		}
	}

	private static class ContextoTurnos {
		private PlantillaTurnos plantilla; private Turno primero; private LocalDateTime inicio;
		private LocalDateTime fin; private Administrador admin;
		private ContextoTurnos(PlantillaTurnos plantilla, Turno primero, LocalDateTime inicio,
				LocalDateTime fin, Administrador admin) {
			this.plantilla = plantilla; this.primero = primero; this.inicio = inicio; this.fin = fin; this.admin = admin;
		}
	}
	
	// Nuevos tests
	

	@Test
	void testCrearTorneoInterno() {
	    // HU-ADMIN-01: Crear torneo
	    TorneoInterno torneo = administrador.crearTorneo("Tenis", "Intermedio", "INDIVIDUAL",10, TipoTorneo.RECREATIVO, 0);
	
	    assertNotNull(torneo);
	    assertEquals(10, torneo.getCupos());
	    assertEquals(TipoTorneo.RECREATIVO, torneo.getTipo());
	}
	
	@Test
	void testCrearTorneoCompetitivo() {
	    // HU-ADMIN-01: Tarifa de torneo competitivo
	    TorneoInterno torneo = administrador.crearTorneo("Tenis", "Avanzado", "INDIVIDUAL",10, TipoTorneo.COMPETITIVO, 30000);
	
	    assertEquals(30000, torneo.getTarifa());
	}
	
	@Test
	void testCuposPreferenciales() {
	    // HU-ADMIN-01: Reservar el 20% redondeado arriba
	    TorneoInterno torneo = administrador.crearTorneo("Tenis", "Avanzado", "INDIVIDUAL", 13, TipoTorneo.COMPETITIVO, 30000);
	
	    assertEquals(3, torneo.getCuposPreferenciales());
	}

	@Test
	void testInscribirPersonaExterna() {
	    // HU-ADMIN-02: Inscripcion exitosa
	    administrador.inscribirExterno(torneo, externo1);
	
	    assertTrue(torneo.estaInscrito(externo1));
	}
	
	@Test
	void testInscribirExternoDuplicado() {
	    // HU-ADMIN-02: Rechazar duplicados
	    administrador.inscribirExterno(torneo, externo1);
	
	    assertThrows(IllegalStateException.class, () ->{ administrador.inscribirExterno(torneo, externo1);});
	}

	@Test
	void testInscribirExternoSinCupos()
	{
	    // HU-ADMIN-02: Rechazar por falta de cupos
	    llenarTorneoConInscripciones();
	
	    assertThrows(IllegalStateException.class, () ->{administrador.inscribirExterno(torneo, externo1);});
	}

	private void llenarTorneoConInscripciones() {
		int cuposGenerales = torneo.getCuposDisponibles() - torneo.getCuposPreferenciales();
		for (int i = 0; i < cuposGenerales; i++) {
			administrador.inscribirExterno(torneo,
					new PersonaExterna("EXT-LLENO-" + i, "Externo " + i, "Avanzado"));
		}
		int preferenciales = torneo.getCuposPreferenciales();
		for (int i = 0; i < preferenciales; i++) {
			Socio socio = new Socio("preferencial-" + i + "@club.co", "clave");
			InscripcionAcademia inscripcion = InscripcionAcademia.inscribirSocio(socio, tenis);
			inscripcion.actualizarElo(100 + i);
			torneo.inscribir(socio);
		}
	}
	
	@Test
	void testInscribirExternoCategoriaIncorrecta()
	{
	    // HU-ADMIN-02: Validar categoria del participante
	    externo1.setCategoria("Principiante");
	
	    assertThrows(IllegalArgumentException.class, () ->{administrador.inscribirExterno(torneo, externo1);});
	}

	@Test
	void testConsultarTorneos() {
	    // HU-ADMIN-03: Consultar torneos registrados
	    assertFalse(administrador.getTorneos().isEmpty());
	}
	
	@Test
	void testConsultarEstadoTorneo() {
	    // HU-ADMIN-03: Consultar estado
	    assertEquals(EstadoTorneo.PENDIENTE, torneo.getEstado());
	}
	
	@Test
	void testConsultarHistorialTorneo() {
	    // HU-ADMIN-03: Consultar historial
	    assertNotNull(administrador.consultarHistorial(torneo.getId()));
	}
	
	@Test
	void testConsultarTorneoInexistente() {
	    // HU-ADMIN-03: Torneo no encontrado
	    assertNull(administrador.buscarTorneo(-1));
	}

}
