package club.persistencia;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.deportes.Deporte;
import club.deportes.DeporteIndividual;
import club.deportes.Modalidad;
import club.instalaciones.Instalacion;
import club.instalaciones.Reserva;
import club.usuarios.Socio;

final class PersistenciaReservasJson {

	static final String ARCHIVO = "instalaciones-reservas.json";

	void cargar(List<Map<String, String>> registros) {
		for (Map<String, String> registro : registros) {
			if (!"INSTALACION".equals(registro.get("registro"))) continue;
			String nombreDeporte = registro.get("deporte");
			DeporteIndividual deporte = nombreDeporte == null || nombreDeporte.isEmpty() ? null
					: (DeporteIndividual) Deporte.getDeportesRegistrados().get(nombreDeporte);
			List<Modalidad> modalidades = new ArrayList<Modalidad>();
			for (String valor : PersistenciaSoporte.separar(registro.get("modalidades"))) modalidades.add(Modalidad.valueOf(valor));
			new Instalacion(PersistenciaSoporte.requerido(registro, "nombre"), deporte, modalidades);
		}
		for (Map<String, String> registro : registros) {
			if (!"RESERVA".equals(registro.get("registro"))) continue;
			List<Socio> participantes = new ArrayList<Socio>();
			for (String email : PersistenciaSoporte.separar(PersistenciaSoporte.requerido(registro, "participantes"))) participantes.add(PersistenciaSoporte.socio(email));
			Reserva.restaurar(participantes, Instalacion.getInstalaciones().get(PersistenciaSoporte.requerido(registro, "instalacion")),
					LocalDateTime.parse(PersistenciaSoporte.requerido(registro, "fechaHora")),
					Duration.ofSeconds(Long.parseLong(PersistenciaSoporte.requerido(registro, "duracion"))),
					Modalidad.valueOf(PersistenciaSoporte.requerido(registro, "modalidad")));
		}
	}

	String serializar() {
		List<String> objetos = new ArrayList<String>();
		for (Instalacion instalacion : Instalacion.getInstalaciones().values()) {
			Map<String, String> valores = new LinkedHashMap<String, String>();
			valores.put("registro", "INSTALACION"); valores.put("nombre", instalacion.getNombre());
			valores.put("deporte", instalacion.getDeporteIndividual() == null ? "" : instalacion.getDeporteIndividual().getNombre());
			valores.put("modalidades", PersistenciaSoporte.unirEnums(instalacion.getModalidades())); objetos.add(Json.objeto(valores));
		}
		for (Reserva reserva : Reserva.getReservas()) {
			Map<String, String> valores = new LinkedHashMap<String, String>();
			valores.put("registro", "RESERVA"); valores.put("participantes", PersistenciaSoporte.unirSocios(reserva.getParticipantes()));
			valores.put("instalacion", reserva.getInstalacion().getNombre()); valores.put("fechaHora", reserva.getFechaHora().toString());
			valores.put("duracion", String.valueOf(reserva.getDuracion().getSeconds())); valores.put("modalidad", reserva.getModalidad().name());
			objetos.add(Json.objeto(valores));
		}
		return Json.arreglo(objetos);
	}
}
