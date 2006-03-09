package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for OPTICS algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class OPTICSWrapper extends AbstractAlgorithmWrapper {
  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "epsilon";

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the distance function " + LocallyWeightedDistanceFunction.class.getName();

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<int>minpts";

  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Minimum points.
   */
  protected int minpts;

  /**
   * Sets epsilon and minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since DBSCANWrapper is a non-abstract class,
   * finally optionHandler is initialized.
   */
  public OPTICSWrapper() {
    super();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Sets the parameters epsilon and minpts additionally to the parameters set
   * by the super-class' method. Both epsilon and minpts are required
   * parameters.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) {
    String[] remainingParameters = super.setParameters(args);

    epsilon = optionHandler.getOptionValue(EPSILON_P);
    try {
      minpts = Integer.parseInt(optionHandler.getOptionValue(MINPTS_P));
    }
    catch (NumberFormatException e) {
      WrongParameterValueException pe = new WrongParameterValueException(MINPTS_P, optionHandler.getOptionValue(MINPTS_P));
      pe.fillInStackTrace();
      throw pe;
    }
    return remainingParameters;
  }

  /**
   * @see AbstractAlgorithmWrapper#initParameters(java.util.List<String>)
   */
  public List<String> initParameters(List<String> remainingParameters) {
    ArrayList<String> parameters = new ArrayList<String>(remainingParameters);

    // algorithm OPTICS
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(OPTICS.class.getName());

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    parameters.add(epsilon);

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    parameters.add(Integer.toString(minpts));

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    parameters.add(EuklideanDistanceFunction.class.getName());

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

    // input
    parameters.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    parameters.add(input);

    // output
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    parameters.add(output);

    if (time) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    return parameters;
  }

  public static void main(String[] args) {
    OPTICSWrapper wrapper = new OPTICSWrapper();
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
}
