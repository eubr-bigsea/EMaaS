package br.edu.BigSeaT44Imp.big.divers.service;

import java.io.Serializable;
import java.util.List;

import br.edu.BigSeaT44Imp.big.divers.datamodel.DadosLazy;
import br.edu.BigSeaT44Imp.big.divers.filtro.IFiltro;
import br.edu.BigSeaT44Imp.big.divers.model.Identificavel;

public interface ILazyFilterService<E extends Identificavel<? extends Serializable>, F extends IFiltro<E>> {

	Integer getTotalRegistros(F filtro) throws BigSeaT44ServiceException;

	E find(Serializable id) throws BigSeaT44ServiceException;

	List<E> getEntidades(DadosLazy dadosLazy, F filtro) throws BigSeaT44ServiceException;
}
