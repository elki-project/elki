package experimentalcode.elke.algorithm;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.AbstractDimensionsSelectingDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DimensionsSelectingEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * <p>
 * Implementation of the SUBCLU algorithm, an algorithm to detect arbitrarily
 * shaped and positioned clusters in subspaces.
 * SUBCLU delivers for each subspace the same clusters DBSCAN would have found,
 * when applied to this subspace separately.
 * </p>
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
public class SUBCLU<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractAlgorithm<V, Clustering<SubspaceModel<V>>> implements ClusteringAlgorithm<Clustering<SubspaceModel<V>>, V> {
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
  private final ObjectParameter<AbstractDimensionsSelectingDoubleDistanceFunction<V>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<AbstractDimensionsSelectingDoubleDistanceFunction<V>>(DISTANCE_FUNCTION_ID, AbstractDimensionsSelectingDoubleDistanceFunction.class, DimensionsSelectingEuclideanDistanceFunction.class);

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
  private final DistanceParameter<DoubleDistance> EPSILON_PARAM;

  /**
   * Holds the value of {@link #EPSILON_PARAM}.
   */
  private DoubleDistance epsilon;

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
  private Clustering<SubspaceModel<V>> result;

  /**
   * Provides the SUBCLU algorithm, adding parameters {@link #EPSILON_PARAM},
   * {@link #MINPTS_PARAM}, and {@link #DISTANCE_FUNCTION_PARAM} to the option
   * handler additionally to parameters of super class.
   */
  public SUBCLU(Parameterization config) {
    super(config);
    logger.getWrappedLogger().setLevel(Level.INFO);

    // distance function
    if (config.grab(this, DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }

    // parameter epsilon
    EPSILON_PARAM = new DistanceParameter<DoubleDistance>(EPSILON_ID, distanceFunction);
    if (config.grab(this, EPSILON_PARAM)) {
      epsilon = EPSILON_PARAM.getValue();
    }

    // parameter minpts
    if (config.grab(this, MINPTS_PARAM)) {
      minpts = MINPTS_PARAM.getValue();
    }
  }

  /**
   * Performs the SUBCLU algorithm on the given database.
   * 
   */
  @Override
  protected Clustering<SubspaceModel<V>> runInTime(Database<V> database) throws IllegalStateException {
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

      result = new Clustering<SubspaceModel<V>>();
      for(Subspace<V> subspace : clusterMap.descendingKeySet()) {

        List<Cluster<Model>> clusters = clusterMap.get(subspace);
        int c = 1;
        for(Cluster<Model> cluster : clusters) {
          Cluster<SubspaceModel<V>> newCluster = new Cluster<SubspaceModel<V>>(cluster.getGroup());
          newCluster.setModel(new SubspaceModel<V>(subspace));
          newCluster.setName("subspace_" + subspaceToString(subspace, "-") + "_cluster_" + c);
          result.addCluster(newCluster);
          c++;
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
  public Clustering<SubspaceModel<V>> getResult() {
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
    ListParameterization config = new ListParameterization();

    // distance function
    config.addParameter(DBSCAN.DISTANCE_FUNCTION_ID, distanceFunction);

    // selected dimensions for distance function
    config.addParameter(AbstractDimensionsSelectingDoubleDistanceFunction.DIMS_ID, Util.parseSelectedBits(subspace.getDimensions(), ","));

    // epsilon
    config.addParameter(DBSCAN.EPSILON_ID, epsilon);

    // minpts
    config.addParameter(DBSCAN.MINPTS_ID, minpts);

    DBSCAN<V, D> dbscan = new DBSCAN<V, D>(config);
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
    BitSet dimensions = subspace.getDimensions();
    for(int dim = dimensions.nextSetBit(0); dim >= 0; dim = dimensions.nextSetBit(dim + 1)) {
      BitSet newDimensions = (BitSet) dimensions.clone();
      newDimensions.set(dim, false);
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
   * Returns a string representation of the dimensions of the specified subspace
   * separated by comma.
   * 
   * @param subspace the subspace
   * @return a string representation of the dimensions of the specified subspace
   */
  private String subspaceToString(Subspace<V> subspace) {
    return subspaceToString(subspace, ", ");
  }

  /**
   * Returns a string representation of the dimensions of the specified
   * subspace.
   * 
   * @param subspace the subspace
   * @param sep the separator between the dimensions
   * @return a string representation of the dimensions of the specified subspace
   */
  private String subspaceToString(Subspace<V> subspace, String sep) {
    StringBuffer result = new StringBuffer();
    result.append("[");
    for(int dim = subspace.getDimensions().nextSetBit(0); dim >= 0; dim = subspace.getDimensions().nextSetBit(dim + 1)) {
      if(result.length() == 1) {
        result.append(dim + 1);
      }
      else {
        result.append(sep).append(dim + 1);
      }
    }
    result.append("]");

    return result.toString();
  }

}
