package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.UpdatableHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
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
public class OPTICS<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D, ClusterOrderResult<D>> {
  /**
   * OptionID for {@link #EPSILON_PARAM}
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("optics.epsilon", "The maximum radius of the neighborhood to be considered.");

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to the distance function specified.
   * <p>
   * Key: {@code -optics.epsilon}
   * </p>
   */
  private final DistanceParameter<D> EPSILON_PARAM;

  /**
   * Hold the value of {@link #EPSILON_PARAM}.
   */
  private D epsilon;

  /**
   * OptionID for {@link #MINPTS_PARAM}
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("optics.minpts", "Threshold for minimum number of points in " + "the epsilon-neighborhood of a point.");

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   * <p>
   * Key: {@code -optics.minpts}
   * </p>
   */
  private final IntParameter MINPTS_PARAM = new IntParameter(MINPTS_ID, new GreaterConstraint(0));

  /**
   * Holds the value of {@link #MINPTS_PARAM}.
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
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public OPTICS(Parameterization config) {
    super(config);
    EPSILON_PARAM = new DistanceParameter<D>(EPSILON_ID, getDistanceFactory());

    if(config.grab(EPSILON_PARAM)) {
      epsilon = EPSILON_PARAM.getValue();
    }

    if(config.grab(MINPTS_PARAM)) {
      minpts = MINPTS_PARAM.getValue();
    }
  }

  /**
   * Performs the OPTICS algorithm on the given database.
   * 
   */
  @Override
  protected ClusterOrderResult<D> runInTime(Database<O> database) {
    final FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("OPTICS", database.size(), logger) : null;

    int size = database.size();
    processedIDs = DBIDUtil.newHashSet(size);
    ClusterOrderResult<D> clusterOrder = new ClusterOrderResult<D>();
    heap = new UpdatableHeap<ClusterOrderEntry<D>>();
    getDistanceFunction().setDatabase(database);

    for(DBID id : database) {
      if(!processedIDs.contains(id)) {
        expandClusterOrder(clusterOrder, database, id, progress);
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
   * @param objectID the currently processed object
   * @param progress the progress object to actualize the current progress if
   *        the algorithm
   */
  protected void expandClusterOrder(ClusterOrderResult<D> clusterOrder, Database<O> database, DBID objectID, FiniteProgress progress) {
    assert(heap.isEmpty());
    heap.add(new ClusterOrderEntry<D>(objectID, null, getDistanceFunction().infiniteDistance()));

    while(!heap.isEmpty()) {
      final ClusterOrderEntry<D> current = heap.poll();
      clusterOrder.add(current);
      processedIDs.add(current.getID());

      List<DistanceResultPair<D>> neighbors = database.rangeQuery(current.getID(), epsilon, getDistanceFunction());
      D coreDistance = neighbors.size() < minpts ? getDistanceFunction().infiniteDistance() : neighbors.get(minpts - 1).getDistance();

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
}