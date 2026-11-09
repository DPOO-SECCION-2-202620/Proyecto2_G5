package club.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.Inscripciones.InscripcionAcademia;
import club.Inscripciones.InscripcionEquipo;
import club.acceso.SesionActual;
import club.deportes.Baloncesto;
import club.deportes.Categoria;
import club.deportes.Deporte;
import club.deportes.Equipo;
import club.deportes.Futbol;
import club.deportes.Tenis;
import club.torneos.CatalogoTorneos;
import club.torneos.TipoTorneo;
import club.torneos.TorneoInterno;
import club.usuarios.Administrador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;

public class PruebaSesionSocio {

    private Socio socio;
    private Socio otro;
    private SesionActual sesion;
    private InscripcionEquipo propia;
    private InscripcionEquipo ajena;
    private Administrador administrador;
    private TorneoInterno individual;
    private TorneoInterno dobles;

    // Prepara socios inscritos en equipos y una disciplina individual
    @BeforeEach
    void preparar() {
        RepositorioUsuarios.getRepositorioUsuarios().clear();
        Equipo.getEquiposRegistrados().clear();
        Equipo.getEquiposDisponibles().clear();
        Deporte.getDeportesRegistrados().clear();
        InscripcionAcademia.getInscripciones().clear();
        CatalogoTorneos.limpiar();
        socio = new Socio("socio-sesion@club.co", "clave");
        otro = new Socio("otro-sesion@club.co", "clave");
        socio.setFechaNacimiento(LocalDate.now().minusYears(14));
        otro.setFechaNacimiento(LocalDate.now().minusYears(14));
        Equipo equipo = new Equipo("Equipo sesión", Categoria.SUB15, 10, new Futbol()) { };
        propia = new InscripcionEquipo();
        propia.inscribirSocio(socio, equipo, Futbol.Posicion.DELANTERO, 10);
        ajena = new InscripcionEquipo();
        ajena.inscribirSocio(otro, equipo, Futbol.Posicion.DEFENSA, 11);
        Tenis tenis = new Tenis();
        InscripcionAcademia.inscribirSocio(socio, tenis);
        InscripcionAcademia.inscribirSocio(otro, tenis);
        administrador = new Administrador("admin-sesion@club.co", "clave");
        individual = administrador.crearTorneo("Tenis", "PRINCIPIANTE", "INDIVIDUAL",
                10, TipoTorneo.RECREATIVO, 0);
        dobles = administrador.crearTorneo("Tenis", "PRINCIPIANTE", "DOBLES",
                10, TipoTorneo.RECREATIVO, 0);
        sesion = new SesionActual(socio);
    }

    // Actualiza los datos propios sin consumir otro cupo
    @Test
    void actualizaInscripcionExistente() {
        int cupos = propia.getEquipo().getCuposDisponibles();
        sesion.actualizarNumeroCamiseta(propia, 12);
        sesion.actualizarPosicion(propia, Futbol.Posicion.PORTERO);
        assertEquals(12, propia.getNumeroCamiseta());
        assertEquals(Futbol.Posicion.PORTERO, propia.getPosicion());
        assertEquals(cupos, propia.getEquipo().getCuposDisponibles());
        assertEquals(1, socio.getInscripciones().size());
    }

    // Conserva la camiseta cuando el numero ya esta ocupado
    @Test
    void rechazaCamisetaOcupada() {
        assertThrows(IllegalArgumentException.class, () -> sesion.actualizarNumeroCamiseta(propia, 11));
        assertEquals(10, propia.getNumeroCamiseta());
    }

    // Rechaza numeros negativos sin alterar la inscripcion
    @Test
    void rechazaCamisetaNegativa() {
        assertThrows(IllegalArgumentException.class, () -> sesion.actualizarNumeroCamiseta(propia, -1));
        assertEquals(10, propia.getNumeroCamiseta());
    }

    // Rechaza posiciones pertenecientes a otro deporte
    @Test
    void rechazaPosicionAjenaAlDeporte() {
        assertThrows(IllegalArgumentException.class,
                () -> sesion.actualizarPosicion(propia, Baloncesto.Posicion.ALERO));
        assertEquals(Futbol.Posicion.DELANTERO, propia.getPosicion());
    }

    // Impide modificar las inscripciones de otro socio
    @Test
    void rechazaCambiosSobreOtroSocio() {
        assertThrows(IllegalStateException.class, () -> sesion.actualizarNumeroCamiseta(ajena, 12));
        assertThrows(IllegalStateException.class, () -> sesion.actualizarPosicion(ajena, Futbol.Posicion.PORTERO));
        assertEquals(11, ajena.getNumeroCamiseta());
        assertEquals(Futbol.Posicion.DEFENSA, ajena.getPosicion());
    }

    // Impide realizar cambios despues de cerrar la sesion
    @Test
    void rechazaSesionCerrada() {
        sesion.cerrarSesion();
        assertThrows(IllegalStateException.class, () -> sesion.actualizarNumeroCamiseta(propia, 12));
        assertThrows(IllegalStateException.class, () -> sesion.inscribirEnTorneo(individual));
    }

    // Inscribe y retira al usuario autenticado liberando su cupo
    @Test
    void inscripcionYRetiroIndividualUsanAlUsuarioActual() {
        int cupos = individual.getCuposDisponibles();
        sesion.inscribirEnTorneo(individual);
        assertTrue(individual.estaInscrito(socio));
        assertFalse(individual.estaInscrito(otro));
        assertEquals(cupos - 1, individual.getCuposDisponibles());
        sesion.retirarseDeTorneo(individual);
        assertFalse(individual.estaInscrito(socio));
        assertEquals(cupos, individual.getCuposDisponibles());
    }

    // Registra la pareja como una unidad y libera el cupo al retirarse
    @Test
    void parejaOcupaUnCupo() {
        int cupos = dobles.getCuposDisponibles();
        sesion.inscribirParejaEnTorneo(dobles, otro);
        assertSame(dobles.getInscripcion(socio), dobles.getInscripcion(otro));
        assertEquals(cupos - 1, dobles.getCuposDisponibles());
        sesion.retirarseDeTorneo(dobles);
        assertFalse(dobles.estaInscrito(socio));
        assertFalse(dobles.estaInscrito(otro));
        assertEquals(cupos, dobles.getCuposDisponibles());
    }

    // Rechaza parejas repetidas o inactivas sin consumir cupos
    @Test
    void rechazaParejaInvalida() {
        int cupos = dobles.getCuposDisponibles();
        assertThrows(IllegalArgumentException.class, () -> sesion.inscribirParejaEnTorneo(dobles, socio));
        otro.setActivo(false);
        assertThrows(IllegalArgumentException.class, () -> sesion.inscribirParejaEnTorneo(dobles, otro));
        assertEquals(cupos, dobles.getCuposDisponibles());
    }

    // Impide inscripciones usando una cuenta administrativa
    @Test
    void rechazaAdministradorComoParticipante() {
        SesionActual sesionAdmin = new SesionActual(administrador);
        assertThrows(IllegalStateException.class, () -> sesionAdmin.inscribirEnTorneo(individual));
        assertTrue(individual.getInscripciones().isEmpty());
    }

    // Rechaza torneos que ya no existen en el catalogo
    @Test
    void rechazaTorneoNoRegistrado() {
        CatalogoTorneos.limpiar();
        assertThrows(IllegalArgumentException.class, () -> sesion.inscribirEnTorneo(individual));
        assertTrue(individual.getInscripciones().isEmpty());
    }
}
