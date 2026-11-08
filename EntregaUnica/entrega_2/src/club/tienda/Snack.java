package club.tienda;

import java.util.List;

public class Snack extends ProductoCafeteria {
	
	//atributos
	private List<String> alergenos;
	
	
	//ctor
	public Snack(String nombre, String descripcion, double precio, List<String> alergenos) {
		super(nombre, descripcion, precio);
		this.alergenos = alergenos;
	}

	//metodos
	public List<String> getAlergenos() {
		return alergenos;
	}


}
