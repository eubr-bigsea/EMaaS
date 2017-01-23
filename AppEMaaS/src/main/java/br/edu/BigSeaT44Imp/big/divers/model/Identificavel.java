package br.edu.BigSeaT44Imp.big.divers.model;

import java.io.Serializable;

public interface Identificavel<I extends Serializable> extends Serializable {

	public I getId();

	public void setId(I id);
}
