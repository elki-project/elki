package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PreprocessorBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.preprocessing.ProjectedDBSCANPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Provides an abstract algorithm requiring a VarianceAnalysisPreprocessor.
 * 
 * @author Arthur Zimek
 * @param <V> the type of NumberVector handled by this Algorithm
 */
public abstract class AbstractProjectedDBSCAN<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>, V> {
  /**
   * OptionID for {@link #OUTER_DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID OUTER_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("projdbscan.outerdistancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Parameter to specify the distance function to determine the distance
   * between database objects, must extend
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction}
   * .
   * <p>
   * Key: {@code -projdbscan.distancefunction}
   * </p>
   * <p>
   * Default value:
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction}
   * </p>
   */
  protected final ObjectParameter<LocallyWeightedDistanceFunction<V, ?, ?>> OUTER_DISTANCE_FUNCTION_PARAM = new ObjectParameter<LocallyWeightedDistanceFunction<V, ?, ?>>(OUTER_DISTANCE_FUNCTION_ID, LocallyWeightedDistanceFunction.class, LocallyWeightedDistanceFunction.class);

  /**
   * OptionID for {@link #INNER_DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID INNER_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("projdbscan.distancefunction", "Distance function to determine the neighbors for variance analysis.");

  /**
   * Parameter distance function
   */
  private final ObjectParameter<DistanceFunction<V, DoubleDistance>> INNER_DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<V, DoubleDistance>>(AbstractProjectedDBSCAN.INNER_DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

  /**
   * Holds the instance of the distance function specified by
   * {@link #INNER_DISTANCE_FUNCTION_PARAM}.
   */
  private LocallyWeightedDistanceFunction<V, ?, ?> distanceFunction;
  
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
  private final DistanceParameter<DoubleDistance> EPSILON_PARAM;

  /**
   * Holds the value of {@link #EPSILON_PARAM}.
   */
  protected DoubleDistance epsilon;

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
  protected int minpts = 1;

  /**
   * Holds a list of clusters found.
   */
  private List<ModifiableDBIDs> resultList;

  /**
   * Holds a set of noise.
   */
  private ModifiableDBIDs noise;

  /**
   * Holds a set of processed ids.
   */
  private ModifiableDBIDs processedIDs;

  /**
   * The inner distance function.
   */
  private DistanceFunction<V, DoubleDistance> innerDistanceFunction;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AbstractProjectedDBSCAN(Parameterization config) {
    super();
    config = config.descend(this);

    if(config.grab(INNER_DISTANCE_FUNCTION_PARAM)) {
      innerDistanceFunction = INNER_DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }

    EPSILON_PARAM = new DistanceParameter<DoubleDistance>(EPSILON_ID, innerDistanceFunction != null ? innerDistanceFunction.getDistanceFactory() : null);
    if(config.grab(EPSILON_PARAM)) {
      epsilon = EPSILON_PARAM.getValue();
    }

    if(config.grab(MINPTS_PARAM)) {
      minpts = MINPTS_PARAM.getValue();
    }

    if(config.grab(OUTER_DISTANCE_FUNCTION_PARAM)) {
      // parameters for the distance function
      ListParameterization distanceFunctionParameters = new ListParameterization();
      //distanceFunctionParameters.addFlag(PreprocessorHandler.OMIT_PREPROCESSING_ID);
      distanceFunctionParameters.addParameter(PreprocessorBasedDistanceFunction.PREPROCESSOR_ID, preprocessorClass());
      distanceFunctionParameters.addParameter(AbstractProjectedDBSCAN.INNER_DISTANCE_FUNCTION_ID, innerDistanceFunction);
      distanceFunctionParameters.addParameter(AbstractProjectedDBSCAN.EPSILON_ID, epsilon);
      distanceFunctionParameters.addParameter(AbstractProjectedDBSCAN.MINPTS_ID, minpts);
      final ChainedParameterization combinedConfig = new ChainedParameterization(distanceFunctionParameters, config);
      combinedConfig.errorsTo(config);
      distanceFunction = OUTER_DISTANCE_FUNCTION_PARAM.instantiateClass(combinedConfig);
    }

    if(config.grab(LAMBDA_PARAM)) {
      lambda = LAMBDA_PARAM.getValue();
    }
  }

  @Override
  protected Clustering<Model> runInTime(Database<V> database) throws IllegalStateException {
    FiniteProgress objprog = getLogger().isVerbose() ? new FiniteProgress("Processing objects", database.size(), getLogger()) : null;
    IndefiniteProgress clusprog = getLogger().isVerbose() ? new IndefiniteProgress("Number of clusters", getLogger()) : null;
    resultList = new ArrayList<ModifiableDBIDs>();
    noise = DBIDUtil.newHashSet();
    processedIDs = DBIDUtil.newHashSet(database.size());
    
    LocallyWeightedDistanceFunction.Instance<V, ?> distFunc = distanceFunction.instantiate(database);
    RangeQuery<V, DoubleDistance> rangeQuery = database.getRangeQuery(distanceFunction);

    if(database.size() >= minpts) {
      for(DBID id : database) {
        if(!processedIDs.contains(id)) {
          expandCluster(database, distFunc, rangeQuery, id, objprog, clusprog);
          if(processedIDs.size() == database.size() && noise.size() == 0) {
            break;
          }
        }
        if(objprog != null && clusprog != null) {
          objprog.setProcessed(processedIDs.size(), getLogger());
          clusprog.setProcessed(resultList.size(), getLogger());
        }
      }
    }
    else {
      for(DBID id : database) {
        noise.add(id);
        if(objprog != null && clusprog != null) {
          objprog.setProcessed(processedIDs.size(), getLogger());
          clusprog.setProcessed(resultList.size(), getLogger());
        }
      }
    }

    if(objprog != null && clusprog != null) {
      objprog.setProcessed(processedIDs.size(), getLogger());
      clusprog.setProcessed(resultList.size(), getLogger());
    }

    Clustering<Model> result = new Clustering<Model>(getLongResultName(), getShortResultName());
    for(Iterator<ModifiableDBIDs> resultListIter = resultList.iterator(); resultListIter.hasNext();) {
      Cluster<Model> c = new Cluster<Model>(resultListIter.next(), ClusterModel.CLUSTER);
      result.addCluster(c);
    }

    Cluster<Model> n = new Cluster<Model>(noise, true, ClusterModel.CLUSTER);
    result.addCluster(n);

    if(objprog != null && clusprog != null) {
      objprog.setProcessed(processedIDs.size(), getLogger());
      clusprog.setProcessed(resultList.size(), getLogger());
    }
    // Signal that the progress has completed.
    if (objprog != null && clusprog != null) {
      objprog.ensureCompleted(getLogger());
      clusprog.setCompleted(getLogger());
    }
    return result;
  }

  /**
   * Return the long result name.
   * 
   * @return Long name for result
   */
  public abstract String getLongResultName();

  /**
   * Return the short result name.
   * 
   * @return Short name for result
   */
  public abstract String getShortResultName();

  /**
   * ExpandCluster function of DBSCAN.
   * 
   * @param database the database to run the algorithm on
   * @param distFunc Distance query to use
   * @param rangeQuery Range query
   * @param startObjectID the object id of the database object to start the
   *        expansion with
   * @param objprog the progress object for logging the current status
   */
  protected void expandCluster(Database<V> database, LocallyWeightedDistanceFunction.Instance<V, ?> distFunc, RangeQuery<V, DoubleDistance> rangeQuery, DBID startObjectID, FiniteProgress objprog, IndefiniteProgress clusprog) {
    String label = database.getObjectLabel(startObjectID);
    Integer corrDim = distFunc.getPreprocessed(startObjectID).getCorrelationDimension();

    if(getLogger().isDebugging()) {
      getLogger().debugFine("EXPAND CLUSTER id = " + startObjectID + " " + label + " " + corrDim + "\n#clusters: " + resultList.size());
    }

    // euclidean epsilon neighborhood < minpts OR local dimensionality >
    // lambda -> noise
    if(corrDim == null || corrDim > lambda) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), getLogger());
        clusprog.setProcessed(resultList.size(), getLogger());
      }
      return;
    }

    // compute weighted epsilon neighborhood
    List<DistanceResultPair<DoubleDistance>> seeds = rangeQuery.getRangeForDBID(startObjectID, epsilon);
    // neighbors < minPts -> noise
    if(seeds.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), getLogger());
        clusprog.setProcessed(resultList.size(), getLogger());
      }
      return;
    }

    // try to expand the cluster
    ModifiableDBIDs currentCluster = DBIDUtil.newArray();
    for(DistanceResultPair<DoubleDistance> seed : seeds) {
      DBID nextID = seed.getID();

      Integer nextID_corrDim = distFunc.getPreprocessed(nextID).getCorrelationDimension();
      // nextID is not reachable from start object
      if(nextID_corrDim > lambda) {
        continue;
      }

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
      DBID q = seeds.remove(0).getID();
      Integer corrDim_q = distFunc.getPreprocessed(q).getCorrelationDimension();
      // q forms no lambda-dim hyperplane
      if(corrDim_q > lambda) {
        continue;
      }

      List<DistanceResultPair<DoubleDistance>> reachables = rangeQuery.getRangeForDBID(q, epsilon);
      if(reachables.size() > minpts) {
        for(DistanceResultPair<DoubleDistance> r : reachables) {
          Integer corrDim_r = distFunc.getPreprocessed(r.getID()).getCorrelationDimension();
          // r is not reachable from q
          if(corrDim_r > lambda) {
            continue;
          }

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
            if(objprog != null && clusprog != null) {
              objprog.setProcessed(processedIDs.size(), getLogger());
              int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
              clusprog.setProcessed(numClusters, getLogger());
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
      for(DBID id : currentCluster) {
        noise.add(id);
      }
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
    }

    if(objprog != null && clusprog != null) {
      objprog.setProcessed(processedIDs.size(), getLogger());
      clusprog.setProcessed(resultList.size(), getLogger());
    }
  }

  /**
   * Returns the class actually used as {@link ProjectedDBSCANPreprocessor
   * VarianceAnalysisPreprocessor}.
   * 
   * @return the class actually used as {@link ProjectedDBSCANPreprocessor
   *         VarianceAnalysisPreprocessor}
   */
  public abstract Class<?> preprocessorClass();
}