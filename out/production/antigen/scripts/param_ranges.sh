#!/bin/bash

# Declare 4 arrays with parameter name/identifier, min value, max
# value, and step size. The last value is current value of the
# parameter, which is initialized to min value.
paramIdenti=()
paramMinVal=()
paramMaxVal=()
paramStepSz=()
paramCurVal=()

function fillParamArrayVals() {
    local param=1
    while [ $# -gt 3 ];
    do
        # Save the 4 values associated with current parameter
        paramIdenti[$param]=$1
        paramMinVal[$param]=$2
        paramMaxVal[$param]=$3
        paramStepSz[$param]=$4
        paramCurVal[$param]=$2
        # Ensure min value is less than max value
        local diff=`echo ${paramMaxVal[param]} - ${paramMinVal[param]} | bc -l`
        if [ ${diff:0:1} == "-" ]; then
            echo "For parameter $1, minimum value ($2) is greater than"\
                 "maximum value ($3). This is an error."
            return 1
        fi
        # Shift parameter values to conveniently skip to next
        # parameter's information.
        shift 4
        ((param++))
    done
    return 0
}

function printParamArrayVals() {
    local paramCount=${#paramIdenti[@]}
    for param in `seq 1 $paramCount`
    do
        echo ${paramIdenti[param]} ${paramMinVal[param]} \
             ${paramMaxVal[param]} ${paramStepSz[param]} \
             ${paramCurVal[param]}
    done
}

# Convenience function to get the current values associated with the
# parameters as a command-line argument
function getCurrVals() {
    local paramCount=${#paramIdenti[@]}
    local paramArgs=""
    for param in `seq 1 $paramCount`
    do
        paramArgs="${paramArgs} ${paramIdenti[param]} ${paramCurVal[param]}"
    done
    echo "$paramArgs"
}

# This function changes the current value of the specified parameter
# to the next value based on the step size.  If the next value exceeds
# the maximul value, then this function resets the current value to
# the initial settings.
#
#  $1 - Index of parameter whose value is to be changed.
#
# This function returns 1 if the variable was reset. It returns 0 if
# the variable is still in range.
#
function setNextVal() {
    local idx=$1
    local next=`echo ${paramCurVal[idx]} + ${paramStepSz[idx]} | bc -l`
    local diff=`echo ${paramMaxVal[idx]} - $next | bc -l`
    if [ ${diff:0:1} == "-" ]; then
        # The difference has become negative. The next value is larger
        # than the maximu value for this parameter. Reset to min
        paramCurVal[idx]=${paramMinVal[idx]}
        return 1
    fi
    # Still within range
    paramCurVal[idx]=$next
    return 0
}

# This is a convenience function to iterate over the range of parameters
# and call a specific function with parameter values.
#
# $1 - Name of the function to call
#      Remaining parameters are additional parameters to the function
#
function processRange() {
    local done="false"
    local paramCount=${#paramIdenti[@]}
    # Repeatedly increment parameters, resetting them to initial value
    # when they exceed their range.
    while [ $done != "true" ];
    do
        # Process current parmaeter settings
        local paramVals=`getCurrVals`
        $* $paramVals
        if [ $? -ne 0 ]; then
            # Processing of parameters failed. Bail out.
            echo "Call to '$* $paramVals' failed."
            return 1
        fi
        # Change to next parameter settings
        local param=1
        while [ $param -le $paramCount ];
        do
            # Change the value of param to its next value based on step
            setNextVal $param
            # If parameter range has exceeded we need to change next param
            if [ $? -eq 0 ]; then
                # Parameter still in range. We don't need to move to next
                break
            fi
            ((param++))
        done
        # End all processing if last parameter's max range exceeded.
        if [ $param -gt $paramCount ]; then
            # The whole parameter space has been explored!
            done="true"
        fi
    done
    # Everything went well
    return 0
}

# Setup the range of values for this script to iterate on. This
# function changes the min and max range of the specified parameter
# based on the number of parallel jobs that are being run.
# Information about the parallel configuration is obtained from
# environment variables that are automatically setup by PBS and MPI.
#
#  $1 - Index of parameter who range is to be chaned.
#
function changeParamRange() {
    local node=0
    local maxNodes=1
    idx=$1
    # Override the node and maxNodes based on MPI parameters
    if [ ! -z "$OMPI_COMM_WORLD_SIZE" ]; then
	maxNodes=${OMPI_COMM_WORLD_SIZE}
	node=${OMPI_COMM_WORLD_RANK}
    fi
    # Now compute the range this process should iterate on
    local min=${paramMinVal[idx]}
    local max=${paramMaxVal[idx]}
    local stp=${paramStepSz[idx]}
    local totSteps=`echo "( $max - $min + $stp ) / $stp" | bc -l`
    local nodeSteps=`echo "$totSteps / $maxNodes" | bc -l`
    # The node step value should be an integer to handle ranges
    # correctly.
    nodeSteps=`echo $nodeSteps | cut -d"." -f1`
    if [ -z "$nodeSteps" ]; then
        nodeSteps=0
    fi
    if [ $nodeSteps -lt 1 ]; then
        echo "Range of values too small to use with the parallel " \
             "configuration."
        echo "Fix range for parameter '${paramIdenti[idx]}'"\
             "or change job configuration to enable division of range"\
             "between parallel processes in the job."
        return 1
    fi
    # Update parameter iteration range 
    paramMinVal[idx]=`echo "$min + $node * $nodeSteps * $stp" | bc -l`
    # Handle edge case for last node to ensure range is covered.
    local lastNode=$(( maxNodes - 1 ))
    if [ $node -lt $lastNode ]; then
        paramMaxVal[idx]=`echo "${paramMinVal[idx]} + ( $nodeSteps - 1 ) * $stp" | bc -l`
    fi
    paramCurVal[idx]=${paramMinVal[idx]}
    return 0
}

function process() {
    echo "$*"
    return 0
}

# End of script
