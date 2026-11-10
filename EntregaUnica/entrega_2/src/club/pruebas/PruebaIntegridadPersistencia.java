package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import club.Inscripciones.InscripcionAcademia;
import club.deportes.Tenis;
import club.persistencia.PersistenciaClubJson;
import club.persistencia.PersistenciaException;
import club.tienda.Articulo;
import club.tienda.BonoDescuento;
import club.tienda.GestorTienda;
import club.torneos.CatalogoTorneos;
import club.torneos.InscripcionTorneo;
import club.torneos.TipoTorneo;
import club.torneos.Torneo;
import club.torneos.TorneoInterno;
import club.usuarios.Administrador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;

public class PruebaIntegridadPersistencia {
    @TempDir
    Path raiz;
    private PersistenciaClubJson persistencia;

    @BeforeEach
    void preparar() {
        persistencia = new PersistenciaClubJson(raiz);
        persistencia.cargarClub();
    }

    @Test
    void cadenasJsonConservanEscapesYUnicode() throws Exception {
        String clave = "\"\\/\n\r\t\b\f\u0001ñ😀";
        new Administrador("escapes@club.co", clave);
        persistencia.salvarClub();
        persistencia.cargarClub();
        assertEquals(clave, RepositorioUsuarios.buscarPorEmail("escapes@club.co").getContrasenia());
        Path archivo = raiz.resolve("current/usuarios.json");
        Files.writeString(archivo, Files.readString(archivo).replace("escapes@club.co", "escap\\u0065s@club.co"));
        persistencia.cargarClub();
        assertEquals(clave, RepositorioUsuarios.buscarPorEmail("escapes@club.co").getContrasenia());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "[{\"tipo\":\"SOCIO\" \"email\":\"a@club.co\"}]",
        "[{\"tipo\":\"SOCIO\",}]",
        "[{},]",
        "[{}{}]",
        "[{\"tipo\":\"SOCIO\",\"tipo\":\"SOCIO\"}]",
        "[{\"email\":\"a\\q@club.co\"}]",
        "[{\"email\":\"a\\u12XZ@club.co\"}]",
        "[{\"email\":\"a\t@club.co\"}]"
    })
    void jsonInvalidoNoReemplazaElEstado(String contenido) throws Exception {
        Administrador administrador = new Administrador("existente@club.co", "clave");
        persistencia.salvarClub();
        Files.writeString(raiz.resolve("current/usuarios.json"), contenido);
        assertThrows(PersistenciaException.class, persistencia::cargarClub);
        assertSame(administrador, RepositorioUsuarios.buscarPorEmail(administrador.getEmail()));
    }

    @Test
    void recuperaUltimoGuardadoSiSeInterrumpeLaPublicacion() throws Exception {
        new Administrador("recuperado@club.co", "clave");
        persistencia.salvarClub();
        Files.move(raiz.resolve("current"), raiz.resolve("current-anterior"));
        persistencia.cargarClub();
        assertNotNull(RepositorioUsuarios.buscarPorEmail("recuperado@club.co"));
        assertTrue(Files.isRegularFile(raiz.resolve("current/usuarios.json")));
    }

    @Test
    void validarGuardadoNoConsumeIdentificadoresNiReemplazaBonos() {
        Administrador administrador = new Administrador("contador@club.co", "clave");
        Socio socio = socio("contador-socio@club.co");
        Tenis tenis = new Tenis();
        InscripcionAcademia.inscribirSocio(socio, tenis);
        TorneoInterno torneo = torneo(administrador);
        torneo.inscribir(socio);
        BonoDescuento bono = new BonoDescuento(socio, 5000);
        socio.guardarBono(bono);
        int torneoSiguiente = CatalogoTorneos.getSiguienteId();
        int inscripcionSiguiente = InscripcionTorneo.getSiguienteId();
        int bonoSiguiente = BonoDescuento.getSiguienteId();
        persistencia.salvarClub();
        assertEquals(torneoSiguiente, CatalogoTorneos.getSiguienteId());
        assertEquals(inscripcionSiguiente, InscripcionTorneo.getSiguienteId());
        assertEquals(bonoSiguiente, BonoDescuento.getSiguienteId());
        assertSame(torneo, CatalogoTorneos.buscar(torneo.getId()));
        assertSame(bono, BonoDescuento.buscar(bono.getId()));
        assertSame(bono, socio.getBonos().get(0));
    }

    @Test
    void encuentroHistoricoConservaSuInscripcionAunqueSeCanceleDespues() {
        Administrador administrador = new Administrador("historico@club.co", "clave");
        Socio primero = socio("historico-1@club.co");
        Socio segundo = socio("historico-2@club.co");
        Tenis tenis = new Tenis();
        InscripcionAcademia.inscribirSocio(primero, tenis);
        InscripcionAcademia.inscribirSocio(segundo, tenis);
        TorneoInterno torneo = torneo(administrador);
        torneo.inscribir(primero);
        torneo.retirar(primero);
        torneo.inscribir(primero);
        torneo.inscribir(segundo);
        InscripcionTorneo inscrita = torneo.getInscripcion(primero);
        torneo.crearEncuentro(primero, segundo).registrarResultado(3, 1);
        torneo.retirar(primero);
        torneo.inscribir(primero);
        int activa = torneo.getInscripcion(primero).getId();
        persistencia.salvarClub();
        persistencia.cargarClub();
        TorneoInterno recuperado = CatalogoTorneos.buscar(torneo.getId());
        InscripcionTorneo historica = recuperado.getInscripciones().stream()
                .filter(i -> i.getId() == inscrita.getId()).findFirst().orElseThrow();
        assertFalse(historica.isActiva());
        assertSame(historica.getUnidadParticipacion(), recuperado.getEncuentros().get(0).getUnidad1());
        assertEquals(activa, recuperado.getInscripcion(RepositorioUsuarios.buscarPorEmail(primero.getEmail())).getId());
    }

    @Test
    void cargaComercialFallidaConservaElGestorYElCatalogoOriginales() throws Exception {
        Administrador administrador = new Administrador("comercial-invalido@club.co", "clave");
        GestorTienda gestor = persistencia.getGestorTienda();
        Articulo articulo = new Articulo("Balón", "Original", 10000, "Deporte");
        gestor.registrarProducto(articulo);
        persistencia.salvarClub();
        Files.writeString(raiz.resolve("current/comercial.json"), """
            [{"registro":"INVENTARIO","producto":"inexistente","ubicacion":"X","cantidad":"1"}]
            """);
        assertThrows(PersistenciaException.class, persistencia::cargarClub);
        assertSame(gestor, persistencia.getGestorTienda());
        assertSame(articulo, gestor.buscarProducto("Balón"));
        assertSame(administrador, RepositorioUsuarios.buscarPorEmail(administrador.getEmail()));
    }

    private Socio socio(String email) {
        Socio socio = new Socio(email, "clave");
        socio.setFechaNacimiento(LocalDate.of(2000, 1, 1));
        return socio;
    }

    private TorneoInterno torneo(Administrador administrador) {
        return administrador.crearTorneo("Tenis", "PRINCIPIANTE", "INDIVIDUAL", 10,
                TipoTorneo.RECREATIVO, 0, Torneo.Formato.LIGA, LocalDateTime.now().plusDays(1));
    }
}
