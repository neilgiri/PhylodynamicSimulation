#!/bin/bash

d=.00

for ((i = 0; i < 7; i++));
do
    javac *.java
    java -Xmx22G Antigen cull $d vaccinate 0.0
    cd simulation_output
    mv out.trees out$d-1.trees
    cd ..

    javac *.java
    java -Xmx22G Antigen cull $d vaccinate 0.1
    cd simulation_output
    mv out.trees out$d-2.trees
    cd ..

    for ((j = 3; j <= 5 ; j++));
    do 
	javac *.java
	java -Xmx22G Antigen cull $d vaccinate 0.0
	cd simulation_output
	mv out.trees out$d-$j.trees
	cd ..
    done
    d=$(echo $d + .05 | bc -l)
done
