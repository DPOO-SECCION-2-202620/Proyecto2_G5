package club.partidos;

import club.usuarios.Socio;

public class EstadisticaTenis extends EstadisticaJugador {
	private int setsGanados;
	private int gamesGanados;
	private int aces;
	private int erroresNoForzados;

	public EstadisticaTenis(Socio socio, int setsGanados, int gamesGanados,
			int aces, int erroresNoForzados) {
		super(socio);
		validarNoNegativo(setsGanados);
		validarNoNegativo(gamesGanados);
		validarNoNegativo(aces);
		validarNoNegativo(erroresNoForzados);
		this.setsGanados = setsGanados;
		this.gamesGanados = gamesGanados;
		this.aces = aces;
		this.erroresNoForzados = erroresNoForzados;
	}

	public int getSetsGanados() { 
		return setsGanados; 
	}
	
	public int getGamesGanados() { 
		return gamesGanados; 
	}
	
	public int getAces() { 
		return aces; 
	}

	public int getErroresNoForzados() { 
		return erroresNoForzados; 
	}
}
