package club.persistencia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.deportes.Deporte;
import club.deportes.Equipo;
import club.personal.Empleado;
import club.tienda.BonoDescuento;
import club.tienda.EstadoBono;
import club.torneos.CatalogoTorneos;
import club.torneos.EncuentroTorneo;
import club.torneos.EstadoInscripcion;
import club.torneos.EstadoTorneo;
import club.torneos.InscripcionTorneo;
import club.torneos.ModalidadTorneo;
import club.torneos.PersonaExterna;
import club.torneos.TipoTorneo;
import club.torneos.Torneo;
import club.torneos.TorneoInterno;
import club.torneos.UnidadParticipacion;
import club.usuarios.Administrador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Usuario;

final class PersistenciaTorneosJson {
	static final String ARCHIVO = "torneos.json";

	void cargar(List<Map<String, String>> registros) {
		for (Map<String, String> registro : registros) if ("TORNEO".equals(registro.get("registro"))) {
			Administrador administrador = (Administrador) RepositorioUsuarios.buscarPorEmail(
					PersistenciaSoporte.requerido(registro, "administrador"));
			Deporte deporte = Deporte.getDeportesRegistrados().get(PersistenciaSoporte.requerido(registro, "deporte"));
			String fecha = registro.getOrDefault("fechaInicio", "");
			TorneoInterno.restaurar(Integer.parseInt(PersistenciaSoporte.requerido(registro, "id")),
					administrador, deporte, PersistenciaSoporte.requerido(registro, "categoria"),
					ModalidadTorneo.valueOf(PersistenciaSoporte.requerido(registro, "modalidad")),
					Integer.parseInt(PersistenciaSoporte.requerido(registro, "cupos")),
					TipoTorneo.valueOf(PersistenciaSoporte.requerido(registro, "tipo")),
					Double.parseDouble(PersistenciaSoporte.requerido(registro, "tarifa")),
					Torneo.Formato.valueOf(PersistenciaSoporte.requerido(registro, "formato")),
					fecha.isEmpty() ? null : LocalDateTime.parse(fecha),
					EstadoTorneo.valueOf(PersistenciaSoporte.requerido(registro, "estado")));
		}
		for (Map<String, String> registro : registros) if ("INSCRIPCION".equals(registro.get("registro"))) {
			TorneoInterno torneo = CatalogoTorneos.buscar(Integer.parseInt(PersistenciaSoporte.requerido(registro, "torneo")));
			List<Object> participantes = new ArrayList<Object>();
			for (String valor : PersistenciaSoporte.separar(PersistenciaSoporte.requerido(registro, "participantes")))
				participantes.add(deserializarParticipante(valor));
			torneo.restaurarInscripcion(Integer.parseInt(PersistenciaSoporte.requerido(registro, "id")), participantes,
					Boolean.parseBoolean(PersistenciaSoporte.requerido(registro, "preferencial")),
					Double.parseDouble(PersistenciaSoporte.requerido(registro, "tarifa")),
					Boolean.parseBoolean(PersistenciaSoporte.requerido(registro, "premio")),
					EstadoInscripcion.valueOf(PersistenciaSoporte.requerido(registro, "estado")),
					LocalDateTime.parse(PersistenciaSoporte.requerido(registro, "fecha")));
		}
		for (Map<String, String> registro : registros) if ("ENCUENTRO".equals(registro.get("registro"))) {
			TorneoInterno torneo = CatalogoTorneos.buscar(Integer.parseInt(PersistenciaSoporte.requerido(registro, "torneo")));
			String primero = registro.getOrDefault("puntaje1", ""), segundo = registro.getOrDefault("puntaje2", "");
			Integer puntaje1 = primero.isEmpty() ? null : Integer.valueOf(primero);
			Integer puntaje2 = segundo.isEmpty() ? null : Integer.valueOf(segundo);
			if (registro.containsKey("inscripcion1") || registro.containsKey("inscripcion2")) {
				torneo.restaurarEncuentro(Integer.parseInt(PersistenciaSoporte.requerido(registro, "inscripcion1")),
						Integer.parseInt(PersistenciaSoporte.requerido(registro, "inscripcion2")), puntaje1, puntaje2);
			} else {
				torneo.restaurarEncuentro(deserializarParticipante(PersistenciaSoporte.requerido(registro, "participante1")),
						deserializarParticipante(PersistenciaSoporte.requerido(registro, "participante2")), puntaje1, puntaje2);
			}
		}
		for (Map<String, String> registro : registros) if ("BONO".equals(registro.get("registro"))) {
			Usuario propietario = RepositorioUsuarios.buscarPorEmail(PersistenciaSoporte.requerido(registro, "propietario"));
			String torneoId = registro.getOrDefault("torneo", "");
			BonoDescuento bono = BonoDescuento.restaurar(PersistenciaSoporte.requerido(registro, "id"),
					propietario, Double.parseDouble(PersistenciaSoporte.requerido(registro, "valor")),
					LocalDateTime.parse(PersistenciaSoporte.requerido(registro, "fecha")),
					EstadoBono.valueOf(PersistenciaSoporte.requerido(registro, "estado")),
					torneoId.isEmpty() ? null : CatalogoTorneos.buscar(Integer.parseInt(torneoId)));
			propietario.guardarBono(bono);
		}
	}

	String serializar() {
		List<String> objetos = new ArrayList<String>();
		for (TorneoInterno torneo : CatalogoTorneos.getTorneos()) {
			Map<String, String> valores = new LinkedHashMap<String, String>();
			valores.put("registro", "TORNEO"); valores.put("id", String.valueOf(torneo.getId()));
			valores.put("administrador", torneo.getAdministradorResponsable().getEmail());
			valores.put("deporte", torneo.getDeporte().getNombre()); valores.put("categoria", torneo.getCategoria());
			valores.put("modalidad", torneo.getModalidad().name()); valores.put("cupos", String.valueOf(torneo.getCupos()));
			valores.put("tipo", torneo.getTipo().name()); valores.put("tarifa", String.valueOf(torneo.getTarifa()));
			valores.put("formato", torneo.getFormato().name()); valores.put("estado", torneo.getEstado().name());
			valores.put("fechaInicio", torneo.getFechaInicio() == null ? "" : torneo.getFechaInicio().toString());
			objetos.add(Json.objeto(valores));
			for (InscripcionTorneo inscripcion : torneo.getInscripciones()) {
				Map<String, String> datos = new LinkedHashMap<String, String>();
				datos.put("registro", "INSCRIPCION"); datos.put("torneo", String.valueOf(torneo.getId()));
				datos.put("id", String.valueOf(inscripcion.getId())); datos.put("fecha", inscripcion.getFecha().toString());
				datos.put("participantes", serializarParticipantes(inscripcion.getParticipantes()));
				datos.put("preferencial", String.valueOf(inscripcion.esPreferencial()));
				datos.put("tarifa", String.valueOf(inscripcion.getTarifaPagada()));
				datos.put("premio", String.valueOf(inscripcion.puedeRecibirPremio()));
				datos.put("estado", inscripcion.getEstado().name()); objetos.add(Json.objeto(datos));
			}
			for (EncuentroTorneo encuentro : torneo.getEncuentros()) {
				Map<String, String> datos = new LinkedHashMap<String, String>();
				datos.put("registro", "ENCUENTRO"); datos.put("torneo", String.valueOf(torneo.getId()));
				datos.put("inscripcion1", idInscripcion(torneo, encuentro.getUnidad1()));
				datos.put("inscripcion2", idInscripcion(torneo, encuentro.getUnidad2()));
				datos.put("participante1", serializarParticipante(encuentro.getUnidad1().getIntegrantes().get(0)));
				datos.put("participante2", serializarParticipante(encuentro.getUnidad2().getIntegrantes().get(0)));
				datos.put("puntaje1", encuentro.tieneResultado() ? String.valueOf(encuentro.getPuntaje1()) : "");
				datos.put("puntaje2", encuentro.tieneResultado() ? String.valueOf(encuentro.getPuntaje2()) : "");
				objetos.add(Json.objeto(datos));
			}
		}
		for (Usuario usuario : RepositorioUsuarios.getRepositorioUsuarios().values()) {
			for (BonoDescuento bono : usuario.getBonos()) {
				Map<String, String> datos = new LinkedHashMap<String, String>();
				datos.put("registro", "BONO"); datos.put("id", bono.getId());
				datos.put("propietario", bono.getPropietarioId()); datos.put("valor", String.valueOf(bono.getValor()));
				datos.put("fecha", bono.getFechaObtencion().toString()); datos.put("estado", bono.getEstado().name());
				datos.put("torneo", bono.getTorneo() == null ? "" : String.valueOf(bono.getTorneo().getId()));
				objetos.add(Json.objeto(datos));
			}
		}
		return Json.arreglo(objetos);
	}

	private String serializarParticipantes(List<Object> participantes) {
		List<String> valores = new ArrayList<String>();
		for (Object participante : participantes) valores.add(serializarParticipante(participante));
		return PersistenciaSoporte.unir(valores);
	}

	private String idInscripcion(TorneoInterno torneo, UnidadParticipacion unidad) {
		for (InscripcionTorneo inscripcion : torneo.getInscripciones())
			if (inscripcion.getUnidadParticipacion() == unidad) return String.valueOf(inscripcion.getId());
		throw new PersistenciaException("Encuentro con inscripción inexistente");
	}

	private String serializarParticipante(Object participante) {
		if (participante instanceof Usuario) return "USUARIO:" + ((Usuario) participante).getEmail();
		if (participante instanceof Equipo) return "EQUIPO:" + ((Equipo) participante).getNombre();
		PersonaExterna externo = (PersonaExterna) participante;
		return "EXTERNO2:" + PersistenciaSoporte.unir(List.of(
				externo.getIdentificacion(), externo.getNombre(), externo.getCategoria()));
	}

	private Object deserializarParticipante(String valor) {
		if (valor.startsWith("USUARIO:")) return RepositorioUsuarios.buscarPorEmail(valor.substring(8));
		if (valor.startsWith("EQUIPO:")) return Equipo.getEquiposRegistrados().get(valor.substring(7));
		List<String> datos;
		if (valor.startsWith("EXTERNO2:")) datos = PersistenciaSoporte.separar(valor.substring(9));
		else if (valor.startsWith("EXTERNO:")) datos = List.of(valor.substring(8).split(";", -1));
		else throw new PersistenciaException("Tipo de participante desconocido");
		if (datos.size() != 3) throw new PersistenciaException("Datos de participante externo incompletos");
		return new PersonaExterna(datos.get(0), datos.get(1), datos.get(2));
	}
}
