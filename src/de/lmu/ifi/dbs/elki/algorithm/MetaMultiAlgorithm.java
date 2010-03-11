package de.lmu.ifi.dbs.elki.algorithm;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * Meta algorithm that will run multiple algorithms and join the result.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object class.
 */
@Title("Meta Multi-Algorithm")
@Description("Used to run multiple algorithms on the same database and merge the result into one MultiResult.")
public class MetaMultiAlgorithm<O extends DatabaseObject> extends AbstractAlgorithm<O, MultiResult> {
  /**
   * Object ID for algorithms.
   */
  public final static OptionID ALGORITHMS_ID = OptionID.getOrCreateOptionID("algorithms", "Algorithms to run");

  /**
   * Parameter to specify the algorithm to be applied, must extend
   * {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}.
   * <p>
   * Key: {@code -algorithms}
   * </p>
   */
  private final ObjectListParameter<Algorithm<O, Result>> ALGORITHMS_PARAM = new ObjectListParameter<Algorithm<O, Result>>(ALGORITHMS_ID, Algorithm.class);

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
    if(config.grab(ALGORITHMS_PARAM)) {
      ListParameterization subconfig = new ListParameterization();
      for(int i = 0; i < ALGORITHMS_PARAM.getListSize(); i++) {
        subconfig.addParameter(OptionID.ALGORITHM_VERBOSE, isVerbose());
        subconfig.addParameter(OptionID.ALGORITHM_TIME, isTime());
      }
      ChainedParameterization chain = new ChainedParameterization(subconfig, config);
      chain.errorsTo(config);
      algorithms = ALGORITHMS_PARAM.instantiateClasses(chain);
      // We don't care about errors for -verbose and -time flags.
      subconfig.logAndClearReportedErrors();
      subconfig.clearErrors();
      chain.logAndClearReportedErrors();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setTime(boolean time) {
    super.setTime(time);
    if(algorithms != null) {
      for(Algorithm<?, ?> alg : algorithms) {
        alg.setTime(time);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setVerbose(boolean verbose) {
    super.setVerbose(verbose);
    if(algorithms != null) {
      for(Algorithm<?, ?> alg : algorithms) {
        alg.setVerbose(verbose);
      }
    }
  }

  /**
   * Result storage.
   */
  private MultiResult result;

  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    result = new MultiResult();
    for(Algorithm<O, Result> alg : algorithms) {
      Result res = alg.run(database);
      result.addResult(res);
    }
    return result;
  }

  @Override
  public MultiResult getResult() {
    return result;
  }
}