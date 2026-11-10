package club.persistencia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.personal.Empleado;
import club.personal.PlantillaTurnos;
import club.personal.SolicitudCambioTurno;
import club.personal.Turno;
import club.usuarios.Administrador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Usuario;

final class PersistenciaPersonalJson {

	static final String ARCHIVO = "personal.json";

	void cargar(List<Map<String, String>> registros) {
		Map<String, Turno> turnos = new LinkedHashMap<String, Turno>();
		Map<String, Administrador> propietarios = new LinkedHashMap<String, Administrador>();
		for (Map<String, String> registro : registros) {
			String tipo = PersistenciaSoporte.requerido(registro, "registro");
			if ("SOLICITUD".equals(tipo)) continue;
			if (!"TURNO".equals(tipo)) throw new PersistenciaException("Registro de personal desconocido: " + tipo);
			String id = PersistenciaSoporte.requerido(registro, "id");
			if (turnos.containsKey(id)) throw new PersistenciaException("Turno duplicado: " + id);
			Administrador administrador = administrador(registro);
			String email = PersistenciaSoporte.requerido(registro, "empleado");
			Usuario usuario = RepositorioUsuarios.buscarPorEmail(email);
			if (!(usuario instanceof Empleado)) throw new PersistenciaException("No existe el empleado " + email);
			Turno turno = new Turno((Empleado) usuario,
					LocalDateTime.parse(PersistenciaSoporte.requerido(registro, "inicio")),
					LocalDateTime.parse(PersistenciaSoporte.requerido(registro, "fin")));
			turnos.put(id, turno);
			propietarios.put(id, administrador);
			if (booleano(registro, "activo"))
				administrador.getPlantillaTurnos().agregarTurno(turno);
		}
		for (Map<String, String> registro : registros) {
			if (!"SOLICITUD".equals(registro.get("registro"))) continue;
			Administrador administrador = administrador(registro);
			String anterior = PersistenciaSoporte.requerido(registro, "turnoAnterior");
			String nuevo = PersistenciaSoporte.requerido(registro, "turnoNuevo");
			if (propietarios.get(anterior) != administrador || propietarios.get(nuevo) != administrador)
				throw new PersistenciaException("La solicitud referencia turnos de otra plantilla o inexistentes");
			administrador.getPlantillaTurnos().restaurarSolicitud(turnos.get(anterior), turnos.get(nuevo),
					booleano(registro, "procesada"), booleano(registro, "aprobada"));
		}
	}

	String serializar() {
		List<String> objetos = new ArrayList<String>();
		int siguienteId = 1;
		for (Usuario usuario : RepositorioUsuarios.getRepositorioUsuarios().values()) {
			if (!(usuario instanceof Administrador)) continue;
			PlantillaTurnos plantilla = ((Administrador) usuario).getPlantillaTurnos();
			List<Turno> activos = plantilla.getTurnos();
			List<Turno> turnos = new ArrayList<Turno>();
			for (Turno turno : activos) if (!turnos.contains(turno)) turnos.add(turno);
			for (SolicitudCambioTurno solicitud : plantilla.getSolicitudes()) {
				if (!turnos.contains(solicitud.getTurnoAnterior())) turnos.add(solicitud.getTurnoAnterior());
				if (!turnos.contains(solicitud.getTurnoNuevo())) turnos.add(solicitud.getTurnoNuevo());
			}
			Map<Turno, String> ids = new IdentityHashMap<Turno, String>();
			for (Turno turno : turnos) {
				String id = "turno-" + siguienteId++;
				ids.put(turno, id);
				Map<String, String> valores = new LinkedHashMap<String, String>();
				valores.put("registro", "TURNO");
				valores.put("id", id);
				valores.put("administrador", usuario.getEmail());
				valores.put("empleado", turno.getEmpleado().getEmail());
				valores.put("inicio", turno.getInicio().toString());
				valores.put("fin", turno.getFin().toString());
				valores.put("activo", String.valueOf(activos.contains(turno)));
				objetos.add(Json.objeto(valores));
			}
			for (SolicitudCambioTurno solicitud : plantilla.getSolicitudes()) {
				Map<String, String> valores = new LinkedHashMap<String, String>();
				valores.put("registro", "SOLICITUD");
				valores.put("administrador", usuario.getEmail());
				valores.put("turnoAnterior", ids.get(solicitud.getTurnoAnterior()));
				valores.put("turnoNuevo", ids.get(solicitud.getTurnoNuevo()));
				valores.put("procesada", String.valueOf(solicitud.isProcesada()));
				valores.put("aprobada", String.valueOf(solicitud.isAprobada()));
				objetos.add(Json.objeto(valores));
			}
		}
		return Json.arreglo(objetos);
	}

	private Administrador administrador(Map<String, String> registro) {
		String email = PersistenciaSoporte.requerido(registro, "administrador");
		Usuario usuario = RepositorioUsuarios.buscarPorEmail(email);
		if (!(usuario instanceof Administrador)) throw new PersistenciaException("No existe el administrador " + email);
		return (Administrador) usuario;
	}

	private boolean booleano(Map<String, String> registro, String campo) {
		String valor = PersistenciaSoporte.requerido(registro, campo);
		if (!"true".equals(valor) && !"false".equals(valor))
			throw new PersistenciaException("El campo " + campo + " debe ser true o false");
		return Boolean.parseBoolean(valor);
	}
}
