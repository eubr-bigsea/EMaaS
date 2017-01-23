package br.edu.BigSeaT44Imp.big.divers.bean;

import br.edu.BigSeaT44Imp.big.divers.dao.BigSeaT44Exception;

public class BigSeaT44PermissaoException extends BigSeaT44Exception {
	private static final long serialVersionUID = 4523726371933709165L;

	public BigSeaT44PermissaoException(String mensagem) {
		super(mensagem);
	}

	public BigSeaT44PermissaoException(String mensagem, Throwable throwable) {
		super(mensagem, throwable);
	}
}
