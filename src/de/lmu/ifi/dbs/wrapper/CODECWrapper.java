package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.CoDeC;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
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
   * @see AbstractAlgorithmWrapper#initParameters(java.util.List<java.lang.String>)
   */
  public List<String> initParameters(List<String> remainingParameters) {
    List<String> parameters = super.initParameters(remainingParameters);

    int index = parameters.indexOf(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.remove(index);
    parameters.remove(index);

    // algorithm CODEC
    parameters.add(0, OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(1, CoDeC.class.getName());

    // clustering algorithm COPAC
    parameters.add(2, OptionHandler.OPTION_PREFIX + CoDeC.CLUSTERING_ALGORITHM_P);
    parameters.add(3, COPAC.class.getName());

    return parameters;
  }

  public static void main(String[] args) {
    CODECWrapper wrapper = new CODECWrapper();
    try {
      wrapper.run(args);
    }
    catch (WrongParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (NoParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (UnusedParameterException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
  }

}
