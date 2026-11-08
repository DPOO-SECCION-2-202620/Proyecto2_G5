package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.Inscripciones.InscripcionAcademia;
import club.deportes.Modalidad;
import club.deportes.Tenis;
import club.entrenamientos.EstadoAsistencia;
import club.entrenamientos.SesionEntrenamiento;
import club.instalaciones.Instalacion;
import club.instalaciones.Reserva;
import club.partidos.Partido;
import club.personal.Entrenador;
import club.reportes.ConsultasAdministrativas;
import club.reportes.PeriodoReporte;
import club.reportes.ReporteDeportivo;
import club.reportes.RubroVenta;
import club.tienda.Articulo;
import club.tienda.DetalleVenta;
import club.tienda.GestorTienda;
import club.tienda.Inventario;
import club.tienda.UbicacionInventario;
import club.tienda.VentaTienda;
import club.usuarios.Administrador;
import club.usuarios.Socio;

public class PruebaReportes {
	private Administrador administrador;
	private ConsultasAdministrativas consultas;

	@BeforeEach
	void setUp() {
		Reserva.getReservas().clear(); Partido.getPartidos().clear();
		SesionEntrenamiento.getSesiones().clear(); InscripcionAcademia.getInscripciones().clear();
		administrador = new Administrador("admin-reportes@club.co", "clave");
		consultas = new ConsultasAdministrativas();
	}

	@Test void consultaReservasPartidosYSesionesSinModificarDatos() {
		Tenis tenis = new Tenis(); Entrenador entrenador = new Entrenador("entrenador-reporte@club.co", "clave");
		Socio socio = new Socio("socio-reporte@club.co", "clave");
		Instalacion instalacion = new Instalacion("Cancha reporte", tenis, List.of(Modalidad.SENCILLOS));
		new Reserva(List.of(socio, new Socio("rival-reporte@club.co", "clave")), instalacion,
				LocalDateTime.now().plusDays(1), Duration.ofHours(1), Modalidad.SENCILLOS);
		new Partido(entrenador, tenis, List.of(socio));
		new SesionEntrenamiento(entrenador, tenis, instalacion,
				LocalDateTime.now().plusDays(2), Duration.ofHours(1));
		assertEquals(1, consultas.consultarReservas(administrador).size());
		assertEquals(1, consultas.consultarPartidos(administrador).size());
		assertEquals(1, consultas.consultarSesiones(administrador).size());
		assertEquals(1, Reserva.getReservas().size());
	}

	@Test void consolidaVentasPorRubroYPeriodo() {
		GestorTienda gestor = new GestorTienda(); Socio socio = new Socio("venta-reporte@club.co", "clave");
		Articulo articulo = new Articulo("Artículo reporte", "Artículo", 100, "Club");
		UbicacionInventario ubicacion = new UbicacionInventario("reporte", "Bodega");
		gestor.registrarProducto(articulo); gestor.registrarUbicacion(ubicacion);
		gestor.reabastecer(new Inventario(articulo, ubicacion, 0), 2);
		VentaTienda venta = new VentaTienda(socio); venta.agregarDetalle(new DetalleVenta(1, articulo));
		gestor.registrarVenta(venta);
		assertEquals(119.0, consultas.consolidarVentas(administrador, gestor,
				RubroVenta.TIENDA, PeriodoReporte.DIARIO, LocalDate.now()), 0.01);
	}

	@Test void reporteDeportivoIncluyeAsistenciaResultadosYRanking() {
		Tenis tenis = new Tenis(); Entrenador entrenador = new Entrenador("entrenador-deportivo@club.co", "clave");
		Socio socio = new Socio("deportivo@club.co", "clave"); InscripcionAcademia.inscribirSocio(socio, tenis);
		Instalacion instalacion = new Instalacion("Cancha deportiva", tenis, List.of(Modalidad.SENCILLOS));
		SesionEntrenamiento sesion = new SesionEntrenamiento(entrenador, tenis, instalacion,
				LocalDateTime.now().plusDays(1), Duration.ofHours(1));
		sesion.convocarSocios(entrenador, List.of(socio));
		sesion.registrarEntrenamiento(entrenador, socio, EstadoAsistencia.ASISTIO, null, null);
		new Partido(entrenador, tenis, List.of(socio));
		ReporteDeportivo reporte = consultas.generarReporteDeportivo(administrador);
		assertEquals(1, reporte.getAsistencias().size());
		assertEquals(1, reporte.getResultados().size());
		assertEquals(1, reporte.getRankings().size());
	}

	@Test void usuarioNoAdministradorNoPuedeConsultarReportesPrivados() {
		assertThrows(IllegalStateException.class,
				() -> consultas.consultarReservas(new Socio("sin-permiso@club.co", "clave")));
	}
}
