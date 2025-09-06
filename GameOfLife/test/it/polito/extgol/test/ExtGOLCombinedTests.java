package it.polito.extgol.test;

import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import it.polito.extgol.Board;
import it.polito.extgol.Cell;
import it.polito.extgol.CellMood;
import it.polito.extgol.CellType;
import it.polito.extgol.Coord;
import it.polito.extgol.EventType;
import it.polito.extgol.ExtendedGameOfLife;
import it.polito.extgol.Game;
import it.polito.extgol.Generation;
import it.polito.extgol.JPAUtil;
import jakarta.persistence.EntityManager;

import static it.polito.extgol.test.TestBranchUtils.assumeBranch;

/**
 * JUnit test suite for the basic GOL
 * 
 */
public class ExtGOLCombinedTests {

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
        game  = Game.createExtended("TestGame", 5, 4);
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
    public void testR1R2Visualization() {
        assumeBranch("r1","r2");
        
        // Seed various cell types
        Map<Coord,CellType> types = Map.of(
            new Coord(1, 0), CellType.BASIC,
            new Coord(2, 0), CellType.HIGHLANDER,
            new Coord(3, 0), CellType.LONER,
            new Coord(4, 0), CellType.SOCIAL,
            new Coord(0, 3), CellType.SOCIAL
        );

        Generation start = Generation.createInitial(game, board, types);

        // Expected visualization:
        String expected = String.join(System.lineSeparator(),
            "0CHLS",  // row 0
            "00000",  // row 1
            "00000",  // row 2
            "S0000"   // row 3
        );

        String viz = board.visualize(start);
        assertEquals(
            "Board.visualize() should render zeros and cell symbols correctly",
            expected,
            viz
        );
    }

    @Test
    public void testR1R2CountCellsByType() {
        assumeBranch("r1","r2");
        Map<Coord,CellType> types = Map.of(
            new Coord(0,0), CellType.LONER,
            new Coord(1,1), CellType.LONER,
            new Coord(2,2), CellType.SOCIAL
        );
        Generation start = Generation.createInitial(game, board, types);
        Map<CellType,Integer> counts = board.countCellsByType(start);
        assertEquals(Integer.valueOf(2), counts.get(CellType.LONER));
        assertEquals(Integer.valueOf(1), counts.get(CellType.SOCIAL));
    }

    @Test
    public void testR2R3BloomAndCataclysmEvents() {
        assumeBranch("r2", "r3");
        Generation.createInitial(game, game.getBoard(),
            List.of(new Coord(1,1), new Coord(1,2), new Coord(2,1), new Coord(2,2))
        );
        Game result = facade.run(game, 5, Map.of(0, EventType.BLOOM, 3, EventType.CATACLYSM));

        // --- Second generation: after BLOOM ---
        Generation secondGeneration = result.getGenerations().get(1);
        Cell c1 = facade.getAliveCells(secondGeneration).get(new Coord(1,1));
        int lp1 = secondGeneration.getEnergyStates().get(c1);
        assertEquals("BLOOM should give +2 LP", 3, lp1);

        // --- Fifth generation: after CATACLYSM ---
        Generation fifthGeneration = result.getGenerations().get(4);
        Cell c2 = facade.getAliveCells(fifthGeneration).get(new Coord(1,1));
        int lp2 = fifthGeneration.getEnergyStates().get(c2);
        assertEquals("CATACLYSM should zero LP", 1, lp2);
    }

    @Test
    public void testR2R3BloomAndFamineEvents() {
        assumeBranch("r2", "R3");

        // seed three cells as a blinker
        Generation.createInitial(game, board,
        List.of(new Coord(1,1), new Coord(1,2), new Coord(2,1), new Coord(2,2))
        );
        Game result = facade.run(game, 5, Map.of(
            1, EventType.BLOOM,
            3, EventType.FAMINE
        ));

        // --- after BLOOM ---
        Generation thirdGeneration = result.getGenerations().get(2);
        Cell bloomCell = facade.getAliveCells(thirdGeneration).get(new Coord(1,1));
        int lpAfterBloom = thirdGeneration.getEnergyStates().get(bloomCell);
        assertEquals(
        "BLOOM should give +2 lifePoints to (1,1)",
        4,
        lpAfterBloom
        );

        // --- after FAMINE ---
        Generation fifthGeneration = result.getGenerations().get(4);
        Cell famineCell = facade.getAliveCells(fifthGeneration).get(new Coord(1,1));
        int lpAfterFamine = fifthGeneration.getEnergyStates().get(famineCell);
        assertEquals(
        "FAMINE should subtract 1 lifePoint from (1,1)",
        5,
        lpAfterFamine
        );
    }

    @Test
    public void testR1R2R3BloodMoonEvent() {
        assumeBranch("r1", "r2", "r3");
        // Same stable block:
        Generation init=Generation.createInitial(game, board,
        List.of(new Coord(1,1), new Coord(1,2),
                new Coord(2,1), new Coord(2,2),
                new Coord(2,3))
        );
        // Turn a cell into a Vampire
        game.setMoods(CellMood.VAMPIRE, List.of(new Coord(1,1)));
        init.snapCells();
        // BLOOD_MOON at step 1
        Game result = facade.run(game, 1, Map.of(0, EventType.BLOOD_MOON));

        Generation secondGeneration = result.getGenerations().get(1);
        Map<Cell,Integer> lp1  = secondGeneration.getEnergyStates();
        Cell vamp1=board.getTile(new Coord(1,1)).getCell();

        int energy=lp1.get(vamp1);
        assertEquals(4, energy);

        // Its neighbors should have turned Vampire
        Cell naive1=board.getTile(new Coord(1,2)).getCell();
        assertEquals(CellMood.VAMPIRE, naive1.getMood());
        
        // Far‐away (2,3) remains Naive with 0 LP
        Cell naive2=board.getTile(new Coord(2,3)).getCell();        
        assertEquals(CellMood.NAIVE, naive2.getMood());
    }

    @Test
    public void testR1R2R3SanctuaryEvent() {
        assumeBranch("r1", "r2", "r3");
        Generation init = Generation.createInitial(game, game.getBoard(),List.of(
            new Coord(1, 1),
            new Coord(2, 1),
            new Coord(1, 2),
            new Coord(2, 2)));
        game.setMoods(CellMood.HEALER, List.of(
            new Coord(1,1), new Coord(2,1)
        ));
        game.setMoods(CellMood.VAMPIRE, List.of(
            new Coord(2,2)
        ));
        init.snapCells();
        Game result = facade.run(game, 1, Map.of(0, EventType.SANCTUARY));
        
        Generation secondGeneration = result.getGenerations().get(1);
        // Healer at (1,1) +1 LP
        Cell healer = facade.getAliveCells(secondGeneration).get(new Coord(1,1));
        assertEquals(Integer.valueOf(2), secondGeneration.getEnergyStates().get(healer));
        // Vampire turned Naive, LP stays 0
        Cell vamp1 = facade.getAliveCells(secondGeneration).get(new Coord(2,2));
        assertEquals(CellMood.NAIVE, secondGeneration.getMoodStates().get(vamp1));
        assertEquals(Integer.valueOf(3), secondGeneration.getEnergyStates().get(vamp1));
    }

    @Test
    public void testR2R3LoadEventsRoundTrip() {
        assumeBranch("r2", "r3");
        // 1) create + schedule
        Game evtGame = Game.createExtended("EvtGame", 4, 4);
        // directly mutate the private backing map:
        evtGame.getEventMapInternal().put(2, EventType.BLOOM);
        evtGame.getEventMapInternal().put(5, EventType.FAMINE);

        // 2) persist
        new it.polito.extgol.ExtendedGameOfLife().saveGame(evtGame);
        Long gameId = evtGame.getId();

        // 3) reload fresh instance
        EntityManager em2 = JPAUtil.getEntityManager();
        Game reloaded = em2.find(Game.class, gameId);
        em2.close();

        // 4) call loader
        Map<Integer,EventType> loaded = Game.loadEvents(reloaded);

        // 5) assert correct persistence
        assertEquals(2, loaded.size());
        assertEquals(EventType.BLOOM,  loaded.get(2));
        assertEquals(EventType.FAMINE, loaded.get(5));
    }
}
