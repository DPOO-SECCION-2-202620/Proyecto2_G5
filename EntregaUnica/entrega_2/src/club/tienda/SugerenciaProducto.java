package club.tienda;

import club.personal.*;
import club.usuarios.*;

public class SugerenciaProducto {
	
	//atributos
	private String nombre;
	private String descripcion;
	private TipoSugerencia tipo;
	private EstadoSugerencia estado;
	private Empleado solicitante;
	
	//ctor
	public SugerenciaProducto(String nombre, String descripcion, TipoSugerencia tipo, Empleado solicitante) {
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.tipo = tipo;
		if (nombre == null || nombre.isBlank() || descripcion == null || tipo == null || solicitante == null) {
			throw new IllegalArgumentException("Los datos de la sugerencia no son válidos");
		}
		this.estado = EstadoSugerencia.PENDIENTE;
		this.solicitante = solicitante;
	}


	//metodos
	public String getNombre() {
		return nombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public EstadoSugerencia getEstado() {
		return estado;
	}

	public Empleado getSolicitante() {
		return solicitante;
	}
	
	public void aprobar(Administrador admin) {
		if (admin == null) throw new IllegalStateException("Solo un administrador puede aprobar sugerencias");
		if (estado != EstadoSugerencia.PENDIENTE) throw new IllegalStateException("La sugerencia ya fue procesada");
		estado = EstadoSugerencia.APROBADA;
	}
	
	public void noAprobar(Administrador admin) {
		if (admin == null) throw new IllegalStateException("Solo un administrador puede rechazar sugerencias");
		if (estado != EstadoSugerencia.PENDIENTE) throw new IllegalStateException("La sugerencia ya fue procesada");
		estado = EstadoSugerencia.RECHAZADA;
	}
	
	
	
	
	

}
