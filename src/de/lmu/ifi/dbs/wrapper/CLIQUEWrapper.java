package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.clique.CLIQUE;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.LessConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for CLIQUE algorithm.
 *
 * @author Elke Achtert
 */
public class CLIQUEWrapper extends FileBasedDatabaseConnectionWrapper {

  /**
   * The value of the xsi parameter.
   */
  private int xsi;

  /**
   * The value of the tau parameter.
   */
  private double tau;

  /**
   * The value of the prune flag.
   */
  private boolean prune;


  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    CLIQUEWrapper wrapper = new CLIQUEWrapper();
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
   * Sets the parameters epsilon and minpts in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public CLIQUEWrapper() {
    super();
    //parameter xsi
    optionHandler.put(new IntParameter(CLIQUE.XSI_P, CLIQUE.XSI_D, new GreaterConstraint(0)));

    //parameter tau
    List<ParameterConstraint<Number>> tauConstraints = new ArrayList<ParameterConstraint<Number>>();
    tauConstraints.add(new GreaterConstraint(0));
    tauConstraints.add(new LessConstraint(1));
    optionHandler.put(new DoubleParameter(CLIQUE.TAU_P, CLIQUE.TAU_D, tauConstraints));

    //flag prune
    optionHandler.put(new Flag(CLIQUE.PRUNE_F, CLIQUE.PRUNE_D));
  }

  /**
   * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() throws UnusedParameterException {
    List<String> parameters = super.getKDDTaskParameters();

    // algorithm CLIQUE
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(CLIQUE.class.getName());

    // xsi
    parameters.add(OptionHandler.OPTION_PREFIX + CLIQUE.XSI_P);
    parameters.add(Integer.toString(xsi));

    // tau
    parameters.add(OptionHandler.OPTION_PREFIX + CLIQUE.TAU_P);
    parameters.add(Double.toString(tau));

    // prune
    if (prune) {
      parameters.add(OptionHandler.OPTION_PREFIX + CLIQUE.PRUNE_F);
    }

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // xsi, tau, prune
    xsi = (Integer) optionHandler.getOptionValue(CLIQUE.XSI_P);
    tau = (Double) optionHandler.getOptionValue(CLIQUE.TAU_P);
    prune = optionHandler.isSet(CLIQUE.PRUNE_F);

    return remainingParameters;
  }
}
