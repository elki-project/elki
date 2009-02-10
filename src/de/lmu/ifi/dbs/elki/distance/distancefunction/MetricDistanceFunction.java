package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;

/**
 * Interface to indicate that a distance function is metrical,
 * i.e. satisfies the triangle equality.
 * 
 * Algorithms such as the M-Tree index structure should use this interface
 * to restrict themselves to appropriate distance functions.
 * 
 * @author Erich Schubert
 *
 * @param <O> database object class
 * @param <D> distance result class
 */
public interface MetricDistanceFunction<O extends DatabaseObject, D extends Distance<D>> extends DistanceFunction<O, D> {
  // TODO indicator interface only
}
