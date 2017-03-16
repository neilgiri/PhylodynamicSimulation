#!/bin/bash

#PBS -N antigen
#PBS -l walltime=06:00:00
#PBS -l mem=8GB
#PBS -V
#PBS -m abe

# Import the shared variables and file names between qsub_job.sh and
# calib.sh (to keep names consistent across scripts)
source ${ANTIGEN_PATH}/scripts/env.sh

# This script must be submitted via PBS job with the following command-line:
#
# $ qsub -l nodes=5:ppn=2 -l mem=20GB -l walltime="06:00:00" -v 'RUN_SCRIPT=calib.sh, MERGE=calib_avg_results.dat, PARAMS=egypt egypt_nj_tree.ph contact 0.1 0.2 0.5 parameters.yml' qsub_job.sh

# Change to job directory
cd "$PBS_O_WORKDIR"

# Convenience function to check if necessary environment variables are defined
function checkEnv {
	# The valid flag is changed to 0 if any of the checks fail below.
	valid=1
	# Check antigen path setup
	checkAntigenPath
	if [ $? -ne 0 ]; then
		# Antigen path is not correctly setup.
		valid=0
	fi
	if [ -z $RUN_SCRIPT ]; then
		echo "Script (in ${ANTIGEN_PATH}/scripts) to be run not specified."
		valid=0
	fi
	# If any of the checks above fail erport an error
	if [ $valid -ne 1 ]; then
		echo "Necessary environment variables not found. Exiting!"
		return 2
	fi
	# Everything looks good so far
	return 0
}

# Function to merge outputs from sub-directories into a single file
# The name of the output file (in different directories) to be merged
# is the only parameter.
#
function mergeOutFile() {
	local outFileName="${PBS_O_WORKDIR}/$1"
	local statsFileName="$1"
	# Iterate over the output directories and merge the files.
	local maxNodes=${PBS_NP}
	maxNodes=$(( maxNodes - 1 ))
	for node in `seq 0 ${maxNodes}`
	do
		# Setup output directory using helper function in env.sh
		setOutputDir $node
		# Append file from output directory to working directory
		cat "${outDir}/${statsFileName}" >> "$outFileName"
	done
}

# Function to merge outputs from sub-directories into a single file
# The name of the output files to merged is specified as a list of
# values.
#
function merge() {
	for outFile in $*
	do
		echo "Merging output file $outFile"
		mergeOutFile "$outFile"
	done
}

# The main function that performs necessary operations.
function main {
	# Check status of environment variables
	checkEnv
	if [ $? -ne 0 ]; then
		# Necessary environment variables not defined. Bail out.
		exit
	fi
	# Launch the script with necessary parameters.
	script="${ANTIGEN_PATH}/scripts/${RUN_SCRIPT}"
	mpiexec "$script" ${PARAMS}
	# if merge of output is desiered do it now.
	if [ ! -z "$MERGE" ]; then
		merge $MERGE
	fi
}

# Let the main function perform all the necessary task.
main $*

# End of script

