package it.polito.extgol;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Utility class for managing the JPA EntityManagerFactory and
 * providing EntityManager instances for database operations.
 *
 * Uses a singleton EntityManagerFactory tied to the persistence unit
 * "game-of-life-pu" to create short-lived EntityManager instances.
 */
public class JPAUtil {

    /** The singleton EntityManagerFactory. */
    private static EntityManagerFactory emf;

    /** The name of the persistence unit defined in persistence.xml. */
    private static final String PU_NAME = "game-of-life-pu";

    private JPAUtil() {} // utility class need not to be instantiated
    
    /**
     * Lazily initializes (if necessary) and returns the shared EntityManagerFactory.
     *
     * @return the open EntityManagerFactory for the configured persistence unit
     */
    private static EntityManagerFactory getCurrentFactory() {
        if (emf == null || !emf.isOpen()) {
            emf = Persistence.createEntityManagerFactory(PU_NAME);
        }
        return emf;
    }

    /**
     * Creates and returns a new EntityManager from the shared factory.
     *
     * Clients should obtain an EntityManager using this method, use it for
     * transactions or queries, and then close it when done.
     *
     * @return a new EntityManager instance
     */
    public static EntityManager getEntityManager() {
        return getCurrentFactory().createEntityManager();
    }

    /**
     * Closes the shared EntityManagerFactory if it is open.
     *
     * Should be invoked during application shutdown to release resources.
     */
    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}