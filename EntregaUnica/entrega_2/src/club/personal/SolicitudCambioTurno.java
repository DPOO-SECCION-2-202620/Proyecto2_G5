package club.personal;

public class SolicitudCambioTurno {
	private Turno turnoAnterior;
	private Turno turnoNuevo;
	private boolean procesada;
	private boolean aprobada;

	public SolicitudCambioTurno(Turno turnoAnterior, Turno turnoNuevo) {
		if (turnoAnterior == null || turnoNuevo == null) throw new IllegalArgumentException("Los turnos son obligatorios");
		this.turnoAnterior = turnoAnterior;
		this.turnoNuevo = turnoNuevo;
	}

	public Turno getTurnoAnterior() { return turnoAnterior; }
	public Turno getTurnoNuevo() { return turnoNuevo; }
	public boolean isProcesada() { return procesada; }
	public boolean isAprobada() { return aprobada; }

	void resolver(boolean aprobada) {
		if (procesada) throw new IllegalStateException("La solicitud ya fue procesada");
		this.procesada = true;
		this.aprobada = aprobada;
	}
}
