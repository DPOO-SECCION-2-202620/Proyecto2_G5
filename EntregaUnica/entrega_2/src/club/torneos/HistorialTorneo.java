package club.torneos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HistorialTorneo {
	private TorneoInterno torneo;
	private List<InscripcionTorneo> inscripciones;
	private List<EncuentroTorneo> encuentros;
	private Map<UnidadParticipacion, Integer> posiciones;

	HistorialTorneo(TorneoInterno torneo) {
		this.torneo = torneo;
		this.inscripciones = torneo.getInscripciones();
		this.encuentros = torneo.getEncuentros();
		this.posiciones = torneo.getPosiciones();
	}

	public TorneoInterno getTorneo() { return torneo; }
	public List<InscripcionTorneo> getInscripciones() { return new ArrayList<InscripcionTorneo>(inscripciones); }
	public List<EncuentroTorneo> getEncuentros() { return new ArrayList<EncuentroTorneo>(encuentros); }
	public Map<UnidadParticipacion, Integer> getPosiciones() {
		return new LinkedHashMap<UnidadParticipacion, Integer>(posiciones);
	}
}
