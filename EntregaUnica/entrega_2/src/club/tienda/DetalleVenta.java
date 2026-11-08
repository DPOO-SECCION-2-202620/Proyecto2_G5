package club.tienda;

public class DetalleVenta {
	//atributos
	private int cantidad;
	private double precioUnitario;
	
	private Producto producto;

	//ctor
	public DetalleVenta(int cantidad,  Producto producto) {
		super();
		if (cantidad <= 0 || producto == null) {
			throw new IllegalArgumentException("El detalle de venta no es válido");
		}
		this.cantidad = cantidad;
		this.precioUnitario = producto.getPrecio();
		this.producto = producto;
	}
	
	//metodos
	public double getSubtotal() {
		return cantidad * precioUnitario;
	}

	public int getCantidad() {
		return cantidad;
	}

	public double getPrecioUnitario() {
		return precioUnitario;
	}

	public Producto getProducto() {
		return producto;
	}
	
	

	
	

}
