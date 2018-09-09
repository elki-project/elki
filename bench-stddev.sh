#!/bin/sh
$ELKI cli -time \
-dbc.in coords-wikidata.csv.gz \
-parser.quote \% \
-dbc.filter z,z,z,z,z,z,z,z,z,z \
-algorithm NullAlgorithm \
-resulthandler DiscardResultHandler
