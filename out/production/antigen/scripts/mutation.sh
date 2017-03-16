#!/bin/bash

d=0.001

for ((i = 0; i < 4; i++));
do
    for ((j = 0; j < 10; j++));
    do 
	javac *.java
	java -Xmx22G Antigen mutation $d
	cd simulation_output
	mv out.trees out$d-$j.trees
	cd ..
    done
    d=$(echo $d + 0.005 | bc -l)
done
