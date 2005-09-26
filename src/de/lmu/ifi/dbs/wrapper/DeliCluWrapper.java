package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.DeLiClu;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.KNNJoin;
import de.lmu.ifi.dbs.algorithm.OPTICS;
import de.lmu.ifi.dbs.algorithm.result.ClusterOrder;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.database.DeLiCluTreeDatabase;
import de.lmu.ifi.dbs.database.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.normalization.AttributeWiseDoubleVectorNormalization;
import de.lmu.ifi.dbs.parser.AbstractParser;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;

/**
 * Wrapper class for the DeliClu algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeliCluWrapper extends AbstractWrapper {
  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<int>minpts";

  /**
   * Minimum points.
   */
  protected String minpts;

  /**
   * Remaining parameters
   */
  private String[] remainingParameters;

  /**
   * Sets minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since DeliCluWrapper is a non-abstract class,
   * finally optionHandler is initialized.
   */
  public DeliCluWrapper() {
    super();
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
    remainingParameters = super.setParameters(args);
    try {
      minpts = optionHandler.getOptionValue(MINPTS_P);
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
   * Runs the DeliClu algorithm.
   */
  public Result runDeliClu() {
    if (output == null)
      throw new IllegalArgumentException("Parameter -output is not set!");

    ArrayList<String> params = getCommonParameters();

    // deliclu algorithm
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(DeLiClu.class.getName());
    // k
    params.add(OptionHandler.OPTION_PREFIX + KNNJoin.K_P);
    params.add(minpts);
    // out
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    params.add(output + "_deli");

    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    return task.run();
  }

  /**
   * Runs the Optics algorithm.
   */
  public Result runOptics(String epsilon) {
    if (output == null)
      throw new IllegalArgumentException("Parameter -output is not set!");

    ArrayList<String> params = getCommonParameters();

    // algorithm
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(OPTICS.class.getName());
    // epsilon
    params.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    params.add(epsilon);
    // out
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    params.add(output + "_optics");


    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    return task.run();
  }

  /**
   * Runs the DeliClu algorithm.
   */
  private ArrayList<String> getCommonParameters() {
    ArrayList<String> params = new ArrayList<String>();
    for (String s : remainingParameters) {
      params.add(s);
    }

    // databse
    params.add(OptionHandler.OPTION_PREFIX + AbstractParser.DATABASE_CLASS_P);
    params.add(DeLiCluTreeDatabase.class.getName());
    // bulk load
    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.BULK_LOAD_F);
    // cache size
    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.CACHE_SIZE_P);
    params.add("12000");
    // cache size
    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.PAGE_SIZE_P);
    params.add("400");
    // minpts
    params.add(OptionHandler.OPTION_PREFIX + DeLiClu.MINPTS_P);
    params.add(minpts);
    // normalization
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    params.add(AttributeWiseDoubleVectorNormalization.class.getName());
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);
    // db connection
    params.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    params.add(input);

    if (time) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    return params;
  }

  /**
   * Runs the ACEP algorithm accordingly to the specified parameters.
   *
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    DeliCluWrapper wrapper = new DeliCluWrapper();
    try {
      wrapper.setParameters(args);
      ClusterOrder co_del = (ClusterOrder) wrapper.runDeliClu();

      Distance maxReach = co_del.getMaxReachability();
      System.out.println(maxReach);
      ClusterOrder co_opt = (ClusterOrder) wrapper.runOptics("1000");

      System.out.println(co_del.equals(co_opt));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }


}
