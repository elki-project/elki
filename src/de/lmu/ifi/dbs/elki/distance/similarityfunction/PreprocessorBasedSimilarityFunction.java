package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;

/**
 * Interface for preprocessor based similarity functions.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface PreprocessorBasedSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends SimilarityFunction<O, D> {
  /**
   * Preprocess the database to get the actual distance function.
   * 
   * @param database
   * @return Actual distance query.
   */
  @Override
  public abstract <T extends O> Instance<T, ?, D> instantiate(Database<T> database);

  /**
   * Instance interface for Preprocessor based distance functions.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Object type
   * @param <D> Distance type
   */
  public static interface Instance<T extends DatabaseObject, P extends Preprocessor.Instance<?>, D extends Distance<D>> extends SimilarityQuery<T, D> {
    /**
     * Get the preprocessor instance.
     * 
     * @return the preprocessor instance
     */
    public P getPreprocessorInstance();
  }
}