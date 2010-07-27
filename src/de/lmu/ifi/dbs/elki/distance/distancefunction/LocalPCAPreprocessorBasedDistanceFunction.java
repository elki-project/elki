package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.preprocessing.LocalProjectionPreprocessor;

/**
 * Interface for local PCA based preprocessors.
 * 
 * @author Erich Schubert
 *
 * @param <O> Database object type
 * @param <D> Distance type
 */
public interface LocalPCAPreprocessorBasedDistanceFunction<O extends NumberVector<?, ?>, P extends LocalProjectionPreprocessor<? super O, ?>, D extends Distance<D>> extends PreprocessorBasedDistanceFunction<O, D> {
  // empty marker interface.
}
