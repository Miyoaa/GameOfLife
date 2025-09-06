package it.polito.extgol;

/**
 * Signals error conditions specific to the Extended Game of Life.
 *
 * Used to indicate invalid configurations or unexpected states
 * during board evolution or game setup.
 */
public class ExtendedGameOfLifeException extends Exception {

    /**
     * Constructs a new ExtendedGameOfLifeException with the specified detail message.
     *
     * @param message the detail message explaining the reason of the exception
     */
    public ExtendedGameOfLifeException(String message) {
        super(message);
    }

    /**
     * Constructs a new ExtendedGameOfLifeException with the specified detail message and cause exception.
     *
     * @param message the detail message explaining the reason of the exception
     * @param cause the original exception cause of this one
     */
    public ExtendedGameOfLifeException(String message, Throwable cause) {
        super(message, cause);
    }
}