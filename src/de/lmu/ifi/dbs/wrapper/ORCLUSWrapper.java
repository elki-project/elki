package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.clustering.ORCLUS;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Wrapper class for COPAC algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ORCLUSWrapper extends FileBasedDatabaseConnectionWrapper {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

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
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
      wrapper.logger.info(e.getMessage());
    }
    catch (Exception e) {
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);
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
