package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * Wrapper class for DBSCAN algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DBSCANWrapper extends FileBasedDatabaseConnectionWrapper {
  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>the maximum radius of the neighborhood to" +
                                         "be considerd, must be suitable to " +
                                         EuklideanDistanceFunction.class.getName();

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    DBSCANWrapper wrapper = new DBSCANWrapper();
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
   * Sets the parameters epsilon and minpts in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public DBSCANWrapper() {
    super();
    parameterToDescription.put(DBSCAN.EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(DBSCAN.MINPTS_P + OptionHandler.EXPECTS_VALUE, DBSCAN.MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() {
    List<String> parameters = super.getParameters();

    // algorithm DBSCAN
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(DBSCAN.class.getName());

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.EPSILON_P);
    parameters.add(optionHandler.getOptionValue(DBSCAN.EPSILON_P));

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
    parameters.add(optionHandler.getOptionValue(DBSCAN.MINPTS_P));

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // database
//    params.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.DATABASE_CLASS_P);
//    params.add(RTreeDatabase.class.getName());

    // distance cache
//    params.add(OptionHandler.OPTION_PREFIX + AbstractDatabase.CACHE_F);

    // bulk load
//    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.BULK_LOAD_F);

    // page size
//    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.PAGE_SIZE_P);
//    params.add("4000");

    // cache size
//    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.CACHE_SIZE_P);
//    params.add("120000");

    return parameters;
  }
}
