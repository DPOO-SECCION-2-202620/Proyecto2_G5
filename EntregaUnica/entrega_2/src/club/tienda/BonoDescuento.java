package club.tienda;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import club.torneos.TorneoInterno;
import club.usuarios.Usuario;

public class BonoDescuento {
	private static final AtomicInteger siguienteId = new AtomicInteger(1);
	private static final Map<String, BonoDescuento> bonosRegistrados = new LinkedHashMap<String, BonoDescuento>();
	private String id;
	private Usuario propietario;
	private double valor;
	private LocalDateTime fechaObtencion;
	private EstadoBono estado;
	private TorneoInterno torneo;
	private Venta ventaReservada;

	public BonoDescuento(Usuario propietario, double valor) {
		this(propietario, valor, null);
	}

	public BonoDescuento(Usuario propietario, double valor, TorneoInterno torneo) {
		if (propietario == null || !Double.isFinite(valor) || valor <= 0) {
			throw new IllegalArgumentException("Los datos del bono no son válidos");
		}
		this.id = "BONO-" + siguienteId.getAndIncrement();
		this.propietario = propietario;
		this.valor = valor;
		this.fechaObtencion = LocalDateTime.now();
		this.estado = EstadoBono.DISPONIBLE;
		this.torneo = torneo;
		bonosRegistrados.put(id, this);
	}

	public static BonoDescuento restaurar(String id, Usuario propietario, double valor,
			LocalDateTime fechaObtencion, EstadoBono estado, TorneoInterno torneo) {
		BonoDescuento bono = new BonoDescuento(propietario, valor, torneo);
		bonosRegistrados.remove(bono.id);
		bono.id = id; bono.fechaObtencion = fechaObtencion; bono.estado = estado;
		bonosRegistrados.put(id, bono);
		if (id.startsWith("BONO-")) {
			try { siguienteId.set(Math.max(siguienteId.get(), Integer.parseInt(id.substring(5)) + 1)); }
			catch (NumberFormatException e) { /* se conserva el siguiente identificador disponible */ }
		}
		return bono;
	}

	public synchronized void reservar(Venta venta) {
		if (!estaDisponible() || (ventaReservada != null && ventaReservada != venta)) {
			throw new IllegalStateException("El bono no está disponible");
		}
		ventaReservada = venta;
	}

	public synchronized void utilizar(Venta venta) {
		if (estado != EstadoBono.DISPONIBLE || ventaReservada != venta) {
			throw new IllegalStateException("El bono no está reservado para esta venta");
		}
		estado = EstadoBono.UTILIZADO;
		ventaReservada = null;
	}

	public synchronized void cancelarReserva(Venta venta) {
		if (ventaReservada == venta) ventaReservada = null;
	}

	public String getId() { return id; }
	public Usuario getPropietario() { return propietario; }
	public String getPropietarioId() { return propietario.getEmail(); }
	public double getValor() { return valor; }
	public LocalDateTime getFechaObtencion() { return fechaObtencion; }
	public EstadoBono getEstado() { return estado; }
	public TorneoInterno getTorneo() { return torneo; }
	public boolean estaDisponible() { return estado == EstadoBono.DISPONIBLE; }
	public static BonoDescuento buscar(String id) { return bonosRegistrados.get(id); }
	public static void limpiar() { bonosRegistrados.clear(); siguienteId.set(1); }
}
