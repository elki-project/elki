#!/bin/sh
bd=$( dirname $( dirname $0 ) )
java=$( test -z "$JAVA" && echo java || echo $JAVA  )
core=$( ls $bd/elki/target/*.jar $bd/elki-core*/target/*.jar | egrep -v "javadoc.jar|sources.jar" )
mods=$( ls $bd/elki*/target/*.jar | egrep -v "javadoc.jar|sources.jar|/elki-core|/elki/|/elki-docutil" )
addons=$( ls $bd/addons/*/target/*.jar | egrep -v "javadoc.jar|sources.jar|elki-bundle" )
if [ -z "$core" ]; then
  echo "ELKI does not appear to be compiled yet. Call 'mvn package' first."  >&2
  exit 1
fi
cp=$( echo "$core:$mods:$addons" | paste -s -d: )
exec $java $JVM_OPTS -cp "$cp" de.lmu.ifi.dbs.elki.application.ELKILauncher "$@"
