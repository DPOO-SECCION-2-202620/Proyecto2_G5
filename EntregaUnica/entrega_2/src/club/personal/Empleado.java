package club.personal;

import club.usuarios.Usuario;
import club.tienda.SaldoFidelidad;

public abstract class Empleado extends Usuario {
	private SaldoFidelidad saldoFidelidad = new SaldoFidelidad();

	public Empleado(String email, String contrasenia) {
		super(email, contrasenia);
	}

	public SaldoFidelidad getSaldoFidelidad() {
		return saldoFidelidad;
	}

}
