#!/bin/bash

# A simple build script to run the Antigen simulator with necesssary
# classpath to various Jar files.

# The script assumes that the Java compiler (javac) from JDK is
# accessible via the default path.

javac -cp classmexer.jar:colt-1.2.0.jar:snakeyaml-1.8.jar:. *.java

# End of script
