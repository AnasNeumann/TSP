# TSP
A simple Travelling Salesman Problem to solve (optimize) with IBM Cplex - Learning Purpose

## Complete process
1. Create a maven project to generate a _pom.xml_ file (used to install external tools like Cplex)

2. Add the following entry to the _pom.xml_ file to install the Cplex engine:
```xml
 <dependencies>
    <dependency>
        <groupId>cplex</groupId>
        <artifactId>cplex</artifactId>
        <version>20.10</version> # _select the right version!_
    </dependency>
</dependencies>
```

3. Add the Cplex folder (downloaded from the IBM website) inside a /lib folder:
![cplex-folder](/documentation/cplex.png)

4. Create a new running configuration with the following command line
```shell
compile exec:java -D exec.mainClass=gl.Main
```

5. Add the following "Runner/JVM configuration": 
```shell
-Djava.library.path=/Applications/CPLEX_Studio201/cplex/bin/x86-64_osx
```

6. Create an architecture of folder to separate the data model an the engine