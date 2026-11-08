package club.partidos;

import java.util.ArrayList;
import java.util.List;

import club.Inscripciones.InscripcionAcademia;
import club.deportes.DeporteIndividual;
import club.deportes.Modalidad;
import club.deportes.Nivel;
import club.personal.Entrenador;
import club.usuarios.Socio;
import club.usuarios.Usuario;

public class PartidoPractica {

	private static final List<PartidoPractica> partidos = new ArrayList<PartidoPractica>();
	private DeporteIndividual deporte;
	private Modalidad modalidad;
	private List<Usuario> participantes;
	private boolean finalizado;

	public PartidoPractica(DeporteIndividual deporte, Modalidad modalidad, List<Usuario> participantes) {
		validar(deporte, modalidad, participantes);
		this.deporte = deporte;
		this.modalidad = modalidad;
		this.participantes = new ArrayList<Usuario>(participantes);
		partidos.add(this);
	}

	private PartidoPractica() { }

	public static PartidoPractica restaurar(DeporteIndividual deporte, Modalidad modalidad,
			List<Usuario> participantes, boolean finalizado) {
		if (deporte == null || modalidad == null || participantes == null) {
			throw new IllegalArgumentException("Faltan datos del partido");
		}
		PartidoPractica partido = new PartidoPractica(); partido.deporte = deporte;
		partido.modalidad = modalidad; partido.participantes = new ArrayList<Usuario>(participantes);
		partido.finalizado = finalizado; partidos.add(partido); return partido;
	}

	private void validar(DeporteIndividual deporte, Modalidad modalidad, List<Usuario> participantes) {
		if (deporte == null || modalidad == null || participantes == null) {
			throw new IllegalArgumentException("Faltan datos del partido");
		}
		int cantidad = modalidad == Modalidad.SENCILLOS ? 2 : 4;
		if (!deporte.getModalidades().contains(modalidad) || participantes.size() != cantidad) {
			throw new IllegalArgumentException("La modalidad o cantidad de participantes no es válida");
		}
		boolean hayEntrenador = false;
		for (Usuario participante : participantes) {
			if (participante instanceof Entrenador) hayEntrenador = true;
			else if (!(participante instanceof Socio)
					|| !InscripcionAcademia.estaInscrito((Socio) participante, deporte)) {
				throw new IllegalArgumentException("Un participante no está inscrito en la disciplina");
			}
		}
		if (!hayEntrenador) {
			int minimo = Integer.MAX_VALUE;
			int maximo = Integer.MIN_VALUE;
			for (Usuario participante : participantes) {
				Nivel nivel = InscripcionAcademia.buscar((Socio) participante, deporte).getNivel();
				minimo = Math.min(minimo, nivel.ordinal());
				maximo = Math.max(maximo, nivel.ordinal());
			}
			if (maximo - minimo > 1) {
				throw new IllegalArgumentException("Los niveles de los participantes no son compatibles");
			}
		}
	}

	public void finalizar(int puntosEquipoUno, int puntosEquipoDos) {
		if (finalizado) throw new IllegalStateException("El ELO de este partido ya fue actualizado");
		if (puntosEquipoUno < 0 || puntosEquipoDos < 0 || puntosEquipoUno == puntosEquipoDos) {
			throw new IllegalArgumentException("El resultado no es válido");
		}
		int integrantesEquipo = modalidad == Modalidad.SENCILLOS ? 1 : 2;
		boolean ganaEquipoUno = puntosEquipoUno > puntosEquipoDos;
		for (int i = 0; i < participantes.size(); i++) {
			Usuario participante = participantes.get(i);
			if (participante instanceof Socio) {
				boolean ganador = i < integrantesEquipo ? ganaEquipoUno : !ganaEquipoUno;
				int puntos = i < integrantesEquipo ? puntosEquipoUno : puntosEquipoDos;
				InscripcionAcademia.buscar((Socio) participante, deporte)
						.actualizarElo(puntos + (ganador ? 5 : -2));
			}
		}
		finalizado = true;
	}

	public static List<PartidoPractica> getPartidos() { 
		return partidos; 
	}
	
	public DeporteIndividual getDeporte() { 
		return deporte; 
	}
	
	public Modalidad getModalidad() { 
		return modalidad; 
	}
	
	public List<Usuario> getParticipantes() { 
		return participantes; 
	}
	
	public boolean isFinalizado() { 
		return finalizado; 
	}
}
