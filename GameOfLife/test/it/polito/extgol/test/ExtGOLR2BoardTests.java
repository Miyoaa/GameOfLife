package it.polito.extgol.test;

import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import it.polito.extgol.Board;
import it.polito.extgol.Cell;
import it.polito.extgol.Coord;
import it.polito.extgol.ExtendedGameOfLife;
import it.polito.extgol.Game;
import it.polito.extgol.Generation;
import it.polito.extgol.Interactable;
import it.polito.extgol.JPAUtil;
import it.polito.extgol.Tile;
import static it.polito.extgol.test.TestBranchUtils.assumeBranch;

public class ExtGOLR2BoardTests {
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
    // R2 Board Extended Behaviors

    // Dynamic Tiles
    @Test
    public void testR2InteractableTilesCreation() {
        assumeBranch("R2");
        // Initially, the board should provide tiles implementing Interactable
        Game gameOfTiles = Game.createExtended("InteractiveTilesGame", 4, 4);
        Board boardOfTiles = gameOfTiles.getBoard();
        
        for (Tile t : boardOfTiles.getTiles()){
            assertNotNull("Dynamic tile should be created at (1,1)", t);
            assertTrue(
                "Dynamic tile should implement Interactable",
                t instanceof Interactable
        );
        }
    }

    
    @Test
    public void testR2TileInteractPositiveModifier() {
        assumeBranch("R2");
        
        Generation.createInitial(game, board, List.of(
            new Coord(1, 1),
            new Coord(2, 1),
            new Coord(1, 2),
            new Coord(2, 2)
        ));
       
        // assign a tile with +3 modifier at that position
        Board.setInteractableTile(game.getBoard(), new Coord(1, 1), 3);

        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);
        Cell cell = facade.getAliveCells(next).get(new Coord(1, 1));

        assertEquals(
            "One positive tile modifier of +3 should yield lifePoints 4 after 1 step in survival-maintaining conditions",
            4,
            cell.getLifePoints()
        );
        assertTrue(
            "Cell should remain alive in survival condition after tile interaction",
            cell.isAlive()
        );
    }
    
    @Test
    public void testR2TileInteractNegativeModifier() {
        assumeBranch("R2");
 
        Generation.createInitial(game, board, List.of(new Coord(1, 1)));
         
        Board.setInteractableTile(game.getBoard(), new Coord(1, 1), -2);
        
        Game result = facade.run(game, 10);
        Generation g2 = result.getGenerations().get(2);
        Cell cell2 = g2.getBoard().getTile(new Coord(1, 1)).getCell();
        int cellEnergy2 = g2.getEnergyStates().get(cell2);

        assertEquals(
            "Cell should have lifepoints -3 (-2 tile modifier at gen2 -1 lifepoint for death, and stopped interaction after death at gen1)",
            -3,
            cellEnergy2
        );

        Generation g10 = result.getGenerations().get(10);
        Cell cell10 = g10.getBoard().getTile(new Coord(1, 1)).getCell();
        int cellEnergy10 = g10.getEnergyStates().get(cell10);

        assertEquals(
            "Cell should have lifepoints -3 (-2 tile modifier at gen2 -1 lifepoint for death, and stopped interaction after death at gen1)",
            -3,
            cellEnergy10
        );

        assertFalse(
            "Cell should be dead after death-inducing conditions + negative tile interaction",
            g2.getAliveCells().contains(cell2)
        );

        assertFalse(
            "Cell should be dead after death-inducing conditions + negative tile interaction",
            g10.getAliveCells().contains(cell10)
        );
    }

    @Test
    public void testR2TileInteractZeroModifier() {
        assumeBranch("R2");
        Generation.createInitial(game, board, List.of(new Coord(1, 1)));
         
        Board.setInteractableTile(game.getBoard(), new Coord(1, 1), 0);
        
        Game result = facade.run(game, 2);
        Generation g2 = result.getGenerations().get(2);
        Cell cell2 = g2.getBoard().getTile(new Coord(1, 1)).getCell();
        int cellEnergy2 = g2.getEnergyStates().get(cell2);

        assertEquals(
            "Cell should have -1 lifepoints from death-inducing conditions after neutral tile interaction",
            -1,
            cellEnergy2
        );
        
    } 


    @Test
    public void testR2CellTileInteraction() {
        assumeBranch("R2");
        
        Generation.createInitial(game, board,List.of(
            new Coord(1, 1),
            new Coord(2, 1),
            new Coord(1, 2),
            new Coord(2, 2)
        ));

        Board.setInteractableTile(game.getBoard(), new Coord(2, 2), +3);
        
        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);
        Cell cell = facade.getAliveCells(next).get(new Coord(2, 2));
        int cellEnergy = next.getEnergyStates().get(cell);
        
        assertEquals(
            "Cell should have 4 lifepoints after 1 generation (+3 lifePoints from tile interaction, +1 lifepoint from survival-maintaining conditions)",
            4,
            cellEnergy
        );
        // cell should remain alive
        assertTrue(
            "Cell should remain alive after tile interaction",
            next.getAliveCells().contains(cell)
        );
    }


    // Analysis & Reporting
    @Test
    public void testR2CountCells() {
        assumeBranch("R2");
        Generation start = Generation.createInitial(game, board, List.of(new Coord(0,0), new Coord(4,4)));
        int count = board.countCells(start);
        assertEquals("countCells should return 2", 2, count);
    }

    

    @Test
    public void testR2GetHighestEnergyCell() {
        assumeBranch("R2");
        Generation.createInitial(game, board,List.of(
            new Coord(1, 1),
            new Coord(2, 1),
            new Coord(1, 2),
            new Coord(2, 2)
        ));
        Board.setInteractableTile(game.getBoard(), new Coord(2,2), 4);
        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);
        Cell highest = board.getHighestEnergyCell(next);
        assertEquals("Highest energy should be at (2,2) but was at "+highest.getCoordinates().getX() +", "+highest.getCoordinates().getY(), new Coord(2,2), highest.getCoordinates());
    }

    @Test
    public void testR2GetCellsByEnergyLevel() {
        assumeBranch("R2");
        Generation.createInitial(game, board, List.of(
            new Coord(1, 1),
            new Coord(2, 1),
            new Coord(1, 2),
            new Coord(2, 2)));

        Board.setInteractableTile(game.getBoard(), new Coord(2,2), 2);
        Board.setInteractableTile(game.getBoard(), new Coord(1,1), 10);
        Game result = facade.run(game, 1);
        Generation next = result.getGenerations().get(1);
        Map<Integer,List<Cell>> byEnergy = board.getCellsByEnergyLevel(next);
        assertTrue(byEnergy.containsKey(3));
        assertTrue(byEnergy.containsKey(11));
    }

    
    @Test
    public void testR2TopEnergyCells() {
        assumeBranch("R2");
        Generation.createInitial(game, board, List.of(
            new Coord(5, 5),

            new Coord(1, 1),
            new Coord(2, 1),
            new Coord(1, 2),
            new Coord(2, 2)));
        Board.setInteractableTile(game.getBoard(), new Coord(2,1), +5);
        Board.setInteractableTile(game.getBoard(), new Coord(2,2), +1);
        Board.setInteractableTile(game.getBoard(), new Coord(5,5), +2);

        Game result = facade.run(game,1);
        Generation next = result.getGenerations().get(1);
        List<Cell> top1 = board.topEnergyCells(next, 1);
        assertEquals("topEnergyCells should return 1 cell", 1, top1.size());
        assertEquals("topEnergyCells should return cell at 2, 1",new Coord(2,1), top1.get(0).getCoordinates());
    }

    @Test
    public void testR2GroupByNeighborCount() {
        assumeBranch("R2");
        Game big = Game.createExtended("biGame", 20, 20);
        
        List<Coord> coords = List.of(
            //blinker: 1 cell with 2 neighbors, 2 cells with 1
            new Coord(10, 5),
            new Coord(10, 4),
            new Coord(10, 3),

            //stable square: 4 cells with 3 neighbors
            new Coord(0, 0),
            new Coord(0, 1),
            new Coord(1, 0),
            new Coord(1, 1)
        );
        Generation start = Generation.createInitial(big, big.getBoard(), coords);
        Game result = facade.run(big,1);
        Generation next = result.getGenerations().get(1);
        Map<Integer,List<Cell>> byNeighbor = board.groupByAliveNeighborCount(next);
        System.out.println(facade.visualize(start));
        System.out.println(facade.visualize(next));
        assertTrue(byNeighbor.containsKey(1));
        assertTrue(byNeighbor.containsKey(2));
        assertTrue(byNeighbor.containsKey(3));

        List<Cell> threes = byNeighbor.get(3);
        List<Cell> twos = byNeighbor.get(2);
        List<Cell> ones = byNeighbor.get(1);
        assertEquals("Four cells should have exactly three alive neighbors", 4, threes.size());
        assertEquals("One cells should have exactly two alive neighbor", 1, twos.size());
        assertEquals("Two cells should have exactly one alive neighbor", 2, ones.size());

    }

    @Test
    public void testR2EnergyStatistics() {
        assumeBranch("R2");
        Generation.createInitial(game, board, List.of(
            new Coord(0, 0),
            new Coord(0, 1),
            new Coord(1, 0),
            new Coord(1, 1),
            new Coord(3, 3)
        ));

        Board.setInteractableTile(game.getBoard(), new Coord(0, 0), +1); 
        Board.setInteractableTile(game.getBoard(), new Coord(1, 0), +2);  
        Board.setInteractableTile(game.getBoard(), new Coord(3, 3), +3);

        Game result = facade.run(game,1);
        Generation next = result.getGenerations().get(1);
        
        IntSummaryStatistics stats = board.energyStatistics(next);
        
        // There should be 4 alive cells
        // assertEquals("Stats count should equal number of cells", 4, stats.getCount())
        // Minimum energy should be initial lifePoints of unmodified cell: 0
        assertEquals("Minimum energy should be 1", 1, stats.getMin());
        // Maximum energy should be initial lifePoints+1+2 (isolated cell dies)
        assertEquals("Maximum energy should be 3", 3, stats.getMax());
        // Sum of energies
        assertEquals("Sum of energies should be 7", 7, stats.getSum());
        // Average energy 
        assertEquals("Average energy should be 1.75", 1.75, stats.getAverage(), 1e-6);
    
    }

    @Test
    public void testR2TimeSeriesStats() {
        assumeBranch("R2");
        Generation.createInitial(game, board, List.of(
            new Coord(0, 0),
            new Coord(0, 1),
            new Coord(1, 0),
            new Coord(1, 1)));

        Board.setInteractableTile(game.getBoard(), new Coord(1, 0), +5); 
        
        facade.run(game,2);
        
        Map<Integer,IntSummaryStatistics> summaryMap = board.getTimeSeriesStats(0,2);
        assertEquals("Time series size should be 3", 3, summaryMap.size());

        // Check second generation (step 1)
        IntSummaryStatistics first = summaryMap.get(1);
        assertEquals("Total live cells at step 1 should be 4", 4, first.getCount());
        assertEquals("Minimum energy should be 1", 1, first.getMin());
        assertEquals("Maximum energy should be 6", 6, first.getMax());

        // Check last generation (step 2)
        IntSummaryStatistics last = summaryMap.get(2);
        assertEquals("Total live cells at step 2 should be 4", 4, last.getCount());
        assertEquals("Minimum energy should be 2", 2, last.getMin());
        assertEquals("Maximum energy should be 12", 12, last.getMax());        
    }
}
