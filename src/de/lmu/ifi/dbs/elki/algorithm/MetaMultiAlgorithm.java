package de.lmu.ifi.dbs.elki.algorithm;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * Meta algorithm that will run multiple algorithms and join the result.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object class.
 */
public class MetaMultiAlgorithm<O extends DatabaseObject> extends AbstractAlgorithm<O, MultiResult> {
  /**
   * Parameter to specify the algorithm to be applied, must extend
   * {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}.
   * <p>
   * Key: {@code -algorithm}
   * </p>
   */
  private final ObjectListParameter<Algorithm<O, Result>> ALGORITHMS_PARAM = new ObjectListParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);
  
  /**
   * The instantiated algorithms to run.
   */
  private List<Algorithm<O, Result>> algorithms;

  /**
   * Constructor
   * 
   * @param config Parameterization
   */
  public MetaMultiAlgorithm(Parameterization config) {
    super(config);
    if (config.grab(this, ALGORITHMS_PARAM)) {
      algorithms = ALGORITHMS_PARAM.instantiateClasses(config);
    }
  }

  /**
   * Result storage.
   */
  private MultiResult result;

  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    result = new MultiResult();
    for (Algorithm<O, Result> alg : algorithms) {
      Result res = alg.run(database);
      result.addResult(res);
    }
    return result;
  }

  @Override
  public Description getDescription() {
    return new Description("MetaMultiAlgorithm", "Meta Multi-Algorithm", "Used to run multiple algorithms on the same database and merge the result into one MultiResult.", "");
  }

  @Override
  public MultiResult getResult() {
    return result;
  }
}