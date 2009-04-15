package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;

/**
 * Marker interface to signal that the similarity function is normalized to
 * produce values in the range of [0:1].
 * 
 * @author Erich Schubert
 * @param <O> object type
 * @param <D> distance type
 *
 */
public interface NormalizedSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends SimilarityFunction<O,D> {
  // Empty - marker interface.
}
