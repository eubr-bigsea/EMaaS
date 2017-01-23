package br.edu.BigSeaT44Imp.big.divers.dao;

public class BigSeaT44PersistenciaException extends BigSeaT44Exception {
	private static final long serialVersionUID = 5948133794930016910L;

	public BigSeaT44PersistenciaException(String mensagem) {
		super(mensagem);
	}

	public BigSeaT44PersistenciaException(String string, Exception e) {
		super(string, e);
	}
}
