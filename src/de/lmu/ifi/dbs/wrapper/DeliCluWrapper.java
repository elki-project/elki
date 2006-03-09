package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.KNNJoin;
import de.lmu.ifi.dbs.algorithm.clustering.DeLiClu;
import de.lmu.ifi.dbs.database.DeLiCluTreeDatabase;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterFormatException;

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
  public String[] setParameters(String[] args) {
    super.setParameters(args);
    try {
      minpts = optionHandler.getOptionValue(MINPTS_P);
    }
    catch (NumberFormatException e) {
      ParameterFormatException pfe = new ParameterFormatException(MINPTS_P, optionHandler.getOptionValue(MINPTS_P));
      pfe.fillInStackTrace();
      throw pfe;
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

    return new String[0];
  }

  /**
   * Initializes the parameters.
   */
  public void initParameters() {
    ArrayList<String> params = getRemainingParameters();

     // deliclu algorithm
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(DeLiClu.class.getName());

    // minpts
    params.add(OptionHandler.OPTION_PREFIX + KNNJoin.K_P);
    params.add(minpts);

    // database
    params.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.DATABASE_CLASS_P);
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
    params.add(AttributeWiseRealVectorNormalization.class.getName());
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // db connection
    params.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    params.add(input);

    // out
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    params.add(output);

    if (time) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    task.run();
  }

  public void run(String[] args) throws UnusedParameterException, NoParameterValueException {
    this.setParameters(args);
    initParameters();
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
    catch (ParameterFormatException e) {
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
