package club.persistencia;

public class PersistenciaException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	public PersistenciaException(String mensaje) {
		super(mensaje);
	}

	public PersistenciaException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}
}
