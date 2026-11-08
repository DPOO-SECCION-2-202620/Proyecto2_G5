package club.pruebas;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import club.acceso.Autenticador;
import club.acceso.SesionActual;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Usuario;

public class PruebaAutenticacion {
    private Autenticador autenticador;

    @BeforeEach
    void setUp() {
        RepositorioUsuarios.getRepositorioUsuarios().clear();
        autenticador = new Autenticador();
    }

    @Test
    void usuarioInexistenteSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> autenticador.autenticar("no-existe@club.co", "clave"));
    }

    @Test
    void contrasenaCorrectaPermiteAutenticacion() {
        Usuario usuario = new Usuario("socio@club.co", "clave") { };

        SesionActual sesion = assertDoesNotThrow(
                () -> autenticador.autenticar("socio@club.co", "clave"));

        assertSame(usuario, sesion.getUsuarioActual());
        assertTrue(sesion.haySesionActiva());
    }

    @Test
    void contrasenaIncorrectaSeRechaza() {
        new Usuario("socio@club.co", "clave-correcta") { };
        assertThrows(IllegalArgumentException.class,
                () -> autenticador.autenticar("socio@club.co", "clave-incorrecta"));
    }
}
