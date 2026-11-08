package club.torneos;

import club.deportes.Equipo;
import club.deportes.Futbol;

public class EncuentroTorneo {
	private TorneoInterno torneo;
	private UnidadParticipacion participante1;
	private UnidadParticipacion participante2;
	private Integer puntaje1;
	private Integer puntaje2;

	private EncuentroTorneo(TorneoInterno torneo, UnidadParticipacion participante1,
			UnidadParticipacion participante2) {
		if (torneo == null || participante1 == null || participante2 == null
				|| participante1 == participante2) {
			throw new IllegalArgumentException("Los participantes del encuentro no son válidos");
		}
		this.torneo = torneo;
		this.participante1 = participante1;
		this.participante2 = participante2;
	}

	public EncuentroTorneo(TorneoInterno torneo, Object participante1, Object participante2) {
		this(torneo, torneo == null ? null : torneo.obtenerUnidad(participante1),
				torneo == null ? null : torneo.obtenerUnidad(participante2));
		torneo.registrarEncuentro(this);
	}

	static EncuentroTorneo restaurar(TorneoInterno torneo, UnidadParticipacion participante1,
			UnidadParticipacion participante2) {
		return new EncuentroTorneo(torneo, participante1, participante2);
	}

	public void registrarResultado(int puntaje1, int puntaje2) {
		if (tieneResultado()) throw new IllegalStateException("El encuentro ya tiene resultado");
		if (puntaje1 < 0 || puntaje2 < 0) throw new IllegalArgumentException("El resultado no es válido");
		if (puntaje1 == puntaje2 && !(torneo.getDeporte() instanceof Futbol)) {
			throw new IllegalArgumentException("El deporte no permite empates");
		}
		this.puntaje1 = puntaje1;
		this.puntaje2 = puntaje2;
		torneo.registrarResultado(this);
		actualizarRankingEquipos();
	}

	void restaurarResultado(int puntaje1, int puntaje2) {
		this.puntaje1 = puntaje1;
		this.puntaje2 = puntaje2;
		torneo.registrarResultado(this);
	}

	private void actualizarRankingEquipos() {
		Object primero = participanteSimple(participante1);
		Object segundo = participanteSimple(participante2);
		if (!(primero instanceof Equipo) || !(segundo instanceof Equipo)) return;
		Equipo equipo1 = (Equipo) primero;
		Equipo equipo2 = (Equipo) segundo;
		if (puntaje1.equals(puntaje2)) {
			equipo1.sumarPuntosRanking(1); equipo2.sumarPuntosRanking(1);
		} else if (puntaje1 > puntaje2) equipo1.sumarPuntosRanking(3);
		else equipo2.sumarPuntosRanking(3);
	}

	private Object participanteSimple(UnidadParticipacion unidad) {
		return unidad.getIntegrantes().size() == 1 ? unidad.getIntegrantes().get(0) : unidad;
	}

	public TorneoInterno getTorneo() { return torneo; }
	public Object getParticipante1() { return participanteSimple(participante1); }
	public Object getParticipante2() { return participanteSimple(participante2); }
	public UnidadParticipacion getUnidad1() { return participante1; }
	public UnidadParticipacion getUnidad2() { return participante2; }
	public Integer getPuntaje1() { return puntaje1; }
	public Integer getPuntaje2() { return puntaje2; }
	public boolean tieneResultado() { return puntaje1 != null; }
	public boolean isFinalizado() { return tieneResultado(); }
}
