package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;

/**
 * Wrapper class for the dependency derivator
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DependencyDerivatorWrapper extends AbstractWrapper {

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
   * Runs the wrapper with the specified arguments.
   *
   * @param args parameter list
   */
  public void run(String[] args) {
    this.setParameters(args);
    this.runDependencyDerivator();
  }

  /**
   * Runs the DependencyDerivator algorithm.
   */
  private void runDependencyDerivator() {
    ArrayList<String> params = getRemainingParameters();

    // algorithm DependencyDerivator
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(DependencyDerivator.class.getName());

    // alpha
//    params.add(OptionHandler.OPTION_PREFIX + DependencyDerivator.ALPHA_P);
//    params.add("0.99");

//    params.add(OptionHandler.OPTION_PREFIX + DependencyDerivator.DIMENSIONALITY_P);
//    params.add("4");

    // normalization
//    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
//    params.add(AttributeWiseRealVectorNormalization.class.getName());
//    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // input
    params.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    params.add(input);

    // output
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    params.add(output);

    if (time) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    task.run();
  }
}
