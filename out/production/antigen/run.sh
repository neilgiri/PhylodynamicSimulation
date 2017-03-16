#!/bin/bash

# A simple script to run the Antigen simulator with all the supplied
# command-line arguments (if any)

# The script assumes that the Java JRE (java) from JDK is accessible
# via the default path.

java -Xmx4G -Xss2M -cp classmexer.jar:colt-1.2.0.jar:snakeyaml-1.8.jar:. Antigen $*

# End of script
