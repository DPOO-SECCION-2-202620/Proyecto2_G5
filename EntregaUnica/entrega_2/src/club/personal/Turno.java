package club.personal;

import java.time.LocalDateTime;

public class Turno {
	private Empleado empleado;
	private LocalDateTime inicio;
	private LocalDateTime fin;

	public Turno(Empleado empleado, LocalDateTime inicio, LocalDateTime fin) {
		if (empleado == null || inicio == null || fin == null || !fin.isAfter(inicio)) {
			throw new IllegalArgumentException("Los datos del turno no son válidos");
		}
		this.empleado = empleado;
		this.inicio = inicio;
		this.fin = fin;
	}

	public Empleado getEmpleado() { return empleado; }
	public LocalDateTime getInicio() { return inicio; }
	public LocalDateTime getFin() { return fin; }

	public boolean cubre(LocalDateTime momento) {
		return !momento.isBefore(inicio) && momento.isBefore(fin);
	}
}
