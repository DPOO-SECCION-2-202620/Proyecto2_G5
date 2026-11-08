
package club.tienda;

import java.time.LocalDateTime;
import java.util.List;

import club.usuarios.Usuario;

public class VentaTienda extends Venta {

    // Constante
    private static final double IVA = 0.19;

    // Constructor 1: venta nueva
    public VentaTienda(Usuario comprador) {
        super(comprador);
    }

    // Constructor 2: venta historica
    public VentaTienda(
            Usuario comprador,
            LocalDateTime fecha,
            List<DetalleVenta> detalleVentas,
            double subtotal,
            double impuestos,
            double descuentos,
            double total,
            int puntosGanados,
            int puntosRedimidos,
            boolean confirmada) {

        super(comprador, fecha, detalleVentas,
              subtotal, impuestos, descuentos, total,
              puntosGanados, puntosRedimidos, confirmada);
    }

    // Aplicar un codigo de descuento
    public double aplicarCodigo(CodigoDescuento codigo) {

        if (estaConfirmada()) {
            throw new IllegalStateException(
                "La venta ya esta confirmada");
        }

        if (codigo == null) {
            throw new IllegalArgumentException(
                "Se requiere un codigo de descuento");
        }

        double porcentaje = codigo.porcentajePara(comprador);

        if (!Double.isFinite(porcentaje) ||
                porcentaje <= 0 || porcentaje > 1) {
            throw new IllegalArgumentException(
                "El codigo no aplica al comprador");
        }

		return aplicarDescuento(porcentaje);
    }

    // Calcular IVA del 19 %
    public double calcularIVA() {
        return (calcularSubtotal() - descuentos) * IVA;
    }

    // Calcular subtotal, descuento, IVA y total
    @Override
    public double liquidar() {

        if (estaConfirmada()) {
            throw new IllegalStateException(
                "No se puede liquidar una venta confirmada");
        }

        if (detalleVentas.isEmpty()) {
            throw new IllegalStateException(
                "La venta no tiene productos");
        }

        // Validar que todos los productos sean articulos
        for (DetalleVenta detalle : detalleVentas) {
            if (!(detalle.getProducto() instanceof Articulo)) {
                throw new IllegalArgumentException(
                    "La venta de tienda solo admite articulos");
            }
        }

        double base = calcularSubtotal();

        if (descuentos < 0 || descuentos > base) {
            throw new IllegalStateException(
                "Descuento fuera de rango");
        }

        double impuestoCalculado = calcularIVA();

        double totalCalculado = base - descuentos
                + impuestoCalculado - puntosRedimidos;

        if (totalCalculado < 0) {
            throw new IllegalStateException(
                "Los puntos exceden el total de la compra");
        }

        this.subtotal = base;
        this.impuestos = impuestoCalculado;
        this.total = totalCalculado;

        return total;
    }
}
