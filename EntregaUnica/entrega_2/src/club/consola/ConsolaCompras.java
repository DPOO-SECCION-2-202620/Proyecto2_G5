package club.consola;

import java.util.List;
import java.util.Locale;

import club.acceso.SesionActual;
import club.instalaciones.Instalacion;
import club.personal.Empleado;
import club.tienda.Articulo;
import club.tienda.BonoDescuento;
import club.tienda.CodigoDescuento;
import club.tienda.DetalleVenta;
import club.tienda.GestorTienda;
import club.tienda.Producto;
import club.tienda.ProductoCafeteria;
import club.tienda.SaldoFidelidad;
import club.tienda.Venta;
import club.tienda.VentaCafeteria;
import club.tienda.VentaTienda;
import club.usuarios.Socio;
import club.usuarios.Usuario;

// Flujo de compra compartido por las consolas de socio y empleado. 
public class ConsolaCompras {

    private final EntradaConsola entrada;
    private final SesionActual sesion;
    private final GestorTienda gestor;

    public ConsolaCompras(EntradaConsola entrada, SesionActual sesion, GestorTienda gestor) {
        if (entrada == null || sesion == null || gestor == null) {
            throw new IllegalArgumentException("La entrada, la sesión y el gestor son obligatorios");
        }
        this.entrada = entrada;
        this.sesion = sesion;
        this.gestor = gestor;
    }

    // Recibe los articulos y codigos existentes, los productos deben estar registrados en el gestor
    public void comprarEnTienda(List<? extends Articulo> articulos, List<CodigoDescuento> codigos) {
        if (codigos == null) throw new IllegalArgumentException("La lista de códigos es obligatoria");
        realizarCompra(new VentaTienda(obtenerComprador()), articulos, codigos);
    }

    // La instalacion indica dónde se consume, null corresponde a una compra sin instalación deportiva
    public void comprarEnCafeteria(List<? extends ProductoCafeteria> productos, Instalacion instalacion) {
        realizarCompra(new VentaCafeteria(obtenerComprador(), instalacion), productos, List.of());
    }

    private void realizarCompra(Venta venta, List<? extends Producto> catalogo, List<CodigoDescuento> codigos) {
        if (catalogo == null) throw new IllegalArgumentException("El catálogo es obligatorio");
        if (catalogo.isEmpty()) {
            entrada.mostrarMensaje("No hay productos disponibles.");
            return;
        }
        try {
            agregarProductos(venta, catalogo);
            if (venta.getDetalleVentas().isEmpty()) {
                entrada.mostrarMensaje("Compra cancelada.");
                return;
            }
            if (venta instanceof VentaCafeteria) prepararCafeteria((VentaCafeteria) venta);
            seleccionarDescuento(venta, codigos);
            venta.liquidar();
            seleccionarPuntos(venta);
            venta.liquidar();
            mostrarResumen(venta);
            if (!entrada.confirmar("¿Confirmar la compra?")) {
                entrada.mostrarMensaje("Compra cancelada.");
                return;
            }
            sesion.registrarVenta(gestor, venta);
            entrada.mostrarMensaje("Compra registrada. Puntos ganados: " + venta.getPuntosGanados());
        } catch (IllegalArgumentException | IllegalStateException e) {
            entrada.mostrarMensaje("No fue posible completar la compra: " + e.getMessage());
        } finally {
            // También libera el bono si se agota la entrada o falla la validación del inventario.
            if (!venta.estaConfirmada() && venta.getBonoAplicado() != null) {
                venta.getBonoAplicado().cancelarReserva(venta);
            }
        }
    }

    private void agregarProductos(Venta venta, List<? extends Producto> catalogo) {
        while (true) {
            Producto producto = entrada.seleccionar("Productos (0 para terminar el carrito)", catalogo,
                    p -> p.getNombre() + " - " + dinero(p.getPrecio()));
            if (producto == null) return;
            int maximo = Integer.MAX_VALUE;
            if (producto instanceof Articulo) {
                maximo = gestor.consultarInventario((Articulo) producto);
                for (DetalleVenta detalle : venta.getDetalleVentas()) {
                    if (detalle.getProducto() == producto) maximo -= detalle.getCantidad();
                }
                if (maximo <= 0) {
                    entrada.mostrarMensaje("No quedan unidades disponibles de este artículo.");
                    continue;
                }
                entrada.mostrarMensaje("Unidades disponibles: " + maximo);
            }
            int cantidad = entrada.leerEntero("Cantidad (0 para volver): ", 0, maximo);
            if (cantidad > 0) {
                venta.agregarDetalle(new DetalleVenta(cantidad, producto));
                entrada.mostrarMensaje("Producto agregado al carrito.");
            }
        }
    }

    private void prepararCafeteria(VentaCafeteria venta) {
        List<String> alergenos = venta.informarAlergenos();
        entrada.mostrarMensaje(alergenos.isEmpty()
                ? "Los productos seleccionados no tienen alérgenos registrados."
                : "Alérgenos: " + String.join(", ", alergenos));
        double porcentaje = entrada.leerDecimal("Propina en porcentaje (sugerida 10; entre 0 y 100): ", 0, 100);
        venta.ajustarPropina(porcentaje / 100);
    }

    private void seleccionarDescuento(Venta venta, List<CodigoDescuento> codigos) {
        while (true) {
            entrada.mostrarMensaje("Descuento: 0. Ninguno | 1. Bono de torneo"
                    + (venta instanceof VentaTienda ? " | 2. Código de empleado" : ""));
            int opcion = entrada.leerEntero("Seleccione una opción: ", 0, venta instanceof VentaTienda ? 2 : 1);
            if (opcion == 0) return;
            try {
                if (opcion == 1) {
                    BonoDescuento bono = entrada.seleccionar("Bonos disponibles",
                            venta.getComprador().getBonosDisponibles(),
                            b -> b.getId() + " - " + dinero(b.getValor()));
                    if (bono == null) continue;
                    venta.aplicarBono(bono);
                } else {
                    String texto = entrada.leerTexto("Código de empleado (0 para volver): ");
                    if (texto.equals("0")) continue;
                    CodigoDescuento codigo = buscarCodigo(codigos, texto);
                    ((VentaTienda) venta).aplicarCodigo(codigo);
                }
                return;
            } catch (IllegalArgumentException | IllegalStateException e) {
                entrada.mostrarMensaje(e.getMessage());
            }
        }
    }

    private CodigoDescuento buscarCodigo(List<CodigoDescuento> codigos, String texto) {
        for (CodigoDescuento codigo : codigos) {
            if (codigo.getCodigo().equals(texto)) return codigo;
        }
        throw new IllegalArgumentException("El código no existe");
    }

    private void seleccionarPuntos(Venta venta) {
        Usuario comprador = venta.getComprador();
        SaldoFidelidad saldo = comprador instanceof Socio
                ? ((Socio) comprador).getSaldoFidelidad() : ((Empleado) comprador).getSaldoFidelidad();
        entrada.mostrarMensaje("Puntos disponibles: " + saldo.getPuntos());
        int maximo = Math.min(saldo.getPuntos(), (int) Math.floor(venta.getTotal()));
        if (maximo > 0) {
            venta.redimirPuntos(entrada.leerEntero("Puntos a utilizar (0 para no utilizar): ", 0, maximo));
        }
    }

    private void mostrarResumen(Venta venta) {
        entrada.mostrarMensaje("Resumen de compra - " + venta.getComprador().getEmail());
        for (DetalleVenta detalle : venta.getDetalleVentas()) {
            entrada.mostrarMensaje(detalle.getCantidad() + " x " + detalle.getProducto().getNombre()
                    + " = " + dinero(detalle.getSubtotal()));
        }
        entrada.mostrarMensaje("Subtotal: " + dinero(venta.getSubtotal()));
        entrada.mostrarMensaje("Descuento: " + dinero(venta.getDescuentos()));
        entrada.mostrarMensaje("Impuestos: " + dinero(venta.getImpuestos()));
        if (venta instanceof VentaCafeteria) {
            entrada.mostrarMensaje("Propina: " + dinero(((VentaCafeteria) venta).getPropina()));
        }
        entrada.mostrarMensaje("Puntos utilizados: " + venta.getPuntosRedimidos());
        entrada.mostrarMensaje("Total: " + dinero(venta.getTotal()));
    }

    private Usuario obtenerComprador() {
        Usuario usuario = sesion.getUsuarioActual();
        if (!sesion.haySesionActiva() || !usuario.isActivo()) {
            throw new IllegalStateException("Se requiere una sesión activa");
        }
        if (!(usuario instanceof Socio) && !(usuario instanceof Empleado)) {
            throw new IllegalStateException("Las compras requieren un socio o un empleado");
        }
        return usuario;
    }

    private String dinero(double valor) {
        return String.format(Locale.ROOT, "$ %.2f", valor);
    }
}
