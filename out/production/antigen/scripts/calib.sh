#!/bin/bash

# Global variables for min, max, and step values specific to this
# script.  These values are changed in main() and setupRange() function.
paramFile=""
param="?"
min=0
max=0
step=0
country=""
refPhyTree=""
reps=20

# Import the shared variables and file names between qsub_job.sh and
# calib.sh (to keep names consistent across scripts)
source ${ANTIGEN_PATH}/scripts/env.sh
source ${ANTIGEN_PATH}/scripts/param_ranges.sh

# Setup the range of values for this script to iterate on. This
# function gets all the parameters to explore (specified as
# command-line parameters) passed to this function.  This function
# uses helper functions defined in the param_ranges.sh script to setup
# parameters and adjust their ranges based on parallel job
# configuration.
function setupRange() {
    # Setup output directory based on PBS job id and MPI rank using
    # helper function in env.sh
    setOutputDir "0"
    # Have helper script process parameters
    fillParamArrayVals $*
    if [ $? -ne 0 ]; then
        # Parameters specified are incorrect.
        return 1
    fi
    # Adjust range of first parameter based on parallel job config.
    changeParamRange 1
    if [ $? -ne 0 ]; then
        # Job configuration incompatible
        return 2
    fi
    # Everything went well
    return 0
}

# Run the simulation using the specified parameters and values.  The
# parameters and values are passed on as parameters to the Java
# simulation.
function runSim() {
    local dir="${ANTIGEN_PATH}"
    local javaOut="${outDir}/java_output.txt"
    local cp="${dir}/classmexer.jar:${dir}/colt-1.2.0.jar:${dir}/snakeyaml-1.8.jar:${dir}:."
    # Run the java code
    echo -n "At time: "
    date
    echo "Running simulation with: $*"
    java -Xmx4G -Xss5M -cp "$cp" Antigen outputDir "$outDir" $* >> "$javaOut"
    local exitCode=$?
    if [ $exitCode -ne 0 ]; then
	echo "Java simulation did not complete successfully."
	return $exitCode
    fi
    # Everything went well
    return 0	
}

# Convenience method to run scripts to compare phylogenetic trees.
# This function assumes parameters are supplied in the following order:
#    $1 - Country name
#    $2 - File with reference phylogenetic tree
#    $3, $4 - Input parameters configuration YML file.
#
#    Remaining parameters are just passed onto the simulator.
#
function simAndCompare() {
    # Set-up variables to make function readable.
    local script="${ANTIGEN_PATH}/scripts/ComparePhyTrees.py"
    local simTree="${outDir}/out.trees"
    local resultsFile="${outDir}/${rawResultsFile}"
    local country="$1"
    local refTree="$2"
    local paramFile="$3"
    # Consume the country and reference tree parameters.
    shift 3
    # Now run the simulation with remaining parameters
    runSim "paramFile" "$paramFile" $*
    local exitCode=$?
    if [ $exitCode -ne 0 ]; then
	return $exitCode
    fi
    # With very small contacts trees may not get created
    if [ ! -f "$simTree" ]; then
        # Create a dummy tree file
        echo ";" > "$simTree"
    fi
    # Now run the comparison scripts and dump output
    echo "Generating comparison stats for simulated tree with $refTree"
    echo -n "$country $* " >> "$resultsFile"
    python ${script} "$country" "$refTree" "$simTree" >> "$resultsFile"
    # Everything went well?
    return $?
}

# Convenience function to compute average from a given number of lines
# The search key for computing the average is passed-in as the
# parameter to this function
#    $1 - Columns to skip
#    $2 - Column to average (after skipping $1 cols)
#    $* - Search key
function getAverage() {
    local rawDataFile="${outDir}/${rawResultsFile}"
    local skipCols="$1"
    local col="$2"
    shift 2
    local key="$*"
    col=$(( skipCols + col ))
    local count=`grep -c "$srchKey" "$rawDataFile"`
    local vals=`grep "$key" "$rawDataFile" | cut -d" " -f $col | tr "\n" "+"`
    local avg=`echo "(${vals} 0) / $reps" | bc -l`
    echo $avg
    return 0
}

#
# Convenience method to run experiments for a given parameter setting
# $reps times and generate average difference/error values.  This
# function is called by the processRange function in the
# param_ranges.sh script. 
#
# This function assumes parameters are supplied in the following order:
#    $1 - Country name
#    $2 - File with reference phylogenetic tree
#    $3 - Input parameters configuration YML file.
#
#    Other parameters are settings for different parameters for simulation
#
function simAndCompareMany() {
    local rawDataFile="${outDir}/${rawResultsFile}"
    local resultsFile="${outDir}/${avgResultsFile}"
    local country="$1"
    local refTree="$2"
    local paramFile="$3"
    shift 3
    # Run $reps repetitions of the simulation
    for rep in `seq 1 $reps`
    do
	# Run the simulation and compare results
	simAndCompare "$country" "$refTree" "$paramFile" $*
	local exitCode=$?
	if [ $exitCode -ne 0 ]; then
	    return $exitCode
	fi
    done
    echo -n "----- Generating average diff-stats for '$*' at "
    date
    # Check to ensure we have $reps lines of output to process
    local key="$country $*"
    local lineCount=`grep -c "$key" "$rawDataFile"`
    if [ $lineCount -ne $reps ]; then
	echo "Expected $reps lines for '$country $*' but got $lineCount"
	return 3
    fi
    # Now compute average errors from $reps for various statistics
    local numParams=$#
    local skipCols=$(( numParams + 2 ))
    local avgRefClusts=`getAverage $skipCols 2 "$key"`
    local avgSimClusts=`getAverage $skipCols 4 "$key"`
    local avgInterClusts=`getAverage $skipCols 6 "$key"`
    local avgIntraClusts=`getAverage $skipCols 8 "$key"`
    local avgChilds=`getAverage $skipCols 10 "$key"`
    local avgDist=`getAverage $skipCols 12 "$key"`
    local avgNodeDepth=`getAverage $skipCols 14 "$key"`
    # Finally print summary statistics into the results file.
    echo  "$key avg_ref_clusters $avgRefClusts \
avg_sim_clusters $avgSimClusts avg_inter_clust_dist_diff $avgInterClusts \
avg_intra_clust_dist_diff $avgIntraClusts avg_node_size_diff $avgChilds \
avg_distance_diff $avgDist avg_depth_diff $avgNodeDepth " >> "$resultsFile"
    # Everything went well?
    return $?
}

# Convenience function to check if necessary environment variables are defined
function checkEnvParams {
    # The valid flag is changed to 0 if any of the checks fail below.
    valid=1
    # Check for values that are expected		
    if [ -z $ANTIGEN_PATH ]; then
	echo "Ensure ANTIGEN_PATH enviornment variable is set."
	echo "ANTIGEN_PATH must be set to the top-level directory as in:"
	echo "/home/${USER}/research/phyloDynH5N1/antigen"
	valid=0
    fi
    # If any of the checks above fail erport an error
    if [ $valid -ne 1 ]; then
	echo "Necessary environment variables not found. Exiting!"
	return 2
    fi
    # Check to ensure that necessary parameters have been supplied.
    if [ $# -lt 7 ]; then
	echo "Specify 7 command-line arguments in the following order"
        echo "Order : <Cntry> <RefPhyTree> <ParamFile> <Parameter> <MinVal> <MaxVal> <Step> ..."
	echo "Example: vietnam vietnam.py parameters.yml contact 0.1 3.5 0.1 stepSize 0.01 0.009 0.001"
	return 1
    fi	
    # Everything looks good so far
    return 0
}

# The main function that performs necessary operations.
function main() {
    # Check to ensure environment variables are defined.
    checkEnvParams $*
    if [ $? -ne 0 ]; then
	exit
    fi
    # Switch to working directory if running as PBS job
    if [ ! -z "$PBS_O_WORKDIR" ]; then
	cd "$PBS_O_WORKDIR"
	echo "Changed working directory to:"
	pwd
    fi
    # Save command-line arguments for future use
    country="$1"
    refPhyTree="$2"
    paramFile="$3"
    shift 3
    # Store parameters into arrays and adjust range based on parallel
    # job configuration.
    setupRange $*
    if [ $? -ne 0 ]; then
        # Error processing parameter range.
        return 1
    fi
    # Use helper function in params_ranges.sh to iterate over the
    # specified ranges of parameters.
    processRange simAndCompareMany "$country" "$refPhyTree" "$paramFile"
    # Use return value as exit code
    return $?
}


# Let the main function perform all the necessary task.
main $*

# End of script
