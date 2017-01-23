package br.edu.BigSeaT44Imp.big.divers.service;

import br.edu.BigSeaT44Imp.big.divers.dao.BigSeaT44Exception;


public class BigSeaT44ServiceException extends BigSeaT44Exception {

	private static final long serialVersionUID = 4523726371933709165L;

	public BigSeaT44ServiceException(String mensagem) {
		super(mensagem);
	}

	public BigSeaT44ServiceException(String mensagem, Throwable throwable) {
		super(mensagem, throwable);
	}

}
