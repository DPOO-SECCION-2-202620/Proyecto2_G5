
package club.tienda;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import club.usuarios.Usuario;
import club.deportes.Padel;
import club.deportes.TenisDeMesa;
import club.instalaciones.Instalacion;

public class VentaCafeteria extends Venta {

    // Constantes
    private static final double IMPUESTO_CONSUMO = 0.08;
    private static final double PROPINA_SUGERIDA = 0.10;

    // Atributos
    private double propinaPorcentaje;
    private boolean avisoAlergenosRealizado;
	private Instalacion instalacion;

    // Constructor 1: venta nueva
    public VentaCafeteria(Usuario comprador) {
		this(comprador, null);
	}

	public VentaCafeteria(Usuario comprador, Instalacion instalacion) {
        super(comprador);

        this.propinaPorcentaje = PROPINA_SUGERIDA;
        this.avisoAlergenosRealizado = false;
		this.instalacion = instalacion;
    }

    // Constructor 2: venta historica
    public VentaCafeteria(
            Usuario comprador,
            LocalDateTime fecha,
            List<DetalleVenta> detalleVentas,
            double subtotal,
            double impuestos,
            double descuentos,
            double total,
            int puntosGanados,
            int puntosRedimidos,
            boolean confirmada,
            double propinaPorcentaje,
            boolean avisoAlergenosRealizado) {

        super(comprador, fecha, detalleVentas,
              subtotal, impuestos, descuentos, total,
              puntosGanados, puntosRedimidos, confirmada);

        this.propinaPorcentaje = propinaPorcentaje;
        this.avisoAlergenosRealizado = avisoAlergenosRealizado;
    }

    // Obtener los alergenos presentes en los snacks
    public List<String> informarAlergenos() {

        if (estaConfirmada()) {
            throw new IllegalStateException(
                "La venta ya esta confirmada");
        }

        List<String> alergenos = new ArrayList<>();

        for (DetalleVenta detalle : detalleVentas) {

            if (detalle.getProducto() instanceof Snack) {

                Snack snack = (Snack) detalle.getProducto();

                for (String alergeno : snack.getAlergenos()) {

                    if (!alergenos.contains(alergeno)) {
                        alergenos.add(alergeno);
                    }
                }
            }
        }

        this.avisoAlergenosRealizado = true;

        return alergenos;
    }

    // Calcular impuesto al consumo del 8 %
    public double calcularImpuesto() {
		return (calcularSubtotal() - descuentos) * IMPUESTO_CONSUMO;
    }

    // Modificar el porcentaje de propina
    public void ajustarPropina(double porcentaje) {

        if (estaConfirmada()) {
            throw new IllegalStateException(
                "La venta ya esta confirmada");
        }

        if (!Double.isFinite(porcentaje) ||
                porcentaje < 0 || porcentaje > 1) {
            throw new IllegalArgumentException(
                "La propina debe estar entre 0 y 1");
        }

        this.propinaPorcentaje = porcentaje;
    }

    // Calcular subtotal, impuesto, propina y total
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

        // Validar productos y aviso de alergenos
        for (DetalleVenta detalle : detalleVentas) {

            if (!(detalle.getProducto()
                    instanceof ProductoCafeteria)) {
                throw new IllegalArgumentException(
                    "Solo se admiten productos de cafeteria");
            }

            if (detalle.getProducto() instanceof Snack
                    && !avisoAlergenosRealizado) {
                throw new IllegalStateException(
                    "Primero se deben informar los alergenos");
            }

			if (detalle.getProducto() instanceof Bebida
					&& ((Bebida) detalle.getProducto()).esCaliente()) {
				validarBebidaCaliente();
			}
        }

        double base = calcularSubtotal();
		double baseConDescuento = base - descuentos;
		double impuestoCalculado = calcularImpuesto();
        double propina = base * propinaPorcentaje;

		double totalCalculado = baseConDescuento + impuestoCalculado
                + propina - puntosRedimidos;

        if (totalCalculado < 0) {
            throw new IllegalStateException(
                "Los puntos exceden el total de la compra");
        }

        this.subtotal = base;
        this.impuestos = impuestoCalculado;
        this.total = totalCalculado;

        return total;
    }

    // Getters
    public double getPropinaPorcentaje() {
        return propinaPorcentaje;
    }

    public boolean isAvisoAlergenosRealizado() {
        return avisoAlergenosRealizado;
    }

	public double getPropina() {
		return calcularSubtotal() * propinaPorcentaje;
	}

	private void validarBebidaCaliente() {
		if (instalacion == null) return;
		if (instalacion.getDeporteIndividual() instanceof Padel
				|| (instalacion.getDeporteIndividual() instanceof TenisDeMesa && instalacion.isTechada())) {
			throw new IllegalStateException("No se permiten bebidas calientes en esta instalación");
		}
	}
}
