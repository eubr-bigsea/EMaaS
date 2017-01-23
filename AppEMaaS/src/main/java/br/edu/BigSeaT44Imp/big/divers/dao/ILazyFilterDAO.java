package br.edu.BigSeaT44Imp.big.divers.dao;

import java.io.Serializable;
import java.util.List;

import br.edu.BigSeaT44Imp.big.divers.datamodel.DadosLazy;
import br.edu.BigSeaT44Imp.big.divers.filtro.IFiltro;
import br.edu.BigSeaT44Imp.big.divers.model.Identificavel;

public interface ILazyFilterDAO<E extends Identificavel<?>, F extends IFiltro<E>> {

	Integer getTotalRegistros(F filtro) throws BigSeaT44PersistenciaException;

	E find(Serializable id) throws BigSeaT44PersistenciaException;

	List<E> getEntidades(DadosLazy dadosLazy, F filtro) throws BigSeaT44PersistenciaException;
}
