package club.persistencia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.instalaciones.Instalacion;
import club.personal.Empleado;
import club.tienda.Articulo;
import club.tienda.Bebida;
import club.tienda.BonoDescuento;
import club.tienda.CodigoDescuento;
import club.tienda.DetalleVenta;
import club.tienda.EstadoSugerencia;
import club.tienda.GestorTienda;
import club.tienda.Inventario;
import club.tienda.Producto;
import club.tienda.ProductoCafeteria;
import club.tienda.Snack;
import club.tienda.SugerenciaProducto;
import club.tienda.TipoSugerencia;
import club.tienda.UbicacionInventario;
import club.tienda.Venta;
import club.tienda.VentaCafeteria;
import club.tienda.VentaTienda;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Usuario;

final class PersistenciaComercialJson {
	static final String ARCHIVO = "comercial.json";
	private final GestorTienda gestor;

	PersistenciaComercialJson(GestorTienda gestor) {
		this.gestor = gestor;
	}

	void cargar(List<Map<String, String>> registros) {
		gestor.limpiar();
		Map<String, List<String>> alergenos = new LinkedHashMap<>();
		Map<String, Map<String, String>> ventas = new LinkedHashMap<>();
		Map<String, List<DetalleVenta>> detalles = new LinkedHashMap<>();
		int siguienteVenta = 0;
		for (Map<String, String> r : registros) {
			String clase = r.getOrDefault("registro", "VENTA");
			if (!List.of("PRODUCTO", "ALERGENO", "UBICACION", "INVENTARIO", "CODIGO",
					"SUGERENCIA", "VENTA", "DETALLE").contains(clase)) {
				throw new PersistenciaException("Registro comercial desconocido: " + clase);
			}
			if ("ALERGENO".equals(clase)) {
				alergenos.computeIfAbsent(requerido(r, "producto"), clave -> new ArrayList<>())
						.add(requerido(r, "valor"));
			} else if ("VENTA".equals(clase)) {
				String id = r.containsKey("registro") ? requerido(r, "id") : "LEGADO-" + siguienteVenta++;
				if (id.isBlank() || ventas.put(id, r) != null) throw new PersistenciaException("Venta duplicada o sin identificador");
				detalles.put(id, new ArrayList<>());
			}
		}
		for (Map<String, String> r : registros) {
			if ("PRODUCTO".equals(r.get("registro"))) {
				String nombre = requerido(r, "nombre"), descripcion = requerido(r, "descripcion");
				double precio = importe(r, "precio");
				Producto producto;
				switch (requerido(r, "tipo")) {
				case "ARTICULO": producto = new Articulo(nombre, descripcion, precio, requerido(r, "categoria")); break;
				case "BEBIDA": producto = new Bebida(nombre, descripcion, precio, booleano(r, "caliente")); break;
				case "SNACK": producto = new Snack(nombre, descripcion, precio, new ArrayList<>()); break;
				default: throw new PersistenciaException("Tipo de producto desconocido");
				}
				gestor.registrarProducto(producto);
			} else if ("UBICACION".equals(r.get("registro"))) {
				gestor.registrarUbicacion(new UbicacionInventario(requerido(r, "id"), requerido(r, "nombre")));
			}
		}
		for (String nombre : alergenos.keySet()) {
			Producto producto = producto(nombre);
			if (!(producto instanceof Snack)) throw new PersistenciaException("Alérgeno sin snack asociado");
			((Snack) producto).getAlergenos().addAll(alergenos.get(nombre));
		}
		for (Map<String, String> r : registros) {
			String clase = r.get("registro");
			if ("INVENTARIO".equals(clase)) {
				Producto producto = producto(requerido(r, "producto"));
				UbicacionInventario ubicacion = gestor.buscarUbicacion(requerido(r, "ubicacion"));
				if (!(producto instanceof Articulo) || ubicacion == null) throw new PersistenciaException("Inventario inconsistente");
				gestor.registrarInventario(new Inventario((Articulo) producto, ubicacion, entero(r, "cantidad")));
			} else if ("CODIGO".equals(clase)) {
				gestor.registrarCodigo(new CodigoDescuento(requerido(r, "codigo"), empleado(requerido(r, "propietario"))));
			} else if ("SUGERENCIA".equals(clase)) {
				gestor.registrarSugerencia(new SugerenciaProducto(requerido(r, "nombre"), requerido(r, "descripcion"),
						TipoSugerencia.valueOf(requerido(r, "tipo")), empleado(requerido(r, "solicitante")),
						EstadoSugerencia.valueOf(requerido(r, "estado"))));
			} else if ("DETALLE".equals(clase)) {
				List<DetalleVenta> destino = detalles.get(requerido(r, "venta"));
				if (destino == null) throw new PersistenciaException("Detalle sin venta asociada");
				destino.add(new DetalleVenta(entero(r, "cantidad"), producto(requerido(r, "producto")), importe(r, "precioUnitario")));
			}
		}
		for (Map.Entry<String, Map<String, String>> entrada : ventas.entrySet()) {
			cargarVenta(entrada.getValue(), detalles.get(entrada.getKey()));
		}
	}

	private void cargarVenta(Map<String, String> r, List<DetalleVenta> detalles) {
		Usuario comprador = RepositorioUsuarios.buscarPorEmail(requerido(r, "comprador"));
		if (comprador == null) throw new PersistenciaException("Comprador inexistente");
		String tipo = requerido(r, "tipo");
		if (!List.of("TIENDA", "CAFETERIA").contains(tipo)) throw new PersistenciaException("Tipo de venta desconocido");
		for (DetalleVenta detalle : detalles) {
			if (("TIENDA".equals(tipo) && !(detalle.getProducto() instanceof Articulo))
					|| ("CAFETERIA".equals(tipo) && !(detalle.getProducto() instanceof ProductoCafeteria))) {
				throw new PersistenciaException("Producto incompatible con la venta");
			}
		}
		LocalDateTime fecha = LocalDateTime.parse(requerido(r, "fecha"));
		double subtotal = importe(r, "subtotal"), impuestos = importe(r, "impuestos");
		double descuentos = importe(r, "descuentos"), total = importe(r, "total");
		int ganados = entero(r, "puntosGanados"), redimidos = entero(r, "puntosRedimidos");
		Venta venta;
		if ("TIENDA".equals(tipo)) {
			venta = new VentaTienda(comprador, fecha, detalles, subtotal, impuestos, descuentos, total, ganados, redimidos, true);
		} else {
			double porcentaje = r.containsKey("propinaPorcentaje") ? importe(r, "propinaPorcentaje")
					: subtotal == 0 ? 0 : Math.max(0, total - subtotal + descuentos - impuestos + redimidos) / subtotal;
			if (porcentaje > 1) throw new PersistenciaException("Porcentaje de propina inválido");
			VentaCafeteria cafeteria = new VentaCafeteria(comprador, fecha, detalles, subtotal, impuestos,
					descuentos, total, ganados, redimidos, true, porcentaje,
					r.containsKey("avisoAlergenos") ? booleano(r, "avisoAlergenos") : true);
			String instalacion = r.getOrDefault("instalacion", "");
			if (!instalacion.isEmpty()) {
				Instalacion existente = Instalacion.getInstalaciones().get(instalacion);
				if (existente == null) throw new PersistenciaException("Instalación de venta inexistente");
				cafeteria.restaurarInstalacion(existente);
			}
			venta = cafeteria;
		}
		String bonoId = requerido(r, "bono");
		BonoDescuento bono = bonoId.isEmpty() ? null : BonoDescuento.buscar(bonoId);
		if (!bonoId.isEmpty() && bono == null) throw new PersistenciaException("Bono de venta inexistente");
		venta.restaurarBono(bono, importe(r, "montoOriginal"), importe(r, "valorDescontado"));
		gestor.registrarVentaHistorica(venta);
	}

	String serializar() {
		List<String> objetos = new ArrayList<>();
		List<Venta> ventas = gestor.getVentas();
		for (Venta venta : Venta.getVentasConfirmadas()) if (!ventas.contains(venta)) ventas.add(venta);
		List<Producto> productos = gestor.getProductos();
		for (Venta venta : ventas) {
			for (DetalleVenta detalle : venta.getDetalleVentas()) {
				boolean existe = false;
				for (Producto producto : productos) {
					if (producto.getNombre().trim().equalsIgnoreCase(detalle.getProducto().getNombre().trim())) existe = true;
				}
				if (!existe) productos.add(detalle.getProducto());
			}
		}
		for (Producto producto : productos) {
			Map<String, String> r = registro("PRODUCTO", "nombre", producto.getNombre(), "descripcion", producto.getDescripcion(),
					"precio", String.valueOf(producto.getPrecio()));
			if (producto instanceof Articulo) {
				r.put("tipo", "ARTICULO");
				r.put("categoria", ((Articulo) producto).getCategoria() == null ? "" : ((Articulo) producto).getCategoria());
			} else if (producto instanceof Bebida) {
				r.put("tipo", "BEBIDA");
				r.put("caliente", String.valueOf(((Bebida) producto).esCaliente()));
			} else if (producto instanceof Snack) {
				r.put("tipo", "SNACK");
			} else throw new PersistenciaException("Tipo de producto desconocido");
			objetos.add(Json.objeto(r));
			if (producto instanceof Snack) {
				for (String alergeno : ((Snack) producto).getAlergenos()) {
					objetos.add(Json.objeto(registro("ALERGENO", "producto", producto.getNombre(), "valor", alergeno)));
				}
			}
		}
		for (UbicacionInventario ubicacion : gestor.getUbicaciones()) {
			objetos.add(Json.objeto(registro("UBICACION", "id", ubicacion.getId(), "nombre", ubicacion.getNombre())));
		}
		for (Inventario inventario : gestor.consultarInventarios()) {
			objetos.add(Json.objeto(registro("INVENTARIO", "producto", inventario.getArticulo().getNombre(),
					"ubicacion", inventario.getUbicacion().getId(), "cantidad", String.valueOf(inventario.consultarCantidad()))));
		}
		for (CodigoDescuento codigo : gestor.getCodigos()) {
			objetos.add(Json.objeto(registro("CODIGO", "codigo", codigo.getCodigo(), "propietario", codigo.getPropietario().getEmail())));
		}
		for (SugerenciaProducto sugerencia : gestor.getSugerencias()) {
			objetos.add(Json.objeto(registro("SUGERENCIA", "nombre", sugerencia.getNombre(), "descripcion", sugerencia.getDescripcion(),
					"tipo", sugerencia.getTipo().name(), "estado", sugerencia.getEstado().name(), "solicitante", sugerencia.getSolicitante().getEmail())));
		}
		for (int i = 0; i < ventas.size(); i++) {
			Venta venta = ventas.get(i);
			Map<String, String> r = registro("VENTA", "id", String.valueOf(i), "tipo", venta instanceof VentaTienda ? "TIENDA" : "CAFETERIA",
					"comprador", venta.getComprador().getEmail(), "fecha", venta.getFecha().toString(),
					"subtotal", String.valueOf(venta.getSubtotal()), "impuestos", String.valueOf(venta.getImpuestos()),
					"descuentos", String.valueOf(venta.getDescuentos()), "total", String.valueOf(venta.getTotal()),
					"puntosGanados", String.valueOf(venta.getPuntosGanados()), "puntosRedimidos", String.valueOf(venta.getPuntosRedimidos()),
					"bono", venta.getBonoAplicado() == null ? "" : venta.getBonoAplicado().getId(),
					"montoOriginal", String.valueOf(venta.getMontoOriginal()), "valorDescontado", String.valueOf(venta.getValorDescontado()));
			if (venta instanceof VentaCafeteria) {
				VentaCafeteria cafeteria = (VentaCafeteria) venta;
				r.put("propinaPorcentaje", String.valueOf(cafeteria.getPropinaPorcentaje()));
				r.put("avisoAlergenos", String.valueOf(cafeteria.isAvisoAlergenosRealizado()));
				r.put("instalacion", cafeteria.getInstalacion() == null ? "" : cafeteria.getInstalacion().getNombre());
			}
			objetos.add(Json.objeto(r));
			for (DetalleVenta detalle : venta.getDetalleVentas()) {
				objetos.add(Json.objeto(registro("DETALLE", "venta", String.valueOf(i), "producto", detalle.getProducto().getNombre(),
						"cantidad", String.valueOf(detalle.getCantidad()), "precioUnitario", String.valueOf(detalle.getPrecioUnitario()))));
			}
		}
		return Json.arreglo(objetos);
	}

	private Map<String, String> registro(String tipo, String... campos) {
		Map<String, String> resultado = new LinkedHashMap<>();
		resultado.put("registro", tipo);
		for (int i = 0; i < campos.length; i += 2) resultado.put(campos[i], campos[i + 1]);
		return resultado;
	}

	private String requerido(Map<String, String> registro, String campo) {
		return PersistenciaSoporte.requerido(registro, campo);
	}

	private double importe(Map<String, String> registro, String campo) {
		double valor = Double.parseDouble(requerido(registro, campo));
		if (!Double.isFinite(valor) || valor < 0) throw new PersistenciaException("Importe inválido: " + campo);
		return valor;
	}

	private int entero(Map<String, String> registro, String campo) {
		int valor = Integer.parseInt(requerido(registro, campo));
		if (valor < 0) throw new PersistenciaException("Cantidad inválida: " + campo);
		return valor;
	}

	private boolean booleano(Map<String, String> registro, String campo) {
		String valor = requerido(registro, campo);
		if (!List.of("true", "false").contains(valor)) throw new PersistenciaException("Booleano inválido: " + campo);
		return Boolean.parseBoolean(valor);
	}

	private Producto producto(String nombre) {
		Producto producto = gestor.buscarProducto(nombre);
		if (producto == null) throw new PersistenciaException("Producto inexistente: " + nombre);
		return producto;
	}

	private Empleado empleado(String email) {
		Usuario usuario = RepositorioUsuarios.buscarPorEmail(email);
		if (!(usuario instanceof Empleado)) throw new PersistenciaException("Empleado inexistente: " + email);
		return (Empleado) usuario;
	}
}
