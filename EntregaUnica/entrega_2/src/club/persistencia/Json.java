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
		int i = saltarEspacios(json, 1);
		while (i < json.length() - 1) {
			if (json.charAt(i) != '{') throw new PersistenciaException("JSON inválido en " + archivo.getFileName());
			int fin = buscarCierre(json, i);
			resultado.add(leerObjeto(json.substring(i + 1, fin)));
			i = saltarEspacios(json, fin + 1);
			if (i == json.length() - 1) break;
			if (i >= json.length() - 1 || json.charAt(i) != ',')
				throw new PersistenciaException("JSON inválido en " + archivo.getFileName());
			i = saltarEspacios(json, i + 1);
			if (i >= json.length() - 1) throw new PersistenciaException("JSON inválido en " + archivo.getFileName());
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
		int i = saltarEspacios(contenido, 0);
		while (i < contenido.length()) {
			Cadena clave = leerCadena(contenido, i);
			i = saltarEspacios(contenido, clave.fin);
			if (i >= contenido.length() || contenido.charAt(i++) != ':') throw new PersistenciaException("Objeto JSON inválido");
			i = saltarEspacios(contenido, i);
			Cadena valor = leerCadena(contenido, i);
			if (valores.containsKey(clave.valor)) throw new PersistenciaException("Campo JSON duplicado: " + clave.valor);
			valores.put(clave.valor, valor.valor);
			i = saltarEspacios(contenido, valor.fin);
			if (i == contenido.length()) break;
			if (contenido.charAt(i) != ',') throw new PersistenciaException("Objeto JSON inválido");
			i = saltarEspacios(contenido, i + 1);
			if (i == contenido.length()) throw new PersistenciaException("Objeto JSON inválido");
		}
		return valores;
	}

	private static Cadena leerCadena(String texto, int inicio) {
		if (inicio >= texto.length() || texto.charAt(inicio) != '"') throw new PersistenciaException("Se esperaba una cadena JSON");
		StringBuilder valor = new StringBuilder();
		for (int i = inicio + 1; i < texto.length(); i++) {
			char c = texto.charAt(i);
			if (c == '"') return new Cadena(valor.toString(), i + 1);
			if (c < 0x20) throw new PersistenciaException("Carácter de control sin escapar en cadena JSON");
			if (c != '\\') {
				valor.append(c);
				continue;
			}
			if (++i >= texto.length()) throw new PersistenciaException("Escape JSON incompleto");
			char escape = texto.charAt(i);
			switch (escape) {
			case '"': case '\\': case '/': valor.append(escape); break;
			case 'n': valor.append('\n'); break;
			case 'r': valor.append('\r'); break;
			case 't': valor.append('\t'); break;
			case 'b': valor.append('\b'); break;
			case 'f': valor.append('\f'); break;
			case 'u':
				if (i + 4 >= texto.length()) throw new PersistenciaException("Escape Unicode incompleto");
				int codigo = 0;
				for (int j = 1; j <= 4; j++) {
					int digito = Character.digit(texto.charAt(i + j), 16);
					if (digito < 0) throw new PersistenciaException("Escape Unicode inválido");
					codigo = codigo * 16 + digito;
				}
				valor.append((char) codigo);
				i += 4;
				break;
			default: throw new PersistenciaException("Escape JSON inválido: " + escape);
			}
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
		StringBuilder resultado = new StringBuilder();
		for (int i = 0; i < valor.length(); i++) {
			char c = valor.charAt(i);
			switch (c) {
			case '\\': resultado.append("\\\\"); break;
			case '"': resultado.append("\\\""); break;
			case '\n': resultado.append("\\n"); break;
			case '\r': resultado.append("\\r"); break;
			case '\t': resultado.append("\\t"); break;
			case '\b': resultado.append("\\b"); break;
			case '\f': resultado.append("\\f"); break;
			default:
				if (c < 0x20) resultado.append(String.format("\\u%04x", (int) c));
				else resultado.append(c);
			}
		}
		return resultado.toString();
	}

	private static int saltarEspacios(String texto, int inicio) {
		int i = inicio;
		while (i < texto.length() && " \t\r\n".indexOf(texto.charAt(i)) >= 0) i++;
		return i;
	}

	private static class Cadena {
		private String valor; private int fin;
		private Cadena(String valor, int fin) { this.valor = valor; this.fin = fin; }
	}
}
