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
import it.polito.extgol.CellMood;
import it.polito.extgol.Coord;
import it.polito.extgol.ExtendedGameOfLife;
import it.polito.extgol.Game;
import it.polito.extgol.Generation;

import static it.polito.extgol.test.TestBranchUtils.assumeBranch;

public class AcceptR1CellsTests {
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
    public void testR1EnergyExchanged() {
        assumeBranch("R1");
        
        // Diagonal block:
        Generation init=Generation.createInitial(game, board,
        List.of(new Coord(0,0), new Coord(1,1),
                 new Coord(2,2))
        );

        board.getTile(new Coord(0,0)).getCell().setLifePoints(5);
        init.snapCells();

        // Run two evolution steps
        Game result = facade.run(game, 2);

        Generation secondGeneration = result.getGenerations().get(1);

        //(0,0) and (2,2) should be dead after 1 step 
        assertEquals(1, secondGeneration.getAliveCells().size());

        Map<Cell,Integer> lp1  = secondGeneration.getEnergyStates();
        Cell surv1=board.getTile(new Coord(1,1)).getCell();
        assertEquals((Integer)1, lp1.get(surv1));


        //dead even if positive life points
        Cell dead5=board.getTile(new Coord(0,0)).getCell();
        assertEquals((Integer)4, lp1.get(dead5));
        assertEquals(false, dead5.isAlive());

        //dead with negative life points
        Cell dead0=board.getTile(new Coord(2,2)).getCell();
        assertEquals((Integer)(-1), lp1.get(dead0));
        assertEquals(false, dead5.isAlive());

          //surv1 is now dead, energy -1
        Generation thirdGeneration = result.getGenerations().get(2);
        Map<Cell,Integer> lp2  = thirdGeneration.getEnergyStates();
        assertEquals(0, thirdGeneration.getAliveCells().size());
        assertEquals((Integer)0, lp2.get(surv1));

    }

    // Specialized cells
    @Test
    public void testR1ResetEnergyRespawn() {
        assumeBranch("R1");
    
        Generation init = Generation.createInitial(game, board,
            List.of(
                new Coord(1,0), new Coord(1,1), new Coord(1,2)
            )
        );
    
        board.getTile(new Coord(1,0)).getCell().setLifePoints(3);  // Focus cell
        init.snapCells();
    
        // Run three evolution steps
        Game result = facade.run(game, 2);
    
        // First generation: (1,0) dies, with 2 life points
        Generation gen1 = result.getGenerations().get(1);
        Cell c = board.getTile(new Coord(1,0)).getCell();
        assertFalse("Cell should be dead in Gen1", gen1.getAliveCells().contains(c));
        Map<Cell,Integer> lp1 = gen1.getEnergyStates();
        assertEquals((Integer)(2), lp1.get(c));
    
        // Second generation: cell revives with 0 life points
        Generation gen2 = result.getGenerations().get(2);
        lp1 = gen2.getEnergyStates();
        assertEquals((Integer)0, lp1.get(c));
        assertTrue("Cell should be alive in Gen2", gen2.getAliveCells().contains(c));

    }

    @Test
    public void testR1VampireTrap() {
        assumeBranch("R1");
        
        // Same stable block:
        Generation init=Generation.createInitial(game, board,
        List.of(new Coord(0,1), new Coord(1,1),
                new Coord(1,0), new Coord(1,1),
                new Coord(2,1)) 
        );

        // Turn all cell but the central into a Vampire
        game.setMoods(CellMood.VAMPIRE, List.of(new Coord(0,1)));
        game.setMoods(CellMood.VAMPIRE, List.of(new Coord(1,0)));
        game.setMoods(CellMood.VAMPIRE, List.of(new Coord(2,1)));

        init.snapCells();

        // Run one evolution step
        Game result = facade.run(game, 1);

        Generation secondGeneration = result.getGenerations().get(1);
        Map<Cell,Integer> lp1  = secondGeneration.getEnergyStates();
        board.getTile(new Coord(1,1)).getCell();

        // Its neighbors should have turned Vampire
        Cell exNaive1=board.getTile(new Coord(1,1)).getCell();
        assertEquals(CellMood.VAMPIRE, exNaive1.getMood());
        
        // Only one vampire stole energy (-1) + 1 energy from 3 neighbours
        assertEquals(0, (int)lp1.get(exNaive1));
        System.out.println(exNaive1.isAlive());

    }

    
    
}

