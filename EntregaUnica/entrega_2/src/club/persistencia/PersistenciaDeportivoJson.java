package club.persistencia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.Inscripciones.InscripcionAcademia;
import club.Inscripciones.InscripcionEquipo;
import club.deportes.Categoria;
import club.deportes.Deporte;
import club.deportes.DeporteConjunto;
import club.deportes.DeporteIndividual;
import club.deportes.Equipo;
import club.usuarios.Socio;

final class PersistenciaDeportivoJson {

	static final String ARCHIVO = "deportivo.json";

	void cargar(List<Map<String, String>> registros) {
		Map<Equipo, Boolean> estados = new LinkedHashMap<Equipo, Boolean>();
		for (Map<String, String> registro : registros)
			if ("DEPORTE".equals(registro.get("registro"))) PersistenciaSoporte.crearDeporte(PersistenciaSoporte.requerido(registro, "tipo"));
		for (Map<String, String> registro : registros) {
			if (!"EQUIPO".equals(registro.get("registro"))) continue;
			Deporte deporte = Deporte.getDeportesRegistrados().get(PersistenciaSoporte.requerido(registro, "deporte"));
			Equipo equipo = new Equipo(PersistenciaSoporte.requerido(registro, "nombre"),
					Categoria.valueOf(PersistenciaSoporte.requerido(registro, "categoria")),
					Integer.parseInt(PersistenciaSoporte.requerido(registro, "cupoMaximo")), deporte) { };
			equipo.sumarPuntosRanking(Integer.parseInt(registro.getOrDefault("puntosRanking", "0")));
			estados.put(equipo, Boolean.parseBoolean(PersistenciaSoporte.requerido(registro, "activo")));
		}
		for (Map<String, String> registro : registros) {
			if ("INSCRIPCION_INDIVIDUAL".equals(registro.get("registro"))) {
				Socio socio = PersistenciaSoporte.socio(PersistenciaSoporte.requerido(registro, "socio"));
				DeporteIndividual deporte = (DeporteIndividual) Deporte.getDeportesRegistrados().get(PersistenciaSoporte.requerido(registro, "deporte"));
				InscripcionAcademia inscripcion = InscripcionAcademia.inscribirSocio(socio, deporte);
				inscripcion.actualizarElo(Double.parseDouble(PersistenciaSoporte.requerido(registro, "elo")));
			} else if ("INSCRIPCION_EQUIPO".equals(registro.get("registro"))) {
				Socio socio = PersistenciaSoporte.socio(PersistenciaSoporte.requerido(registro, "socio"));
				Equipo equipo = Equipo.getEquiposRegistrados().get(PersistenciaSoporte.requerido(registro, "equipo"));
				InscripcionEquipo.restaurar(socio, equipo,
						PersistenciaSoporte.posicion((DeporteConjunto) equipo.getDeporte(), PersistenciaSoporte.requerido(registro, "posicion")),
						Integer.parseInt(PersistenciaSoporte.requerido(registro, "camiseta")));
			}
		}
		for (Map.Entry<Equipo, Boolean> estado : estados.entrySet()) estado.getKey().setActivo(estado.getValue());
	}

	String serializar() {
		List<String> objetos = new ArrayList<String>();
		for (Deporte deporte : Deporte.getDeportesRegistrados().values())
			objetos.add(Json.objeto(Map.of("registro", "DEPORTE", "tipo", deporte.getClass().getSimpleName())));
		for (Equipo equipo : Equipo.getEquiposRegistrados().values()) {
			Map<String, String> valores = new LinkedHashMap<String, String>();
			valores.put("registro", "EQUIPO"); valores.put("nombre", equipo.getNombre());
			valores.put("categoria", equipo.getCategoria().name()); valores.put("cupoMaximo", String.valueOf(equipo.getCupoMaximo()));
			valores.put("deporte", equipo.getDeporte().getNombre()); valores.put("activo", String.valueOf(equipo.isActivo()));
			valores.put("puntosRanking", String.valueOf(equipo.getPuntosRanking()));
			objetos.add(Json.objeto(valores));
		}
		for (InscripcionAcademia inscripcion : InscripcionAcademia.getInscripciones())
			objetos.add(Json.objeto(Map.of("registro", "INSCRIPCION_INDIVIDUAL", "socio", inscripcion.getSocio().getEmail(),
					"deporte", inscripcion.getDeporte().getNombre(), "elo", String.valueOf(inscripcion.getElo()))));
		for (Equipo equipo : Equipo.getEquiposRegistrados().values()) for (InscripcionEquipo inscripcion : equipo.getNomina())
			objetos.add(Json.objeto(Map.of("registro", "INSCRIPCION_EQUIPO", "socio", inscripcion.getSocio().getEmail(),
					"equipo", equipo.getNombre(), "posicion", inscripcion.getPosicion().name(), "camiseta", String.valueOf(inscripcion.getNumeroCamiseta()))));
		return Json.arreglo(objetos);
	}
}
