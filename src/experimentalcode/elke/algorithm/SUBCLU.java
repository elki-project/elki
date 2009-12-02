package experimentalcode.elke.algorithm;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.AxesModel;
import de.lmu.ifi.dbs.elki.data.model.DimensionModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.AbstractDimensionsSelectingDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DimensionsSelectingEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * <p>
 * Implementation of the SUBCLU algorithm, an algorithm to detect arbitrarily
 * shaped and positioned clusters in subspaces.
 * </p>
 * SUBCLU delivers for each subspace the same clusters DBSCAN would have found,
 * when applied to this subspace separately.
 * 
 * <p>
 * Reference: <br>
 * K. Kailing, H.-P. Kriegel, P. Kroeger: Density connected Subspace Clustering
 * for High Dimensional Data. <br>
 * In Proc. SIAM Int. Conf. on Data Mining (SDM'04), Lake Buena Vista, FL, 2004.
 * </p>
 * 
 * @author Elke Achtert
 * @param <V> the type of FeatureVector handled by this Algorithm
 * @param <D> the type of Distance used
 */
// todo elke implementation
/**
 * @author Elke Achtert
 * 
 * @param <V>
 * @param <D>
 */
public class SUBCLU<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractAlgorithm<V, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>, V> {

  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("subclu.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * The distance function to determine the distance between database objects.
   * <p>
   * Default value: {@link DimensionsSelectingEuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -subclu.distancefunction}
   * </p>
   */
  private final ClassParameter<AbstractDimensionsSelectingDoubleDistanceFunction<V>> DISTANCE_FUNCTION_PARAM = new ClassParameter<AbstractDimensionsSelectingDoubleDistanceFunction<V>>(DISTANCE_FUNCTION_ID, AbstractDimensionsSelectingDoubleDistanceFunction.class, DimensionsSelectingEuclideanDistanceFunction.class.getName());

  /**
   * Holds the instance of the distance function specified by
   * {@link #DISTANCE_FUNCTION_PARAM}.
   */
  private AbstractDimensionsSelectingDoubleDistanceFunction<V> distanceFunction;

  /**
   * OptionID for {@link #EPSILON_PARAM}
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("subclu.epsilon", "The maximum radius of the neighborhood to be considered.");

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to
   * {@link AbstractDimensionsSelectingDoubleDistanceFunction}.
   * <p>
   * Key: {@code -subclu.epsilon}
   * </p>
   */
  private final PatternParameter EPSILON_PARAM = new PatternParameter(EPSILON_ID);

  /**
   * Holds the value of {@link #EPSILON_PARAM}.
   */
  private String epsilon;

  /**
   * OptionID for {@link #MINPTS_PARAM}
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("subclu.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   * <p>
   * Key: {@code -subclu.minpts}
   * </p>
   */
  private final IntParameter MINPTS_PARAM = new IntParameter(MINPTS_ID, new GreaterConstraint(0));

  /**
   * Holds the value of {@link #MINPTS_PARAM}.
   */
  private int minpts;

  /**
   * Holds the result;
   */
  private Clustering<Model> result;

  /**
   * Provides the SUBCLU algorithm, adding parameters {@link #EPSILON_PARAM},
   * {@link #MINPTS_PARAM}, and {@link #DISTANCE_FUNCTION_PARAM} to the option
   * handler additionally to parameters of super class.
   */
  public SUBCLU() {
    super();
    logger.getWrappedLogger().setLevel(Level.INFO);

    // parameter epsilon
    addOption(EPSILON_PARAM);

    // parameter minpts
    addOption(MINPTS_PARAM);

    // distance function
    addOption(DISTANCE_FUNCTION_PARAM);

    // global constraint epsilon <-> distance function
    optionHandler.setGlobalParameterConstraint(new GlobalDistanceFunctionPatternConstraint<AbstractDimensionsSelectingDoubleDistanceFunction<V>>(EPSILON_PARAM, DISTANCE_FUNCTION_PARAM));
  }

  /**
   * Performs the SUBCLU algorithm on the given database.
   * 
   */
  @Override
  protected Clustering<Model> runInTime(Database<V> database) throws IllegalStateException {
    try {
      int dimensionality = database.dimensionality();

      // Generate all 1-dimensional clusters
      if(logger.isVerbose()) {
        logger.verbose("*** Generate all 1-dimensional clusters ***");
      }

      // mapping of dimensionality to set of subspaces
      HashMap<Integer, List<Subspace<V>>> subspaceMap = new HashMap<Integer, List<Subspace<V>>>();

      // list of 1-dimensional subspaces containing clusters
      List<Subspace<V>> s_1 = new ArrayList<Subspace<V>>();
      subspaceMap.put(0, s_1);

      // mapping of subspaces to list of clusters
      TreeMap<Subspace<V>, List<Cluster<Model>>> clusterMap = new TreeMap<Subspace<V>, List<Cluster<Model>>>(new Subspace.DimensionComparator());

      for(int d = 0; d < dimensionality; d++) {
        Subspace<V> currentSubspace = new Subspace<V>(d);
        List<Cluster<Model>> clusters = runDBSCAN(database, null, currentSubspace);

        if(logger.isDebuggingFiner()) {
          StringBuffer msg = new StringBuffer();
          msg.append("\n").append(clusters.size()).append(" clusters in subspace ").append(subspaceToString(currentSubspace)).append(": \n");
          for(Cluster<Model> cluster : clusters) {
            msg.append("      " + cluster.getIDs() + "\n");
          }
          logger.debugFiner(msg.toString());
        }

        if(!clusters.isEmpty()) {
          s_1.add(currentSubspace);
          clusterMap.put(currentSubspace, clusters);
        }
      }

      // Generate (d+1)-dimensional clusters from d-dimensional clusters
      for(int d = 0; d < dimensionality - 1; d++) {
        if(logger.isVerbose()) {
          logger.verbose("\n*** Generate " + (d + 2) + "-dimensional clusters from " + (d + 1) + "-dimensional clusters ***");
        }

        List<Subspace<V>> subspaces = subspaceMap.get(d);
        if(subspaces == null || subspaces.isEmpty()) {
          if(logger.isVerbose()) {
            logger.verbose("No more subspaces found");
          }
          break;
        }

        List<Subspace<V>> candidates = generateSubspaceCandidates(subspaces);
        List<Subspace<V>> s_d = new ArrayList<Subspace<V>>();

        for(Subspace<V> candidate : candidates) {
          Subspace<V> bestSubspace = bestSubspace(subspaces, candidate, clusterMap);
          if(logger.isDebuggingFine()) {
            logger.debugFine("best subspace of " + subspaceToString(candidate) + ": " + subspaceToString(bestSubspace));
          }

          List<Cluster<Model>> bestSubspaceClusters = clusterMap.get(bestSubspace);
          List<Cluster<Model>> clusters = new ArrayList<Cluster<Model>>();
          for(Cluster<Model> cluster : bestSubspaceClusters) {
            List<Cluster<Model>> candidateClusters = runDBSCAN(database, new ArrayList<Integer>(cluster.getIDs()), candidate);
            if(!candidateClusters.isEmpty()) {
              clusters.addAll(candidateClusters);
            }
          }

          if(logger.isDebuggingFine()) {
            StringBuffer msg = new StringBuffer();
            msg.append(clusters.size() + " cluster(s) in subspace " + candidate + ": \n");
            for(Cluster<Model> c : clusters) {
              msg.append("      " + c.getIDs() + "\n");
            }
            logger.debugFine(msg.toString());
          }

          if(!clusters.isEmpty()) {
            s_d.add(candidate);
            clusterMap.put(candidate, clusters);
          }
        }

        if(!s_d.isEmpty()) {
          subspaceMap.put(d + 1, s_d);
        }
      }

      result = new Clustering<Model>();
      for(Subspace<V> subspace : clusterMap.keySet()) {
        List<Cluster<Model>> clusters = clusterMap.get(subspace);
        for(Cluster<Model> cluster : clusters) {
          Cluster<Model> newCluster = new Cluster<Model>(cluster.getGroup());
          newCluster.setModel(new AxesModel(dimensions(subspace)));
          newCluster.setName(cluster.getName());
          result.addCluster(newCluster);
        }
      }
    }
    catch(ParameterException e) {
      throw new IllegalStateException(e);
    }
    catch(UnableToComplyException e) {
      throw new IllegalStateException(e);
    }
    return result;
  }

  /**
   * Returns the result of the algorithm.
   * 
   * @return the result of the algorithm
   */
  public Clustering<Model> getResult() {
    return result;
  }

  /**
   * Returns a description of the algorithm.
   * 
   * @return a description of the algorithm
   */
  public Description getDescription() {
    return new Description("SUBCLU", "Density connected Subspace Clustering", "Algorithm to detect arbitrarily shaped and positioned clusters " + "in subspaces. SUBCLU delivers for each subspace the same clusters " + "DBSCAN would have found, when applied to this subspace seperately. ", "K. Kailing, H.-P. Kriegel, P. Kroeger: " + "Density connected Subspace Clustering for High Dimensional Data. " + "In Proc. SIAM Int. Conf. on Data Mining (SDM'04), Lake Buena Vista, FL, 2004.");
  }

  /**
   * Calls the super method and sets additionally the value of the parameters
   * {@link #EPSILON_PARAM}, {@link #MINPTS_PARAM}, and
   * {@link #DISTANCE_FUNCTION_PARAM} and instantiates {@link #distanceFunction}
   * according to the value of parameter {@link #DISTANCE_FUNCTION_PARAM}. The
   * remaining parameters are passed to the {@link #distanceFunction}.
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    // epsilon
    epsilon = EPSILON_PARAM.getValue();
    // minpts
    minpts = MINPTS_PARAM.getValue();

    // distance function
    distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();
    addParameterizable(distanceFunction);
    remainingParameters = distanceFunction.setParameters(remainingParameters);

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Runs the DBSCAN algorithm on the specified partition of the database in the
   * given subspace. If parameter {@link #ids} is null DBSCAN will be applied to
   * the whole database.
   * 
   * @param database the database holding the objects to run DBSCAN on
   * @param ids the IDs of the database defining the partition to run DBSCAN on
   *        - if this parameter is null DBSCAN will be applied to the whole
   *        database
   * @param subspace the subspace to run DBSCAN on
   * @return the clustering result of the DBSCAN run
   * @throws ParameterException in case of wrong parameter-setting
   * @throws UnableToComplyException in case of problems during the creation of
   *         the database partition
   */
  private List<Cluster<Model>> runDBSCAN(Database<V> database, List<Integer> ids, Subspace<V> subspace) throws ParameterException, UnableToComplyException {
    // init DBSCAN
    DBSCAN<V, D> dbscan = new DBSCAN<V, D>();
    ArrayList<String> parameters = new ArrayList<String>();

    // distance function
    OptionUtil.addParameter(parameters, DBSCAN.DISTANCE_FUNCTION_ID, distanceFunction.getClass().getName());

    // selected dimensions for distance function
    parameters.add(OptionHandler.OPTION_PREFIX + AbstractDimensionsSelectingDoubleDistanceFunction.DIMS_ID.getName());
    parameters.add(Util.parseSelectedBits(dimensions(subspace), ","));

    // additional distance function parameters
    ArrayList<String> distanceFunctionParams = distanceFunction.getParameters();
    for(String param : distanceFunctionParams) {
      parameters.add(param);
    }

    // epsilon
    OptionUtil.addParameter(parameters, DBSCAN.EPSILON_ID, epsilon);

    // minpts
    OptionUtil.addParameter(parameters, DBSCAN.MINPTS_ID, Integer.toString(minpts));

    dbscan.setParameters(parameters);

    // run DBSCAN
    if(logger.isVerbose()) {
      logger.verbose("\nRun DBSCAN on subspace " + subspaceToString(subspace));
    }
    if(ids == null) {
      dbscan.run(database);
    }
    else {
      Database<V> db = database.partition(ids);
      dbscan.run(db);
    }

    // separate cluster and noise
    List<Cluster<Model>> clusterAndNoise = dbscan.getResult().getAllClusters();
    List<Cluster<Model>> clusters = new ArrayList<Cluster<Model>>();
    for(Cluster<Model> c : clusterAndNoise) {
      if(!c.isNoise()) {
        clusters.add(c);
      }
    }
    return clusters;
  }

  /**
   * Generates {@code d+1}-dimensional subspace candidates from the specified
   * {@code d}-dimensional subspaces.
   * 
   * @param subspaces the {@code d}-dimensional subspaces
   * @return the {@code d+1}-dimensional subspace candidates
   */
  private List<Subspace<V>> generateSubspaceCandidates(List<Subspace<V>> subspaces) {
    List<Subspace<V>> candidates = new ArrayList<Subspace<V>>();

    if(subspaces.isEmpty()) {
      return candidates;
    }

    // Generate (d+1)-dimensional candidate subspaces
    int d = subspaces.get(0).dimensionality();

    StringBuffer msgFine = new StringBuffer("\n");
    if(logger.isDebuggingFiner()) {
      msgFine.append("subspaces ").append(subspaces).append("\n");
    }

    for(int i = 0; i < subspaces.size(); i++) {
      Subspace<V> s1 = subspaces.get(i);
      for(int j = i + 1; j < subspaces.size(); j++) {
        Subspace<V> s2 = subspaces.get(j);
        Subspace<V> candidate = s1.join(s2);
        if(logger.isDebuggingFiner()) {
          msgFine.append("candidate: ").append(subspaceToString(candidate)).append("\n");
        }
        if(candidate != null) {
          // prune irrelevant candidate subspaces
          List<Subspace<V>> lowerSubspaces = lowerSubspaces(candidate);
          if(logger.isDebuggingFiner()) {
            msgFine.append("lowerSubspaces: ").append(lowerSubspaces).append("\n");
          }
          boolean irrelevantCandidate = false;
          for(Subspace<V> s : lowerSubspaces) {
            if(!subspaces.contains(s)) {
              irrelevantCandidate = true;
              break;
            }
          }
          if(!irrelevantCandidate) {
            candidates.add(candidate);
          }
        }
      }
    }

    if(logger.isDebuggingFiner()) {
      logger.debugFiner(msgFine.toString());
    }
    if(logger.isVerbose()) {
      StringBuffer msg = new StringBuffer();
      msg.append(d + 1).append("-dimensional candidate subspaces: ");
      for(Subspace<V> candidate : candidates) {
        msg.append(subspaceToString(candidate)).append(" ");
      }
      logger.verbose(msg.toString());
    }

    return candidates;
  }

  /**
   * Returns the list of all {@code (d-1)}-dimensional subspaces of the
   * specified {@code d}-dimensional subspace.
   * 
   * @param subspace the {@code d}-dimensional subspace
   * @return a list of all {@code (d-1)}-dimensional subspaces
   */
  private List<Subspace<V>> lowerSubspaces(Subspace<V> subspace) {
    int dimensionality = subspace.dimensionality();
    if(dimensionality <= 1) {
      return null;
    }

    // order result according to the dimensions
    List<Subspace<V>> result = new ArrayList<Subspace<V>>();
    SortedSet<Integer> dimensions = subspace.getDimensions();
    for(int dim : dimensions) {
      SortedSet<Integer> newDimensions = new TreeSet<Integer>(dimensions);
      newDimensions.remove(dim);
      result.add(new Subspace<V>(newDimensions));
    }

    return result;
  }

  /**
   * Determines the {@code d}-dimensional subspace of the {@code (d+1)}
   * -dimensional candidate with minimal number of objects in the cluster.
   * 
   * @param subspaces the list of {@code d}-dimensional subspaces containing
   *        clusters
   * @param candidate the {@code (d+1)}-dimensional candidate subspace
   * @param clusterMap the mapping of subspaces to clusters
   * @return the {@code d}-dimensional subspace of the {@code (d+1)}
   *         -dimensional candidate with minimal number of objects in the
   *         cluster
   */
  private Subspace<V> bestSubspace(List<Subspace<V>> subspaces, Subspace<V> candidate, TreeMap<Subspace<V>, List<Cluster<Model>>> clusterMap) {
    Subspace<V> bestSubspace = null;

    for(Subspace<V> subspace : subspaces) {
      int min = Integer.MAX_VALUE;

      if(subspace.isSubspace(candidate)) {
        List<Cluster<Model>> clusters = clusterMap.get(subspace);
        for(Cluster<Model> cluster : clusters) {
          int clusterSize = cluster.size();
          if(clusterSize < min) {
            min = clusterSize;
            bestSubspace = subspace;
          }
        }
      }
    }

    return bestSubspace;
  }

  /**
   * Returns a string representation of the dimensions of the specified
   * subspace.
   * 
   * @param subspace the subspace
   * @return a string representation of the dimensions of the specified subspace
   */
  private String subspaceToString(Subspace<V> subspace) {
    StringBuffer result = new StringBuffer();
    result.append("[");
    for(Integer dimension : subspace.getDimensions()) {
      if(result.length() == 1) {
        result.append(dimension + 1);
      }
      else {
        result.append(", ").append(dimension + 1);
      }
    }
    result.append("]");

    return result.toString();
  }
  
  /**
   * Returns a BitSet representing the dimensions which build the specified subspace.
   * @param subspace the subspace
   * @return a BitSet representing the dimensions which build the specified subspace
   */
  private BitSet dimensions(Subspace<V> subspace) {
    SortedSet<Integer> dimensions = subspace.getDimensions();
    BitSet dimensionBits = new BitSet();
    for(Integer d : dimensions) {
      dimensionBits.set(d);
    }
    return dimensionBits;
  }
}
