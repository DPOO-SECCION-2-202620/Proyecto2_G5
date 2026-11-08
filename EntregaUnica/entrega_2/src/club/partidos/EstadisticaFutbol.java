package club.partidos;

import club.usuarios.Socio;

public class EstadisticaFutbol extends EstadisticaJugador {
	private int goles;
	private int asistencias;
	private int amarillas;
	private boolean roja;

	public EstadisticaFutbol(Socio socio, int goles, int asistencias, int amarillas, boolean roja) {
		super(socio);
		validarNoNegativo(goles);
		validarNoNegativo(asistencias);
		validarNoNegativo(amarillas);
		this.goles = goles;
		this.asistencias = asistencias;
		this.amarillas = amarillas;
		this.roja = roja;
	}

	public int getGoles() { 
		return goles; 
	}
	
	public int getAsistencias() { 
		return asistencias; 
	}
	
	public int getAmarillas() { 
		return amarillas; 
	}
	
	public boolean isRoja() { 
		return roja; 
	}
}
