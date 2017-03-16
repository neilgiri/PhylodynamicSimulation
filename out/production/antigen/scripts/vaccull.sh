#!/bin/bash

d=.00
dd=.00

for ((i = 0; i < 6; i++));
do
    for ((j = 0; j < 6; j++));
    do 
	javac *.java
	java -Xmx22G Antigen cull $d vaccinate $dd
	cd simulation_output
	mv out.trees out$d-$dd.trees
	cd ..
	dd=$(echo $dd + .02 | bc -l)
    done
    dd=.00
    d=$(echo $d + .02 | bc -l)
done
