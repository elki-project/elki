package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for the dependency derivator
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DependencyDerivatorWrapper extends AbstractAlgorithmWrapper {

  public static void main(String[] args) {
    DependencyDerivatorWrapper derivator = new DependencyDerivatorWrapper();
    try {
      derivator.run(args);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * @see AbstractAlgorithmWrapper#initParameters(java.util.List<String>)
   */
  public List<String> initParameters(List<String> remainingParameters) {
    ArrayList<String> parameters = new ArrayList<String>(remainingParameters);

    // algorithm DependencyDerivator
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(DependencyDerivator.class.getName());

    // normalization
//    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
//    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
//    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // input
    parameters.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    parameters.add(input);

    // output
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    parameters.add(output);

    if (time) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    return parameters;
  }
}
