package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for any class that can handle results
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses AnyResult oneway - - processes
 * @apiviz.uses Normalization
 *
 * @param <O> Object type
 * @param <R> Result type
 */
public interface ResultHandler<O extends DatabaseObject, R extends AnyResult> extends Parameterizable {
  /**
   * Process a result.
   * 
   * @param db Database the result is for
   * @param result Result object
   */
  public abstract void processResult(Database<O> db, R result);

  /**
   * Setter for normalization
   * 
   * @param normalization new normalization object
   */
  public abstract void setNormalization(Normalization<O> normalization);
}