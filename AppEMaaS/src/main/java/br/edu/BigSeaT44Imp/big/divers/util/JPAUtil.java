package br.edu.BigSeaT44Imp.big.divers.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JPAUtil {
	private static EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistence_CA");

	public EntityManager getEntityManager() {
		return emf.createEntityManager();
	}
}