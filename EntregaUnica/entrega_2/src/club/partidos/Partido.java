package club.partidos;

import java.util.ArrayList;
import java.util.List;

import club.deportes.Baloncesto;
import club.deportes.Deporte;
import club.deportes.Futbol;
import club.deportes.Padel;
import club.deportes.Tenis;
import club.deportes.TenisDeMesa;
import club.deportes.Voleibol;
import club.personal.Entrenador;
import club.usuarios.Socio;

public class Partido {

	private static final List<Partido> partidos = new ArrayList<Partido>();
	protected Entrenador entrenador;
	protected Deporte deporte;
	protected List<Socio> participantes;
	protected List<EstadisticaJugador> estadisticas = new ArrayList<EstadisticaJugador>();

	public Partido(Entrenador entrenador, Deporte deporte, List<Socio> participantes) {
		if (entrenador == null || deporte == null || participantes == null) {
			throw new IllegalArgumentException("Faltan datos del partido");
		}
		this.entrenador = entrenador;
		this.deporte = deporte;
		this.participantes = new ArrayList<Socio>(participantes);
		partidos.add(this);
	}

	protected Partido() { }

	protected void restaurarDatos(Entrenador entrenador, Deporte deporte, List<Socio> participantes) {
		if (entrenador == null || deporte == null || participantes == null) {
			throw new IllegalArgumentException("Faltan datos del partido");
		}
		this.entrenador = entrenador; this.deporte = deporte;
		this.participantes = new ArrayList<Socio>(participantes); partidos.add(this);
	}

	public static Partido restaurar(Entrenador entrenador, Deporte deporte, List<Socio> participantes) {
		Partido partido = new Partido(); partido.restaurarDatos(entrenador, deporte, participantes); return partido;
	}

	public void restaurarEstadistica(EstadisticaJugador estadistica) {
		if (estadistica == null || !participantes.contains(estadistica.getSocio())) {
			throw new IllegalArgumentException("La estadística no corresponde a un participante");
		}
		estadistica.asociarPartido(this); estadisticas.add(estadistica);
	}

	public void registrarEstadistica(Entrenador entrenador, EstadisticaJugador estadistica) {
		if (!partidos.contains(this)) {
			throw new IllegalStateException("El evento no existe");
		}
		if (this.entrenador != entrenador) {
			throw new IllegalStateException("El entrenador no administra este evento");
		}
		if (estadistica == null || !participantes.contains(estadistica.getSocio())) {
			throw new IllegalArgumentException("El jugador no participó en el evento");
		}
		if (!correspondeAlDeporte(estadistica)) {
			throw new IllegalArgumentException("La estadística no corresponde al deporte");
		}
		estadistica.asociarPartido(this);
		estadisticas.add(estadistica);
		if (estadistica instanceof EstadisticaFutbol) {
			EstadisticaFutbol futbol = (EstadisticaFutbol) estadistica;
			if (futbol.getAmarillas() >= 2 || futbol.isRoja()) {
				estadistica.getSocio().registrarSuspension(1);
			}
		}
	}

	private boolean correspondeAlDeporte(EstadisticaJugador estadistica) {
		if (deporte instanceof Futbol) 
			return estadistica instanceof EstadisticaFutbol;
		if (deporte instanceof Baloncesto) 
			return estadistica instanceof EstadisticaBaloncesto;
		if (deporte instanceof Voleibol) 
			return estadistica instanceof EstadisticaVoleibol;
		if (deporte instanceof Tenis || deporte instanceof Padel) 
			return estadistica instanceof EstadisticaTenis;
		if (deporte instanceof TenisDeMesa) 
			return estadistica instanceof EstadisticaTenisDeMesa;
		return false;
	}

	public static List<Partido> getPartidos() { 
		return partidos; 
	}
	
	public Entrenador getEntrenador() { 
		return entrenador;
	}
	
	public Deporte getDeporte() { 
		return deporte; 
	}
	
	public List<Socio> getParticipantes() { 
		return participantes; 
	}
	
	public List<EstadisticaJugador> getEstadisticas() { 
		return estadisticas; 
	}
	
	public static List<EstadisticaJugador> consultarEstadisticas(Partido partido) {
		if (partido == null || !partidos.contains(partido)) return new ArrayList<EstadisticaJugador>();
		return new ArrayList<EstadisticaJugador>(partido.estadisticas);
	}

	public static List<EstadisticaJugador> consultarEstadisticas(Socio socio) {
		List<EstadisticaJugador> encontradas = new ArrayList<EstadisticaJugador>();
		for (Partido partido : partidos) {
			for (EstadisticaJugador estadistica : partido.estadisticas) {
				if (estadistica.getSocio() == socio) encontradas.add(estadistica);
			}
		}
		return encontradas;
	}
}
