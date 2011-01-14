package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.model.OPTICSModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
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
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.UpdatableHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.HierarchyHashmapList;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.ModifiableHierarchy;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
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
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("optics.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

  /**
   * Holds the value of {@link #MINPTS_ID}.
   */
  private int minpts;

  /**
   * Parameter to specify the steepness threshold.
   */
  public static final OptionID XI_ID = OptionID.getOrCreateOptionID("optics.xi", "Threshold for the steepness requirement.");

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
  @SuppressWarnings("unchecked")
  @Override
  protected ClusterOrderResult<D> runInTime(Database<O> database) {
    final FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("OPTICS", database.size(), logger) : null;

    RangeQuery<O, D> rangeQuery = database.getRangeQuery(getDistanceFunction(), epsilon);

    int size = database.size();
    processedIDs = DBIDUtil.newHashSet(size);
    ClusterOrderResult<D> clusterOrder = new ClusterOrderResult<D>("OPTICS Clusterorder", "optics-clusterorder");
    heap = new UpdatableHeap<ClusterOrderEntry<D>>();

    for(DBID id : database) {
      if(!processedIDs.contains(id)) {
        expandClusterOrder(clusterOrder, database, rangeQuery, id, progress);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }

    if(ixi < 1.) {
      if(NumberDistance.class.isInstance(getDistanceFunction().getDistanceFactory())) {
        logger.verbose("Extracting clusters with Chi: " + (1. - ixi));
        extractClusters((ClusterOrderResult<DoubleDistance>) clusterOrder, database);
      }
      else {
        logger.verbose("Chi cluster extraction only supported for number distances!");
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
  protected void expandClusterOrder(ClusterOrderResult<D> clusterOrder, Database<O> database, RangeQuery<O, D> rangeQuery, DBID objectID, FiniteProgress progress) {
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

  protected <N extends NumberDistance<N, ?>> void extractClusters(ClusterOrderResult<N> clusterOrderResult, Database<?> database) {
    // TODO: add progress?
    List<ClusterOrderEntry<N>> clusterOrder = clusterOrderResult.getClusterOrder();
    N mib = null;
    List<SteepDownArea> sdaset = new java.util.Vector<SteepDownArea>();
    ModifiableHierarchy<Cluster<OPTICSModel>> hier = new HierarchyHashmapList<Cluster<OPTICSModel>>();
    HashSet<Cluster<OPTICSModel>> curclusters = new HashSet<Cluster<OPTICSModel>>();
    HashSetModifiableDBIDs unclaimedids = DBIDUtil.newHashSet(database.getIDs());
    int index = 0;
    while(index < clusterOrder.size()) {
      ClusterOrderEntry<N> e = clusterOrder.get(index);
      mib = DistanceUtil.max(mib, e.getReachability());
      // The last point cannot be the start of a steep area
      if(index + 1 < clusterOrder.size()) {
        ClusterOrderEntry<N> esucc = clusterOrder.get(index + 1);
        // Chi-steep down area
        if(e.getReachability().doubleValue() * ixi >= esucc.getReachability().doubleValue()) {
          if(logger.isDebuggingFinest()) {
            logger.debugFinest("Chi-steep down start at " + index);
          }
          // Update mib values with current mib and filter
          updateFilterSDASet(mib.doubleValue(), sdaset);
          final double startval = e.getReachability().doubleValue();
          int startsteep = index;
          int endsteep = index;
          {
            while(index + 1 < clusterOrder.size()) {
              index++;
              e = esucc;
              esucc = clusterOrder.get(index + 1);
              // not going downward - stop here.
              if(e.getReachability().doubleValue() < esucc.getReachability().doubleValue()) {
                break;
              }
              // still steep - continue.
              if(e.getReachability().doubleValue() * ixi >= esucc.getReachability().doubleValue()) {
                endsteep = index;
              }
              else {
                // Stop looking after minpts steps.
                if(index - endsteep > minpts) {
                  break;
                }
              }
            }
          }
          mib = clusterOrder.get(endsteep).getReachability();
          sdaset.add(new SteepDownArea(startsteep, endsteep, startval, 0));
          continue;
        }
        else
        // Chi-steep up area
        if(e.getReachability().doubleValue() <= esucc.getReachability().doubleValue() * ixi) {
          // Update mib values with current mib and filter
          updateFilterSDASet(mib.doubleValue(), sdaset);
          // find end of steep-up-area, update global mib
          int startsteep = index;
          int endsteep = index;
          {
            while(index + 1 < clusterOrder.size()) {
              index++;
              e = esucc;
              esucc = clusterOrder.get(index + 1);
              // not going upward - stop here.
              if(e.getReachability().doubleValue() > esucc.getReachability().doubleValue()) {
                break;
              }
              // still steep - continue.
              if(e.getReachability().doubleValue() <= esucc.getReachability().doubleValue() * ixi) {
                endsteep = index;
              }
              else {
                // Stop looking after minpts steps.
                if(index - endsteep > minpts) {
                  break;
                }
              }
            }
          }
          // TODO: cache result
          mib = clusterOrder.get(endsteep).getReachability();
          if(logger.isDebuggingFinest()) {
            logger.debugFinest("Chi-steep up area: " + startsteep + " - " + endsteep);
          }
          // Validate and computer clusters
          // logger.debug("SDA size:"+sdaset.size()+" "+sdaset);
          ListIterator<SteepDownArea> sdaiter = sdaset.listIterator(sdaset.size());
          while(sdaiter.hasPrevious()) {
            SteepDownArea sda = sdaiter.previous();
            // logger.debug("Comparing: eU="+mib.doubleValue()+" SDA: "+sda);
            if(mib.doubleValue() * ixi >= sda.mib && index - sda.startindex + 1 >= minpts) {
              ModifiableDBIDs dbids = DBIDUtil.newArray();
              for(int idx = sda.startindex; idx <= endsteep; idx++) {
                final DBID dbid = clusterOrder.get(idx).getID();
                // Collect only unclaimed IDs.
                if(unclaimedids.remove(dbid)) {
                  dbids.add(dbid);
                }
              }
              if(logger.isDebuggingFine()) {
                logger.debugFine("Found cluster with " + dbids.size() + " objects.");
              }
              OPTICSModel model = new OPTICSModel(sda.startindex, endsteep);
              Cluster<OPTICSModel> cluster = new Cluster<OPTICSModel>("Cluster_" + sda.startindex + "_" + endsteep, dbids, model, hier);
              Iterator<Cluster<OPTICSModel>> iter = curclusters.iterator();
              while(iter.hasNext()) {
                Cluster<OPTICSModel> clus = iter.next();
                OPTICSModel omodel = clus.getModel();
                if(model.getStartIndex() <= omodel.getStartIndex() && omodel.getEndIndex() <= model.getEndIndex()) {
                  hier.add(cluster, clus);
                  iter.remove();
                }
              }
              curclusters.add(cluster);
            }
          }
          // We have already incremented the index.
          continue;
        }
      }
      index += 1;
    }
    if(curclusters.size() > 0 || unclaimedids.size() > 0) {
      final Clustering<OPTICSModel> clustering = new Clustering<OPTICSModel>("OPTICS Chi-Clusters", "optics");
      if(unclaimedids.size() > 0) {
        final Cluster<OPTICSModel> noiseclus = new Cluster<OPTICSModel>("Noise", unclaimedids, true, new OPTICSModel(0, clusterOrder.size() - 1), hier);
        for(Cluster<OPTICSModel> cluster : curclusters) {
          hier.add(noiseclus, cluster);
        }
        clustering.addCluster(noiseclus);
      }
      else {
        for(Cluster<OPTICSModel> cluster : curclusters) {
          clustering.addCluster(cluster);
        }
      }
      clusterOrderResult.addChildResult(clustering);
    }
  }

  /**
   * Update the mib values of SteepDownAreas, and remove obsolete areas.
   * 
   * @param mib Maximum in-between value
   * @param sdaset Set of steep down areas.
   */
  private void updateFilterSDASet(double mib, List<SteepDownArea> sdaset) {
    Iterator<SteepDownArea> iter = sdaset.iterator();
    while(iter.hasNext()) {
      SteepDownArea sda = iter.next();
      if(sda.startDouble * ixi <= mib) {
        iter.remove();
      }
      else {
        // Update
        sda.mib = Math.max(sda.mib, mib);
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
    double xi = getParameterXi(config);
    if(config.hasErrors()) {
      return null;
    }
    return new OPTICS<O, D>(distanceFunction, epsilon, minpts, xi);
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

  /**
   * Get the xi parameter value.
   * 
   * @param config Parameterization
   * @return xi parameter value
   */
  protected static double getParameterXi(Parameterization config) {
    final DoubleParameter param = new DoubleParameter(XI_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.CLOSE, 1.0, IntervalConstraint.IntervalBoundary.OPEN), true);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 0.0;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Data structure to represent a steep-down-area for the xi method.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private static class SteepDownArea {
    int startindex;

    int endindex;

    double startDouble;

    double mib;

    /**
     * Constructor
     * 
     * @param startindex
     * @param endindex
     * @param startDouble
     * @param mib
     */
    public SteepDownArea(int startindex, int endindex, double startDouble, double mib) {
      super();
      this.startindex = startindex;
      this.endindex = endindex;
      this.startDouble = startDouble;
      this.mib = mib;
    }
  }
}