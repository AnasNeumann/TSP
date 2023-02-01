import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.Random;

/**
 * An instance of the problem
 */
public class Instance {
    public int[][] paths;
    public int start;
    public Solution solution;

    /**
     * Randomly generate a new instance
     * @param cplex
     * @param size
     * @param minDistance
     * @param maxDistance
     * @return
     */
    public static Instance randomInit(IloCplex cplex, int size, int minDistance, int maxDistance) throws IloException {
        Instance i = new Instance(); // 1. create new problem
        i.paths = new int[size][size]; // 2. build the path with distances = 0
        i.start = new Random().nextInt(size); // 3. generate random start
        for(int v1=0; v1<size-1; v1++){
            i.paths[v1][v1] = 0;
            for(int v2=v1+1; v2<size; v2++){
                // 4. generate random distances between cities
                int distance = new Random().nextInt(maxDistance + 1) + minDistance;
                i.paths[v1][v2] = distance;
                i.paths[v2][v1] = distance;
            }
        }
        i.solution = Solution.init(size, cplex); // 5. init the solution
        return i;
    }
}
