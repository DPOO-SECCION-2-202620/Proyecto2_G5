package club.competencias;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.deportes.Categoria;
import club.deportes.DeporteConjunto;
import club.deportes.Equipo;
import club.partidos.EstadisticaJugador;
import club.personal.Entrenador;
import club.usuarios.Administrador;

public class Competencia {

	private static final Map<String, Competencia> competencias = new LinkedHashMap<String, Competencia>();
	private String nombre;
	private String entidadOrganizadora;
	private int anio;
	private String temporada;
	private FormatoCompetencia formato;
	private DeporteConjunto deporte;
	private List<Categoria> categoriasPermitidas;
	private int minimoJugadores;
	private List<Equipo> equipos = new ArrayList<Equipo>();
	private List<PartidoCompetencia> partidos = new ArrayList<PartidoCompetencia>();
	private Map<Equipo, Integer> puntos = new HashMap<Equipo, Integer>();

	public Competencia(String nombre, String entidadOrganizadora, int anio, String temporada,
			FormatoCompetencia formato, DeporteConjunto deporte,
			List<Categoria> categoriasPermitidas, int minimoJugadores) {
		if (nombre == null || entidadOrganizadora == null || temporada == null || formato == null
				|| deporte == null || categoriasPermitidas == null || categoriasPermitidas.isEmpty()
				|| anio <= 0 || minimoJugadores <= 0) {
			throw new IllegalArgumentException("Los datos de la competencia no son válidos");
		}
		this.nombre = nombre;
		this.entidadOrganizadora = entidadOrganizadora;
		this.anio = anio;
		this.temporada = temporada;
		this.formato = formato;
		this.deporte = deporte;
		this.categoriasPermitidas = new ArrayList<Categoria>(categoriasPermitidas);
		this.minimoJugadores = minimoJugadores;
		competencias.put(nombre, this);
	}

	public void inscribirEquipo(Administrador administrador, Equipo equipo) {
		if (administrador == null) throw new IllegalStateException("Se requiere un administrador");
		if (!competencias.containsValue(this) || !Equipo.getEquiposRegistrados().containsValue(equipo)) {
			throw new IllegalStateException("La competencia o el equipo no existe");
		}
		if (!equipo.getDeporte().getClass().equals(deporte.getClass())) {
			throw new IllegalArgumentException("El equipo pertenece a otro deporte");
		}
		if (!categoriasPermitidas.contains(equipo.getCategoria())) {
			throw new IllegalArgumentException("La categoría no está permitida");
		}
		if (equipo.getJugadoresHabilitados().size() < minimoJugadores) {
			throw new IllegalStateException("El equipo no cumple el mínimo de jugadores habilitados");
		}
		if (equipos.contains(equipo)) throw new IllegalStateException("El equipo ya está inscrito");
		equipos.add(equipo);
		puntos.put(equipo, 0);
	}

	public void registrarPartido(PartidoCompetencia partido) { partidos.add(partido); }

	public void restaurarEquipo(Equipo equipo, int puntos) {
		if (equipo == null || puntos < 0 || equipos.contains(equipo)) {
			throw new IllegalArgumentException("La participación no es válida");
		}
		equipos.add(equipo);
		this.puntos.put(equipo, puntos);
	}

	public void registrarResultado(Equipo local, Equipo visitante, int marcadorLocal, int marcadorVisitante) {
		if (marcadorLocal == marcadorVisitante) {
			puntos.put(local, puntos.get(local) + 1);
			puntos.put(visitante, puntos.get(visitante) + 1);
		} else if (marcadorLocal > marcadorVisitante) {
			puntos.put(local, puntos.get(local) + 3);
		} else {
			puntos.put(visitante, puntos.get(visitante) + 3);
		}
	}

	public List<Equipo> getTablaPosiciones() {
		List<Equipo> tabla = new ArrayList<Equipo>(equipos);
		tabla.sort(Comparator.comparingInt((Equipo equipo) -> puntos.get(equipo)).reversed());
		return tabla;
	}

	public List<PartidoCompetencia> generarEnfrentamientos(Entrenador entrenador) {
		if (formato != FormatoCompetencia.FASES) {
			throw new IllegalStateException("La competencia no es por fases");
		}
		if (equipos.size() < 2) throw new IllegalStateException("No hay suficientes equipos");
		return PartidoCompetencia.crearEmparejamientos(this, equipos, entrenador);
	}

	public List<EstadisticaJugador> getRankingEstadistica() {
		List<EstadisticaJugador> estadisticas = new ArrayList<EstadisticaJugador>();
		for (PartidoCompetencia partido : partidos) estadisticas.addAll(partido.getEstadisticas());
		return estadisticas;
	}

	public static Map<String, Competencia> getCompetencias() { 
		return competencias; 
	}
	
	public String getNombre() { 
		return nombre; 
	}
	
	public int getAnio() { 
		return anio; 
	}

	public String getEntidadOrganizadora() {
		return entidadOrganizadora;
	}

	public int getMinimoJugadores() {
		return minimoJugadores;
	}
	
	public String getTemporada() { 
		return temporada; 
	}
	
	public FormatoCompetencia getFormato() { 
		return formato; 
	}
	
	public DeporteConjunto getDeporte() { 
		return deporte; 
	}
	
	public List<Categoria> getCategoriasPermitidas() { 
		return categoriasPermitidas; 
	}
	
	public List<Equipo> getEquipos() { 
		return equipos; 
	}
	
	public List<PartidoCompetencia> getPartidos() { 
		return partidos; 
	}
	
	public int getPuntos(Equipo equipo) { 
		return puntos.getOrDefault(equipo, 0); 
	}

	
}
