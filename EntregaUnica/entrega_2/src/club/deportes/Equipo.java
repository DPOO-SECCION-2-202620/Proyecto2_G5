package club.deportes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import club.Inscripciones.InscripcionEquipo;
import club.usuarios.Socio;

public abstract class Equipo {
	
	//Atributos
	private static final Map<String, Equipo> EquiposDisponibles = new HashMap<String,Equipo>();
	private static final Map<String, Equipo> equiposRegistrados = new HashMap<String, Equipo>();
	private String nombre;
	private Categoria categoria;
	private int cuposDisponibles;
	private int cupoMaximo;
	private boolean activo;
	private int puntosRanking;
	private List<InscripcionEquipo> nomina = new ArrayList<InscripcionEquipo>();
	
	private Deporte deporte;

	//ctor
	public Equipo(String nombre, Categoria categoria, int cupoMaximo, Deporte deporte) {
		super();
		if (nombre == null || categoria == null || deporte == null || cupoMaximo < 0) {
			throw new IllegalArgumentException("Los datos del equipo no son válidos");
		}
		this.nombre = nombre;
		this.categoria = categoria;
		this.cupoMaximo = cupoMaximo;
		this.cuposDisponibles = cupoMaximo;
		this.activo = true;
		this.puntosRanking = 0;
		this.deporte = deporte;
		
		Equipo.EquiposDisponibles.put(nombre, this);
		Equipo.equiposRegistrados.put(nombre, this);
	}


	//metodos
	public static Map<String, Equipo> getEquiposDisponibles(){
		return EquiposDisponibles;
	}

	public static Map<String, Equipo> getEquiposRegistrados() {
		return equiposRegistrados;
	}
	
	
	public static List<Equipo> getEquiposDisponiblesPorCategoria(Socio socio, DeporteConjunto deporte){
		if (socio.getFechaNacimiento() == null) {
			throw new IllegalStateException("El socio debe registrar su fecha de nacimiento antes de continuar");
		}
		
		List<Equipo> equiposEncontrados = new ArrayList<Equipo>();
		for (Equipo equipo : EquiposDisponibles.values()) {
			if (equipo.deporte == deporte && equipo.activo && equipo.cuposDisponibles > 0
					&& equipo.categoria.admiteEdad(socio.getFechaNacimiento())) {
				equiposEncontrados.add(equipo);
			}
		}
		
		return equiposEncontrados;
	}

	public String getNombre() {
		return nombre;
	}

	public Categoria getCategoria() {
		return categoria;
	}

	public int getCuposDisponibles() {
		return cuposDisponibles;
	}

	public int getCupoMaximo() {
		return cupoMaximo;
	}

	public boolean isActivo() {
		return activo;
	}

	public Deporte getDeporte() {
		return deporte;
	}

	public int getPuntosRanking() {
		return puntosRanking;
	}

	public void sumarPuntosRanking(int puntos) {
		if (puntos < 0) throw new IllegalArgumentException("Los puntos no pueden ser negativos");
		this.puntosRanking += puntos;
	}

	public List<InscripcionEquipo> getNomina() {
		return nomina;
	}

	public void anadirInscripcion(InscripcionEquipo inscripcion) {
		nomina.add(inscripcion);
	}

	public List<Socio> getJugadoresHabilitados() {
		List<Socio> habilitados = new ArrayList<Socio>();
		for (InscripcionEquipo inscripcion : nomina) {
			if (inscripcion.getSocio().estaHabilitado()) {
				habilitados.add(inscripcion.getSocio());
			}
		}
		return habilitados;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
		if (activo && cuposDisponibles > 0) EquiposDisponibles.put(nombre, this);
		else EquiposDisponibles.remove(nombre);
	}
	
	
	public void ocuparCupoEnEquipo() {
		if (this.cuposDisponibles <= 0) {
			throw new IllegalStateException("El equipo no tiene cupos disponibles");
		}
		this.cuposDisponibles -= 1;
		
		if (this.cuposDisponibles == 0) {
			EquiposDisponibles.remove(this.nombre);
		}
	}

	
	public void liberarCupoEnEquipo() {
		if (this.cuposDisponibles < this.cupoMaximo) {
			this.cuposDisponibles += 1;
		}
		
		if (this.activo && this.cuposDisponibles > 0) {
			EquiposDisponibles.put(this.nombre, this);
		}
		
	}
	
}
