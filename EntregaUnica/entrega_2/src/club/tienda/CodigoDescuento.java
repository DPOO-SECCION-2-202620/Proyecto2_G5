package club.tienda;

import club.personal.*;
import club.usuarios.Usuario;

public class CodigoDescuento {
	
	//atributos
	private String codigo;
	private Empleado propietario;
	
	private static final double DESCUENTO_PROPIO = 0.15;
	private static final double DESCUENTO_COMPARTIDO = 0.08;
	
	//ctor
	public CodigoDescuento(String codigo, Empleado propietario) {
		if (codigo == null || codigo.isBlank() || propietario == null) {
			throw new IllegalArgumentException("El código de descuento no es válido");
		}
		this.codigo = codigo;
		this.propietario = propietario;
	}


	//metodos
	public String getCodigo() {
		return codigo;
	}

	public Empleado getPropietario() {
		return propietario;
	}
	
	public double porcentajePara(Usuario comprador) {
		if(comprador.equals(this.getPropietario())) {
			return DESCUENTO_PROPIO;
		} else if (comprador instanceof club.usuarios.Socio) {
			return DESCUENTO_COMPARTIDO;
		}
		return 0;
		
	}
	
	

}
