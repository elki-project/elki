/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

import java.util.*;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.OPTICSModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Extract clusters from OPTICS Plots using the original Xi extraction.
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
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICSXi")
@Priority(Priority.RECOMMENDED)
public class OPTICSXi extends AbstractAlgorithm<Clustering<OPTICSModel>> implements ClusteringAlgorithm<Clustering<OPTICSModel>> {
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

  public Clustering<OPTICSModel> run(Database database, Relation<?> relation) {
    // TODO: ensure we are using the same relation?
    ClusterOrder opticsresult = optics.run(database);
    if(LOG.isVerbose()) {
      LOG.verbose("Extracting clusters with Xi: " + xi);
    }
    return extractClusters(opticsresult, relation, 1.0 - xi, optics.getMinPts());
  }

  /**
   * Extract clusters from a cluster order result.
   *
   * @param clusterOrderResult cluster order result
   * @param relation Relation
   * @param ixi Parameter 1 - Xi
   * @param minpts Parameter minPts
   */
  private Clustering<OPTICSModel> extractClusters(ClusterOrder clusterOrderResult, Relation<?> relation, double ixi, int minpts) {
    ArrayDBIDs clusterOrder = clusterOrderResult.ids;
    DoubleDataStore reach = clusterOrderResult.reachability;

    DBIDArrayIter tmp = clusterOrder.iter();
    DBIDVar tmp2 = DBIDUtil.newVar();
    double mib = 0.0;
    List<SteepArea> salist = keepsteep ? new ArrayList<SteepArea>() : null;
    List<SteepDownArea> sdaset = new ArrayList<>();
    final Clustering<OPTICSModel> clustering = new Clustering<>("OPTICS Xi-Clusters", "optics");
    HashSet<Cluster<OPTICSModel>> curclusters = new HashSet<>();
    HashSetModifiableDBIDs unclaimedids = DBIDUtil.newHashSet(relation.getDBIDs());

    FiniteProgress scanprog = LOG.isVerbose() ? new FiniteProgress("OPTICS Xi cluster extraction", clusterOrder.size(), LOG) : null;
    for(SteepScanPosition scan = new SteepScanPosition(clusterOrderResult); scan.hasNext();) {
      if(scanprog != null) {
        scanprog.setProcessed(scan.index, LOG);
      }
      // Update maximum-inbetween
      mib = MathUtil.max(mib, scan.getReachability());
      // The last point cannot be the start of a steep area.
      if(!scan.next.valid()) {
        break;
      }
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
            continue;
          }
          // Not going downward at all - stop here.
          // Always stop looking after minpts "flat" steps.
          if(!scan.steepDown(1.0) || scan.index - endsteep > minpts) {
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
        final SteepUpArea sua;
        // Compute steep-up area
        {
          int startsteep = scan.index, endsteep = scan.index;
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
              continue;
            }
            // Not going upward - stop here.
            // Stop looking after minpts non-up steps.
            if(!scan.steepUp(1.0) || scan.index - endsteep > minpts) {
              break;
            }
          }
          if(Double.isInfinite(esuccr)) {
            scan.next();
          }
          sua = new SteepUpArea(startsteep, endsteep, esuccr);
          if(LOG.isDebuggingFinest()) {
            LOG.debugFinest("New steep up area: " + sua.toString());
          }
          if(salist != null) {
            salist.add(sua);
          }
        }
        // Validate and computer clusters
        // LOG.debug("SDA size:"+sdaset.size()+" "+sdaset);
        ListIterator<SteepDownArea> sdaiter = sdaset.listIterator(sdaset.size());
        // Iterate backwards for correct hierarchy generation.
        while(sdaiter.hasPrevious()) {
          SteepDownArea sda = sdaiter.previous();
          if(LOG.isDebuggingFinest()) {
            LOG.debugFinest("Comparing: eU=" + mib + " SDA: " + sda.toString());
          }
          // Condition 3b: end-of-steep-up > maximum-in-between lower
          if(mib * ixi < sda.getMib()) {
            if(LOG.isDebuggingFinest()) {
              LOG.debugFinest("mib * ixi = " + mib * ixi + " >= sda.getMib() = " + sda.getMib());
            }
            continue;
          }
          // By default, clusters cover both the steep up and steep down area
          int cstart = sda.getStartIndex(),
              cend = MathUtil.min(sua.getEndIndex(), clusterOrder.size() - 1);
          // However, we sometimes have to adjust this (Condition 4):
          {
            // Case b)
            if(sda.getMaximum() * ixi >= sua.getMaximum()) {
              while(cstart < cend && //
                  reach.doubleValue(tmp.seek(cstart + 1)) > sua.getMaximum()) {
                cstart++;
              }
            }
            // Case c)
            else if(sua.getMaximum() * ixi >= sda.getMaximum()) {
              while(cend > cstart && //
                  reach.doubleValue(tmp.seek(cend - 1)) > sda.getMaximum()) {
                cend--;
              }
            }
            // Case a) is the default
          }
          // MST-based filtering technique of Schubert:
          // ensure that the predecessor is in the current cluster.
          // This filter removes common artifacts from the Xi method
          if(!nocorrect) {
            double startval = clusterOrderResult.reachability.doubleValue(tmp.seek(cstart));
            simplify: while(cend > cstart) {
              tmp.seek(cend);
              // If the reachability is less than the first points', then there
              // must be some point inbetween that "reached" the current point.
              if(clusterOrderResult.reachability.doubleValue(tmp) < startval) {
                break simplify;
              }
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
          }
          // Condition 3a: obey minpts
          if(cend - cstart + 1 < minpts) {
            if(LOG.isDebuggingFinest()) {
              LOG.debugFinest("MinPts not satisfied.");
            }
            continue;
          }
          // Build the cluster
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
        continue;
      }
      // Flat - advance anyway.
      scan.next();
    }
    if(scanprog != null) {
      scanprog.setProcessed(clusterOrder.size(), LOG);
    }
    if(!unclaimedids.isEmpty()) {
      boolean noise = reach.doubleValue(tmp.seek(clusterOrder.size() - 1)) >= Double.POSITIVE_INFINITY;
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
    clustering.addChildResult(clusterOrderResult);
    if(salist != null) {
      clusterOrderResult.addChildResult(new SteepAreaResult(salist));
    }
    return clustering;
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
        if(mib > sda.getMib()) {
          sda.setMib(mib);
        }
      }
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return optics.getInputTypeRestriction();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
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
    }

    /**
     * Advance to the next entry
     */
    public void next() {
      index++;
      cur.advance();
      next.advance();
    }

    /**
     * Test whether there is a next value.
     *
     * @return end of cluster order
     */
    public boolean hasNext() {
      return index < co.size();
    }

    /**
     * Test for a steep up point.
     *
     * @param ixi steepness factor (1-xi)
     * @return truth value
     */
    public boolean steepUp(double ixi) {
      if(co.reachability.doubleValue(cur) >= Double.POSITIVE_INFINITY) {
        return false;
      }
      if(!next.valid()) {
        return true;
      }
      return co.reachability.doubleValue(cur) <= co.reachability.doubleValue(next) * ixi;
    }

    /**
     * Test for a steep down area.
     *
     * @param ixi Steepness factor (1-xi)
     * @return truth value
     */
    public boolean steepDown(double ixi) {
      if(!next.valid()) {
        return false;
      }
      if(co.reachability.doubleValue(next) >= Double.POSITIVE_INFINITY) {
        return false;
      }
      return co.reachability.doubleValue(cur) * ixi >= co.reachability.doubleValue(next);
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
      return (next.valid()) ? co.reachability.doubleValue(next) : Double.POSITIVE_INFINITY;
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
    private int startindex;

    /**
     * End index of steep area
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
    public String getLongName() {
      return "Xi-Steep areas";
    }

    @Override
    public String getShortName() {
      return "xi-steep-areas";
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
  public static class Parameterizer extends AbstractParameterizer {
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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter xiP = new DoubleParameter(XI_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE)//
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
      if(config.grab(xiP)) {
        xi = xiP.doubleValue();
      }

      ClassParameter<OPTICSTypeAlgorithm> opticsP = new ClassParameter<>(XIALG_ID, OPTICSTypeAlgorithm.class, OPTICSHeap.class);
      if(config.grab(opticsP)) {
        optics = opticsP.instantiateClass(config);
      }

      Flag nocorrectF = new Flag(NOCORRECT_ID);
      if(config.grab(nocorrectF)) {
        nocorrect = nocorrectF.isTrue();
      }

      Flag keepsteepF = new Flag(KEEPSTEEP_ID);
      if(config.grab(keepsteepF)) {
        keepsteep = keepsteepF.isTrue();
      }
    }

    @Override
    protected OPTICSXi makeInstance() {
      return new OPTICSXi(optics, xi, nocorrect, keepsteep);
    }
  }
}
