package de.lmu.ifi.dbs.elki.distance;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorClient;

/**
 * Describes the requirements of any measurement function (e.g. distance
 * function or similarity function) needing a preprocessor running on a
 * database.
 * 
 * @author Elke Achtert
 * @param <D> the type of Distance used as measurement for comparing database
 *        objects
 * @param <O> the type of DatabaseObject for which a measurement is provided for
 *        comparison
 * @param <P> the type of Preprocessor used
 */
public interface PreprocessorBasedMeasurementFunction<O extends DatabaseObject, P extends Preprocessor<O>, D extends Distance<D>> extends MeasurementFunction<O, D>, PreprocessorClient<P, O> {
  /**
   * Returns the preprocessor of this measurement function.
   * 
   * @return the preprocessor of this measurement function
   */
  P getPreprocessor();
}
