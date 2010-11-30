package de.lmu.ifi.dbs.elki.algorithm.outlier;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;

/**
 * Generic super interface for outlier detection algorithms.
 * 
 * Adds a result generics restriction to OutlierResult. 
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses OutlierResult
 *
 * @param <O> object type
 * @param <R> result type
 */
public interface OutlierAlgorithm<O extends DatabaseObject, R extends OutlierResult> extends Algorithm<O, R> {
  // Empty
}
