package club.consola;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import club.acceso.SesionActual;
import club.competencias.Competencia;
import club.deportes.Categoria;
import club.deportes.Deporte;
import club.deportes.DeporteConjunto;
import club.deportes.Equipo;
import club.partidos.PartidoInterno;
import club.personal.Entrenador;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Usuario;

// Pantallas del administrador para equipos, partidos internos y competencias universitarias
class ConsolaAdminDeportes {

    private static final int MAXIMO_CUPO_EQUIPO = 40;

    private final EntradaConsola entrada;
    private final SesionActual sesion;

    ConsolaAdminDeportes(EntradaConsola entrada, SesionActual sesion) {
        this.entrada = entrada;
        this.sesion = sesion;
    }

    void ejecutar() {
        entrada.mostrarMensaje("Equipos y competencias");
        entrada.mostrarMensaje("1. Consultar equipos\n2. Crear equipo\n3. Dar de baja un equipo");
        entrada.mostrarMensaje("4. Programar partido interno\n5. Inscribir equipo en competencia universitaria\n0. Volver");
        switch (entrada.leerEntero("Seleccione una opción: ", 0, 5)) {
            case 1: consultarEquipos(); break;
            case 2: crearEquipo(); break;
            case 3: darDeBajaEquipo(); break;
            case 4: programarPartidoInterno(); break;
            case 5: inscribirEnCompetencia(); break;
            default: break;
        }
    }

    private void consultarEquipos() {
        List<Equipo> equipos = equiposOrdenados(false);
        if (equipos.isEmpty()) entrada.mostrarMensaje("No hay equipos registrados.");
        for (Equipo equipo : equipos) entrada.mostrarMensaje(FormatoAdministrador.equipo(equipo));
    }

    // HU Administrador 3: crear un equipo
    private void crearEquipo() {
        String nombre = entrada.leerTexto("Nombre del equipo: ");
        if (Equipo.getEquiposRegistrados().containsKey(nombre)) {
            entrada.mostrarMensaje("Ya existe un equipo con ese nombre.");
            return;
        }
        List<Deporte> deportes = new ArrayList<>();
        for (Deporte deporte : Deporte.getDeportesRegistrados().values()) {
            if (deporte instanceof DeporteConjunto) deportes.add(deporte);
        }
        Deporte deporte = entrada.seleccionar("Deporte de conjunto", deportes, Deporte::getNombre);
        if (deporte == null) return;
        Categoria categoria = entrada.seleccionar("Categoría", Arrays.asList(Categoria.values()), Enum::name);
        if (categoria == null) return;
        int minimo = ((DeporteConjunto) deporte).getMinimoJugadores();
        int cupo = entrada.leerEntero("Cupo máximo (" + minimo + " a " + MAXIMO_CUPO_EQUIPO + "): ", minimo, MAXIMO_CUPO_EQUIPO);
        if (!entrada.confirmar("¿Crear el equipo " + nombre + " de " + deporte.getNombre() + " " + categoria + "?")) return;
        // Equipo es abstracta; se instancia igual que en la carga de persistencia
        Equipo equipo = new Equipo(nombre, categoria, cupo, deporte) { };
        entrada.mostrarMensaje("Equipo creado: " + FormatoAdministrador.equipo(equipo));
    }

    // HU Administrador 4: dar de baja un equipo
    private void darDeBajaEquipo() {
        Equipo equipo = entrada.seleccionar("Equipos activos", equiposOrdenados(true), FormatoAdministrador::equipo);
        if (equipo == null || !entrada.confirmar("¿Dar de baja el equipo " + equipo.getNombre() + "?")) return;
        sesion.darDeBaja(equipo);
        entrada.mostrarMensaje("El equipo quedó registrado como no disponible para nuevas actividades.");
    }

    // HU Administrador 1: programar un partido interno
    private void programarPartidoInterno() {
        List<Equipo> equipos = equiposOrdenados(true);
        Equipo local = entrada.seleccionar("Equipo local", equipos, FormatoAdministrador::equipo);
        if (local == null) return;
        equipos.remove(local);
        Equipo visitante = entrada.seleccionar("Equipo visitante", equipos, FormatoAdministrador::equipo);
        if (visitante == null) return;
        Entrenador entrenador = seleccionarEntrenador();
        if (entrenador == null) return;
        LocalDateTime fecha = entrada.leerFechaHora("Fecha y hora del partido");
        if (fecha == null || !entrada.confirmar("¿Programar " + local.getNombre() + " contra " + visitante.getNombre() + "?")) return;
        PartidoInterno partido = sesion.programarPartidoInterno(local, visitante, fecha, entrenador);
        entrada.mostrarMensaje("Partido programado: " + FormatoAdministrador.partido(partido));
    }

    // HU Administrador 2: inscribir un equipo en una competencia universitaria
    private void inscribirEnCompetencia() {
        List<Competencia> competencias = new ArrayList<>(Competencia.getCompetencias().values());
        Competencia competencia = entrada.seleccionar("Competencias disponibles", competencias, FormatoAdministrador::competencia);
        if (competencia == null) return;
        List<Equipo> candidatos = new ArrayList<>();
        for (Equipo equipo : equiposOrdenados(true)) {
            if (!competencia.getEquipos().contains(equipo)) candidatos.add(equipo);
        }
        Equipo equipo = entrada.seleccionar("Equipo a inscribir", candidatos, FormatoAdministrador::equipo);
        if (equipo == null || !entrada.confirmar("¿Inscribir a " + equipo.getNombre() + " en " + competencia.getNombre() + "?")) return;
        sesion.inscribirEquipoCompetencia(competencia, equipo);
        entrada.mostrarMensaje("Equipo inscrito en la competencia.");
    }

    private Entrenador seleccionarEntrenador() {
        List<Entrenador> entrenadores = new ArrayList<>();
        for (Usuario usuario : RepositorioUsuarios.getRepositorioUsuarios().values()) {
            if (usuario instanceof Entrenador && usuario.isActivo()) entrenadores.add((Entrenador) usuario);
        }
        entrenadores.sort(Comparator.comparing(Usuario::getEmail));
        return entrada.seleccionar("Entrenador responsable", entrenadores, Usuario::getEmail);
    }

    private List<Equipo> equiposOrdenados(boolean soloActivos) {
        List<Equipo> equipos = new ArrayList<>();
        for (Equipo equipo : Equipo.getEquiposRegistrados().values()) {
            if (!soloActivos || equipo.isActivo()) equipos.add(equipo);
        }
        equipos.sort(Comparator.comparing(Equipo::getNombre));
        return equipos;
    }
}
