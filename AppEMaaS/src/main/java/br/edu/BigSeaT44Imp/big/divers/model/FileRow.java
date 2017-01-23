package br.edu.BigSeaT44Imp.big.divers.model;

import java.io.Serializable;

public class FileRow implements Serializable{

	private static final long serialVersionUID = 1L;
	private String row;
	
	public FileRow(String row) {
		if (row == null) {
			throw new NullPointerException();
		}
		this.row = row;
	}

	public String getRow() {
		return row;
	}

	public void setRow(String row) {
		this.row = row;
	}	
}
