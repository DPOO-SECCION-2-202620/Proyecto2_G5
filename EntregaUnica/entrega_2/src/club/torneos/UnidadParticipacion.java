package club.torneos;

import java.util.ArrayList;
import java.util.List;

public class UnidadParticipacion {
	private ModalidadTorneo modalidad;
	private List<Object> integrantes;

	public UnidadParticipacion(ModalidadTorneo modalidad, List<Object> integrantes) {
		if (modalidad == null || integrantes == null || integrantes.isEmpty()) {
			throw new IllegalArgumentException("La unidad de participación no es válida");
		}
		this.modalidad = modalidad;
		this.integrantes = new ArrayList<Object>();
		for (Object integrante : integrantes) {
			if (integrante == null || this.integrantes.contains(integrante)) {
				throw new IllegalArgumentException("Los integrantes deben existir y ser distintos");
			}
			this.integrantes.add(integrante);
		}
	}

	public ModalidadTorneo getModalidad() {
		return modalidad;
	}

	public List<Object> getIntegrantes() {
		return new ArrayList<Object>(integrantes);
	}

	public boolean contiene(Object participante) {
		return integrantes.contains(participante);
	}
}
