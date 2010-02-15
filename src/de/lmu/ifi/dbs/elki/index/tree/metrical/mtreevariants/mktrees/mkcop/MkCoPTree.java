package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.KNNList;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.util.PQNode;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.Identifiable;
import de.lmu.ifi.dbs.elki.utilities.QueryStatistic;
import de.lmu.ifi.dbs.elki.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.elki.utilities.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;

/**
 * MkCopTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for
 * parameter k < kmax.
 * 
 * @param <O> Object type
 * @param <D> Distance type
 * @param <N> Number type
 * @author Elke Achtert
 */
public class MkCoPTree<O extends DatabaseObject, D extends NumberDistance<D, N>, N extends Number> extends AbstractMTree<O, D, MkCoPTreeNode<O, D, N>, MkCoPEntry<D, N>> {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("mkcop.k", "positive integer specifying the maximal number k of reverse" + "k nearest neighbors to be supported.");

  /**
   * Parameter for k
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0));

  /**
   * Parameter k.
   */
  int k_max;

  /**
   * The values of log(1),..,log(k_max)
   */
  private double[] log_k;

  /**
   * Provides some statistics about performed reverse knn-queries.
   */
  private QueryStatistic rkNNStatistics = new QueryStatistic();

  /**
   * Creates a new MkCopTree.
   */
  public MkCoPTree(Parameterization config) {
    super(config);
    if(config.grab(this, K_PARAM)) {
      k_max = K_PARAM.getValue();

      // init log k
      log_k = new double[k_max];
      for(int k = 1; k <= k_max; k++) {
        log_k[k - 1] = Math.log(k);
      }
    }
  }

  /**
   * Inserts the specified object into this MDkNNTree-Tree. This operation is
   * not supported.
   * 
   * @param object the object to be inserted
   */
  public void insert(O object) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  /**
   * Performs necessary operations before inserting the specified entry.
   * 
   * @param entry the entry to be inserted
   */
  @Override
  protected void preInsert(MkCoPEntry<D, N> entry) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  /**
   * Inserts the specified objects into this MDkNNTree-Tree.
   * 
   * @param objects the object to be inserted
   */
  public void insert(List<O> objects) {
    if(logger.isDebugging()) {
      logger.debugFine("insert " + objects + "\n");
    }

    if(!initialized) {
      initialize(objects.get(0));
    }

    List<Integer> ids = new ArrayList<Integer>();
    Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();

    // insert
    for(O object : objects) {
      // create knnList for the object
      ids.add(object.getID());
      knnLists.put(object.getID(), new KNNList<D>(k_max + 1, getDistanceFunction().infiniteDistance()));

      // insert the object
      super.insert(object, false);
    }

    // do batch nn
    batchNN(getRoot(), ids, knnLists);

    // adjust the knn distances
    adjustApproximatedKNNDistances(getRootEntry(), knnLists);

    // if (debug) {
    // getRoot().test(this, getRootEntry());
    // }
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   * 
   * @param object the query object
   * @param k the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  @Override
  public List<DistanceResultPair<D>> reverseKNNQuery(O object, int k) {
    if(k > this.k_max) {
      throw new IllegalArgumentException("Parameter k has to be less or equal than " + "parameter kmax of the MCop-Tree!");
    }

    List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
    List<Integer> candidates = new ArrayList<Integer>();
    doReverseKNNQuery(k, object.getID(), result, candidates);

    // refinement of candidates
    Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();
    for(Integer id : candidates) {
      knnLists.put(id, new KNNList<D>(k, getDistanceFunction().infiniteDistance()));
    }
    batchNN(getRoot(), candidates, knnLists);

    Collections.sort(result);
    Collections.sort(candidates);

    rkNNStatistics.addCandidates(candidates.size());
    rkNNStatistics.addTrueHits(result.size());

    for(Integer id : candidates) {
      List<DistanceResultPair<D>> knns = knnLists.get(id).toList();
      for(DistanceResultPair<D> qr : knns) {
        if(qr.getID() == object.getID()) {
          result.add(new DistanceResultPair<D>(qr.getDistance(), id));
          break;
        }
      }

    }
    Collections.sort(result);

    rkNNStatistics.addResults(result.size());
    return result;
  }

  /**
   * Returns the statistic for performed rknn queries.
   * 
   * @return the statistic for performed rknn queries
   */
  public QueryStatistic getRkNNStatistics() {
    return rkNNStatistics;
  }

  /**
   * Clears the values of the statistic for performed rknn queries
   */
  public void clearRkNNStatistics() {
    rkNNStatistics.clear();
  }

  /**
   * Returns the value of the k_max parameter.
   * 
   * @return the value of the k_max parameter
   */
  public int getK_max() {
    return k_max;
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   */
  @Override
  protected void initializeCapacities(@SuppressWarnings("unused") O object, boolean verbose) {
    D dummyDistance = getDistanceFunction().nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if(pageSize - overhead < 0) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance + consApprox) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 4 + distanceSize + distanceSize + 10) + 1;

    if(dirCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    if(dirCapacity < 10) {
      logger.warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance +
    // consApprox + progrApprox) + 1
    leafCapacity = (int) (pageSize - overhead) / (4 + distanceSize + 2 * 10) + 1;

    if(leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      logger.warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }

    initialized = true;

    if(verbose) {
      logger.verbose("Directory Capacity: " + (dirCapacity - 1) + "\nLeaf Capacity:    " + (leafCapacity - 1));
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
  private void doReverseKNNQuery(int k, Integer q, List<DistanceResultPair<D>> result, List<Integer> candidates) {

    final Heap<D, Identifiable> pq = new DefaultHeap<D, Identifiable>();

    // push root
    pq.addNode(new PQNode<D>(getDistanceFunction().nullDistance(), getRootEntry().getID(), null));

    // search in tree
    while(!pq.isEmpty()) {
      PQNode<D> pqNode = (PQNode<D>) pq.getMinNode();

      MkCoPTreeNode<O, D, N> node = getNode(pqNode.getValue().getID());

      // directory node
      if(!node.isLeaf()) {
        for(int i = 0; i < node.getNumEntries(); i++) {
          MkCoPEntry<D, N> entry = node.getEntry(i);
          D distance = getDistanceFunction().distance(entry.getRoutingObjectID(), q);
          D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ? getDistanceFunction().nullDistance() : distance.minus(entry.getCoveringRadius());
          D approximatedKnnDist_cons = entry.approximateConservativeKnnDistance(k, getDistanceFunction());

          if(minDist.compareTo(approximatedKnnDist_cons) <= 0) {
            pq.addNode(new PQNode<D>(minDist, entry.getID(), entry.getRoutingObjectID()));
          }
        }
      }
      // data node
      else {
        for(int i = 0; i < node.getNumEntries(); i++) {
          MkCoPLeafEntry<D, N> entry = (MkCoPLeafEntry<D, N>) node.getEntry(i);
          D distance = getDistanceFunction().distance(entry.getRoutingObjectID(), q);
          D approximatedKnnDist_prog = entry.approximateProgressiveKnnDistance(k, getDistanceFunction());

          if(distance.compareTo(approximatedKnnDist_prog) <= 0) {
            result.add(new DistanceResultPair<D>(distance, entry.getRoutingObjectID()));
          }
          else {
            D approximatedKnnDist_cons = entry.approximateConservativeKnnDistance(k, getDistanceFunction());
            double diff = distance.doubleValue() - approximatedKnnDist_cons.doubleValue();
            if(diff <= 0.0000000001) {
              candidates.add(entry.getRoutingObjectID());
            }
          }
        }
      }
    }
  }

  private List<D> getKNNList(Integer id, Map<Integer, KNNList<D>> knnLists) {
    KNNList<D> knns = knnLists.get(id);
    List<D> result = knns.distancesToList();
    // result.remove(0);
    return result;
  }

  /**
   * Adjusts the knn distance in the subtree of the specified root entry.
   * 
   * @param entry the root entry of the current subtree
   * @param knnLists a map of knn lists for each leaf entry
   */
  private void adjustApproximatedKNNDistances(MkCoPEntry<D, N> entry, Map<Integer, KNNList<D>> knnLists) {
    MkCoPTreeNode<O, D, N> node = file.readPage(entry.getID());

    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkCoPLeafEntry<D, N> leafEntry = (MkCoPLeafEntry<D, N>) node.getEntry(i);
        approximateKnnDistances(leafEntry, getKNNList(leafEntry.getRoutingObjectID(), knnLists));
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkCoPEntry<D, N> dirEntry = node.getEntry(i);
        adjustApproximatedKNNDistances(dirEntry, knnLists);
      }
    }

    ApproximationLine approx = node.conservativeKnnDistanceApproximation(k_max);
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
  private void approximateKnnDistances(MkCoPLeafEntry<D, N> entry, List<D> knnDistances) {
    StringBuffer msg = new StringBuffer();
    if(logger.isDebugging()) {
      msg.append("\nknnDistances " + knnDistances);
    }

    // count the zero distances
    int k_0 = 0;
    for(int i = 0; i < k_max; i++) {
      double dist = knnDistances.get(i).doubleValue();
      if(dist == 0) {
        k_0++;
      }
      else {
        break;
      }
    }

    // init variables
    double[] log_k = new double[k_max - k_0];
    System.arraycopy(this.log_k, k_0, log_k, 0, k_max - k_0);

    double sum_log_kDist = 0;
    double sum_log_k_kDist = 0;
    double[] log_kDist = new double[k_max - k_0];

    for(int i = 0; i < k_max - k_0; i++) {
      double dist = knnDistances.get(i + k_0).doubleValue();
      log_kDist[i] = Math.log(dist);
      sum_log_kDist += log_kDist[i];
      sum_log_k_kDist += log_kDist[i] * log_k[i];
    }

    double sum_log_k = 0;
    double sum_log_k2 = 0;
    // noinspection ForLoopReplaceableByForEach
    for(int i = 0; i < log_k.length; i++) {
      sum_log_k += log_k[i];
      sum_log_k2 += (log_k[i] * log_k[i]);
    }

    if(logger.isDebugging()) {
      msg.append("\nk_0 " + k_0);
      msg.append("\nk_max " + k_max);
      msg.append("\nlog_k(" + log_k.length + ") " + FormatUtil.format(log_k));
      msg.append("\nsum_log_k " + sum_log_k);
      msg.append("\nsum_log_k^2 " + sum_log_k2);
      msg.append("\nkDists " + knnDistances);
      msg.append("\nlog_kDist(" + log_kDist.length + ") " + FormatUtil.format(log_kDist));
      msg.append("\nsum_log_kDist " + sum_log_kDist);
      msg.append("\nsum_log_k_kDist " + sum_log_k_kDist);
    }

    // lower and upper hull
    ConvexHull convexHull = new ConvexHull(log_k, log_kDist);

    // approximate upper hull
    ApproximationLine conservative = approximateUpperHull(convexHull, log_k, log_kDist);

    ApproximationLine c2 = approximateUpperHull_PAPER(convexHull, log_k, sum_log_k, sum_log_k2, log_kDist, sum_log_kDist, sum_log_k_kDist);

    double err1 = ssqerr(k_0, k_max, log_k, log_kDist, conservative.getM(), conservative.getT());
    double err2 = ssqerr(k_0, k_max, log_k, log_kDist, c2.getM(), c2.getT());

    if(logger.isDebugging()) {
      msg.append("err1 " + err1);
      msg.append("err2 " + err2);
    }

    if(err1 > err2 && err1 - err2 > 0.000000001) {
      // if (err1 > err2) {

      StringBuffer warning = new StringBuffer();
      int u = convexHull.getNumberOfPointsInUpperHull();
      int[] upperHull = convexHull.getUpperHull();
      warning.append("\nentry " + entry.getRoutingObjectID());
      warning.append("\nlower Hull " + convexHull.getNumberOfPointsInLowerHull() + " " + FormatUtil.format(convexHull.getLowerHull()));
      warning.append("\nupper Hull " + convexHull.getNumberOfPointsInUpperHull() + " " + FormatUtil.format(convexHull.getUpperHull()));
      warning.append("\nerr1 " + err1);
      warning.append("\nerr2 " + err2);
      warning.append("\nconservative1 " + conservative);
      warning.append("\nconservative2 " + c2);

      for(int i = 0; i < u; i++) {
        warning.append("\nlog_k[" + upperHull[i] + "] = " + log_k[upperHull[i]]);
        warning.append("\nlog_kDist[" + upperHull[i] + "] = " + log_kDist[upperHull[i]]);
      }
      // warning(warning.toString());
    }

    // approximate lower hull
    ApproximationLine progressive = approximateLowerHull(convexHull, log_k, sum_log_k, sum_log_k2, log_kDist, sum_log_kDist, sum_log_k_kDist);

    entry.setConservativeKnnDistanceApproximation(conservative);
    entry.setProgressiveKnnDistanceApproximation(progressive);

    if(logger.isDebugging()) {
      logger.debugFine(msg.toString());
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

    StringBuffer msg = new StringBuffer();
    int[] lowerHull = convexHull.getLowerHull();
    int l = convexHull.getNumberOfPointsInLowerHull();
    int k_0 = k_max - lowerHull.length + 1;

    // linear search on all line segments on the lower convex hull
    msg.append("lower hull l = " + l + "\n");
    double low_error = Double.MAX_VALUE;
    double low_m = 0.0;
    double low_t = 0.0;

    for(int i = 1; i < l; i++) {
      double cur_m = (log_kDist[lowerHull[i]] - log_kDist[lowerHull[i - 1]]) / (log_k[lowerHull[i]] - log_k[lowerHull[i - 1]]);
      double cur_t = log_kDist[lowerHull[i]] - cur_m * log_k[lowerHull[i]];
      double cur_error = ssqerr(k_0, k_max, log_k, log_kDist, cur_m, cur_t);
      msg.append("  Segment = " + i + " m = " + cur_m + " t = " + cur_t + " lowerror = " + cur_error + "\n");
      if(cur_error < low_error) {
        low_error = cur_error;
        low_m = cur_m;
        low_t = cur_t;
      }
    }

    // linear search on all points of the lower convex hull
    boolean is_right = true; // NEEDED FOR PROOF CHECK
    for(int i = 0; i < l; i++) {
      double cur_m = optimize(k_0, k_max, sum_log_k, sum_log_k2, log_k[lowerHull[i]], log_kDist[lowerHull[i]], sum_log_k_kDist, sum_log_kDist);
      double cur_t = log_kDist[lowerHull[i]] - cur_m * log_k[lowerHull[i]];
      // only valid if both neighboring points are underneath y=mx+t
      if((i == 0 || log_kDist[lowerHull[i - 1]] >= log_kDist[lowerHull[i]] - cur_m * (log_k[lowerHull[i]] - log_k[lowerHull[i - 1]])) && (i == l - 1 || log_kDist[lowerHull[i + 1]] >= log_kDist[lowerHull[i]] + cur_m * (log_k[lowerHull[i + 1]] - log_k[lowerHull[i]]))) {
        double cur_error = ssqerr(k_0, k_max, log_k, log_kDist, cur_m, cur_t);
        if(cur_error < low_error) {
          low_error = cur_error;
          low_m = cur_m;
          low_t = cur_t;
        }
      }

      // check proof of bisection search
      if(!(i > 0 && log_kDist[lowerHull[i - 1]] < log_kDist[lowerHull[i]] - cur_m * (log_k[lowerHull[i]] - log_k[lowerHull[i - 1]])) && !is_right) {
        // warning("ERROR lower: The bisection search will not work properly !");
        if(!(i < l - 1 && log_kDist[lowerHull[i + 1]] < log_kDist[lowerHull[i]] + cur_m * (log_k[lowerHull[i + 1]] - log_k[lowerHull[i]]))) {
          is_right = false;
        }
      }
    }

    ApproximationLine lowerApproximation = new ApproximationLine(k_0, low_m, low_t);
    return lowerApproximation;
  }

  private ApproximationLine approximateUpperHull(ConvexHull convexHull, double[] log_k, double[] log_kDist) {
    StringBuffer msg = new StringBuffer();

    int[] upperHull = convexHull.getUpperHull();
    int u = convexHull.getNumberOfPointsInUpperHull();
    int k_0 = k_max - upperHull.length + 1;

    ApproximationLine approx = null;
    double error = Double.POSITIVE_INFINITY;
    for(int i = 0; i < u - 1; i++) {
      int ii = upperHull[i];
      int jj = upperHull[i + 1];
      double current_m = (log_kDist[jj] - log_kDist[ii]) / (log_k[jj] - log_k[ii]);
      double current_t = log_kDist[ii] - current_m * log_k[ii];
      ApproximationLine current_approx = new ApproximationLine(k_0, current_m, current_t);

      if(logger.isDebugging()) {
        msg.append("\nlog_kDist[" + jj + "] " + log_kDist[jj]);
        msg.append("\nlog_kDist[" + ii + "] " + log_kDist[ii]);
        msg.append("\nlog_k[" + jj + "] " + log_k[jj]);
        msg.append("\nlog_k[" + ii + "] " + log_k[ii]);
        msg.append("\n" + (log_kDist[jj] - log_kDist[ii]));
        msg.append("\ncurrent_approx_" + i + " " + current_approx);
      }

      boolean ok = true;
      double currentError = 0;
      for(int k = k_0; k <= k_max; k++) {
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

    if(logger.isDebugging()) {
      msg.append("\nupper Approx " + approx);
      logger.debugFine(msg.toString());
    }
    return approx;
  }

  private ApproximationLine approximateUpperHull_PAPER(ConvexHull convexHull, double[] log_k, double sum_log_k, double sum_log_k2, double[] log_kDist, double sum_log_kDist, double sum_log_k_kDist) {
    StringBuffer msg = new StringBuffer();

    int[] upperHull = convexHull.getUpperHull();
    int u = convexHull.getNumberOfPointsInUpperHull();

    List<Integer> marked = new ArrayList<Integer>();

    int k_0 = k_max - upperHull.length + 1;

    int a = u / 2;
    while(marked.size() != u) {
      marked.add(a);
      double x_a = log_k[upperHull[a]];
      double y_a = log_kDist[upperHull[a]];

      double m_a = optimize(k_0, k_max, sum_log_k, sum_log_k2, x_a, y_a, sum_log_k_kDist, sum_log_kDist);
      double t_a = y_a - m_a * x_a;

      if(logger.isDebugging()) {
        msg.append("\na=" + a + " m_a=" + m_a + ", t_a=" + t_a);
        msg.append("\n err " + ssqerr(k_0, k_max, log_k, log_kDist, m_a, m_a));
      }

      double x_p = a == 0 ? Double.NaN : log_k[upperHull[a - 1]];
      double y_p = a == 0 ? Double.NaN : log_kDist[upperHull[a - 1]];
      double x_s = a == u ? Double.NaN : log_k[upperHull[a + 1]];
      double y_s = a == u ? Double.NaN : log_kDist[upperHull[a + 1]];

      boolean lessThanPre = a == 0 || y_p <= m_a * x_p + t_a;
      boolean lessThanSuc = a == u || y_s <= m_a * x_s + t_a;

      if(lessThanPre && lessThanSuc) {
        ApproximationLine appr = new ApproximationLine(k_0, m_a, t_a);
        if(logger.isDebugging()) {
          msg.append("\n1 anchor = " + a);
          logger.debugFine(msg.toString());
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
          if(logger.isDebugging()) {
            msg.append("2 anchor = " + a);
            msg.append(" appr1 " + appr);
            msg.append(" x_a " + x_a + ", y_a " + y_a);
            msg.append(" x_p " + x_p + ", y_p " + y_p);
            msg.append(" a " + a);
            msg.append(" upperHull " + FormatUtil.format(upperHull));
            logger.debugFine(msg.toString());
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
          if(y_a == y_p)
            m_a = 0;
          t_a = y_a - m_a * x_a;
          ApproximationLine appr = new ApproximationLine(k_0, m_a, t_a);

          if(logger.isDebugging()) {
            msg.append("3 anchor = " + a + " -- " + (a + 1));
            msg.append(" appr2 " + appr);
            logger.debugFine(msg.toString());
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

  @SuppressWarnings("unused")
  private ApproximationLine approximateUpperHull_OLD(ConvexHull convexHull, double[] log_k, double sum_log_k, double sum_log_k2, double[] log_kDist, double sum_log_kDist, double sum_log_k_kDist) {
    StringBuffer msg = new StringBuffer();
    int[] upperHull = convexHull.getUpperHull();
    int u = convexHull.getNumberOfPointsInUpperHull();
    int k_0 = k_max - upperHull.length + 1;

    // linear search on all line segments on the upper convex hull
    msg.append("upper hull:").append(u);
    double upp_error = Double.MAX_VALUE;
    double upp_m = 0.0;
    double upp_t = 0.0;
    for(int i = 1; i < u; i++) {
      double cur_m = (log_kDist[upperHull[i]] - log_kDist[upperHull[i - 1]]) / (log_k[upperHull[i]] - log_k[upperHull[i - 1]]);
      double cur_t = log_kDist[upperHull[i]] - cur_m * log_k[upperHull[i]];
      double cur_error = ssqerr(k_0, k_max, log_k, log_kDist, cur_m, cur_t);
      if(cur_error < upp_error) {
        upp_error = cur_error;
        upp_m = cur_m;
        upp_t = cur_t;
      }
    }
    // linear search on all points of the upper convex hull
    boolean is_left = true; // NEEDED FOR PROOF CHECK
    for(int i = 0; i < u; i++) {
      double cur_m = optimize(k_0, k_max, sum_log_k, sum_log_k2, log_k[upperHull[i]], log_kDist[upperHull[i]], sum_log_k_kDist, sum_log_kDist);
      double cur_t = log_kDist[upperHull[i]] - cur_m * log_k[upperHull[i]];
      // only valid if both neighboring points are underneath y=mx+t
      if((i == 0 || log_kDist[upperHull[i - 1]] <= log_kDist[upperHull[i]] - cur_m * (log_k[upperHull[i]] - log_k[upperHull[i - 1]])) && (i == u - 1 || log_kDist[upperHull[i + 1]] <= log_kDist[upperHull[i]] + cur_m * (log_k[upperHull[i + 1]] - log_k[upperHull[i]]))) {
        double cur_error = ssqerr(k_0, k_max, log_k, log_kDist, cur_m, cur_t);
        if(cur_error < upp_error) {
          upp_error = cur_error;
          upp_m = cur_m;
          upp_t = cur_t;
        }
      }

      // check proof of bisection search
      if(!(i > 0 && log_kDist[upperHull[i - 1]] > log_kDist[upperHull[i]] - cur_m * (log_k[upperHull[i]] - log_k[upperHull[i - 1]])) && !is_left) {
        // warning("ERROR upper: The bisection search will not work properly !"
        // +
        // "\n" + Util.format(log_kDist));
      }
      if(!(i < u - 1 && log_kDist[upperHull[i + 1]] > log_kDist[upperHull[i]] + cur_m * (log_k[upperHull[i + 1]] - log_k[upperHull[i]]))) {
        is_left = false;
      }
    }

    ApproximationLine upperApproximation = new ApproximationLine(k_0, upp_m, upp_t);
    return upperApproximation;
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * 
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  @Override
  protected MkCoPTreeNode<O, D, N> createNewLeafNode(int capacity) {
    return new MkCoPTreeNode<O, D, N>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * 
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  @Override
  protected MkCoPTreeNode<O, D, N> createNewDirectoryNode(int capacity) {
    return new MkCoPTreeNode<O, D, N>(file, capacity, false);
  }

  /**
   * Creates a new leaf entry representing the specified data object in the
   * specified subtree.
   * 
   * @param object the data object to be represented by the new entry
   * @param parentDistance the distance from the object to the routing object of
   *        the parent node
   */
  @Override
  protected MkCoPEntry<D, N> createNewLeafEntry(O object, D parentDistance) {
    MkCoPLeafEntry<D, N> leafEntry = new MkCoPLeafEntry<D, N>(object.getID(), parentDistance, null, null);
    return leafEntry;
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
  protected MkCoPEntry<D, N> createNewDirectoryEntry(MkCoPTreeNode<O, D, N> node, Integer routingObjectID, D parentDistance) {
    return new MkCoPDirectoryEntry<D, N>(routingObjectID, parentDistance, node.getID(), node.coveringRadius(routingObjectID, this), null);
    // node.conservativeKnnDistanceApproximation(k_max));
  }

  /**
   * Creates an entry representing the root node.
   * 
   * @return an entry representing the root node
   */
  @Override
  protected MkCoPEntry<D, N> createRootEntry() {
    return new MkCoPDirectoryEntry<D, N>(null, null, 0, null, null);
  }

  /**
   * Return the node base class.
   * 
   * @return node base class
   */
  @Override
  protected Class<MkCoPTreeNode<O, D, N>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MkCoPTreeNode.class);
  }
}