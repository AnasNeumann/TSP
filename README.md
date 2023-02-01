# TSP
A simple Travelling Salesman Problem to solve (optimize) with IBM Cplex - Learning Purpose

## I. Complete process to install and link Ilog Cplex
1. Download and install Cplex and Ilog Cplex

2. Create a maven project to generate a _pom.xml_ file (used to install external tools like Ilog Cplex)

3. Add the following entry to the _pom.xml_ file to install the Ilog Cplex engine: 
```xml
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

5. Run the commande "_Maven -> Reimport_" to link your code and Ilog Cplex

6. Create a new Maven running configuration with the following command line
```shell
# Select the correct local path to your main class
compile exec:java -D exec.mainClass=main.java.Main
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
    for(int v1=0; v1<size; v1++){
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

10. Create a Solution.java class to save the selected paths and the total distance and add an init method
```java
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;

public class Solution {
        public IloIntVar[][] selectedPaths;
        public IloIntVar totalDistance;
    
        public static Solution init(int size, IloCplex cplex) throws IloException {
            Solution s = new Solution();
            s.selectedPaths = new IloIntVar[size][size];
            for(int v1=0; v1<size; v1++)
                for(int v2=0; v2<size; v2++)
                    s.selectedPaths[v1][v2] = cplex.boolVar("PATH_{"+v1+","+v2+"}");
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
    for(int v1=0; v1<size; v1++){
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
public void displayPath(IloCplex cplex, int from) throws IloException {
    int distance = (int) cplex.getValue(totalDistance);
    System.out.println("Total distance: "+distance);
    do{
        System.out.print(from);
        from = nextCity(cplex, from);
        if(from != -1) System.out.print("->");
    } while(from != 1);
}

public int nextCity(IloCplex cplex, int from) throws IloException {
    for(int to=0; to<selectedPaths.length; to++)
      if((int) cplex.getValue(selectedPaths[from][to])>1)
          return to;
    return -1;
}
```


## III. Create the search engine that solve the problem (with respect to the constraints and objective function)



## IV. Try your code and display the results