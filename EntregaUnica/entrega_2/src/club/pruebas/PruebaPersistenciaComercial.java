package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import club.instalaciones.Instalacion;
import club.personal.Entrenador;
import club.persistencia.PersistenciaClubJson;
import club.tienda.Articulo;
import club.tienda.Bebida;
import club.tienda.CodigoDescuento;
import club.tienda.DetalleVenta;
import club.tienda.EstadoSugerencia;
import club.tienda.GestorTienda;
import club.tienda.Inventario;
import club.tienda.Snack;
import club.tienda.SugerenciaProducto;
import club.tienda.TipoSugerencia;
import club.tienda.UbicacionInventario;
import club.tienda.VentaCafeteria;
import club.tienda.VentaTienda;
import club.usuarios.Administrador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;

public class PruebaPersistenciaComercial {
	@TempDir
	Path directorio;

	@Test void conservaCatalogoInventariosCodigosYSugerencias() {
		PersistenciaClubJson persistencia = new PersistenciaClubJson(directorio);
		persistencia.cargarClub();
		GestorTienda gestor = persistencia.getGestorTienda();
		Entrenador empleado = new Entrenador("comercial@club.co", "clave");
		Administrador administrador = new Administrador("admin-comercial@club.co", "clave");
		Articulo articulo = new Articulo("Balón|Séneca", "Descripción; con \"comillas\"", 30000, "Fútbol");
		Snack snack = new Snack("Barra", "Con semillas", 5000, List.of("maní|nuez", "leche;soya", "línea\n\"trigo\""));
		gestor.registrarProducto(articulo);
		gestor.registrarProducto(snack);
		gestor.registrarProducto(new Bebida("Café", "Tinto", 2500, true));
		UbicacionInventario primera = new UbicacionInventario("BOD|1", "Bodega principal");
		UbicacionInventario segunda = new UbicacionInventario("BOD;2", "Bodega secundaria");
		gestor.registrarUbicacion(primera);
		gestor.registrarUbicacion(segunda);
		gestor.registrarInventario(new Inventario(articulo, primera, 7));
		gestor.registrarInventario(new Inventario(articulo, segunda, 0));
		gestor.registrarCodigo(new CodigoDescuento("DESCUENTO|15", empleado));
		for (EstadoSugerencia estado : EstadoSugerencia.values()) {
			SugerenciaProducto sugerencia = new SugerenciaProducto("Sugerencia " + estado, "Descripción", TipoSugerencia.BEBIDA, empleado);
			gestor.registrarSugerencia(sugerencia);
			if (estado == EstadoSugerencia.APROBADA) gestor.aprobarSugerencia(sugerencia, administrador);
			if (estado == EstadoSugerencia.RECHAZADA) gestor.rechazarSugerencia(sugerencia, administrador);
		}
		persistencia.salvarClub();
		persistencia.cargarClub();

		GestorTienda recuperado = persistencia.getGestorTienda();
		Articulo articuloRecuperado = (Articulo) recuperado.buscarProducto(articulo.getNombre());
		assertEquals(3, recuperado.getProductos().size());
		assertEquals(articulo.getDescripcion(), articuloRecuperado.getDescripcion());
		assertEquals("Fútbol", articuloRecuperado.getCategoria());
		assertEquals(snack.getAlergenos(), ((Snack) recuperado.buscarProducto("Barra")).getAlergenos());
		assertTrue(((Bebida) recuperado.buscarProducto("Café")).esCaliente());
		assertEquals(2, recuperado.getUbicaciones().size());
		assertEquals(List.of(7, 0), recuperado.consultarInventarios().stream().map(Inventario::consultarCantidad).toList());
		assertSame(articuloRecuperado, recuperado.consultarInventarios().get(0).getArticulo());
		assertSame(recuperado.buscarUbicacion("BOD|1"), recuperado.consultarInventarios().get(0).getUbicacion());
		assertEquals("DESCUENTO|15", recuperado.getCodigos().get(0).getCodigo());
		assertSame(RepositorioUsuarios.buscarPorEmail(empleado.getEmail()), recuperado.getCodigos().get(0).getPropietario());
		assertEquals(List.of(EstadoSugerencia.values()), recuperado.getSugerencias().stream().map(SugerenciaProducto::getEstado).toList());
		assertTrue(recuperado.getSugerencias().stream().allMatch(s -> s.getTipo() == TipoSugerencia.BEBIDA));
	}

	@Test void conservaDetallesPrecioHistoricoYPropinaSinReprocesarVentas() {
		PersistenciaClubJson persistencia = new PersistenciaClubJson(directorio);
		persistencia.cargarClub();
		GestorTienda gestor = persistencia.getGestorTienda();
		Socio comprador = new Socio("comprador@club.co", "clave");
		Articulo articulo = new Articulo("Camiseta", "Séneca", 10000, "Ropa");
		Snack snack = new Snack("Galleta", "Integral", 5000, List.of("Trigo"));
		gestor.registrarProducto(articulo);
		gestor.registrarProducto(snack);
		UbicacionInventario ubicacion = new UbicacionInventario("B1", "Bodega");
		gestor.registrarUbicacion(ubicacion);
		gestor.registrarInventario(new Inventario(articulo, ubicacion, 20));
		VentaTienda tienda = new VentaTienda(comprador);
		tienda.agregarDetalle(new DetalleVenta(2, articulo));
		gestor.registrarVenta(tienda);
		VentaCafeteria cafeteria = new VentaCafeteria(comprador, new Instalacion("Terraza"));
		cafeteria.agregarDetalle(new DetalleVenta(2, snack));
		cafeteria.informarAlergenos();
		cafeteria.ajustarPropina(0.25);
		gestor.registrarVenta(cafeteria);
		articulo.actualizarPrecio(15000);
		int puntos = comprador.getSaldoFidelidad().getPuntos();
		persistencia.salvarClub();
		persistencia.cargarClub();
		persistencia.salvarClub();
		persistencia.cargarClub();

		GestorTienda recuperado = persistencia.getGestorTienda();
		assertEquals(2, recuperado.getVentas().size());
		VentaTienda tiendaRecuperada = (VentaTienda) recuperado.getVentas().get(0);
		VentaCafeteria cafeteriaRecuperada = (VentaCafeteria) recuperado.getVentas().get(1);
		assertTrue(tiendaRecuperada.estaConfirmada());
		assertEquals(2, tiendaRecuperada.getDetalleVentas().get(0).getCantidad());
		assertEquals(10000, tiendaRecuperada.getDetalleVentas().get(0).getPrecioUnitario());
		assertEquals(15000, tiendaRecuperada.getDetalleVentas().get(0).getProducto().getPrecio());
		assertSame(recuperado.buscarProducto("Camiseta"), tiendaRecuperada.getDetalleVentas().get(0).getProducto());
		assertEquals(tienda.getTotal(), tiendaRecuperada.getTotal());
		assertEquals(2500, cafeteriaRecuperada.getPropina());
		assertEquals(0.25, cafeteriaRecuperada.getPropinaPorcentaje());
		assertTrue(cafeteriaRecuperada.isAvisoAlergenosRealizado());
		assertSame(Instalacion.getInstalaciones().get("Terraza"), cafeteriaRecuperada.getInstalacion());
		assertEquals(18, recuperado.consultarInventario((Articulo) recuperado.buscarProducto("Camiseta")));
		assertEquals(puntos, ((Socio) RepositorioUsuarios.buscarPorEmail(comprador.getEmail())).getSaldoFidelidad().getPuntos());
	}

	@Test void migraResumenHistoricoSinInventarProductosNiPerderPropina() throws IOException {
		PersistenciaClubJson persistencia = new PersistenciaClubJson(directorio);
		persistencia.cargarClub();
		new Socio("historico@club.co", "clave");
		persistencia.salvarClub();
		Files.writeString(directorio.resolve("current/comercial.json"), """
				[{"tipo":"CAFETERIA","comprador":"historico@club.co","fecha":"2026-11-09T13:00",
				"subtotal":"10000","impuestos":"800","descuentos":"0","total":"11800",
				"puntosGanados":"236","puntosRedimidos":"0","bono":"","montoOriginal":"10000","valorDescontado":"0"}]
				""");
		persistencia.cargarClub();
		persistencia.salvarClub();
		persistencia.cargarClub();

		assertTrue(persistencia.getGestorTienda().getProductos().isEmpty());
		assertEquals(1, persistencia.getGestorTienda().getVentas().size());
		VentaCafeteria historica = (VentaCafeteria) persistencia.getGestorTienda().getVentas().get(0);
		assertTrue(historica.getDetalleVentas().isEmpty());
		assertEquals(1000, historica.getPropina(), 0.0001);
		assertEquals(11800, historica.getTotal());
		assertEquals(0, ((Socio) RepositorioUsuarios.buscarPorEmail("historico@club.co")).getSaldoFidelidad().getPuntos());
	}
}
