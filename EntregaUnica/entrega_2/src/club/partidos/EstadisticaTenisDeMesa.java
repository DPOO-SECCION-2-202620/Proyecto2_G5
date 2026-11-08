package club.partidos;

import club.usuarios.Socio;

public class EstadisticaTenisDeMesa extends EstadisticaJugador {
	private int setsGanados;
	private int puntosTotales;

	public EstadisticaTenisDeMesa(Socio socio, int setsGanados, int puntosTotales) {
		super(socio);
		validarNoNegativo(setsGanados);
		validarNoNegativo(puntosTotales);
		this.setsGanados = setsGanados;
		this.puntosTotales = puntosTotales;
	}

	public int getSetsGanados() { 
		return setsGanados; 
	}
	
	public int getPuntosTotales() { 
		return puntosTotales; 
	}
}
