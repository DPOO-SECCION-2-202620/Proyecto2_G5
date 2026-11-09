package club.consola;

import java.util.ArrayList;
import java.util.List;

import club.competencias.Competencia;
import club.competencias.PartidoCompetencia;
import club.deportes.Categoria;
import club.deportes.Equipo;
import club.partidos.Partido;
import club.partidos.PartidoInterno;
import club.personal.Entrenador;
import club.personal.Turno;
import club.torneos.PersonaExterna;
import club.torneos.TorneoInterno;
import club.torneos.UnidadParticipacion;
import club.usuarios.Usuario;

// Textos compartidos por las pantallas de la consola del administrador
final class FormatoAdministrador {

    private FormatoAdministrador() { }

    static String torneo(TorneoInterno torneo) {
        return "#" + torneo.getId() + " " + torneo.getDeporte().getNombre() + " - " + torneo.getCategoria()
                + " - " + torneo.getModalidad() + " - " + torneo.getTipo() + " - " + estado(torneo)
                + " - Cupos libres: " + torneo.getCuposDisponibles() + "/" + torneo.getCupos()
                + " - Inicio: " + torneo.getFechaInicio();
    }

    // El enunciado habla de torneos "En curso" o "Finalizado"
    static String estado(TorneoInterno torneo) {
        switch (torneo.getEstado()) {
            case PENDIENTE: return "Pendiente (inscripciones abiertas)";
            case EN_CURSO: return "En curso";
            case FINALIZADO: return "Finalizado";
            default: return torneo.getEstado().name();
        }
    }

    static String participante(Object participante) {
        if (participante instanceof Usuario) return ((Usuario) participante).getEmail();
        if (participante instanceof Equipo) return ((Equipo) participante).getNombre();
        if (participante instanceof PersonaExterna) {
            PersonaExterna persona = (PersonaExterna) participante;
            return persona.getNombre() + " (externo, " + persona.getIdentificacion() + ")";
        }
        if (participante instanceof UnidadParticipacion) {
            List<String> integrantes = new ArrayList<>();
            for (Object integrante : ((UnidadParticipacion) participante).getIntegrantes()) {
                integrantes.add(participante(integrante));
            }
            return String.join(" / ", integrantes);
        }
        return "Participante";
    }

    static String equipo(Equipo equipo) {
        return equipo.getNombre() + " - " + equipo.getDeporte().getNombre() + " " + equipo.getCategoria()
                + " - Nómina: " + equipo.getNomina().size() + "/" + equipo.getCupoMaximo()
                + " - Habilitados: " + equipo.getJugadoresHabilitados().size()
                + (equipo.isActivo() ? "" : " - DADO DE BAJA");
    }

    static String competencia(Competencia competencia) {
        List<String> categorias = new ArrayList<>();
        for (Categoria categoria : competencia.getCategoriasPermitidas()) categorias.add(categoria.name());
        return competencia.getNombre() + " (" + competencia.getEntidadOrganizadora() + " " + competencia.getAnio()
                + ") - " + competencia.getDeporte().getNombre() + " - Categorías: " + String.join(", ", categorias)
                + " - Mínimo de jugadores habilitados: " + competencia.getMinimoJugadores()
                + " - Formato: " + competencia.getFormato();
    }

    static String partido(Partido partido) {
        if (partido instanceof PartidoCompetencia) {
            PartidoCompetencia p = (PartidoCompetencia) partido;
            String marcador = p.getMarcadorLocal() == null ? "sin resultado"
                    : p.getMarcadorLocal() + " a " + p.getMarcadorVisitante();
            return "[Competencia " + p.getCompetencia().getNombre() + "] " + p.getEquipoLocal().getNombre()
                    + " contra " + p.getEquipoVisitante().getNombre() + " - " + p.getFecha() + " - " + marcador;
        }
        if (partido instanceof PartidoInterno) {
            PartidoInterno p = (PartidoInterno) partido;
            return "[Interno] " + p.getEquipoLocal().getNombre() + " contra " + p.getEquipoVisitante().getNombre()
                    + " - " + p.getFecha();
        }
        return "[" + partido.getDeporte().getNombre() + "] Participantes: " + partido.getParticipantes().size();
    }

    static String turno(Turno turno) {
        String cargo = turno.getEmpleado() instanceof Entrenador ? "Entrenador" : "Fisioterapeuta";
        return turno.getEmpleado().getEmail() + " (" + cargo + ") - " + turno.getInicio() + " hasta " + turno.getFin();
    }

    static String usuario(Usuario usuario) {
        return usuario.getEmail() + (usuario.isActivo() ? "" : " - INACTIVO");
    }
}
