package club.persistencia;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import club.deportes.Baloncesto;
import club.deportes.Deporte;
import club.deportes.DeporteConjunto;
import club.deportes.Futbol;
import club.deportes.Padel;
import club.deportes.Tenis;
import club.deportes.TenisDeMesa;
import club.deportes.Voleibol;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;
import club.usuarios.Usuario;

final class PersistenciaSoporte {

	private PersistenciaSoporte() { }

	static String requerido(Map<String, String> registro, String campo) {
		String valor = registro.get(campo);
		if (valor == null) throw new PersistenciaException("Falta el campo " + campo);
		return valor;
	}

	static List<String> separar(String valores) {
		if (valores == null || valores.isEmpty()) return new ArrayList<String>();
		if (valores.startsWith("@2:")) {
			List<String> resultado = new ArrayList<>();
			for (String valor : valores.substring(3).split("\\|", -1)) {
				resultado.add(URLDecoder.decode(valor, StandardCharsets.UTF_8));
			}
			return resultado;
		}
		return List.of(valores.split("\\|", -1));
	}

	static String unir(List<String> valores) {
		if (valores.isEmpty()) return "";
		List<String> codificados = new ArrayList<>();
		for (String valor : valores) codificados.add(URLEncoder.encode(valor, StandardCharsets.UTF_8));
		return "@2:" + String.join("|", codificados);
	}

	static String unirEnums(List<? extends Enum<?>> valores) {
		List<String> nombres = new ArrayList<String>();
		for (Enum<?> valor : valores) nombres.add(valor.name());
		return unir(nombres);
	}

	static String unirSocios(List<Socio> socios) {
		List<String> emails = new ArrayList<String>();
		for (Socio socio : socios) emails.add(socio.getEmail());
		return unir(emails);
	}

	static Socio socio(String email) {
		Usuario usuario = RepositorioUsuarios.buscarPorEmail(email);
		if (!(usuario instanceof Socio)) throw new PersistenciaException("No existe el socio " + email);
		return (Socio) usuario;
	}

	static Enum<?> posicion(DeporteConjunto deporte, String nombre) {
		for (Enum<?> posicion : deporte.getPosiciones()) if (posicion.name().equals(nombre)) return posicion;
		throw new PersistenciaException("Posición inexistente: " + nombre);
	}

	static Deporte crearDeporte(String tipo) {
		if ("Futbol".equals(tipo)) return new Futbol();
		if ("Baloncesto".equals(tipo)) return new Baloncesto();
		if ("Voleibol".equals(tipo)) return new Voleibol();
		if ("Tenis".equals(tipo)) return new Tenis();
		if ("Padel".equals(tipo)) return new Padel();
		if ("TenisDeMesa".equals(tipo)) return new TenisDeMesa();
		throw new PersistenciaException("Tipo de deporte desconocido: " + tipo);
	}
}
