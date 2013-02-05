#!/bin/bash
# Begin simple.sh
##############
#$# creator : Devon Smith
#%# email: smithde@oclc.org
#$# created : 2010-06-18
#$# title : simple.sh
#$# description : Add Simple Core jars to the classpath
########## Define a usage function 
function usage {
    echo "Usage: $this <library directory>"
    if [ "$1" = "exit" ]; then exit; fi
}
########## Set to "false" to turn verbose off
function echo_stdout { echo "$@"; }
function echo_stderr { echo "$@" >& 2; }
# Set verbose to 'echo_stderr' or 'echo_stdout' to see output trace info
verbose=false
$verbose Verbose output on ${verbose##*_}

########## Setup some common paths
# The absolute, canonical ( no ".." ) path to this script
canonical=$(cd -P -- "$(dirname -- "$0")" && printf '%s\n' "$(pwd -P)/$(basename -- "$0")")
# Just the filename of this script
this=$(basename $canonical)
$verbose This: $this
# The directory this script is in
here=$(dirname $canonical)
$verbose Here: $here

########## When the script is being executed
today=$(date +%F)
now=$(date +%H:%M:%S)
$verbose Time: $today $now
##########
if [ "$1" = "-h" ]; then usage 'exit' ; fi
##########

libdir=$1
$verbose Library Dir: $libdir
if [ ! -d $libdir ]; then echo "Arg #1 (\"$libdir\") is not a directory - Simple Core JARs not found'"; exit; fi

simple_libdir=$libdir/simple-core-3.1.3/jar
$verbose Simple Core Library Dir: $simple_libdir
simple_jars=(simple-core-3.1.3.jar)

for jar in ${simple_jars[*]}; do CP=${CP:+$CP:}$simple_libdir/$jar; done
$verbose Classpath: $CP

echo $CP

############
# End simple.sh
# vim:ts=4:indentexpr=
