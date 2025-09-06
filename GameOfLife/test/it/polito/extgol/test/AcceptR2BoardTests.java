package it.polito.extgol.test;

import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
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

import static it.polito.extgol.test.TestBranchUtils.assumeBranch;

public class AcceptR2BoardTests {
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
    // Analysis & Reporting

    @Test
    public void testR2CountCellsAllDead() {
        assumeBranch("R2");
        Generation start = Generation.createInitial(game, board, List.of());
        int count = board.countCells(start);
        assertEquals("countCells should return 0", 0, count);
    }

    

    @Test
    public void testR2GetHighestEnergyCellBlinker() {
        assumeBranch("R2");
        Generation.createInitial(game, board,List.of(
            new Coord(2, 1),
            new Coord(2, 2),
            new Coord(3, 3)
        ));
        Board.setInteractableTile(game.getBoard(), new Coord(2,1), 3);
        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);
        Cell highest = board.getHighestEnergyCell(next);
        assertEquals("Highest energy should be at (2,2) but was at "+highest.getCoordinates().getX() +", "+highest.getCoordinates().getY(), new Coord(2,2), highest.getCoordinates());
    }

    @Test
    public void testR2GetHighestEnergyCellTie1() {
        assumeBranch("R2");
        Generation.createInitial(game, board,List.of(
            new Coord(1, 1),
            new Coord(2, 1),
            new Coord(1, 2),
            new Coord(2, 2)
        ));
        Board.setInteractableTile(game.getBoard(), new Coord(1,2), 4);
        Board.setInteractableTile(game.getBoard(), new Coord(2,1), 4);
        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);
        Cell highest = board.getHighestEnergyCell(next);
        assertEquals("Highest energy should be at (1,2) but was at "+highest.getCoordinates().getX() +","+highest.getCoordinates().getY(), new Coord(2,1), highest.getCoordinates());
    }

    @Test
    public void testR2GetHighestEnergyCellTie2() {
        assumeBranch("R2");
        Generation.createInitial(game, board,List.of(
            new Coord(1, 1),
            new Coord(2, 1),
            new Coord(1, 2),
            new Coord(2, 2)
        ));
        Board.setInteractableTile(game.getBoard(), new Coord(1,2), 4);
        Board.setInteractableTile(game.getBoard(), new Coord(1,1), 4);
        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);
        Cell highest = board.getHighestEnergyCell(next);
        assertEquals("Highest energy should be at (1,1) but was at "+highest.getCoordinates().getX() +","+highest.getCoordinates().getY(), new Coord(1,1), highest.getCoordinates());
    }
    
    @Test
    public void testR2GroupByAliveAloneCell() {
        assumeBranch("R2");
        Generation init=Generation.createInitial(game, board,List.of(
            new Coord(1, 1)
            
        ));
        Map<Integer, List<Cell>> byNeighbor=board.groupByAliveNeighborCount(init);
        assertTrue(byNeighbor.containsKey(0));
        assertEquals(1, byNeighbor.get(0).size());
        
    }
    
    @Test
    public void testR2TimeSeriesStats() {
        assumeBranch("R2");
        Generation.createInitial(game, board, List.of(
            new Coord(1, 1)));
        
        facade.run(game,2);
        
        //same start and stop
        Map<Integer,IntSummaryStatistics> summaryMap = board.getTimeSeriesStats(0,0);
        assertEquals("Time series size should be 1", 1, summaryMap.size());

        //All dead in second generation, so count and mean 0!
        summaryMap=board.getTimeSeriesStats(0,1);
        assertEquals("Time series size should be 2", 2, summaryMap.size());
        IntSummaryStatistics first = summaryMap.get(1);
        assertEquals("Average energy should be 0", 0, first.getAverage(),0.0001);
        assertEquals("Total live cells at step 1 should be 0", 0, first.getCount());
     
    }
    
    
}
