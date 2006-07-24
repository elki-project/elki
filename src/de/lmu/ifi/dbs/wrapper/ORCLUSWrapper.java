package de.lmu.ifi.dbs.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.ORCLUS;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Wrapper class for COPAC algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ORCLUSWrapper extends FileBasedDatabaseConnectionWrapper {

  /**
   * The value of the k parameter.
   */
  private String k;

  /**
   * The value of the k_i parameter.
   */
  private String k_i;

  /**
   * The value of the dim parameter.
   */
  private String dim;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    ORCLUSWrapper wrapper = new ORCLUSWrapper();
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
   * Sets the parameters k, k_i and dim in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public ORCLUSWrapper() {
    super();
    optionHandler.put(ORCLUS.K_P, new Parameter(ORCLUS.K_P,ORCLUS.K_D,Parameter.Types.INT));
    optionHandler.put(ORCLUS.K_I_P, new Parameter(ORCLUS.K_I_P,ORCLUS.K_I_D,Parameter.Types.INT));
    optionHandler.put(ORCLUS.DIM_P, new Parameter(ORCLUS.DIM_P,ORCLUS.DIM_D,Parameter.Types.INT));
  }

  /**
   * @see KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() {
    List<String> parameters = super.getKDDTaskParameters();

    // ORCLUS algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(ORCLUS.class.getName());

    // dim
    parameters.add(OptionHandler.OPTION_PREFIX + ORCLUS.DIM_P);
    parameters.add(dim);

    // k
    parameters.add(OptionHandler.OPTION_PREFIX + ORCLUS.K_P);
    parameters.add(k);

    // k_i
    parameters.add(OptionHandler.OPTION_PREFIX + ORCLUS.K_I_P);
    parameters.add(k_i);

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // k, dim
    k = optionHandler.getOptionValue(ORCLUS.K_P);
    dim = optionHandler.getOptionValue(ORCLUS.DIM_P);

    // k_i
    if (optionHandler.isSet(ORCLUS.K_I_P)) {
      k_i = optionHandler.getOptionValue(ORCLUS.K_I_P);
    }
    else {
      k_i = Integer.toString(ORCLUS.K_I_DEFAULT);
    }

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(ORCLUS.K_P, k);
    mySettings.addSetting(ORCLUS.K_I_P, k_i);
    mySettings.addSetting(ORCLUS.DIM_P, dim);
    return settings;
  }


}
