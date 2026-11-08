package club.persistencia;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.personal.Entrenador;
import club.personal.Fisioterapeuta;
import club.usuarios.Administrador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;
import club.usuarios.Usuario;

final class PersistenciaUsuariosJson {

	static final String ARCHIVO = "usuarios.json";

	void cargar(List<Map<String, String>> registros) {
		for (Map<String, String> registro : registros) {
			String tipo = PersistenciaSoporte.requerido(registro, "tipo");
			String email = PersistenciaSoporte.requerido(registro, "email");
			String contrasenia = PersistenciaSoporte.requerido(registro, "contrasenia");
			Usuario usuario;
			if ("SOCIO".equals(tipo)) usuario = new Socio(email, contrasenia);
			else if ("ADMINISTRADOR".equals(tipo)) usuario = new Administrador(email, contrasenia);
			else if ("ENTRENADOR".equals(tipo)) usuario = new Entrenador(email, contrasenia);
			else if ("FISIOTERAPEUTA".equals(tipo)) usuario = new Fisioterapeuta(email, contrasenia);
			else throw new PersistenciaException("Tipo de usuario desconocido: " + tipo);
			usuario.setActivo(Boolean.parseBoolean(registro.getOrDefault("activo", "true")));
			if (usuario instanceof Socio && registro.containsKey("fechaNacimiento")) {
				((Socio) usuario).setFechaNacimiento(LocalDate.parse(registro.get("fechaNacimiento")));
			}
			if (usuario instanceof Socio) ((Socio) usuario).restaurarSuspensiones(
					Integer.parseInt(registro.getOrDefault("suspensionesTotales", "0")),
					Integer.parseInt(registro.getOrDefault("suspensionRestante", "0")));
			if (usuario instanceof Socio) ((Socio) usuario).getSaldoFidelidad().agregarPuntos(
					Integer.parseInt(registro.getOrDefault("puntosFidelidad", "0")));
		}
	}

	String serializar() {
		List<String> objetos = new ArrayList<String>();
		for (Usuario usuario : RepositorioUsuarios.getRepositorioUsuarios().values()) {
			String tipo = usuario instanceof Socio ? "SOCIO"
					: usuario instanceof Administrador ? "ADMINISTRADOR"
					: usuario instanceof Fisioterapeuta ? "FISIOTERAPEUTA" : "ENTRENADOR";
			Map<String, String> valores = new LinkedHashMap<String, String>();
			valores.put("tipo", tipo); valores.put("email", usuario.getEmail());
			valores.put("contrasenia", usuario.getContrasenia());
			valores.put("activo", String.valueOf(usuario.isActivo()));
			if (usuario instanceof Socio && ((Socio) usuario).getFechaNacimiento() != null)
				valores.put("fechaNacimiento", ((Socio) usuario).getFechaNacimiento().toString());
			if (usuario instanceof Socio) {
				valores.put("suspensionesTotales", String.valueOf(((Socio) usuario).getSuspensionesTotales()));
				valores.put("suspensionRestante", String.valueOf(((Socio) usuario).getSuspensionRestante()));
				valores.put("puntosFidelidad", String.valueOf(((Socio) usuario).getSaldoFidelidad().getPuntos()));
			}
			objetos.add(Json.objeto(valores));
		}
		return Json.arreglo(objetos);
	}
}
