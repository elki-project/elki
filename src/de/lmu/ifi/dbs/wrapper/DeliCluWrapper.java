package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.*;
import de.lmu.ifi.dbs.algorithm.result.ClusterOrder;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.database.DeLiCluTreeDatabase;
import de.lmu.ifi.dbs.database.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.distance.AbstractDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseDoubleVectorNormalization;
import de.lmu.ifi.dbs.parser.AbstractParser;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;

/**
 * Wrapper class forthe DeliClu algorithm.
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
   * Parameter pagesize.
   */
  public static final String PAGESIZE_P = "pagesize";

  /**
   * Description for parameter pagesize.
   */
  public static final String PAGESIZE_D = "<double>pagesize";

  /**
   * The default pagesize.
   */
  public static final String DEFAULT_PAGE_SIZE = "4000";

  /**
   * Parameter pagesize.
   */
  public static final String CACHESIZE_P = "cachesize";

  /**
   * Description for parameter pagesize.
   */
  public static final String CACHESIZE_D = "<double>cachesize";

   /**
   * The default cachesize.
   */
  public static final String DEFAULT_CACHE_SIZE = "100000";

  /**
   * Minimum points.
   */
  protected String minpts;

  /**
   * pagesize.
   */
  protected String pagesize;

  /**
   * Cachesize.
   */
  protected String cachesize;

  /**
   * Sets minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since DeliCluWrapper is a non-abstract class,
   * finally optionHandler is initialized.
   */
  public DeliCluWrapper() {
    super();
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    parameterToDescription.put(PAGESIZE_P + OptionHandler.EXPECTS_VALUE, PAGESIZE_D);
    parameterToDescription.put(CACHESIZE_P + OptionHandler.EXPECTS_VALUE, CACHESIZE_D);
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
      minpts = optionHandler.getOptionValue(MINPTS_P);
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }

    if (optionHandler.isSet(PAGESIZE_P)) {
      pagesize = optionHandler.getOptionValue(PAGESIZE_P);
    }
    else {
      pagesize = DEFAULT_PAGE_SIZE;
    }

    if (optionHandler.isSet(CACHESIZE_P)) {
      cachesize = optionHandler.getOptionValue(CACHESIZE_P);
    }
    else {
      cachesize = DEFAULT_CACHE_SIZE;
    }

    System.out.println("page size = " + pagesize);
    System.out.println("cache size = " +cachesize);

    return new String[0];
  }

  /**
   * Runs the DeliClu algorithm.
   */
  public Result runDeliClu() {
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
  public Result runSLink() {
    if (output == null)
      throw new IllegalArgumentException("Parameter -output is not set!");

    ArrayList<String> params = new ArrayList<String>();

    // database
    params.add(OptionHandler.OPTION_PREFIX + AbstractParser.DATABASE_CLASS_P);
    params.add(SequentialDatabase.class.getName());

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

    // algorithm
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(SLINK.class.getName());
    // out
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    params.add(output + "_slink");


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
    ArrayList<String> params = getRemainingParameters();

    // database
    params.add(OptionHandler.OPTION_PREFIX + AbstractParser.DATABASE_CLASS_P);
    params.add(DeLiCluTreeDatabase.class.getName());

    // bulk load
    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.BULK_LOAD_F);

    // page size
    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.PAGE_SIZE_P);
    params.add(pagesize);

    // cache size
    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.CACHE_SIZE_P);
    params.add(cachesize);

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

  public void run(String[] args) {
    this.setParameters(args);

    ClusterOrder co_del = (ClusterOrder) this.runDeliClu();

    double maxReach = Double.parseDouble(co_del.getMaxReachability().toString()) ;
    System.out.println("maxReach " + maxReach);
    ClusterOrder co_opt = (ClusterOrder) this.runOptics("" + (maxReach + 0.01));
//    ClusterOrder co_opt = (ClusterOrder) this.runOptics("0.7");

//    ClusterOrder co_opt = (ClusterOrder) this.runOptics(AbstractDistanceFunction.INFINITY_PATTERN);
    System.out.println(co_del.equals(co_opt));
    this.runSLink();
  }

  /**
   * Runs the ACEP algorithm accordingly to the specified parameters.
   *
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    DeliCluWrapper wrapper = new DeliCluWrapper();
    try {
      wrapper.run(args);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }


}
