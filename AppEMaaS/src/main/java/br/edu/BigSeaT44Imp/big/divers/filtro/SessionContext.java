package br.edu.BigSeaT44Imp.big.divers.filtro;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import br.edu.BigSeaT44Imp.big.divers.model.Usuario;

public class SessionContext {
	
	private static SessionContext instance;
    
    public static SessionContext getInstance(){
         if (instance == null){
             instance = new SessionContext();
         }
         
         return instance;
    }
    
    private SessionContext(){
    }
    
    private ExternalContext currentExternalContext() {
         if (FacesContext.getCurrentInstance() == null){
             throw new RuntimeException("FacesContext cannot be called out of an HTTP request.");
         }else{
             return FacesContext.getCurrentInstance().getExternalContext();
         }
    }
    
    public Usuario getUsuarioLogado(){
         return (Usuario) getAttribute("loggedUser");
    }
    
    public void setUsuarioLogado(Usuario usuario) {
         setAttribute("loggedUser", usuario);
    }
    
    public void encerrarSessao(){   
         currentExternalContext().invalidateSession();
    }
    
    public Object getAttribute(String nome){
         return currentExternalContext().getSessionMap().get(nome);
    }
    
    public void setAttribute(String nome, Object valor){
         currentExternalContext().getSessionMap().put(nome, valor);
    }
}