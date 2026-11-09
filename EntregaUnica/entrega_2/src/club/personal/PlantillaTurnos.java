package club.personal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import club.usuarios.Administrador;

public class PlantillaTurnos {
	private List<Turno> turnos = new ArrayList<Turno>();
	private List<SolicitudCambioTurno> solicitudes = new ArrayList<SolicitudCambioTurno>();

	public void agregarTurno(Turno turno) {
		if (turno == null) throw new IllegalArgumentException("El turno es obligatorio");
		turnos.add(turno);
	}

	public SolicitudCambioTurno solicitarCambio(Turno anterior, Turno nuevo) {
		if (!turnos.contains(anterior)) throw new IllegalArgumentException("El turno anterior no existe");
		SolicitudCambioTurno solicitud = new SolicitudCambioTurno(anterior, nuevo);
		solicitud.setPlantilla(this);
		solicitudes.add(solicitud);
		return solicitud;
	}

	public void aprobarCambio(Administrador administrador, SolicitudCambioTurno solicitud) {
		if (administrador == null || solicitud == null || solicitud.isProcesada()) {
			throw new IllegalArgumentException("La solicitud no es válida");
		}
		List<Turno> propuestos = new ArrayList<Turno>(turnos);
		if (!propuestos.remove(solicitud.getTurnoAnterior())) throw new IllegalArgumentException("El turno anterior no existe");
		propuestos.add(solicitud.getTurnoNuevo());
		if (!mantieneCobertura(propuestos)) {
			solicitud.resolver(false);
			throw new IllegalStateException("El cambio no mantiene dos entrenadores y un fisioterapeuta");
		}
		turnos.clear(); turnos.addAll(propuestos); solicitud.resolver(true);
	}

	public List<Turno> getTurnos() { return new ArrayList<Turno>(turnos); }

	// Consulta las solicitudes pendientes y procesadas de la plantilla
	public List<SolicitudCambioTurno> getSolicitudes() {
		return new ArrayList<SolicitudCambioTurno>(solicitudes);
	}

	public int getEntrenadoresDisponibles() {
		int disponibles = 0;
		for (Turno turno : turnos) if (turno.getEmpleado() instanceof Entrenador) disponibles++;
		return disponibles;
	}

	public int getFisioterapeutasDisponibles() {
		int disponibles = 0;
		for (Turno turno : turnos) if (turno.getEmpleado() instanceof Fisioterapeuta) disponibles++;
		return disponibles;
	}

	public static void aprobarSolicitud(Administrador administrador, SolicitudCambioTurno solicitud) {
		if (solicitud == null || solicitud.getPlantilla() == null) {
			throw new IllegalArgumentException("La solicitud no pertenece a una plantilla");
		}
		solicitud.getPlantilla().aprobarCambio(administrador, solicitud);
	}

	public boolean estaDeTurno(Empleado empleado, LocalDateTime momento) {
		if (empleado == null || momento == null) return false;
		for (Turno turno : turnos) {
			if (turno.getEmpleado() == empleado && turno.cubre(momento)) return true;
		}
		return false;
	}

	private boolean mantieneCobertura(List<Turno> propuestos) {
		List<LocalDateTime> momentos = new ArrayList<LocalDateTime>();
		for (Turno turno : propuestos) { momentos.add(turno.getInicio()); momentos.add(turno.getFin()); }
		momentos.sort(Comparator.naturalOrder());
		for (int i = 0; i < momentos.size() - 1; i++) {
			if (!momentos.get(i).isBefore(momentos.get(i + 1))) continue;
			LocalDateTime momento = momentos.get(i).plusNanos(1);
			int entrenadores = 0, fisioterapeutas = 0;
			for (Turno turno : propuestos) if (turno.cubre(momento)) {
				if (turno.getEmpleado() instanceof Entrenador) entrenadores++;
				if (turno.getEmpleado() instanceof Fisioterapeuta) fisioterapeutas++;
			}
			if (entrenadores < 2 || fisioterapeutas < 1) return false;
		}
		return true;
	}
}
