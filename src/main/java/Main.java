import ilog.concert.IloException;

/**
 * Main class
 */
public class Main {
    public static final int NUMBER_CITIES = 6; // 1. Four cities
    public static final int MAX_DISTANCE = 15; // 2. Maximum 15 miles
    public static final int MIN_DISTANCE = 1; // 3. Minimum 1 miles

    /**
     * Main Method
     * @param args
     */
    public static void main(String[] args) {
        try {
            Engine.get().solve(Instance.randomInit(Engine.get().cplex, NUMBER_CITIES, MIN_DISTANCE, MAX_DISTANCE));
        } catch (IloException e) {
            e.printStackTrace();
        }
    }
}