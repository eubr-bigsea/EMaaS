package br.edu.BigSeaT44Imp.big.divers.dao;

public class BigSeaT44Exception extends Exception{
	private static final long serialVersionUID = 3607940088523410450L;

	public BigSeaT44Exception(String mensagem) {
		super(mensagem);
	}

	public BigSeaT44Exception(String mensagem, Throwable throwable) {
		super(mensagem, throwable);
	}
}
