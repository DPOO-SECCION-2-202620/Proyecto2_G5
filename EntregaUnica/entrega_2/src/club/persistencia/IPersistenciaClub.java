package club.persistencia;

import club.tienda.GestorTienda;

public interface IPersistenciaClub {

	public void cargarClub();

	public void salvarClub();

	public void salvarClubHastaConfirmar();

	public GestorTienda getGestorTienda();
}
