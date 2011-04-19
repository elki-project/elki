package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for any class that can handle results
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses Result oneway - - processes
 */
public interface ResultHandler extends Parameterizable {
  /**
   * Process a result.
   * 
   * @param db Database the result is for
   * @param result Result object
   */
  public abstract void processResult(Database db, Result result);
}