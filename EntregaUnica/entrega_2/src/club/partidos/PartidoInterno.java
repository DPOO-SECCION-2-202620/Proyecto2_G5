package club.partidos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import club.deportes.Equipo;
import club.personal.Entrenador;
import club.usuarios.Administrador;
import club.usuarios.Socio;

public class PartidoInterno extends Partido {

	private static final List<PartidoInterno> partidosInternos = new ArrayList<PartidoInterno>();
	private Equipo equipoLocal;
	private Equipo equipoVisitante;
	private LocalDateTime fecha;

	public PartidoInterno(Administrador administrador, Equipo equipoLocal, Equipo equipoVisitante,
			LocalDateTime fecha, Entrenador entrenador) {
		super(entrenador, equipoLocal == null ? null : equipoLocal.getDeporte(),
				validarYObtenerParticipantes(administrador, equipoLocal, equipoVisitante, fecha));
		this.equipoLocal = equipoLocal;
		this.equipoVisitante = equipoVisitante;
		this.fecha = fecha;
		partidosInternos.add(this);
	}

	private PartidoInterno() { super(); }

	public static PartidoInterno restaurar(Equipo local, Equipo visitante,
			LocalDateTime fecha, Entrenador entrenador, List<Socio> participantes) {
		PartidoInterno partido = new PartidoInterno();
		partido.restaurarDatos(entrenador, local.getDeporte(), participantes);
		partido.equipoLocal = local; partido.equipoVisitante = visitante; partido.fecha = fecha;
		partidosInternos.add(partido); return partido;
	}

	private static List<Socio> validarYObtenerParticipantes(Administrador administrador,
			Equipo local, Equipo visitante, LocalDateTime fecha) {
		if (administrador == null) throw new IllegalStateException("Se requiere un administrador");
		if (!Equipo.getEquiposRegistrados().containsValue(local)
				|| !Equipo.getEquiposRegistrados().containsValue(visitante)) {
			throw new IllegalStateException("Uno de los equipos no existe");
		}
		if (!local.getDeporte().getClass().equals(visitante.getDeporte().getClass())) {
			throw new IllegalArgumentException("Los equipos pertenecen a deportes diferentes");
		}
		if (local.getCategoria() != visitante.getCategoria()) {
			throw new IllegalArgumentException("Los equipos pertenecen a categorías diferentes");
		}
		int minimo = ((club.deportes.DeporteConjunto) local.getDeporte()).getMinimoJugadores();
		if (local.getJugadoresHabilitados().size() < minimo
				|| visitante.getJugadoresHabilitados().size() < minimo) {
			throw new IllegalStateException("Un equipo no alcanza el mínimo de jugadores habilitados");
		}
		if (fecha == null || !fecha.isAfter(LocalDateTime.now()) || local == visitante) {
			throw new IllegalArgumentException("La fecha o los equipos no son válidos");
		}
		List<Socio> participantes = new ArrayList<Socio>(local.getJugadoresHabilitados());
		participantes.addAll(visitante.getJugadoresHabilitados());
		return participantes;
	}

	public static List<PartidoInterno> getPartidosInternos() { 
		return partidosInternos; 
	}
	
	public Equipo getEquipoLocal() { 
		return equipoLocal; 
	}
	
	public Equipo getEquipoVisitante() { 
		return equipoVisitante;
	}
	
	public LocalDateTime getFecha() { 
		return fecha;
	}
}
