package br.edu.BigSeaT44Imp.big.divers.bean;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import br.edu.BigSeaT44Imp.big.divers.filtro.SessionContext;
import br.edu.BigSeaT44Imp.big.divers.model.Usuario;
import br.edu.BigSeaT44Imp.big.divers.util.Paginas;

@Component
@Scope("session")
@SessionScoped
public class LoginBean implements Serializable  {
	private static final long serialVersionUID = 2090132113208542049L;

	/** The user name. */
	private String username = null;

	/** The password. */
	private String password = null;

	@ManagedProperty(value = "#{authenticationManager}")
	@Autowired
	private AuthenticationManager authenticationManager;

	/**
	 * Logged User
	 */
	private Usuario loggedUser;
	
	private boolean timeout = false;
	
	/**
	 * Login.
	 * 
	 * @return the string
	 */
	public String login() {
		try {
			Authentication request = new UsernamePasswordAuthenticationToken(getUsername(), getPassword());
			Authentication result = authenticationManager.authenticate(request);
			Usuario user = (Usuario)result.getPrincipal();
			this.setUsuarioLogado(user);
//			SecurityContextHolder.getContext().setAuthentication(result);
			SessionContext.getInstance().setAttribute("loggedUser", user);
		} catch (Exception e) {
			e.printStackTrace();
			reportarMensagemDeErro("User ou password is invalid");

			return null;
		}
		return Paginas.INDEX;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	private void reportarMensagemDeErro(String detalhe) {
		FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Authentication error", detalhe);
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}

	public String logout() {
//		FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
	
		SessionContext.getInstance().encerrarSessao();
		return Paginas.LOGIN;
	}

	/**
	 * Cancel.
	 * 
	 * @return the string
	 */
	public String cancel() {
		return null;
	}

	public AuthenticationManager getAuthenticationManager() {
		return authenticationManager;
	}

	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	/**
	 * Gets the password.
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the password.
	 * 
	 * @param password the new password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	public static Usuario getLoggedUser() {
		return (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}

	public Usuario getUsuarioLogado() {
		return loggedUser;
	}

	public void setUsuarioLogado(Usuario loggedUser) {
		this.loggedUser = loggedUser;
	}

	public void timeout() throws Exception {
		logout();
		FacesContext.getCurrentInstance().getExternalContext().redirect("../login.xhtml");
		setTimeout(true);
	}

	public void messageTimeout() {
		if (isTimeout()) {
			reportarMensagemDeErroTimeOut("Session closed!");
			setTimeout(false);
		}
	}
	
	private void reportarMensagemDeErroTimeOut(String detail) {
		FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Downtime reached", detail);
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}

	public boolean isTimeout() {
		return timeout;
	}

	public void setTimeout(boolean timeout) {
		this.timeout = timeout;
	}
}
