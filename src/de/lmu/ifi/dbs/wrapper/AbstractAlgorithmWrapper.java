package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;

import java.util.Arrays;
import java.util.List;

/**
 * AbstractAlgorithmWrapper is an abstract super class for all wrapper
 * classes running algorithms.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractAlgorithmWrapper extends AbstractWrapper {

  /**
   * @see Wrapper#run(String[])
   */
  public final void run(String[] args) {
    List<String> parameters = Arrays.asList(setParameters(args));

    // database connection
    parameters.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    parameters.add(getInput());

    // output
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    parameters.add(getOutput());

    if (isTime()) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (isVerbose()) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    addParameters(parameters);
    KDDTask task = new KDDTask();
    task.setParameters(parameters.toArray(new String[parameters.size()]));
    task.run();
  }

  /**
   * Adds the parameters to the specified array that are necessary to run this wrapper correctly.
   *
   * @param parameters th arry holding the parameters to run this wrapper
   */
  public abstract void addParameters(List<String> parameters);
}
