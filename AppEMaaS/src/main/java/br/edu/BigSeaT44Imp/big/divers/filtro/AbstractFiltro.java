package br.edu.BigSeaT44Imp.big.divers.filtro;

import java.io.Serializable;
import java.util.Map;

import br.edu.BigSeaT44Imp.big.divers.datamodel.DadosLazy;
import br.edu.BigSeaT44Imp.big.divers.model.Identificavel;


public class AbstractFiltro<E extends Identificavel<?>> implements IFiltro<E>, Serializable, Cloneable {

	private static final long serialVersionUID = 3249086838284671147L;

	private DadosLazy dadosLazy;

	private Map<String, String> filters;

	public DadosLazy getDadosLazy() {
		return dadosLazy;
	}

	public void setDadosLazy(DadosLazy dadosLazy) {
		this.dadosLazy = dadosLazy;
	}

	public Map<String, String> getFilters() {
		return filters;
	}

	public void setFilters(Map<String, String> filters) {
		this.filters = filters;
	}
	
	protected AbstractFiltro<?> clone() {
		AbstractFiltro<?> af = new AbstractFiltro<Identificavel<?>>();
		return af;
	}
	
}
