package de.lmu.ifi.dbs.elki.algorithm.clustering;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.OPTICSModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.HierarchyHashmapList;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.ModifiableHierarchy;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Class to handle OPTICS Xi extraction.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses OPTICSTypeAlgorithm oneway
 * @apiviz.uses ClusterOrderResult oneway
 * @apiviz.has SteepAreaResult
 * 
 * @param <N> Number distance used by OPTICS
 */
public class OPTICSXi<N extends NumberDistance<N, ?>> extends AbstractAlgorithm<Clustering<OPTICSModel>> implements ClusteringAlgorithm<Clustering<OPTICSModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(OPTICSXi.class);

  /**
   * Parameter to specify the actual OPTICS algorithm to use.
   */
  public static final OptionID XIALG_ID = OptionID.getOrCreateOptionID("opticsxi.algorithm", "The actual OPTICS-type algorithm to use.");

  /**
   * Parameter to specify the steepness threshold.
   */
  public static final OptionID XI_ID = OptionID.getOrCreateOptionID("opticsxi.xi", "Threshold for the steepness requirement.");

  /**
   * The actual algorithm we use.
   */
  OPTICSTypeAlgorithm<N> optics;

  /**
   * Xi parameter
   */
  double xi;

  /**
   * Constructor.
   * 
   * @param optics OPTICS algorithm to use
   * @param xi Xi value
   */
  public OPTICSXi(OPTICSTypeAlgorithm<N> optics, double xi) {
    super();
    this.optics = optics;
    this.xi = xi;
  }

  public Clustering<OPTICSModel> run(Database database, Relation<?> relation) {
    // TODO: ensure we are using the same relation?
    ClusterOrderResult<N> opticsresult = optics.run(database);

    if(!NumberDistance.class.isInstance(optics.getDistanceFactory())) {
      logger.verbose("Xi cluster extraction only supported for number distances!");
      return null;
    }

    if(logger.isVerbose()) {
      logger.verbose("Extracting clusters with Xi: " + xi);
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
  // TODO: resolve handling of the last point in the cluster order
  private Clustering<OPTICSModel> extractClusters(ClusterOrderResult<N> clusterOrderResult, Relation<?> relation, double ixi, int minpts) {
    // TODO: add progress?
    List<ClusterOrderEntry<N>> clusterOrder = clusterOrderResult.getClusterOrder();
    double mib = 0.0;
    // TODO: make it configurable to keep this list; this is mostly useful for
    // visualization
    List<SteepArea> salist = new ArrayList<SteepArea>();
    List<SteepDownArea> sdaset = new java.util.Vector<SteepDownArea>();
    ModifiableHierarchy<Cluster<OPTICSModel>> hier = new HierarchyHashmapList<Cluster<OPTICSModel>>();
    HashSet<Cluster<OPTICSModel>> curclusters = new HashSet<Cluster<OPTICSModel>>();
    HashSetModifiableDBIDs unclaimedids = DBIDUtil.newHashSet(relation.getDBIDs());

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
                  endsteep = Math.min(scan.index + 1, clusterOrder.size() - 1);
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
      clustering.addChildResult(clusterOrderResult);
      if(salist != null) {
        clusterOrderResult.addChildResult(new SteepAreaResult(salist));
      }
      return clustering;
    }
    return null;
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

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return optics.getInputTypeRestriction();
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
   * @apiviz.exclude
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
      return index < co.size();
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<D extends NumberDistance<D, ?>> extends AbstractParameterizer {
    protected OPTICSTypeAlgorithm<D> optics;

    protected double xi = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter xiP = new DoubleParameter(XI_ID);
      xiP.addConstraint(new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.CLOSE, 1.0, IntervalConstraint.IntervalBoundary.OPEN));
      if(config.grab(xiP)) {
        xi = xiP.getValue();
      }

      ClassParameter<OPTICSTypeAlgorithm<D>> opticsP = new ClassParameter<OPTICSTypeAlgorithm<D>>(XIALG_ID, OPTICSTypeAlgorithm.class, OPTICS.class);
      if(config.grab(opticsP)) {
        optics = opticsP.instantiateClass(config);
      }
    }

    @Override
    protected OPTICSXi<D> makeInstance() {
      return new OPTICSXi<D>(optics, xi);
    }
  }
}