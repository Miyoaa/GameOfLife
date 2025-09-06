package it.polito.extgol;

/**
 * Defines the types of cells, each with distinct behaviors in the extended Game of Life.
 */
public enum CellType {
    
    /**
     * Standard Conway cell: follows default Game of Life rules.
     */
    BASIC,
    
    /**
     * High-energy cell: it can withstand death-inducing conditions for three generations.
     */
    HIGHLANDER,
    
    /**
     * Isolationist cell: survives with as few as one neighbor.
     */
    LONER,
    
    /**
     * Crowd-loving cell: can survive even in highly populated situations,
     * up to eight neighbors.
     */
    SOCIAL
}