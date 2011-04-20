package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.UpdatableHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
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
public class OPTICS<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm<O, D, ClusterOrderResult<D>> {
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
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("optics.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

  /**
   * Parameter to specify the steepness threshold.
   */
  public static final OptionID XI_ID = OptionID.getOrCreateOptionID("optics.xi", "Threshold for the steepness requirement.");

  /**
   * Hold the value of {@link #EPSILON_ID}.
   */
  private D epsilon;

  /**
   * Holds the value of {@link #MINPTS_ID}.
   */
  private int minpts;

  /**
   * Holds the value of {@link #XI_ID}.
   */
  private double ixi;

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
   * @param xi Xi value
   */
  public OPTICS(DistanceFunction<? super O, D> distanceFunction, D epsilon, int minpts, double xi) {
    super(distanceFunction);
    this.epsilon = epsilon;
    this.minpts = minpts;
    this.ixi = 1.0 - xi;
  }

  /**
   * Performs the OPTICS algorithm on the given database.
   * 
   */
  @Override
  protected ClusterOrderResult<D> runInTime(Database database) {
    // Default value is infinite distance
    if(epsilon == null) {
      epsilon = getDistanceFunction().getDistanceFactory().infiniteDistance();
    }
    RangeQuery<O, D> rangeQuery = database.getRangeQuery(getDistanceQuery(database), epsilon);

    int size = rangeQuery.getRelation().size();
    final FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("OPTICS", size, logger) : null;

    processedIDs = DBIDUtil.newHashSet(size);
    ClusterOrderResult<D> clusterOrder = new ClusterOrderResult<D>("OPTICS Clusterorder", "optics-clusterorder");
    heap = new UpdatableHeap<ClusterOrderEntry<D>>();

    for(DBID id : rangeQuery.getRelation().iterDBIDs()) {
      if(!processedIDs.contains(id)) {
        expandClusterOrder(clusterOrder, database, rangeQuery, id, progress);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }

    if(ixi < 1.) {
      if(NumberDistance.class.isInstance(getDistanceFunction().getDistanceFactory())) {
        logger.verbose("Extracting clusters with Xi: " + (1. - ixi));
        ClusterOrderResult<DoubleDistance> distanceClusterOrder = ClassGenericsUtil.castWithGenericsOrNull(ClusterOrderResult.class, clusterOrder);
        OPTICSXi.extractClusters(distanceClusterOrder, getRelation(database), ixi, minpts);
      }
      else {
        logger.verbose("Xi cluster extraction only supported for number distances!");
      }
    }

    return clusterOrder;
  }

  /**
   * OPTICS-function expandClusterOrder.
   * 
   * @param clusterOrder Cluster order result to expand
   * @param database the database on which the algorithm is run
   * @param rangeQuery the range query to use
   * @param objectID the currently processed object
   * @param progress the progress object to actualize the current progress if
   *        the algorithm
   */
  protected void expandClusterOrder(ClusterOrderResult<D> clusterOrder, Database database, RangeQuery<O, D> rangeQuery, DBID objectID, FiniteProgress progress) {
    assert (heap.isEmpty());
    heap.add(new ClusterOrderEntry<D>(objectID, null, getDistanceFunction().getDistanceFactory().infiniteDistance()));

    while(!heap.isEmpty()) {
      final ClusterOrderEntry<D> current = heap.poll();
      clusterOrder.add(current);
      processedIDs.add(current.getID());

      List<DistanceResultPair<D>> neighbors = rangeQuery.getRangeForDBID(current.getID(), epsilon);
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

  @Override
  public TypeInformation getInputTypeRestriction() {
    return getDistanceFunction().getInputTypeRestriction();
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
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    protected D epsilon = null;

    protected int minpts = 0;

    protected double xi = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DistanceParameter<D> epsilonP = new DistanceParameter<D>(EPSILON_ID, distanceFunction, true);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }

      IntParameter minptsP = new IntParameter(MINPTS_ID, new GreaterConstraint(0));
      if(config.grab(minptsP)) {
        minpts = minptsP.getValue();
      }

      DoubleParameter xiP = new DoubleParameter(XI_ID, true);
      xiP.addConstraint(new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.CLOSE, 1.0, IntervalConstraint.IntervalBoundary.OPEN));
      if(config.grab(xiP)) {
        xi = xiP.getValue();
      }
    }

    @Override
    protected OPTICS<O, D> makeInstance() {
      return new OPTICS<O, D>(distanceFunction, epsilon, minpts, xi);
    }
  }
}