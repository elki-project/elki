package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.DiSH;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class for DiSH algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DiSHWrapper extends FileBasedDatabaseConnectionWrapper {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
  //private static final boolean DEBUG = true;

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
    DiSHWrapper wrapper = new DiSHWrapper();
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
      e.printStackTrace();
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameter minpts and k in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public DiSHWrapper() {
    super();
    parameterToDescription.put(OPTICS.MINPTS_P + OptionHandler.EXPECTS_VALUE, OPTICS.MINPTS_D);
    parameterToDescription.put(DiSHPreprocessor.EPSILON_P + OptionHandler.EXPECTS_VALUE, DiSHPreprocessor.EPSILON_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() throws ParameterException {
    List<String> parameters = super.getParameters();

    // DiSH algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(DiSH.class.getName());

    // minpts for OPTICS
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    parameters.add(optionHandler.getOptionValue(OPTICS.MINPTS_P));

    // minpts for preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.MINPTS_P);
    parameters.add(optionHandler.getOptionValue(OPTICS.MINPTS_P));

    // epsilon for preprocessor
    if (optionHandler.isSet(DiSHPreprocessor.EPSILON_P)) {
      parameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.EPSILON_P);
      parameters.add(optionHandler.getOptionValue(DiSHPreprocessor.EPSILON_P));
    }

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    return parameters;
  }
}
