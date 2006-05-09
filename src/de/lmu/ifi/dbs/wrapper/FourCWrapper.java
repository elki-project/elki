package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.clustering.FourC;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper for the 4C algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Arthur Zimek (<a  href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourCWrapper extends FileBasedDatabaseConnectionWrapper {
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
    FourCWrapper wrapper = new FourCWrapper();
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
   * Provides a wrapper for the 4C algorithm.
   */
  public FourCWrapper() {
    super();
    parameterToDescription.put(FourC.EPSILON_P + OptionHandler.EXPECTS_VALUE, FourC.EPSILON_D);
    parameterToDescription.put(FourC.MINPTS_P + OptionHandler.EXPECTS_VALUE, FourC.MINPTS_D);
    parameterToDescription.put(FourC.LAMBDA_P + OptionHandler.EXPECTS_VALUE, FourC.LAMBDA_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() throws ParameterException {
    List<String> parameters = super.getParameters();

    // 4C algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(FourC.class.getName());

    // epsilon for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + FourC.EPSILON_P);
    parameters.add(optionHandler.getOptionValue(FourC.EPSILON_P));

    // minpts for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + FourC.MINPTS_P);
    parameters.add(optionHandler.getOptionValue(FourC.MINPTS_P));

    // lambda for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + FourC.LAMBDA_P);
    parameters.add(optionHandler.getOptionValue(FourC.LAMBDA_P));

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    return parameters;
  }
}
