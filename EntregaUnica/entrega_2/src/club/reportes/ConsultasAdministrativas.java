package club.reportes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import club.Inscripciones.InscripcionAcademia;
import club.entrenamientos.RegistroEntrenamiento;
import club.entrenamientos.SesionEntrenamiento;
import club.instalaciones.Reserva;
import club.partidos.Partido;
import club.tienda.GestorTienda;
import club.tienda.Venta;
import club.tienda.VentaCafeteria;
import club.tienda.VentaTienda;
import club.usuarios.Administrador;
import club.usuarios.Usuario;

public class ConsultasAdministrativas {

	public List<Reserva> consultarReservas(Usuario usuario) {
		validarAdministrador(usuario);
		return new ArrayList<Reserva>(Reserva.getReservas());
	}

	public List<Partido> consultarPartidos(Usuario usuario) {
		validarAdministrador(usuario);
		return new ArrayList<Partido>(Partido.getPartidos());
	}

	public List<SesionEntrenamiento> consultarSesiones(Usuario usuario) {
		validarAdministrador(usuario);
		return new ArrayList<SesionEntrenamiento>(SesionEntrenamiento.getSesiones());
	}

	public double consolidarVentas(Usuario usuario, GestorTienda gestor, RubroVenta rubro,
			PeriodoReporte periodo, LocalDate fecha) {
		validarAdministrador(usuario);
		if (gestor == null || rubro == null || periodo == null || fecha == null) {
			throw new IllegalArgumentException("Los criterios del reporte no son válidos");
		}
		LocalDate inicio = fecha, fin = fecha;
		if (periodo == PeriodoReporte.SEMANAL) {
			inicio = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			fin = fecha.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
		} else if (periodo == PeriodoReporte.MENSUAL) {
			inicio = fecha.withDayOfMonth(1); fin = fecha.withDayOfMonth(fecha.lengthOfMonth());
		}
		double total = 0;
		for (Venta venta : gestor.consultarVentas(inicio, fin)) {
			if (rubro == RubroVenta.TIENDA && venta instanceof VentaTienda) total += venta.getTotal();
			if (rubro == RubroVenta.CAFETERIA && venta instanceof VentaCafeteria) total += venta.getTotal();
		}
		return total;
	}

	public ReporteDeportivo generarReporteDeportivo(Usuario usuario) {
		validarAdministrador(usuario);
		List<RegistroEntrenamiento> asistencias = new ArrayList<RegistroEntrenamiento>();
		for (SesionEntrenamiento sesion : SesionEntrenamiento.getSesiones()) {
			asistencias.addAll(sesion.getRegistros().values());
		}
		return new ReporteDeportivo(asistencias, Partido.getPartidos(), InscripcionAcademia.getInscripciones());
	}

	private void validarAdministrador(Usuario usuario) {
		if (!(usuario instanceof Administrador) || !usuario.isActivo()) {
			throw new IllegalStateException("Se requiere un administrador activo");
		}
	}
}
