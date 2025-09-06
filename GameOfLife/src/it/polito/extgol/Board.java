package it.polito.extgol;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

/**
 * Entity representing the game board grid in the Extended Game of Life.
 *
 * This class models a two-dimensional board of fixed dimensions (defaulting to 5×5)
 * composed of Tile entities. It maintains the mapping from coordinates to tiles,
 * establishes neighbor relationships, and provides both basic Conway visualization
 * and hooks for extended behaviors such as energy modifiers, interactive tiles,
 * and analytic methods over cell lifePoints.
 *
 * Core responsibilities:
 * - Persistence via JPA annotations (@Entity, @Id, @OneToMany, etc.)
 * - Initialization of the tile grid and adjacency links
 * - Retrieval of tiles and cells for simulation logic
 * - String-based visualization of cell states in a generation
 * - Factory support for the extended version (interactable tiles, default moods/types)
 * - Analytic operations over generations (e.g., counting, grouping, statistics)
 */
@Entity
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Number of columns on the board. */
    @Column(nullable = false)
    private Integer width=5;

    /** Number of rows on the board. */
    @Column(nullable = false)    
    private Integer height=5;

    /** Inverse one-to-one back to owning Game. */
    @OneToOne(mappedBy = "board", fetch = FetchType.LAZY)
    private Game game;

    /**
     * Map of tile coordinates to Tile entities.  
     */
    @OneToMany(
      mappedBy      = "board",
      cascade       = CascadeType.ALL,
      orphanRemoval = true,
      fetch         = FetchType.LAZY
    )
    @MapKey(name = "tileCoord")
    private Map<Coord, Tile> tiles = new HashMap<>();

    /**
     * Default constructor required by JPA.
     */
    public Board() {}

    /**
     * Constructs a Board of the given width and height, associates it with the
     * specified Game (if non-null), and initializes all Tiles and their neighbor
     * relationships.
     *
     * @param width  the number of columns in the board grid
     * @param height the number of rows in the board grid
     * @param g      the Game instance this board belongs to;
     */
    public Board(int width, int height, Game g) {
        this.width = width;
        this.height = height;
        this.game = g;
        initializeTiles();
    }

    /**
     * Factory method to create a fully initialized Board for the extended Game of Life.
     * 
     * This sets up a new Board of the given dimensions, associates it with the provided
     * Game instance, and applies default extended settings:
     * 
     *   -All tiles are made interactable with a lifePointModifier of 0.
     *   -All cells are initialized with a NAIVE mood and BASIC cell type.
     *
     *
     * @param width  the number of columns on the board
     * @param height the number of rows on the board
     * @param game   the Game instance to which this board belongs
     * @return the Board instance ready for use in the extended simulation
     */
    public static Board createExtended(int width, int height, Game game) {
        Board board = new Board(width, height, game);

        // Initialize all tiles as interactable with zero modifier
        for (Tile t : board.getTiles()) {
            Board.setInteractableTile(board, t.getCoordinates(), 0);
        }

        // Set default mood and type for every cell
        for (Cell c : board.getCellSet()) {
            c.setMood(CellMood.NAIVE);
            c.setType(CellType.BASIC);
        }

        return board;
    }

    /**
     * Populates and links all Tile instances for this Board.
     *
     * This method clears any existing tiles, then:
     *   1. Creates a Tile at each (x, y) coordinate within the board’s width and height,
     *      associates it with this Board and its Game, and stores it in the tiles map.
     *   2. Iterates over every Tile to establish neighbor relationships by
     *      calling Tile's initializeNeighbors(...) with the adjacent tiles.
     *
     * This setup ensures each tile knows its position and its surrounding tiles,
     * enabling neighbor-based logic in the simulation.
     */
    private void initializeTiles() {
        tiles.clear();        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = new Tile(x, y, this, this.game);
                tile.setBoard(this);
                tiles.put(tile.getCoordinates(), tile);
            }
        }
        for (Tile t : tiles.values()) {   
            t.initializeNeighbors(getAdjacentTiles(t));
        }
    }

    /**
     * Computes and returns all neighboring Tiles surrounding the specified tile.
     *
     * Iterates over the eight possible offsets (dx, dy) around the tile’s coordinates,
     * skips the tile itself, and includes only those tiles that exist within the neighborhood.
     *
     * @param tile the central Tile for which neighbors are sought
     * @return a Set of adjacent Tile instances (up to eight) surrounding the given tile
     */
    public Set<Tile> getAdjacentTiles(Tile tile) {
        Set<Tile> adj = new HashSet<>();
        int cx = tile.getX();
        int cy = tile.getY();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                // skip the center tile itself
                if (dx == 0 && dy == 0) continue;

                Tile t = getTile(new Coord(cx + dx, cy + dy));
                
                if (t != null) { // skipping null references (e.g., border conditions)
                    adj.add(t);
                }
            }
        }
        return adj;
    }

    /**
    * Getters and setters
    */

    /**
     * Returns the unique identifier for this Board.
     *
     * @return the board’s id
     */
    public int getId() {
        return id;
    }

    /**
     * Retrieves the Tile at the specified coordinates.
     *
     * @param c the Coord position to look up
     * @return the Tile at those coordinates
     */
    public Tile getTile(Coord c){
        return tiles.get(c);
    }

    /**
     * Returns an immutable list of all Tiles on this Board.
     *
     * This defensive copy prevents external modification of the board’s tile collection.
     *
     * @return a List of all Tile instances on the board
     */
    public List<Tile> getTiles() {
        return List.copyOf(tiles.values());
    }

    /**
     * Gathers and returns the set of all Cells currently placed on this Board.
     *
     * @return a Set of all Cell instances belonging to this board
     */
    public Set<Cell> getCellSet() {
        Set<Cell> cellSet = new HashSet<>();
        for (Tile t : tiles.values()) {
            cellSet.add(t.getCell());
        }
        return cellSet;
    }
    
    /**
     * Visualizes the given Generation by mapping alive and dead cells onto a character grid.
     * Alive cells are represented by 'C' and dead cells by '0'.
     * Each row of the board is separated by a newline character.
     *
     * @param generation the Generation object containing the current cell states
     * @return a multi-line String representing the board, where each line corresponds to a row (y-coordinate)
     */
    public String visualize(Generation generation) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell cell = getTile(new Coord(x, y)).getCell();
                if (cell == null || !cell.isAlive()) {
                    sb.append('0');
                } else {
                    switch (cell.getType()) {
                        case HIGHLANDER -> sb.append('H');
                        case LONER      -> sb.append('L');
                        case SOCIAL     -> sb.append('S');
                        default         -> sb.append('C');
                    }
                }
            }
            // use height here so you don't append a newline after the last row
            if (y < height - 1) {
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    // EXTENDED BEHAVIORS

    /**
     * Creates an interactable Tile with the specified lifePoints modifier 
     * at the given coordinate on the board.
     *
     * @param board               the Board on which to place the tile
     * @param coord               the Coord position for the new tile
     * @param lifePointsModifier  the amount of lifePoints this tile will add (or subtract) each generation
     * @return the newly created Interactable tile
     */
    public static Interactable setInteractableTile(Board board, Coord coord, Integer lifePointsModifier) {
        Tile tile = board.getTile(coord);
        if (tile == null) {
            throw new IllegalArgumentException("No tile at " + coord);
        }
        tile.setPointModifier(lifePointsModifier);
        return tile;
    }

    /**
     * Returns the total number of alive cells in the given generation.
     *
     * @param gen the Generation instance to analyze
     * @return the count of alive cells in gen
     */
    public Integer countCells(Generation generation) {
        int count =0;
        for(Tile tile : tiles.values()){
            Cell cell = tile.getCell();
            if(cell != null && cell.isAlive()){
                count++;
            }
        }        
        return count;
    }

    /**
     * Finds the single cell with the highest lifePoints in the given generation.
     * In case of a tie, returns the cell closest to the top-left corner.
     *
     * @param gen the Generation instance to analyze
     * @return the Cell with maximum lifePoints, or null if no cells are alive
     */
    public Cell getHighestEnergyCell(Generation gen) {
        return tiles.values().stream()
            .map(Tile::getCell)
            .filter(cell -> cell != null && cell.isAlive())
            .max(Comparator
                .comparingInt(Cell::getLifePoints)
                .thenComparingInt(cell -> -cell.getY())
                .thenComparingInt(cell -> -cell.getX()))
            .orElse(null);
    }

    /**
     * Groups all alive cells in the generation by their current lifePoints.
     *
     * @param gen the Generation instance to analyze
     * @return a Map from lifePoints value to the List of Cells having that energy
     */
    public Map<Integer, List<Cell>> getCellsByEnergyLevel(Generation gen) {
        return gen.getAliveCells().stream()
            .collect(Collectors.groupingBy(Cell::getLifePoints));
    }


    /**
     * Counts alive cells per CellType in the given generation.
     *
     * @param gen the Generation instance to analyze
     * @return a Map from CellType to the count of alive cells of that type
     */
    public Map<CellType, Integer> countCellsByType(Generation gen) {
        return  gen.getAliveCells().stream()
        .collect(Collectors.groupingBy(Cell::getType, Collectors.summingInt(c -> 1)));
    };
    

    /**
     * Returns the top n cells sorted by descending lifePoints.
     *
     * @param gen the Generation instance to analyze
     * @param n   the number of top-energy cells to return
     * @return a List of the top n Cells by lifePoints, in descending order
     */
    public List<Cell> topEnergyCells(Generation gen, int n) {
        return gen.getAliveCells().stream() // R2
            .sorted(
                Comparator
                    .comparingInt(Cell::getLifePoints).reversed() 
                    .thenComparing(c -> c.getCoordinates().getY()) 
                    .thenComparing(c -> c.getCoordinates().getX())
            )
            .limit(n)
            .collect(Collectors.toList());
    }



    /**
     * Groups each alive cell by its number of live neighbors.
     *
     * @param gen the Generation instance to analyze
     * @return a Map from neighbor count to the List of Cells having that many alive neighbors
     */
    public Map<Integer, List<Cell>> groupByAliveNeighborCount(Generation gen) {
        Map<Integer, List<Cell>> neighborMap = new HashMap<>();

        // Iterate only the cells alive in this generation
        for (Cell cell : gen.getAliveCells()) {
            // Count alive neighbors using the updated cell.isAlive() state
            int aliveNeighbors = cell.countAliveNeighbors(); // R1

            // Group by number of neighbors
            neighborMap
                .computeIfAbsent(aliveNeighbors, k -> new ArrayList<>())
                .add(cell);
        }
        return neighborMap;
    }

    /**
     * Computes summary statistics (count, min, max, sum, average) over all alive cells’ lifePoints.
     *
     * @param gen the Generation instance to analyze
     * @return an IntSummaryStatistics with aggregated lifePoints metrics
     */
    public java.util.IntSummaryStatistics energyStatistics(Generation gen) {
        return gen.getAliveCells().stream()           // only alive cells in this gen
            .mapToInt(Cell::getLifePoints)            // extract their lifePoints
            .summaryStatistics();                     // build stats object
    }

    /**
     * Returns a time series of energy statistics for each generation step in [fromStep, toStep].
     *
     * @param fromStep the starting generation index (inclusive)
     * @param toStep   the ending generation index (inclusive)
     * @return a Map from generation step index to its IntSummaryStatistics
     */
    public Map<Integer, IntSummaryStatistics> getTimeSeriesStats(int fromStep, int toStep) {
        if (fromStep > toStep) {
            throw new IllegalArgumentException("fromStep must be <= toStep");
        }

        List<Generation> generations = game.getGenerations();
        Map<Integer, IntSummaryStatistics> timeSeries = new HashMap<>();

        for (Generation gen : generations) {
            int step = gen.getStep();
            if (step >= fromStep && step <= toStep) {
                IntSummaryStatistics stats = gen.getCellAlivenessStates().entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .mapToInt(e -> gen.getEnergySnapshot().get(e.getKey()))
                    .summaryStatistics();

                timeSeries.put(step, stats);
            }
        }

        return timeSeries;
    }

}