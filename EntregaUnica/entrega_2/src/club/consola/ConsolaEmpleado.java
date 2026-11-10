package club.consola;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import club.ClubDeportivoSeneca;
import club.Inscripciones.InscripcionAcademia;
import club.Inscripciones.InscripcionEquipo;
import club.acceso.SesionActual;
import club.competencias.PartidoCompetencia;
import club.deportes.Baloncesto;
import club.deportes.Deporte;
import club.deportes.DeporteConjunto;
import club.deportes.DeporteIndividual;
import club.deportes.Equipo;
import club.deportes.Futbol;
import club.deportes.Padel;
import club.deportes.Tenis;
import club.deportes.TenisDeMesa;
import club.deportes.Voleibol;
import club.entrenamientos.EstadoAsistencia;
import club.entrenamientos.RegistroEntrenamiento;
import club.entrenamientos.SesionEntrenamiento;
import club.instalaciones.Instalacion;
import club.partidos.EstadisticaBaloncesto;
import club.partidos.EstadisticaFutbol;
import club.partidos.EstadisticaJugador;
import club.partidos.EstadisticaTenis;
import club.partidos.EstadisticaTenisDeMesa;
import club.partidos.EstadisticaVoleibol;
import club.partidos.Partido;
import club.partidos.PartidoInterno;
import club.personal.Empleado;
import club.personal.Entrenador;
import club.personal.SolicitudCambioTurno;
import club.personal.Turno;
import club.persistencia.PersistenciaException;
import club.tienda.Articulo;
import club.tienda.BonoDescuento;
import club.tienda.CodigoDescuento;
import club.tienda.GestorTienda;
import club.tienda.Producto;
import club.tienda.ProductoCafeteria;
import club.tienda.SugerenciaProducto;
import club.tienda.TipoSugerencia;
import club.torneos.CatalogoTorneos;
import club.torneos.EncuentroTorneo;
import club.torneos.EstadoTorneo;
import club.torneos.HistorialTorneo;
import club.torneos.InscripcionTorneo;
import club.torneos.ModalidadTorneo;
import club.torneos.PersonaExterna;
import club.torneos.TorneoInterno;
import club.torneos.UnidadParticipacion;
import club.usuarios.RepositorioUsuarios;
import club.usuarios.Socio;
import club.usuarios.Usuario;

public class ConsolaEmpleado {

    private final EntradaConsola entrada;
    private final SesionActual sesion;
    private final GestorTienda gestor;
    private final List<CodigoDescuento> codigos;
    private final ConsolaCompras compras;

    // Recibe la sesion y los servicios usados por el empleado
    public ConsolaEmpleado(EntradaConsola entrada, SesionActual sesion,
            GestorTienda gestor, List<CodigoDescuento> codigos) {
        if (entrada == null || sesion == null || gestor == null || codigos == null) {
            throw new IllegalArgumentException("La entrada, la sesión, el gestor y los códigos son obligatorios");
        }
        this.entrada = entrada;
        this.sesion = sesion;
        this.gestor = gestor;
        this.codigos = new ArrayList<>(codigos);
        this.compras = new ConsolaCompras(entrada, sesion, gestor);
        obtenerEmpleado();
    }

    // Devuelve true al cerrar sesion y false al salir
    public boolean ejecutar() {
        while (true) {
            obtenerEmpleado();
            mostrarMenu();
            int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 9);
            if (opcion == 0 || opcion == 9) {
                sesion.cerrarSesion();
                return opcion == 9;
            }
            try {
                switch (opcion) {
                    case 1: consultarBeneficios(); break;
                    case 2: gestionarTurnos(); break;
                    case 3: gestionarTorneos(); break;
                    case 4: realizarCompra(); break;
                    case 5: gestionarSugerencias(); break;
                    case 6: gestionarEntrenamientos(); break;
                    case 7: gestionarPartidos(); break;
                    case 8: consultarDisponibilidad(); break;
                    default: break;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                entrada.mostrarMensaje("No fue posible realizar la operación: " + e.getMessage());
            }
        }
    }

    // Muestra las opciones disponibles segun el tipo de empleado
    private void mostrarMenu() {
        entrada.mostrarMensaje("Club Deportivo Séneca - Empleado: " + obtenerEmpleado().getEmail());
        entrada.mostrarMensaje("1. Mi perfil y beneficios\n2. Mis turnos y solicitudes\n3. Torneos");
        entrada.mostrarMensaje("4. Tienda y cafetería\n5. Sugerencias de productos");
        if (obtenerEmpleado() instanceof Entrenador) {
            entrada.mostrarMensaje("6. Entrenamientos\n7. Partidos y estadísticas\n8. Disponibilidad de equipos");
        }
        entrada.mostrarMensaje("9. Cerrar sesión\n0. Guardar y salir");
    }

    // Consulta puntos bonos y codigos propios del empleado
    private void consultarBeneficios() {
        Empleado empleado = obtenerEmpleado();
        entrada.mostrarMensaje("Correo: " + empleado.getEmail());
        entrada.mostrarMensaje("Cargo: " + (empleado instanceof Entrenador ? "Entrenador" : "Fisioterapeuta"));
        entrada.mostrarMensaje("Puntos disponibles: " + empleado.getSaldoFidelidad().getPuntos());
        boolean hayCodigos = false;
        for (CodigoDescuento codigo : codigos) {
            if (codigo.getPropietario() == empleado) {
                entrada.mostrarMensaje("Código: " + codigo.getCodigo() + " - Descuento propio: "
                        + codigo.porcentajePara(empleado) * 100 + "%");
                hayCodigos = true;
            }
        }
        if (!hayCodigos) entrada.mostrarMensaje("No tiene códigos de descuento registrados.");
        List<BonoDescuento> bonos = empleado.getBonosDisponibles();
        if (bonos.isEmpty()) entrada.mostrarMensaje("No tiene bonos disponibles.");
        for (BonoDescuento bono : bonos) {
            entrada.mostrarMensaje(bono.getId() + " - Valor: " + bono.getValor());
        }
    }

    // Consulta turnos y permite solicitar un cambio de horario
    private void gestionarTurnos() {
        List<Turno> turnos = sesion.consultarMisTurnos();
        if (turnos.isEmpty()) entrada.mostrarMensaje("No tiene turnos asignados.");
        for (Turno turno : turnos) entrada.mostrarMensaje(describirTurno(turno));
        entrada.mostrarMensaje("Mis solicitudes:");
        for (SolicitudCambioTurno solicitud : sesion.consultarMisSolicitudesCambio()) {
            String estado = !solicitud.isProcesada() ? "Pendiente"
                    : solicitud.isAprobada() ? "Aprobada" : "Rechazada";
            entrada.mostrarMensaje(describirTurno(solicitud.getTurnoAnterior()) + " → "
                    + describirTurno(solicitud.getTurnoNuevo()) + " - " + estado);
        }
        entrada.mostrarMensaje("1. Solicitar cambio de turno\n0. Volver");
        if (entrada.leerEntero("Seleccione una opción: ", 0, 1) == 0) return;
        Turno turno = entrada.seleccionar("Mis turnos", turnos, this::describirTurno);
        if (turno == null) return;
        LocalDateTime inicio = entrada.leerFechaHora("Nuevo inicio");
        if (inicio == null) return;
        LocalDateTime fin = entrada.leerFechaHora("Nuevo fin");
        if (fin == null || !entrada.confirmar("¿Enviar la solicitud de cambio?")) return;
        sesion.solicitarCambioTurno(turno, inicio, fin);
        entrada.mostrarMensaje("Solicitud enviada. El horario cambiará cuando un administrador la apruebe.");
    }

    // Describe el horario de un turno
    private String describirTurno(Turno turno) {
        return turno.getInicio() + " hasta " + turno.getFin();
    }

    // Consulta sugerencias propias y permite enviar una nueva
    private void gestionarSugerencias() {
        List<SugerenciaProducto> sugerencias = sesion.consultarMisSugerencias(gestor);
        if (sugerencias.isEmpty()) entrada.mostrarMensaje("No tiene sugerencias registradas.");
        for (SugerenciaProducto sugerencia : sugerencias) {
            entrada.mostrarMensaje(sugerencia.getNombre() + " - " + sugerencia.getEstado());
        }
        entrada.mostrarMensaje("1. Enviar sugerencia\n0. Volver");
        if (entrada.leerEntero("Seleccione una opción: ", 0, 1) == 0) return;
        TipoSugerencia tipo = entrada.seleccionar("Tipo de producto",
                Arrays.asList(TipoSugerencia.values()), Enum::name);
        if (tipo == null) return;
        String nombre = entrada.leerTexto("Nombre del producto: ");
        String descripcion = entrada.leerTexto("Descripción: ");
        if (entrada.confirmar("¿Enviar la sugerencia?")) {
            sesion.registrarSugerencia(gestor, nombre, descripcion, tipo);
            entrada.mostrarMensaje("Sugerencia registrada.");
        }
    }

    // Abre el flujo compartido de tienda o cafeteria
    private void realizarCompra() {
        entrada.mostrarMensaje("1. Tienda\n2. Cafetería\n0. Volver");
        int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 2);
        if (opcion == 1) {
            List<Articulo> articulos = new ArrayList<>();
            for (Producto producto : gestor.getProductos()) {
                if (producto instanceof Articulo) articulos.add((Articulo) producto);
            }
            compras.comprarEnTienda(articulos, codigos);
        } else if (opcion == 2) {
            List<ProductoCafeteria> productos = new ArrayList<>();
            for (Producto producto : gestor.getProductos()) {
                if (producto instanceof ProductoCafeteria) productos.add((ProductoCafeteria) producto);
            }
            if (productos.isEmpty()) {
                entrada.mostrarMensaje("No hay productos disponibles.");
                return;
            }
            Instalacion instalacion = null;
            if (entrada.confirmar("¿Consumirá la compra en una instalación deportiva?")) {
                instalacion = seleccionarInstalacion(null);
                if (instalacion == null) return;
            }
            compras.comprarEnCafeteria(productos, instalacion);
        }
    }

    // Permite consultar torneos e inscribirse sin costo como empleado
    private void gestionarTorneos() {
        List<TorneoInterno> torneos = CatalogoTorneos.getTorneos();
        if (torneos.isEmpty()) {
            entrada.mostrarMensaje("No hay torneos registrados.");
            return;
        }
        for (TorneoInterno torneo : torneos) entrada.mostrarMensaje(describirTorneo(torneo));
        entrada.mostrarMensaje("En torneos competitivos los empleados no pagan inscripción ni reciben premio en efectivo.");
        entrada.mostrarMensaje("1. Consultar historial\n2. Inscribirme\n3. Retirarme");
        boolean entrenador = obtenerEmpleado() instanceof Entrenador;
        if (entrenador) entrada.mostrarMensaje("4. Inscribir equipo");
        entrada.mostrarMensaje("0. Volver");
        int opcion = entrada.leerEntero("Seleccione una opción: ", 0, entrenador ? 4 : 3);
        if (opcion == 0) return;
        List<TorneoInterno> disponibles = new ArrayList<>();
        for (TorneoInterno torneo : torneos) {
            if (opcion == 1 || torneo.getEstado() == EstadoTorneo.PENDIENTE
                    && (opcion == 2 && torneo.getModalidad() != ModalidadTorneo.EQUIPOS
                    || opcion == 3 && torneo.estaInscrito(obtenerEmpleado())
                    || opcion == 4 && torneo.getModalidad() == ModalidadTorneo.EQUIPOS)) {
                disponibles.add(torneo);
            }
        }
        TorneoInterno torneo = entrada.seleccionar("Torneos", disponibles, this::describirTorneo);
        if (torneo == null) return;
        if (opcion == 1) consultarHistorial(torneo);
        else if (opcion == 2) inscribirseEnTorneo(torneo);
        else if (opcion == 3 && entrada.confirmar("¿Retirar su inscripción y la de su pareja si corresponde?")) {
            sesion.retirarseDeTorneo(torneo);
            entrada.mostrarMensaje("Inscripción retirada. Cupo liberado.");
        } else if (opcion == 4) {
            Equipo equipo = seleccionarEquipo(torneo.getDeporte());
            if (equipo != null && entrada.confirmar("¿Inscribir al equipo " + equipo.getNombre() + "?")) {
                sesion.inscribirEquipoTorneo(torneo, equipo);
                entrada.mostrarMensaje("Equipo inscrito.");
            }
        }
    }

    // Describe el estado y los cupos de un torneo
    private String describirTorneo(TorneoInterno torneo) {
        return torneo.getId() + ". " + torneo.getDeporte().getNombre() + " - " + torneo.getCategoria()
                + " - " + torneo.getModalidad() + " - " + torneo.getTipo() + " - " + torneo.getEstado()
                + " - Cupos: " + torneo.getCuposDisponibles() + " - Inicio: " + torneo.getFechaInicio();
    }

    // Inscribe al empleado solo o con una pareja activa
    private void inscribirseEnTorneo(TorneoInterno torneo) {
        Usuario pareja = null;
        if (torneo.getModalidad() == ModalidadTorneo.DOBLES) {
            List<Usuario> disponibles = new ArrayList<>();
            for (Usuario usuario : RepositorioUsuarios.getRepositorioUsuarios().values()) {
                if (usuario != obtenerEmpleado() && usuario.isActivo()
                        && (usuario instanceof Socio || usuario instanceof Empleado)) disponibles.add(usuario);
            }
            disponibles.sort(Comparator.comparing(Usuario::getEmail));
            pareja = entrada.seleccionar("Pareja del torneo", disponibles, Usuario::getEmail);
            if (pareja == null) return;
        }
        if (!entrada.confirmar("¿Confirmar la inscripción en el torneo?")) return;
        if (pareja == null) sesion.inscribirEnTorneo(torneo);
        else sesion.inscribirParejaEnTorneo(torneo, pareja);
        InscripcionTorneo inscripcion = torneo.getInscripcion(obtenerEmpleado());
        entrada.mostrarMensaje("Inscripción registrada. Tarifa: " + inscripcion.getTarifaPagada());
    }

    // Consulta participantes encuentros y posiciones en cualquier estado
    private void consultarHistorial(TorneoInterno torneo) {
        HistorialTorneo historial = torneo.getHistorial();
        entrada.mostrarMensaje(describirTorneo(torneo));
        entrada.mostrarMensaje("Inscripciones:");
        for (InscripcionTorneo inscripcion : historial.getInscripciones()) {
            entrada.mostrarMensaje(describirParticipante(inscripcion.getUnidadParticipacion())
                    + " - " + inscripcion.getEstado());
        }
        entrada.mostrarMensaje("Encuentros:");
        for (EncuentroTorneo encuentro : historial.getEncuentros()) {
            entrada.mostrarMensaje(describirParticipante(encuentro.getUnidad1()) + " contra "
                    + describirParticipante(encuentro.getUnidad2()) + " - " + (encuentro.tieneResultado()
                    ? encuentro.getPuntaje1() + " a " + encuentro.getPuntaje2() : "Pendiente"));
        }
        entrada.mostrarMensaje("Posiciones:");
        for (Map.Entry<UnidadParticipacion, Integer> posicion : historial.getPosiciones().entrySet()) {
            entrada.mostrarMensaje(describirParticipante(posicion.getKey()) + " - " + posicion.getValue());
        }
    }

    // Describe los integrantes de una inscripcion de torneo
    private String describirParticipante(Object participante) {
        if (participante instanceof Usuario) return ((Usuario) participante).getEmail();
        if (participante instanceof Equipo) return ((Equipo) participante).getNombre();
        if (participante instanceof PersonaExterna) return ((PersonaExterna) participante).getNombre();
        if (participante instanceof UnidadParticipacion) {
            List<String> integrantes = new ArrayList<>();
            for (Object integrante : ((UnidadParticipacion) participante).getIntegrantes()) {
                integrantes.add(describirParticipante(integrante));
            }
            return String.join(" / ", integrantes);
        }
        return "Participante";
    }

    // Ofrece las operaciones de entrenamiento del entrenador
    private void gestionarEntrenamientos() {
        obtenerEntrenador();
        entrada.mostrarMensaje("1. Programar sesión\n2. Modificar convocatoria");
        entrada.mostrarMensaje("3. Registrar asistencia y evaluación\n4. Consultar registros\n0. Volver");
        int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 4);
        if (opcion == 0) return;
        if (opcion == 1) {
            programarEntrenamiento();
            return;
        }
        List<SesionEntrenamiento> propias = new ArrayList<>();
        for (SesionEntrenamiento entrenamiento : SesionEntrenamiento.getSesiones()) {
            if (entrenamiento.getEntrenador() == obtenerEntrenador()) propias.add(entrenamiento);
        }
        propias.sort(Comparator.comparing(SesionEntrenamiento::getFechaHora));
        SesionEntrenamiento entrenamiento = entrada.seleccionar("Mis entrenamientos", propias,
                e -> e.getDeporte().getNombre() + " - " + e.getFechaHora() + " - " + e.getInstalacion().getNombre());
        if (entrenamiento == null) return;
        if (opcion == 2) modificarConvocatoria(entrenamiento);
        else if (opcion == 3) registrarAsistencia(entrenamiento);
        else consultarRegistros(entrenamiento);
    }

    // Programa una sesion con equipo o jugadores seleccionados
    private void programarEntrenamiento() {
        Deporte deporte = entrada.seleccionar("Deportes", new ArrayList<>(Deporte.getDeportesRegistrados().values()),
                Deporte::getNombre);
        if (deporte == null) return;
        Instalacion instalacion = seleccionarInstalacion(deporte);
        if (instalacion == null) return;
        LocalDateTime fecha = entrada.leerFechaHora("Inicio del entrenamiento");
        if (fecha == null) return;
        int minutos = entrada.leerEntero("Duración en minutos (0 para cancelar): ", 0, Integer.MAX_VALUE);
        if (minutos == 0) return;
        Equipo equipo = null;
        List<Socio> convocados = new ArrayList<>();
        if (deporte instanceof DeporteConjunto) {
            equipo = seleccionarEquipo(deporte);
            if (equipo == null) return;
            for (InscripcionEquipo inscripcion : equipo.getNomina()) convocados.add(inscripcion.getSocio());
        } else {
            convocados = seleccionarConvocados((DeporteIndividual) deporte, List.of());
        }
        if (convocados.isEmpty()) {
            entrada.mostrarMensaje("Debe seleccionar al menos un jugador para el entrenamiento.");
            return;
        }
        if (!entrada.confirmar("¿Programar el entrenamiento con " + convocados.size() + " convocados?")) return;
        SesionEntrenamiento entrenamiento = sesion.programarSesionEntrenamiento(
                deporte, instalacion, fecha, Duration.ofMinutes(minutos));
        if (equipo != null) sesion.convocarEquipo(entrenamiento, equipo);
        else sesion.convocarSocios(entrenamiento, convocados);
        entrada.mostrarMensaje("Entrenamiento programado.");
    }

    // Selecciona una instalacion compatible con el deporte indicado
    private Instalacion seleccionarInstalacion(Deporte deporte) {
        List<Instalacion> instalaciones = new ArrayList<>();
        for (Instalacion instalacion : Instalacion.getInstalaciones().values()) {
            if (deporte == null || instalacion.getDeporteIndividual() == null
                    || instalacion.getDeporteIndividual().getClass().equals(deporte.getClass())) {
                instalaciones.add(instalacion);
            }
        }
        instalaciones.sort(Comparator.comparing(Instalacion::getNombre));
        return entrada.seleccionar("Instalaciones", instalaciones, Instalacion::getNombre);
    }

    // Selecciona un equipo activo del deporte indicado
    private Equipo seleccionarEquipo(Deporte deporte) {
        List<Equipo> equipos = new ArrayList<>();
        for (Equipo equipo : Equipo.getEquiposRegistrados().values()) {
            if (equipo.isActivo() && (deporte == null
                    || equipo.getDeporte().getClass().equals(deporte.getClass()))) equipos.add(equipo);
        }
        equipos.sort(Comparator.comparing(Equipo::getNombre));
        return entrada.seleccionar("Equipos", equipos,
                e -> e.getNombre() + " - " + e.getDeporte().getNombre() + " - " + e.getCategoria());
    }

    // Selecciona jugadores inscritos sin repetir convocados
    private List<Socio> seleccionarConvocados(DeporteIndividual deporte, List<Socio> actuales) {
        List<Socio> seleccionados = new ArrayList<>();
        while (true) {
            List<Socio> disponibles = new ArrayList<>();
            for (Usuario usuario : RepositorioUsuarios.getRepositorioUsuarios().values()) {
                if (usuario instanceof Socio && usuario.isActivo() && !actuales.contains(usuario)
                        && !seleccionados.contains(usuario) && InscripcionAcademia.estaInscrito((Socio) usuario, deporte)) {
                    disponibles.add((Socio) usuario);
                }
            }
            disponibles.sort(Comparator.comparing(Usuario::getEmail));
            Socio socio = entrada.seleccionar("Convocados (0 para terminar)", disponibles, Usuario::getEmail);
            if (socio == null) return seleccionados;
            seleccionados.add(socio);
        }
    }

    // Agrega o retira jugadores de una convocatoria propia
    private void modificarConvocatoria(SesionEntrenamiento entrenamiento) {
        for (Socio socio : entrenamiento.getConvocados()) entrada.mostrarMensaje(socio.getEmail());
        entrada.mostrarMensaje("1. Agregar convocados\n2. Retirar convocado\n0. Volver");
        int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 2);
        if (opcion == 1) {
            if (entrenamiento.getDeporte() instanceof DeporteConjunto) {
                Equipo equipo = seleccionarEquipo(entrenamiento.getDeporte());
                if (equipo != null && entrada.confirmar("¿Convocar al equipo?")) sesion.convocarEquipo(entrenamiento, equipo);
                else return;
            } else {
                List<Socio> nuevos = seleccionarConvocados((DeporteIndividual) entrenamiento.getDeporte(),
                        entrenamiento.getConvocados());
                if (nuevos.isEmpty() || !entrada.confirmar("¿Agregar los jugadores seleccionados?")) return;
                sesion.convocarSocios(entrenamiento, nuevos);
            }
        } else if (opcion == 2) {
            Socio socio = entrada.seleccionar("Convocados", entrenamiento.getConvocados(), Usuario::getEmail);
            if (socio == null || !entrada.confirmar("¿Retirar al jugador de la convocatoria?")) return;
            sesion.eliminarConvocado(entrenamiento, socio);
        } else return;
        entrada.mostrarMensaje("Convocatoria actualizada.");
    }

    // Registra asistencia y una evaluacion opcional de un convocado
    private void registrarAsistencia(SesionEntrenamiento entrenamiento) {
        Socio socio = entrada.seleccionar("Convocados", entrenamiento.getConvocados(), Usuario::getEmail);
        if (socio == null) return;
        EstadoAsistencia estado = entrada.seleccionar("Asistencia", Arrays.asList(EstadoAsistencia.values()), Enum::name);
        if (estado == null) return;
        int nota = entrada.leerEntero("Calificación de 1 a 5 (0 sin calificar): ", 0, 5);
        String comentario = entrada.leerLinea("Comentario (puede dejarlo vacío): ");
        if (entrada.confirmar("¿Guardar el registro?")) {
            sesion.registrarEntrenamiento(entrenamiento, socio, estado, nota == 0 ? null : nota, comentario);
            entrada.mostrarMensaje("Asistencia y evaluación registradas.");
        }
    }

    // Consulta asistencia y evaluaciones de una sesion propia
    private void consultarRegistros(SesionEntrenamiento entrenamiento) {
        for (Socio socio : entrenamiento.getConvocados()) {
            RegistroEntrenamiento registro = entrenamiento.getRegistros().get(socio);
            entrada.mostrarMensaje(socio.getEmail() + " - " + (registro == null ? "Sin registro"
                    : registro.getEstado() + " - Nota: " + (registro.getCalificacion() == null
                    ? "Sin calificar" : registro.getCalificacion()) + " - " + registro.getComentario()));
        }
    }

    // Ofrece el registro de estadisticas y resultados de eventos propios
    private void gestionarPartidos() {
        obtenerEntrenador();
        entrada.mostrarMensaje("1. Registrar estadísticas\n2. Consultar estadísticas");
        entrada.mostrarMensaje("3. Registrar resultado de competencia\n4. Cumplir partido de suspensión\n0. Volver");
        int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 4);
        if (opcion == 0) return;
        Partido partido = seleccionarPartido(opcion == 3);
        if (partido == null) return;
        if (opcion == 1) {
            Socio socio = entrada.seleccionar("Jugadores del evento", partido.getParticipantes(), Usuario::getEmail);
            if (socio == null) return;
            EstadisticaJugador estadistica = leerEstadistica(partido.getDeporte(), socio);
            if (!entrada.confirmar("¿Registrar las estadísticas del jugador?")) return;
            sesion.registrarEstadistica(partido, estadistica);
            entrada.mostrarMensaje("Estadísticas registradas.");
        } else if (opcion == 2) {
            List<EstadisticaJugador> estadisticas = sesion.consultarEstadisticas(partido);
            if (estadisticas.isEmpty()) entrada.mostrarMensaje("No hay estadísticas registradas.");
            for (EstadisticaJugador estadistica : estadisticas) {
                entrada.mostrarMensaje(estadistica.getSocio().getEmail() + " - " + describirEstadistica(estadistica));
            }
        } else if (opcion == 3) {
            int local = leerCantidad("Marcador local: ");
            int visitante = leerCantidad("Marcador visitante: ");
            if (!entrada.confirmar("¿Registrar el resultado?")) return;
            sesion.registrarResultado((PartidoCompetencia) partido, local, visitante);
            entrada.mostrarMensaje("Resultado registrado.");
        } else {
            List<Socio> sancionados = new ArrayList<>();
            for (Socio socio : partido.getParticipantes()) {
                if (socio.getSuspensionRestante() > 0) sancionados.add(socio);
            }
            Socio socio = entrada.seleccionar("Jugadores con suspensión", sancionados, Usuario::getEmail);
            if (socio == null || !entrada.confirmar("¿Registrar un partido de suspensión cumplido?")) return;
            sesion.cumplirPartidoSuspension(socio);
            entrada.mostrarMensaje("Suspensión actualizada.");
        }
    }

    // Selecciona un evento administrado por el entrenador autenticado
    private Partido seleccionarPartido(boolean soloCompetencia) {
        List<Partido> propios = new ArrayList<>();
        List<Deporte> deportes = new ArrayList<>();
        for (Partido partido : Partido.getPartidos()) {
            if (partido.getEntrenador() == obtenerEntrenador()
                    && (!soloCompetencia || partido instanceof PartidoCompetencia)) {
                propios.add(partido);
                if (!deportes.contains(partido.getDeporte())) deportes.add(partido.getDeporte());
            }
        }
        Deporte deporte = entrada.seleccionar("Deportes con eventos propios", deportes, Deporte::getNombre);
        if (deporte == null) return null;
        List<Partido> eventos = new ArrayList<>();
        for (Partido partido : propios) {
            if (partido.getDeporte() == deporte) eventos.add(partido);
        }
        return entrada.seleccionar("Mis eventos", eventos, this::describirPartido);
    }

    // Describe los equipos y la fecha de un evento
    private String describirPartido(Partido partido) {
        if (partido instanceof PartidoCompetencia) {
            PartidoCompetencia p = (PartidoCompetencia) partido;
            return p.getEquipoLocal().getNombre() + " contra " + p.getEquipoVisitante().getNombre() + " - " + p.getFecha();
        }
        if (partido instanceof PartidoInterno) {
            PartidoInterno p = (PartidoInterno) partido;
            return p.getEquipoLocal().getNombre() + " contra " + p.getEquipoVisitante().getNombre() + " - " + p.getFecha();
        }
        return partido.getDeporte().getNombre() + " - Participantes: " + partido.getParticipantes().size();
    }

    // Lee las medidas correspondientes al deporte del evento
    private EstadisticaJugador leerEstadistica(Deporte deporte, Socio socio) {
        if (deporte instanceof Futbol) {
            return new EstadisticaFutbol(socio, leerCantidad("Goles: "), leerCantidad("Asistencias: "),
                    leerCantidad("Tarjetas amarillas: "), entrada.confirmar("¿Recibió tarjeta roja?"));
        }
        if (deporte instanceof Baloncesto) {
            return new EstadisticaBaloncesto(socio, leerCantidad("Puntos: "), leerCantidad("Rebotes: "));
        }
        if (deporte instanceof Voleibol) {
            return new EstadisticaVoleibol(socio, leerCantidad("Aces: "), leerCantidad("Bloqueos: "));
        }
        if (deporte instanceof Tenis || deporte instanceof Padel) {
            return new EstadisticaTenis(socio, leerCantidad("Sets ganados: "), leerCantidad("Games ganados: "),
                    leerCantidad("Aces: "), leerCantidad("Errores no forzados: "));
        }
        if (deporte instanceof TenisDeMesa) {
            return new EstadisticaTenisDeMesa(socio, leerCantidad("Sets ganados: "), leerCantidad("Puntos totales: "));
        }
        throw new IllegalArgumentException("El deporte no admite registros de estadísticas");
    }

    // Lee una cantidad entera no negativa
    private int leerCantidad(String mensaje) {
        return entrada.leerEntero(mensaje, 0, Integer.MAX_VALUE);
    }

    // Describe las medidas de un registro deportivo
    private String describirEstadistica(EstadisticaJugador estadistica) {
        if (estadistica instanceof EstadisticaFutbol) {
            EstadisticaFutbol e = (EstadisticaFutbol) estadistica;
            return "Goles: " + e.getGoles() + " - Asistencias: " + e.getAsistencias()
                    + " - Amarillas: " + e.getAmarillas() + " - Roja: " + (e.isRoja() ? "Sí" : "No");
        }
        if (estadistica instanceof EstadisticaBaloncesto) {
            EstadisticaBaloncesto e = (EstadisticaBaloncesto) estadistica;
            return "Puntos: " + e.getPuntos() + " - Rebotes: " + e.getRebotes();
        }
        if (estadistica instanceof EstadisticaVoleibol) {
            EstadisticaVoleibol e = (EstadisticaVoleibol) estadistica;
            return "Aces: " + e.getAces() + " - Bloqueos: " + e.getBloqueos();
        }
        if (estadistica instanceof EstadisticaTenis) {
            EstadisticaTenis e = (EstadisticaTenis) estadistica;
            return "Sets: " + e.getSetsGanados() + " - Games: " + e.getGamesGanados()
                    + " - Aces: " + e.getAces() + " - Errores no forzados: " + e.getErroresNoForzados();
        }
        if (estadistica instanceof EstadisticaTenisDeMesa) {
            EstadisticaTenisDeMesa e = (EstadisticaTenisDeMesa) estadistica;
            return "Sets: " + e.getSetsGanados() + " - Puntos: " + e.getPuntosTotales();
        }
        return "Sin detalle disponible";
    }

    // Consulta el minimo de jugadores habilitados de un equipo
    private void consultarDisponibilidad() {
        obtenerEntrenador();
        Equipo equipo = seleccionarEquipo(null);
        if (equipo == null) return;
        entrada.mostrarMensaje("Jugadores habilitados: " + equipo.getJugadoresHabilitados().size());
        entrada.mostrarMensaje("Puede disputar partido: " + (sesion.puedeDisputarPartido(equipo) ? "Sí" : "No"));
        List<Socio> noDisponibles = sesion.consultarJugadoresNoDisponibles(equipo);
        if (noDisponibles.isEmpty()) entrada.mostrarMensaje("No hay jugadores suspendidos.");
        for (Socio socio : noDisponibles) {
            entrada.mostrarMensaje(socio.getEmail() + " - Partidos de suspensión pendientes: " + socio.getSuspensionRestante());
        }
    }

    // Comprueba que la sesion pertenezca a un empleado activo
    private Empleado obtenerEmpleado() {
        Usuario usuario = sesion.getUsuarioActual();
        if (!(usuario instanceof Empleado) || !usuario.isActivo()) {
            throw new IllegalStateException("Se requiere un empleado con sesión activa");
        }
        return (Empleado) usuario;
    }

    // Comprueba el permiso para las funciones exclusivas de entrenador
    private Entrenador obtenerEntrenador() {
        Empleado empleado = obtenerEmpleado();
        if (!(empleado instanceof Entrenador)) throw new IllegalStateException("Esta opción requiere un entrenador");
        return (Entrenador) empleado;
    }

    // Inicia la aplicacion del empleado y guarda al terminar
    public static void main(String[] args) {
        EntradaConsola entrada = new EntradaConsola(new Scanner(System.in), System.out);
        try (ClubDeportivoSeneca club = new ClubDeportivoSeneca()) {
            ConsolaInicioSesion inicio = new ConsolaInicioSesion(entrada, club.getAutenticador());
            GestorTienda gestor = club.getGestorTienda();
            List<CodigoDescuento> codigos = gestor.getCodigos();
            boolean continuar = true;
            while (continuar) {
                SesionActual sesion = inicio.iniciarSesion(Empleado.class);
                if (sesion == null) break;
                continuar = new ConsolaEmpleado(entrada, sesion, gestor, codigos).ejecutar();
            }
        } catch (NoSuchElementException e) {
            entrada.mostrarMensaje("Entrada finalizada.");
        } catch (PersistenciaException e) {
            entrada.mostrarMensaje("No fue posible completar la persistencia: " + e.getMessage());
            return;
        }
        entrada.mostrarMensaje("Información guardada. Programa finalizado.");
    }
}
