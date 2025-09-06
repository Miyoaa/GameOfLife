package it.polito.extgol;

/**
 * Interface defining cell-to-cell interactions during evolution.
 * 
 * Implemented by Cell classes to model how cells interact.
 */
public interface Interactable {

    /**
     * Applies an interaction from this cell onto another cell.
     *
     * @param other the neighboring Cell involved in the interaction
     */
    void interact(Cell other);

}