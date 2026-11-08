package club.acceso;
import club.usuarios.*;

public class Autenticador {
	private SesionActual sesionActual;
	
	
	//metodos
	public SesionActual autenticar(String email, String contrasenia) {
		//obtener usuario por email
		Usuario usuario = RepositorioUsuarios.buscarPorEmail(email);
		
		//validar usuario existente
		if (usuario == null) {
			throw new IllegalArgumentException("El usuario no existe");
		}
		if (!usuario.isActivo()) {
			throw new IllegalStateException("El usuario está inactivo");
		}
			
		//validar contrasena
		if (!usuario.isContrasenia(contrasenia)) {
				throw new IllegalArgumentException("contrsasenia incorrecta");
					
		}
		
		//iniciar sesion actual
		this.sesionActual = new SesionActual(usuario);
		return this.sesionActual;
	}

	public SesionActual getSesionActual() {
		return sesionActual;
	}

}

