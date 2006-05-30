package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.outlier.OnlineLOF;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class for LOF algorithm. Performs an attribute wise normalization
 * on the database objects.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class OnlineLOFWrapper extends FileBasedDatabaseConnectionWrapper {
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
    OnlineLOFWrapper wrapper = new OnlineLOFWrapper();
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
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      cause.printStackTrace();
      e.printStackTrace();
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameters epsilon and minpts in the parameter map additionally
   * to the parameters provided by super-classes.
   */
  public OnlineLOFWrapper() {
    super();
    parameterToDescription.put(OnlineLOF.MINPTS_P + OptionHandler.EXPECTS_VALUE, OnlineLOF.MINPTS_D);
    parameterToDescription.put(OnlineLOF.INSERTIONS_P + OptionHandler.EXPECTS_VALUE, OnlineLOF.INSERTIONS_D);
    parameterToDescription.put(OnlineLOF.LOF_P + OptionHandler.EXPECTS_VALUE, OnlineLOF.LOF_D);
    parameterToDescription.put(OnlineLOF.NN_P + OptionHandler.EXPECTS_VALUE, OnlineLOF.NN_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() throws ParameterException {
    List<String> parameters = super.getParameters();

    // algorithm OnlineLOF
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(OnlineLOF.class.getName());

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.MINPTS_P);
    parameters.add(optionHandler.getOptionValue(OnlineLOF.MINPTS_P));

    // insertions
    parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.INSERTIONS_P);
    parameters.add(optionHandler.getOptionValue(OnlineLOF.INSERTIONS_P));

    // lof
    parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.LOF_P);
    parameters.add(optionHandler.getOptionValue(OnlineLOF.LOF_P));

    // nn
    parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.NN_P);
    parameters.add(optionHandler.getOptionValue(OnlineLOF.NN_P));

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.DISTANCE_FUNCTION_P);
    parameters.add(EuklideanDistanceFunction.class.getName());

    // page size
//    parameters.add(OptionHandler.OPTION_PREFIX + LOF.PAGE_SIZE_P);
//    parameters.add("8000");

    // cache size
//    parameters.add(OptionHandler.OPTION_PREFIX + LOF.CACHE_SIZE_P);
//    parameters.add("" + 8000 * 10);


    return parameters;
  }
}
