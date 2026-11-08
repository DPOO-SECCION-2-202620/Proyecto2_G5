package club.persistencia;
// Hecho por IA
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {

	private Json() { }

	static List<Map<String, String>> leerArreglo(Path archivo) throws IOException {
		String json = Files.readString(archivo, StandardCharsets.UTF_8).trim();
		if (!json.startsWith("[") || !json.endsWith("]")) throw new PersistenciaException("JSON inválido en " + archivo.getFileName());
		List<Map<String, String>> resultado = new ArrayList<Map<String, String>>();
		int i = 1;
		while (i < json.length() - 1) {
			while (i < json.length() - 1 && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) i++;
			if (i >= json.length() - 1) break;
			if (json.charAt(i) != '{') throw new PersistenciaException("JSON inválido en " + archivo.getFileName());
			int fin = buscarCierre(json, i);
			resultado.add(leerObjeto(json.substring(i + 1, fin)));
			i = fin + 1;
		}
		return resultado;
	}

	static void escribir(Path archivo, String contenido) throws IOException {
		Files.writeString(archivo, contenido + System.lineSeparator(), StandardCharsets.UTF_8);
	}

	static String arreglo(List<String> objetos) {
		return "[" + String.join(",", objetos) + "]";
	}

	static String objeto(Map<String, String> valores) {
		List<String> campos = new ArrayList<String>();
		for (Map.Entry<String, String> campo : valores.entrySet()) {
			campos.add("\"" + escapar(campo.getKey()) + "\":\"" + escapar(campo.getValue()) + "\"");
		}
		return "{" + String.join(",", campos) + "}";
	}

	private static Map<String, String> leerObjeto(String contenido) {
		Map<String, String> valores = new LinkedHashMap<String, String>();
		int i = 0;
		while (i < contenido.length()) {
			while (i < contenido.length() && (Character.isWhitespace(contenido.charAt(i)) || contenido.charAt(i) == ',')) i++;
			Cadena clave = leerCadena(contenido, i); i = clave.fin;
			while (i < contenido.length() && Character.isWhitespace(contenido.charAt(i))) i++;
			if (i >= contenido.length() || contenido.charAt(i++) != ':') throw new PersistenciaException("Objeto JSON inválido");
			while (i < contenido.length() && Character.isWhitespace(contenido.charAt(i))) i++;
			Cadena valor = leerCadena(contenido, i); i = valor.fin;
			valores.put(clave.valor, valor.valor);
		}
		return valores;
	}

	private static Cadena leerCadena(String texto, int inicio) {
		if (inicio >= texto.length() || texto.charAt(inicio) != '"') throw new PersistenciaException("Se esperaba una cadena JSON");
		StringBuilder valor = new StringBuilder(); boolean escape = false;
		for (int i = inicio + 1; i < texto.length(); i++) {
			char c = texto.charAt(i);
			if (escape) { valor.append(c == 'n' ? '\n' : c); escape = false; }
			else if (c == '\\') escape = true;
			else if (c == '"') return new Cadena(valor.toString(), i + 1);
			else valor.append(c);
		}
		throw new PersistenciaException("Cadena JSON sin cerrar");
	}

	private static int buscarCierre(String texto, int inicio) {
		boolean cadena = false, escape = false;
		for (int i = inicio + 1; i < texto.length(); i++) {
			char c = texto.charAt(i);
			if (escape) escape = false;
			else if (c == '\\' && cadena) escape = true;
			else if (c == '"') cadena = !cadena;
			else if (c == '}' && !cadena) return i;
		}
		throw new PersistenciaException("Objeto JSON sin cerrar");
	}

	private static String escapar(String valor) {
		return valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}

	private static class Cadena {
		private String valor; private int fin;
		private Cadena(String valor, int fin) { this.valor = valor; this.fin = fin; }
	}
}
