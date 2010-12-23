package de.lmu.ifi.dbs.elki.evaluation;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for post-algorithm evaluations, such as histograms, outlier score
 * evaluations, ...
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Normalization
 * 
 * @param <O> Database object type
 */
public interface Evaluator<O extends DatabaseObject> extends Parameterizable {
  /**
   * Process a result.
   * 
   * @param db Database the result is for
   * @param result Result object
   * @param hierarchy Result hierarchy to use
   */
  public abstract void processResult(Database<O> db, Result result, ResultHierarchy hierarchy);

  /**
   * Setter for normalization
   * 
   * @param normalization new normalization object
   */
  public abstract void setNormalization(Normalization<O> normalization);
}
