package club.consola;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;

// Comparte la lectura y los mensajes sin cerrar el Scanner de la aplicacion
public class EntradaConsola {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private final Scanner entrada;
    private final PrintStream salida;

    public EntradaConsola(Scanner entrada, PrintStream salida) {
        if (entrada == null || salida == null) {
            throw new IllegalArgumentException("La entrada y la salida son obligatorias");
        }
        this.entrada = entrada;
        this.salida = salida;
    }

    public void mostrarMensaje(String mensaje) {
        salida.println(mensaje);
    }

    // Conserva los espacios, por ejemplo al leer una contraseña
    public String leerLinea(String mensaje) {
        salida.print(mensaje);
        salida.flush();
        if (!entrada.hasNextLine()) {
            throw new NoSuchElementException("Se cerró la entrada de consola");
        }
        return entrada.nextLine();
    }

    public String leerTexto(String mensaje) {
        while (true) {
            String texto = leerLinea(mensaje).trim();
            if (!texto.isEmpty()) return texto;
            mostrarMensaje("Debe escribir un valor.");
        }
    }

    public int leerEntero(String mensaje, int minimo, int maximo) {
        if (minimo > maximo) throw new IllegalArgumentException("El rango no es válido");
        while (true) {
            try {
                int valor = Integer.parseInt(leerLinea(mensaje).trim());
                if (valor >= minimo && valor <= maximo) return valor;
            } catch (NumberFormatException e) {
                // Se vuelve a solicitar tanto un número mal escrito como uno fuera de rango.
            }
            mostrarMensaje("Escriba un entero entre " + minimo + " y " + maximo + ".");
        }
    }

    public double leerDecimal(String mensaje, double minimo, double maximo) {
        if (!Double.isFinite(minimo) || !Double.isFinite(maximo) || minimo > maximo) {
            throw new IllegalArgumentException("El rango no es válido");
        }
        while (true) {
            try {
                double valor = Double.parseDouble(leerLinea(mensaje).trim().replace(',', '.'));
                if (Double.isFinite(valor) && valor >= minimo && valor <= maximo) return valor;
            } catch (NumberFormatException e) {
               
            }
            mostrarMensaje("Escriba un número entre " + minimo + " y " + maximo + ".");
        }
    }

    // Devuelve null cuando el usuario escribe 0 para cancelar. 
    public LocalDate leerFecha(String mensaje) {
        while (true) {
            String texto = leerLinea(mensaje + " (AAAA-MM-DD; 0 para cancelar): ").trim();
            if (texto.equals("0")) return null;
            try {
                return LocalDate.parse(texto);
            } catch (DateTimeParseException e) {
                mostrarMensaje("Escriba una fecha válida, por ejemplo 2026-11-08.");
            }
        }
    }

    //Devuelve null cuando el usuario escribe 0 para cancelar
    public LocalDateTime leerFechaHora(String mensaje) {
        while (true) {
            String texto = leerLinea(mensaje + " (AAAA-MM-DD HH:mm; 0 para cancelar): ").trim();
            if (texto.equals("0")) return null;
            try {
                return LocalDateTime.parse(texto, FECHA_HORA);
            } catch (DateTimeParseException e) {
                mostrarMensaje("Escriba una fecha y hora válidas, por ejemplo 2026-11-08 14:30.");
            }
        }
    }

    public boolean confirmar(String mensaje) {
        while (true) {
            String opcion = leerLinea(mensaje + " (s/n): ").trim().toLowerCase(Locale.ROOT);
            if (opcion.equals("s") || opcion.equals("si") || opcion.equals("sí")) return true;
            if (opcion.equals("n") || opcion.equals("no")) return false;
            mostrarMensaje("Responda s o n.");
        }
    }

    // Muestra una lista numerada, devuelve null al cancelar o si la lista está vaciaa
    public <T> T seleccionar(String titulo, List<T> opciones, Function<? super T, String> descripcion) {
        if (opciones == null || descripcion == null) {
            throw new IllegalArgumentException("Las opciones y su descripción son obligatorias");
        }
        mostrarMensaje(titulo);
        if (opciones.isEmpty()) {
            mostrarMensaje("No hay opciones disponibles.");
            return null;
        }
        for (int i = 0; i < opciones.size(); i++) {
            mostrarMensaje((i + 1) + ". " + descripcion.apply(opciones.get(i)));
        }
        mostrarMensaje("0. Volver");
        int opcion = leerEntero("Seleccione una opción: ", 0, opciones.size());
        return opcion == 0 ? null : opciones.get(opcion - 1);
    }
}
