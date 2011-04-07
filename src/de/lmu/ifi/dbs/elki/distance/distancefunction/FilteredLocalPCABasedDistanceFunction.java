package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.FilteredLocalPCAIndex;

/**
 * Interface for local PCA based preprocessors.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Instance
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public interface FilteredLocalPCABasedDistanceFunction<O extends NumberVector<?, ?>, P extends FilteredLocalPCAIndex<? super O>, D extends Distance<D>> extends IndexBasedDistanceFunction<O, D> {
  /**
   * Instantiate with a database to get the actual distance query.
   * 
   * @param database
   * @return Actual distance query.
   */
  @Override
  public <T extends O> Instance<T, ?, D> instantiate(Database<T> database);

  /**
   * Instance produced by the distance function.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Database object type
   * @param <I> Index type
   * @param <D> Distance type
   */
  public static interface Instance<T extends NumberVector<?, ?>, I extends Index<T>, D extends Distance<D>> extends IndexBasedDistanceFunction.Instance<T, I, D> {
    // No additional restrictions
  }
}