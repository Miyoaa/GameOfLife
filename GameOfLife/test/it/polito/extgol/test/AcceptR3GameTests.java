package it.polito.extgol.test;

import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import it.polito.extgol.Board;
import it.polito.extgol.Cell;
import it.polito.extgol.Coord;
import it.polito.extgol.EventType;
import it.polito.extgol.ExtendedGameOfLife;
import it.polito.extgol.Game;
import it.polito.extgol.Generation;
import it.polito.extgol.JPAUtil;

import static it.polito.extgol.test.TestBranchUtils.assumeBranch;

public class AcceptR3GameTests {
    private ExtendedGameOfLife facade;
    private Game game;
    private Board board;

    /**
     * Close JPA after all tests.
     */
    @AfterClass
    public static void closeDB() {
        JPAUtil.close();
    }

    /**
     * Prepare a clean database and new game before each test.
     */
    @Before
    public void setUp() {
        TestDatabaseUtil.clearDatabase();
        facade = new ExtendedGameOfLife();
        game  = Game.createExtended("TestGame", 6, 6);
        board = game.getBoard();
    }

    // R3 Game Extended Behaviors

    @Test
    public void testR3FamineAndBloom() {
        assumeBranch("R3");
        
        Generation.createInitial(game, board, List.of(
            new Coord(1, 1),
            new Coord(2, 1),
            new Coord(1, 2),
            new Coord(2, 2)));

        Board.setInteractableTile(game.getBoard(), new Coord(1,2),-1);
        board.getTile(new Coord(2, 2)).getCell().setLifePoints(3);
        board.getTile(new Coord(1, 1)).getCell().setLifePoints(3);
        board.getTile(new Coord(2, 1)).getCell().setLifePoints(3);
  
        // Apply FAMINE at gen 0, then evolve one generation, then BLOOM at gen 1 
        Game result = facade.run(game, 2, Map.of(0, EventType.FAMINE, 1, EventType.BLOOM));
        Generation afterFamine=result.getGenerations().get(1);
        Generation afterBloom=result.getGenerations().get(2);
        
        //after FAMINE, (1,2) is dead
        Map<Cell, Integer> lp1=afterFamine.getEnergyStates();
        assertEquals((Integer)(-1), lp1.get(board.getTile(new Coord(1, 2)).getCell()));
        assertFalse(afterFamine.getAliveCells().contains(board.getTile(new Coord(1, 2)).getCell()));

        //after BLOOM (1,2) is born again, but life is 0 (respawn after bloom, so NO +2)
        assertTrue(afterBloom.getAliveCells().contains(board.getTile(new Coord(1, 2)).getCell()));
        Map<Cell, Integer> lp2=afterBloom.getEnergyStates();
        assertEquals((Integer)(0), lp2.get(board.getTile(new Coord(1, 2)).getCell()));
    }

    @Test
    public void testR3EventDontAffectDead() {
        assumeBranch("R3");
        // Initialize with one alive cell and give it some energy
        Generation.createInitial(game, board, List.of(
            new Coord(1, 1),
            new Coord(2, 1),
            new Coord(1, 2),
            new Coord(2, 2)));

        board.getTile(new Coord(2, 2)).getCell().setLifePoints(3);
        board.getTile(new Coord(1, 1)).getCell().setLifePoints(3);
        board.getTile(new Coord(2, 1)).getCell().setLifePoints(3);
  
        
        // Apply BLOOM 
        Game result = facade.run(game, 1, Map.of(0, EventType.BLOOM));
        Generation afterBloom=result.getGenerations().get(1);
        
        //after BLOOM, (1,2) has 2 lifepoints
        Map<Cell, Integer> lp1=afterBloom.getEnergyStates();
        assertEquals((Integer)(3), lp1.get(board.getTile(new Coord(1, 2)).getCell()));
        
        //after BLOOM (0,0) doesn't change lp as it was dead 
        assertEquals((Integer)(0), lp1.get(board.getTile(new Coord(0, 0)).getCell()));
        assertFalse(afterBloom.getAliveCells().contains(board.getTile(new Coord(0, 0)).getCell()));        
    }
}
