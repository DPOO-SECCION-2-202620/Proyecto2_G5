
package club.tienda;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import club.usuarios.Administrador;
import club.personal.Empleado;
import club.usuarios.Socio;
import club.usuarios.Usuario;

public class GestorTienda {

    // Atributos
    private List<Producto> productos;
    private List<Inventario> inventarios;
    private List<UbicacionInventario> ubicaciones;
    private List<Venta> ventas;
    private List<SugerenciaProducto> sugerencias;
    private List<CodigoDescuento> codigos;

    // Constructor
    public GestorTienda() {
        this.productos = new ArrayList<>();
        this.inventarios = new ArrayList<>();
        this.ubicaciones = new ArrayList<>();
        this.ventas = new ArrayList<>();
        this.sugerencias = new ArrayList<>();
        this.codigos = new ArrayList<>();
    }

    public void limpiar() {
        productos.clear();
        inventarios.clear();
        ubicaciones.clear();
        ventas.clear();
        sugerencias.clear();
        codigos.clear();
    }

    public List<CodigoDescuento> getCodigos() {
        return new ArrayList<>(codigos);
    }

    public void registrarCodigo(CodigoDescuento codigo) {
        if (codigo == null) throw new IllegalArgumentException("El código es obligatorio");
        for (CodigoDescuento existente : codigos) {
            if (existente.getCodigo().equalsIgnoreCase(codigo.getCodigo())) {
                throw new IllegalArgumentException("El código ya está registrado");
            }
        }
        codigos.add(codigo);
    }

    // -----------------------------
    // PRODUCTOS
    // -----------------------------

    public Producto buscarProducto(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                "Nombre de producto invalido");
        }

        for (Producto producto : productos) {

            if (producto.getNombre().trim()
                    .equalsIgnoreCase(nombre.trim())) {
                return producto;
            }
        }

        return null;
    }

    // Devuelve una copia del catalogo para mostrarlo en consola
    public List<Producto> getProductos() {
        return new ArrayList<>(productos);
    }

    public void registrarProducto(Producto producto) {

        if (producto == null) {
            throw new IllegalArgumentException(
                "El producto no puede ser nulo");
        }

        if (buscarProducto(producto.getNombre()) != null) {
            throw new IllegalArgumentException(
                "Ya existe un producto con ese nombre");
        }

        productos.add(producto);
    }

    // -----------------------------
    // UBICACIONES
    // -----------------------------

    public UbicacionInventario buscarUbicacion(String id) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                "Identificador de ubicacion invalido");
        }

        for (UbicacionInventario ubicacion : ubicaciones) {

            if (ubicacion.getId().trim().equals(id.trim())) {
                return ubicacion;
            }
        }

        return null;
    }

    public void registrarUbicacion(
            UbicacionInventario ubicacion) {

        if (ubicacion == null) {
            throw new IllegalArgumentException(
                "La ubicacion no puede ser nula");
        }

        if (buscarUbicacion(ubicacion.getId()) != null) {
            throw new IllegalArgumentException(
                "Ya existe esa ubicacion");
        }

        ubicaciones.add(ubicacion);
    }

    public List<UbicacionInventario> getUbicaciones() {
        return new ArrayList<>(ubicaciones);
    }

    // -----------------------------
    // INVENTARIOS
    // -----------------------------

    public int consultarInventario(Articulo articulo) {

        if (articulo == null) {
            throw new IllegalArgumentException(
                "El articulo no puede ser nulo");
        }

        int totalDisponible = 0;

        for (Inventario inventario : inventarios) {

            if (inventario.getArticulo() == articulo) {
                totalDisponible += inventario.consultarCantidad();
            }
        }

        return totalDisponible;
    }

    public List<Inventario> consultarInventarios() {
        return new ArrayList<>(inventarios);
    }

    public void registrarInventario(Inventario inventario) {
        if (inventario == null || !productos.contains(inventario.getArticulo())
                || !ubicaciones.contains(inventario.getUbicacion())) {
            throw new IllegalArgumentException("Artículo o ubicación no registrados");
        }
        for (Inventario existente : inventarios) {
            if (existente.getArticulo() == inventario.getArticulo()
                    && existente.getUbicacion() == inventario.getUbicacion()) {
                throw new IllegalArgumentException("El inventario ya está registrado");
            }
        }
        inventarios.add(inventario);
    }

    public void reabastecer(
            Inventario inventario, int cantidad) {

        if (inventario == null || cantidad <= 0) {
            throw new IllegalArgumentException(
                "Inventario o cantidad invalidos");
        }

        if (!productos.contains(inventario.getArticulo())
                || !ubicaciones.contains(
                        inventario.getUbicacion())) {

            throw new IllegalArgumentException(
                "Articulo o ubicacion no registrados");
        }

        // Si es un registro nuevo, incorporarlo al gestor.
        if (!inventarios.contains(inventario)) {

            for (Inventario existente : inventarios) {

                if (existente.getArticulo()
                        == inventario.getArticulo()
                        && existente.getUbicacion()
                        == inventario.getUbicacion()) {

                    throw new IllegalArgumentException(
                        "Ya existe otro inventario para ese articulo y ubicacion");
                }
            }

            inventario.reabastecer(cantidad);
            inventarios.add(inventario);

        } else {
            inventario.reabastecer(cantidad);
        }
    }

    public void trasladar(
            Inventario origen,
            Inventario destino,
            int cantidad) {

        if (origen == null || destino == null
                || cantidad <= 0) {

            throw new IllegalArgumentException(
                "Datos del traslado invalidos");
        }

        if (origen == destino
                || !inventarios.contains(origen)
                || !inventarios.contains(destino)) {

            throw new IllegalArgumentException(
                "Inventarios de traslado no validos");
        }

        if (origen.getArticulo() != destino.getArticulo()) {
            throw new IllegalArgumentException(
                "Los inventarios deben ser del mismo articulo");
        }

        if (origen.consultarCantidad() < cantidad) {
            throw new IllegalStateException(
                "Existencias insuficientes");
        }

        origen.trasladar(destino, cantidad);
    }

    // -----------------------------
    // VENTAS
    // -----------------------------

    public void registrarVenta(Venta venta) {

        if (venta == null
                || venta.estaConfirmada()
                || ventas.contains(venta)) {

            throw new IllegalArgumentException(
                "Venta nula, confirmada o ya registrada");
        }

        List<DetalleVenta> detalles = venta.getDetalleVentas();

        if (detalles.isEmpty()) {
            throw new IllegalStateException(
                "La venta no tiene productos");
        }

        // Preparar las cantidades de inventario
        // que se descontaran al confirmar la venta.
        Map<Inventario, Integer> cantidadesADescontar =
                new IdentityHashMap<>();

        for (DetalleVenta detalle : detalles) {

            if (detalle == null
                    || detalle.getProducto() == null
                    || detalle.getCantidad() <= 0) {

                throw new IllegalArgumentException(
                    "Detalle de venta invalido");
            }

            if (!productos.contains(detalle.getProducto())) {
                throw new IllegalArgumentException(
                    "Producto no registrado en el catalogo");
            }

            // Los inventarios registrados corresponden
            // a articulos de tienda.
            if (venta instanceof VentaTienda) {

                if (!(detalle.getProducto() instanceof Articulo)) {
                    throw new IllegalArgumentException(
                        "La tienda solo vende articulos");
                }

                Articulo articulo =
                        (Articulo) detalle.getProducto();

                int pendiente = detalle.getCantidad();

                for (Inventario inventario : inventarios) {

                    if (inventario.getArticulo() == articulo
                            && pendiente > 0) {

                        int reservado =
                                cantidadesADescontar.getOrDefault(
                                    inventario, 0);

                        int disponible =
                                inventario.consultarCantidad()
                                - reservado;

                        int unidades = Math.min(
                                pendiente,
                                Math.max(0, disponible));

                        if (unidades > 0) {

                            cantidadesADescontar.put(
                                inventario,
                                reservado + unidades);

                            pendiente -= unidades;
                        }
                    }
                }

                if (pendiente > 0) {
                    throw new IllegalStateException(
                        "Inventario insuficiente para "
                        + articulo.getNombre());
                }
            }
        }

        // Obtener el saldo de fidelidad del comprador.
        Usuario comprador = venta.getComprador();
        SaldoFidelidad saldo = null;

        if (comprador instanceof Socio) {

            saldo = ((Socio) comprador)
                    .getSaldoFidelidad();

        } else if (comprador instanceof Empleado) {

            saldo = ((Empleado) comprador)
                    .getSaldoFidelidad();
        }

        // Validar puntos solicitados.
        int puntosUsados = venta.getPuntosRedimidos();

        if (puntosUsados < 0
                || (puntosUsados > 0
                    && (saldo == null
                        || saldo.getPuntos() < puntosUsados))) {

            throw new IllegalStateException(
                "Puntos de fidelidad insuficientes");
        }

        // Venta.confirmar() llama internamente a
        // liquidar() y marca la venta como confirmada.
        venta.confirmar();

        // Descontar inventarios.
        for (Map.Entry<Inventario, Integer> salida
                : cantidadesADescontar.entrySet()) {

            salida.getKey().descontar(salida.getValue());
        }

        // Actualizar puntos de fidelidad.
        if (saldo != null) {

            if (puntosUsados > 0) {
                saldo.redimir(puntosUsados);
            }

            int nuevosPuntos = venta.asignarPuntos();

            if (nuevosPuntos > 0) {
                saldo.agregarPuntos(nuevosPuntos);
            }
        }

        // Registrar en el historial.
        ventas.add(venta);
    }

    public List<Venta> consultarVentas(
            LocalDate inicio, LocalDate fin) {

        if (inicio == null || fin == null
                || inicio.isAfter(fin)) {

            throw new IllegalArgumentException(
                "Rango de fechas invalido");
        }

        List<Venta> resultado = new ArrayList<>();

        for (Venta venta : ventas) {

            LocalDate fechaVenta =
                    venta.getFecha().toLocalDate();

            if (!fechaVenta.isBefore(inicio)
                    && !fechaVenta.isAfter(fin)) {

                resultado.add(venta);
            }
        }

        return resultado;
    }

	public List<Venta> getVentas() {
		return new ArrayList<Venta>(ventas);
	}

    public void registrarVentaHistorica(Venta venta) {
        if (venta == null || !venta.estaConfirmada() || ventas.contains(venta)) {
            throw new IllegalArgumentException("La venta histórica no es válida");
        }
        ventas.add(venta);
        if (!Venta.getVentasConfirmadas().contains(venta)) Venta.registrarHistorica(venta);
    }

    // -----------------------------
    // SUGERENCIAS
    // -----------------------------

    // Consulta las sugerencias registradas sin exponer la lista interna
    public List<SugerenciaProducto> getSugerencias() {
        return new ArrayList<>(sugerencias);
    }

    public void registrarSugerencia(
            SugerenciaProducto sugerencia) {

        if (sugerencia == null
                || sugerencias.contains(sugerencia)) {

            throw new IllegalArgumentException(
                "Sugerencia nula o duplicada");
        }

        sugerencias.add(sugerencia);
    }

    public void aprobarSugerencia(
            SugerenciaProducto sugerencia,
            Administrador admin) {

        if (admin == null || sugerencia == null
                || !sugerencias.contains(sugerencia)) {

            throw new IllegalArgumentException(
                "Administrador o sugerencia invalidos");
        }

        sugerencia.aprobar(admin);
    }

    public void rechazarSugerencia(
            SugerenciaProducto sugerencia,
            Administrador admin) {

        if (admin == null || sugerencia == null
                || !sugerencias.contains(sugerencia)) {

            throw new IllegalArgumentException(
                "Administrador o sugerencia invalidos");
        }

        sugerencia.noAprobar(admin);
    }
}
