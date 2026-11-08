package club.torneos;

public class PersonaExterna {
	private String identificacion;
	private String nombre;
	private String categoria;

	public PersonaExterna(String identificacion, String nombre, String categoria) {
		if (identificacion == null || identificacion.isBlank() || nombre == null || nombre.isBlank()
				|| categoria == null || categoria.isBlank()) {
			throw new IllegalArgumentException("Los datos de la persona externa no son válidos");
		}
		this.identificacion = identificacion;
		this.nombre = nombre;
		this.categoria = categoria;
	}

	public String getIdentificacion() { return identificacion; }
	public String getNombre() { return nombre; }
	public String getCategoria() { return categoria; }

	public void setCategoria(String categoria) {
		if (categoria == null || categoria.isBlank()) throw new IllegalArgumentException("La categoría no es válida");
		this.categoria = categoria;
	}

	@Override
	public boolean equals(Object objeto) {
		if (this == objeto) return true;
		if (!(objeto instanceof PersonaExterna)) return false;
		return identificacion.equals(((PersonaExterna) objeto).identificacion);
	}

	@Override
	public int hashCode() {
		return identificacion.hashCode();
	}
}
