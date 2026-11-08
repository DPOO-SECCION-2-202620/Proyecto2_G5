package club.deportes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class DeporteConjunto extends Deporte{
	//atributos
	private int cantidadJugadores;
	private List<? extends Enum<?>> posiciones;
	
	//ctor
	public DeporteConjunto(String nombre, int cantidadJugadores, List<? extends Enum<?>> posiciones) {
		
		super(nombre);
		this.cantidadJugadores = cantidadJugadores;
		this.posiciones = posiciones;
	}
	
	//metodos
	public int getMinimoJugadores() {
		
		return cantidadJugadores;
	}

	public List<? extends Enum<?>> getPosiciones() {
		return posiciones;
	}

	public List<Equipo> getRanking(int cantidad) {
		if (cantidad < 0) throw new IllegalArgumentException("La cantidad no es válida");
		List<Equipo> ranking = new ArrayList<Equipo>();
		for (Equipo equipo : Equipo.getEquiposRegistrados().values()) {
			if (equipo.getDeporte().getClass().equals(getClass())) ranking.add(equipo);
		}
		ranking.sort(Comparator.comparingInt(Equipo::getPuntosRanking).reversed());
		return new ArrayList<Equipo>(ranking.subList(0, Math.min(cantidad, ranking.size())));
	}
	

}

	
