package club.torneos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import club.deportes.Deporte;
import club.deportes.DeporteConjunto;
import club.deportes.DeporteIndividual;
import club.deportes.Equipo;
import club.Inscripciones.InscripcionAcademia;
import club.personal.Empleado;
import club.personal.Entrenador;
import club.usuarios.Administrador;
import club.usuarios.Socio;

public class TorneoInterno extends Torneo {
	private List<EncuentroTorneo> encuentros = new ArrayList<EncuentroTorneo>();
	private Map<UnidadParticipacion, Integer> posiciones = new LinkedHashMap<UnidadParticipacion, Integer>();

	public TorneoInterno(Administrador administrador, Deporte deporte, String categoria,
			ModalidadTorneo modalidad, int cupos, TipoTorneo tipo, double tarifa,
			Formato formato, LocalDateTime fechaInicio) {
		super(administrador, deporte, categoria, modalidad, cupos, tipo, tarifa, formato, fechaInicio);
		registrarEnCatalogo(this);
	}

	public void inscribir(Object participante) {
		validarModalidadIndividual();
		List<Object> participantes = List.of(participante);
		boolean preferencial = puedeUsarCupoPreferencial(participantes);
		validarParticipantes(participantes, preferencial);
		registrar(participantes, preferencial);
	}

	public void inscribirPareja(Object primero, Object segundo) {
		if (getModalidad() != ModalidadTorneo.DOBLES) throw new IllegalStateException("El torneo no es de dobles");
		List<Object> participantes = List.of(primero, segundo);
		boolean preferencial = puedeUsarCupoPreferencial(participantes);
		validarParticipantes(participantes, preferencial);
		registrar(participantes, preferencial);
	}

	public void inscribirPreferencial(Object participante) {
		validarModalidadIndividual();
		List<Object> participantes = List.of(participante);
		if (!puedeUsarCupoPreferencial(participantes)) {
			throw new IllegalArgumentException("El participante no es preferencial");
		}
		validarParticipantes(participantes, true);
		registrar(participantes, true);
	}

	public void inscribirExterno(Administrador administrador, PersonaExterna participante) {
		if (getAdministrador() != administrador) {
			throw new IllegalStateException("Solo el administrador del torneo puede inscribir externos");
		}
		if (participante == null || !getCategoria().equalsIgnoreCase(participante.getCategoria())) {
			throw new IllegalArgumentException("El participante no pertenece a la categoría");
		}
		validarParticipantes(List.of(participante), false);
		registrar(List.of(participante), false);
	}

	private void validarParticipantes(List<Object> participantes, boolean preferencial) {
		validarAbierto();
		for (Object participante : participantes) {
			if (participante == null) throw new IllegalArgumentException("El participante es obligatorio");
			if (estaInscrito(participante)) throw new IllegalStateException("El participante ya está inscrito");
			if (participante instanceof Empleado && getAdministrador().estaDeTurno(
					(Empleado) participante, getFechaInicio())) {
				throw new IllegalStateException("El empleado está de turno");
			}
			if (participante instanceof Socio && getDeporte() instanceof DeporteIndividual) {
				InscripcionAcademia inscripcion = InscripcionAcademia.buscar(
						(Socio) participante, (DeporteIndividual) getDeporte());
				if (inscripcion == null || !inscripcion.getNivel().name().equalsIgnoreCase(getCategoria())) {
					throw new IllegalArgumentException("El participante no pertenece al nivel del torneo");
				}
			}
		}
		if (getCuposDisponibles() <= 0) throw new IllegalStateException("El torneo no tiene cupos");
		if (preferencial) {
			if (getCuposPreferenciales() <= 0) throw new IllegalStateException("No hay cupos preferenciales");
		} else if (getCuposDisponibles() <= getCuposPreferenciales()) {
			throw new IllegalStateException("Los cupos restantes son preferenciales");
		}
	}

	private void validarModalidadIndividual() {
		if (getModalidad() != ModalidadTorneo.INDIVIDUAL) {
			throw new IllegalStateException("El torneo no es individual");
		}
	}

	private boolean puedeUsarCupoPreferencial(List<Object> participantes) {
		if (getCuposPreferenciales() <= 0) return false;
		for (Object participante : participantes) if (!esPreferencial(participante)) return false;
		return true;
	}

	private boolean esPreferencial(Object participante) {
		if (getDeporte() instanceof DeporteIndividual && participante instanceof Socio) {
			return ((DeporteIndividual) getDeporte()).getRanking(getCuposPreferenciales()).contains(participante);
		}
		if (getDeporte() instanceof DeporteConjunto && participante instanceof Equipo) {
			return ((DeporteConjunto) getDeporte()).getRanking(getCuposPreferenciales()).contains(participante);
		}
		return false;
	}

	private void registrar(List<Object> participantes, boolean preferencial) {
		double tarifa = getTarifa();
		boolean premio = true;
		for (Object participante : participantes) if (participante instanceof Empleado) {
			tarifa = 0; premio = false;
		}
		UnidadParticipacion unidad = new UnidadParticipacion(getModalidad(), participantes);
		inscripciones.add(new InscripcionTorneo(this, unidad, preferencial, tarifa, premio));
		posiciones.put(unidad, 0);
	}

	public void inscribirEquipo(Entrenador entrenador, Equipo equipo) {
		if (entrenador == null || !entrenador.isActivo()) {
			throw new IllegalStateException("Se requiere un entrenador autorizado");
		}
		if (getModalidad() != ModalidadTorneo.EQUIPOS || !(getDeporte() instanceof DeporteConjunto)) {
			throw new IllegalStateException("El torneo no es de equipos");
		}
		if (equipo == null || !equipo.getDeporte().getClass().equals(getDeporte().getClass())) {
			throw new IllegalArgumentException("El equipo pertenece a otro deporte");
		}
		if (!getCategoria().equalsIgnoreCase(equipo.getCategoria().name())) {
			throw new IllegalArgumentException("El equipo pertenece a otra categoría");
		}
		if (equipo.getNomina().size() < ((DeporteConjunto) getDeporte()).getMinimoJugadores()) {
			throw new IllegalStateException("El equipo no tiene la nómina completa");
		}
		List<Object> participantes = List.of(equipo);
		boolean preferencial = puedeUsarCupoPreferencial(participantes);
		validarParticipantes(participantes, preferencial);
		registrar(participantes, preferencial);
	}

	public void retirar(Object participante) {
		validarAbierto();
		InscripcionTorneo inscripcion = getInscripcion(participante);
		if (inscripcion == null) throw new IllegalStateException("El participante no está inscrito");
		inscripcion.cancelar();
	}

	public boolean estaInscrito(Object participante) {
		return getInscripcion(participante) != null;
	}

	public InscripcionTorneo getInscripcion(Object participante) {
		for (InscripcionTorneo inscripcion : inscripciones) {
			if (inscripcion.isActiva() && inscripcion.getUnidadParticipacion().contiene(participante)) return inscripcion;
		}
		return null;
	}

	public List<InscripcionTorneo> getInscripciones() {
		return new ArrayList<InscripcionTorneo>(inscripciones);
	}

	public List<Object> getRankingPreferencial() {
		List<Object> ranking = new ArrayList<Object>();
		if (getDeporte() instanceof DeporteIndividual) {
			ranking.addAll(((DeporteIndividual) getDeporte()).getRanking(getCuposPreferenciales()));
		} else if (getDeporte() instanceof DeporteConjunto) {
			ranking.addAll(((DeporteConjunto) getDeporte()).getRanking(getCuposPreferenciales()));
		}
		return ranking;
	}

	public EncuentroTorneo crearEncuentro(Object participante1, Object participante2) {
		return new EncuentroTorneo(this, participante1, participante2);
	}

	UnidadParticipacion obtenerUnidad(Object participante) {
		InscripcionTorneo inscripcion = getInscripcion(participante);
		return inscripcion == null ? null : inscripcion.getUnidadParticipacion();
	}

	void registrarEncuentro(EncuentroTorneo encuentro) {
		if (encuentro == null || encuentro.getUnidad1() == encuentro.getUnidad2()) {
			throw new IllegalArgumentException("Los participantes no pertenecen al torneo");
		}
		encuentros.add(encuentro);
	}

	void registrarResultado(EncuentroTorneo encuentro) {
		if (!encuentros.contains(encuentro)) throw new IllegalArgumentException("El encuentro no pertenece al torneo");
		int primero = posiciones.getOrDefault(encuentro.getUnidad1(), 0);
		int segundo = posiciones.getOrDefault(encuentro.getUnidad2(), 0);
		if (encuentro.getPuntaje1().equals(encuentro.getPuntaje2())) {
			posiciones.put(encuentro.getUnidad1(), primero + 1);
			posiciones.put(encuentro.getUnidad2(), segundo + 1);
		} else if (encuentro.getPuntaje1() > encuentro.getPuntaje2()) {
			posiciones.put(encuentro.getUnidad1(), primero + 3);
		} else {
			posiciones.put(encuentro.getUnidad2(), segundo + 3);
		}
	}

	public List<EncuentroTorneo> getEncuentros() {
		return new ArrayList<EncuentroTorneo>(encuentros);
	}

	public Map<UnidadParticipacion, Integer> getPosiciones() {
		boolean hayResultados = false;
		for (EncuentroTorneo encuentro : encuentros) if (encuentro.tieneResultado()) hayResultados = true;
		return hayResultados ? new LinkedHashMap<UnidadParticipacion, Integer>(posiciones)
				: new LinkedHashMap<UnidadParticipacion, Integer>();
	}

	public HistorialTorneo getHistorial() {
		return new HistorialTorneo(this);
	}

	public static TorneoInterno restaurar(int id, Administrador administrador, Deporte deporte,
			String categoria, ModalidadTorneo modalidad, int cupos, TipoTorneo tipo,
			double tarifa, Formato formato, LocalDateTime fechaInicio, EstadoTorneo estado) {
		TorneoInterno torneo = new TorneoInterno(administrador, deporte, categoria, modalidad,
				cupos, tipo, tarifa, formato, fechaInicio);
		torneo.restaurarEstado(id, estado);
		return torneo;
	}

	public InscripcionTorneo restaurarInscripcion(int id, List<Object> participantes,
			boolean preferencial, double tarifa, boolean premio, EstadoInscripcion estado,
			LocalDateTime fecha) {
		if (id <= 0 || buscarInscripcionPorId(id) != null)
			throw new IllegalArgumentException("Identificador de inscripción inválido o duplicado");
		UnidadParticipacion unidad = new UnidadParticipacion(getModalidad(), participantes);
		InscripcionTorneo inscripcion = new InscripcionTorneo(this, unidad, preferencial, tarifa, premio);
		inscripcion.restaurarDatos(id, estado, fecha);
		inscripciones.add(inscripcion);
		posiciones.put(unidad, 0);
		return inscripcion;
	}

	public EncuentroTorneo restaurarEncuentro(Object participante1, Object participante2,
			Integer puntaje1, Integer puntaje2) {
		InscripcionTorneo inscripcion1 = buscarInscripcionHistorica(participante1);
		InscripcionTorneo inscripcion2 = buscarInscripcionHistorica(participante2);
		return restaurarEncuentro(inscripcion1, inscripcion2, puntaje1, puntaje2);
	}

	public EncuentroTorneo restaurarEncuentro(int inscripcion1, int inscripcion2,
			Integer puntaje1, Integer puntaje2) {
		return restaurarEncuentro(buscarInscripcionPorId(inscripcion1), buscarInscripcionPorId(inscripcion2),
				puntaje1, puntaje2);
	}

	private InscripcionTorneo buscarInscripcionPorId(int id) {
		for (InscripcionTorneo inscripcion : inscripciones)
			if (inscripcion.getId() == id) return inscripcion;
		return null;
	}

	private EncuentroTorneo restaurarEncuentro(InscripcionTorneo inscripcion1, InscripcionTorneo inscripcion2,
			Integer puntaje1, Integer puntaje2) {
		if (inscripcion1 == null || inscripcion2 == null) throw new IllegalArgumentException("Participante inexistente");
		if (inscripcion1 == inscripcion2) throw new IllegalArgumentException("El encuentro requiere dos inscripciones distintas");
		if ((puntaje1 == null) != (puntaje2 == null)) throw new IllegalArgumentException("Resultado incompleto");
		EncuentroTorneo encuentro = EncuentroTorneo.restaurar(this,
				inscripcion1.getUnidadParticipacion(), inscripcion2.getUnidadParticipacion());
		encuentros.add(encuentro);
		if (puntaje1 != null && puntaje2 != null) encuentro.restaurarResultado(puntaje1, puntaje2);
		return encuentro;
	}

	private InscripcionTorneo buscarInscripcionHistorica(Object participante) {
		InscripcionTorneo activa = getInscripcion(participante);
		if (activa != null) return activa;
		for (int i = inscripciones.size() - 1; i >= 0; i--)
			if (inscripciones.get(i).getUnidadParticipacion().contiene(participante)) return inscripciones.get(i);
		return null;
	}

	public List<Object> getParticipantes() {
		List<Object> participantes = new ArrayList<Object>();
		for (InscripcionTorneo inscripcion : inscripciones)
			if (inscripcion.isActiva()) participantes.addAll(inscripcion.getParticipantes());
		return participantes;
	}

}
