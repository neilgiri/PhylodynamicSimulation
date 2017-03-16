#!/bin/bash

d=4

for ((i = 0; i < 20; i++));
do
    javac *.java
    java -Xmx22G Antigen recovery $d
    cd simulation_output
    cat out.timeseries >> recovery.dat
    cd ..
    d=$(echo $d + 1 | bc -l)
done
