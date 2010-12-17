package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;

/**
 * Interface for preprocessor/index based similarity functions.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface IndexBasedSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends SimilarityFunction<O, D> {
  /**
   * Preprocess the database to get the actual distance function.
   * 
   * @param database
   * @return Actual distance query.
   */
  @Override
  public abstract <T extends O> Instance<T, ?, D> instantiate(Database<T> database);

  /**
   * Instance interface for index/preprocessor based distance functions.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Object type
   * @param <D> Distance type
   */
  public static interface Instance<T extends DatabaseObject, I extends Index<?>, D extends Distance<D>> extends SimilarityQuery<T, D> {
    /**
     * Get the index used.
     * 
     * @return the index used
     */
    public I getIndex();
  }
}