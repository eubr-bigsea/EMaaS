package br.edu.BigSeaT44Imp.big.divers.filtro;

import java.util.Map;

import br.edu.BigSeaT44Imp.big.divers.model.Identificavel;

public interface IFiltro<E extends Identificavel<?>> {

	public Map<String, String> getFilters();

	public void setFilters(Map<String, String> filters);
}
