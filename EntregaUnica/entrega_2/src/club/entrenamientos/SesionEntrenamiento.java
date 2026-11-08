package club.entrenamientos;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import club.Inscripciones.InscripcionAcademia;
import club.deportes.Deporte;
import club.deportes.DeporteConjunto;
import club.deportes.DeporteIndividual;
import club.deportes.Equipo;
import club.instalaciones.Instalacion;
import club.personal.Entrenador;
import club.usuarios.Administrador;
import club.usuarios.Socio;
import club.usuarios.Usuario;

public class SesionEntrenamiento {

	private static final List<SesionEntrenamiento> sesiones = new ArrayList<SesionEntrenamiento>();
	private Entrenador entrenador;
	private Deporte deporte;
	private Instalacion instalacion;
	private LocalDateTime fechaHora;
	private Duration duracion;
	private List<Socio> convocados = new ArrayList<Socio>();
	private Map<Socio, RegistroEntrenamiento> registros = new HashMap<Socio, RegistroEntrenamiento>();

	public SesionEntrenamiento(Entrenador entrenador, Deporte deporte, Instalacion instalacion,
			LocalDateTime fechaHora, Duration duracion) {
		validarDatos(entrenador, deporte, instalacion, fechaHora, duracion);
		this.entrenador = entrenador;
		this.deporte = deporte;
		this.instalacion = instalacion;
		this.fechaHora = fechaHora;
		this.duracion = duracion;
		instalacion.registrarSesion(this);
		sesiones.add(this);
	}

	public static SesionEntrenamiento restaurar(Entrenador entrenador, Deporte deporte,
			Instalacion instalacion, LocalDateTime fechaHora, Duration duracion,
			List<Socio> convocados) {
		if (entrenador == null || deporte == null || instalacion == null
				|| fechaHora == null || duracion == null || convocados == null) {
			throw new IllegalArgumentException("Los datos de la sesión no son válidos");
		}
		SesionEntrenamiento sesion = new SesionEntrenamiento();
		sesion.entrenador = entrenador; sesion.deporte = deporte; sesion.instalacion = instalacion;
		sesion.fechaHora = fechaHora; sesion.duracion = duracion;
		sesion.convocados.addAll(convocados);
		instalacion.registrarSesion(sesion); sesiones.add(sesion);
		return sesion;
	}

	public void restaurarRegistro(Socio socio, EstadoAsistencia estado,
			Integer calificacion, String comentario) {
		if (!convocados.contains(socio)) throw new IllegalArgumentException("El socio no está convocado");
		registros.put(socio, new RegistroEntrenamiento(this, socio, estado, calificacion, comentario));
	}

	private SesionEntrenamiento() { }

	private void validarDatos(Entrenador entrenador, Deporte deporte, Instalacion instalacion,
			LocalDateTime fechaHora, Duration duracion) {
		if (entrenador == null || deporte == null || instalacion == null
				|| fechaHora == null || duracion == null) {
			throw new IllegalArgumentException("Falta información para programar la sesión");
		}
		if (!fechaHora.isAfter(LocalDateTime.now()) || duracion.isZero() || duracion.isNegative()) {
			throw new IllegalArgumentException("La fecha, hora o duración no es válida");
		}
		if (!Instalacion.getInstalaciones().containsValue(instalacion)) {
			throw new IllegalStateException("La instalación no existe");
		}
		if (!instalacion.estaDisponible(fechaHora, fechaHora.plus(duracion))) {
			throw new IllegalStateException("La instalación no está disponible");
		}
	}

	public static List<SesionEntrenamiento> getSesiones() {
		return sesiones;
	}

	public Entrenador getEntrenador() {
		return entrenador;
	}

	public Deporte getDeporte() {
		return deporte;
	}

	public Instalacion getInstalacion() {
		return instalacion;
	}

	public LocalDateTime getFechaHora() {
		return fechaHora;
	}

	public Duration getDuracion() {
		return duracion;
	}

	public List<Socio> getConvocados() {
		return convocados;
	}

	public void convocarEquipo(Usuario usuario, Equipo equipo) {
		validarPermiso(usuario);
		validarSesionRegistrada();
		if (!(deporte instanceof DeporteConjunto)
				|| !equipo.getDeporte().getClass().equals(deporte.getClass())) {
			throw new IllegalArgumentException("El equipo pertenece a un deporte diferente");
		}
		List<Socio> sociosEquipo = new ArrayList<Socio>();
		equipo.getNomina().forEach(inscripcion -> sociosEquipo.add(inscripcion.getSocio()));
		validarSinDuplicados(sociosEquipo);
		convocados.addAll(sociosEquipo);
	}

	public void convocarSocios(Usuario usuario, List<Socio> socios) {
		validarPermiso(usuario);
		validarSesionRegistrada();
		if (!(deporte instanceof DeporteIndividual)) {
			throw new IllegalArgumentException("La sesión no corresponde a un deporte individual");
		}
		for (Socio socio : socios) {
			if (!InscripcionAcademia.estaInscrito(socio, (DeporteIndividual) deporte)) {
				throw new IllegalArgumentException("El socio no está inscrito en el deporte");
			}
		}
		validarSinDuplicados(socios);
		convocados.addAll(socios);
	}

	public void eliminarConvocado(Usuario usuario, Socio socio) {
		validarPermiso(usuario);
		validarSesionRegistrada();
		convocados.remove(socio);
	}

	public RegistroEntrenamiento registrarEntrenamiento(Usuario usuario, Socio socio,
			EstadoAsistencia estado, Integer calificacion, String comentario) {
		if (usuario != entrenador) {
			throw new IllegalStateException("Solo el entrenador responsable puede registrar la asistencia");
		}
		validarSesionRegistrada();
		if (!convocados.contains(socio)) {
			throw new IllegalArgumentException("El socio no pertenece a los convocados");
		}
		RegistroEntrenamiento registro = registros.get(socio);
		if (registro == null) {
			registro = new RegistroEntrenamiento(this, socio, estado, calificacion, comentario);
			registros.put(socio, registro);
		} else {
			registro.actualizar(estado, calificacion, comentario);
		}
		return registro;
	}

	public Map<Socio, RegistroEntrenamiento> getRegistros() {
		return registros;
	}

	private void validarPermiso(Usuario usuario) {
		if (usuario != entrenador && !(usuario instanceof Administrador)) {
			throw new IllegalStateException("Solo el entrenador responsable o un administrador pueden modificar la convocatoria");
		}
	}

	private void validarSesionRegistrada() {
		if (!sesiones.contains(this)) {
			throw new IllegalStateException("La sesión no existe");
		}
	}

	private void validarSinDuplicados(List<Socio> socios) {
		List<Socio> seleccionados = new ArrayList<Socio>();
		for (Socio socio : socios) {
			if (convocados.contains(socio) || seleccionados.contains(socio)) {
				throw new IllegalStateException("El socio ya está convocado");
			}
			seleccionados.add(socio);
		}
	}
}
