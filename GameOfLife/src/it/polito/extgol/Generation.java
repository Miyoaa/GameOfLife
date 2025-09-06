package it.polito.extgol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

/**
 * Entity representing a generation within a Game of Life simulation.
 * Each generation records the game state of every cell on the board at its
 * given step.
 * 
 * Use createInitial(...) to construct the initial state (step 0), and
 * createNextGeneration(...) to
 * advance from a previous generation.
 */
@Entity
@Table(name = "generation", uniqueConstraints = @UniqueConstraint(columnNames = { "game_id", "step" }))
public class Generation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Sequential step index (0 for initial generation). */
    @Column(nullable = false)
    private Integer step;

    /** Owning Game instance. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /** Board context for boundary checks. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    /**
     * Persistent map of each Cell to its alive state at this generation.
     * Keys are Cell entities; values are true for alive, false for dead.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "generation_state", joinColumns = {
            // This maps generation_state.generation_id → generation.id
            @JoinColumn(name = "generation_id", referencedColumnName = "id"),
            // These map generation_state.game_id → generation.game_id
            // generation_state.board_id → generation.board_id
            @JoinColumn(name = "game_id", referencedColumnName = "game_id"),
            @JoinColumn(name = "board_id", referencedColumnName = "board_id")
    })
    @MapKeyJoinColumn(name = "cell_id")
    @Column(name = "is_alive", nullable = false)
    private Map<Cell, Boolean> cellAlivenessStates = new HashMap<>();

    @Column(name = "event_type")
    @Enumerated(EnumType.STRING)
    private EventType event;

    public EventType getEvent(){
        return event;
    }

    public void setEvent(EventType e){
        this.event = e;
    }

    /**
     * Protected no-argument constructor required by JPA.
     *
     * This constructor is used by the persistence provider to create instances via
     * reflection.
     * Do not invoke directly in application code.
     */
    protected Generation() {
    }

    /**
     * Internal constructor used by factory methods to fully initialize a
     * Generation.
     *
     * @param game  the Game instance this generation belongs to
     * @param board the Board context capturing the cell layout for this generation
     * @param step  the zero-based index of this generation in the game sequence
     */
    protected Generation(Game game, Board board, int step) {
        this.game = game;
        this.board = board;
        this.step = step;
    }

    @Transient
    private Map<Cell, Integer> energySnapshot = new HashMap<>();

    public Map<Cell, Integer> getEnergySnapshot() {
        return Map.copyOf(energySnapshot);
    }


    /**
     * Backward-compatible constructor for partial initialization.
     *
     * The Board reference will be set later by factory methods such as
     * createNextGeneration.
     *
     * @param game the Game instance this generation belongs to
     * @param step the zero-based index of this generation in the game sequence
     */
    protected Generation(Game game, int step) {
        this.game = game;
        this.step = step;
    }

    /**
     * Creates cells of a given type in place to the ones present at the coordinates
     *
     * @param coords list of coordinates whose Cells should be updated
     * @param type   the cell type to assign
     */
    public void setType(List<Coord> coords, CellType type) {
        for (Coord c : coords) {
            Cell cell = board.getTile(c).getCell();
            cell.setType(type);
        }
    }

    /**
     * Creates the initial generation (step 0) with all cells dead,
     * captures their states, and records this generation in the game.
     *
     * @param game  the Game instance to initialize
     * @param board the Board context for the new generation
     * @return a new Generation representing step 0 with all cells set dead
     * @throws ExtendedGameOfLifeException
     */
    public static Generation createInitial(Game game, Board board) {
        game.clearGenerations();
        Generation init = new Generation(game, board, 0);
        init.snapCells();
        game.addGeneration(init, 0);
        return init;
    }

    /**
     * Creates the initial generation (step 0) with a specified subset of cells
     * alive,
     * captures their states, and records this generation in the game.
     *
     * @param game       the Game instance to initialize
     * @param board      the Board context for the new generation
     * @param aliveCells a list of coordinates for cells to set alive
     * @return a new Generation representing step 0 with the given cells alive
     */
    public static Generation createInitial(Game game, Board board, List<Coord> aliveCells) {
        Objects.requireNonNull(game, "Game cannot be null");
        Objects.requireNonNull(board, "Board cannot be null");
        Objects.requireNonNull(aliveCells, "aliveCells cannot be null");

        game.clearGenerations();
        Generation init = new Generation(game, board, 0);
        init.setState(aliveCells, true);
        init.snapCells();
        game.addGeneration(init, 0);
        return init;
    }

    /**
     * Advances from the given previous generation to the next step,
     * captures the new state snapshot, and appends it to the game history.
     *
     * @param prev the previous Generation to base the next upon, cannot be
     *             {@code null}
     * @return a new Generation representing the next sequential step
     */
    public static Generation createNextGeneration(Generation prev) {
        Objects.requireNonNull(prev, "Previous generation cannot be null");

        Board board = prev.getBoard();
        Game game = prev.getGame();
        Generation next = new Generation(game, board, prev.getStep() + 1);

        for (Tile tile : board.getTiles()) {
            Cell cell = tile.getCell();
            cell.addGeneration(next); // reuse the same Cell instance
        }

        next.snapCells(); // stores alive status and energy
        game.addGeneration(next, next.getStep());

        return next;
    }

    /**
     * Captures the current state of every cell on the board
     * into the persistent cellAlivenessStates map and returns an unmodifiable
     * snapshot.
     *
     * Iterates over each Tile in the associated Board, validates that a Cell
     * exists on the tile, and records its isAlive value. After clearing any
     * previous state, it populates the map and returns an immutable copy.
     *
     * @return an unmodifiable Map of Cell to Boolean indicating each cell’s alive
     *         state
     * @throws ExtendedGameOfLifeException if any Tile does not contain a Cell
     */
    public Map<Cell, Boolean> snapCells() {
        cellAlivenessStates.clear();
        energySnapshot.clear();
        
        for (Tile tile : board.getTiles()) {
            Cell cell = tile.getCell();
            if (cell == null) {
                throw new IllegalStateException("Each tile should hold a cell!");
            }
            cellAlivenessStates.put(cell, cell.isAlive());
            energySnapshot.put(cell, cell.getLifePoints()); // snapshot energy
        }

        return Map.copyOf(cellAlivenessStates);
    }

    /**
     * Retrieves all cells that are marked as alive in this generation’s snapshot.
     *
     * Iterates over the internal map of Cell to Boolean, filters for entries
     * where the Boolean value is true, and collects the corresponding Cell keys.
     *
     * @return a Set of Cell instances that are alive in this generation
     */
    public Set<Cell> getAliveCells() {
        return cellAlivenessStates.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Updates the alive/dead status for the given coordinates,
     * then snapshots these states into the persistent map.
     *
     * @param coords    list of coordinates whose Cells should be updated
     * @param aliveness the alive state to assign (true = alive, false = dead)
     */
    public void setState(List<Coord> coords, boolean aliveness) {

        for (Coord c : coords) {
            Cell cell = board.getTile(c).getCell();
            cell.setAlive(true);
        }
        this.snapCells();
    }

    /**
     * Returns the unique database identifier for this generation snapshot.
     *
     * @return the primary key of this Generation entity
     */
    public Long getId() {
        return id;
    }

    /**
     * Retrieves the zero-based index of this generation in the game sequence.
     *
     * @return the generation’s step number (0 for the initial generation)
     */
    public int getStep() {
        return step;
    }

    /**
     * Returns the Game instance that owns this generation.
     *
     * @return the parent Game to which this Generation belongs
     */
    public Game getGame() {
        return game;
    }

    /**
     * Sets the owning Game for this Generation.
     *
     * Establishes the back-reference from this Generation to its Game,
     * ensuring proper linkage when rebuilding or persisting the generation history.
     *
     * @param game the Game instance to associate with this Generation
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * Returns the Board context used for this generation.
     *
     * @return the Board associated with this Generation
     */
    public Board getBoard() {
        return board;
    }

    // EXTENDED BEHAVIORS

    /**
     * Creates the initial generation (step 0) with coordinate-specified cell types
     * marked alive, captures their states, and registers the generation in the
     * game.
     *
     * @param game         the Game instance to initialize
     * @param board        the Board context for the new generation
     * @param cellTypesMap a map from Coord to CellType indicating which types to
     *                     create and set alive
     * @return a new Generation representing step 0 with the given cell types alive
     * @throws ExtendedGameOfLifeException if game, board, or cellTypesMap is null
     */
    public static Generation createInitial(Game game, Board board, Map<Coord, CellType> cellTypesMap) {
        Objects.requireNonNull(game, "Game cannot be null");
        Objects.requireNonNull(board, "Board cannot be null");
        Objects.requireNonNull(cellTypesMap, "cellTypesMap cannot be null");
        List<Coord> aliveCells = new ArrayList<>();
        
        //set the type of the cells
        for (Map.Entry<Coord, CellType> entry : cellTypesMap.entrySet()) {
            Coord coord = entry.getKey();
            CellType type = entry.getValue();
            Cell cell = board.getTile(coord).getCell();
            cell.setType(type);
            aliveCells.add(coord);
        }

        game.clearGenerations();
        Generation init = new Generation(game, board, 0);
        init.setState(aliveCells, true);
        init.snapCells();
        game.addGeneration(init, 0);
        return init;
    }

    /**
     * Retrieves one sort of step generation's energy (lifePoints) values for all cells in this
     * generation.
     *
     * @return a Map from Cell to its Integer lifePoints value
     */
    public Map<Cell, Integer> getEnergyStates() {
        /*
         contemparory generation have stored the energy(lifepoints) into energySnapshot,
        so it is not necessary to iterate each cell again here to collect energy of cells, 
        otherwise it will change the cells' lifepoints in recording table

        because this method only used in test part, and according to the test part, 
        it will receive the cells' lifepoints in generation table
        but if we use for loop, it will record the newest lifepoints
        rather than specific lifepoints after evolve and event

        */ 
        return Map.copyOf(energySnapshot); 
    }

    /**
     * Returns an immutable snapshot of each cell’s alive/dead state.
     *
     * @return a Map from Cell to Boolean indicating aliveness (true = alive, false
     *         = dead)
     */
    public Map<Cell, Boolean> getCellAlivenessStates() {
    return Map.copyOf(cellAlivenessStates); 
    }

    /**
     * Retrieves the current mood of each cell in this generation.
     *
     * @return a Map from Cell to CellMood representing each cell’s interaction
     *         style
     * @throws UnsupportedOperationException until implemented
     */
    public Map<Cell, CellMood> getMoodStates() {
        Map<Cell, CellMood> moodMap = new HashMap<>();
        // For each cell on the board, record its current mood
        for (Cell cell : board.getCellSet()) {
            moodMap.put(cell, cell.getMood());
        }
        return moodMap;
    }

    /**
     * Injects the persistent map of cell aliveness states loaded by Hibernate.
     *
     * @param cellAlivenessStates a Map from Cell to Boolean indicating each cell’s
     *                            alive/dead state
     */
    public void setCellAlivenessStates(Map<Cell, Boolean> cellAlivenessStates) {
        this.cellAlivenessStates = cellAlivenessStates;
    }

}
