package it.polito.extgol;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

/**
 * Entity representing a single square on the Game of Life board.
 * Holds coordinate position, occupying Cell, and link back to its Board.
 */
@Entity
public class Tile implements Interactable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Coordinates of the tile on the board. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x",
            column = @Column(name = "tile_x", nullable = false)),
        @AttributeOverride(name = "y",
            column = @Column(name = "tile_y", nullable = false))
    })
    private Coord tileCoord;

    /** Reference to the board containing this tile. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    /** Reference to the owning game. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, updatable = false)
    private Game game;


    /**
     * The cell occupying this tile.
     */
    @OneToOne(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY
    )
    @JoinColumn(name = "cell_id", nullable = false, unique = true) // Ensures that each Tile is associated with a unique Cell (one-to-one relationship)
    private Cell cell;

    /** Neighboring tiles for interactions. Not persisted. */
    @Transient
    private Set<Tile> neighbors = new HashSet<Tile>();

    //lifePointModifier
    @Column(nullable = false)   //persistence added for attribute lifePointModifier
    private Integer lifePointModifier;

    /**
     * Default constructor required by JPA.
     */
    public Tile() {
    }

    /**
     * Constructs a tile at the given coordinates,
     * and the respective cell initialized as not alive
     * @param x column index (0-based)
     * @param y row index (0-based)
     */
    public Tile(int x, int y, Board b, Game g) {
        this.tileCoord = new Coord(x,y);
        this.board = b;
        this.game = g;
        this.cell = new Cell(this.tileCoord, this, b, g);
        this.lifePointModifier=0;
    }

    /**
     * Retrieves the Board to which this tile belongs.
     *
     * @return the parent Board instance
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Associates this tile with the given Board.
     *
     * @param board the Board on which this tile resides
     */
    public void setBoard(Board board) {
        this.board = board;
    }

    /**
     * Retrieves the Cell currently occupying this tile.
     *
     * @return the Cell on this tile, or null if the tile is empty
     */
    public Cell getCell() {
        return cell;
    }

    /**
     * Places or removes a Cell on this tile.
     *
     * @param cell the Cell to assign to this tile, or null to clear it
     */
    public void setCell(Cell cell) {
        this.cell = cell;
    }

    /**
     * Checks whether this tile currently hosts a live cell.
     *
     * @return true if a non-null, alive Cell occupies this tile; false otherwise
     */
    public boolean hasCell() {
        return cell != null && cell.isAlive();
    }

    /**
     * Initializes the neighbor relationships for this tile.
     *
     * @param neighborsList the Set of tiles adjacent to this one
     */
    public void initializeNeighbors(Set<Tile> neighborsList) {
        this.neighbors = neighborsList;
    }

    /**
     * Provides the set of neighboring tiles around this tile.
     *
     * @return a Set of adjacent Tile instances
     */
    public Set<Tile> getNeighbors() {
        return this.neighbors;
    }

    /**
     * Returns the database identifier for this Tile.
     *
     * @return the unique ID of this tile
     */
    public Long getId() {
        return id;
    }

    /**
     * Retrieves the coordinate object representing this tile's position.
     *
     * @return a Coord containing the tile’s x and y values
     */
    public Coord getCoordinates() {
        return this.tileCoord;
    }

    /**
     * Returns the X‐coordinate (column index) of this tile.
     *
     * @return the x value of this tile’s coordinate
     */
    public int getX() {
        return this.tileCoord.getX();
    }

    /**
     * Returns the Y‐coordinate (row index) of this tile.
     *
     * @return the y value of this tile’s coordinate
     */
    public int getY() {
        return this.tileCoord.getY();
    }

    // EXTENDED BEHAVIORS
    
    /**
     * Retrieves the life point modifier that this tile applies to any cell occupying it.
     * 
     * A positive value grants extra energy to the cell each generation, while
     * a negative value drains energy. A zero value has no effect.
     *
     * @return the integer modifier to a cell’s lifePoints when evolving
     */
    public Integer getLifePointModifier() {
        return this.lifePointModifier;
    }

    public void setPointModifier (Integer modifier){
        this.lifePointModifier = modifier;
    }

    @Override
    public void interact(Cell cell) {
        if (cell != null && cell.isAlive()) {
            cell.setLifePoints(cell.getLifePoints() + this.lifePointModifier);
        }
    }

}
