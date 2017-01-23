package br.edu.BigSeaT44Imp.big.divers.bean;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.FacesContext;

@SuppressWarnings("unchecked")
public abstract class AbstractBean {

	public static <T> T findBean(String beanName) {
		FacesContext context = FacesContext.getCurrentInstance();
		return (T) context.getApplication().evaluateExpressionGet(context, "#{" + beanName + "}", Object.class);
	}

	protected void reportMessageError(String detail) {
		reportMessage(true, detail);
	}

	protected void reportMessageSuccessful(String detail) {
		reportMessage(false, detail);
	}

	protected void reportMessage(boolean isError, String detail) {
		String type = "Success!";
		Severity severity = FacesMessage.SEVERITY_INFO;
		if (isError) {
			type = "Error!";
			severity = FacesMessage.SEVERITY_ERROR;
			FacesContext.getCurrentInstance().validationFailed();
		}

		FacesMessage msg = new FacesMessage(severity, type, detail);
		FacesContext instance = FacesContext.getCurrentInstance();
		instance.addMessage(null, msg);
	}
	
	protected void reportMessageErrorAutorization(String detail) {
		String type = "Access Denied!";
		Severity severity = FacesMessage.SEVERITY_WARN;
		FacesContext.getCurrentInstance().validationFailed();

		FacesMessage msg = new FacesMessage(severity, type, detail);
		FacesContext instance = FacesContext.getCurrentInstance();
		instance.addMessage(null, msg);
	}
	
	public String checkPath(String name){
		String result;
		if(name.contains("\\")){
			result = name.replaceAll("\\\\", "/");	
		}else{
			result = name;
		}
		
		if(!result.endsWith("/")){
			result += "/";
		}
		return result;
	}
	
	public String convertSpace(String file){
		return file.replaceAll("[\n]", "</br>");
	}
}
