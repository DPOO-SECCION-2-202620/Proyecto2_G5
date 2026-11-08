package club.persistencia;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.deportes.Deporte;
import club.entrenamientos.EstadoAsistencia;
import club.entrenamientos.RegistroEntrenamiento;
import club.entrenamientos.SesionEntrenamiento;
import club.instalaciones.Instalacion;
import club.personal.Entrenador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;

final class PersistenciaEntrenamientosJson {

	static final String ARCHIVO = "entrenamientos.json";

	void cargar(List<Map<String, String>> registros) {
		Map<String, SesionEntrenamiento> sesiones = new LinkedHashMap<String, SesionEntrenamiento>();
		for (Map<String, String> registro : registros) {
			if (!"SESION".equals(registro.get("registro"))) continue;
			List<Socio> convocados = new ArrayList<Socio>();
			for (String email : PersistenciaSoporte.separar(PersistenciaSoporte.requerido(registro, "convocados"))) convocados.add(PersistenciaSoporte.socio(email));
			SesionEntrenamiento sesion = SesionEntrenamiento.restaurar(
					(Entrenador) RepositorioUsuarios.buscarPorEmail(PersistenciaSoporte.requerido(registro, "entrenador")),
					Deporte.getDeportesRegistrados().get(PersistenciaSoporte.requerido(registro, "deporte")),
					Instalacion.getInstalaciones().get(PersistenciaSoporte.requerido(registro, "instalacion")),
					LocalDateTime.parse(PersistenciaSoporte.requerido(registro, "fechaHora")),
					Duration.ofSeconds(Long.parseLong(PersistenciaSoporte.requerido(registro, "duracion"))), convocados);
			sesiones.put(PersistenciaSoporte.requerido(registro, "id"), sesion);
		}
		for (Map<String, String> registro : registros) {
			if (!"REGISTRO".equals(registro.get("registro"))) continue;
			String calificacion = PersistenciaSoporte.requerido(registro, "calificacion");
			sesiones.get(PersistenciaSoporte.requerido(registro, "sesion")).restaurarRegistro(
					PersistenciaSoporte.socio(PersistenciaSoporte.requerido(registro, "socio")),
					EstadoAsistencia.valueOf(PersistenciaSoporte.requerido(registro, "estado")),
					calificacion.isEmpty() ? null : Integer.valueOf(calificacion), PersistenciaSoporte.requerido(registro, "comentario"));
		}
	}

	String serializar() {
		List<String> objetos = new ArrayList<String>();
		for (int i = 0; i < SesionEntrenamiento.getSesiones().size(); i++) {
			SesionEntrenamiento sesion = SesionEntrenamiento.getSesiones().get(i); String id = String.valueOf(i);
			Map<String, String> valores = new LinkedHashMap<String, String>();
			valores.put("registro", "SESION"); valores.put("id", id); valores.put("entrenador", sesion.getEntrenador().getEmail());
			valores.put("deporte", sesion.getDeporte().getNombre()); valores.put("instalacion", sesion.getInstalacion().getNombre());
			valores.put("fechaHora", sesion.getFechaHora().toString()); valores.put("duracion", String.valueOf(sesion.getDuracion().getSeconds()));
			valores.put("convocados", PersistenciaSoporte.unirSocios(sesion.getConvocados())); objetos.add(Json.objeto(valores));
			for (RegistroEntrenamiento registro : sesion.getRegistros().values()) {
				Map<String, String> evaluacion = new LinkedHashMap<String, String>();
				evaluacion.put("registro", "REGISTRO"); evaluacion.put("sesion", id); evaluacion.put("socio", registro.getSocio().getEmail());
				evaluacion.put("estado", registro.getEstado().name()); evaluacion.put("calificacion", registro.getCalificacion() == null ? "" : registro.getCalificacion().toString());
				evaluacion.put("comentario", registro.getComentario() == null ? "" : registro.getComentario()); objetos.add(Json.objeto(evaluacion));
			}
		}
		return Json.arreglo(objetos);
	}
}
