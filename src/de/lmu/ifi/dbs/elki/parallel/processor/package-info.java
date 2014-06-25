/**
 * Processor API of ELKI, and some essential shared processors.
 * 
 * A processor in ELKI is a function, that can be applied in parallel to different objects
 * in the database. It follows the factory design pattern, as it needs to be instantiated
 * for every thread separately.
 * 
 * While this bears some similarity to mappers as used in Map Reduce,
 * this is not an implementation of a map-reduce framework. This is why there
 * is no "reducer" in the ELKI framework.
 * 
 * A key difference is that mappers may be combined into the same thread, and exchange values
 * via the {@link de.lmu.ifi.dbs.elki.parallel.variables.SharedVariable} API.
 * 
 * The other key difference is that ELKI is not (yet?) running in a distributed framework,
 * therefore it is perfectly possible to have a mapper query the database, or write to
 * an output storage. It may be necessary to apply locking in such cases!
 * 
 * As we want to write to some storage at the end, the last processor usually will not have
 * an "output", thus the name "map" is not a good match anymore.
 * 
 * @apiviz.exclude .*\.Instance$
 */
package de.lmu.ifi.dbs.elki.parallel.processor;