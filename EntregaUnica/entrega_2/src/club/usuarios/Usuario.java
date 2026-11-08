package club.usuarios;

import club.usuarios.RepositorioUsuarios;
import club.tienda.BonoDescuento;
import java.util.ArrayList;
import java.util.List;



public abstract class Usuario {
	private String email; 
	private String contrasenia;
	private boolean activo;
	private List<BonoDescuento> bonos = new ArrayList<BonoDescuento>();
	
	
	
	
	public Usuario(String email, String contrasenia) {
		super();
		this.email = email;
		this.contrasenia = contrasenia;
		this.activo = true;
		RepositorioUsuarios.anadirUsuarioARepo(this);;
	}

	public String getEmail() {
		return email;
	} 

	public String getContrasenia() {
		return contrasenia;
	}
	
	public boolean isContrasenia(String contraseniaIngresada) {
		if(contrasenia.equals(contraseniaIngresada)) {
			return true; }else {return false;
		}
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

	public void guardarBono(BonoDescuento bono) {
		if (bono == null || bono.getPropietario() != this) {
			throw new IllegalArgumentException("El bono no pertenece al usuario");
		}
		if (!bonos.contains(bono)) bonos.add(bono);
	}

	public List<BonoDescuento> getBonos() {
		return new ArrayList<BonoDescuento>(bonos);
	}

	public List<BonoDescuento> getBonosDisponibles() {
		List<BonoDescuento> disponibles = new ArrayList<BonoDescuento>();
		for (BonoDescuento bono : bonos) if (bono.estaDisponible()) disponibles.add(bono);
		return disponibles;
	}
	
	
}
