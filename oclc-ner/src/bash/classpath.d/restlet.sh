#!/bin/bash
# Begin restlet.sh
##############
#$# creator : Devon Smith
#%# email: smithde@oclc.org
#$# created : 2010-06-22
#$# title : restlet.sh
#$# description : Add Restlet jars to the classpath
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
if [ ! -d $libdir ]; then echo "Arg #1 (\"$libdir\") is not a directory - Restlet JARs not found'"; exit; fi

restlet_libdir=$libdir/restlet-1.0.11/lib
$verbose Restlet Library Dir: $restlet_libdir
restlet_jars=(org.restlet.jar com.noelios.restlet.jar com.noelios.restlet.ext.simple_3.1.jar)

for jar in ${restlet_jars[*]}; do CP=${CP:+$CP:}$restlet_libdir/$jar; done
$verbose Classpath: $CP

echo $CP

############
# End restlet.sh
# vim:ts=4:indentexpr=
