import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;

public class Solution {
    public IloIntVar[][] selectedPaths;
    public IloIntVar totalDistance;

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
        for(int v1=0; v1<size; v1++)
            for(int v2=0; v2<size; v2++)
                s.selectedPaths[v1][v2] = cplex.boolVar("PATH_{"+v1+","+v2+"}");
        return s;
    }

    /**
     * Display the solution
     * @param cplex
     * @throws IloException
     */
    public void displayPath(IloCplex cplex, int from) throws IloException {
        int distance = (int) cplex.getValue(totalDistance);
        System.out.println("Total distance: "+distance);
        do{
            System.out.print(from);
            from = nextCity(cplex, from);
            if(from != -1) System.out.print("->");
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
          if((int) cplex.getValue(selectedPaths[from][to])>1)
              return to;
        return -1;
    }
}