package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.ArrayList;
import java.util.Collection;
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
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
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
  @Override
  protected ClusterOrderResult<D> runInTime(Database<O> database) {
    final FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("OPTICS", database.size(), logger) : null;

    // Default value is infinite distance
    if(epsilon == null) {
      epsilon = getDistanceFunction().getDistanceFactory().infiniteDistance();
    }
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
        logger.verbose("Extracting clusters with Xi: " + (1. - ixi));
        ClusterOrderResult<DoubleDistance> distanceClusterOrder = ClassGenericsUtil.castWithGenericsOrNull(ClusterOrderResult.class, clusterOrder);
        extractClusters(distanceClusterOrder, database, ixi, minpts);
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

  // TODO: resolve handling of the last point in the cluster order
  public static <N extends NumberDistance<N, ?>> void extractClusters(ClusterOrderResult<N> clusterOrderResult, Database<?> database, double ixi, int minpts) {
    // TODO: add progress?
    List<ClusterOrderEntry<N>> clusterOrder = clusterOrderResult.getClusterOrder();
    double mib = 0.0;
    // TODO: make it configurable to keep this list; this is mostly useful for
    // visualization
    List<SteepArea> salist = new ArrayList<SteepArea>();
    List<SteepDownArea> sdaset = new java.util.Vector<SteepDownArea>();
    ModifiableHierarchy<Cluster<OPTICSModel>> hier = new HierarchyHashmapList<Cluster<OPTICSModel>>();
    HashSet<Cluster<OPTICSModel>> curclusters = new HashSet<Cluster<OPTICSModel>>();
    HashSetModifiableDBIDs unclaimedids = DBIDUtil.newHashSet(database.getIDs());

    SteepScanPosition<N> scan = new SteepScanPosition<N>(clusterOrder);
    while(scan.hasNext()) {
      final int curpos = scan.index;
      // Update maximum-inbetween
      mib = Math.max(mib, scan.ecurr.getReachability().doubleValue());
      // The last point cannot be the start of a steep area.
      if(scan.esucc != null) {
        // Xi-steep down area
        if(scan.steepDown(ixi)) {
          // Update mib values with current mib and filter
          updateFilterSDASet(mib, sdaset, ixi);
          final double startval = scan.ecurr.getReachability().doubleValue();
          int startsteep = scan.index;
          int endsteep = Math.min(scan.index + 1, clusterOrder.size());
          {
            while(scan.hasNext()) {
              scan.next();
              // not going downward at all - stop here.
              if(!scan.steepDown(1.0)) {
                break;
              }
              // still steep - continue.
              if(scan.steepDown(ixi)) {
                endsteep = Math.min(scan.index + 1, clusterOrder.size());
              }
              else {
                // Stop looking after minpts "flat" steps.
                if(scan.index - endsteep > minpts) {
                  break;
                }
              }
            }
          }
          mib = clusterOrder.get(endsteep).getReachability().doubleValue();
          final SteepDownArea sda = new SteepDownArea(startsteep, endsteep, startval, 0);
          if(logger.isDebuggingFinest()) {
            logger.debugFinest("Xi " + sda.toString());
          }
          sdaset.add(sda);
          if(salist != null) {
            salist.add(sda);
          }
          continue;
        }
        else
        // Xi-steep up area
        if(scan.steepUp(ixi)) {
          // Update mib values with current mib and filter
          updateFilterSDASet(mib, sdaset, ixi);
          final SteepUpArea sua;
          // Compute steep-up area
          {
            int startsteep = scan.index;
            int endsteep = scan.index + 1;
            mib = scan.ecurr.getReachability().doubleValue();
            double esuccr = scan.esucc.getReachability().doubleValue();
            // There is nothing higher than infinity
            if(!Double.isInfinite(esuccr)) {
              // find end of steep-up-area, eventually updating mib again
              while(scan.hasNext()) {
                scan.next();
                // not going upward - stop here.
                if(!scan.steepUp(1.0)) {
                  break;
                }
                // still steep - continue.
                if(scan.steepUp(ixi)) {
                  endsteep = Math.min(scan.index + 1, clusterOrder.size());
                  mib = scan.ecurr.getReachability().doubleValue();
                  esuccr = scan.esucc.getReachability().doubleValue();
                }
                else {
                  // Stop looking after minpts non-up steps.
                  if(scan.index - endsteep > minpts) {
                    break;
                  }
                }
              }
            }
            sua = new SteepUpArea(startsteep, endsteep, esuccr);
            if(logger.isDebuggingFinest()) {
              logger.debugFinest("Xi " + sua.toString());
            }
            if(salist != null) {
              salist.add(sua);
            }
          }
          // Validate and computer clusters
          // logger.debug("SDA size:"+sdaset.size()+" "+sdaset);
          ListIterator<SteepDownArea> sdaiter = sdaset.listIterator(sdaset.size());
          // Iterate backwards for correct hierarchy generation.
          while(sdaiter.hasPrevious()) {
            SteepDownArea sda = sdaiter.previous();
            // logger.debug("Comparing: eU="+mib.doubleValue()+" SDA: "+sda.toString());
            // Condition 3b: end-of-steep-up > maximum-in-between lower
            if(mib * ixi < sda.getMib()) {
              continue;
            }
            // By default, clusters cover both the steep up and steep down area
            int cstart = sda.getStartIndex();
            int cend = sua.getEndIndex();
            // However, we sometimes have to adjust this (Condition 4):
            {
              // Case b)
              if(sda.getMaximum() * ixi >= sua.getMaximum()) {
                while(cstart < sda.getEndIndex()) {
                  if(clusterOrder.get(cstart + 1).getReachability().doubleValue() > sua.getMaximum()) {
                    cstart++;
                  }
                  else {
                    break;
                  }
                }
              }
              // Case c)
              else if(sua.getMaximum() * ixi >= sda.getMaximum()) {
                while(cend > sua.getStartIndex()) {
                  if(clusterOrder.get(cend - 1).getReachability().doubleValue() > sda.getMaximum()) {
                    cend--;
                  }
                  else {
                    break;
                  }
                }
              }
              // Case a) is the default
            }
            // Condition 3a: obey minpts
            if(cend - cstart + 1 < minpts) {
              continue;
            }
            // Build the cluster
            ModifiableDBIDs dbids = DBIDUtil.newArray();
            for(int idx = cstart; idx <= cend; idx++) {
              final DBID dbid = clusterOrder.get(idx).getID();
              // Collect only unclaimed IDs.
              if(unclaimedids.remove(dbid)) {
                dbids.add(dbid);
              }
            }
            if(logger.isDebuggingFine()) {
              logger.debugFine("Found cluster with " + dbids.size() + " new objects, length " + (cstart - cend + 1));
            }
            OPTICSModel model = new OPTICSModel(cstart, cend);
            Cluster<OPTICSModel> cluster = new Cluster<OPTICSModel>("Cluster_" + cstart + "_" + cend, dbids, model, hier);
            // Build the hierarchy
            {
              Iterator<Cluster<OPTICSModel>> iter = curclusters.iterator();
              while(iter.hasNext()) {
                Cluster<OPTICSModel> clus = iter.next();
                OPTICSModel omodel = clus.getModel();
                if(model.getStartIndex() <= omodel.getStartIndex() && omodel.getEndIndex() <= model.getEndIndex()) {
                  hier.add(cluster, clus);
                  iter.remove();
                }
              }
            }
            curclusters.add(cluster);
          }
        }
      }
      // Make sure to advance at least one step
      if(curpos == scan.index) {
        scan.next();
      }
    }
    if(curclusters.size() > 0 || unclaimedids.size() > 0) {
      final Clustering<OPTICSModel> clustering = new Clustering<OPTICSModel>("OPTICS Xi-Clusters", "optics");
      if(unclaimedids.size() > 0) {
        final Cluster<OPTICSModel> allcluster;
        if(clusterOrder.get(clusterOrder.size() - 1).getReachability().isInfiniteDistance()) {
          allcluster = new Cluster<OPTICSModel>("Noise", unclaimedids, true, new OPTICSModel(0, clusterOrder.size() - 1), hier);
        }
        else {
          allcluster = new Cluster<OPTICSModel>("Cluster", unclaimedids, new OPTICSModel(0, clusterOrder.size() - 1), hier);
        }
        for(Cluster<OPTICSModel> cluster : curclusters) {
          hier.add(allcluster, cluster);
        }
        clustering.addCluster(allcluster);
      }
      else {
        for(Cluster<OPTICSModel> cluster : curclusters) {
          clustering.addCluster(cluster);
        }
      }
      clusterOrderResult.addChildResult(clustering);
      if(salist != null) {
        clusterOrderResult.addChildResult(new SteepAreaResult(salist));
      }
    }
  }

  /**
   * Update the mib values of SteepDownAreas, and remove obsolete areas.
   * 
   * @param mib Maximum in-between value
   * @param sdaset Set of steep down areas.
   */
  private static void updateFilterSDASet(double mib, List<SteepDownArea> sdaset, double ixi) {
    Iterator<SteepDownArea> iter = sdaset.iterator();
    while(iter.hasNext()) {
      SteepDownArea sda = iter.next();
      if(sda.getMaximum() * ixi <= mib) {
        iter.remove();
      }
      else {
        // Update
        sda.setMib(Math.max(sda.getMib(), mib));
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
    final DistanceParameter<D> param = new DistanceParameter<D>(EPSILON_ID, distanceFunction, true);
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
   * Position when scanning for steep areas
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exlude
   * 
   * @param <N> Distance type
   */
  private static class SteepScanPosition<N extends NumberDistance<N, ?>> {
    /**
     * Cluster order
     */
    List<ClusterOrderEntry<N>> co;

    /**
     * Current position
     */
    int index = 0;

    /**
     * Current entry
     */
    ClusterOrderEntry<N> ecurr = null;

    /**
     * Next entry
     */
    ClusterOrderEntry<N> esucc = null;

    /**
     * Constructor.
     * 
     * @param co Cluster order
     */
    public SteepScanPosition(List<ClusterOrderEntry<N>> co) {
      super();
      this.co = co;
      this.ecurr = (co.size() >= 1) ? co.get(0) : null;
      this.esucc = (co.size() >= 2) ? co.get(1) : null;
    }

    /**
     * Advance to the next entry
     */
    public void next() {
      index++;
      ecurr = esucc;
      if(index + 1 < co.size()) {
        esucc = co.get(index + 1);
      }
    }

    /**
     * Test whether there is a next value.
     * 
     * @return end of cluster order
     */
    public boolean hasNext() {
      return index + 1 < co.size();
    }

    /**
     * Test for a steep up point.
     * 
     * @param ixi steepness factor (1-xi)
     * @return truth value
     */
    public boolean steepUp(double ixi) {
      if(ecurr.getReachability().isInfiniteDistance()) {
        return false;
      }
      if(esucc == null) {
        return true;
      }
      return ecurr.getReachability().doubleValue() <= esucc.getReachability().doubleValue() * ixi;
    }

    /**
     * Test for a steep down area.
     * 
     * @param ixi Steepness factor (1-xi)
     * @return truth value
     */
    public boolean steepDown(double ixi) {
      if(esucc == null) {
        return false;
      }
      if(esucc.getReachability().isInfiniteDistance()) {
        return false;
      }
      return ecurr.getReachability().doubleValue() * ixi >= esucc.getReachability().doubleValue();
    }
  }

  /**
   * Data structure to represent a steep-down-area for the xi method.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class SteepArea {
    /**
     * Start index of down-area
     */
    private int startindex;

    /**
     * End index of down-area
     */
    private int endindex;

    /**
     * Maximum value
     */
    private double maximum;

    /**
     * Constructor.
     * 
     * @param startindex Start index
     * @param endindex End index
     * @param maximum Maximum value
     */
    public SteepArea(int startindex, int endindex, double maximum) {
      super();
      this.startindex = startindex;
      this.endindex = endindex;
      this.maximum = maximum;
    }

    /**
     * Start index
     * 
     * @return the start index
     */
    public int getStartIndex() {
      return startindex;
    }

    /**
     * End index
     * 
     * @return the end index
     */
    public int getEndIndex() {
      return endindex;
    }

    /**
     * Get the start value = maximum value
     * 
     * @return the starting value
     */
    public double getMaximum() {
      return maximum;
    }

  }

  /**
   * Data structure to represent a steep-down-area for the xi method.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class SteepDownArea extends SteepArea {
    /**
     * Maximum in-between value
     */
    private double mib;

    /**
     * Constructor
     * 
     * @param startindex Start index
     * @param endindex End index
     * @param startDouble Start value (= maximum)
     * @param mib Maximum inbetween value (for Xi extraction; modifiable)
     */
    public SteepDownArea(int startindex, int endindex, double startDouble, double mib) {
      super(startindex, endindex, startDouble);
      this.mib = mib;
    }

    /**
     * Get the maximum in-between value
     * 
     * @return the mib value
     */
    public double getMib() {
      return mib;
    }

    /**
     * Update the maximum in-between value
     * 
     * @param mib the mib to set
     */
    public void setMib(double mib) {
      this.mib = mib;
    }

    @Override
    public String toString() {
      return "SteepDownArea(" + getStartIndex() + " - " + getEndIndex() + ", max=" + getMaximum() + ", mib=" + mib + ")";
    }
  }

  /**
   * Data structure to represent a steep-down-area for the xi method.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class SteepUpArea extends SteepArea {
    /**
     * Constructor
     * 
     * @param startindex Starting index
     * @param endindex End index
     * @param endDouble End value (= maximum)
     */
    public SteepUpArea(int startindex, int endindex, double endDouble) {
      super(startindex, endindex, endDouble);
    }

    @Override
    public String toString() {
      return "SteepUpArea(" + getStartIndex() + " - " + getEndIndex() + ", max=" + getMaximum() + ")";
    }
  }

  /**
   * Result containing the chi-steep areas.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has SteepArea
   */
  public static class SteepAreaResult implements IterableResult<SteepArea> {
    /**
     * Storage
     */
    Collection<SteepArea> areas;

    /**
     * Constructor.
     * 
     * @param areas Areas
     */
    public SteepAreaResult(Collection<SteepArea> areas) {
      super();
      this.areas = areas;
    }

    @Override
    public String getLongName() {
      return "Chi-Steep areas";
    }

    @Override
    public String getShortName() {
      return "chi-steep-areas";
    }

    @Override
    public Iterator<SteepArea> iterator() {
      return areas.iterator();
    }
  }
}