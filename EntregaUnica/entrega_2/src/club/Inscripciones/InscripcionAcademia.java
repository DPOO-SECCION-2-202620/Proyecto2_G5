package club.Inscripciones;

import java.util.ArrayList;
import java.util.List;

import club.deportes.Deporte;
import club.deportes.DeporteIndividual;
import club.deportes.Nivel;
import club.usuarios.Socio;
import club.usuarios.Usuario;

public class InscripcionAcademia {

	private static final List<InscripcionAcademia> inscripciones = new ArrayList<InscripcionAcademia>();
	private Socio socio;
	private DeporteIndividual deporte;
	private Nivel nivel;
	private double elo;

	private InscripcionAcademia(Socio socio, DeporteIndividual deporte) {
		this.socio = socio;
		this.deporte = deporte;
		this.nivel = Nivel.PRINCIPIANTE;
		this.elo = 0;
	}

	public static InscripcionAcademia inscribirSocio(Usuario usuario, DeporteIndividual deporte) {
		if (!(usuario instanceof Socio)) {
			throw new IllegalStateException("El usuario no es un socio");
		}
		if (deporte == null || !Deporte.getDeportesRegistrados().containsValue(deporte)) {
			throw new IllegalStateException("El deporte individual no existe");
		}
		Socio socio = (Socio) usuario;
		for (InscripcionAcademia inscripcion : inscripciones) {
			if (inscripcion.socio == socio
					&& inscripcion.deporte.getNombre().equals(deporte.getNombre())) {
				throw new IllegalStateException("El socio ya está inscrito en esta disciplina");
			}
		}
		InscripcionAcademia inscripcion = new InscripcionAcademia(socio, deporte);
		inscripciones.add(inscripcion);
		return inscripcion;
	}

	public static List<InscripcionAcademia> getInscripciones() {
		return inscripciones;
	}

	public static boolean estaInscrito(Socio socio, DeporteIndividual deporte) {
		return buscar(socio, deporte) != null;
	}

	public static InscripcionAcademia buscar(Socio socio, DeporteIndividual deporte) {
		for (InscripcionAcademia inscripcion : inscripciones) {
			if (inscripcion.socio == socio
					&& inscripcion.deporte.getNombre().equals(deporte.getNombre())) {
				return inscripcion;
			}
		}
		return null;
	}

	public void actualizarElo(double cambio) {
		this.elo += cambio;
		if (this.elo >= 90) this.nivel = Nivel.AVANZADO;
		else if (this.elo >= 30) this.nivel = Nivel.INTERMEDIO;
		else this.nivel = Nivel.PRINCIPIANTE;
	}

	public Socio getSocio() {
		return socio;
	}

	public DeporteIndividual getDeporte() {
		return deporte;
	}

	public Nivel getNivel() {
		return nivel;
	}

	public double getElo() {
		return elo;
	}
}
