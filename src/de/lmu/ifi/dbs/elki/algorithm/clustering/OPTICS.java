package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.UpdatableHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * OPTICS provides the OPTICS algorithm.
 * <p>
 * Reference: M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander: OPTICS:
 * Ordering Points to Identify the Clustering Structure. <br>
 * In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99).
 * </p>
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObjects handled by the algorithm
 * @param <D> the type of Distance used to discern objects
 */
@Title("OPTICS: Density-Based Hierarchical Clustering")
@Description("Algorithm to find density-connected sets in a database based on the parameters 'minPts' and 'epsilon' (specifying a volume). These two parameters determine a density threshold for clustering.")
@Reference(authors = "M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander", title = "OPTICS: Ordering Points to Identify the Clustering Structure", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)", url = "http://dx.doi.org/10.1145/304181.304187")
public class OPTICS<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm<O, D, ClusterOrderResult<D>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(OPTICS.class);
  
  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to the distance function specified.
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("optics.epsilon", "The maximum radius of the neighborhood to be considered.");

  /**
   * Hold the value of {@link #EPSILON_ID}.
   */
  private D epsilon;

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("optics.minpts", "Threshold for minimum number of points in " + "the epsilon-neighborhood of a point.");

  /**
   * Holds the value of {@link #MINPTS_ID}.
   */
  private int minpts;

  /**
   * Holds a set of processed ids.
   */
  private ModifiableDBIDs processedIDs;

  /**
   * The priority queue for the algorithm.
   */
  private UpdatableHeap<ClusterOrderEntry<D>> heap;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts value
   */
  public OPTICS(DistanceFunction<? super O, D> distanceFunction, D epsilon, int minpts) {
    super(distanceFunction);
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  /**
   * Performs the OPTICS algorithm on the given database.
   * 
   */
  @Override
  protected ClusterOrderResult<D> runInTime(Database<O> database) {
    final FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("OPTICS", database.size(), logger) : null;

    DistanceQuery<O, D> distFunc = getDistanceFunction().instantiate(database);

    int size = database.size();
    processedIDs = DBIDUtil.newHashSet(size);
    ClusterOrderResult<D> clusterOrder = new ClusterOrderResult<D>("OPTICS Clusterorder", "optics-clusterorder");
    heap = new UpdatableHeap<ClusterOrderEntry<D>>();

    for(DBID id : database) {
      if(!processedIDs.contains(id)) {
        expandClusterOrder(clusterOrder, database, distFunc, id, progress);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }
    return clusterOrder;
  }

  /**
   * OPTICS-function expandClusterOrder.
   * 
   * @param database the database on which the algorithm is run
   * @param distFunc the distance function
   * @param objectID the currently processed object
   * @param progress the progress object to actualize the current progress if
   *        the algorithm
   */
  protected void expandClusterOrder(ClusterOrderResult<D> clusterOrder, Database<O> database, DistanceQuery<O, D> distFunc, DBID objectID, FiniteProgress progress) {
    assert (heap.isEmpty());
    heap.add(new ClusterOrderEntry<D>(objectID, null, getDistanceFunction().getDistanceFactory().infiniteDistance()));

    while(!heap.isEmpty()) {
      final ClusterOrderEntry<D> current = heap.poll();
      clusterOrder.add(current);
      processedIDs.add(current.getID());

      List<DistanceResultPair<D>> neighbors = database.rangeQuery(current.getID(), epsilon, distFunc);
      D coreDistance = neighbors.size() < minpts ? getDistanceFunction().getDistanceFactory().infiniteDistance() : neighbors.get(minpts - 1).getDistance();

      if(!coreDistance.isInfiniteDistance()) {
        for(DistanceResultPair<D> neighbor : neighbors) {
          if(processedIDs.contains(neighbor.getID())) {
            continue;
          }
          D reachability = DistanceUtil.max(neighbor.getDistance(), coreDistance);
          heap.add(new ClusterOrderEntry<D>(neighbor.getID(), current.getID(), reachability));
        }
      }
      if(progress != null) {
        progress.setProcessed(processedIDs.size(), logger);
      }
    }
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return Clustering Algorithm
   */
  public static <O extends DatabaseObject, D extends Distance<D>> OPTICS<O, D> parameterize(Parameterization config) {
    DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);
    D epsilon = getParameterEpsilon(config, distanceFunction);
    int minpts = getParameterMinpts(config);
    if(config.hasErrors()) {
      return null;
    }
    return new OPTICS<O, D>(distanceFunction, epsilon, minpts);
  }

  /**
   * Get the epsilon parameter value.
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param config Parameterization
   * @param distanceFunction distance function (for factory)
   * @return Epsilon value
   */
  protected static <O extends DatabaseObject, D extends Distance<D>> D getParameterEpsilon(Parameterization config, DistanceFunction<O, D> distanceFunction) {
    final DistanceParameter<D> param = new DistanceParameter<D>(EPSILON_ID, distanceFunction);
    if(config.grab(param)) {
      return param.getValue();
    }
    return null;
  }

  /**
   * Get the minPts parameter value.
   * 
   * @param config Parameterization
   * @return minpts parameter value
   */
  protected static int getParameterMinpts(Parameterization config) {
    final IntParameter param = new IntParameter(MINPTS_ID, new GreaterConstraint(0));
    if(config.grab(param)) {
      return param.getValue();
    }
    return -1;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}