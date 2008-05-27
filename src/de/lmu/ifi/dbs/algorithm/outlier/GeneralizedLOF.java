package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.outlier.GeneralizedLOFResult;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.*;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.Iterator;
import java.util.List;

/**
 * Algorithm to compute density-based local outlier factors in a database based
 * on a specified parameter minpts.
 *
 * @author Peer Kr&ouml;ger
 */
public class GeneralizedLOF<O extends DatabaseObject> extends DistanceBasedAlgorithm<O, DoubleDistance> {

  /**
   * The default reachability distance function.
   */
  public static final String DEFAULT_REACHABILITY_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

  /**
   * Parameter for distance function to determine reachability distance.
   */
  public static final String REACHABILITY_DISTANCE_FUNCTION_P = "reachdistfunction";

  /**
   * Description for parameter reachability distance function.
   */
  public static final String REACHABILITY_DISTANCE_FUNCTION_D = "The distance function to determine the reachability distance between database objects " + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class) + ". Default: " + DEFAULT_REACHABILITY_DISTANCE_FUNCTION;

  /**
   * The reachability distance function.
   */
  private DistanceFunction<O, DoubleDistance> reachabilityDistanceFunction;

  /**
   * Parameter k nearest neighbors.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter  k nearest neighbors.
   */
  public static final String K_D = "positive number of nearest neighbors of an object to be considered for computing its LOF.";

  /**
   * Number of neighbors to be considered.
   */
  int k;

  /**
   * Provides the result of the algorithm.
   */
  GeneralizedLOFResult<O> result;

  /**
   * Sets minimum points to the optionhandler additionally to the parameters
   * provided by super-classes.
   */
  public GeneralizedLOF() {
    super();
    ClassParameter<DistanceFunction<O, DoubleDistance>> reachabilityDistance = new ClassParameter(REACHABILITY_DISTANCE_FUNCTION_P, REACHABILITY_DISTANCE_FUNCTION_D, DistanceFunction.class);
    reachabilityDistance.setDefaultValue(DEFAULT_REACHABILITY_DISTANCE_FUNCTION);
    optionHandler.put(reachabilityDistance);
    //parameter k
    optionHandler.put(new IntParameter(K_P, K_D, new GreaterConstraint(0)));
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(Database)
   */
  protected void runInTime(Database<O> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());
    reachabilityDistanceFunction.setDatabase(database, isVerbose(), isTime());
    if (isVerbose()) {
      verbose("\nLOF ");
    }

    {// compute neighbors of each db object
      if (isVerbose()) {
        verbose("\ncomputing neighborhoods");
      }
      Progress progressNeighborhoods = new Progress("LOF", database.size());
      int counter = 1;
      for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
        Integer id = iter.next();
        List<QueryResult<DoubleDistance>> neighbors = database.kNNQueryForID(id, k + 1, getDistanceFunction());
        neighbors.remove(0);
        database.associate(AssociationID.NEIGHBORS, id, neighbors);
        if (isVerbose()) {
          progressNeighborhoods.setProcessed(counter);
          progress(progressNeighborhoods);
        }
      }
      if (isVerbose()) {
        verbose("");
      }
    }

    {// computing reach dist function neighborhoods
      if (isVerbose()) {
        verbose("\ncomputing neighborhods for reachability function");
      }
      Progress reachDistNeighborhoodsProgress = new Progress("Reachability DIstance Neighborhoods", database.size());
      int counter = 1;
      for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
        Integer id = iter.next();
        List<QueryResult<DoubleDistance>> neighbors = database.kNNQueryForID(id, k + 1, reachabilityDistanceFunction);
        neighbors.remove(0);
        database.associate(AssociationID.NEIGHBORS_2, id, neighbors);
        if (isVerbose()) {
          reachDistNeighborhoodsProgress.setProcessed(counter);
          progress(reachDistNeighborhoodsProgress);
        }
      }
      if (isVerbose()) {
        verbose("");
      }
    }
    {// computing LRDs
      if (isVerbose()) {
        verbose("\ncomputing LRDs");
      }
      Progress lrdsProgress = new Progress("LRD", database.size());
      int counter = 1;
      for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
        Integer id = iter.next();
        double sum = 0;
        List<QueryResult<DoubleDistance>> neighbors = (List<QueryResult<DoubleDistance>>) database.getAssociation(AssociationID.NEIGHBORS_2, id);
        for (Iterator<QueryResult<DoubleDistance>> neighbor = neighbors.iterator(); neighbor.hasNext();) {
          QueryResult<DoubleDistance> neighborQueryResult = neighbor.next();
          List<QueryResult<DoubleDistance>> neighborsNeighbors = (List<QueryResult<DoubleDistance>>) database.getAssociation(AssociationID.NEIGHBORS_2, neighborQueryResult.getID());
          sum += Math.max(neighborQueryResult.getDistance().getDoubleValue(),
                          neighborsNeighbors.get(neighborsNeighbors.size() - 1).getDistance().getDoubleValue());
        }
        Double lrd = neighbors.size() / sum;
        database.associate(AssociationID.LRD, id, lrd);
        if (isVerbose()) {
          lrdsProgress.setProcessed(counter);
          progress(lrdsProgress);
        }
      }
      if (isVerbose()) {
        verbose("");
      }
    }
    // XXX: everything here appears to be stupid
    {// compute LOF of each db object
      if (isVerbose()) {
        verbose("\ncomputing LOFs");
      }

      Progress progressLOFs = new Progress("LOF for objects", database.size());
      int counter = 1;
      for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
        Integer id = iter.next();
        //computeLOF(database, id);
        Double lrd = (Double) database.getAssociation(AssociationID.LRD, id);
        List<QueryResult<DoubleDistance>> neighbors = (List<QueryResult<DoubleDistance>>) database.getAssociation(AssociationID.NEIGHBORS, id);
        double sum = 0;
        for (Iterator<QueryResult<DoubleDistance>> neighbor = neighbors.iterator(); neighbor.hasNext();) {
          sum += (Double) database.getAssociation(AssociationID.LRD, neighbor.next().getID()) / lrd;
        }
        Double lof = neighbors.size() / sum;
        database.associate(AssociationID.LOF, id, lof);
        if (isVerbose()) {
          progressLOFs.setProcessed(counter);
          progress(progressLOFs);
        }
      }
      if (isVerbose()) {
        verbose("");
      }

      result = new GeneralizedLOFResult<O>(database);
    }
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("GeneralizedLOF", "Generalized Local Outlier Factor", "Algorithm to compute density-based local outlier factors in a database based on the parameter " + K_P + " and different distance functions", "unpublished");
  }

  /**
   * Sets the parameters minpts additionally to the parameters set by the
   * super-class method.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // minpts
    k = (Integer) optionHandler.getOptionValue(K_P);
    // reachabilityDistanceFunction
    String reachabilityDistanceFunctionName = (String) optionHandler.getOptionValue(REACHABILITY_DISTANCE_FUNCTION_P);
    try {
      reachabilityDistanceFunction = Util.instantiate(DistanceFunction.class, reachabilityDistanceFunctionName);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(REACHABILITY_DISTANCE_FUNCTION_P, reachabilityDistanceFunctionName, REACHABILITY_DISTANCE_FUNCTION_D, e);
    }

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
   */
  public Result<O> getResult() {
    return result;
  }
}
