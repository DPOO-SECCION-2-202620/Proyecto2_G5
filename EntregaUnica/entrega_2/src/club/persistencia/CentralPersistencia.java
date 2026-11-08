package club.persistencia;

public class CentralPersistencia {

	public static final String JSON = "JSON";

	private CentralPersistencia() { }

	public static IPersistenciaClub getPersistenciaClub(String tipoArchivo) {
		if (JSON.equals(tipoArchivo)) return new PersistenciaClubJson();
		throw new IllegalArgumentException("El tipo de persistencia no es válido: " + tipoArchivo);
	}
}
