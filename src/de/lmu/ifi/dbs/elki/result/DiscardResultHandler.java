package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

/**
 * A dummy result handler that discards the actual result, for use in benchmarks.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object class
 * @param <R> Result class
 */
public class DiscardResultHandler<O extends DatabaseObject, R extends Result> extends AbstractParameterizable implements ResultHandler<O, R> {
  /**
   * Default constructor.
   */
  public DiscardResultHandler() {
    // empty constructor
  }
  
  /**
   * Process the result... by discarding
   * 
   * @param db discarded
   * @param result discarded
   */
  @Override
  public void processResult(Database<O> db, R result) {
    // discard the result.
  }

  /**
   * Set normalization
   * 
   * @param normalization discarded
   */
  @Override
  public void setNormalization(Normalization<O> normalization) {
    // do nothing
  }
}
