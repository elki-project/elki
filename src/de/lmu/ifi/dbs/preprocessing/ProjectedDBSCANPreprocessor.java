package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract superclass for preprocessor of algorithms extending
 * the ProjectedDBSCAN alghorithm.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class ProjectedDBSCANPreprocessor implements Preprocessor {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "preprocessorEpsilon";

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the Euklidean distance function";

  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  protected Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * OptionHandler for handling options.
   */
  protected OptionHandler optionHandler;

  /**
   * The distance function for the variance analysis.
   */
  protected EuklideanDistanceFunction<RealVector> rangeQueryDistanceFunction = new EuklideanDistanceFunction<RealVector>();

  /**
   * Holds the currently set parameter array.
   */
  private String[] currentParameterArray = new String[0];

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database.
   */
  protected ProjectedDBSCANPreprocessor() {
    parameterToDescription = new Hashtable<String, String>();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);

    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see Preprocessor#run(de.lmu.ifi.dbs.database.Database, boolean, boolean)
   */
  public void run(Database<RealVector> database, boolean verbose, boolean time) {
    if (database == null) {
      throw new IllegalArgumentException("Database must not be null!");
    }

    long start = System.currentTimeMillis();
    rangeQueryDistanceFunction.setDatabase(database, verbose, time);

    Progress progress = new Progress(this.getClass().getName(), database.size());
    if (verbose) {
      logger.info("Preprocessing:\n");
    }
    Iterator<Integer> it = database.iterator();
    int processed = 1;
    while (it.hasNext()) {
      Integer id = it.next();
      List<QueryResult<DoubleDistance>> qrs = database.rangeQuery(id, epsilon, rangeQueryDistanceFunction);

      List<Integer> ids = new ArrayList<Integer>(qrs.size());
      for (QueryResult<DoubleDistance> qr : qrs) {
        ids.add(qr.getID());
      }

      runVarianceAnalysis(id, ids, database);

      progress.setProcessed(processed++);
      if (verbose) {
        logger.log(new ProgressLogRecord(Level.INFO, Util.status(progress), progress.getTask(), progress.status()));

      }
    }
    if (verbose) {
      logger.info("\n");
    }

    long end = System.currentTimeMillis();
    if (time) {
      long elapsedTime = end - start;
      logger.info(this.getClass().getName() + " runtime: "
                  + elapsedTime + " milliseconds.\n");
    }
  }

  /**
   * This method implements the type of variance analysis to be computed for a given point.
   * <p/>
   * Example1: for 4C, this method should implement a PCA for the given point.
   * Example2: for PreDeCon, this method should implement a simple axis-parallel variance analysis.
   *
   * @param id       the given point
   * @param ids      neighbors of the given point
   * @param database the database for which the preprocessing is performed
   */
  protected abstract void runVarianceAnalysis(Integer id, List<Integer> ids, Database<RealVector> database);

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    // epsilon
    epsilon = optionHandler.getOptionValue(EPSILON_P);
    try {
      rangeQueryDistanceFunction.valueOf(epsilon);
    }
    catch (IllegalArgumentException e) {
      throw new WrongParameterValueException(EPSILON_P, epsilon, EPSILON_D, e);
    }

    remainingParameters = rangeQueryDistanceFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);

    return remainingParameters;
  }

  /**
   * Sets the difference of the first array minus the second array as the
   * currently set parameter array.
   *
   * @param complete the complete array
   * @param part     an array that contains only elements of the first array
   */
  protected void setParameters(String[] complete, String[] part) {
    currentParameterArray = Util.difference(complete, part);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
   */
  public String[] getParameters() {
    String[] param = new String[currentParameterArray.length];
    System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
    return param;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

    AttributeSettings mySettings = new AttributeSettings(this);
    mySettings.addSetting(EPSILON_P, epsilon);
    attributeSettings.add(mySettings);

    return attributeSettings;
  }

}
