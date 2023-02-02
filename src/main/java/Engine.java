import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An instance of Cplex engine for Travelling-Salesman Problem
 */
public class Engine {
    public static Engine singleton = null;
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
            buildObjectiveFunction(i); // 1. Build the objective function based on i
            buildConstraints(i); // 2. Build the constraints function based on i
            cplex.exportModel("./model.lp"); // 3. Export the model to read it
            if(cplex.solve()){ // 4. Cplex found a feasible or optimal solution
                cplex.output().println("SUCCESS ! Solution status = " + cplex.getStatus()); // 4.1 Print the status of the solution
                i.solution.displayPath(cplex, i); // 4.2 Display the details of the solution
            } else // 5. Cplex didn't found any solution
                cplex.output().println("ERROR ! Solution status = " + cplex.getStatus());
            double end = (System.currentTimeMillis() - start)/1000.0;
            cplex.output().println("End of computation after: "+end+" seconds"); // 6. Display the final computing time
        } catch (IloException e) {
            e.printStackTrace();
        }
        cplex.end(); // 7. Close the Cplex engine
        return i;
    }

    /**
     * Minimize the total distance of all selected paths
     * @param i
     * @throws IloException
     */
    public void buildObjectiveFunction(Instance i) throws IloException {
        IloLinearNumExpr obj = cplex.linearNumExpr();
        for(int v1=0; v1<i.paths.length -1; v1++) {
            for (int v2 = v1+1; v2 < i.paths.length; v2++) {
                obj.addTerm(i.paths[v1][v2], i.solution.selectedPaths[v1][v2]);
                obj.addTerm(i.paths[v2][v1], i.solution.selectedPaths[v2][v1]);
            }
        }
        cplex.addMinimize(obj);
    }

    /**
     * Build exactly three constraints
     * Dantzig–Fulkerson–Johnson formulation
     * @param i
     * @throws IloException
     */
    public void buildConstraints(Instance i) throws IloException {
        for(int v1=0; v1<i.paths.length; v1++) {
            IloLinearNumExpr totalFromCity = cplex.linearNumExpr();
            IloLinearNumExpr totalToCity   = cplex.linearNumExpr();
            for (int v2=0; v2<i.paths.length; v2++) {
                if(v1 != v2){
                    totalFromCity.addTerm(1, i.solution.selectedPaths[v1][v2]);
                    totalToCity.addTerm(1, i.solution.selectedPaths[v2][v1]);
                }
            }
            cplex.addEq(1, totalFromCity, "C1_"+v1); // C1. Exactly one (no more no less) path is selected to go to a city
            cplex.addEq(1, totalToCity, "C2_"+v1); // C2. Exactly one (no more no less) path is selected to go from a city
        }

        // C3. There is only one start and hence one tour (no sub-tours when more than 2 cities)
        int[][] subsets = buildsubsets(i.paths.length);
        cplex.output().println("All subsets to test: ");
        cplex.output().println(Arrays.deepToString(subsets));
        for(int sub=0; sub<subsets.length; sub++){
            int[] subset = subsets[sub];
            IloLinearNumExpr totalPaths = cplex.linearNumExpr();
            for(int v1=0; v1<subset.length; v1++)
                for (int v2=0; v2<subset.length; v2++)
                    if (v1 != v2) totalPaths.addTerm(1, i.solution.selectedPaths[subset[v1]][subset[v2]]);
            cplex.addLe(totalPaths, subset.length - 1, "C3_"+Arrays.toString(subset));
        }
    }

    /**
     * Build all the subsets
     * @param nbrCities
     * @return int[][] subsets
     */
    public int[][] buildsubsets(int nbrCities){
        int[][] subsets = new int[nbrSubtours(nbrCities)][];
        int i = 0;
        for(int currentSize=2; currentSize<nbrCities; currentSize++){
            int[][] subsetsBySize = buildPosition(
                    initSubset(nbrSubtours(nbrCities,currentSize),currentSize),
                    0,0, nbrCities,currentSize-1,0);
            for(int j=0; j<subsetsBySize.length; j++)
                subsets[i+j] = subsetsBySize[j];
            i += subsetsBySize.length;
        }
        return subsets;
    }

    /**
     * Build the level position of all subsets starting from startingCity (with a total of citiesToTest)
     * @param subsets
     * @param startingCity
     * @param citiesToTest
     * @param level
     * @returnint[][] subsets
     */
    public int[][] buildPosition(int[][] subsets, int startSubsets, int startingCity, int citiesToTest, int level, int positionToFill){
        int nbrAppearanceAtPosition = 0;
        int firstAppearance = startSubsets;
        for(int city=startingCity; city<citiesToTest; city++){
            firstAppearance = firstAppearance + nbrAppearanceAtPosition;
            nbrAppearanceAtPosition = nbrSubsets(level,citiesToTest,city);
            for(int s=firstAppearance; s<firstAppearance+nbrAppearanceAtPosition; s++)
                subsets[s][positionToFill] = city;
            if(level>0)
                buildPosition(subsets,firstAppearance,city+1,citiesToTest,level-1, positionToFill+1);
        }
        return subsets;
    }

    /**
     * Compute the number of subsets, hence the number of appearance of city at level
     * @param level
     * @param currentNbrCities
     * @param city
     * @return
     */
    public int nbrSubsets(int level, int currentNbrCities, int city){
        if(level <= 0) return 1;
        int totalSubsets = 0;
        for(int nextCity = city+1; nextCity<currentNbrCities; nextCity++)
            totalSubsets += nbrSubsets(level-1, currentNbrCities, nextCity);
        return totalSubsets;
    }

    /**
     * Nbr possible subtours for nbrCities
     * @param nbrCities
     * @return int
     */
    public int nbrSubtours(int nbrCities){
        int total = 0;
        for(int subSize=2; subSize<nbrCities; subSize++)
            total += nbrSubtours(nbrCities, subSize);
        return total;
    }

    /**
     * Nbr subtours of size subsize
     * @param nbrCities
     * @param subSize
     * @return int
     */
    public int nbrSubtours(int nbrCities, int subSize){
        return factorial(nbrCities)/(factorial(subSize) * factorial(nbrCities-subSize));
    }

    /**
     * Init subsets
     * @param s1
     * @param s2
     * @return int[][] subsets
     */
    public int[][] initSubset(int s1, int s2){
        int[][] subsets = new int[s1][s2];
        for(int s=0; s<s1; s++)
            subsets[s] = new int[s2];
        return subsets;
    }

    /**
     * Factorial computation (RECURSIVE VERSION)
     * @param n
     * @return n!
     */
    public int factorial(int n) {
        if (n<=0) return 1;
        return n * factorial(n-1);
    }
}