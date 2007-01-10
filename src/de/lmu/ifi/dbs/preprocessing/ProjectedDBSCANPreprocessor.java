package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.algorithm.clustering.ProjectedDBSCAN;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.logging.LogLevel;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract superclass for preprocessor of algorithms extending
 * the ProjectedDBSCAN alghorithm.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class ProjectedDBSCANPreprocessor<D extends Distance<D>> extends AbstractParameterizable implements Preprocessor<RealVector> {

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
   * The default range query distance function.
   */
  public static final String DEFAULT_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

  /**
   * Parameter for range query distance function.
   */
  public static final String DISTANCE_FUNCTION_P = "distancefunction";

  /**
   * Description for parameter range query distance function.
   */
  public static final String DISTANCE_FUNCTION_D = "the distance function to determine the neighbors for variance analysis "
                                                   + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class)
                                                   + ". Default: "
                                                   + DEFAULT_DISTANCE_FUNCTION;

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
  protected DistanceFunction<RealVector, D> rangeQueryDistanceFunction;

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database.
   */
  protected ProjectedDBSCANPreprocessor() {
    super();
    //parameter epsilon
    PatternParameter eps_param = new PatternParameter(EPSILON_P, EPSILON_D);
    optionHandler.put(EPSILON_P, eps_param);
    
    //parameter minpts
    optionHandler.put(MINPTS_P, new IntParameter(MINPTS_P, MINPTS_D, new GreaterConstraint(0)));
    
    // parameter range query distance function
    ClassParameter distance = new ClassParameter(DISTANCE_FUNCTION_P, DISTANCE_FUNCTION_D, DistanceFunction.class);
    distance.setDefaultValue(DEFAULT_DISTANCE_FUNCTION);
    optionHandler.put(DISTANCE_FUNCTION_P, distance);
    
    GlobalParameterConstraint gpc = new DistanceFunctionGlobalPatternConstraint(eps_param, distance);
    optionHandler.setGlobalParameterConstraint(gpc);
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
      verbose("Preprocessing:");
    }
    Iterator<Integer> it = database.iterator();
    int processed = 1;
    while (it.hasNext()) {
      Integer id = it.next();
      List<QueryResult<D>> neighbors = database.rangeQuery(id, epsilon, rangeQueryDistanceFunction);

      if (neighbors.size() >= minpts) {
        runVarianceAnalysis(id, neighbors, database);
      }
      else {
        QueryResult<D> firstQR = neighbors.get(0);
        neighbors = new ArrayList<QueryResult<D>>();
        neighbors.add(firstQR);
        runVarianceAnalysis(id, neighbors, database);
      }

      progress.setProcessed(processed++);
      if (verbose) {
        progress(new ProgressLogRecord(LogLevel.PROGRESS, Util.status(progress), progress.getTask(), progress.status()));
      }
    }
    if (verbose) {
      verbose("");
    }

    long end = System.currentTimeMillis();
    if (time) {
      long elapsedTime = end - start;
      verbose(this.getClass().getName() + " runtime: "
              + elapsedTime + " milliseconds.");
    }
  }

  /**
   * This method implements the type of variance analysis to be computed for a given point.
   * <p/>
   * Example1: for 4C, this method should implement a PCA for the given point.
   * Example2: for PreDeCon, this method should implement a simple axis-parallel variance analysis.
   *
   * @param id        the given point
   * @param neighbors the neighbors as query results of the given point
   * @param database  the database for which the preprocessing is performed
   */
  protected abstract void runVarianceAnalysis(Integer id, List<QueryResult<D>> neighbors, Database<RealVector> database);

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // range query distance function
    String className = (String)optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
    try {
      // noinspection unchecked
      rangeQueryDistanceFunction = Util.instantiate(DistanceFunction.class, className);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(DISTANCE_FUNCTION_P, className, DISTANCE_FUNCTION_D, e);
    }

    remainingParameters = rangeQueryDistanceFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);

    // epsilon
    epsilon = (String)optionHandler.getOptionValue(EPSILON_P);

    // minpts
    minpts = (Integer)optionHandler.getOptionValue(MINPTS_P);

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
    mySettings.addSetting(DISTANCE_FUNCTION_P, rangeQueryDistanceFunction.getClass().getName());

    attributeSettings.addAll(rangeQueryDistanceFunction.getAttributeSettings());

    return attributeSettings;
  }

}