package club.deportes;

import java.util.List;

public class Tenis extends DeporteIndividual {

	public Tenis() {
		super("Tenis", List.of(Modalidad.SENCILLOS, Modalidad.DOBLES));
	}
}

