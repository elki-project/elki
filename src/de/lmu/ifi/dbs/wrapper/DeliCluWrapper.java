package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.KNNJoin;
import de.lmu.ifi.dbs.algorithm.clustering.DeLiClu;
import de.lmu.ifi.dbs.database.DeLiCluTreeDatabase;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * Wrapper class for the DeliClu algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeliCluWrapper extends AbstractAlgorithmWrapper {
  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = DeLiClu.MINPTS_P;

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = DeLiClu.MINPTS_D;

  /**
   * Parameter pagesize.
   */
  public static final String PAGE_SIZE_P = DeLiCluTreeDatabase.PAGE_SIZE_P;

  /**
   * Description for parameter pagesize.
   */
  public static final String PAGE_SIZE_D = DeLiCluTreeDatabase.PAGE_SIZE_D;

  /**
   * The default pagesize.
   */
  public static final String DEFAULT_PAGE_SIZE = Integer.toString(DeLiCluTreeDatabase.DEFAULT_PAGE_SIZE);

  /**
   * Parameter pagesize.
   */
  public static final String CACHE_SIZE_P = DeLiCluTreeDatabase.CACHE_SIZE_P;

  /**
   * Description for parameter pagesize.
   */
  public static final String CACHE_SIZE_D = DeLiCluTreeDatabase.CACHE_SIZE_D;

  /**
   * The default cachesize.
   */
  public static final String DEFAULT_CACHE_SIZE = Integer.toString(DeLiCluTreeDatabase.DEFAULT_CACHE_SIZE);

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
    parameterToDescription.put(PAGE_SIZE_P + OptionHandler.EXPECTS_VALUE, PAGE_SIZE_D);
    parameterToDescription.put(CACHE_SIZE_P + OptionHandler.EXPECTS_VALUE, CACHE_SIZE_D);
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

    // minpts
    minpts = optionHandler.getOptionValue(MINPTS_P);

    // pagesize
    if (optionHandler.isSet(PAGE_SIZE_P)) {
      pagesize = optionHandler.getOptionValue(PAGE_SIZE_P);
    }
    else {
      pagesize = DEFAULT_PAGE_SIZE;
    }

    // cachesize
    if (optionHandler.isSet(CACHE_SIZE_P)) {
      cachesize = optionHandler.getOptionValue(CACHE_SIZE_P);
    }
    else {
      cachesize = DEFAULT_CACHE_SIZE;
    }

    return remainingParameters;
  }

  /**
   * @see AbstractAlgorithmWrapper#addParameters(java.util.List<java.lang.String>)
   */
  public void addParameters(List<String> parameters) {
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
