package club.partidos;

import club.usuarios.Socio;

public class EstadisticaVoleibol extends EstadisticaJugador {
	private int aces;
	private int bloqueos;

	public EstadisticaVoleibol(Socio socio, int aces, int bloqueos) {
		super(socio);
		validarNoNegativo(aces);
		validarNoNegativo(bloqueos);
		this.aces = aces;
		this.bloqueos = bloqueos;
	}

	public int getAces() { 
		return aces; 
	}
	
	public int getBloqueos() { 
		return bloqueos; 
	}
}
