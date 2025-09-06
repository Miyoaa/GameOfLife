package it.polito.extgol.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import it.polito.extgol.Board;
import it.polito.extgol.Cell;
import it.polito.extgol.CellMood;
import it.polito.extgol.CellType;
import it.polito.extgol.Coord;
import it.polito.extgol.ExtendedGameOfLife;
import it.polito.extgol.Game;
import it.polito.extgol.Generation;
import static it.polito.extgol.test.TestBranchUtils.assumeBranch;

public class ExtGOLR1CellsTests {
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
    // R1 Cells Extended Behaviors

    // Specialized cells
    @Test
    public void testR1SpecializedCellsInitialized() {
        assumeBranch("R1");
        Map<Coord, CellType> cellTypesMap = new HashMap<>();
        cellTypesMap.put(new Coord(0, 0), CellType.HIGHLANDER);
        cellTypesMap.put(new Coord(1, 1), CellType.SOCIAL);
        cellTypesMap.put(new Coord(2, 2), CellType.LONER);
        game.clearGenerations();
        Generation start = Generation.createInitial(game, board, cellTypesMap);
        Map<Coord, Cell> alive = facade.getAliveCells(start);

        assertEquals("There should be exactly 3 alive cells initialized", 3,
                     alive.size());
        assertTrue("Cell at (0,0) should be alive", alive.containsKey(new Coord(0, 0)));
        assertTrue("Cell at (1,1) should be alive", alive.containsKey(new Coord(1, 1)));
        assertTrue("Cell at (2,2) should be alive", alive.containsKey(new Coord(2, 2)));
    }

    @Test
    public void testR1HighlanderTakesThreeGenerationsToDie() {
        assumeBranch("R1");
        Map<Coord, CellType> cellTypesMap = new HashMap<>();
        cellTypesMap.put(new Coord(1, 1), CellType.HIGHLANDER);

        game.clearGenerations();
        Generation.createInitial(game, board, cellTypesMap);
        
        Game result = facade.run(game, 5);

        assertEquals("Generation 0 should have the HIGHLANDER alive", 1,
                     result.getGenerations().get(0).getAliveCells().size());
        assertEquals("Generation 1 should still have the HIGHLANDER alive", 1,
                     result.getGenerations().get(1).getAliveCells().size());
        assertEquals("Generation 2 should still have the HIGHLANDER alive", 1,
                     result.getGenerations().get(2).getAliveCells().size());
        assertEquals("Generation 5 should have no HIGHLANDER alive", 0,
                     result.getGenerations().get(5).getAliveCells().size());
    }

    @Test
    public void testR1LonerThrivesWithOneNeighbor() {
        assumeBranch("R1");
        Map<Coord, CellType> cellTypesMap = new HashMap<>();
        cellTypesMap.put(new Coord(1, 1), CellType.LONER);
        cellTypesMap.put(new Coord(1, 2), CellType.LONER);

        game.clearGenerations();
        Generation.createInitial(game, board, cellTypesMap);
        
        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);

        assertEquals("LONER and its LONER neighbor should both survive in next generation", 2,
                     next.getAliveCells().size());
        Map<Coord, Cell> alive = facade.getAliveCells(next);
        assertTrue("LONER Cell at (1,1) should survive", alive.containsKey(new Coord(1, 1)));
        assertTrue("LONER Cell at (1,2) should survive", alive.containsKey(new Coord(1, 2)));
    }

    @Test
    public void testR1SocialSurvivesUpToEightNeighbors() {
        assumeBranch("R1");
        Map<Coord, CellType> cellTypesMap = new HashMap<>();
         
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                CellType type = (dx == 0 && dy == 0) ? CellType.SOCIAL : CellType.HIGHLANDER;
                cellTypesMap.put(new Coord(1 + dx, 1 + dy), type);
            }
        }
        game.clearGenerations();
        Generation.createInitial(game, board, cellTypesMap);
        
        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);

        assertEquals("All 9 cells should be alive", 9, next.getAliveCells().size());

        assertTrue("SOCIAL cell at (1,1) should survive when surrounded by eight neighbors",
                   next.getAliveCells().contains(board.getTile(new Coord(1, 1)).getCell()));
    }

    
    // Vampires and healers

    @Test
    public void testR1HealerGivesLifeToNaive() {
        assumeBranch("R1");

        Cell healer = new Cell();
        healer.setAlive(true);
        healer.setMood(CellMood.HEALER);

        Cell naive = new Cell();
        naive.setAlive(true);
        naive.setMood(CellMood.NAIVE);

        healer.interact(naive);

        assertEquals(
            "Healer should grant 1 lifePoint to a Naive",
            1,
            naive.getLifePoints()
        );
        assertEquals(
            "Healer's own lifePoints remain unchanged",
            0,
            healer.getLifePoints()
        );
    }

    @Test
    public void testR1HealerAndHealerNoEffect() {
        assumeBranch("R1");

        Cell h1 = new Cell();
        h1.setAlive(true);
        h1.setMood(CellMood.HEALER);
        h1.setLifePoints(2);

        Cell h2 = new Cell();
        h2.setAlive(true);
        h2.setMood(CellMood.HEALER);
        h2.setLifePoints(2);

        h1.interact(h2);

        assertEquals(
            "Healer interacting with Healer: no lifePoints change",
            2,
            h1.getLifePoints()
        );
        assertEquals(
            "Healer interacting with Healer: no lifePoints change",
            2,
            h2.getLifePoints()
        );
    }

    @Test
    public void testR1HealerAndVampireInteraction() {
        assumeBranch("R1");
        Cell healer = new Cell();
        healer.setAlive(true);
        healer.setMood(CellMood.HEALER);
        healer.setLifePoints(2);

        Cell vampire = new Cell();
        vampire.setAlive(true);
        vampire.setMood(CellMood.VAMPIRE);
        vampire.setLifePoints(2);

        healer.interact(vampire);

        assertEquals(
            "Vampire should absorb 1 lifePoint from Healer",
            3,
            vampire.getLifePoints()
        );
        assertEquals(
            "Healer should lose 1 lifePoint when interacting with Vampire",
            1,
            healer.getLifePoints()
        );
    }

    @Test
    public void testR1VampireAndVampireNoEffect() {
        assumeBranch("R1");

        Cell v1 = new Cell();
        v1.setAlive(true);
        v1.setMood(CellMood.VAMPIRE);
        v1.setLifePoints(3);

        Cell v2 = new Cell();
        v2.setAlive(true);
        v2.setMood(CellMood.VAMPIRE);
        v2.setLifePoints(3);

        v1.interact(v2);

        assertEquals(
            "Vampire interacting with Vampire: there should not be any lifePoints change",
            3,
            v1.getLifePoints()
        );
        assertEquals(
            "Vampire interacting with Vampire: there should not be any lifePoints change",
            3,
            v2.getLifePoints()
        );
    }

    @Test
    public void testR1VampireAbsorbsNaiveAndConverts() {
        assumeBranch("R1");
        
        // Same stable block:
        Generation init=Generation.createInitial(game, board,
        List.of(new Coord(1,1), new Coord(1,2),
                new Coord(2,1), new Coord(2,2))
        );

        // Turn a cell into a Vampire
        game.setMoods(CellMood.VAMPIRE, List.of(new Coord(1,1)));
        board.getTile(new Coord(2,1)).getCell().setLifePoints(1);
        init.snapCells();

        // Run one evolution step
        Game result = facade.run(game, 1);

        Generation secondGeneration = result.getGenerations().get(1);
        Map<Cell,Integer> lp1  = secondGeneration.getEnergyStates();
        Cell vamp1=board.getTile(new Coord(1,1)).getCell();

        // Its neighbors should have turned Vampire
        Cell ex_naive1=board.getTile(new Coord(1,2)).getCell();
        assertEquals(CellMood.VAMPIRE, ex_naive1.getMood());
        assertEquals(0, (int)lp1.get(ex_naive1));

        Cell ex_naive2=board.getTile(new Coord(2,1)).getCell();        
        assertEquals(CellMood.VAMPIRE, ex_naive2.getMood());
        assertEquals(1, (int)lp1.get(ex_naive2));

        Cell ex_naive3=board.getTile(new Coord(2,2)).getCell();        
        assertEquals(CellMood.VAMPIRE, ex_naive3.getMood());
        assertEquals(0, (int)lp1.get(ex_naive3));

        //vampire has stolen 1 energy from each neighbour
        int energy=lp1.get(vamp1);
        assertEquals(4, energy);
    }
    
}
