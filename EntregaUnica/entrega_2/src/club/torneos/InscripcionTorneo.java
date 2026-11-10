package club.torneos;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class InscripcionTorneo {
	private static int siguienteId = 1;
	private int id;
	private TorneoInterno torneo;
	private UnidadParticipacion unidadParticipacion;
	private boolean preferencial;
	private double tarifaPagada;
	private boolean recibePremio;
	private EstadoInscripcion estado;
	private LocalDateTime fecha;

	public static int getSiguienteId() { return siguienteId; }
	public static void restaurarSiguienteId(int siguiente) { siguienteId = siguiente; }

	InscripcionTorneo(TorneoInterno torneo, UnidadParticipacion unidadParticipacion, boolean preferencial,
			double tarifaPagada, boolean recibePremio) {
		this.id = siguienteId++;
		this.torneo = torneo;
		this.unidadParticipacion = unidadParticipacion;
		this.preferencial = preferencial;
		this.tarifaPagada = tarifaPagada;
		this.recibePremio = recibePremio;
		this.estado = EstadoInscripcion.ACTIVA;
		this.fecha = LocalDateTime.now();
	}

	void restaurarDatos(int id, EstadoInscripcion estado, LocalDateTime fecha) {
		this.id = id;
		this.estado = estado;
		this.fecha = fecha;
		siguienteId = Math.max(siguienteId, id + 1);
	}

	public List<Object> getParticipantes() {
		return new ArrayList<Object>(unidadParticipacion.getIntegrantes());
	}

	void cancelar() { estado = EstadoInscripcion.CANCELADA; }
	public int getId() { return id; }
	public TorneoInterno getTorneo() { return torneo; }
	public UnidadParticipacion getUnidadParticipacion() { return unidadParticipacion; }
	public EstadoInscripcion getEstado() { return estado; }
	public LocalDateTime getFecha() { return fecha; }
	public boolean isActiva() { return estado == EstadoInscripcion.ACTIVA; }

	public boolean esPreferencial() {
		return preferencial;
	}

	public double getTarifaPagada() {
		return tarifaPagada;
	}

	public boolean puedeRecibirPremio() {
		return recibePremio;
	}
}
