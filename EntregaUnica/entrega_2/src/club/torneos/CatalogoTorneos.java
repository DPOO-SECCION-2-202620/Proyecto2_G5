package club.torneos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CatalogoTorneos {
	private static final Map<Integer, TorneoInterno> torneos = new LinkedHashMap<Integer, TorneoInterno>();
	private static int siguienteId = 1;

	private CatalogoTorneos() { }

	static int registrar(TorneoInterno torneo) {
		int id = siguienteId++;
		torneos.put(id, torneo);
		return id;
	}

	static void restaurarId(TorneoInterno torneo, int idAnterior, int id) {
		torneos.remove(idAnterior);
		torneos.put(id, torneo);
		siguienteId = Math.max(siguienteId, id + 1);
	}

	public static TorneoInterno buscar(int id) {
		return torneos.get(id);
	}

	public static List<TorneoInterno> getTorneos() {
		return new ArrayList<TorneoInterno>(torneos.values());
	}

	public static void limpiar() {
		torneos.clear();
		siguienteId = 1;
	}

	public static int getSiguienteId() { return siguienteId; }

	public static void restaurarCatalogo(List<TorneoInterno> originales, int siguiente) {
		torneos.clear();
		for (TorneoInterno torneo : originales) {
			torneos.put(torneo.getId(), torneo);
		}
		siguienteId = siguiente;
	}
}
