#!/bin/bash

d=1.5

for ((i = 1; i <= 10; i++));
do
    javac *.java
    java -Xmx22G Antigen dimen $i
    cd simulation_output
    cat out.trees >> vietnam_dimen.dat
    cd ..

done
