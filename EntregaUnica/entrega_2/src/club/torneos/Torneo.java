package club.torneos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import club.deportes.Deporte;
import club.usuarios.Administrador;

public abstract class Torneo {
	public enum Formato {
		LIGA,
		FASES
	}

	private int id;
	private Deporte deporte;
	private String categoria;
	private ModalidadTorneo modalidad;
	private int cupos;
	private int cuposPreferencialesIniciales;
	private TipoTorneo tipo;
	private double tarifa;
	private EstadoTorneo estado;
	private Formato formato;
	private LocalDateTime fechaInicio;
	private Administrador administrador;
	protected List<InscripcionTorneo> inscripciones = new ArrayList<InscripcionTorneo>();

	protected Torneo(Administrador administrador, Deporte deporte, String categoria,
			ModalidadTorneo modalidad, int cupos, TipoTorneo tipo, double tarifa,
			Formato formato, LocalDateTime fechaInicio) {
		if (administrador == null || deporte == null || categoria == null || categoria.isBlank()
				|| modalidad == null || cupos <= 0 || tipo == null || tarifa < 0 || formato == null) {
			throw new IllegalArgumentException("Los datos del torneo no son válidos");
		}
		if (tipo == TipoTorneo.RECREATIVO && tarifa != 0) {
			throw new IllegalArgumentException("Un torneo recreativo no tiene tarifa");
		}
		this.administrador = administrador;
		this.deporte = deporte;
		this.categoria = categoria;
		this.modalidad = modalidad;
		this.cupos = cupos;
		this.cuposPreferencialesIniciales = (int) Math.ceil(cupos * 0.20);
		this.tipo = tipo;
		this.tarifa = tarifa;
		this.estado = EstadoTorneo.PENDIENTE;
		this.formato = formato;
		this.fechaInicio = fechaInicio;
	}

	protected void registrarEnCatalogo(TorneoInterno torneo) {
		this.id = CatalogoTorneos.registrar(torneo);
	}

	protected void restaurarEstado(int id, EstadoTorneo estado) {
		int anterior = this.id;
		this.id = id;
		this.estado = estado;
		CatalogoTorneos.restaurarId((TorneoInterno) this, anterior, id);
	}

	public void iniciar() {
		iniciar(administrador);
	}

	public void iniciar(Administrador administrador) {
		if (this.administrador != administrador) throw new IllegalStateException("Solo el administrador puede iniciar el torneo");
		if (estado != EstadoTorneo.PENDIENTE) throw new IllegalStateException("El torneo no está pendiente");
		if (fechaInicio != null && LocalDateTime.now().isBefore(fechaInicio)) {
			throw new IllegalStateException("La fecha de inicio todavía no ha llegado");
		}
		estado = EstadoTorneo.EN_CURSO;
	}

	public void finalizar(Administrador administrador) {
		if (this.administrador != administrador) throw new IllegalStateException("Solo el administrador puede finalizar el torneo");
		if (estado != EstadoTorneo.EN_CURSO) throw new IllegalStateException("El torneo no está en curso");
		estado = EstadoTorneo.FINALIZADO;
	}

	public int getId() { return id; }
	public Deporte getDeporte() { return deporte; }
	public String getCategoria() { return categoria; }
	public ModalidadTorneo getModalidad() { return modalidad; }
	public int getCupos() { return cupos; }
	public TipoTorneo getTipo() { return tipo; }
	public double getTarifa() { return tarifa; }
	public EstadoTorneo getEstado() { return estado; }
	public Formato getFormato() { return formato; }
	public LocalDateTime getFechaInicio() { return fechaInicio; }
	public int getCuposDisponibles() {
		int activas = 0;
		for (InscripcionTorneo inscripcion : inscripciones) if (inscripcion.isActiva()) activas++;
		return cupos - activas;
	}
	public int getCuposPreferenciales() {
		int usados = 0;
		for (InscripcionTorneo inscripcion : inscripciones)
			if (inscripcion.isActiva() && inscripcion.esPreferencial()) usados++;
		return cuposPreferencialesIniciales - usados;
	}

	protected void validarAbierto() {
		if (estado != EstadoTorneo.PENDIENTE) throw new IllegalStateException("El torneo ya inició");
	}

	protected Administrador getAdministrador() {
		return administrador;
	}

	public Administrador getAdministradorResponsable() {
		return administrador;
	}
}
