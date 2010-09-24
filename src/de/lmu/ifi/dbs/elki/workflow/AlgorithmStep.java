package de.lmu.ifi.dbs.elki.workflow;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * The "algorithms" step, where data is analyzed.
 * 
 * @author Erich Schubert
 * 
 * @param <O> database object type
 */
public class AlgorithmStep<O extends DatabaseObject> implements Parameterizable {
  /**
   * Holds the algorithm to run.
   */
  private List<Algorithm<O, Result>> algorithms;

  /**
   * The algorithm output
   */
  private MultiResult result = null;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AlgorithmStep(Parameterization config) {
    super();
    config = config.descend(this);
    // parameter algorithm
    final ObjectListParameter<Algorithm<O, Result>> ALGORITHM_PARAM = new ObjectListParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);
    if(config.grab(ALGORITHM_PARAM)) {
      algorithms = ALGORITHM_PARAM.instantiateClasses(config);
    }
  }

  /**
   * Run algorithms.
   * 
   * @param database Database
   * @param existing Existing results
   * @return Algorithm result
   */
  public MultiResult runAlgorithms(Database<O> database, Result existing) {
    result = new MultiResult();
    for(Algorithm<O, Result> algorithm : algorithms) {
      result.addResult(algorithm.run(database));
    }
    // Add existing results.
    result.addResult(existing);
    return result;
  }

  /**
   * Get the algorithm result.
   * 
   * @return Algorithm result.
   */
  public MultiResult getResult() {
    return result;
  }
}