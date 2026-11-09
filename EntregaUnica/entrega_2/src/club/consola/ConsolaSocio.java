package club.consola;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import club.ClubDeportivoSeneca;
import club.Inscripciones.InscripcionAcademia;
import club.Inscripciones.InscripcionEquipo;
import club.acceso.SesionActual;
import club.deportes.Deporte;
import club.deportes.DeporteConjunto;
import club.deportes.DeporteIndividual;
import club.deportes.Equipo;
import club.deportes.Modalidad;
import club.entrenamientos.RegistroEntrenamiento;
import club.entrenamientos.SesionEntrenamiento;
import club.instalaciones.Instalacion;
import club.instalaciones.Reserva;
import club.partidos.EstadisticaBaloncesto;
import club.partidos.EstadisticaFutbol;
import club.partidos.EstadisticaJugador;
import club.partidos.EstadisticaTenis;
import club.partidos.EstadisticaTenisDeMesa;
import club.partidos.EstadisticaVoleibol;
import club.partidos.PartidoPractica;
import club.personal.Empleado;
import club.personal.Entrenador;
import club.persistencia.PersistenciaException;
import club.tienda.Articulo;
import club.tienda.BonoDescuento;
import club.tienda.CodigoDescuento;
import club.tienda.GestorTienda;
import club.tienda.Producto;
import club.tienda.ProductoCafeteria;
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

public class ConsolaSocio {

    private final EntradaConsola entrada;
    private final SesionActual sesion;
    private final GestorTienda gestor;
    private final List<CodigoDescuento> codigos;
    private final ConsolaCompras compras;

    // Recibe la sesion y los servicios usados por el socio
    public ConsolaSocio(EntradaConsola entrada, SesionActual sesion,
            GestorTienda gestor, List<CodigoDescuento> codigos) {
        if (entrada == null || sesion == null || gestor == null || codigos == null) {
            throw new IllegalArgumentException("La entrada, la sesión, el gestor y los códigos son obligatorios");
        }
        this.entrada = entrada;
        this.sesion = sesion;
        this.gestor = gestor;
        this.codigos = new ArrayList<>(codigos);
        this.compras = new ConsolaCompras(entrada, sesion, gestor);
        obtenerSocio();
    }

    // Devuelve true al cerrar sesion y false al salir
    public boolean ejecutar() {
        while (true) {
            obtenerSocio();
            mostrarMenu();
            int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 9);
            if (opcion == 0 || opcion == 9) {
                sesion.cerrarSesion();
                return opcion == 9;
            }
            try {
                switch (opcion) {
                    case 1: mostrarPerfil(); break;
                    case 2: gestionarEquipos(); break;
                    case 3: gestionarDeportesIndividuales(); break;
                    case 4: gestionarReservasYPractica(); break;
                    case 5: gestionarTorneos(); break;
                    case 6: realizarCompra(); break;
                    case 7: consultarActividades(); break;
                    case 8: consultarPuntosYBonos(); break;
                    default: break;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                entrada.mostrarMensaje("No fue posible realizar la operación: " + e.getMessage());
            }
        }
    }

    // Muestra las opciones principales del socio
    private void mostrarMenu() {
        entrada.mostrarMensaje("Club Deportivo Séneca - Socio: " + obtenerSocio().getEmail());
        entrada.mostrarMensaje("1. Mi perfil\n2. Mis equipos\n3. Deportes individuales");
        entrada.mostrarMensaje("4. Reservas y práctica\n5. Torneos\n6. Tienda y cafetería");
        entrada.mostrarMensaje("7. Mis actividades y estadísticas\n8. Puntos y bonos");
        entrada.mostrarMensaje("9. Cerrar sesión\n0. Guardar y salir");
    }

    // Consulta el perfil y permite registrar la fecha de nacimiento
    private void mostrarPerfil() {
        Socio socio = obtenerSocio();
        entrada.mostrarMensaje("Correo: " + socio.getEmail());
        entrada.mostrarMensaje("Fecha de nacimiento: " + (socio.getFechaNacimiento() == null
                ? "Sin registrar" : socio.getFechaNacimiento()));
        entrada.mostrarMensaje("Partidos de suspensión pendientes: " + socio.getSuspensionRestante());
        if (socio.getFechaNacimiento() == null && entrada.confirmar("¿Registrar su fecha de nacimiento?")) {
            registrarFechaNacimiento();
        }
    }

    // Registra una fecha de nacimiento confirmada por el socio
    private void registrarFechaNacimiento() {
        LocalDate fecha = entrada.leerFecha("Fecha de nacimiento");
        if (fecha != null && entrada.confirmar("¿Registrar " + fecha + "?")) {
            sesion.registrarFechaNacimiento(fecha);
            entrada.mostrarMensaje("Fecha de nacimiento registrada.");
        }
    }

    // Solicita la fecha de nacimiento cuando una inscripcion la necesita
    private boolean asegurarFechaNacimiento() {
        if (obtenerSocio().getFechaNacimiento() == null) {
            entrada.mostrarMensaje("Debe registrar su fecha de nacimiento antes de inscribirse.");
            registrarFechaNacimiento();
        }
        return obtenerSocio().getFechaNacimiento() != null;
    }

    // Consulta equipos y ofrece las operaciones de inscripcion
    private void gestionarEquipos() {
        List<InscripcionEquipo> inscripciones = obtenerSocio().getInscripciones();
        if (inscripciones.isEmpty()) entrada.mostrarMensaje("No pertenece a ningún equipo.");
        for (InscripcionEquipo inscripcion : inscripciones) {
            entrada.mostrarMensaje(inscripcion.getEquipo().getNombre() + " - Camiseta "
                    + inscripcion.getNumeroCamiseta() + " - " + inscripcion.getPosicion());
        }
        entrada.mostrarMensaje("1. Inscribirme\n2. Cambiar camiseta\n3. Cambiar posición\n0. Volver");
        int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 3);
        if (opcion == 1) inscribirseEnEquipo();
        else if (opcion == 2 || opcion == 3) actualizarDatosEnEquipo(opcion);
    }

    // Inscribe al socio en un equipo compatible con su edad
    private void inscribirseEnEquipo() {
        if (!asegurarFechaNacimiento()) return;
        List<DeporteConjunto> deportes = new ArrayList<>();
        for (Deporte deporte : Deporte.getDeportesRegistrados().values()) {
            if (deporte instanceof DeporteConjunto) deportes.add((DeporteConjunto) deporte);
        }
        DeporteConjunto deporte = entrada.seleccionar("Deportes de conjunto", deportes, Deporte::getNombre);
        if (deporte == null) return;
        List<Equipo> equipos = sesion.consultarEquiposDisponibles(deporte);
        equipos.sort(Comparator.comparing(Equipo::getNombre));
        Equipo equipo = entrada.seleccionar("Equipos disponibles", equipos,
                e -> e.getNombre() + " - " + e.getCategoria() + " - Cupos: " + e.getCuposDisponibles());
        if (equipo == null) return;
        Enum<?> posicion = entrada.seleccionar("Posiciones", deporte.getPosiciones(), Enum::name);
        if (posicion == null) return;
        int numero = entrada.leerEntero("Número de camiseta: ", 0, Integer.MAX_VALUE);
        if (entrada.confirmar("¿Inscribirse en " + equipo.getNombre() + "?")) {
            sesion.inscribirEnEquipo(equipo, posicion, numero);
            entrada.mostrarMensaje("Inscripción registrada.");
        }
    }

    // Actualiza la camiseta o posicion en uno de los equipos propios
    private void actualizarDatosEnEquipo(int opcion) {
        InscripcionEquipo inscripcion = entrada.seleccionar("Mis equipos", obtenerSocio().getInscripciones(),
                i -> i.getEquipo().getNombre());
        if (inscripcion == null) return;
        if (opcion == 2) {
            int numero = entrada.leerEntero("Nuevo número de camiseta: ", 0, Integer.MAX_VALUE);
            if (!entrada.confirmar("¿Cambiar la camiseta?")) return;
            sesion.actualizarNumeroCamiseta(inscripcion, numero);
        } else {
            DeporteConjunto deporte = (DeporteConjunto) inscripcion.getEquipo().getDeporte();
            Enum<?> posicion = entrada.seleccionar("Posiciones", deporte.getPosiciones(), Enum::name);
            if (posicion == null || !entrada.confirmar("¿Cambiar la posición?")) return;
            sesion.actualizarPosicion(inscripcion, posicion);
        }
        entrada.mostrarMensaje("Datos del equipo actualizados.");
    }

    // Consulta academias y permite inscribirse en una disciplina
    private void gestionarDeportesIndividuales() {
        boolean inscrito = false;
        for (InscripcionAcademia inscripcion : InscripcionAcademia.getInscripciones()) {
            if (inscripcion.getSocio() == obtenerSocio()) {
                entrada.mostrarMensaje(inscripcion.getDeporte().getNombre() + " - " + inscripcion.getNivel()
                        + " - ELO: " + inscripcion.getElo());
                inscrito = true;
            }
        }
        if (!inscrito) entrada.mostrarMensaje("No está inscrito en deportes individuales.");
        entrada.mostrarMensaje("1. Inscribirme\n2. Consultar ranking\n0. Volver");
        int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 2);
        if (opcion == 0) return;
        DeporteIndividual deporte = entrada.seleccionar("Deportes individuales",
                sesion.consultarDeportesIndividualesDisponibles(), Deporte::getNombre);
        if (deporte == null) return;
        if (opcion == 1) {
            if (!entrada.confirmar("¿Inscribirse en " + deporte.getNombre() + "?")) return;
            InscripcionAcademia inscripcion = sesion.inscribirEnDeporteIndividual(deporte);
            entrada.mostrarMensaje("Inscripción registrada. Nivel: " + inscripcion.getNivel());
        } else {
            List<Socio> ranking = deporte.getRanking(RepositorioUsuarios.getRepositorioUsuarios().size());
            if (ranking.isEmpty()) entrada.mostrarMensaje("No hay jugadores en el ranking.");
            for (int i = 0; i < ranking.size(); i++) {
                Socio jugador = ranking.get(i);
                entrada.mostrarMensaje((i + 1) + ". " + jugador.getEmail() + " - ELO: "
                        + InscripcionAcademia.buscar(jugador, deporte).getElo());
            }
        }
    }

    // Ofrece la reserva de instalaciones y los partidos de practica
    private void gestionarReservasYPractica() {
        entrada.mostrarMensaje("1. Reservar instalación\n2. Programar partido de práctica\n0. Volver");
        int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 2);
        if (opcion == 1) reservarInstalacion();
        else if (opcion == 2) programarPractica();
    }

    // Registra una reserva con el socio como responsable
    private void reservarInstalacion() {
        Instalacion instalacion = seleccionarInstalacion();
        if (instalacion == null) return;
        Modalidad modalidad = entrada.seleccionar("Modalidades", instalacion.getModalidades(), Enum::name);
        if (modalidad == null) return;
        LocalDateTime fecha = entrada.leerFechaHora("Inicio de la reserva");
        if (fecha == null) return;
        int minutos = entrada.leerEntero("Duración en minutos (0 para cancelar): ", 0, Integer.MAX_VALUE);
        if (minutos == 0) return;
        List<Socio> participantes = seleccionarSocios(modalidad == Modalidad.SENCILLOS ? 2 : 4);
        if (participantes == null || !entrada.confirmar("¿Confirmar la reserva?")) return;
        sesion.reservarInstalacion(participantes, instalacion, fecha, Duration.ofMinutes(minutos), modalidad);
        entrada.mostrarMensaje("Reserva registrada.");
    }

    // Selecciona una instalacion registrada en el club
    private Instalacion seleccionarInstalacion() {
        List<Instalacion> instalaciones = new ArrayList<>(Instalacion.getInstalaciones().values());
        instalaciones.sort(Comparator.comparing(Instalacion::getNombre));
        return entrada.seleccionar("Instalaciones", instalaciones, Instalacion::getNombre);
    }

    // Selecciona socios activos distintos e incluye al responsable
    private List<Socio> seleccionarSocios(int cantidad) {
        List<Socio> participantes = new ArrayList<>();
        participantes.add(obtenerSocio());
        while (participantes.size() < cantidad) {
            List<Socio> disponibles = new ArrayList<>();
            for (Usuario usuario : RepositorioUsuarios.getRepositorioUsuarios().values()) {
                if (usuario instanceof Socio && usuario.isActivo() && !participantes.contains(usuario)) {
                    disponibles.add((Socio) usuario);
                }
            }
            disponibles.sort(Comparator.comparing(Usuario::getEmail));
            Socio socio = entrada.seleccionar("Participante " + (participantes.size() + 1)
                    + " de " + cantidad, disponibles, Usuario::getEmail);
            if (socio == null) return null;
            participantes.add(socio);
        }
        return participantes;
    }

    // Programa una practica con jugadores inscritos o entrenadores
    private void programarPractica() {
        DeporteIndividual deporte = entrada.seleccionar("Deportes individuales",
                sesion.consultarDeportesIndividualesDisponibles(), Deporte::getNombre);
        if (deporte == null) return;
        Modalidad modalidad = entrada.seleccionar("Modalidades", deporte.getModalidades(), Enum::name);
        if (modalidad == null) return;
        List<Usuario> participantes = new ArrayList<>();
        participantes.add(obtenerSocio());
        int cantidad = modalidad == Modalidad.SENCILLOS ? 2 : 4;
        while (participantes.size() < cantidad) {
            List<Usuario> disponibles = new ArrayList<>();
            for (Usuario usuario : RepositorioUsuarios.getRepositorioUsuarios().values()) {
                if (usuario.isActivo() && !participantes.contains(usuario)
                        && (usuario instanceof Entrenador || usuario instanceof Socio
                        && InscripcionAcademia.estaInscrito((Socio) usuario, deporte))) {
                    disponibles.add(usuario);
                }
            }
            disponibles.sort(Comparator.comparing(Usuario::getEmail));
            Usuario jugador = entrada.seleccionar("Jugador " + (participantes.size() + 1)
                    + " de " + cantidad, disponibles, Usuario::getEmail);
            if (jugador == null) return;
            participantes.add(jugador);
        }
        if (entrada.confirmar("¿Registrar el partido de práctica?")) {
            sesion.programarPartidoPractica(deporte, modalidad, participantes);
            entrada.mostrarMensaje("Partido de práctica registrado.");
        }
    }

    // Permite consultar torneos e inscribirse o retirarse
    private void gestionarTorneos() {
        List<TorneoInterno> torneos = CatalogoTorneos.getTorneos();
        if (torneos.isEmpty()) {
            entrada.mostrarMensaje("No hay torneos registrados.");
            return;
        }
        for (TorneoInterno torneo : torneos) entrada.mostrarMensaje(describirTorneo(torneo));
        entrada.mostrarMensaje("1. Consultar historial\n2. Inscribirme\n3. Retirarme\n0. Volver");
        int opcion = entrada.leerEntero("Seleccione una opción: ", 0, 3);
        if (opcion == 0) return;
        List<TorneoInterno> disponibles = new ArrayList<>();
        for (TorneoInterno torneo : torneos) {
            if (opcion == 1 || torneo.getEstado() == EstadoTorneo.PENDIENTE
                    && (opcion == 2 && torneo.getModalidad() != ModalidadTorneo.EQUIPOS
                    || opcion == 3 && torneo.estaInscrito(obtenerSocio()))) {
                disponibles.add(torneo);
            }
        }
        TorneoInterno torneo = entrada.seleccionar("Torneos", disponibles, this::describirTorneo);
        if (torneo == null) return;
        if (opcion == 1) consultarHistorial(torneo);
        else if (opcion == 2) inscribirseEnTorneo(torneo);
        else if (entrada.confirmar("¿Retirar su inscripción y la de su pareja si corresponde?")) {
            sesion.retirarseDeTorneo(torneo);
            entrada.mostrarMensaje("Inscripción retirada. Cupo liberado.");
        }
    }

    // Describe el estado y los cupos de un torneo
    private String describirTorneo(TorneoInterno torneo) {
        return torneo.getId() + ". " + torneo.getDeporte().getNombre() + " - " + torneo.getCategoria()
                + " - " + torneo.getModalidad() + " - " + torneo.getTipo() + " - " + torneo.getEstado()
                + " - Cupos: " + torneo.getCuposDisponibles() + " - Preferenciales: "
                + torneo.getCuposPreferenciales() + " - Tarifa: " + torneo.getTarifa();
    }

    // Inscribe al socio solo o con una pareja elegida
    private void inscribirseEnTorneo(TorneoInterno torneo) {
        Usuario pareja = null;
        if (torneo.getModalidad() == ModalidadTorneo.DOBLES) {
            List<Usuario> disponibles = new ArrayList<>();
            for (Usuario usuario : RepositorioUsuarios.getRepositorioUsuarios().values()) {
                if (usuario != obtenerSocio() && usuario.isActivo()
                        && (usuario instanceof Socio || usuario instanceof Empleado)) disponibles.add(usuario);
            }
            disponibles.sort(Comparator.comparing(Usuario::getEmail));
            pareja = entrada.seleccionar("Pareja del torneo", disponibles, Usuario::getEmail);
            if (pareja == null) return;
        }
        if (!entrada.confirmar("¿Confirmar la inscripción en el torneo?")) return;
        if (pareja == null) sesion.inscribirEnTorneo(torneo);
        else sesion.inscribirParejaEnTorneo(torneo, pareja);
        InscripcionTorneo inscripcion = torneo.getInscripcion(obtenerSocio());
        entrada.mostrarMensaje("Inscripción registrada. Tarifa: " + inscripcion.getTarifaPagada()
                + " - Cupo preferencial: " + (inscripcion.esPreferencial() ? "Sí" : "No"));
    }

    // Muestra participantes encuentros y posiciones sin importar el estado
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

    // Muestra nombres legibles para jugadores parejas y equipos
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
                instalacion = seleccionarInstalacion();
                if (instalacion == null) return;
            }
            compras.comprarEnCafeteria(productos, instalacion);
        }
    }

    // Consulta las reservas practicas y entrenamientos del socio
    private void consultarActividades() {
        Socio socio = obtenerSocio();
        entrada.mostrarMensaje("Mis reservas:");
        for (Reserva reserva : Reserva.getReservas()) {
            if (reserva.getParticipantes().contains(socio)) {
                entrada.mostrarMensaje(reserva.getInstalacion().getNombre() + " - " + reserva.getFechaHora()
                        + " - " + reserva.getDuracion().toMinutes() + " minutos - " + reserva.getModalidad());
            }
        }
        entrada.mostrarMensaje("Mis partidos de práctica:");
        for (PartidoPractica partido : PartidoPractica.getPartidos()) {
            if (partido.getParticipantes().contains(socio)) {
                entrada.mostrarMensaje(partido.getDeporte().getNombre() + " - " + partido.getModalidad()
                        + " - " + (partido.isFinalizado() ? "Finalizado" : "Pendiente"));
            }
        }
        entrada.mostrarMensaje("Mis entrenamientos:");
        for (SesionEntrenamiento entrenamiento : SesionEntrenamiento.getSesiones()) {
            if (entrenamiento.getConvocados().contains(socio)) {
                entrada.mostrarMensaje(entrenamiento.getDeporte().getNombre() + " - " + entrenamiento.getFechaHora()
                        + " - " + entrenamiento.getInstalacion().getNombre());
                RegistroEntrenamiento registro = entrenamiento.getRegistros().get(socio);
                if (registro != null) entrada.mostrarMensaje("Asistencia: " + registro.getEstado()
                        + " - Calificación: " + registro.getCalificacion() + " - " + registro.getComentario());
            }
        }
        entrada.mostrarMensaje("Mis estadísticas:");
        for (EstadisticaJugador estadistica : sesion.consultarEstadisticas(socio)) {
            entrada.mostrarMensaje(describirEstadistica(estadistica));
        }
    }

    // Describe las estadisticas segun el deporte del registro
    private String describirEstadistica(EstadisticaJugador estadistica) {
        if (estadistica instanceof EstadisticaFutbol) {
            EstadisticaFutbol e = (EstadisticaFutbol) estadistica;
            return "Fútbol - Goles: " + e.getGoles() + " - Asistencias: " + e.getAsistencias()
                    + " - Amarillas: " + e.getAmarillas() + " - Roja: " + (e.isRoja() ? "Sí" : "No");
        }
        if (estadistica instanceof EstadisticaBaloncesto) {
            EstadisticaBaloncesto e = (EstadisticaBaloncesto) estadistica;
            return "Baloncesto - Puntos: " + e.getPuntos() + " - Rebotes: " + e.getRebotes();
        }
        if (estadistica instanceof EstadisticaVoleibol) {
            EstadisticaVoleibol e = (EstadisticaVoleibol) estadistica;
            return "Voleibol - Aces: " + e.getAces() + " - Bloqueos: " + e.getBloqueos();
        }
        if (estadistica instanceof EstadisticaTenis) {
            EstadisticaTenis e = (EstadisticaTenis) estadistica;
            return "Tenis - Sets: " + e.getSetsGanados() + " - Games: " + e.getGamesGanados()
                    + " - Aces: " + e.getAces() + " - Errores no forzados: " + e.getErroresNoForzados();
        }
        if (estadistica instanceof EstadisticaTenisDeMesa) {
            EstadisticaTenisDeMesa e = (EstadisticaTenisDeMesa) estadistica;
            return "Tenis de mesa - Sets: " + e.getSetsGanados() + " - Puntos: " + e.getPuntosTotales();
        }
        return "Registro de estadísticas sin detalle disponible.";
    }

    // Consulta los puntos y bonos disponibles del socio
    private void consultarPuntosYBonos() {
        Socio socio = obtenerSocio();
        entrada.mostrarMensaje("Puntos disponibles: " + socio.getSaldoFidelidad().getPuntos());
        List<BonoDescuento> bonos = socio.getBonosDisponibles();
        if (bonos.isEmpty()) entrada.mostrarMensaje("No tiene bonos disponibles.");
        for (BonoDescuento bono : bonos) {
            entrada.mostrarMensaje(bono.getId() + " - Valor: " + bono.getValor()
                    + " - Obtenido: " + bono.getFechaObtencion());
        }
    }

    // Comprueba que la sesion pertenezca a un socio activo
    private Socio obtenerSocio() {
        Usuario usuario = sesion.getUsuarioActual();
        if (!(usuario instanceof Socio) || !usuario.isActivo()) {
            throw new IllegalStateException("Se requiere un socio con sesión activa");
        }
        return (Socio) usuario;
    }

    // Inicia la aplicacion del socio y guarda al terminar
    public static void main(String[] args) {
        EntradaConsola entrada = new EntradaConsola(new Scanner(System.in), System.out);
        try (ClubDeportivoSeneca club = new ClubDeportivoSeneca()) {
            ConsolaInicioSesion inicio = new ConsolaInicioSesion(entrada, club.getAutenticador());
            GestorTienda gestor = new GestorTienda();
            List<CodigoDescuento> codigos = new ArrayList<>();
            boolean continuar = true;
            while (continuar) {
                SesionActual sesion = inicio.iniciarSesion(Socio.class);
                if (sesion == null) break;
                continuar = new ConsolaSocio(entrada, sesion, gestor, codigos).ejecutar();
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
