package club.tienda;

public class Bebida extends ProductoCafeteria {
	
	//atributos
	private boolean caliente;
	
	//CTOR
	public Bebida(String nombre, String descripcion, double precio, boolean caliente) {
		super(nombre, descripcion, precio);
		this.caliente = caliente;
	}


	//metodos
	public boolean esCaliente() {
		return caliente;
	}
	

}
