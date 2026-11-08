package club.deportes;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

import club.Inscripciones.InscripcionAcademia;
import club.usuarios.Socio;

public abstract class DeporteIndividual extends Deporte{
	
	//atributos
	private List<Modalidad> modalidades;
	
	//ctor 

	public DeporteIndividual(String nombre, List<Modalidad> modalidades) {
		super(nombre);
		this.modalidades = modalidades;
	}

	public List<Modalidad> getModalidades() {
		return modalidades;
	}

	public static List<DeporteIndividual> getDeportesIndividualesDisponibles() {
		List<DeporteIndividual> deportesIndividuales = new ArrayList<DeporteIndividual>();
		for (Deporte deporte : Deporte.getDeportesRegistrados().values()) {
			if (deporte instanceof DeporteIndividual) {
				deportesIndividuales.add((DeporteIndividual) deporte);
			}
		}
		return deportesIndividuales;
	}

	public List<Socio> getRanking(int cantidad) {
		if (cantidad < 0) throw new IllegalArgumentException("La cantidad no es válida");
		List<InscripcionAcademia> ranking = new ArrayList<InscripcionAcademia>();
		for (InscripcionAcademia inscripcion : InscripcionAcademia.getInscripciones()) {
			if (inscripcion.getDeporte().getClass().equals(getClass())) ranking.add(inscripcion);
		}
		ranking.sort(Comparator.comparingDouble(InscripcionAcademia::getElo).reversed());
		List<Socio> socios = new ArrayList<Socio>();
		for (int i = 0; i < Math.min(cantidad, ranking.size()); i++) {
			socios.add(ranking.get(i).getSocio());
		}
		return socios;
	}
	

}
