
package club.tienda;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import club.usuarios.Usuario;
import club.personal.Empleado;
import club.usuarios.*;


public abstract class Venta {
	private static final List<Venta> ventasConfirmadas = new ArrayList<Venta>();

    // Atributos
    protected LocalDateTime fecha;
    protected double subtotal;
    protected double impuestos;
    protected double descuentos;
    protected double total;
    protected int puntosGanados;
    protected int puntosRedimidos;

    protected List<DetalleVenta> detalleVentas;
    protected Usuario comprador;

    private boolean confirmada;
	private BonoDescuento bonoAplicado;
	private boolean descuentoConvencional;
	private double montoOriginal;
	private double valorDescontado;

    // ctor 1: venta nueva
    protected Venta(Usuario comprador) {

        if (comprador == null) {
            throw new IllegalArgumentException(
                "El comprador es obligatorio");
        }

        this.comprador = comprador;
        this.fecha = LocalDateTime.now();
        this.detalleVentas = new ArrayList<>();

        this.subtotal = 0;
        this.impuestos = 0;
        this.descuentos = 0;
        this.total = 0;

        this.puntosGanados = 0;
        this.puntosRedimidos = 0;
        this.confirmada = false;
    }

    // ctor 2: venta historica (persistencia)
    protected Venta(
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

        if (comprador == null || fecha == null ||
                detalleVentas == null) {
            throw new IllegalArgumentException(
                "Datos historicos incompletos");
        }

        this.comprador = comprador;
        this.fecha = fecha;
        this.detalleVentas = new ArrayList<>(detalleVentas);

        this.subtotal = subtotal;
        this.impuestos = impuestos;
        this.descuentos = descuentos;
        this.total = total;

        this.puntosGanados = puntosGanados;
        this.puntosRedimidos = puntosRedimidos;
        this.confirmada = confirmada;
    }
    //metodos
    // Agregar un producto a la venta
    public void agregarDetalle(DetalleVenta detalle) {

        if (confirmada) {
            throw new IllegalStateException(
                "No se puede modificar una venta confirmada");
        }

        if (detalle == null) {
            throw new IllegalArgumentException(
                "El detalle no puede ser nulo");
        }

		if (bonoAplicado != null) bonoAplicado.cancelarReserva(this);
		bonoAplicado = null;
		descuentoConvencional = false;
		detalleVentas.add(detalle);

        // Si cambia el carrito, se debe reaplicar
        // cualquier descuento que hubiera sido calculado.
        descuentos = 0;
        puntosGanados = 0;
    }

    // Calcular el subtotal de los productos
    protected double calcularSubtotal() {

        double suma = 0;

        for (DetalleVenta detalle : detalleVentas) {
            suma += detalle.getSubtotal();
        }

        return suma;
    }

    // Cada subclase implementa su propia liquidacion
    public abstract double liquidar();

	public void aplicarBono(BonoDescuento bono) {
		if (confirmada) throw new IllegalStateException("La venta ya está confirmada");
		if (bono == null) throw new IllegalArgumentException("El bono es obligatorio");
		if (bonoAplicado != null || descuentoConvencional || descuentos > 0) {
			throw new IllegalStateException("Los descuentos no son acumulables");
		}
		if (bono.getPropietario() != comprador) throw new IllegalArgumentException("El bono pertenece a otro usuario");
		if (fecha.isBefore(bono.getFechaObtencion())) throw new IllegalStateException("La compra es anterior al bono");
		double base = calcularSubtotal();
		if (base <= 0) throw new IllegalStateException("La venta no tiene productos");
		bono.reservar(this);
		bonoAplicado = bono;
		montoOriginal = base;
		valorDescontado = Math.min(base, bono.getValor());
		descuentos = valorDescontado;
		subtotal = base;
		total = base - descuentos;
	}

	public double aplicarDescuento(double porcentaje) {
		if (confirmada) throw new IllegalStateException("La venta ya está confirmada");
		if (bonoAplicado != null || descuentoConvencional) throw new IllegalStateException("Los descuentos no son acumulables");
		if (!Double.isFinite(porcentaje) || porcentaje <= 0 || porcentaje > 1) {
			throw new IllegalArgumentException("El porcentaje no es válido");
		}
		montoOriginal = calcularSubtotal();
		valorDescontado = montoOriginal * porcentaje;
		descuentos = valorDescontado;
		descuentoConvencional = true;
		total = montoOriginal - descuentos;
		return descuentos;
	}

    // Registrar puntos que se quieren redimir
    public void redimirPuntos(int cantidad) {

        if (confirmada) {
            throw new IllegalStateException(
                "La venta ya esta confirmada");
        }

        if (cantidad < 0) {
            throw new IllegalArgumentException(
                "Los puntos no pueden ser negativos");
        }

        this.puntosRedimidos = cantidad;
        this.puntosGanados = 0;
    }

    // Calcular puntos ganados (2 % del total)
    public int asignarPuntos() {

        if (!(comprador instanceof Socio) &&
                !(comprador instanceof Empleado)) {
            puntosGanados = 0;
            return 0;
        }

        puntosGanados = (int) Math.floor(total * 0.02);

        return puntosGanados;
    }

    // Confirmar una venta
    public void confirmar() {

        if (confirmada) {
            throw new IllegalStateException(
                "La venta ya esta confirmada");
        }

        if (detalleVentas.isEmpty()) {
            throw new IllegalStateException(
                "La venta no tiene productos");
        }

		try {
			liquidar();
			if (bonoAplicado != null) bonoAplicado.utilizar(this);
			this.confirmada = true;
			ventasConfirmadas.add(this);
		} catch (RuntimeException e) {
			if (bonoAplicado != null) bonoAplicado.cancelarReserva(this);
			bonoAplicado = null; descuentos = 0; valorDescontado = 0;
			throw e;
		}
    }

	public void confirmarVenta() { confirmar(); }

    // Getters
    public LocalDateTime getFecha() {
        return fecha;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getImpuestos() {
        return impuestos;
    }

    public double getDescuentos() {
        return descuentos;
    }

    public double getTotal() {
        return total;
    }

    public int getPuntosGanados() {
        return puntosGanados;
    }

    public int getPuntosRedimidos() {
        return puntosRedimidos;
    }

    public List<DetalleVenta> getDetalleVentas() {
        return new ArrayList<>(detalleVentas);
    }

    public Usuario getComprador() {
        return comprador;
    }

    public boolean estaConfirmada() {
        return confirmada;
    }

	public BonoDescuento getBonoAplicado() { return bonoAplicado; }
	public double getMontoOriginal() { return montoOriginal == 0 ? calcularSubtotal() : montoOriginal; }
	public double getValorDescontado() { return valorDescontado; }

	public void restaurarBono(BonoDescuento bono, double montoOriginal, double valorDescontado) {
		this.bonoAplicado = bono;
		this.montoOriginal = montoOriginal;
		this.valorDescontado = valorDescontado;
	}

	public static void registrarHistorica(Venta venta) { ventasConfirmadas.add(venta); }
	public static List<Venta> getVentasConfirmadas() { return new ArrayList<Venta>(ventasConfirmadas); }
	public static void limpiarVentasConfirmadas() { ventasConfirmadas.clear(); }
}
