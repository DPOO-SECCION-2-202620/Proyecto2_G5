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
import club.tienda.GestorTienda;
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
			PersistenciaComercialJson.ARCHIVO,
			PersistenciaPersonalJson.ARCHIVO);
	private static final long REINTENTO_MINUTOS = 2;
	private final Path raiz;
	private final Path current;
	private final Path temp;
	private GestorTienda gestorTienda = new GestorTienda();

	public PersistenciaClubJson() {
		this(Path.of("persistencia"));
	}

	public PersistenciaClubJson(Path raiz) {
		this.raiz = raiz;
		this.current = raiz.resolve("current");
		this.temp = raiz.resolve("temp");
	}

	@Override
	public synchronized void cargarClub() {
		try {
			Path anterior = raiz.resolve("current-anterior");
			if (!Files.exists(current) && Files.exists(anterior)) mover(anterior, current);
			boolean primeraEjecucion = !Files.exists(current);
			Files.createDirectories(current);
			Files.createDirectories(temp);
			if (primeraEjecucion) crearEstadoVacio();
			cargar();
		} catch (Exception e) {
			throw envolver("No fue posible cargar la persistencia", e);
		}
	}

	@Override
	public synchronized void salvarClub() {
		try {
			prepararTemp();
			publicarTemp();
		} catch (IOException | RuntimeException e) {
			throw envolver("No fue posible guardar la persistencia", e);
		}
	}

	@Override
	public synchronized void salvarClubHastaConfirmar() {
		while (true) {
			try {
				prepararTemp();
				publicarTemp();
				return;
			} catch (IOException e) {
				try {
					TimeUnit.MINUTES.sleep(REINTENTO_MINUTOS);
				} catch (InterruptedException interrupcion) {
					Thread.currentThread().interrupt();
					throw new PersistenciaException("El guardado fue interrumpido", interrupcion);
				}
			} catch (RuntimeException e) {
				throw envolver("No fue posible guardar la persistencia", e);
			}
		}
	}

	private void cargar() throws IOException {
		Map<String, List<Map<String, String>>> datos = leerDatos(current);
		EstadoClub anterior = new EstadoClub();
		try {
			gestorTienda = reconstruir(datos);
		} catch (RuntimeException e) {
			anterior.restaurar();
			throw e;
		}
	}

	private Map<String, List<Map<String, String>>> leerDatos(Path directorio) throws IOException {
		Map<String, List<Map<String, String>>> datos = new LinkedHashMap<String, List<Map<String, String>>>();
		for (String archivo : ARCHIVOS) {
			Path ruta = directorio.resolve(archivo);
			if (archivo.equals(PersistenciaPersonalJson.ARCHIVO) && !Files.exists(ruta)) {
				datos.put(archivo, List.of());
			} else {
				if (!Files.isRegularFile(ruta)) throw new PersistenciaException("Falta el archivo obligatorio " + archivo);
				datos.put(archivo, Json.leerArreglo(ruta));
			}
		}
		return datos;
	}

	private GestorTienda reconstruir(Map<String, List<Map<String, String>>> datos) {
		new ValidadorPersistencia().validar(datos, ARCHIVOS.size());
		limpiarEstado();
		new PersistenciaUsuariosJson().cargar(datos.get(PersistenciaUsuariosJson.ARCHIVO));
		new PersistenciaPersonalJson().cargar(datos.get(PersistenciaPersonalJson.ARCHIVO));
		new PersistenciaDeportivoJson().cargar(datos.get(PersistenciaDeportivoJson.ARCHIVO));
		new PersistenciaTorneosJson().cargar(datos.get(PersistenciaTorneosJson.ARCHIVO));
		new PersistenciaReservasJson().cargar(datos.get(PersistenciaReservasJson.ARCHIVO));
		new PersistenciaCompetenciasJson().cargar(datos.get(PersistenciaCompetenciasJson.ARCHIVO));
		new PersistenciaPartidosJson().cargar(datos.get(PersistenciaPartidosJson.ARCHIVO));
		new PersistenciaEntrenamientosJson().cargar(datos.get(PersistenciaEntrenamientosJson.ARCHIVO));
		GestorTienda gestor = new GestorTienda();
		new PersistenciaComercialJson(gestor).cargar(datos.get(PersistenciaComercialJson.ARCHIVO));
		return gestor;
	}

	@Override
	public GestorTienda getGestorTienda() {
		return gestorTienda;
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
		Json.escribir(temp.resolve(PersistenciaComercialJson.ARCHIVO), new PersistenciaComercialJson(gestorTienda).serializar());
		Json.escribir(temp.resolve(PersistenciaPersonalJson.ARCHIVO), new PersistenciaPersonalJson().serializar());
		EstadoClub anterior = new EstadoClub();
		try {
			reconstruir(leerDatos(temp));
		} finally {
			anterior.restaurar();
		}
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
			for (Path ruta : rutas.sorted((a, b) -> b.compareTo(a)).toList()) Files.delete(ruta);
		}
	}

	private PersistenciaException envolver(String mensaje, Exception e) {
		return e instanceof PersistenciaException ? (PersistenciaException) e
				: new PersistenciaException(mensaje + ": " + e.getMessage(), e);
	}
}
