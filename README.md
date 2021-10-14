# Constraint Acquisition plateform

## DOWNLOAD EXECUTALBLE JAR FILE 

- [acq-1.0.0.jar](https://gite.lirmm.fr/lazaar/ConstraintAcquisition)

## INSTALLATION

The only thing you need to run the jar file is a Java Runtime Environment.
It can be found [here](https://www.java.com/en/download/).    
[See more details about jar files usages](https://docs.oracle.com/javase/tutorial/deployment/jar/basicsindex.html)  
If you want to use the source code, the pom.xml file of this project contains all the necessary libraries to run the project.

## USAGE

To get available options:
```shell
java -jar acq-*.jar -h
```

## DEPENDENCIES

To use and compile the source code, make sure you have binded the following librairies to your project:
- [javax activation](https://mvnrepository.com/artifact/javax.activation/activation/1.1)
- [choco solver 4.10.0](https://github.com/chocoteam/choco-solver/releases/tag/4.10.0)
- [javamail api](https://javaee.github.io/javamail/)


For more uses (unit tests and graphic mode), add the extra librairies :
- [javaFX](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) (contained in the jdk8)
- [JUnit](https://mvnrepository.com/artifact/junit/junit/4.12)
