package club.tienda;

public class UbicacionInventario {
	private String id;
	private String nombre;

	public UbicacionInventario(String id, String nombre) {
		if (id == null || id.isBlank() || nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("Los datos de la ubicación no son válidos");
		}
		this.id = id;
		this.nombre = nombre;
	}

	public String getId() { return id; }
	public String getNombre() { return nombre; }

	public void cambiarNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre no es válido");
		this.nombre = nombre;
	}
}
