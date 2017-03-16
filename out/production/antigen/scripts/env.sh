#!/bin/bash

# This script file provides some common variables and functions that
# are shared/used by 2-or-more scripts in this folder.

# Some reasonably fixed and shared file names
rawResultsFile="calib_raw_results.dat"
avgResultsFile="calib_avg_results.dat"
outDir=""

# Convenience function to check if ANTIGEN_PATH environment variable
# is setup for use by the scripts.
function checkAntigenPath() {
	if [ -z $ANTIGEN_PATH ]; then
		echo "Ensure ANTIGEN_PATH enviornment variable is set."
		echo "ANTIGEN_PATH must be set to the top-level directory as in:"
		echo "/home/${USER}/research/phyloDynH5N1/antigen"
		return 1
	fi
	# Check if the path looks correct
	envShPath="${ANTIGEN_PATH}/scripts/env.sh"
	if [ ! -f "$envShPath" ]; then
		echo "ANTIGEN_PATH is set to ${ANTIGEN_PATH}"
		echo "However, that setting is incorrect because the file"
		echo "scrips/envh.sh was not found in that directory."
		return 2
	fi
	# Things look good.
	return 0
}

# Convenience method to setup output directory based on the PBS job id
# and MPI rank. This function requires 1 argument:
#
#    $1 - The MPI rank to be used as default value.
#
function setOutputDir() {
	local node=$1
	local jobID="0"
	# Override the node and maxNodes based on MPI parameters
	if [ ! -z "$OMPI_COMM_WORLD_SIZE" ]; then
		node=${OMPI_COMM_WORLD_RANK}
	fi
	# Setup output directory based on PBS job id, if available
	if [ ! -z "$PBS_JOBID" ]; then
		jobID=`echo "$PBS_JOBID" | cut -d"." -f1`
	fi
	# Setup output directory path.
	outDir="job_${jobID}_rank_${node}/"
	# Create the directory if one does not exist
	mkdir -p "$outDir"
}

# end of script
