package br.edu.BigSeaT44Imp.big.divers.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import br.edu.BigSeaT44Imp.big.divers.filtro.AbstractFiltro;
import br.edu.BigSeaT44Imp.big.divers.model.Identificavel;

public class DAO<T extends Identificavel<? extends Serializable>> implements Serializable {
	private static final long serialVersionUID = 8917430907072776566L;

	@PersistenceContext
	private EntityManager em;

	private Class<T> persistentClass;

	public DAO(Class<T> persistentClass) {
		this.persistentClass = persistentClass;
	}
	
	public DAO(Class<T> persistentClass, EntityManager em) {
		this.persistentClass = persistentClass;
		this.em = em;
	}

	public EntityManager getEntityManager() {
		return em;
	}

	protected Class<T> getPersistentClass() {
		return persistentClass;
	}

	public T find(Serializable id) throws BigSeaT44PersistenciaException {
		try {
			T result = em.find(persistentClass, id);

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BigSeaT44PersistenciaException("Objeto não pode ser recuperado.", e);
		}
	}

	public T findByField(String field, Object value) throws BigSeaT44PersistenciaException {
		try {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<T> criteria = builder.createQuery(persistentClass);

			Root<T> root = criteria.from(persistentClass);

			Predicate conjunction = builder.conjunction();
			conjunction = builder.and(conjunction, builder.equal(root.<T>get(field), value));

			criteria.where(conjunction);

			T result = em.createQuery(criteria).getSingleResult();

			return result;
		} catch (NoResultException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BigSeaT44PersistenciaException("As informações não puderam ser recuperadas.", e);
		}
	}

	public List<T> findAllByField(String field, Object value) throws BigSeaT44PersistenciaException {
		try {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<T> criteria = builder.createQuery(persistentClass);
			Root<T> root = criteria.from(persistentClass);

			Predicate conjunction = builder.conjunction();
			conjunction = builder.and(conjunction, builder.equal(root.<T>get(field), value));

			criteria.where(conjunction);

			List<T> result = em.createQuery(criteria).getResultList();

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BigSeaT44PersistenciaException("As informações não puderam ser recuperadas.", e);
		}
	}
	
	public List<T> findAllByFields(AbstractFiltro<T> filtro) throws BigSeaT44PersistenciaException {
		throw new UnsupportedOperationException(
			"O DAO da entidade nao sobrescreveu este metodo ou nao esta sendo usado um DAO especifico.");
	}
	
	@SuppressWarnings("unchecked")
	protected List<T> findAllByFieldIsNotEmpty(String field) throws BigSeaT44PersistenciaException {
		List<T> resultado = null;
		try {
			TypedQuery<Object> query = getEntityManager().createQuery(
				"SELECT e FROM " + persistentClass.getCanonicalName() + " e WHERE e." + field + " IS NOT EMPTY", Object.class);
			List<Object> list = query.getResultList();
			resultado = list.isEmpty() ? new ArrayList<T>() : (List<T>) (Object) list;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BigSeaT44PersistenciaException("A lista de Objetos não pode ser recuperada.", e);
		}
		return resultado;
	}
	
	@SuppressWarnings("unchecked")
	protected List<T> findAllByFieldIsNotNull(String field) throws BigSeaT44PersistenciaException {
		List<T> resultado = null;
		try {
			TypedQuery<Object> query = getEntityManager().createQuery(
				"SELECT e FROM " + persistentClass.getCanonicalName() + " e WHERE e." + field + " IS NOT NULL", Object.class);
			List<Object> list = query.getResultList();
			resultado = list.isEmpty() ? new ArrayList<T>() : (List<T>) (Object) list;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BigSeaT44PersistenciaException("A lista de Objetos não pode ser recuperada.", e);
		}

		return resultado;
	}

	public void save(T t) throws BigSeaT44PersistenciaException {
		try {
			checarRestricoesNoSave(t);
			em.persist(t);
		} catch (BigSeaT44PersistenciaRestricaoException exRestricao) {
			exRestricao.printStackTrace();
			throw exRestricao;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BigSeaT44PersistenciaException("Ocorreu um erro ao tentar salvar a entidade", e);
		}
	}

	protected void checarRestricoesNoSave(T objeto) throws BigSeaT44PersistenciaException {
		// fazer nada
	}

	@SuppressWarnings("unchecked")
	public void remove(T t) throws BigSeaT44PersistenciaException {
		try {
			T objParaRemover = em.getReference(persistentClass, ((Identificavel<Serializable>) t).getId());
			checarRestricoesNoRemove(objParaRemover);
			em.remove(objParaRemover);
		} catch (BigSeaT44PersistenciaRestricaoException exRestricao) {
			exRestricao.printStackTrace();
			throw exRestricao;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BigSeaT44PersistenciaException("Ocorreu um erro ao tentar remover a entidade", e);
		}
	}

	protected void checarRestricoesNoRemove(T objeto) throws BigSeaT44PersistenciaRestricaoException {
		// fazer nada
	}

	public T update(T t) throws BigSeaT44PersistenciaException {
		try {
			checarRestricoesNoUpdate(t);
			T obj = em.merge(t);

			return obj;
		} catch (BigSeaT44PersistenciaRestricaoException exRestricao) {
			exRestricao.printStackTrace();
			throw exRestricao;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BigSeaT44PersistenciaException("Ocorreu um erro ao tentar atualizar a entidade", e);
		}
	}

	protected void checarRestricoesNoUpdate(T objeto) throws BigSeaT44PersistenciaException {
		// fazer nada
	}

	public List<T> findAll() throws BigSeaT44PersistenciaException {
		try {
			TypedQuery<T> query = em.createQuery("FROM " + persistentClass.getName(), persistentClass);
			List<T> list = query.getResultList();
			for (T t : list) {
				em.detach(t);
			}

			return list;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BigSeaT44PersistenciaException("As informações não puderam ser recuperadas.", e);
		}
	}

	public void refresh(T t) throws BigSeaT44PersistenciaException {
		try {
			em.refresh(t);
		} catch (Exception e) {
			e.printStackTrace();
			throw new BigSeaT44PersistenciaException("As informações não puderam ser recuperadas.", e);
		}
	}

	@SuppressWarnings("unused")
	private Predicate getConjunction(Root<T> root, CriteriaBuilder builder, Map<String, Object> map) {
		Predicate conjunction = builder.conjunction();
	
		return conjunction;
	}

	protected <E extends Serializable> E getId(Identificavel<E> identificavel) {
		if (identificavel == null) {
			return null;
		}
		return identificavel.getId();
	}

	protected Long contaDependenciasList(Identificavel<?> obj, String nomeEntidade, String nomeAtributo) throws BigSeaT44PersistenciaException {
		Long resultado = null;

		try {
			TypedQuery<Long> typedQuery = getEntityManager().createQuery(
				"select count(*) from " + nomeEntidade + " ent where :obj member of ent." + nomeAtributo, Long.class);
			typedQuery.setParameter("obj", obj);
			resultado = typedQuery.getSingleResult();
		} catch (Exception e) {
			e.printStackTrace();
			throw new BigSeaT44PersistenciaException("As informações não puderam ser recuperadas.", e);
		}

		return resultado;
	}

	protected Long contaDependencias(Serializable id, String nomeEntidade, String nomeAtributo) throws BigSeaT44PersistenciaException {
		Long resultado = null;

		try {
			TypedQuery<Long> typedQuery = getEntityManager().createQuery(
				"select count(*) from " + nomeEntidade + " ent where ent." + nomeAtributo + ".id = :id", Long.class);
			typedQuery.setParameter("id", id);
			resultado = typedQuery.getSingleResult();
		} catch (Exception e) {
			e.printStackTrace();
			throw new BigSeaT44PersistenciaException("As informações não puderam ser recuperadas.", e);
		}

		return resultado;
	}
}
