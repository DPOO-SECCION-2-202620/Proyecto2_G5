package club.entrenamientos;

import club.usuarios.Socio;

public class RegistroEntrenamiento {

	private SesionEntrenamiento sesion;
	private Socio socio;
	private EstadoAsistencia estado;
	private Integer calificacion;
	private String comentario;

	public RegistroEntrenamiento(SesionEntrenamiento sesion, Socio socio,
			EstadoAsistencia estado, Integer calificacion, String comentario) {
		this.sesion = sesion;
		this.socio = socio;
		actualizar(estado, calificacion, comentario);
	}

	public void actualizar(EstadoAsistencia estado, Integer calificacion, String comentario) {
		if (estado == null) {
			throw new IllegalArgumentException("El estado de asistencia no es válido");
		}
		if (calificacion != null && (calificacion < 1 || calificacion > 5)) {
			throw new IllegalArgumentException("La calificación debe estar entre 1 y 5");
		}
		this.estado = estado;
		this.calificacion = calificacion;
		this.comentario = comentario;
	}

	public SesionEntrenamiento getSesion() {
		return sesion;
	}

	public Socio getSocio() {
		return socio;
	}

	public EstadoAsistencia getEstado() {
		return estado;
	}

	public Integer getCalificacion() {
		return calificacion;
	}

	public String getComentario() {
		return comentario;
	}
}
