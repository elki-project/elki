package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.preprocessing.ProjectedDBSCANPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.progress.IndefiniteProgress;

/**
 * Provides an abstract algorithm requiring a VarianceAnalysisPreprocessor.
 * 
 * @author Arthur Zimek
 * @param <V> the type of Realvector handled by this Algorithm
 */
public abstract class ProjectedDBSCAN<V extends RealVector<V, ?>> extends AbstractAlgorithm<V, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>, V> {

  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("projdbscan.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Parameter to specify the distance function to determine the distance
   * between database objects, must extend
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractLocallyWeightedDistanceFunction}
   * .
   * <p>
   * Key: {@code -projdbscan.distancefunction}
   * </p>
   * <p>
   * Default value:
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction}
   * </p>
   */
  protected final ClassParameter<LocallyWeightedDistanceFunction<V, ?>> DISTANCE_FUNCTION_PARAM = new ClassParameter<LocallyWeightedDistanceFunction<V, ?>>(DISTANCE_FUNCTION_ID, LocallyWeightedDistanceFunction.class, LocallyWeightedDistanceFunction.class.getCanonicalName());

  /**
   * Holds the instance of the distance function specified by
   * {@link #DISTANCE_FUNCTION_PARAM}.
   */
  private LocallyWeightedDistanceFunction<V, ?> distanceFunction;

  /**
   * OptionID for {@link #EPSILON_PARAM}
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("projdbscan.epsilon", "The maximum radius of the neighborhood to be considered.");

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to {@link LocallyWeightedDistanceFunction}.
   * <p>
   * Key: {@code -projdbscan.epsilon}
   * </p>
   */
  private final PatternParameter EPSILON_PARAM = new PatternParameter(EPSILON_ID);

  /**
   * Holds the value of {@link #EPSILON_PARAM}.
   */
  protected String epsilon;

  /**
   * OptionID for {@link #LAMBDA_PARAM}
   */
  public static final OptionID LAMBDA_ID = OptionID.getOrCreateOptionID("projdbscan.lambda", "The intrinsic dimensionality of the clusters to find.");

  /**
   * Parameter to specify the intrinsic dimensionality of the clusters to find,
   * must be an integer greater than 0.
   * <p>
   * Key: {@code -projdbscan.lambda}
   * </p>
   */
  private final IntParameter LAMBDA_PARAM = new IntParameter(LAMBDA_ID, new GreaterConstraint(0));

  /**
   * Holds the value of {@link #LAMBDA_PARAM}.
   */
  private int lambda;

  /**
   * OptionID for {@link #MINPTS_PARAM}
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("projdbscan.minpts", "Threshold for minimum number of points in " + "the epsilon-neighborhood of a point.");

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   * <p>
   * Key: {@code -projdbscan.minpts}
   * </p>
   */
  private final IntParameter MINPTS_PARAM = new IntParameter(MINPTS_ID, new GreaterConstraint(0));

  /**
   * Holds the value of {@link #MINPTS_PARAM}.
   */
  protected int minpts;

  /**
   * Holds a list of clusters found.
   */
  private List<List<Integer>> resultList;

  /**
   * Provides the result of the algorithm.
   */
  private Clustering<Model> result;

  /**
   * Holds a set of noise.
   */
  private Set<Integer> noise;

  /**
   * Holds a set of processed ids.
   */
  private Set<Integer> processedIDs;

  /**
   * Provides the abstract algorithm for variance analysis based DBSCAN, adding
   * parameters {@link #EPSILON_PARAM}, {@link #MINPTS_PARAM},
   * {@link #LAMBDA_PARAM}, and {@link #DISTANCE_FUNCTION_PARAM} to the option
   * handler additionally to parameters of super class.
   */
  protected ProjectedDBSCAN() {
    super();

    // parameter epsilon
    addOption(EPSILON_PARAM);

    // minpts
    addOption(MINPTS_PARAM);

    // lambda
    addOption(LAMBDA_PARAM);

    // parameter distance function
    addOption(DISTANCE_FUNCTION_PARAM);

    // global parameter constraint epsilon <-> distance function
    GlobalParameterConstraint con = new GlobalDistanceFunctionPatternConstraint<LocallyWeightedDistanceFunction<V, ?>>(EPSILON_PARAM, DISTANCE_FUNCTION_PARAM);
    optionHandler.setGlobalParameterConstraint(con);
  }

  @Override
  protected Clustering<Model> runInTime(Database<V> database) throws IllegalStateException {
    try {
      FiniteProgress objprog = new FiniteProgress("Processing objects", database.size());
      IndefiniteProgress clusprog = new IndefiniteProgress("Number of clusters");
      resultList = new ArrayList<List<Integer>>();
      noise = new HashSet<Integer>();
      processedIDs = new HashSet<Integer>(database.size());
      distanceFunction.setDatabase(database, isVerbose(), isTime());
      if(logger.isVerbose()) {
        logger.verbose("Clustering:");
      }
      if(database.size() >= minpts) {
        for(Integer id : database) {
          if(!processedIDs.contains(id)) {
            expandCluster(database, id, objprog, clusprog);
            if(processedIDs.size() == database.size() && noise.size() == 0) {
              break;
            }
          }
          if(isVerbose()) {
            objprog.setProcessed(processedIDs.size());
            clusprog.setProcessed(resultList.size());
            logger.progress(objprog, clusprog);
          }
        }
      }
      else {
        for(Integer id : database) {
          noise.add(id);
          if(isVerbose()) {
            objprog.setProcessed(processedIDs.size());
            clusprog.setProcessed(resultList.size());
            logger.progress(objprog, clusprog);
          }
        }
      }

      if(isVerbose()) {
        objprog.setProcessed(processedIDs.size());
        clusprog.setProcessed(resultList.size());
        logger.progress(objprog, clusprog);
      }

      result = new Clustering<Model>();
      for(Iterator<List<Integer>> resultListIter = resultList.iterator(); resultListIter.hasNext();) {
        DatabaseObjectGroup group = new DatabaseObjectGroupCollection<List<Integer>>(resultListIter.next());
        Cluster<Model> c = new Cluster<Model>(group, ClusterModel.CLUSTER);
        result.addCluster(c);
      }

      DatabaseObjectGroup group = new DatabaseObjectGroupCollection<Set<Integer>>(noise);
      Cluster<Model> n = new Cluster<Model>(group, true, ClusterModel.CLUSTER);
      result.addCluster(n);

      if(isVerbose()) {
        objprog.setProcessed(processedIDs.size());
        clusprog.setProcessed(resultList.size());
        logger.progress(objprog, clusprog);
      }
      // Signal that the progress has completed.
      clusprog.setCompleted();
    }
    catch(Exception e) {
      throw new IllegalStateException(e);
    }
    return result;
  }

  /**
   * ExpandCluster function of DBSCAN.
   * 
   * @param database the database to run the algorithm on
   * @param startObjectID the object id of the database object to start the
   *        expansion with
   * @param objprog the progress object for logging the current status
   */
  protected void expandCluster(Database<V> database, Integer startObjectID, FiniteProgress objprog, IndefiniteProgress clusprog) {
    String label = database.getAssociation(AssociationID.LABEL, startObjectID);
    Integer corrDim = database.getAssociation(AssociationID.LOCAL_DIMENSIONALITY, startObjectID);

    if(logger.isDebugging()) {
      logger.debugFine("EXPAND CLUSTER id = " + startObjectID + " " + label + " " + corrDim + "\n#clusters: " + resultList.size());
    }

    // euclidean epsilon neighborhood < minpts OR local dimensionality >
    // lambda -> noise
    if(corrDim == null || corrDim > lambda) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if(logger.isVerbose()) {
        objprog.setProcessed(processedIDs.size());
        clusprog.setProcessed(resultList.size());
        logger.progress(objprog, clusprog);
      }
      return;
    }

    // compute weighted epsilon neighborhood
    List<DistanceResultPair<DoubleDistance>> seeds = database.rangeQuery(startObjectID, epsilon, distanceFunction);
    // neighbors < minPts -> noise
    if(seeds.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if(logger.isVerbose()) {
        objprog.setProcessed(processedIDs.size());
        clusprog.setProcessed(resultList.size());
        logger.progress(objprog, clusprog);
      }
      return;
    }

    // try to expand the cluster
    List<Integer> currentCluster = new ArrayList<Integer>();
    for(DistanceResultPair<DoubleDistance> seed : seeds) {
      Integer nextID = seed.getID();

      Integer nextID_corrDim = database.getAssociation(AssociationID.LOCAL_DIMENSIONALITY, nextID);
      // nextID is not reachable from start object
      if(nextID_corrDim > lambda)
        continue;

      if(!processedIDs.contains(nextID)) {
        currentCluster.add(nextID);
        processedIDs.add(nextID);
      }
      else if(noise.contains(nextID)) {
        currentCluster.add(nextID);
        noise.remove(nextID);
      }
    }
    seeds.remove(0);

    while(seeds.size() > 0) {
      Integer q = seeds.remove(0).getID();
      Integer corrDim_q = database.getAssociation(AssociationID.LOCAL_DIMENSIONALITY, q);
      // q forms no lambda-dim hyperplane
      if(corrDim_q > lambda)
        continue;

      List<DistanceResultPair<DoubleDistance>> reachables = database.rangeQuery(q, epsilon, distanceFunction);
      if(reachables.size() > minpts) {
        for(DistanceResultPair<DoubleDistance> r : reachables) {
          Integer corrDim_r = database.getAssociation(AssociationID.LOCAL_DIMENSIONALITY, r.getSecond());
          // r is not reachable from q
          if(corrDim_r > lambda)
            continue;

          boolean inNoise = noise.contains(r.getID());
          boolean unclassified = !processedIDs.contains(r.getID());
          if(inNoise || unclassified) {
            if(unclassified) {
              seeds.add(r);
            }
            currentCluster.add(r.getID());
            processedIDs.add(r.getID());
            if(inNoise) {
              noise.remove(r.getID());
            }
            if(logger.isVerbose()) {
              objprog.setProcessed(processedIDs.size());
              int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
              clusprog.setProcessed(numClusters);
              logger.progress(objprog, clusprog);
            }
          }
        }
      }

      if(processedIDs.size() == database.size() && noise.size() == 0) {
        break;
      }
    }

    if(currentCluster.size() >= minpts) {
      resultList.add(currentCluster);
    }
    else {
      for(Integer id : currentCluster) {
        noise.add(id);
      }
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
    }

    if(logger.isVerbose()) {
      objprog.setProcessed(processedIDs.size());
      clusprog.setProcessed(resultList.size());
      logger.progress(objprog, clusprog);
    }
  }

  /**
   * Calls the super method and instantiates {@link #distanceFunction} according
   * to the value of parameter {@link #DISTANCE_FUNCTION_PARAM} and sets
   * additionally the values of the parameters {@link #EPSILON_PARAM}
   * {@link #MINPTS_PARAM}, and {@link #LAMBDA_PARAM}.
   * <p/>
   * The remaining parameters are passed to the {@link #distanceFunction}.
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    // epsilon
    epsilon = EPSILON_PARAM.getValue();

    // minpts
    minpts = MINPTS_PARAM.getValue();

    // lambda
    lambda = LAMBDA_PARAM.getValue();

    // parameters for the distance function
    // todo provide setters
    ArrayList<String> distanceFunctionParameters = new ArrayList<String>();
    // omit preprocessing flag
    OptionUtil.addFlag(distanceFunctionParameters, PreprocessorHandler.OMIT_PREPROCESSING_ID);
    // preprocessor
    OptionUtil.addParameter(distanceFunctionParameters, PreprocessorHandler.PREPROCESSOR_ID, preprocessorClass().getName());
    // preprocessor epsilon
    OptionUtil.addParameter(distanceFunctionParameters, DBSCAN.EPSILON_ID, epsilon);
    // preprocessor minpts
    OptionUtil.addParameter(distanceFunctionParameters, MINPTS_ID, Integer.toString(minpts));
    // merge remaining parameters
    distanceFunctionParameters.addAll(remainingParameters);
    
    List<OptionID> overriddenParameters = new ArrayList<OptionID>();
    overriddenParameters.add(PreprocessorHandler.OMIT_PREPROCESSING_ID);
    overriddenParameters.add(PreprocessorHandler.PREPROCESSOR_ID);
    overriddenParameters.add(DBSCAN.EPSILON_ID);
    overriddenParameters.add(MINPTS_ID);

    // distance function
    distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();
    addParameterizable(distanceFunction, overriddenParameters);
    remainingParameters = distanceFunction.setParameters(distanceFunctionParameters);

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;

  }

  /**
   * Returns the class actually used as {@link ProjectedDBSCANPreprocessor
   * VarianceAnalysisPreprocessor}.
   * 
   * @return the class actually used as {@link ProjectedDBSCANPreprocessor
   *         VarianceAnalysisPreprocessor}
   */
  public abstract Class<?> preprocessorClass();

  public Clustering<Model> getResult() {
    return result;
  }
}