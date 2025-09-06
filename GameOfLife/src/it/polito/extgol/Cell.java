package it.polito.extgol;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

/**
 * Entity representing a cell in the Extended Game of Life.
 *
 * Serves as the base class for all cell types, embedding its board coordinates,
 * alive/dead state, energy budget (lifePoints), and interaction mood.
 * Each Cell is linked to a Board, Game, Tile, and a history of Generations.
 * Implements Evolvable to apply Conway’s rules plus energy checks each
 * generation,
 * and Interactable to model cell–cell energy exchanges.
 */
@Entity
public class Cell implements Evolvable, Interactable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * In-memory coordinates, persisted as two columns cell_x and cell_y.
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "cell_x", nullable = false)),
            @AttributeOverride(name = "y", column = @Column(name = "cell_y", nullable = false))
    })
    private Coord cellCoord;

    /** Persisted alive/dead state */
    @Column(name = "is_alive", nullable = false)
    protected Boolean isAlive = false;

    /** Persisted lifepoints (default 0) */
    @Column(name = "lifepoints", nullable = false)
    protected Integer lifepoints = 0;

    /** Reference to the parent board (read-only). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false, updatable = false)
    protected Board board;

    /** Reference to the owning game (read-only). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, updatable = false)
    protected Game game;

    /** Transient list tracking generations this cell belongs to. */
    @Transient
    protected List<Generation> generations = new ArrayList<>();

    /** Back-reference: Tile owns the foreign key mapping. */
    @OneToOne(mappedBy = "cell", fetch = FetchType.LAZY)
    protected Tile tile;

    //attribute cellType
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CellType cellType;

    //attribute cellMood
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CellMood cellMood;

    /*
     * Counts the consecutive generations during which a Highlander
     * has survived in lethal conditions. This value must be persisted
     * in the database to maintain state across loads.
    */
    @Column(name = "death_count", nullable = false)
    private int deathCount = 0;     //only used for the evolve of the HIGHLANDER

    // flag for checking if it the cell will transfor its mood in vampire during the next generation
    @Transient  //not persisted in the database
    private boolean willBeVampire = false;

    /** Default constructor for JPA compliance. */
    public Cell() {
    }

    /**
    * Copy constructor for creating a deep copy of a cell (for generation snapshots).
    */
    public Cell(Cell other) {
        this.cellCoord = other.getCoordinates();
        this.isAlive = other.isAlive();
        this.lifepoints = other.getLifePoints();
        this.deathCount = other.deathCount;
        this.cellType = other.getType();
        this.cellMood = other.getMood();
        this.tile = other.tile;
        this.board = other.board;
        this.game = other.game;
    }

    /**
     * Constructs a new Cell at given coordinates, defaulting to dead.
     * 
     * @param coord the cell's coordinates
     */
    public Cell(Coord tileCoord) {
        this.cellCoord = tileCoord;
        this.isAlive = false;
        this.cellType = CellType.BASIC; //starts as a BASIC cellType
        this.cellMood = CellMood.NAIVE; //starts as a NAIVE cellMood
    }

    /**
     * Constructs a new Cell with its tile, board, and game context.
     * 
     * @param coord the cell's coordinates
     * @param tile  the owning Tile
     * @param board the Board context
     * @param game  the owning Game
     */
    public Cell(Coord tileCoord, Tile t, Board b, Game g) {
        this.cellCoord = tileCoord;
        this.isAlive = false;
        this.tile = t;
        this.board = b;
        this.game = g;
        this.cellType = CellType.BASIC; //starts as a BASIC cellType
        this.cellMood = CellMood.NAIVE; //starts as a NAIVE cellMood
    }

    /**
     * Applies the classic Conway’s Game of Life rules to calculate the cell’s next
     * alive/dead state.
     *
     * Rules:
     * - Underpopulation: A live cell with fewer than 2 neighbors dies.
     * - Overpopulation: A live cell with more than 3 neighbors dies.
     * - Respawn: A dead cell with exactly 3 neighbors becomes alive.
     * - Survival: A live cell with 2 or 3 neighbors stays alive.
     *
     * @param aliveNeighbors the count of alive neighboring cells
     * @return true if the cell will live, false otherwise
     */
    @Override
    public Boolean evolve(int aliveNeighbors) {
        // Assume the cell retains its current state by default
        Boolean willLive = this.isAlive;

        // kills a cell if lifepoints<0
        if (willLive && lifepoints < 0) {
            willLive = false;
            lifepoints--;
            if(this.getType() == CellType.HIGHLANDER){  //only for HIGHLANDER
                deathCount=0;
            }
        }

        switch (this.cellType) {    //evolve changes between cellTypes
            case BASIC:
                // Overpopulation: more than 3 neighbors kills a live cell
                if (willLive && aliveNeighbors > 3) {
                    willLive = false;
                    lifepoints--;
                }
                // Underpopulation: fewer than 2 neighbors kills a live cell
                else if (willLive && aliveNeighbors < 2) {
                    willLive = false;
                    lifepoints--;
                }
                // Respawn: exactly 3 neighbors brings a dead cell to life
                else if (!willLive && aliveNeighbors == 3) {
                    willLive = true;
                    lifepoints = 0;
                }
                // Survival: live cell with 2 or 3 neighbors stays alive
                else if (willLive && (aliveNeighbors == 2 || aliveNeighbors == 3)) {
                    lifepoints++;
                }
                break;

            case HIGHLANDER:
                if (willLive) {
                    // Lethal condition: fewer than 2 or more than 3 neighbors
                    if (aliveNeighbors < 2 || aliveNeighbors > 3) {
                        deathCount++;
                        // Only die after three consecutive lethal generations
                        if (deathCount >= 3) {
                            willLive = false;
                            lifepoints--;
                            deathCount = 0;
                        } else {
                            // Survive this lethal generation
                            willLive = true;
                        }
                    } else {
                        // Normal survival resets the death count and increases energy
                        deathCount = 0;
                        lifepoints++;
                    }
                } else {
                    // A dead cell can only respawn with exactly 3 neighbors
                    if (aliveNeighbors == 3) {
                        willLive = true;
                        lifepoints = 0;
                        deathCount = 0;
                    }
                }
                break;

            case LONER:
                if (willLive) {
                    // Loner survives with at least 1 neighbor and at most 3
                    if (aliveNeighbors < 1 || aliveNeighbors > 3) {
                        willLive = false;
                        lifepoints--;
                    } else {
                        lifepoints++;
                    }
                } else {
                    // Dead cell respawns only with exactly 3 neighbors
                    if (aliveNeighbors == 3) {
                        willLive = true;
                        lifepoints = 0;
                    }
                }
                break;

            case SOCIAL:
                if (willLive) {
                    // Social survives with between 2 and 8 neighbors
                    if (aliveNeighbors < 2 || aliveNeighbors > 8) {
                        willLive = false;
                        lifepoints--;
                    } else {
                        lifepoints++;
                    }
                } else {
                    // Dead cell respawns only with exactly 3 neighbors
                    if (aliveNeighbors == 3) {
                        willLive = true;
                        lifepoints = 0;
                    }
                }
                break;
        }
        
        //changes the mood to --> VAMPIRE if the flag is set
        if (this.willBeVampire) {
            this.cellMood = CellMood.VAMPIRE;
            this.willBeVampire = false;
        }

        return willLive;    // return the next alive/dead status
    }


    /**
     * Retrieves all tiles adjacent to this cell's tile.
     *
     * This method returns a copy of the underlying neighbor list to ensure
     * external code cannot modify the board topology.
     *
     * @return an immutable List of neighboring Tile instances
     */
    public List<Tile> getNeighbors() {
        return List.copyOf(tile.getNeighbors());
    }

    /**
     * Counts the number of live cells adjacent to this cell’s tile.
     *
     * Iterates over all neighboring tiles and increments the count for each
     * tile that hosts an alive Cell.
     *
     * @return the total number of alive neighboring cells
     */
    public int countAliveNeighbors() {
        int count = 0;
        for (Tile t : tile.getNeighbors()) {
            if (t.getCell() != null && t.getCell().isAlive())
                count++;
        }
        return count;
    }

    /**
     * Registers this cell in the specified generation’s back-reference list.
     *
     * Used internally by the ORM to maintain the relationship between
     * cells and the generations they belong to. Adds the given generation
     * to the cell’s internal history.
     *
     * @param gen the Generation instance to associate with this cell
     */
    void addGeneration(Generation gen) {
        generations.add(gen);
    }

    /**
     * Provides an unmodifiable history of all generations in which this cell has
     * appeared.
     *
     * Returns a copy of the internal list to prevent external modification
     * of the cell’s generation history.
     *
     * @return an immutable List of Generation instances tracking this cell’s
     *         lineage
     */
    public List<Generation> getGenerations() {
        return List.copyOf(generations);
    }

    /**
     * Returns the X coordinate of this cell on the board.
     *
     * @return the cell’s X position
     */
    public int getX() {
        return this.cellCoord.getX();
    }

    /**
     * Returns the Y coordinate of this cell on the board.
     *
     * @return the cell’s Y position
     */
    public int getY() {
        return this.cellCoord.getY();
    }

    /**
     * Retrieves the full coordinate object for this cell.
     *
     * @return a Coord instance representing this cell’s position
     */
    public Coord getCoordinates() {
        return this.cellCoord;
    }

    /**
     * Checks whether this cell is currently alive.
     *
     * @return true if the cell is alive; false if it is dead
     */
    public boolean isAlive() {
        return isAlive;
    }

    /**
     * Updates the alive/dead state of this cell.
     *
     * @param isAlive true to mark the cell as alive; false to mark it as dead
     */
    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }

    /**
     * Returns a string representation of this cell’s position in the format "x,y".
     *
     * Overrides Object.toString() to provide a concise coordinate-based
     * representation.
     * 
     * @return a comma-separated string of the cell’s X and Y coordinates
     */
    @Override
    public String toString() {
        return getX() + "," + getY();
    }

    // EXTENDED BEHAVIORS

    /**
     * Retrieves the current energy level of this cell.
     *
     * @return the number of life points the cell currently has
     */
    public int getLifePoints() {
        return this.lifepoints;
    }

    /**
     * Updates the energy level of this cell.
     *
     * @param lifePoints the new number of life points to assign to the cell
     */
    public void setLifePoints(int lifePoints) {
        this.lifepoints = lifePoints;
    }

    //Getter for willBeVampire
    public boolean getWillBeVampire() { 
        return willBeVampire; 
    }

    //Setter for willBeVampire
    public void setWillBeVampire(boolean flag) { 
        this.willBeVampire = flag; 
    }
 
    /**
     * Implements the interact() method of Interactable to
     * define the interaction between this cell and another cell.
     * Adjusts life points or mood based on the interaction rules.
     *
     * @param otherCell the Cell object to interact with
     */
    @Override
    public void interact(Cell otherCell) {
        if (otherCell == null || !this.isAlive || !otherCell.isAlive()) {
            return;
        }

        switch (this.cellMood) {
            case HEALER:
                if (otherCell.getMood() == CellMood.NAIVE) {
                    // Healer gives 1 lifePoint to a naive cell
                    otherCell.setLifePoints(otherCell.getLifePoints() + 1);
                } else if (otherCell.getMood() == CellMood.VAMPIRE) {
                    // Vampire drains 1 lifePoint from healer
                    this.setLifePoints(this.getLifePoints() - 1);
                    otherCell.setLifePoints(otherCell.getLifePoints() + 1);
                }
                break;

            case VAMPIRE:
                if (otherCell.getMood() == CellMood.NAIVE && otherCell.getLifePoints() >= 0) {
                    // Vampire bites naive only if target has non-negative lifePoints
                    otherCell.setLifePoints(otherCell.getLifePoints() - 1);
                    this.setLifePoints(this.getLifePoints() + 1);
                    otherCell.setWillBeVampire(true);
                } else if (otherCell.getMood() == CellMood.HEALER) {
                    // Vampire drains 1 lifePoint from healer
                    this.setLifePoints(this.getLifePoints() + 1);
                    otherCell.setLifePoints(otherCell.getLifePoints() - 1);
                }
                break;

            default:
                // NAIVE or any other mood: no action
                break;
        }
    }

    /**
     * Assigns a specific cell type to this cell, influencing its behavior.
     *
     * @param t the CellType to set (e.g., BASIC, HIGHLANDER, LONER, SOCIAL)
     */
    public void setType(CellType t) {
        this.cellType=t;
    }

    //setter for cellType
    public CellType getType() {
    return this.cellType;
    }

    /**
     * Sets the current mood of this cell, impacting how it interacts with others.
     *
     * @param mood the CellMood to assign (NAIVE, HEALER, or VAMPIRE)
     */
    public void setMood(CellMood mood) {
        this.cellMood = mood;
    }

    /**
     * Retrieves the current mood of this cell.
     *
     * @return the CellMood representing the cell’s interaction style
     */
    public CellMood getMood() {
        return this.cellMood;
    }

}
