package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Interface to mark preprocessor based distance functions.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses Preprocessor.Instance
 * 
 * @param <O> Database object type
 * @param <D> Distance function
 */
public interface PreprocessorBasedDistanceFunction<O extends DatabaseObject, D extends Distance<D>> extends DistanceFunction<O, D> {
  /**
   * OptionID for the preprocessor parameter
   */
  public static final OptionID PREPROCESSOR_ID = OptionID.getOrCreateOptionID("distancefunction.preprocessor", "Preprocessor to use.");

  /**
   * Instantiate with a database to get the actual distance query.
   * 
   * @param database
   * @return Actual distance query.
   */
  @Override
  public <T extends O> Instance<T, ?, D> instantiate(Database<T> database);

  /**
   * Instance interface for Preprocessor based distance functions.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Object type
   * @param <D> Distance type
   */
  public static interface Instance<T extends DatabaseObject, P extends Preprocessor.Instance<?>, D extends Distance<D>> extends DistanceQuery<T, D> {
    /**
     * Get the preprocessor instance.
     * 
     * @return the preprocessor instance
     */
    public P getPreprocessorInstance();
  }
}