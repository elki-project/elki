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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MTreeSearchCandidate;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparableMinHeap;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import net.jafama.FastMath;

/**
 * MkCopTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for
 * parameter k &lt; kmax.
 *
 * @author Elke Achtert
 * @since 0.2
 *
 * @navhas - contains - MkCoPTreeNode
 * @has - - - ConvexHull
 *
 * @param <O> Object type
 */
public abstract class MkCoPTree<O> extends AbstractMkTree<O, MkCoPTreeNode<O>, MkCoPEntry, MkTreeSettings<O, MkCoPTreeNode<O>, MkCoPEntry>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(MkCoPTree.class);

  /**
   * The values of log(1),..,log(k_max)
   */
  private double[] log_k;

  /**
   * Constructor.
   *
   * @param relation Relation to index
   * @param pagefile Page file
   * @param settings Tree settings
   */
  public MkCoPTree(Relation<O> relation, PageFile<MkCoPTreeNode<O>> pagefile, MkTreeSettings<O, MkCoPTreeNode<O>, MkCoPEntry> settings) {
    super(relation, pagefile, settings);
    // init log k
    log_k = new double[settings.kmax];
    for(int k = 1; k <= settings.kmax; k++) {
      log_k[k - 1] = FastMath.log(k);
    }
  }

  /**
   * @throws UnsupportedOperationException since this operation is not supported
   */
  @Override
  protected void preInsert(MkCoPEntry entry) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  /**
   * @throws UnsupportedOperationException since this operation is not supported
   */
  @Override
  public void insert(MkCoPEntry entry, boolean withPreInsert) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  @Override
  public void insertAll(List<MkCoPEntry> entries) {
    if(entries.isEmpty()) {
      return;
    }

    if(LOG.isDebugging()) {
      LOG.debugFine("insert " + entries + "\n");
    }

    if(!initialized) {
      initialize(entries.get(0));
    }

    ModifiableDBIDs ids = DBIDUtil.newArray(entries.size());

    // insert
    for(MkCoPEntry entry : entries) {
      ids.add(entry.getRoutingObjectID());
      // insert the object
      super.insert(entry, false);
    }

    // perform nearest neighbor queries
    Map<DBID, KNNList> knnLists = batchNN(getRoot(), ids, settings.kmax);

    // adjust the knn distances
    adjustApproximatedKNNDistances(getRootEntry(), knnLists);

    if(EXTRA_INTEGRITY_CHECKS) {
      getRoot().integrityCheck(this, getRootEntry());
    }
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param id the query object id
   * @param k the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  @Override
  public DoubleDBIDList reverseKNNQuery(DBIDRef id, int k) {
    if(k > settings.kmax) {
      throw new IllegalArgumentException("Parameter k has to be less or equal than " + "parameter kmax of the MCop-Tree!");
    }

    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    ModifiableDBIDs candidates = DBIDUtil.newArray();
    doReverseKNNQuery(k, id, result, candidates);

    // refinement of candidates
    Map<DBID, KNNList> knnLists = batchNN(getRoot(), candidates, k);

    result.sort();
    // Collections.sort(candidates);

    // FIXME: re-add statistics.
    // rkNNStatistics.addCandidates(candidates.size());
    // rkNNStatistics.addTrueHits(result.size());

    for(DBIDIter iter = candidates.iter(); iter.valid(); iter.advance()) {
      DBID cid = DBIDUtil.deref(iter);
      KNNList cands = knnLists.get(cid);
      for(DoubleDBIDListIter iter2 = cands.iter(); iter2.valid(); iter2.advance()) {
        if(DBIDUtil.equal(id, iter2)) {
          result.add(iter2.doubleValue(), cid);
          break;
        }
      }
    }
    result.sort();

    // FIXME: re-add statistics.
    // rkNNStatistics.addResults(result.size());
    return result;
  }

  /**
   * Returns the value of the k_max parameter.
   *
   * @return the value of the k_max parameter
   */
  public int getKmax() {
    return settings.kmax;
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   */
  @Override
  protected void initializeCapacities(MkCoPEntry exampleLeaf) {
    int distanceSize = ByteArrayUtil.SIZE_DOUBLE; // exampleLeaf.getParentDistance().externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if(getPageSize() - overhead < 0) {
      throw new AbortException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    // dirCapacity = (file.getPageSize() - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance + consApprox) + 1
    dirCapacity = (int) (getPageSize() - overhead) / (4 + 4 + distanceSize + distanceSize + 10) + 1;

    if(dirCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(dirCapacity < 10) {
      LOG.warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }

    // leafCapacity = (file.getPageSize() - overhead) / (objectID +
    // parentDistance +
    // consApprox + progrApprox) + 1
    leafCapacity = (int) (getPageSize() - overhead) / (4 + distanceSize + 2 * 10) + 1;

    if(leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      LOG.warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }

    initialized = true;

    if(LOG.isVerbose()) {
      LOG.verbose("Directory Capacity: " + (dirCapacity - 1) + "\nLeaf Capacity:    " + (leafCapacity - 1));
    }
  }

  /**
   * Performs a reverse knn query.
   *
   * @param k the parameter k of the rknn query
   * @param q the id of the query object
   * @param result holds the true results (they need not to be refined)
   * @param candidates holds possible candidates for the result (they need a
   *        refinement)
   */
  private void doReverseKNNQuery(int k, DBIDRef q, ModifiableDoubleDBIDList result, ModifiableDBIDs candidates) {
    final ComparableMinHeap<MTreeSearchCandidate> pq = new ComparableMinHeap<>();

    // push root
    pq.add(new MTreeSearchCandidate(0., getRootID(), null, Double.NaN));

    // search in tree
    while(!pq.isEmpty()) {
      MTreeSearchCandidate pqNode = pq.poll();
      // FIXME: cache the distance to the routing object in the queue node!

      MkCoPTreeNode<O> node = getNode(pqNode.nodeID);

      // directory node
      if(!node.isLeaf()) {
        for(int i = 0; i < node.getNumEntries(); i++) {
          MkCoPEntry entry = node.getEntry(i);
          double distance = distance(entry.getRoutingObjectID(), q);
          double minDist = entry.getCoveringRadius() > distance ? 0. : distance - entry.getCoveringRadius();
          double approximatedKnnDist_cons = entry.approximateConservativeKnnDistance(k);

          if(minDist <= approximatedKnnDist_cons) {
            pq.add(new MTreeSearchCandidate(minDist, getPageID(entry), entry.getRoutingObjectID(), Double.NaN));
          }
        }
      }
      // data node
      else {
        for(int i = 0; i < node.getNumEntries(); i++) {
          MkCoPLeafEntry entry = (MkCoPLeafEntry) node.getEntry(i);
          double distance = distance(entry.getRoutingObjectID(), q);
          double approximatedKnnDist_prog = entry.approximateProgressiveKnnDistance(k);

          if(distance <= approximatedKnnDist_prog) {
            result.add(distance, entry.getRoutingObjectID());
          }
          else {
            double approximatedKnnDist_cons = entry.approximateConservativeKnnDistance(k);
            double diff = distance - approximatedKnnDist_cons;
            if(diff <= 1E-10) {
              candidates.add(entry.getRoutingObjectID());
            }
          }
        }
      }
    }
  }

  /**
   * Adjusts the knn distance in the subtree of the specified root entry.
   *
   * @param entry the root entry of the current subtree
   * @param knnLists a map of knn lists for each leaf entry
   */
  private void adjustApproximatedKNNDistances(MkCoPEntry entry, Map<DBID, KNNList> knnLists) {
    MkCoPTreeNode<O> node = getNode(entry);

    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkCoPLeafEntry leafEntry = (MkCoPLeafEntry) node.getEntry(i);
        approximateKnnDistances(leafEntry, knnLists.get(leafEntry.getRoutingObjectID()));
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkCoPEntry dirEntry = node.getEntry(i);
        adjustApproximatedKNNDistances(dirEntry, knnLists);
      }
    }

    ApproximationLine approx = node.conservativeKnnDistanceApproximation(settings.kmax);
    entry.setConservativeKnnDistanceApproximation(approx);
  }

  /*
   * auxiliary function for approxKdist methods.
   */
  private double ssqerr(int k0, int kmax, double[] logk, double[] log_kDist, double m, double t) {
    int k = kmax - k0;
    double result = 0;
    for(int i = 0; i < k; i++) {
      // double h = log_kDist[i] - (m * (logk[i] - logk[0]) + t); ???
      double h = log_kDist[i] - m * logk[i] - t;
      result += h * h;
    }
    return result;
  }

  /*
   * auxiliary function for approxKdist methods.
   */
  private double optimize(int k0, int kmax, double sumx, double sumx2, double xp, double yp, double sumxy, double sumy) {
    int k = kmax - k0 + 1;
    return (sumxy - xp * sumy - yp * sumx + k * xp * yp) / (sumx2 - 2 * sumx * xp + k * xp * xp);
    // return (-xp * yp * + yp * sumx - sumxy + xp * sumy) / (-xp * xp *
    // kmax - sumx2 + 2 * xp * sumx);
  }

  /**
   * Computes logarithmic skew (fractal dimension ie. m) and in kappx[0] and
   * kappx[1] the non-logarithmic values of the approximated first and last
   * nearest neighbor distances
   *
   * @param knnDistances TODO: Spezialbehandlung fuer identische Punkte in DB
   *        (insbes. Distanz 0)
   */
  private void approximateKnnDistances(MkCoPLeafEntry entry, KNNList knnDistances) {
    StringBuilder msg = LOG.isDebugging() ? new StringBuilder(1000) : null;
    if(msg != null) {
      msg.append("knnDistances ").append(knnDistances);
    }

    DoubleDBIDListIter iter = knnDistances.iter();
    // count the zero distances
    int k_0 = 0;
    for(iter.seek(0); iter.valid(); iter.advance()) {
      if(iter.doubleValue() != 0) {
        break;
      }
      k_0++;
    }

    // init variables
    double[] log_k = new double[settings.kmax - k_0];
    System.arraycopy(this.log_k, k_0, log_k, 0, settings.kmax - k_0);

    double sum_log_kDist = 0, sum_log_k_kDist = 0;
    double[] log_kDist = new double[settings.kmax - k_0];

    iter.seek(k_0);
    for(int i = 0; iter.valid(); iter.advance(), i++) {
      final double logd = log_kDist[i] = FastMath.log(iter.doubleValue());
      sum_log_kDist += logd;
      sum_log_k_kDist += logd * log_k[i];
    }

    double sum_log_k = 0, sum_log_k2 = 0;
    for(int i = 0; i < log_k.length; i++) {
      final double logki = log_k[i];
      sum_log_k += logki;
      sum_log_k2 += logki * logki;
    }

    if(msg != null) {
      msg.append("\nk_0 ").append(k_0) //
          .append("\nk_max ").append(settings.kmax) //
          .append("\nlog_k(").append(log_k.length).append(") ").append(FormatUtil.format(log_k)) //
          .append("\nsum_log_k ").append(sum_log_k) //
          .append("\nsum_log_k^2 ").append(sum_log_k2) //
          .append("\nkDists ").append(knnDistances) //
          .append("\nlog_kDist(").append(log_kDist.length).append(") ").append(FormatUtil.format(log_kDist)) //
          .append("\nsum_log_kDist ").append(sum_log_kDist) //
          .append("\nsum_log_k_kDist ").append(sum_log_k_kDist);
    }

    // lower and upper hull
    ConvexHull convexHull = new ConvexHull(log_k, log_kDist);

    // approximate upper hull
    ApproximationLine conservative = approximateUpperHull(convexHull, log_k, log_kDist);

    ApproximationLine c2 = approximateUpperHullPaper(convexHull, log_k, sum_log_k, sum_log_k2, log_kDist, sum_log_kDist, sum_log_k_kDist);

    double err1 = ssqerr(k_0, settings.kmax, log_k, log_kDist, conservative.getM(), conservative.getT());
    double err2 = ssqerr(k_0, settings.kmax, log_k, log_kDist, c2.getM(), c2.getT());

    if(msg != null) {
      msg.append("err1 ").append(err1).append("err2 ").append(err2);
    }

    if(err1 > err2 && err1 - err2 > 0.000000001) {
      // if (err1 > err2) {

      StringBuilder warning = new StringBuilder(10000);
      int u = convexHull.getNumberOfPointsInUpperHull();
      int[] upperHull = convexHull.getUpperHull();
      warning.append("\nentry ").append(entry.getRoutingObjectID()) //
          .append("\nlower Hull ").append(convexHull.getNumberOfPointsInLowerHull()).append(' ').append(FormatUtil.format(convexHull.getLowerHull())) //
          .append("\nupper Hull ").append(convexHull.getNumberOfPointsInUpperHull()).append(' ').append(FormatUtil.format(convexHull.getUpperHull())) //
          .append("\nerr1 ").append(err1) //
          .append("\nerr2 ").append(err2) //
          .append("\nconservative1 ").append(conservative) //
          .append("\nconservative2 ").append(c2);

      for(int i = 0; i < u; i++) {
        warning.append("\nlog_k[").append(upperHull[i]).append("] = ").append(log_k[upperHull[i]]) //
            .append("\nlog_kDist[").append(upperHull[i]).append("] = ").append(log_kDist[upperHull[i]]);
      }
      LOG.warning(warning.toString());
    }

    // approximate lower hull
    ApproximationLine progressive = approximateLowerHull(convexHull, log_k, sum_log_k, sum_log_k2, log_kDist, sum_log_kDist, sum_log_k_kDist);

    entry.setConservativeKnnDistanceApproximation(conservative);
    entry.setProgressiveKnnDistanceApproximation(progressive);

    if(msg != null) {
      LOG.debugFine(msg.toString());
    }
  }

  /**
   * Approximates the lower hull.
   *
   * @param convexHull
   * @param log_kDist
   * @param sum_log_kDist
   * @param sum_log_k_kDist
   */
  private ApproximationLine approximateLowerHull(ConvexHull convexHull, double[] log_k, double sum_log_k, double sum_log_k2, double[] log_kDist, double sum_log_kDist, double sum_log_k_kDist) {
    // StringBuilder msg = new StringBuilder(1000);
    int[] lowerHull = convexHull.getLowerHull();
    int l = convexHull.getNumberOfPointsInLowerHull();
    int k_0 = settings.kmax - lowerHull.length + 1;

    // linear search on all line segments on the lower convex hull
    // msg.append("lower hull l = ").append(l).append('\n');
    double low_error = Double.MAX_VALUE;
    double low_m = 0.0;
    double low_t = 0.0;

    for(int i = 1; i < l; i++) {
      double cur_m = (log_kDist[lowerHull[i]] - log_kDist[lowerHull[i - 1]]) / (log_k[lowerHull[i]] - log_k[lowerHull[i - 1]]);
      double cur_t = log_kDist[lowerHull[i]] - cur_m * log_k[lowerHull[i]];
      double cur_error = ssqerr(k_0, settings.kmax, log_k, log_kDist, cur_m, cur_t);
      // msg.append(" Segment = ").append(i).append(" m =
      // ").append(cur_m).append(" t = ").append(cur_t).append(" lowerror =
      // ").append(cur_error).append('\n');
      if(cur_error < low_error) {
        low_error = cur_error;
        low_m = cur_m;
        low_t = cur_t;
      }
    }

    // linear search on all points of the lower convex hull
    boolean is_right = true; // NEEDED FOR PROOF CHECK
    for(int i = 0; i < l; i++) {
      double cur_m = optimize(k_0, settings.kmax, sum_log_k, sum_log_k2, log_k[lowerHull[i]], log_kDist[lowerHull[i]], sum_log_k_kDist, sum_log_kDist);
      double cur_t = log_kDist[lowerHull[i]] - cur_m * log_k[lowerHull[i]];
      // only valid if both neighboring points are underneath y=mx+t
      if((i == 0 || log_kDist[lowerHull[i - 1]] >= log_kDist[lowerHull[i]] - cur_m * (log_k[lowerHull[i]] - log_k[lowerHull[i - 1]])) && (i == l - 1 || log_kDist[lowerHull[i + 1]] >= log_kDist[lowerHull[i]] + cur_m * (log_k[lowerHull[i + 1]] - log_k[lowerHull[i]]))) {
        double cur_error = ssqerr(k_0, settings.kmax, log_k, log_kDist, cur_m, cur_t);
        if(cur_error < low_error) {
          low_error = cur_error;
          low_m = cur_m;
          low_t = cur_t;
        }
      }

      // check proof of bisection search
      if(!(i > 0 && log_kDist[lowerHull[i - 1]] < log_kDist[lowerHull[i]] - cur_m * (log_k[lowerHull[i]] - log_k[lowerHull[i - 1]])) && !is_right) {
        LOG.warning("ERROR lower: The bisection search will not work properly!");
        if(!(i < l - 1 && log_kDist[lowerHull[i + 1]] < log_kDist[lowerHull[i]] + cur_m * (log_k[lowerHull[i + 1]] - log_k[lowerHull[i]]))) {
          is_right = false;
        }
      }
    }
    return new ApproximationLine(k_0, low_m, low_t);
  }

  private ApproximationLine approximateUpperHull(ConvexHull convexHull, double[] log_k, double[] log_kDist) {
    StringBuilder msg = LOG.isDebugging() ? new StringBuilder(1000) : null;

    int[] upperHull = convexHull.getUpperHull();
    int u = convexHull.getNumberOfPointsInUpperHull();
    int k_0 = settings.kmax - upperHull.length + 1;

    ApproximationLine approx = null;
    double error = Double.POSITIVE_INFINITY;
    for(int i = 0; i < u - 1; i++) {
      int ii = upperHull[i];
      int jj = upperHull[i + 1];
      double current_m = (log_kDist[jj] - log_kDist[ii]) / (log_k[jj] - log_k[ii]);
      double current_t = log_kDist[ii] - current_m * log_k[ii];
      ApproximationLine current_approx = new ApproximationLine(k_0, current_m, current_t);

      if(msg != null) {
        msg.append("\nlog_kDist[").append(jj).append("] ").append(log_kDist[jj]) //
            .append("\nlog_kDist[").append(ii).append("] ").append(log_kDist[ii]) //
            .append("\nlog_k[").append(jj).append("] ").append(log_k[jj]) //
            .append("\nlog_k[").append(ii).append("] ").append(log_k[ii]) //
            .append('\n').append((log_kDist[jj] - log_kDist[ii])) //
            .append("\ncurrent_approx_").append(i).append(' ').append(current_approx);
      }

      boolean ok = true;
      double currentError = 0;
      for(int k = k_0; k <= settings.kmax; k++) {
        double appDist = current_approx.getValueAt(k);
        if(appDist < log_kDist[k - k_0] && log_kDist[k - k_0] - appDist > 0.000000001) {
          ok = false;
          break;
        }
        currentError += (appDist - log_kDist[k - k_0]);
      }

      if(ok && currentError < error) {
        approx = current_approx;
        error = currentError;
      }
    }

    if(msg != null) {
      LOG.debugFine(msg.append("\nupper Approx ").append(approx).toString());
    }
    return approx;
  }

  private ApproximationLine approximateUpperHullPaper(ConvexHull convexHull, double[] log_k, double sum_log_k, double sum_log_k2, double[] log_kDist, double sum_log_kDist, double sum_log_k_kDist) {
    StringBuilder msg = LOG.isDebugging() ? new StringBuilder(1000) : null;

    int[] upperHull = convexHull.getUpperHull();
    int u = convexHull.getNumberOfPointsInUpperHull();

    List<Integer> marked = new ArrayList<>();

    int k_0 = settings.kmax - upperHull.length + 1;

    int a = u / 2;
    while(marked.size() != u) {
      marked.add(a);
      double x_a = log_k[upperHull[a]];
      double y_a = log_kDist[upperHull[a]];

      double m_a = optimize(k_0, settings.kmax, sum_log_k, sum_log_k2, x_a, y_a, sum_log_k_kDist, sum_log_kDist);
      double t_a = y_a - m_a * x_a;

      if(msg != null) {
        msg.append("\na=").append(a).append(" m_a=").append(m_a).append(", t_a=").append(t_a) //
            .append("\n err ").append(ssqerr(k_0, settings.kmax, log_k, log_kDist, m_a, m_a));
      }

      double x_p = a == 0 ? Double.NaN : log_k[upperHull[a - 1]];
      double y_p = a == 0 ? Double.NaN : log_kDist[upperHull[a - 1]];
      double x_s = a == u ? Double.NaN : log_k[upperHull[a + 1]];
      double y_s = a == u ? Double.NaN : log_kDist[upperHull[a + 1]];

      boolean lessThanPre = a == 0 || y_p <= m_a * x_p + t_a;
      boolean lessThanSuc = a == u || y_s <= m_a * x_s + t_a;

      if(lessThanPre && lessThanSuc) {
        ApproximationLine appr = new ApproximationLine(k_0, m_a, t_a);
        if(msg != null) {
          LOG.debugFine(msg.append("\n1 anchor = ").append(a).toString());
        }
        return appr;
      }
      else if(!lessThanPre) {
        if(marked.contains(a - 1)) {
          m_a = (y_a - y_p) / (x_a - x_p);
          if(y_a == y_p) {
            m_a = 0;
          }
          t_a = y_a - m_a * x_a;

          ApproximationLine appr = new ApproximationLine(k_0, m_a, t_a);
          if(msg != null) {
            LOG.debugFine(msg.append("2 anchor = ").append(a) //
                .append(" appr1 ").append(appr) //
                .append(" x_a ").append(x_a).append(", y_a ").append(y_a) //
                .append(" x_p ").append(x_p).append(", y_p ").append(y_p) //
                .append(" a ").append(a) //
                .append(" upperHull ").append(FormatUtil.format(upperHull)).toString());
          }
          return appr;
        }
        else {
          a = a - 1;
        }
      }
      else {
        if(marked.contains(a + 1)) {
          m_a = (y_a - y_s) / (x_a - x_s);
          if(y_a == y_p) {
            m_a = 0;
          }
          t_a = y_a - m_a * x_a;
          ApproximationLine appr = new ApproximationLine(k_0, m_a, t_a);

          if(msg != null) {
            LOG.debugFine(msg.append("3 anchor = ").append(a).append(" -- ").append((a + 1)) //
                .append(" appr2 ").append(appr).toString());
          }
          return appr;
        }
        else {
          a = a + 1;
        }
      }
    }

    // warning("Should never happen!");
    return null;
  }

  // TODO: cleanup.
  @SuppressWarnings("unused")
  private ApproximationLine approximateUpperHullOld(ConvexHull convexHull, double[] log_k, double sum_log_k, double sum_log_k2, double[] log_kDist, double sum_log_kDist, double sum_log_k_kDist) {
    StringBuilder msg = new StringBuilder(10000);
    int[] upperHull = convexHull.getUpperHull();
    int u = convexHull.getNumberOfPointsInUpperHull();
    int k_0 = settings.kmax - upperHull.length + 1;

    // linear search on all line segments on the upper convex hull
    msg.append("upper hull:").append(u);
    double upp_error = Double.MAX_VALUE;
    double upp_m = 0.0;
    double upp_t = 0.0;
    for(int i = 1; i < u; i++) {
      double cur_m = (log_kDist[upperHull[i]] - log_kDist[upperHull[i - 1]]) / (log_k[upperHull[i]] - log_k[upperHull[i - 1]]);
      double cur_t = log_kDist[upperHull[i]] - cur_m * log_k[upperHull[i]];
      double cur_error = ssqerr(k_0, settings.kmax, log_k, log_kDist, cur_m, cur_t);
      if(cur_error < upp_error) {
        upp_error = cur_error;
        upp_m = cur_m;
        upp_t = cur_t;
      }
    }
    // linear search on all points of the upper convex hull
    boolean is_left = true; // NEEDED FOR PROOF CHECK
    for(int i = 0; i < u; i++) {
      double cur_m = optimize(k_0, settings.kmax, sum_log_k, sum_log_k2, log_k[upperHull[i]], log_kDist[upperHull[i]], sum_log_k_kDist, sum_log_kDist);
      double cur_t = log_kDist[upperHull[i]] - cur_m * log_k[upperHull[i]];
      // only valid if both neighboring points are underneath y=mx+t
      if((i == 0 || log_kDist[upperHull[i - 1]] <= log_kDist[upperHull[i]] - cur_m * (log_k[upperHull[i]] - log_k[upperHull[i - 1]])) && (i == u - 1 || log_kDist[upperHull[i + 1]] <= log_kDist[upperHull[i]] + cur_m * (log_k[upperHull[i + 1]] - log_k[upperHull[i]]))) {
        double cur_error = ssqerr(k_0, settings.kmax, log_k, log_kDist, cur_m, cur_t);
        if(cur_error < upp_error) {
          upp_error = cur_error;
          upp_m = cur_m;
          upp_t = cur_t;
        }
      }

      // check proof of bisection search
      if(!(i > 0 && log_kDist[upperHull[i - 1]] > log_kDist[upperHull[i]] - cur_m * (log_k[upperHull[i]] - log_k[upperHull[i - 1]])) && !is_left) {
        LOG.warning("ERROR upper: The bisection search will not work properly !" + "\n" + FormatUtil.format(log_kDist));
      }
      if(!(i < u - 1 && log_kDist[upperHull[i + 1]] > log_kDist[upperHull[i]] + cur_m * (log_k[upperHull[i + 1]] - log_k[upperHull[i]]))) {
        is_left = false;
      }
    }
    return new ApproximationLine(k_0, upp_m, upp_t);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @return a new leaf node
   */
  @Override
  protected MkCoPTreeNode<O> createNewLeafNode() {
    return new MkCoPTreeNode<>(leafCapacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @return a new directory node
   */
  @Override
  protected MkCoPTreeNode<O> createNewDirectoryNode() {
    return new MkCoPTreeNode<>(dirCapacity, false);
  }

  /**
   * Creates a new directory entry representing the specified node.
   *
   * @param node the node to be represented by the new entry
   * @param routingObjectID the id of the routing object of the node
   * @param parentDistance the distance from the routing object of the node to
   *        the routing object of the parent node
   */
  @Override
  protected MkCoPEntry createNewDirectoryEntry(MkCoPTreeNode<O> node, DBID routingObjectID, double parentDistance) {
    return new MkCoPDirectoryEntry(routingObjectID, parentDistance, node.getPageID(), node.coveringRadiusFromEntries(routingObjectID, this), null);
    // node.conservativeKnnDistanceApproximation(k_max));
  }

  /**
   * Creates an entry representing the root node.
   *
   * @return an entry representing the root node
   */
  @Override
  protected MkCoPEntry createRootEntry() {
    return new MkCoPDirectoryEntry(null, 0., 0, 0., null);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
