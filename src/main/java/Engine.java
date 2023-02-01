import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 * An instance of Cplex engine for Travelling-Salesman Problem
 */
public class Engine {
    public static Engine singleton;
    public IloCplex cplex;

    // 1. Cplex configuration
    public static final int MAX_TIME = 60 * 3; // max computing time = 3 minutes
    public static final int MAX_MEMORY = 5000; // max memory used = 500 Mo
    public static final double INTEGER_TOLERANCE = 0.0; // no real approximation

    // 2. Private constructor (reduce memory usage and computing time)
    private Engine() throws IloException {
        this.cplex = new IloCplex();
        cplex.setParam(IloCplex.Param.MIP.Tolerances.Integrality,INTEGER_TOLERANCE);
        cplex.setParam(IloCplex.Param.TimeLimit,MAX_TIME);
        cplex.setParam(IloCplex.Param.MIP.Limits.TreeMemory,MAX_MEMORY);
        cplex.setParam(IloCplex.Param.WorkMem,MAX_MEMORY);
    }

    // 3. Get single instance
    public static Engine get() throws IloException {
        if(singleton == null) singleton = new Engine();
        return singleton;
    }

    /**
     * Solve the instance
     * @param i
     * @return Instance i
     */
    public Instance solve(Instance i){
        long start = System.currentTimeMillis();
        try {
            buildObjectiveFunction(); // 1. Build the objective function based on i
            buildConstraints(); // 2. Build the constraints function based on i
            if(cplex.solve()){ // 3. Cplex found a feasible or optimal solution
                cplex.output().println("SUCCESS ! Solution status = " + cplex.getStatus()); // 3.1 Print the status of the solution
                i.solution.displayPath(cplex, i.start); // 3.2 Display the details of the solution
            } else // 4. Cplex didn't found any solution
                cplex.output().println("ERROR ! Solution status = " + cplex.getStatus());
            double end = (System.currentTimeMillis() - start)/1000.0;
            cplex.output().println("End of computation after: "+end+" seconds"); // 5. Display the final computing time
        } catch (IloException e) {
            e.printStackTrace();
        }
        cplex.end(); // 6. Close the Cplex engine
        return i;
    }

    public void buildObjectiveFunction(){

    }

    public void buildConstraints(){

    }

}