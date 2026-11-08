package club.persistencia;
//Hecho por IA
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import club.Inscripciones.InscripcionAcademia;
import club.competencias.Competencia;
import club.deportes.Deporte;
import club.deportes.Equipo;
import club.entrenamientos.SesionEntrenamiento;
import club.instalaciones.Instalacion;
import club.instalaciones.Reserva;
import club.partidos.Partido;
import club.partidos.PartidoInterno;
import club.partidos.PartidoPractica;
import club.torneos.CatalogoTorneos;
import club.tienda.BonoDescuento;
import club.tienda.Venta;
import club.usuarios.RepositorioUsuarios;

public class PersistenciaClubJson implements IPersistenciaClub {

	private static final List<String> ARCHIVOS = List.of(
			PersistenciaUsuariosJson.ARCHIVO,
			PersistenciaDeportivoJson.ARCHIVO,
			PersistenciaTorneosJson.ARCHIVO,
			PersistenciaReservasJson.ARCHIVO,
			PersistenciaCompetenciasJson.ARCHIVO,
			PersistenciaPartidosJson.ARCHIVO,
			PersistenciaEntrenamientosJson.ARCHIVO,
			PersistenciaComercialJson.ARCHIVO);
	private static final long REINTENTO_MINUTOS = 2;
	private final Path raiz;
	private final Path current;
	private final Path temp;
	private final ScheduledExecutorService reintentos = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread hilo = new Thread(r, "persistencia-club");
		hilo.setDaemon(true);
		return hilo;
	});

	public PersistenciaClubJson() {
		this(Path.of("persistencia"));
	}

	public PersistenciaClubJson(Path raiz) {
		this.raiz = raiz;
		this.current = raiz.resolve("current");
		this.temp = raiz.resolve("temp");
	}

	@Override
	public void cargarClub() {
		try {
			boolean primeraEjecucion = !Files.exists(current);
			Files.createDirectories(current);
			Files.createDirectories(temp);
			if (primeraEjecucion) crearEstadoVacio();
			else validarConjunto(current);
			cargar();
		} catch (Exception e) {
			throw envolver("No fue posible cargar la persistencia", e);
		}
	}

	@Override
	public void salvarClub() {
		try {
			prepararTemp();
			publicarTemp();
		} catch (Exception e) {
			programarReintento();
			throw envolver("No fue posible guardar la persistencia", e);
		}
	}

	@Override
	public void salvarClubHastaConfirmar() {
		while (true) {
			try {
				prepararTemp();
				publicarTemp();
				return;
			} catch (Exception e) {
				try {
					TimeUnit.MINUTES.sleep(REINTENTO_MINUTOS);
				} catch (InterruptedException interrupcion) {
					Thread.currentThread().interrupt();
					throw new PersistenciaException("El guardado fue interrumpido", interrupcion);
				}
			}
		}
	}

	private void cargar() throws IOException {
		Map<String, List<Map<String, String>>> datos = new LinkedHashMap<String, List<Map<String, String>>>();
		for (String archivo : ARCHIVOS) datos.put(archivo, Json.leerArreglo(current.resolve(archivo)));
		new ValidadorPersistencia().validar(datos, ARCHIVOS.size());
		limpiarEstado();
		new PersistenciaUsuariosJson().cargar(datos.get(PersistenciaUsuariosJson.ARCHIVO));
		new PersistenciaDeportivoJson().cargar(datos.get(PersistenciaDeportivoJson.ARCHIVO));
		new PersistenciaTorneosJson().cargar(datos.get(PersistenciaTorneosJson.ARCHIVO));
		new PersistenciaReservasJson().cargar(datos.get(PersistenciaReservasJson.ARCHIVO));
		new PersistenciaCompetenciasJson().cargar(datos.get(PersistenciaCompetenciasJson.ARCHIVO));
		new PersistenciaPartidosJson().cargar(datos.get(PersistenciaPartidosJson.ARCHIVO));
		new PersistenciaEntrenamientosJson().cargar(datos.get(PersistenciaEntrenamientosJson.ARCHIVO));
		new PersistenciaComercialJson().cargar(datos.get(PersistenciaComercialJson.ARCHIVO));
	}

	private void prepararTemp() throws IOException {
		limpiarDirectorio(temp);
		Files.createDirectories(temp);
		Json.escribir(temp.resolve(PersistenciaUsuariosJson.ARCHIVO), new PersistenciaUsuariosJson().serializar());
		Json.escribir(temp.resolve(PersistenciaDeportivoJson.ARCHIVO), new PersistenciaDeportivoJson().serializar());
		Json.escribir(temp.resolve(PersistenciaTorneosJson.ARCHIVO), new PersistenciaTorneosJson().serializar());
		Json.escribir(temp.resolve(PersistenciaReservasJson.ARCHIVO), new PersistenciaReservasJson().serializar());
		Json.escribir(temp.resolve(PersistenciaCompetenciasJson.ARCHIVO), new PersistenciaCompetenciasJson().serializar());
		Json.escribir(temp.resolve(PersistenciaPartidosJson.ARCHIVO), new PersistenciaPartidosJson().serializar());
		Json.escribir(temp.resolve(PersistenciaEntrenamientosJson.ARCHIVO), new PersistenciaEntrenamientosJson().serializar());
		Json.escribir(temp.resolve(PersistenciaComercialJson.ARCHIVO), new PersistenciaComercialJson().serializar());
		validarConjunto(temp);
	}

	private void publicarTemp() throws IOException {
		Path anterior = raiz.resolve("current-anterior");
		limpiarDirectorio(anterior);
		boolean movidoAnterior = false;
		try {
			if (Files.exists(current)) { mover(current, anterior); movidoAnterior = true; }
			mover(temp, current);
			Files.createDirectories(temp);
			limpiarDirectorio(anterior);
		} catch (IOException e) {
			if (!Files.exists(current) && movidoAnterior) mover(anterior, current);
			throw e;
		}
	}

	private void crearEstadoVacio() throws IOException {
		for (String archivo : ARCHIVOS) Json.escribir(current.resolve(archivo), "[]");
	}

	private void validarConjunto(Path directorio) throws IOException {
		for (String archivo : ARCHIVOS) {
			Path ruta = directorio.resolve(archivo);
			if (!Files.isRegularFile(ruta)) throw new PersistenciaException("Falta el archivo obligatorio " + archivo);
			Json.leerArreglo(ruta);
		}
	}

	private void limpiarEstado() {
		RepositorioUsuarios.getRepositorioUsuarios().clear();
		Deporte.getDeportesRegistrados().clear();
		Equipo.getEquiposDisponibles().clear(); Equipo.getEquiposRegistrados().clear();
		CatalogoTorneos.limpiar();
		BonoDescuento.limpiar(); Venta.limpiarVentasConfirmadas();
		InscripcionAcademia.getInscripciones().clear();
		Instalacion.getInstalaciones().clear(); Reserva.getReservas().clear();
		Competencia.getCompetencias().clear(); SesionEntrenamiento.getSesiones().clear();
		Partido.getPartidos().clear(); PartidoInterno.getPartidosInternos().clear(); PartidoPractica.getPartidos().clear();
	}

	private void mover(Path origen, Path destino) throws IOException {
		try { Files.move(origen, destino, StandardCopyOption.ATOMIC_MOVE); }
		catch (AtomicMoveNotSupportedException e) { Files.move(origen, destino); }
	}

	private void limpiarDirectorio(Path directorio) throws IOException {
		if (!Files.exists(directorio)) return;
		try (var rutas = Files.walk(directorio)) {
			rutas.sorted((a, b) -> b.compareTo(a)).forEach(ruta -> {
				try { Files.delete(ruta); }
				catch (IOException e) { throw new PersistenciaException("No se pudo limpiar " + ruta, e); }
			});
		}
	}

	private void programarReintento() {
		reintentos.schedule(() -> {
			try { salvarClub(); }
			catch (PersistenciaException e) { /* guardar programa el siguiente intento */ }
		}, REINTENTO_MINUTOS, TimeUnit.MINUTES);
	}

	private PersistenciaException envolver(String mensaje, Exception e) {
		return e instanceof PersistenciaException ? (PersistenciaException) e : new PersistenciaException(mensaje, e);
	}
}
