package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;

/**
 * Wrapper class for LOF algorithm. Performs an attribute wise normalization
 * on the database objects.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LOFWrapper extends FileBasedDatabaseConnectionWrapper {
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
    LOFWrapper wrapper = new LOFWrapper();
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
   * Sets the parameters epsilon and minpts in the parameter map additionally
   * to the parameters provided by super-classes.
   */
  public LOFWrapper() {
    super();
    parameterToDescription.put(LOF.MINPTS_P + OptionHandler.EXPECTS_VALUE, LOF.MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() throws ParameterException {
    List<String> parameters = super.getParameters();

    // algorithm LOF
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(LOF.class.getName());

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + LOF.MINPTS_P);
    parameters.add(optionHandler.getOptionValue(LOF.MINPTS_P));

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    parameters.add(EuklideanDistanceFunction.class.getName());

    // normalization
//    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
//    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
//    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // database
    // params.add(OptionHandler.OPTION_PREFIX +
    // AbstractDatabaseConnection.DATABASE_CLASS_P);
    // params.add(RTreeDatabase.class.getName());

    // distance cache
    // params.add(OptionHandler.OPTION_PREFIX + AbstractDatabase.CACHE_F);

    // bulk load
    // params.add(OptionHandler.OPTION_PREFIX +
    // SpatialIndexDatabase.BULK_LOAD_F);

    // page size
    // params.add(OptionHandler.OPTION_PREFIX +
    // SpatialIndexDatabase.PAGE_SIZE_P);
    // params.add("4000");

    // cache size
    // params.add(OptionHandler.OPTION_PREFIX +
    // SpatialIndexDatabase.CACHE_SIZE_P);
    // params.add("120000");

    return parameters;
  }
}
