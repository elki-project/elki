package de.lmu.ifi.dbs.elki.workflow;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * The "algorithms" step, where data is analyzed.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Algorithm
 * @apiviz.has Result
 * @apiviz.uses Database
 * 
 * @param <O> database object type
 */
public class AlgorithmStep<O extends DatabaseObject> implements WorkflowStep {
  /**
   * Holds the algorithm to run.
   */
  private List<Algorithm<O, Result>> algorithms;

  /**
   * The algorithm output
   */
  private BasicResult result = null;

  /**
   * Constructor.
   *
   * @param algorithms
   */
  public AlgorithmStep(List<Algorithm<O, Result>> algorithms) {
    super();
    this.algorithms = algorithms;
  }

  /**
   * Run algorithms.
   * 
   * @param database Database
   * @return Algorithm result
   */
  public HierarchicalResult runAlgorithms(Database<O> database) {
    result = new BasicResult("Algorithm Step", "main");
    result.addChildResult(database);
    for(Algorithm<O, Result> algorithm : algorithms) {
      Result res = algorithm.run(database);
      if(res != null) {
        result.addChildResult(res);
      }
    }
    return result;
  }

  /**
   * Get the algorithm result.
   * 
   * @return Algorithm result.
   */
  public HierarchicalResult getResult() {
    return result;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends DatabaseObject> extends AbstractParameterizer {
    /**
     * Holds the algorithm to run.
     */
    protected List<Algorithm<O, Result>> algorithms;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // parameter algorithm
      final ObjectListParameter<Algorithm<O, Result>> ALGORITHM_PARAM = new ObjectListParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);
      if(config.grab(ALGORITHM_PARAM)) {
        algorithms = ALGORITHM_PARAM.instantiateClasses(config);
      }
    }

    @Override
    protected AlgorithmStep<O> makeInstance() {
      return new AlgorithmStep<O>(algorithms);
    }
  }
}