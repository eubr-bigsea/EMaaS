package br.edu.BigSeaT44Imp.big.divers.dao;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.primefaces.model.SortOrder;

import br.edu.BigSeaT44Imp.big.divers.datamodel.DadosLazy;
import br.edu.BigSeaT44Imp.big.divers.filtro.AbstractFiltro;
import br.edu.BigSeaT44Imp.big.divers.filtro.IFiltro;
import br.edu.BigSeaT44Imp.big.divers.model.Identificavel;

@SuppressWarnings("unchecked")
public abstract class AbstractLazyFilterDAO<E extends Identificavel<?>, F extends IFiltro<E>> extends DAO<E> implements
	ILazyFilterDAO<E, F> {

	private static final long serialVersionUID = -3578713491902663379L;

	public AbstractLazyFilterDAO(Class<E> persistentClass) {
		super(persistentClass);
	}

	public AbstractLazyFilterDAO(Class<E> persistentClass, EntityManager em) {
		super(persistentClass);
	}

	@Override
	public List<E> findAllByFields(AbstractFiltro<E> filtroP) throws BigSeaT44PersistenciaException {
		return getEntidades(filtroP.getDadosLazy(), (F) filtroP);
	}

	@Override
	public Integer getTotalRegistros(F filtro) throws BigSeaT44PersistenciaException {
		return ((Long) getEntidades(null, filtro, false)).intValue();
	}

	@Override
	public List<E> getEntidades(DadosLazy dadosLazy, F filtro) throws BigSeaT44PersistenciaException {
		return (List<E>) getEntidades(dadosLazy, filtro, true);
	}

	private Object getEntidades(DadosLazy dadosLazy, F filtro, boolean retornarRegistros) throws BigSeaT44PersistenciaException {
		Object resultado = null;

		// SELECT...
		StringBuilder queryBuilder = null;
		if (retornarRegistros) {
			queryBuilder = new StringBuilder(getClausulaSelectRegistros());
		} else {
			queryBuilder = new StringBuilder(getClausulaSelectNumRegistros());
		}
		queryBuilder.append(" ");

		// JOINS e WHERE...
		Set<String> leftJoins = new TreeSet<String>();
		StringBuilder clausulas = new StringBuilder("WHERE 1 = 1 ");
		Map<String, Object> valores = new HashMap<String, Object>();
		preencherClausulasWhereFiltroColuna(filtro, valores, leftJoins, clausulas);
		preencherClausulasWhere(filtro, valores, leftJoins, clausulas);
		String leftJoinsString = leftJoins.toString().substring(1, leftJoins.toString().length() - 1);
		queryBuilder.append(leftJoinsString);
		queryBuilder.append(clausulas.toString());

		// ORDER BY...
		StringBuilder orderBy = getClausulaOrderBy(dadosLazy);
		queryBuilder.append(orderBy.toString());

		// QUERY...
		TypedQuery<?> query = null;
		if (retornarRegistros) {
			query = getEntityManager().createQuery(queryBuilder.toString(), getPersistentClass());
		} else {
			query = getEntityManager().createQuery(queryBuilder.toString(), Long.class);
		}

		// QUERY PARAMETERS...
		for (Entry<String, Object> entry : valores.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			query.setParameter(name, value);
		}

		// PAGINATION...
		if (dadosLazy != null) {
			if (dadosLazy.getPageSize() != null) {
				query.setMaxResults(dadosLazy.getPageSize());
			}
			if (dadosLazy.getFirstResult() != null) {
				query.setFirstResult(dadosLazy.getFirstResult());
			}
		}

		// RESULT...
		if (retornarRegistros) {
			resultado = query.getResultList();
		} else {
			resultado = query.getSingleResult();
		}

		return resultado;
	}

	/**
	 * OBS: Este filtro de coluna só funciona para campos do tipo String.
	 * 
	 * @param filtro
	 * @param valores
	 * @param leftJoins
	 * @param clausulas
	 */
	private void preencherClausulasWhereFiltroColuna(F filtro, Map<String, Object> valores, Set<String> leftJoins,
		StringBuilder clausulas) {
		
		Map<String, String> filters = filtro.getFilters();
		if (filters != null) {
			for (Entry<String, String> entry : filters.entrySet()) {
				String campo = entry.getKey();
				String valor = entry.getValue();
				appendClausula(clausulas, String.format("%s.%s LIKE :%s_", getAlias(), campo, campo));
				valores.put(campo + "_", "%" + valor + "%");
			}
		}
	}

	protected abstract String getAlias();

	protected abstract void preencherClausulasWhere(F filtro, Map<String, Object> valores, Set<String> leftJoins, StringBuilder clausulas);

	protected abstract String getClausulaSelectRegistros();

	protected abstract String getClausulaSelectNumRegistros();

	protected StringBuilder getClausulaOrderBy(DadosLazy dadosLazy) {
		StringBuilder orderBy = new StringBuilder();
		
		if (dadosLazy != null && dadosLazy.getSortField() != null) {
			orderBy.append(" ORDER BY ");
			String clausulaOrderBy = gerarClausulaOrderBy(dadosLazy.getSortField(), dadosLazy.getSortOrder());
			orderBy.append(clausulaOrderBy);
		}
		
		return orderBy;
	}

	private String gerarClausulaOrderBy(String sortField, SortOrder sortOrder) {
		String sortOrderStr = sortOrder == SortOrder.ASCENDING ? " ASC " : " DESC ";
		
		String res = "";
		String sep = ", ";
		
		try {
			//Necessário para obter o nome da coluna na tabela a partir do atributo JPA da entidade.
			//Funciona apenas se for declarada a anotação "Column" no atributo da entidade
			Field field = getPersistentClass().getDeclaredField(sortField);
			Column annotationCol = field.getAnnotation(Column.class);
			
			//É preciso verificar se essa anotação "Column" foi declarada o nome da coluna da tabela, se não
			//então usar o mesmo nome do sortField
			sortField = annotationCol != null && annotationCol.name() != null && !annotationCol.name().isEmpty() ? 
				annotationCol.name() : sortField;
			
			List<String> sortFieldsStr = Arrays.asList(new String[] { sortField });
			
			for (String sortFieldStr : sortFieldsStr) {
				res += sortFieldStr;
				res += sortOrderStr;
				res += sep;
			}

			res = res.substring(0, res.length() - sep.length());
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
		
		return res;
	}

	protected StringBuilder appendClausula(StringBuilder builder, String clausula) {
		return builder.append("AND ").append(clausula).append(" ");
	}

	protected void preencherClausulasWherePeriodoData(String alias, String atributo, Date dataInicial,
		String dataInicialStr, Date dataFinal, String dataFinalStr, Map<String, Object> valores,
		StringBuilder clausulas) {
		
		String aliasAtributo = alias + "." + atributo;
		if (dataInicial != null) {
			dataInicial = getMenorDataDoDia(dataInicial);
		}
		
		if (dataFinal != null) {
			dataFinal = getMaiorDataDoDia(dataFinal);
		}
		
		if (dataInicial != null && dataFinal != null) {
			appendClausula(clausulas, aliasAtributo + " BETWEEN :" + dataInicialStr + " AND :" + dataFinalStr);
			valores.put(dataInicialStr, dataInicial);
			valores.put(dataFinalStr, dataFinal);
		} else if (dataInicial != null) {
			appendClausula(clausulas, aliasAtributo + " >= :" + dataInicialStr);
			valores.put(dataInicialStr, dataInicial);
		} else if (dataFinal != null) {
			appendClausula(clausulas, aliasAtributo + " <= :" + dataFinalStr);
			valores.put(dataFinalStr, dataFinal);
		}
	}

	private Date getMaiorDataDoDia(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);

		return cal.getTime();
	}

	private Date getMenorDataDoDia(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		return cal.getTime();
	}
}
