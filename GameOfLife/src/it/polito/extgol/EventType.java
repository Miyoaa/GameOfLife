package it.polito.extgol;

/**
 * Enumeration of global event types that can be applied to the entire board
 * during a generation, altering cells’ lifePoints and moods.
 */
public enum EventType {

    /** 
     * Sets every cell’s lifePoints to zero, simulating a catastrophic reset.
     */
    CATACLYSM,

    /** 
     * Absorbs 1 lifePoint from each cell, representing a period of scarcity.
     */
    FAMINE,

    /** 
     * Grants 2 lifePoints to each cell, causing a burst of growth.
     */
    BLOOM,

    /** 
     * Each Vampire cell steals 1 lifePoint from each adjacent Naive or Healer 
     * and converts them into Vampires.
     */
    BLOOD_MOON,

    /** 
     * All Healer cells gain 1 lifePoint, and all Vampire cells revert to Naive.
     */
    SANCTUARY
}