package it.polito.extgol;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

/**
 * Repository for persisting and loading Board entities.
 */
public class BoardRepository extends GenericExtGOLRepository<Board, Integer> {

    public BoardRepository() {
        super(Board.class);
    }

    /**
     * Loads a Board by ID, eagerly fetching tiles and their associated cells.
     *
     * @param id the primary key of the board
     * @return the Board entity, or null if not found
     */
    public Board load(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                "SELECT DISTINCT b FROM Board b " +
                "LEFT JOIN FETCH b.tiles t " +
                "LEFT JOIN FETCH t.cell " +
                "WHERE b.id = :id", Board.class)
                .setParameter("id", id)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    /**
     * Obtain an EntityManager from the JPA utility.
     *
     * @return EntityManager instance
     */
    protected EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }
}