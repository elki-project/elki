package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.PreprocessorBasedMeasurementFunction;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;

/**
 * Interface to mark preprocessor based distance functions.
 * 
 * @author Erich Schubert
 *
 * @param <O> Database object type
 * @param <P> Preprocessor type
 * @param <D> Distance function
 */
public interface PreprocessorBasedDistanceFunction<O extends DatabaseObject, P extends Preprocessor<O>, D extends Distance<D>> extends PreprocessorBasedMeasurementFunction<O, P, D>, DistanceFunction<O, D> {
  // Empty - see "extends" requirements 
}