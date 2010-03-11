package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;

/**
 * Interface for any class that can handle results
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <R> Result type
 */
public interface ResultHandler<O extends DatabaseObject, R extends Result> extends Parameterizable {
  /**
   * Process a result.
   * 
   * @param db Database the result is for
   * @param result Result object
   */
  public abstract void processResult(Database<O> db, R result) throws IllegalStateException;

  /**
   * Setter for normalization
   * 
   * @param normalization new normalization object
   */
  public abstract void setNormalization(Normalization<O> normalization);
}