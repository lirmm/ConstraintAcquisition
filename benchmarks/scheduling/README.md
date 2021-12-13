# Instance Generator for Temporal Networks

Using Allen's Interval Algebra, potentially extendable to other algebras.

## Requirements

The scripts require that the packages from `requirements.txt` are installed.
Also the package `qualreas` from https://github.com/alreich/qualreas needs to be available; unfortunately it cannot be installed via `pip`, but needs to be cloned some, e.g. the same directory as the scripts.

## Script Overview

There are a couple of scripts that have many redundancies but with different intentions.

For all generators can the execution take a while and might, depending on the parameters, not finish at all or in acceptable time.
That is also the reason why there are so many different implementations for different purposes.
*It is recommended to check the source code for what is actually happening.*

Instances are written in the temacq instance format in `rcpsp/`.

`instance_generator.py`:
Generates a random instance with a fixed number of unrestricted edges and a minimum/maximum density.
Parameters: `number of tasks`, `minimum density`, `maximum density`, `number of unrestricted edges`

If you want to limit which relations are selected, you can tweak the parameter `all_relations` in the script. Useful if a feasible instance with only 1-2 relations should be created.

`instance_generator_v1.py`:
Generates a random instance with a fixed number of unrestricted edges.
Has special code paths for sparse instances with only 1 or 2 relations per edge.

Parameters: `number of tasks`, `perc. of edges for a relation is set initially`, `max. relations per pair`, `number of unrestricted edges`

`instance_generator_unchecked.py`:
Generates a random instance without checking whether it is feasible.
Parameters: `number of tasks`, `max. relations per pair`, `percentage of edges with all relations`

`instance_statistics.py`:
It takes a directory as an argument and prints some statistics for every instance in it.
Reads the temacq instance format.

`instance_validation.py`:
It takes a directory as an argument and checks for every instance in it, if it is feasible and how many solution candidates (possible combinations of the different relations) there are.
Reads the temacq instance format.
