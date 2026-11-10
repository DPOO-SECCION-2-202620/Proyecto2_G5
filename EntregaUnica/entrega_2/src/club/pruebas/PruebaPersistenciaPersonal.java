package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import club.deportes.Modalidad;
import club.deportes.Tenis;
import club.instalaciones.Instalacion;
import club.personal.Entrenador;
import club.personal.Fisioterapeuta;
import club.personal.PlantillaTurnos;
import club.personal.SolicitudCambioTurno;
import club.personal.Turno;
import club.persistencia.PersistenciaClubJson;
import club.persistencia.PersistenciaException;
import club.usuarios.Administrador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;

public class PruebaPersistenciaPersonal {

	@TempDir
	Path directorioTemporal;

	@Test
	void conservaPuntosDeSociosYEmpleados() {
		PersistenciaClubJson persistencia = preparar();
		Socio socio = new Socio("socio-puntos@club.co", "clave");
		Entrenador entrenador = new Entrenador("entrenador-puntos@club.co", "clave");
		Fisioterapeuta fisioterapeuta = new Fisioterapeuta("fisio-puntos@club.co", "clave");
		socio.getSaldoFidelidad().agregarPuntos(80);
		socio.registrarSuspension(2);
		entrenador.getSaldoFidelidad().agregarPuntos(120);
		fisioterapeuta.getSaldoFidelidad().agregarPuntos(250);

		persistencia.salvarClub();
		persistencia.cargarClub();

		Socio socioRecuperado = (Socio) RepositorioUsuarios.buscarPorEmail(socio.getEmail());
		assertEquals(80, socioRecuperado.getSaldoFidelidad().getPuntos());
		assertEquals(1, socioRecuperado.getSuspensionesTotales());
		assertEquals(2, socioRecuperado.getSuspensionRestante());
		assertEquals(120, ((Entrenador) RepositorioUsuarios.buscarPorEmail(entrenador.getEmail()))
				.getSaldoFidelidad().getPuntos());
		assertEquals(250, ((Fisioterapeuta) RepositorioUsuarios.buscarPorEmail(fisioterapeuta.getEmail()))
				.getSaldoFidelidad().getPuntos());
	}

	@Test
	void conservaInstalacionesTechadasYDescubiertas() {
		PersistenciaClubJson persistencia = preparar();
		Tenis tenis = new Tenis();
		new Instalacion("Cancha cubierta", tenis, List.of(Modalidad.SENCILLOS), true);
		new Instalacion("Cancha exterior", tenis, List.of(Modalidad.SENCILLOS), false);

		persistencia.salvarClub();
		persistencia.cargarClub();

		assertTrue(Instalacion.getInstalaciones().get("Cancha cubierta").isTechada());
		assertFalse(Instalacion.getInstalaciones().get("Cancha exterior").isTechada());
	}

	@Test
	void conservaTurnosYSolicitudesSinVolverAAplicarCambios() {
		PersistenciaClubJson persistencia = preparar();
		Administrador administrador = new Administrador("admin-turnos@club.co", "clave");
		Entrenador primero = new Entrenador("primero-turnos@club.co", "clave");
		Entrenador segundo = new Entrenador("segundo-turnos@club.co", "clave");
		Fisioterapeuta fisioterapeuta = new Fisioterapeuta("fisio-turnos@club.co", "clave");
		LocalDateTime inicio = LocalDateTime.of(2026, 11, 12, 8, 0);
		LocalDateTime fin = inicio.plusHours(4);
		PlantillaTurnos plantilla = administrador.getPlantillaTurnos();
		Turno turnoPrimero = new Turno(primero, inicio, fin);
		Turno turnoSegundo = new Turno(segundo, inicio, fin);
		Turno turnoFisio = new Turno(fisioterapeuta, inicio, fin);
		plantilla.agregarTurno(turnoPrimero);
		plantilla.agregarTurno(turnoSegundo);
		plantilla.agregarTurno(turnoFisio);
		SolicitudCambioTurno rechazada = plantilla.solicitarCambio(turnoFisio,
				new Turno(fisioterapeuta, inicio.plusHours(1), fin));
		assertThrows(IllegalStateException.class, () -> administrador.aprobarCambioTurno(rechazada));
		SolicitudCambioTurno aprobada = plantilla.solicitarCambio(turnoPrimero, new Turno(primero, inicio, fin));
		administrador.aprobarCambioTurno(aprobada);
		plantilla.solicitarCambio(turnoSegundo, new Turno(segundo, inicio, fin));
		persistencia.salvarClub();

		for (int i = 0; i < 2; i++) {
			persistencia.cargarClub();
			Administrador recuperado = (Administrador) RepositorioUsuarios.buscarPorEmail(administrador.getEmail());
			PlantillaTurnos recuperada = recuperado.getPlantillaTurnos();
			List<SolicitudCambioTurno> solicitudes = recuperada.getSolicitudes();
			assertEquals(3, recuperada.getTurnos().size());
			assertEquals(3, solicitudes.size());
			assertTrue(solicitudes.get(0).isProcesada());
			assertFalse(solicitudes.get(0).isAprobada());
			assertTrue(recuperada.getTurnos().contains(solicitudes.get(0).getTurnoAnterior()));
			assertTrue(solicitudes.get(1).isProcesada());
			assertTrue(solicitudes.get(1).isAprobada());
			assertFalse(recuperada.getTurnos().contains(solicitudes.get(1).getTurnoAnterior()));
			assertTrue(recuperada.getTurnos().contains(solicitudes.get(1).getTurnoNuevo()));
			assertFalse(solicitudes.get(2).isProcesada());
			assertTrue(recuperada.getTurnos().contains(solicitudes.get(2).getTurnoAnterior()));
			assertFalse(recuperada.getTurnos().contains(solicitudes.get(2).getTurnoNuevo()));
			assertSame(RepositorioUsuarios.buscarPorEmail(primero.getEmail()),
					solicitudes.get(1).getTurnoNuevo().getEmpleado());
			assertThrows(IllegalArgumentException.class, () -> recuperado.aprobarCambioTurno(solicitudes.get(1)));
			persistencia.salvarClub();
		}

		Administrador recuperado = (Administrador) RepositorioUsuarios.buscarPorEmail(administrador.getEmail());
		SolicitudCambioTurno pendiente = recuperado.getPlantillaTurnos().getSolicitudes().get(2);
		recuperado.aprobarCambioTurno(pendiente);
		assertTrue(pendiente.isAprobada());
		assertEquals(3, recuperado.getPlantillaTurnos().getTurnos().size());
	}

	@Test
	void admitePersistenciaAnteriorSinArchivoPersonal() throws IOException {
		PersistenciaClubJson persistencia = preparar();
		new Administrador("admin-anterior@club.co", "clave");
		persistencia.salvarClub();
		Files.delete(directorioTemporal.resolve("current/personal.json"));

		persistencia.cargarClub();

		Administrador recuperado = (Administrador) RepositorioUsuarios.buscarPorEmail("admin-anterior@club.co");
		assertNotNull(recuperado);
		assertTrue(recuperado.getPlantillaTurnos().getTurnos().isEmpty());
		assertTrue(recuperado.getPlantillaTurnos().getSolicitudes().isEmpty());
	}

	@Test
	void rechazaEstadoPersonalInvalidoSinPerderUsuariosActuales() throws IOException {
		PersistenciaClubJson persistencia = preparar();
		Administrador administrador = new Administrador("admin-invalido@club.co", "clave");
		Entrenador entrenador = new Entrenador("entrenador-invalido@club.co", "clave");
		persistencia.salvarClub();
		Files.writeString(directorioTemporal.resolve("current/personal.json"), "[{\"registro\":\"TURNO\","
				+ "\"id\":\"turno-1\",\"administrador\":\"admin-invalido@club.co\","
				+ "\"empleado\":\"entrenador-invalido@club.co\",\"inicio\":\"2026-11-12T08:00\","
				+ "\"fin\":\"2026-11-12T12:00\",\"activo\":\"desconocido\"}]");

		assertThrows(PersistenciaException.class, persistencia::cargarClub);
		assertSame(administrador, RepositorioUsuarios.buscarPorEmail(administrador.getEmail()));
		assertSame(entrenador, RepositorioUsuarios.buscarPorEmail(entrenador.getEmail()));
	}

	private PersistenciaClubJson preparar() {
		PersistenciaClubJson persistencia = new PersistenciaClubJson(directorioTemporal);
		persistencia.cargarClub();
		return persistencia;
	}
}
