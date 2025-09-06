package it.polito.extgol;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class GameRepository extends GenericExtGOLRepository<Game, Long>{
    public GameRepository (){
        super(Game.class);
    }

    public Game load(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT DISTINCT g FROM Game g " +
                "LEFT JOIN FETCH g.board b " +
                "LEFT JOIN FETCH b.tiles t " +
                "LEFT JOIN FETCH t.cell " +
                "LEFT JOIN FETCH g.generations gen " +
                "LEFT JOIN FETCH gen.cellAlivenessStates cas " +
                "LEFT JOIN FETCH g.eventSchedule es " +
                "WHERE g.id = :id",
                Game.class
            )
            .setParameter("id", id)
            .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    /*
     * reading all of relavent entity and events from database 
    */
    }

}