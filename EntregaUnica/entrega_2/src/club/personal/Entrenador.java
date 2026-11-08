package club.personal;

import club.deportes.Equipo;
import club.torneos.TorneoInterno;

public class Entrenador extends Empleado {

	public Entrenador(String email, String contrasenia) {
		super(email, contrasenia);
	}

	public void inscribirEquipo(TorneoInterno torneo, Equipo equipo) {
		if (torneo == null || equipo == null) throw new IllegalArgumentException("Los datos son obligatorios");
		torneo.inscribirEquipo(this, equipo);
	}
}
