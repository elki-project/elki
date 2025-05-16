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
import elki.utilities.datastructures.heap.DoubleIntegerMinHeap;

/**
 * Index accelerated HDBSCAN* clustering algorithm with a heap-of-searchers
 * strategy. This is more memory intensive than the restarting search approach,
 * but will need fewer distance computations.
 * <p>
 * Reference:
 *
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
public class HDBSCANHS<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(HDBSCANHS.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Number of neighbors for core distances
   */
  int minPts;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param minPts Minimum number of points for coredists
   */
  public HDBSCANHS(Distance<? super O> distance, int minPts) {
    super();
    this.distance = distance;
    this.minPts = minPts;
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
    new Instance(ids, builder).run(relation);
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
     * Cluster merge helper
     */
    protected ClusterMergeHistoryBuilder builder;

    /**
     * Priority searchers.
     */
    protected PrioritySearcher<DBIDRef>[] pqs;

    /**
     * Primary heap.
     */
    private DoubleIntegerMinHeap heap;

    /**
     * Auxiliary heaps.
     */
    private DoubleIntegerMinHeap[] heaps;

    /**
     * Core distances of each point
     */
    private double[] coredist;

    /**
     * Constructor for a single run.
     *
     * @param ids IDs
     * @param builder Merge helper
     */
    public Instance(DBIDEnum ids, ClusterMergeHistoryBuilder builder) {
      this.ids = ids;
      this.builder = builder;
    }

    /**
     * Run the main algorithm
     */
    public void run(Relation<? extends O> relation) {
      initializeCoreDists(relation);
      initializeHeap(relation);
      FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Clustering", ids.size(), LOG) : null;
      if(cprog != null) {
        cprog.setProcessed(builder.mergecount + 1, LOG);
      }
      while(true) {
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
        // need refill?
        if(nn.isEmpty() || nn.peekKey() > pqs[a].allLowerBound()) {
          refillNeighbors(a, ca);
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
      Arrays.fill(coredist, Double.NaN);
      for(DBIDArrayIter ita = ids.iter(); ita.valid(); ita.advance(), LOG.incrementProcessed(cprog)) {
        int a = ita.getOffset();
        if(coredist[a] != coredist[a]) { // not yet set
          KNNList knn = knnq.getKNN(ita, minPts);
          coredist[a] = knn.getKNNDistance();
          // Mark duplicates
          for(int i = 0; i < knn.size() && knn.doubleValue(i) == 0.; i++) {
            coredist[ids.index(knn.iter().seek(i))] = knn.getKNNDistance();
          }
        }
      }
      LOG.ensureCompleted(cprog);
    }

    /**
     * Build the initial heap.
     * 
     * @param relation data relation
     */
    private void initializeHeap(Relation<? extends O> relation) {
      FiniteProgress iprog = LOG.isVerbose() ? new FiniteProgress("Heap initialization", ids.size(), LOG) : null;
      this.heap = new DoubleIntegerMinHeap(ids.size());
      @SuppressWarnings("unchecked")
      PrioritySearcher<DBIDRef>[] pqs = (PrioritySearcher<DBIDRef>[]) new PrioritySearcher<?>[ids.size()];
      this.pqs = pqs;
      this.heaps = new DoubleIntegerMinHeap[ids.size()];
      for(DBIDArrayIter ita = ids.iter(); ita.valid(); ita.advance(), LOG.incrementProcessed(iprog)) {
        int a = ita.getOffset(), ca = builder.get(a);
        if(builder.getSize(ca) > 1) {
          continue; // duplicate
        }
        double cd = coredist[a];
        DoubleIntegerMinHeap h = heaps[a] = new DoubleIntegerMinHeap();
        PrioritySearcher<DBIDRef> pq = this.pqs[a] = new QueryBuilder<>(relation, distance).priorityByDBID();
        // Initial search to get an initial priority
        double thres = Double.POSITIVE_INFINITY;
        for(pq.search(DBIDUtil.deref(ita)); pq.valid() && pq.allLowerBound() < thres; pq.advance()) {
          final int b = ids.index(pq);
          if(a == b) {
            continue;
          }
          final double d = pq.computeExactDistance();
          if(d == 0.) { // duplicate, merge immediately
            int cb = builder.get(b);
            if(ca != cb) {
              ca = builder.add(ca, cd, cb);
            }
            continue;
          }
          double rd = MathUtil.max(cd, d, coredist[b]);
          h.add(rd, b);
          thres = h.peekKey();
          // do not use pq.decreaseCutoff, as we continue later
        }
        if(!h.isEmpty()) {
          heap.add(thres, a);
        }
      }
      LOG.ensureCompleted(iprog);
      if(LOG.isDebugging()) {
        LOG.debug("Performed " + builder.mergecount + " merges of duplicates (may involve more objects) during initialization.");
      }
    }

    /**
     * Refill the nearest neighbors.
     * 
     * @param a Query object number
     * @param ca Cluster id of the query object
     */
    private void refillNeighbors(int a, int ca) {
      PrioritySearcher<DBIDRef> pq = pqs[a];
      DoubleIntegerMinHeap h = heaps[a];
      double thres = h.isEmpty() ? Double.POSITIVE_INFINITY : h.peekKey();
      double cd = coredist[a];
      for(; pq.valid() && pq.allLowerBound() < thres; pq.advance()) {
        final int b = ids.index(pq);
        if(a == b || builder.get(b) == ca) {
          continue;
        }
        double d = pq.computeExactDistance();
        double rd = MathUtil.max(cd, d, coredist[b]);
        h.add(rd, b);
        thres = h.peekKey();
        // do not use pq.decreaseCutoff, as we continue with the searcher
      }
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
    @Override
    public HDBSCANHS<O> make() {
      return new HDBSCANHS<>(distance, minPts);
    }
  }
}
