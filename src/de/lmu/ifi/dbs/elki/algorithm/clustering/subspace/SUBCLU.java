package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.ProxyView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.AbstractDimensionsSelectingDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DimensionsSelectingEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * <p>
 * Implementation of the SUBCLU algorithm, an algorithm to detect arbitrarily
 * shaped and positioned clusters in subspaces. SUBCLU delivers for each
 * subspace the same clusters DBSCAN would have found, when applied to this
 * subspace separately.
 * </p>
 * <p>
 * Reference: <br>
 * K. Kailing, H.-P. Kriegel, P. Kroeger: Density connected Subspace Clustering
 * for High Dimensional Data. <br>
 * In Proc. SIAM Int. Conf. on Data Mining (SDM'04), Lake Buena Vista, FL, 2004.
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses DBSCAN
 * @apiviz.uses DimensionsSelectingEuclideanDistanceFunction
 * @apiviz.has SubspaceModel
 * 
 * @param <V> the type of FeatureVector handled by this Algorithm
 */
@Title("SUBCLU: Density connected Subspace Clustering")
@Description("Algorithm to detect arbitrarily shaped and positioned clusters in subspaces. SUBCLU delivers for each subspace the same clusters DBSCAN would have found, when applied to this subspace seperately.")
@Reference(authors = "K. Kailing, H.-P. Kriegel, P. Kröger", title = "Density connected Subspace Clustering for High Dimensional Data. ", booktitle = "Proc. SIAM Int. Conf. on Data Mining (SDM'04), Lake Buena Vista, FL, 2004")
public class SUBCLU<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V, Clustering<SubspaceModel<V>>> implements ClusteringAlgorithm<Clustering<SubspaceModel<V>>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SUBCLU.class);

  /**
   * The distance function to determine the distance between database objects.
   * <p>
   * Default value: {@link DimensionsSelectingEuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -subclu.distancefunction}
   * </p>
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("subclu.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to
   * {@link AbstractDimensionsSelectingDoubleDistanceFunction}.
   * <p>
   * Key: {@code -subclu.epsilon}
   * </p>
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("subclu.epsilon", "The maximum radius of the neighborhood to be considered.");

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   * <p>
   * Key: {@code -subclu.minpts}
   * </p>
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("subclu.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

  /**
   * Holds the instance of the distance function specified by
   * {@link #DISTANCE_FUNCTION_ID}.
   */
  private AbstractDimensionsSelectingDoubleDistanceFunction<V> distanceFunction;

  /**
   * Holds the value of {@link #EPSILON_ID}.
   */
  private DoubleDistance epsilon;

  /**
   * Holds the value of {@link #MINPTS_ID}.
   */
  private int minpts;

  /**
   * Holds the result;
   */
  private Clustering<SubspaceModel<V>> result;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts value
   */
  public SUBCLU(AbstractDimensionsSelectingDoubleDistanceFunction<V> distanceFunction, DoubleDistance epsilon, int minpts) {
    super();
    this.distanceFunction = distanceFunction;
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  /**
   * Performs the SUBCLU algorithm on the given database.
   */
  @Override
  protected Clustering<SubspaceModel<V>> runInTime(Database database) throws IllegalStateException {
    try {
      Relation<V> dataQuery = getRelation(database);
      int dimensionality = DatabaseUtil.dimensionality(dataQuery);

      StepProgress stepprog = logger.isVerbose() ? new StepProgress(dimensionality) : null;

      // Generate all 1-dimensional clusters
      if(stepprog != null) {
        stepprog.beginStep(1, "Generate all 1-dimensional clusters.", logger);
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
        List<Cluster<Model>> clusters = runDBSCAN(dataQuery, null, currentSubspace);

        if(logger.isDebuggingFiner()) {
          StringBuffer msg = new StringBuffer();
          msg.append("\n").append(clusters.size()).append(" clusters in subspace ").append(currentSubspace.dimensonsToString()).append(": \n");
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
        if(stepprog != null) {
          stepprog.beginStep(d + 2, "Generate " + (d + 2) + "-dimensional clusters from " + (d + 1) + "-dimensional clusters.", logger);
        }

        List<Subspace<V>> subspaces = subspaceMap.get(d);
        if(subspaces == null || subspaces.isEmpty()) {
          if(stepprog != null) {
            for(int dim = d + 1; dim < dimensionality - 1; dim++) {
              stepprog.beginStep(dim + 2, "Generation of" + (dim + 2) + "-dimensional clusters not applicable, because no more " + (d + 2) + "-dimensional subspaces found.", logger);
            }
          }
          break;
        }

        List<Subspace<V>> candidates = generateSubspaceCandidates(subspaces);
        List<Subspace<V>> s_d = new ArrayList<Subspace<V>>();

        for(Subspace<V> candidate : candidates) {
          Subspace<V> bestSubspace = bestSubspace(subspaces, candidate, clusterMap);
          if(logger.isDebuggingFine()) {
            logger.debugFine("best subspace of " + candidate.dimensonsToString() + ": " + bestSubspace.dimensonsToString());
          }

          List<Cluster<Model>> bestSubspaceClusters = clusterMap.get(bestSubspace);
          List<Cluster<Model>> clusters = new ArrayList<Cluster<Model>>();
          for(Cluster<Model> cluster : bestSubspaceClusters) {
            List<Cluster<Model>> candidateClusters = runDBSCAN(dataQuery, cluster.getIDs(), candidate);
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

      // build result
      int numClusters = 1;
      result = new Clustering<SubspaceModel<V>>("SUBCLU clustering", "subclu-clustering");
      for(Subspace<V> subspace : clusterMap.descendingKeySet()) {
        List<Cluster<Model>> clusters = clusterMap.get(subspace);
        for(Cluster<Model> cluster : clusters) {
          Cluster<SubspaceModel<V>> newCluster = new Cluster<SubspaceModel<V>>(cluster.getIDs());
          newCluster.setModel(new SubspaceModel<V>(subspace, DatabaseUtil.centroid(dataQuery, cluster.getIDs())));
          newCluster.setName("cluster_" + numClusters++);
          result.addCluster(newCluster);
        }
      }

      if(stepprog != null) {
        stepprog.setCompleted(logger);
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
   * Runs the DBSCAN algorithm on the specified partition of the database in the
   * given subspace. If parameter {@code ids} is null DBSCAN will be applied to
   * the whole database.
   * 
   * @param relation the database holding the objects to run DBSCAN on
   * @param ids the IDs of the database defining the partition to run DBSCAN on
   *        - if this parameter is null DBSCAN will be applied to the whole
   *        database
   * @param subspace the subspace to run DBSCAN on
   * @return the clustering result of the DBSCAN run
   * @throws ParameterException in case of wrong parameter-setting
   * @throws UnableToComplyException in case of problems during the creation of
   *         the database partition
   */
  private List<Cluster<Model>> runDBSCAN(Relation<V> relation, DBIDs ids, Subspace<V> subspace) throws ParameterException, UnableToComplyException {
    // distance function
    distanceFunction.setSelectedDimensions(subspace.getDimensions());

    ProxyDatabase proxy;
    if(ids == null) {
      // TODO: in this case, we might want to use an index - the proxy below will prevent this!
      ids = relation.getDBIDs();
    }
    
    proxy = new ProxyDatabase(ids);
    Relation<V> prep = ProxyView.wrap(proxy, ids, relation);
    proxy.addRelation(prep);

    DBSCAN<V, DoubleDistance> dbscan = new DBSCAN<V, DoubleDistance>(distanceFunction, epsilon, minpts);
    // run DBSCAN
    if(logger.isVerbose()) {
      logger.verbose("\nRun DBSCAN on subspace " + subspace.dimensonsToString());
    }
    Clustering<Model> dbsres = dbscan.run(proxy);

    // separate cluster and noise
    List<Cluster<Model>> clusterAndNoise = dbsres.getAllClusters();
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

        if(candidate != null) {
          if(logger.isDebuggingFiner()) {
            msgFine.append("candidate: ").append(candidate.dimensonsToString()).append("\n");
          }
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
    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append(d + 1).append("-dimensional candidate subspaces: ");
      for(Subspace<V> candidate : candidates) {
        msg.append(candidate.dimensonsToString()).append(" ");
      }
      logger.debug(msg.toString());
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

  @Override
  public VectorFieldTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    protected int minpts = 0;

    protected DoubleDistance epsilon = null;

    protected AbstractDimensionsSelectingDoubleDistanceFunction<V> distance = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<AbstractDimensionsSelectingDoubleDistanceFunction<V>> param = new ObjectParameter<AbstractDimensionsSelectingDoubleDistanceFunction<V>>(DISTANCE_FUNCTION_ID, AbstractDimensionsSelectingDoubleDistanceFunction.class, DimensionsSelectingEuclideanDistanceFunction.class);
      if(config.grab(param)) {
        distance = param.instantiateClass(config);
      }

      DistanceParameter<DoubleDistance> epsilonP = new DistanceParameter<DoubleDistance>(EPSILON_ID, distance);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }

      IntParameter minptsP = new IntParameter(MINPTS_ID, new GreaterConstraint(0));
      if(config.grab(minptsP)) {
        minpts = minptsP.getValue();
      }
    }

    @Override
    protected SUBCLU<V> makeInstance() {
      return new SUBCLU<V>(distance, epsilon, minpts);
    }
  }
}