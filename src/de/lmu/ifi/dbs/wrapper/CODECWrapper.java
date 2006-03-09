package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.CoDeC;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterFormatException;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.List;


/**
 * Wrapper class for the CoDeC algorithm.
 *
 * @author Elke Achtert (<a  href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CODECWrapper extends COPACWrapper {
  /**
   * Runs the CODEC algorithm accordingly to the specified parameters.
   *
   * @param args parameter list according to description
   */
  public void run(String[] args) {
    this.setParameters(args);

    List<String> params = initParameters();
    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    task.run();
  }

  /**
   * Initializes the parameters.
   *
   * @return an array containing the parameters to run the algorithm.
   */
  protected List<String> initParameters() {
    List<String> params = super.initParameters();
    params.remove(0);
    params.remove(0);

    // algorithm CODEC
    params.add(0, OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(1, CoDeC.class.getName());

    // clustering algorithm COPAC
    params.add(2, OptionHandler.OPTION_PREFIX + CoDeC.CLUSTERING_ALGORITHM_P);
    params.add(3, COPAC.class.getName());

    return params;
  }

  public static void main(String[] args) {
    CODECWrapper codec = new CODECWrapper();
    try {
      codec.run(args);
    }
    catch (ParameterFormatException e) {
      System.err.println(codec.optionHandler.usage(e.getMessage()));
    }
    catch (NoParameterValueException e) {
      System.err.println(codec.optionHandler.usage(e.getMessage()));
    }
    catch (UnusedParameterException e) {
      System.err.println(codec.optionHandler.usage(e.getMessage()));
    }
  }

}
