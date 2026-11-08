package club.reportes;

import java.util.ArrayList;
import java.util.List;

import club.Inscripciones.InscripcionAcademia;
import club.entrenamientos.RegistroEntrenamiento;
import club.partidos.Partido;

public class ReporteDeportivo {
	private List<RegistroEntrenamiento> asistencias;
	private List<Partido> resultados;
	private List<InscripcionAcademia> rankings;

	public ReporteDeportivo(List<RegistroEntrenamiento> asistencias,
			List<Partido> resultados, List<InscripcionAcademia> rankings) {
		this.asistencias = new ArrayList<RegistroEntrenamiento>(asistencias);
		this.resultados = new ArrayList<Partido>(resultados);
		this.rankings = new ArrayList<InscripcionAcademia>(rankings);
	}

	public List<RegistroEntrenamiento> getAsistencias() { 
		return new ArrayList<RegistroEntrenamiento>(asistencias); 
	}
	
	public List<Partido> getResultados() { 
		return new ArrayList<Partido>(resultados); 
	}
	
	public List<InscripcionAcademia> getRankings() { 
		return new ArrayList<InscripcionAcademia>(rankings); 
	}
}
