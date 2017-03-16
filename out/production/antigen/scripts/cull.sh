#!/bin/bash

d=.00

for ((i = 0; i < 7; i++));
do
    for ((j = 0; j < 5; j++));
    do 
	javac *.java
	java -Xmx22G Antigen cull $d
	cd simulation_output
	#cat out.timeseries >> cull.dat
	cat out5.trees >> out5-$d-$j.trees
	cat out1.trees >> out1-$d-$j.trees
	cat out2.trees >> out2-$d-$j.trees
	cat out3.trees >> out3-$d-$j.trees
	cat out4.trees >> out4-$d-$j.trees
	cd ..
    done
    d=$(echo $d + .05 | bc -l)
done
