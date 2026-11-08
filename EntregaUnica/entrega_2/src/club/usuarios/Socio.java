package club.usuarios;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import club.Inscripciones.InscripcionEquipo;
import club.tienda.SaldoFidelidad;

public class Socio extends Usuario {

	private LocalDate fechaNacimiento;
	private List<InscripcionEquipo> inscripciones = new ArrayList<InscripcionEquipo>();
	private int suspensionesTotales;
	private int suspensionRestante;
	
	private SaldoFidelidad puntosFidelidad;
	

	public Socio(String email, String contrasenia) {
		super(email, contrasenia);
		this.puntosFidelidad = new SaldoFidelidad();
	}

	public SaldoFidelidad getSaldoFidelidad() {
		return puntosFidelidad;
	}
	
	
	public LocalDate getFechaNacimiento() {
		return this.fechaNacimiento;
	}

	public List<InscripcionEquipo> getInscripciones() {
		return inscripciones;
	}

	public void anadirInscripcion(InscripcionEquipo inscripcion) {
		inscripciones.add(inscripcion);
	}

	public void registrarSuspension(int partidos) {
		if (partidos <= 0) {
			throw new IllegalArgumentException("La suspensión debe ser positiva");
		}
		this.suspensionesTotales += 1;
		this.suspensionRestante += partidos;
	}

	public void cumplirPartidoSuspension() {
		if (suspensionRestante > 0) suspensionRestante -= 1;
	}

	public boolean estaHabilitado() { return suspensionRestante == 0; }
	public int getSuspensionesTotales() { return suspensionesTotales; }
	public int getSuspensionRestante() { return suspensionRestante; }

	public void restaurarSuspensiones(int suspensionesTotales, int suspensionRestante) {
		if (suspensionesTotales < 0 || suspensionRestante < 0) {
			throw new IllegalArgumentException("Las suspensiones no son válidas");
		}
		this.suspensionesTotales = suspensionesTotales;
		this.suspensionRestante = suspensionRestante;
	}

	public void setFechaNacimiento(LocalDate fechaNacimiento) {
		if (this.fechaNacimiento != null) {
			throw new IllegalStateException("Para cambiar su fecha de nacimiento hable con un administrador");
		}
		validarFechaNacimiento(fechaNacimiento);
		this.fechaNacimiento = fechaNacimiento;
	}

	void editarFechaNacimiento(LocalDate fechaNacimiento) {
		validarFechaNacimiento(fechaNacimiento);
		this.fechaNacimiento = fechaNacimiento;
	}

	private void validarFechaNacimiento(LocalDate fechaNacimiento) {
		if (fechaNacimiento == null || fechaNacimiento.isBefore(LocalDate.of(1900, 1, 1))
				|| fechaNacimiento.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("La fecha de nacimiento no es válida");
		}
	}


}
