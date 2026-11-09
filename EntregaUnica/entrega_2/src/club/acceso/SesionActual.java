package club.acceso;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

import club.Inscripciones.InscripcionAcademia;
import club.Inscripciones.InscripcionEquipo;
import club.competencias.Competencia;
import club.competencias.PartidoCompetencia;
import club.deportes.Deporte;
import club.deportes.DeporteConjunto;
import club.deportes.DeporteIndividual;
import club.deportes.Equipo;
import club.entrenamientos.SesionEntrenamiento;
import club.entrenamientos.EstadoAsistencia;
import club.entrenamientos.RegistroEntrenamiento;
import club.instalaciones.Instalacion;
import club.instalaciones.Reserva;
import club.partidos.EstadisticaJugador;
import club.partidos.Partido;
import club.partidos.PartidoInterno;
import club.partidos.PartidoPractica;
import club.personal.Entrenador;
import club.personal.Empleado;
import club.personal.SolicitudCambioTurno;
import club.reportes.ConsultasAdministrativas;
import club.reportes.PeriodoReporte;
import club.reportes.ReporteDeportivo;
import club.reportes.RubroVenta;
import club.tienda.GestorTienda;
import club.tienda.Venta;
import club.torneos.TipoTorneo;
import club.torneos.Torneo;
import club.torneos.TorneoInterno;
import club.torneos.PersonaExterna;
import club.torneos.CatalogoTorneos;
import club.usuarios.*;

public class SesionActual {
	
	//atributos
	private Usuario usuarioActual = null;

	
	//ctor 
	public SesionActual(Usuario usuarioActual) {
		super();
		this.usuarioActual = usuarioActual;
	}
	
	//metodos
	public void cerrarSesion() {
		this.usuarioActual = null;
	}
	
	public Usuario getUsuarioActual() {
		return this.usuarioActual;
	}
	
	public void registrarFechaNacimiento(LocalDate fechaNacimiento) {
		obtenerSocio().setFechaNacimiento(fechaNacimiento);
	}

	public List<Equipo> consultarEquiposDisponibles(DeporteConjunto deporte) {
		return Equipo.getEquiposDisponiblesPorCategoria(obtenerSocio(), deporte);
	}

	public InscripcionEquipo inscribirEnEquipo(Equipo equipo, Enum<?> posicion, int numeroCamiseta) {
		InscripcionEquipo inscripcion = new InscripcionEquipo();
		inscripcion.inscribirSocio(obtenerSocio(), equipo, posicion, numeroCamiseta);
		return inscripcion;
	}

	// Cambia la camiseta de una inscripcion del socio autenticado
	public void actualizarNumeroCamiseta(InscripcionEquipo inscripcion, int numeroCamiseta) {
		validarInscripcionPropia(inscripcion);
		inscripcion.actualizarNumeroCamiseta(numeroCamiseta);
	}

	// Cambia la posicion de una inscripcion del socio autenticado
	public void actualizarPosicion(InscripcionEquipo inscripcion, Enum<?> posicion) {
		validarInscripcionPropia(inscripcion);
		inscripcion.actualizarPosicion(posicion);
	}

	public List<DeporteIndividual> consultarDeportesIndividualesDisponibles() {
		obtenerSocio();
		return DeporteIndividual.getDeportesIndividualesDisponibles();
	}

	public InscripcionAcademia inscribirEnDeporteIndividual(DeporteIndividual deporte) {
		return InscripcionAcademia.inscribirSocio(obtenerSocio(), deporte);
	}

	public Reserva reservarInstalacion(List<Socio> participantes, Instalacion instalacion,
			LocalDateTime fechaHora, Duration duracion, club.deportes.Modalidad modalidad) {
		Socio socio = obtenerSocio();
		if (participantes == null || participantes.isEmpty() || participantes.get(0) != socio) {
			throw new IllegalStateException("El socio autenticado debe ser el responsable");
		}
		return new Reserva(participantes, instalacion, fechaHora, duracion, modalidad);
	}

	public PartidoPractica programarPartidoPractica(DeporteIndividual deporte,
			club.deportes.Modalidad modalidad, List<Usuario> participantes) {
		validarSesionActiva();
		return new PartidoPractica(deporte, modalidad, participantes);
	}

	public SesionEntrenamiento programarSesionEntrenamiento(Deporte deporte,
			Instalacion instalacion, LocalDateTime fechaHora, Duration duracion) {
		return new SesionEntrenamiento(obtenerEntrenador(),
				deporte, instalacion, fechaHora, duracion);
	}

	public void convocarEquipo(SesionEntrenamiento sesion, Equipo equipo) {
		validarSesionActiva();
		sesion.convocarEquipo(this.usuarioActual, equipo);
	}

	public void convocarSocios(SesionEntrenamiento sesion, List<Socio> socios) {
		validarSesionActiva();
		sesion.convocarSocios(this.usuarioActual, socios);
	}

	public void eliminarConvocado(SesionEntrenamiento sesion, Socio socio) {
		validarSesionActiva();
		sesion.eliminarConvocado(this.usuarioActual, socio);
	}

	public RegistroEntrenamiento registrarEntrenamiento(SesionEntrenamiento sesion,
			Socio socio, EstadoAsistencia estado, Integer calificacion, String comentario) {
		validarSesionActiva();
		return sesion.registrarEntrenamiento(
				this.usuarioActual, socio, estado, calificacion, comentario);
	}

	public void registrarEstadistica(Partido partido, EstadisticaJugador estadistica) {
		partido.registrarEstadistica(obtenerEntrenador(), estadistica);
	}

	public List<EstadisticaJugador> consultarEstadisticas(Socio socio) {
		validarSesionActiva();
		return Partido.consultarEstadisticas(socio);
	}

	public List<EstadisticaJugador> consultarEstadisticas(Partido partido) {
		validarSesionActiva();
		return Partido.consultarEstadisticas(partido);
	}

	public void registrarResultado(PartidoCompetencia partido, int local, int visitante) {
		partido.registrarResultado(obtenerEntrenador(), local, visitante);
	}

	public void cumplirPartidoSuspension(Socio socio) {
		obtenerEntrenador();
		socio.cumplirPartidoSuspension();
	}

	public boolean puedeDisputarPartido(Equipo equipo) {
		obtenerEntrenador();
		validarEquipoRegistrado(equipo);
		return equipo.puedeDisputarPartido();
	}

	public List<Socio> consultarJugadoresNoDisponibles(Equipo equipo) {
		obtenerEntrenador();
		validarEquipoRegistrado(equipo);
		return equipo.getJugadoresNoDisponibles();
	}

	public Socio registrarSocio(String email, String contrasenia) {
		return obtenerAdministrador().registrarSocio(email, contrasenia);
	}

	public LocalDate consultarFechaNacimiento(Socio socio) {
		return obtenerAdministrador().consultarFechaNacimiento(socio);
	}

	public void editarFechaNacimiento(Socio socio, LocalDate fechaNacimiento) {
		obtenerAdministrador().editarFechaNacimiento(socio, fechaNacimiento);
	}

	public void inscribirEquipoCompetencia(Competencia competencia, Equipo equipo) {
		obtenerAdministrador().inscribirEquipoCompetencia(competencia, equipo);
	}

	public PartidoCompetencia programarPartidoCompetencia(Competencia competencia,
			Equipo local, Equipo visitante, LocalDateTime fecha, Entrenador entrenador) {
		return obtenerAdministrador().programarPartidoCompetencia(
				competencia, local, visitante, fecha, entrenador);
	}

	public PartidoInterno programarPartidoInterno(Equipo local, Equipo visitante,
			LocalDateTime fecha, Entrenador entrenador) {
		return obtenerAdministrador().programarPartidoInterno(local, visitante, fecha, entrenador);
	}

	public TorneoInterno crearTorneo(String deporte, String categoria, String modalidad,
			int cupos, TipoTorneo tipo, double tarifa, Torneo.Formato formato,
			LocalDateTime fechaInicio) {
		return obtenerAdministrador().crearTorneo(deporte, categoria, modalidad,
				cupos, tipo, tarifa, formato, fechaInicio);
	}

	public void iniciarTorneo(TorneoInterno torneo) {
		obtenerAdministrador().iniciarTorneo(torneo);
	}

	public void finalizarTorneo(TorneoInterno torneo) {
		obtenerAdministrador().finalizarTorneo(torneo);
	}

	public void inscribirExterno(TorneoInterno torneo, PersonaExterna participante) {
		obtenerAdministrador().inscribirExterno(torneo, participante);
	}

	public void inscribirEquipoTorneo(TorneoInterno torneo, Equipo equipo) {
		obtenerEntrenador().inscribirEquipo(torneo, equipo);
	}

	// Inscribe al participante autenticado en un torneo individual
	public void inscribirEnTorneo(TorneoInterno torneo) {
		validarParticipanteTorneo(torneo);
		torneo.inscribir(usuarioActual);
	}

	// Inscribe al participante autenticado con una pareja activa
	public void inscribirParejaEnTorneo(TorneoInterno torneo, Usuario pareja) {
		validarParticipanteTorneo(torneo);
		if (pareja == null || pareja == usuarioActual || !pareja.isActivo()
				|| !(pareja instanceof Socio || pareja instanceof Empleado)) {
			throw new IllegalArgumentException("La pareja no es válida");
		}
		torneo.inscribirPareja(usuarioActual, pareja);
	}

	// Retira del torneo al participante autenticado
	public void retirarseDeTorneo(TorneoInterno torneo) {
		validarParticipanteTorneo(torneo);
		torneo.retirar(usuarioActual);
	}

	public void registrarVenta(GestorTienda gestor, Venta venta) {
		validarSesionActiva();
		if (venta.getComprador() != usuarioActual) throw new IllegalStateException("La venta no corresponde al usuario autenticado");
		gestor.registrarVenta(venta);
	}

	public void darDeBaja(Equipo equipo) { obtenerAdministrador().darDeBaja(equipo); }
	public void darDeBaja(Socio socio) { obtenerAdministrador().darDeBaja(socio); }
	public void aprobarCambioTurno(SolicitudCambioTurno solicitud) {
		obtenerAdministrador().aprobarCambioTurno(solicitud);
	}

	public List<Reserva> consultarReservasAdministrativas() {
		return new ConsultasAdministrativas().consultarReservas(obtenerAdministrador());
	}

	public List<Partido> consultarPartidosAdministrativos() {
		return new ConsultasAdministrativas().consultarPartidos(obtenerAdministrador());
	}

	public List<SesionEntrenamiento> consultarSesionesAdministrativas() {
		return new ConsultasAdministrativas().consultarSesiones(obtenerAdministrador());
	}

	public double consolidarVentas(GestorTienda gestor, RubroVenta rubro,
			PeriodoReporte periodo, LocalDate fecha) {
		return new ConsultasAdministrativas().consolidarVentas(
				obtenerAdministrador(), gestor, rubro, periodo, fecha);
	}

	public ReporteDeportivo generarReporteDeportivo() {
		return new ConsultasAdministrativas().generarReporteDeportivo(obtenerAdministrador());
	}

	public boolean haySesionAtiva() {
		return this.usuarioActual != null;
	}

	public boolean haySesionActiva() {
		return haySesionAtiva();
	}

	private void validarSesionActiva() {
		if (!haySesionAtiva()) throw new IllegalStateException("No hay una sesión activa");
		if (!usuarioActual.isActivo()) throw new IllegalStateException("El usuario está inactivo");
	}

	private void validarEquipoRegistrado(Equipo equipo) {
		if (equipo == null || !Equipo.getEquiposRegistrados().containsValue(equipo)) {
			throw new IllegalArgumentException("El equipo no existe");
		}
	}

	// Comprueba que la inscripcion pertenezca al socio autenticado
	private void validarInscripcionPropia(InscripcionEquipo inscripcion) {
		Socio socio = obtenerSocio();
		if (inscripcion == null || inscripcion.getSocio() != socio) {
			throw new IllegalStateException("La inscripción no pertenece al socio autenticado");
		}
	}

	// Comprueba la sesion y la existencia del torneo
	private void validarParticipanteTorneo(TorneoInterno torneo) {
		validarSesionActiva();
		if (!(usuarioActual instanceof Socio || usuarioActual instanceof Empleado)) {
			throw new IllegalStateException("Se requiere un socio o empleado autenticado");
		}
		if (torneo == null || CatalogoTorneos.buscar(torneo.getId()) != torneo) {
			throw new IllegalArgumentException("El torneo no existe");
		}
	}

	private Socio obtenerSocio() {
		validarSesionActiva();
		if (!(usuarioActual instanceof Socio)) throw new IllegalStateException("Se requiere un socio autenticado");
		return (Socio) usuarioActual;
	}

	private Entrenador obtenerEntrenador() {
		validarSesionActiva();
		if (!(usuarioActual instanceof Entrenador)) throw new IllegalStateException("Se requiere un entrenador autenticado");
		return (Entrenador) usuarioActual;
	}

	private Administrador obtenerAdministrador() {
		validarSesionActiva();
		if (!(usuarioActual instanceof Administrador)) throw new IllegalStateException("Se requiere un administrador autenticado");
		return (Administrador) usuarioActual;
	}
	

}
