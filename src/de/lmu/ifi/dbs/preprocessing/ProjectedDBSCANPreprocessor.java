package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.algorithm.clustering.ProjectedDBSCAN;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract superclass for preprocessor of algorithms extending
 * the ProjectedDBSCAN alghorithm.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class ProjectedDBSCANPreprocessor extends AbstractParameterizable implements Preprocessor {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = ProjectedDBSCAN.EPSILON_P;

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = ProjectedDBSCAN.EPSILON_D;

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = ProjectedDBSCAN.MINPTS_P;

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = ProjectedDBSCAN.MINPTS_D;

  /**
   * Contains the value of parameter epsilon;
   */
  private String epsilon;

  /**
   * Contains the value of parameter minpts;
   */
  private int minpts;

  /**
   * The distance function for the variance analysis.
   */
  protected EuklideanDistanceFunction<RealVector> rangeQueryDistanceFunction = new EuklideanDistanceFunction<RealVector>();

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database.
   */
  protected ProjectedDBSCANPreprocessor() {
    super();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
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
      List<QueryResult<DoubleDistance>> neighbors = database.rangeQuery(id, epsilon, rangeQueryDistanceFunction);

      if (neighbors.size() >= minpts) {
        List<Integer> ids = new ArrayList<Integer>(neighbors.size());
        for (QueryResult<DoubleDistance> qr : neighbors) {
          ids.add(qr.getID());
        }
        runVarianceAnalysis(id, ids, database);
      }


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
    String[] remainingParameters = super.setParameters(args);

    // epsilon
    epsilon = optionHandler.getOptionValue(EPSILON_P);
    try {
      rangeQueryDistanceFunction.valueOf(epsilon);
    }
    catch (IllegalArgumentException e) {
      throw new WrongParameterValueException(EPSILON_P, epsilon, EPSILON_D, e);
    }

    // minpts
    String minptsString = optionHandler.getOptionValue(MINPTS_P);
    try {
      minpts = Integer.parseInt(minptsString);
      if (minpts <= 0) {
        throw new WrongParameterValueException(MINPTS_P, minptsString, MINPTS_D);
      }
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(MINPTS_P, minptsString, MINPTS_D, e);
    }

    remainingParameters = rangeQueryDistanceFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(EPSILON_P, epsilon);
    mySettings.addSetting(MINPTS_P, Integer.toString(minpts));

    return attributeSettings;
  }

}
