package br.edu.BigSeaT44Imp.big.divers.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.edu.BigSeaT44Imp.big.divers.model.Usuario;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class UsuarioService implements Serializable, UserDetailsService {
	private static final long serialVersionUID = 5411162027754217045L;

	private List<Usuario> usuarios;

	public UsuarioService() {}
	
	
	
	

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		carregaUsuarios();
		return findByUsername(username);
	}


	private void carregaUsuarios() {
		String[] usuariosArray = {"admin","secretario","aluno"};
		if (usuarios == null) {
			usuarios = new ArrayList<Usuario>();
			for(int i=0; i < usuariosArray.length; i++){
				Usuario u = new Usuario();
				u.setId(new Long(i));
				u.setLogin(usuariosArray[i]);
				u.setPassword("202cb962ac59075b964b07152d234b70");
				usuarios.add(u);
			}
		
			
		}
		
	}





	private Usuario findByUsername(String username) {
		
		for(Usuario user: usuarios){
			if(user.getLogin().equalsIgnoreCase(username)){
				return user;
			}
		}
		throw new UsernameNotFoundException("Usuário não localizado");
	}
	
	
}