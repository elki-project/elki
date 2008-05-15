package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.AbstractDimensionsSelectingDoubleDistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.DimensionsSelectingEuklideanDistanceFunction;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Implementation of the SUBCLU algorithm, an algorithm to
 * detect arbitrarily shaped and positioned clusters
 * in subspaces.
 * SUBCLU delivers for each subspace the same clusters
 * DBSCAN would have found, when applied to this
 * subspace seperately.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class SUBCLU<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractAlgorithm<V> implements Clustering<V> {

  /**
   * The default distance function.
   */
  public static final String DEFAULT_DISTANCE_FUNCTION = DimensionsSelectingEuklideanDistanceFunction.class.getName();

  /**
   * Parameter for distance function.
   */
  public static final String DISTANCE_FUNCTION_P = "distancefunction";

  /**
   * Description for parameter distance function.
   */
  public static final String DISTANCE_FUNCTION_D = "the distance function to determine the distance between database objects "
                                                   + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(AbstractDimensionsSelectingDoubleDistanceFunction.class)
                                                   + ". Default: "
                                                   + DEFAULT_DISTANCE_FUNCTION;

  /**
   * Parameter to specify the maximum radius of the neighborhood to be considered,
   * must be suitable to AbstractDimensionsSelectingDoubleDistanceFunction.
   * <p>Key: (@code -epsilon) </p>
   */
  public static final PatternParameter EPSILON_PARAM = new PatternParameter("epsilon",
                                                                            "the maximum radius of the neighborhood " +
                                                                            "to be considered, must be suitable to " +
                                                                            AbstractDimensionsSelectingDoubleDistanceFunction.class.getName());


  /**
   * The distance function.
   */
  private AbstractDimensionsSelectingDoubleDistanceFunction<V> distanceFunction;


  /**
   * The value of parameter epsilon.
   */
  private String epsilon;

  /**
   * Minimum points.
   */
  private int minpts;

  /**
   * Sets epsilon and minimum points to the optionhandler additionally to the
   * parameters provided by super-classes.
   */
  public SUBCLU() {
    super();
    this.debug = true;

    // distance function
    // noinspection unchecked
    ClassParameter<AbstractDimensionsSelectingDoubleDistanceFunction<V>> distance = new ClassParameter(DISTANCE_FUNCTION_P, DISTANCE_FUNCTION_D, AbstractDimensionsSelectingDoubleDistanceFunction.class);
    distance.setDefaultValue(DEFAULT_DISTANCE_FUNCTION);
    optionHandler.put(distance);

    // epsilon
    optionHandler.put(EPSILON_PARAM);
    // global constraint
    try {
      // noinspection unchecked
      GlobalParameterConstraint gpc = new GlobalDistanceFunctionPatternConstraint(EPSILON_PARAM, (ClassParameter<? extends AbstractDimensionsSelectingDoubleDistanceFunction<?>>) optionHandler.getOption(DISTANCE_FUNCTION_P));
      optionHandler.setGlobalParameterConstraint(gpc);
    }
    catch (UnusedParameterException e) {
      verbose("Could not instantiate global parameter constraint concerning parameter " +
              EPSILON_PARAM.getName() + " and " + DISTANCE_FUNCTION_P +
              " because parameter " + DISTANCE_FUNCTION_P + " is not specified! " + e.getMessage());
    }

    // minpts
    optionHandler.put(new IntParameter(DBSCAN.MINPTS_P, DBSCAN.MINPTS_D, new GreaterConstraint(0)));
  }


  /**
   * Performs the SUBCLU algorithm on the given database.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized
   *                               properly (e.g. the setParameters(String[]) method has been failed
   *                               to be called).
   */
  protected void runInTime(Database<V> database) throws IllegalStateException {
    try {
      int dimensionality = database.dimensionality();
      // 1. Generate all 1-D clusters
      if (isVerbose()) {
        verbose("*** Step 1: Generate all 1-D clusters ***");
      }
      for (int d = 0; d < dimensionality; d++) {
        BitSet selectedDimensions = new BitSet();
        selectedDimensions.set(d);
        DBSCAN<V, D> dbscan = initDBSCAN(selectedDimensions);
        dbscan.run(database);
        ClustersPlusNoise clusters = dbscan.getResult();
        if (debug) {
          debugFine(d + " clusters: " + clusters);
        }
      }
    }
    catch (ParameterException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  public ClusteringResult<V> getResult() {
    // todo
    return null;
  }

  /**
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   */
  public Description getDescription() {
    return new Description("SUBCLU",
                           "Density connected Subspace Clustering",
                           "Algorithm to detect arbitrarily shaped and positioned clusters " +
                           "in subspaces. SUBCLU delivers for each subspace the same clusters " +
                           "DBSCAN would have found, when applied to this subspace seperately.. ",
                           "K. Kailing, H.-P. Kriegel, P. Kroeger: " +
                           "Density connected Subspace Clustering for High Dimensional Data. " +
                           "In Proc. SIAM Int. Conf. on Data Mining (SDM'04), Lake Buena Vista, FL, 2004.");
  }

  /**
   * Sets the parameters epsilon and minpts additionally to the parameters set
   * by the super-class' method. Both epsilon and minpts are required
   * parameters.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // distance function
    String className = (String) optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
    try {
      // noinspection unchecked
      distanceFunction = Util.instantiate(AbstractDimensionsSelectingDoubleDistanceFunction.class, className);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(DISTANCE_FUNCTION_P, className, DISTANCE_FUNCTION_D, e);
    }
    remainingParameters = distanceFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);

    // epsilon
    epsilon = optionHandler.getParameterValue(EPSILON_PARAM);
    // minpts
    minpts = (Integer) optionHandler.getOptionValue(DBSCAN.MINPTS_P);

    return remainingParameters;
  }

  /**
   * Initializes the DBSCAN algorithm
   *
   * @return an instance of the DBSCAN algorithm
   * @throws ParameterException in case of wrong parameter-setting
   */
  private DBSCAN<V, D> initDBSCAN(BitSet dimensions) throws ParameterException {
    DBSCAN<V, D> dbscan = new DBSCAN<V, D>();
    List<String> parameters = new ArrayList<String>();

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + DISTANCE_FUNCTION_P);
    parameters.add(distanceFunction.getClass().toString());

    // selected dimensions for distance function
    parameters.add(OptionHandler.OPTION_PREFIX + AbstractDimensionsSelectingDoubleDistanceFunction.DIMS_P);
    parameters.add(Util.parseSelectedBits(dimensions, ","));

    // aditional distance function paramaters
    String[] distanceFunctionParams = distanceFunction.getParameters();
    for (String param : distanceFunctionParams) {
      parameters.add(param);
    }

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + EPSILON_PARAM.getName());
    parameters.add(epsilon);

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
    parameters.add(Integer.toString(minpts));

    dbscan.setParameters(parameters.toArray(new String[parameters.size()]));
    return dbscan;
  }
}
