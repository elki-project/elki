#!/bin/sh
bd=$( dirname $( dirname $0 ) )
java=$( test -z "$JAVA" && echo java || echo $JAVA  )
core=$( ls -d $bd/elki/target/classes $bd/elki-core*/target/classes )
mods=$( ls -d $bd/elki*/target/classes | egrep -v "/elki-core|/elki/|elki-docutil" )
addons=$( ls -d $bd/addons/*/target/classes | egrep -v "addons/bundle" )
# This is really ugly... collect .jars on the class path, but avoid duplicates.
deps=$( ls -d $bd/*/target/dependency/*.jar $bd/addons/*/target/dependency/*.jar | egrep -v "elki-docutil" \
| sed -e 's,.*target/dependency/,,' | egrep -v "^elki|^hamcrest|^junit" | sort -u \
| while read dep; do ls $bd/*/target/dependency/$dep $bd/addons/*/target/dependency/$dep 2>/dev/null | head -1; done )
if [ -z "$core" ]; then
  echo "ELKI does not appear to be compiled yet. Call 'mvn package' first."  >&2
  exit 1
fi
cp=$( echo "$core:$mods:$addons:$deps" | paste -s -d: )
exec $java $JVM_OPTS -cp "$cp" de.lmu.ifi.dbs.elki.application.ELKILauncher "$@"
