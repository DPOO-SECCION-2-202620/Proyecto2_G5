package club;

import java.io.PrintStream;
import java.util.Scanner;

import club.acceso.Autenticador;
import club.acceso.SesionActual;
import club.persistencia.CentralPersistencia;
import club.persistencia.IPersistenciaClub;

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

	public void ejecutar(Scanner entrada, PrintStream salida) {
		if (!disponible) throw new IllegalStateException("El sistema no está disponible");
		boolean continuar = true;
		while (continuar && entrada.hasNextLine()) {
			SesionActual sesion = autenticador.getSesionActual();
			if (sesion == null || !sesion.haySesionActiva()) {
				salida.println("Club Deportivo Séneca");
				salida.println("1. Iniciar sesión");
				salida.println("0. Salir");
				salida.print("Seleccione una opción: ");
				String opcion = entrada.nextLine().trim();
				if ("1".equals(opcion)) iniciarSesion(entrada, salida);
				else if ("0".equals(opcion)) continuar = false;
				else salida.println("Opción no válida.");
			} else {
				salida.println("Sesión iniciada: " + sesion.getUsuarioActual().getEmail());
				salida.println("1. Cerrar sesión");
				salida.println("0. Guardar y salir");
				salida.print("Seleccione una opción: ");
				String opcion = entrada.nextLine().trim();
				if ("1".equals(opcion)) sesion.cerrarSesion();
				else if ("0".equals(opcion)) continuar = false;
				else salida.println("Opción no válida.");
			}
		}
		close();
		salida.println("Información guardada. Programa finalizado.");
	}

	private void iniciarSesion(Scanner entrada, PrintStream salida) {
		salida.print("Correo: ");
		if (!entrada.hasNextLine()) return;
		String email = entrada.nextLine().trim();
		salida.print("Contraseña: ");
		if (!entrada.hasNextLine()) return;
		String contrasenia = entrada.nextLine();
		try {
			autenticador.autenticar(email, contrasenia);
			salida.println("Inicio de sesión exitoso.");
		} catch (IllegalArgumentException e) {
			salida.println("No fue posible iniciar sesión: " + e.getMessage());
		}
	}

	@Override
	public synchronized void close() {
		if (!disponible) return;
		persistencia.salvarClubHastaConfirmar();
		disponible = false;
	}

	public static void main(String[] args) {
		ClubDeportivoSeneca club = new ClubDeportivoSeneca();
		club.ejecutar(new Scanner(System.in), System.out);
	}
}
