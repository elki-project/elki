package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.List;

/**
 * Wrapper class for OPTICS algorithm. Performs an attribute wise normalization
 * on the database objects.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class OPTICSWrapper extends NormalizationWrapper {

  /**
   * The value of the epsilon parameter.
   */
  private String epsilon;

  /**
   * The value of the minpts parameter.
   */
  private int minpts;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    OPTICSWrapper wrapper = new OPTICSWrapper();
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
   * Sets the parameters epsilon and minpts in the parameter map additionally
   * to the parameters provided by super-classes.
   */
  public OPTICSWrapper() {
    super();
    // parameter epsilon
    optionHandler.put(OPTICS.EPSILON_P, new PatternParameter(OPTICS.EPSILON_P, OPTICS.EPSILON_D));

    //parameter min points
    optionHandler.put(OPTICS.MINPTS_P, new IntParameter(OPTICS.MINPTS_P, OPTICS.MINPTS_D, new GreaterConstraint(0)));
  }

  /**
   * @see KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() {
    List<String> parameters = super.getKDDTaskParameters();

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

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // epsilon, minpts
    epsilon = (String) optionHandler.getOptionValue(OPTICS.EPSILON_P);
    minpts = (Integer) optionHandler.getOptionValue(OPTICS.MINPTS_P);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(OPTICS.EPSILON_P, epsilon);
    mySettings.addSetting(OPTICS.MINPTS_P, Integer.toString(minpts));
    return settings;
  }
}
