Compiling Source Code:
$ ./build.sh


Running Source Code:
$ ./run.sh

The `run.sh` includes setting on maximum heap size.  This value is set
to 4GB and can be increased to 22G if the simulation needs more memory
to run.

Output files are all found in the 'simulation_output' directory
-out.trees = phylogenetic tree in newick format
-out.months = brooding population for each month
-out.timeseries = simulation events

Example scripts for experiments run in the past are found in the 'scripts' directory

'parameters.yml' contains a list of parameters and their descriptions/functions

To run an experiment using vaccination and culling, the code in 'HostPopulation.java'
under the 'stepForward' and 'sample' methods need to be uncommented.

Analysis scripts are found in figures folder. The Python code details what the input
and output files are. 
