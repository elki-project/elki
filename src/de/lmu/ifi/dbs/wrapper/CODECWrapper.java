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
 * Wrapper class for the CoDeC algorithm. Partitions a database according to the correlation dimension of
 * its objects, performs the algorithm DBSCAN over the partitions and then determines the correlation
 * dependencies in each cluster of each partition.
 *
 * @author Elke Achtert (<a  href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CODECWrapper extends COPACWrapper {
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

  /**
   * Initailizes the parametrs for the algorithm to apply.
   *
   * @param parameters the parametrs array
   */
  protected void initParametersForAlgorithm(List<String> parameters) {
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(CoDeC.class.getName());

    // clustering algorithm COPAC
    parameters.add(OptionHandler.OPTION_PREFIX + CoDeC.CLUSTERING_ALGORITHM_P);
    parameters.add(COPAC.class.getName());
  }
}
