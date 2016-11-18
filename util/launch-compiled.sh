#!/bin/sh
bd=$( dirname $( dirname $0 ) )
java=$( test -z "$JAVA" && echo java || echo $JAVA  )
core=$( ls -d $bd/elki/build/*/main $bd/elki-core*/build/*/main )
mods=$( ls -d $bd/elki*/build/*/main | egrep -v "/elki-core|/elki/|elki-docutil" )
addons=$( ls -d $bd/addons/*/build/*/main | egrep -v "addons/bundle" )
# This is really ugly... collect .jars on the class path, but avoid duplicates.
deps=$( ls -d $bd/*/build/libs/lib/*.jar $bd/addons/*/build/libs/lib/*.jar | egrep -v "elki-docutil" \
| sed -e 's,.*build/libs/lib/,,' | egrep -v "^elki|^hamcrest|^junit" | sort -u \
| while read dep; do ls $bd/*/build/libs/lib/$dep $bd/addons/*/build/libs/lib/$dep 2>/dev/null | head -1; done )
if [ -z "$core" ]; then
  echo "ELKI does not appear to be compiled yet. Call './gradlew compile' first."  >&2
  exit 1
fi
cp=$( echo "$core:$mods:$addons:$deps" | paste -s -d: )
exec $java $JVM_OPTS -cp "$cp" de.lmu.ifi.dbs.elki.application.ELKILauncher "$@"
