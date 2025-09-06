package it.polito.extgol;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

/**
 * Facade coordinating the core operations of the Extended Game of Life simulation.
 *
 * This class provides high-level methods to:
 *   - Evolve a single generation or advance multiple steps.
 *   - Visualize the board state and retrieve alive cells by coordinate.
 *   - Persist and reload entire game instances.
 */
public class ExtendedGameOfLife {

    private Game currentGameFlag;

    /**
     * Computes and returns the next generation based on the current one.
     *
     * The method follows these steps:
     *   1. Validates that the current generation has an associated Board and Game.
     *   2. Computes the next alive/dead state for each cell based solely on the current state.
     *   3. Creates a new Generation object representing the next simulation step.
     *   4. Applies all calculated state changes simultaneously, ensuring consistency.
     *   5. Captures a snapshot of all cells' states into the persistent map for future retrieval.
     *
     * @param current The current generation snapshot used for evolving to the next state.
     * @return A new Generation object reflecting the evolved board state.
     * @throws IllegalStateException If Generation is not properly initialized.
     */
    public Generation evolve(Generation current) {
        Objects.requireNonNull(current, "Current generation cannot be null");

        Board board = current.getBoard();
        Game game = current.getGame();

        if(board == null || game == null){
            throw new IllegalStateException("Generation must have associated Board and Game!");
        }

        // Step 0: Tile interaction with the cell on it
        for (Tile tile : board.getTiles()) {
            if (tile.getCell() == null) {   //check for exception case
                throw new IllegalStateException("Missing cell on tile " + tile.getId());
            }
            tile.interact(tile.getCell());  //makes the tile interact with the cell
        }

        // Step 0.1: Each cell interacts with its neighbors cells
        for (Tile t : board.getTiles()) {
            Cell c = t.getCell();
            if (!c.isAlive()) continue;
            // for each neighbor in fixed order (board.getTiles() is already row-major)
            for (Tile nt : t.getNeighbors()) {
                Cell n = nt.getCell();
                if (n.isAlive()) {
                    c.interact(n);  //interaction between cells
                }
            }
        }

        EventType prevEvent = current.getEvent();
        // Step 1: Compute next state for each cell based only on current generation state
        Map<Cell, Boolean> nextStates = new HashMap<>();
        for (Tile tile : board.getTiles()) {
            Cell c = tile.getCell();

            int aliveNeighbors = c.countAliveNeighbors();
            boolean nextState;
            if(prevEvent ==EventType.FAMINE && !c.isAlive()){
                nextState = false;
            }else{
                nextState = c.evolve(aliveNeighbors);
            }

            nextStates.put(c, nextState);
        }
        
        // Step 2: Instantiate the next Generation based on current
        Generation nextGen = Generation.createNextGeneration(current);

        // Step 3: Apply all computed states simultaneously to avoid intermediate inconsistencies
        for (Map.Entry<Cell, Boolean> e : nextStates.entrySet()) {
            Cell c = e.getKey();
            c.setAlive(e.getValue());
            c.addGeneration(nextGen);  // register cell with new generation
        }

        // Step 4: Persist snapshot of the next generation state
        nextGen.snapCells();

        return nextGen;
    }

    /**
     * Advances the simulation by evolving the game state through a given number of steps.
     *
     * Starting from the game's initial generation, this method repeatedly computes the next
     * generation and appends it to the game's history.
     *
     * @param game  The Game instance whose generations will be advanced.
     * @param steps The number of evolution steps (generations) to perform.
     * @return The same Game instance, updated with the new generation.
     */
    public Game run(Game game, int steps) { // without events
        this.currentGameFlag = game;
        Generation current = game.getStart();
        for (int i = 0; i < steps; i++) {
            Generation next = evolve(current);
            current = next;
        }
        return game;
    }

    /**
     * Advances the simulation by evolving the game state through a given number of steps.
     *
     * Starting from the game's initial generation, this method repeatedly computes the next
     * generation and appends it to the game's history. 
     * 
     * It applies any events at their scheduled generations.
     *
     * At each step:
     *   1. If an event is scheduled for the current step (according to eventMap), the
     *      corresponding event is applied to all tiles before evolution.
     *   2. The board then evolves to the next generation, which is added to the game.
     *
     * @param game      The Game instance to run and update.
     * @param steps     The total number of generations to simulate.
     * @param eventMap  A map from generation index (0-based) to the EventType to trigger;
     *                  if a step is not present in the map, no event is applied that step.
     * @return          The same Game instance, now containing the extended generation history.
     */
    public Game run(Game game, int steps, Map<Integer, EventType> eventMap) {   //with events
        Objects.requireNonNull(game, "game must not be null");
        Generation currentGen = game.getStart();
        if (steps<0) {
            return game;
        }

        Map<Integer, EventType> schedule = eventMap == null ? Map.of() : eventMap;
        
        for(int i =0; i<steps; i++){
            // Events impact all tiles on the board at the beginning of each generation, 
            // affecting the evolution of its associated CELL into the next
            
           // if we met an event, we should process cell with unrollEvent
           EventType event = schedule.get(i);
           
           if(event != null){
                for(Tile t : currentGen.getBoard().getTiles()){ //Retrieve all tiles in order to apply the event logic
                        game.unrollEvent(event, t.getCell());
                }
                currentGen.setEvent(event); 
                game.scheduleEvent(i, event);
           }

            currentGen.snapCells(); // record lifePoints and
            game.sanctuaryEffect(currentGen);
            // evolve to next generation
            Generation nextGen = evolve(currentGen);

           // 3. trigger BLOOD MOON if bloodMoonActive exists
           game.bloodMoonEffects(nextGen);
           // going to next generation.
           currentGen = nextGen;
        }
        return game;
    }

    /**
     * Builds and returns a map associating each coordinate with its alive Cell 
     * instance for the specified generation.
     *
     * Iterates over all alive cells present in the given generation and constructs 
     * a coordinate-based map, facilitating cell access.
     *
     * @param generation The generation whose alive cells are mapped.
     * @return A Map from Coord (coordinates) to Cell instances representing all alive cells.
    */

    public Map<Coord, Cell> getAliveCells(Generation generation) {
        Map<Coord, Cell> alive = new HashMap<>();
        for (Cell c : generation.getAliveCells()) {
            alive.put(c.getCoordinates(), c);
        }
        return alive;
        /*
         * Maps the coordinates (x, y) of currently alive cells to their corresponding Cell instance, i.e., key: (x, y) -> value: Cell

        */
    }

    /**
     * Generates a visual string representation of the specified generation's board state.
     *
     * It produces a multi-line textual snapshot showing cells and their status.
     * "C" -> alive cell
     * "0" -> dead cell
     *
     * @param generation The Generation instance to visualize.
     * @return A multi-line String-based representiion of the board's current state.
    */
    public String visualize(Generation generation) {
        return generation.getBoard().visualize(generation);
    }

    /**
     * Persists the complete state of the provided Game instance, including its Board, Tiles,
     * Cells, and all associated Generations.
     *
     * If the Game is new, it will be created and persisted.
     * Otherwise, its state will be updated (merged) in the database. Ensures transactional 
     * safety and consistency through commit and rollback handling.
     *
     * @param game The Game instance to persist or update.
     */
    public void saveGame(Game game) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (game.getId() == null) {
                em.persist(game);
            } else {
                em.merge(game);
            }
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            this.currentGameFlag = game;
            em.close();
        }
    }

    /**
     * Loads and returns a persisted map of game events keyed by generation step.
     *
     * Delegates retrieval to the corresponding repository class, which in turn implements 
     * the provided generic repository class for persistence. This method reconstructs 
     * the event timeline for inspection or replay.
     *
     * @return A Map<Integer, EventType> mapping generation steps to associated events.
     */

    public Map<Integer, EventType> loadEvents() {
        // it probably happen game -> null situation, so we have to check it and then load it to game.class method
        if(currentGameFlag ==null || currentGameFlag.getId() == null)
            throw new IllegalStateException("No game is running");
        // passing it to game loadEvents method to implement it in order to get  mapping generation steps to associated events.
        return Game.loadEvents(currentGameFlag);
    }
}
