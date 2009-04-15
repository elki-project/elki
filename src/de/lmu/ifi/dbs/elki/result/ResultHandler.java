package de.lmu.ifi.dbs.elki.result;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

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
   * @param settings Settings used.
   */
  public abstract void processResult(Database<O> db, R result, List<AttributeSettings> settings);

  /**
   * Setter for normalization
   * 
   * @param normalization new normalization object
   */
  public abstract void setNormalization(Normalization<O> normalization);

}