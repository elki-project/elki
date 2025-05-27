/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2025
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
package elki.clustering.hierarchical;

import java.util.Arrays;

import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.query.PrioritySearcher;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.LinearScanEuclideanPrioritySearcher;
import elki.database.query.distance.LinearScanPrioritySearcher;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MathUtil;
import elki.utilities.Alias;
import elki.utilities.datastructures.heap.DoubleIntegerHeap;
import elki.utilities.datastructures.heap.DoubleIntegerMinHeap;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Index accelerated HDBSCAN* clustering algorithm with a heap-of-candidates
 * strategy, where for each point we store a buffer of candidates in a heap.
 * <p>
 * This variant uses a kNN search initially, then a priority search later on.
 * <p>
 * Reference:
 *
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
@Alias({ "HDBSCAN-BS" })
public class HDBSCANBS<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(HDBSCANBS.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * How many extra neighbors to retrieve.
   */
  protected int slack = 1;

  /**
   * Number of neighbors for core distances
   */
  int minPts;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param minPts Minimum number of points for coredists
   * @param slack Number of additional neighbors to retrieve
   */
  public HDBSCANBS(Distance<? super O> distance, int minPts, int slack) {
    super();
    this.distance = distance;
    this.minPts = minPts;
    this.slack = slack;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Run the clustering algorithm.
   * 
   * @param relation Input data
   * @return Cluster merge history
   */
  public ClusterMergeHistory run(Relation<O> relation) {
    DBIDEnum ids = DBIDUtil.ensureEnum(relation.getDBIDs());
    ClusterMergeHistoryBuilder builder = new ClusterMergeHistoryBuilder(ids, distance.isSquared());
    // Create one for testing we have a suitable index.
    PrioritySearcher<DBIDRef> pq = new QueryBuilder<>(relation, distance) //
        .lowSelectivity().priorityByDBID();
    if(pq instanceof LinearScanPrioritySearcher || pq instanceof LinearScanEuclideanPrioritySearcher) {
      throw new UnsupportedOperationException("No index acceleration available. This will be very slow.");
    }
    new Instance(ids, builder, pq).run(relation);
    assert builder.mergecount == ids.size() - 1;
    builder.optimizeOrder();
    return builder.complete();
  }

  /**
   * Instance for a single run.
   * 
   * @author Erich Schubert
   */
  protected class Instance {
    /**
     * Object IDs
     */
    protected DBIDEnum ids;

    /**
     * Temporary iterator for mapping
     */
    protected DBIDArrayIter ita;

    /**
     * Cluster merge helper
     */
    protected ClusterMergeHistoryBuilder builder;

    /**
     * Priority search
     */
    protected PrioritySearcher<DBIDRef> pq;

    /**
     * Primary heap.
     */
    private DoubleIntegerMinHeap heap;

    /**
     * Nearest (known) neighbors
     */
    private DoubleIntegerMinHeap[] heaps;

    /**
     * Threshold for complete results
     */
    private double[] threshold;

    /**
     * Core distances of each point
     */
    private double[] coredist;

    /**
     * Constructor for a single run.
     *
     * @param ids IDs
     * @param builder Merge helper
     * @param pq Priority searcher
     */
    public Instance(DBIDEnum ids, ClusterMergeHistoryBuilder builder, PrioritySearcher<DBIDRef> pq) {
      this.ids = ids;
      this.builder = builder;
      this.pq = pq;
      this.ita = ids.iter();
    }

    /**
     * Run the main algorithm
     */
    public void run(Relation<? extends O> relation) {
      initializeCoreDists(relation);
      FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Clustering", ids.size(), LOG) : null;
      if(cprog != null) {
        cprog.setProcessed(builder.mergecount + 1, LOG);
      }
      while(!heap.isEmpty()) {
        final double curd = heap.peekKey();
        final int a = heap.peekValue();
        DoubleIntegerMinHeap nn = heaps[a];
        final int b = nn.peekValue();
        int ca = builder.get(a), cb = builder.get(b);
        if(ca != cb) {
          ca = builder.add(ca, curd, cb);
          LOG.incrementProcessed(cprog);
          if(builder.mergecount == ids.size() - 1) {
            break;
          }
        }
        nn.poll();
        // Skip other already merged neighbors
        while(!nn.isEmpty() && builder.get(nn.peekValue()) == ca) {
          nn.poll();
        }
        // need refill?
        if(nn.isEmpty() || nn.peekKey() > threshold[a]) {
          refillNeighbors(a, ca, curd);
        }
        if(nn.isEmpty()) {
          heap.poll();
          continue;
        }
        heap.replaceTopElement(nn.peekKey(), a);
      }
      LOG.ensureCompleted(cprog);
    }

    /**
     * We do this separately, with a kNN query as this tends to be faster.
     * 
     * @param relation data relation
     */
    private void initializeCoreDists(Relation<? extends O> relation) {
      KNNSearcher<DBIDRef> knnq = new QueryBuilder<>(relation, distance).kNNByDBID(minPts);
      FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Core distances", ids.size(), LOG) : null;
      this.coredist = new double[ids.size()];
      this.seen = new boolean[ids.size()];
      Arrays.fill(coredist, Double.NaN);
      KNNList[] knns = new KNNList[ids.size()];
      for(DBIDArrayIter ita = ids.iter(); ita.valid(); ita.advance(), LOG.incrementProcessed(cprog)) {
        int a = ita.getOffset();
        if(coredist[a] == coredist[a]) { // not NaN, duplicate
          continue;
        }
        KNNList knn = knns[a] = knnq.getKNN(ita, minPts);
        coredist[a] = knn.getKNNDistance();
        // Mark duplicates
        for(int i = 0; i < knn.size() && knn.doubleValue(i) == 0.; i++) {
          coredist[ids.index(knn.iter().seek(i))] = knn.getKNNDistance();
        }
      }
      initializeHeap(knns);
      LOG.ensureCompleted(cprog);
    }

    /**
     * Initialize the heaps from the kNN lists and the primary heap.
     * 
     * @param knns kNN heaps.
     */
    private void initializeHeap(KNNList[] knns) {
      FiniteProgress iprog = LOG.isVerbose() ? new FiniteProgress("Heap initialization", ids.size(), LOG) : null;
      this.heap = new DoubleIntegerMinHeap(ids.size());
      this.heaps = new DoubleIntegerMinHeap[ids.size()];
      this.threshold = new double[ids.size()];
      for(ita.seek(0); ita.valid(); ita.advance(), LOG.incrementProcessed(iprog)) {
        int a = ita.getOffset(), ca = builder.get(a);
        if(builder.getSize(ca) > 1) {
          continue; // duplicate
        }
        double cd = coredist[a];
        DoubleIntegerMinHeap h = heaps[a] = new DoubleIntegerMinHeap();
        for(DoubleDBIDListIter it = knns[a].iter(); it.valid(); it.advance()) {
          final int b = ids.index(it);
          if(a == b) {
            continue;
          }
          if(it.doubleValue() == 0.) { // duplicate, merge immediately
            int cb = builder.get(b);
            if(ca != cb) {
              ca = builder.add(ca, cd, cb);
            }
            continue;
          }
          double rd = MathUtil.max(cd, it.doubleValue(), coredist[b]);
          h.add(rd, b);
        }
        if(!h.isEmpty()) {
          heap.add(h.peekKey(), a);
          threshold[a] = pq.allLowerBound();
        }
      }
      LOG.ensureCompleted(iprog);
      if(LOG.isDebugging()) {
        LOG.debug("Performed " + builder.mergecount + " merges of duplicates (may involve more objects) during initialization.");
      }
    }

    /**
     * Last id used for refilling
     */
    int last = -1;

    /**
     * Set
     */
    boolean[] seen;

    /**
     * Refill the nearest neighbors.
     * 
     * @param a Query object number
     * @param ca Cluster id of the query object
     * @param skip Last merge distance, for skipping
     */
    private void refillNeighbors(int a, int ca, double skip) {
      DoubleIntegerMinHeap h = heaps[a];
      double thres = h.isEmpty() ? Double.POSITIVE_INFINITY : h.peekKey();
      double cd = coredist[a];
      // Avoid adding entries repeatedly
      if(last != a) {
        Arrays.fill(seen, false);
        for(DoubleIntegerHeap.UnsortedIter it = h.unsortedIter(); it.valid(); it.advance()) {
          seen[it.getValue()] = true;
        }
        pq.search(ita.seek(a)).increaseSkip(skip);
        last = a;
      }
      int remain = slack;
      for(; pq.valid() && (pq.allLowerBound() < thres || remain-- > 0); pq.advance()) {
        final int b = ids.index(pq);
        if(a == b || builder.get(b) == ca || seen[b]) {
          continue;
        }
        double d = pq.computeExactDistance();
        if(d < skip) {
          continue;
        }
        double rd = MathUtil.max(cd, d, coredist[b]);
        h.add(rd, b);
        seen[b] = true;
        thres = h.peekKey();
        // do not use pq.decreaseCutoff, as we may continue with the searcher
      }
      threshold[a] = pq.allLowerBound(); // Save the current lower bound
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O> extends AbstractHDBSCAN.Par<O> {
    /**
     * Slack parameter ID.
     */
    public static final OptionID SLACK_ID = BufferedSearchSingleLink.Par.SLACK_ID;

    /**
     * How many extra neighbors to retrieve.
     */
    protected int slack = 1;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(SLACK_ID, 2) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> slack = x);
    }

    @Override
    public HDBSCANBS<O> make() {
      return new HDBSCANBS<>(distance, minPts, slack);
    }
  }
}
