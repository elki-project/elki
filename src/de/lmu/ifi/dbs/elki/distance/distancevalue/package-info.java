/**
 * <p>Distance values, i.e. object storing an actual <em>distance</em> value along with
 * comparison functions and value parsers.</p>
 * 
 * <p>Distances follow a factory pattern. Usually, a class will have a static instance
 * called <code>FACTORY</code> that can be used to obtain e.g. infinity or zero distances
 * as well as parse a string value into a new distance value.</p> 
 */
package de.lmu.ifi.dbs.elki.distance.distancevalue;