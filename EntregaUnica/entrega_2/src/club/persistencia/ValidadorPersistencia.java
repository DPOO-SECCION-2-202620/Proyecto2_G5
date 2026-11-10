package club.persistencia;
//Hecho por IA

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.competencias.FormatoCompetencia;
import club.deportes.Categoria;
import club.deportes.Modalidad;
import club.entrenamientos.EstadoAsistencia;
import club.torneos.EstadoInscripcion;
import club.torneos.EstadoTorneo;
import club.torneos.ModalidadTorneo;
import club.torneos.TipoTorneo;
import club.torneos.Torneo;
import club.tienda.EstadoBono;

final class ValidadorPersistencia {

	void validar(Map<String, List<Map<String, String>>> datos, int cantidadArchivos) {
		if (datos.size() != cantidadArchivos) throw new PersistenciaException("El conjunto está incompleto");
		Map<String, String> usuarios = validarUsuarios(datos.get(PersistenciaUsuariosJson.ARCHIVO));
		Map<String, String> deportes = new LinkedHashMap<String, String>();
		Map<String, String> equipos = validarDeportivo(datos.get(PersistenciaDeportivoJson.ARCHIVO), usuarios, deportes);
		validarTorneos(datos.get(PersistenciaTorneosJson.ARCHIVO), usuarios, deportes, equipos);
		Map<String, Boolean> instalaciones = validarReservas(datos.get(PersistenciaReservasJson.ARCHIVO), usuarios, deportes);
		Map<String, Boolean> competencias = validarCompetencias(datos.get(PersistenciaCompetenciasJson.ARCHIVO), deportes, equipos);
		validarPartidos(datos.get(PersistenciaPartidosJson.ARCHIVO), usuarios, deportes, equipos, competencias);
		validarEntrenamientos(datos.get(PersistenciaEntrenamientosJson.ARCHIVO), usuarios, deportes, instalaciones);
		validarComercial(datos.get(PersistenciaComercialJson.ARCHIVO), usuarios);
	}

	private void validarComercial(List<Map<String, String>> registros, Map<String, String> usuarios) {
		for (Map<String, String> r : registros) {
			String registro = r.getOrDefault("registro", "VENTA");
			if (List.of("PRODUCTO", "ALERGENO", "UBICACION", "INVENTARIO", "CODIGO", "SUGERENCIA", "DETALLE").contains(registro)) continue;
			if (!"VENTA".equals(registro)) throw new PersistenciaException("Registro comercial desconocido");
			if (!List.of("TIENDA", "CAFETERIA").contains(requerido(r, "tipo"))) throw new PersistenciaException("Tipo de venta desconocido");
			if (!usuarios.containsKey(requerido(r, "comprador"))) throw new PersistenciaException("Comprador inexistente");
			LocalDateTime.parse(requerido(r, "fecha")); Double.parseDouble(requerido(r, "subtotal"));
			Double.parseDouble(requerido(r, "impuestos")); Double.parseDouble(requerido(r, "descuentos"));
			Double.parseDouble(requerido(r, "total")); Integer.parseInt(requerido(r, "puntosGanados"));
			Integer.parseInt(requerido(r, "puntosRedimidos")); requerido(r, "bono");
			Double.parseDouble(requerido(r, "montoOriginal")); Double.parseDouble(requerido(r, "valorDescontado"));
		}
	}

	private void validarTorneos(List<Map<String, String>> registros, Map<String, String> usuarios,
			Map<String, String> deportes, Map<String, String> equipos) {
		Map<String, Boolean> torneos = new LinkedHashMap<String, Boolean>();
		for (Map<String, String> r : registros) if ("TORNEO".equals(r.get("registro"))) {
			String id = requerido(r, "id");
			if (!"ADMINISTRADOR".equals(usuarios.get(requerido(r, "administrador")))) throw new PersistenciaException("Administrador de torneo inexistente");
			if (!deportes.containsKey(requerido(r, "deporte"))) throw new PersistenciaException("Deporte de torneo inexistente");
			requerido(r, "categoria"); ModalidadTorneo.valueOf(requerido(r, "modalidad"));
			if (Integer.parseInt(requerido(r, "cupos")) <= 0) throw new PersistenciaException("Cupos de torneo inválidos");
			TipoTorneo.valueOf(requerido(r, "tipo")); Double.parseDouble(requerido(r, "tarifa"));
			Torneo.Formato.valueOf(requerido(r, "formato")); EstadoTorneo.valueOf(requerido(r, "estado"));
			String fecha = requerido(r, "fechaInicio"); if (!fecha.isEmpty()) LocalDateTime.parse(fecha);
			if (torneos.put(id, true) != null) throw new PersistenciaException("Torneo duplicado");
		}
		for (Map<String, String> r : registros) {
			String clase = r.get("registro");
			if ("INSCRIPCION".equals(clase)) {
				if (!torneos.containsKey(requerido(r, "torneo"))) throw new PersistenciaException("Inscripción con torneo inexistente");
				Integer.parseInt(requerido(r, "id")); LocalDateTime.parse(requerido(r, "fecha"));
				validarBooleano(requerido(r, "preferencial")); Double.parseDouble(requerido(r, "tarifa"));
				validarBooleano(requerido(r, "premio")); EstadoInscripcion.valueOf(requerido(r, "estado"));
				for (String participante : separar(requerido(r, "participantes"))) validarParticipanteTorneo(participante, usuarios, equipos);
			} else if ("ENCUENTRO".equals(clase)) {
				if (!torneos.containsKey(requerido(r, "torneo"))) throw new PersistenciaException("Encuentro con torneo inexistente");
				if (r.containsKey("inscripcion1") || r.containsKey("inscripcion2")) {
					Integer.parseInt(requerido(r, "inscripcion1"));
					Integer.parseInt(requerido(r, "inscripcion2"));
				} else {
					validarParticipanteTorneo(requerido(r, "participante1"), usuarios, equipos);
					validarParticipanteTorneo(requerido(r, "participante2"), usuarios, equipos);
				}
				String primero = requerido(r, "puntaje1"), segundo = requerido(r, "puntaje2");
				if (!primero.isEmpty()) Integer.parseInt(primero); if (!segundo.isEmpty()) Integer.parseInt(segundo);
			} else if ("BONO".equals(clase)) {
				requerido(r, "id"); if (!usuarios.containsKey(requerido(r, "propietario"))) throw new PersistenciaException("Propietario de bono inexistente");
				if (Double.parseDouble(requerido(r, "valor")) <= 0) throw new PersistenciaException("Valor de bono inválido");
				LocalDateTime.parse(requerido(r, "fecha")); EstadoBono.valueOf(requerido(r, "estado"));
				String torneo = requerido(r, "torneo"); if (!torneo.isEmpty() && !torneos.containsKey(torneo)) throw new PersistenciaException("Torneo de bono inexistente");
			} else if (!"TORNEO".equals(clase)) throw new PersistenciaException("Registro de torneo desconocido");
		}
	}

	private void validarParticipanteTorneo(String participante, Map<String, String> usuarios,
			Map<String, String> equipos) {
		if (participante.startsWith("USUARIO:") && usuarios.containsKey(participante.substring(8))) return;
		if (participante.startsWith("EQUIPO:") && equipos.containsKey(participante.substring(7))) return;
		if (participante.startsWith("EXTERNO2:") && separar(participante.substring(9)).size() == 3) return;
		if (participante.startsWith("EXTERNO:") && participante.substring(8).split(";", -1).length == 3) return;
		throw new PersistenciaException("Participante de torneo inexistente");
	}

	private Map<String, String> validarUsuarios(List<Map<String, String>> registros) {
		Map<String, String> usuarios = new LinkedHashMap<String, String>();
		for (Map<String, String> r : registros) {
			String tipo = requerido(r, "tipo");
			if (!List.of("SOCIO", "ADMINISTRADOR", "ENTRENADOR", "FISIOTERAPEUTA").contains(tipo)) throw new PersistenciaException("Tipo de usuario desconocido: " + tipo);
			String email = requerido(r, "email"); requerido(r, "contrasenia");
			validarBooleano(r.getOrDefault("activo", "true"));
			if (usuarios.put(email, tipo) != null) throw new PersistenciaException("Usuario duplicado: " + email);
			if (r.containsKey("fechaNacimiento")) LocalDate.parse(r.get("fechaNacimiento"));
			if (Integer.parseInt(r.getOrDefault("suspensionesTotales", "0")) < 0 || Integer.parseInt(r.getOrDefault("suspensionRestante", "0")) < 0)
				throw new PersistenciaException("Suspensiones inválidas");
			if (Integer.parseInt(r.getOrDefault("puntosFidelidad", "0")) < 0)
				throw new PersistenciaException("Puntos de fidelidad inválidos");
		}
		return usuarios;
	}

	private Map<String, String> validarDeportivo(List<Map<String, String>> registros,
			Map<String, String> usuarios, Map<String, String> deportes) {
		Map<String, String> equipos = new LinkedHashMap<String, String>();
		for (Map<String, String> r : registros) if ("DEPORTE".equals(r.get("registro"))) {
			String tipo = requerido(r, "tipo"), nombre = nombreDeporte(tipo);
			if (deportes.put(nombre, tipo) != null) throw new PersistenciaException("Deporte duplicado: " + nombre);
		}
		for (Map<String, String> r : registros) {
			String clase = r.get("registro");
			if ("EQUIPO".equals(clase)) {
				String nombre = requerido(r, "nombre");
				if (!deportes.containsKey(requerido(r, "deporte"))) throw new PersistenciaException("Equipo con deporte inexistente");
				Categoria.valueOf(requerido(r, "categoria")); if (Integer.parseInt(requerido(r, "cupoMaximo")) < 0) throw new PersistenciaException("Cupo inválido");
				if (Integer.parseInt(r.getOrDefault("puntosRanking", "0")) < 0) throw new PersistenciaException("Puntuación inválida");
				validarBooleano(requerido(r, "activo")); if (equipos.put(nombre, requerido(r, "deporte")) != null) throw new PersistenciaException("Equipo duplicado");
			} else if ("INSCRIPCION_INDIVIDUAL".equals(clase)) {
				if (!"SOCIO".equals(usuarios.get(requerido(r, "socio"))) || !deportes.containsKey(requerido(r, "deporte"))) throw new PersistenciaException("Inscripción individual inconsistente");
				double elo = Double.parseDouble(requerido(r, "elo"));
				if (!Double.isFinite(elo) || elo < 0) throw new PersistenciaException("ELO inválido");
			} else if ("INSCRIPCION_EQUIPO".equals(clase)) {
				if (!"SOCIO".equals(usuarios.get(requerido(r, "socio"))) || !equipos.containsKey(requerido(r, "equipo"))) throw new PersistenciaException("Inscripción de equipo inconsistente");
				requerido(r, "posicion"); Integer.parseInt(requerido(r, "camiseta"));
			} else if (!"DEPORTE".equals(clase)) throw new PersistenciaException("Registro deportivo desconocido");
		}
		return equipos;
	}

	private Map<String, Boolean> validarReservas(List<Map<String, String>> registros,
			Map<String, String> usuarios, Map<String, String> deportes) {
		Map<String, Boolean> instalaciones = new LinkedHashMap<String, Boolean>();
		for (Map<String, String> r : registros) {
			String clase = r.get("registro");
			if ("INSTALACION".equals(clase)) {
				String nombre = requerido(r, "nombre"), deporte = requerido(r, "deporte");
				validarBooleano(r.getOrDefault("techada", "false"));
				if (!deporte.isEmpty() && !deportes.containsKey(deporte)) throw new PersistenciaException("Instalación con deporte inexistente");
				for (String modalidad : separar(requerido(r, "modalidades"))) Modalidad.valueOf(modalidad);
				if (instalaciones.put(nombre, true) != null) throw new PersistenciaException("Instalación duplicada");
			} else if ("RESERVA".equals(clase)) {
				if (!instalaciones.containsKey(requerido(r, "instalacion"))) throw new PersistenciaException("Reserva con instalación inexistente");
				for (String email : separar(requerido(r, "participantes"))) if (!"SOCIO".equals(usuarios.get(email))) throw new PersistenciaException("Reserva con socio inexistente");
				LocalDateTime.parse(requerido(r, "fechaHora")); if (Long.parseLong(requerido(r, "duracion")) <= 0) throw new PersistenciaException("Duración inválida");
				Modalidad.valueOf(requerido(r, "modalidad"));
			} else throw new PersistenciaException("Registro de instalación desconocido");
		}
		return instalaciones;
	}

	private Map<String, Boolean> validarCompetencias(List<Map<String, String>> registros,
			Map<String, String> deportes, Map<String, String> equipos) {
		Map<String, Boolean> competencias = new LinkedHashMap<String, Boolean>();
		for (Map<String, String> r : registros) {
			String clase = r.get("registro");
			if ("COMPETENCIA".equals(clase)) {
				String nombre = requerido(r, "nombre"); requerido(r, "entidad"); Integer.parseInt(requerido(r, "anio")); requerido(r, "temporada");
				FormatoCompetencia.valueOf(requerido(r, "formato")); if (!deportes.containsKey(requerido(r, "deporte"))) throw new PersistenciaException("Competencia con deporte inexistente");
				for (String categoria : separar(requerido(r, "categorias"))) Categoria.valueOf(categoria); Integer.parseInt(requerido(r, "minimoJugadores"));
				if (competencias.put(nombre, true) != null) throw new PersistenciaException("Competencia duplicada");
			} else if ("PARTICIPACION".equals(clase)) {
				if (!competencias.containsKey(requerido(r, "competencia")) || !equipos.containsKey(requerido(r, "equipo"))) throw new PersistenciaException("Participación inconsistente");
				Integer.parseInt(requerido(r, "puntos"));
			} else throw new PersistenciaException("Registro de competencia desconocido");
		}
		return competencias;
	}

	private void validarPartidos(List<Map<String, String>> registros, Map<String, String> usuarios,
			Map<String, String> deportes, Map<String, String> equipos, Map<String, Boolean> competencias) {
		Map<String, Boolean> partidos = new LinkedHashMap<String, Boolean>();
		for (Map<String, String> r : registros) {
			String clase = r.get("registro");
			if ("PARTIDO".equals(clase)) {
				String id = requerido(r, "id"), tipo = requerido(r, "tipo");
				if (!"ENTRENADOR".equals(usuarios.get(requerido(r, "entrenador"))) || !deportes.containsKey(requerido(r, "deporte"))) throw new PersistenciaException("Partido inconsistente");
				for (String email : separar(requerido(r, "participantes"))) if (!"SOCIO".equals(usuarios.get(email))) throw new PersistenciaException("Participante inexistente");
				if ("INTERNO".equals(tipo) || "COMPETENCIA".equals(tipo)) validarEquiposPartido(r, equipos);
				if ("COMPETENCIA".equals(tipo) && !competencias.containsKey(requerido(r, "competencia"))) throw new PersistenciaException("Competencia inexistente");
				if (!List.of("INTERNO", "COMPETENCIA", "EVENTO").contains(tipo)) throw new PersistenciaException("Tipo de partido desconocido");
				if (partidos.put(id, true) != null) throw new PersistenciaException("Partido duplicado");
			} else if ("ESTADISTICA".equals(clase)) {
				if (!partidos.containsKey(requerido(r, "partido")) || !"SOCIO".equals(usuarios.get(requerido(r, "socio")))) throw new PersistenciaException("Estadística inconsistente");
				validarEstadistica(r);
			} else if ("PRACTICA".equals(clase)) {
				if (!deportes.containsKey(requerido(r, "deporte"))) throw new PersistenciaException("Práctica con deporte inexistente");
				Modalidad.valueOf(requerido(r, "modalidad")); validarBooleano(requerido(r, "finalizado"));
				for (String email : separar(requerido(r, "participantes"))) if (!usuarios.containsKey(email)) throw new PersistenciaException("Participante inexistente");
			} else throw new PersistenciaException("Registro de partido desconocido");
		}
	}

	private void validarEntrenamientos(List<Map<String, String>> registros, Map<String, String> usuarios,
			Map<String, String> deportes, Map<String, Boolean> instalaciones) {
		Map<String, List<String>> sesiones = new LinkedHashMap<String, List<String>>();
		for (Map<String, String> r : registros) {
			String clase = r.get("registro");
			if ("SESION".equals(clase)) {
				String id = requerido(r, "id");
				if (!"ENTRENADOR".equals(usuarios.get(requerido(r, "entrenador"))) || !deportes.containsKey(requerido(r, "deporte")) || !instalaciones.containsKey(requerido(r, "instalacion"))) throw new PersistenciaException("Sesión inconsistente");
				LocalDateTime.parse(requerido(r, "fechaHora")); if (Long.parseLong(requerido(r, "duracion")) <= 0) throw new PersistenciaException("Duración inválida");
				for (String email : separar(requerido(r, "convocados"))) if (!"SOCIO".equals(usuarios.get(email))) throw new PersistenciaException("Convocado inexistente");
				if (sesiones.put(id, separar(requerido(r, "convocados"))) != null) throw new PersistenciaException("Sesión duplicada");
			} else if ("REGISTRO".equals(clase)) {
				if (!sesiones.containsKey(requerido(r, "sesion")) || !"SOCIO".equals(usuarios.get(requerido(r, "socio")))) throw new PersistenciaException("Registro de entrenamiento inconsistente");
				if (!sesiones.get(r.get("sesion")).contains(r.get("socio"))) throw new PersistenciaException("Asistencia de un socio no convocado");
				EstadoAsistencia.valueOf(requerido(r, "estado")); String nota = requerido(r, "calificacion");
				if (!nota.isEmpty()) { int valor = Integer.parseInt(nota); if (valor < 1 || valor > 5) throw new PersistenciaException("Calificación inválida"); }
				requerido(r, "comentario");
			} else throw new PersistenciaException("Registro de entrenamiento desconocido");
		}
	}

	private void validarEquiposPartido(Map<String, String> r, Map<String, String> equipos) {
		if (!equipos.containsKey(requerido(r, "local")) || !equipos.containsKey(requerido(r, "visitante"))) throw new PersistenciaException("Partido con equipo inexistente");
		LocalDateTime.parse(requerido(r, "fecha"));
	}

	private void validarEstadistica(Map<String, String> r) {
		String tipo = requerido(r, "tipo"); Integer.parseInt(requerido(r, "a")); Integer.parseInt(requerido(r, "b"));
		if ("FUTBOL".equals(tipo)) { Integer.parseInt(requerido(r, "c")); validarBooleano(requerido(r, "d")); }
		else if ("TENIS".equals(tipo)) { Integer.parseInt(requerido(r, "c")); Integer.parseInt(requerido(r, "d")); }
		else if (!List.of("BALONCESTO", "VOLEIBOL", "TENIS_MESA").contains(tipo)) throw new PersistenciaException("Tipo de estadística desconocido");
	}

	private String requerido(Map<String, String> r, String campo) { return PersistenciaSoporte.requerido(r, campo); }
	private List<String> separar(String valores) { return PersistenciaSoporte.separar(valores); }
	private void validarBooleano(String valor) { if (!"true".equals(valor) && !"false".equals(valor)) throw new PersistenciaException("Booleano inválido"); }
	private String nombreDeporte(String tipo) {
		if ("Futbol".equals(tipo)) return "Fútbol"; if ("Baloncesto".equals(tipo)) return "Baloncesto";
		if ("Voleibol".equals(tipo)) return "Voleibol"; if ("Tenis".equals(tipo)) return "Tenis";
		if ("Padel".equals(tipo)) return "Pádel"; if ("TenisDeMesa".equals(tipo)) return "Tenis de mesa";
		throw new PersistenciaException("Tipo de deporte desconocido: " + tipo);
	}
}
