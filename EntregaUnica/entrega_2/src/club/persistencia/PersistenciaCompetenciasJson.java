package club.persistencia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.competencias.Competencia;
import club.competencias.FormatoCompetencia;
import club.deportes.Categoria;
import club.deportes.Deporte;
import club.deportes.DeporteConjunto;
import club.deportes.Equipo;

final class PersistenciaCompetenciasJson {

	static final String ARCHIVO = "competencias.json";

	void cargar(List<Map<String, String>> registros) {
		for (Map<String, String> registro : registros) {
			if (!"COMPETENCIA".equals(registro.get("registro"))) continue;
			List<Categoria> categorias = new ArrayList<Categoria>();
			for (String categoria : PersistenciaSoporte.separar(PersistenciaSoporte.requerido(registro, "categorias"))) categorias.add(Categoria.valueOf(categoria));
			new Competencia(PersistenciaSoporte.requerido(registro, "nombre"), PersistenciaSoporte.requerido(registro, "entidad"),
					Integer.parseInt(PersistenciaSoporte.requerido(registro, "anio")), PersistenciaSoporte.requerido(registro, "temporada"),
					FormatoCompetencia.valueOf(PersistenciaSoporte.requerido(registro, "formato")),
					(DeporteConjunto) Deporte.getDeportesRegistrados().get(PersistenciaSoporte.requerido(registro, "deporte")),
					categorias, Integer.parseInt(PersistenciaSoporte.requerido(registro, "minimoJugadores")));
		}
		for (Map<String, String> registro : registros) if ("PARTICIPACION".equals(registro.get("registro")))
			Competencia.getCompetencias().get(PersistenciaSoporte.requerido(registro, "competencia")).restaurarEquipo(
					Equipo.getEquiposRegistrados().get(PersistenciaSoporte.requerido(registro, "equipo")),
					Integer.parseInt(PersistenciaSoporte.requerido(registro, "puntos")));
	}

	String serializar() {
		List<String> objetos = new ArrayList<String>();
		for (Competencia competencia : Competencia.getCompetencias().values()) {
			Map<String, String> valores = new LinkedHashMap<String, String>();
			valores.put("registro", "COMPETENCIA"); valores.put("nombre", competencia.getNombre());
			valores.put("entidad", competencia.getEntidadOrganizadora()); valores.put("anio", String.valueOf(competencia.getAnio()));
			valores.put("temporada", competencia.getTemporada()); valores.put("formato", competencia.getFormato().name());
			valores.put("deporte", competencia.getDeporte().getNombre()); valores.put("categorias", PersistenciaSoporte.unirEnums(competencia.getCategoriasPermitidas()));
			valores.put("minimoJugadores", String.valueOf(competencia.getMinimoJugadores())); objetos.add(Json.objeto(valores));
			for (Equipo equipo : competencia.getEquipos()) objetos.add(Json.objeto(Map.of("registro", "PARTICIPACION",
					"competencia", competencia.getNombre(), "equipo", equipo.getNombre(), "puntos", String.valueOf(competencia.getPuntos(equipo)))));
		}
		return Json.arreglo(objetos);
	}
}
