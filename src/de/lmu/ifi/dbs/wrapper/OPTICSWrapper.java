package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.OPTICS;
import de.lmu.ifi.dbs.database.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.database.MTreeDatabase;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseDoubleVectorNormalization;
import de.lmu.ifi.dbs.parser.AbstractParser;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;

/**
 * Wrapper class for OPTICS algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class OPTICSWrapper extends AbstractWrapper {
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
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    super.setParameters(args);
    try {
      epsilon = optionHandler.getOptionValue(EPSILON_P);
      minpts = Integer.parseInt(optionHandler.getOptionValue(MINPTS_P));
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return new String[0];
  }

  /**
   * Runs the OPTICS algorithm.
   */
  public void runOPTICS() {
    ArrayList<String> params = getRemainingParameters();

    // algorithm OPTICS
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(OPTICS.class.getName());

    // epsilon
    params.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    params.add(epsilon);

    // minpts
    params.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    params.add(Integer.toString(minpts));

    // distance function
    params.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    params.add(EuklideanDistanceFunction.class.getName());

    // normalization
//    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
//    params.add(AttributeWiseDoubleVectorNormalization.class.getName());
//    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // database
    params.add(OptionHandler.OPTION_PREFIX + AbstractParser.DATABASE_CLASS_P);
    params.add(MTreeDatabase.class.getName());

    // distance cache
    params.add(OptionHandler.OPTION_PREFIX + MTreeDatabase.CACHE_F);

    // bulk load
//    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.BULK_LOAD_F);

    // page size
    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.PAGE_SIZE_P);
    params.add("4000");

    // cache size
    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.CACHE_SIZE_P);
    params.add("120000");

    // input
    params.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    params.add(input);

    // output
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    params.add(output);

    if (time) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    task.run();
  }

  /**
   * Runs the COPAC algorithm accordingly to the specified parameters.
   *
   * @param args parameter list according to description
   */
  public void run(String[] args) {
    this.setParameters(args);
    this.runOPTICS();
  }

  public static void main(String[] args) {
    OPTICSWrapper copac = new OPTICSWrapper();
    try {
      copac.run(args);
    }
    catch (AbortException e) {
      System.err.println(e.getMessage());
    }
    catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
    }
    catch (IllegalStateException e) {
      System.err.println(e.getMessage());
    }
  }
}
