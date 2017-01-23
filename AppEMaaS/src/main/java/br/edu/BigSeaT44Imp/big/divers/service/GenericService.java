package br.edu.BigSeaT44Imp.big.divers.service;

import java.io.Serializable;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.edu.BigSeaT44Imp.big.divers.dao.BigSeaT44PersistenciaException;
import br.edu.BigSeaT44Imp.big.divers.dao.DAO;
import br.edu.BigSeaT44Imp.big.divers.filtro.AbstractFiltro;
import br.edu.BigSeaT44Imp.big.divers.model.Identificavel;


@Transactional
@Service
public abstract class GenericService<E extends Identificavel<? extends Serializable>> implements Serializable, ServiceInterface<E> {

	private static final long serialVersionUID = -2309763094662822607L;
	
	public GenericService() {

	}

	protected abstract DAO<E> getDaoEntidade();

	public void persistir(E obj) throws BigSeaT44ServiceException {
		try {
			getDaoEntidade().save(obj);
		} catch (BigSeaT44PersistenciaException e) {
			throw new BigSeaT44ServiceException(e.getMessage(), e);
		}

	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<E> findAll() throws BigSeaT44ServiceException {
		try {
			return getDaoEntidade().findAll();
		} catch (BigSeaT44PersistenciaException e) {
			throw new BigSeaT44ServiceException(e.getMessage(), e);
		}
	}

	public void remove(E obj) throws BigSeaT44ServiceException {
		try {
			getDaoEntidade().remove(obj);
		} catch (BigSeaT44PersistenciaException e) {
			throw new BigSeaT44ServiceException(e.getMessage(), e);
		}
	}

	public void update(E obj) throws BigSeaT44ServiceException {
		try {
			getDaoEntidade().update(obj);
		} catch (BigSeaT44PersistenciaException e) {
			throw new BigSeaT44ServiceException(e.getMessage(), e);
		}
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<E> findAllByFields(AbstractFiltro<E> filtro) throws BigSeaT44ServiceException {
		try {
			return getDaoEntidade().findAllByFields(filtro);
		} catch (BigSeaT44PersistenciaException e) {
			throw new BigSeaT44ServiceException("Ocorreu algum erro ao tentar recuperar as entidades.", e);
		}
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public E getById(Serializable id) throws BigSeaT44ServiceException {
		try {
			return getDaoEntidade().find(id);
		} catch (BigSeaT44PersistenciaException e) {
			e.printStackTrace();
			throw new BigSeaT44ServiceException("Ocorreu algum erro ao tentar recuperar a entidade.", e);
		}

	}
	


	

}
