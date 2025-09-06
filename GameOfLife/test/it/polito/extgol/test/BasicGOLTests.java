package it.polito.extgol.test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import it.polito.extgol.Board;
import it.polito.extgol.Cell;
import it.polito.extgol.Coord;
import it.polito.extgol.ExtendedGameOfLife;
import it.polito.extgol.Game;
import it.polito.extgol.Generation;
import it.polito.extgol.JPAUtil;
/**
 * JUnit test suite for the basic GOL
 * 
 */
public class BasicGOLTests {

    private ExtendedGameOfLife facade;
    private Game game;
    private Board board;

    /**
     * Set up a fresh database and game before each test.
     */
    @Before
    public void setUp() {
        TestDatabaseUtil.clearDatabase();
        facade = new ExtendedGameOfLife();
        game  = Game.create("TestGame", 3, 3);
        board = game.getBoard();
    }
     
    /**
     * Close JPA resources after all tests.
     */
    @AfterClass
    public static void closeDB(){
        JPAUtil.close();
    }

    @Test
    public void testRunZeroSteps() {
        Game result = facade.run(game, 0);
        assertEquals(1, result.getGenerations().size());
        assertSame(result.getStart(), result.getGenerations().get(0));
    }

    @Test
    public void testSingleCellDiesViaRun() {
        game.getStart();
        Cell lone = board.getTile(new Coord(1,1)).getCell();
        lone.setAlive(true);

        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);
        assertTrue("Lone cell should die", next.getAliveCells().isEmpty());
    }

    @Test
    public void testBlockStillLifeViaRun() {
        List<Coord> coords = List.of(
            new Coord(1, 1),
            new Coord(2, 1),
            new Coord(1, 2),
            new Coord(2, 2)
        );
        Generation.createInitial(game, board, coords);
        
        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);

        assertEquals(4, next.getAliveCells().size());
        Map<Coord,Cell> alive = facade.getAliveCells(next);
        for (Coord c : coords) {
            assertTrue("Cell at "+c.getX()+","+c.getY()+" should survive",
                       alive.containsKey(c));
        }
    }

    @Test
    public void testBlinkerOscillatorViaRun() {
        List<Coord> coords = List.of(
            new Coord(0, 1),
            new Coord(1, 1),
            new Coord(2, 1)
        );
        Generation.createInitial(game, board, coords);

        Game one = facade.run(game, 2);
        Generation g1 = one.getGenerations().get(1);
        Map<Coord,Cell> alive1 = facade.getAliveCells(g1);
        assertEquals(3, alive1.size());
        assertTrue(alive1.containsKey(new Coord(1, 0)));
        assertTrue(alive1.containsKey(new Coord(1,1)));
        assertTrue(alive1.containsKey(new Coord(1,2)));
        
        Generation g2 = one.getGenerations().get(2);
        Map<Coord,Cell> alive2 = facade.getAliveCells(g2);
        assertEquals(3, alive2.size());
        assertTrue(alive2.containsKey(new Coord(0, 1)));
        assertTrue(alive2.containsKey(new Coord(1, 1)));
        assertTrue(alive2.containsKey(new Coord(2, 1)));
    }

    @Test
    public void testCornerBirth() {
        List<Coord> coords = List.of(
            new Coord(0, 1),
            new Coord(1, 0),
            new Coord(1, 1)
        );
        Generation.createInitial(game, board, coords);
        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);
        Map<Coord,Cell> alive = facade.getAliveCells(next);
        assertTrue("Cell should be born at (0,0)", alive.containsKey(new Coord(0,0)));
    }

    @Test
    public void testInvalidGenerationThrows() {
        assertThrows("Cannot create an initial generation with null game or board",
           Exception.class,
                     ()-> Generation.createInitial(null, null, new LinkedList<>()));
    }

    @Test
    public void testCellSurvival() {
        List<Coord> coords = List.of(
            new Coord(1, 1),
            new Coord(1, 2),
            new Coord(2, 1)
        );
        Generation.createInitial(game, board, coords);
        Cell c1 = board.getTile(new Coord(1, 1)).getCell();
        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);
        Cell c2 = facade.getAliveCells(next).get(new Coord(1,1));
        assertSame("Cell instance should be preserved", c1, c2);
    }

    @Test
    public void testVisualizeEmptyBoard() {
        String viz = board.visualize(game.getStart());
        String expected = String.join(System.lineSeparator(),
                "000",
                "000",
                "000");
        assertEquals(expected, viz);
    }

    @Test
    public void testVisualizeBlock() {
        List<Coord> coords = List.of(
            new Coord(1, 1),
            new Coord(1, 2),
            new Coord(2, 1),
            new Coord(2, 2)
        );
        Generation start = Generation.createInitial(game, board, coords);
        String viz = board.visualize(start);
        String expected = String.join(System.lineSeparator(),
            "000",   
            "0CC",   
            "0CC"    
        );
        assertEquals(expected, viz);
    }
    
    @Test
    public void testGliderMovesCorrectlyOn10x10() {
        // Setup a 10x10 game
        Game bigGame = Game.create("BigGame", 10, 10);
        ExtendedGameOfLife bigFacade = new ExtendedGameOfLife();
        
        // Place a glider at top-left: cells at (1,0),(2,1),(0,2),(1,2),(2,2)
        List<Coord> coords = List.of(
            new Coord(1, 0),
            new Coord(2, 1),
            new Coord(0, 2),
            new Coord(1, 2),
            new Coord(2, 2)
        );
        Generation.createInitial(bigGame, bigGame.getBoard(), coords);
        // Run 4 steps: glider should have moved down-right by (1,1)
        Game after4 = bigFacade.run(bigGame, 4);
        Generation g4 = after4.getGenerations().get(4);
        Map<Coord,Cell> alive4 = bigFacade.getAliveCells(g4);
        // Expected glider positions: shift each by +1,+1
        for (Coord c : coords) {
            Coord key = new Coord(c.getX()+1, c.getY()+1);
            assertTrue("Glider cell should be at " + key, alive4.containsKey(key));
        }
        
    }

    @Test
    public void testOn10x10MultipleSteps() { 
        Game bigGame = Game.create("PerfGame", 10, 10);
        ExtendedGameOfLife bigFacade = new ExtendedGameOfLife();
        
        // Seeds in corners
        List<Coord> coords = List.of(
            new Coord(0, 0),
            new Coord(9, 0),
            new Coord(0, 9),
            new Coord(9, 9),
            new Coord(5, 5)
        );
        Generation.createInitial(bigGame, bigGame.getBoard(), coords);

        // Run 20 steps and ensure no exceptions and non-negative generation size
        Game run = bigFacade.run(bigGame, 20);

        assertEquals("Game should have 21 generations (initial+20)", 21, run.getGenerations().size());
        // Check last generation cell count is >= 0 and <= 100
        Generation last = run.getGenerations().get(20);
        int count = last.getAliveCells().size();
        assertTrue("Alive cell count should be between 0 and 100", count >= 0 && count <= 100);
    }

    /**
     * Verifies time-based evolution on a 10x10 board: 20 steps produce 21 generations,
     * with sequential steps.
     */
    @Test
    public void testTimeBasedEvolution20GenerationsOn10x10() {
        // Initialize a 10x10 game with no initial live cells
        Game timeGame = Game.create("TimeTest10", 10, 10);
        // Run 20 generations
        Game timeResult = facade.run(timeGame, 20);
        // Expect step 0 through step 20 => 21 generations
        assertEquals("Should have 21 generations on 10x10", 21, timeResult.getGenerations().size());
        
        for (int i = 0; i < timeResult.getGenerations().size(); i++) {
            Generation gen = timeResult.getGenerations().get(i);
            // Step index should match iteration
            assertEquals("Generation step index on 10x10", i, gen.getStep());
            // Since no initial cells, all generations remain empty
            assertTrue("Generation " + i + " should have no live cells on empty board", gen.getAliveCells().isEmpty());
        }
    }
}
