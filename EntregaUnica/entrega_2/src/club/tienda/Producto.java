package club.tienda;

public abstract class Producto {
	
	//atributos
	protected String nombre;
	protected String descripcion; 
	protected  double precio;
	
	//ctor
	public Producto(String nombre, String descripcion, double precio) {
		super();
		if (nombre == null || nombre.isBlank() || descripcion == null
				|| !Double.isFinite(precio) || precio < 0) {
			throw new IllegalArgumentException("Los datos del producto no son válidos");
		}
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.precio = precio;
	}
	
	//metodos

	public double getPrecio() {
		return precio;
	}

	public void actualizarPrecio(double precio) {
		if (!Double.isFinite(precio) || precio < 0) throw new IllegalArgumentException("El precio no es válido");
		this.precio = precio;
	}

	public String getNombre() {
		return nombre;
	}

	public String getDescripcion() {
		return descripcion;
	} 
	

	
	

	
	

}
