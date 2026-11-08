package club.instalaciones;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import club.deportes.Modalidad;
import club.usuarios.Socio;

public class Reserva {

	private static final List<Reserva> reservas = new ArrayList<Reserva>();
	private List<Socio> participantes;
	private Socio responsable;
	private Instalacion instalacion;
	private LocalDateTime fechaHora;
	private Duration duracion;
	private Modalidad modalidad;

	public Reserva(List<Socio> participantes, Instalacion instalacion,
			LocalDateTime fechaHora, Duration duracion, Modalidad modalidad) {
		validar(participantes, instalacion, fechaHora, duracion, modalidad);
		this.participantes = new ArrayList<Socio>(participantes);
		this.responsable = participantes.get(0);
		this.instalacion = instalacion;
		this.fechaHora = fechaHora;
		this.duracion = duracion;
		this.modalidad = modalidad;
		reservas.add(this);
	}

	public static Reserva restaurar(List<Socio> participantes, Instalacion instalacion,
			LocalDateTime fechaHora, Duration duracion, Modalidad modalidad) {
		if (participantes == null || participantes.isEmpty() || instalacion == null
				|| fechaHora == null || duracion == null || modalidad == null) {
			throw new IllegalArgumentException("Los datos de la reserva no son válidos");
		}
		Reserva reserva = new Reserva();
		reserva.participantes = new ArrayList<Socio>(participantes);
		reserva.responsable = participantes.get(0);
		reserva.instalacion = instalacion;
		reserva.fechaHora = fechaHora;
		reserva.duracion = duracion;
		reserva.modalidad = modalidad;
		reservas.add(reserva);
		return reserva;
	}

	private Reserva() { }

	private void validar(List<Socio> participantes, Instalacion instalacion,
			LocalDateTime inicio, Duration duracion, Modalidad modalidad) {
		if (participantes == null || participantes.isEmpty() || instalacion == null
				|| inicio == null || duracion == null || modalidad == null
				|| !inicio.isAfter(LocalDateTime.now())
				|| duracion.isZero() || duracion.isNegative()) {
			throw new IllegalArgumentException("Los datos de la reserva no son válidos");
		}
		if (!Instalacion.getInstalaciones().containsValue(instalacion)) {
			throw new IllegalStateException("La instalación no existe");
		}
		int cantidadEsperada = modalidad == Modalidad.SENCILLOS ? 2 : 4;
		if (!instalacion.admiteModalidad(modalidad) || participantes.size() != cantidadEsperada) {
			throw new IllegalArgumentException("La instalación no admite la modalidad o participantes");
		}
		LocalDateTime fin = inicio.plus(duracion);
		if (!instalacion.estaDisponible(inicio, fin)) {
			throw new IllegalStateException("La instalación no está disponible");
		}
		for (Reserva reserva : reservas) {
			LocalDateTime finRegistrado = reserva.fechaHora.plus(reserva.duracion);
			if (reserva.instalacion == instalacion
					&& inicio.isBefore(finRegistrado) && fin.isAfter(reserva.fechaHora)) {
				throw new IllegalStateException("La instalación ya está reservada en ese horario");
			}
		}
	}

	public static List<Reserva> getReservas() { 
		return reservas;
	}
	
	public List<Socio> getParticipantes() { 
		return participantes; 
	}
	
	public Socio getResponsable() { return responsable; 
	}
	
	public int getCantidadJugadores() { 
		return participantes.size(); 
	}
	
	public Instalacion getInstalacion() { 
		return instalacion; 
	}
	
	public LocalDateTime getFechaHora() { 
		return fechaHora; 
	}
	
	public Duration getDuracion() { 
		return duracion; 
	}
	
	public Modalidad getModalidad() { 
		return modalidad; 
	}
}
