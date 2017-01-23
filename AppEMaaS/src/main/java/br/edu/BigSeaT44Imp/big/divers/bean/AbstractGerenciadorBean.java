package br.edu.BigSeaT44Imp.big.divers.bean;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.BigSeaT44Imp.big.divers.filtro.AbstractFiltro;
import br.edu.BigSeaT44Imp.big.divers.model.Identificavel;
import br.edu.BigSeaT44Imp.big.divers.service.BigSeaT44ServiceException;
import br.edu.BigSeaT44Imp.big.divers.service.GenericService;

public abstract class AbstractGerenciadorBean<E extends Identificavel<?>> extends AbstractBean {

	static final Logger LOGGER = LoggerFactory.getLogger(AbstractGerenciadorBean.class);

//	private E entidade;

	public AbstractGerenciadorBean() {
	}

	protected abstract GenericService<E> getService();

	public abstract AbstractFiltro<E> getFiltro();

	public abstract void setFiltro(AbstractFiltro<E> filtro);

	public abstract void filtrar();

	public abstract E getEntidade();

	public abstract void setEntidade(E entidade);

	public String persist() {
		if (getEntidade() == null) {
			reportMessageError("Erro ao persistir entidade");
			return null;
		}
		try {
			if (getEntidade().getId() == null) {
				LOGGER.info("MBEAN: Tentando criar uma entidade {}.", getClass().getCanonicalName());
				
				insert();
				reportMessageSuccessful("Entidade adicionada.");
				
				return getPaginaGerenciador();
			} else {
				LOGGER.info("MBEAN: Tentando atualizar uma entidade {} de ID = {}.", getClass().getCanonicalName(),
						getEntidade().getId());
				update();
				reportMessageSuccessful("Entidade atualizada.");
				setEntidade(null);
				
				return getPaginaGerenciador();
			}
		} catch(BigSeaT44PermissaoException e) {
			reportMessageErrorAutorization(e.getMessage());
			
			return null;
		} catch (Exception e) {
			LOGGER.error("MBEAN: Uma exceção ocorreu ao tentar persistir uma entidade", e);
			reportMessageError(e.getMessage());
			
			return null;
		}
	}

	protected void insert() throws BigSeaT44ServiceException, BigSeaT44BeanException, BigSeaT44PermissaoException {
		preInsert();
		E novoObj = getEntidade();
		getService().persistir(novoObj);
		posInsert();
	}

	protected void preInsert() {
		// Implementar caso necessário a execução de algum procedimento ANTES do insert
	}

	protected void posInsert() {
		// Implementar caso necessário a execução de algum procedimento DEPOIS do insert
	}

	protected void update() throws BigSeaT44ServiceException, BigSeaT44BeanException, BigSeaT44PermissaoException {
		preUpdate();
		getService().update(getEntidade());
		posUpdate();
	}

	protected void preUpdate() {
		// Implementar caso necessário a execução de algum procedimento ANTES do update
	}

	protected void posUpdate() {
		// Implementar caso necessário a execução de algum procedimento DEPOIS do update
	}

	public void remove() {
		if (getEntidade() == null) {
			reportMessageError("Nenhuma linha selecionada");
		} else {
			try {
				
				LOGGER.info("MBEAN: Tentando remover uma entidade {} de ID = {}.", getClass().getCanonicalName(),
					getEntidade().getId());
				getService().remove(getEntidade());
				reportMessageSuccessful("Entidade removida.");
			} catch (BigSeaT44ServiceException e) {
				LOGGER.error("MBEAN: Uma exceção ocorreu ao tentar remover uma entidade", e);
				reportMessageError(e.getMessage());
			}
			setEntidade(null);
		}
	}

	public void editarEntidade() {
		LOGGER.info("MBEAN: Acessando a página de edição da entidade {} de ID = {}.", getClass().getCanonicalName(),
			getEntidade().getId());

		if (getEntidade() == null) {
			reportMessageError("Nenhuma entidade selecionada");
		} else {
			try {
				setEntidade(getService().getById((Serializable) getEntidade().getId()));
			} catch (BigSeaT44ServiceException e) {
				LOGGER.error("MBEAN: Uma exceção ocorreu ao tentar editar uma entidade", e);
				reportMessageError(e.getMessage());
			}
//			setEntidade(null);

			preparaEdicaoEntidade();
		}
	}

	public abstract void criarEntidade();

	public abstract String getPaginaGerenciador();

	public abstract String getPaginaCriar();

	public abstract String getPaginaEditar();

	public abstract String getPaginaDetalhar();

	public abstract String getPaginaGerenciadorLimpandoBusca();

	protected void preparaEdicaoEntidade() { }

	protected void carregarListasParaLookup() throws BigSeaT44ServiceException { }

	@Override
	protected void reportMessage(boolean isErro, String detalhe) {
		String tipo = "Sucesso!";
		Severity severity = FacesMessage.SEVERITY_INFO;
		if (isErro) {
			tipo = "Erro!";
			severity = FacesMessage.SEVERITY_ERROR;
			FacesContext.getCurrentInstance().validationFailed();
		}

		FacesMessage msg = new FacesMessage(severity, tipo, detalhe);
		FacesContext instance = FacesContext.getCurrentInstance();
		instance.addMessage(null, msg);
		ExternalContext externalContext = instance.getExternalContext();
		externalContext.getFlash().setKeepMessages(true);
		LOGGER.info( "MBEAN: [{}] [{}] Mensagem: \"{}\".", getClass().getCanonicalName(), tipo, detalhe);
	}
}
