package club.tienda;

public class Articulo extends Producto{
	
	//atributos
	private String categoria;
	
	//ctor
	public Articulo(String nombre, String descripcion, double precio, String categoria) {
		super(nombre, descripcion, precio);
		this.categoria = categoria;
	}
	
	//metodos
	public String getCategoria() {
		return categoria;
	}
	

	
	
	

}
