package it.polito.extgol.test;

import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Before;
import org.junit.Test;

import it.polito.extgol.Board;
import it.polito.extgol.Cell;
import it.polito.extgol.CellType;
import it.polito.extgol.Coord;
import it.polito.extgol.EventType;
import it.polito.extgol.ExtendedGameOfLife;
import it.polito.extgol.Game;
import it.polito.extgol.Generation;

import static it.polito.extgol.test.TestBranchUtils.assumeBranch;

/**
 * JUnit test suite for the basic GOL
 * 
 */
public class AcceptCombinedTests {

    private ExtendedGameOfLife facade;
    private Game game;
    private Board board;

    /**
     * Close JPA after all tests.
     */
    @AfterClass
    public static void closeDB() {
        //JPAUtil.close()
    }

    /**
     * Prepare a clean database and new game before each test.
     */
    @Before
    public void setUp() {
        TestDatabaseUtil.clearDatabase();
        facade = new ExtendedGameOfLife();
        game  = Game.createExtended("TestGame", 3, 3);
        board = game.getBoard();
    }

 

    @Test
    public void testR1R2CountCellsByTypeExtended() {
        assumeBranch("r1","r2");
        Map<Coord,CellType> types = Map.of(
            new Coord(0,0), CellType.LONER,
            new Coord(1,1), CellType.LONER,
            new Coord(2,2), CellType.SOCIAL,
            new Coord(1,2), CellType.BASIC,
            new Coord(1,0), CellType.HIGHLANDER
        );
        Generation start = Generation.createInitial(game, board, types);
        Map<CellType,Integer> counts = board.countCellsByType(start);
        assertEquals(Integer.valueOf(2), counts.get(CellType.LONER));
        assertEquals(Integer.valueOf(1), counts.get(CellType.SOCIAL));
        assertEquals(Integer.valueOf(1), counts.get(CellType.BASIC));
        assertEquals(Integer.valueOf(1), counts.get(CellType.HIGHLANDER));
    }

    @Test
    public void testR1R2CountCellsByTypeAllDead() {
        assumeBranch("r1","r2");
      
        Generation start = Generation.createInitial(game, board);
        Map<CellType,Integer> counts = board.countCellsByType(start);
        assertEquals(null, counts.get(CellType.LONER));
        assertEquals(null, counts.get(CellType.SOCIAL));
        assertEquals(null, counts.get(CellType.BASIC));
        assertEquals(null, counts.get(CellType.HIGHLANDER));
    }
    

    @Test
    public void testR2R3BloomAndStatistics() {
        assumeBranch("r2", "r3");
        Generation.createInitial(game, game.getBoard(),
            List.of(new Coord(1,1), new Coord(1,2), new Coord(2,1), new Coord(2,2))
        );
        
        board.getTile(new Coord(1,1)).getCell().setLifePoints(2);
        board.getTile(new Coord(2,1)).getCell().setLifePoints(1);

        Game result = facade.run(game, 1, Map.of(0, EventType.BLOOM));

        // --- Second generation: after BLOOM ---
        Generation next = result.getGenerations().get(1);
        Map<Integer,List<Cell>> byEnergy = board.getCellsByEnergyLevel(next);
        assertTrue(byEnergy.containsKey(3));
        assertEquals(2, byEnergy.get(3).size());
        assertTrue(byEnergy.containsKey(5));
        assertEquals(1, byEnergy.get(5).size());
        assertTrue(byEnergy.containsKey(4));
        assertEquals(1, byEnergy.get(4).size());

        Cell highest = board.getHighestEnergyCell(next);
        assertEquals("Highest energy should be at (0,0) but was at "+highest.getCoordinates().getX() +", "+highest.getCoordinates().getY(), new Coord(1,1), highest.getCoordinates());
    }

    @Test
    public void testR2R3CataclysmAndStatistics() {
        assumeBranch("r2", "r3");
        Generation.createInitial(game, game.getBoard(),
            List.of(new Coord(1,1), new Coord(1,2), new Coord(2,1), new Coord(2,2))
        );
        
        board.getTile(new Coord(1,1)).getCell().setLifePoints(2);
        board.getTile(new Coord(2,1)).getCell().setLifePoints(1);

        Game result = facade.run(game, 1, Map.of(0, EventType.CATACLYSM));

        // --- Second generation: after CATACLYSM ---    
        Generation next = result.getGenerations().get(1);
        Map<Integer,List<Cell>> byEnergy = board.getCellsByEnergyLevel(next);
        assertFalse(byEnergy.containsKey(2));
        assertEquals(4, byEnergy.get(1).size());
       
        Cell highest = board.getHighestEnergyCell(next);
        assertEquals("Highest energy should be at (1,1) but was at "+highest.getCoordinates().getX() +", "+highest.getCoordinates().getY(), new Coord(1,1), highest.getCoordinates());
    }


    
}
