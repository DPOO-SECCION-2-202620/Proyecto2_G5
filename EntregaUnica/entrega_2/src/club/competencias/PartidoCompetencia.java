package club.competencias;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import club.deportes.Equipo;
import club.deportes.Futbol;
import club.partidos.Partido;
import club.personal.Entrenador;
import club.usuarios.Socio;

public class PartidoCompetencia extends Partido {

	private Competencia competencia;
	private Equipo equipoLocal;
	private Equipo equipoVisitante;
	private LocalDateTime fecha;
	private Integer marcadorLocal;
	private Integer marcadorVisitante;

	public PartidoCompetencia(Competencia competencia, Equipo equipoLocal, Equipo equipoVisitante,
			LocalDateTime fecha, Entrenador entrenador) {
		super(entrenador, competencia == null ? null : competencia.getDeporte(),
				validarYObtenerParticipantes(competencia, equipoLocal, equipoVisitante, fecha));
		this.competencia = competencia;
		this.equipoLocal = equipoLocal;
		this.equipoVisitante = equipoVisitante;
		this.fecha = fecha;
		competencia.registrarPartido(this);
	}

	private PartidoCompetencia() { super(); }

	public static PartidoCompetencia restaurar(Competencia competencia, Equipo local,
			Equipo visitante, LocalDateTime fecha, Entrenador entrenador,
			List<Socio> participantes, Integer marcadorLocal, Integer marcadorVisitante) {
		PartidoCompetencia partido = new PartidoCompetencia();
		partido.restaurarDatos(entrenador, competencia.getDeporte(), participantes);
		partido.competencia = competencia; partido.equipoLocal = local;
		partido.equipoVisitante = visitante; partido.fecha = fecha;
		partido.marcadorLocal = marcadorLocal; partido.marcadorVisitante = marcadorVisitante;
		competencia.registrarPartido(partido); return partido;
	}

	private static List<Socio> validarYObtenerParticipantes(Competencia competencia,
			Equipo equipoLocal, Equipo equipoVisitante, LocalDateTime fecha) {
		if (!Competencia.getCompetencias().containsValue(competencia)
				|| !competencia.getEquipos().contains(equipoLocal)
				|| !competencia.getEquipos().contains(equipoVisitante)) {
			throw new IllegalStateException("Los equipos no están habilitados en la competencia");
		}
		if (fecha == null || !fecha.isAfter(LocalDateTime.now()) || equipoLocal == equipoVisitante) {
			throw new IllegalArgumentException("Los datos del partido no son válidos");
		}
		List<Socio> participantes = new ArrayList<Socio>();
		participantes.addAll(equipoLocal.getJugadoresHabilitados());
		participantes.addAll(equipoVisitante.getJugadoresHabilitados());
		return participantes;
	}

	public void registrarResultado(Entrenador entrenador, int marcadorLocal, int marcadorVisitante) {
		if (getEntrenador() != entrenador) throw new IllegalStateException("El entrenador no administra el partido");
		if (this.marcadorLocal != null) throw new IllegalStateException("El resultado ya fue registrado");
		if (marcadorLocal < 0 || marcadorVisitante < 0) {
			throw new IllegalArgumentException("El resultado no es válido");
		}
		if (marcadorLocal == marcadorVisitante && !(competencia.getDeporte() instanceof Futbol)) {
			throw new IllegalArgumentException("El deporte no permite empates");
		}
		this.marcadorLocal = marcadorLocal;
		this.marcadorVisitante = marcadorVisitante;
		competencia.registrarResultado(equipoLocal, equipoVisitante, marcadorLocal, marcadorVisitante);
		if (marcadorLocal == marcadorVisitante) {
			equipoLocal.sumarPuntosRanking(1);
			equipoVisitante.sumarPuntosRanking(1);
		} else if (marcadorLocal > marcadorVisitante) {
			equipoLocal.sumarPuntosRanking(3);
		} else {
			equipoVisitante.sumarPuntosRanking(3);
		}
	}

	static List<PartidoCompetencia> crearEmparejamientos(Competencia competencia,
			List<Equipo> equipos, Entrenador entrenador) {
		List<PartidoCompetencia> enfrentamientos = new ArrayList<PartidoCompetencia>();
		for (int i = 0; i + 1 < equipos.size(); i += 2) {
			enfrentamientos.add(new PartidoCompetencia(competencia, equipos.get(i), equipos.get(i + 1),
					LocalDateTime.now().plusDays(i + 1), entrenador));
		}
		return enfrentamientos;
	}

	public Competencia getCompetencia() { 
		return competencia; 
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
	
	public Integer getMarcadorLocal() { 
		return marcadorLocal; 
	}
	
	public Integer getMarcadorVisitante() { 
		return marcadorVisitante; 
	}
}
