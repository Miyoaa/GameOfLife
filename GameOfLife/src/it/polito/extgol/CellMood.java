package it.polito.extgol;

/**
 * Defines the interaction style or “mood” of a cell, influencing how it
 * exchanges lifePoints with other cells during the interaction phase.
 */
public enum CellMood {

    /**
     * Default state with no special interaction effects.
     * Does not grant or drain lifePoints when interacting.
     */
    NAIVE,

    /**
     * Drains lifePoints from other cells upon interaction.
     */
    VAMPIRE,

    /**
     * Grants lifePoints to other cells upon interaction.
     */
    HEALER
}