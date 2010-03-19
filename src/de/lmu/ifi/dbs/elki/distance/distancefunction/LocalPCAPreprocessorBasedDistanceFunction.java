package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.preprocessing.LocalPCAPreprocessor;

/**
 * Interface for local PCA based preprocessors.
 * 
 * @author Erich Schubert
 *
 * @param <O> Database object type
 * @param <P> Preprocessor type
 * @param <D> Distance type
 */
public interface LocalPCAPreprocessorBasedDistanceFunction<O extends NumberVector<O, ?>, P extends LocalPCAPreprocessor<O>, D extends Distance<D>> extends PreprocessorBasedDistanceFunction<O, P, D> {
  // Empty - additional constraints are only in generics!
}
