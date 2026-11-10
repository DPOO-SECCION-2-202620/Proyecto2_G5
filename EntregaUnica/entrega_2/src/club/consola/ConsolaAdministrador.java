package club.consola;

import java.util.NoSuchElementException;
import java.util.Scanner;

import club.ClubDeportivoSeneca;
import club.acceso.SesionActual;
import club.persistencia.PersistenciaException;
import club.tienda.GestorTienda;
import club.usuarios.Administrador;

// Consola principal del administrador. Cada grupo de funcionalidades vive en su propia clase.
public class ConsolaAdministrador {

    private final EntradaConsola entrada;
    private final SesionActual sesion;
    private final ConsolaAdminTorneos torneos;
    private final ConsolaAdminDeportes deportes;
    private final ConsolaAdminPersonal personal;
    private final ConsolaAdminReportes reportes;

    public ConsolaAdministrador(EntradaConsola entrada, SesionActual sesion, GestorTienda gestor) {
        if (entrada == null || sesion == null || gestor == null) {
            throw new IllegalArgumentException("La entrada, la sesión y el gestor son obligatorios");
        }
        if (!(sesion.getUsuarioActual() instanceof Administrador)) {
            throw new IllegalStateException("Se requiere un administrador autenticado");
        }
        this.entrada = entrada;
        this.sesion = sesion;
        this.torneos = new ConsolaAdminTorneos(entrada, sesion);
        this.deportes = new ConsolaAdminDeportes(entrada, sesion);
        this.personal = new ConsolaAdminPersonal(entrada, sesion);
        this.reportes = new ConsolaAdminReportes(entrada, sesion, gestor);
    }

    // Devuelve true al cerrar sesión y false al salir
    public boolean ejecutar() {
        while (true) {
            mostrarMenu();
            int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 6);
            if (opcion == 0 || opcion == 6) {
                sesion.cerrarSesion();
                return opcion == 6;
            }
            try {
                switch (opcion) {
                    case 1: torneos.ejecutar(); break;
                    case 2: deportes.ejecutar(); break;
                    case 3: personal.gestionarTurnos(); break;
                    case 4: personal.gestionarSocios(); break;
                    case 5: reportes.ejecutar(); break;
                    default: break;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                entrada.mostrarMensaje("No fue posible realizar la operación: " + e.getMessage());
            }
        }
    }

    private void mostrarMenu() {
        entrada.mostrarMensaje("");
        entrada.mostrarMensaje("Club Deportivo Séneca - Administrador: " + sesion.getUsuarioActual().getEmail());
        entrada.mostrarMensaje("1. Torneos internos\n2. Equipos, partidos internos y competencias");
        entrada.mostrarMensaje("3. Turnos del personal\n4. Socios\n5. Reportes y consultas");
        entrada.mostrarMensaje("6. Cerrar sesión\n0. Guardar y salir");
    }

    public static void main(String[] args) {
        EntradaConsola entrada = new EntradaConsola(new Scanner(System.in), System.out);
        try (ClubDeportivoSeneca club = new ClubDeportivoSeneca()) {
            ConsolaInicioSesion inicio = new ConsolaInicioSesion(entrada, club.getAutenticador());
            GestorTienda gestor = club.getGestorTienda();
            boolean continuar = true;
            while (continuar) {
                SesionActual sesion = inicio.iniciarSesion(Administrador.class);
                if (sesion == null) break;
                continuar = new ConsolaAdministrador(entrada, sesion, gestor).ejecutar();
            }
        } catch (NoSuchElementException e) {
            entrada.mostrarMensaje("Entrada finalizada.");
        } catch (PersistenciaException e) {
            entrada.mostrarMensaje("No fue posible completar la persistencia: " + e.getMessage());
            return;
        }
        entrada.mostrarMensaje("Información guardada. Programa finalizado.");
    }
}
