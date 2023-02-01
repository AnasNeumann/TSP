import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;

/**
 * A solution to the problem
 */
public class Solution {
    public IloIntVar[][] selectedPaths;

    /**
     * Init a new solution
     * @param size
     * @param cplex
     * @return Solution s
     * @throws IloException
     */
    public static Solution init(int size, IloCplex cplex) throws IloException {
        Solution s = new Solution();
        s.selectedPaths = new IloIntVar[size][size];
        for(int v1=0; v1<size-1; v1++) {
            for (int v2 = v1+1; v2 < size; v2++) {
                // Create only boolean variables: selected or not
                s.selectedPaths[v1][v2] = cplex.boolVar("PATH_{" + v1 + "," + v2 + "}");
                s.selectedPaths[v2][v1] = cplex.boolVar("PATH_{" + v2 + "," + v1 + "}");
            }
        }
        return s;
    }

    /**
     * Display the solution
     * @param cplex
     * @param i
     * @throws IloException
     */
    public void displayPath(IloCplex cplex, Instance i) throws IloException {
        cplex.output().println("Total distance: "+cplex.getObjValue());
        cplex.output().print("Path: ");
        int from = i.start;
        int to;
        do{
            cplex.output().print("City_"+from);
            to = nextCity(cplex, from);
            if(to != -1){
                cplex.output().print(" -> ("+i.paths[from][to]+") ");
                from = to;
            }
        } while(from != 1);
    }

    /**
     * Get the the next city of -1
     * @param cplex
     * @param from
     * @return int
     * @throws IloException
     */
    public int nextCity(IloCplex cplex, int from) throws IloException {
        for(int to=0; to<selectedPaths.length; to++)
          if(to != from && (int) cplex.getValue(selectedPaths[from][to])>0)
              return to;
        return -1;
    }
}