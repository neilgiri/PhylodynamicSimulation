#!/bin/bash

for ((i = 0; i < 50; i++));
do
    javac *.java
    java -Xmx1G Antigen
    
    cd simulation_output
    cat out.sir >> sir_pop.dat
    cd ..

done
