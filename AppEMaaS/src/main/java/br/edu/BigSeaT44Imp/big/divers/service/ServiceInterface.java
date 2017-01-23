package br.edu.BigSeaT44Imp.big.divers.service;

import java.util.List;

public interface ServiceInterface<T> {

	public void persistir(T obj) throws BigSeaT44ServiceException;

	public List<T> findAll() throws BigSeaT44ServiceException;

	

	public void remove(T obj) throws BigSeaT44ServiceException;

	public void update(T obj) throws BigSeaT44ServiceException;

}
