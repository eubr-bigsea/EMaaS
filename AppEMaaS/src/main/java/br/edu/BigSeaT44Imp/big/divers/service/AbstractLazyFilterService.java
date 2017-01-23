package br.edu.BigSeaT44Imp.big.divers.service;

import java.io.Serializable;
import java.util.List;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.edu.BigSeaT44Imp.big.divers.dao.AbstractLazyFilterDAO;
import br.edu.BigSeaT44Imp.big.divers.dao.BigSeaT44PersistenciaException;
import br.edu.BigSeaT44Imp.big.divers.datamodel.DadosLazy;
import br.edu.BigSeaT44Imp.big.divers.filtro.IFiltro;
import br.edu.BigSeaT44Imp.big.divers.model.Identificavel;


@Transactional
public abstract class AbstractLazyFilterService<E extends Identificavel<? extends Serializable>, F extends IFiltro<E>> extends
		GenericService<E> implements ILazyFilterService<E, F> {

	private static final long serialVersionUID = -563117388573052707L;

	@Override
	protected abstract AbstractLazyFilterDAO<E, F> getDaoEntidade();

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Integer getTotalRegistros(F filtro) throws BigSeaT44ServiceException {
		Integer resultado = null;

		try {
			resultado = getDaoEntidade().getTotalRegistros(filtro);
		} catch (BigSeaT44PersistenciaException e) {
			throw new BigSeaT44ServiceException(e.getMessage(), e);
		}

		return resultado;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public E find(Serializable id) throws BigSeaT44ServiceException {
		E resultado = null;
		try {
			resultado = getDaoEntidade().find(id);
		} catch (BigSeaT44PersistenciaException e) {
			throw new BigSeaT44ServiceException(e.getMessage(), e);
		}

		return resultado;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<E> getEntidades(DadosLazy dadosLazy, F filtro) throws BigSeaT44ServiceException {
		List<E> resultado = null;
		try {
			resultado = getDaoEntidade().getEntidades(dadosLazy, filtro);
		} catch (BigSeaT44PersistenciaException e) {
			throw new BigSeaT44ServiceException(e.getMessage(), e);
		}
		return resultado;
	}

}
