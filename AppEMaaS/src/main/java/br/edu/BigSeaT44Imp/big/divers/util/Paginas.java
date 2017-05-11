package br.edu.BigSeaT44Imp.big.divers.util;

public enum Paginas {
	CURSOS ("cursos"),
	FILES ("files"),
	OUTROS("");
	
	// COMPLEMENTOS
	public static final String REDIRECT_TRUE = "?faces-redirect=true";
	
	// PASTAS
	public static final String PROTECTED_FOLDER = "/protegidos/";
	
	// INDEX
	public static final String INDEX = PROTECTED_FOLDER + "index" + REDIRECT_TRUE;
	
	// PRINCIPAL
	public static final String PRINCIPAL = PROTECTED_FOLDER + "principal" + REDIRECT_TRUE;

	// LOGIN
	public static final String LOGIN = "/login" + REDIRECT_TRUE;
	
	private final String diretorio;
	
	private Paginas(String diretorio){
		this.diretorio=diretorio;
	}
	
	public String getInitialPath(){
		return PROTECTED_FOLDER + this.getDiretorio()+"/";
	}
	
	public String getPaginaGerenciador(){
		return PROTECTED_FOLDER + this.getDiretorio()+"/" + "gerenciador" + REDIRECT_TRUE; 
	}
	
	public String getPaginaCriar(){
		return PROTECTED_FOLDER + this.getDiretorio()+"/" + "criar" + REDIRECT_TRUE; 
	}
	
	public String getPaginaEditar(){
		return PROTECTED_FOLDER + this.getDiretorio()+"/" + "editar" + REDIRECT_TRUE; 
	}
	
	public String getPaginaDetalhes(){
		return PROTECTED_FOLDER + this.getDiretorio()+"/" + "detalhar" + REDIRECT_TRUE; 
	}

	public String getPaginaMapa(){
		return PROTECTED_FOLDER + this.getDiretorio()+"/" + "mapa" + REDIRECT_TRUE; 
	}
	
	public String getDiretorio() {
		return diretorio;
	}
}
