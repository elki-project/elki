package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

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
  public abstract DatabaseSimilarityFunction<O, D> preprocess(Database<O> database);
}
