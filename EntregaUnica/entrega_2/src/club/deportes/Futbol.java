package club.deportes;

import java.util.List;

public class Futbol extends DeporteConjunto {

	public enum Posicion {
		PORTERO,
		DEFENSA,
		MEDIOCAMPISTA,
		DELANTERO
	}

	public Futbol() {
		super("Fútbol", 7, List.of(Posicion.values()));
	}
}

