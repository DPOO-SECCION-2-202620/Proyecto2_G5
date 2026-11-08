package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import club.Inscripciones.InscripcionEquipo;
import club.deportes.Categoria;
import club.deportes.Equipo;
import club.deportes.Futbol;
import club.deportes.Tenis;
import club.persistencia.PersistenciaClubJson;
import club.persistencia.PersistenciaException;
import club.tienda.BonoDescuento;
import club.torneos.CatalogoTorneos;
import club.torneos.EncuentroTorneo;
import club.torneos.PersonaExterna;
import club.torneos.TipoTorneo;
import club.torneos.TorneoInterno;
import club.usuarios.Administrador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;

public class PruebaPersistencia {
	@TempDir
	Path directorioTemporal;

    @Test void guardarYRecargarConservaEstado() {
		PersistenciaClubJson persistencia = new PersistenciaClubJson(directorioTemporal.resolve("datos"));
		persistencia.cargarClub();
		Socio socio = new Socio("persistencia@club.co", "clave");
		socio.setFechaNacimiento(LocalDate.of(2000, 1, 1));
		Futbol futbol = new Futbol();
		Equipo equipo = new Equipo("Equipo persistido", Categoria.MAYORES, 10, futbol) { };
		new InscripcionEquipo().inscribirSocio(socio, equipo, Futbol.Posicion.DEFENSA, 4);
		persistencia.salvarClub();

		new PersistenciaClubJson(directorioTemporal.resolve("datos")).cargarClub();

		Socio recuperado = (Socio) RepositorioUsuarios.buscarPorEmail("persistencia@club.co");
		assertNotNull(recuperado);
		assertEquals(LocalDate.of(2000, 1, 1), recuperado.getFechaNacimiento());
		assertSame(recuperado, Equipo.getEquiposRegistrados().get("Equipo persistido")
				.getNomina().get(0).getSocio());
    }

    @Test void cargaIncompletaNoDejaEstadoParcial() {
		Path raiz = directorioTemporal.resolve("datos");
		PersistenciaClubJson persistencia = new PersistenciaClubJson(raiz);
		persistencia.cargarClub();
		Socio existente = new Socio("existente@club.co", "clave");
		persistencia.salvarClub();
		try {
			Files.delete(raiz.resolve("current/deportivo.json"));
		} catch (IOException e) {
			fail(e);
		}

		assertThrows(PersistenciaException.class, persistencia::cargarClub);
		assertSame(existente, RepositorioUsuarios.buscarPorEmail("existente@club.co"));
    }

    @Test void guardadoFallidoMantieneDirectorioAnterior() {
		Path raiz = directorioTemporal.resolve("datos");
		PersistenciaClubJson persistencia = new PersistenciaClubJson(raiz);
		persistencia.cargarClub();
		new Socio("confirmado@club.co", "clave");
		persistencia.salvarClub();
		Path usuarios = raiz.resolve("current/usuarios.json");
		String confirmado;
		try {
			confirmado = Files.readString(usuarios);
			Files.setPosixFilePermissions(directorioTemporal.resolve("datos"), Set.of(
					PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));
			assertThrows(PersistenciaException.class, persistencia::salvarClub);
			assertEquals(confirmado, Files.readString(usuarios));
		} catch (IOException e) {
			fail(e);
		} finally {
			try {
				Files.setPosixFilePermissions(directorioTemporal.resolve("datos"), Set.of(
						PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
						PosixFilePermission.OWNER_EXECUTE));
			} catch (IOException e) {
				fail(e);
			}
		}
    }

	@Test void guardarYRecargarConservaPuntosTorneosYBonos() {
		Path raiz = directorioTemporal.resolve("datos");
		PersistenciaClubJson persistencia = new PersistenciaClubJson(raiz);
		persistencia.cargarClub();

		Administrador administrador = new Administrador("admin-persistencia@club.co", "clave");
		Socio socio = new Socio("bono-persistencia@club.co", "clave");
		Futbol futbol = new Futbol();
		Equipo equipo = new Equipo("Equipo con puntos", Categoria.MAYORES, 10, futbol) { };
		equipo.sumarPuntosRanking(6);
		new Tenis();
		TorneoInterno torneo = administrador.crearTorneo("Tenis", "Avanzado", "INDIVIDUAL",
				10, TipoTorneo.RECREATIVO, 0);
		PersonaExterna primero = new PersonaExterna("EXT-P1", "Participante uno", "Avanzado");
		PersonaExterna segundo = new PersonaExterna("EXT-P2", "Participante dos", "Avanzado");
		administrador.inscribirExterno(torneo, primero);
		administrador.inscribirExterno(torneo, segundo);
		EncuentroTorneo encuentro = torneo.crearEncuentro(primero, segundo);
		encuentro.registrarResultado(3, 1);
		BonoDescuento bono = new BonoDescuento(socio, 10000, torneo);
		socio.guardarBono(bono);
		int torneoId = torneo.getId();
		String bonoId = bono.getId();

		persistencia.salvarClub();
		new PersistenciaClubJson(raiz).cargarClub();

		Equipo equipoRecuperado = Equipo.getEquiposRegistrados().get("Equipo con puntos");
		TorneoInterno torneoRecuperado = CatalogoTorneos.buscar(torneoId);
		Socio socioRecuperado = (Socio) RepositorioUsuarios.buscarPorEmail("bono-persistencia@club.co");
		BonoDescuento bonoRecuperado = BonoDescuento.buscar(bonoId);
		assertEquals(6, equipoRecuperado.getPuntosRanking());
		assertEquals(2, torneoRecuperado.getParticipantes().size());
		assertEquals(1, torneoRecuperado.getEncuentros().size());
		assertTrue(torneoRecuperado.getEncuentros().get(0).tieneResultado());
		assertEquals(3, torneoRecuperado.getEncuentros().get(0).getPuntaje1());
		assertSame(socioRecuperado, bonoRecuperado.getPropietario());
		assertSame(torneoRecuperado, bonoRecuperado.getTorneo());
		assertTrue(socioRecuperado.getBonos().contains(bonoRecuperado));
	}
}
