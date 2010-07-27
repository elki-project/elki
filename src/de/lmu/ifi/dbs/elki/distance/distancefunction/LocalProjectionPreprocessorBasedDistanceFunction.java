package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectionResult;
import de.lmu.ifi.dbs.elki.preprocessing.LocalProjectionPreprocessor;

/**
 * Interface for local PCA based preprocessors.
 * 
 * @author Erich Schubert
 *
 * @param <O> Database object type
 * @param <D> Distance type
 */
public interface LocalProjectionPreprocessorBasedDistanceFunction<O extends NumberVector<?, ?>, P extends LocalProjectionPreprocessor<? super O, R>, R extends ProjectionResult, D extends Distance<D>> extends PreprocessorBasedDistanceFunction<O, D> {
  /**
   * Instantiate with a database to get the actual distance query.
   * 
   * @param database
   * @return Actual distance query.
   */
  @Override
  public <T extends O> Instance<T, ? extends LocalProjectionPreprocessor.Instance<R>, D> instantiate(Database<T> database);
}