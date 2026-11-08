package club.deportes;

import java.util.List;

public class TenisDeMesa extends DeporteIndividual {

	public TenisDeMesa() {
		super("Tenis de mesa", List.of(Modalidad.SENCILLOS, Modalidad.DOBLES));
	}
}

