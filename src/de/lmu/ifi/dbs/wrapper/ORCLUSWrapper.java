package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.ORCLUS;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.List;

/**
 * Wrapper class for COPAC algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ORCLUSWrapper extends FileBasedDatabaseConnectionWrapper {
  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    ORCLUSWrapper wrapper = new ORCLUSWrapper();
    try {
      wrapper.run(args);
    }
    catch (ParameterException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
  }

  /**
   * Sets the parameters k, k_i and dim in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public ORCLUSWrapper() {
    super();
    parameterToDescription.put(ORCLUS.K_P + OptionHandler.EXPECTS_VALUE, ORCLUS.K_D);
    parameterToDescription.put(ORCLUS.K_I_P + OptionHandler.EXPECTS_VALUE, ORCLUS.K_I_D);
    parameterToDescription.put(ORCLUS.DIM_P + OptionHandler.EXPECTS_VALUE, ORCLUS.DIM_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() throws ParameterException {
    List<String> parameters = super.getParameters();

    // ORCLUS algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(ORCLUS.class.getName());

    // dim
    parameters.add(OptionHandler.OPTION_PREFIX + ORCLUS.DIM_P);
    parameters.add(optionHandler.getOptionValue(ORCLUS.DIM_P));

    // k
    parameters.add(OptionHandler.OPTION_PREFIX + ORCLUS.K_P);
    parameters.add(optionHandler.getOptionValue(ORCLUS.K_P));

    // k_i
    if (optionHandler.isSet(ORCLUS.K_I_P)) {
      parameters.add(OptionHandler.OPTION_PREFIX + ORCLUS.K_I_P);
      parameters.add(optionHandler.getOptionValue(ORCLUS.K_I_P));
    }

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    return parameters;
  }


}
