package club.tienda;

public class Inventario {
	
	//atributos
	private Articulo articulo;
	private UbicacionInventario ubicacion;
	private int cantidad;
	
	//ctor
	public Inventario(Articulo articulo, UbicacionInventario ubicacion, int cantidad) {
		super();
		if (articulo == null || ubicacion == null || cantidad < 0) {
			throw new IllegalArgumentException("Los datos del inventario no son válidos");
		}
		this.articulo = articulo;
		this.ubicacion = ubicacion;
		this.cantidad = cantidad;
	}
	
	//metodos
	public Articulo getArtiuclo() {
		return articulo;
	}

	public Articulo getArticulo() { return articulo; }

	public UbicacionInventario getUbicacion() {
		return ubicacion;
	}

	public int consultarCantidad() {
		return cantidad;
	}
	
	public void descontar(int cantidad) {
		if (cantidad <= 0 || cantidad > this.cantidad) throw new IllegalStateException("Existencias insuficientes");
		this.cantidad -= cantidad;
	}
	
	public void reabastecer(int cantidad) {
		if (cantidad <= 0) throw new IllegalArgumentException("La cantidad no es válida");
		this.cantidad += cantidad;
	}
	
	public void trasladar(Inventario destino, int cantidad) {
		if (destino == null || destino == this || destino.articulo != articulo || cantidad <= 0) {
			throw new IllegalArgumentException("Los datos del traslado no son válidos");
		}
		if (cantidad > this.cantidad) throw new IllegalStateException("Existencias insuficientes");
		this.cantidad -= cantidad;
		destino.cantidad += cantidad;
	}
	
	
	
	
	

}
