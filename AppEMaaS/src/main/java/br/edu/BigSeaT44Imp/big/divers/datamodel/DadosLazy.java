package br.edu.BigSeaT44Imp.big.divers.datamodel;

import java.io.Serializable;

import org.primefaces.model.SortOrder;

public class DadosLazy implements Serializable {

	private static final long serialVersionUID = 3269998314331937924L;

	private Integer firstResult;

	private Integer pageSize;

	private String sortField;

	private SortOrder sortOrder;

	public DadosLazy() {
	}

	public DadosLazy(Integer firstResult, Integer pageSize, String sortField, SortOrder sortOrder) {
		this.firstResult = firstResult;
		this.pageSize = pageSize;
		this.sortField = sortField;
		this.sortOrder = sortOrder;
	}

	public Integer getFirstResult() {
		return firstResult;
	}

	public void setFirstResult(Integer firstResult) {
		this.firstResult = firstResult;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public String getSortField() {
		return sortField;
	}

	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	public SortOrder getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(SortOrder sortOrder) {
		this.sortOrder = sortOrder;
	}
}
