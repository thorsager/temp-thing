#!/bin/sh

if [[ -z $JAVA_HOME ]]; then
   echo "JAVA_HOME must be set to run the example applications"
   exit 1
fi

ANT_HOME=${PWD}/../ant
chmod 755 $ANT_HOME/bin/ant

PATH=$ANT_HOME/bin:$JAVA_HOME/bin:$PATH
CLASSPATH=

ant $@
