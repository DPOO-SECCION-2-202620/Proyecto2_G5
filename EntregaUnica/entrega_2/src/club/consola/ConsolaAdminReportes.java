package club.consola;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import club.Inscripciones.InscripcionAcademia;
import club.acceso.SesionActual;
import club.entrenamientos.RegistroEntrenamiento;
import club.entrenamientos.SesionEntrenamiento;
import club.instalaciones.Reserva;
import club.partidos.Partido;
import club.reportes.PeriodoReporte;
import club.reportes.ReporteDeportivo;
import club.reportes.RubroVenta;
import club.tienda.GestorTienda;

// HU Administrador 6: reportes de ventas, reportes deportivos e historiales
class ConsolaAdminReportes {

    private final EntradaConsola entrada;
    private final SesionActual sesion;
    private final GestorTienda gestor;

    ConsolaAdminReportes(EntradaConsola entrada, SesionActual sesion, GestorTienda gestor) {
        this.entrada = entrada;
        this.sesion = sesion;
        this.gestor = gestor;
    }

    void ejecutar() {
        entrada.mostrarMensaje("Reportes y consultas");
        entrada.mostrarMensaje("1. Reporte de ventas\n2. Asistencia a entrenamientos\n3. Resultados de partidos");
        entrada.mostrarMensaje("4. Rankings de deportes individuales\n5. Historial de reservas\n6. Historial de sesiones de entrenamiento\n0. Volver");
        switch (entrada.leerEntero("Seleccione una opción: ", 0, 6)) {
            case 1: reporteVentas(); break;
            case 2: asistencias(); break;
            case 3: resultados(); break;
            case 4: rankings(); break;
            case 5: reservas(); break;
            case 6: sesiones(); break;
            default: break;
        }
    }

    private void reporteVentas() {
        RubroVenta rubro = entrada.seleccionar("Rubro", Arrays.asList(RubroVenta.values()), Enum::name);
        if (rubro == null) return;
        PeriodoReporte periodo = entrada.seleccionar("Periodo", Arrays.asList(PeriodoReporte.values()), Enum::name);
        if (periodo == null) return;
        LocalDate fecha = entrada.leerFecha("Fecha de referencia del periodo");
        if (fecha == null) return;
        double total = sesion.consolidarVentas(gestor, rubro, periodo, fecha);
        entrada.mostrarMensaje("Total de ventas de " + rubro + " (" + periodo + " que incluye " + fecha + "): "
                + String.format("%,.2f", total));
    }

    private void asistencias() {
        List<RegistroEntrenamiento> registros = reporte().getAsistencias();
        if (registros.isEmpty()) entrada.mostrarMensaje("No hay registros de asistencia.");
        for (RegistroEntrenamiento r : registros) {
            entrada.mostrarMensaje(r.getSesion().getFechaHora() + " - " + r.getSesion().getDeporte().getNombre() + " - "
                    + r.getSocio().getEmail() + " - " + r.getEstado()
                    + (r.getCalificacion() == null ? "" : " - Nota: " + r.getCalificacion())
                    + (r.getComentario() == null || r.getComentario().isBlank() ? "" : " - " + r.getComentario()));
        }
    }

    private void resultados() {
        List<Partido> partidos = reporte().getResultados();
        if (partidos.isEmpty()) entrada.mostrarMensaje("No hay partidos registrados.");
        for (Partido partido : partidos) entrada.mostrarMensaje(FormatoAdministrador.partido(partido));
    }

    private void rankings() {
        List<InscripcionAcademia> inscripciones = reporte().getRankings();
        if (inscripciones.isEmpty()) entrada.mostrarMensaje("No hay inscripciones en deportes individuales.");
        inscripciones.sort((a, b) -> Double.compare(b.getElo(), a.getElo()));
        for (InscripcionAcademia i : inscripciones) {
            entrada.mostrarMensaje(i.getDeporte().getNombre() + " - " + i.getSocio().getEmail() + " - "
                    + i.getNivel() + " - ELO " + i.getElo());
        }
    }

    private void reservas() {
        List<Reserva> reservas = sesion.consultarReservasAdministrativas();
        if (reservas.isEmpty()) entrada.mostrarMensaje("No hay reservas registradas.");
        for (Reserva r : reservas) {
            entrada.mostrarMensaje(r.getFechaHora() + " - " + r.getInstalacion().getNombre() + " - " + r.getModalidad()
                    + " - Responsable: " + r.getResponsable().getEmail() + " - Jugadores: " + r.getCantidadJugadores());
        }
    }

    private void sesiones() {
        List<SesionEntrenamiento> sesiones = sesion.consultarSesionesAdministrativas();
        if (sesiones.isEmpty()) entrada.mostrarMensaje("No hay sesiones de entrenamiento registradas.");
        for (SesionEntrenamiento s : sesiones) {
            entrada.mostrarMensaje(s.getFechaHora() + " - " + s.getDeporte().getNombre() + " - "
                    + s.getInstalacion().getNombre() + " - Entrenador: " + s.getEntrenador().getEmail()
                    + " - Convocados: " + s.getConvocados().size());
        }
    }

    private ReporteDeportivo reporte() {
        return sesion.generarReporteDeportivo();
    }
}
