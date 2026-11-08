package club.deportes;

import java.util.List;

public class Baloncesto extends DeporteConjunto {

	public enum Posicion {
		BASE,
		ESCOLTA,
		ALERO,
		ALA_PIVOT,
		PIVOT
	}

	public Baloncesto() {
		super("Baloncesto", 5, List.of(Posicion.values()));
	}
}
