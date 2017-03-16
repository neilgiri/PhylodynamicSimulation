#!/bin/bash

# A script to run the simulation many times in parallel.

# Global variables for min, max, and step values specific to this
# script.  These values are changed in main() and setup() function.
outDir="outDir"
paramFile="parameters.yml"

# Import the shared variables and file names between qsub_job.sh and
# calib.sh (to keep names consistent across scripts)
source ${ANTIGEN_PATH}/scripts/env.sh
source ${ANTIGEN_PATH}/scripts/param_ranges.sh

# Run the simulation for a given iteration.
# The iteration value is passed-in as the parameter.
#  $1 -- Iteration suffix.
function runSim() {
    local dir="${ANTIGEN_PATH}"
    local javaOut="${outDir}/java_output_$1.txt"
    local cp="${dir}/classmexer.jar:${dir}/colt-1.2.0.jar:${dir}/snakeyaml-1.8.jar:${dir}:."
    # Remove iteration suffix from parameter list
    shift
    # Run the java code
    echo -n "At time: "
    date
    echo "Running simulation with: $*"
    java -Xmx4G -Xss5M -cp "$cp" Antigen "paramFile" "$paramFile" outputDir "$outDir" $* >> "$javaOut"
    local exitCode=$?
    if [ $exitCode -ne 0 ]; then
	echo "Java simulation did not complete successfully."
	return $exitCode
    fi
    # Everything went well
    return 0	
}

#
# Convenience method to run experiments 'n' number of times.
# This function assumes parameters are supplied in the following order:
#    $1 - Input parameters configuration YML file.
#    $2 - Number of replication of the simulation to run.

#    Other parameters are settings for different parameters for simulation
#
function simMany() {
    local paramFile="$1"
    local reps="$2"
    shift 2
    echo "Process ${OMPI_COMM_WORLD_RANK} is running $reps repetitions..."
    # Run 10 repetitions of the simulation
    for rep in `seq 1 $reps`
    do
	# Run the simulation and compare results
	runSim $rep $*
	local exitCode=$?
	if [ $exitCode -ne 0 ]; then
	    return $exitCode
	fi
    done
    # Everything went well
    return 0
}

# Convenience function to check if necessary environment variables are defined
function checkEnvParams {
    # Check for values that are expected
    checkAntigenPath
    # If any of the checks above fail erport an error
    if [ $? -ne 0 ]; then
	echo "Necessary environment variables not found. Exiting!"
	return 2
    fi
    # Check to ensure that necessary parameters have been supplied.
    if [ $# -lt 2 ]; then
	echo "Specify at least 2 command-line arguments in the following order"
        echo "Order : <Reps> <ParamFile> [additional parameters]"
	echo "Example: 30 parameters.yml contact 0.1 stepSize 0.01"
	return 1
    fi	
    # Everything looks good so far
    return 0
}

# Convenience function to setup global ranges.  This function is
# called just once from main().  The number of reps is passed-in as
# the only parmaeter.
#
#    $1: The total number of reps specified by the user.
function setup() {
    local totReps=$1
    # Switch to working directory if running as PBS job
    if [ ! -z "$PBS_O_WORKDIR" ]; then
	cd "$PBS_O_WORKDIR"
	echo "Changed working directory to:"
	pwd
    fi    
    # Setup output directory based on PBS job id and MPI rank using
    # helper function in env.sh
    setOutputDir "0"
    # Compute the number of reps this process must do.
    local node=0
    local maxNodes=1
    # Override the node and maxNodes based on MPI parameters
    if [ ! -z "$OMPI_COMM_WORLD_SIZE" ]; then
	maxNodes=${OMPI_COMM_WORLD_SIZE}
	node=${OMPI_COMM_WORLD_RANK}
    fi
    # Now compute the number of reps this process should do.
    reps=$(( totReps / maxNodes ))
    # Fix the reps for the first node to ensure it does not exceed totReps    
    if [ $maxNodes -gt 1 ]; then
        local spill=$(( totReps % maxNodes ))
        if [ $spill -gt $node ]; then
            reps=$(( reps + 1 ))
        fi
    fi
    # Everything went well
    return 0
}

# The main function that performs necessary operations.
function main() {
    # Check to ensure environment variables are defined.
    checkEnvParams $*
    if [ $? -ne 0 ]; then
	exit
    fi
    # Save command-line arguments for future use
    local totReps=$1
    paramFile="$2"
    shift 2
    # Setup global variables for this process based on PBS info.
    setup $totReps
    # Run the number of repitions for this process
    simMany "$paramFile" $reps $*
    # Use return value as exit code
    return $?
}


# Let the main function perform all the necessary task.
main $*

# End of script
