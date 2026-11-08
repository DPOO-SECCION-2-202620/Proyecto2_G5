package club.tienda;

public class SaldoFidelidad {
	
	//atributos
	private int puntos;

	public SaldoFidelidad() {
		this.puntos = 0;
	}

	//metodos
	public int getPuntos() {
		return puntos;
	}
	
	public void agregarPuntos(int cantidad) {
		if (cantidad < 0) throw new IllegalArgumentException("Los puntos no son válidos");
		this.puntos += cantidad;
	}
	
	public void redimirPuntos(int cantidad) {
		redimir(cantidad);
	}

	public void redimir(int cantidad) {
		if (cantidad <= 0) throw new IllegalArgumentException("La cantidad de puntos no es válida");
		if (cantidad > puntos) throw new IllegalStateException("Puntos de fidelidad insuficientes");
		this.puntos -= cantidad;
		
	}
	
	
	

}
