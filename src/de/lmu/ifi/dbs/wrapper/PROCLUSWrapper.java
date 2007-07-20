package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.PROCLUS;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for PROCLUS algorithm. Performs an attribute wise normalization
 * on the database objects.
 *
 * @author Elke Achtert 
 */
public class PROCLUSWrapper extends FileBasedDatabaseConnectionWrapper {

  /**
   * The value of the k parameter.
   */
  private int k;

  /**
   * The value of the k_i parameter.
   */
  private int k_i;

  /**
   * The value of the dim parameter.
   */
  private int dim;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    PROCLUSWrapper wrapper = new PROCLUSWrapper();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
      wrapper.verbose(e.getMessage());
    }
    catch (Exception e) {
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameters k, k_i and dim in the parameter map additionally to
   * the parameters provided by super-classes.
   */
  public PROCLUSWrapper() {
    super();
    optionHandler.put(PROCLUS.K_P, new IntParameter(PROCLUS.K_P, PROCLUS.K_D, new GreaterConstraint(0)));

    IntParameter ki = new IntParameter(PROCLUS.K_I_P, PROCLUS.K_I_D, new GreaterConstraint(0));
    ki.setDefaultValue(PROCLUS.K_I_DEFAULT);
    optionHandler.put(PROCLUS.K_I_P, ki);

    optionHandler.put(PROCLUS.L_P, new IntParameter(PROCLUS.L_P, PROCLUS.L_D, new GreaterConstraint(0)));
  }

  /**
   * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() throws UnusedParameterException {
    List<String> parameters = super.getKDDTaskParameters();

    // PROCLUS algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(PROCLUS.class.getName());

    // dim
    parameters.add(OptionHandler.OPTION_PREFIX + PROCLUS.L_P);
    parameters.add(Integer.toString(dim));

    // k
    parameters.add(OptionHandler.OPTION_PREFIX + PROCLUS.K_P);
    parameters.add(Integer.toString(k));

    // k_i
    parameters.add(OptionHandler.OPTION_PREFIX + PROCLUS.K_I_P);
    parameters.add(Integer.toString(k_i));

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    k = (Integer) optionHandler.getOptionValue(PROCLUS.K_P);
    dim = (Integer) optionHandler.getOptionValue(PROCLUS.L_P);
    k_i = (Integer) optionHandler.getOptionValue(PROCLUS.K_I_P);

    return remainingParameters;
  }
}
