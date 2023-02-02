# TSP
A simple Travelling Salesman Problem to solve (optimize) with IBM Cplex - Learning Purpose

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
    } while(from != 1);
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

16. Additional constraint for the Dantzig–Fulkerson–Johnson formulation (avoid sub-tours):
```java
//TODO
```

17. Understand the concept of subsets
![calcul-of-subsets](/documentation/calcul.png)

## IV. Try your code and display the results

18. Create a class Main with a main method and call the engine to generate a random instance and solve it
```java
import ilog.concert.IloException;

public class Main {
    public static final int NUMBER_CITIES = 6; // 1. Four cities
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

19. Example of result:
