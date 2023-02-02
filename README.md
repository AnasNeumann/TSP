# TSP
A simple Travelling Salesman Problem to solve (optimize) with IBM Cplex for learning purposes

_"Given a list of cities and the distances between each pair of cities, what is the shortest possible route that visits each city exactly once and returns to the origin city?"_

*(Wikipedia.org, 2023)* - https://en.wikipedia.org/wiki/Travelling_salesman_problem

![problem-studied](/documentation/problem.png)

## I. Complete process to install and link Ilog Cplex
1. Download Cplex and Ilog Cplex from the official website: https://www.ibm.com/products/ilog-cplex-optimization-studio/cplex-optimizer and install the engine on your computer

2. Create a maven project (in any IDE) to generate a _pom.xml_ file (used to install external tools like Ilog Cplex)

3. Add the following entry to the _pom.xml_ file to install the Ilog Cplex engine and build from the src folder: 
```xml
<build>
    <sourceDirectory>src/main/java</sourceDirectory>
    <plugins>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.6.1</version>
            <configuration>
                <source>1.8</source>
                <target>1.8</target>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.2.4</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <transformers>
                            <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                <mainClass>gl.Main</mainClass>
                            </transformer>
                        </transformers>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
 </build>
 <dependencies>
    <dependency>
        <groupId>cplex</groupId>
        <artifactId>cplex</artifactId>
        <version>20.10</version> # select the right version!
    </dependency>
</dependencies>
```

4. Add the Ilog Cplex folder (downloaded from the IBM website) inside a /lib folder:

![cplex-folder](/documentation/cplex.png)

5. Run the commande "_Maven -> Reimport_" to link your code with Ilog Cplex (and detect the API classes to use)

6. Create a new Maven running configuration (in any IDE) with the following command line
```shell
# Select the correct local path to your main class
compile exec:java -D exec.mainClass=Main
```

7. Add the following "Runner/JVM configuration": 
```shell
# Select the correct path to your cplex instance
-Djava.library.path=/Applications/CPLEX_Studio201/cplex/bin/x86-64_osx 
```

## II. Create the data model (instance and solution)
8. Create a new  Instance.java class to model the problem
```java
import java.util.Random;

public class Instance {
    public int[][] paths;
    public int start;
}
```

9. Add to the instance a method to randomly generate a new problem
```java
public static Instance randomInit(int size, int minDistance, int maxDistance){
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
    return i;
}
```

10. Create a Solution.java class to save the selected paths and add an init method
```java
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;

public class Solution {
        public IloIntVar[][] selectedPaths;
    
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
    }
}
```

11. Update the init method to link the solution to the instance
```java
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
```

12. Create a recursive/iterative method to display the final solution
```java
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
    } while(from != -1 && from != i.start);
    cplex.output().println("City_"+from);
}

public int nextCity(IloCplex cplex, int from) throws IloException {
    for(int to=0; to<selectedPaths.length; to++)
      if(to != from && (int) cplex.getValue(selectedPaths[from][to])>0)
          return to;
    return -1;
}
```

## III. Create the search engine that solve the problem (with respect to the constraints and objective function)

13. Add a singleton class for engine that set-up an instance of cplex engine
```java
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

public class Engine {
    public static Engine singleton;
    public IloCplex cplex;

    // 1. Cplex configuration
    public static final int MAX_TIME = 60 * 3; // 1.1 Max computing time = 3 minutes
    public static final int MAX_MEMORY = 5000; // 1.2 Max memory used = 500 Mo
    public static final double INTEGER_TOLERANCE = 0.0; // 1.3 No real approximation

    // 2. Private constructor (reduce memory usage and computing time)
    private Engine() throws IloException {
        this.cplex = new IloCplex();
        cplex.setParam(IloCplex.Param.MIP.Tolerances.Integrality,INTEGER_TOLERANCE);
        cplex.setParam(IloCplex.Param.TimeLimit,MAX_TIME);
        cplex.setParam(IloCplex.Param.MIP.Limits.TreeMemory,MAX_MEMORY);
        cplex.setParam(IloCplex.Param.WorkMem,MAX_MEMORY);
    }

    // 3. Get or create the single instance
    public static Engine get() throws IloException {
        if(singleton == null) singleton = new Engine();
        return singleton;
    }
}
```

14. Add (to the engine) the global structure of the solving algorithm
```java
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
```

15. Add (to the engine) a method that build the objective function: Minimize the total distance of all selected paths
```java
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
```

16. Add (to the engine) a method that build the two (2) main constraints
```java
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
}
```

16. Understand the concept of subsets

![calcul-of-subsets](/documentation/calcul.png)

17. Implementation of the additional constraint for the Dantzig–Fulkerson–Johnson formulation (avoid sub-tours):
```java
public void buildConstraints(Instance i) throws IloException {
    [...] // Constraints C1 and C2 as detailled above

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

int[][] buildPosition(int[][] subsets, int startSubsets, int startingCity, int citiesToTest, int level, int positionToFill){
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

int nbrSubsets(int level, int currentNbrCities, int city){
    if(level <= 0) return 1;
    int totalSubsets = 0;
    for(int nextCity = city+1; nextCity<currentNbrCities; nextCity++)
        totalSubsets += nbrSubsets(level-1, currentNbrCities, nextCity);
    return totalSubsets;
}

public int nbrSubtours(int nbrCities){
    int total = 0;
    for(int subSize=2; subSize<nbrCities; subSize++)
        total += nbrSubtours(nbrCities, subSize);
    return total;
}

public int nbrSubtours(int nbrCities, int subSize){
    return factorial(nbrCities)/(factorial(subSize) * factorial(nbrCities-subSize));
}

public int[][] initSubset(int s1, int s2){
    int[][] subsets = new int[s1][s2];
    for(int s=0; s<s1; s++)
        subsets[s] = new int[s2];
    return subsets;
}

int factorial(int n) {
    if (n<=0) return 1;
    return n * factorial(n-1);
}
```

##
As on can see the algorithm to build all the subsets of size 2 to (n-1) cities is a little bit complex: 
* First, compute the number of subsets by size (in number of cities) using the previously seen formula
* Create empty subsets and organize them by size as groups
* For each group of subsets:
    1. For each position, starting by position 0:
        1. Get the first/next city to appear
        2. Compute the correct number of appearance of this city (if its the last position, this number is 1)
        3. Recursive call to (i.) for the next position (but only for the subsets that start by the current city)
        4. Go back to (position-1) and restart for the next city

## IV. Try your code and display the results

18. Create a class Main with a main method and call the engine to generate a random instance and solve it
```java
import ilog.concert.IloException;

public class Main {
    public static final int NUMBER_CITIES = 6; // 1. Six cities
    public static final int MAX_DISTANCE = 15; // 2. Maximum 15 miles
    public static final int MIN_DISTANCE = 1; // 3. Minimum 1 miles

    public static void main(String[] args) {
        try {
            Engine.get().solve(Instance.randomInit(Engine.get().cplex, NUMBER_CITIES, MIN_DISTANCE, MAX_DISTANCE));
        } catch (IloException e) {
            e.printStackTrace();
        }
    }
}
```

19. Example of result for a problem of 4 cities:
```bash
===========*=*=*=============
      PROBLEM STUDIED
===========*=*=*=============
[0][6][15][10]
[6][0][8][5]
[15][8][0][9]
[10][5][9][0]
Starting city = 0

===========*=*=*=============
      SUBSETS CREATION
===========*=*=*=============
All subsets to test: 
[[0, 1], [0, 2], [0, 3], [1, 2], [1, 3], [2, 3], [0, 1, 2], [0, 1, 3], [0, 2, 3], [1, 2, 3]]

===========*=*=*=============
      CPLEX RUNNING
===========*=*=*=============
Version identifier: 20.1.0.0 | 2020-11-10 | 9bedb6d68
CPXPARAM_TimeLimit                               180
CPXPARAM_WorkMem                                 5000
CPXPARAM_MIP_Tolerances_Integrality              0
CPXPARAM_MIP_Limits_TreeMemory                   5000
Tried aggregator 1 time.
Reduced MIP has 18 rows, 12 columns, and 60 nonzeros.
Reduced MIP has 12 binaries, 0 generals, 0 SOSs, and 0 indicators.
Presolve time = 0.00 sec. (0.03 ticks)
Found incumbent of value 33.000000 after 0.00 sec. (0.09 ticks)
Probing time = 0.00 sec. (0.01 ticks)
Tried aggregator 1 time.
Detecting symmetries...
Reduced MIP has 18 rows, 12 columns, and 60 nonzeros.
Reduced MIP has 12 binaries, 0 generals, 0 SOSs, and 0 indicators.
Presolve time = 0.00 sec. (0.04 ticks)
Probing time = 0.00 sec. (0.01 ticks)
Clique table members: 14.
MIP emphasis: balance optimality and feasibility.
MIP search method: dynamic search.
Parallel mode: deterministic, using up to 12 threads.
Root relaxation solution time = 0.00 sec. (0.02 ticks)

        Nodes                                         Cuts/
   Node  Left     Objective  IInf  Best Integer    Best Bound    ItCnt     Gap

*     0+    0                           33.0000        0.0000           100.00%
      0     0        cutoff             33.0000                      6    0.00%

Root node processing (before b&c):
  Real time             =    0.00 sec. (0.20 ticks)
Parallel b&c, 12 threads:
  Real time             =    0.00 sec. (0.00 ticks)
  Sync time (average)   =    0.00 sec.
  Wait time (average)   =    0.00 sec.
                          ------------
Total (root+branch&cut) =    0.00 sec. (0.20 ticks)

===========*=*=*=============
      RESULTS
===========*=*=*=============
SUCCESS! Solution status = Optimal
Total distance: 33.0
Path: City_0 -> (10) City_3 -> (9) City_2 -> (8) City_1 -> (6) City_0
End of computation after: 0.014 seconds
```

20. Example of generated LP file (for 3 cities):
```lp
Minimize
 obj1: 14 PATH_{0,1} + 14 PATH_{1,0} + 13 PATH_{0,2} + 13 PATH_{2,0}
       + 9 PATH_{1,2} + 9 PATH_{2,1}

Subject To
 C1_0#0:      PATH_{0,1} + PATH_{0,2}  = 1
 C2_0#1:      PATH_{1,0} + PATH_{2,0}  = 1
 C1_1#2:      PATH_{1,0} + PATH_{1,2}  = 1
 C2_1#3:      PATH_{0,1} + PATH_{2,1}  = 1
 C1_2#4:      PATH_{2,0} + PATH_{2,1}  = 1
 C2_2#5:      PATH_{0,2} + PATH_{1,2}  = 1
 C3_(0,_1)#6: PATH_{0,1} + PATH_{1,0} <= 1
 C3_(0,_2)#7: PATH_{0,2} + PATH_{2,0} <= 1
 C3_(1,_2)#8: PATH_{1,2} + PATH_{2,1} <= 1

Bounds
 0 <= PATH_{0,1} <= 1
 0 <= PATH_{1,0} <= 1
 0 <= PATH_{0,2} <= 1
 0 <= PATH_{2,0} <= 1
 0 <= PATH_{1,2} <= 1
 0 <= PATH_{2,1} <= 1

Binaries
 PATH_{0,1}  PATH_{1,0}  PATH_{0,2}  PATH_{2,0}  PATH_{1,2}  PATH_{2,1} 
End
```

_*I hope you enjoyed this!*_
