package club.instalaciones;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import club.entrenamientos.SesionEntrenamiento;
import club.deportes.DeporteIndividual;
import club.deportes.Modalidad;

public class Instalacion {

	private static final Map<String, Instalacion> instalaciones = new HashMap<String, Instalacion>();
	private String nombre;
	private List<SesionEntrenamiento> sesiones = new ArrayList<SesionEntrenamiento>();
	private DeporteIndividual deporteIndividual;
	private List<Modalidad> modalidades = new ArrayList<Modalidad>();
	private boolean techada;

	public Instalacion(String nombre) {
		this.nombre = nombre;
		instalaciones.put(nombre, this);
	}

	public Instalacion(String nombre, DeporteIndividual deporteIndividual, List<Modalidad> modalidades) {
		this(nombre, deporteIndividual, modalidades, false);
	}

	public Instalacion(String nombre, DeporteIndividual deporteIndividual,
			List<Modalidad> modalidades, boolean techada) {
		this(nombre);
		this.deporteIndividual = deporteIndividual;
		this.modalidades = new ArrayList<Modalidad>(modalidades);
		this.techada = techada;
	}

	public String getNombre() {
		return nombre;
	}

	public static Map<String, Instalacion> getInstalaciones() {
		return instalaciones;
	}

	public boolean estaDisponible(LocalDateTime inicio, LocalDateTime fin) {
		for (SesionEntrenamiento sesion : sesiones) {
			LocalDateTime inicioRegistrado = sesion.getFechaHora();
			LocalDateTime finRegistrado = inicioRegistrado.plus(sesion.getDuracion());
			if (inicio.isBefore(finRegistrado) && fin.isAfter(inicioRegistrado)) {
				return false;
			}
		}
		return true;
	}

	public void registrarSesion(SesionEntrenamiento sesion) {
		sesiones.add(sesion);
	}

	public List<SesionEntrenamiento> getSesiones() {
		return sesiones;
	}

	public DeporteIndividual getDeporteIndividual() { 
		return deporteIndividual; 
	}
	
	public boolean admiteModalidad(Modalidad modalidad) { 
		return modalidades.contains(modalidad); 
	}

	public List<Modalidad> getModalidades() {
		return modalidades;
	}

	public boolean isTechada() { return techada; }
}
