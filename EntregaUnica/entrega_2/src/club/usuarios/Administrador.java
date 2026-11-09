package club.usuarios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import club.competencias.Competencia;
import club.competencias.PartidoCompetencia;
import club.deportes.Equipo;
import club.partidos.PartidoInterno;
import club.personal.Entrenador;
import club.personal.Empleado;
import club.personal.PlantillaTurnos;
import club.personal.SolicitudCambioTurno;
import club.tienda.SugerenciaProducto;
import club.deportes.Deporte;
import club.torneos.ModalidadTorneo;
import club.torneos.CatalogoTorneos;
import club.torneos.HistorialTorneo;
import club.torneos.PersonaExterna;
import club.torneos.TipoTorneo;
import club.torneos.Torneo;
import club.torneos.TorneoInterno;

public class Administrador extends Usuario {
	private PlantillaTurnos plantillaTurnos = new PlantillaTurnos();

	public Administrador(String email, String contrasenia) {
		super(email, contrasenia);
	}

	public Socio registrarSocio(String email, String contrasenia) {
		return new Socio(email, contrasenia);
	}

	public LocalDate consultarFechaNacimiento(Socio socio) {
		return socio.getFechaNacimiento();
	}

	public void editarFechaNacimiento(Socio socio, LocalDate fechaNacimiento) {
		socio.editarFechaNacimiento(fechaNacimiento);
	}

	public void inscribirEquipoCompetencia(Competencia competencia, Equipo equipo) {
		competencia.inscribirEquipo(this, equipo);
	}

	public PartidoCompetencia programarPartidoCompetencia(Competencia competencia,
			Equipo local, Equipo visitante, LocalDateTime fecha, Entrenador entrenador) {
		return new PartidoCompetencia(competencia, local, visitante, fecha, entrenador);
	}

	public PartidoInterno programarPartidoInterno(Equipo local, Equipo visitante,
			LocalDateTime fecha, Entrenador entrenador) {
		return new PartidoInterno(this, local, visitante, fecha, entrenador);
	}

	public void darDeBaja(Equipo equipo) {
		if (equipo == null || !Equipo.getEquiposRegistrados().containsValue(equipo)) {
			throw new IllegalArgumentException("El equipo no existe");
		}
		equipo.setActivo(false);
	}

	public void darDeBaja(Socio socio) {
		if (socio == null || RepositorioUsuarios.buscarPorEmail(socio.getEmail()) != socio) {
			throw new IllegalArgumentException("El socio no existe");
		}
		socio.setActivo(false);
	}

	public void aprobarSugerencia(SugerenciaProducto sugerencia) {
		sugerencia.aprobar(this);
	}

	public void rechazarSugerencia(SugerenciaProducto sugerencia) {
		sugerencia.noAprobar(this);
	}

	public void aprobarCambioTurno(SolicitudCambioTurno solicitud) {
		PlantillaTurnos.aprobarSolicitud(this, solicitud);
	}

	public TorneoInterno crearTorneo(String deporte, String categoria, String modalidad,
			int cupos, TipoTorneo tipo, double tarifa) {
		return crearTorneo(deporte, categoria, modalidad, cupos, tipo, tarifa,
				Torneo.Formato.LIGA, LocalDateTime.now());
	}

	public TorneoInterno crearTorneo(String deporte, String categoria, String modalidad,
			int cupos, TipoTorneo tipo, double tarifa, Torneo.Formato formato,
			LocalDateTime fechaInicio) {
		Deporte deporteEncontrado = Deporte.getDeportesRegistrados().get(deporte);
		if (deporteEncontrado == null) {
			for (Deporte registrado : Deporte.getDeportesRegistrados().values()) {
				if (registrado.getNombre().equalsIgnoreCase(deporte)) deporteEncontrado = registrado;
			}
		}
		if (deporteEncontrado == null) throw new IllegalArgumentException("El deporte no existe");
		TorneoInterno torneo = new TorneoInterno(this, deporteEncontrado, categoria,
				ModalidadTorneo.valueOf(modalidad), cupos, tipo, tarifa,
				formato, fechaInicio);
		return torneo;
	}

	public void inscribirExterno(TorneoInterno torneo, PersonaExterna participante) {
		if (torneo == null || CatalogoTorneos.buscar(torneo.getId()) != torneo) throw new IllegalArgumentException("El torneo no existe");
		torneo.inscribirExterno(this, participante);
	}

	public List<TorneoInterno> getTorneos() {
		return CatalogoTorneos.getTorneos();
	}

	public TorneoInterno buscarTorneo(int id) {
		return CatalogoTorneos.buscar(id);
	}

	public HistorialTorneo consultarHistorial(int id) {
		TorneoInterno torneo = buscarTorneo(id);
		return torneo == null ? null : torneo.getHistorial();
	}

	public void iniciarTorneo(TorneoInterno torneo) {
		if (torneo == null || buscarTorneo(torneo.getId()) != torneo) throw new IllegalArgumentException("El torneo no existe");
		torneo.iniciar(this);
	}

	public void finalizarTorneo(TorneoInterno torneo) {
		if (torneo == null || buscarTorneo(torneo.getId()) != torneo) throw new IllegalArgumentException("El torneo no existe");
		torneo.finalizar(this);
	}

	public PlantillaTurnos getPlantillaTurnos() {
		return plantillaTurnos;
	}

	public boolean estaDeTurno(Empleado empleado, LocalDateTime momento) {
		return plantillaTurnos.estaDeTurno(empleado, momento);
	}
}
