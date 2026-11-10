package club;

import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import club.acceso.Autenticador;
import club.acceso.SesionActual;
import club.consola.ConsolaAdministrador;
import club.consola.ConsolaEmpleado;
import club.consola.ConsolaInicioSesion;
import club.consola.ConsolaSocio;
import club.consola.EntradaConsola;
import club.persistencia.CentralPersistencia;
import club.persistencia.IPersistenciaClub;
import club.persistencia.PersistenciaException;
import club.personal.Empleado;
import club.tienda.GestorTienda;
import club.usuarios.Administrador;
import club.usuarios.Socio;
import club.usuarios.Usuario;

public class ClubDeportivoSeneca implements AutoCloseable {

	private final IPersistenciaClub persistencia;
	private final Autenticador autenticador;
	private boolean disponible;

	public ClubDeportivoSeneca() {
		this(CentralPersistencia.getPersistenciaClub(CentralPersistencia.JSON));
	}

	public ClubDeportivoSeneca(IPersistenciaClub persistencia) {
		this.persistencia = persistencia;
		this.autenticador = new Autenticador();
		persistencia.cargarClub();
		disponible = true;
		Runtime.getRuntime().addShutdownHook(new Thread(this::close, "guardado-club"));
	}

	public Autenticador getAutenticador() {
		if (!disponible) throw new IllegalStateException("El sistema no está disponible");
		return autenticador;
	}

	public GestorTienda getGestorTienda() {
		if (!disponible) throw new IllegalStateException("El sistema no está disponible");
		return persistencia.getGestorTienda();
	}

	public void ejecutar(Scanner entrada, PrintStream salida) {
		if (!disponible) throw new IllegalStateException("El sistema no está disponible");
		EntradaConsola consola = new EntradaConsola(entrada, salida);
		ConsolaInicioSesion inicio = new ConsolaInicioSesion(consola, autenticador);
		try {
			boolean continuar = true;
			while (continuar) {
				SesionActual sesion = inicio.iniciarSesion(Usuario.class);
				if (sesion == null) break;
				Usuario usuario = sesion.getUsuarioActual();
				GestorTienda gestor = getGestorTienda();
				if (usuario instanceof Administrador)
					continuar = new ConsolaAdministrador(consola, sesion, gestor).ejecutar();
				else if (usuario instanceof Socio)
					continuar = new ConsolaSocio(consola, sesion, gestor, gestor.getCodigos()).ejecutar();
				else if (usuario instanceof Empleado)
					continuar = new ConsolaEmpleado(consola, sesion, gestor, gestor.getCodigos()).ejecutar();
				else throw new IllegalStateException("Tipo de usuario desconocido");
			}
		} catch (NoSuchElementException e) {
			salida.println("Entrada finalizada.");
		}
		close();
		salida.println("Información guardada. Programa finalizado.");
	}

	@Override
	public synchronized void close() {
		if (!disponible) return;
		persistencia.salvarClubHastaConfirmar();
		disponible = false;
	}

	public static void main(String[] args) {
		try (ClubDeportivoSeneca club = new ClubDeportivoSeneca()) {
			club.ejecutar(new Scanner(System.in), System.out);
		} catch (PersistenciaException e) {
			System.out.println("No fue posible completar la persistencia: " + e.getMessage());
		}
	}
}
