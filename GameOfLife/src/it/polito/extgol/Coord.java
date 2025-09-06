package it.polito.extgol;

import jakarta.persistence.Embeddable;

/**
 * Represents a two-dimensional coordinate on the game board.
 * 
 * This class is marked @Embeddable so that its fields can be embedded
 * directly into owning entity tables (e.g., Cell or Tile).
 */
@Embeddable
public class Coord {

    /** The X (column) position on the board. */
    private int x;

    /** The Y (row) position on the board. */
    private int y;

    /**
     * Protected no-arg constructor required by JPA.
     * 
     */
    protected Coord() { }

    /**
     * Constructs a coordinate with the given X and Y values.
     *
     * @param x the column index
     * @param y the row index
     */
    public Coord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the X (column) component of this coordinate.
     *
     * @return the X position
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the Y (row) component of this coordinate.
     *
     * @return the Y position
     */
    public int getY() {
        return y;
    }

    /**
     * Compares this Coord to another for equality.
     * Two coordinates are equal if both their X and Y values match.
     *
     * @param o the object to compare against
     * @return true if o is a Coord with the same X and Y; false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Coord)) return false;
        Coord c = (Coord) o;
        return x == c.x && y == c.y;
    }

    /**
     * Computes a hash code consistent with equals().
     *
     * @return a hash code combining X and Y values
     */
    @Override
    public int hashCode() {
        return 31 * x + y;
    }
}