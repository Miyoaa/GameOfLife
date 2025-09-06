package it.polito.extgol;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

/**
 * A generic JPA repository providing basic operations for any
 * entity type annotated with @Entity. Subclasses must supply the
 * specific entity class to enable runtime operations.
 *
 * @param <E> the type of the JPA entity
 * @param <I> the type of the entity’s identifier
 */
public class GenericExtGOLRepository<E, I> {

    private final Class<E> entityClass;
    protected final String entityName;

    /**
     * Constructs a repository for the given entity class.
     *
     * @param entityClass the Class object for the entity type; must be non-null
     * @throws NullPointerException     if entityClass is null
     * @throws IllegalArgumentException if the class is not annotated with @Entity
     */
    protected GenericExtGOLRepository(Class<E> entityClass) {
        Objects.requireNonNull(entityClass, "Entity class must not be null");
        this.entityClass = entityClass;
        this.entityName = getEntityName(entityClass);
    }

    /**
     * Resolves the JPA entity name for use in queries.
     * 
     * @param entityClass the Class to inspect for @Entity
     * @return the entity name, either the annotation’s name or the simple class name
     * @throws IllegalArgumentException if the class is not annotated as @Entity
     */
    protected static String getEntityName(Class<?> entityClass) {
        Entity annotation = entityClass.getAnnotation(Entity.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                "Class " + entityClass.getName() + " must be annotated as @Entity");
        }
        String name = annotation.name();
        return name.isEmpty() ? entityClass.getSimpleName() : name;
    }

    /**
     * Finds an entity instance by its identifier.
     *
     * @param id the primary key of the entity to retrieve
     * @return an Optional containing the found entity, or empty if not found
     */
    public Optional<E> findById(I id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            E entity = em.find(entityClass, id);
            return Optional.ofNullable(entity);
        } finally {
            em.close();
        }
    }

    /**
     * Retrieves all instances of the entity type from the database.
     *
     * @return a List containing all persisted entities of this type
     */
    public List<E> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT e FROM " + entityName + " e", entityClass)
                     .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Persists a new entity instance to the database.
     *
     * @param entity the entity to be created and managed
     * @throws RuntimeException if the transaction fails
     */
    public void create(E entity) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(entity);
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    /**
     * Merges changes of a detached entity into the current persistence context.
     *
     * @param entity the modified entity to synchronize with the database
     * @throws RuntimeException if the transaction fails
     */
    public void update(E entity) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(entity);
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    /**
     * Deletes an entity instance from the database.
     *
     * @param entity the entity to remove
     * @throws RuntimeException if the transaction fails
     */
    public void delete(E entity) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            E managed = em.contains(entity) ? entity : em.merge(entity);
            em.remove(managed);
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }
}