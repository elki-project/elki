package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Marker interface for similarity functions working on primitive objects, and
 * limited to the 0-1 value range.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface NormalizedPrimitiveSimilarityFunction<O, D extends Distance<D>> extends PrimitiveSimilarityFunction<O, D>, NormalizedSimilarityFunction<O, D> {
  // empty marker interface
}
