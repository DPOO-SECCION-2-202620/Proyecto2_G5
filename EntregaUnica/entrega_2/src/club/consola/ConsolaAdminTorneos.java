package club.consola;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import club.acceso.SesionActual;
import club.deportes.Categoria;
import club.deportes.Deporte;
import club.deportes.DeporteConjunto;
import club.deportes.DeporteIndividual;
import club.deportes.Nivel;
import club.torneos.CatalogoTorneos;
import club.torneos.EncuentroTorneo;
import club.torneos.EstadoTorneo;
import club.torneos.HistorialTorneo;
import club.torneos.InscripcionTorneo;
import club.torneos.ModalidadTorneo;
import club.torneos.PersonaExterna;
import club.torneos.TipoTorneo;
import club.torneos.Torneo;
import club.torneos.TorneoInterno;
import club.torneos.UnidadParticipacion;

// Pantallas del administrador para crear, administrar y consultar torneos internos
class ConsolaAdminTorneos {

    private static final int MAXIMO_CUPOS = 64;
    private static final double MAXIMA_TARIFA = 10_000_000;

    private final EntradaConsola entrada;
    private final SesionActual sesion;

    ConsolaAdminTorneos(EntradaConsola entrada, SesionActual sesion) {
        this.entrada = entrada;
        this.sesion = sesion;
    }

    void ejecutar() {
        entrada.mostrarMensaje("Torneos internos");
        entrada.mostrarMensaje("1. Crear torneo\n2. Inscribir persona externa\n3. Consultar torneos e historial");
        entrada.mostrarMensaje("4. Iniciar torneo\n5. Registrar encuentro\n6. Registrar resultado de encuentro");
        entrada.mostrarMensaje("7. Finalizar torneo\n0. Volver");
        switch (entrada.leerEntero("Seleccione una opción: ", 0, 7)) {
            case 1: crearTorneo(); break;
            case 2: inscribirExterno(); break;
            case 3: consultarTorneos(); break;
            case 4: iniciarTorneo(); break;
            case 5: registrarEncuentro(); break;
            case 6: registrarResultado(); break;
            case 7: finalizarTorneo(); break;
            default: break;
        }
    }

    // HU: crear un nuevo torneo interno
    private void crearTorneo() {
        Deporte deporte = entrada.seleccionar("Deporte o disciplina",
                new ArrayList<>(Deporte.getDeportesRegistrados().values()), Deporte::getNombre);
        if (deporte == null) return;
        ModalidadTorneo modalidad = deporte instanceof DeporteConjunto ? ModalidadTorneo.EQUIPOS
                : entrada.seleccionar("Modalidad", Arrays.asList(ModalidadTorneo.INDIVIDUAL, ModalidadTorneo.DOBLES),
                        Enum::name);
        if (modalidad == null) return;
        String categoria = seleccionarCategoriaONivel(deporte, "Categoría o nivel del torneo");
        if (categoria == null) return;
        int cupos = entrada.leerEntero("Número de cupos (2 a " + MAXIMO_CUPOS + "): ", 2, MAXIMO_CUPOS);
        TipoTorneo tipo = entrada.seleccionar("Tipo de torneo", Arrays.asList(TipoTorneo.values()), Enum::name);
        if (tipo == null) return;
        double tarifa = 0;
        if (tipo == TipoTorneo.COMPETITIVO) {
            tarifa = entrada.leerDecimal("Tarifa de inscripción (mayor que 0): ", 0.01, MAXIMA_TARIFA);
        }
        Torneo.Formato formato = entrada.seleccionar("Formato", Arrays.asList(Torneo.Formato.values()), Enum::name);
        if (formato == null) return;
        LocalDateTime inicio = leerFechaDesdeHoy("Fecha y hora de inicio");
        if (inicio == null) return;
        int preferenciales = (int) Math.ceil(cupos * 0.20);
        entrada.mostrarMensaje("Resumen: " + deporte.getNombre() + " " + categoria + ", " + modalidad + ", " + cupos
                + " cupos (" + preferenciales + " preferenciales), " + tipo
                + (tarifa > 0 ? ", tarifa " + tarifa : "") + ", " + formato + ", inicio " + inicio);
        if (!entrada.confirmar("¿Crear el torneo?")) return;
        TorneoInterno torneo = sesion.crearTorneo(deporte.getNombre(), categoria, modalidad.name(),
                cupos, tipo, tarifa, formato, inicio);
        entrada.mostrarMensaje("Torneo creado exitosamente: " + FormatoAdministrador.torneo(torneo));
        entrada.mostrarMensaje("Cupos preferenciales reservados: " + torneo.getCuposPreferenciales());
    }

    // HU: inscribir una persona externa a la universidad
    private void inscribirExterno() {
        TorneoInterno torneo = seleccionarTorneo(EstadoTorneo.PENDIENTE, true);
        if (torneo == null) return;
        String identificacion = entrada.leerTexto("Documento de identidad: ");
        String nombre = entrada.leerTexto("Nombre completo: ");
        String categoria = seleccionarCategoriaONivel(torneo.getDeporte(), "Categoría o nivel de la persona");
        if (categoria == null || !entrada.confirmar("¿Inscribir a " + nombre + " en el torneo #" + torneo.getId() + "?")) return;
        sesion.inscribirExterno(torneo, new PersonaExterna(identificacion, nombre, categoria));
        entrada.mostrarMensaje("Inscripción exitosa. Cupos libres: " + torneo.getCuposDisponibles());
    }

    // HU: consultar informacion actual e historica de los torneos
    private void consultarTorneos() {
        List<TorneoInterno> torneos = CatalogoTorneos.getTorneos();
        if (torneos.isEmpty()) {
            entrada.mostrarMensaje("No hay torneos registrados.");
            return;
        }
        for (TorneoInterno torneo : torneos) entrada.mostrarMensaje(FormatoAdministrador.torneo(torneo));
        int id = entrada.leerEntero("Número del torneo para ver su historial (0 para volver): ", 0, Integer.MAX_VALUE);
        if (id == 0) return;
        TorneoInterno torneo = CatalogoTorneos.buscar(id);
        if (torneo == null) {
            entrada.mostrarMensaje("No se encontró el torneo solicitado.");
            return;
        }
        mostrarHistorial(torneo);
    }

    private void mostrarHistorial(TorneoInterno torneo) {
        HistorialTorneo historial = torneo.getHistorial();
        entrada.mostrarMensaje(FormatoAdministrador.torneo(torneo));
        entrada.mostrarMensaje("Estado: " + FormatoAdministrador.estado(torneo)
                + (torneo.getTipo() == TipoTorneo.COMPETITIVO ? " - Tarifa: " + torneo.getTarifa() : ""));
        entrada.mostrarMensaje("Participantes:");
        if (historial.getInscripciones().isEmpty()) entrada.mostrarMensaje("  (sin inscripciones)");
        for (InscripcionTorneo inscripcion : historial.getInscripciones()) {
            entrada.mostrarMensaje("  " + FormatoAdministrador.participante(inscripcion.getUnidadParticipacion())
                    + " - " + inscripcion.getEstado() + (inscripcion.esPreferencial() ? " - cupo preferencial" : "")
                    + " - pagó " + inscripcion.getTarifaPagada()
                    + (inscripcion.puedeRecibirPremio() ? "" : " - sin premio en efectivo"));
        }
        entrada.mostrarMensaje("Encuentros:");
        if (historial.getEncuentros().isEmpty()) entrada.mostrarMensaje("  (sin encuentros)");
        for (EncuentroTorneo encuentro : historial.getEncuentros()) entrada.mostrarMensaje("  " + describir(encuentro));
        entrada.mostrarMensaje("Posiciones (puntos):");
        if (historial.getPosiciones().isEmpty()) entrada.mostrarMensaje("  (sin resultados registrados)");
        for (Map.Entry<UnidadParticipacion, Integer> posicion : historial.getPosiciones().entrySet()) {
            entrada.mostrarMensaje("  " + FormatoAdministrador.participante(posicion.getKey()) + " - " + posicion.getValue());
        }
    }

    private void iniciarTorneo() {
        TorneoInterno torneo = seleccionarTorneo(EstadoTorneo.PENDIENTE, false);
        if (torneo == null || !entrada.confirmar("¿Iniciar el torneo? Ya no se permitirán inscripciones ni retiros.")) return;
        sesion.iniciarTorneo(torneo);
        entrada.mostrarMensaje("Torneo en curso.");
    }

    private void registrarEncuentro() {
        TorneoInterno torneo = seleccionarTorneo(EstadoTorneo.EN_CURSO, false);
        if (torneo == null) return;
        List<UnidadParticipacion> unidades = new ArrayList<>();
        for (InscripcionTorneo inscripcion : torneo.getInscripciones()) {
            if (inscripcion.isActiva()) unidades.add(inscripcion.getUnidadParticipacion());
        }
        UnidadParticipacion primero = entrada.seleccionar("Primer participante", unidades, FormatoAdministrador::participante);
        if (primero == null) return;
        unidades.remove(primero);
        UnidadParticipacion segundo = entrada.seleccionar("Segundo participante", unidades, FormatoAdministrador::participante);
        if (segundo == null) return;
        torneo.crearEncuentro(primero.getIntegrantes().get(0), segundo.getIntegrantes().get(0));
        entrada.mostrarMensaje("Encuentro registrado.");
    }

    private void registrarResultado() {
        TorneoInterno torneo = seleccionarTorneo(EstadoTorneo.EN_CURSO, false);
        if (torneo == null) return;
        List<EncuentroTorneo> pendientes = new ArrayList<>();
        for (EncuentroTorneo encuentro : torneo.getEncuentros()) if (!encuentro.tieneResultado()) pendientes.add(encuentro);
        EncuentroTorneo encuentro = entrada.seleccionar("Encuentros sin resultado", pendientes, this::describir);
        if (encuentro == null) return;
        int puntaje1 = entrada.leerEntero("Puntaje de " + FormatoAdministrador.participante(encuentro.getUnidad1()) + ": ", 0, 999);
        int puntaje2 = entrada.leerEntero("Puntaje de " + FormatoAdministrador.participante(encuentro.getUnidad2()) + ": ", 0, 999);
        encuentro.registrarResultado(puntaje1, puntaje2);
        entrada.mostrarMensaje("Resultado registrado.");
    }

    private void finalizarTorneo() {
        TorneoInterno torneo = seleccionarTorneo(EstadoTorneo.EN_CURSO, false);
        if (torneo == null || !entrada.confirmar("¿Finalizar el torneo #" + torneo.getId() + "?")) return;
        sesion.finalizarTorneo(torneo);
        entrada.mostrarMensaje("Torneo finalizado.");
        mostrarHistorial(torneo);
    }

    // Muestra solo los torneos en el estado pedido; los externos solo entran a torneos individuales o de dobles
    private TorneoInterno seleccionarTorneo(EstadoTorneo estado, boolean paraPersonas) {
        List<TorneoInterno> opciones = new ArrayList<>();
        for (TorneoInterno torneo : CatalogoTorneos.getTorneos()) {
            if (torneo.getEstado() == estado && (!paraPersonas || torneo.getModalidad() != ModalidadTorneo.EQUIPOS)) {
                opciones.add(torneo);
            }
        }
        return entrada.seleccionar("Torneos", opciones, FormatoAdministrador::torneo);
    }

    // Deportes de conjunto usan categorías por edad; disciplinas individuales usan niveles
    private String seleccionarCategoriaONivel(Deporte deporte, String titulo) {
        List<String> opciones = new ArrayList<>();
        if (deporte instanceof DeporteIndividual) for (Nivel nivel : Nivel.values()) opciones.add(nivel.name());
        else for (Categoria categoria : Categoria.values()) opciones.add(categoria.name());
        return entrada.seleccionar(titulo, opciones, texto -> texto);
    }

    // El torneo solo se puede iniciar cuando llega esta fecha, por eso se acepta desde hoy
    private LocalDateTime leerFechaDesdeHoy(String mensaje) {
        while (true) {
            LocalDateTime fecha = entrada.leerFechaHora(mensaje);
            if (fecha == null || !fecha.toLocalDate().isBefore(java.time.LocalDate.now())) return fecha;
            entrada.mostrarMensaje("La fecha no puede ser anterior a hoy.");
        }
    }

    private String describir(EncuentroTorneo encuentro) {
        return FormatoAdministrador.participante(encuentro.getUnidad1()) + " contra "
                + FormatoAdministrador.participante(encuentro.getUnidad2()) + " - "
                + (encuentro.tieneResultado() ? encuentro.getPuntaje1() + " a " + encuentro.getPuntaje2() : "Pendiente");
    }
}
