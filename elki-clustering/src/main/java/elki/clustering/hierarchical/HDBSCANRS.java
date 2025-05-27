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
import elki.utilities.datastructures.heap.DoubleIntegerMinHeap;

/**
 * Index accelerated HDBSCAN with a Restarting-Search strategy.
 * with a heap-of-candidates strategy. In the worst case this can be much
 * slower, but it can also work very well and needs much less memory than the
 * heap-of-searchers approach.
 * <p>
 * Reference:
 *
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
@Alias({ "HDBSCAN-RS" })
public class HDBSCANRS<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(HDBSCANRS.class);

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
  public HDBSCANRS(Distance<? super O> distance, int minPts) {
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
    private int[] nns;

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
      initializeHeap();
      FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Clustering", ids.size(), LOG) : null;
      if(cprog != null) {
        cprog.setProcessed(builder.mergecount + 1, LOG);
      }
      while(!heap.isEmpty()) {
        final double curd = heap.peekKey();
        int a = heap.peekValue(), b = nns[a];
        int ca = builder.get(a), cb = builder.get(b);
        if(ca != cb) {
          ca = builder.add(ca, curd, cb);
          LOG.incrementProcessed(cprog);
          if(builder.mergecount == ids.size() - 1) {
            break;
          }
        }
        // Update nn of a:
        double dist = refillNeighbors(a, ca, curd);
        if(nns[a] < 0) { // or dist == Double.POSITIVE_INFINITY
          heap.poll();
          continue;
        }
        heap.replaceTopElement(dist, a);
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
        if(coredist[a] == coredist[a]) { // not NaN, duplicate
          continue;
        }
        KNNList knn = knnq.getKNN(ita, minPts);
        coredist[a] = knn.getKNNDistance();
        // Mark duplicates
        for(int i = 0; i < knn.size() && knn.doubleValue(i) == 0.; i++) {
          coredist[ids.index(knn.iter().seek(i))] = knn.getKNNDistance();
        }
      }
      LOG.ensureCompleted(cprog);
    }

    /**
     * Build the initial heap.
     */
    private void initializeHeap() {
      FiniteProgress iprog = LOG.isVerbose() ? new FiniteProgress("Heap initialization", ids.size(), LOG) : null;
      this.heap = new DoubleIntegerMinHeap(ids.size());
      this.nns = new int[ids.size()];
      for(ita.seek(0); ita.valid(); ita.advance(), LOG.incrementProcessed(iprog)) {
        int a = ita.getOffset(), ca = builder.get(a);
        if(builder.getSize(ca) > 1) {
          continue; // duplicate
        }
        double cd = coredist[a];
        int best = -1;
        double bestd = Double.POSITIVE_INFINITY;
        for(pq.search(ita); pq.valid(); pq.advance()) {
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
          if(rd < bestd) {
            best = b;
            pq.decreaseCutoff(bestd = rd);
          }
        }
        if(best >= 0) {
          heap.add(bestd, a);
          nns[a] = best;
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
     * Refill the nearest neighbors.
     * <p>
     * If the current distance is less than the core distance, we use a skip of
     * 0 because the current distance is the core distance and not a lower bound
     * to the actual distance.
     * 
     * @param a Query object number
     * @param ca Cluster id of the query object
     * @param curd Current distance, for skipping
     */
    private double refillNeighbors(int a, int ca, double curd) {
      double cd = coredist[a];
      double skip = curd >= cd ? curd : 0;
      double thres = Double.POSITIVE_INFINITY;
      int best = -1;
      if(last != a) {
        pq.search(ita.seek(a)).increaseSkip(skip);
        last = a;
      }
      for(; pq.valid() && pq.allLowerBound() < thres; pq.advance()) {
        final int b = ids.index(pq);
        if(a == b || builder.get(b) == ca) {
          continue;
        }
        double d = pq.computeExactDistance();
        if(d < skip) {
          continue;
        }
        double rd = MathUtil.max(cd, d, coredist[b]);
        if(rd < thres) {
          best = b;
          thres = rd;
          // do not use pq.decreaseCutoff, as we may continue with the searcher
        }
      }
      nns[a] = best;
      return thres;
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
    public HDBSCANRS<O> make() {
      return new HDBSCANRS<>(distance, minPts);
    }
  }
}
