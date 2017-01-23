package br.edu.BigSeaT44Imp.big.divers.converter;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import br.edu.BigSeaT44Imp.big.divers.dao.DAO;
import br.edu.BigSeaT44Imp.big.divers.model.Identificavel;

public abstract class ConversorGenerico<T extends Identificavel<?>> implements Converter, Serializable {
	private static final long serialVersionUID = -4827845879166774837L;

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		if (value == null || value.trim().equals("")) {
			return null;
		}

		T identificavel = null;

		try {
			identificavel = getDao().find(Long.parseLong(value));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (identificavel == null) {
			throw new ConverterException(new FacesMessage("Identificacao desconhecida:  " + value));
		}

		return identificavel;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value == null || value.equals("")) {
			return "";
		} else {
			Identificavel<?> i = (Identificavel<?>) value;
			return String.valueOf(i.getId());
		}
	}
	
	public abstract DAO<T> getDao();
}
