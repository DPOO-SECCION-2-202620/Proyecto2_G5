package club.partidos;

import club.usuarios.Socio;

public abstract class EstadisticaJugador {

	private Socio socio;
	private Partido partido;

	public EstadisticaJugador(Socio socio) {
		if (socio == null) {
			throw new IllegalArgumentException("El socio es obligatorio");
		}
		this.socio = socio;
	}

	void asociarPartido(Partido partido) {
		this.partido = partido;
	}

	public Socio getSocio() {
		return socio;
	}

	public Partido getPartido() {
		return partido;
	}

	protected void validarNoNegativo(int valor) {
		if (valor < 0) {
			throw new IllegalArgumentException("Las estadísticas no pueden ser negativas");
		}
	}
}
