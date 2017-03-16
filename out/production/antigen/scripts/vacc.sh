#!/bin/bash

d=.00

for ((i = 0; i < 7; i++));
do
    for ((j = 0; j < 10; j++));
    do 
	javac *.java
	java -Xmx22G Antigen vaccinate $d
	cd simulation_output
	mv out.trees out$d-$j.trees
	cd ..
    done
    d=$(echo $d + .05 | bc -l)
done
