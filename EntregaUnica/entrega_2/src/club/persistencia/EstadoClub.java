package club.persistencia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.Inscripciones.InscripcionAcademia;
import club.competencias.Competencia;
import club.deportes.Deporte;
import club.deportes.Equipo;
import club.entrenamientos.SesionEntrenamiento;
import club.instalaciones.Instalacion;
import club.instalaciones.Reserva;
import club.partidos.Partido;
import club.partidos.PartidoInterno;
import club.partidos.PartidoPractica;
import club.tienda.BonoDescuento;
import club.tienda.Venta;
import club.torneos.CatalogoTorneos;
import club.torneos.TorneoInterno;
import club.torneos.InscripcionTorneo;
import club.usuarios.RepositorioUsuarios;

final class EstadoClub {

    private final List<Runnable> restauraciones = new ArrayList<>();

    EstadoClub() {
        guardar(RepositorioUsuarios.getRepositorioUsuarios());
        guardar(Deporte.getDeportesRegistrados());
        guardar(Equipo.getEquiposDisponibles());
        guardar(Equipo.getEquiposRegistrados());
        guardar(InscripcionAcademia.getInscripciones());
        guardar(Instalacion.getInstalaciones());
        guardar(Reserva.getReservas());
        guardar(Competencia.getCompetencias());
        guardar(SesionEntrenamiento.getSesiones());
        guardar(Partido.getPartidos());
        guardar(PartidoInterno.getPartidosInternos());
        guardar(PartidoPractica.getPartidos());
        List<TorneoInterno> torneos = CatalogoTorneos.getTorneos();
        List<BonoDescuento> bonos = BonoDescuento.getBonosRegistrados();
        List<Venta> ventas = Venta.getVentasConfirmadas();
        int siguienteTorneo = CatalogoTorneos.getSiguienteId();
        int siguienteBono = BonoDescuento.getSiguienteId();
        int siguienteInscripcion = InscripcionTorneo.getSiguienteId();
        restauraciones.add(() -> CatalogoTorneos.restaurarCatalogo(torneos, siguienteTorneo));
        restauraciones.add(() -> BonoDescuento.restaurarCatalogo(bonos, siguienteBono));
        restauraciones.add(() -> InscripcionTorneo.restaurarSiguienteId(siguienteInscripcion));
        restauraciones.add(() -> {
            Venta.limpiarVentasConfirmadas();
            for (Venta venta : ventas) Venta.registrarHistorica(venta);
        });
    }

    private <K, V> void guardar(Map<K, V> registro) {
        Map<K, V> anterior = new LinkedHashMap<>(registro);
        restauraciones.add(() -> {
            registro.clear();
            registro.putAll(anterior);
        });
    }

    private <T> void guardar(List<T> registro) {
        List<T> anterior = new ArrayList<>(registro);
        restauraciones.add(() -> {
            registro.clear();
            registro.addAll(anterior);
        });
    }

    void restaurar() {
        for (Runnable restauracion : restauraciones) restauracion.run();
    }
}
