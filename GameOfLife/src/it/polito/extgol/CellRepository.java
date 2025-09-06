package it.polito.extgol;
import jakarta.persistence.EntityManager;

//Repository for persisting and loading Cell entities.
public class CellRepository extends GenericExtGOLRepository<Cell, Long> {

    public CellRepository() {
        super(Cell.class);
    }

    /**
     * Loads a Cell from the database by its primary key.
     *
     * @param id the identifier of the Cell to load
     * @return the managed Cell instance, or null if not found
     */
    public Cell load(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Cell.class, id);
        } finally {
            em.close();
        }
    }

    /**
     * Obtain an EntityManager from the JPAUtil.
     *
     * @return a new EntityManager instance
     */
    protected EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }

}

