package club.partidos;

import club.usuarios.Socio;

public class EstadisticaBaloncesto extends EstadisticaJugador {
	private int puntos;
	private int rebotes;

	public EstadisticaBaloncesto(Socio socio, int puntos, int rebotes) {
		super(socio);
		validarNoNegativo(puntos);
		validarNoNegativo(rebotes);
		this.puntos = puntos;
		this.rebotes = rebotes;
	}

	public int getPuntos() { 
		return puntos; 
		}
	public int getRebotes() { 
		return rebotes; 
		}
}
