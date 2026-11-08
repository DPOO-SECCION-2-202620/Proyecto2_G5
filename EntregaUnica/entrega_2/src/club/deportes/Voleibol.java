package club.deportes;

import java.util.List;

public class Voleibol extends DeporteConjunto {

	public enum Posicion {
		COLOCADOR,
		OPUESTO,
		CENTRAL,
		RECEPTOR_ATACANTE,
		LIBERO
	}

	public Voleibol() {
		super("Voleibol", 6, List.of(Posicion.values()));
	}
}

