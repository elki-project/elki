/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.clustering.optics;

import java.util.*;

import elki.clustering.ClusteringAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.OPTICSModel;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.datastore.DoubleDataStore;
import elki.database.ids.*;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MathUtil;
import elki.result.IterableResult;
import elki.result.Metadata;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ClassParameter;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;

/**
 * Extract clusters from OPTICS plots using the original &xi; (Xi) extraction,
 * which defines steep areas if the reachability drops below 1-&xi;,
 * respectively increases to 1+&xi;, of the current value, then constructs
 * valleys that begin with a steep down, and end with a matching steep up area.
 * <p>
 * Note: this implementation includes an additional filter step that prunes
 * elements from a steep up area that don't have the predecessor in the cluster.
 * This removes a popular type of artifacts.
 * <p>
 * Reference:
 * <p>
 * Mihael Ankerst, Markus M. Breunig, Hans-Peter Kriegel, Jörg Sander<br>
 * OPTICS: Ordering Points to Identify the Clustering Structure<br>
 * Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)
 * <p>
 * Filtering technique:
 * <p>
 * Erich Schubert, Michael Gertz<br>
 * Improving the Cluster Structure Extracted from OPTICS Plots<br>
 * Proc. Lernen, Wissen, Daten, Analysen (LWDA 2018)
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @composed - runs - OPTICSTypeAlgorithm
 * @assoc - reads - ClusterOrder
 * @navassoc - produces - SteepAreaResult
 * @navassoc - produces - Clustering
 * @composed - - - SteepScanPosition
 */
@Title("OPTICS Xi Cluster Extraction")
@Reference(authors = "Mihael Ankerst, Markus M. Breunig, Hans-Peter Kriegel, Jörg Sander", //
    title = "OPTICS: Ordering Points to Identify the Clustering Structure", //
    booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)", //
    url = "https://doi.org/10.1145/304181.304187", //
    bibkey = "DBLP:conf/sigmod/AnkerstBKS99")
@Reference(authors = "Erich Schubert, Michael Gertz", //
    title = "Improving the Cluster Structure Extracted from OPTICS Plots", //
    booktitle = "Proc. Lernen, Wissen, Daten, Analysen (LWDA 2018)", //
    url = "http://ceur-ws.org/Vol-2191/paper37.pdf", //
    bibkey = "DBLP:conf/lwa/SchubertG18")
@Priority(Priority.RECOMMENDED)
public class OPTICSXi implements ClusteringAlgorithm<Clustering<OPTICSModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(OPTICSXi.class);

  /**
   * The actual algorithm we use.
   */
  OPTICSTypeAlgorithm optics;

  /**
   * Xi parameter
   */
  double xi;

  /**
   * Disable the predecessor correction.
   */
  boolean nocorrect;

  /**
   * Keep the steep areas, for visualization.
   */
  boolean keepsteep;

  /**
   * Constructor.
   *
   * @param optics OPTICS algorithm to use
   * @param xi Xi value
   * @param nocorrect Disable the predecessor correction
   * @param keepsteep Keep the steep areas for visualization
   */
  public OPTICSXi(OPTICSTypeAlgorithm optics, double xi, boolean nocorrect, boolean keepsteep) {
    super();
    this.optics = optics;
    this.xi = xi;
    this.nocorrect = nocorrect;
    this.keepsteep = keepsteep;
  }

  /**
   * Constructor.
   *
   * @param optics OPTICS algorithm to use
   * @param xi Xi value
   */
  public OPTICSXi(OPTICSTypeAlgorithm optics, double xi) {
    this(optics, xi, false, false);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return optics.getInputTypeRestriction();
  }

  @Override
  public Clustering<OPTICSModel> autorun(Database database) {
    return run(optics.autorun(database));
  }

  /**
   * Process the cluster order of an OPTICS clustering.
   *
   * @param clusterOrder cluster order result
   * @return Clustering
   */
  public Clustering<OPTICSModel> run(ClusterOrder clusterOrder) {
    return extractClusters(clusterOrder, 1.0 - xi, optics.getMinPts());
  }

  /**
   * Extract clusters from a cluster order result.
   *
   * @param clusterOrderResult cluster order result
   * @param ixi Parameter 1 - Xi
   * @param minpts Parameter minPts
   */
  private Clustering<OPTICSModel> extractClusters(ClusterOrder clusterOrderResult, double ixi, int minpts) {
    ArrayDBIDs clusterOrder = clusterOrderResult.ids;
    DBIDArrayIter iter = clusterOrder.iter();
    DoubleDataStore reach = clusterOrderResult.reachability;
    ClusterHierarchyBuilder builder = new ClusterHierarchyBuilder(clusterOrder);

    double mib = 0.0;
    List<SteepArea> salist = keepsteep ? new ArrayList<>() : null;
    List<SteepDownArea> sdaset = new ArrayList<>();

    for(SteepScanPosition scan = new SteepScanPosition(clusterOrderResult); scan.hasNext();) {
      // Update maximum-inbetween
      mib = MathUtil.max(mib, scan.getReachability());
      // Xi-steep down area
      if(scan.steepDown(ixi)) {
        // Update mib values with current mib and filter
        updateFilterSDASet(mib, sdaset, ixi);
        final double startval = scan.getReachability();
        mib = 0.;
        int startsteep = scan.index, endsteep = scan.index;
        for(scan.next(); scan.hasNext(); scan.next()) {
          // still steep - continue.
          if(scan.steepDown(ixi)) {
            endsteep = scan.index;
          }
          // Not going downward at all - stop here.
          // Always stop looking after minpts "flat" steps.
          else if(!scan.steepDown(1.0) || scan.index - endsteep > minpts) {
            break;
          }
        }
        final SteepDownArea sda = new SteepDownArea(startsteep, endsteep, startval, 0);
        if(LOG.isDebuggingFinest()) {
          LOG.debugFinest("New steep down area: " + sda.toString());
        }
        sdaset.add(sda);
        if(salist != null) {
          salist.add(sda);
        }
        continue;
      }
      // Xi-steep up area
      if(scan.steepUp(ixi)) {
        // Update mib values with current mib and filter
        updateFilterSDASet(mib, sdaset, ixi);
        // Compute steep-up area
        int startsteep = scan.index, endsteep = scan.index;
        {
          mib = scan.getReachability();
          double esuccr = scan.getNextReachability();
          // Find end of steep-up-area, eventually updating mib again
          while(!Double.isInfinite(esuccr) && scan.hasNext()) {
            scan.next();
            // still steep - continue.
            if(scan.steepUp(ixi)) {
              endsteep = scan.index;
              mib = scan.getReachability();
              esuccr = scan.getNextReachability();
            }
            // Not going upward - stop here.
            // Stop looking after minpts non-up steps.
            else if(!scan.steepUp(1.0) || scan.index - endsteep > minpts) {
              break;
            }
          }
          if(LOG.isDebuggingFinest()) {
            LOG.debugFinest("New steep up area: " + startsteep + "-" + endsteep + " max:" + esuccr);
          }
          if(salist != null) {
            salist.add(new SteepUpArea(startsteep, endsteep, esuccr));
          }
          if(Double.isInfinite(esuccr)) {
            scan.next();
          }
        }
        // Validate and computer clusters
        ListIterator<SteepDownArea> sdaiter = sdaset.listIterator(sdaset.size());
        // Iterate backwards for correct hierarchy generation.
        while(sdaiter.hasPrevious()) {
          SteepDownArea sda = sdaiter.previous();
          // By default, clusters cover both the steep up and steep down area
          int cstart = sda.getStartIndex(), cend = endsteep;
          // MST-based filtering technique of Schubert:
          // ensure that the predecessor is in the current cluster.
          // This filter removes common artifacts from the Xi method
          cend = nocorrect ? cend : predecessorFilter(clusterOrderResult, cstart, cend, iter);
          // End of steep-up region (use next value)
          double eU = cend + 1 < clusterOrderResult.size() ? reach.doubleValue(iter.seek(cend + 1)) : Double.POSITIVE_INFINITY;
          // Condition 3b: maximum-in-between < end-of-steep-up * ixi
          if(LOG.isDebuggingFinest()) {
            LOG.debugFinest("Comparing: eU=" + eU + " SDA: " + sda.toString());
          }
          if(sda.mib > MathUtil.min(sda.maximum, eU) * ixi) {
            if(LOG.isDebuggingFinest()) {
              LOG.debugFinest("mib = " + sda.mib + " > min(sD, eU) * ixi  = " + MathUtil.min(sda.maximum, eU) * ixi);
            }
            continue;
          }
          // However, we sometimes have to adjust this (Condition 4):
          {
            // Case b)
            if(sda.maximum * ixi >= eU) {
              while(cstart < cend && reach.doubleValue(iter.seek(cstart + 1)) * ixi > eU) {
                cstart++;
              }
            }
            // Case c)
            else if(eU * ixi >= sda.maximum) {
              while(cend > cstart && reach.doubleValue(iter.seek(cend)) * ixi > sda.maximum) {
                cend--;
              }
            }
            // Case a) is the default
          }
          // Filter again, mostly in case we modified cstart
          cend = nocorrect ? cend : predecessorFilter(clusterOrderResult, cstart, cend, iter);
          // Condition 3a: obey minpts
          if(cend - cstart + 1 < minpts) {
            if(LOG.isDebuggingFinest()) {
              LOG.debugFinest("MinPts not satisfied.");
            }
            continue;
          }
          // Build the cluster
          builder.addCluster(iter, cstart, cend);
        }
        continue;
      }
      // Flat - advance anyway.
      scan.next();
    }
    Clustering<OPTICSModel> clustering = builder.build(clusterOrderResult, iter);
    Metadata.hierarchyOf(clustering).addChild(clusterOrderResult);
    if(salist != null) {
      Metadata.hierarchyOf(clusterOrderResult).addChild(new SteepAreaResult(salist));
    }
    return clustering;
  }

  /**
   * Filtering step to remove bad tailing points from the clusters.
   * <p>
   * Erich Schubert, Michael Gertz<br>
   * Improving the Cluster Structure Extracted from OPTICS Plots<br>
   * Proc. Lernen, Wissen, Daten, Analysen (LWDA 2018)
   *
   * @param clusterOrderResult Cluster order
   * @param cstart Cluster start
   * @param cend Cluster end
   * @param tmp Cluster order iterator
   * @return New end position
   */
  private static int predecessorFilter(ClusterOrder clusterOrderResult, int cstart, int cend, DBIDArrayIter tmp) {
    if(cend == clusterOrderResult.size()) {
      return cend;
    }
    double startval = clusterOrderResult.reachability.doubleValue(tmp.seek(cstart));
    DBIDVar tmp2 = null;
    simplify: while(cend > cstart) {
      tmp.seek(cend);
      // If the reachability is less than the first points', then there
      // must be some point inbetween that "reached" the current point.
      if(clusterOrderResult.reachability.doubleValue(tmp) < startval) {
        break simplify;
      }
      tmp2 = tmp2 != null ? tmp2 : DBIDUtil.newVar(); // Lazy init
      clusterOrderResult.predecessor.assignVar(tmp, tmp2);
      // "Slow" search for predecessor:
      for(int i = cstart; i < cend; i++) {
        if(DBIDUtil.equal(tmp2, tmp.seek(i))) {
          break simplify;
        }
      }
      // Not found, prune the last point
      if(LOG.isDebuggingFinest()) {
        LOG.debugFinest("Pruned one point by predecessor rule.");
      }
      --cend;
    }
    return cend;
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
      if(sda.maximum * ixi <= mib) {
        iter.remove();
      }
      else if(mib > sda.mib) {
        sda.mib = mib;
      }
    }
  }

  /**
   * Class to build the hierarchical clustering result structure.
   * 
   * @author Erich Schubert
   */
  private static class ClusterHierarchyBuilder {
    /**
     * ELKI clustering object
     */
    Clustering<OPTICSModel> clustering;

    /**
     * Current "unattached" clusters.
     */
    HashSet<Cluster<OPTICSModel>> curclusters;

    /**
     * Unclaimed objects that will be assigned to a top level or noise cluster
     * in the end.
     */
    HashSetModifiableDBIDs unclaimedids;

    /**
     * Constructor.
     *
     * @param ids All object ids (arbitrary order)
     */
    public ClusterHierarchyBuilder(DBIDs ids) {
      clustering = new Clustering<>();
      curclusters = new HashSet<>();
      unclaimedids = DBIDUtil.newHashSet(ids);
    }

    /**
     * Build a cluster object.
     *
     * @param tmp DBID array iterator
     * @param cstart Interval start
     * @param cend Interval end
     */
    private void addCluster(DBIDArrayIter tmp, int cstart, int cend) {
      ModifiableDBIDs dbids = DBIDUtil.newArray();
      for(int idx = cstart; idx <= cend; idx++) {
        tmp.seek(idx);
        // Collect only unclaimed IDs.
        if(unclaimedids.remove(tmp)) {
          dbids.add(tmp);
        }
      }
      if(LOG.isDebuggingFine()) {
        LOG.debugFine("Found cluster with " + dbids.size() + " new objects, length " + (cend - cstart + 1));
      }
      OPTICSModel model = new OPTICSModel(cstart, cend);
      Cluster<OPTICSModel> cluster = new Cluster<>("Cluster_" + cstart + "_" + cend, dbids, model);
      // Build the hierarchy
      for(Iterator<Cluster<OPTICSModel>> iter = curclusters.iterator(); iter.hasNext();) {
        Cluster<OPTICSModel> clus = iter.next();
        OPTICSModel omodel = clus.getModel();
        if(model.getStartIndex() <= omodel.getStartIndex() && omodel.getEndIndex() <= model.getEndIndex()) {
          clustering.addChildCluster(cluster, clus);
          iter.remove(); // Not a top-level cluster
        }
      }
      curclusters.add(cluster);
    }

    /**
     * Build the main clustering result.
     *
     * @param clusterOrder Cluster order
     * @param iter Array iterator for the cluster order
     * @return Clustering
     */
    private Clustering<OPTICSModel> build(ClusterOrder clusterOrder, DBIDArrayIter iter) {
      if(!unclaimedids.isEmpty()) {
        boolean noise = Double.isInfinite(clusterOrder.getReachability(iter.seek(clusterOrder.size() - 1)));
        Cluster<OPTICSModel> allcluster = new Cluster<>(noise ? "Noise" : "Cluster", unclaimedids, noise, new OPTICSModel(0, clusterOrder.size() - 1));
        for(Cluster<OPTICSModel> cluster : curclusters) {
          clustering.addChildCluster(allcluster, cluster);
        }
        clustering.addToplevelCluster(allcluster);
      }
      else {
        for(Cluster<OPTICSModel> cluster : curclusters) {
          clustering.addToplevelCluster(cluster);
        }
      }
      Metadata.of(clustering).setLongName("OPTICS Xi-Clusters");
      return clustering;
    }
  }

  /**
   * Position when scanning for steep areas
   *
   * @author Erich Schubert
   */
  private static class SteepScanPosition {
    /**
     * Cluster order
     */
    ClusterOrder co;

    /**
     * Current position
     */
    int index;

    /**
     * Variable for accessing.
     */
    private DBIDArrayIter cur, next;

    /**
     * Progress for logging.
     */
    private FiniteProgress prog;

    /**
     * Constructor.
     *
     * @param co Cluster order
     */
    public SteepScanPosition(ClusterOrder co) {
      super();
      this.co = co;
      this.index = 0;
      this.cur = co.ids.iter();
      this.next = co.ids.iter();
      if(next.valid()) {
        next.advance();
      }
      this.prog = LOG.isVerbose() ? new FiniteProgress("OPTICS Xi cluster extraction", co.size(), LOG) : null;
    }

    /**
     * Advance to the next entry
     */
    public void next() {
      index++;
      cur.advance();
      next.advance();
      LOG.incrementProcessed(prog);
    }

    /**
     * Test whether there is a next value.
     *
     * @return end of cluster order
     */
    public boolean hasNext() {
      if(index == co.size()) {
        LOG.ensureCompleted(prog);
      }
      return index < co.size();
    }

    /**
     * Test for a steep up point.
     *
     * @param ixi steepness factor (1-xi)
     * @return truth value
     */
    public boolean steepUp(double ixi) {
      final double creach = co.reachability.doubleValue(cur);
      return creach < Double.POSITIVE_INFINITY && //
          (!next.valid() || creach <= co.reachability.doubleValue(next) * ixi);
    }

    /**
     * Test for a steep down area.
     *
     * @param ixi Steepness factor (1-xi)
     * @return truth value
     */
    public boolean steepDown(double ixi) {
      final double nreach = getNextReachability();
      return nreach < Double.POSITIVE_INFINITY && //
          nreach <= co.reachability.doubleValue(cur) * ixi;
    }

    /**
     * Get current reachability.
     *
     * @return Reachability
     */
    public double getReachability() {
      return co.reachability.doubleValue(cur);
    }

    /**
     * Get current reachability.
     *
     * @return Reachability
     */
    public double getNextReachability() {
      return next.valid() ? co.reachability.doubleValue(next) : Double.POSITIVE_INFINITY;
    }
  }

  /**
   * Data structure to represent a steep-down-area for the xi method.
   *
   * @author Erich Schubert
   */
  public abstract static class SteepArea {
    /**
     * Start index of steep area
     */
    int startindex;

    /**
     * End index of steep area
     */
    int endindex;

    /**
     * Maximum value
     */
    double maximum;

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
  }

  /**
   * Data structure to represent a steep-down-area for the xi method.
   *
   * @author Erich Schubert
   */
  public static class SteepDownArea extends SteepArea {
    /**
     * Maximum in-between value (updated in
     * {@link OPTICSXi#updateFilterSDASet}).
     */
    double mib;

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

    @Override
    public String toString() {
      return "SteepDownArea(" + getStartIndex() + " - " + getEndIndex() + ", max=" + maximum + ", mib=" + mib + ")";
    }
  }

  /**
   * Data structure to represent a steep-down-area for the xi method.
   *
   * @author Erich Schubert
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
      return "SteepUpArea(" + getStartIndex() + " - " + getEndIndex() + ", max=" + maximum + ")";
    }
  }

  /**
   * Result containing the xi-steep areas.
   *
   * @author Erich Schubert
   *
   * @has - - - SteepArea
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
    public Iterator<SteepArea> iterator() {
      return areas.iterator();
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter to specify the actual OPTICS algorithm to use.
     */
    public static final OptionID XIALG_ID = new OptionID("opticsxi.algorithm", "The actual OPTICS-type algorithm to use.");

    /**
     * Parameter to specify the steepness threshold.
     */
    public static final OptionID XI_ID = new OptionID("opticsxi.xi", "Threshold for the steepness requirement.");

    /**
     * Parameter to disable the correction function.
     */
    public static final OptionID NOCORRECT_ID = new OptionID("opticsxi.nocorrect", "Disable the predecessor correction.");

    /**
     * Parameter to keep the steep areas
     */
    public static final OptionID KEEPSTEEP_ID = new OptionID("opticsxi.keepsteep", "Keep the steep up/down areas of the plot.");

    protected OPTICSTypeAlgorithm optics;

    protected double xi = 0.;

    protected boolean nocorrect = false, keepsteep = false;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(XI_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE)//
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .grab(config, x -> xi = x);
      new ClassParameter<OPTICSTypeAlgorithm>(XIALG_ID, OPTICSTypeAlgorithm.class, OPTICSHeap.class) //
          .grab(config, x -> optics = x);
      new Flag(NOCORRECT_ID).grab(config, x -> nocorrect = x);
      new Flag(KEEPSTEEP_ID).grab(config, x -> keepsteep = x);
    }

    @Override
    public OPTICSXi make() {
      return new OPTICSXi(optics, xi, nocorrect, keepsteep);
    }
  }
}
