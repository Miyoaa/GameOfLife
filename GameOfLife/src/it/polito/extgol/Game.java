package it.polito.extgol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Entity representing a Game of Life simulation instance.
 * 
 * Maintains the board, generation history, and global event schedule for each
 * simulation. Provides factory methods for classic and extended game setups,
 * as well as operations for evolving and querying game state.
 */
@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable name for this game instance.
     */
     @Column(nullable = false, unique = true)
    private String name;

    /**
     * The board on which this game is played.
     *
     * One board per game; cascade so the board is persisted/removed along with the game.  
     * Stored in TILE table as board_id FK.  
     */
    @OneToOne(
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY,
        optional = false
        )
    @JoinColumn(name = "board_id", nullable = false, unique = true)
    private Board board;

    /**
     * List of all generations in this game, including the initial.
     * 
     * All generations (including initial) in time order.  
     * Uses an ORDER_COLUMN so the DB keeps the sequence.
     */
    @OneToMany(
      mappedBy       = "game",
      cascade        = CascadeType.ALL,
      orphanRemoval  = true,
      fetch          = FetchType.LAZY
    )
    @OrderColumn(name = "generation_index")
    private List<Generation> generations = new ArrayList<>();


    /**
     *  Map<Integer, EventType> is persisted to a table, so-called game_event_schedule
     * indicating key word map to the column of generation_step
     * indicating value word map to the column of event_type saving as string
     */
    @ElementCollection
    @CollectionTable(
        name = "game_event_schedule",
        joinColumns = @JoinColumn(name = "game_id")
    )
    @MapKeyColumn(name = "generation_step")
    @Column(name = "event_type")
    @Enumerated(EnumType.STRING)
    private Map<Integer, EventType> eventSchedule = new HashMap<>();

    private Boolean bloodMoonActive = false;
    private Boolean sanctuaryActive = false;

    /**
     * Default constructor for JPA.
     */
    protected Game() {
    }

    /**
     * Constructs a Game with the specified name.
     * Creates a default 5×5 Board and defers initialization of the first generation
     * to the associated factory or caller.
     *
     * @param name the human-readable name for this game instance
     */
    protected Game(String name) {
        this.name = name;
    }

    /**
     * Constructs a Game with the specified name and board dimensions.
     * Creates a new Board of the given size, associates it with this Game,
     * and initializes the initial Generation on that board.
     *
     * @param name   the human-readable name for this game instance
     * @param width  the number of columns for the game board
     * @param height the number of rows for the game board
     */
    public Game(String name, int width, int height) {
        this.name = name;
        this.board = new Board(width, height, this);
        Generation.createInitial(this, board);
    }

    /**
     * Factory method to create and fully initialize a Game instance.
     * Uses a protected constructor to set the name, builds a Board of the given size,
     * sets up the first Generation, and returns the ready-to-run Game.
     *
     * @param name   the human-readable name for this game instance
     * @param width  the number of columns for the game board
     * @param height the number of rows for the game board
     * @return a new Game configured with its board and initial generation
     */
    public static Game create(String name, int width, int height) {
        Game game = new Game(name);
        Board board = new Board(width, height, game);
        game.setBoard(board);
        Generation.createInitial(game, board);
        return game;
    }

    /**
     * Factory method to create and fully initialize an extended Game instance.
     * Creates a Game with the given name, constructs an extended Board using
     * specialized tiles and default cell settings, and initializes the first Generation.
     *
     * @param name   the human-readable name for this game instance
     * @param width  the number of columns for the game board
     * @param height the number of rows for the game board
     * @return a new Game configured with its extended board and initial generation
     */
    public static Game createExtended(String name, int width, int height) {
        Game game = new Game(name);
        // All tiles are made interactable with a lifePointModifier of 0
        // All cells are initialized with a NAIVE mood and BASIC cell type.
        Board board = Board.createExtended(width, height, game);
        game.setBoard(board);
        Generation.createInitial(game, board);
        return game;
    }

     /**
     * Appends a new Generation to the end of this game’s timeline.
     * Sets the generation’s back-reference to this Game before adding.
     *
     * @param generation the Generation instance to add to the sequence
     */
    public void addGeneration(Generation generation) {
        generation.setGame(this);
        generations.add(generation);
    }

    /**
     * Inserts a Generation at the specified index in the game’s timeline.
     * Shifts subsequent generations to higher indices. Sets the generation’s
     * back-reference to this Game before insertion.
     *
     * @param generation the Generation instance to insert
     * @param step       the zero-based index at which to insert this generation
     */
    public void addGeneration(Generation generation, Integer step) {
        generation.setGame(this);
        generations.add(step, generation);
    }

    /**
     * Removes all generations from this Game’s history.
     * After clearing, the game will have no recorded generations until new ones are added.
     */
    public void clearGenerations() {
        generations.clear();
    }

    /**
     * Retrieves the full history of generations in this game, in chronological order.
     *
     * @return a List of Generation instances representing each step in the simulation
     */
    public List<Generation> getGenerations() {
        return generations;
    }

    /**
     * Returns the unique identifier for this Game.
     *
     * @return the database ID of this game instance
     */
    public Long getId() {
        return id;
    }

    /**
     * Retrieves the human-readable name of this Game.
     *
     * @return the name assigned to this game
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the name of this Game.
     *
     * @param name the new human-readable name to assign
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the Board associated with this Game.
     *
     * @return the Board instance on which this game is played
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Associates a Board with this Game.
     *
     * @param b the Board instance to assign to this game
     */
    public void setBoard(Board b) {
        this.board = b;
    }

    /**
     * Returns the initial Generation of the game (step 0).
     *
     * @return the first Generation in the game’s sequence
     */
    public Generation getStart() {
        return this.generations.get(0);
    }

    /**
     * Applies the specified event to a single cell.
     *
     * @param event the EventType to apply
     * @param cell  the Cell instance to which the event should be applied
     */

    /*
     * Game
        └── List<Generation>
            ├─ Generation.step (int)
            ├─ Generation.event (EventType)    <-- weather trigger the event?
            └─ Generation.board (Board)
                            └── Board.cells (List<Cell>)
                                        ├─ Cell.cellType
                                        ├─ Cell.cellMood
                                        └─ Cell.lifePoints
    event effect next generation state of cell,Only alive CELLs can intereact.
     */
    public void unrollEvent(EventType event, Cell cell) {
        if (!cell.isAlive()){
            return;
        }
        switch (event) {
            case CATACLYSM: //  all Tiles reset all lifePoints from the Cell they hold to 0.
                cell.setLifePoints(0);
                break;  
            case FAMINE:// all Tiles absorb exactly 1 lifePoints from the Cell they hold.
                cell.setLifePoints(cell.getLifePoints() -1);
                if(cell.getLifePoints() < 0){
                    cell.setAlive(false);
                }  
                break;
            case BLOOM: // Tiles grant exactly 2 lifePoints to the cells sitting on them.
                cell.setLifePoints(cell.getLifePoints() + 2);
                break;
            case BLOOD_MOON: //VAMPIREs gain the ability to turn healers into VAMPIREs with their bite.
                // do not thing, special processing
                setBloodMoonActive(true);
                break;
            case SANCTUARY: 
                setSanctuaryActive(true);
                break;
            default:
                if (event == null || cell == null) throw new IllegalArgumentException("an unexpected event");
        }
    }

    /**
     * Applies sanctuary effect to the current generation's cells when sanctuary is active.
     * Healer cells gain +1 life point, vampire cells are converted to naive mood.
     * 
     * @param currentGen The current generation to apply effects to
     * @param sanctuaryActive Flag indicating if sanctuary is active (will be set to false after processing)
     */
    public void sanctuaryEffect(Generation currentGen){
        if ( !getSanctuaryActive() || currentGen ==null) {
            return;
        }

        Board board = currentGen.getBoard();
        for(Cell cell : board.getCellSet()){
            if(cell.isAlive() && cell.getMood() == CellMood.HEALER && cell != null){
                cell.setLifePoints(cell.getLifePoints() + 1);
            } else if( cell.isAlive() && cell.getMood() == CellMood.VAMPIRE && cell != null){
                cell.setMood(CellMood.NAIVE);
            }
        }
        setSanctuaryActive(false);
    }

    public void setBloodMoonActive( Boolean active){
        this.bloodMoonActive = active;
    }

    public boolean getBloodMoonActive(){
        return this.bloodMoonActive;
    }

    public void setSanctuaryActive( Boolean active){
        this.sanctuaryActive = active;
    }

    public boolean getSanctuaryActive(){
        return this.sanctuaryActive;
    }

    /**
     * A VAMPIRE bites a non-vampire only if the target currently has lifepoints ≥ 0. 
     * The bite instantly alters the target’s lifepoints
     * 
     * every alive VAMPIRE steals 1 lifepoint from each adjacent NAIVE or HEALER
     * for NAIVE, it will be turned into VAMPIRE in next generation, but we should separate to transfer them.
     * for HEALER, it will be turned into VAMPIRE ONLY in BLOODMOON event, as usual, in next generation.
     * 
     * @param bloodMoonActive: active the blood moon event
     * @param nextGen the next generation 
     */
    public void bloodMoonEffects(Generation nextGen){
        if (!getBloodMoonActive() || nextGen ==null) {
            return;
        }

        Board board = nextGen.getBoard(); // get the board from a generation
        // collect all cells in borad.
        Set<Cell> healerSet = new HashSet<>();

        // catch all vampire in the borad
        for(Cell vampire: board.getCellSet() ){
            if(!vampire.isAlive()) continue; // skip the dead cell
            if(vampire.getMood() != CellMood.VAMPIRE) continue; // check whether is a vampire, if not, skip this cell

            for (Tile neigh : vampire.getNeighbors()){
                // iterate neighbour cells
                Cell healer = neigh.getCell();
                if ( healer != null && (healer.getMood() == CellMood.HEALER) && healer.isAlive()){
                    healerSet.add(healer);
                }
            }
        }

        // we can turn HEALER into VAMPIRE here, because we obtain next generation.
        for (Cell h: healerSet){
            h.setMood(CellMood.VAMPIRE);
        }
        setBloodMoonActive(false);
    }

    /**
     * Sets the given mood for all cells at the specified coordinates.
     * Useful for scenarios like converting a batch of cells to VAMPIRE or HEALER.
     *
     * @param mood              the CellMood to assign (NAIVE, HEALER, or VAMPIRE)
     * @param targetCoordinates the list of coordinates of cells to update
     */
    public void setMood(CellMood mood, List<Coord> targetCoordinates) {
        //NOT TO IMPLEMENT (read on Discord Fixed messages)
    }

    /**
     * Assigns a common mood to multiple cells in one operation.
     * Currently unimplemented; will throw an exceunption until provided.
     *
     * @param mood        the CellMood to set (e.g., VAMPIRE)
     * @param coordinates the list of cell coordinates to update
     */
    public void setMoods(CellMood mood, List<Coord> coordinates) {
        if (board == null || coordinates == null || mood == null) return;
        for (Coord coord : coordinates) {
            Cell cell = board.getTile(coord).getCell();
            if (cell != null) {
                cell.setMood(mood);
            }
        }
    }

    /**
     * Retrieves the internal mapping of scheduled events for this game.
     * Each entry maps a generation index to an EventType.
     *
     * @return a mutable Map from generation step to EventType
     */
    public Map<Integer, EventType> getEventMapInternal() {
        return eventSchedule;
    }

    /**
     * Schedules an event to occur at a specific generation.
     * 
     * @param generation The generation number when the event should occur
     * @param event      The type of event to be scheduled
     */
    public void scheduleEvent(int generation, EventType event) {
        eventSchedule.put(generation, event);
    }

    /**
     * Loads the persisted event schedule for the given Game instance.
     * Delegates to the repository classes implementing
     * GenericExtGOLRepository to fetch the map of events
     * from the database, then returns it as an immutable map.
     *
     * @param game the detached Game instance whose events should be reloaded
     * @return an immutable Map from generation step to EventType
     */
    public static Map<Integer, EventType> loadEvents(Game game) {
        // game -> board-> tile->cell ->gen -> cellAlive -> event
        GameRepository repoTable = new GameRepository();
        Game persistedGame = repoTable.load(game.getId());
        Map<Integer, EventType> schedule = new HashMap<>(persistedGame.getEventMapInternal());
        return Collections.unmodifiableMap(schedule);
    }
}
