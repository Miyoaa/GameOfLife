package it.polito.extgol.test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

public class TestDatabaseUtil {

  public static void clearDatabase() {
    EntityManager em = it.polito.extgol.JPAUtil.getEntityManager();
    EntityTransaction tx = em.getTransaction();
    tx.begin();

    em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

    List<String> tables = em.createNativeQuery(
        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'").getResultList();

    for (String table : tables) {
      em.createNativeQuery("TRUNCATE TABLE " + table).executeUpdate();
    }

    em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();

    tx.commit();
    em.close();
  }
}
