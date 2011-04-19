package de.lmu.ifi.dbs.elki.evaluation;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for post-algorithm evaluations, such as histograms, outlier score
 * evaluations, ...
 * 
 * @author Erich Schubert
 */
public interface Evaluator extends Parameterizable {
  /**
   * Process a result.
   * 
   * @param db Database the result is for
   * @param result Result object
   * @param hierarchy Result hierarchy to use
   */
  public abstract void processResult(Database db, Result result, ResultHierarchy hierarchy);
}
