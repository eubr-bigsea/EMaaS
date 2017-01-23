package br.edu.BigSeaT44Imp.big.divers.model;

import java.util.Collection;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Entidade cuja manutenção não é de responsabilidade do sistema, sendo utilizada apenas para ser referenciada por
 * outras Entidades.
 * 
 * @author Anderson
 * @version 1.0
 */
@Entity
@Table(name = "TB_USUR")
public class Usuario implements Identificavel<Long>, UserDetails {

	private static final long serialVersionUID = 4000352375162587618L;

	public static final String ROLE_PREFIX = "ROLE_";
	public static final String OP_PREFIX = "OP_";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_USUR")
	private Long id;

	/**
	 * Perfil de acesso do Usuário no sistema
	 */
	@Column(name = "PERFIL")
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "TB_USUARIO_PERFIS", joinColumns = @JoinColumn(name = "ID_USUARIO"))
	@Enumerated(EnumType.STRING)
	private List<Perfil> perfis;
	
	/**
	 * Login do Usuário no sistema
	 */
	@Column(name = "NM_LOGIN_USUR")
	private String login;

	@Transient
	private String password;
	
	@Transient
	private Collection<? extends GrantedAuthority> authorities;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public List<Perfil> getPerfis() {
		return perfis;
	}

	public void setPerfis(List<Perfil> perfis) {
		this.perfis = perfis;
	}

	@Override
	@Transient
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return this.authorities;
	}
	
	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return getLogin();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isRoot() {
		return getPerfis() != null && getPerfis().contains(Perfil.ADM);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((login == null) ? 0 : login.hashCode());
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Usuario other = (Usuario) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (login == null) {
			if (other.login != null)
				return false;
		} else if (!login.equals(other.login))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		return true;
	}
}
