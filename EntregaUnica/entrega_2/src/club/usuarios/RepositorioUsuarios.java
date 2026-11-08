package club.usuarios;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import club.usuarios.Usuario;


public class RepositorioUsuarios {
	
	//atributos
	private static final Map<String,Usuario> usuarios = new HashMap<>();


	
	//metodos
	
	public static void anadirUsuarioARepo(Usuario usuario) {
		usuarios.put(usuario.getEmail(), usuario);
	}
	
	public static final Map<String,Usuario> getRepositorioUsuarios(){
		return usuarios;
	}
	
	public static Usuario buscarPorEmail(String email) {
		
		return usuarios.get(email);
	}
		
		
}
