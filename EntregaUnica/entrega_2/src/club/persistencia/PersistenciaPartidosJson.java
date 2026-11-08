package club.persistencia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.competencias.Competencia;
import club.competencias.PartidoCompetencia;
import club.deportes.Deporte;
import club.deportes.DeporteIndividual;
import club.deportes.Equipo;
import club.deportes.Modalidad;
import club.partidos.EstadisticaBaloncesto;
import club.partidos.EstadisticaFutbol;
import club.partidos.EstadisticaJugador;
import club.partidos.EstadisticaTenis;
import club.partidos.EstadisticaTenisDeMesa;
import club.partidos.EstadisticaVoleibol;
import club.partidos.Partido;
import club.partidos.PartidoInterno;
import club.partidos.PartidoPractica;
import club.personal.Entrenador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;
import club.usuarios.Usuario;

final class PersistenciaPartidosJson {

	static final String ARCHIVO = "partidos-estadisticas.json";

	void cargar(List<Map<String, String>> registros) {
		Map<String, Partido> partidos = new LinkedHashMap<String, Partido>();
		for (Map<String, String> registro : registros) {
			if (!"PARTIDO".equals(registro.get("registro"))) continue;
			List<Socio> participantes = new ArrayList<Socio>();
			for (String email : PersistenciaSoporte.separar(PersistenciaSoporte.requerido(registro, "participantes"))) participantes.add(PersistenciaSoporte.socio(email));
			Entrenador entrenador = (Entrenador) RepositorioUsuarios.buscarPorEmail(PersistenciaSoporte.requerido(registro, "entrenador"));
			String tipo = PersistenciaSoporte.requerido(registro, "tipo"); Partido partido;
			if ("INTERNO".equals(tipo)) {
				partido = PartidoInterno.restaurar(Equipo.getEquiposRegistrados().get(PersistenciaSoporte.requerido(registro, "local")),
						Equipo.getEquiposRegistrados().get(PersistenciaSoporte.requerido(registro, "visitante")),
						LocalDateTime.parse(PersistenciaSoporte.requerido(registro, "fecha")), entrenador, participantes);
			} else if ("COMPETENCIA".equals(tipo)) {
				String local = PersistenciaSoporte.requerido(registro, "marcadorLocal");
				String visitante = PersistenciaSoporte.requerido(registro, "marcadorVisitante");
				partido = PartidoCompetencia.restaurar(Competencia.getCompetencias().get(PersistenciaSoporte.requerido(registro, "competencia")),
						Equipo.getEquiposRegistrados().get(PersistenciaSoporte.requerido(registro, "local")),
						Equipo.getEquiposRegistrados().get(PersistenciaSoporte.requerido(registro, "visitante")),
						LocalDateTime.parse(PersistenciaSoporte.requerido(registro, "fecha")), entrenador, participantes,
						local.isEmpty() ? null : Integer.valueOf(local), visitante.isEmpty() ? null : Integer.valueOf(visitante));
			} else partido = Partido.restaurar(entrenador,
					Deporte.getDeportesRegistrados().get(PersistenciaSoporte.requerido(registro, "deporte")), participantes);
			partidos.put(PersistenciaSoporte.requerido(registro, "id"), partido);
		}
		for (Map<String, String> registro : registros) {
			if ("ESTADISTICA".equals(registro.get("registro")))
				partidos.get(PersistenciaSoporte.requerido(registro, "partido")).restaurarEstadistica(crearEstadistica(registro));
			else if ("PRACTICA".equals(registro.get("registro"))) {
				List<Usuario> participantes = new ArrayList<Usuario>();
				for (String email : PersistenciaSoporte.separar(PersistenciaSoporte.requerido(registro, "participantes"))) participantes.add(RepositorioUsuarios.buscarPorEmail(email));
				PartidoPractica.restaurar((DeporteIndividual) Deporte.getDeportesRegistrados().get(PersistenciaSoporte.requerido(registro, "deporte")),
						Modalidad.valueOf(PersistenciaSoporte.requerido(registro, "modalidad")), participantes,
						Boolean.parseBoolean(PersistenciaSoporte.requerido(registro, "finalizado")));
			}
		}
	}

	String serializar() {
		List<String> objetos = new ArrayList<String>();
		for (int i = 0; i < Partido.getPartidos().size(); i++) {
			Partido partido = Partido.getPartidos().get(i); String id = String.valueOf(i);
			Map<String, String> valores = new LinkedHashMap<String, String>();
			valores.put("registro", "PARTIDO"); valores.put("id", id); valores.put("entrenador", partido.getEntrenador().getEmail());
			valores.put("deporte", partido.getDeporte().getNombre()); valores.put("participantes", PersistenciaSoporte.unirSocios(partido.getParticipantes()));
			if (partido instanceof PartidoCompetencia) {
				PartidoCompetencia pc = (PartidoCompetencia) partido; valores.put("tipo", "COMPETENCIA");
				valores.put("competencia", pc.getCompetencia().getNombre()); valores.put("local", pc.getEquipoLocal().getNombre());
				valores.put("visitante", pc.getEquipoVisitante().getNombre()); valores.put("fecha", pc.getFecha().toString());
				valores.put("marcadorLocal", pc.getMarcadorLocal() == null ? "" : pc.getMarcadorLocal().toString());
				valores.put("marcadorVisitante", pc.getMarcadorVisitante() == null ? "" : pc.getMarcadorVisitante().toString());
			} else if (partido instanceof PartidoInterno) {
				PartidoInterno pi = (PartidoInterno) partido; valores.put("tipo", "INTERNO");
				valores.put("local", pi.getEquipoLocal().getNombre()); valores.put("visitante", pi.getEquipoVisitante().getNombre());
				valores.put("fecha", pi.getFecha().toString());
			} else valores.put("tipo", "EVENTO");
			objetos.add(Json.objeto(valores));
			for (EstadisticaJugador estadistica : partido.getEstadisticas()) objetos.add(serializarEstadistica(id, estadistica));
		}
		for (PartidoPractica partido : PartidoPractica.getPartidos()) {
			Map<String, String> valores = new LinkedHashMap<String, String>(); valores.put("registro", "PRACTICA");
			valores.put("deporte", partido.getDeporte().getNombre()); valores.put("modalidad", partido.getModalidad().name());
			List<String> emails = new ArrayList<String>(); for (Usuario usuario : partido.getParticipantes()) emails.add(usuario.getEmail());
			valores.put("participantes", String.join("|", emails)); valores.put("finalizado", String.valueOf(partido.isFinalizado()));
			objetos.add(Json.objeto(valores));
		}
		return Json.arreglo(objetos);
	}

	private String serializarEstadistica(String partido, EstadisticaJugador estadistica) {
		Map<String, String> valores = new LinkedHashMap<String, String>(); valores.put("registro", "ESTADISTICA");
		valores.put("partido", partido); valores.put("socio", estadistica.getSocio().getEmail());
		if (estadistica instanceof EstadisticaFutbol) {
			EstadisticaFutbol e = (EstadisticaFutbol) estadistica; valores.put("tipo", "FUTBOL");
			valores.put("a", String.valueOf(e.getGoles())); valores.put("b", String.valueOf(e.getAsistencias()));
			valores.put("c", String.valueOf(e.getAmarillas())); valores.put("d", String.valueOf(e.isRoja()));
		} else if (estadistica instanceof EstadisticaBaloncesto) {
			EstadisticaBaloncesto e = (EstadisticaBaloncesto) estadistica; valores.put("tipo", "BALONCESTO");
			valores.put("a", String.valueOf(e.getPuntos())); valores.put("b", String.valueOf(e.getRebotes()));
		} else if (estadistica instanceof EstadisticaVoleibol) {
			EstadisticaVoleibol e = (EstadisticaVoleibol) estadistica; valores.put("tipo", "VOLEIBOL");
			valores.put("a", String.valueOf(e.getAces())); valores.put("b", String.valueOf(e.getBloqueos()));
		} else if (estadistica instanceof EstadisticaTenis) {
			EstadisticaTenis e = (EstadisticaTenis) estadistica; valores.put("tipo", "TENIS");
			valores.put("a", String.valueOf(e.getSetsGanados())); valores.put("b", String.valueOf(e.getGamesGanados()));
			valores.put("c", String.valueOf(e.getAces())); valores.put("d", String.valueOf(e.getErroresNoForzados()));
		} else {
			EstadisticaTenisDeMesa e = (EstadisticaTenisDeMesa) estadistica; valores.put("tipo", "TENIS_MESA");
			valores.put("a", String.valueOf(e.getSetsGanados())); valores.put("b", String.valueOf(e.getPuntosTotales()));
		}
		return Json.objeto(valores);
	}

	private EstadisticaJugador crearEstadistica(Map<String, String> registro) {
		Socio socio = PersistenciaSoporte.socio(PersistenciaSoporte.requerido(registro, "socio"));
		String tipo = PersistenciaSoporte.requerido(registro, "tipo");
		int a = Integer.parseInt(PersistenciaSoporte.requerido(registro, "a")); int b = Integer.parseInt(PersistenciaSoporte.requerido(registro, "b"));
		if ("FUTBOL".equals(tipo)) return new EstadisticaFutbol(socio, a, b, Integer.parseInt(PersistenciaSoporte.requerido(registro, "c")), Boolean.parseBoolean(PersistenciaSoporte.requerido(registro, "d")));
		if ("BALONCESTO".equals(tipo)) return new EstadisticaBaloncesto(socio, a, b);
		if ("VOLEIBOL".equals(tipo)) return new EstadisticaVoleibol(socio, a, b);
		if ("TENIS".equals(tipo)) return new EstadisticaTenis(socio, a, b, Integer.parseInt(PersistenciaSoporte.requerido(registro, "c")), Integer.parseInt(PersistenciaSoporte.requerido(registro, "d")));
		if ("TENIS_MESA".equals(tipo)) return new EstadisticaTenisDeMesa(socio, a, b);
		throw new PersistenciaException("Tipo de estadística desconocido");
	}
}
