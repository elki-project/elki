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
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class forthe DeliClu algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeliCluWrapper extends AbstractAlgorithmWrapper {
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
    String[] remainingParameters = super.setParameters(args);
    try {
      minpts = optionHandler.getOptionValue(MINPTS_P);
    }
    catch (NumberFormatException e) {
      WrongParameterValueException pfe = new WrongParameterValueException(MINPTS_P, optionHandler.getOptionValue(MINPTS_P));
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

    return remainingParameters;
  }

  /**
   * @see AbstractAlgorithmWrapper#initParameters(java.util.List<java.lang.String>)
   */
  public List<String> initParameters(List<String> remainingParameters) {
    ArrayList<String> parameters = new ArrayList<String>(remainingParameters);

     // deliclu algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(DeLiClu.class.getName());

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + KNNJoin.K_P);
    parameters.add(minpts);

    // database
    parameters.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.DATABASE_CLASS_P);
    parameters.add(DeLiCluTreeDatabase.class.getName());

    // bulk load
    parameters.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.BULK_LOAD_F);

    // page size
    parameters.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.PAGE_SIZE_P);
    parameters.add(pagesize);

    // cache size
    parameters.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.CACHE_SIZE_P);
    parameters.add(cachesize);

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + DeLiClu.MINPTS_P);
    parameters.add(minpts);

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // db connection
    parameters.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    parameters.add(input);

    // out
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    parameters.add(output);

    if (time) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    return parameters;
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
