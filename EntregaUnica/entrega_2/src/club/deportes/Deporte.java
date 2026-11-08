package club.deportes;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Deporte {
	//atributos
	private static final Map<String, Deporte> deportesRegistrados = new LinkedHashMap<String, Deporte>();
	private String nombre; 
	
	//ctor
	public Deporte (String nombre) {
		this.nombre = nombre;
		deportesRegistrados.put(nombre, this);
	}
	
	//metodos
	public String getNombre() {
		return nombre;
	}

	public static Map<String, Deporte> getDeportesRegistrados() {
		return deportesRegistrados;
	}

}
