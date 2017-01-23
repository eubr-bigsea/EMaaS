package br.edu.BigSeaT44Imp.big.divers.model;

public enum  Perfil  {
	
	ADM (1, "Administrador"),
	PROF (2, "Professor"),
	ALUNO (3, "Aluno"),
	SECRETARIO (4, "Secretário");
	
	private final String perfil;
	private final int codigo;
	
	private Perfil(final int codigo, final String perfil) {
		this.perfil = perfil;
		this.codigo = codigo;
	}

	public String getPerfil() {
		return perfil;
	}

	public int getCodigo() {
		return codigo;
	}
}
