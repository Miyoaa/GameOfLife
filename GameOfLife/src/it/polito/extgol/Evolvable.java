package it.polito.extgol;

/**
 * Defines the contract for any cell-like entity that can evolve its alive/dead state
 * based on neighborhood conditions each generation.
 *
 * Implementing classes encapsulate the rules (e.g., Conway’s rules plus extensions)
 * that determine whether they will survive, die, or respawn in the next step.
 */
public interface Evolvable {

    /**
     * Determines the implementor’s next alive/dead state using its internal rules
     * and the count of currently alive neighbors.
     *
     * This method is invoked once per generation during the evolution phase.
     *
     * @param aliveNeighbors the number of adjacent cells that are alive
     * @return true if this cell will be alive in the next generation;
     *         false otherwise
     */
    Boolean evolve(int aliveNeighbors);
}