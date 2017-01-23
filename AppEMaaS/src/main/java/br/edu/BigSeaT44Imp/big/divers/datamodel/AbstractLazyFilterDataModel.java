package br.edu.BigSeaT44Imp.big.divers.datamodel;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import br.edu.BigSeaT44Imp.big.divers.filtro.IFiltro;
import br.edu.BigSeaT44Imp.big.divers.model.Identificavel;
import br.edu.BigSeaT44Imp.big.divers.service.BigSeaT44ServiceException;
import br.edu.BigSeaT44Imp.big.divers.service.ILazyFilterService;

@SuppressWarnings("unchecked")
public abstract class AbstractLazyFilterDataModel<T extends Identificavel<? extends Serializable>, F extends IFiltro<T>> extends
	LazyDataModel<T> {

	private static final long serialVersionUID = 704959881758741431L;

	private List<T> entidades;

	private F filtro;

	private boolean search;

	public F getInstanciaFiltro() {
		F resultado = null;
		
		try {
			ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();
			Class<F> type = (Class<F>) superClass.getActualTypeArguments()[1];
			resultado = type.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return resultado;
	}

	public AbstractLazyFilterDataModel() {
		setFiltro(getInstanciaFiltro());
	}

	protected abstract ILazyFilterService<T, F> getService();

	public List<T> getEntidades(DadosLazy dadosLazy, F filtro) {
		try {
			return getService().getEntidades(dadosLazy, filtro);
		} catch (BigSeaT44ServiceException e) {
			// TODO tratar erro de forma adequada
			e.printStackTrace();

			return new ArrayList<T>();
		}
	}

	public Integer getTotalRegistros(F filtro) {
		try {
			return getService().getTotalRegistros(filtro);
		} catch (BigSeaT44ServiceException e) {
			// TODO tratar erro de forma adequada
			e.printStackTrace();
			
			return 0;
		}
	}

	public T find(Serializable id) {
		try {
			return getService().find(id);
		} catch (BigSeaT44ServiceException e) {
			// TODO tratar erro de forma adequada
			e.printStackTrace();
			
			return null;
		}
	}

	@Override
	public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
		if (!search) {
			setRowCount(0);
	
			return new ArrayList<T>();
		}

		F filtro = getFiltro();
		Map<String, String> mapa = converteMapa(filters);
		filtro.setFilters(mapa);
		loadRowCount();
		
		if (getRowCount() <= first) {
			// Determinar o "first" como o primeiro registro da última página
			boolean numLinhasNaoMultDoTamPagina = getRowCount() % pageSize != 0;
			first = numLinhasNaoMultDoTamPagina ? (getRowCount() - getRowCount() % pageSize) : getRowCount() - pageSize;
			first = Math.max(first, 0);
		}

		this.entidades = this.getEntidades(new DadosLazy(first, pageSize, sortField, sortOrder), filtro);
		return this.entidades;
	}

	private Map<String, String> converteMapa(Map<String, Object> filters) {
		Map<String, String> mapa = new HashMap<String, String>();
		
		for (Map.Entry<String, Object> entry : filters.entrySet()){
		    mapa.put(entry.getKey(), entry.getValue().toString());
		}
		
		return mapa;
	}

	public void loadRowCount() {
		search = true;
		int totalRegistros = this.getTotalRegistros(getFiltro());
		setRowCount(totalRegistros);
	}

	@Override
	public T getRowData(String rowKey) {
		Serializable id;
		
		if (NumberUtils.isNumber(rowKey)) {
			try {
				id	= (Integer.parseInt(rowKey));
			} catch (NumberFormatException n) {
				id	= (Long.parseLong(rowKey));
			}
		} else {
			id =  rowKey;
		}
		
		try {
			return this.find(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Object getRowKey(T entidade) {
		return entidade.getId();
	}

	public void doSearch() {
		this.setFiltro(getInstanciaFiltro());
		this.loadRowCount();
		search = true;
	}

	public F getFiltro() {
		return filtro;
	}

	public void setFiltro(F filtro) {
		this.filtro = filtro;
	}

	public boolean isSearch() {
		return search;
	}

	public void setSearch(boolean search) {
		this.search = search;
	}

	public void clearList() {
		setSearch(false);
	}
}
