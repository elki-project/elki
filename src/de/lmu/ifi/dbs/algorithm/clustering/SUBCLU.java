package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;
import java.util.ArrayList;

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
public class SUBCLU<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D> implements Clustering<O> {

  /**
   * Epsilon.
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
    PatternParameter eps = new PatternParameter(DBSCAN.EPSILON_P, DBSCAN.EPSILON_D);
    optionHandler.put(eps);
    // global constraint
    try {
      // noinspection unchecked
      GlobalParameterConstraint gpc = new GlobalDistanceFunctionPatternConstraint(eps, (ClassParameter<? extends DistanceFunction<?, ?>>) optionHandler.getOption(DISTANCE_FUNCTION_P));
      optionHandler.setGlobalParameterConstraint(gpc);
    }
    catch (UnusedParameterException e) {
      verbose("Could not instantiate global parameter constraint concerning parameter " +
          DBSCAN.EPSILON_P + " and " + DISTANCE_FUNCTION_P +
          " because parameter " + DISTANCE_FUNCTION_P + " is not specified! " + e.getMessage());
    }

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
  protected void runInTime(Database<O> database) throws IllegalStateException {
    try {
      initDBSCAN();
      
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
  public ClusteringResult<O> getResult() {
    // todo
    return null;
  }

  /**
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   *         todo procedings
   */
  public Description getDescription() {
    return new Description("SUBCLU",
        "Density connected Subspace Clustering",
        "Algorithm to detect arbitrarily shaped and positioned clusters " +
            "in subspaces. SUBCLU delivers for each subspace the same clusters " +
            "DBSCAN would have found, when applied to this subspace seperately.. ",
        "K. Kailing, H.-P. Kriegel, P. Kroeger: " +
            "Density connected Subspace Clustering for High Dimensional Data. " +
            "In Proc. SDM Conference, Seattle, WA, 2004.");
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

    epsilon = (String) optionHandler.getOptionValue(DBSCAN.EPSILON_P);

    // minpts
    minpts = (Integer) optionHandler.getOptionValue(DBSCAN.MINPTS_P);

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Initializes the DBSCAN algorithm
   * @return an instance of the DBSCAN algorithm
   * @throws ParameterException in case of wrong parameter-setting
   */
  private DBSCAN<O,D> initDBSCAN() throws ParameterException {
    DBSCAN<O,D> dbscan = new DBSCAN<O,D>();
    List<String> parameters = new ArrayList<String>();

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + DISTANCE_FUNCTION_P);
    parameters.add(getDistanceFunction().getClass().toString());

    // distance function paramaters
    String[] distanceFunctionParams = getDistanceFunction().getParameters();
    for (String param: distanceFunctionParams) {
      parameters.add(param);
    }

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.EPSILON_P);
    parameters.add(epsilon);

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
    parameters.add(Integer.toString(minpts));

    dbscan.setParameters(parameters.toArray(new String[parameters.size()]));
    return dbscan;
  }
}
