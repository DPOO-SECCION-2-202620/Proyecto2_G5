package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import club.Inscripciones.InscripcionAcademia;
import club.Inscripciones.InscripcionEquipo;
import club.consola.ConsolaAdministrador;
import club.deportes.Categoria;
import club.deportes.Deporte;
import club.deportes.Equipo;
import club.deportes.Futbol;
import club.deportes.Modalidad;
import club.deportes.Tenis;
import club.entrenamientos.EstadoAsistencia;
import club.entrenamientos.RegistroEntrenamiento;
import club.entrenamientos.SesionEntrenamiento;
import club.instalaciones.Instalacion;
import club.partidos.PartidoPractica;
import club.personal.Entrenador;
import club.persistencia.PersistenciaClubJson;
import club.persistencia.PersistenciaException;
import club.tienda.Venta;
import club.tienda.VentaTienda;
import club.torneos.CatalogoTorneos;
import club.torneos.EncuentroTorneo;
import club.torneos.InscripcionTorneo;
import club.torneos.PersonaExterna;
import club.torneos.TipoTorneo;
import club.torneos.Torneo;
import club.torneos.TorneoInterno;
import club.usuarios.Administrador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;

public class PruebaRegresionesPersistencia {
    @TempDir
    Path directorioTemporal;

    private Path raiz;
    private PersistenciaClubJson persistencia;

    @BeforeEach
    void iniciarPersistencia() {
        raiz = directorioTemporal.resolve("persistencia");
        persistencia = new PersistenciaClubJson(raiz);
        persistencia.cargarClub();
    }

    @Test
    void consolaAdministrativaArrancaConVentasHistoricas() throws Exception {
        new Administrador("administrador@club.co", "clave");
        Socio comprador = crearSocio("comprador@club.co");
        Venta.registrarHistorica(new VentaTienda(comprador, LocalDateTime.now(), List.of(),
                10000, 1900, 0, 11900, 119, 0, true));
        persistencia.salvarClub();
        Path salida = directorioTemporal.resolve("arranque.txt");
        String clases = Path.of(ConsolaAdministrador.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toString();
        Process proceso = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Duser.timezone=America/Bogota", "-cp", clases, ConsolaAdministrador.class.getName())
                .directory(directorioTemporal.toFile()).redirectErrorStream(true)
                .redirectOutput(salida.toFile()).start();
        try {
            proceso.getOutputStream().write("0\n".getBytes(StandardCharsets.UTF_8));
            proceso.getOutputStream().close();
            assertTrue(proceso.waitFor(15, TimeUnit.SECONDS), "La consola no terminó");
            String texto = Files.readString(salida);
            assertEquals(0, proceso.exitValue(), texto);
            assertTrue(texto.contains("Inicio de sesión"), texto);
            assertTrue(texto.contains("Información guardada"), texto);
            assertFalse(texto.contains("No fue posible completar la persistencia"), texto);
        } finally {
            if (proceso.isAlive()) proceso.destroyForcibly();
        }
        persistencia.cargarClub();
        assertEquals(1, Venta.getVentasConfirmadas().size());
        assertEquals(11900, Venta.getVentasConfirmadas().get(0).getTotal());
    }

    @Test
    void retirarConvocadoConAsistenciaConservaHistorialYPermiteRecargar() {
        SesionEntrenamiento sesion = crearSesionConAsistencia("asistente@club.co");
        Socio socio = sesion.getConvocados().get(0);
        RegistroEntrenamiento registro = sesion.getRegistros().get(socio);

        assertThrows(IllegalStateException.class,
                () -> sesion.eliminarConvocado(sesion.getEntrenador(), socio));
        assertTrue(sesion.getConvocados().contains(socio));
        assertSame(registro, sesion.getRegistros().get(socio));
        persistencia.salvarClub();
        persistencia.cargarClub();

        SesionEntrenamiento recuperada = SesionEntrenamiento.getSesiones().get(0);
        Socio recuperado = (Socio) RepositorioUsuarios.buscarPorEmail(socio.getEmail());
        assertTrue(recuperada.getConvocados().contains(recuperado));
        assertEquals(EstadoAsistencia.ASISTIO, recuperada.getRegistros().get(recuperado).getEstado());
        assertEquals("Asistencia registrada", recuperada.getRegistros().get(recuperado).getComentario());
    }

    @Test
    void practicaDePrincipiantesNoGeneraEloNegativoAlRecargar() {
        Socio ganador = crearSocio("ganador@club.co");
        Socio perdedor = crearSocio("perdedor@club.co");
        Tenis tenis = new Tenis();
        InscripcionAcademia.inscribirSocio(ganador, tenis);
        InscripcionAcademia.inscribirSocio(perdedor, tenis);

        new PartidoPractica(tenis, Modalidad.SENCILLOS, List.of(ganador, perdedor)).finalizar(1, 0);

        assertEquals(0, InscripcionAcademia.buscar(perdedor, tenis).getElo());
        persistencia.salvarClub();
        persistencia.cargarClub();
        Tenis recuperado = (Tenis) Deporte.getDeportesRegistrados().get("Tenis");
        assertEquals(0, InscripcionAcademia.buscar(
                (Socio) RepositorioUsuarios.buscarPorEmail(perdedor.getEmail()), recuperado).getElo());
        assertEquals(6, InscripcionAcademia.buscar(
                (Socio) RepositorioUsuarios.buscarPorEmail(ganador.getEmail()), recuperado).getElo());
        assertTrue(PartidoPractica.getPartidos().get(0).isFinalizado());
    }

    @Test
    void eloRechazaValoresNoFinitosSinCambiarClasificacion() {
        Socio socio = crearSocio("elo@club.co");
        InscripcionAcademia inscripcion = InscripcionAcademia.inscribirSocio(socio, new Tenis());
        inscripcion.actualizarElo(35);

        assertThrows(IllegalArgumentException.class, () -> inscripcion.actualizarElo(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> inscripcion.actualizarElo(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> inscripcion.actualizarElo(Double.NEGATIVE_INFINITY));
        assertEquals(35, inscripcion.getElo());
        assertEquals("INTERMEDIO", inscripcion.getNivel().name());
    }

    @Test
    void externoConSeparadoresConservaTodosSusDatos() {
        Administrador administrador = new Administrador("externos@club.co", "clave");
        new Tenis();
        TorneoInterno torneo = crearTorneo(administrador);
        PersonaExterna externo = new PersonaExterna("EXT;|\\1", "Ana; Pérez | López \\ sede", "PRINCIPIANTE");
        administrador.inscribirExterno(torneo, externo);
        int torneoId = torneo.getId();

        persistencia.salvarClub();
        persistencia.cargarClub();

        PersonaExterna recuperado = (PersonaExterna) CatalogoTorneos.buscar(torneoId).getParticipantes().get(0);
        assertEquals(externo.getIdentificacion(), recuperado.getIdentificacion());
        assertEquals(externo.getNombre(), recuperado.getNombre());
        assertEquals(externo.getCategoria(), recuperado.getCategoria());
    }

    @Test
    void listasConSeparadoresConservanLasReferencias() {
        SesionEntrenamiento sesion = crearSesionConAsistencia("jugador|norte;\\uno@club.co");
        String email = sesion.getConvocados().get(0).getEmail();

        persistencia.salvarClub();
        persistencia.cargarClub();

        Socio recuperado = (Socio) RepositorioUsuarios.buscarPorEmail(email);
        SesionEntrenamiento recuperada = SesionEntrenamiento.getSesiones().get(0);
        assertSame(recuperado, recuperada.getConvocados().get(0));
        assertSame(recuperado, recuperada.getRegistros().get(recuperado).getSocio());
    }

    @Test
    void asistenciaHuerfanaNoReemplazaElEstadoEnMemoria() throws Exception {
        SesionEntrenamiento sesion = crearSesionConAsistencia("convocado@club.co");
        Socio socio = sesion.getConvocados().get(0);
        RegistroEntrenamiento registro = sesion.getRegistros().get(socio);
        persistencia.salvarClub();
        Path archivo = raiz.resolve("current/entrenamientos.json");
        String original = Files.readString(archivo);
        String corrupto = original.replaceAll("\"convocados\":\"[^\"]*\"", "\"convocados\":\"\"");
        assertNotEquals(original, corrupto);
        Files.writeString(archivo, corrupto);

        assertThrows(PersistenciaException.class, persistencia::cargarClub);

        assertSame(socio, RepositorioUsuarios.buscarPorEmail(socio.getEmail()));
        assertSame(sesion, SesionEntrenamiento.getSesiones().get(0));
        assertSame(registro, sesion.getRegistros().get(socio));
        assertSame(sesion.getDeporte(), Deporte.getDeportesRegistrados().get("Tenis"));
        assertSame(sesion.getInstalacion(), Instalacion.getInstalaciones().get("Cancha de prueba"));
    }

    @Test
    void posicionInvalidaNoReemplazaUsuariosEquiposNiInscripciones() throws Exception {
        Socio socio = crearSocio("equipo@club.co");
        Equipo equipo = new Equipo("Equipo existente", Categoria.MAYORES, 10, new Futbol()) { };
        new InscripcionEquipo().inscribirSocio(socio, equipo, Futbol.Posicion.DEFENSA, 4);
        InscripcionEquipo inscripcion = equipo.getNomina().get(0);
        persistencia.salvarClub();
        Path archivo = raiz.resolve("current/deportivo.json");
        String original = Files.readString(archivo);
        String corrupto = original.replace("\"posicion\":\"DEFENSA\"", "\"posicion\":\"INEXISTENTE\"");
        assertNotEquals(original, corrupto);
        Files.writeString(archivo, corrupto);

        assertThrows(PersistenciaException.class, persistencia::cargarClub);

        assertSame(socio, RepositorioUsuarios.buscarPorEmail(socio.getEmail()));
        assertSame(equipo, Equipo.getEquiposRegistrados().get(equipo.getNombre()));
        assertSame(inscripcion, equipo.getNomina().get(0));
        assertSame(equipo.getDeporte(), Deporte.getDeportesRegistrados().get("Fútbol"));
    }

    @Test
    void guardarMantieneLasReferenciasDelEstadoEnMemoria() {
        SesionEntrenamiento sesion = crearSesionConAsistencia("memoria@club.co");
        Socio socio = sesion.getConvocados().get(0);
        RegistroEntrenamiento registro = sesion.getRegistros().get(socio);
        InscripcionAcademia inscripcion = InscripcionAcademia.buscar(socio, (Tenis) sesion.getDeporte());

        persistencia.salvarClub();

        assertSame(socio, RepositorioUsuarios.buscarPorEmail(socio.getEmail()));
        assertSame(sesion, SesionEntrenamiento.getSesiones().get(0));
        assertSame(registro, sesion.getRegistros().get(socio));
        assertSame(sesion.getDeporte(), Deporte.getDeportesRegistrados().get("Tenis"));
        assertSame(sesion.getInstalacion(), Instalacion.getInstalaciones().get("Cancha de prueba"));
        assertSame(sesion, sesion.getInstalacion().getSesiones().get(0));
        assertSame(inscripcion, InscripcionAcademia.buscar(socio, (Tenis) sesion.getDeporte()));
    }

    @Test
    void guardadoInconsistenteConservaLaUltimaCopiaConfirmada() throws Exception {
        SesionEntrenamiento sesion = crearSesionConAsistencia("guardado@club.co");
        persistencia.salvarClub();
        Path archivo = raiz.resolve("current/entrenamientos.json");
        String confirmado = Files.readString(archivo);
        sesion.getConvocados().clear();

        assertThrows(PersistenciaException.class, persistencia::salvarClub);

        assertEquals(confirmado, Files.readString(archivo));
        persistencia.cargarClub();
        assertEquals(1, SesionEntrenamiento.getSesiones().get(0).getConvocados().size());
        assertEquals(1, SesionEntrenamiento.getSesiones().get(0).getRegistros().size());
    }

    @Test
    void encuentroConservaLaInscripcionActivaTrasRetiroYReinscripcion() {
        Administrador administrador = new Administrador("torneo@club.co", "clave");
        Socio primero = crearSocio("primero@club.co");
        Socio segundo = crearSocio("segundo@club.co");
        Tenis tenis = new Tenis();
        InscripcionAcademia.inscribirSocio(primero, tenis);
        InscripcionAcademia.inscribirSocio(segundo, tenis);
        TorneoInterno torneo = crearTorneo(administrador);
        torneo.inscribir(primero);
        int cancelada = torneo.getInscripcion(primero).getId();
        torneo.retirar(primero);
        torneo.inscribir(primero);
        torneo.inscribir(segundo);
        int activa = torneo.getInscripcion(primero).getId();
        administrador.iniciarTorneo(torneo);
        torneo.crearEncuentro(primero, segundo).registrarResultado(3, 1);
        int torneoId = torneo.getId();

        persistencia.salvarClub();
        persistencia.cargarClub();

        TorneoInterno recuperado = CatalogoTorneos.buscar(torneoId);
        Socio jugador = (Socio) RepositorioUsuarios.buscarPorEmail(primero.getEmail());
        InscripcionTorneo inscripcion = recuperado.getInscripcion(jugador);
        EncuentroTorneo encuentro = recuperado.getEncuentros().get(0);
        assertNotEquals(cancelada, activa);
        assertEquals(activa, inscripcion.getId());
        assertSame(inscripcion.getUnidadParticipacion(), encuentro.getUnidad1());
        assertEquals(3, encuentro.getPuntaje1());
        assertEquals(1, encuentro.getPuntaje2());
    }

    @Test
    void encuentroHistoricoConservaLaInscripcionRetiradaTrasReinscripcion() {
        Administrador administrador = new Administrador("historico@club.co", "clave");
        Socio primero = crearSocio("historico-primero@club.co");
        Socio segundo = crearSocio("historico-segundo@club.co");
        Tenis tenis = new Tenis();
        InscripcionAcademia.inscribirSocio(primero, tenis);
        InscripcionAcademia.inscribirSocio(segundo, tenis);
        TorneoInterno torneo = crearTorneo(administrador);
        torneo.inscribir(primero);
        torneo.inscribir(segundo);
        int historica = torneo.getInscripcion(primero).getId();
        EncuentroTorneo encuentro = torneo.crearEncuentro(primero, segundo);
        torneo.retirar(primero);
        torneo.inscribir(primero);
        administrador.iniciarTorneo(torneo);
        encuentro.registrarResultado(3, 1);
        int torneoId = torneo.getId();

        persistencia.salvarClub();
        persistencia.cargarClub();

        TorneoInterno recuperado = CatalogoTorneos.buscar(torneoId);
        Socio jugador = (Socio) RepositorioUsuarios.buscarPorEmail(primero.getEmail());
        InscripcionTorneo inscripcion = recuperado.getInscripciones().stream()
                .filter(i -> i.getId() == historica).findFirst().orElseThrow();
        assertFalse(inscripcion.isActiva());
        assertSame(inscripcion.getUnidadParticipacion(), recuperado.getEncuentros().get(0).getUnidad1());
        assertNotSame(recuperado.getInscripcion(jugador).getUnidadParticipacion(),
                recuperado.getEncuentros().get(0).getUnidad1());
    }

    private Socio crearSocio(String email) {
        Socio socio = new Socio(email, "clave");
        socio.setFechaNacimiento(LocalDate.of(2000, 1, 1));
        return socio;
    }

    private SesionEntrenamiento crearSesionConAsistencia(String email) {
        Socio socio = crearSocio(email);
        Tenis tenis = new Tenis();
        InscripcionAcademia.inscribirSocio(socio, tenis);
        Entrenador entrenador = new Entrenador("entrenamiento@club.co", "clave");
        Instalacion instalacion = new Instalacion("Cancha de prueba", tenis, List.of(Modalidad.SENCILLOS));
        SesionEntrenamiento sesion = new SesionEntrenamiento(entrenador, tenis, instalacion,
                LocalDateTime.now().plusDays(1), Duration.ofMinutes(60));
        sesion.convocarSocios(entrenador, List.of(socio));
        sesion.registrarEntrenamiento(entrenador, socio, EstadoAsistencia.ASISTIO, 4, "Asistencia registrada");
        return sesion;
    }

    private TorneoInterno crearTorneo(Administrador administrador) {
        return administrador.crearTorneo("Tenis", "PRINCIPIANTE", "INDIVIDUAL", 10,
                TipoTorneo.RECREATIVO, 0, Torneo.Formato.LIGA, LocalDateTime.now().minusMinutes(1));
    }
}
