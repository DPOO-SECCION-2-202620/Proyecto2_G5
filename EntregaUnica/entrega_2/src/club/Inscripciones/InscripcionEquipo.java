package club.Inscripciones;

import java.util.List;

import club.deportes.DeporteConjunto;
import club.deportes.Equipo;
import club.usuarios.Socio;

public class InscripcionEquipo {
	
	private Enum<?> posicion;
	private int numeroCamiseta;
	private int numeroSuspenciones = 0;
	private int suspensionRestante = 0;
	private Socio socio; 
	private Equipo equipo;
	
	
	public void inscribirSocio(Socio socio, Equipo equipo, Enum<?> posicion, int numeroCamiseta) {
		validarInscripcion(socio, equipo, posicion, numeroCamiseta);
		this.socio = socio;
		this.equipo = equipo;
		this.posicion = posicion;
		this.numeroCamiseta = numeroCamiseta;
		equipo.ocuparCupoEnEquipo();
		equipo.anadirInscripcion(this);
		socio.anadirInscripcion(this);
	}

	public static InscripcionEquipo restaurar(Socio socio, Equipo equipo,
			Enum<?> posicion, int numeroCamiseta) {
		if (socio == null || equipo == null || posicion == null
				|| !((DeporteConjunto) equipo.getDeporte()).getPosiciones().contains(posicion)) {
			throw new IllegalArgumentException("Los datos de la inscripción no son válidos");
		}
		InscripcionEquipo inscripcion = new InscripcionEquipo();
		inscripcion.socio = socio;
		inscripcion.equipo = equipo;
		inscripcion.posicion = posicion;
		inscripcion.numeroCamiseta = numeroCamiseta;
		equipo.ocuparCupoEnEquipo();
		equipo.anadirInscripcion(inscripcion);
		socio.anadirInscripcion(inscripcion);
		return inscripcion;
	}
	
	private void validarInscripcion(Socio socio, Equipo equipo, Enum<?> posicion, int numeroCamiseta) {
		if (!Equipo.getEquiposDisponibles().containsValue(equipo)) {
			throw new IllegalStateException("El equipo no está disponible");
		}
		if (!equipo.isActivo()) {
			throw new IllegalStateException("El equipo está inactivo");
		}
		if (equipo.getCuposDisponibles() <= 0) {
			throw new IllegalStateException("El equipo no tiene cupos disponibles");
		}
		if (socio.getFechaNacimiento() == null
				|| !equipo.getCategoria().admiteEdad(socio.getFechaNacimiento())) {
			throw new IllegalArgumentException("El socio no cumple los requisitos de edad");
		}
		DeporteConjunto deporte = (DeporteConjunto) equipo.getDeporte();
		if (posicion == null || !deporte.getPosiciones().contains(posicion)) {
			throw new IllegalArgumentException("La posición no pertenece al deporte seleccionado");
		}
		for (InscripcionEquipo inscripcion : socio.getInscripciones()) {
			if (inscripcion.equipo == equipo) {
				throw new IllegalStateException("El socio ya pertenece a este equipo");
			}
			if (inscripcion.equipo.getDeporte().getClass().equals(equipo.getDeporte().getClass())) {
				throw new IllegalStateException("El socio ya pertenece a otro equipo del mismo deporte");
			}
		}
		for (InscripcionEquipo inscripcion : equipo.getNomina()) {
			if (inscripcion.numeroCamiseta == numeroCamiseta) {
				throw new IllegalArgumentException("El número de camiseta ya está en uso");
			}
		}
	}

	public Enum<?> getPosicion() {
		return posicion;
	}

	public int getNumeroCamiseta() {
		return numeroCamiseta;
	}

	public Socio getSocio() {
		return socio;
	}

	public Equipo getEquipo() {
		return equipo;
	}
	
	public List<Equipo> consultarEquiposDisponibles(Socio socio, DeporteConjunto deporte) {
		return Equipo.getEquiposDisponiblesPorCategoria(socio, deporte);
	}
}
