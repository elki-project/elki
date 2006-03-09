package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;

import java.util.Arrays;
import java.util.List;

/**
 * AbstractAlgorithmWrapper is an abstract super class for all wrapper
 * classes running algorithms.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractAlgorithmWrapper extends AbstractWrapper {
  /**
   * @see Wrapper#run(String[])
   */
  public final void run(String[] args) {
    String[] remainingParameters = setParameters(args);

    List<String> params = initParameters(Arrays.asList(remainingParameters));
    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    task.run();
  }

  /**
   * Initializes the parameters.
   *
   * @return an array containing the parameters to run the algorithm.
   */
  public abstract List<String> initParameters(List<String> remainingParameters);
}
