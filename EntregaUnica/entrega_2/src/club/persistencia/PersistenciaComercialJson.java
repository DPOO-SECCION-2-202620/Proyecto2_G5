package club.persistencia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.tienda.BonoDescuento;
import club.tienda.Venta;
import club.tienda.VentaCafeteria;
import club.tienda.VentaTienda;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Usuario;

final class PersistenciaComercialJson {
	static final String ARCHIVO = "comercial.json";

	void cargar(List<Map<String, String>> registros) {
		for (Map<String, String> registro : registros) {
			Usuario comprador = RepositorioUsuarios.buscarPorEmail(PersistenciaSoporte.requerido(registro, "comprador"));
			String tipo = PersistenciaSoporte.requerido(registro, "tipo");
			LocalDateTime fecha = LocalDateTime.parse(PersistenciaSoporte.requerido(registro, "fecha"));
			double subtotal = Double.parseDouble(PersistenciaSoporte.requerido(registro, "subtotal"));
			double impuestos = Double.parseDouble(PersistenciaSoporte.requerido(registro, "impuestos"));
			double descuentos = Double.parseDouble(PersistenciaSoporte.requerido(registro, "descuentos"));
			double total = Double.parseDouble(PersistenciaSoporte.requerido(registro, "total"));
			int ganados = Integer.parseInt(PersistenciaSoporte.requerido(registro, "puntosGanados"));
			int redimidos = Integer.parseInt(PersistenciaSoporte.requerido(registro, "puntosRedimidos"));
			Venta venta = "TIENDA".equals(tipo)
					? new VentaTienda(comprador, fecha, new ArrayList<>(), subtotal, impuestos,
							descuentos, total, ganados, redimidos, true)
					: new VentaCafeteria(comprador, fecha, new ArrayList<>(), subtotal, impuestos,
							descuentos, total, ganados, redimidos, true, 0, true);
			String bono = registro.getOrDefault("bono", "");
			venta.restaurarBono(bono.isEmpty() ? null : BonoDescuento.buscar(bono),
					Double.parseDouble(PersistenciaSoporte.requerido(registro, "montoOriginal")),
					Double.parseDouble(PersistenciaSoporte.requerido(registro, "valorDescontado")));
			Venta.registrarHistorica(venta);
		}
	}

	String serializar() {
		List<String> objetos = new ArrayList<String>();
		for (Venta venta : Venta.getVentasConfirmadas()) {
			Map<String, String> valores = new LinkedHashMap<String, String>();
			valores.put("tipo", venta instanceof VentaTienda ? "TIENDA" : "CAFETERIA");
			valores.put("comprador", venta.getComprador().getEmail()); valores.put("fecha", venta.getFecha().toString());
			valores.put("subtotal", String.valueOf(venta.getSubtotal())); valores.put("impuestos", String.valueOf(venta.getImpuestos()));
			valores.put("descuentos", String.valueOf(venta.getDescuentos())); valores.put("total", String.valueOf(venta.getTotal()));
			valores.put("puntosGanados", String.valueOf(venta.getPuntosGanados())); valores.put("puntosRedimidos", String.valueOf(venta.getPuntosRedimidos()));
			valores.put("bono", venta.getBonoAplicado() == null ? "" : venta.getBonoAplicado().getId());
			valores.put("montoOriginal", String.valueOf(venta.getMontoOriginal()));
			valores.put("valorDescontado", String.valueOf(venta.getValorDescontado()));
			objetos.add(Json.objeto(valores));
		}
		return Json.arreglo(objetos);
	}
}
